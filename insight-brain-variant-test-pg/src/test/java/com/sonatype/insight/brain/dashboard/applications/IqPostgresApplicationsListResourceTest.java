/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.applications;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dashboard.DashboardResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.variant.IqPostgresTest;
import com.sonatype.insight.brain.variant.IqTestContext;

import java.util.Date;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqPostgresTest
class IqPostgresApplicationsListResourceTest
{
  // Injected by IqPostgresServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  @AfterEach
  void tearDownPreviewFlag() {
    ctx.tempEntity()
        .deleteSystemConfigurationProperty(
            SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.getPropertyName());
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(DashboardResource.RESOURCE_PATH);
  }

  /** Mirrors {@code AbstractResourceTest.createUserWithPermissions} — not exposed on {@link IqTestContext}. */
  private User createUserWithPermissions(Permission... permissions) {
    User user = ctx.tempEntity().newUser();
    Role role = ctx.tempEntity().newRole(false /* global */, permissions);
    ctx.tempEntity().newMembershipMapping(Organization.ROOT_ORGANIZATION_ID, role.getId(), user.getUsername());
    return user;
  }

  @Test
  void listApplications_flagOn_returnsPaginatedIndexRows() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("Tribbles");
    ctx.tempEntity().newApplication("Apple Java", "apple-java1", org.getId());
    ApplicationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.page = 0;
    request.pageSize = 50;

    HttpResponse response = restRequest()
        .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
        .body(request)
        .post();

