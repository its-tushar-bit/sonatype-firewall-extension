/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.aggregation;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.joda.time.LocalDate;
import org.junit.Test;

import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class PolicyViolationAggregationResourceTest
    extends AbstractResourceTest
{
  protected HttpRequest restRequest(String path) {
    return super.restRequest().path(PolicyViolationAggregationResource.RESOURCE_PATH, path);
  }

  @Test
  public void testGetMttrs() throws Exception {
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
    Date date1 = new LocalDate().withDayOfMonth(1).minusMonths(1).toDate();
    Date date2 = new Date(date1.getTime() + 1000);
    Date date3 = new Date(date1.getTime() + 5000);
    String appId1 = app1.getId();
    String appId2 = app2.getId();
    Policy policy1 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "policy1", 5);
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(appId1, BuildStageType.ID, "scan1", date1);
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(appId2, BuildStageType.ID, "scan2", date1);
    tempEntity.newPolicyEvaluation(appId1, BuildStageType.ID, "scan3", date2);
    tempEntity.newPolicyEvaluation(appId2, BuildStageType.ID, "scan4", date3);
    tempEntity.newPolicyViolation(eval1, policy1);
    tempEntity.newPolicyViolation(eval2, policy1);

    Set<String> orgIds = Collections.singleton(orgId1);
    Set<String> appIds = Collections.singleton(appId1);

    // test that app ids are passed
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("applicationIds", appIds);
    HttpResponse response = restRequest(PolicyViolationAggregationResource.GET_MTTRS).body(requestBody).post();
    assertMttrResponse(response, date1);

    // test that org ids are passed
    requestBody = new HashMap<>();
    requestBody.put("organizationIds", orgIds);
    response = restRequest(PolicyViolationAggregationResource.GET_MTTRS).body(requestBody).post();
    assertMttrResponse(response, date1);
  }

  @Test
  public void testGetAverages() throws Exception {
    // create two apps in two orgs
    Application app1 = tempEntity.newApplicationWithParent("appId1", "app 1", "test org 1");
    Application app2 = tempEntity.newApplicationWithParent("appId2", "app 2", "test org 2");
    Date startOfMonth = new LocalDate().withDayOfMonth(1).toDate();
    Date middleOfMonth = new LocalDate().withDayOfMonth(15).toDate();

    // an evaluation for each app, with one violation each
    Policy policy1 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "policy1", 5);
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scan1", middleOfMonth);
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "scan2", middleOfMonth);
    tempEntity.newPolicyViolation(eval1, policy1);
    tempEntity.newPolicyViolation(eval2, policy1);

    Set<String> orgIds = Collections.singleton(app1.getOrganizationId());
    Set<String> appIds = Collections.singleton(app1.getId());

    // test that app ids are passed
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("applicationIds", appIds);
    HttpResponse response = restRequest(PolicyViolationAggregationResource.GET_AVERAGES).body(requestBody).post();
    assertAveragesResponse(response, startOfMonth);

    // test that org ids are passed
    requestBody = new HashMap<>();
    requestBody.put("organizationIds", orgIds);
    response = restRequest(PolicyViolationAggregationResource.GET_AVERAGES).body(requestBody).post();
    assertAveragesResponse(response, startOfMonth);
  }

  @Test
  public void testGetApplicationCounts() throws Exception {
    // create two apps in two orgs
    Application app1 = tempEntity.newApplicationWithParent("appId1", "app 1", "test org 1");
    Application app2 = tempEntity.newApplicationWithParent("appId2", "app 2", "test org 2");
    Date middleOfMonth = new LocalDate().minusMonths(1).withDayOfMonth(15).toDate();

    // an evaluation for each app, with one violation each
    Policy policy1 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "policy1", 5);
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scan1", middleOfMonth);
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "scan2", middleOfMonth);
    tempEntity.newPolicyViolation(eval1, policy1);
    tempEntity.newPolicyViolation(eval2, policy1);

    Set<String> orgIds = Collections.singleton(app1.getOrganizationId());
    Set<String> appIds = Collections.singleton(app1.getId());

    // test that app ids are passed
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("applicationIds", appIds);
    HttpResponse response = restRequest(PolicyViolationAggregationResource.GET_APPLICATION_COUNTS).body(requestBody).post();
    assertApplicationCountsResponse(response);

    // test that org ids are passed
    requestBody = new HashMap<>();
    requestBody.put("organizationIds", orgIds);
    response = restRequest(PolicyViolationAggregationResource.GET_APPLICATION_COUNTS).body(requestBody).post();
    assertApplicationCountsResponse(response);
  }

  private void assertMttrResponse(HttpResponse response, Date date) {
    MttrDTO[] dtos = response.getBody(MttrDTO[].class);

    assertThat(dtos, arrayWithSize(1));
    assertThat(dtos[0].timePeriodStart, is(date));
    assertThat(dtos[0].mttrInSeconds, is(1));
    assertThat(dtos[0].criticalMttrInSeconds, is(nullValue()));
  }

  private void assertAveragesResponse(HttpResponse response, Date date) {
    SuccessMetricsAveragesDTO dto = response.getBody(SuccessMetricsAveragesDTO.class);

    assertThat(dto, is(notNullValue()));
    assertThat(dto.activeApplicationCount, is(1));
    assertThat(dto.averageDiscoveredPolicyViolations, hasSize(1));
    assertThat(dto.averageDiscoveredPolicyViolations.get(0).timePeriodStart, is(date));
    assertThat(dto.averageDiscoveredPolicyViolations.get(0).license.averageDiscoveredSevere, closeTo(1.0, 0.0001));
    assertThat(dto.averageDiscoveredPolicyViolations.get(0).license.averageDiscoveredCritical, closeTo(0.0, 0.0001));
    assertThat(dto.averageDiscoveredPolicyViolations.get(0).evaluationCount, is(1));
  }

  private void assertApplicationCountsResponse(HttpResponse response) {
    ApplicationCountsDTO dto = response.getBody(ApplicationCountsDTO.class);

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
