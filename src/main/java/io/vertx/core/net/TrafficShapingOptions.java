/*
 * Copyright (c) 2011-2019 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */

package io.vertx.core.net;

import io.netty.util.internal.ObjectUtil;
import io.vertx.codegen.annotations.DataObject;

/**
 * Options describing how {@link io.netty.handler.traffic.GlobalTrafficShapingHandler} will handle traffic shaping.
 */
@DataObject(generateConverter = true, publicConverter = false)
public class TrafficShapingOptions
{
  private final long inboundGlobalBandwidth;
  private final long outboundGlobalBandwidth;
  private long peakOutboundGlobalBandwidth;
  private long maxDelayToWaitTime;
  private long checkIntervalForStats;

  public TrafficShapingOptions(long inboundGlobalBandwidth, long outboundGlobalBandwidth)
  {
    this.inboundGlobalBandwidth = inboundGlobalBandwidth;
    this.outboundGlobalBandwidth = outboundGlobalBandwidth;
  }

  public void setMaxDelayToWaitTime(long maxDelayToWaitTime)
  {
    this.maxDelayToWaitTime = maxDelayToWaitTime;
    ObjectUtil.checkPositive(this.maxDelayToWaitTime, "maxDelayToWaitTime");
  }

  public void setCheckIntervalForStats(long checkIntervalForStats)
  {
    this.checkIntervalForStats = checkIntervalForStats;
    ObjectUtil.checkPositive(this.checkIntervalForStats, "checkIntervalForStats");
  }

  public void setPeakOutboundGlobalBandwidth(long peakOutboundGlobalBandwidth)
  {
    this.peakOutboundGlobalBandwidth = peakOutboundGlobalBandwidth;
    ObjectUtil.checkPositive(this.peakOutboundGlobalBandwidth , "peakOutboundGlobalBandwidth");
  }

  public long getInboundGlobalBandwidth() {
    return inboundGlobalBandwidth;
  }

  public long getOutboundGlobalBandwidth() {
    return outboundGlobalBandwidth;
  }

  public long getPeakOutboundGlobalBandwidth() {
    return peakOutboundGlobalBandwidth;
  }

  public long getMaxDelayToWaitTime() {
    return maxDelayToWaitTime;
  }

  public long getCheckIntervalForStats() {
    return checkIntervalForStats;
  }
}
