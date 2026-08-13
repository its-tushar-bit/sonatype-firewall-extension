/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.applications;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dashboard.DashboardResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real-index old-vs-new read-path parity for Applications list (CLM-42228). Kept in the original
 * {@code com.sonatype.insight.brain.dashboard.applications} package because it uses the package-private
 * {@code ApplicationsListTestSupport} helper.
 * <p>
 * Page membership order may differ (legacy relevance/docId vs session {@code documentKey}), so
 * assertions compare totals, facet count maps, and the union of application ids across pages.
 */
@IqH2Test
class IqH2ApplicationsListReadPathParityResourceTest
{
  private IqTestContext ctx;

  private static final String READ_PATH_PROPERTY = "nexusOne.search.readPath.applications";

  @AfterEach
  void tearDown() {
    System.clearProperty(READ_PATH_PROPERTY);
    ctx.tempEntity()
        .deleteSystemConfigurationProperty(
            SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.getPropertyName());
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(DashboardResource.RESOURCE_PATH);
  }

  @Test
  void listApplications_oldVsNew_realIndex_equalTotalFacetsAndPagedMembership() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("ParityTribe");
    Application appA = ctx.tempEntity().newApplication("Parity App A", "parity-app-a", org.getId());
    Application appB = ctx.tempEntity().newApplication("Parity App B", "parity-app-b", org.getId());
    Application appC = ctx.tempEntity().newApplication("Parity App C", "parity-app-c", org.getId());
    Policy policyA = ctx.tempEntity().newPolicy(appA);
    Policy policyB = ctx.tempEntity().newPolicy(appB);
    PolicyEvaluation evalA = ctx.tempEntity().newPolicyEvaluation(appA.getId(), Stage.ID_BUILD, "parity-scan-a");
    PolicyEvaluation evalB = ctx.tempEntity().newPolicyEvaluation(appB.getId(), Stage.ID_BUILD, "parity-scan-b");
    InsightWork insightWork = ctx.lookup(InsightWork.class);
    ReportTestUtils.createReportFile(evalA.getOwnerId(), evalA.getScanId(),
        ReportTestUtils.zipReportDir("/IndexSearchingTest/policyViolationReport", ctx.tempFolder()), insightWork);
    ReportTestUtils.createReportFile(evalB.getOwnerId(), evalB.getScanId(),
        ReportTestUtils.zipReportDir("/IndexSearchingTest/policyViolationReport", ctx.tempFolder()), insightWork);
    ctx.tempEntity().newPolicyViolation(evalA, policyA);
    ctx.tempEntity().newPolicyViolation(evalB, policyB);
    // appC has no violations — still listed; stage facets come from A/B only.
    ApplicationsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.organizationIds = Set.of(org.getId());
    request.pageSize = 2;
    request.includeFacets = true;

    System.setProperty(READ_PATH_PROPERTY, "old");
    ParitySnapshot oldSnapshot = collectAllPages(request);
    System.setProperty(READ_PATH_PROPERTY, "new");
    ParitySnapshot newSnapshot = collectAllPages(request);

    assertThat(newSnapshot.total).isEqualTo(oldSnapshot.total).isEqualTo(3);
    assertThat(newSnapshot.applicationIds)
        .containsExactlyInAnyOrderElementsOf(oldSnapshot.applicationIds)
        .containsExactlyInAnyOrder(
            appA.getPublicId(),
            appB.getPublicId(),
            appC.getPublicId());
    assertThat(newSnapshot.pageCount()).isEqualTo(oldSnapshot.pageCount()).isEqualTo(2);
    assertThat(newSnapshot.pageSizes).isEqualTo(oldSnapshot.pageSizes);

