/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReport;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.collect.Sets;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.LICENSE;
import static com.sonatype.insight.brain.successmetrics.SuccessMetricsTestUtils.ONE_HOUR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class SuccessMetricsReportDataServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private SuccessMetricsReportDataService successMetricsReportDataService;

  private LocalDate today = new LocalDate();

  private Set<String> orgIds;

  private Set<String> appIds;

  @Before
  public void before() {
    ApplicationComponent buildComponent = tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID,
        "ababababab", ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"));
    Policy licensePolicy =
        tempEntity.newPolicy(org, 5, LogicalOperator.AND, new Condition(LicenseConditionType.ID, "is", "Apache-2.0"));
    PolicyEvaluation buildEval = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "now", new Date());
    tempEntity.newPolicyViolation(buildEval, licensePolicy, 7, LICENSE, buildComponent.getComponentIdentifier(),
        buildComponent.getHash(), FailActionType.ID);
    orgIds = new HashSet<>(Collections.singletonList(org.getId()));
    appIds = new HashSet<>(Collections.singletonList(app.getId()));
  }

  private SuccessMetricsReport createSuccessMetricsReport(Set<String> organizationIds, Set<String> applicationIds) {
    SuccessMetricsReportScopeDTO scope = new SuccessMetricsReportScopeDTO();
    scope.organizationIds = organizationIds;
    scope.applicationIds = applicationIds;

    return tempEntity.newSuccessMetricsReport(getUsername(), "report", JsonUtils.format(scope));
  }

  @Test
  public void testGetChartData_ExplicitApplicationFilter_Unauthenticated() {
    createPolicyViolation(app, today, ONE_HOUR);
    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null, Collections.singleton(app.getId()));

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      successMetricsReportDataService.getChartData(successMetricsReport.getId());
      // can't look up SuccessMetricsReport if the user isn't logged in
    }).withMessageContaining("Cannot find a success metrics report");
  }

  @Test
  public void testGetChartData_ExplicitApplicationFilter_Unauthorized() {
    createPolicyViolation(app, today, ONE_HOUR);
    login();
    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null, Collections.singleton(app.getId()));
    assertEmptyResults(successMetricsReportDataService.getChartData(successMetricsReport.getId()));
  }

  @Test
  public void testGetChartData_ExplicitOrganizationFilter_Unauthenticated() {
    createPolicyViolation(app, today, ONE_HOUR);
    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(Collections.singleton(org.getId()), null);

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      successMetricsReportDataService.getChartData(successMetricsReport.getId());
      // can't look up SuccessMetricsReport if the user isn't logged in
    }).withMessageContaining("Cannot find a success metrics report");
  }

  @Test
  public void testGetChartData_ExplicitOrganizationFilter_Unauthorized() {
    createPolicyViolation(app, today, ONE_HOUR);
    login();
    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(Collections.singleton(org.getId()), null);
    assertEmptyResults(successMetricsReportDataService.getChartData(successMetricsReport.getId()));
  }

  @Test
  public void testGetChartData_Mttrs_ExplicitApplicationFilter_Authorized() {
    Application app2 = tempEntity.newApplication(org.getId());

    createPolicyViolation(app, today, ONE_HOUR);
    createPolicyViolation(app2, today, ONE_HOUR * 2);
    grantReadPermission(app.getId());

    Set<String> appIds = Sets.newHashSet(app.getId(), app2.getId());
    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null, appIds);
    assertMttrResults(successMetricsReportDataService.getChartData(successMetricsReport.getId()).mttrs, today);
  }

  @Test
  public void testGetChartData_Mttrs_ExplicitOrganizationFilter_Authorized() {
    Application app2 = tempEntity.newApplication(org.getId());

    createPolicyViolation(app, today, ONE_HOUR);
    createPolicyViolation(app2, today, ONE_HOUR * 2);

    grantReadPermission(app.getId());
    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(Collections.singleton(org.getId()), null);
    assertMttrResults(successMetricsReportDataService.getChartData(successMetricsReport.getId()).mttrs, today);
  }

  @Test
  public void testGetChartData_Mttrs_ExplicitOrganizationFilter_AuthorizedOneOrg() {
    Organization org2 = tempEntity.newOrganization();
    Application app2 = tempEntity.newApplication(org2.getId());

    createPolicyViolation(app, today, ONE_HOUR);
    createPolicyViolation(app2, today, ONE_HOUR * 2);

    grantReadPermission(org.getId());

    Set<String> orgIds = Sets.newHashSet(org.getId(), org2.getId());
    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(orgIds, null);
    assertMttrResults(successMetricsReportDataService.getChartData(successMetricsReport.getId()).mttrs, today);
  }

  @Test
  public void testGetChartData_ImplicitApplicationFilter_Unauthenticated() {
    createPolicyViolation(app, today, ONE_HOUR);
    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null, null);

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      successMetricsReportDataService.getChartData(successMetricsReport.getId());
      // can't look up SuccessMetricsReport if the user isn't logged in
    }).withMessageContaining("Cannot find a success metrics report");
  }

  @Test
  public void testGetChartData_ImplicitApplicationFilter_Unauthorized() {
    createPolicyViolation(app, today, ONE_HOUR);
    login();
    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null, null);
    assertEmptyResults(successMetricsReportDataService.getChartData(successMetricsReport.getId()));
  }

  @Test
  public void testGetChartData_Mttrs_ImplicitApplicationFilter_Authorized() {
    Application app2 = tempEntity.newApplication(org.getId());

    createPolicyViolation(app, today, ONE_HOUR);
    createPolicyViolation(app2, today, ONE_HOUR * 2);
    grantReadPermission(app.getId());
    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null, null);
    assertMttrResults(successMetricsReportDataService.getChartData(successMetricsReport.getId()).mttrs, today);
  }

  @Test
  public void testGetChartData_Averages__ExplicitApplicationFilter_Authorized() {
    Application app2 = tempEntity.newApplication(org.getId());

    createPolicyViolation(app, today);
    createPolicyViolation(app2, today);

    grantReadPermission(app.getId());

    Set<String> appIds = Sets.newHashSet(app.getId(), app2.getId());
    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null, appIds);
    assertAveragesResults(successMetricsReportDataService.getChartData(successMetricsReport.getId()).averages);
  }

  @Test
  public void testGetChartData_Averages__ExplicitOrganizationFilter_Authorized() {
    Application app2 = tempEntity.newApplication(org.getId());

    createPolicyViolation(app, today);
    createPolicyViolation(app2, today);

    grantReadPermission(app.getId());
    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(Collections.singleton(org.getId()), null);
    assertAveragesResults(successMetricsReportDataService.getChartData(successMetricsReport.getId()).averages);
  }

  @Test
  public void testGetChartData_Averages__ExplicitOrganizationFilter_AuthorizedOneOrg() {
    Organization org2 = tempEntity.newOrganization();
    Application app2 = tempEntity.newApplication(org2.getId());

    createPolicyViolation(app, today);
    createPolicyViolation(app2, today);

    grantReadPermission(org.getId());

    Set<String> orgIds = Sets.newHashSet(org.getId(), org2.getId());
    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(orgIds, null);
    assertAveragesResults(successMetricsReportDataService.getChartData(successMetricsReport.getId()).averages);
  }

  @Test
  public void testGetChartData_Averages__ImplicitApplicationFilter_Authorized() {
    Application app2 = tempEntity.newApplication(org.getId());

    createPolicyViolation(app, today, ONE_HOUR);
    createPolicyViolation(app2, today, ONE_HOUR * 2);
    grantReadPermission(app.getId());
    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null, null);
    assertAveragesResults(successMetricsReportDataService.getChartData(successMetricsReport.getId()).averages);
  }

  @Test
  public void testGetChartData_ApplicationCounts_ExplicitApplicationFilter_Authorized() {
    Application app2 = tempEntity.newApplication(org.getId());

    createPolicyViolation(app, today);
    createPolicyViolation(app2, today);
    grantReadPermission(app.getId());

    Set<String> appIds = Sets.newHashSet(app.getId(), app2.getId());
    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null, appIds);
    assertApplicationCountsResult(
        successMetricsReportDataService.getChartData(successMetricsReport.getId()).applicationCounts);
  }

  @Test
  public void testGetChartData_ApplicationCounts_ExplicitOrganizationFilter_Authorized() {
    Application app2 = tempEntity.newApplication(org.getId());

    createPolicyViolation(app, today);
    createPolicyViolation(app2, today);

    grantReadPermission(app.getId());
    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(Collections.singleton(org.getId()), null);
    assertApplicationCountsResult(
        successMetricsReportDataService.getChartData(successMetricsReport.getId()).applicationCounts);
  }

  @Test
  public void testGetChartData_ApplicationCounts_ExplicitOrganizationFilter_AuthorizedOneOrg() {
    Organization org2 = tempEntity.newOrganization();
    Application app2 = tempEntity.newApplication(org2.getId());

    createPolicyViolation(app, today);
    createPolicyViolation(app2, today);

    grantReadPermission(org.getId());

    Set<String> orgIds = Sets.newHashSet(org.getId(), org2.getId());
    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(orgIds, null);
    assertApplicationCountsResult(
        successMetricsReportDataService.getChartData(successMetricsReport.getId()).applicationCounts);
  }

  @Test
  public void testGetChartData_ApplicationCounts_ImplicitApplicationFilter_Authorized() {
    Application app2 = tempEntity.newApplication(org.getId());

    createPolicyViolation(app, today);
    createPolicyViolation(app2, today);
    grantReadPermission(app.getId());
    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null, null);
    assertApplicationCountsResult(
        successMetricsReportDataService.getChartData(successMetricsReport.getId()).applicationCounts);
  }

  @Test
  public void testGetChartData_AuthorizationChangesAfterFirstReportGeneration() {
    LocalDate today = new LocalDate();

    Application app = tempEntity.newApplicationWithParent();

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null, Collections.singleton(app.getId()));

    login();
    createPolicyViolation(app, today, ONE_HOUR);

    // cause the initial report data to be generated
    successMetricsReportDataService.getChartData(successMetricsReport.getId());

    grantReadPermission(app.getId());

    // run the chart again
    SuccessMetricsChartDataDTO results = successMetricsReportDataService.getChartData(successMetricsReport.getId());

    // make sure that the data that comes back is fresh and not still using the cached empty
    // SuccessMetricsReportData from the previous run
    assertMttrResults(results.mttrs, today);
    assertApplicationCountsResult(results.applicationCounts);
    assertAveragesResults(results.averages);
  }

  private void createPolicyViolation(Application app, LocalDate today) {
    createPolicyViolation(app, today, ONE_HOUR);
  }

  private void createPolicyViolation(Application app, LocalDate today, long violationResolutionTimeMs) {
    Date eval1Date = today.withDayOfMonth(2).minusMonths(1).toDateTimeAtStartOfDay().toDate();
    Date eval2Date = new Date(eval1Date.getTime() + violationResolutionTimeMs);

    Policy policy =
        tempEntity.newPolicy(app, 5, LogicalOperator.AND, new Condition(LicenseConditionType.ID, "is", "GPL-2.0"));

    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "eval1", eval1Date);
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "eval2", eval2Date);

    // violation appears in eval1 but is resolved in eval2
    PolicyViolation violation = tempEntity.newPolicyViolation(eval1, policy);
    violation.setFixTime(eval2Date);
    new PolicyViolationDAO().update(violation);
  }

  private void assertMttrResults(List<MttrDTO> mttrDTOs, LocalDate today) {
    assertThat(mttrDTOs).hasSize(1);

    MttrDTO dto = mttrDTOs.get(0);
    assertThat(dto.timePeriodName).isEqualTo(today.minusMonths(1).monthOfYear().getAsShortText(Locale.US));
    assertThat(dto.mttrInSeconds).isEqualTo((int) ONE_HOUR / 1000);
    assertThat(dto.criticalMttrInSeconds).isNull();
  }

  private void assertAveragesResults(AverageDiscoveredPolicyViolationsDTO dto) {
    assertThat(dto.licenseViolations.averageDiscovered).isEqualTo(1.0);
    assertThat(dto.evaluationCount).isEqualTo(2.0);
  }

  private void assertApplicationCountsResult(ApplicationCountsDTO dto) {
    assertThat(dto.totalApplications).isEqualTo(1);
    assertThat(dto.activeApplications).isEqualTo(1);
    assertThat(dto.total.applicationsWithViolations).isEqualTo(1);
    assertThat(dto.total.applicationsWithCriticalViolations).isEqualTo(0);
    assertThat(dto.security.applicationsWithViolations).isEqualTo(0);
    assertThat(dto.security.applicationsWithCriticalViolations).isEqualTo(0);
    assertThat(dto.license.applicationsWithViolations).isEqualTo(1);
    assertThat(dto.license.applicationsWithCriticalViolations).isEqualTo(0);
    assertThat(dto.quality.applicationsWithViolations).isEqualTo(0);
    assertThat(dto.quality.applicationsWithCriticalViolations).isEqualTo(0);
    assertThat(dto.other.applicationsWithViolations).isEqualTo(0);
    assertThat(dto.other.applicationsWithCriticalViolations).isEqualTo(0);
  }

  private void assertEmptyResults(SuccessMetricsChartDataDTO chartDataDTO) {
    assertEmptyResults(chartDataDTO.mttrs);
    assertEmptyResults(chartDataDTO.averages);
    assertEmptyResults(chartDataDTO.applicationCounts);
  }

  private void assertEmptyResults(AverageDiscoveredPolicyViolationsDTO dto) {
    assertThat(dto.evaluationCount).isEqualTo(0.0);
    assertThat(dto.totalViolations.averageDiscovered).isEqualTo(0.0);
    assertThat(dto.totalViolations.averageDiscoveredCritical).isEqualTo(0.0);
    assertThat(dto.securityViolations.averageDiscovered).isEqualTo(0.0);
    assertThat(dto.securityViolations.averageDiscoveredCritical).isEqualTo(0.0);
    assertThat(dto.licenseViolations.averageDiscovered).isEqualTo(0.0);
    assertThat(dto.licenseViolations.averageDiscoveredCritical).isEqualTo(0.0);
    assertThat(dto.qualityViolations.averageDiscovered).isEqualTo(0.0);
    assertThat(dto.qualityViolations.averageDiscoveredCritical).isEqualTo(0.0);
    assertThat(dto.otherViolations.averageDiscovered).isEqualTo(0.0);
    assertThat(dto.otherViolations.averageDiscoveredCritical).isEqualTo(0.0);
  }

  private void assertEmptyResults(ApplicationCountsDTO dto) {
    assertThat(dto.totalApplications).isEqualTo(0);
    assertThat(dto.activeApplications).isEqualTo(0);
    assertThat(dto.total.applicationsWithViolations).isEqualTo(0);
    assertThat(dto.total.applicationsWithCriticalViolations).isEqualTo(0);
    assertThat(dto.security.applicationsWithViolations).isEqualTo(0);
    assertThat(dto.security.applicationsWithCriticalViolations).isEqualTo(0);
    assertThat(dto.license.applicationsWithViolations).isEqualTo(0);
    assertThat(dto.license.applicationsWithCriticalViolations).isEqualTo(0);
    assertThat(dto.quality.applicationsWithViolations).isEqualTo(0);
    assertThat(dto.quality.applicationsWithCriticalViolations).isEqualTo(0);
    assertThat(dto.other.applicationsWithViolations).isEqualTo(0);
    assertThat(dto.other.applicationsWithCriticalViolations).isEqualTo(0);
  }

  private void assertEmptyResults(List<?> dtos) {
    assertThat(dtos).isEmpty();
  }

  @Test
  public void testGetComponentCounts_Organization_Unauthenticated() {
    SuccessMetricsReport report = createSuccessMetricsReport(orgIds, null);
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      successMetricsReportDataService.getComponentCounts(report.getId());
      // can't look up SuccessMetricsReport if the user isn't logged in
    }).withMessageContaining("Cannot find a success metrics report");
  }

  @Test
  public void testGetComponentCounts_Organization_Unauthorized() {
    login();
    SuccessMetricsReport report = createSuccessMetricsReport(orgIds, null);
    ComponentCountsDTO result = successMetricsReportDataService.getComponentCounts(report.getId());
    assertThat(result).isNotNull();
    assertThat(result.componentsPerApplication).isEqualTo(0);
    assertThat(result.componentsInTheMostApplications).hasSize(0);
    assertThat(result.componentsWithTheMostViolations).hasSize(0);
  }

  @Test
  public void testGetComponentCounts_Organization_Authorized() {
    grantReadPermission(app.getId());
    SuccessMetricsReport report = createSuccessMetricsReport(orgIds, null);
    ComponentCountsDTO result = successMetricsReportDataService.getComponentCounts(report.getId());
    assertThat(result).isNotNull();
    assertThat(result.componentsPerApplication).isEqualTo(1);
    assertThat(result.componentsInTheMostApplications.get(0).count).isEqualTo(1);
    assertThat(result.componentsWithTheMostViolations.get(0).count).isEqualTo(1);
  }

  @Test
  public void testGetComponentCounts_Application_Unauthenticated() {
    SuccessMetricsReport report = createSuccessMetricsReport(null, appIds);
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      successMetricsReportDataService.getComponentCounts(report.getId());
      // can't look up SuccessMetricsReport if the user isn't logged in
    }).withMessageContaining("Cannot find a success metrics report");
  }

  @Test
  public void testGetComponentCounts_Application_Unauthorized() {
    login();
    SuccessMetricsReport report = createSuccessMetricsReport(null, appIds);
    ComponentCountsDTO result = successMetricsReportDataService.getComponentCounts(report.getId());
    assertThat(result).isNotNull();
    assertThat(result.componentsPerApplication).isEqualTo(0);
    assertThat(result.componentsInTheMostApplications).hasSize(0);
    assertThat(result.componentsWithTheMostViolations).hasSize(0);
  }

  @Test
  public void testGetComponentCount_Application_Authorized() {
    grantReadPermission(app.getId());
    SuccessMetricsReport report = createSuccessMetricsReport(null, appIds);
    ComponentCountsDTO result = successMetricsReportDataService.getComponentCounts(report.getId());
    assertThat(result).isNotNull();
    assertThat(result.componentsPerApplication).isEqualTo(1);
    assertThat(result.componentsInTheMostApplications.get(0).count).isEqualTo(1);
    assertThat(result.componentsWithTheMostViolations.get(0).count).isEqualTo(1);
  }
}
