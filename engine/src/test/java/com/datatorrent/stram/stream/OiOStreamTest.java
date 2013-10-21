/*
 *  Copyright (c) 2012-2013 Malhar, Inc.
 *  All Rights Reserved.
 */
package com.datatorrent.stram.stream;

import com.datatorrent.api.*;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datatorrent.api.Context.OperatorContext;
import com.datatorrent.api.DAG.Locality;

import com.datatorrent.stram.StramLocalCluster;
import com.datatorrent.stram.engine.GenericNodeTest.GenericOperator;
import com.datatorrent.stram.engine.ProcessingModeTests.CollectorOperator;
import com.datatorrent.stram.engine.RecoverableInputOperator;
import com.datatorrent.stram.plan.logical.LogicalPlan;
import com.datatorrent.stram.plan.logical.LogicalPlan.StreamMeta;
import java.util.*;

/**
 *
 * @author Chetan Narsude <chetan@datatorrent.com>
 */
public class OiOStreamTest
{
  public OiOStreamTest()
  {
  }

  //@Test
  public void validatePositiveOiO()
  {
    logger.info("Checking the logic for sanity checking of OiO");

    LogicalPlan plan = new LogicalPlan();
    RecoverableInputOperator inputOperator = plan.addOperator("IntegerGenerator", new RecoverableInputOperator());
    CollectorOperator outputOperator = plan.addOperator("IntegerCollector", new CollectorOperator());
    plan.addStream("PossibleOiO", inputOperator.output, outputOperator.input).setLocality(Locality.THREAD_LOCAL);

    try {
      plan.validate();
      Assert.assertTrue("OiO validation", true);
    }
    catch (ConstraintViolationException ex) {
      Assert.fail("OIO Single InputPort");
    }
    catch (ValidationException ex) {
      Assert.fail("OIO Single InputPort");
    }
  }

  //@Test
  public void validatePositiveOiOiO()
  {
    logger.info("Checking the logic for sanity checking of OiO");

    LogicalPlan plan = new LogicalPlan();
    ThreadIdValidatingInputOperator inputOperator = plan.addOperator("inputOperator", new ThreadIdValidatingInputOperator());
    ThreadIdValidatingGenericIntermediateOperator intermediateOperator = plan.addOperator("intermediateOperator", new ThreadIdValidatingGenericIntermediateOperator());
    ThreadIdValidatingOutputOperator outputOperator = plan.addOperator("outputOperator", new ThreadIdValidatingOutputOperator());

    plan.addStream("OiO1", inputOperator.output, intermediateOperator.input).setLocality(Locality.THREAD_LOCAL);
    plan.addStream("OiO2", intermediateOperator.output, outputOperator.input).setLocality(Locality.THREAD_LOCAL);

    try {
      plan.validate();
      Assert.assertTrue("OiO validation", true);
    }
    catch (ConstraintViolationException ex) {
      Assert.fail("OIO Single InputPort");
    }
    catch (ValidationException ex) {
      Assert.fail("OIO Single InputPort");
    }
  }

  //@Test
  public void validatePositiveOiOiOdiamond()
  {
    logger.info("Checking the logic for sanity checking of OiO");

    LogicalPlan plan = new LogicalPlan();
    ThreadIdValidatingInputOperator inputOperator = plan.addOperator("inputOperator", new ThreadIdValidatingInputOperator());
    ThreadIdValidatingGenericIntermediateOperator intermediateOperator1 = plan.addOperator("intermediateOperator1", new ThreadIdValidatingGenericIntermediateOperator());
    ThreadIdValidatingGenericIntermediateOperator intermediateOperator2 = plan.addOperator("intermediateOperator2", new ThreadIdValidatingGenericIntermediateOperator());
    ThreadIdValidatingGenericOperatorWithTwoInputPorts outputOperator = plan.addOperator("outputOperator", new ThreadIdValidatingGenericOperatorWithTwoInputPorts());

    plan.addStream("OiOin", inputOperator.output, intermediateOperator1.input, intermediateOperator2.input).setLocality(Locality.THREAD_LOCAL);
    plan.addStream("OiOout1", intermediateOperator1.output, outputOperator.input).setLocality(Locality.THREAD_LOCAL);
    plan.addStream("OiOout2", intermediateOperator2.output, outputOperator.input2).setLocality(Locality.THREAD_LOCAL);

    try {
      plan.validate();
      Assert.assertTrue("OiO validation", true);
    }
    catch (ConstraintViolationException ex) {
      Assert.fail("OIO Single InputPort");
    }
    catch (ValidationException ex) {
      Assert.fail("OIO Single InputPort");
    }
  }

