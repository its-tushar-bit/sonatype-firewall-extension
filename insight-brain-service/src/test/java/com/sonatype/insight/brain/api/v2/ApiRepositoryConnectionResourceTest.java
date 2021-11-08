/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryConnectionDTO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryConnectionDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.ExperimentalFeature;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

public class ApiRepositoryConnectionResourceTest
    extends AbstractResourceTest
{
  public static final String FEATURE_FLAG = ExperimentalFeature.INNER_SOURCE_REPOSITORY_INTEGRATION.getFlag();

  private RepositoryConnectionDAO dao = new RepositoryConnectionDAO();

  private PasswordHandler pwHandler;

  private InsightConfig insightConfig;

  private Application app;

  private Organization org;

  @Before
  public void before() {
    pwHandler = getCLMServer().getInstance(PasswordHandler.class);
    insightConfig = getCLMServer().getInstance(InsightConfig.class);
    insightConfig.setExperimentalFeatures(ImmutableMap.of(FEATURE_FLAG, true));
    app = tempEntity.newApplicationWithParent();
    org = tempEntity.newOrganization();
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.REPOSITORY_CONNECTION_CONFIG_PATH_V2).auth();
  }

  @Test
  public void testAddRepositoryConnection_Organization() throws Exception {
    testAddRepositoryConnection(org.getId(), OwnerType.ORGANIZATION);
  }

  @Test
  public void testAddRepositoryConnection_Application() throws Exception {
    testAddRepositoryConnection(app.getId(), OwnerType.APPLICATION);
  }

  @Test
  public void testAddRepositoryConnection_FeatureDisabled() throws Exception {
    insightConfig.setExperimentalFeatures(ImmutableMap.of(FEATURE_FLAG, false));
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.ownerId = app.getId();
    dto.baseUrl = "http://baseurl.com";
    dto.username = "user";
    dto.password = "pass";

    HttpResponse response = restRequest().path(DefaultRepositoryConnectionResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(dto)
        .post();
    assertThat(response.getStatusCode()).isEqualTo(403);
    assertThat(response.getBodyText()).isEqualTo(FEATURE_FLAG + " feature is disabled");
  }

  @Test
  public void testAddRepositoryConnection_InvalidContent() throws Exception {
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.ownerId = app.getId();
    dto.baseUrl = null;
    dto.username = "user";
    dto.password = "pass";

    HttpResponse response = restRequest().path(DefaultRepositoryConnectionResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(dto)
        .post();
    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBodyText()).isEqualTo("missing repository base URL");
  }

  @Test
  public void testUpdateRepositoryConnection_Application() throws Exception {
    testUpdateRepositoryConnection(app.getId(), OwnerType.APPLICATION);
  }

  @Test
  public void testUpdateRepositoryConnection_Organization() throws Exception {
    testUpdateRepositoryConnection(org.getId(), OwnerType.ORGANIZATION);
  }

  @Test
  public void testUpdateRepositoryConnection_FeatureDisabled() throws Exception {
    insightConfig.setExperimentalFeatures(ImmutableMap.of(FEATURE_FLAG, false));
    tempEntity.newRepositoryConnection(app.getId(), "http://baseurl1.com", "user1", "pass1".toCharArray());
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.ownerId = app.getId();
    dto.baseUrl = "http://baseurl1.com";

    HttpResponse response = restRequest().path(DefaultRepositoryConnectionResource.BY_REPOSITORY)
        .parameter(OwnerType.APPLICATION, app.getId(), "someOtherId")
        .body(dto)
        .put();
    assertThat(response.getStatusCode()).isEqualTo(403);
    assertThat(response.getBodyText()).isEqualTo(FEATURE_FLAG + " feature is disabled");
  }

  @Test
  public void testUpdateRepositoryConnection_Conflict() throws Exception {
    tempEntity.newRepositoryConnection(app.getId(), "http://baseurl1.com", "user1", "pass1".toCharArray());

    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.ownerId = app.getId();
    dto.baseUrl = "http://baseurl1.com";
    dto.username = "user2";
    dto.password = "pass2";

    HttpResponse response = restRequest().path(DefaultRepositoryConnectionResource.BY_REPOSITORY)
        .parameter(OwnerType.APPLICATION, app.getId(), "someOtherId")
        .body(dto)
        .put();
    assertThat(response.getStatusCode()).isEqualTo(409);
    assertThat(response.getBodyText()).isEqualTo(String.format(
        "repository connection URL configuration exist for %s with id: %s", OwnerType.APPLICATION, app.getId()));
  }

  @Test
  public void testDeleteRepositoryConnection_Application() throws Exception {
    testDeleteRepositoryConnection(app.getId(), OwnerType.APPLICATION);
  }

  @Test
  public void testDeleteRepositoryConnection_Organization() throws Exception {
    testDeleteRepositoryConnection(org.getId(), OwnerType.ORGANIZATION);
  }

  @Test
  public void testDeleteRepositoryConnection_FeatureDisabled() throws Exception {
    insightConfig.setExperimentalFeatures(ImmutableMap.of(FEATURE_FLAG, false));

    RepositoryConnection existingConnection =
        tempEntity.newRepositoryConnection(app.getId(), "http://baseurl.com", "user", "pass".toCharArray());
    HttpResponse response = restRequest().path(DefaultRepositoryConnectionResource.BY_REPOSITORY)
        .parameter(OwnerType.APPLICATION, app.getId(), existingConnection.getId())
        .delete();

    assertThat(response.getStatusCode()).isEqualTo(403);
    assertThat(response.getBodyText()).isEqualTo(FEATURE_FLAG + " feature is disabled");
  }

  @Test
  public void testDeleteRepositoryConnection_NotFound() throws Exception {
    HttpResponse response = restRequest().path(DefaultRepositoryConnectionResource.BY_REPOSITORY)
        .parameter(OwnerType.APPLICATION, app.getId(), "nonExistentId")
        .delete();
    assertThat(response.getStatusCode()).isEqualTo(404);
  }

  @Test
  public void testGetRepositoryConnections_Application() throws Exception {
    testGetRepositoryConnections(app.getId(), OwnerType.APPLICATION);
  }

  @Test
  public void testGetRepositoryConnections_Organization() throws Exception {
    testGetRepositoryConnections(org.getId(), OwnerType.ORGANIZATION);
  }

  @Test
  public void testGetRepositoryConnections_FeatureDisabled() throws Exception {
    insightConfig.setExperimentalFeatures(ImmutableMap.of(FEATURE_FLAG, false));

    tempEntity.newRepositoryConnection(app.getId(), "http://baseurl.com", "user", "pass".toCharArray());
    HttpResponse response = restRequest().path(DefaultRepositoryConnectionResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, app.getId())
        .get();

    assertThat(response.getStatusCode()).isEqualTo(403);
    assertThat(response.getBodyText()).isEqualTo(FEATURE_FLAG + " feature is disabled");
  }

  @Test
  public void testGetRepositoryConnections_NotFound() throws Exception {
    HttpResponse response = restRequest().path(DefaultRepositoryConnectionResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, "nonExistentId")
        .get();
    assertThat(response.getStatusCode()).isEqualTo(404);
  }

  private void testGetRepositoryConnections(String id, OwnerType ownerType) throws Exception {
    RepositoryConnection conn1 =
        tempEntity.newRepositoryConnection(id, "http://baseurl1.com", "user1", "pass1".toCharArray());
    RepositoryConnection conn2 =
        tempEntity.newRepositoryConnection(id, "http://baseurl2.com", "user2", "pass2".toCharArray());

    HttpResponse response = restRequest().path(DefaultRepositoryConnectionResource.BY_OWNER)
        .parameter(ownerType, id)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    List<ApiRepositoryConnectionDTO> responseDtos = response.getBody(List.class);
    assertThat(responseDtos).hasSize(2)
        .extracting("repositoryConnectionId", "baseUrl", "username")
        .containsExactlyInAnyOrder(
            tuple(conn1.getId(), "http://baseurl1.com", "user1"),
            tuple(conn2.getId(), "http://baseurl2.com", "user2"));
  }

  private void testDeleteRepositoryConnection(final String id, final OwnerType ownerType) throws Exception {
    RepositoryConnection existingConnection =
        tempEntity.newRepositoryConnection(id, "http://baseurl.com", "user", "pass".toCharArray());

    HttpResponse response = restRequest().path(DefaultRepositoryConnectionResource.BY_REPOSITORY)
        .parameter(ownerType, id, existingConnection.getId())
        .delete();
    assertThat(response.getStatusCode()).isEqualTo(204);
    assertThat(dao.getById(existingConnection.getId())).isNull();
  }

  private void testUpdateRepositoryConnection(String id, OwnerType ownerType) throws Exception {
    RepositoryConnection existingConnection =
        tempEntity.newRepositoryConnection(id, "http://baseurl.com", "user", "pass".toCharArray());
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.ownerId = id;
    dto.baseUrl = "http://updatedurl.com/";
    dto.username = "updateduser";
    dto.password = "updatedpass";

    HttpResponse response = restRequest().path(DefaultRepositoryConnectionResource.BY_REPOSITORY)
        .parameter(ownerType, id, existingConnection.getId())
        .body(dto)
        .put();
    assertThat(response.getStatusCode()).isEqualTo(200);
    ApiRepositoryConnectionDTO responseDto = response.getBody(ApiRepositoryConnectionDTO.class);
    assertThat(responseDto.repositoryConnectionId).isEqualTo(existingConnection.getId());
    assertThat(responseDto.baseUrl).isEqualTo(dto.baseUrl);
    assertThat(responseDto.username).isEqualTo(dto.username);
    assertThat(responseDto.password).isNull();

    RepositoryConnection stored = dao.getById(existingConnection.getId());
    assertThat(Arrays.equals(pwHandler.decryptPassword(stored.getPassword()), dto.password.toCharArray())).isTrue();
  }

  private void testAddRepositoryConnection(String id, OwnerType ownerType) throws Exception {
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.ownerId = id;
    dto.baseUrl = "http://localrepo.com/";
    dto.username = "user";
    dto.password = "pass";

    HttpResponse response = restRequest().path(DefaultRepositoryConnectionResource.BY_OWNER)
        .parameter(ownerType, id)
        .body(dto)
        .post();
    assertThat(response.getStatusCode()).isEqualTo(200);
    ApiRepositoryConnectionDTO responseDto = response.getBody(ApiRepositoryConnectionDTO.class);
    assertThat(responseDto.repositoryConnectionId).isNotNull();
    assertThat(responseDto.baseUrl).isEqualTo(dto.baseUrl);
    assertThat(responseDto.username).isEqualTo(dto.username);
    assertThat(responseDto.password).isNull();

    RepositoryConnection stored = dao.getById(responseDto.repositoryConnectionId);
    assertThat(Arrays.equals(pwHandler.decryptPassword(stored.getPassword()), "pass".toCharArray())).isTrue();
  }
}
