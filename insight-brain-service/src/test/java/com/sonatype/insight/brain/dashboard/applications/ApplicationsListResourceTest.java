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
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightWork;

import java.util.Set;

import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationsListResourceTest
    extends AbstractResourceTest
{
  @After
  public void tearDownPreviewFlag() {
    tempEntity.deleteSystemConfigurationProperty(
        SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.getPropertyName());
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(DashboardResource.RESOURCE_PATH);
  }

  @Test
  public void listApplications_flagOn_returnsPaginatedIndexRows() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("Tribbles");
    tempEntity.newApplication("Apple Java", "apple-java1", org.getId());
    ApplicationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.page = 0;
    request.pageSize = 50;

    HttpResponse response = restRequest()
        .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
        .body(request)
        .post();

    assertResponseStatus(200, response);
    ApplicationsListResponseDTO body = response.getBody(ApplicationsListResponseDTO.class);
    assertThat(body.source).isEqualTo(ApplicationsListResponseDTO.SOURCE_INDEX);
    assertThat(body.total).isGreaterThanOrEqualTo(1);
    assertThat(body.applications).isNotEmpty();
    assertThat(body.applications.get(0).applicationId).isNotBlank();
    assertThat(body.applications.get(0).applicationName).isNotBlank();
    assertThat(body.facets).isNotNull();
    assertThat(body.facets.totalApplications).isGreaterThanOrEqualTo(1);
    assertThat(body.facets.organizations).isNull();
    assertThat(body.facets.applications).isNull();
    assertThat(body.facets.stages).isNull();
  }

  @Test
  public void listApplications_searchFiltersByPublicId() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("Tribbles");
    tempEntity.newApplication("Apple Java", "apple-java1", org.getId());
    tempEntity.newApplication("Banana Java", "banana-java2", org.getId());
    ApplicationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.search = "banana";
    request.page = 0;
    request.pageSize = 50;

    HttpResponse response = restRequest()
        .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
        .body(request)
        .post();

    assertResponseStatus(200, response);
    ApplicationsListResponseDTO body = response.getBody(ApplicationsListResponseDTO.class);
    assertThat(body.applications).extracting(item -> item.applicationId).contains("banana-java2");
    assertThat(body.applications).extracting(item -> item.applicationId).doesNotContain("apple-java1");
  }

  @Test
  public void listApplications_secondPage_returnsDistinctApplications() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("PaginationTribe");
    tempEntity.newApplication("Page App A", "page-app-a", org.getId());
    tempEntity.newApplication("Page App B", "page-app-b", org.getId());
    tempEntity.newApplication("Page App C", "page-app-c", org.getId());
    ApplicationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

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
  public void listApplications_noIndex_returns409() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);
    User user = createUserWithPermissions(Permission.READ);

    ApplicationsListTestSupport.runWithoutSearchIndex(
        lookup(InsightWork.class).getSearchIndexDir(),
        () -> {
          try {
            HttpResponse response = restRequest()
                .auth(user)
                .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
                .body(new ApplicationsListRequestDTO())
                .post();
            assertResponseStatus(409, response);
          }
          catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Test
  public void listApplications_flagOff_returns404() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);

    HttpResponse response = restRequest()
        .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
        .body(new ApplicationsListRequestDTO())
        .post();

    assertResponseStatus(404, response);
  }

  @Test
  public void listApplications_includeFacetsFalse_omitsFacets() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("FacetsTribe");
    tempEntity.newApplication("Facet App", "facet-app", org.getId());
    ApplicationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.includeFacets = false;

    HttpResponse response = restRequest()
        .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
        .body(request)
        .post();

    assertResponseStatus(200, response);
    assertThat(response.getBody(ApplicationsListResponseDTO.class).facets).isNull();
  }

  @Test
  public void listApplications_pageSizeAboveMax_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.pageSize = ApplicationsListService.MAX_PAGE_SIZE + 1;

    HttpResponse response = restRequest()
        .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
        .body(request)
        .post();

    assertResponseStatus(400, response);
  }

  @Test
  public void listApplications_negativePage_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.page = -1;

    HttpResponse response = restRequest()
        .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
        .body(request)
        .post();

    assertResponseStatus(400, response);
  }

  @Test
  public void listApplications_searchTooLong_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.search = "x".repeat(ApplicationsListService.MAX_SEARCH_LENGTH + 1);

    HttpResponse response = restRequest()
        .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
        .body(request)
        .post();

    assertResponseStatus(400, response);
  }

  @Test
  public void listApplications_multiWordSearch_matchesAllTokens() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("SearchTribe");
    tempEntity.newApplication("Apple Pie Shop", "apple-pie", org.getId());
    tempEntity.newApplication("Apple Juice Co", "apple-juice", org.getId());
    ApplicationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.search = "apple pie";
    request.organizationIds = Set.of(org.getId());

    HttpResponse response = restRequest()
        .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
        .body(request)
        .post();

    assertResponseStatus(200, response);
    ApplicationsListResponseDTO body = response.getBody(ApplicationsListResponseDTO.class);
    assertThat(body.applications).extracting(item -> item.applicationId).containsExactly("apple-pie");
  }

  @Test
  public void listApplications_scopedUser_seesOnlyReadableApplications() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("RbacTribe");
    Application readable = tempEntity.newApplication("Readable App", "readable-app", org.getId());
    tempEntity.newApplication("Hidden App", "hidden-app", org.getId());
    User reader = tempEntity.newUser("scoped-apps-reader");
    Role readRole = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(readable.getId(), readRole.getId(), reader.getUsername());
    ApplicationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    HttpResponse response = restRequest()
        .auth(reader)
        .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
        .body(new ApplicationsListRequestDTO())
        .post();

    assertResponseStatus(200, response);
    ApplicationsListResponseDTO body = response.getBody(ApplicationsListResponseDTO.class);
    assertThat(body.total).isEqualTo(1);
    assertThat(body.applications).hasSize(1);
    assertThat(body.applications.get(0).applicationId).isEqualTo(readable.getPublicId());
  }

  @Test
  public void listApplications_cleanEvaluation_returnsLastEvaluationTimeWithoutStageRisks() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("CleanTribe");
    Application app = tempEntity.newApplication("Clean App", "clean-app", org.getId());
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "clean-scan-1");
    ApplicationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.organizationIds = Set.of(org.getId());

    HttpResponse response = restRequest()
        .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
        .body(request)
        .post();

    assertResponseStatus(200, response);
    ApplicationsListResponseDTO body = response.getBody(ApplicationsListResponseDTO.class);
    assertThat(body.applications).hasSize(1);
    assertThat(body.applications.get(0).stageRisks).isEmpty();
    assertThat(body.applications.get(0).lastEvaluationTime).isNotNull();
  }

  @Test
  public void listApplications_withEvaluation_returnsEnrichedStageRisks() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("EvalTribe");
    Application app = tempEntity.newApplication("Eval App", "eval-app", org.getId());
    Policy policy = tempEntity.newPolicy(app);
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "eval-scan-1");
    tempEntity.newPolicyViolation(evaluation, policy);
    ApplicationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.organizationIds = Set.of(org.getId());

    HttpResponse response = restRequest()
        .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
        .body(request)
        .post();

    assertResponseStatus(200, response);
    ApplicationsListResponseDTO body = response.getBody(ApplicationsListResponseDTO.class);
    assertThat(body.applications).hasSize(1);
    assertThat(body.applications.get(0).stageRisks).isNotEmpty();
    assertThat(body.applications.get(0).lastEvaluationTime).isNotNull();
  }

  @Test
  public void listApplications_unsupportedTagIdsFilter_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.tagIds = Set.of("some-tag-id");

    HttpResponse response = restRequest()
        .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
        .body(request)
        .post();

    assertResponseStatus(400, response);
  }

  @Test
  public void listApplications_unsupportedStageIdsFilter_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.stageIds = Set.of(Stage.ID_BUILD);

    HttpResponse response = restRequest()
        .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
        .body(request)
        .post();

    assertResponseStatus(400, response);
  }

  @Test
  public void listApplications_unsupportedOrderBy_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.orderBy = "-lastEvaluationTime";

    HttpResponse response = restRequest()
        .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
        .body(request)
        .post();

    assertResponseStatus(400, response);
  }

  @Test
  public void listApplications_invalidPageSize_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.pageSize = 0;

    HttpResponse response = restRequest()
        .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
        .body(request)
        .post();

    assertResponseStatus(400, response);
  }
}
