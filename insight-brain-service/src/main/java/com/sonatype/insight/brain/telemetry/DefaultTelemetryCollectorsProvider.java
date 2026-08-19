/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Named
@Singleton
public class DefaultTelemetryCollectorsProvider
    implements TelemetryCollectorsProvider
{
  private final Set<TelemetryCollector> telemetryCollectors;

  @Inject
  public DefaultTelemetryCollectorsProvider(final Set<TelemetryCollector> telemetryCollectors) {
    this.telemetryCollectors = telemetryCollectors;
  }

  @Override
  public Set<TelemetryCollector> getTelemetryCollectors() {
    return telemetryCollectors;
  }
}
