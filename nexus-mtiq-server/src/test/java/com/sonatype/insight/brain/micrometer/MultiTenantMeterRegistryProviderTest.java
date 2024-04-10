/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.micrometer;

import com.sonatype.insight.brain.metrics.datadog.StatsdMetricsConfig;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.statsd.StatsdMeterRegistry;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiTenantMeterRegistryProviderTest
{
  private final MultiTenantMeterRegistryProvider underTest = new MultiTenantMeterRegistryProvider();

  private final MultiTenantInsightConfig config = new MultiTenantInsightConfig();

  @Test
  public void testProvideMeterRegistry_regularFlow() {
    StatsdMetricsConfig statsdMetricsConfig = new StatsdMetricsConfig();
    statsdMetricsConfig.setEnabled(true);
    statsdMetricsConfig.setMetricsPrefix("test");
    statsdMetricsConfig.setBuffered(true);
    statsdMetricsConfig.setHost("localhost");
    statsdMetricsConfig.setPort(8125);
    statsdMetricsConfig.setMetricsTeam("mtiq");
    config.setStatsdMetricsConfig(statsdMetricsConfig);

    MeterRegistry meterRegistry = underTest.provideMeterRegistry(config);
    CompositeMeterRegistry compositeMeterRegistry = (CompositeMeterRegistry) meterRegistry;

    assertThat(meterRegistry)
        .isNotNull()
        .isExactlyInstanceOf(CompositeMeterRegistry.class);
    assertThat(compositeMeterRegistry.getRegistries()).isNotEmpty();
    assertThat(compositeMeterRegistry.getRegistries().stream().findFirst().get())
        .isExactlyInstanceOf(StatsdMeterRegistry.class);
  }

  @Test
  public void testProvideMeterRegistry_isNotConfigured() {
    MeterRegistry meterRegistry = underTest.provideMeterRegistry(config);

    assertThat(meterRegistry).isNull();
  }

  @Test
  public void testProvideMeterRegistry_isDisabled() {
    StatsdMetricsConfig statsdMetricsConfig = new StatsdMetricsConfig();
    statsdMetricsConfig.setEnabled(false);

    config.setStatsdMetricsConfig(statsdMetricsConfig);

    MeterRegistry meterRegistry = underTest.provideMeterRegistry(config);

    assertThat(meterRegistry).isNull();
  }
}
