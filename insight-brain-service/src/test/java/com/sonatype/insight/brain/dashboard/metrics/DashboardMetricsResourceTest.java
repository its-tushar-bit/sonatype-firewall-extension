/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics;

import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightWork;

import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.DASHBOARD_METRICS_SQL_MODE;

public class DashboardMetricsResourceTest
    extends AbstractResourceTest
{
  @After
  public void tearDownPreviewFlag() {
    tempEntity.deleteSystemConfigurationProperty(
        SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.getPropertyName());
    tempEntity.deleteSystemConfigurationProperty(DASHBOARD_METRICS_SQL_MODE);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(DashboardMetricsResource.RESOURCE_PATH);
  }

  @Test
  public void testGetMetrics_flagOn_returns200WithApplicationCount() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization();
    tempEntity.newApplication(org.getId());
    DashboardMetricsTestSupport.populateIndex(lookup(SearchIndexClient.class));

    HttpResponse response = restRequest()
        .path(DashboardMetricsResource.METRICS_PATH)
        .body(new DashboardMetricsRequestDTO())
        .post();

    assertResponseStatus(200, response);
    DashboardMetricsDTO metrics = response.getBody(DashboardMetricsDTO.class);
    assertThat(metrics.applications.total).isGreaterThanOrEqualTo(1);
    assertThat(metrics.applications.source).isEqualTo("index");
    assertThat(metrics.violations).isNotNull();
    assertThat(metrics.violations.source).isEqualTo("index");
    assertThat(metrics.components).isNotNull();
    assertThat(metrics.components.source).isEqualTo("index");
    assertThat(metrics.organizations).isNotNull();
    assertThat(metrics.organizations.source).isEqualTo("index");
    assertThat(metrics.policies).isNotNull();
    assertThat(metrics.policies.source).isEqualTo("index");
    assertThat(metrics.vulnerabilities).isNotNull();
    assertThat(metrics.vulnerabilities.source).isEqualTo("index");
    assertThat(metrics.legal).isNotNull();
    assertThat(metrics.legal.source).isEqualTo("index");
    assertThat(metrics.legal.breakdown).containsKeys("applications", "components");
    assertThat(metrics.waivers).isNotNull();
    assertThat(metrics.waivers.source).isEqualTo("sql");
  }

  @Test
  public void testGetMetrics_sqlModeOn_returnsSqlSourcesForMigratedMetrics() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);
    tempEntity.newSystemConfigurationProperty(DASHBOARD_METRICS_SQL_MODE, "ON");
    DashboardMetricsTestSupport.clearDashboardMetricsCache(lookup(DashboardMetricsService.class));
    Organization org = tempEntity.newOrganization();
    tempEntity.newApplication(org.getId());
    DashboardMetricsTestSupport.populateIndex(lookup(SearchIndexClient.class));

    HttpResponse response = restRequest()
        .path(DashboardMetricsResource.METRICS_PATH)
        .body(new DashboardMetricsRequestDTO())
        .post();

    assertResponseStatus(200, response);
    DashboardMetricsDTO metrics = response.getBody(DashboardMetricsDTO.class);
    assertThat(metrics.applications.source).isEqualTo("sql");
    assertThat(metrics.organizations.source).isEqualTo("sql");
    assertThat(metrics.policies.source).isEqualTo("sql");
    assertThat(metrics.violations.source).isEqualTo("sql");
    assertThat(metrics.components.source).isEqualTo("index");
    assertThat(metrics.lastUpdatedAt).isNotNull();
  }

  @Test
  public void testGetMetrics_summaryTier_returnsOnlySummaryFields() throws Exception {
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.includeHeavyMetrics = false;

    DashboardMetricsDTO metrics = postMetrics(request);

    assertThat(metrics.applications).isNotNull();
    assertThat(metrics.organizations).isNotNull();
    assertThat(metrics.policies).isNotNull();
    assertThat(metrics.waivers).isNotNull();
    assertThat(metrics.lastUpdatedAt).isNotNull();
    assertThat(metrics.violations).isNull();
    assertThat(metrics.components).isNull();
    assertThat(metrics.vulnerabilities).isNull();
    assertThat(metrics.legal).isNull();
  }

  @Test
  public void testGetMetrics_heavyTier_returnsOnlyHeavyFields() throws Exception {
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.includeHeavyMetrics = true;

    DashboardMetricsDTO metrics = postMetrics(request);

    assertThat(metrics.applications).isNull();
    assertThat(metrics.organizations).isNull();
    assertThat(metrics.policies).isNull();
    assertThat(metrics.waivers).isNull();
    assertThat(metrics.lastUpdatedAt).isNull();
    assertThat(metrics.violations).isNotNull();
    assertThat(metrics.components).isNotNull();
    assertThat(metrics.vulnerabilities).isNotNull();
    assertThat(metrics.legal).isNotNull();
  }

  @Test
  public void testGetMetrics_tagFilteredSummary_returns200WithMixedMetricValues() throws Exception {
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.tagIds = Set.of("tag-1");
    request.includeHeavyMetrics = false;

    DashboardMetricsDTO metrics = postMetrics(request);

    assertThat(metrics.applications).extracting("total").isNull();
    assertThat(metrics.applications).extracting("errorCode").isEqualTo("UNSUPPORTED_FILTER_COMBINATION");
    assertThat(metrics.applications).extracting("unsupportedDimensions").isEqualTo(List.of("tagIds"));
    assertThat(metrics.waivers).isNotNull();
    assertThat(metrics.waivers.total).isNotNegative();
    assertThat(metrics.violations).isNull();
  }

  @Test
  public void testGetMetrics_noIndex_returns409() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);
    User user = createUserWithPermissions(Permission.READ);

    DashboardMetricsTestSupport.runWithoutSearchIndex(
        lookup(InsightWork.class).getSearchIndexDir(),
        () -> {
          try {
            HttpResponse response = restRequest()
                .auth(user)
                .path(DashboardMetricsResource.METRICS_PATH)
                .body(new DashboardMetricsRequestDTO())
                .post();
            assertResponseStatus(409, response);
          }
          catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Test
  public void testGetMetrics_flagOff_returns403() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);

    HttpResponse response = restRequest()
        .path(DashboardMetricsResource.METRICS_PATH)
        .body(new DashboardMetricsRequestDTO())
        .post();

    assertResponseStatus(403, response);
  }

  private DashboardMetricsDTO postMetrics(DashboardMetricsRequestDTO request) throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);
    DashboardMetricsTestSupport.populateIndex(lookup(SearchIndexClient.class));

    HttpResponse response = restRequest()
        .path(DashboardMetricsResource.METRICS_PATH)
        .body(request)
        .post();

    assertResponseStatus(200, response);
    return response.getBody(DashboardMetricsDTO.class);
  }
}
