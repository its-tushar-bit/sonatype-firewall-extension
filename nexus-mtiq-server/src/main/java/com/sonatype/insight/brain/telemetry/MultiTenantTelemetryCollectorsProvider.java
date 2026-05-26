/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Primary;

/**
 * Some collectors cannot simply be removed from the application context because other components still depend on them.
 * A collector may not want to be part of this list but may still be used elsewhere within the system, for example by
 * <code>RepositoryQueryService</code>. Fully disabling that collector would prevent IQ from starting. To disable other
 * collectors for multi-tenant mode add the class name to the <code>DISABLED_COLLECTORS</code> list.
 */
@Named
@Singleton
@Primary
public class MultiTenantTelemetryCollectorsProvider
    implements TelemetryCollectorsProvider
{
  private final Set<TelemetryCollector> telemetryCollectors;

  private static final List<Class> DISABLED_COLLECTORS =
      Arrays.asList(new Class[]{
        SourceControlRateLimitTelemetryCollector.class,
        SourceControlMetricsTelemetryCollector.class
      });

  @Inject
  public MultiTenantTelemetryCollectorsProvider(final Set<TelemetryCollector> telemetryCollectors) {
    this.telemetryCollectors = filterDisabledCollectors(telemetryCollectors);
  }

  @Override
  public Set<TelemetryCollector> getTelemetryCollectors() {
    return telemetryCollectors;
  }

  private static Set<TelemetryCollector> filterDisabledCollectors(final Set<TelemetryCollector> telemetryCollectors) {
    return telemetryCollectors.stream()
        .filter(collector -> DISABLED_COLLECTORS.stream()
            .noneMatch(c -> c.isInstance(collector)))
        .collect(Collectors.toSet());
  }
}
