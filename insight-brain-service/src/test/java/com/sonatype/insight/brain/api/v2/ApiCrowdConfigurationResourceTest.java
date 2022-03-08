/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiCrowdConfigurationDTO;
import com.sonatype.insight.brain.dataaccess.configuration.crowd.CrowdConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.crowd.CrowdConfiguration;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.ExperimentalFeature;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import static com.google.common.collect.ImmutableMap.of;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiCrowdConfigurationResourceTest
    extends AbstractResourceTest
{
  private static final String EXPECTED_FEATURE_DISABLED_MESSAGE =
      ExperimentalFeature.CROWD_INTEGRATION.getFlag() + " feature is disabled.";

  private final CrowdConfigurationDAO dao = new CrowdConfigurationDAO();

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.CROWD_CONFIG_RESOURCE_PATH_V2);
  }

  @Before
  public void before() {
    getCLMServer().getInstance(InsightConfig.class)
        .setExperimentalFeatures(of(ExperimentalFeature.CROWD_INTEGRATION.getFlag(), true));
  }

  @Test
  public void testGetCrowdConfiguration() throws Exception {
    CrowdConfiguration crowdConfiguration = tempEntity.newCrowdConfiguration();

    HttpResponse response = restRequest().get();

    assertResponseStatus(200, response);
    ApiCrowdConfigurationDTO dto = response.getBody(ApiCrowdConfigurationDTO.class);
    assertThat(dto).isNotNull();
    assertThat(dto.serverUrl).isEqualTo(crowdConfiguration.getServerUrl());
    assertThat(dto.applicationName).isEqualTo(crowdConfiguration.getApplicationName());
    assertThat(dto.applicationPassword).isNull();
    JsonNode node = new ObjectMapper().readTree(response.getBodyText());
    assertThat(node.has("applicationPassword")).isFalse();
  }

  @Test
  public void testGetCrowdConfiguration_FeatureDisabled() throws Exception {
    getCLMServer().getInstance(InsightConfig.class)
        .setExperimentalFeatures(of(ExperimentalFeature.CROWD_INTEGRATION.getFlag(), false));

    HttpResponse response = restRequest().get();

    assertResponseStatus(403, response);
    assertThat(response.getBodyText()).isEqualTo(EXPECTED_FEATURE_DISABLED_MESSAGE);
  }

  @Test
  public void testInsertOrUpdateCrowdConfiguration() throws Exception {
    ApiCrowdConfigurationDTO dto = new ApiCrowdConfigurationDTO();
    dto.serverUrl = "serverUrl";
    dto.applicationName = "applicationName";
    dto.applicationPassword = "applicationPassword".toCharArray();

    HttpResponse response = restRequest().body(dto).put();

    assertResponseStatus(204, response);
    CrowdConfiguration crowdConfiguration = dao.get();
    assertThat(crowdConfiguration).isNotNull();
    assertThat(crowdConfiguration.getServerUrl()).isEqualTo(dto.serverUrl);
    assertThat(crowdConfiguration.getApplicationName()).isEqualTo(dto.applicationName);
    assertThat(getCLMServer().getInstance(PasswordHandler.class)
        .decryptPassword(crowdConfiguration.getApplicationPassword())).isEqualTo(dto.applicationPassword);
  }

  @Test
  public void testInsertOrUpdateCrowdConfiguration_FeatureDisabled() throws Exception {
    getCLMServer().getInstance(InsightConfig.class)
        .setExperimentalFeatures(of(ExperimentalFeature.CROWD_INTEGRATION.getFlag(), false));

    HttpResponse response = restRequest().body(null).put();

    assertResponseStatus(403, response);
    assertThat(response.getBodyText()).isEqualTo(EXPECTED_FEATURE_DISABLED_MESSAGE);
  }

  @Test
  public void testDeleteCrowdConfiguration() throws Exception {
    tempEntity.newCrowdConfiguration();

    HttpResponse response = restRequest().delete();

    assertResponseStatus(204, response);
    CrowdConfiguration crowdConfiguration = dao.get();
    assertThat(crowdConfiguration).isNull();
  }

  @Test
  public void testDeleteCrowdConfiguration_FeatureDisabled() throws Exception {
    getCLMServer().getInstance(InsightConfig.class)
        .setExperimentalFeatures(of(ExperimentalFeature.CROWD_INTEGRATION.getFlag(), false));

    HttpResponse response = restRequest().delete();

    assertResponseStatus(403, response);
    assertThat(response.getBodyText()).isEqualTo(EXPECTED_FEATURE_DISABLED_MESSAGE);
  }
}
