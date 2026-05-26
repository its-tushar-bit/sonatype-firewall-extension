/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.micrometer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.metrics.datadog.StatsdMetricsConfig;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.statsd.StatsdMeterRegistry;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class MultiTenantMeterRegistryProviderTest
{
  private final MultiTenantInsightConfig config = new MultiTenantInsightConfig();

  private final MultiTenantMeterRegistryProvider underTest = new MultiTenantMeterRegistryProvider(config);

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

    MeterRegistry meterRegistry = underTest.get();

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
    config.setStatsdMetricsConfig(null);

    MeterRegistry meterRegistry = underTest.get();

    assertThat(meterRegistry).isNull();
  }

  @Test
  public void testProvideMeterRegistry_isDisabled() {
    StatsdMetricsConfig statsdMetricsConfig = new StatsdMetricsConfig();
    statsdMetricsConfig.setEnabled(false);

    config.setStatsdMetricsConfig(statsdMetricsConfig);

    MeterRegistry meterRegistry = underTest.get();

    assertThat(meterRegistry).isNull();
  }

  @Test
  public void testProvideMeterRegistryFailsWithActionableMessageForSingleTenantConfig() {
    assertThatThrownBy(() -> new MultiTenantMeterRegistryProvider(new InsightConfig()).get())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("MultiTenantMeterRegistryProvider.get")
        .hasMessageContaining(MultiTenantInsightConfig.class.getName())
        .hasMessageContaining(InsightConfig.class.getName())
        .hasMessageContaining("config.class=" + MultiTenantInsightConfig.class.getName());
  }
}
