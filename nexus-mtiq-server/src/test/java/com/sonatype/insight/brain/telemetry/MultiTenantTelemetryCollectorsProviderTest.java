/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Set;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class MultiTenantTelemetryCollectorsProviderTest
{
  @Test
  public void testCollectors_RemovesUnwantedCollectors() {
    SourceControlRateLimitTelemetryCollector disabledCollector = mock(SourceControlRateLimitTelemetryCollector.class);
    PropertiesTelemetryCollector enabledCollector = mock(PropertiesTelemetryCollector.class);

    MultiTenantTelemetryCollectorsProvider underTest =
        new MultiTenantTelemetryCollectorsProvider(Set.of(disabledCollector, enabledCollector));

    Set<TelemetryCollector> actual = underTest.getTelemetryCollectors();
    assertThat(actual).hasSize(1);
    assertThat(actual.iterator().next()).isInstanceOf(PropertiesTelemetryCollector.class);
  }
}
