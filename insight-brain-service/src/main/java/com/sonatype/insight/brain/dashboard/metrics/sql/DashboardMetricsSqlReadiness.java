/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.brain.tenancy.TenantReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class DashboardMetricsSqlReadiness
    implements TenantManaged
{
  private static final Logger log = LoggerFactory.getLogger(DashboardMetricsSqlReadiness.class);

  private static final String INDEX_NAME = "policy_violation_app_stage_open_unfixed_idx";

  private static final Duration CACHE_DURATION = Duration.ofSeconds(30);

  private static final String INDEX_READINESS_SQL = """
      SELECT i.indisvalid
      FROM pg_index i
      JOIN pg_class c ON c.oid = i.indexrelid
      JOIN pg_namespace n ON n.oid = c.relnamespace
      WHERE c.relname = ?
        AND n.nspname = ?
      """;

  private final OperationalDataStore operationalDataStore;

  private final DashboardMetricsSqlModeProvider modeProvider;

  private final DashboardMetricsSqlTelemetry telemetry;

  private final Clock clock;

  private final Function<String, DashboardMetricsSqlReadinessState> probe;

  private final TenantReference<TenantState> stateByTenant = new TenantReference<>(TenantState::new);

  @Inject
  public DashboardMetricsSqlReadiness(
      final OperationalDataStore operationalDataStore,
      final DashboardMetricsSqlModeProvider modeProvider,
      final DashboardMetricsSqlTelemetry telemetry)
  {
    this.operationalDataStore = operationalDataStore;
    this.modeProvider = modeProvider;
    this.telemetry = telemetry;
    this.clock = Clock.systemUTC();
    this.probe = this::probeIndex;
  }

  DashboardMetricsSqlReadiness(
      final OperationalDataStore operationalDataStore,
      final DashboardMetricsSqlModeProvider modeProvider,
      final DashboardMetricsSqlTelemetry telemetry,
      final Clock clock,
      final Function<String, DashboardMetricsSqlReadinessState> probe)
  {
    this.operationalDataStore = operationalDataStore;
    this.modeProvider = modeProvider;
    this.telemetry = telemetry;
    this.clock = clock;
    this.probe = probe;
  }

  public DashboardMetricsSqlReadinessState state() {
    // Embedded H2 is test/light-prod only. The partial index is a Postgres optimization; treat
    // embedded as ready so SQL mode ON can be exercised without a pg_index probe.
    if (operationalDataStore.isDatabaseEmbedded()) {
      recordReadiness(DashboardMetricsSqlReadinessState.VALID);
      return DashboardMetricsSqlReadinessState.VALID;
    }

    String tenantSchema = operationalDataStore.getDatabaseSchema();
    Instant now = clock.instant();
    // Read config outside the per-tenant lock so a DAO round-trip never sits under the monitor.
    int readinessGraceMinutes = modeProvider.readinessGraceMinutes();
    TenantState tenantState = stateByTenant.get();

    CachedState toServe = null;
    synchronized (tenantState) {
      CachedState cached = tenantState.cachedState;
      if (cached != null && now.isBefore(cached.checkedAt.plus(CACHE_DURATION))) {
        emitDiagnosticIfProlonged(tenantState, tenantSchema, cached.state, now, readinessGraceMinutes);
        toServe = cached;
      }
    }

    if (toServe == null) {
      DashboardMetricsSqlReadinessState probed = probe.apply(tenantSchema);
      Instant checkedAt = clock.instant();
      synchronized (tenantState) {
        CachedState cached = tenantState.cachedState;
        if (cached != null && checkedAt.isBefore(cached.checkedAt.plus(CACHE_DURATION))) {
          toServe = cached;
        }
        else {
          toServe = new CachedState(probed, checkedAt);
          tenantState.cachedState = toServe;
        }
        emitDiagnosticIfProlonged(tenantState, tenantSchema, toServe.state, checkedAt, readinessGraceMinutes);
      }
    }

    recordReadiness(toServe.state);
    return toServe.state;
  }

  public DashboardMetricsSqlMode effectiveMode(final DashboardMetricsSqlMode configured) {
    if (configured == DashboardMetricsSqlMode.OFF) {
      return DashboardMetricsSqlMode.OFF;
    }
    return state() == DashboardMetricsSqlReadinessState.VALID
        ? configured
        : DashboardMetricsSqlMode.OFF;
  }

  @Override
  public void deregister() {
    stateByTenant.remove();
  }

  private DashboardMetricsSqlReadinessState probeIndex(final String tenantSchema) {
    try (Connection connection = operationalDataStore.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(INDEX_READINESS_SQL))
    {
      statement.setString(1, INDEX_NAME);
      statement.setString(2, tenantSchema);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return DashboardMetricsSqlReadinessState.MISSING;
        }
        return resultSet.getBoolean("indisvalid")
            ? DashboardMetricsSqlReadinessState.VALID
            : DashboardMetricsSqlReadinessState.INVALID;
      }
    }
    catch (SQLException e) {
      log.error("Unable to check dashboard metrics SQL index readiness for tenant schema {}", tenantSchema, e);
      return DashboardMetricsSqlReadinessState.INVALID;
    }
  }

  private void emitDiagnosticIfProlonged(
      final TenantState tenantState,
      final String tenantSchema,
      final DashboardMetricsSqlReadinessState state,
      final Instant now,
      final int readinessGraceMinutes)
  {
    if (state == DashboardMetricsSqlReadinessState.VALID) {
      tenantState.notReadySince = null;
      tenantState.diagnosticEmitted = false;
      return;
    }

    if (tenantState.notReadySince == null) {
      tenantState.notReadySince = now;
    }
    Duration grace = Duration.ofMinutes(readinessGraceMinutes);
    if (!now.isBefore(tenantState.notReadySince.plus(grace)) && !tenantState.diagnosticEmitted) {
      tenantState.diagnosticEmitted = true;
      recordNotReadyBeyondGrace();
      log.warn(
          "Dashboard metrics SQL is disabled for tenant schema {} because index {} is {}; "
              + "readiness has not recovered within {} minutes",
          tenantSchema,
          INDEX_NAME,
          state,
          readinessGraceMinutes);
    }
  }

  private void recordReadiness(final DashboardMetricsSqlReadinessState state) {
    try {
      telemetry.recordReadiness(state);
    }
    catch (RuntimeException ignored) {
      // Telemetry is strictly best-effort and must not affect readiness.
    }
  }

  private void recordNotReadyBeyondGrace() {
    try {
      telemetry.recordNotReadyBeyondGrace();
    }
    catch (RuntimeException ignored) {
      // Telemetry is strictly best-effort and must not affect readiness.
    }
  }

  private static final class TenantState
  {
    private CachedState cachedState;

    private Instant notReadySince;

    private boolean diagnosticEmitted;
  }

  private record CachedState(DashboardMetricsSqlReadinessState state, Instant checkedAt)
  {
  }
}