  //@Test
  public void validatePositiveOiOOptionalInput()
  {
    LogicalPlan plan = new LogicalPlan();
    RecoverableInputOperator inputOp1 = plan.addOperator("InputOperator1", new RecoverableInputOperator());
    GenericOperator genOp = plan.addOperator("GenericOperator", new GenericOperator());
    plan.addStream("OiO1", inputOp1.output, genOp.ip1).setLocality(Locality.THREAD_LOCAL);

    try {
      plan.validate();
      Assert.assertTrue("OiO validation", true);
    }
    catch (ConstraintViolationException ex) {
      Assert.fail("OiO Single Connected InputPort");
    }
    catch (ValidationException ex) {
      Assert.fail("OiO Single Connected InputPort");
    }
  }

  //@Test
  public void validateNegativeOiO()
  {
    LogicalPlan plan = new LogicalPlan();
    RecoverableInputOperator inputOp1 = plan.addOperator("InputOperator1", new RecoverableInputOperator());
    RecoverableInputOperator inputOp2 = plan.addOperator("InputOperator2", new RecoverableInputOperator());
    GenericOperator genOp = plan.addOperator("GenericOperator", new GenericOperator());
    StreamMeta oio1 = plan.addStream("OiO1", inputOp1.output, genOp.ip1).setLocality(Locality.THREAD_LOCAL);
    StreamMeta oio2 = plan.addStream("OiO2", inputOp2.output, genOp.ip2).setLocality(Locality.THREAD_LOCAL);

    try {
      plan.validate();
      Assert.fail("OIO Both InputPorts");
    }
    catch (ConstraintViolationException ex) {
      Assert.assertTrue("OiO validation passed", true);
    }
    catch (ValidationException ex) {
      Assert.assertTrue("OiO validation passed", true);
    }

    oio1.setLocality(null);
    try {
      plan.validate();
      Assert.fail("OIO First InputPort");
    }
    catch (ConstraintViolationException ex) {
      Assert.assertTrue("OiO validation passed", true);
    }
    catch (ValidationException ex) {
      Assert.assertTrue("OiO validation passed", true);
    }

    oio1.setLocality(Locality.THREAD_LOCAL);
    oio2.setLocality(null);
    try {
      plan.validate();
      Assert.fail("OIO Second InputPort");
    }
    catch (ConstraintViolationException ex) {
      Assert.assertTrue("OiO validation passed", true);
    }
    catch (ValidationException ex) {
      Assert.assertTrue("OiO validation passed", true);
    }
  }

  public static class ThreadIdValidatingInputOperator implements InputOperator
  {
    public final transient DefaultOutputPort<Long> output = new DefaultOutputPort<Long>();
    //public final transient DefaultOutputPort<Long> output2 = new DefaultOutputPort<Long>();
    public static long threadId;

    @Override
    public void emitTuples()
    {
      assert (threadId == Thread.currentThread().getId());
    }

    @Override
    public void beginWindow(long windowId)
    {
      assert (threadId == Thread.currentThread().getId());
    }

    @Override
    public void endWindow()
    {
      assert (threadId == Thread.currentThread().getId());
      throw new RuntimeException(new InterruptedException());
    }

    @Override
    public void setup(OperatorContext context)
    {
      threadId = Thread.currentThread().getId();
    }

    @Override
    public void teardown()
    {
      //assert (threadId == Thread.currentThread().getId());
    }

  }

