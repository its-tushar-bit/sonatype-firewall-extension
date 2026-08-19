/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import com.sonatype.insight.telemetry.model.TelemetryData;

public interface PaginatedTelemetryCollector
    extends TelemetryCollector
{
  TelemetryData firstPage();

  boolean hasMoreData();

  TelemetryData nextPage();
}
