/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics.sql;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.test.LogOutput;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsSqlMode.OFF;
import static com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsSqlMode.ON;
import static com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsSqlMode.SHADOW;
import static com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsSqlReadinessState.INVALID;
import static com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsSqlReadinessState.MISSING;
import static com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsSqlReadinessState.VALID;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsNewTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DashboardMetricsSqlReadinessTest
{
  private static final String SCHEMA = "tenant_one";

  @Rule
  public LogOutput logOutput = new LogOutput(DashboardMetricsSqlReadiness.class);

  @Mock
  private OperationalDataStore operationalDataStore;

  @Mock
  private DashboardMetricsSqlModeProvider modeProvider;

  @Mock
  private DashboardMetricsSqlTelemetry telemetry;

  private MutableClock clock;

  @Before
  public void setUp() {
    clock = new MutableClock(Instant.parse("2026-07-16T12:00:00Z"));
    when(operationalDataStore.getDatabaseSchema()).thenReturn(SCHEMA);
    when(modeProvider.readinessGraceMinutes()).thenReturn(60);
  }

  @Test
  public void validIndexPreservesConfiguredMode() {
    DashboardMetricsSqlReadiness underTest = readiness(_ -> VALID);

    assertThat(underTest.effectiveMode(ON)).isEqualTo(ON);
    assertThat(underTest.effectiveMode(SHADOW)).isEqualTo(SHADOW);
  }

  @Test
  public void invalidIndexForcesOff() {
    DashboardMetricsSqlReadiness underTest = readiness(_ -> INVALID);

    assertThat(underTest.effectiveMode(ON)).isEqualTo(OFF);
  }

  @Test
  public void missingIndexForcesOff() {
    DashboardMetricsSqlReadiness underTest = readiness(_ -> MISSING);

    assertThat(underTest.effectiveMode(SHADOW)).isEqualTo(OFF);
  }

  @Test
  public void embeddedDatabaseIsValidWithoutPgCatalogQuery() {
    @SuppressWarnings("unchecked")
    Function<String, DashboardMetricsSqlReadinessState> probe = org.mockito.Mockito.mock(Function.class);
    when(operationalDataStore.isDatabaseEmbedded()).thenReturn(true);
    DashboardMetricsSqlReadiness underTest =
        new DashboardMetricsSqlReadiness(operationalDataStore, modeProvider, telemetry, clock, probe);

    assertThat(underTest.state()).isEqualTo(VALID);
    verify(probe, never()).apply(org.mockito.ArgumentMatchers.anyString());
    verify(telemetry).recordReadiness(VALID);
  }

  @Test
  public void readinessResultIsCachedForThirtySeconds() {
    AtomicInteger probeCalls = new AtomicInteger();
    List<String> probedSchemas = new ArrayList<>();
    DashboardMetricsSqlReadiness underTest = readiness(schema -> {
      probeCalls.incrementAndGet();
      probedSchemas.add(schema);
      return VALID;
    });

    assertThat(underTest.state()).isEqualTo(VALID);
    clock.advance(Duration.ofSeconds(29));
    assertThat(underTest.state()).isEqualTo(VALID);
    assertThat(probeCalls).hasValue(1);
    assertThat(probedSchemas).containsExactly(SCHEMA);

    clock.advance(Duration.ofSeconds(1));
    assertThat(underTest.state()).isEqualTo(VALID);
    assertThat(probeCalls).hasValue(2);
    assertThat(probedSchemas).containsExactly(SCHEMA, SCHEMA);
  }

  @Test
  public void readinessCacheIsIsolatedByTenantEvenWhenSchemasMatch() {
    AtomicInteger probeCalls = new AtomicInteger();
    AtomicReference<DashboardMetricsSqlReadinessState> probeResult = new AtomicReference<>(VALID);
    DashboardMetricsSqlReadiness underTest = readiness(schema -> {
      probeCalls.incrementAndGet();
      return probeResult.get();
    });

    Tenant firstTenant = testAsNewTenant("readiness-first", tenant -> {
      assertThat(underTest.state()).isEqualTo(VALID);
    });
    testAsNewTenant("readiness-second", tenant -> {
      probeResult.set(INVALID);
      assertThat(underTest.state()).isEqualTo(INVALID);
    });
    testAsTenant(firstTenant, tenant -> {
      assertThat(underTest.state()).isEqualTo(VALID);
    });

    assertThat(probeCalls).hasValue(2);
  }

  @Test
  public void deregisterEvictsReadinessForReusedTenant() {
    AtomicInteger probeCalls = new AtomicInteger();
    AtomicReference<DashboardMetricsSqlReadinessState> probeResult = new AtomicReference<>(INVALID);
    DashboardMetricsSqlReadiness underTest = readiness(schema -> {
      probeCalls.incrementAndGet();
      return probeResult.get();
    });

    testAsNewTenant("reused-readiness", tenant -> {
      assertThat(underTest.state()).isEqualTo(INVALID);
      underTest.deregister();
    });
    probeResult.set(VALID);
    testAsNewTenant("reused-readiness", tenant -> {
      assertThat(underTest.state()).isEqualTo(VALID);
    });

    assertThat(probeCalls).hasValue(2);
  }

  @Test
  public void deregisterClearsDiagnosticStateForReusedTenant() {
    DashboardMetricsSqlReadiness underTest = readiness(schema -> INVALID);

    testAsNewTenant("reused-diagnostic", tenant -> {
      assertThat(underTest.state()).isEqualTo(INVALID);
      clock.advance(Duration.ofMinutes(61));
      assertThat(underTest.state()).isEqualTo(INVALID);
      assertThat(logOutput.getWarnMessages(DashboardMetricsSqlReadiness.class.getName())).hasSize(1);
      underTest.deregister();
    });

    testAsNewTenant("reused-diagnostic", tenant -> {
      assertThat(underTest.state()).isEqualTo(INVALID);
      assertThat(logOutput.getWarnMessages(DashboardMetricsSqlReadiness.class.getName())).hasSize(1);
      clock.advance(Duration.ofMinutes(61));
      assertThat(underTest.state()).isEqualTo(INVALID);
      assertThat(logOutput.getWarnMessages(DashboardMetricsSqlReadiness.class.getName())).hasSize(2);
    });
  }

  @Test
  public void prolongedNotReadyEmitsDiagnosticAfterGraceOnly() {
    DashboardMetricsSqlReadiness underTest = readiness(_ -> INVALID);

    assertThat(underTest.state()).isEqualTo(INVALID);
    clock.advance(Duration.ofMinutes(59));
    assertThat(underTest.state()).isEqualTo(INVALID);
    assertThat(logOutput.getWarnMessages(DashboardMetricsSqlReadiness.class.getName())).isEmpty();

    clock.advance(Duration.ofMinutes(2));
    assertThat(underTest.state()).isEqualTo(INVALID);
    List<String> diagnostics = logOutput.getWarnMessages(DashboardMetricsSqlReadiness.class.getName());
    assertThat(diagnostics).hasSize(1);
    assertThat(diagnostics.get(0)).contains("tenant_one", "INVALID", "60 minutes");
    verify(telemetry, times(3)).recordReadiness(INVALID);
    verify(telemetry).recordNotReadyBeyondGrace();
  }

  @Test
  public void missingIndexEmitsMissingReadiness() {
    DashboardMetricsSqlReadiness underTest = readiness(_ -> MISSING);

    assertThat(underTest.state()).isEqualTo(MISSING);

    verify(telemetry).recordReadiness(MISSING);
  }

  @Test
  public void telemetryFailureDoesNotReplaceReadinessState() {
    DashboardMetricsSqlReadiness underTest = readiness(_ -> VALID);
    doThrow(new IllegalStateException("telemetry unavailable"))
        .when(telemetry)
        .recordReadiness(VALID);

    assertThat(underTest.state()).isEqualTo(VALID);
  }

  @Test
  public void telemetryFailureDoesNotChangeEffectiveMode() {
    DashboardMetricsSqlReadiness underTest = readiness(_ -> VALID);
    doThrow(new IllegalStateException("telemetry unavailable"))
        .when(telemetry)
        .recordReadiness(VALID);

    assertThat(underTest.effectiveMode(ON)).isEqualTo(ON);
  }

  private DashboardMetricsSqlReadiness readiness(
      final Function<String, DashboardMetricsSqlReadinessState> probe)
  {
    return new DashboardMetricsSqlReadiness(operationalDataStore, modeProvider, telemetry, clock, probe);
  }

  private static final class MutableClock
      extends Clock
  {
    private Instant current;

    private MutableClock(final Instant current) {
      this.current = current;
    }

    private void advance(final Duration duration) {
      current = current.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(final ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return current;
    }
  }
}
