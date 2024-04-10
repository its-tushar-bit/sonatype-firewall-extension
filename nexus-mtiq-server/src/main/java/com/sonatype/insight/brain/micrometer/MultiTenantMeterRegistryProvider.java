/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.micrometer;

import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.metrics.datadog.StatsdMetricsConfig;
import com.sonatype.insight.brain.metrics.micrometer.MeterRegistryProvider;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.statsd.StatsdConfig;
import io.micrometer.statsd.StatsdFlavor;
import io.micrometer.statsd.StatsdMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class MultiTenantMeterRegistryProvider
    implements MeterRegistryProvider
{
  private static final Logger log = LoggerFactory.getLogger(MultiTenantMeterRegistryProvider.class);

  private static final String DD_ENV_VAR = "DD_ENV";

  @Override
  public MeterRegistry provideMeterRegistry(InsightConfig insightConfig) {
    MultiTenantInsightConfig config = (MultiTenantInsightConfig) insightConfig;
    if (config.getStatsdMetricsConfig() == null || !config.getStatsdMetricsConfig().isEnabled()) {
      log.info("StatsdMetrics is disabled, cannot provide a MeterRegistry for Micrometer.");
      return null;
    }

    log.info("Creating MultiTenant MeterRegistry for Micrometer.");
    CompositeMeterRegistry registry = new CompositeMeterRegistry();
    registry.add(getStatsdMeterRegistry(config));

    return registry;
  }

  private static MeterRegistry getStatsdMeterRegistry(MultiTenantInsightConfig config) {
    StatsdMeterRegistry statsdMeterRegistry =
        StatsdMeterRegistry.builder(getStatsdConfig(config.getStatsdMetricsConfig()))
            .clock(Clock.SYSTEM)
            .build();

    statsdMeterRegistry.config()
        .namingConvention((name, type, baseUnit) -> config.getStatsdMetricsConfig().getMetricsPrefix() + "." + name)
        .commonTags("team", config.getStatsdMetricsConfig().getMetricsTeam(),
            "env", System.getenv().get(DD_ENV_VAR) != null ? System.getenv().get(DD_ENV_VAR) : "missing_env");

    return statsdMeterRegistry;
  }

  private static StatsdConfig getStatsdConfig(StatsdMetricsConfig statsdMetricsConfig) {
    return new StatsdConfig()
    {
      @Override
      public String get(String config) {
        return null;
      }

      @Override
      public String prefix() {
        return statsdMetricsConfig.getMetricsPrefix();
      }

      @Override
      public StatsdFlavor flavor() {
        return StatsdFlavor.DATADOG;
      }

      @Override
      public boolean enabled() {
        return statsdMetricsConfig.isEnabled();
      }

      @Override
      public String host() {
        return statsdMetricsConfig.getHost();
      }

      @Override
      public int port() {
        return statsdMetricsConfig.getPort();
      }

      @Override
      public boolean buffered() {
        return statsdMetricsConfig.isBuffered();
      }
    };
  }
}
