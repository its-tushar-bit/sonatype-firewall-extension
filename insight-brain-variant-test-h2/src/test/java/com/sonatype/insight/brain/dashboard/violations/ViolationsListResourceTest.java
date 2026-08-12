/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.violations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dashboard.DashboardResource;
import com.sonatype.insight.brain.dashboard.PolicyViolationState;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Converted from the legacy {@code ViolationsListResourceTest}. Kept in the original package/simple name
 * because it uses the package-private {@link ViolationsListTestSupport} helper.
 */
@IqH2Test
class ViolationsListResourceTest
{
  // Injected by IqH2ServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private static final String VIOLATION_REPORT_RESOURCE = "/IndexSearchingTest/policyViolationReport";

  @AfterEach
  void tearDownPreviewFlag() {
    ctx.tempEntity()
        .deleteSystemConfigurationProperty(
            SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.getPropertyName());
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(DashboardResource.RESOURCE_PATH);
  }

  private User createUserWithPermissions(Permission... permissions) {
    User user = ctx.tempEntity().newUser();
    Role role = ctx.tempEntity().newRole(false /* global */, permissions);
    ctx.tempEntity().newMembershipMapping(Organization.ROOT_ORGANIZATION_ID, role.getId(), user.getUsername());
    return user;
  }

  // --- Feature flag gating ---------------------------------------------------------------------

  @Test
  void listViolations_flagOn_returnsPaginatedIndexRows() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("ViolationsTribe");
    Application app = ctx.tempEntity().newApplication("Vln App", "vln-app", org.getId());
    seedStandardViolations(org, app, "std");
    ViolationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ViolationsListResponseDTO body = post(scopedRequest(org)).getBody(ViolationsListResponseDTO.class);

