/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import com.sonatype.insight.brain.common.metering.MeteredThreadPoolExecutor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides a Micrometer {@link MeterRegistry} for the single-tenant IQ Server as an explicit
 * {@link SimpleMeterRegistry} bean.
 * <p>
 * Spring Boot's metrics auto-configuration ({@code SimpleMetricsExportAutoConfiguration}) would
 * otherwise provide an equivalent fallback {@link SimpleMeterRegistry} via
 * {@code @ConditionalOnMissingBean(MeterRegistry.class)}. Declaring the bean here makes the
 * registry explicit and independent of that auto-configuration; because Boot's fallback backs
 * off when this bean is present, the two never collide and {@code /actuator/metrics} remains
 * backed by a {@link SimpleMeterRegistry} either way, so no auto-configuration exclusion is
 * required.
 * <p>
 * This configuration is guarded by the MTIQ marker property so it never activates in the
 * multi-tenant context, where {@code MultiTenantMeterRegistryProvider} supplies the StatsD-backed
 * registry instead.
 * <p>
 * Unlike {@code MultiTenantMetricsConfiguration}, this configuration intentionally does <em>not</em>
 * call {@link MeteredThreadPoolExecutor#injectMeterRegistry}. The static-field path is MTIQ-only:
 * single-tenant executors built via the implicit-registry constructor see a {@code null} static
 * registry and emit no {@code executor.*} metrics. This is deliberate, not an oversight - do not
 * "fix" it by adding the call, which would introduce single-tenant executor metrics that have never
 * existed (and a single-tenant {@link SimpleMeterRegistry} exports nowhere regardless).
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "sonatype.mtiq.enabled", havingValue = "false", matchIfMissing = true)
public class MetricsConfiguration
{
  @Bean
  public MeterRegistry meterRegistry() {
    return new SimpleMeterRegistry();
  }
}
