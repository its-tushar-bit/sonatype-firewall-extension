/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

/**
 * @since 1.136.0
 */
public class QuarantinedComponentReportTelemetry
{
  public String componentHash;

  public String token;

  public long generateTime;

  public long viewTime;

  public boolean anonymousAccessEnabled;
}
