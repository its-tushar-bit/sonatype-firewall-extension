/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.micrometer;

import com.sonatype.insight.brain.common.metering.MeteredThreadPoolExecutor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the Micrometer {@link MeterRegistry} for the multi-tenant IQ Server.
 * <p>
 * Adapts the {@link MultiTenantMeterRegistryProvider} into a {@link MeterRegistry} bean. Without
 * this adapter the provider is registered only as its own type, so metric emitters injecting a
 * {@link MeterRegistry} receive {@code null} and stop reporting {@code mtiq.*} metrics.
 * <p>
 * This configuration is guarded by the MTIQ marker property so it only activates in the
 * multi-tenant context; the single-tenant IQ Server uses {@code MetricsConfiguration} instead.
 * <p>
 * {@link MultiTenantMeterRegistryProvider#get()} returns {@code null} when neither StatsD nor OTLP
 * metrics are configured. In that case this configuration still supplies an in-memory
 * {@link SimpleMeterRegistry} that exports nowhere, so a {@link MeterRegistry} bean is always
 * present: metric emitters and the Actuator {@code metrics} endpoint (which require a
 * {@link MeterRegistry}) resolve, and the server starts whether or not an exporter is configured.
 * <p>
 * The registry is also pushed into {@link MeteredThreadPoolExecutor}'s static field; Spring does
 * not inject static members, so without this call executors created without an explicit registry
 * would never report {@code mtiq.executor.*} metrics.
 * <p>
 * That static registry is JVM-wide and shared across all tenants. This is deliberate and not
 * cross-tenant leakage: MTIQ uses a single global {@code CompositeMeterRegistry} and carries tenant
 * identity through meter tags, not through separate per-tenant registries.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "sonatype.mtiq.enabled", havingValue = "true")
public class MultiTenantMetricsConfiguration
{
  @Bean
  public MeterRegistry meterRegistry(final MultiTenantMeterRegistryProvider multiTenantMeterRegistryProvider) {
    MeterRegistry meterRegistry = multiTenantMeterRegistryProvider.get();
    if (meterRegistry == null) {
      // No exporter configured (neither StatsD nor OTLP). Supply an in-memory registry that exports
      // nowhere so a MeterRegistry bean is always present - consumers and the Actuator metrics
      // endpoint require one, and the server must start regardless.
      meterRegistry = new SimpleMeterRegistry();
    }
    MeteredThreadPoolExecutor.injectMeterRegistry(meterRegistry);
    return meterRegistry;
  }

  /**
   * Clears the {@link MeteredThreadPoolExecutor} static field when the context closes, so a
   * subsequent in-process context (e.g. a reused test harness) cannot read a stale registry.
   */
  @PreDestroy
  public void clearStaticMeterRegistry() {
    MeteredThreadPoolExecutor.injectMeterRegistry(null);
  }
}
