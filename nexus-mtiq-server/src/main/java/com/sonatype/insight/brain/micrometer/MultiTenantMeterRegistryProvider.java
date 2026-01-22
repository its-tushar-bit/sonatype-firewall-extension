/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.micrometer;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.metrics.datadog.StatsdMetricsConfig;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;

import com.google.inject.Provider;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.statsd.StatsdConfig;
import io.micrometer.statsd.StatsdFlavor;
import io.micrometer.statsd.StatsdMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class MultiTenantMeterRegistryProvider
    implements Provider<MeterRegistry>
{
  private static final Logger log = LoggerFactory.getLogger(MultiTenantMeterRegistryProvider.class);

  private final InsightConfig insightConfig;

  @Inject
  public MultiTenantMeterRegistryProvider(final InsightConfig insightConfig) {
    this.insightConfig = insightConfig;
  }

  @Override
  public MeterRegistry get() {
    MultiTenantInsightConfig config = (MultiTenantInsightConfig) insightConfig;
    if (config.getStatsdMetricsConfig() == null || !config.getStatsdMetricsConfig().isEnabled()) {
      log.info("StatsdMetrics is disabled, cannot provide a MeterRegistry for Micrometer.");
      return null;
    }

    log.info("Creating MultiTenant MeterRegistry for Micrometer.");
    CompositeMeterRegistry registry = new CompositeMeterRegistry();
    registry.add(getStatsdMeterRegistry(config.getStatsdMetricsConfig()));

    return registry;
  }

  private static MeterRegistry getStatsdMeterRegistry(StatsdMetricsConfig statsdMetricsConfig) {
    StatsdMeterRegistry statsdMeterRegistry =
        StatsdMeterRegistry.builder(buildStatsdConfig(statsdMetricsConfig))
            .clock(Clock.SYSTEM)
            .build();

    statsdMeterRegistry.config()
        .namingConvention((name, type, baseUnit) -> statsdMetricsConfig.getMetricsPrefix() + "." + name)
        .commonTags(buildCommonTags(statsdMetricsConfig.getMetricsTeam()));

    return statsdMeterRegistry;
  }

  private static Iterable<Tag> buildCommonTags(final String team) {
    Tags tags = Tags.of("team", team);

    if (System.getenv().get("DD_ENV") != null) {
      tags = tags.and("env", System.getenv().get("DD_ENV"));
    }

    if (System.getenv().get("DD_SERVICE") != null) {
      tags = tags.and("service", System.getenv().get("DD_SERVICE"));
    }

    if (System.getenv().get("DD_VERSION") != null) {
      tags = tags.and("version", System.getenv().get("DD_VERSION"));
    }

    if (System.getenv().get("AWS_REGION") != null) {
      tags = tags.and("region", System.getenv().get("AWS_REGION"));
    }

    if (System.getenv().get("HOSTNAME") != null) {
      tags = tags.and("hostname", System.getenv().get("HOSTNAME"));
    }

    return tags;
  }

  private static StatsdConfig buildStatsdConfig(StatsdMetricsConfig statsdMetricsConfig) {
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
