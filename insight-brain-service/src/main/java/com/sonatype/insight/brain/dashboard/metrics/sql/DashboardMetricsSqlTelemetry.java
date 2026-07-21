/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics.sql;

import java.time.Duration;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

/**
 * Privacy-safe telemetry for dashboard SQL metrics. Principal shape identifies the query predicate:
 * {@code GLOBAL} is an unfiltered global request; every other outcome is {@code RESTRICTED}.
 */
@Named
@Singleton
public class DashboardMetricsSqlTelemetry
{
  private static final String READINESS = "dashboard.metrics.sql.readiness";

  private static final String NOT_READY_BEYOND_GRACE = "dashboard.metrics.sql.not_ready_beyond_grace";

  private static final String SCOPE_RESOLUTION_FAILURE = "dashboard.metrics.sql.scope_resolution_failure";

  private static final String SCOPE_RESOLUTION_DURATION = "dashboard.metrics.sql.scope_resolution.duration";

  private static final String QUERY_DURATION = "dashboard.metrics.sql.query.duration";

  private final MeterRegistry meterRegistry;

  @Inject
  public DashboardMetricsSqlTelemetry(@Nullable final MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public void recordReadiness(final DashboardMetricsSqlReadinessState state) {
    increment(READINESS, Tags.of("state", state.name()));
  }

  public void recordNotReadyBeyondGrace() {
    increment(NOT_READY_BEYOND_GRACE, Tags.empty());
  }

  public void recordScopeResolutionFailure() {
    increment(SCOPE_RESOLUTION_FAILURE, Tags.empty());
  }

  public void recordScopeResolution(final long durationNanos, final ResolvedScope.Kind kind) {
    record(SCOPE_RESOLUTION_DURATION, durationNanos, Tags.of("principal_shape", principalShape(kind)));
  }

  public void recordQuery(final Metric metric, final long durationNanos, final boolean success) {
    record(QUERY_DURATION, durationNanos, Tags.of("metric", metric.name(), "outcome", success ? "SUCCESS" : "FAILED"));
  }

  private void increment(final String name, final Tags tags) {
    try {
      if (meterRegistry != null) {
        meterRegistry.counter(name, tags).increment();
      }
    }
    catch (RuntimeException ignored) {
      // Telemetry is strictly best-effort and must never alter dashboard behavior.
    }
  }

  private void record(final String name, final long durationNanos, final Tags tags) {
    try {
      if (meterRegistry != null) {
        meterRegistry.timer(name, tags).record(Duration.ofNanos(durationNanos));
      }
    }
    catch (RuntimeException ignored) {
      // Telemetry is strictly best-effort and must never alter dashboard behavior.
    }
  }

  private static String principalShape(final ResolvedScope.Kind kind) {
    return kind == ResolvedScope.Kind.GLOBAL ? "GLOBAL" : "RESTRICTED";
  }

  public enum Metric
  {
    APPLICATIONS,
    ORGANIZATIONS,
    POLICIES,
    VIOLATIONS
  }
}
