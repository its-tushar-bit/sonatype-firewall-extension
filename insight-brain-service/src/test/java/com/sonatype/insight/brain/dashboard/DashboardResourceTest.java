/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.junit.Test;

import static com.sonatype.insight.brain.dashboard.PolicyViolationDTOTestUtils.assertPolicyViolationDTO;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;

public class DashboardResourceTest
    extends AbstractResourceTest
{

  private DashboardFilterDAO dashboardFilterDAO = new DashboardFilterDAO();

  @Test
  public void testGetPolicyViolations() throws Exception {
    Application app = tempEntity.newApplicationWithParent("app1", "test application");

    Policy buildPolicy = tempEntity.newPolicy(app.getId(), "build policy");

    PolicyViolation violation = createPolicyViolation(app, buildPolicy, BuildStageType.ID);

    Response response = AuthedRestAccess.get(getRestUrl(DashboardResource.SERVICE_PATH + '/'
        + DashboardResource.GET_POLICY_VIOLATIONS_PATH));

    assertResponseStatus(200, response);
    PolicyViolationDTO[] dtos = JsonHelpers.fromJson(response.getResponseBody(), PolicyViolationDTO[].class);
    assertThat(dtos, arrayWithSize(1));

    assertPolicyViolationDTO(Arrays.asList(dtos), violation, app, buildPolicy);
  }

  @Test
  public void testGetPolicyViolationsWithMultipleStages() throws Exception {
    Application app = tempEntity.newApplicationWithParent("app1", "test application");
    Policy policy = tempEntity.newPolicy(app.getId(), "build policy");

    PolicyViolation buildViolation = createPolicyViolation(app, policy, BuildStageType.ID);
    PolicyViolation releaseViolation = createPolicyViolation(app, policy, ReleaseStageType.ID);

    Response response = AuthedRestAccess.get(getRestUrl(DashboardResource.SERVICE_PATH + '/'
        + DashboardResource.GET_POLICY_VIOLATIONS_PATH)
        + "?stageIds=build&stageIds=release");

    assertResponseStatus(200, response);
    PolicyViolationDTO[] dtos = JsonHelpers.fromJson(response.getResponseBody(), PolicyViolationDTO[].class);
    assertThat(dtos, arrayWithSize(2));

    List<PolicyViolationDTO> policyViolationDTOs = Arrays.asList(dtos);
    assertPolicyViolationDTO(policyViolationDTOs, buildViolation, app, policy);
    assertPolicyViolationDTO(policyViolationDTOs, releaseViolation, app, policy);
  }

  @Test
  public void testGetNewestPolicyViolations() throws Exception {
    Application app = tempEntity.newApplicationWithParent("app1", "test application");

    Policy buildPolicy = tempEntity.newPolicy(app.getId(), "build policy");

    createPolicyViolation(app, buildPolicy, BuildStageType.ID);
    PolicyViolation violation = createPolicyViolation(app, buildPolicy, BuildStageType.ID);
    tempEntity.newNewestPolicyViolation(violation.getId(), app.getId(), BuildStageType.ID);

    Response response = AuthedRestAccess.get(getRestUrl(DashboardResource.SERVICE_PATH + '/'
        + DashboardResource.GET_POLICY_VIOLATIONS_PATH)
        + "?newest=true");

    assertResponseStatus(200, response);
    PolicyViolationDTO[] dtos = JsonHelpers.fromJson(response.getResponseBody(), PolicyViolationDTO[].class);
    assertThat(dtos, arrayWithSize(1));

    assertPolicyViolationDTO(Arrays.asList(dtos), violation, app, buildPolicy);
  }

  @Test
  public void testGetPolicyViolationsByApplicationIds() throws Exception {
    Application app1 = tempEntity.newApplicationWithParent("app1", "test application 1");
    Application app2 = tempEntity.newApplicationWithParent("app2", "test application 2");

    Policy buildPolicy = tempEntity.newPolicy(app1.getId(), "build policy");
    Policy anotherBuildPolicy = tempEntity.newPolicy(app1.getId(), "another build policy");
    Policy app2BuildPolicy = tempEntity.newPolicy(app2.getId(), "app2 build policy");

    createPolicyViolation(app1, buildPolicy, BuildStageType.ID);
    createPolicyViolation(app1, anotherBuildPolicy, BuildStageType.ID);
    PolicyViolation app2Violation = createPolicyViolation(app2, app2BuildPolicy, BuildStageType.ID);

    Response response = AuthedRestAccess.get(getRestUrl(DashboardResource.SERVICE_PATH + '/'
        + DashboardResource.GET_POLICY_VIOLATIONS_PATH)
        + "?applicationPublicIds=" + app2.getPublicId());

    assertResponseStatus(200, response);
    PolicyViolationDTO[] dtos = JsonHelpers.fromJson(response.getResponseBody(), PolicyViolationDTO[].class);
    assertThat(dtos, arrayWithSize(1));

    assertPolicyViolationDTO(Arrays.asList(dtos), app2Violation, app2, app2BuildPolicy);
  }

  @Test
  public void testGetPolicyViolationsByApplicationIdsWithMultipleStages() throws Exception {
    Application app1 = tempEntity.newApplicationWithParent("app1", "test application 1");
    Application app2 = tempEntity.newApplicationWithParent("app2", "test application 2");

    Policy buildPolicy = tempEntity.newPolicy(app1.getId(), "build policy");
    Policy anotherBuildPolicy = tempEntity.newPolicy(app1.getId(), "another build policy");
    Policy app2Policy = tempEntity.newPolicy(app2.getId(), "app2 build policy");

    createPolicyViolation(app1, buildPolicy, BuildStageType.ID);
    createPolicyViolation(app1, anotherBuildPolicy, BuildStageType.ID);
    PolicyViolation app2BuildViolation = createPolicyViolation(app2, app2Policy, BuildStageType.ID);
    PolicyViolation app2ReleaseViolation = createPolicyViolation(app2, app2Policy, ReleaseStageType.ID);

    Response response = AuthedRestAccess.get(getRestUrl(DashboardResource.SERVICE_PATH + '/'
        + DashboardResource.GET_POLICY_VIOLATIONS_PATH)
        + "?applicationPublicIds=" + app2.getPublicId() + "&stageIds=build&stageIds=release");

    assertResponseStatus(200, response);
    PolicyViolationDTO[] dtos = JsonHelpers.fromJson(response.getResponseBody(), PolicyViolationDTO[].class);
    assertThat(dtos, arrayWithSize(2));

    List<PolicyViolationDTO> policyViolationDTOs = Arrays.asList(dtos);
    assertPolicyViolationDTO(policyViolationDTOs, app2BuildViolation, app2, app2Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2ReleaseViolation, app2, app2Policy);
  }

  @Test
  public void testGetNewestPolicyViolationsByApplicationIds() throws Exception {
    Application app1 = tempEntity.newApplicationWithParent("app1", "test application 1");
    Application app2 = tempEntity.newApplicationWithParent("app2", "test application 2");

    Policy buildPolicy = tempEntity.newPolicy(app1.getId(), "build policy");
    Policy anotherBuildPolicy = tempEntity.newPolicy(app1.getId(), "another build policy");
    Policy app2BuildPolicy = tempEntity.newPolicy(app2.getId(), "app2 build policy");

    createPolicyViolation(app1, buildPolicy, BuildStageType.ID);
    createPolicyViolation(app1, anotherBuildPolicy, BuildStageType.ID);
    createPolicyViolation(app2, app2BuildPolicy, BuildStageType.ID);
    PolicyViolation app2Violation = createPolicyViolation(app2, app2BuildPolicy, BuildStageType.ID);
    tempEntity.newNewestPolicyViolation(app2Violation.getId(), app2.getId(), BuildStageType.ID);

    Response response = AuthedRestAccess.get(getRestUrl(DashboardResource.SERVICE_PATH + '/'
        + DashboardResource.GET_POLICY_VIOLATIONS_PATH)
        + "?applicationPublicIds=" + app2.getPublicId() + "&newest=true");

    assertResponseStatus(200, response);
    PolicyViolationDTO[] dtos = JsonHelpers.fromJson(response.getResponseBody(), PolicyViolationDTO[].class);
    assertThat(dtos, arrayWithSize(1));

    assertPolicyViolationDTO(Arrays.asList(dtos), app2Violation, app2, app2BuildPolicy);
  }

  @Test
  public void testGetPolicyViolationsForNonExistentApplication() throws Exception {
    Response response = AuthedRestAccess.get(getRestUrl(DashboardResource.SERVICE_PATH + '/'
        + DashboardResource.GET_POLICY_VIOLATIONS_PATH)
        + "?applicationPublicIds=noapp1&applicationPublicIds=noapp2");

    assertResponseStatus(404, response);
  }

  @Test
  public void testGetPolicyViolationsWithDifferentStageTypeIds() throws Exception {
    Application app = tempEntity.newApplicationWithParent("app1", "test application");

    Policy buildPolicy = tempEntity.newPolicy(app.getId(), "build policy");
    Policy releasePolicy = tempEntity.newPolicy(app.getId(), "release policy");

    createPolicyViolation(app, buildPolicy, BuildStageType.ID);
    PolicyViolation violationRelease = createPolicyViolation(app, releasePolicy, ReleaseStageType.ID);

    Response response = AuthedRestAccess.get(getRestUrl(DashboardResource.SERVICE_PATH + '/'
        + DashboardResource.GET_POLICY_VIOLATIONS_PATH)
        + "?stageIds=" + ReleaseStageType.ID);

    assertResponseStatus(200, response);
    PolicyViolationDTO[] dtos = JsonHelpers.fromJson(response.getResponseBody(), PolicyViolationDTO[].class);
    assertThat(dtos, arrayWithSize(1));
    assertEquals(dtos[0].id, violationRelease.getId());
  }

  @Test
  public void testGetPolicyViolationsWithPolicyThreatLevel() throws Exception {
    Application app = tempEntity.newApplicationWithParent("app1", "test application");

    Policy buildPolicy = tempEntity.newPolicy(app.getId(), "build policy");
    Policy releasePolicy = tempEntity.newPolicy(app.getId(), "release policy");

    createPolicyViolation(app, buildPolicy, BuildStageType.ID);
    PolicyViolation violationRelease = createPolicyViolation(app, releasePolicy, ReleaseStageType.ID);

    // Range resulting in 0 results.
    Response response = AuthedRestAccess.get(getRestUrl(DashboardResource.SERVICE_PATH + '/'
        + DashboardResource.GET_POLICY_VIOLATIONS_PATH)
        + "?stageIds=" + ReleaseStageType.ID + "&policyThreatLevelRange=0,0");

    assertResponseStatus(200, response);
    PolicyViolationDTO[] dtos = JsonHelpers.fromJson(response.getResponseBody(), PolicyViolationDTO[].class);
    assertThat(dtos, arrayWithSize(0));

    // Range resulting in 1 result.
    response = AuthedRestAccess.get(getRestUrl(DashboardResource.SERVICE_PATH + '/'
        + DashboardResource.GET_POLICY_VIOLATIONS_PATH)
        + "?stageIds=" + ReleaseStageType.ID + "&policyThreatLevelRange=4,6");

    assertResponseStatus(200, response);
    dtos = JsonHelpers.fromJson(response.getResponseBody(), PolicyViolationDTO[].class);
    assertThat(dtos, arrayWithSize(1));
    assertEquals(dtos[0].id, violationRelease.getId());
  }

  @Test
  public void testGetPolicyViolationsWithPolicyThreatCategories() throws Exception {
    Application app = tempEntity.newApplicationWithParent("app1", "test application");

    Policy buildPolicy = tempEntity.newPolicy(app.getId(), "build policy");
    Policy releasePolicy = tempEntity.newPolicy(app.getId(), "release policy");

    createPolicyViolation(app, buildPolicy, BuildStageType.ID);
    PolicyViolation violationRelease = createPolicyViolation(app, releasePolicy, ReleaseStageType.ID);

    // Filter for a category that will return 0 results.
    Response response = AuthedRestAccess.get(getRestUrl(DashboardResource.SERVICE_PATH + '/'
        + DashboardResource.GET_POLICY_VIOLATIONS_PATH)
        + "?stageIds=" + ReleaseStageType.ID + "&policyThreatCategories=other");

    assertResponseStatus(200, response);
    PolicyViolationDTO[] dtos = JsonHelpers.fromJson(response.getResponseBody(), PolicyViolationDTO[].class);
    assertThat(dtos, arrayWithSize(0));

    // Filter for a category that will return 1 result.
    response = AuthedRestAccess.get(getRestUrl(DashboardResource.SERVICE_PATH + '/'
        + DashboardResource.GET_POLICY_VIOLATIONS_PATH)
        + "?stageIds=" + ReleaseStageType.ID + "&policyThreatCategories=quality,license");

    assertResponseStatus(200, response);
    dtos = JsonHelpers.fromJson(response.getResponseBody(), PolicyViolationDTO[].class);
    assertThat(dtos, arrayWithSize(1));
    assertEquals(dtos[0].id, violationRelease.getId());
  }

  @Test
  public void testGetPolicyViolationsWithPolicyThreatLevelRangeAndPolicyThreatCategories() throws Exception {
    Application app = tempEntity.newApplicationWithParent("app1", "test application");

    Policy buildPolicy = tempEntity.newPolicy(app.getId(), "build policy");
    Policy releasePolicy = tempEntity.newPolicy(app.getId(), "release policy");

    createPolicyViolation(app, buildPolicy, BuildStageType.ID);
    PolicyViolation violationRelease = createPolicyViolation(app, releasePolicy, ReleaseStageType.ID);

    // Filter with the correct threat range but the wrong category.
    Response response = AuthedRestAccess.get(getRestUrl(DashboardResource.SERVICE_PATH + '/'
        + DashboardResource.GET_POLICY_VIOLATIONS_PATH)
        + "?stageIds=" + ReleaseStageType.ID + "&policyThreatCategories=other&policyThreatLevelRange=4,6");

    assertResponseStatus(200, response);
    PolicyViolationDTO[] dtos = JsonHelpers.fromJson(response.getResponseBody(), PolicyViolationDTO[].class);
    assertThat(dtos, arrayWithSize(0));

    // Filter with the correct threat category but the wrong threat range.
    response = AuthedRestAccess.get(getRestUrl(DashboardResource.SERVICE_PATH + '/'
        + DashboardResource.GET_POLICY_VIOLATIONS_PATH)
        + "?stageIds=" + ReleaseStageType.ID + "&policyThreatCategories=license&policyThreatLevelRange=0,0");

    assertResponseStatus(200, response);
    dtos = JsonHelpers.fromJson(response.getResponseBody(), PolicyViolationDTO[].class);
    assertThat(dtos, arrayWithSize(0));

    // The right category and range, will return 1 result.
    response = AuthedRestAccess.get(getRestUrl(DashboardResource.SERVICE_PATH + '/'
        + DashboardResource.GET_POLICY_VIOLATIONS_PATH)
        + "?stageIds=" + ReleaseStageType.ID + "&policyThreatCategories=license&policyThreatLevelRange=4,6");

    assertResponseStatus(200, response);
    dtos = JsonHelpers.fromJson(response.getResponseBody(), PolicyViolationDTO[].class);
    assertThat(dtos, arrayWithSize(1));
    assertEquals(dtos[0].id, violationRelease.getId());
  }

  @Test
  public void testGetPolicyViolationsWithEmptyApplicationIdsQueryParameter() throws Exception {
    Response response = AuthedRestAccess.get(getRestUrl(DashboardResource.SERVICE_PATH + '/'
        + DashboardResource.GET_POLICY_VIOLATIONS_PATH)
        + "?applicationPublicIds");

    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(),
        is("Unable to get policy violations for null or empty application public IDs."));
  }

  @Test
  public void testGetPolicyViolationsWithEmptyPolicyThreatCategory() throws Exception {
    Response response = AuthedRestAccess.get(getRestUrl(DashboardResource.SERVICE_PATH + '/'
        + DashboardResource.GET_POLICY_VIOLATIONS_PATH)
        + "?policyThreatCategories");

    // Custom query parameter types are parsed as null when left empty, meaning that the filter will then not be
    // constructed leaving the resource in a valid state.
    assertResponseStatus(200, response);
  }

  @Test
  public void testGetPolicyViolationsWithEmptyPolicyThreatLevelRange() throws Exception {
    Response response = AuthedRestAccess.get(getRestUrl(DashboardResource.SERVICE_PATH + '/'
        + DashboardResource.GET_POLICY_VIOLATIONS_PATH)
        + "?policyThreatLevelRange");

    // Custom query parameter types are parsed as null when left empty, meaning that the filter will then not be
    // constructed leaving the resource in a valid state.
    assertResponseStatus(200, response);
  }

  @Test
  public void testGetPolicyViolationsWithNonExistentStageTypeId() throws Exception {
    Response response = AuthedRestAccess.get(getRestUrl(DashboardResource.SERVICE_PATH + '/'
        + DashboardResource.GET_POLICY_VIOLATIONS_PATH)
        + "?stageIds=" + "not-a-real-id");

    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), is("Unknown stage type: not-a-real-id."));
  }

  @Test
  public void testGetPolicyViolationsWithNonExistentPolicyThreatCategory() throws Exception {
    Response response = AuthedRestAccess.get(getRestUrl(DashboardResource.SERVICE_PATH + '/'
        + DashboardResource.GET_POLICY_VIOLATIONS_PATH)
        + "?policyThreatCategories=" + "not-a-real-policy-threat-category");

    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(),
        is("Unknown policy threat category with name: not-a-real-policy-threat-category"));
  }

  @Test
  public void testGetPolicyViolationsWithBadPolicyThreatLevelRange() throws Exception {
    Response response = AuthedRestAccess.get(getRestUrl(DashboardResource.SERVICE_PATH + '/'
        + DashboardResource.GET_POLICY_VIOLATIONS_PATH)
        + "?policyThreatLevelRange=1,0");

    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(),
        is("Minimum policy threat level should not exceed maximum policy threat level."));
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

  private PolicyViolation createPolicyViolation(Application app, Policy tempPolicy, String stageTypeId) {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), stageTypeId, "test scan id");
    return tempEntity.newPolicyViolation(evaluation.getId(), evaluation.getTime(), tempPolicy);
  }

  private String getRestUrl() {
    return getRestUrl(DashboardResource.SERVICE_PATH + "/" + DashboardResource.FILTERS_PATH);
  }
}
