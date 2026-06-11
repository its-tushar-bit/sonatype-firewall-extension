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
import io.micrometer.registry.otlp.OtlpMeterRegistry;
import io.micrometer.statsd.StatsdMeterRegistry;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class MultiTenantMeterRegistryProviderTest
{
  private final MultiTenantInsightConfig config = new MultiTenantInsightConfig();

  /** Provider that always reports an OTLP endpoint as configured. */
  private final MultiTenantMeterRegistryProvider underTest = new MultiTenantMeterRegistryProvider(config)
  {
    @Override
    boolean isOtlpEndpointConfigured() {
      return true;
    }
  };

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
    config.setOtlpMetricsEnabled(false);

    MeterRegistry meterRegistry = underTest.get();

    CompositeMeterRegistry compositeMeterRegistry = (CompositeMeterRegistry) meterRegistry;

    assertThat(meterRegistry)
        .isNotNull()
        .isExactlyInstanceOf(CompositeMeterRegistry.class);
    assertThat(compositeMeterRegistry.getRegistries()).hasSize(1);
    assertThat(compositeMeterRegistry.getRegistries().stream().findFirst().get())
        .isExactlyInstanceOf(StatsdMeterRegistry.class);
  }

  @Test
  public void testProvideMeterRegistry_isNotConfigured() {
    config.setStatsdMetricsConfig(null);
    config.setOtlpMetricsEnabled(false);

    MeterRegistry meterRegistry = underTest.get();

    assertThat(meterRegistry).isNull();
  }

  @Test
  public void testProvideMeterRegistry_bothDisabled() {
    StatsdMetricsConfig statsdMetricsConfig = new StatsdMetricsConfig();
    statsdMetricsConfig.setEnabled(false);

    config.setStatsdMetricsConfig(statsdMetricsConfig);
    config.setOtlpMetricsEnabled(false);

    MeterRegistry meterRegistry = underTest.get();

    assertThat(meterRegistry).isNull();
  }

  @Test
  public void testProvideMeterRegistry_otlpEnabled() {
    config.setStatsdMetricsConfig(null);
    config.setOtlpMetricsEnabled(true);

    MeterRegistry meterRegistry = underTest.get();

    assertThat(meterRegistry).isNotNull();
    assertThat(meterRegistry).isExactlyInstanceOf(CompositeMeterRegistry.class);

    CompositeMeterRegistry compositeMeterRegistry = (CompositeMeterRegistry) meterRegistry;
    assertThat(compositeMeterRegistry.getRegistries())
        .hasSize(1)
        .first()
        .isExactlyInstanceOf(OtlpMeterRegistry.class);
  }

  @Test
  public void testProvideMeterRegistry_otlpDisabled() {
    StatsdMetricsConfig statsdMetricsConfig = new StatsdMetricsConfig();
    statsdMetricsConfig.setEnabled(false);
    statsdMetricsConfig.setMetricsPrefix("test");
    statsdMetricsConfig.setMetricsTeam("mtiq");
    config.setStatsdMetricsConfig(statsdMetricsConfig);
    config.setOtlpMetricsEnabled(false);

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

  @Test
  public void testProvideMeterRegistry_otlpEnabledButNoEndpoint() {
    MultiTenantMeterRegistryProvider noEndpointProvider = new MultiTenantMeterRegistryProvider(config)
    {
      @Override
      boolean isOtlpEndpointConfigured() {
        return false;
      }
    };

    StatsdMetricsConfig statsdMetricsConfig = new StatsdMetricsConfig();
    statsdMetricsConfig.setEnabled(false);
    statsdMetricsConfig.setMetricsPrefix("test");
    statsdMetricsConfig.setMetricsTeam("mtiq");
    config.setStatsdMetricsConfig(statsdMetricsConfig);
    config.setOtlpMetricsEnabled(true);

    MeterRegistry meterRegistry = noEndpointProvider.get();

    // No endpoint configured, so OTLP registry is skipped; with StatsD also disabled, result is null
    assertThat(meterRegistry).isNull();
  }

  @Test
  public void testProvideMeterRegistry_bothEnabled() {
    StatsdMetricsConfig statsdMetricsConfig = new StatsdMetricsConfig();
    statsdMetricsConfig.setEnabled(true);
    statsdMetricsConfig.setMetricsPrefix("test");
    statsdMetricsConfig.setBuffered(true);
    statsdMetricsConfig.setHost("localhost");
    statsdMetricsConfig.setPort(8125);
    statsdMetricsConfig.setMetricsTeam("mtiq");
    config.setStatsdMetricsConfig(statsdMetricsConfig);
    config.setOtlpMetricsEnabled(true);

    MeterRegistry meterRegistry = underTest.get();

    assertThat(meterRegistry).isNotNull();
    assertThat(meterRegistry).isExactlyInstanceOf(CompositeMeterRegistry.class);

    CompositeMeterRegistry compositeMeterRegistry = (CompositeMeterRegistry) meterRegistry;
    assertThat(compositeMeterRegistry.getRegistries()).hasSize(2);
    assertThat(compositeMeterRegistry.getRegistries())
        .extracting("class")
        .containsExactlyInAnyOrder(StatsdMeterRegistry.class, OtlpMeterRegistry.class);
  }
}
