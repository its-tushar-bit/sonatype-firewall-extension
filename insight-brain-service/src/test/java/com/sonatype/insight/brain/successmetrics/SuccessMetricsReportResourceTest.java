/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReport;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.collect.Ordering;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class SuccessMetricsReportResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(SuccessMetricsReportResource.RESOURCE_PATH);
  }

  @Test
  public void testSuccessMetricCRUD() throws Exception {
    String metricsName = "Metrics";
    User tempUser = tempEntity.newUser();
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());

    SuccessMetricsReportScopeDTO successMetricsScopeDTO = new SuccessMetricsReportScopeDTO(
        new HashSet<>(Arrays.asList(app.getId())), null);
    SuccessMetricsReportDTO successMetricsDTO = new SuccessMetricsReportDTO(metricsName, successMetricsScopeDTO);
    
    // Create
    HttpRequest request = restRequest().auth(tempUser.getUsername(), tempUser.getPassword());
    HttpResponse response = request.body(successMetricsDTO).post();
    assertResponseStatus(200, response);
    SuccessMetricsReportDTO result = response.getBody(SuccessMetricsReportDTO.class);
    assertThat(result, notNullValue());
    assertThat(result.id, notNullValue());
    assertThat(result.name, is(successMetricsDTO.name));
    
    // Get the SuccessMetricsReport
    response = request.get();
    assertResponseStatus(200, response);
    SuccessMetricsReportDTO[] results = response.getBody(SuccessMetricsReportDTO[].class);
    assertThat(results.length, is(1));
    assertThat(results[0].name, is(metricsName));

    // Try to update (unsupported)
    response = restRequest().auth(tempUser.getUsername(), tempUser.getPassword()).body(results[0])
        .subpath("{successMetricsId}").parameter(results[0].id).put();
    assertResponseStatus(405, response);
    assertThat(response.getStatusText(), is("Method Not Allowed"));

    // Delete
    response = restRequest().auth(tempUser.getUsername(), tempUser.getPassword()).subpath("{successMetricsId}")
        .parameter(results[0].id).delete();
    assertResponseStatus(204, response);

    // Get the SuccessMetricsReport
    response = request.get();
    assertResponseStatus(200, response);
    results = response.getBody(SuccessMetricsReportDTO[].class);
    assertThat(results.length, is(0));
  }

  @Test
  public void testGetChartData() throws Exception {
    /*
     * create two apps in two separate orgs. Each app has two evaluations - one that has a violation and a later
     * one that doesn't (ie, the violation is resolved in the later evaluation). The amount of time between the two
     * evaluations is different for each app. We test by passing in the id of only one app/org and ensuring that the
     * data that comes back is correct for that app/org and does not include the data for the other one
     */
    Organization org1 = tempEntity.newOrganization();
    String orgId1 = org1.getId();
    Application app1 = tempEntity.newApplication(orgId1);
    Organization org2 = tempEntity.newOrganization();
    String orgId2 = org2.getId();
    Application app2 = tempEntity.newApplication(orgId2);
    LocalDate now = new LocalDate();
    Date date1 = now.withDayOfMonth(1).minusMonths(1).toDate();
    Date date2 = new Date(date1.getTime() + 1000);
    Date date3 = new Date(date1.getTime() + 5000);
    String appId1 = app1.getId();
    String appId2 = app2.getId();
    Policy policy1 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(appId1, BuildStageType.ID, "scan1", date1);
    PolicyViolation violation1 = tempEntity.newPolicyViolation(eval1, policy1);
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(appId2, BuildStageType.ID, "scan2", date1);
    PolicyViolation violation2 = tempEntity.newPolicyViolation(eval2, policy1);
    tempEntity.newPolicyEvaluation(appId1, BuildStageType.ID, "scan3", date2);
    violation1.setFixTime(date2);
    new PolicyViolationDAO().update(violation1);
    tempEntity.newPolicyEvaluation(appId2, BuildStageType.ID, "scan4", date3);
    violation2.setFixTime(date3);
    new PolicyViolationDAO().update(violation2);

    SuccessMetricsReport report = createSuccessMetricsReport(null, Collections.singleton(appId1));

    HttpResponse response = restRequest().path(SuccessMetricsReportResource.CHART_DATA_PATH).parameter(report.getId())
        .get();

    SuccessMetricsChartDataDTO chartDto = response.getBody(SuccessMetricsChartDataDTO.class);

    assertMttrResponse(chartDto, new YearMonth(date1).monthOfYear().getAsShortText(Locale.US));
    assertAveragesResponse(chartDto);
    assertApplicationCountsResponse(chartDto);
    Date updateTime = Ordering.natural().max(now.withDayOfMonth(1).toDate(), now.withDayOfWeek(1).toDate());
    assertThat(chartDto.lastUpdated, is(updateTime));
    assertThat(chartDto.monthCount, is(1));
  }

  private SuccessMetricsReport createSuccessMetricsReport(Set<String> organizationIds, Set<String> applicationIds) {
    SuccessMetricsReportScopeDTO scope = new SuccessMetricsReportScopeDTO();
    scope.organizationIds = organizationIds;
    scope.applicationIds = applicationIds;

    return tempEntity.newSuccessMetricsReport(getUsername(), "report", JsonUtils.format(scope));
  }

  private void assertMttrResponse(SuccessMetricsChartDataDTO chartDto, String monthName) {
    List<MttrDTO> dtos = chartDto.mttrs;

    assertThat(dtos, hasSize(1));
    assertThat(dtos.get(0).timePeriodName, is(monthName));
    assertThat(dtos.get(0).mttrInSeconds, is(1));
    assertThat(dtos.get(0).criticalMttrInSeconds, is(nullValue()));
  }

  private void assertAveragesResponse(SuccessMetricsChartDataDTO chartDto) {
    AverageDiscoveredPolicyViolationsDTO dto = chartDto.averages;

    assertThat(dto, is(notNullValue()));
    assertThat(dto.licenseViolations.averageDiscovered, closeTo(1.0, 0.0001));
    assertThat(dto.licenseViolations.averageDiscoveredCritical, closeTo(0.0, 0.0001));
    assertThat(dto.evaluationCount, closeTo(2.0, 0.0001));
  }

  private void assertApplicationCountsResponse(SuccessMetricsChartDataDTO chartDto) {
    ApplicationCountsDTO dto = chartDto.applicationCounts;

    assertThat(dto, is(notNullValue()));
    assertThat(dto.totalApplications, is(1));
    assertThat(dto.activeApplications, is(1));
    assertThat(dto.total.applicationsWithViolations, is(1));
    assertThat(dto.total.applicationsWithCriticalViolations, is(0));
    assertThat(dto.security.applicationsWithViolations, is(0));
    assertThat(dto.security.applicationsWithCriticalViolations, is(0));
    assertThat(dto.license.applicationsWithViolations, is(1));
    assertThat(dto.license.applicationsWithCriticalViolations, is(0));
    assertThat(dto.quality.applicationsWithViolations, is(0));
    assertThat(dto.quality.applicationsWithCriticalViolations, is(0));
    assertThat(dto.other.applicationsWithViolations, is(0));
    assertThat(dto.other.applicationsWithCriticalViolations, is(0));
  }
}
