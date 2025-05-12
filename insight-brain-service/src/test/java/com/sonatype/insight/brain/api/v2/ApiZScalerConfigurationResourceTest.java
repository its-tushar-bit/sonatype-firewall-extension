/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.dataaccess.configuration.ZScalerConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.ZScalerConfiguration;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.ZScalerMockServerRule;
import com.sonatype.insight.brain.zscaler.ApiZScalerConfigurationService.ApiZScalerConfigurationDTO;
import com.sonatype.insight.brain.zscaler.ZScalerCategory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.sonatype.insight.brain.api.PublicApiPaths.ZSCALER_CONFIG_RESOURCE_PATH_V2;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiZScalerConfigurationResourceTest
    extends AbstractResourceTest
{
  @Rule
  public ZScalerMockServerRule zScalerMockServer = new ZScalerMockServerRule();

  private ZScalerConfigurationDAO zScalerConfigurationDAO;

  private PasswordHandler passwordHandler;

  @Before
  public void setup() throws Exception {
    zScalerConfigurationDAO = lookup(ZScalerConfigurationDAO.class);
    passwordHandler = lookup(PasswordHandler.class);
    restRequest().path(PublicApiPaths.CONFIG_FEATURES_PATH).path(SystemConfigurationProperty.ZSCALER).post();
  }

  @After
  public void tearDown() throws Exception {
    restRequest().path(PublicApiPaths.CONFIG_FEATURES_PATH).path(SystemConfigurationProperty.ZSCALER).delete();
  }

  @Test
  public void testGetZScalerConfiguration() throws Exception {
    ZScalerConfiguration config = tempEntity.newZScalerConfiguration("user", "password", "host", "apikey");

    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2).get();

    assertResponseStatus(200, response);
    ApiZScalerConfigurationDTO configDTO = response.getBody(ApiZScalerConfigurationDTO.class);
    assertThat(configDTO).isNotNull();
    assertThat(configDTO.getUsername()).isEqualTo(config.getUsername());
    assertThat(configDTO.getPassword()).isNull();
    assertThat(configDTO.getHostname()).isEqualTo(config.getHostname());
    assertThat(configDTO.getApiKey()).isEqualTo(config.getApikey());
  }

  @Test
  public void testGetZScalerConfiguration_noConfiguration() throws Exception {
    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2).get();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Zscaler not configured.");
  }

  @Test
  public void testSetZScalerConfiguration() throws Exception {
    ApiZScalerConfigurationDTO request = new ApiZScalerConfigurationDTO();
    request.setUsername("testusername");
    request.setPassword("testpassword");
    request.setHostname("testhostname");
    request.setApiKey("testapikey");

    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2).body(request).put();

    assertResponseStatus(204, response);

    ZScalerConfiguration config = zScalerConfigurationDAO.get();
    assertThat(config).isNotNull();
    assertThat(config.getUsername()).isEqualTo(request.getUsername());
    assertThat(passwordHandler.decryptPassword(config.getPassword())).isEqualTo(request.getPassword());
    assertThat(config.getHostname()).isEqualTo(request.getHostname());
    assertThat(config.getApikey()).isEqualTo(request.getApiKey());
  }

  @Test
  public void testSetZScalerConfiguration_nullConfiguration() throws Exception {
    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2).body(null).put();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Configuration is required.");
  }

  @Test
  public void testDeleteZScalerConfiguration() throws Exception {
    tempEntity.newZScalerConfiguration("user", "password", "host", "apikey");

    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2).delete();

    assertResponseStatus(204, response);

    ZScalerConfiguration config = zScalerConfigurationDAO.get();
    assertThat(config).isNull();
  }

  @Test
  public void testDeleteZScalerConfiguration_noConfiguration() throws Exception {
    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2).get();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Zscaler not configured.");
  }

  @Test
  public void testPatchZScalerConfiguration() throws Exception {
    String username = "username";
    String password = passwordHandler.encryptPassword("password");
    String apiKey = "cajgffdcgkej";

    ZScalerCategory mavenCategory = new ZScalerCategory();
    mavenCategory.setId("maven-category");
    mavenCategory.setConfiguredName("sonatype-maven-shadow-download-defense");
    mavenCategory.setCustomCategory(true);
    mavenCategory.setUrls(List.of("https://repo1.maven.org/maven2/"));

    tempEntity.newZScalerConfiguration(username, password, zScalerMockServer.getBaseUrl(), apiKey);

    zScalerMockServer.mockAuthentication(200, "{\"token\":\"mock-token\"}");
    zScalerMockServer.mockGetQuota(200, "{\"uniqueUrlsProvisioned\":\"1000\", \"remainingUrlsQuota\":\"10\"}");
    zScalerMockServer.mockGetCustomUrlCategories(200, new ObjectMapper().writeValueAsString(List.of(mavenCategory)));
    zScalerMockServer.mockUpdateCustomUrlCategories(200, "{\"status\":\"success\"}");
    zScalerMockServer.mockActivateChanges(200, "{\"status\":\"success\"}");

    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2 + "/MAVEN").patch();

    assertResponseStatus(204, response);

    zScalerMockServer.getWireMockServer()
        .verify(postRequestedFor(urlPathMatching("/api/v1/authenticatedSession")));
    zScalerMockServer.getWireMockServer()
        .verify(getRequestedFor(urlPathMatching("/api/v1/urlCategories/urlQuota")));
    zScalerMockServer.getWireMockServer().verify(getRequestedFor(urlPathMatching("/api/v1/urlCategories"))
        .withQueryParam("customOnly", equalTo("true")));
    zScalerMockServer.getWireMockServer().verify(getRequestedFor(urlPathMatching("/api/v1/urlCategories/.*")));
    zScalerMockServer.getWireMockServer().verify(postRequestedFor(urlPathMatching("/api/v1/status/activate")));
  }

  @Test
  public void testPatchZScalerConfiguration_noConfiguration() throws Exception {
    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2 + "/MAVEN").patch();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("No zScaler configuration found");
  }

  @Test
  public void testPatchZScalerConfiguration_authenticationException() throws Exception {
    tempEntity.newZScalerConfiguration(
        "username", passwordHandler.encryptPassword("password"), zScalerMockServer.getBaseUrl(), "cajgffdcgkej");

    zScalerMockServer.mockAuthentication(401, "{\"error\":\"Invalid credentials\"}");

    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2 + "/MAVEN").patch();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("Authentication failed: {\"error\":\"Invalid credentials\"}");
  }

  @Test
  public void testPatchZScalerConfiguration_getCustomUrlCategoriesException() throws Exception {
    tempEntity.newZScalerConfiguration(
        "username", passwordHandler.encryptPassword("password"), zScalerMockServer.getBaseUrl(), "cajgffdcgkej");

    zScalerMockServer.mockAuthentication(200, "{\"token\":\"mock-token\"}");
    zScalerMockServer.mockGetCustomUrlCategories(500, "{\"error\":\"Internal Server Error\"}");

    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2 + "/MAVEN").patch();

    assertResponseStatus(204, response);
  }

  @Test
  public void testPatchZScalerConfiguration_updateCustomUrlCategoriesException() throws Exception {
    tempEntity.newZScalerConfiguration(
        "username", passwordHandler.encryptPassword("password"), zScalerMockServer.getBaseUrl(), "cajgffdcgkej");

    zScalerMockServer.mockAuthentication(200, "{\"token\":\"mock-token\"}");
    zScalerMockServer.mockGetCustomUrlCategories(200, "[]");
    zScalerMockServer.mockUpdateCustomUrlCategories(500, "{\"error\":\"Update failed\"}");

    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2 + "/MAVEN").patch();

    assertResponseStatus(204, response);
  }

  @Test
  public void testPatchZScalerConfiguration_createCustomUrlCategoryException() throws Exception {
    tempEntity.newZScalerConfiguration(
        "username", passwordHandler.encryptPassword("password"), zScalerMockServer.getBaseUrl(), "cajgffdcgkej");

    zScalerMockServer.mockAuthentication(200, "{\"token\":\"mock-token\"}");
    zScalerMockServer.mockGetCustomUrlCategories(200, "[]");
    zScalerMockServer.mockCreateCustomUrlCategory(500, "{\"error\":\"Creation failed\"}");

    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2 + "/MAVEN").patch();

    assertResponseStatus(204, response);
  }

  @Test
  public void testPatchZScalerConfiguration_activateChangesException() throws Exception {
    tempEntity.newZScalerConfiguration(
        "username", passwordHandler.encryptPassword("password"), zScalerMockServer.getBaseUrl(), "cajgffdcgkej");

    zScalerMockServer.mockAuthentication(200, "{\"token\":\"mock-token\"}");
    zScalerMockServer.mockGetCustomUrlCategories(200, "[]");
    zScalerMockServer.mockUpdateCustomUrlCategories(200, "{\"status\":\"success\"}");
    zScalerMockServer.mockActivateChanges(500, "{\"error\":\"Activation failed\"}");

    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2 + "/MAVEN").patch();

    assertResponseStatus(204, response);
  }
}
