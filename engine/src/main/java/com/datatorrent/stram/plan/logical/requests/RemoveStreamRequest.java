/*
 *  Copyright (c) 2012-2013 DataTorrent, Inc.
 *  All Rights Reserved.
 */
package com.datatorrent.stram.plan.logical.requests;

import com.datatorrent.stram.plan.physical.PlanModifier;

/**
 * <p>RemoveStreamRequest class.</p>
 *
 * @author David Yan <david@datatorrent.com>
 * @since 0.3.2
 */
public class RemoveStreamRequest extends LogicalPlanRequest
{
  private String streamName;

  public String getStreamName()
  {
    return streamName;
  }

  public void setStreamName(String streamName)
  {
    this.streamName = streamName;
  }

  @Override
  public void execute(PlanModifier pm)
  {
    pm.removeStream(streamName);
  }

}