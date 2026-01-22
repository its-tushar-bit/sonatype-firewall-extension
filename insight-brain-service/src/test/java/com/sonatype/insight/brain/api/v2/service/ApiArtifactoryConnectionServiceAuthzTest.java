/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response.StatusType;

import com.sonatype.insight.brain.api.v2.dto.legal.ApiArtifactoryConnectionDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiOwnerArtifactoryConnectionDTO;
import com.sonatype.insight.brain.artifactory.ArtifactoryClient;
import com.sonatype.insight.brain.artifactory.ArtifactoryClientFactory;
import com.sonatype.insight.brain.artifactory.ArtifactoryClientFactory.ArtifactoryClientBuilder;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.artifactory.ArtifactoryConnectionDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.artifactory.ArtifactoryConnection;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.inject.Binder;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ApiArtifactoryConnectionServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiArtifactoryConnectionService artifactoryConnectionService;

  @Inject
  private ArtifactoryConnectionDAO dao;

  @Inject
  private ApplicationDAO applicationDAO;

  @Mock
  private ArtifactoryClientFactory mockFactory;

  @Mock
  private ArtifactoryClientBuilder mockBuilder;

  @Mock
  private ArtifactoryClient client;

  @Override
  public void configure(final Binder binder) {
    binder.bind(ArtifactoryClientFactory.class).toInstance(mockFactory);
    super.configure(binder);
  }

  @Before
  public void before() {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());
    app.setArtifactoryConnectionEnabled(true);
    applicationDAO.update(app);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetArtifactoryConnection_Unauthenticated() {
    artifactoryConnectionService.getArtifactoryConnection(OwnerType.ORGANIZATION, org.getId(), null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetArtifactoryConnection_Unauthorized() {
    login();
    artifactoryConnectionService.getArtifactoryConnection(OwnerType.ORGANIZATION, org.getId(), null);
  }

  @Test(expected = NotFoundException.class)
  public void testGetArtifactoryConnection_Authorized() {
    grantReadPermission(org.getId());
    artifactoryConnectionService.getArtifactoryConnection(OwnerType.ORGANIZATION, org.getId(), "doesNotExist");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetOwnerArtifactoryConnection_Unauthenticated() {
    artifactoryConnectionService.getOwnerArtifactoryConnection(OwnerType.ORGANIZATION, org.getId(), false);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetOwnerArtifactoryConnection_Unauthorized() {
    login();
    artifactoryConnectionService.getOwnerArtifactoryConnection(OwnerType.ORGANIZATION, org.getId(), false);
  }

  @Test
  public void testGetOwnerArtifactoryConnection_Authorized() {
    grantGlobalPermission(Permission.READ);
    assertGetOwnerArtifactoryConnection();
  }

  @Test
  public void testGetArtifactoryConnection_Authorized_ByOwner() {
    grantReadPermission(app.getId());
    assertGetOwnerArtifactoryConnection();
  }

  private void assertGetOwnerArtifactoryConnection() {
    tempEntity.newArtifactoryConnection(app.getId(), "url1", "user1", "pass1".toCharArray());
    ApiOwnerArtifactoryConnectionDTO result =
        artifactoryConnectionService.getOwnerArtifactoryConnection(OwnerType.APPLICATION, app.getId(), false);
    assertThat(result.artifactoryConnection).isNotNull();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddArtifactoryConnection_Unauthenticated() {
    testAddArtifactoryConnection();
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddArtifactoryConnection_Unauthorized() {
    login();
    testAddArtifactoryConnection();
  }

  @Test
  public void testAddArtifactoryConnection_Authorized() {
    grantWritePermission(org.getId());
    testAddArtifactoryConnection();
  }

  private void testAddArtifactoryConnection() {
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.baseUrl = "baseUrl";
    dto.username = "user";
    dto.password = "pass";

    ApiArtifactoryConnectionDTO createdDto =
        artifactoryConnectionService.addArtifactoryConnection(OwnerType.ORGANIZATION, org.getId(), dto);

    assertThat(createdDto.artifactoryConnectionId).isNotNull();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdateArtifactoryConnection_Unauthenticated() {
    testUpdateArtifactoryConnection();
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdateArtifactoryConnection_Unauthorized() {
    login();
    testUpdateArtifactoryConnection();
  }

  @Test
  public void testUpdateArtifactoryConnection_Authorized() {
    grantWritePermission(org.getId());
    testUpdateArtifactoryConnection();
  }

  private void testUpdateArtifactoryConnection() {
    ArtifactoryConnection existingConnection =
        tempEntity.newArtifactoryConnection(org.getId(), "baseUrl", "user", "pass".toCharArray());
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.baseUrl = "updated baseUrl";
    dto.username = "user";
    dto.password = "pass";

    artifactoryConnectionService.updateArtifactoryConnection(OwnerType.ORGANIZATION, org.getId(),
        existingConnection.getId(), dto);

    existingConnection = dao.getById(existingConnection.getId());
    assertThat(existingConnection.getBaseUrl()).isEqualTo(dto.baseUrl);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteArtifactoryConnection_Unauthenticated() {
    testDeleteArtifactoryConnection();
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteArtifactoryConnection_Unauthorized() {
    login();
    testDeleteArtifactoryConnection();
  }

  @Test
  public void testDeleteArtifactoryConnection_Authorized() {
    grantWritePermission(app.getId());
    testDeleteArtifactoryConnection();
  }

  private void testDeleteArtifactoryConnection() {
    ArtifactoryConnection connection =
        tempEntity.newArtifactoryConnection(app.getId(), "baseUrl", "user", "pass".toCharArray());

    artifactoryConnectionService.deleteArtifactoryConnection(OwnerType.APPLICATION, app.getId(), connection.getId());
    assertThat(dao.getById(connection.getId())).isNull();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testTestArtifactoryConnection_Unauthenticated() {
    testTestArtifactoryConnection();
  }

  @Test(expected = UnauthorizedException.class)
  public void testTestArtifactoryConnection_Unauthorized() {
    login();
    testTestArtifactoryConnection();
  }

  @Test
  public void testTestArtifactoryConnection_Authorized() throws Exception {
    grantReadPermission(app.getId());
    setupMocks();
    when(client.getServerStatusViaAQL()).thenReturn(Status.OK);

    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.baseUrl = "baseUrl";
    dto.username = "user";
    dto.password = "pass";
    StatusType status = artifactoryConnectionService.testArtifactoryConnection(OwnerType.APPLICATION, app.getId(), dto);

    assertThat(status).isEqualTo(Status.OK);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdateOwnerArtifactoryConnectionStatus_Unauthenticated() {
    artifactoryConnectionService.updateOwnerArtifactoryConnectionStatus(OwnerType.APPLICATION, app.getId(), null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdateOwnerArtifactoryConnectionStatus_Unauthorized() {
    login();
    artifactoryConnectionService.updateOwnerArtifactoryConnectionStatus(OwnerType.APPLICATION, app.getId(), null);
  }

  @Test(expected = BadRequestException.class)
  public void testUpdateOwnerArtifactoryConnectionStatus_Authorized() {
    grantWritePermission(app.getId());
    artifactoryConnectionService.updateOwnerArtifactoryConnectionStatus(OwnerType.APPLICATION, app.getId(), null);
  }

  private void testTestArtifactoryConnection() {
    String appId = tempEntity.newApplicationWithParent().getId();
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.baseUrl = "baseUrl";
    artifactoryConnectionService.testArtifactoryConnection(OwnerType.APPLICATION, appId, dto);
  }

  private void setupMocks() {
    when(mockFactory.create()).thenReturn(mockBuilder);
    when(mockBuilder.forArtifactory(any(), any(), any())).thenReturn(client);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testTestArtifactoryConnection_ByArtifactoryConnectionId_Unauthenticated() {
    artifactoryConnectionService.testArtifactoryConnection(app.getType(), app.getId(), (String) null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testTestArtifactoryConnection_ByArtifactoryConnectionId_Unauthorized() {
    login();
    artifactoryConnectionService.testArtifactoryConnection(app.getType(), app.getId(), (String) null);
  }

  @Test(expected = NotFoundException.class)
  public void testTestArtifactoryConnection_ByArtifactoryConnectionId_Authorized() {
    grantReadPermission(app.getId());
    artifactoryConnectionService.testArtifactoryConnection(app.getType(), app.getId(), "doesNotExist");
  }
}
