/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.legal;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dashboard.DashboardResource;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kept in the original {@code com.sonatype.insight.brain.dashboard.legal} package because
 * {@link #listLegalFindings_noIndex_returns409} calls the package-private
 * {@link LegalListTestSupport#runWithoutSearchIndex}/{@link LegalListTestSupport#populateIndex}.
 * <p>
 * Integration coverage for {@link LegalListResource} — flag gating, RBAC, pagination, facets,
 * multi-word LTG filters, and validation (CLM-43207). Seeded from
 * {@code /IndexSearchingTest/policyViolationReport} licenses.json (7 LEGAL_VIOLATION docs).
 */
@IqH2Test
class IqH2LegalListResourceTest
{
  private static final String LEGAL_REPORT = "/IndexSearchingTest/policyViolationReport";

  /** Fixture with LTGs yields seven LEGAL_VIOLATION docs (metrics authz baseline). */
  private static final int EXPECTED_LEGAL_FINDINGS = 7;

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

  @Test
  void listLegalFindings_flagOn_returnsPaginatedIndexRowsAndFacets() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("LegalTribe");
    Application app = ctx.tempEntity().newApplication("Legal App", "legal-app", org.getId());
    seedLegalFindings(org, app, "std");
    LegalListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    LegalListResponseDTO body = post(scopedRequest(org)).getBody(LegalListResponseDTO.class);

    assertThat(body.source).isEqualTo(LegalListResponseDTO.SOURCE_INDEX);
    assertThat(body.total).isEqualTo(EXPECTED_LEGAL_FINDINGS);
    assertThat(body.findings).hasSize(EXPECTED_LEGAL_FINDINGS);
    assertThat(body.findings.get(0).legalFindingId).isNotBlank();
    assertThat(body.findings.get(0).legalFindingId.split("\\|")).hasSize(5);
    assertThat(body.findings.get(0).applicationId).isEqualTo(app.getId());
    assertThat(body.facets).isNotNull();
    assertThat(body.facets.totalFindings).isEqualTo(EXPECTED_LEGAL_FINDINGS);
    assertThat(body.facets.stages).isNotNull();
    assertThat(body.facets.organizations).containsKey(org.getId());
    assertThat(body.facets.applications).containsKey(app.getId());
    assertThat(body.facets.organizationNames).containsEntry(org.getId(), org.getName());
    assertThat(body.facets.applicationNames).containsEntry(app.getId(), app.getName());
  }

  @Test
  void listLegalFindings_flagOff_returns404() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);

    ctx.assertResponseStatus(404, post(new LegalListRequestDTO()));
  }

  @Test
  void listLegalFindings_noIndex_returns409() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);
    // Reproduces AbstractResourceTest.createUserWithPermissions(Permission.READ): a non-global role
    // mapped at the root organization.
    User user = ctx.tempEntity().newUser();
    Role readRole = ctx.tempEntity().newRole(false /* global */, Permission.READ);
    ctx.tempEntity()
        .newMembershipMapping(
            com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID, readRole.getId(), user.getUsername());

    LegalListTestSupport.runWithoutSearchIndex(
        ctx.lookup(InsightWork.class).getSearchIndexDir(),
        () -> {
          try {
            HttpResponse response = restRequest()
                .auth(user)
                .path(LegalListResource.LEGAL_LIST_PATH)
                .body(new LegalListRequestDTO())
                .post();
            ctx.assertResponseStatus(409, response);
          }
          catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Test
  void listLegalFindings_defaultSort_ordersByThreatDescendingWithinPage() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("LegalSortTribe");
    Application app = ctx.tempEntity().newApplication("Legal Sort App", "legal-sort-app", org.getId());
    seedLegalFindings(org, app, "sort");
    LegalListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    LegalListResponseDTO body = post(scopedRequest(org)).getBody(LegalListResponseDTO.class);

    List<Integer> threats = body.findings.stream().map(row -> row.threatLevel).toList();
    assertThat(threats).isSortedAccordingTo(Comparator.nullsLast(Comparator.reverseOrder()));
  }

  @Test
  void listLegalFindings_unsupportedOrderBy_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    LegalListRequestDTO request = new LegalListRequestDTO();
    request.orderBy = "-licenseName";

    ctx.assertResponseStatus(400, post(request));
  }

  @Test
  void listLegalFindings_searchTooLong_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    LegalListRequestDTO request = new LegalListRequestDTO();
    request.search = "x".repeat(LegalListService.MAX_SEARCH_LENGTH + 1);

    ctx.assertResponseStatus(400, post(request));
  }

  @Test
  void listLegalFindings_negativePage_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    LegalListRequestDTO request = new LegalListRequestDTO();
    request.page = -1;

    ctx.assertResponseStatus(400, post(request));
  }

  @Test
  void listLegalFindings_pageSizeAboveMax_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    LegalListRequestDTO request = new LegalListRequestDTO();
    request.pageSize = LegalListService.MAX_PAGE_SIZE + 1;

    ctx.assertResponseStatus(400, post(request));
  }

  @Test
  void listLegalFindings_secondPage_returnsDistinctFindings() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("LegalPageTribe");
    Application app = ctx.tempEntity().newApplication("Legal Page App", "legal-page-app", org.getId());
    seedLegalFindings(org, app, "page");
    LegalListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    LegalListRequestDTO request = scopedRequest(org);
    request.pageSize = 3;

    request.page = 0;
    LegalListResponseDTO page0 = post(request).getBody(LegalListResponseDTO.class);
    request.page = 1;
    LegalListResponseDTO page1 = post(request).getBody(LegalListResponseDTO.class);

    assertThat(page0.total).isEqualTo(EXPECTED_LEGAL_FINDINGS);
    assertThat(page0.findings).hasSize(3);
    assertThat(page0.hasNextPage).isTrue();
    assertThat(page1.findings).isNotEmpty();
    assertThat(page1.hasNextPage).isEqualTo(page1.page * page1.pageSize + page1.findings.size() < page1.total);

    List<String> page0Ids = page0.findings.stream().map(row -> row.legalFindingId).toList();
    List<String> page1Ids = page1.findings.stream().map(row -> row.legalFindingId).toList();
    assertThat(page0Ids).doesNotContainAnyElementsOf(page1Ids);
  }

  @Test
  void listLegalFindings_includeFacetsFalse_omitsFacets() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("LegalNoFacetTribe");
    Application app = ctx.tempEntity().newApplication("Legal No Facet App", "legal-no-facet-app", org.getId());
    seedLegalFindings(org, app, "nofacet");
    LegalListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    LegalListRequestDTO request = scopedRequest(org);
    request.includeFacets = false;

    assertThat(post(request).getBody(LegalListResponseDTO.class).facets).isNull();
  }

  @Test
  void listLegalFindings_threatLevelRangeFilter_limitsRows() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("LegalThreatTribe");
    Application app = ctx.tempEntity().newApplication("Legal Threat App", "legal-threat-app", org.getId());
    seedLegalFindings(org, app, "threat");
    LegalListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    LegalListRequestDTO request = scopedRequest(org);
    request.licenseThreatLevelRange = new PolicyThreatLevelFilter(7, 10);
    LegalListResponseDTO body = post(request).getBody(LegalListResponseDTO.class);

    assertThat(body.findings).isNotEmpty();
    assertThat(body.findings).allMatch(row -> row.threatLevel == null || row.threatLevel >= 7);
  }

  @Test
  void listLegalFindings_multiWordLtgFilter_matchesExactPhrase() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("LegalLtgTribe");
    Application app = ctx.tempEntity().newApplication("Legal LTG App", "legal-ltg-app", org.getId());
    seedLegalFindings(org, app, "ltg");
    // Unique multi-word LTG name (Root already owns "Copyleft"/"Permissive"); maps GPL so those
    // docs surface under this phrase and prove quoting does not OR-split on spaces.
    String multiWordLtg = "Weak Copyleft Ltg";
    ctx.tempEntity().newLicenseThreatGroup(org.getId(), multiWordLtg, 7, "GPL-3.0", "GPL-2.0");
    LegalListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    LegalListRequestDTO weak = scopedRequest(org);
    weak.licenseThreatGroupNames = Set.of(multiWordLtg);
    LegalListResponseDTO weakBody = post(weak).getBody(LegalListResponseDTO.class);
    assertThat(weakBody.findings).isNotEmpty();
    assertThat(weakBody.findings)
        .extracting(row -> row.licenseThreatGroupName)
        .containsOnly(multiWordLtg);

    // Unquoted OR-split would match "Copyleft" as a free token; exact phrase must not.
    LegalListRequestDTO copyleftToken = scopedRequest(org);
    copyleftToken.licenseThreatGroupNames = Set.of("Copyleft");
    LegalListResponseDTO copyleftBody = post(copyleftToken).getBody(LegalListResponseDTO.class);
    assertThat(copyleftBody.findings)
        .extracting(row -> row.licenseThreatGroupName)
        .doesNotContain(multiWordLtg);
  }

  @Test
  void listLegalFindings_stageFilter_limitsRows() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("LegalStageTribe");
    Application app = ctx.tempEntity().newApplication("Legal Stage App", "legal-stage-app", org.getId());
    seedLegalFindings(org, app, "stage");
    LegalListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    LegalListRequestDTO buildRequest = scopedRequest(org);
    buildRequest.stageIds = Set.of(Stage.ID_BUILD);
    assertThat(post(buildRequest).getBody(LegalListResponseDTO.class).findings)
        .hasSize(EXPECTED_LEGAL_FINDINGS);

    LegalListRequestDTO releaseRequest = scopedRequest(org);
    releaseRequest.stageIds = Set.of(Stage.ID_RELEASE);
    assertThat(post(releaseRequest).getBody(LegalListResponseDTO.class).findings).isEmpty();
  }

  @Test
  void listLegalFindings_scopedUser_seesOnlyReadableFindings() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("LegalRbacTribe");
    Application readable = ctx.tempEntity().newApplication("Readable Legal App", "readable-legal-app", org.getId());
    Application hidden = ctx.tempEntity().newApplication("Hidden Legal App", "hidden-legal-app", org.getId());
    seedLegalFindings(org, readable, "readable");
    seedLegalFindings(org, hidden, "hidden");

    User reader = ctx.tempEntity().newUser("scoped-legal-reader");
    Role readRole = ctx.tempEntity().newRole(false /* global */, Permission.READ);
    ctx.tempEntity().newMembershipMapping(readable.getId(), readRole.getId(), reader.getUsername());
    LegalListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    HttpResponse response = restRequest()
        .auth(reader)
        .path(LegalListResource.LEGAL_LIST_PATH)
        .body(new LegalListRequestDTO())
        .post();

    ctx.assertResponseStatus(200, response);
    LegalListResponseDTO body = response.getBody(LegalListResponseDTO.class);
    assertThat(body.total).isEqualTo(EXPECTED_LEGAL_FINDINGS);
    assertThat(body.findings).extracting(row -> row.applicationId).containsOnly(readable.getId());
  }

  @Test
  void listLegalFindings_unauthenticated_returns401() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    HttpResponse response = restRequest()
        .path(LegalListResource.LEGAL_LIST_PATH)
        .body(new LegalListRequestDTO())
        .anon()
        .post();

    ctx.assertResponseStatus(401, response);
  }

  @Test
  void listLegalFindings_authenticatedWithNoGrants_seesNoFindings() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("LegalNoGrantsTribe");
    Application app = ctx.tempEntity().newApplication("Legal No Grants App", "legal-no-grants-app", org.getId());
    seedLegalFindings(org, app, "nogrants");
    User strangerWithNoGrants = ctx.tempEntity().newUser("legal-no-grants");
    LegalListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    HttpResponse response = restRequest()
        .auth(strangerWithNoGrants)
        .path(LegalListResource.LEGAL_LIST_PATH)
        .body(new LegalListRequestDTO())
        .post();

    ctx.assertResponseStatus(200, response);
    LegalListResponseDTO body = response.getBody(LegalListResponseDTO.class);
    assertThat(body.total).isEqualTo(0);
    assertThat(body.findings).isEmpty();
  }

  private HttpResponse post(final LegalListRequestDTO request) throws Exception {
    return restRequest()
        .path(LegalListResource.LEGAL_LIST_PATH)
        .body(request)
        .post();
  }

  private static LegalListRequestDTO scopedRequest(final Organization org) {
    LegalListRequestDTO request = new LegalListRequestDTO();
    request.organizationIds = Set.of(org.getId());
    return request;
  }

  /**
   * Seeds a build-stage report whose {@code licenses.json} produces LEGAL_VIOLATION index docs.
   * Threat group labels come from Root's default LTGs (same approach as Dashboard metrics tests) —
   * do not recreate {@code Copyleft}/{@code Permissive} on the child org (name collision).
   */
  private void seedLegalFindings(
      final Organization org,
      final Application app,
      final String scanSuffix) throws Exception
  {
    PolicyEvaluation evaluation =
        ctx.tempEntity().newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "legal-scan-" + scanSuffix);
    ReportTestUtils.createReportFile(
        evaluation.getOwnerId(),
        evaluation.getScanId(),
        ReportTestUtils.zipReportDir(LEGAL_REPORT, ctx.tempFolder()),
        ctx.lookup(InsightWork.class));
  }
}
