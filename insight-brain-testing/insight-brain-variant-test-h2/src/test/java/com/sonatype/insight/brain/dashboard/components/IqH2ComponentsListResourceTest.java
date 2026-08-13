/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

import java.util.Set;
import java.util.function.Consumer;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dashboard.DashboardResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
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
 * H2 port of {@code ComponentsListResourceTest}. Kept in the original package because
 * {@link ComponentsListTestSupport} (package-private) is used to populate the search index.
 */
@IqH2Test
class IqH2ComponentsListResourceTest
{
  private static final String COMPONENT_REPORT_RESOURCE = "/IndexSearchingTest/policyViolationReport";

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
  void listComponents_flagOn_returnsPaginatedIndexRows() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("ComponentsTribe");
    Application app = ctx.tempEntity().newApplication("Comp App", "comp-app", org.getId());
    seedComponentReport(app, "comp-scan-1");
    ComponentsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ComponentsListRequestDTO request = new ComponentsListRequestDTO();
    request.organizationIds = Set.of(org.getId());
    request.page = 0;
    request.pageSize = 50;

    HttpResponse response = restRequest()
        .path(ComponentsListResource.COMPONENTS_LIST_PATH)
        .body(request)
        .post();

    ctx.assertResponseStatus(200, response);
    ComponentsListResponseDTO body = response.getBody(ComponentsListResponseDTO.class);
    assertThat(body.source).isEqualTo(ComponentsListResponseDTO.SOURCE_INDEX);
    assertThat(body.total).isGreaterThanOrEqualTo(1);
    assertThat(body.components).isNotEmpty();
    assertThat(body.components.get(0).hash).isNotBlank();
    // Classic SQL enrichment should populate risk for components with open violations.
    // score is nullable Integer (stubs omit it); unbox only when present.
    assertThat(body.components)
        .anyMatch(row -> (row.score != null && row.score > 0)
            || row.affectedApplications > 0
            || row.displayName != null);
    assertThat(body.facets).isNotNull();
    assertThat(body.facets.totalComponents).isGreaterThanOrEqualTo(1);
  }

  @Test
  void listComponents_flagOff_returns404() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);

    HttpResponse response = restRequest()
        .path(ComponentsListResource.COMPONENTS_LIST_PATH)
        .body(new ComponentsListRequestDTO())
        .post();

    ctx.assertResponseStatus(404, response);
  }

  @Test
  void listComponents_unsupportedOrderBy_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    ComponentsListRequestDTO request = new ComponentsListRequestDTO();
    request.orderBy = "-policyThreatLevel";

    HttpResponse response = restRequest()
        .path(ComponentsListResource.COMPONENTS_LIST_PATH)
        .body(request)
        .post();

    ctx.assertResponseStatus(400, response);
  }

  @Test
  void listComponents_searchFiltersByComponentName() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("SearchComponentsTribe");
    Application app = ctx.tempEntity().newApplication("Search Comp App", "search-comp-app", org.getId());
    seedComponentReport(app, "search-comp-scan");
    ComponentsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ComponentsListRequestDTO request = new ComponentsListRequestDTO();
    request.organizationIds = Set.of(org.getId());
    request.search = "log4j";
    request.page = 0;
    request.pageSize = 50;

    HttpResponse response = restRequest()
        .path(ComponentsListResource.COMPONENTS_LIST_PATH)
        .body(request)
        .post();

    ctx.assertResponseStatus(200, response);
    ComponentsListResponseDTO body = response.getBody(ComponentsListResponseDTO.class);
    assertThat(body.components).isNotEmpty();
    assertThat(body.components)
        .extracting(row -> row.derivedComponentName == null ? "" : row.derivedComponentName.toLowerCase())
        .anyMatch(name -> name.contains("log4j"));
  }

  /**
   * Wave C exposes application and stage facets on the rail (CLM-43211), so selecting one has to
   * narrow the list while the sibling facet stays visible — otherwise the rail dead-ends after the
   * first click.
   */
  @Test
  void listComponents_applicationFilter_narrowsResultsAndKeepsSiblingFacets() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("ScopeComponentsTribe");
    Application first = ctx.tempEntity().newApplication("Scope Comp One", "scope-comp-one", org.getId());
    Application second = ctx.tempEntity().newApplication("Scope Comp Two", "scope-comp-two", org.getId());
    seedComponentReport(first, "scope-comp-scan-1");
    seedComponentReport(second, "scope-comp-scan-2");
    ComponentsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ComponentsListResponseDTO all = listComponents(request -> request.organizationIds = Set.of(org.getId()));
    assertThat(all.facets.applications).containsKeys(first.getId(), second.getId());

    ComponentsListResponseDTO filtered = listComponents(request -> {
      request.organizationIds = Set.of(org.getId());
      request.applicationIds = Set.of(first.getId());
    });

    assertThat(filtered.total).isGreaterThanOrEqualTo(1);
    // The unselected sibling stays in the rail so the selection can be widened or swapped.
    assertThat(filtered.facets.applications).containsKeys(first.getId(), second.getId());
  }

  /**
   * Stage counts and names come from {@code POLICY_VIOLATION} docs, so the fixture needs a real
   * violation — a component report alone leaves the stage facet absent. The rail has no other
   * source for the label, since a component row carries no stage breakdown.
   */
  @Test
  void listComponents_stageFacet_carriesCountsAndDisplayNames() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("StageComponentsTribe");
    Application app = ctx.tempEntity().newApplication("Stage Comp App", "stage-comp-app", org.getId());
    seedViolatingComponent(app, Stage.ID_BUILD, "stage-comp-scan");
    ComponentsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    ComponentsListResponseDTO body = listComponents(request -> request.organizationIds = Set.of(org.getId()));

    assertThat(body.facets.stages).containsKey(Stage.ID_BUILD);
    assertThat(body.facets.stageNames).containsKey(Stage.ID_BUILD);
  }

  /**
   * The stage filter is violation-scoped, so a stage with no violations must return nothing rather
   * than silently drop the filter and show the unfiltered estate.
   */
  @Test
  void listComponents_stageWithNoViolations_returnsEmptyRatherThanIgnoringTheFilter() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization("EmptyStageComponentsTribe");
    Application app = ctx.tempEntity().newApplication("Empty Stage App", "empty-stage-app", org.getId());
    seedViolatingComponent(app, Stage.ID_BUILD, "empty-stage-scan");
    ComponentsListTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    assertThat(listComponents(request -> request.organizationIds = Set.of(org.getId())).total)
        .isGreaterThan(0);

    ComponentsListResponseDTO body = listComponents(request -> {
      request.organizationIds = Set.of(org.getId());
      request.stageIds = Set.of(Stage.ID_RELEASE);
    });

    assertThat(body.components).isEmpty();
    assertThat(body.total).isZero();
  }

  private ComponentsListResponseDTO listComponents(
      final Consumer<ComponentsListRequestDTO> customizer) throws Exception
  {
    ComponentsListRequestDTO request = new ComponentsListRequestDTO();
    request.page = 0;
    request.pageSize = 50;
    customizer.accept(request);

    HttpResponse response = restRequest()
        .path(ComponentsListResource.COMPONENTS_LIST_PATH)
        .body(request)
        .post();

    ctx.assertResponseStatus(200, response);
    return response.getBody(ComponentsListResponseDTO.class);
  }

  private void seedComponentReport(final Application app, final String scanId) throws Exception {
    PolicyEvaluation evaluation = ctx.tempEntity().newPolicyEvaluation(app.getId(), Stage.ID_BUILD, scanId);
    ReportTestUtils.createReportFile(
        evaluation.getOwnerId(),
        evaluation.getScanId(),
        ReportTestUtils.zipReportDir(COMPONENT_REPORT_RESOURCE, ctx.tempFolder()),
        ctx.lookup(InsightWork.class));
  }

  /** Seeds a scanned component report plus a violation at {@code stageId}, which stage facets read. */
  private void seedViolatingComponent(
      final Application app,
      final String stageId,
      final String scanId) throws Exception
  {
    PolicyEvaluation evaluation = ctx.tempEntity().newPolicyEvaluation(app.getId(), stageId, scanId);
    ReportTestUtils.createReportFile(
        evaluation.getOwnerId(),
        evaluation.getScanId(),
        ReportTestUtils.zipReportDir(COMPONENT_REPORT_RESOURCE, ctx.tempFolder()),
        ctx.lookup(InsightWork.class));
    ctx.tempEntity().newPolicyViolation(evaluation, ctx.tempEntity().newPolicy(app));
  }
}
