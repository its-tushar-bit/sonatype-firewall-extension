/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Arrays;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiOwnerRepositoryConnectionsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryConnectionDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryConnectionStatusRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiStatusDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryConnectionDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.model.repository.RepositoryFormat;
import com.sonatype.insight.brain.repository.client.NexusRepository3Client;
import com.sonatype.insight.brain.repository.client.NexusRepository3ClientTest;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.variant.IqPostgresTest;
import com.sonatype.insight.brain.variant.IqTestContext;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.sonatype.insight.brain.repository.client.NexusRepository3Client.NXRM_STATUS_RESOURCE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * Package-scoped: touches {@link ApiRepositoryConnectionResource}'s package-private {@code BY_OWNER}/
 * {@code BY_REPOSITORY}/{@code BY_OWNER_TEST_PATH}/{@code BY_REPOSITORY_TEST_PATH} constants — so the
 * class stays in the original resource's package (see convert-resource-test-to-variant skill, Step 3).
 */
@IqPostgresTest
class IqPostgresApiRepositoryConnectionResourceTest
{
  @RegisterExtension
  static WireMockExtension nxrm3MockSever = WireMockExtension.newInstance()
      .options(wireMockConfig().dynamicPort())
      .build();

  private IqTestContext ctx;

  private RepositoryConnectionDAO dao;

  private OrganizationDAO organizationDAO;

  private ApplicationDAO applicationDAO;

  private PasswordHandler pwHandler;

  private Application app;

  private Organization org;

  @BeforeEach
  void before() {
    dao = ctx.lookup(RepositoryConnectionDAO.class);
    organizationDAO = ctx.lookup(OrganizationDAO.class);
    applicationDAO = ctx.lookup(ApplicationDAO.class);

    pwHandler = ctx.lookup(PasswordHandler.class);
    app = ctx.tempEntity().newApplicationWithParent();
    app.setRepositoryConnectionEnabled(true);
    applicationDAO.update(app);
    org = ctx.tempEntity().newOrganization();
    org.setRepositoryConnectionEnabled(true);
    organizationDAO.update(org);
  }