  public static class ThreadIdValidatingOutputOperator implements Operator
  {
    public static long threadId;
    public static List<Long> threadList = new ArrayList<Long>();

    public final transient DefaultInputPort<Number> input = new DefaultInputPort<Number>()
    {
      @Override
      public void process(Number tuple)
      {
        assert (threadList.contains(Thread.currentThread().getId()));
      }

    };

    @Override
    public void beginWindow(long windowId)
    {
      assert (threadList.contains(Thread.currentThread().getId()));
    }

    @Override
    public void endWindow()
    {
      assert (threadList.contains(Thread.currentThread().getId()));
    }

    @Override
    public void setup(OperatorContext context)
    {
      threadId = Thread.currentThread().getId();
      threadList.add(Thread.currentThread().getId());
    }

    @Override
    public void teardown()
    {
      assert (threadList.contains(Thread.currentThread().getId()));
    }

  }

  public static class ThreadIdValidatingGenericIntermediateOperator implements Operator
  {
    public static long threadId;
    //public static HashMap<Integer, Long> threadMap = new HashMap<Integer, Long>();
    public static List<Long> threadList = Collections.synchronizedList(new ArrayList<Long>());

    public final transient DefaultInputPort<Number> input = new DefaultInputPort<Number>()
    {
      @Override
      public void process(Number tuple)
      {
        assert (threadList.contains(Thread.currentThread().getId()));
      }

    };

    @Override
    public void beginWindow(long windowId)
    {
      assert (threadList.contains(Thread.currentThread().getId()));
    }

    @Override
    public void endWindow()
    {
      assert (threadList.contains(Thread.currentThread().getId()));
    }

    @Override
    public void setup(OperatorContext context)
    {
      threadId = Thread.currentThread().getId();
      //int operatorId = context.getId();
      //int hashCode1 = this.hashCode();
      //int hashCode2 = Thread.currentThread().hashCode();
      //if(!threadList.contains(threadId)){
        threadList.add(Thread.currentThread().getId());
      //}

      //threadMap.put(this.hashCode(), Thread.currentThread().getId());
    }

    @Override
    public void teardown()
    {
      //assert (threadId == Thread.currentThread().getId());
    }

    public final transient DefaultOutputPort<Long> output = new DefaultOutputPort<Long>();
  }

  public static class ThreadIdValidatingGenericOperatorWithTwoInputPorts implements Operator
  {
    public static long threadId;
    public final transient DefaultInputPort<Number> input = new DefaultInputPort<Number>()
    {
      @Override
      public void process(Number tuple)
      {
        assert (threadId == Thread.currentThread().getId());
      }

    };

    public final transient DefaultInputPort<Number> input2 = new DefaultInputPort<Number>()
    {
      @Override
      public void process(Number tuple)
      {
        assert (threadId == Thread.currentThread().getId());
      }

    };

    @Override
    public void beginWindow(long windowId)
    {
      assert (threadId == Thread.currentThread().getId());
    }

    @Override
    public void endWindow()
    {
      assert (threadId == Thread.currentThread().getId());
    }

    @Override
    public void setup(OperatorContext context)
    {
      threadId = Thread.currentThread().getId();
    }

    @Override
    public void teardown()
    {
      assert (threadId == Thread.currentThread().getId());
    }

    //public final transient DefaultOutputPort<Long> output = new DefaultOutputPort<Long>();

  }

  //@Test
  public void validateOiOImplementation() throws Exception
  {
    LogicalPlan lp = new LogicalPlan();
    ThreadIdValidatingInputOperator io = lp.addOperator("Input Operator", new ThreadIdValidatingInputOperator());
    ThreadIdValidatingOutputOperator go = lp.addOperator("Output Operator", new ThreadIdValidatingOutputOperator());
    StreamMeta stream = lp.addStream("Stream", io.output, go.input);

    /* The first test makes sure that when they are not ThreadLocal they use different threads */
    lp.validate();
    StramLocalCluster slc = new StramLocalCluster(lp);
    slc.run();
    Assert.assertFalse("Thread Id", ThreadIdValidatingInputOperator.threadId == ThreadIdValidatingOutputOperator.threadId);

    /* This test makes sure that since they are ThreadLocal, they indeed share a thread */
    stream.setLocality(Locality.THREAD_LOCAL);
    lp.validate();
    slc = new StramLocalCluster(lp);
    slc.run();
    Assert.assertEquals("Thread Id", ThreadIdValidatingInputOperator.threadId, ThreadIdValidatingOutputOperator.threadId);
  }


