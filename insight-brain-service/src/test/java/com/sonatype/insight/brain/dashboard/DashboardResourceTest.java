/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.ArrayList;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class DashboardResourceTest
    extends AbstractResourceTest
{

  private DashboardFilterDAO dashboardFilterDAO = new DashboardFilterDAO();

  @Test
  public void testGetNewestPolicyViolations() throws Exception {
    Application app = tempEntity.newApplicationWithParent("app1", "test application");

    Policy buildPolicy = tempEntity.newPolicy(app.getId(), "build policy");

    createNewestPolicyViolation(app, buildPolicy, BuildStageType.ID);

    Response response = AuthedRestAccess.get(getRestUrl(DashboardResource.SERVICE_PATH + '/'
        + DashboardResource.GET_NEWEST_RISKS_PATH));

    assertResponseStatus(200, response);
    NewestRiskDTO[] dtos = JsonHelpers.fromJson(response.getResponseBody(), NewestRiskDTO[].class);
    assertThat(dtos, arrayWithSize(1));
  }

  @Test
  public void testDashboardUserFilterCRUD() throws Exception {
    // No filter the first time
    Response response = AuthedRestAccess.get(getRestUrl());
    assertResponseStatus(204, response);

    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Tag tag = tempEntity.newTag(org.getId());
    DashboardFilterDTO dashboardFilterDTO = createDashboardFilter(app, tag);

    // Test the create
    String body = JsonHelpers.asJson(dashboardFilterDTO);
    response = AuthedRestAccess.put(getRestUrl(), body);
    assertResponseStatus(200, response);

    DashboardFilter dashboardFilter = dashboardFilterDAO.getByUsername("admin");
    assertThat(dashboardFilter, notNullValue());
    // Register to make sure the the filter is deleted after the test
    tempEntity.register(dashboardFilter);

    DashboardFilterDTO returnedDashboardFilterDTO = JsonHelpers
        .fromJson(response.getResponseBody(), DashboardFilterDTO.class);
    assertThat(returnedDashboardFilterDTO, notNullValue());
    assertDashboardFilterDTO(returnedDashboardFilterDTO, dashboardFilterDTO);

    // Now test the update
    dashboardFilterDTO = returnedDashboardFilterDTO;
    dashboardFilterDTO.minPolicyThreatLevel = 8;
    dashboardFilterDTO.maxPolicyThreatLevel = 20;
    body = JsonHelpers.asJson(dashboardFilterDTO);
    response = AuthedRestAccess.put(getRestUrl(), body);

    assertResponseStatus(200, response);
    returnedDashboardFilterDTO = JsonHelpers.fromJson(response.getResponseBody(), DashboardFilterDTO.class);
    assertThat(returnedDashboardFilterDTO, notNullValue());
    assertDashboardFilterDTO(returnedDashboardFilterDTO, dashboardFilterDTO);

    // Now test get
    response = AuthedRestAccess.get(getRestUrl());
    assertResponseStatus(200, response);
    returnedDashboardFilterDTO = JsonHelpers.fromJson(response.getResponseBody(), DashboardFilterDTO.class);
    assertThat(returnedDashboardFilterDTO, notNullValue());
    assertDashboardFilterDTO(returnedDashboardFilterDTO, dashboardFilterDTO);

    // Finally test the delete
    response = AuthedRestAccess.delete(getRestUrl());
    assertResponseStatus(204, response);
    assertThat(dashboardFilterDAO.getByUsername("admin"), nullValue());
  }

  private void assertDashboardFilterDTO(DashboardFilterDTO actual, DashboardFilterDTO expected) {
    assertThat(actual.minPolicyThreatLevel, is(expected.minPolicyThreatLevel));
    assertThat(actual.maxPolicyThreatLevel, is(expected.maxPolicyThreatLevel));
    assertThat(actual.applicationFilters.size(), is(1));
    assertThat(actual.applicationFilters.get(0), is(expected.applicationFilters.get(0)));
    assertThat(actual.tagFilters.size(), is(1));
    assertThat(actual.tagFilters.get(0), is(expected.tagFilters.get(0)));
    assertThat(actual.policyThreatCategoryFilters.size(), is(1));
    assertThat(actual.policyThreatCategoryFilters.get(0), is(expected.policyThreatCategoryFilters.get(0)));
    assertThat(actual.stageTypeFilters.size(), is(1));
    assertThat(actual.stageTypeFilters.get(0), is(expected.stageTypeFilters.get(0)));
  }

  private DashboardFilterDTO createDashboardFilter(Application application, Tag tag) {
    DashboardFilterDTO dashboardFilterDTO = new DashboardFilterDTO();
    dashboardFilterDTO.minPolicyThreatLevel = 1;
    dashboardFilterDTO.maxPolicyThreatLevel = 10;

    dashboardFilterDTO.applicationFilters = new ArrayList<>();
    dashboardFilterDTO.applicationFilters.add(application.getId());

    dashboardFilterDTO.tagFilters = new ArrayList<>();
    dashboardFilterDTO.tagFilters.add(tag.getId());

    dashboardFilterDTO.policyThreatCategoryFilters = new ArrayList<>();
    dashboardFilterDTO.policyThreatCategoryFilters.add(PolicyThreatCategory.SECURITY);

    dashboardFilterDTO.stageTypeFilters = new ArrayList<>();
    dashboardFilterDTO.stageTypeFilters.add(Stage.ID_BUILD);

    return dashboardFilterDTO;
  }

  @Test
  public void testGetFilterSummary() throws Exception {
    Response response = AuthedRestAccess.get(getRestUrl(DashboardResource.SERVICE_PATH + '/'
        + DashboardResource.FILTERS_SUMMARY_PATH));
    assertResponseStatus(200, response);
    FilterSummaryDTO dto = fromJson(response, FilterSummaryDTO.class);
    assertThat(dto, is(notNullValue()));
  }

  private PolicyViolation createNewestPolicyViolation(Application app, Policy tempPolicy, String stageTypeId) {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), stageTypeId, "test scan id");
    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, tempPolicy);
    tempEntity.newNewestPolicyViolation(violation.getId(), app.getId(), stageTypeId);
    return violation;
  }

  private String getRestUrl() {
    return getRestUrl(DashboardResource.SERVICE_PATH + "/" + DashboardResource.FILTERS_PATH);
  }
}
