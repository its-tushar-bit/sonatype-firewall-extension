/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

public class SourceControlRateLimitTelemetry
{
  public static final String SOURCE_CONTROL_RATE_LIMITS = "source_control_rate_limits";

  public String scm;

  public String userHash;

  public int calls;

  public int minRemaining;

  public int timesExceeded;

  public SourceControlRateLimitTelemetry(String scm, String userHash, int calls, int minRemaining, int timesExceeded) {
    this.scm = scm;
    this.userHash = userHash;
    this.calls = calls;
    this.minRemaining = minRemaining;
    this.timesExceeded = timesExceeded;
  }
}
