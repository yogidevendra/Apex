/*
 *  Copyright (c) 2012 Malhar, Inc.
 *  All Rights Reserved.
 */
package com.malhartech.dag;

import com.malhartech.annotation.NodeAnnotation;
import com.malhartech.annotation.PortAnnotation;
import com.malhartech.util.CircularBuffer;
import java.nio.BufferOverflowException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import org.apache.hadoop.test.GenericTestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Chetan Narsude <chetan@malhar-inc.com>
 */
public abstract class AbstractInputNode implements Node, Runnable
{
  private static final Logger logger = LoggerFactory.getLogger(AbstractInputNode.class);
  private transient String id;
  private transient HashMap<String, CircularBuffer<Object>> afterBeginWindows;
  private transient HashMap<String, CircularBuffer<Tuple>> afterEndWindows;
  private transient HashMap<String, Sink> outputs = new HashMap<String, Sink>();
  @SuppressWarnings("VolatileArrayField")
  private transient volatile Sink[] sinks = NO_SINKS;
  private transient NodeContext ctx;
  private transient volatile int producedTupleCount;
  private transient volatile int spinMillis;
  private transient volatile int bufferCapacity;

  @Override
  public void setup(NodeConfiguration config)
  {
    id = config.get("Id");
    spinMillis = config.getInt("SpinMillis", 10);
    bufferCapacity = config.getInt("BufferCapacity", 1024 * 1024);

    afterBeginWindows = new HashMap<String, CircularBuffer<Object>>();
    afterEndWindows = new HashMap<String, CircularBuffer<Tuple>>();

    Class<? extends Node> clazz = this.getClass();
    NodeAnnotation na = clazz.getAnnotation(NodeAnnotation.class);
    if (na != null) {
      PortAnnotation[] ports = na.ports();
      for (PortAnnotation pa: ports) {
        if (pa.type() == PortAnnotation.PortType.OUTPUT || pa.type() == PortAnnotation.PortType.BIDI) {
          afterBeginWindows.put(pa.name(), new CircularBuffer<Object>(bufferCapacity));
          afterEndWindows.put(pa.name(), new CircularBuffer<Tuple>(bufferCapacity));
        }
      }
    }
  }

  @Override
  @SuppressWarnings("SleepWhileInLoop")
  public final void activate(NodeContext context)
  {
    ctx = context;
    activateSinks();
    run();

    try {
      EndStreamTuple est = new EndStreamTuple();
      for (CircularBuffer<Tuple> cb: afterEndWindows.values()) {
        while (true) {
          try {
            cb.add(est);
            break;
          }
          catch (BufferOverflowException boe) {
            Thread.sleep(spinMillis);
          }
        }
      }

      /*
       * make sure that it's sent.
       */
      boolean pendingMessages;
      do {
        Thread.sleep(spinMillis);

        pendingMessages = false;
        for (CircularBuffer<Tuple> cb: afterEndWindows.values()) {
          if (cb.size() > 0) {
            pendingMessages = true;
            break;
          }
        }
      }
      while (pendingMessages);
    }
    catch (InterruptedException ex) {
      logger.info("Not waiting for the emitted tuples to be flushed as got interrupted by {}", ex.getLocalizedMessage());
    }
  }

  @Override
  public final void deactivate()
  {
    sinks = NO_SINKS;
  }

  @Override
  public void teardown()
  {
    outputs.clear();

    afterEndWindows.clear();
    afterEndWindows = null;
    afterBeginWindows.clear();
    afterBeginWindows = null;
    // Should make variable "shutdown" part of AbstrastInputNode, users should not have to override teardown()
    // as they may forget to call super.teardown()
    // Also move "outputconnected" here as that is a very common need
  }