  @AfterEach
  void after() {
    SystemConfigurationPropertyFeature.INNER_SOURCE_REPOSITORY_INTEGRATION.setEnabled(true);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.REPOSITORY_CONNECTION_CONFIG_PATH_V2).auth();
  }

  @Test
  void testAddRepositoryConnection_Organization() throws Exception {
    testAddRepositoryConnection(org.getId(), OwnerType.ORGANIZATION);
  }

  @Test
  void testAddRepositoryConnection_Application() throws Exception {
    testAddRepositoryConnection(app.getId(), OwnerType.APPLICATION);
  }

  @Test
  void testAddRepositoryConnection_FeatureDisabled() throws Exception {
    SystemConfigurationPropertyFeature.INNER_SOURCE_REPOSITORY_INTEGRATION.setEnabled(false);
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.ownerId = app.getId();
    dto.baseUrl = "http://baseurl.com";
    dto.username = "user";
    dto.password = "pass";

    HttpResponse response = restRequest().path(ApiRepositoryConnectionResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(dto)
        .post();
    assertThat(response.getStatusCode()).isEqualTo(403);
    assertThat(response.getBodyText()).isEqualTo(
        SystemConfigurationPropertyFeature.INNER_SOURCE_REPOSITORY_INTEGRATION.getId() + " feature is disabled");
  }

  @Test
  void testAddRepositoryConnection_InvalidContent() throws Exception {
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.ownerId = app.getId();
    dto.baseUrl = null;
    dto.username = "user";
    dto.password = "pass";

    HttpResponse response = restRequest().path(ApiRepositoryConnectionResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(dto)
        .post();
    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBodyText()).isEqualTo("missing repository base URL");
  }

  @Test
  void testUpdateRepositoryConnection_Application() throws Exception {
    testUpdateRepositoryConnection(app.getId(), OwnerType.APPLICATION);
  }

  @Test
  void testUpdateRepositoryConnection_Organization() throws Exception {
    testUpdateRepositoryConnection(org.getId(), OwnerType.ORGANIZATION);
  }

  @Test
  void testUpdateRepositoryConnection_FeatureDisabled() throws Exception {
    SystemConfigurationPropertyFeature.INNER_SOURCE_REPOSITORY_INTEGRATION.setEnabled(false);
    ctx.tempEntity().newRepositoryConnection(app.getId(), "http://baseurl1.com", "user1", "pass1".toCharArray());
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.ownerId = app.getId();
    dto.baseUrl = "http://baseurl1.com";

    HttpResponse response = restRequest().path(ApiRepositoryConnectionResource.BY_REPOSITORY)
        .parameter(OwnerType.APPLICATION, app.getId(), "someOtherId")
        .body(dto)
        .put();
    assertThat(response.getStatusCode()).isEqualTo(403);
    assertThat(response.getBodyText()).isEqualTo(
        SystemConfigurationPropertyFeature.INNER_SOURCE_REPOSITORY_INTEGRATION.getId() + " feature is disabled");
  }

  @Test
  void testUpdateRepositoryConnection_Conflict() throws Exception {
    ctx.tempEntity().newRepositoryConnection(app.getId(), "http://baseurl1.com", "user1", "pass1".toCharArray());
    RepositoryConnection toUpdate =
        ctx.tempEntity()
            .newRepositoryConnection(app.getId(), "http://baseurl1.com", RepositoryFormat.MAVEN, "user1",
                "pass1".toCharArray());

    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.ownerId = toUpdate.getOwnerId();
    dto.baseUrl = toUpdate.getBaseUrl();
    dto.format = RepositoryFormat.GENERIC;
    dto.username = toUpdate.getUsername();
    dto.password = String.valueOf(toUpdate.getPassword());

    HttpResponse response = restRequest().path(ApiRepositoryConnectionResource.BY_REPOSITORY)
        .parameter(OwnerType.APPLICATION, app.getId(), toUpdate.getId())
        .body(dto)
        .put();
    assertThat(response.getStatusCode()).isEqualTo(409);
    assertThat(response.getBodyText()).isEqualTo(
        String.format("repository connection format %s configuration exists for %s with id: %s",
            RepositoryFormat.GENERIC, OwnerType.APPLICATION, app.getId()));
  }

  @Test
  void testDeleteRepositoryConnection_Application() throws Exception {
    testDeleteRepositoryConnection(app.getId(), OwnerType.APPLICATION);
  }

  @Test
  void testDeleteRepositoryConnection_Organization() throws Exception {
    testDeleteRepositoryConnection(org.getId(), OwnerType.ORGANIZATION);
  }

  @Test
  void testDeleteRepositoryConnection_FeatureDisabled() throws Exception {
    SystemConfigurationPropertyFeature.INNER_SOURCE_REPOSITORY_INTEGRATION.setEnabled(false);

    RepositoryConnection existingConnection =
        ctx.tempEntity().newRepositoryConnection(app.getId(), "http://baseurl.com", "user", "pass".toCharArray());
    HttpResponse response = restRequest().path(ApiRepositoryConnectionResource.BY_REPOSITORY)
        .parameter(OwnerType.APPLICATION, app.getId(), existingConnection.getId())
        .delete();

    assertThat(response.getStatusCode()).isEqualTo(403);
    assertThat(response.getBodyText()).isEqualTo(
        SystemConfigurationPropertyFeature.INNER_SOURCE_REPOSITORY_INTEGRATION.getId() + " feature is disabled");
  }

  @Test
  void testDeleteRepositoryConnection_NotFound() throws Exception {
    HttpResponse response = restRequest().path(ApiRepositoryConnectionResource.BY_REPOSITORY)
        .parameter(OwnerType.APPLICATION, app.getId(), "nonExistentId")
        .delete();
    assertThat(response.getStatusCode()).isEqualTo(404);
  }

  @Test
  void testGetRepositoryConnections_Application() throws Exception {
    testGetRepositoryConnections(app.getId(), OwnerType.APPLICATION);
  }

  @Test
  void testGetRepositoryConnections_Organization() throws Exception {
    testGetRepositoryConnections(org.getId(), OwnerType.ORGANIZATION);
  }

  @Test
  void testGetRepositoryConnections_FeatureDisabled() throws Exception {
    SystemConfigurationPropertyFeature.INNER_SOURCE_REPOSITORY_INTEGRATION.setEnabled(false);

    ctx.tempEntity().newRepositoryConnection(app.getId(), "http://baseurl.com", "user", "pass".toCharArray());
    HttpResponse response = restRequest().path(ApiRepositoryConnectionResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, app.getId())
        .get();

    assertThat(response.getStatusCode()).isEqualTo(403);
    assertThat(response.getBodyText()).isEqualTo(
        SystemConfigurationPropertyFeature.INNER_SOURCE_REPOSITORY_INTEGRATION.getId() + " feature is disabled");
  }

  @Test
  void testGetRepositoryConnections_NotFound() throws Exception {
    HttpResponse response = restRequest().path(ApiRepositoryConnectionResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, "nonExistentId")
        .get();
    assertThat(response.getStatusCode()).isEqualTo(404);
  }

  private void testGetRepositoryConnections(String id, OwnerType ownerType) throws Exception {
    RepositoryConnection conn1 =
        ctx.tempEntity().newRepositoryConnection(id, "http://baseurl1.com", "user1", "pass1".toCharArray());
    RepositoryConnection conn2 =
        ctx.tempEntity()
            .newRepositoryConnection(id, "http://baseurl2.com", RepositoryFormat.MAVEN, "user2",
                "pass2".toCharArray());

    HttpResponse response = restRequest().path(ApiRepositoryConnectionResource.BY_OWNER)
        .parameter(ownerType, id)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    ApiOwnerRepositoryConnectionsDTO result = response.getBody(ApiOwnerRepositoryConnectionsDTO.class);
    assertThat(result.repositoryConnections).hasSize(2)
        .extracting("repositoryConnectionId", "baseUrl", "username")
        .containsExactlyInAnyOrder(
            tuple(conn1.getId(), "http://baseurl1.com", "user1"),
            tuple(conn2.getId(), "http://baseurl2.com", "user2"));
  }

  @Test
  void testGetRepositoryConnections_InheritTrue() throws Exception {
    Organization organization = ctx.tempEntity().newOrganization();
    Application application = ctx.tempEntity().newApplication(organization.getId());
    organization.setAllowRepositoryConnectionOverride(false);
    organization.setRepositoryConnectionEnabled(true);
    organizationDAO.update(organization);
    RepositoryConnection orgRepositoryConnection =
        ctx.tempEntity()
            .newRepositoryConnection(organization.getId(), "http://baseurl2.com", "user2",
                "pass2".toCharArray());

    HttpResponse response = restRequest().path(ApiRepositoryConnectionResource.BY_OWNER)
        .parameter(application.getType(), application.getId())
        .query("inherit", true)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    ApiOwnerRepositoryConnectionsDTO result = response.getBody(ApiOwnerRepositoryConnectionsDTO.class);
    assertThat(result.repositoryConnections).hasSize(1)
        .extracting("repositoryConnectionId", "ownerId", "baseUrl", "username")
        .containsExactly(tuple(orgRepositoryConnection.getId(), organization.getId(), "http://baseurl2.com",
            "user2"));
  }

  @Test
  void testGetRepositoryConnections_InheritFalse() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    String orgId = application.getParentOwnerId();
    ctx.tempEntity().newRepositoryConnection(orgId, "http://baseurl2.com", "user2", "pass2".toCharArray());

    HttpResponse response = restRequest().path(ApiRepositoryConnectionResource.BY_OWNER)
        .parameter(application.getType(), application.getId())
        .query("inherit", false)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    ApiOwnerRepositoryConnectionsDTO result = response.getBody(ApiOwnerRepositoryConnectionsDTO.class);
    assertThat(result.repositoryConnections).isEmpty();
  }

  @Test
  void testGetRepositoryConnections_InheritDefault() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    String orgId = application.getParentOwnerId();
    ctx.tempEntity().newRepositoryConnection(orgId, "http://baseurl2.com", "user2", "pass2".toCharArray());

    HttpResponse response = restRequest().path(ApiRepositoryConnectionResource.BY_OWNER)
        .parameter(application.getType(), application.getId())
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    ApiOwnerRepositoryConnectionsDTO result = response.getBody(ApiOwnerRepositoryConnectionsDTO.class);
    assertThat(result).isNotNull();
  }

  @Test
  void testGetRepositoryConnection() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    RepositoryConnection repositoryConnection =
        ctx.tempEntity().newRepositoryConnection(org.getId(), "http://baseurl2.com", "user2", "pass2".toCharArray());

    HttpResponse response = restRequest().path(ApiRepositoryConnectionResource.BY_REPOSITORY)
        .parameter(org.getType(), org.getId(), repositoryConnection.getId())
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    ApiRepositoryConnectionDTO result = response.getBody(ApiRepositoryConnectionDTO.class);
    assertThat(result.repositoryConnectionId).isEqualTo(repositoryConnection.getId());
  }

  @Test
  void testGetRepositoryConnection_FeatureDisabled() throws Exception {
    SystemConfigurationPropertyFeature.INNER_SOURCE_REPOSITORY_INTEGRATION.setEnabled(false);

    HttpResponse response = restRequest().path(ApiRepositoryConnectionResource.BY_REPOSITORY)
        .parameter("application", "appId", "repositoryConnectionId")
        .get();
    assertThat(response.getStatusCode()).isEqualTo(403);
    assertThat(response.getBodyText()).isEqualTo(
        SystemConfigurationPropertyFeature.INNER_SOURCE_REPOSITORY_INTEGRATION.getId() + " feature is disabled");
  }

  private void testDeleteRepositoryConnection(final String id, final OwnerType ownerType) throws Exception {
    RepositoryConnection existingConnection =
        ctx.tempEntity().newRepositoryConnection(id, "http://baseurl.com", "user", "pass".toCharArray());

    HttpResponse response = restRequest().path(ApiRepositoryConnectionResource.BY_REPOSITORY)
        .parameter(ownerType, id, existingConnection.getId())
        .delete();
    assertThat(response.getStatusCode()).isEqualTo(204);
    assertThat(dao.getById(existingConnection.getId())).isNull();
  }

  private void testUpdateRepositoryConnection(String id, OwnerType ownerType) throws Exception {
    RepositoryConnection existingConnection =
        ctx.tempEntity().newRepositoryConnection(id, "http://baseurl.com", "user", "pass".toCharArray());
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.ownerId = id;
    dto.baseUrl = "http://updatedurl.com/";
    dto.format = RepositoryFormat.MAVEN;
    dto.username = "updateduser";
    dto.password = "updatedpass";

    HttpResponse response = restRequest().path(ApiRepositoryConnectionResource.BY_REPOSITORY)
        .parameter(ownerType, id, existingConnection.getId())
        .body(dto)
        .put();
    assertThat(response.getStatusCode()).isEqualTo(200);
    ApiRepositoryConnectionDTO responseDto = response.getBody(ApiRepositoryConnectionDTO.class);
    assertThat(responseDto.repositoryConnectionId).isEqualTo(existingConnection.getId());
    assertThat(responseDto.baseUrl).isEqualTo(dto.baseUrl);
    assertThat(responseDto.format).isEqualTo(dto.format);
    assertThat(responseDto.username).isEqualTo(dto.username);
    assertThat(responseDto.password).isNull();

    RepositoryConnection stored = dao.getById(existingConnection.getId());
    assertThat(Arrays.equals(pwHandler.decryptPassword(stored.getPassword()), dto.password.toCharArray())).isTrue();
  }

  private void testAddRepositoryConnection(String id, OwnerType ownerType) throws Exception {
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.ownerId = id;
    dto.baseUrl = "http://localrepo.com/";
    dto.format = RepositoryFormat.MAVEN;
    dto.username = "user";
    dto.password = "pass";

    HttpResponse response = restRequest().path(ApiRepositoryConnectionResource.BY_OWNER)
        .parameter(ownerType, id)
        .body(dto)
        .post();
    assertThat(response.getStatusCode()).isEqualTo(200);
    ApiRepositoryConnectionDTO responseDto = response.getBody(ApiRepositoryConnectionDTO.class);
    assertThat(responseDto.repositoryConnectionId).isNotNull();
    assertThat(responseDto.baseUrl).isEqualTo(dto.baseUrl);
    assertThat(responseDto.format).isEqualTo(dto.format);
    assertThat(responseDto.username).isEqualTo(dto.username);
    assertThat(responseDto.password).isNull();

    RepositoryConnection stored = dao.getById(responseDto.repositoryConnectionId);
    assertThat(Arrays.equals(pwHandler.decryptPassword(stored.getPassword()), "pass".toCharArray())).isTrue();
  }

  @Test
  void testTestRepositoryConnection() throws Exception {
    String appId = ctx.tempEntity().newApplicationWithParent().getId();
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.baseUrl = nxrm3MockSever.baseUrl();
    dto.username = "user";
    dto.password = "pass";

    nxrm3MockSever.stubFor(get(urlPathMatching(NXRM_STATUS_RESOURCE))
        .withBasicAuth("user", "pass")
        .willReturn(aResponse().withHeader(NexusRepository3Client.NXRM_VERSION_HEADER_NAME,
            NexusRepository3ClientTest.NXRM_VERSION_HEADER_MOCK_VALUE).withStatus(200)));

    HttpResponse response = restRequest().path(ApiRepositoryConnectionResource.BY_OWNER_TEST_PATH)
        .parameter(OwnerType.APPLICATION, appId)
        .body(dto)
        .post();
    assertThat(response.getStatusCode()).isEqualTo(200);
  }

  @Test
  void testTestRepositoryConnection_Unauthorized() throws Exception {
    String appId = ctx.tempEntity().newApplicationWithParent().getId();
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.baseUrl = nxrm3MockSever.baseUrl();
    dto.username = "user";
    dto.password = "pass";

    nxrm3MockSever.stubFor(get(urlPathMatching(NXRM_STATUS_RESOURCE))
        .withBasicAuth("user", "pass")
        .willReturn(aResponse().withHeader(NexusRepository3Client.NXRM_VERSION_HEADER_NAME,
            NexusRepository3ClientTest.NXRM_VERSION_HEADER_MOCK_VALUE).withStatus(401)));

    HttpResponse response = restRequest().path(ApiRepositoryConnectionResource.BY_OWNER_TEST_PATH)
        .parameter(OwnerType.APPLICATION, appId)
        .body(dto)
        .post();
    assertThat(response.getStatusCode()).isEqualTo(200);
    ApiStatusDTO statusDTO = response.getBody(ApiStatusDTO.class);
    assertThat(statusDTO.code).isEqualTo(401);
    assertThat(statusDTO.message).isEqualTo("Unauthorized");
  }

  @Test
  void testTestRepositoryConnection_FeatureDisabled() throws Exception {
    SystemConfigurationPropertyFeature.INNER_SOURCE_REPOSITORY_INTEGRATION.setEnabled(false);

    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.baseUrl = nxrm3MockSever.baseUrl();

    HttpResponse response = restRequest().path(ApiRepositoryConnectionResource.BY_OWNER_TEST_PATH)
        .parameter(OwnerType.APPLICATION, "appId")
        .body(dto)
        .post();
    assertThat(response.getStatusCode()).isEqualTo(403);
    assertThat(response.getBodyText()).isEqualTo(
        SystemConfigurationPropertyFeature.INNER_SOURCE_REPOSITORY_INTEGRATION.getId() + " feature is disabled");
  }

  @Test
  void testTestRepositoryConnection_InvalidContent() throws Exception {
    String appId = ctx.tempEntity().newApplicationWithParent().getId();
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();

    HttpResponse response = restRequest().path(ApiRepositoryConnectionResource.BY_OWNER_TEST_PATH)
        .parameter(OwnerType.APPLICATION, appId)
        .body(dto)
        .post();
    assertThat(response.getStatusCode()).isEqualTo(400);
  }

  @Test
  void testTestRepositoryConnection_ByRepositoryConnectionId() throws Exception {
    String appId = ctx.tempEntity().newApplicationWithParent().getId();
    RepositoryConnection repositoryConnection = ctx.tempEntity().newRepositoryConnection(appId);
    repositoryConnection.setBaseUrl(nxrm3MockSever.baseUrl());
    repositoryConnection.setPassword(pwHandler.encryptPassword(repositoryConnection.getPassword()));
    dao.update(repositoryConnection);

    nxrm3MockSever.stubFor(get(urlPathMatching(NXRM_STATUS_RESOURCE))
        .withBasicAuth(repositoryConnection.getUsername(),
            String.valueOf(pwHandler.decryptPassword(repositoryConnection.getPassword())))
        .willReturn(aResponse().withHeader(NexusRepository3Client.NXRM_VERSION_HEADER_NAME,
            NexusRepository3ClientTest.NXRM_VERSION_HEADER_MOCK_VALUE).withStatus(200)));

    HttpResponse response = restRequest().path(ApiRepositoryConnectionResource.BY_REPOSITORY_TEST_PATH)
        .parameter(OwnerType.APPLICATION, appId, repositoryConnection.getId())
        .post();
    assertThat(response.getStatusCode()).isEqualTo(200);
    ApiStatusDTO statusDTO = response.getBody(ApiStatusDTO.class);
    assertThat(statusDTO.code).isEqualTo(200);
    assertThat(statusDTO.message).isEqualTo("OK");
  }

  @Test
  void testTestRepositoryConnection_ByRepositoryConnectionId_Unauthorized() throws Exception {
    String appId = ctx.tempEntity().newApplicationWithParent().getId();
    RepositoryConnection repositoryConnection = ctx.tempEntity().newRepositoryConnection(appId);
    repositoryConnection.setBaseUrl(nxrm3MockSever.baseUrl());
    repositoryConnection.setPassword(pwHandler.encryptPassword(repositoryConnection.getPassword()));
    dao.update(repositoryConnection);

    nxrm3MockSever.stubFor(get(urlPathMatching(NXRM_STATUS_RESOURCE))
        .withBasicAuth(repositoryConnection.getUsername(),
            String.valueOf(pwHandler.decryptPassword(repositoryConnection.getPassword())))
        .willReturn(aResponse().withHeader(NexusRepository3Client.NXRM_VERSION_HEADER_NAME,
            NexusRepository3ClientTest.NXRM_VERSION_HEADER_MOCK_VALUE).withStatus(401)));

    HttpResponse response = restRequest().path(ApiRepositoryConnectionResource.BY_REPOSITORY_TEST_PATH)
        .parameter(OwnerType.APPLICATION, appId, repositoryConnection.getId())
        .post();
    assertThat(response.getStatusCode()).isEqualTo(200);
    ApiStatusDTO statusDTO = response.getBody(ApiStatusDTO.class);
    assertThat(statusDTO.code).isEqualTo(401);
    assertThat(statusDTO.message).isEqualTo("Unauthorized");
  }

  @Test
  void testTestRepositoryConnection_ByRepositoryConnectionId_FeatureDisabled() throws Exception {
    SystemConfigurationPropertyFeature.INNER_SOURCE_REPOSITORY_INTEGRATION.setEnabled(false);

    HttpResponse response = restRequest().path(ApiRepositoryConnectionResource.BY_REPOSITORY_TEST_PATH)
        .parameter(OwnerType.APPLICATION, "appId", "repositoryConnectionId")
        .post();
    assertThat(response.getStatusCode()).isEqualTo(403);
    assertThat(response.getBodyText()).isEqualTo(
        SystemConfigurationPropertyFeature.INNER_SOURCE_REPOSITORY_INTEGRATION.getId() + " feature is disabled");
  }

  @Test
  void testUpdateOwnerRepositoryConnectionStatus_Application() throws Exception {
    String appId = ctx.tempEntity().newApplicationWithParent().getId();
    ApiRepositoryConnectionStatusRequestDTO dto = new ApiRepositoryConnectionStatusRequestDTO();
    dto.allowOverride = false;
    dto.enabled = true;

    HttpResponse response = restRequest().path(ApiRepositoryConnectionResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, appId)
        .body(dto)
        .put();
    assertThat(response.getStatusCode()).isEqualTo(204);
  }

  @Test
  void testUpdateOwnerRepositoryConnectionStatus_Organization() throws Exception {
    String orgId = ctx.tempEntity().newOrganization().getId();
    ApiRepositoryConnectionStatusRequestDTO dto = new ApiRepositoryConnectionStatusRequestDTO();
    dto.allowOverride = false;
    dto.enabled = true;

    HttpResponse response = restRequest().path(ApiRepositoryConnectionResource.BY_OWNER)
        .parameter(OwnerType.ORGANIZATION, orgId)
        .body(dto)
        .put();
    assertThat(response.getStatusCode()).isEqualTo(204);
  }

  @Test
  void testUpdateOwnerRepositoryConnectionStatus_FeatureDisabled() throws Exception {
    SystemConfigurationPropertyFeature.INNER_SOURCE_REPOSITORY_INTEGRATION.setEnabled(false);
    ApiRepositoryConnectionStatusRequestDTO dto = new ApiRepositoryConnectionStatusRequestDTO();

    HttpResponse response = restRequest().path(ApiRepositoryConnectionResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, "appId")
        .body(dto)
        .put();
    assertThat(response.getStatusCode()).isEqualTo(403);
    assertThat(response.getBodyText()).isEqualTo(
        SystemConfigurationPropertyFeature.INNER_SOURCE_REPOSITORY_INTEGRATION.getId() + " feature is disabled");
  }

  @Test
  void testUpdateOwnerRepositoryConnectionStatus_InvalidContent() throws Exception {
    String appId = ctx.tempEntity().newApplicationWithParent().getId();

    HttpResponse response = restRequest().path(ApiRepositoryConnectionResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, appId)
        .body(null)
        .put();
    assertThat(response.getStatusCode()).isEqualTo(400);
  }
}
