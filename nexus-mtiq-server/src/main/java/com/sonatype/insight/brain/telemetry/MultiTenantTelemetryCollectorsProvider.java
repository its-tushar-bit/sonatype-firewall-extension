/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Collectors can not be disabled as part of guice/sisu due to some collectors breaking the single responsibility rule
 * and are therefore required by other implementations. A collector may not want to be part of this collectors list but
 * may in fact be used elsewhere within the system, e.g. <code>RepositoryQueryService</code>. Fully disabling this
 * collector would prevent IQ from starting. To disable other collectors for multi-tenant add the class name to the
 * <code>DISABLED_COLLECTORS</code> list
 */
@Named
@Singleton
public class MultiTenantTelemetryCollectorsProvider
    implements TelemetryCollectorsProvider
{
  private final List<TelemetryCollector> telemetryCollectors;

  private static final List<Class> DISABLED_COLLECTORS =
      Arrays.asList(new Class[]{
          SourceControlRateLimitTelemetryCollector.class,
          SourceControlMetricsTelemetryCollector.class
      });

  @Inject
  public MultiTenantTelemetryCollectorsProvider(final List<TelemetryCollector> telemetryCollectors) {
    this.telemetryCollectors = filterDisabledCollectors(telemetryCollectors);
  }

  @Override
  public List<TelemetryCollector> getTelemetryCollectors() {
    return telemetryCollectors;
  }

  private static List<TelemetryCollector> filterDisabledCollectors(final List<TelemetryCollector> telemetryCollectors) {
    return telemetryCollectors.stream()
        .filter(collector -> DISABLED_COLLECTORS.stream()
            .noneMatch(c -> c.isInstance(collector)))
        .collect(Collectors.toList());
  }
}
