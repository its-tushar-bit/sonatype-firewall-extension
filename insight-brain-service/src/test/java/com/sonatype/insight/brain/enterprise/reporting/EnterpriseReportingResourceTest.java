/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.enterprise.reporting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.security.Role.SYSTEM_ADMIN_ROLE_ID;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class EnterpriseReportingResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(EnterpriseReportingResource.RESOURCE_PATH);
  }

  @Before
  public void before() {
    ApiConfigFeaturesService.SystemConfigurationPropertyFeature
        .INTEGRATED_ENTERPRISE_REPORTING.setEnabled(true);
  }

  @After
  public void cleanup() {
    ApiConfigFeaturesService.SystemConfigurationPropertyFeature
        .INTEGRATED_ENTERPRISE_REPORTING.setEnabled(false);
  }

  @Test
  public void testCreateSSOEmbedUrl_LookerError() throws Exception {
    hdsMockServer.respondWith("error").andStatus(409).atUri("rest/enterpriseReporting/ssoEmbedUrl");
    HttpResponse response = restRequest().path(EnterpriseReportingResource.SSO_EMBED_URL_PATH)
        .body(new DashboardRequestDTO("rolling_recap")).post();
    assertResponseStatus(409, response);
  }

  @Test
  public void testCreateSSOEmbedUrl_Success() throws Exception {
    String username = "admin";
    //Set<String> membership = new HashSet<>(Arrays.asList("developers", "qa"));
    final Organization organization = tempEntity.newOrganization("Test Org");
    final Application application = tempEntity.newApplication("Some App", "SOME_APP", organization.getId());
    final Application application2 = tempEntity.newApplication("Some App 2", "SOME_APP2", organization.getId());
    final Application application3 = tempEntity.newApplication("Some App 3", "SOME_APP3", organization.getId());
    final Application application4 = tempEntity.newApplication("Some App 4", "SOME_APP4", organization.getId());
    tempEntity.newMembershipMapping(application.getId(), SYSTEM_ADMIN_ROLE_ID, username);
    tempEntity.newMembershipMapping(application2.getId(), SYSTEM_ADMIN_ROLE_ID, username);
    tempEntity.newMembershipMapping(application3.getId(), SYSTEM_ADMIN_ROLE_ID, username);
    tempEntity.newMembershipMapping(application4.getId(), SYSTEM_ADMIN_ROLE_ID, username);

    String lookerSSOUrl = "looker.someurl.com";
    String baseUrl = "https://looker.example.com";
    hdsMockServer.respondWith("{\"url\":\"" + lookerSSOUrl + "\"}").atUri("rest/enterpriseReporting/ssoEmbedUrl");
    hdsMockServer.respondWith("{\"baseUrl\":\"" + baseUrl + "\"}").atUri("rest/enterpriseReporting/config");
    DashboardRequestDTO dashboardRequestDTO =
        new DashboardRequestDTO("rolling_recap");

    HttpResponse response = restRequest().path(EnterpriseReportingResource.SSO_EMBED_URL_PATH)
        .body(dashboardRequestDTO).post();
    assertResponseStatus(200, response);
    String expectedResponse = "{\"url\":\"" + lookerSSOUrl + "\",\"baseUrl\":\"" + baseUrl + "\"}";
    assertThat(response.getBodyText()).contains(expectedResponse);
  }

  @Test
  public void testGetLookerDashboardMetadata_Success() throws Exception {
    hdsMockServer.respondWith(createDashboardVersionJson()).atUri("rest/enterpriseReporting/currentVersion");
    hdsMockServer.respondWith(createDashboardMetadataJsonList()).atUri("rest/enterpriseReporting/dashboards");
    hdsMockServer.respondWith(new byte[0]).atUri("rest/enterpriseReporting/icons/rolling-recap.svg");
    HttpResponse response = restRequest().path(EnterpriseReportingResource.DASHBOARDS_METADATA_PATH).get();
    assertResponseStatus(200, response);
    DashboardMetadataListDTO responseList =
        response.getBody(DashboardMetadataListDTO.class);
    assertThat(responseList).isNotNull();
    assertThat(responseList.dashboardMetadata.size()).isEqualTo(1);
    DashboardMetadataDTO dashboardMetadataDTO = responseList.dashboardMetadata.get(0);
    assertThat(dashboardMetadataDTO.dashboardId).isEqualTo("sbom-scorecard");
    assertThat(dashboardMetadataDTO.title).isEqualTo("Sbom Report Overview");
    assertThat(dashboardMetadataDTO.description).isEqualTo("A comprehensive view of monthly sboms");
    assertThat(dashboardMetadataDTO.features.size()).isEqualTo(2);
    assertThat(dashboardMetadataDTO.accessButtonText).isEqualTo("Open Dashboard");
    assertThat(dashboardMetadataDTO.previewImage).isEqualTo("preview001.jpg");
    assertThat(dashboardMetadataDTO.priority).isEqualTo(1);
    assertThat(dashboardMetadataDTO.spotlight).isTrue();
  }

  @Test
  public void testGetIcon_Success() throws Exception {
    assertTestGetIcon(200, "rolling-recap.svg");
  }

  @Test
  public void testGetIcon_404_NotFound() throws Exception {
    assertTestGetIcon(404, "fake-icon-name.svg");
  }

  @Test
  public void testGetIcon_400_BadRequest() throws Exception {
    assertTestGetIcon(400, "..\\fake-icon-name.svg");
  }

  private void assertTestGetIcon(int expectedStatus, String iconName) throws Exception {
    hdsMockServer.respondWith(createDashboardVersionJson()).atUri("rest/enterpriseReporting/currentVersion");
    hdsMockServer.respondWith(createDashboardMetadataJsonList()).atUri("rest/enterpriseReporting/dashboards");
    hdsMockServer.respondWith(getBytesFromIconsZip()).atUri("rest/enterpriseReporting/icons");
    restRequest().path(EnterpriseReportingResource.DASHBOARDS_METADATA_PATH).get();
    HttpResponse response = restRequest()
        .path(EnterpriseReportingResource.GET_IER_ICON_PATH)
        .parameter(iconName)
        .get();
    assertResponseStatus(expectedStatus, response);
  }

  private String createDashboardMetadataJsonList() {
    return "{\n" +
        "  \"dashboardMetadata\": [\n" +
        "    {\n" +
        "      \"dashboardId\": \"sbom-scorecard\",\n" +
        "      \"title\": \"Sbom Report Overview\",\n" +
        "      \"description\": \"A comprehensive view of monthly sboms\",\n" +
        "      \"features\": [\n" +
        "        \"Graphs\",\n" +
        "        \"Tables\"\n" +
        "      ],\n" +
        "      \"accessButtonText\": \"Open Dashboard\",\n" +
        "      \"previewImage\": \"preview001.jpg\",\n" +
        "      \"priority\": 1,\n" +
        "      \"spotlight\": true\n" +
        "    }\n" +
        "  ]\n" +
        "}";
  }

  private byte[] getBytesFromIconsZip() throws IOException {
    return Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/icons_svg.zip").getPath()));
  }

  private String createDashboardVersionJson() {
    return "{\"version\":1}";
  }
}
