/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Reusable per-test reset for the process-global {@link AdvancedSearchTelemetryMetrics} singleton.
 *
 * <p>
 * The reused {@code @ComponentH2Test} context shares this non-transactional collector across every
 * class in the module, so searches recorded by one test (or by a sibling class that ran earlier in
 * the same fork) would otherwise leak into the next test's telemetry. Draining the collector before
 * each test makes search telemetry deterministic without rebuilding the Spring context. Lives in the
 * {@code com.sonatype.insight.brain.telemetry} package so it can call the package-private
 * {@code computeStatsAndReset()} drain, exactly as the telemetry tests did inline before.
 *
 * <p>
 * Apply with {@code @ExtendWith(AdvancedSearchTelemetryResetExtension.class)} on any reused-context
 * test that reads advanced-search telemetry.
 *
 * <p>
 * <b>Scope:</b> {@code computeStatsAndReset()} is called without switching tenant context, so it drains only the
 * tenant active when {@code beforeEach} runs (the default {@code SINGLE_TENANT} in the standard H2 component
 * fixture). A test that records searches under a named tenant via
 * {@code testAsNewTenant(...)}/{@code testAsTenant(...)}
 * must drain that tenant's counts itself (as {@code AdvancedSearchTelemetryMetricsTest} does inside each closure);
 * this extension will not clean up per-tenant state left behind by a test that failed mid-closure.
 */
public class AdvancedSearchTelemetryResetExtension
    implements BeforeEachCallback
{
  @Override
  public void beforeEach(final ExtensionContext context) {
    SpringExtension.getApplicationContext(context)
        .getBean(AdvancedSearchTelemetryMetrics.class)
        .computeStatsAndReset();
  }
}
