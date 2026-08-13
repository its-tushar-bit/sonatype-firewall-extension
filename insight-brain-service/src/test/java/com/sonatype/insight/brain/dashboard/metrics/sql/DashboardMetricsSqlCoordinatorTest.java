/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics.sql;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.dashboard.metrics.MetricValueDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO.RawThreatLevelCount;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.policy.StageTypeService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DashboardMetricsSqlCoordinatorTest
{
  @Mock
  private ApplicationDAO applicationDAO;

  @Mock
  private OrganizationDAO organizationDAO;

  @Mock
  private PolicyDAO policyDAO;

  @Mock
  private PolicyViolationDAO policyViolationDAO;

  @Mock
  private StageTypeService stageTypeService;

  @Mock
  private DashboardViolationThreatBandMapper threatBandMapper;

  @Mock
  private DashboardMetricsSqlTelemetry telemetry;

  private DashboardMetricsSqlCoordinator underTest;

  @BeforeEach
  public void setUp() {
    lenient().when(stageTypeService.getLicensedStageTypes(StageTypeService.DASHBOARD_CONTEXT)).thenReturn(List.of());
    underTest = new DashboardMetricsSqlCoordinator(
        applicationDAO,
        organizationDAO,
        policyDAO,
        policyViolationDAO,
        stageTypeService,
        threatBandMapper,
        telemetry);
  }

  @Test
  public void noAccessReturnsZerosWithoutDaoCalls() {
    ResolvedScope scope = ResolvedScope.denyAll(ResolvedScope.DenyReason.NO_ACCESS);

    assertThat(underTest.countApplications(scope)).isEqualToComparingFieldByField(
        new MetricValueDTO(0L, Map.of("stages", 0L), "sql"));
    assertThat(underTest.countOrganizations(scope)).isEqualToComparingFieldByField(
        new MetricValueDTO(0L, null, "sql"));
    assertThat(underTest.countPolicies(scope)).isEqualToComparingFieldByField(
        new MetricValueDTO(0L, null, "sql"));
    assertThat(underTest.countViolations(scope)).isEqualToComparingFieldByField(
        new MetricValueDTO(0L, Map.of("low", 0L, "moderate", 0L, "severe", 0L, "critical", 0L), "sql"));

    verifyNoInteractions(applicationDAO, organizationDAO, policyDAO, policyViolationDAO);
  }

  @Test
  public void resolutionFailureReturnsUnavailableWithoutDaoCalls() {
    ResolvedScope scope = ResolvedScope.denyAll(ResolvedScope.DenyReason.RESOLUTION_FAILED);

    assertUnavailable(underTest.countApplications(scope));
    assertUnavailable(underTest.countOrganizations(scope));
    assertUnavailable(underTest.countPolicies(scope));
    assertUnavailable(underTest.countViolations(scope));

    verifyNoInteractions(applicationDAO, organizationDAO, policyDAO, policyViolationDAO);
  }

  @Test
  public void applicationsUsesGlobalNullOrRestrictedApplicationIds() {
    when(applicationDAO.selectCountByApplicationIds(null)).thenReturn(8L);
    when(applicationDAO.selectCountByApplicationIds(Set.of("app"))).thenReturn(3L);

    assertThat(underTest.countApplications(global())).isEqualToComparingFieldByField(
        new MetricValueDTO(8L, Map.of("stages", 0L), "sql"));
    assertThat(underTest.countApplications(restricted())).isEqualToComparingFieldByField(
        new MetricValueDTO(3L, Map.of("stages", 0L), "sql"));

    verify(applicationDAO).selectCountByApplicationIds(isNull());
    verify(applicationDAO).selectCountByApplicationIds(Set.of("app"));
  }

  @Test
  public void organizationsNeverUsesApplicationIds() {
    when(organizationDAO.selectCountByOrganizationIds(null)).thenReturn(8L);
    when(organizationDAO.selectCountByOrganizationIds(Set.of("org"))).thenReturn(3L);

    assertThat(underTest.countOrganizations(global()).total).isEqualTo(8L);
    assertThat(underTest.countOrganizations(restricted()).total).isEqualTo(3L);

    verify(organizationDAO).selectCountByOrganizationIds(isNull());
    verify(organizationDAO).selectCountByOrganizationIds(Set.of("org"));
    verify(organizationDAO, never()).selectCountByOrganizationIds(Set.of("app"));
  }

  @Test
  public void policiesUsesDirectPolicyOwnerIdsNotWaiverHierarchyOwners() {
    when(policyDAO.selectCountByOwnerIds(null)).thenReturn(8L);
    when(policyDAO.selectCountByOwnerIds(Set.of("policy-owner"))).thenReturn(3L);

    assertThat(underTest.countPolicies(global()).total).isEqualTo(8L);
    assertThat(underTest.countPolicies(restricted()).total).isEqualTo(3L);

    verify(policyDAO).selectCountByOwnerIds(isNull());
    verify(policyDAO).selectCountByOwnerIds(Set.of("policy-owner"));
  }

  @Test
  public void violationsUsesApplicationIdsAndFourBandMapper() {
    List<RawThreatLevelCount> rawCounts = List.of(new RawThreatLevelCount((short) 1, 2L));
    Map<String, Long> bands = Map.of("low", 2L, "moderate", 0L, "severe", 0L, "critical", 0L);
    when(policyViolationDAO.countUnfixedByThreatLevel(eq(Set.of("app")), isNull())).thenReturn(rawCounts);
    when(threatBandMapper.map(rawCounts)).thenReturn(bands);

    MetricValueDTO result = underTest.countViolations(restricted());

    assertThat(result).isEqualToComparingFieldByField(new MetricValueDTO(2L, bands, "sql"));
    verify(policyViolationDAO).countUnfixedByThreatLevel(eq(Set.of("app")), isNull());
    verify(threatBandMapper).map(rawCounts);
  }

  @Test
  public void applicationsPreservesLicensedStageBreakdown() {
    StageType first = org.mockito.Mockito.mock(StageType.class);
    StageType second = org.mockito.Mockito.mock(StageType.class);
    when(applicationDAO.selectCountByApplicationIds(null)).thenReturn(8L);
    when(stageTypeService.getLicensedStageTypes(StageTypeService.DASHBOARD_CONTEXT))
        .thenReturn(List.of(first, second));

    MetricValueDTO result = underTest.countApplications(global());

    assertThat(result).isEqualToComparingFieldByField(new MetricValueDTO(8L, Map.of("stages", 2L), "sql"));
  }

  @Test
  public void noAccessPreservesMetricSpecificZeroBreakdowns() {
    ResolvedScope scope = ResolvedScope.denyAll(ResolvedScope.DenyReason.NO_ACCESS);
    when(stageTypeService.getLicensedStageTypes(StageTypeService.DASHBOARD_CONTEXT)).thenReturn(List.of());

    assertThat(underTest.countApplications(scope).breakdown).containsEntry("stages", 0L);
    assertThat(underTest.countViolations(scope).breakdown)
        .containsExactlyInAnyOrderEntriesOf(
            Map.of("low", 0L, "moderate", 0L, "severe", 0L, "critical", 0L));
  }

  @Test
  public void oneDaoFailureReturnsUnavailableOnlyForThatMetric() {
    when(applicationDAO.selectCountByApplicationIds(null)).thenThrow(new IllegalStateException("database unavailable"));
    when(organizationDAO.selectCountByOrganizationIds(null)).thenReturn(3L);

    assertUnavailable(underTest.countApplications(global()));
    assertThat(underTest.countOrganizations(global()).total).isEqualTo(3L);
  }

  @Test
  public void telemetryFailureDoesNotReplaceValidMetricData() {
    when(organizationDAO.selectCountByOrganizationIds(null)).thenReturn(3L);
    doThrow(new IllegalStateException("telemetry unavailable"))
        .when(telemetry)
        .recordQuery(
            eq(DashboardMetricsSqlTelemetry.Metric.ORGANIZATIONS),
            anyLong(),
            anyBoolean());

    MetricValueDTO result = underTest.countOrganizations(global());

    assertThat(result).isEqualToComparingFieldByField(new MetricValueDTO(3L, null, "sql"));
  }

  @Test
  public void telemetryFailureDoesNotEscapeDaoFailurePath() {
    when(organizationDAO.selectCountByOrganizationIds(null))
        .thenThrow(new IllegalStateException("database unavailable"));
    doThrow(new IllegalStateException("telemetry unavailable"))
        .when(telemetry)
        .recordQuery(
            eq(DashboardMetricsSqlTelemetry.Metric.ORGANIZATIONS),
            anyLong(),
            anyBoolean());

    assertUnavailable(underTest.countOrganizations(global()));
  }

  private static ResolvedScope global() {
    return new ResolvedScope(
        ResolvedScope.Kind.GLOBAL, null, Set.of(), Set.of(), Set.of(), Set.of(), false);
  }

  private static ResolvedScope restricted() {
    return new ResolvedScope(
        ResolvedScope.Kind.RESTRICTED,
        null,
        Set.of("waiver-owner"),
        Set.of("policy-owner"),
        Set.of("org"),
        Set.of("app"),
        true);
  }

  private static void assertUnavailable(final MetricValueDTO result) {
    assertThat(result.total).isNull();
    assertThat(result.breakdown).isNull();
    assertThat(result.source).isEqualTo("sql");
    assertThat(result.errorCode).isEqualTo(MetricValueDTO.METRIC_UNAVAILABLE);
  }
}
