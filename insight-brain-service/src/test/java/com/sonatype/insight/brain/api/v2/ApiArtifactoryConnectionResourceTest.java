/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Arrays;
import java.util.Collections;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiArtifactoryConnectionStatusRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiStatusDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiArtifactoryConnectionDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiOwnerArtifactoryConnectionDTO;
import com.sonatype.insight.brain.artifactory.ArtifactoryMockServerRule;
import com.sonatype.insight.brain.artifactory.ArtifactoryClient;
import com.sonatype.insight.brain.artifactory.client.ArtifactoryQueryLanguageUtils;
import com.sonatype.insight.brain.artifactory.client.ChecksumType;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.artifactory.ArtifactoryConnectionDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.artifactory.ArtifactoryConnection;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.options;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.sonatype.insight.brain.artifactory.ArtifactoryClient.TEST_SHA256;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiArtifactoryConnectionResourceTest
    extends AbstractResourceTest
{
  @Rule
  public WireMockRule artifactoryMockServer = new WireMockRule(wireMockConfig().dynamicPort());

  private ArtifactoryConnectionDAO dao;

  private OrganizationDAO organizationDAO;

  private PasswordHandler pwHandler;

  private Application app;

  private Organization org;

  @Before
  public void before() {
    dao = lookup(ArtifactoryConnectionDAO.class);
    ApplicationDAO applicationDAO = lookup(ApplicationDAO.class);
    organizationDAO = lookup(OrganizationDAO.class);

    pwHandler = getCLMServer().getInstance(PasswordHandler.class);
    app = tempEntity.newApplicationWithParent();
    app.setArtifactoryConnectionEnabled(true);
    applicationDAO.update(app);
    org = tempEntity.newOrganization();
    org.setArtifactoryConnectionEnabled(true);
    organizationDAO.update(org);

    // Stub OPTIONS requests that may come from CORS preflight or health checks
    artifactoryMockServer.stubFor(options(urlEqualTo("/"))
        .willReturn(aResponse().withStatus(200)));
  }

  private void feature(boolean enable) {
    SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(enable);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.ARTIFACTORY_CONNECTION_CONFIG_PATH_V2).auth();
  }

  @Test
  public void testAddArtifactoryConnection_Organization() throws Exception {
    testAddArtifactoryConnection(org.getId(), OwnerType.ORGANIZATION);
  }

  @Test
  public void testAddArtifactoryConnection_Application() throws Exception {
    testAddArtifactoryConnection(app.getId(), OwnerType.APPLICATION);
  }

  @Test
  public void testAddArtifactoryConnection_FeatureDisabled_ByDefault() throws Exception {
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.ownerId = app.getId();
    dto.baseUrl = "http://baseurl.com";
    dto.username = "user";
    dto.password = "pass";

    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(dto)
        .post();
    assertThat(response.getStatusCode()).isEqualTo(403);
    assertThat(response.getBodyText()).isEqualTo(
        SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.getId() + " feature is disabled");
  }

  @Test
  public void testAddArtifactoryConnection_InvalidContent() throws Exception {
    feature(true);
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.ownerId = app.getId();
    dto.baseUrl = null;
    dto.username = "user";
    dto.password = "pass";

    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(dto)
        .post();
    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBodyText()).isEqualTo("missing artifactory base URL");
  }

  @Test
  public void testUpdateArtifactoryConnection_Application() throws Exception {
    testUpdateArtifactoryConnection(app.getId(), OwnerType.APPLICATION);
  }

  @Test
  public void testUpdateArtifactoryConnection_Organization() throws Exception {
    testUpdateArtifactoryConnection(org.getId(), OwnerType.ORGANIZATION);
  }

  @Test
  public void testUpdateArtifactoryConnection_FeatureDisabled() throws Exception {
    tempEntity.newArtifactoryConnection(app.getId(), "http://baseurl1.com", "user1", "pass1".toCharArray());
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.ownerId = app.getId();
    dto.baseUrl = "http://baseurl1.com";

    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_ARTIFACTORY)
        .parameter(OwnerType.APPLICATION, app.getId(), "someOtherId")
        .body(dto)
        .put();
    assertThat(response.getStatusCode()).isEqualTo(403);
    assertThat(response.getBodyText()).isEqualTo(
        SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.getId() + " feature is disabled");
  }

  @Test
  public void testDeleteArtifactoryConnection_Application() throws Exception {
    testDeleteArtifactoryConnection(app.getId(), OwnerType.APPLICATION);
  }

  @Test
  public void testDeleteArtifactoryConnection_Organization() throws Exception {
    testDeleteArtifactoryConnection(org.getId(), OwnerType.ORGANIZATION);
  }

  @Test
  public void testDeleteArtifactoryConnection_FeatureDisabled() throws Exception {
    ArtifactoryConnection existingConnection =
        tempEntity.newArtifactoryConnection(app.getId(), "http://baseurl.com", "user", "pass".toCharArray());
    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_ARTIFACTORY)
        .parameter(OwnerType.APPLICATION, app.getId(), existingConnection.getId())
        .delete();

    assertThat(response.getStatusCode()).isEqualTo(403);
    assertThat(response.getBodyText()).isEqualTo(
        SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.getId() + " feature is disabled");
  }

  @Test
  public void testDeleteArtifactoryConnection_NotFound() throws Exception {
    feature(true);
    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_ARTIFACTORY)
        .parameter(OwnerType.APPLICATION, app.getId(), "nonExistentId")
        .delete();
    assertThat(response.getStatusCode()).isEqualTo(404);
  }

  @Test
  public void testGetArtifactoryConnection_Application() throws Exception {
    testGetArtifactoryConnection(app.getId(), OwnerType.APPLICATION);
  }

  @Test
  public void testGetArtifactoryConnection_Organization() throws Exception {
    testGetArtifactoryConnection(org.getId(), OwnerType.ORGANIZATION);
  }

  @Test
  public void testGetArtifactoryConnectionByOwner_FeatureDisabled() throws Exception {
    tempEntity.newArtifactoryConnection(app.getId(), "http://baseurl.com", "user", "pass".toCharArray());
    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, app.getId())
        .get();

    assertThat(response.getStatusCode()).isEqualTo(403);
    assertThat(response.getBodyText()).isEqualTo(
        SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.getId() + " feature is disabled");
  }

  @Test
  public void testGetArtifactoryConnection_NotFound() throws Exception {
    feature(true);
    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, "nonExistentId")
        .get();
    assertThat(response.getStatusCode()).isEqualTo(404);
  }

  private void testGetArtifactoryConnection(String id, OwnerType ownerType) throws Exception {
    feature(true);
    ArtifactoryConnection conn =
        tempEntity.newArtifactoryConnection(id, "http://baseurl1.com", "user1", "pass1".toCharArray());

    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_OWNER)
        .parameter(ownerType, id)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    ApiOwnerArtifactoryConnectionDTO result = response.getBody(ApiOwnerArtifactoryConnectionDTO.class);
    assertThat(result.artifactoryConnection).isNotNull()
        .extracting("artifactoryConnectionId", "baseUrl", "username")
        .containsExactlyInAnyOrder(conn.getId(), "http://baseurl1.com", "user1");
  }

  @Test
  public void testGetArtifactoryConnection_InheritTrue() throws Exception {
    feature(true);
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    organization.setAllowArtifactoryConnectionOverride(false);
    organization.setArtifactoryConnectionEnabled(true);
    organizationDAO.update(organization);
    ArtifactoryConnection orgArtifactoryConnection =
        tempEntity.newArtifactoryConnection(
            organization.getId(), "http://baseurl2.com", "user2", "pass2".toCharArray());

    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_OWNER)
        .parameter(application.getType(), application.getId())
        .query("inherit", true)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    ApiOwnerArtifactoryConnectionDTO result = response.getBody(ApiOwnerArtifactoryConnectionDTO.class);
    assertThat(result.artifactoryConnection).isNotNull()
        .extracting("artifactoryConnectionId", "ownerId", "baseUrl", "username")
        .containsExactly(orgArtifactoryConnection.getId(), organization.getId(), "http://baseurl2.com", "user2");
  }

  @Test
  public void testGetArtifactoryConnection_InheritFalse() throws Exception {
    feature(true);
    Application application = tempEntity.newApplicationWithParent();
    String orgId = application.getParentOwnerId();
    tempEntity.newArtifactoryConnection(orgId, "http://baseurl2.com", "user2", "pass2".toCharArray());

    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_OWNER)
        .parameter(application.getType(), application.getId())
        .query("inherit", false)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    ApiOwnerArtifactoryConnectionDTO result = response.getBody(ApiOwnerArtifactoryConnectionDTO.class);
    assertThat(result.artifactoryConnection).isNull();
  }

  @Test
  public void testGetArtifactoryConnection_InheritDefault() throws Exception {
    feature(true);
    Application application = tempEntity.newApplicationWithParent();
    String orgId = application.getParentOwnerId();
    tempEntity.newArtifactoryConnection(orgId, "http://baseurl2.com", "user2", "pass2".toCharArray());

    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_OWNER)
        .parameter(application.getType(), application.getId())
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    ApiOwnerArtifactoryConnectionDTO result = response.getBody(ApiOwnerArtifactoryConnectionDTO.class);
    assertThat(result).isNotNull();
  }

  @Test
  public void testGetArtifactoryConnection() throws Exception {
    feature(true);
    Organization org = tempEntity.newOrganization();
    ArtifactoryConnection artifactoryConnection =
        tempEntity.newArtifactoryConnection(org.getId(), "http://baseurl2.com", "user2", "pass2".toCharArray());

    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_ARTIFACTORY)
        .parameter(org.getType(), org.getId(), artifactoryConnection.getId())
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    ApiArtifactoryConnectionDTO result = response.getBody(ApiArtifactoryConnectionDTO.class);
    assertThat(result.artifactoryConnectionId).isEqualTo(artifactoryConnection.getId());
  }

  @Test
  public void testGetArtifactoryConnectionByArtifactory_FeatureDisabled() throws Exception {
    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_ARTIFACTORY)
        .parameter("application", "appId", "artifactoryConnectionId")
        .get();
    assertThat(response.getStatusCode()).isEqualTo(403);
    assertThat(response.getBodyText()).isEqualTo(
        SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.getId() + " feature is disabled");
  }

  private void testDeleteArtifactoryConnection(final String id, final OwnerType ownerType) throws Exception {
    feature(true);
    ArtifactoryConnection existingConnection =
        tempEntity.newArtifactoryConnection(id, "http://baseurl.com", "user", "pass".toCharArray());

    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_ARTIFACTORY)
        .parameter(ownerType, id, existingConnection.getId())
        .delete();
    assertThat(response.getStatusCode()).isEqualTo(204);
    assertThat(dao.getById(existingConnection.getId())).isNull();
  }

  private void testUpdateArtifactoryConnection(String id, OwnerType ownerType) throws Exception {
    feature(true);

    ArtifactoryConnection existingConnection =
        tempEntity.newArtifactoryConnection(id, "http://baseurl.com", "user", "pass".toCharArray());
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.ownerId = id;
    dto.baseUrl = "http://updatedurl.com/";
    dto.username = "updateduser";
    dto.password = "updatedpass";

    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_ARTIFACTORY)
        .parameter(ownerType, id, existingConnection.getId())
        .body(dto)
        .put();
    assertThat(response.getStatusCode()).isEqualTo(200);
    ApiArtifactoryConnectionDTO responseDto = response.getBody(ApiArtifactoryConnectionDTO.class);
    assertThat(responseDto.artifactoryConnectionId).isEqualTo(existingConnection.getId());
    assertThat(responseDto.baseUrl).isEqualTo(dto.baseUrl);
    assertThat(responseDto.username).isEqualTo(dto.username);
    assertThat(responseDto.password).isNull();

    ArtifactoryConnection stored = dao.getById(existingConnection.getId());
    assertThat(Arrays.equals(pwHandler.decryptPassword(stored.getPassword()), dto.password.toCharArray())).isTrue();
  }

  private void testAddArtifactoryConnection(String id, OwnerType ownerType) throws Exception {
    feature(true);

    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.ownerId = id;
    dto.baseUrl = "http://localrepo.com/";
    dto.username = "user";
    dto.password = "pass";

    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_OWNER)
        .parameter(ownerType, id)
        .body(dto)
        .post();
    assertThat(response.getStatusCode()).isEqualTo(200);
    ApiArtifactoryConnectionDTO responseDto = response.getBody(ApiArtifactoryConnectionDTO.class);
    assertThat(responseDto.artifactoryConnectionId).isNotNull();
    assertThat(responseDto.baseUrl).isEqualTo(dto.baseUrl);
    assertThat(responseDto.username).isEqualTo(dto.username);
    assertThat(responseDto.password).isNull();

    ArtifactoryConnection stored = dao.getById(responseDto.artifactoryConnectionId);
    assertThat(Arrays.equals(pwHandler.decryptPassword(stored.getPassword()), "pass".toCharArray())).isTrue();
  }

  @Test
  public void testTestArtifactoryConnection_ViaAQL() throws Exception {
    feature(true);
    String appId = tempEntity.newApplicationWithParent().getId();
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.baseUrl = artifactoryMockServer.baseUrl();
    dto.username = "user";
    dto.password = "pass";

    artifactoryMockServer.stubFor(post(urlPathMatching(ArtifactoryClient.AQL_SEARCH_PATH))
        .withBasicAuth("user", "pass")
        .withRequestBody(
            equalTo(
                ArtifactoryQueryLanguageUtils.createChecksumSearch(
                    ChecksumType.SHA256,
                    Collections.singleton(ArtifactoryClient.TEST_SHA256),
                    Collections.emptySet())))
        .willReturn(aResponse().withStatus(200)));

    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_OWNER_TEST_PATH)
        .parameter(OwnerType.APPLICATION, appId)
        .body(dto)
        .post();
    assertThat(response.getStatusCode()).isEqualTo(200);
  }

  @Test
  public void testTestArtifactoryConnection_ViaQueryParam() throws Exception {
    feature(true);
    String appId = tempEntity.newApplicationWithParent().getId();
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.baseUrl = artifactoryMockServer.baseUrl();

    artifactoryMockServer.stubFor(get(urlPathMatching(ArtifactoryClient.CHECKSUM_SEARCH_PATH))
        .willReturn(aResponse().withStatus(200)));

    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_OWNER_TEST_PATH)
        .parameter(OwnerType.APPLICATION, appId)
        .body(dto)
        .post();
    assertThat(response.getStatusCode()).isEqualTo(200);
  }

  @Test
  public void testTestArtifactoryConnection_Unauthorized() throws Exception {
    feature(true);
    String appId = tempEntity.newApplicationWithParent().getId();
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.baseUrl = artifactoryMockServer.baseUrl();
    dto.username = "user";
    dto.password = "pass";

    artifactoryMockServer.stubFor(post(urlPathMatching(ArtifactoryClient.AQL_SEARCH_PATH))
        .withBasicAuth("user", "pass")
        .withRequestBody(
            equalTo(
                ArtifactoryQueryLanguageUtils.createChecksumSearch(
                    ChecksumType.SHA256,
                    Collections.singleton(ArtifactoryClient.TEST_SHA256),
                    Collections.emptySet())))
        .willReturn(aResponse().withHeader(ArtifactoryClient.ARTIFACTORY_ID_HEADER_NAME,
            ArtifactoryMockServerRule.ARTIFACTORY_ID_HEADER_MOCK_VALUE).withStatus(401)));

    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_OWNER_TEST_PATH)
        .parameter(OwnerType.APPLICATION, appId)
        .body(dto)
        .post();
    assertThat(response.getStatusCode()).isEqualTo(200);
    ApiStatusDTO statusDTO = response.getBody(ApiStatusDTO.class);
    assertThat(statusDTO.code).isEqualTo(401);
    assertThat(statusDTO.message).isEqualTo("Unauthorized");
  }

  @Test
  public void testTestArtifactoryConnection_FeatureDisabled() throws Exception {
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.baseUrl = artifactoryMockServer.baseUrl();

    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_OWNER_TEST_PATH)
        .parameter(OwnerType.APPLICATION, "appId")
        .body(dto)
        .post();
    assertThat(response.getStatusCode()).isEqualTo(403);
    assertThat(response.getBodyText()).isEqualTo(
        SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.getId() + " feature is disabled");
  }

  @Test
  public void testTestArtifactoryConnection_InvalidContent() throws Exception {
    feature(true);
    String appId = tempEntity.newApplicationWithParent().getId();
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();

    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_OWNER_TEST_PATH)
        .parameter(OwnerType.APPLICATION, appId)
        .body(dto)
        .post();
    assertThat(response.getStatusCode()).isEqualTo(400);
  }

  @Test
  public void testTestArtifactoryConnection_ByArtifactoryConnectionId_ViaQueryParam() throws Exception {
    feature(true);
    String appId = tempEntity.newApplicationWithParent().getId();
    ArtifactoryConnection artifactoryConnection = tempEntity.newArtifactoryConnection(
        appId, "http://baseurl.com", null, null);
    artifactoryConnection.setBaseUrl(artifactoryMockServer.baseUrl());
    dao.update(artifactoryConnection);

    artifactoryMockServer.stubFor(get(urlPathMatching(ArtifactoryClient.CHECKSUM_SEARCH_PATH))
        .withQueryParam("sha256", equalTo(TEST_SHA256))
        .willReturn(aResponse().withHeader(ArtifactoryClient.ARTIFACTORY_ID_HEADER_NAME,
            ArtifactoryMockServerRule.ARTIFACTORY_ID_HEADER_MOCK_VALUE).withStatus(200)));

    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_ARTIFACTORY_TEST_PATH)
        .parameter(OwnerType.APPLICATION, appId, artifactoryConnection.getId())
        .post();
    assertThat(response.getStatusCode()).isEqualTo(200);
    ApiStatusDTO statusDTO = response.getBody(ApiStatusDTO.class);
    assertThat(statusDTO.code).isEqualTo(200);
    assertThat(statusDTO.message).isEqualTo("OK");
  }

  @Test
  public void testTestArtifactoryConnection_ByArtifactoryConnectionId_ViaAQL() throws Exception {
    feature(true);
    String appId = tempEntity.newApplicationWithParent().getId();
    ArtifactoryConnection artifactoryConnection = tempEntity.newArtifactoryConnection(
        appId, "http://baseurl.com", "user", "pass".toCharArray());
    artifactoryConnection.setBaseUrl(artifactoryMockServer.baseUrl());
    artifactoryConnection.setPassword(pwHandler.encryptPassword(artifactoryConnection.getPassword()));
    dao.update(artifactoryConnection);

    artifactoryMockServer.stubFor(post(urlPathMatching(ArtifactoryClient.AQL_SEARCH_PATH))
        .withBasicAuth(artifactoryConnection.getUsername(),
            String.valueOf(pwHandler.decryptPassword(artifactoryConnection.getPassword())))
        .withRequestBody(
            equalTo(
                ArtifactoryQueryLanguageUtils.createChecksumSearch(
                    ChecksumType.SHA256,
                    Collections.singleton(ArtifactoryClient.TEST_SHA256),
                    Collections.emptySet())))
        .willReturn(aResponse().withHeader(ArtifactoryClient.ARTIFACTORY_ID_HEADER_NAME,
            ArtifactoryMockServerRule.ARTIFACTORY_ID_HEADER_MOCK_VALUE).withStatus(200)));

    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_ARTIFACTORY_TEST_PATH)
        .parameter(OwnerType.APPLICATION, appId, artifactoryConnection.getId())
        .post();
    assertThat(response.getStatusCode()).isEqualTo(200);
    ApiStatusDTO statusDTO = response.getBody(ApiStatusDTO.class);
    assertThat(statusDTO.code).isEqualTo(200);
    assertThat(statusDTO.message).isEqualTo("OK");
  }

  @Test
  public void testTestArtifactoryConnection_ByArtifactoryConnectionId_Unauthorized() throws Exception {
    feature(true);
    String appId = tempEntity.newApplicationWithParent().getId();
    ArtifactoryConnection artifactoryConnection = tempEntity.newArtifactoryConnection(
        appId, "http://baseurl.com", "user", "pass".toCharArray());
    artifactoryConnection.setBaseUrl(artifactoryMockServer.baseUrl());
    artifactoryConnection.setPassword(pwHandler.encryptPassword(artifactoryConnection.getPassword()));
    dao.update(artifactoryConnection);

    artifactoryMockServer.stubFor(post(urlPathMatching(ArtifactoryClient.AQL_SEARCH_PATH))
        .withBasicAuth(artifactoryConnection.getUsername(),
            String.valueOf(pwHandler.decryptPassword(artifactoryConnection.getPassword())))
        .withRequestBody(
            equalTo(
                ArtifactoryQueryLanguageUtils.createChecksumSearch(
                    ChecksumType.SHA256,
                    Collections.singleton(ArtifactoryClient.TEST_SHA256),
                    Collections.emptySet())))
        .willReturn(aResponse().withHeader(ArtifactoryClient.ARTIFACTORY_ID_HEADER_NAME,
            ArtifactoryMockServerRule.ARTIFACTORY_ID_HEADER_MOCK_VALUE).withStatus(401)));

    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_ARTIFACTORY_TEST_PATH)
        .parameter(OwnerType.APPLICATION, appId, artifactoryConnection.getId())
        .post();
    assertThat(response.getStatusCode()).isEqualTo(200);
    ApiStatusDTO statusDTO = response.getBody(ApiStatusDTO.class);
    assertThat(statusDTO.code).isEqualTo(401);
    assertThat(statusDTO.message).isEqualTo("Unauthorized");
  }

  @Test
  public void testTestArtifactoryConnection_ByArtifactoryConnectionId_FeatureDisabled() throws Exception {
    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_ARTIFACTORY_TEST_PATH)
        .parameter(OwnerType.APPLICATION, "appId", "artifactoryConnectionId")
        .post();
    assertThat(response.getStatusCode()).isEqualTo(403);
    assertThat(response.getBodyText()).isEqualTo(
        SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.getId() + " feature is disabled");
  }

  @Test
  public void testUpdateOwnerArtifactoryConnectionStatus_Application() throws Exception {
    feature(true);
    String appId = tempEntity.newApplicationWithParent().getId();
    ApiArtifactoryConnectionStatusRequestDTO dto = new ApiArtifactoryConnectionStatusRequestDTO();
    dto.allowOverride = false;
    dto.enabled = true;

    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, appId)
        .body(dto)
        .put();
    assertThat(response.getStatusCode()).isEqualTo(204);
  }

  @Test
  public void testUpdateOwnerArtifactoryConnectionStatus_Organization() throws Exception {
    feature(true);
    String orgId = tempEntity.newOrganization().getId();
    ApiArtifactoryConnectionStatusRequestDTO dto = new ApiArtifactoryConnectionStatusRequestDTO();
    dto.allowOverride = false;
    dto.enabled = true;

    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_OWNER)
        .parameter(OwnerType.ORGANIZATION, orgId)
        .body(dto)
        .put();
    assertThat(response.getStatusCode()).isEqualTo(204);
  }

  @Test
  public void testUpdateOwnerArtifactoryConnectionStatus_FeatureDisabled() throws Exception {
    ApiArtifactoryConnectionStatusRequestDTO dto = new ApiArtifactoryConnectionStatusRequestDTO();

    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, "appId")
        .body(dto)
        .put();
    assertThat(response.getStatusCode()).isEqualTo(403);
    assertThat(response.getBodyText()).isEqualTo(
        SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.getId() + " feature is disabled");
  }

  @Test
  public void testUpdateOwnerArtifactoryConnectionStatus_InvalidContent() throws Exception {
    feature(true);
    String appId = tempEntity.newApplicationWithParent().getId();

    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, appId)
        .body(null)
        .put();
    assertThat(response.getStatusCode()).isEqualTo(400);
  }
}