    assertThat(body.source).isEqualTo(ViolationsListResponseDTO.SOURCE_INDEX);
    assertThat(body.total).isEqualTo(3);
    assertThat(body.violations).hasSize(3);
    assertThat(body.violations.get(0).policyViolationId).isNotBlank();
    assertThat(body.violations.get(0).applicationId).isEqualTo(app.getId());
    assertThat(body.violations.get(0).severity).isNotBlank();
    assertThat(body.facets).isNotNull();
    assertThat(body.facets.totalViolations).isEqualTo(3);
    assertThat(body.facets.states).isNotNull();
    assertThat(body.facets.threatCategories).isNotNull();
    assertThat(body.facets.stages).isNotNull();
  }

  @Test
  void listViolations_sessionReadPath_returnsRowsAndFacetsFromSeededIndex() throws Exception {
    System.setProperty("nexusOne.search.readPath.violations", "new");
    try {
      SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

      Organization org = ctx.tempEntity().newOrganization("SessionPathTribe");
      Application app = ctx.tempEntity().newApplication("Session Path App", "session-path-app", org.getId());
      seedStandardViolations(org, app, "sess");
      ViolationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

      ViolationsListResponseDTO body = post(scopedRequest(org)).getBody(ViolationsListResponseDTO.class);

      assertThat(body.source).isEqualTo(ViolationsListResponseDTO.SOURCE_INDEX);
      assertThat(body.total).isEqualTo(3);
      assertThat(body.violations).hasSize(3);
      assertThat(body.violations.get(0).policyViolationId).isNotBlank();
      assertThat(body.violations.get(0).applicationId).isEqualTo(app.getId());
      assertThat(body.violations.get(0).severity).isNotBlank();
      assertThat(body.facets).isNotNull();
      assertThat(body.facets.totalViolations).isEqualTo(3);
      assertThat(body.facets.states).isNotNull();
      assertThat(body.facets.threatCategories).isNotNull();
      assertThat(body.facets.stages).isNotNull();
      assertThat(body.facets.organizations).containsKey(org.getId());
      assertThat(body.facets.applications).containsKey(app.getId());
      assertThat(body.facets.organizationNames).containsEntry(org.getId(), org.getName());
      assertThat(body.facets.applicationNames).containsEntry(app.getId(), app.getName());
    }
    finally {
      System.clearProperty("nexusOne.search.readPath.violations");
    }
  }

  @Test
  void listViolations_flagOff_returns404() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);

    HttpResponse response = post(new ViolationsListRequestDTO());

    ctx.assertResponseStatus(404, response);
  }

  @Test
  void listViolations_noIndex_returns409() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);
    User user = createUserWithPermissions(Permission.READ);

    ViolationsListTestSupport.runWithoutSearchIndex(
        ctx.lookup(InsightWork.class).getSearchIndexDir(),
        () -> {
          try {
            HttpResponse response = restRequest()
                .auth(user)
                .path(ViolationsListResource.VIOLATIONS_LIST_PATH)
                .body(new ViolationsListRequestDTO())
                .post();
            ctx.assertResponseStatus(409, response);
          }
          catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  // --- Sort ------------------------------------------------------------------------------------

  @Test
  void listViolations_defaultSort_ordersByThreatDescending() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("SortTribe");
    Application app = ctx.tempEntity().newApplication("Sort App", "sort-app", org.getId());
    seedStandardViolations(org, app, "sort");
    ViolationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ViolationsListResponseDTO body = post(scopedRequest(org)).getBody(ViolationsListResponseDTO.class);

    assertThat(body.violations).extracting(row -> row.threatLevel).containsExactly(10, 8, 3);
  }

  @Test
  void listViolations_orderByThreatAscending_ordersByThreatAscending() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("SortAscTribe");
    Application app = ctx.tempEntity().newApplication("Sort Asc App", "sort-asc-app", org.getId());
    seedStandardViolations(org, app, "sortasc");
    ViolationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ViolationsListRequestDTO request = scopedRequest(org);
    request.orderBy = "policyThreatLevel";
    ViolationsListResponseDTO body = post(request).getBody(ViolationsListResponseDTO.class);

    assertThat(body.violations).extracting(row -> row.threatLevel).containsExactly(3, 8, 10);
  }

  @Test
  void listViolations_unsupportedOrderBy_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.orderBy = "-policyName";

    ctx.assertResponseStatus(400, post(request));
  }

  /**
   * Documents the known sort limitation (CLM-42254 review): threat-level ordering is applied per index
   * page after retrieval — {@code SearchIndexClient} has no sort parameter — so each returned page is
   * sorted highest-threat-first among the rows it happens to contain, but the "highest threat first"
   * contract is NOT guaranteed globally across page boundaries. Index-level sort is tracked under
   * CLM-42262. Kept as an explicit {@code total > pageSize} test so the limitation stays visible in the
   * suite rather than only in prose.
   */
  @Test
  void listViolations_sortIsPerPage_globalThreatOrderNotGuaranteedAcrossPages() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("CrossPageSortTribe");
    Application app = ctx.tempEntity().newApplication("Cross Page App", "cross-page-app", org.getId());
    seedStandardViolations(org, app, "xpage");
    ViolationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ViolationsListRequestDTO request = scopedRequest(org);
    request.pageSize = 2;

    request.page = 0;
    ViolationsListResponseDTO page0 = post(request).getBody(ViolationsListResponseDTO.class);
    request.page = 1;
    ViolationsListResponseDTO page1 = post(request).getBody(ViolationsListResponseDTO.class);

    // Each page is internally sorted highest-threat-first (the guarantee we actually provide)...
    assertThat(threatLevels(page0)).isSortedAccordingTo(Comparator.reverseOrder());
    assertThat(threatLevels(page1)).isSortedAccordingTo(Comparator.reverseOrder());

    // ...but because the sort is per-page, we only assert completeness across pages, not a global order.
    List<Integer> allThreats = new ArrayList<>(threatLevels(page0));
    allThreats.addAll(threatLevels(page1));
    assertThat(allThreats).containsExactlyInAnyOrder(10, 8, 3);
  }

  // --- Search ----------------------------------------------------------------------------------

  @Test
  void listViolations_searchFiltersByApplicationPublicId() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("SearchTribe");
    Application apple = ctx.tempEntity().newApplication("Apple Svc", "apple-svc", org.getId());
    Application banana = ctx.tempEntity().newApplication("Banana Svc", "banana-svc", org.getId());
    seedStandardViolations(org, apple, "apple");
    seedStandardViolations(org, banana, "banana");
    ViolationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ViolationsListRequestDTO request = scopedRequest(org);
    request.search = "banana-svc";
    ViolationsListResponseDTO body = post(request).getBody(ViolationsListResponseDTO.class);

    assertThat(body.violations).isNotEmpty();
    assertThat(body.violations).extracting(row -> row.applicationId).containsOnly(banana.getId());
  }

  @Test
  void listViolations_searchTooLong_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.search = "x".repeat(ViolationsListService.MAX_SEARCH_LENGTH + 1);

    ctx.assertResponseStatus(400, post(request));
  }

  // --- Pagination validation -------------------------------------------------------------------

  @Test
  void listViolations_negativePage_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.page = -1;

    ctx.assertResponseStatus(400, post(request));
  }

  @Test
  void listViolations_pageSizeAboveMax_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.pageSize = ViolationsListService.MAX_PAGE_SIZE + 1;

    ctx.assertResponseStatus(400, post(request));
  }

  @Test
  void listViolations_zeroPageSize_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.pageSize = 0;

    ctx.assertResponseStatus(400, post(request));
  }

  @Test
  void listViolations_secondPage_returnsDistinctViolations() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("PageTribe");
    Application app = ctx.tempEntity().newApplication("Page App", "page-app", org.getId());
    seedStandardViolations(org, app, "page");
    ViolationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ViolationsListRequestDTO request = scopedRequest(org);
    request.pageSize = 2;

    request.page = 0;
    ViolationsListResponseDTO page0 = post(request).getBody(ViolationsListResponseDTO.class);
    request.page = 1;
    ViolationsListResponseDTO page1 = post(request).getBody(ViolationsListResponseDTO.class);

    assertThat(page0.total).isEqualTo(3);
    assertThat(page0.violations).hasSize(2);
    assertThat(page0.hasNextPage).isTrue();
    assertThat(page1.violations).hasSize(1);
    assertThat(page1.hasNextPage).isFalse();

    List<String> page0Ids = page0.violations.stream().map(row -> row.policyViolationId).toList();
    List<String> page1Ids = page1.violations.stream().map(row -> row.policyViolationId).toList();
    assertThat(page0Ids).doesNotContainAnyElementsOf(page1Ids);
  }

  // --- Facets ----------------------------------------------------------------------------------

  @Test
  void listViolations_includeFacetsFalse_omitsFacets() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("NoFacetTribe");
    Application app = ctx.tempEntity().newApplication("No Facet App", "no-facet-app", org.getId());
    seedStandardViolations(org, app, "nofacet");
    ViolationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ViolationsListRequestDTO request = scopedRequest(org);
    request.includeFacets = false;

    assertThat(post(request).getBody(ViolationsListResponseDTO.class).facets).isNull();
  }

  @Test
  void listViolations_facetStateCounts_splitOpenAndWaived() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("StateFacetTribe");
    Application app = ctx.tempEntity().newApplication("State Facet App", "state-facet-app", org.getId());
    seedStandardViolations(org, app, "statefacet");
    ViolationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ViolationsListFacetsDTO facets = post(scopedRequest(org)).getBody(ViolationsListResponseDTO.class).facets;

    assertThat(facets.states).containsEntry(PolicyViolationState.OPEN.name(), 2L);
    assertThat(facets.states).containsEntry(PolicyViolationState.WAIVED.name(), 1L);
  }

  // --- Filters ---------------------------------------------------------------------------------

  @Test
  void listViolations_threatLevelRangeFilter_limitsRows() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("ThreatTribe");
    Application app = ctx.tempEntity().newApplication("Threat App", "threat-app", org.getId());
    seedStandardViolations(org, app, "threat");
    ViolationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ViolationsListRequestDTO request = scopedRequest(org);
    request.policyThreatLevelRange = new PolicyThreatLevelFilter(7, 10);
    ViolationsListResponseDTO body = post(request).getBody(ViolationsListResponseDTO.class);

    assertThat(body.violations).extracting(row -> row.threatLevel).containsExactly(10, 8);
  }

  @Test
  void listViolations_openStateFilter_excludesWaived() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("StateTribe");
    Application app = ctx.tempEntity().newApplication("State App", "state-app", org.getId());
    seedStandardViolations(org, app, "state");
    ViolationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ViolationsListRequestDTO request = scopedRequest(org);
    request.policyViolationStates = new PolicyViolationStateFilter(PolicyViolationState.OPEN);
    ViolationsListResponseDTO body = post(request).getBody(ViolationsListResponseDTO.class);

    assertThat(body.violations).hasSize(2);
    assertThat(body.violations).extracting(row -> row.state).containsOnly(PolicyViolationState.OPEN.name());
  }

  @Test
  void listViolations_threatCategoryFilter_limitsRows() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("CategoryTribe");
    Application app = ctx.tempEntity().newApplication("Category App", "category-app", org.getId());
    seedStandardViolations(org, app, "category");
    ViolationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ViolationsListRequestDTO request = scopedRequest(org);
    request.policyThreatCategories = new PolicyThreatCategoryFilter(PolicyThreatCategory.SECURITY);
    ViolationsListResponseDTO body = post(request).getBody(ViolationsListResponseDTO.class);

    assertThat(body.violations).hasSize(1);
    assertThat(body.violations.get(0).threatCategory).isEqualTo(PolicyThreatCategory.SECURITY.getName());
  }

  @Test
  void listViolations_stageFilter_limitsRows() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("StageTribe");
    Application app = ctx.tempEntity().newApplication("Stage App", "stage-app", org.getId());
    seedStandardViolations(org, app, "stage");
    ViolationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ViolationsListRequestDTO buildRequest = scopedRequest(org);
    buildRequest.stageIds = Set.of(Stage.ID_BUILD);
    assertThat(post(buildRequest).getBody(ViolationsListResponseDTO.class).violations).hasSize(3);

    ViolationsListRequestDTO releaseRequest = scopedRequest(org);
    releaseRequest.stageIds = Set.of(Stage.ID_RELEASE);
    assertThat(post(releaseRequest).getBody(ViolationsListResponseDTO.class).violations).isEmpty();
  }

  @Test
  void listViolations_unknownApplicationCategoryFilter_returnsEmpty() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("CategoryTribe");
    Application app = ctx.tempEntity().newApplication("Category App", "category-app", org.getId());
    seedStandardViolations(org, app, "catfilt");
    ViolationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    // Unknown category ids resolve to a no-match TERMS clause (not a 400).
    ViolationsListRequestDTO request = scopedRequest(org);
    request.applicationCategoryIds = Set.of("some-category-id");
    ViolationsListResponseDTO body = post(request).getBody(ViolationsListResponseDTO.class);

    assertThat(body.violations).isEmpty();
    assertThat(body.total).isZero();
  }

  @Test
  void listViolations_applicationCategoryFilter_returnsTaggedAppViolations() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("TaggedCategoryTribe");
    Application tagged = ctx.tempEntity().newApplication("Tagged App", "tagged-cat-app", org.getId());
    Application untagged = ctx.tempEntity().newApplication("Untagged App", "untagged-cat-app", org.getId());
    var category = ctx.tempEntity().newTag(org.getId(), "Platform Services");
    ctx.tempEntity().newApplicationTag(tagged.getId(), category.getId());

    seedStandardViolations(org, tagged, "tagged");
    seedStandardViolations(org, untagged, "untagged");
    ViolationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ViolationsListRequestDTO request = scopedRequest(org);
    request.applicationCategoryIds = Set.of(category.getId());
    ViolationsListResponseDTO body = post(request).getBody(ViolationsListResponseDTO.class);

    assertThat(body.total).isEqualTo(3);
    assertThat(body.violations).hasSize(3);
    assertThat(body.violations).extracting(row -> row.applicationId).containsOnly(tagged.getId());
  }

  @Test
  void listViolations_unsupportedAgeFilter_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.ageInDays = 30;

    ctx.assertResponseStatus(400, post(request));
  }

  @Test
  void listViolations_autoWaiverFilter_selectsAutoWaivedOnly() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("AutoWaiverTribe");
    Application app = ctx.tempEntity().newApplication("Auto Waiver App", "auto-waiver-app", org.getId());
    seedWaiverMixViolations(org, app, "autowvr");
    ViolationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ViolationsListRequestDTO request = scopedRequest(org);
    request.waivedWithAutoWaiver = true;
    ViolationsListResponseDTO body = post(request).getBody(ViolationsListResponseDTO.class);

    assertThat(body.violations).hasSize(1);
    assertThat(body.violations.get(0).waivedWithAutoWaiver).isTrue();
    assertThat(body.violations.get(0).state).isEqualTo(PolicyViolationState.WAIVED.name());
  }

  @Test
  void listViolations_manualWaiverFilter_selectsManuallyWaivedOnly() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("ManualWaiverTribe");
    Application app = ctx.tempEntity().newApplication("Manual Waiver App", "manual-waiver-app", org.getId());
    seedWaiverMixViolations(org, app, "manwvr");
    ViolationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ViolationsListRequestDTO request = scopedRequest(org);
    request.waivedWithAutoWaiver = false;
    ViolationsListResponseDTO body = post(request).getBody(ViolationsListResponseDTO.class);

    assertThat(body.violations).hasSize(1);
    assertThat(body.violations.get(0).waivedWithAutoWaiver).isFalse();
    assertThat(body.violations.get(0).state).isEqualTo(PolicyViolationState.WAIVED.name());
  }

  @Test
  void listViolations_autoWaiverWithWaivedState_returnsAutoWaivedRow() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("AutoWaiverStateTribe");
    Application app = ctx.tempEntity().newApplication("Auto Waiver State App", "auto-waiver-state-app", org.getId());
    seedWaiverMixViolations(org, app, "awstate");
    ViolationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ViolationsListRequestDTO request = scopedRequest(org);
    request.policyViolationStates = new PolicyViolationStateFilter(PolicyViolationState.WAIVED);
    request.waivedWithAutoWaiver = true;

    ViolationsListResponseDTO body = post(request).getBody(ViolationsListResponseDTO.class);
    assertThat(body.violations).hasSize(1);
    assertThat(body.violations.get(0).waivedWithAutoWaiver).isTrue();
  }

  @Test
  void listViolations_autoWaiverWithOpenState_returnsEmpty() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("AutoWaiverOpenTribe");
    Application app = ctx.tempEntity().newApplication("Auto Waiver Open App", "auto-waiver-open-app", org.getId());
    seedWaiverMixViolations(org, app, "awopen");
    ViolationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ViolationsListRequestDTO request = scopedRequest(org);
    request.policyViolationStates = new PolicyViolationStateFilter(PolicyViolationState.OPEN);
    request.waivedWithAutoWaiver = true;

    // OPEN and auto-waived is a contradictory combination — the index correctly returns no rows.
    assertThat(post(request).getBody(ViolationsListResponseDTO.class).violations).isEmpty();
  }

  @Test
  void listViolations_facetWaiverTypeCounts_splitAutoAndManual() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("WaiverFacetTribe");
    Application app = ctx.tempEntity().newApplication("Waiver Facet App", "waiver-facet-app", org.getId());
    seedWaiverMixViolations(org, app, "wvrfacet");
    ViolationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ViolationsListFacetsDTO facets = post(scopedRequest(org)).getBody(ViolationsListResponseDTO.class).facets;

    assertThat(facets.waiverTypes)
        .containsEntry(ViolationsListFacetsBuilder.WAIVER_TYPE_AUTO, 1L)
        .containsEntry(ViolationsListFacetsBuilder.WAIVER_TYPE_MANUAL, 1L);
  }

  @Test
  void listViolations_facetWaiverTypeCounts_showBothOptionsWhenOneSelected() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("WaiverSwitchTribe");
    Application app = ctx.tempEntity().newApplication("Waiver Switch App", "waiver-switch-app", org.getId());
    seedWaiverMixViolations(org, app, "wvrswitch");
    ViolationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    // Select AUTO. Because the waiver-type facet is single-select, it is counted against the query minus
    // its own clause, so MANUAL keeps its switchable count instead of collapsing to 0 under the
    // AUTO-narrowed rows (mealingr review, CLM-42261).
    ViolationsListRequestDTO request = scopedRequest(org);
    request.waivedWithAutoWaiver = true;
    ViolationsListResponseDTO body = post(request).getBody(ViolationsListResponseDTO.class);

    assertThat(body.violations).hasSize(1); // rows are still AUTO-narrowed
    assertThat(body.facets.waiverTypes)
        .containsEntry(ViolationsListFacetsBuilder.WAIVER_TYPE_AUTO, 1L)
        .containsEntry(ViolationsListFacetsBuilder.WAIVER_TYPE_MANUAL, 1L);
  }

  @Test
  void listViolations_legacyStateFilter_returnsPureLegacyOnly() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("LegacyStateTribe");
    Application app = ctx.tempEntity().newApplication("Legacy State App", "legacy-state-app", org.getId());
    seedLegacyMixViolations(org, app, "legstate");
    ViolationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ViolationsListRequestDTO request = scopedRequest(org);
    request.policyViolationStates = new PolicyViolationStateFilter(PolicyViolationState.LEGACY_VIOLATION);
    ViolationsListResponseDTO body = post(request).getBody(ViolationsListResponseDTO.class);

    // Only the pure-legacy violation reads as LEGACY_VIOLATION. The waived+legacy violation indexes as
    // Waived by precedence and is NOT returned here (documented divergence from the SQL path).
    assertThat(body.violations).hasSize(1);
    assertThat(body.violations.get(0).state).isEqualTo(PolicyViolationState.LEGACY_VIOLATION.name());
  }

  @Test
  void listViolations_openStateFilter_excludesLegacy() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("LegacyOpenTribe");
    Application app = ctx.tempEntity().newApplication("Legacy Open App", "legacy-open-app", org.getId());
    seedLegacyMixViolations(org, app, "legopen");
    ViolationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ViolationsListRequestDTO request = scopedRequest(org);
    request.policyViolationStates = new PolicyViolationStateFilter(PolicyViolationState.OPEN);
    ViolationsListResponseDTO body = post(request).getBody(ViolationsListResponseDTO.class);

    // OPEN excludes Legacy (and Waived/AutoWaived): only the plain open violation is returned.
    assertThat(body.violations).hasSize(1);
    assertThat(body.violations.get(0).state).isEqualTo(PolicyViolationState.OPEN.name());
  }

  @Test
  void listViolations_facetStateCounts_splitOpenWaivedLegacy() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("LegacyFacetTribe");
    Application app = ctx.tempEntity().newApplication("Legacy Facet App", "legacy-facet-app", org.getId());
    seedLegacyMixViolations(org, app, "legfacet");
    ViolationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ViolationsListFacetsDTO facets = post(scopedRequest(org)).getBody(ViolationsListResponseDTO.class).facets;

    // Seed: 1 open, 1 plain-waived, 1 pure-legacy, 1 waived+legacy (indexes Waived). So WAIVED=2,
    // LEGACY=1, OPEN=1. OPEN must exclude the legacy rows.
    assertThat(facets.states)
        .containsEntry(PolicyViolationState.OPEN.name(), 1L)
        .containsEntry(PolicyViolationState.WAIVED.name(), 2L)
        .containsEntry(PolicyViolationState.LEGACY_VIOLATION.name(), 1L);
  }

  // --- Multi-select state filters against a real index -----------------------------------------
  // These assert the returned row SETS (not just the generated query string) for OR-combined state
  // selections. Selecting OPEN together with exactly one other state is the case where the OPEN clause
  // must carry its own positive anchor inside the OR; a bare NOT there parses to a BooleanQuery with no
  // positive anchor and returns zero rows, which a query-string-only assertion cannot catch.

  @Test
  void listViolations_openAndWaivedStateFilter_returnsOpenAndWaivedRows() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("LegacyOpenWaivedTribe");
    Application app = ctx.tempEntity().newApplication("Legacy Open Waived App", "legacy-openwaived-app", org.getId());
    seedLegacyMixViolations(org, app, "legopw");
    ViolationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ViolationsListRequestDTO request = scopedRequest(org);
    request.policyViolationStates =
        new PolicyViolationStateFilter(PolicyViolationState.OPEN, PolicyViolationState.WAIVED);
    ViolationsListResponseDTO body = post(request).getBody(ViolationsListResponseDTO.class);

    // Seed: 1 open, 1 plain-waived, 1 pure-legacy, 1 waived+legacy (indexes Waived). OPEN+WAIVED must
    // return the open row plus both waived rows (3), and must NOT drop to zero (the anchored-OPEN fix).
    assertThat(body.violations).extracting(row -> row.state)
        .containsExactlyInAnyOrder(
            PolicyViolationState.OPEN.name(),
            PolicyViolationState.WAIVED.name(),
            PolicyViolationState.WAIVED.name());
  }

  @Test
  void listViolations_openAndLegacyStateFilter_returnsOpenAndPureLegacyRows() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("LegacyOpenLegacyTribe");
    Application app = ctx.tempEntity().newApplication("Legacy Open Legacy App", "legacy-openlegacy-app", org.getId());
    seedLegacyMixViolations(org, app, "legopl");
    ViolationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ViolationsListRequestDTO request = scopedRequest(org);
    request.policyViolationStates =
        new PolicyViolationStateFilter(PolicyViolationState.OPEN, PolicyViolationState.LEGACY_VIOLATION);
    ViolationsListResponseDTO body = post(request).getBody(ViolationsListResponseDTO.class);

    // OPEN+LEGACY must return the open row plus the pure-legacy row (2). The waived+legacy row indexes
    // as Waived and is excluded. Must NOT drop to zero (the anchored-OPEN fix).
    assertThat(body.violations).extracting(row -> row.state)
        .containsExactlyInAnyOrder(
            PolicyViolationState.OPEN.name(),
            PolicyViolationState.LEGACY_VIOLATION.name());
  }

  @Test
  void listViolations_waivedAndLegacyStateFilter_returnsWaivedAndPureLegacyRows() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("LegacyWaivedLegacyTribe");
    Application app = ctx.tempEntity().newApplication("Legacy Waived Legacy App", "legacy-wvdlegacy-app", org.getId());
    seedLegacyMixViolations(org, app, "legwl");
    ViolationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ViolationsListRequestDTO request = scopedRequest(org);
    request.policyViolationStates =
        new PolicyViolationStateFilter(PolicyViolationState.WAIVED, PolicyViolationState.LEGACY_VIOLATION);
    ViolationsListResponseDTO body = post(request).getBody(ViolationsListResponseDTO.class);

    // WAIVED+LEGACY (both positive clauses, no OPEN anchor needed): both waived rows plus the
    // pure-legacy row (3). The open row is excluded.
    assertThat(body.violations).extracting(row -> row.state)
        .containsExactlyInAnyOrder(
            PolicyViolationState.WAIVED.name(),
            PolicyViolationState.WAIVED.name(),
            PolicyViolationState.LEGACY_VIOLATION.name());
  }

  @Test
  void listViolations_allThreeStateFilter_returnsAllRows() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("LegacyAllStatesTribe");
    Application app = ctx.tempEntity().newApplication("Legacy All States App", "legacy-allstates-app", org.getId());
    seedLegacyMixViolations(org, app, "legall");
    ViolationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ViolationsListRequestDTO request = scopedRequest(org);
    request.policyViolationStates = new PolicyViolationStateFilter(
        PolicyViolationState.OPEN, PolicyViolationState.WAIVED, PolicyViolationState.LEGACY_VIOLATION);
    ViolationsListResponseDTO body = post(request).getBody(ViolationsListResponseDTO.class);

    // All three states is the whole indexed domain: all four seeded violations are returned.
    assertThat(body.violations).extracting(row -> row.state)
        .containsExactlyInAnyOrder(
            PolicyViolationState.OPEN.name(),
            PolicyViolationState.WAIVED.name(),
            PolicyViolationState.WAIVED.name(),
            PolicyViolationState.LEGACY_VIOLATION.name());
  }

  // --- RBAC ------------------------------------------------------------------------------------

  @Test
  void listViolations_scopedUser_seesOnlyReadableViolations() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("RbacTribe");
    Application readable = ctx.tempEntity().newApplication("Readable App", "readable-vln-app", org.getId());
    Application hidden = ctx.tempEntity().newApplication("Hidden App", "hidden-vln-app", org.getId());
    seedStandardViolations(org, readable, "readable");
    seedStandardViolations(org, hidden, "hidden");

    User reader = ctx.tempEntity().newUser("scoped-violations-reader");
    Role readRole = ctx.tempEntity().newRole(false /* global */, Permission.READ);
    ctx.tempEntity().newMembershipMapping(readable.getId(), readRole.getId(), reader.getUsername());
    ViolationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    HttpResponse response = restRequest()
        .auth(reader)
        .path(ViolationsListResource.VIOLATIONS_LIST_PATH)
        .body(new ViolationsListRequestDTO())
        .post();

    ctx.assertResponseStatus(200, response);
    ViolationsListResponseDTO body = response.getBody(ViolationsListResponseDTO.class);
    assertThat(body.total).isEqualTo(3);
    assertThat(body.violations).extracting(row -> row.applicationId).containsOnly(readable.getId());
  }

  @Test
  void listViolations_unauthenticated_returns401() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    HttpResponse response = restRequest()
        .path(ViolationsListResource.VIOLATIONS_LIST_PATH)
        .body(new ViolationsListRequestDTO())
        .anon()
        .post();

    ctx.assertResponseStatus(401, response);
  }

  @Test
  void listViolations_authenticatedWithNoGrants_seesNoViolations() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("NoGrantsTribe");
    Application app = ctx.tempEntity().newApplication("No Grants App", "no-grants-app", org.getId());
    seedStandardViolations(org, app, "nogrants");
    // Authenticated user with no role/membership mapping: RBAC scoping must yield nothing rather than
    // letting a broken permission gate expose violations (CLM-42254 review — guards the fail-open case).
    User strangerWithNoGrants = ctx.tempEntity().newUser("violations-no-grants");
    ViolationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    HttpResponse response = restRequest()
        .auth(strangerWithNoGrants)
        .path(ViolationsListResource.VIOLATIONS_LIST_PATH)
        .body(new ViolationsListRequestDTO())
        .post();

    ctx.assertResponseStatus(200, response);
    ViolationsListResponseDTO body = response.getBody(ViolationsListResponseDTO.class);
    assertThat(body.total).isEqualTo(0);
    assertThat(body.violations).isEmpty();
  }

  // --- Helpers ---------------------------------------------------------------------------------

  private static List<Integer> threatLevels(final ViolationsListResponseDTO body) {
    return body.violations.stream().map(row -> row.threatLevel).toList();
  }

  private HttpResponse post(final ViolationsListRequestDTO request) throws Exception {
    return restRequest()
        .path(ViolationsListResource.VIOLATIONS_LIST_PATH)
        .body(request)
        .post();
  }

  private static ViolationsListRequestDTO scopedRequest(final Organization org) {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.organizationIds = Set.of(org.getId());
    return request;
  }

  /**
   * Seeds one build-stage report and three violations for {@code app}: a critical security
   * violation (threat 10, open), a license violation (threat 8, open), and a quality violation
   * (threat 3, manually waived). {@code hashPrefix} keeps component hashes unique per application.
   */
  private void seedStandardViolations(
      final Organization org,
      final Application app,
      final String hashPrefix) throws Exception
  {
    PolicyEvaluation evaluation = ctx.tempEntity()
        .newPolicyEvaluation(app.getId(), Stage.ID_BUILD,
            "violations-scan-" + hashPrefix);
    ReportTestUtils.createReportFile(evaluation.getOwnerId(), evaluation.getScanId(),
        ReportTestUtils.zipReportDir(VIOLATION_REPORT_RESOURCE, ctx.tempFolder()), ctx.lookup(InsightWork.class));

    Policy security = ctx.tempEntity().newPolicy(org.getId(), "Security - Critical " + hashPrefix);
    Policy license = ctx.tempEntity().newPolicy(org.getId(), "Legal - Copyleft " + hashPrefix);
    Policy quality = ctx.tempEntity().newPolicy(org.getId(), "Quality - Standards " + hashPrefix);

    ctx.tempEntity()
        .newPolicyViolation(evaluation, security, 10, PolicyThreatCategory.SECURITY,
            "org.apache.logging", "log4j-core", "2.14.0", hash(hashPrefix, "log4j"));
    ctx.tempEntity()
        .newPolicyViolation(evaluation, license, 8, PolicyThreatCategory.LICENSE,
            "com.lodash", "lodash", "4.17.15", hash(hashPrefix, "lodash"));

    PolicyWaiver waiver = ctx.tempEntity().newWaiver(quality.getId(), org.getId());
    ctx.tempEntity()
        .newWaivedPolicyViolation(evaluation, quality, 3, PolicyThreatCategory.QUALITY,
            ComponentIdentifier.createMavenCoordinates("net.busybox", "busybox", "1.33"),
            hash(hashPrefix, "busybox"), waiver);
  }

  /**
   * Seeds a build-stage report with one open, one manually-waived, and one auto-waived violation for
   * {@code app}. The auto-waived row carries {@code autoPolicyWaiverId}, which
   * {@code DocumentBuilderHelper.deriveWaiverStatus} indexes as {@code AutoWaived} (vs {@code Waived}
   * for the manual waiver), so waiver-type filtering and facets have a distinct row of each kind.
   */
  private void seedWaiverMixViolations(
      final Organization org,
      final Application app,
      final String hashPrefix) throws Exception
  {
    PolicyEvaluation evaluation = ctx.tempEntity()
        .newPolicyEvaluation(app.getId(), Stage.ID_BUILD,
            "violations-waiver-" + hashPrefix);
    ReportTestUtils.createReportFile(evaluation.getOwnerId(), evaluation.getScanId(),
        ReportTestUtils.zipReportDir(VIOLATION_REPORT_RESOURCE, ctx.tempFolder()), ctx.lookup(InsightWork.class));

    Policy security = ctx.tempEntity().newPolicy(org.getId(), "Security - Critical " + hashPrefix);
    Policy manualPolicy = ctx.tempEntity().newPolicy(org.getId(), "Quality - Standards " + hashPrefix);
    Policy autoPolicy = ctx.tempEntity().newPolicy(org.getId(), "Legal - Non-Standard " + hashPrefix);

    ctx.tempEntity()
        .newPolicyViolation(evaluation, security, 10, PolicyThreatCategory.SECURITY,
            "org.apache.logging", "log4j-core", "2.14.0", hash(hashPrefix, "log4j"));

    PolicyWaiver manualWaiver = ctx.tempEntity().newWaiver(manualPolicy.getId(), org.getId());
    ctx.tempEntity()
        .newWaivedPolicyViolation(evaluation, manualPolicy, 3, PolicyThreatCategory.QUALITY,
            ComponentIdentifier.createMavenCoordinates("net.busybox", "busybox", "1.33"),
            hash(hashPrefix, "busybox"), manualWaiver);

    AutoPolicyWaiver autoWaiver = ctx.tempEntity().newAutoPolicyWaiver(org.getId());
    PolicyWaiver waiverForAuto = ctx.tempEntity().newWaiver(autoPolicy.getId(), org.getId());
    PolicyViolation autoWaived = ctx.tempEntity()
        .newWaivedPolicyViolation(evaluation, autoPolicy, 6,
            PolicyThreatCategory.LICENSE, ComponentIdentifier.createMavenCoordinates("org.openssl", "openssl", "3.0"),
            hash(hashPrefix, "openssl"), waiverForAuto);
    autoWaived.setAutoPolicyWaiverId(autoWaiver.getId());
    PolicyViolationDAO policyViolationDAO = ctx.lookup(PolicyViolationDAO.class);
    try (TransactionContext tx = policyViolationDAO.createTransactionContext()) {
      tx.begin();
      policyViolationDAO.update(tx, autoWaived);
      tx.commit();
    }
  }

  /**
   * Seeds a build-stage report with four violations for {@code app}: one plain open, one plain
   * manually-waived, one pure-legacy ({@code legacyViolationTime} set, no waiver), and one waived+legacy
   * ({@code waiveTime} + {@code legacyViolationTime} both set). The waived+legacy row indexes as
   * {@code Waived} by precedence, so it reads under WAIVED, not LEGACY.
   */
  private void seedLegacyMixViolations(
      final Organization org,
      final Application app,
      final String hashPrefix) throws Exception
  {
    PolicyEvaluation evaluation = ctx.tempEntity()
        .newPolicyEvaluation(app.getId(), Stage.ID_BUILD,
            "violations-legacy-" + hashPrefix);
    ReportTestUtils.createReportFile(evaluation.getOwnerId(), evaluation.getScanId(),
        ReportTestUtils.zipReportDir(VIOLATION_REPORT_RESOURCE, ctx.tempFolder()), ctx.lookup(InsightWork.class));

    Policy openPolicy = ctx.tempEntity().newPolicy(org.getId(), "Security - Critical " + hashPrefix);
    Policy waivedPolicy = ctx.tempEntity().newPolicy(org.getId(), "Quality - Standards " + hashPrefix);
    Policy legacyPolicy = ctx.tempEntity().newPolicy(org.getId(), "Legal - Copyleft " + hashPrefix);
    Policy waivedLegacyPolicy = ctx.tempEntity().newPolicy(org.getId(), "Legal - Non-Standard " + hashPrefix);

    ctx.tempEntity()
        .newPolicyViolation(evaluation, openPolicy, 10, PolicyThreatCategory.SECURITY,
            "org.apache.logging", "log4j-core", "2.14.0", hash(hashPrefix, "log4j"));

    PolicyWaiver waiver = ctx.tempEntity().newWaiver(waivedPolicy.getId(), org.getId());
    ctx.tempEntity()
        .newWaivedPolicyViolation(evaluation, waivedPolicy, 3, PolicyThreatCategory.QUALITY,
            ComponentIdentifier.createMavenCoordinates("net.busybox", "busybox", "1.33"),
            hash(hashPrefix, "busybox"), waiver);

    PolicyViolation legacy = ctx.tempEntity()
        .newPolicyViolation(evaluation, legacyPolicy, 8,
            PolicyThreatCategory.LICENSE, "com.lodash", "lodash", "4.17.15", hash(hashPrefix, "lodash"));
    legacy.setLegacyViolationTime(new Date());

    PolicyWaiver waiverForLegacy = ctx.tempEntity().newWaiver(waivedLegacyPolicy.getId(), org.getId());
    PolicyViolation waivedLegacy = ctx.tempEntity()
        .newWaivedPolicyViolation(evaluation, waivedLegacyPolicy, 6,
            PolicyThreatCategory.LICENSE, ComponentIdentifier.createMavenCoordinates("org.openssl", "openssl", "3.0"),
            hash(hashPrefix, "openssl"), waiverForLegacy);
    waivedLegacy.setLegacyViolationTime(new Date());

    PolicyViolationDAO policyViolationDAO = ctx.lookup(PolicyViolationDAO.class);
    try (TransactionContext tx = policyViolationDAO.createTransactionContext()) {
      tx.begin();
      policyViolationDAO.update(tx, legacy);
      policyViolationDAO.update(tx, waivedLegacy);
      tx.commit();
    }
  }

  /** Builds a deterministic component hash that fits the {@code policy_violation.hash} VARCHAR(20). */
  private static String hash(final String prefix, final String tag) {
    String base = prefix + "-" + tag;
    if (base.length() > 20) {
      return base.substring(0, 20);
    }
    return base;
  }
}
