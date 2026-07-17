/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.micrometer;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.MtiqConfigSupport;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.registry.otlp.OtlpMeterRegistry;
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

    if (!config.isOtlpMetricsEnabled()) {
      log.info("OTLP metrics are disabled, cannot provide a MeterRegistry for Micrometer.");
      return null;
    }

    return getOtlpMeterRegistry();
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
}
