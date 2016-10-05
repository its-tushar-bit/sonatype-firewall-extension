/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.StageReleaseStageType;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

public class DashboardResourceTest
    extends AbstractResourceTest
{

  private DashboardFilterDAO dashboardFilterDAO = new DashboardFilterDAO();

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(DashboardResource.RESOURCE_PATH);
  }

  @Test
  public void testGetNewestRisks() throws Exception {
    Application app = tempEntity.newApplicationWithParent("app1", "test application");

    Policy buildPolicy = tempEntity.newPolicy(app.getId(), "build policy");

    createFirstOccurrencePolicyViolation(app, buildPolicy, BuildStageType.ID);

    HttpResponse response = restRequest().path(DashboardResource.GET_NEWEST_RISKS_PATH)
        .body(new RisksFilterDTO()).post();

    assertResponseStatus(200, response);
    NewestRiskDTO[] dtos = response.getBody(NewestRiskDTO[].class);
    assertThat(dtos, arrayWithSize(1));
  }

  @Test
  public void testGetPolicySummary() throws Exception {
    Application app = tempEntity.newApplicationWithParent("app1", "test application");

    Policy buildPolicy = tempEntity.newPolicy(app.getId(), "build policy");

    createFirstOccurrencePolicyViolation(app, buildPolicy, BuildStageType.ID);

    HttpResponse response = restRequest().path(DashboardResource.GET_POLICY_SUMMARY_PATH)
        .body(new RisksFilterDTO()).post();

    assertResponseStatus(200, response);
    PolicySummaryDTO dto = response.getBody(PolicySummaryDTO.class);
    assertThat(dto, notNullValue());
    assertThat(dto.weeklyDeltaNew, hasSize(12));
  }

  @Test
  public void testDashboardUserFilterCRUD() throws Exception {
    // start with the default filter
    HttpRequest request = restRequest().path(DashboardResource.FILTERS_PATH);
    HttpResponse response = request.get();
    assertResponseStatus(200, response);

    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Tag tag = tempEntity.newTag(org.getId());
    DashboardFilterDTO dashboardFilterDTO = createDashboardFilter(app, tag);

    // Test the create
    response = request.body(dashboardFilterDTO).put();
    assertResponseStatus(200, response);

    DashboardFilter dashboardFilter = dashboardFilterDAO.getByUsername("admin");
    assertThat(dashboardFilter, notNullValue());
    // Register to make sure the the filter is deleted after the test
    tempEntity.register(dashboardFilter);

    DashboardFilterDTO returnedDashboardFilterDTO = response.getBody(DashboardFilterDTO.class);
    assertThat(returnedDashboardFilterDTO, notNullValue());
    assertDashboardFilterDTO(returnedDashboardFilterDTO, dashboardFilterDTO);

    // Now test the update
    dashboardFilterDTO = returnedDashboardFilterDTO;
    dashboardFilterDTO.minPolicyThreatLevel = 8;
    dashboardFilterDTO.maxPolicyThreatLevel = 20;
    response = request.body(dashboardFilterDTO).put();

    assertResponseStatus(200, response);
    returnedDashboardFilterDTO = response.getBody(DashboardFilterDTO.class);
    assertThat(returnedDashboardFilterDTO, notNullValue());
    assertDashboardFilterDTO(returnedDashboardFilterDTO, dashboardFilterDTO);

    // Now test get
    response = request.get();
    assertResponseStatus(200, response);
    returnedDashboardFilterDTO = response.getBody(DashboardFilterDTO.class);
    assertThat(returnedDashboardFilterDTO, notNullValue());
    assertDashboardFilterDTO(returnedDashboardFilterDTO, dashboardFilterDTO);

    // Finally test the delete
    response = request.body(null).delete();
    assertResponseStatus(204, response);
    assertThat(dashboardFilterDAO.getByUsername("admin"), nullValue());
  }

  private void assertDashboardFilterDTO(DashboardFilterDTO actual, DashboardFilterDTO expected) {
    assertThat(actual.minPolicyThreatLevel, is(expected.minPolicyThreatLevel));
    assertThat(actual.maxPolicyThreatLevel, is(expected.maxPolicyThreatLevel));
    assertThat(actual.applicationFilters, hasSize(1));
    assertThat(actual.applicationFilters.get(0), is(expected.applicationFilters.get(0)));
    assertThat(actual.tagFilters, hasSize(1));
    assertThat(actual.tagFilters.get(0), is(expected.tagFilters.get(0)));
    assertThat(actual.policyThreatCategoryFilters, hasSize(1));
    assertThat(actual.policyThreatCategoryFilters.get(0), is(expected.policyThreatCategoryFilters.get(0)));
    assertThat(actual.stageTypeFilters, hasSize(1));
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
    HttpResponse response = restRequest().path(DashboardResource.FILTERS_SUMMARY_PATH)
        .body(new RisksFilterDTO()).post();
    assertResponseStatus(200, response);
    FilterSummaryDTO dto = response.getBody(FilterSummaryDTO.class);
    assertThat(dto, is(notNullValue()));
  }

  @Test
  public void testGetNewestRisksExport() throws Exception {
    Application app = tempEntity.newApplicationWithParent("app1", "test application");
    Policy buildPolicy = tempEntity.newPolicy(app.getId(), "build policy");
    createFirstOccurrencePolicyViolation(app, buildPolicy, BuildStageType.ID);
    Policy stagePolicy = tempEntity.newPolicy(app.getId(), "stage policy");
    createFirstOccurrencePolicyViolation(app, stagePolicy, StageReleaseStageType.ID);

    RisksFilterDTO filter = new RisksFilterDTO();
    HttpResponse response = restRequest().path(DashboardResource.GET_NEWEST_RISKS_EXPORT_PATH)
        .part("filter", new String(JsonUtils.generate(filter))).post();

    assertResponseStatus(200, response);
    String timestamp = new SimpleDateFormat("yyyyMMdd-HH").format(new Date());
    assertThat(response.getHeader("Content-Disposition"), startsWith("attachment; filename=\"results-violations-" + timestamp));
    assertThat(response.getContentType(), is(equalTo("text/csv")));
    List<String> lines = Splitter.on("\r\n").splitToList(response.getBodyText());
    assertThat(lines.get(0), is(equalTo("Threat Level,Policy Name,Application Name,Component Name,Date First Seen")));
    assertThat(lines.get(1), startsWith("5,stage policy,test application,Group1 : Artifact1 : Version1,"));
    assertThat(lines.get(2), startsWith("5,build policy,test application,Group1 : Artifact1 : Version1,"));

    filter.stageIds = Sets.newHashSet(StageReleaseStageType.ID);
    response = restRequest().path(DashboardResource.GET_NEWEST_RISKS_EXPORT_PATH)
        .part("filter", new String(JsonUtils.generate(filter))).post();
    lines = Splitter.on("\r\n").splitToList(response.getBodyText());
    assertThat(lines.get(1), startsWith("5,stage policy,test application,Group1 : Artifact1 : Version1,"));
  }

  @Test
  public void testGetApplicationRisksExport() throws Exception {
    Application app = tempEntity.newApplicationWithParent("app1", "test application");
    Policy buildPolicy = tempEntity.newPolicy(app.getId(), "build policy");
    createFirstOccurrencePolicyViolation(app, buildPolicy, BuildStageType.ID);
    Policy stagePolicy = tempEntity.newPolicy(app.getId(), "stage policy");
    createFirstOccurrencePolicyViolation(app, stagePolicy, StageReleaseStageType.ID);

    RisksFilterDTO filter = new RisksFilterDTO();
    HttpResponse response = restRequest().path(DashboardResource.GET_APPLICATION_RISKS_EXPORT_PATH)
        .part("filter", new String(JsonUtils.generate(filter))).post();

    assertResponseStatus(200, response);
    String timestamp = new SimpleDateFormat("yyyyMMdd-HH").format(new Date());
    assertThat(response.getHeader("Content-Disposition"), startsWith("attachment; filename=\"results-applications-" + timestamp));
    assertThat(response.getContentType(), is(equalTo("text/csv")));
    List<String> lines = Splitter.on("\r\n").splitToList(response.getBodyText());
    assertThat(lines.get(0), is(equalTo("Application Name,Total Risk,Critical,Severe,Moderate,Low")));
    assertThat(lines.get(1), is(equalTo("test application,10,0,10,0,0")));

    filter.stageIds = Sets.newHashSet(StageReleaseStageType.ID);
    response = restRequest().path(DashboardResource.GET_APPLICATION_RISKS_EXPORT_PATH)
        .part("filter", new String(JsonUtils.generate(filter))).post();
    lines = Splitter.on("\r\n").splitToList(response.getBodyText());
    assertThat(lines.get(1), is(equalTo("test application,5,0,5,0,0")));
  }

  private PolicyViolation createFirstOccurrencePolicyViolation(Application app, Policy tempPolicy, String stageTypeId) {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), stageTypeId, "test scan id");
    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, tempPolicy);
    tempEntity.newFirstOccurrencePolicyViolation(violation.getId(), app.getId(), stageTypeId);
    return violation;
  }

  @Test
  public void testGetComponentRisksExport_returnValidCsvFileName() throws Exception {
    RisksFilterDTO filter = new RisksFilterDTO();
    HttpResponse response = restRequest().path(DashboardResource.GET_COMPONENT_RISKS_EXPORT_PATH)
        .part("filter", new String(JsonUtils.generate(filter))).post();
    assertResponseStatus(200, response);
    assertThat(response.getContentType(), is(equalTo("text/csv")));
    assertThat(response.getHeader("Content-Disposition"), startsWith("attachment; filename=\"results-components-"));
  }

  @Test
  public void testGetComponentRisksExport_returnValidCsvHeadersWithoutAppSetup() throws Exception {
    RisksFilterDTO filter = new RisksFilterDTO();
    HttpResponse response = restRequest().path(DashboardResource.GET_COMPONENT_RISKS_EXPORT_PATH)
        .part("filter", new String(JsonUtils.generate(filter))).post();
    assertResponseStatus(200, response);
    assertThat(response.getContentType(), is(equalTo("text/csv")));

    List<String> lines = Splitter.on("\r\n").splitToList(response.getBodyText());
    assertThat(lines.get(0), is(equalTo(ComponentRiskDTO.getCsvHeader())));
    assertThat(lines.size(), is(equalTo(1)));
  }

  @Test
  public void testGetComponentRisksExport_returnValidCsvContent() throws Exception {
    Application app = tempEntity.newApplicationWithParent("app1", "test application");
    Policy buildPolicy = tempEntity.newPolicy(app.getId(), "build policy");
    createFirstOccurrencePolicyViolation(app, buildPolicy, BuildStageType.ID);

    RisksFilterDTO filter = new RisksFilterDTO();
    HttpResponse response = restRequest().path(DashboardResource.GET_COMPONENT_RISKS_EXPORT_PATH)
        .part("filter", new String(JsonUtils.generate(filter))).post();
    assertResponseStatus(200, response);

    List<String> lines = Splitter.on("\r\n").splitToList(response.getBodyText());
    assertThat(lines.get(1), equalTo("Group1 : Artifact1 : Version1,1,5,0,5,0,0"));
  }
}
