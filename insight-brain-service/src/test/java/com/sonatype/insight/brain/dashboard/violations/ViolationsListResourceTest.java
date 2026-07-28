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
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ViolationsListResourceTest
    extends AbstractResourceTest
{
  private static final String VIOLATION_REPORT_RESOURCE = "/IndexSearchingTest/policyViolationReport";

  @After
  public void tearDownPreviewFlag() {
    tempEntity.deleteSystemConfigurationProperty(
        SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.getPropertyName());
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(DashboardResource.RESOURCE_PATH);
  }

  // --- Feature flag gating ---------------------------------------------------------------------

  @Test
  public void listViolations_flagOn_returnsPaginatedIndexRows() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("ViolationsTribe");
    Application app = tempEntity.newApplication("Vln App", "vln-app", org.getId());
    seedStandardViolations(org, app, "std");
    ViolationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

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
  public void listViolations_sessionReadPath_returnsRowsAndFacetsFromSeededIndex() throws Exception {
    System.setProperty("nexusOne.search.readPath.violations", "new");
    try {
      SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

      Organization org = tempEntity.newOrganization("SessionPathTribe");
      Application app = tempEntity.newApplication("Session Path App", "session-path-app", org.getId());
      seedStandardViolations(org, app, "sess");
      ViolationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

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
  public void listViolations_flagOff_returns404() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);

    HttpResponse response = post(new ViolationsListRequestDTO());

    assertResponseStatus(404, response);
  }

  @Test
  public void listViolations_noIndex_returns409() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);
    User user = createUserWithPermissions(Permission.READ);

    ViolationsListTestSupport.runWithoutSearchIndex(
        lookup(InsightWork.class).getSearchIndexDir(),
        () -> {
          try {
            HttpResponse response = restRequest()
                .auth(user)
                .path(ViolationsListResource.VIOLATIONS_LIST_PATH)
                .body(new ViolationsListRequestDTO())
                .post();
            assertResponseStatus(409, response);
          }
          catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  // --- Sort ------------------------------------------------------------------------------------

  @Test
  public void listViolations_defaultSort_ordersByThreatDescending() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("SortTribe");
    Application app = tempEntity.newApplication("Sort App", "sort-app", org.getId());
    seedStandardViolations(org, app, "sort");
    ViolationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    ViolationsListResponseDTO body = post(scopedRequest(org)).getBody(ViolationsListResponseDTO.class);

    assertThat(body.violations).extracting(row -> row.threatLevel).containsExactly(10, 8, 3);
  }

  @Test
  public void listViolations_orderByThreatAscending_ordersByThreatAscending() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("SortAscTribe");
    Application app = tempEntity.newApplication("Sort Asc App", "sort-asc-app", org.getId());
    seedStandardViolations(org, app, "sortasc");
    ViolationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    ViolationsListRequestDTO request = scopedRequest(org);
    request.orderBy = "policyThreatLevel";
    ViolationsListResponseDTO body = post(request).getBody(ViolationsListResponseDTO.class);

    assertThat(body.violations).extracting(row -> row.threatLevel).containsExactly(3, 8, 10);
  }

  @Test
  public void listViolations_unsupportedOrderBy_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.orderBy = "-policyName";

    assertResponseStatus(400, post(request));
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
  public void listViolations_sortIsPerPage_globalThreatOrderNotGuaranteedAcrossPages() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("CrossPageSortTribe");
    Application app = tempEntity.newApplication("Cross Page App", "cross-page-app", org.getId());
    seedStandardViolations(org, app, "xpage");
    ViolationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

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
  public void listViolations_searchFiltersByApplicationPublicId() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("SearchTribe");
    Application apple = tempEntity.newApplication("Apple Svc", "apple-svc", org.getId());
    Application banana = tempEntity.newApplication("Banana Svc", "banana-svc", org.getId());
    seedStandardViolations(org, apple, "apple");
    seedStandardViolations(org, banana, "banana");
    ViolationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    ViolationsListRequestDTO request = scopedRequest(org);
    request.search = "banana-svc";
    ViolationsListResponseDTO body = post(request).getBody(ViolationsListResponseDTO.class);

    assertThat(body.violations).isNotEmpty();
    assertThat(body.violations).extracting(row -> row.applicationId).containsOnly(banana.getId());
  }

  @Test
  public void listViolations_searchTooLong_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.search = "x".repeat(ViolationsListService.MAX_SEARCH_LENGTH + 1);

    assertResponseStatus(400, post(request));
  }

  // --- Pagination validation -------------------------------------------------------------------

  @Test
  public void listViolations_negativePage_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.page = -1;

    assertResponseStatus(400, post(request));
  }

  @Test
  public void listViolations_pageSizeAboveMax_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.pageSize = ViolationsListService.MAX_PAGE_SIZE + 1;

    assertResponseStatus(400, post(request));
  }

  @Test
  public void listViolations_zeroPageSize_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.pageSize = 0;

    assertResponseStatus(400, post(request));
  }

  @Test
  public void listViolations_secondPage_returnsDistinctViolations() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("PageTribe");
    Application app = tempEntity.newApplication("Page App", "page-app", org.getId());
    seedStandardViolations(org, app, "page");
    ViolationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

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
  public void listViolations_includeFacetsFalse_omitsFacets() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("NoFacetTribe");
    Application app = tempEntity.newApplication("No Facet App", "no-facet-app", org.getId());
    seedStandardViolations(org, app, "nofacet");
    ViolationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    ViolationsListRequestDTO request = scopedRequest(org);
    request.includeFacets = false;

    assertThat(post(request).getBody(ViolationsListResponseDTO.class).facets).isNull();
  }

  @Test
  public void listViolations_facetStateCounts_splitOpenAndWaived() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("StateFacetTribe");
    Application app = tempEntity.newApplication("State Facet App", "state-facet-app", org.getId());
    seedStandardViolations(org, app, "statefacet");
    ViolationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    ViolationsListFacetsDTO facets = post(scopedRequest(org)).getBody(ViolationsListResponseDTO.class).facets;

    assertThat(facets.states).containsEntry(PolicyViolationState.OPEN.name(), 2L);
    assertThat(facets.states).containsEntry(PolicyViolationState.WAIVED.name(), 1L);
  }

  // --- Filters ---------------------------------------------------------------------------------

  @Test
  public void listViolations_threatLevelRangeFilter_limitsRows() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("ThreatTribe");
    Application app = tempEntity.newApplication("Threat App", "threat-app", org.getId());
    seedStandardViolations(org, app, "threat");
    ViolationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    ViolationsListRequestDTO request = scopedRequest(org);
    request.policyThreatLevelRange = new PolicyThreatLevelFilter(7, 10);
    ViolationsListResponseDTO body = post(request).getBody(ViolationsListResponseDTO.class);

    assertThat(body.violations).extracting(row -> row.threatLevel).containsExactly(10, 8);
  }

  @Test
  public void listViolations_openStateFilter_excludesWaived() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("StateTribe");
    Application app = tempEntity.newApplication("State App", "state-app", org.getId());
    seedStandardViolations(org, app, "state");
    ViolationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    ViolationsListRequestDTO request = scopedRequest(org);
    request.policyViolationStates = new PolicyViolationStateFilter(PolicyViolationState.OPEN);
    ViolationsListResponseDTO body = post(request).getBody(ViolationsListResponseDTO.class);

    assertThat(body.violations).hasSize(2);
    assertThat(body.violations).extracting(row -> row.state).containsOnly(PolicyViolationState.OPEN.name());
  }

  @Test
  public void listViolations_threatCategoryFilter_limitsRows() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("CategoryTribe");
    Application app = tempEntity.newApplication("Category App", "category-app", org.getId());
    seedStandardViolations(org, app, "category");
    ViolationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    ViolationsListRequestDTO request = scopedRequest(org);
    request.policyThreatCategories = new PolicyThreatCategoryFilter(PolicyThreatCategory.SECURITY);
    ViolationsListResponseDTO body = post(request).getBody(ViolationsListResponseDTO.class);

    assertThat(body.violations).hasSize(1);
    assertThat(body.violations.get(0).threatCategory).isEqualTo(PolicyThreatCategory.SECURITY.getName());
  }

  @Test
  public void listViolations_stageFilter_limitsRows() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("StageTribe");
    Application app = tempEntity.newApplication("Stage App", "stage-app", org.getId());
    seedStandardViolations(org, app, "stage");
    ViolationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    ViolationsListRequestDTO buildRequest = scopedRequest(org);
    buildRequest.stageIds = Set.of(Stage.ID_BUILD);
    assertThat(post(buildRequest).getBody(ViolationsListResponseDTO.class).violations).hasSize(3);

    ViolationsListRequestDTO releaseRequest = scopedRequest(org);
    releaseRequest.stageIds = Set.of(Stage.ID_RELEASE);
    assertThat(post(releaseRequest).getBody(ViolationsListResponseDTO.class).violations).isEmpty();
  }

  @Test
  public void listViolations_unsupportedApplicationCategoryFilter_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.applicationCategoryIds = Set.of("some-category-id");

    assertResponseStatus(400, post(request));
  }

  @Test
  public void listViolations_unsupportedAgeFilter_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.ageInDays = 30;

    assertResponseStatus(400, post(request));
  }

  @Test
  public void listViolations_autoWaiverFilter_selectsAutoWaivedOnly() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("AutoWaiverTribe");
    Application app = tempEntity.newApplication("Auto Waiver App", "auto-waiver-app", org.getId());
    seedWaiverMixViolations(org, app, "autowvr");
    ViolationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    ViolationsListRequestDTO request = scopedRequest(org);
    request.waivedWithAutoWaiver = true;
    ViolationsListResponseDTO body = post(request).getBody(ViolationsListResponseDTO.class);

    assertThat(body.violations).hasSize(1);
    assertThat(body.violations.get(0).waivedWithAutoWaiver).isTrue();
    assertThat(body.violations.get(0).state).isEqualTo(PolicyViolationState.WAIVED.name());
  }

  @Test
  public void listViolations_manualWaiverFilter_selectsManuallyWaivedOnly() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("ManualWaiverTribe");
    Application app = tempEntity.newApplication("Manual Waiver App", "manual-waiver-app", org.getId());
    seedWaiverMixViolations(org, app, "manwvr");
    ViolationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    ViolationsListRequestDTO request = scopedRequest(org);
    request.waivedWithAutoWaiver = false;
    ViolationsListResponseDTO body = post(request).getBody(ViolationsListResponseDTO.class);

    assertThat(body.violations).hasSize(1);
    assertThat(body.violations.get(0).waivedWithAutoWaiver).isFalse();
    assertThat(body.violations.get(0).state).isEqualTo(PolicyViolationState.WAIVED.name());
  }

  @Test
  public void listViolations_autoWaiverWithWaivedState_returnsAutoWaivedRow() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("AutoWaiverStateTribe");
    Application app = tempEntity.newApplication("Auto Waiver State App", "auto-waiver-state-app", org.getId());
    seedWaiverMixViolations(org, app, "awstate");
    ViolationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    ViolationsListRequestDTO request = scopedRequest(org);
    request.policyViolationStates = new PolicyViolationStateFilter(PolicyViolationState.WAIVED);
    request.waivedWithAutoWaiver = true;

    ViolationsListResponseDTO body = post(request).getBody(ViolationsListResponseDTO.class);
    assertThat(body.violations).hasSize(1);
    assertThat(body.violations.get(0).waivedWithAutoWaiver).isTrue();
  }

  @Test
  public void listViolations_autoWaiverWithOpenState_returnsEmpty() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("AutoWaiverOpenTribe");
    Application app = tempEntity.newApplication("Auto Waiver Open App", "auto-waiver-open-app", org.getId());
    seedWaiverMixViolations(org, app, "awopen");
    ViolationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    ViolationsListRequestDTO request = scopedRequest(org);
    request.policyViolationStates = new PolicyViolationStateFilter(PolicyViolationState.OPEN);
    request.waivedWithAutoWaiver = true;

    // OPEN and auto-waived is a contradictory combination — the index correctly returns no rows.
    assertThat(post(request).getBody(ViolationsListResponseDTO.class).violations).isEmpty();
  }

  @Test
  public void listViolations_facetWaiverTypeCounts_splitAutoAndManual() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("WaiverFacetTribe");
    Application app = tempEntity.newApplication("Waiver Facet App", "waiver-facet-app", org.getId());
    seedWaiverMixViolations(org, app, "wvrfacet");
    ViolationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    ViolationsListFacetsDTO facets = post(scopedRequest(org)).getBody(ViolationsListResponseDTO.class).facets;

    assertThat(facets.waiverTypes)
        .containsEntry(ViolationsListFacetsBuilder.WAIVER_TYPE_AUTO, 1L)
        .containsEntry(ViolationsListFacetsBuilder.WAIVER_TYPE_MANUAL, 1L);
  }

  @Test
  public void listViolations_facetWaiverTypeCounts_showBothOptionsWhenOneSelected() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("WaiverSwitchTribe");
    Application app = tempEntity.newApplication("Waiver Switch App", "waiver-switch-app", org.getId());
    seedWaiverMixViolations(org, app, "wvrswitch");
    ViolationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

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
  public void listViolations_legacyStateFilter_returnsPureLegacyOnly() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("LegacyStateTribe");
    Application app = tempEntity.newApplication("Legacy State App", "legacy-state-app", org.getId());
    seedLegacyMixViolations(org, app, "legstate");
    ViolationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    ViolationsListRequestDTO request = scopedRequest(org);
    request.policyViolationStates = new PolicyViolationStateFilter(PolicyViolationState.LEGACY_VIOLATION);
    ViolationsListResponseDTO body = post(request).getBody(ViolationsListResponseDTO.class);

    // Only the pure-legacy violation reads as LEGACY_VIOLATION. The waived+legacy violation indexes as
    // Waived by precedence and is NOT returned here (documented divergence from the SQL path).
    assertThat(body.violations).hasSize(1);
    assertThat(body.violations.get(0).state).isEqualTo(PolicyViolationState.LEGACY_VIOLATION.name());
  }

  @Test
  public void listViolations_openStateFilter_excludesLegacy() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("LegacyOpenTribe");
    Application app = tempEntity.newApplication("Legacy Open App", "legacy-open-app", org.getId());
    seedLegacyMixViolations(org, app, "legopen");
    ViolationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    ViolationsListRequestDTO request = scopedRequest(org);
    request.policyViolationStates = new PolicyViolationStateFilter(PolicyViolationState.OPEN);
    ViolationsListResponseDTO body = post(request).getBody(ViolationsListResponseDTO.class);

    // OPEN excludes Legacy (and Waived/AutoWaived): only the plain open violation is returned.
    assertThat(body.violations).hasSize(1);
    assertThat(body.violations.get(0).state).isEqualTo(PolicyViolationState.OPEN.name());
  }

  @Test
  public void listViolations_facetStateCounts_splitOpenWaivedLegacy() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("LegacyFacetTribe");
    Application app = tempEntity.newApplication("Legacy Facet App", "legacy-facet-app", org.getId());
    seedLegacyMixViolations(org, app, "legfacet");
    ViolationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

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
  public void listViolations_openAndWaivedStateFilter_returnsOpenAndWaivedRows() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("LegacyOpenWaivedTribe");
    Application app = tempEntity.newApplication("Legacy Open Waived App", "legacy-openwaived-app", org.getId());
    seedLegacyMixViolations(org, app, "legopw");
    ViolationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

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
  public void listViolations_openAndLegacyStateFilter_returnsOpenAndPureLegacyRows() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("LegacyOpenLegacyTribe");
    Application app = tempEntity.newApplication("Legacy Open Legacy App", "legacy-openlegacy-app", org.getId());
    seedLegacyMixViolations(org, app, "legopl");
    ViolationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

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
  public void listViolations_waivedAndLegacyStateFilter_returnsWaivedAndPureLegacyRows() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("LegacyWaivedLegacyTribe");
    Application app = tempEntity.newApplication("Legacy Waived Legacy App", "legacy-wvdlegacy-app", org.getId());
    seedLegacyMixViolations(org, app, "legwl");
    ViolationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

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
  public void listViolations_allThreeStateFilter_returnsAllRows() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("LegacyAllStatesTribe");
    Application app = tempEntity.newApplication("Legacy All States App", "legacy-allstates-app", org.getId());
    seedLegacyMixViolations(org, app, "legall");
    ViolationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

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
  public void listViolations_scopedUser_seesOnlyReadableViolations() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("RbacTribe");
    Application readable = tempEntity.newApplication("Readable App", "readable-vln-app", org.getId());
    Application hidden = tempEntity.newApplication("Hidden App", "hidden-vln-app", org.getId());
    seedStandardViolations(org, readable, "readable");
    seedStandardViolations(org, hidden, "hidden");

    User reader = tempEntity.newUser("scoped-violations-reader");
    Role readRole = tempEntity.newRole(false /* global */, Permission.READ);
    tempEntity.newMembershipMapping(readable.getId(), readRole.getId(), reader.getUsername());
    ViolationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    HttpResponse response = restRequest()
        .auth(reader)
        .path(ViolationsListResource.VIOLATIONS_LIST_PATH)
        .body(new ViolationsListRequestDTO())
        .post();

    assertResponseStatus(200, response);
    ViolationsListResponseDTO body = response.getBody(ViolationsListResponseDTO.class);
    assertThat(body.total).isEqualTo(3);
    assertThat(body.violations).extracting(row -> row.applicationId).containsOnly(readable.getId());
  }

  @Test
  public void listViolations_unauthenticated_returns401() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    HttpResponse response = restRequest()
        .path(ViolationsListResource.VIOLATIONS_LIST_PATH)
        .body(new ViolationsListRequestDTO())
        .anon()
        .post();

    assertResponseStatus(401, response);
  }

  @Test
  public void listViolations_authenticatedWithNoGrants_seesNoViolations() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("NoGrantsTribe");
    Application app = tempEntity.newApplication("No Grants App", "no-grants-app", org.getId());
    seedStandardViolations(org, app, "nogrants");
    // Authenticated user with no role/membership mapping: RBAC scoping must yield nothing rather than
    // letting a broken permission gate expose violations (CLM-42254 review — guards the fail-open case).
    User strangerWithNoGrants = tempEntity.newUser("violations-no-grants");
    ViolationsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    HttpResponse response = restRequest()
        .auth(strangerWithNoGrants)
        .path(ViolationsListResource.VIOLATIONS_LIST_PATH)
        .body(new ViolationsListRequestDTO())
        .post();

    assertResponseStatus(200, response);
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
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD,
        "violations-scan-" + hashPrefix);
    ReportTestUtils.createReportFile(evaluation.getApplicationId(), evaluation.getScanId(),
        ReportTestUtils.zipReportDir(VIOLATION_REPORT_RESOURCE, tempDir), lookup(InsightWork.class));

    Policy security = tempEntity.newPolicy(org.getId(), "Security - Critical " + hashPrefix);
    Policy license = tempEntity.newPolicy(org.getId(), "Legal - Copyleft " + hashPrefix);
    Policy quality = tempEntity.newPolicy(org.getId(), "Quality - Standards " + hashPrefix);

    tempEntity.newPolicyViolation(evaluation, security, 10, PolicyThreatCategory.SECURITY,
        "org.apache.logging", "log4j-core", "2.14.0", hash(hashPrefix, "log4j"));
    tempEntity.newPolicyViolation(evaluation, license, 8, PolicyThreatCategory.LICENSE,
        "com.lodash", "lodash", "4.17.15", hash(hashPrefix, "lodash"));

    PolicyWaiver waiver = tempEntity.newWaiver(quality.getId(), org.getId());
    tempEntity.newWaivedPolicyViolation(evaluation, quality, 3, PolicyThreatCategory.QUALITY,
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
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD,
        "violations-waiver-" + hashPrefix);
    ReportTestUtils.createReportFile(evaluation.getApplicationId(), evaluation.getScanId(),
        ReportTestUtils.zipReportDir(VIOLATION_REPORT_RESOURCE, tempDir), lookup(InsightWork.class));

    Policy security = tempEntity.newPolicy(org.getId(), "Security - Critical " + hashPrefix);
    Policy manualPolicy = tempEntity.newPolicy(org.getId(), "Quality - Standards " + hashPrefix);
    Policy autoPolicy = tempEntity.newPolicy(org.getId(), "Legal - Non-Standard " + hashPrefix);

    tempEntity.newPolicyViolation(evaluation, security, 10, PolicyThreatCategory.SECURITY,
        "org.apache.logging", "log4j-core", "2.14.0", hash(hashPrefix, "log4j"));

    PolicyWaiver manualWaiver = tempEntity.newWaiver(manualPolicy.getId(), org.getId());
    tempEntity.newWaivedPolicyViolation(evaluation, manualPolicy, 3, PolicyThreatCategory.QUALITY,
        ComponentIdentifier.createMavenCoordinates("net.busybox", "busybox", "1.33"),
        hash(hashPrefix, "busybox"), manualWaiver);

    AutoPolicyWaiver autoWaiver = tempEntity.newAutoPolicyWaiver(org.getId());
    PolicyWaiver waiverForAuto = tempEntity.newWaiver(autoPolicy.getId(), org.getId());
    PolicyViolation autoWaived = tempEntity.newWaivedPolicyViolation(evaluation, autoPolicy, 6,
        PolicyThreatCategory.LICENSE, ComponentIdentifier.createMavenCoordinates("org.openssl", "openssl", "3.0"),
        hash(hashPrefix, "openssl"), waiverForAuto);
    autoWaived.setAutoPolicyWaiverId(autoWaiver.getId());
    PolicyViolationDAO policyViolationDAO = lookup(PolicyViolationDAO.class);
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
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD,
        "violations-legacy-" + hashPrefix);
    ReportTestUtils.createReportFile(evaluation.getApplicationId(), evaluation.getScanId(),
        ReportTestUtils.zipReportDir(VIOLATION_REPORT_RESOURCE, tempDir), lookup(InsightWork.class));

    Policy openPolicy = tempEntity.newPolicy(org.getId(), "Security - Critical " + hashPrefix);
    Policy waivedPolicy = tempEntity.newPolicy(org.getId(), "Quality - Standards " + hashPrefix);
    Policy legacyPolicy = tempEntity.newPolicy(org.getId(), "Legal - Copyleft " + hashPrefix);
    Policy waivedLegacyPolicy = tempEntity.newPolicy(org.getId(), "Legal - Non-Standard " + hashPrefix);

    tempEntity.newPolicyViolation(evaluation, openPolicy, 10, PolicyThreatCategory.SECURITY,
        "org.apache.logging", "log4j-core", "2.14.0", hash(hashPrefix, "log4j"));

    PolicyWaiver waiver = tempEntity.newWaiver(waivedPolicy.getId(), org.getId());
    tempEntity.newWaivedPolicyViolation(evaluation, waivedPolicy, 3, PolicyThreatCategory.QUALITY,
        ComponentIdentifier.createMavenCoordinates("net.busybox", "busybox", "1.33"),
        hash(hashPrefix, "busybox"), waiver);

    PolicyViolation legacy = tempEntity.newPolicyViolation(evaluation, legacyPolicy, 8,
        PolicyThreatCategory.LICENSE, "com.lodash", "lodash", "4.17.15", hash(hashPrefix, "lodash"));
    legacy.setLegacyViolationTime(new Date());

    PolicyWaiver waiverForLegacy = tempEntity.newWaiver(waivedLegacyPolicy.getId(), org.getId());
    PolicyViolation waivedLegacy = tempEntity.newWaivedPolicyViolation(evaluation, waivedLegacyPolicy, 6,
        PolicyThreatCategory.LICENSE, ComponentIdentifier.createMavenCoordinates("org.openssl", "openssl", "3.0"),
        hash(hashPrefix, "openssl"), waiverForLegacy);
    waivedLegacy.setLegacyViolationTime(new Date());

    PolicyViolationDAO policyViolationDAO = lookup(PolicyViolationDAO.class);
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
