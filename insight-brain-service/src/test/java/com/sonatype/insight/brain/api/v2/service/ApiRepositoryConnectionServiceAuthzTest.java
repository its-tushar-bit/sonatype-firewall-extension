/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.api.v2.dto.ApiOwnerRepositoryConnectionsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryConnectionDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryConnectionDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.repository.RepositoryClient;
import com.sonatype.insight.brain.repository.client.RepositoryClientFactory;
import com.sonatype.insight.brain.repository.client.RepositoryClientFactory.RepositoryClientBuilder;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response.StatusType;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ApiRepositoryConnectionServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiRepositoryConnectionService repositoryConnectionService;

  @Inject
  private RepositoryConnectionDAO dao;

  @Inject
  private ApplicationDAO applicationDAO;

  @Mock
  private RepositoryClientFactory mockFactory;

  @Mock
  private RepositoryClientBuilder mockBuilder;

  @Mock
  private RepositoryClient client;

  @Before
  public void before() {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());
    app.setRepositoryConnectionEnabled(true);
    applicationDAO.update(app);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetRepositoryConnection_Unauthenticated() {
    repositoryConnectionService.getRepositoryConnection(OwnerType.ORGANIZATION, org.getId(), null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetRepositoryConnection_Unauthorized() {
    login();
    repositoryConnectionService.getRepositoryConnection(OwnerType.ORGANIZATION, org.getId(), null);
  }

  @Test(expected = NotFoundException.class)
  public void testGetRepositoryConnection_Authorized() {
    grantReadPermission(org.getId());
    repositoryConnectionService.getRepositoryConnection(OwnerType.ORGANIZATION, org.getId(), "doesNotExist");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetOwnerRepositoryConnections_Unauthenticated() {
    repositoryConnectionService.getOwnerRepositoryConnections(OwnerType.ORGANIZATION, org.getId(), false);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetOwnerRepositoryConnections_Unauthorized() {
    login();
    repositoryConnectionService.getOwnerRepositoryConnections(OwnerType.ORGANIZATION, org.getId(), false);
  }

  @Test
  public void testGetOwnerRepositoryConnections_Authorized() {
    grantGlobalPermission(Permission.READ);
    assertGetOwnerRepositoryConnections();
  }

  @Test
  public void testGetRepositoryConnections_Authorized_ByOwner() {
    grantReadPermission(app.getId());
    assertGetOwnerRepositoryConnections();
  }

  private void assertGetOwnerRepositoryConnections() {
    tempEntity.newRepositoryConnection(app.getId(), "url1", "user1", "pass1".toCharArray());
    ApiOwnerRepositoryConnectionsDTO result =
        repositoryConnectionService.getOwnerRepositoryConnections(OwnerType.APPLICATION, app.getId(), false);
    assertThat(result.repositoryConnections).hasSize(1);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddRepositoryConnection_Unauthenticated() {
    testAddRepositoryConnection();
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddRepositoryConnection_Unauthorized() {
    login();
    testAddRepositoryConnection();
  }

  @Test
  public void testAddRepositoryConnection_Authorized() {
    grantWritePermission(org.getId());
    testAddRepositoryConnection();
  }

  private void testAddRepositoryConnection() {
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.baseUrl = "baseUrl";
    dto.username = "user";
    dto.password = "pass";

    ApiRepositoryConnectionDTO createdDto =
        repositoryConnectionService.addRepositoryConnection(OwnerType.ORGANIZATION, org.getId(), dto);

    assertThat(createdDto.repositoryConnectionId).isNotNull();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdateRepositoryConnection_Unauthenticated() {
    testUpdateRepositoryConnection();
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdateRepositoryConnection_Unauthorized() {
    login();
    testUpdateRepositoryConnection();
  }

  @Test
  public void testUpdateRepositoryConnection_Authorized() {
    grantWritePermission(org.getId());
    testUpdateRepositoryConnection();
  }

  private void testUpdateRepositoryConnection() {
    RepositoryConnection existingConnection =
        tempEntity.newRepositoryConnection(org.getId(), "baseUrl", "user", "pass".toCharArray());
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.baseUrl = "updated baseUrl";
    dto.username = "user";
    dto.password = "pass";

    repositoryConnectionService.updateRepositoryConnection(OwnerType.ORGANIZATION, org.getId(),
        existingConnection.getId(), dto);

    existingConnection = dao.getById(existingConnection.getId());
    assertThat(existingConnection.getBaseUrl()).isEqualTo(dto.baseUrl);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteRepositoryConnection_Unauthenticated() {
    testDeleteRepositoryConnection();
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteRepositoryConnection_Unauthorized() {
    login();
    testDeleteRepositoryConnection();
  }

  @Test
  public void testDeleteRepositoryConnection_Authorized() {
    grantWritePermission(app.getId());
    testDeleteRepositoryConnection();
  }

  private void testDeleteRepositoryConnection() {
    RepositoryConnection connection =
        tempEntity.newRepositoryConnection(app.getId(), "baseUrl", "user", "pass".toCharArray());

    repositoryConnectionService.deleteRepositoryConnection(OwnerType.APPLICATION, app.getId(), connection.getId());
    assertThat(dao.getById(connection.getId())).isNull();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testTestRepositoryConnection_Unauthenticated() {
    testTestRepositoryConnection();
  }

  @Test(expected = UnauthorizedException.class)
  public void testTestRepositoryConnection_Unauthorized() {
    login();
    testTestRepositoryConnection();
  }

  @Test
  public void testTestRepositoryConnection_Authorized() throws Exception {
    grantReadPermission(app.getId());
    setupMocks();
    when(client.getServerStatus()).thenReturn(Status.OK);

    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.baseUrl = "baseUrl";
    dto.username = "user";
    dto.password = "pass";
    StatusType status = repositoryConnectionService.testRepositoryConnection(OwnerType.APPLICATION, app.getId(), dto);

    assertThat(status).isEqualTo(Status.OK);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdateOwnerRepositoryConnectionStatus_Unauthenticated() {
    repositoryConnectionService.updateOwnerRepositoryConnectionStatus(OwnerType.APPLICATION, app.getId(), null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdateOwnerRepositoryConnectionStatus_Unauthorized() {
    login();
    repositoryConnectionService.updateOwnerRepositoryConnectionStatus(OwnerType.APPLICATION, app.getId(), null);
  }

  @Test(expected = BadRequestException.class)
  public void testUpdateOwnerRepositoryConnectionStatus_Authorized() {
    grantWritePermission(app.getId());
    repositoryConnectionService.updateOwnerRepositoryConnectionStatus(OwnerType.APPLICATION, app.getId(), null);
  }

  private void testTestRepositoryConnection() {
    String appId = tempEntity.newApplicationWithParent().getId();
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.baseUrl = "baseUrl";
    repositoryConnectionService.testRepositoryConnection(OwnerType.APPLICATION, appId, dto);
  }

  private void setupMocks() {
    when(mockFactory.create()).thenReturn(mockBuilder);
    when(mockBuilder.forNexus3(any(), any(), any())).thenReturn(client);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testTestRepositoryConnection_ByRepositoryConnectionId_Unauthenticated() {
    repositoryConnectionService.testRepositoryConnection(app.getType(), app.getId(), (String) null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testTestRepositoryConnection_ByRepositoryConnectionId_Unauthorized() {
    login();
    repositoryConnectionService.testRepositoryConnection(app.getType(), app.getId(), (String) null);
  }

  @Test(expected = NotFoundException.class)
  public void testTestRepositoryConnection_ByRepositoryConnectionId_Authorized() {
    grantReadPermission(app.getId());
    repositoryConnectionService.testRepositoryConnection(app.getType(), app.getId(), "doesNotExist");
  }
}
