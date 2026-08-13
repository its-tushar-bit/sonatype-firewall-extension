/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiCrowdConfigurationResource;
import com.sonatype.insight.brain.api.v2.dto.ApiCrowdConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiStatusDTO;
import com.sonatype.insight.brain.dataaccess.configuration.crowd.CrowdConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.configuration.crowd.CrowdConfiguration;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kept in the original {@code com.sonatype.insight.brain.security} package because {@link CrowdMockServerRule}'s
 * {@code before()}/{@code after()} (inherited from JUnit4's {@code ExternalResource}) are {@code protected} with
 * no widened override, so calling them (instead of using an unsupported JUnit4 {@code @Rule}) requires being in the
 * same package as {@link CrowdMockServerRule}.
 */
@IqH2Test
class IqH2ApiCrowdConfigurationResourceTest
{
  private IqTestContext ctx;

  private final CrowdMockServerRule crowdMockServer = new CrowdMockServerRule();

  private static final String EXPECTED_FEATURE_DISABLED_MESSAGE =
      SystemConfigurationPropertyFeature.CROWD_INTEGRATION.getId() + " feature is disabled.";

  private CrowdConfigurationDAO dao;

  @BeforeEach
  void setUp() throws Throwable {
    crowdMockServer.before();
    dao = ctx.lookup(CrowdConfigurationDAO.class);
  }

  @AfterEach
  void tearDown() {
    crowdMockServer.after();
    SystemConfigurationPropertyFeature.CROWD_INTEGRATION.setEnabled(true);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.CROWD_CONFIG_RESOURCE_PATH_V2);
  }

  @Test
  void testGetCrowdConfiguration() throws Exception {
    CrowdConfiguration crowdConfiguration = ctx.tempEntity().newCrowdConfiguration();

    HttpResponse response = restRequest().get();

    ctx.assertResponseStatus(200, response);
    ApiCrowdConfigurationDTO dto = response.getBody(ApiCrowdConfigurationDTO.class);
    assertThat(dto).isNotNull();
    assertThat(dto.serverUrl).isEqualTo(crowdConfiguration.getServerUrl());
    assertThat(dto.applicationName).isEqualTo(crowdConfiguration.getApplicationName());
    assertThat(dto.applicationPassword).isNull();
    JsonNode node = new ObjectMapper().readTree(response.getBodyText());
    assertThat(node.has("applicationPassword")).isFalse();
  }

  @Test
  void testGetCrowdConfiguration_FeatureDisabled() throws Exception {
    SystemConfigurationPropertyFeature.CROWD_INTEGRATION.setEnabled(false);

    HttpResponse response = restRequest().get();

    ctx.assertResponseStatus(403, response);
    assertThat(response.getBodyText()).isEqualTo(EXPECTED_FEATURE_DISABLED_MESSAGE);
  }

  @Test
  void testInsertOrUpdateCrowdConfiguration() throws Exception {
    ApiCrowdConfigurationDTO dto = new ApiCrowdConfigurationDTO();
    dto.serverUrl = "serverUrl";
    dto.applicationName = "applicationName";
    dto.applicationPassword = "applicationPassword".toCharArray();

    HttpResponse response = restRequest().body(dto).put();

    ctx.assertResponseStatus(204, response);
    CrowdConfiguration crowdConfiguration = dao.get();
    assertThat(crowdConfiguration).isNotNull();
    assertThat(crowdConfiguration.getServerUrl()).isEqualTo(dto.serverUrl);
    assertThat(crowdConfiguration.getApplicationName()).isEqualTo(dto.applicationName);
    assertThat(ctx.lookup(PasswordHandler.class)
        .decryptPassword(crowdConfiguration.getApplicationPassword())).isEqualTo(dto.applicationPassword);
  }

  @Test
  void testInsertOrUpdateCrowdConfiguration_FeatureDisabled() throws Exception {
    SystemConfigurationPropertyFeature.CROWD_INTEGRATION.setEnabled(false);

    HttpResponse response = restRequest().body(null).put();

    ctx.assertResponseStatus(403, response);
    assertThat(response.getBodyText()).isEqualTo(EXPECTED_FEATURE_DISABLED_MESSAGE);
  }

  @Test
  void testDeleteCrowdConfiguration() throws Exception {
    ctx.tempEntity().newCrowdConfiguration();

    HttpResponse response = restRequest().delete();

    ctx.assertResponseStatus(204, response);
    CrowdConfiguration crowdConfiguration = dao.get();
    assertThat(crowdConfiguration).isNull();
  }

  @Test
  void testDeleteCrowdConfiguration_FeatureDisabled() throws Exception {
    SystemConfigurationPropertyFeature.CROWD_INTEGRATION.setEnabled(false);

    HttpResponse response = restRequest().delete();

    ctx.assertResponseStatus(403, response);
    assertThat(response.getBodyText()).isEqualTo(EXPECTED_FEATURE_DISABLED_MESSAGE);
  }

  @Test
  void testTestCrowdConfiguration_NoDTO_Success() throws Exception {
    crowdMockServer.mockTestConnection();
    ctx.tempEntity()
        .newCrowdConfiguration(crowdMockServer.getBaseUrl() + "/crowd", "applicationName",
            ctx.lookup(PasswordHandler.class).encryptPassword("applicationPassword".toCharArray()));

    HttpResponse response = restRequest().path(ApiCrowdConfigurationResource.TEST_PATH).post();

    ctx.assertResponseStatus(200, response);
    ApiStatusDTO dto = response.getBody(ApiStatusDTO.class);
    assertThat(dto.code).isEqualTo(200);
    assertThat(dto.message).isNull();
  }

  @Test
  void testTestCrowdConfiguration_NoDTO_Fail() throws Exception {
    crowdMockServer.mockTestConnectionError(401);
    ctx.tempEntity()
        .newCrowdConfiguration(crowdMockServer.getBaseUrl() + "/crowd", "applicationName",
            ctx.lookup(PasswordHandler.class).encryptPassword("applicationPassword".toCharArray()));

    HttpResponse response = restRequest().path(ApiCrowdConfigurationResource.TEST_PATH).post();

    ctx.assertResponseStatus(200, response);
    ApiStatusDTO dto = response.getBody(ApiStatusDTO.class);
    assertThat(dto.code).isEqualTo(400);
    assertThat(dto.message).isEqualTo("Error");
  }

  @Test
  void testTestCrowdConfiguration_NoDTO_FeatureDisabled() throws Exception {
    SystemConfigurationPropertyFeature.CROWD_INTEGRATION.setEnabled(false);

    HttpResponse response =
        restRequest().path(ApiCrowdConfigurationResource.TEST_PATH).post();

    ctx.assertResponseStatus(403, response);
    assertThat(response.getBodyText()).isEqualTo(EXPECTED_FEATURE_DISABLED_MESSAGE);
  }

  @Test
  void testTestCrowdConfiguration_DTO_Success() throws Exception {
    crowdMockServer.mockTestConnection();
    ApiCrowdConfigurationDTO dto = new ApiCrowdConfigurationDTO();
    dto.serverUrl = crowdMockServer.getBaseUrl() + "/crowd";
    dto.applicationName = "applicationName";
    dto.applicationPassword = "applicationPassword".toCharArray();

    HttpResponse response = restRequest().path(ApiCrowdConfigurationResource.TEST_PATH).body(dto).post();

    ctx.assertResponseStatus(200, response);
    ApiStatusDTO result = response.getBody(ApiStatusDTO.class);
    assertThat(result.code).isEqualTo(200);
    assertThat(result.message).isNull();
  }

  @Test
  void testTestCrowdConfiguration_DTO_Fail() throws Exception {
    crowdMockServer.mockTestConnectionError(401);
    ApiCrowdConfigurationDTO dto = new ApiCrowdConfigurationDTO();
    dto.serverUrl = crowdMockServer.getBaseUrl() + "/crowd";
    dto.applicationName = "applicationName";
    dto.applicationPassword = "applicationPassword".toCharArray();

    HttpResponse response = restRequest().path(ApiCrowdConfigurationResource.TEST_PATH).body(dto).post();

    ctx.assertResponseStatus(200, response);
    ApiStatusDTO result = response.getBody(ApiStatusDTO.class);
    assertThat(result.code).isEqualTo(400);
    assertThat(result.message).isEqualTo("Error");
  }

  @Test
  void testTestCrowdConfiguration_DTO_FeatureDisabled() throws Exception {
    SystemConfigurationPropertyFeature.CROWD_INTEGRATION.setEnabled(false);

    HttpResponse response =
        restRequest().path(ApiCrowdConfigurationResource.TEST_PATH).body(new ApiCrowdConfigurationDTO()).post();

    ctx.assertResponseStatus(403, response);
    assertThat(response.getBodyText()).isEqualTo(EXPECTED_FEATURE_DISABLED_MESSAGE);
  }
}