  @Override
  public final Sink connect(String port, Sink component)
  {
    Sink retvalue;
    if (Component.INPUT.equals(port)) {
      retvalue = this;
    }
    else {
      if (component == null) {
        outputs.remove(port);
      }
      else {
        outputs.put(port, component);
      }
      if (sinks != NO_SINKS) {
        activateSinks();
      }
      retvalue = null;
    }

    connected(port, component);
    return retvalue;
  }

  public void connected(String id, Sink dagpart)
  {
    /* implementation to be optionally overridden by the user */
  }

  @Override
  @SuppressWarnings("SillyAssignment")
  public final void process(Object payload)
  {
    Tuple t = (Tuple)payload;
    switch (t.getType()) {
      case BEGIN_WINDOW:
        for (int i = sinks.length; i-- > 0;) {
          sinks[i].process(payload);
        }

        for (Entry<String, CircularBuffer<Object>> e: afterBeginWindows.entrySet()) {
          final Sink s = outputs.get(e.getKey());
          CircularBuffer<?> cb = e.getValue();
          for (int i = cb.size(); i-- > 0;) {
            s.process(cb.get());
          }
        }
        break;

      case END_WINDOW:
        for (Entry<String, CircularBuffer<Object>> e: afterBeginWindows.entrySet()) {
          final Sink s = outputs.get(e.getKey());
          CircularBuffer<?> cb = e.getValue();
          for (int i = cb.size(); i-- > 0;) {
            s.process(cb.get());
          }
        }
        for (final Sink s: sinks) {
          s.process(payload);
        }

        ctx.report(producedTupleCount, 0L, ((Tuple)payload).getWindowId());
        producedTupleCount = 0;

        // the default is UNSPECIFIED which we ignore anyways as we ignore everything
        // that we do not understand!
        try {
          switch (ctx.getRequestType()) {
            case BACKUP:
              ctx.backup(this, ((Tuple)payload).getWindowId());
              break;

            case RESTORE:
              logger.info("restore requests are not implemented");
              break;
          }
        }
        catch (Exception e) {
          logger.warn("Exception while catering to external request", e.getLocalizedMessage());
        }

        // i think there should be just one queue instead of one per port - lets defer till we find an example.
        for (Entry<String, CircularBuffer<Tuple>> e: afterEndWindows.entrySet()) {
          final Sink s = outputs.get(e.getKey());
          CircularBuffer<?> cb = e.getValue();
          for (int i = cb.size(); i-- > 0;) {
            s.process(cb.get());
          }
        }
        break;

      default:
        for (final Sink s: sinks) {
          s.process(payload);
        }
    }
  }

  @SuppressWarnings("SleepWhileInLoop")
  public void emit(String id, Object payload)
  {
    if (payload instanceof Tuple) {
      while (true) {
        try {
          afterEndWindows.get(id).add((Tuple)payload);
          break;
        }
        catch (BufferOverflowException ex) {
          try {
            Thread.sleep(spinMillis);
          }
          catch (InterruptedException ex1) {
            break;
          }
        }
      }
    }
    else {
      while (true) {
        try {
          afterBeginWindows.get(id).add(payload);
          break;
        }
        catch (BufferOverflowException ex) {
          try {
            Thread.sleep(spinMillis);
          }
          catch (InterruptedException ex1) {
            break;
          }
        }
      }
    }

    producedTupleCount++;
  }

  @Override
  public void beginWindow()
  {
  }

  @Override
  public void endWindow()
  {
  }

  @Override
  public int hashCode()
  {
    return id == null ? super.hashCode() : id.hashCode();
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final AbstractInputNode other = (AbstractInputNode)obj;
    if ((this.id == null) ? (other.id != null) : !this.id.equals(other.id)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "{" + "id=" + id + ", outputs=" + outputs.keySet() + '}';
  }

  @SuppressWarnings("SillyAssignment")
  private void activateSinks()
  {
    sinks = new Sink[outputs.size()];

    int i = 0;
    for (Sink s: outputs.values()) {
      sinks[i++] = s;
    }
    sinks = sinks;
  }
}