    ctx.assertResponseStatus(200, response);
    ApplicationsListResponseDTO body = response.getBody(ApplicationsListResponseDTO.class);
    assertThat(body.source).isEqualTo(ApplicationsListResponseDTO.SOURCE_INDEX);
    assertThat(body.total).isGreaterThanOrEqualTo(1);
    assertThat(body.applications).isNotEmpty();
    assertThat(body.applications.get(0).applicationId).isNotBlank();
    assertThat(body.applications.get(0).applicationName).isNotBlank();
    assertThat(body.facets).isNotNull();
    assertThat(body.facets.totalApplications).isGreaterThanOrEqualTo(1);
    assertThat(body.facets.organizations).isNotNull();
    assertThat(body.facets.applications).isNotNull();
    // No policy evaluations in this fixture, so stage violation counts are absent.
    assertThat(body.facets.stages).isNull();
  }

  @Test
  void listApplications_searchFiltersByPublicId() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("Tribbles");
    ctx.tempEntity().newApplication("Apple Java", "apple-java1", org.getId());
    ctx.tempEntity().newApplication("Banana Java", "banana-java2", org.getId());
    ApplicationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.search = "banana";
    request.page = 0;
    request.pageSize = 50;

    HttpResponse response = restRequest()
        .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
        .body(request)
        .post();

    ctx.assertResponseStatus(200, response);
    ApplicationsListResponseDTO body = response.getBody(ApplicationsListResponseDTO.class);
    assertThat(body.applications).extracting(item -> item.applicationId).contains("banana-java2");
    assertThat(body.applications).extracting(item -> item.applicationId).doesNotContain("apple-java1");
  }

  @Test
  void listApplications_secondPage_returnsDistinctApplications() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("PaginationTribe");
    ctx.tempEntity().newApplication("Page App A", "page-app-a", org.getId());
    ctx.tempEntity().newApplication("Page App B", "page-app-b", org.getId());
    ctx.tempEntity().newApplication("Page App C", "page-app-c", org.getId());
    ApplicationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.organizationIds = Set.of(org.getId());
    request.pageSize = 2;

    request.page = 0;
    ApplicationsListResponseDTO page0 = restRequest()
        .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
        .body(request)
        .post()
        .getBody(ApplicationsListResponseDTO.class);

    request.page = 1;
    ApplicationsListResponseDTO page1 = restRequest()
        .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
        .body(request)
        .post()
        .getBody(ApplicationsListResponseDTO.class);

    assertThat(page0.total).isEqualTo(3);
    assertThat(page0.applications).hasSize(2);
    assertThat(page0.hasNextPage).isTrue();
    assertThat(page1.applications).hasSize(1);
    assertThat(page1.hasNextPage).isFalse();

    var page0Ids = page0.applications.stream().map(card -> card.applicationId).toList();
    var page1Ids = page1.applications.stream().map(card -> card.applicationId).toList();
    assertThat(page0Ids).doesNotContainAnyElementsOf(page1Ids);
    assertThat(java.util.stream.Stream.concat(page0Ids.stream(), page1Ids.stream()))
        .containsExactlyInAnyOrder("page-app-a", "page-app-b", "page-app-c");
  }

  @Test
  void listApplications_noIndex_returns409() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);
    User user = createUserWithPermissions(Permission.READ);

    ApplicationsListTestSupport.runWithoutSearchIndex(
        ctx.lookup(InsightWork.class).getSearchIndexDir(),
        () -> {
          try {
            HttpResponse response = restRequest()
                .auth(user)
                .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
                .body(new ApplicationsListRequestDTO())
                .post();
            ctx.assertResponseStatus(409, response);
          }
          catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Test
  void listApplications_flagOff_returns404() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);

    HttpResponse response = restRequest()
        .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
        .body(new ApplicationsListRequestDTO())
        .post();

    ctx.assertResponseStatus(404, response);
  }

  @Test
  void listApplications_includeFacetsFalse_omitsFacets() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("FacetsTribe");
    ctx.tempEntity().newApplication("Facet App", "facet-app", org.getId());
    ApplicationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.includeFacets = false;

    HttpResponse response = restRequest()
        .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
        .body(request)
        .post();

    ctx.assertResponseStatus(200, response);
    assertThat(response.getBody(ApplicationsListResponseDTO.class).facets).isNull();
  }

  @Test
  void listApplications_pageSizeAboveMax_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.pageSize = ApplicationsListService.MAX_PAGE_SIZE + 1;

    HttpResponse response = restRequest()
        .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
        .body(request)
        .post();

    ctx.assertResponseStatus(400, response);
  }

  @Test
  void listApplications_negativePage_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.page = -1;

    HttpResponse response = restRequest()
        .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
        .body(request)
        .post();

    ctx.assertResponseStatus(400, response);
  }

  @Test
  void listApplications_searchTooLong_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.search = "x".repeat(ApplicationsListService.MAX_SEARCH_LENGTH + 1);

    HttpResponse response = restRequest()
        .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
        .body(request)
        .post();

    ctx.assertResponseStatus(400, response);
  }

  @Test
  void listApplications_multiWordSearch_matchesAllTokens() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("SearchTribe");
    ctx.tempEntity().newApplication("Apple Pie Shop", "apple-pie", org.getId());
    ctx.tempEntity().newApplication("Apple Juice Co", "apple-juice", org.getId());
    ApplicationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.search = "apple pie";
    request.organizationIds = Set.of(org.getId());

    HttpResponse response = restRequest()
        .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
        .body(request)
        .post();

    ctx.assertResponseStatus(200, response);
    ApplicationsListResponseDTO body = response.getBody(ApplicationsListResponseDTO.class);
    assertThat(body.applications).extracting(item -> item.applicationId).containsExactly("apple-pie");
  }

  @Test
  void listApplications_scopedUser_seesOnlyReadableApplications() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("RbacTribe");
    Application readable = ctx.tempEntity().newApplication("Readable App", "readable-app", org.getId());
    ctx.tempEntity().newApplication("Hidden App", "hidden-app", org.getId());
    User reader = ctx.tempEntity().newUser("scoped-apps-reader");
    Role readRole = ctx.tempEntity().newRole(false, Permission.READ);
    ctx.tempEntity().newMembershipMapping(readable.getId(), readRole.getId(), reader.getUsername());
    ApplicationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    HttpResponse response = restRequest()
        .auth(reader)
        .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
        .body(new ApplicationsListRequestDTO())
        .post();

    ctx.assertResponseStatus(200, response);
    ApplicationsListResponseDTO body = response.getBody(ApplicationsListResponseDTO.class);
    assertThat(body.total).isEqualTo(1);
    assertThat(body.applications).hasSize(1);
    assertThat(body.applications.get(0).applicationId).isEqualTo(readable.getPublicId());
  }

  @Test
  void listApplications_cleanEvaluation_returnsLastEvaluationTimeWithoutStageRisks() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("CleanTribe");
    Application app = ctx.tempEntity().newApplication("Clean App", "clean-app", org.getId());
    ctx.tempEntity().newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "clean-scan-1");
    ApplicationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.organizationIds = Set.of(org.getId());

    HttpResponse response = restRequest()
        .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
        .body(request)
        .post();

    ctx.assertResponseStatus(200, response);
    ApplicationsListResponseDTO body = response.getBody(ApplicationsListResponseDTO.class);
    assertThat(body.applications).hasSize(1);
    assertThat(body.applications.get(0).stageRisks).isEmpty();
    assertThat(body.applications.get(0).lastEvaluationTime).isNotNull();
  }

  @Test
  void listApplications_withEvaluation_returnsEnrichedStageRisks() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("EvalTribe");
    Application app = ctx.tempEntity().newApplication("Eval App", "eval-app", org.getId());
    Policy policy = ctx.tempEntity().newPolicy(app);
    PolicyEvaluation evaluation = ctx.tempEntity().newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "eval-scan-1");
    ctx.tempEntity().newPolicyViolation(evaluation, policy);
    ApplicationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.organizationIds = Set.of(org.getId());

    HttpResponse response = restRequest()
        .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
        .body(request)
        .post();

    ctx.assertResponseStatus(200, response);
    ApplicationsListResponseDTO body = response.getBody(ApplicationsListResponseDTO.class);
    assertThat(body.applications).hasSize(1);
    assertThat(body.applications.get(0).stageRisks).isNotEmpty();
    assertThat(body.applications.get(0).lastEvaluationTime).isNotNull();
  }

  @Test
  void listApplications_unsupportedTagIdsFilter_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.tagIds = Set.of("some-tag-id");

    HttpResponse response = restRequest()
        .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
        .body(request)
        .post();

    ctx.assertResponseStatus(400, response);
  }

  @Test
  void listApplications_stageIdsFilter_returnsMatchingApplications() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("StageFilterTribe");
    Application buildApp = ctx.tempEntity().newApplication("Build App", "build-app", org.getId());
    Application developApp = ctx.tempEntity().newApplication("Develop App", "develop-app", org.getId());
    Policy buildPolicy = ctx.tempEntity().newPolicy(buildApp);
    Policy developPolicy = ctx.tempEntity().newPolicy(developApp);
    PolicyEvaluation buildEvaluation =
        ctx.tempEntity().newPolicyEvaluation(buildApp.getId(), Stage.ID_BUILD, "build-scan-1");
    PolicyEvaluation developEvaluation =
        ctx.tempEntity().newPolicyEvaluation(developApp.getId(), Stage.ID_DEVELOP, "develop-scan-1");
    InsightWork insightWork = ctx.lookup(InsightWork.class);
    ReportTestUtils.createReportFile(buildEvaluation.getOwnerId(), buildEvaluation.getScanId(),
        ReportTestUtils.zipReportDir("/IndexSearchingTest/policyViolationReport", ctx.tempFolder()), insightWork);
    ReportTestUtils.createReportFile(developEvaluation.getOwnerId(), developEvaluation.getScanId(),
        ReportTestUtils.zipReportDir("/IndexSearchingTest/policyViolationReport", ctx.tempFolder()), insightWork);
    ctx.tempEntity().newPolicyViolation(buildEvaluation, buildPolicy);
    ctx.tempEntity().newPolicyViolation(developEvaluation, developPolicy);
    ApplicationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.organizationIds = Set.of(org.getId());
    request.stageIds = Set.of(Stage.ID_BUILD);

    HttpResponse response = restRequest()
        .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
        .body(request)
        .post();

    ctx.assertResponseStatus(200, response);
    ApplicationsListResponseDTO body = response.getBody(ApplicationsListResponseDTO.class);
    assertThat(body.applications).extracting(item -> item.applicationId).containsExactly("build-app");
    assertThat(body.total).isEqualTo(1);
  }

  @Test
  void listApplications_orderByLastEvaluationTime_sortsNewestFirstWithinPage() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("SortTribe");
    Application olderApp = ctx.tempEntity().newApplication("Older App", "older-app", org.getId());
    Application newerApp = ctx.tempEntity().newApplication("Newer App", "newer-app", org.getId());
    ctx.tempEntity().newPolicyEvaluation(olderApp.getId(), Stage.ID_BUILD, "older-scan", new Date(1_000L));
    ctx.tempEntity().newPolicyEvaluation(newerApp.getId(), Stage.ID_BUILD, "newer-scan", new Date(2_000L));
    ApplicationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.organizationIds = Set.of(org.getId());
    request.orderBy = "-lastEvaluationTime";

    HttpResponse response = restRequest()
        .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
        .body(request)
        .post();

    ctx.assertResponseStatus(200, response);
    ApplicationsListResponseDTO body = response.getBody(ApplicationsListResponseDTO.class);
    assertThat(body.applications).hasSize(2);
    assertThat(body.applications.get(0).applicationId).isEqualTo("newer-app");
    assertThat(body.applications.get(1).applicationId).isEqualTo("older-app");
    assertThat(body.applications.get(0).lastEvaluationTime).isEqualTo(2_000L);
    assertThat(body.applications.get(1).lastEvaluationTime).isEqualTo(1_000L);
  }

  @Test
  void listApplications_unsupportedOrderBy_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.orderBy = "-NAME";

    HttpResponse response = restRequest()
        .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
        .body(request)
        .post();

    ctx.assertResponseStatus(400, response);
  }

  @Test
  void listApplications_invalidPageSize_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.pageSize = 0;

    HttpResponse response = restRequest()
        .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
        .body(request)
        .post();

    ctx.assertResponseStatus(400, response);
  }
}
