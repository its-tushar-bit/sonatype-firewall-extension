/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics.sql;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dashboard.metrics.DashboardMetricsService;
import com.sonatype.insight.brain.dashboard.metrics.MetricValueDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO.RawThreatLevelCount;
import com.sonatype.insight.brain.policy.StageTypeService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes independently isolated SQL-backed dashboard metrics against authorization-resolved scope.
 */
@Named
@Singleton
public class DashboardMetricsSqlCoordinator
{
  private static final Logger log = LoggerFactory.getLogger(DashboardMetricsSqlCoordinator.class);

  private static final Map<String, Long> ZERO_VIOLATION_BANDS =
      Map.of("low", 0L, "moderate", 0L, "severe", 0L, "critical", 0L);

  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final PolicyDAO policyDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final StageTypeService stageTypeService;

  private final DashboardViolationThreatBandMapper threatBandMapper;

  private final DashboardMetricsSqlTelemetry telemetry;

  @Inject
  public DashboardMetricsSqlCoordinator(
      final ApplicationDAO applicationDAO,
      final OrganizationDAO organizationDAO,
      final PolicyDAO policyDAO,
      final PolicyViolationDAO policyViolationDAO,
      final StageTypeService stageTypeService,
      final DashboardViolationThreatBandMapper threatBandMapper,
      final DashboardMetricsSqlTelemetry telemetry)
  {
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
    this.policyDAO = policyDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.stageTypeService = stageTypeService;
    this.threatBandMapper = threatBandMapper;
    this.telemetry = telemetry;
  }

  public MetricValueDTO countApplications(final ResolvedScope scope) {
    MetricValueDTO denied = deniedValue(scope, DashboardMetricsSqlTelemetry.Metric.APPLICATIONS);
    if (denied != null) {
      return denied;
    }
    return execute(
        DashboardMetricsSqlTelemetry.Metric.APPLICATIONS,
        () -> applicationValue(applicationDAO.selectCountByApplicationIds(applicationIds(scope))));
  }

  public MetricValueDTO countOrganizations(final ResolvedScope scope) {
    MetricValueDTO denied = deniedValue(scope, DashboardMetricsSqlTelemetry.Metric.ORGANIZATIONS);
    if (denied != null) {
      return denied;
    }
    return execute(
        DashboardMetricsSqlTelemetry.Metric.ORGANIZATIONS,
        () -> new MetricValueDTO(organizationDAO.selectCountByOrganizationIds(organizationIds(scope)), null,
            DashboardMetricsService.METRIC_SOURCE_SQL));
  }

  public MetricValueDTO countPolicies(final ResolvedScope scope) {
    MetricValueDTO denied = deniedValue(scope, DashboardMetricsSqlTelemetry.Metric.POLICIES);
    if (denied != null) {
      return denied;
    }
    return execute(
        DashboardMetricsSqlTelemetry.Metric.POLICIES,
        () -> new MetricValueDTO(policyDAO.selectCountByOwnerIds(policyOwnerIds(scope)), null,
            DashboardMetricsService.METRIC_SOURCE_SQL));
  }

  public MetricValueDTO countViolations(final ResolvedScope scope) {
    MetricValueDTO denied = deniedValue(scope, DashboardMetricsSqlTelemetry.Metric.VIOLATIONS);
    if (denied != null) {
      return denied;
    }
    // Stage filtering is intentionally out of scope for SQL violations; stage-filtered requests stay
    // on the index-serving path until the SQL metric path can apply the same dimension.
    return execute(DashboardMetricsSqlTelemetry.Metric.VIOLATIONS, () -> {
      List<RawThreatLevelCount> rawCounts =
          policyViolationDAO.countUnfixedByThreatLevel(applicationIds(scope), null);
      Map<String, Long> bands = threatBandMapper.map(rawCounts);
      return new MetricValueDTO(rawCounts.stream().mapToLong(RawThreatLevelCount::count).sum(), bands,
          DashboardMetricsService.METRIC_SOURCE_SQL);
    });
  }

  private MetricValueDTO applicationValue(final long total) {
    long stages = stageTypeService.getLicensedStageTypes(StageTypeService.DASHBOARD_CONTEXT).size();
    return new MetricValueDTO(total, Map.of("stages", stages), DashboardMetricsService.METRIC_SOURCE_SQL);
  }

  private MetricValueDTO deniedValue(
      final ResolvedScope scope,
      final DashboardMetricsSqlTelemetry.Metric metric)
  {
    if (scope.kind() != ResolvedScope.Kind.DENY_ALL) {
      return null;
    }
    if (scope.denyReason() == ResolvedScope.DenyReason.RESOLUTION_FAILED) {
      return MetricValueDTO.unavailable(DashboardMetricsService.METRIC_SOURCE_SQL);
    }
    return switch (metric) {
      case APPLICATIONS -> applicationValue(0L);
      case ORGANIZATIONS, POLICIES -> new MetricValueDTO(0L, null, DashboardMetricsService.METRIC_SOURCE_SQL);
      case VIOLATIONS -> new MetricValueDTO(0L, ZERO_VIOLATION_BANDS, DashboardMetricsService.METRIC_SOURCE_SQL);
    };
  }

  @Nullable
  private static Set<String> applicationIds(final ResolvedScope scope) {
    return scope.kind() == ResolvedScope.Kind.GLOBAL ? null : scope.applicationIds();
  }

  @Nullable
  private static Set<String> organizationIds(final ResolvedScope scope) {
    return scope.kind() == ResolvedScope.Kind.GLOBAL ? null : scope.organizationIds();
  }

  @Nullable
  private static Set<String> policyOwnerIds(final ResolvedScope scope) {
    return scope.kind() == ResolvedScope.Kind.GLOBAL ? null : scope.policyOwnerIds();
  }

  private MetricValueDTO execute(
      final DashboardMetricsSqlTelemetry.Metric metric,
      final Supplier<MetricValueDTO> query)
  {
    long startedAt = System.nanoTime();
    MetricValueDTO result;
    try {
      result = query.get();
    }
    catch (RuntimeException e) {
      log.error("Dashboard SQL metric {} failed", metric, e);
      recordQuery(metric, System.nanoTime() - startedAt, false);
      return MetricValueDTO.unavailable(DashboardMetricsService.METRIC_SOURCE_SQL);
    }
    recordQuery(metric, System.nanoTime() - startedAt, true);
    return result;
  }

  private void recordQuery(
      final DashboardMetricsSqlTelemetry.Metric metric,
      final long durationNanos,
      final boolean success)
  {
    try {
      telemetry.recordQuery(metric, durationNanos, success);
    }
    catch (RuntimeException ignored) {
      // Telemetry is strictly best-effort and must not affect metric results.
    }
  }
}
