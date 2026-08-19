/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics;

import java.util.Set;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.DASHBOARD_METRICS_SQL_MODE;

/**
 * H2 port of {@code DashboardMetricsResourceTest}. Kept in the original package because
 * {@link DashboardMetricsTestSupport#runWithoutSearchIndex} is package-private.
 */
@IqH2Test
class IqH2DashboardMetricsResourceTest
{
  private IqTestContext ctx;

  @AfterEach
  void tearDownPreviewFlag() {
    ctx.tempEntity()
        .deleteSystemConfigurationProperty(
            SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.getPropertyName());
    ctx.tempEntity().deleteSystemConfigurationProperty(DASHBOARD_METRICS_SQL_MODE);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(DashboardMetricsResource.RESOURCE_PATH);
  }

  @Test
  void testGetMetrics_flagOn_returns200WithApplicationCount() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = ctx.tempEntity().newOrganization();
    ctx.tempEntity().newApplication(org.getId());
    DashboardMetricsTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    HttpResponse response = restRequest()
        .path(DashboardMetricsResource.METRICS_PATH)
        .body(new DashboardMetricsRequestDTO())
        .post();

    ctx.assertResponseStatus(200, response);
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
  void testGetMetrics_sqlModeOn_returnsSqlSourcesForMigratedMetrics() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);
    ctx.tempEntity().newSystemConfigurationProperty(DASHBOARD_METRICS_SQL_MODE, "ON");
    DashboardMetricsTestSupport.clearDashboardMetricsCache(ctx.lookup(DashboardMetricsService.class));
    Organization org = ctx.tempEntity().newOrganization();
    ctx.tempEntity().newApplication(org.getId());
    DashboardMetricsTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    HttpResponse response = restRequest()
        .path(DashboardMetricsResource.METRICS_PATH)
        .body(new DashboardMetricsRequestDTO())
        .post();

    ctx.assertResponseStatus(200, response);
    DashboardMetricsDTO metrics = response.getBody(DashboardMetricsDTO.class);
    assertThat(metrics.applications.source).isEqualTo("sql");
    assertThat(metrics.organizations.source).isEqualTo("sql");
    assertThat(metrics.policies.source).isEqualTo("sql");
    assertThat(metrics.violations.source).isEqualTo("sql");
    assertThat(metrics.components.source).isEqualTo("index");
    assertThat(metrics.lastUpdatedAt).isNotNull();
  }

  @Test
  void testGetMetrics_summaryTier_returnsOnlySummaryFields() throws Exception {
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
  void testGetMetrics_heavyTier_returnsOnlyHeavyFields() throws Exception {
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.includeHeavyMetrics = true;

    DashboardMetricsDTO metrics = postMetrics(request);

    assertThat(metrics.applications).isNull();
    assertThat(metrics.organizations).isNull();
    assertThat(metrics.policies).isNull();
    assertThat(metrics.waivers).isNull();
    // Index snapshot time is tier-independent so SHADOW persistent classification works for VIOLATIONS.
    assertThat(metrics.lastUpdatedAt).isNotNull();
    assertThat(metrics.violations).isNotNull();
    assertThat(metrics.components).isNotNull();
    assertThat(metrics.vulnerabilities).isNotNull();
    assertThat(metrics.legal).isNotNull();
  }

  @Test
  void testGetMetrics_tagFilteredSummary_returns200WithMixedMetricValues() throws Exception {
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.tagIds = Set.of("tag-1");
    request.includeHeavyMetrics = false;

    DashboardMetricsDTO metrics = postMetrics(request);

    assertThat(metrics.applications.total).isNotNull();
    assertThat(metrics.applications.errorCode).isNull();
    assertThat(metrics.waivers).isNotNull();
    assertThat(metrics.waivers.total).isNotNegative();
    assertThat(metrics.violations).isNull();
  }

  @Test
  void testGetMetrics_noIndex_returns409() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);
    User createdUser = createUserWithPermissions(Permission.READ);

    DashboardMetricsTestSupport.runWithoutSearchIndex(
        ctx.lookup(InsightWork.class).getSearchIndexDir(),
        () -> {
          try {
            HttpResponse response = restRequest()
                .auth(createdUser)
                .path(DashboardMetricsResource.METRICS_PATH)
                .body(new DashboardMetricsRequestDTO())
                .post();
            ctx.assertResponseStatus(409, response);
          }
          catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Test
  void testGetMetrics_flagOff_returns403() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);

    HttpResponse response = restRequest()
        .path(DashboardMetricsResource.METRICS_PATH)
        .body(new DashboardMetricsRequestDTO())
        .post();

    ctx.assertResponseStatus(403, response);
  }

  private User createUserWithPermissions(Permission... permissions) {
    User user = ctx.tempEntity().newUser();
    com.sonatype.insight.brain.model.security.Role role = ctx.tempEntity().newRole(false, permissions);
    ctx.tempEntity().newMembershipMapping(Organization.ROOT_ORGANIZATION_ID, role.getId(), user.getUsername());
    return user;
  }

  private DashboardMetricsDTO postMetrics(DashboardMetricsRequestDTO request) throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);
    DashboardMetricsTestSupport.populateIndex(ctx.lookup(SearchIndexClient.class));

    HttpResponse response = restRequest()
        .path(DashboardMetricsResource.METRICS_PATH)
        .body(request)
        .post();

    ctx.assertResponseStatus(200, response);
    return response.getBody(DashboardMetricsDTO.class);
  }
}
