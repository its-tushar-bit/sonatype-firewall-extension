/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.service.AbstractMultiTenantResourceTest;

import org.junit.Test;

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_CONFIG_FEATURES_PATH;
import static org.assertj.core.api.Assertions.assertThat;

public class ConfigFeaturesResourceTest
    extends AbstractMultiTenantResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.adminRequest().path("api/").path(ADMIN_CONFIG_FEATURES_PATH).query("tenant=global");
  }

  @Test
  public void testFeatures() throws Exception {
    HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);
    String[] features = response.getBody(String[].class);

    assertThat(features).containsExactlyInAnyOrder(
        SystemConfigurationPropertyFeature.DASHBOARD_CAN_BE_ENABLED.getId(),
        SystemConfigurationPropertyFeature.LOGOUT_AUTH0_ON_LOGOUT.getId(),
        SystemConfigurationPropertyFeature.EMAIL_CONFIGURATION.getId()
    );
  }

  @Test
  public void testFeatures_all() throws Exception {
    HttpResponse response = restRequest().path("all").get();
    assertResponseStatus(200, response);
    String[] features = response.getBody(String[].class);

    assertThat(features).containsExactlyInAnyOrder(
        SystemConfigurationPropertyFeature.DASHBOARD_CAN_BE_ENABLED.getId(),
        SystemConfigurationPropertyFeature.LOGOUT_AUTH0_ON_LOGOUT.getId(),
        SystemConfigurationPropertyFeature.EMAIL_CONFIGURATION.getId()
    );
  }
}