  //@Test
  public void validateOiOiOImplementation() throws Exception
  {
    LogicalPlan lp = new LogicalPlan();
    ThreadIdValidatingInputOperator inputOperator = lp.addOperator("inputOperator", new ThreadIdValidatingInputOperator());
    ThreadIdValidatingGenericIntermediateOperator intermediateOperator = lp.addOperator("intermediateOperator", new ThreadIdValidatingGenericIntermediateOperator());
    ThreadIdValidatingOutputOperator outputOperator = lp.addOperator("outputOperator", new ThreadIdValidatingOutputOperator());

    StreamMeta stream1 = lp.addStream("OiO1", inputOperator.output, intermediateOperator.input);
    StreamMeta stream2 = lp.addStream("OiO2", intermediateOperator.output, outputOperator.input);

    StramLocalCluster slc;

    /* The first test makes sure that when they are not ThreadLocal they use different threads */
    lp.validate();
    slc = new StramLocalCluster(lp);
    slc.run();
    Assert.assertFalse("Thread Id 1", ThreadIdValidatingInputOperator.threadId == ThreadIdValidatingGenericIntermediateOperator.threadId);
    Assert.assertFalse("Thread Id 2", ThreadIdValidatingGenericIntermediateOperator.threadId == ThreadIdValidatingOutputOperator.threadId);

    /* This test makes sure that since they are ThreadLocal, they indeed share a thread */
    stream1.setLocality(Locality.THREAD_LOCAL);
    stream2.setLocality(Locality.THREAD_LOCAL);
    lp.validate();
    slc = new StramLocalCluster(lp);
    slc.run();
    Assert.assertEquals("Thread Id 3", ThreadIdValidatingInputOperator.threadId, ThreadIdValidatingGenericIntermediateOperator.threadId);
    Assert.assertEquals("Thread Id 4", ThreadIdValidatingGenericIntermediateOperator.threadId, ThreadIdValidatingOutputOperator.threadId);
  }

