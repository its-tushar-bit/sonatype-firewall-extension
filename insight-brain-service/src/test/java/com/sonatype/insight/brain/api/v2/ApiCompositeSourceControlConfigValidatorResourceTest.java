/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.git.ConfigurationValidationResult;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.sonatype.insight.brain.api.PublicApiPaths.COMPOSITE_SOURCE_CONTROL_CONFIG_VALIDATOR_PATH_V2;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class ApiCompositeSourceControlConfigValidatorResourceTest
    extends AbstractResourceTest
{
  @Rule
  public WireMockRule gitService = new WireMockRule(wireMockConfig().dynamicPort());

  private Application app;

  private PasswordHandler pwHandler;

  private String validUrl;

  @Before
  public void setup() {
    gitService.stubFor(get(urlPathEqualTo("/api/v3/user"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody("{\"username\":\"foo\"}")
            .withStatus(HttpStatus.SC_OK)));
    gitService.stubFor(get(urlPathMatching("/api/v3/repos/.*/.*"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody("{ \"private\": false }")));
    gitService.stubFor(post(urlPathEqualTo("/api/v3/repos/organization/project/pulls"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withStatus(200)
            .withBody("{}")));
    validUrl = String.format("%s/organization/project", gitService.baseUrl());

    pwHandler = getCLMServer().getInstance(PasswordHandler.class);
    app = tempEntity.newApplicationWithParent();
    tempEntity
        .newSourceControl(ROOT_ORGANIZATION_ID, null, null, null, SourceControlProvider.GITHUB, null, null,
            "BASE_BRANCH", null);
    tempEntity.newSourceControl(app.getId(), validUrl, null, encrypt("TOKEN"), null, null, true, null, null);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(COMPOSITE_SOURCE_CONTROL_CONFIG_VALIDATOR_PATH_V2).auth();
  }

  @Test
  public void testValidateSourceControlConfig_ValidApplication() throws Exception {
    final HttpResponse response = restRequest()
        .parameter(app.getId())
        .get();
    assertResponseStatus(200, response);
    final ConfigurationValidationResult result = response.getBody(ConfigurationValidationResult.class);

    assertThat(result).isNotNull();
    assertThat(result.getConfigurationComplete().isValid()).isTrue();
  }

  @Test
  public void testValidateSourceControlConfig_InvalidApplication() throws Exception {
    final HttpResponse response = restRequest()
        .parameter("1234")
        .get();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).contains("Application with ID 1234 does not exist.");
  }

  @Test
  public void testValidateSourceControlConfig_UnexpectedException() throws Exception {
    Application appWithBrokenToken = tempEntity.newApplicationWithParent();
    tempEntity
        .newSourceControl(appWithBrokenToken.getId(), validUrl, null, null /* token */, null, null, true, null, null);

    // Retrieving the GitRepositoryInfo will throw a NullPointerException because the token is null.
    final HttpResponse response = restRequest()
        .parameter(appWithBrokenToken.getId())
        .get();
    assertResponseStatus(200, response);
    ConfigurationValidationResult configurationValidationResult = response.getBody(ConfigurationValidationResult.class);
    assertThat(configurationValidationResult.getTokenPermissions().isValid()).isFalse();
    assertThat(configurationValidationResult.getTokenPermissions().getMessage())
        .isEqualTo("Unable to test permissions.");
  }

  private String encrypt(String password) {
    return new String(pwHandler.encryptPassword(password.toCharArray()));
  }
}
