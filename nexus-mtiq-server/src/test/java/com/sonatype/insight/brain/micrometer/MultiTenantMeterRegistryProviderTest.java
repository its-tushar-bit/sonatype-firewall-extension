/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.micrometer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.registry.otlp.OtlpMeterRegistry;
import org.junit.Test;

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
  public void testProvideMeterRegistry_otlpEnabled() {
    config.setOtlpMetricsEnabled(true);

    MeterRegistry meterRegistry = underTest.get();

    assertThat(meterRegistry).isExactlyInstanceOf(OtlpMeterRegistry.class);
  }

  @Test
  public void testProvideMeterRegistry_otlpDisabled() {
    config.setOtlpMetricsEnabled(false);

    MeterRegistry meterRegistry = underTest.get();

    assertThat(meterRegistry).isNull();
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
    config.setOtlpMetricsEnabled(true);

    MeterRegistry meterRegistry = noEndpointProvider.get();

    assertThat(meterRegistry).isNull();
  }

  @Test
  public void testProvideMeterRegistry_singleTenantConfig_throwsActionableMessage() {
    assertThatThrownBy(() -> new MultiTenantMeterRegistryProvider(new InsightConfig()).get())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("MultiTenantMeterRegistryProvider.get")
        .hasMessageContaining(MultiTenantInsightConfig.class.getName())
        .hasMessageContaining(InsightConfig.class.getName())
        .hasMessageContaining("config.class=" + MultiTenantInsightConfig.class.getName());
  }
}