  //@Test
  public void validateOiOiODiamondImplementation() throws Exception
  {
    LogicalPlan lp = new LogicalPlan();
    ThreadIdValidatingInputOperator inputOperator = lp.addOperator("inputOperator", new ThreadIdValidatingInputOperator());
    ThreadIdValidatingGenericIntermediateOperator intermediateOperator1 = lp.addOperator("intermediateOperator1", new ThreadIdValidatingGenericIntermediateOperator());
    ThreadIdValidatingGenericIntermediateOperator intermediateOperator2 = lp.addOperator("intermediateOperator2", new ThreadIdValidatingGenericIntermediateOperator());
    ThreadIdValidatingGenericOperatorWithTwoInputPorts outputOperator = lp.addOperator("outputOperator", new ThreadIdValidatingGenericOperatorWithTwoInputPorts());
    //ThreadIdValidatingOutputOperator idValidatingOutputOperator = lp.addOperator("outputSink", new ThreadIdValidatingOutputOperator());

    StreamMeta stream1 = lp.addStream("OiOinput", inputOperator.output, intermediateOperator1.input, intermediateOperator2.input);
    StreamMeta stream2 = lp.addStream("OiOintermediateToOutput1", intermediateOperator1.output, outputOperator.input);
    StreamMeta stream3 = lp.addStream("OiOintermediateToOutput2", intermediateOperator2.output, outputOperator.input2);
    //lp.addStream("NonOiOOutputSink", outputOperator.output, idValidatingOutputOperator.input);

    StramLocalCluster slc;

    /*
     * The first test makes sure that when they are not ThreadLocal they use different threads
     */

    lp.validate();
    slc = new StramLocalCluster(lp);
    slc.run();

    Assert.assertEquals("nonOIO: Number of threads", 2 ,ThreadIdValidatingGenericIntermediateOperator.threadList.size());
    Assert.assertFalse("nonOIO: Thread Ids of input operator and intermediate operator1",
                        ThreadIdValidatingInputOperator.threadId == (long)ThreadIdValidatingGenericIntermediateOperator.threadList.get(0));
    Assert.assertFalse("nonOIO: Thread Ids of input operator and intermediate operator2",
                        ThreadIdValidatingInputOperator.threadId == (long)ThreadIdValidatingGenericIntermediateOperator.threadList.get(1));
    Assert.assertFalse("nonOIO: Thread Ids of two intermediate operators", ThreadIdValidatingGenericIntermediateOperator.threadList.get(0) == ThreadIdValidatingGenericIntermediateOperator.threadList.get(1));
    Assert.assertFalse("nonOIO: Thread Ids of input and output operators", ThreadIdValidatingInputOperator.threadId == outputOperator.threadId);

    /*
     * This test makes sure that since all operators in diamond are ThreadLocal, they indeed share a thread
     */

    ThreadIdValidatingGenericIntermediateOperator.threadList.clear();
    stream1.setLocality(Locality.THREAD_LOCAL);
    stream2.setLocality(Locality.THREAD_LOCAL);
    stream3.setLocality(Locality.THREAD_LOCAL);
    lp.validate();
    slc = new StramLocalCluster(lp);
    slc.run();

    Assert.assertEquals("OIO: Number of threads", 2 ,ThreadIdValidatingGenericIntermediateOperator.threadList.size());
    Assert.assertEquals("OIO: Thread Ids of input operator and intermediate operator1",
                        ThreadIdValidatingInputOperator.threadId, (long)ThreadIdValidatingGenericIntermediateOperator.threadList.get(0));
    Assert.assertEquals("OIO: Thread Ids of input operator and intermediate operator2",
                        ThreadIdValidatingInputOperator.threadId, (long)ThreadIdValidatingGenericIntermediateOperator.threadList.get(1));
    Assert.assertEquals("OIO: Thread Ids of two intermediate operators", ThreadIdValidatingGenericIntermediateOperator.threadList.get(0), ThreadIdValidatingGenericIntermediateOperator.threadList.get(1));
    Assert.assertEquals("OIO: Thread Ids of input and output operators", ThreadIdValidatingInputOperator.threadId, outputOperator.threadId);

    /*
     * This test makes sure that since one input stream to the output stream is not ThreadLocal, the output stream does not share a thread
     */
    ThreadIdValidatingGenericIntermediateOperator.threadList.clear();
    stream1.setLocality(Locality.THREAD_LOCAL);
    stream2.setLocality(null);
    stream3.setLocality(Locality.THREAD_LOCAL);
    lp.validate();
    slc = new StramLocalCluster(lp);
    slc.run();

    Assert.assertEquals("OIO: Number of threads", 2 ,ThreadIdValidatingGenericIntermediateOperator.threadList.size());
    Assert.assertEquals("OIO: Thread Ids of input operator and intermediate operator1",
                        ThreadIdValidatingInputOperator.threadId, (long)ThreadIdValidatingGenericIntermediateOperator.threadList.get(0));
    Assert.assertEquals("OIO: Thread Ids of input operator and intermediate operator2",
                        ThreadIdValidatingInputOperator.threadId, (long)ThreadIdValidatingGenericIntermediateOperator.threadList.get(1));
    Assert.assertEquals("OIO: Thread Ids of two intermediate operators", ThreadIdValidatingGenericIntermediateOperator.threadList.get(0), ThreadIdValidatingGenericIntermediateOperator.threadList.get(1));
    Assert.assertFalse("nonOIO: Thread Ids of input and output operators", ThreadIdValidatingInputOperator.threadId == outputOperator.threadId);


  }

