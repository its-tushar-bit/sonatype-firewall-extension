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

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerComponent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReport;
import com.sonatype.insight.brain.variant.IqPostgresTest;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.collect.Ordering;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
 * IQ Server on PostgreSQL — converted from {@code SuccessMetricsReportResourceTest}. Package kept as the original
 * resource's package because {@link SuccessMetricsReportResource#CHART_DATA_PATH} and
 * {@link SuccessMetricsReportResource#COMPONENT_COUNTS_PATH} are package-private.
 */
@IqPostgresTest
class IqPostgresSuccessMetricsReportResourceTest
{
  private IqTestContext ctx;

  private PolicyViolationDAO policyViolationDAO;

  @BeforeEach
  void setUp() {
    policyViolationDAO = ctx.lookup(PolicyViolationDAO.class);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(SuccessMetricsReportResource.RESOURCE_PATH);
  }

  @Test
  void testSuccessMetricCRUD() throws Exception {
    String metricsName = "Metrics";
    User tempUser = ctx.tempEntity().newUser();
    Role role = ctx.tempEntity().newRole(true, Permission.CONFIGURE_SYSTEM);
    ctx.tempEntity().newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), tempUser.getUsername());
    Organization org = ctx.tempEntity().newOrganization();
    Application app = ctx.tempEntity().newApplication(org.getId());

    SuccessMetricsReportScopeDTO successMetricsScopeDTO = new SuccessMetricsReportScopeDTO(
        new HashSet<>(Collections.singletonList(app.getId())), null);
    SuccessMetricsReportDTO successMetricsDTO = new SuccessMetricsReportDTO(metricsName, successMetricsScopeDTO);

    // Create
    HttpRequest request = restRequest().auth(tempUser);
    HttpResponse response = request.body(successMetricsDTO).post();
    ctx.assertResponseStatus(200, response);
    SuccessMetricsReportDTO result = response.getBody(SuccessMetricsReportDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.id).isNotNull();
    assertThat(result.name).isEqualTo(successMetricsDTO.name);

    // Get the SuccessMetricsReport
    response = request.get();
    ctx.assertResponseStatus(200, response);
    SuccessMetricsReportDTO[] results = response.getBody(SuccessMetricsReportDTO[].class);
    assertThat(results.length).isEqualTo(1);
    assertThat(results[0].name).isEqualTo(metricsName);

    // Try to update (unsupported)
    response = request.subpath("{successMetricsId}").parameter(results[0].id).body(results[0]).put();
    ctx.assertResponseStatus(405, response);

    // Delete
    response = request.subpath("{successMetricsId}").parameter(results[0].id).delete();
    ctx.assertResponseStatus(204, response);

    // Get the SuccessMetricsReport
    response = request.get();
    ctx.assertResponseStatus(200, response);
    results = response.getBody(SuccessMetricsReportDTO[].class);
    assertThat(results).isEmpty();
  }

  @Test
  void testGetChartData() throws Exception {
    /*
     * create two apps in two separate orgs. Each app has two evaluations - one that has a violation and a later
     * one that doesn't (ie, the violation is resolved in the later evaluation). The amount of time between the two
     * evaluations is different for each app. We test by passing in the id of only one app/org and ensuring that the
     * data that comes back is correct for that app/org and does not include the data for the other one
     */
    Organization org1 = ctx.tempEntity().newOrganization();
    String orgId1 = org1.getId();
    Application app1 = ctx.tempEntity().newApplication(orgId1);
    Organization org2 = ctx.tempEntity().newOrganization();
    String orgId2 = org2.getId();
    Application app2 = ctx.tempEntity().newApplication(orgId2);
    LocalDate now = new LocalDate();
    Date date1 = now.withDayOfMonth(1).minusMonths(1).toDate();
    Date date2 = new Date(date1.getTime() + 1000);
    Date date3 = new Date(date1.getTime() + 5000);
    String appId1 = app1.getId();
    String appId2 = app2.getId();
    Policy policy1 = ctx.tempEntity().newPolicy(Organization.ROOT_ORGANIZATION_ID);
    PolicyEvaluation eval1 = ctx.tempEntity().newPolicyEvaluation(appId1, BuildStageType.ID, "scan1", date1);
    PolicyViolation violation1 = ctx.tempEntity().newPolicyViolation(eval1, policy1);
    PolicyEvaluation eval2 = ctx.tempEntity().newPolicyEvaluation(appId2, BuildStageType.ID, "scan2", date1);
    PolicyViolation violation2 = ctx.tempEntity().newPolicyViolation(eval2, policy1);
    ctx.tempEntity().newPolicyEvaluation(appId1, BuildStageType.ID, "scan3", date2);
    violation1.setFixTime(date2);
    policyViolationDAO.update(violation1);
    ctx.tempEntity().newPolicyEvaluation(appId2, BuildStageType.ID, "scan4", date3);
    violation2.setFixTime(date3);
    policyViolationDAO.update(violation2);

    SuccessMetricsReport report = createSuccessMetricsReport(null, Collections.singleton(appId1));

    HttpResponse response = restRequest().path(SuccessMetricsReportResource.CHART_DATA_PATH)
        .parameter(report.getId())
        .get();

    SuccessMetricsChartDataDTO chartDto = response.getBody(SuccessMetricsChartDataDTO.class);

    assertMttrResponse(chartDto, new YearMonth(date1).monthOfYear().getAsShortText(Locale.US));
    assertAveragesResponse(chartDto);
    assertApplicationCountsResponse(chartDto);
    Date updateTime = Ordering.natural().max(now.withDayOfMonth(1).toDate(), now.withDayOfWeek(1).toDate());
    assertThat(chartDto.lastUpdated).isEqualTo(updateTime);
    assertThat(chartDto.monthCount).isEqualTo(1);
  }

  private SuccessMetricsReport createSuccessMetricsReport(Set<String> organizationIds, Set<String> applicationIds) {
    SuccessMetricsReportScopeDTO scope = new SuccessMetricsReportScopeDTO();
    scope.organizationIds = organizationIds;
    scope.applicationIds = applicationIds;

    return ctx.tempEntity().newSuccessMetricsReport(ctx.getUsername(), "report", JsonUtils.format(scope));
  }

  private void assertMttrResponse(SuccessMetricsChartDataDTO chartDto, String monthName) {
    List<MttrDTO> dtos = chartDto.mttrs;

    assertThat(dtos).hasSize(1);
    assertThat(dtos.get(0).timePeriodName).isEqualTo(monthName);
    assertThat(dtos.get(0).mttrInSeconds).isEqualTo(1);
    assertThat(dtos.get(0).criticalMttrInSeconds).isNull();
  }

  private void assertAveragesResponse(SuccessMetricsChartDataDTO chartDto) {
    AverageDiscoveredPolicyViolationsDTO dto = chartDto.averages;

    assertThat(dto).isNotNull();
    assertThat(dto.securityViolations.averageDiscovered).isCloseTo(1.0, offset(0.0001));
    assertThat(dto.securityViolations.averageDiscoveredCritical).isCloseTo(0.0, offset(0.0001));
    assertThat(dto.evaluationCount).isCloseTo(2.0, offset(0.0001));
  }

  private void assertApplicationCountsResponse(SuccessMetricsChartDataDTO chartDto) {
    ApplicationCountsDTO dto = chartDto.applicationCounts;

    assertThat(dto).isNotNull();
    assertThat(dto.totalApplications).isEqualTo(1);
    assertThat(dto.activeApplications).isEqualTo(1);
    assertThat(dto.total.applicationsWithViolations).isEqualTo(1);
    assertThat(dto.total.applicationsWithCriticalViolations).isEqualTo(0);
    assertThat(dto.security.applicationsWithViolations).isEqualTo(1);
    assertThat(dto.security.applicationsWithCriticalViolations).isEqualTo(0);
    assertThat(dto.license.applicationsWithViolations).isEqualTo(0);
    assertThat(dto.license.applicationsWithCriticalViolations).isEqualTo(0);
    assertThat(dto.quality.applicationsWithViolations).isEqualTo(0);
    assertThat(dto.quality.applicationsWithCriticalViolations).isEqualTo(0);
    assertThat(dto.other.applicationsWithViolations).isEqualTo(0);
    assertThat(dto.other.applicationsWithCriticalViolations).isEqualTo(0);
  }

  @Test
  void testGetComponentCounts() throws Exception {
    // create two apps in two orgs
    Application app1 = ctx.tempEntity().newApplicationWithParent("appId1", "app 1", "test org 1");
    Application app2 = ctx.tempEntity().newApplicationWithParent("appId2", "app 2", "test org 2");

    // an evaluation for each app, with one violation each
    OwnerComponent component1 = ctx.tempEntity()
        .newApplicationComponent(app1.getId(), BuildStageType.ID, "scan1",
            ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"));
    ctx.tempEntity()
        .newApplicationComponent(app2.getId(), BuildStageType.ID, "scan2",
            ComponentIdentifier.createMavenCoordinates("groupId2", "artifactId2", "version2"));
    Policy policy1 = ctx.tempEntity().newPolicy(Organization.ROOT_ORGANIZATION_ID);
    PolicyEvaluation eval1 =
        ctx.tempEntity().newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId1", new Date());
    PolicyEvaluation eval2 =
        ctx.tempEntity().newPolicyEvaluation(app2.getId(), BuildStageType.ID, "scanId2", new Date());
    ctx.tempEntity().newPolicyViolation(eval1, policy1, "groupId", "artifactId", "version", "scan1", "reason1");
    ctx.tempEntity().newPolicyViolation(eval2, policy1, "groupId2", "artifactId2", "version2", "scan2", "reason2");

    Set<String> orgIds = Collections.singleton(app1.getOrganizationId());
    Set<String> appIds = Collections.singleton(app1.getId());

    SuccessMetricsReport report = createSuccessMetricsReport(orgIds, appIds);

    HttpResponse response = restRequest().path(SuccessMetricsReportResource.COMPONENT_COUNTS_PATH)
        .parameter(report.getId())
        .get();

    ctx.assertResponseStatus(200, response);
    assertGetComponentCountResponse(response, component1);
  }

  private void assertGetComponentCountResponse(HttpResponse response, OwnerComponent expectedComponent) {
    ComponentCountsDTO componentCountsDTO = response.getBody(ComponentCountsDTO.class);

    assertThat(componentCountsDTO).isNotNull();
    assertThat(componentCountsDTO.componentsPerApplication).isEqualTo(1);
    assertThat(componentCountsDTO.componentsInTheMostApplications).hasSize(1);
    assertThat(componentCountsDTO.componentsInTheMostApplications.get(0).count).isEqualTo(1);
    assertThat(componentCountsDTO.componentsInTheMostApplications.get(0).componentDisplayName)
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(expectedComponent.getComponentIdentifier()).toString());
    assertThat(componentCountsDTO.componentsWithTheMostViolations).hasSize(1);
    assertThat(componentCountsDTO.componentsWithTheMostViolations.get(0).count).isEqualTo(1);
    assertThat(componentCountsDTO.componentsWithTheMostViolations.get(0).componentDisplayName)
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(expectedComponent.getComponentIdentifier()).toString());
  }
}
