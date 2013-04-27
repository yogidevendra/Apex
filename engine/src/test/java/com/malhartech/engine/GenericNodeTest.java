/*
 *  Copyright (c) 2012 Malhar, Inc.
 *  All Rights Reserved.
 */
package com.malhartech.engine;

import com.malhartech.api.Context.PortContext;
import com.malhartech.api.*;
import com.malhartech.bufferserver.packet.MessageType;
import com.malhartech.tuple.EndStreamTuple;
import com.malhartech.tuple.EndWindowTuple;
import com.malhartech.tuple.Tuple;
import com.malhartech.util.AttributeMap;
import com.malhartech.util.AttributeMap.AttributeKey;
import com.malhartech.util.AttributeMap.DefaultAttributeMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author Chetan Narsude <chetan@malhar-inc.com>
 */
public class GenericNodeTest
{
  long beginWindowId;
  long endWindowId;

  class GenericOperator implements Operator
  {
    DefaultInputPort<Object> ip1 = new DefaultInputPort<Object>(this)
    {
      @Override
      public void process(Object tuple)
      {
        op.emit(tuple);
      }

    };
    DefaultInputPort<Object> ip2 = new DefaultInputPort<Object>(this)
    {
      @Override
      public void process(Object tuple)
      {
        op.emit(tuple);
      }

    };
    DefaultOutputPort<Object> op = new DefaultOutputPort<Object>(this);

    @Override
    public void beginWindow(long windowId)
    {
      beginWindowId = windowId;
    }

    @Override
    public void endWindow()
    {
      endWindowId = beginWindowId;
    }

    @Override
    public void setup(Context.OperatorContext context)
    {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void teardown()
    {
      throw new UnsupportedOperationException("Not supported yet.");
    }

  }

  @Test
  @SuppressWarnings("SleepWhileInLoop")
  public void testSynchingLogic() throws InterruptedException
  {
    long sleeptime = 25L;
    final ArrayList<Object> list = new ArrayList<Object>();
    GenericOperator go = new GenericOperator();
    final GenericNode gn = new GenericNode(1, go);
    DefaultReservoir reservoir1 = new DefaultReservoir("ip1Res", 1024);
    DefaultReservoir reservoir2 = new DefaultReservoir("ip2Res", 1024);
    Sink<Object> output = new Sink<Object>()
    {
      @Override
      public void process(Object tuple)
      {
        list.add(tuple);
      }

    };

    AttributeMap<PortContext> attributes = new AttributeMap<PortContext>()
    {
      @Override
      public <T> Attribute<T> attr(AttributeKey<PortContext, T> key)
      {
        return null;
      }

      @Override
      public <T> T attrValue(AttributeKey<PortContext, T> key, T defaultValue)
      {
        return defaultValue;
      }

    };
    gn.connectInputPort("ip1", reservoir1);
    gn.connectInputPort("ip2", reservoir2);
    gn.connectOutputPort("op", output);

    final AtomicBoolean ab = new AtomicBoolean(false);
    Thread t = new Thread()
    {
      @Override
      public void run()
      {
        ab.set(true);
        gn.activate(new OperatorContext(0, this, new DefaultAttributeMap<com.malhartech.api.Context.OperatorContext>(), new DefaultAttributeMap<com.malhartech.api.DAGContext>(), new HashMap<String, AttributeMap<com.malhartech.api.Context.PortContext>>(), new HashMap<String, AttributeMap<com.malhartech.api.Context.PortContext>>()));
      }

    };
    t.start();

    do {
      Thread.sleep(sleeptime);
    }
    while (ab.get() == false);


    Tuple beginWindow1 = new Tuple(MessageType.BEGIN_WINDOW);
    beginWindow1.setWindowId(0x1L);

    reservoir1.add(beginWindow1);
    Thread.sleep(sleeptime);
    Assert.assertEquals(1, list.size());

    reservoir2.add(beginWindow1);
    Thread.sleep(sleeptime);
    Assert.assertEquals(1, list.size());

    Tuple endWindow1 = new EndWindowTuple(0x1L);

    reservoir1.add(endWindow1);
    Thread.sleep(sleeptime);
    Assert.assertEquals(1, list.size());

    Tuple beginWindow2 = new Tuple(MessageType.BEGIN_WINDOW);
    beginWindow2.setWindowId(0x2L);

    reservoir1.add(beginWindow2);
    Thread.sleep(sleeptime);
    Assert.assertEquals(1, list.size());

    reservoir2.add(endWindow1);
    Thread.sleep(sleeptime);
    Assert.assertEquals(3, list.size());

    reservoir2.add(beginWindow2);
    Thread.sleep(sleeptime);
    Assert.assertEquals(3, list.size());

    Tuple endWindow2 = new EndWindowTuple(0x2L);

    reservoir2.add(endWindow2);
    Thread.sleep(sleeptime);
    Assert.assertEquals(3, list.size());

    reservoir1.add(endWindow2);
    Thread.sleep(sleeptime);
    Assert.assertEquals(4, list.size());

    EndStreamTuple est = new EndStreamTuple(0L);

    reservoir1.add(est);
    Thread.sleep(sleeptime);
    Assert.assertEquals(4, list.size());

    Tuple beginWindow3 = new Tuple(MessageType.BEGIN_WINDOW);
    beginWindow3.setWindowId(0x3L);

    reservoir2.add(beginWindow3);
    Thread.sleep(sleeptime);
    Assert.assertEquals(5, list.size());

    Tuple endWindow3 = new EndWindowTuple(0x3L);

    reservoir2.add(endWindow3);
    Thread.sleep(sleeptime);
    Assert.assertEquals(6, list.size());

    Assert.assertNotSame(Thread.State.TERMINATED, t.getState());

    reservoir2.add(est);
    Thread.sleep(sleeptime);
    Assert.assertEquals(7, list.size());

    Thread.sleep(sleeptime);

    Assert.assertEquals(Thread.State.TERMINATED, t.getState());
  }

}
