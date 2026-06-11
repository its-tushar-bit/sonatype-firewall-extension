/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.micrometer;

import com.sonatype.insight.brain.metrics.datadog.StatsdMetricsConfig;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.MtiqConfigSupport;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.registry.otlp.OtlpMeterRegistry;
import io.micrometer.statsd.StatsdConfig;
import io.micrometer.statsd.StatsdFlavor;
import io.micrometer.statsd.StatsdMeterRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
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
    MultiTenantInsightConfig config = MtiqConfigSupport.requireMultiTenantInsightConfig(
        insightConfig,
        "MultiTenantMeterRegistryProvider.get");

    boolean statsdEnabled = config.getStatsdMetricsConfig() != null && config.getStatsdMetricsConfig().isEnabled();
    boolean otlpEnabled = config.isOtlpMetricsEnabled();

    if (!statsdEnabled && !otlpEnabled) {
      log.info("Both StatsdMetrics and OTLP metrics are disabled, cannot provide a MeterRegistry for Micrometer.");
      return null;
    }

    log.info("Creating MultiTenant MeterRegistry for Micrometer (StatsD: {}, OTLP: {}).", statsdEnabled, otlpEnabled);
    CompositeMeterRegistry registry = new CompositeMeterRegistry();

    if (statsdEnabled) {
      registry.add(getStatsdMeterRegistry(config.getStatsdMetricsConfig()));
    }

    if (otlpEnabled) {
      MeterRegistry otlpRegistry = getOtlpMeterRegistry();
      if (otlpRegistry != null) {
        registry.add(otlpRegistry);
      }
    }

    if (registry.getRegistries().isEmpty()) {
      log.info("No metrics registries were successfully configured; returning null.");
      return null;
    }

    return registry;
  }

  private static MeterRegistry getStatsdMeterRegistry(StatsdMetricsConfig statsdMetricsConfig) {
    StatsdMeterRegistry statsdMeterRegistry =
        StatsdMeterRegistry.builder(buildStatsdConfig(statsdMetricsConfig))
            .clock(Clock.SYSTEM)
            .build();

    statsdMeterRegistry.config()
        .namingConvention((name, type, baseUnit) -> statsdMetricsConfig.getMetricsPrefix() + "." + name)
        .commonTags(buildStatsdCommonTags(statsdMetricsConfig.getMetricsTeam()));

    return statsdMeterRegistry;
  }

  /**
   * Creates an OTLP MeterRegistry that sends metrics to an OpenTelemetry collector.
   * <p>
   * The default OtlpConfig reads standard OTEL_* environment variables for endpoint,
   * authentication, and resource attributes:
   * <ul>
   * <li>OTEL_EXPORTER_OTLP_ENDPOINT - Base OTLP endpoint URL</li>
   * <li>OTEL_EXPORTER_OTLP_HEADERS - Headers for authentication</li>
   * <li>OTEL_RESOURCE_ATTRIBUTES - Resource-level attributes (service.name, deployment.environment,
   * service.version, etc.) automatically attached to all exported metrics</li>
   * </ul>
   * <p>
   * Unlike the StatsD registry, no metric-level commonTags for env/service/version are needed here.
   * OTEL_RESOURCE_ATTRIBUTES provides these as OTLP resource attributes, which is the standard
   * mechanism for service identification in OTel backends.
   */
  private MeterRegistry getOtlpMeterRegistry() {
    if (!isOtlpEndpointConfigured()) {
      log.warn("OTLP metrics enabled but no OTLP endpoint configured (checked OTEL_EXPORTER_OTLP_ENDPOINT, "
          + "OTEL_EXPORTER_OTLP_METRICS_ENDPOINT env vars and otel.exporter.otlp.* system properties); "
          + "skipping OTLP registry creation");
      return null;
    }
    return new OtlpMeterRegistry();
  }

  /**
   * Checks whether an OTLP endpoint is configured via environment variables or system properties.
   * OTel supports both configuration mechanisms; system properties use the dot-notation equivalent
   * of the env var names (e.g., otel.exporter.otlp.endpoint).
   */
  boolean isOtlpEndpointConfigured() {
    return System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT") != null
        || System.getenv("OTEL_EXPORTER_OTLP_METRICS_ENDPOINT") != null
        || System.getProperty("otel.exporter.otlp.endpoint") != null
        || System.getProperty("otel.exporter.otlp.metrics.endpoint") != null;
  }

  private static Iterable<Tag> buildStatsdCommonTags(final String team) {
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