    assertThat(newSnapshot.facets).isNotNull();
    assertThat(oldSnapshot.facets).isNotNull();
    assertThat(newSnapshot.facets.totalApplications).isEqualTo(oldSnapshot.facets.totalApplications);
    assertThat(newSnapshot.facets.organizations).isEqualTo(oldSnapshot.facets.organizations);
    assertThat(newSnapshot.facets.applications).isEqualTo(oldSnapshot.facets.applications);
    assertThat(newSnapshot.facets.stages).isEqualTo(oldSnapshot.facets.stages);
    assertThat(newSnapshot.facets.stages).containsEntry(Stage.ID_BUILD, 2L);
    assertThat(newSnapshot.facets.organizationNames)
        .isNotNull()
        .containsEntry(org.getId(), org.getName());
    assertThat(oldSnapshot.facets.organizationNames)
        .isNotNull()
        .containsEntry(org.getId(), org.getName());
    assertThat(newSnapshot.facets.applicationNames)
        .isNotNull()
        .containsEntry(appA.getId(), appA.getName())
        .containsEntry(appB.getId(), appB.getName())
        .containsEntry(appC.getId(), appC.getName());
    assertThat(oldSnapshot.facets.applicationNames)
        .isNotNull()
        .containsEntry(appA.getId(), appA.getName());

    // Org rewrite + documentKey sort are exercised when search filters by org name token.
    ApplicationsListRequestDTO searchRequest = new ApplicationsListRequestDTO();
    searchRequest.organizationIds = Set.of(org.getId());
    searchRequest.search = "Parity";
    searchRequest.pageSize = 50;
    searchRequest.includeFacets = true;

    System.setProperty(READ_PATH_PROPERTY, "old");
    ApplicationsListResponseDTO oldSearch = list(searchRequest);
    System.setProperty(READ_PATH_PROPERTY, "new");
    ApplicationsListResponseDTO newSearch = list(searchRequest);

    assertThat(newSearch.total).isEqualTo(oldSearch.total).isEqualTo(3);
    assertThat(publicIds(newSearch)).containsExactlyInAnyOrderElementsOf(publicIds(oldSearch));
  }

  private ParitySnapshot collectAllPages(final ApplicationsListRequestDTO baseRequest) throws Exception {
    List<Integer> pageSizes = new ArrayList<>();
    Set<String> ids = new LinkedHashSet<>();
    ApplicationsListFacetsDTO facets = null;
    long total = -1;
    int page = 0;
    boolean hasNext;
    do {
      ApplicationsListRequestDTO request = copyRequest(baseRequest);
      request.page = page;
      ApplicationsListResponseDTO body = list(request);
      if (page == 0) {
        total = body.total;
        facets = body.facets;
      }
      pageSizes.add(body.applications == null ? 0 : body.applications.size());
      ids.addAll(publicIds(body));
      hasNext = body.hasNextPage;
      page++;
      assertThat(page).as("pagination should terminate").isLessThanOrEqualTo(10);
    }
    while (hasNext);
    return new ParitySnapshot(total, ids, pageSizes, facets);
  }

  private ApplicationsListResponseDTO list(final ApplicationsListRequestDTO request) throws Exception {
    HttpResponse response = restRequest()
        .path(ApplicationsListResource.APPLICATIONS_LIST_PATH)
        .body(request)
        .post();
    ctx.assertResponseStatus(200, response);
    return response.getBody(ApplicationsListResponseDTO.class);
  }

  private static ApplicationsListRequestDTO copyRequest(final ApplicationsListRequestDTO source) {
    ApplicationsListRequestDTO copy = new ApplicationsListRequestDTO();
    copy.organizationIds = source.organizationIds == null ? null : new HashSet<>(source.organizationIds);
    copy.applicationIds = source.applicationIds == null ? null : new HashSet<>(source.applicationIds);
    copy.stageIds = source.stageIds == null ? null : new HashSet<>(source.stageIds);
    copy.search = source.search;
    copy.pageSize = source.pageSize;
    copy.includeFacets = source.includeFacets;
    copy.orderBy = source.orderBy;
    return copy;
  }

  private static List<String> publicIds(final ApplicationsListResponseDTO body) {
    if (body.applications == null) {
      return List.of();
    }
    return body.applications.stream().map(card -> card.applicationId).collect(Collectors.toList());
  }

  private record ParitySnapshot(
      long total,
      Set<String> applicationIds,
      List<Integer> pageSizes,
      ApplicationsListFacetsDTO facets)
  {
    int pageCount() {
      return pageSizes.size();
    }
  }
}
