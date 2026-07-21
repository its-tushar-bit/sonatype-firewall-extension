/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics.sql;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsSqlReadinessState.MISSING;
import static com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsSqlReadinessState.VALID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DashboardMetricsSqlTelemetryTest
{
  private SimpleMeterRegistry meterRegistry;

  private DashboardMetricsSqlTelemetry underTest;

  @Before
  public void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    underTest = new DashboardMetricsSqlTelemetry(meterRegistry);
  }

  @Test
  public void queryTimersAreTaggedWithMetricAndOutcome() {
    underTest.recordQuery(DashboardMetricsSqlTelemetry.Metric.APPLICATIONS, TimeUnit.MILLISECONDS.toNanos(4), true);
    underTest.recordQuery(DashboardMetricsSqlTelemetry.Metric.APPLICATIONS, TimeUnit.MILLISECONDS.toNanos(6), false);

    assertThat(timer("dashboard.metrics.sql.query.duration", "metric", "APPLICATIONS", "outcome", "SUCCESS").count())
        .isEqualTo(1);
    assertThat(timer("dashboard.metrics.sql.query.duration", "metric", "APPLICATIONS", "outcome", "FAILED").count())
        .isEqualTo(1);
  }

  @Test
  public void scopeResolutionUsesBinaryPrincipalShapes() {
    underTest.recordScopeResolution(1L, ResolvedScope.Kind.GLOBAL);
    underTest.recordScopeResolution(1L, ResolvedScope.Kind.RESTRICTED);
    underTest.recordScopeResolution(1L, ResolvedScope.Kind.DENY_ALL);

    assertThat(timer("dashboard.metrics.sql.scope_resolution.duration", "principal_shape", "GLOBAL").count())
        .isEqualTo(1);
    assertThat(timer("dashboard.metrics.sql.scope_resolution.duration", "principal_shape", "RESTRICTED").count())
        .isEqualTo(2);
    assertThat(meterRegistry.find("dashboard.metrics.sql.scope_resolution.duration")
        .tag("principal_shape", "DENY_ALL")
        .timer()).isNull();
  }

  @Test
  public void readinessCountersUseExpectedTags() {
    underTest.recordReadiness(VALID);
    underTest.recordReadiness(MISSING);

    assertThat(counter("dashboard.metrics.sql.readiness", "state", "VALID").count()).isEqualTo(1);
    assertThat(counter("dashboard.metrics.sql.readiness", "state", "MISSING").count()).isEqualTo(1);
  }

  @Test
  public void nullRegistryDoesNotThrow() {
    DashboardMetricsSqlTelemetry nullRegistryTelemetry = new DashboardMetricsSqlTelemetry(null);

    nullRegistryTelemetry.recordReadiness(VALID);
    nullRegistryTelemetry.recordNotReadyBeyondGrace();
    nullRegistryTelemetry.recordScopeResolutionFailure();
    nullRegistryTelemetry.recordScopeResolution(1L, ResolvedScope.Kind.DENY_ALL);
    nullRegistryTelemetry.recordQuery(DashboardMetricsSqlTelemetry.Metric.POLICIES, 1L, true);
  }

  @Test
  public void registryLookupFailureIsSwallowed() {
    MeterRegistry throwingRegistry = mock(MeterRegistry.class);
    when(throwingRegistry.counter(anyString(), org.mockito.ArgumentMatchers.<Iterable<Tag>>any()))
        .thenThrow(new IllegalStateException("registry unavailable"));
    DashboardMetricsSqlTelemetry throwingTelemetry = new DashboardMetricsSqlTelemetry(throwingRegistry);

    assertThatCode(() -> throwingTelemetry.recordReadiness(VALID)).doesNotThrowAnyException();
  }

  @Test
  public void instrumentRecordingFailuresAreSwallowed() {
    MeterRegistry throwingRegistry = mock(MeterRegistry.class);
    Counter counter = mock(Counter.class);
    Timer timer = mock(Timer.class);
    when(throwingRegistry.counter(anyString(), org.mockito.ArgumentMatchers.<Iterable<Tag>>any()))
        .thenReturn(counter);
    when(throwingRegistry.timer(anyString(), org.mockito.ArgumentMatchers.<Iterable<Tag>>any()))
        .thenReturn(timer);
    doThrow(new IllegalStateException("counter unavailable")).when(counter).increment();
    doThrow(new IllegalStateException("timer unavailable")).when(timer).record(any(Duration.class));
    DashboardMetricsSqlTelemetry throwingTelemetry = new DashboardMetricsSqlTelemetry(throwingRegistry);

    assertThatCode(() -> throwingTelemetry.recordReadiness(VALID)).doesNotThrowAnyException();
    assertThatCode(() -> throwingTelemetry.recordQuery(
        DashboardMetricsSqlTelemetry.Metric.APPLICATIONS, 1L, true)).doesNotThrowAnyException();
  }

  private Timer timer(final String name, final String... tags) {
    Timer timer = meterRegistry.find(name).tags(tags).timer();
    assertThat(timer).isNotNull();
    return timer;
  }

  private Counter counter(final String name, final String... tags) {
    Counter counter = meterRegistry.find(name).tags(tags).counter();
    assertThat(counter).isNotNull();
    return counter;
  }
}
