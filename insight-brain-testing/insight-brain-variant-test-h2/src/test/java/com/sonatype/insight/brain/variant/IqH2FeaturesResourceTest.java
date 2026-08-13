/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.features.FeaturesResource;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;

import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.features.FeaturesResource.ENABLE_SSO_ONLY;
import static com.sonatype.insight.brain.features.FeaturesResource.ENABLE_UNAUTHENTICATED_PAGES;
import static com.sonatype.insight.brain.features.FeaturesResource.OAUTH2_ENABLED;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * H2 port of {@code FeaturesResourceTest}.
 */
@IqH2Test
class IqH2FeaturesResourceTest
{
  private IqTestContext ctx;

  private HttpRequest restRequest() {
    return ctx.restRequest().path(FeaturesResource.RESOURCE_PATH);
  }

  @Test
  void testFeatures_Licensed() throws Exception {
    // Get all features
    HttpResponse response = restRequest().get();
    ctx.assertResponseStatus(200, response);
    String[] features = response.getBody(String[].class);
    assertThat(features).contains("policy", "labels", "policy-violations");
  }

  @Test
  void testFeatures_Unlicensed() throws Exception {
    // Get all features
    ctx.uninstallLicense();
    HttpResponse response = restRequest().get();
    ctx.assertResponseStatus(200, response);
    String[] features = response.getBody(String[].class);
    assertThat(features).isEmpty();
  }

  @Test
  void testGetEnableUnauthenticatedPages() throws Exception {
    HttpResponse response = restRequest().path(ENABLE_UNAUTHENTICATED_PAGES).anon().get();
    ctx.assertResponseStatus(200, response);
    String[] features = response.getBody(String[].class);
    assertThat(features).hasSize(1).containsOnly("enable-unauthenticated-pages");
  }

  @Test
  void testGetEnableSsoOnly() throws Exception {
    SystemConfigurationPropertyFeature.ENABLE_SSO_ONLY.setEnabled(true);
    HttpResponse response = restRequest().path(ENABLE_SSO_ONLY).anon().get();
    ctx.assertResponseStatus(200, response);
    String[] features = response.getBody(String[].class);
    assertThat(features).hasSize(1).containsOnly("enable-sso-only");
  }

  @Test
  void testGetOAuth2Enabled() throws Exception {
    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(true);
    HttpResponse response = restRequest().path(OAUTH2_ENABLED).anon().get();
    ctx.assertResponseStatus(200, response);
    String[] features = response.getBody(String[].class);
    assertThat(features).hasSize(1).containsOnly("oauth2-enabled");
  }
}