  @Test
  public void validateOiOiOTreeImplementation() throws Exception
  {
    LogicalPlan lp = new LogicalPlan();
    ThreadIdValidatingInputOperator inputOperator1 = lp.addOperator("inputOperator1", new ThreadIdValidatingInputOperator());
    ThreadIdValidatingGenericIntermediateOperator intermediateOperatorfromInputOper1 = lp.addOperator("intermediateOperatorfromInputOper1", new ThreadIdValidatingGenericIntermediateOperator());
    ThreadIdValidatingGenericIntermediateOperator intermediateOperatorfromInterOper11 = lp.addOperator("intermediateOperatorfromInterOper11", new ThreadIdValidatingGenericIntermediateOperator());
    ThreadIdValidatingGenericIntermediateOperator intermediateOperatorfromInterOper12 = lp.addOperator("intermediateOperatorfromInterOper12", new ThreadIdValidatingGenericIntermediateOperator());
    ThreadIdValidatingOutputOperator outputOperatorFromInputOper = lp.addOperator("outputOperatorFromInputOper", new ThreadIdValidatingOutputOperator());
    ThreadIdValidatingOutputOperator outputOperatorFromInterOper11 = lp.addOperator("outputOperatorFromInterOper11", new ThreadIdValidatingOutputOperator());
    ThreadIdValidatingOutputOperator outputOperatorFromInterOper21 = lp.addOperator("outputOperatorFromInterOper21", new ThreadIdValidatingOutputOperator());
    ThreadIdValidatingOutputOperator outputOperatorFromInterOper22 = lp.addOperator("outputOperatorFromInterOper22", new ThreadIdValidatingOutputOperator());

    StreamMeta stream1 = lp.addStream("OiO1", inputOperator1.output, outputOperatorFromInputOper.input, intermediateOperatorfromInputOper1.input);
    StreamMeta stream2 = lp.addStream("OiO2", intermediateOperatorfromInputOper1.output, intermediateOperatorfromInterOper11.input, intermediateOperatorfromInterOper12.input);
    StreamMeta stream3 = lp.addStream("OiO3", intermediateOperatorfromInterOper11.output, outputOperatorFromInterOper11.input);
    StreamMeta stream4 = lp.addStream("nonOiO1", intermediateOperatorfromInterOper12.output, outputOperatorFromInterOper21.input, outputOperatorFromInterOper22.input);

    StramLocalCluster slc;
    stream1.setLocality(Locality.THREAD_LOCAL);
    stream2.setLocality(Locality.THREAD_LOCAL);
    stream3.setLocality(Locality.THREAD_LOCAL);
    lp.validate();
    slc = new StramLocalCluster(lp);
    slc.run();

    Assert.assertEquals("OIO: Number of threads ThreadIdValidatingGenericIntermediateOperator", 3 ,ThreadIdValidatingGenericIntermediateOperator.threadList.size());
    Assert.assertEquals("OIO: Number of unique threads ThreadIdValidatingGenericIntermediateOperator", 1 , (new HashSet<Long>(ThreadIdValidatingGenericIntermediateOperator.threadList)).size());
    Assert.assertEquals("OIO: Number of threads ThreadIdValidatingOutputOperator", 4 ,ThreadIdValidatingOutputOperator.threadList.size());
    Assert.assertEquals("OIO: Number of unique threads ThreadIdValidatingOutputOperator", 3 , (new HashSet<Long>(ThreadIdValidatingOutputOperator.threadList)).size());
    Assert.assertTrue("OIO:: inputOperator1 : ThreadIdValidatingOutputOperator", ThreadIdValidatingOutputOperator.threadList.contains(ThreadIdValidatingInputOperator.threadId));
    Assert.assertTrue("OIO:: inputOperator1 : ThreadIdValidatingGenericIntermediateOperator", ThreadIdValidatingGenericIntermediateOperator.threadList.contains(ThreadIdValidatingInputOperator.threadId));

  }

  private static final Logger logger = LoggerFactory.getLogger(OiOStreamTest.class);
}