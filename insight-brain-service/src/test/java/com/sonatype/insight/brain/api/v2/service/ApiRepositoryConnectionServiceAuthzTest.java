/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.List;
import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryConnectionDTO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryConnectionDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiRepositoryConnectionServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiRepositoryConnectionService repositoryConnectionService;

  @Inject
  private RepositoryConnectionDAO dao;

  @Test(expected = UnauthenticatedException.class)
  public void testGetRepositoryConnections_Unauthenticated() {
    repositoryConnectionService.getRepositoryConnections(OwnerType.ORGANIZATION, org.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetRepositoryConnections_Unauthorized() {
    login();
    repositoryConnectionService.getRepositoryConnections(OwnerType.ORGANIZATION, org.getId());
  }

  @Test
  public void testGetRepositoryConnections_Authorized() {
    grantGlobalPermission(Permission.READ);
    assertGetRepositoryConnections();
  }

  @Test
  public void testGetRepositoryConnections_Authorized_ByOwner() {
    grantReadPermission(app.getId());
    assertGetRepositoryConnections();
  }

  private void assertGetRepositoryConnections() {
    tempEntity.newRepositoryConnection(app.getId(), "url1", "user1", "pass1".toCharArray());
    List<ApiRepositoryConnectionDTO> connections =
        repositoryConnectionService.getRepositoryConnections(OwnerType.APPLICATION, app.getId());
    assertThat(connections).hasSize(1);
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
}
