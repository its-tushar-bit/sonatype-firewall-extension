/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import javax.ws.rs.core.HttpHeaders;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.admin.authorization.AuthorizationTestHelper;
import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.service.AbstractMultiTenantResourceTest;

import org.junit.Test;

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_CONFIG_FEATURES_PATH;
import static org.assertj.core.api.Assertions.assertThat;

public class ConfigFeaturesResourceTest
    extends AbstractMultiTenantResourceTest
{
  protected HttpRequest restRequest(String path) {
    return super.adminRequest().path("api/").path(path);
  }

  @Test
  public void testFeatures() throws Exception {
    HttpResponse response = callConfigFeaturesEndpoint().get();
    assertResponseStatus(200, response);
    String[] features = response.getBody(String[].class);

    assertThat(features).containsExactlyInAnyOrder(
        SystemConfigurationPropertyFeature.DASHBOARD_CAN_BE_ENABLED.getId(),
        SystemConfigurationPropertyFeature.LOGOUT_AUTH0_ON_LOGOUT.getId(),
        SystemConfigurationPropertyFeature.WEBHOOK_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.EMAIL_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.AUTOMATIC_SCM_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.DEFAULT_BRANCH_MONITORING.getId(),
        SystemConfigurationPropertyFeature.PR_COMMENTING.getId(),
        SystemConfigurationPropertyFeature.PR_LINE_COMMENTING.getId(),
        SystemConfigurationPropertyFeature.REPORTS_LIST_CAN_BE_ENABLED.getId(),
        SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.getId()
    );
  }

  @Test
  public void testFeatures_all() throws Exception {
    HttpResponse response = callConfigFeaturesEndpoint().path("all").get();
    assertResponseStatus(200, response);
    String[] features = response.getBody(String[].class);

    assertThat(features).containsExactlyInAnyOrder(
        SystemConfigurationPropertyFeature.DASHBOARD_CAN_BE_ENABLED.getId(),
        SystemConfigurationPropertyFeature.LOGOUT_AUTH0_ON_LOGOUT.getId(),
        SystemConfigurationPropertyFeature.WEBHOOK_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.EMAIL_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.ENABLE_SSO_ONLY.getId(),
        SystemConfigurationPropertyFeature.AUTOMATIC_SCM_CONFIGURATION.getId(),
        SystemConfigurationPropertyFeature.DEFAULT_BRANCH_MONITORING.getId(),
        SystemConfigurationPropertyFeature.PR_COMMENTING.getId(),
        SystemConfigurationPropertyFeature.PR_LINE_COMMENTING.getId(),
        SystemConfigurationPropertyFeature.REPORTS_LIST_CAN_BE_ENABLED.getId(),
        SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.getId()
    );
  }

  private HttpRequest callConfigFeaturesEndpoint() throws Exception {
    return restRequest(ADMIN_CONFIG_FEATURES_PATH)
        .query("tenant=global")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + AuthorizationTestHelper.createJwt());
  }
}
