/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.telemetry;

import org.aspectj.lang.Aspects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the CTW-managed {@link GuideUsageEventAspect} singleton to Spring so its collector
 * dependency is injected. Mirrors {@code SecurityAopConfiguration}: the explicit setter wiring is
 * required because test configs mark beans lazy-init, and the CTW aspect fires regardless of whether
 * Spring eagerly resolved it &mdash; if the bean is never resolved, the {@code @Inject} setter is never
 * called and the collector stays null.
 *
 * <p>
 * The production component scan resolves this {@code @Configuration} eagerly, so the collector is
 * wired before any Guide lookup arrives. Integration tests that use a restricted context (e.g.
 * {@code BrainInjectedTest} with {@code useDefaultFilters=false}) do not import this configuration and
 * would see a null collector (the aspect then no-ops); such a test must import this config and
 * eager-init the bean to exercise recording. The {@code @Bean} method below logs a one-shot startup
 * line so operators can confirm telemetry is wired in production logs.
 */
@Configuration
public class GuideTelemetryAopConfiguration
{
  private static final Logger log = LoggerFactory.getLogger(GuideTelemetryAopConfiguration.class);

  @Bean
  public GuideUsageEventAspect guideUsageEventAspect(final GuideUsageTelemetryCollector collector) {
    GuideUsageEventAspect aspect = Aspects.aspectOf(GuideUsageEventAspect.class);
    aspect.setCollector(collector);
    log.info("Guide usage telemetry aspect wired; events will be buffered for daily drain");
    return aspect;
  }
}
