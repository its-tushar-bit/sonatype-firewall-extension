/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics;

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

public class DashboardMetricsResourceTest
    extends AbstractResourceTest
{
  @After
  public void tearDownPreviewFlag() {
    tempEntity.deleteSystemConfigurationProperty(
        SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.getPropertyName());
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
}
