/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

import java.util.Set;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dashboard.DashboardResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightWork;

import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentsListResourceTest
    extends AbstractResourceTest
{
  private static final String COMPONENT_REPORT_RESOURCE = "/IndexSearchingTest/policyViolationReport";

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
  public void listComponents_flagOn_returnsPaginatedIndexRows() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("ComponentsTribe");
    Application app = tempEntity.newApplication("Comp App", "comp-app", org.getId());
    seedComponentReport(app, "comp-scan-1");
    ComponentsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    ComponentsListRequestDTO request = new ComponentsListRequestDTO();
    request.organizationIds = Set.of(org.getId());
    request.page = 0;
    request.pageSize = 50;

    HttpResponse response = restRequest()
        .path(ComponentsListResource.COMPONENTS_LIST_PATH)
        .body(request)
        .post();

    assertResponseStatus(200, response);
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
  public void listComponents_flagOff_returns404() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);

    HttpResponse response = restRequest()
        .path(ComponentsListResource.COMPONENTS_LIST_PATH)
        .body(new ComponentsListRequestDTO())
        .post();

    assertResponseStatus(404, response);
  }

  @Test
  public void listComponents_unsupportedOrderBy_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    ComponentsListRequestDTO request = new ComponentsListRequestDTO();
    request.orderBy = "-policyThreatLevel";

    HttpResponse response = restRequest()
        .path(ComponentsListResource.COMPONENTS_LIST_PATH)
        .body(request)
        .post();

    assertResponseStatus(400, response);
  }

  @Test
  public void listComponents_searchFiltersByComponentName() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("SearchComponentsTribe");
    Application app = tempEntity.newApplication("Search Comp App", "search-comp-app", org.getId());
    seedComponentReport(app, "search-comp-scan");
    ComponentsListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    ComponentsListRequestDTO request = new ComponentsListRequestDTO();
    request.organizationIds = Set.of(org.getId());
    request.search = "log4j";
    request.page = 0;
    request.pageSize = 50;

    HttpResponse response = restRequest()
        .path(ComponentsListResource.COMPONENTS_LIST_PATH)
        .body(request)
        .post();

    assertResponseStatus(200, response);
    ComponentsListResponseDTO body = response.getBody(ComponentsListResponseDTO.class);
    assertThat(body.components).isNotEmpty();
    assertThat(body.components)
        .extracting(row -> row.derivedComponentName == null ? "" : row.derivedComponentName.toLowerCase())
        .anyMatch(name -> name.contains("log4j"));
  }

  private void seedComponentReport(final Application app, final String scanId) throws Exception {
    var evaluation = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, scanId);
    ReportTestUtils.createReportFile(
        evaluation.getOwnerId(),
        evaluation.getScanId(),
        ReportTestUtils.zipReportDir(COMPONENT_REPORT_RESOURCE, tempDir),
        lookup(InsightWork.class));
  }
}
