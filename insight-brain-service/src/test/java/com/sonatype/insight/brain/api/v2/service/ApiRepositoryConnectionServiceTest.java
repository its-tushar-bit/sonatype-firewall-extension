/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryConnectionDTO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryConnectionDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;

public class ApiRepositoryConnectionServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiRepositoryConnectionService repositoryConnectionService;

  @Inject
  private RepositoryConnectionDAO dao;

  @Inject
  private PasswordHandler passwordHandler;

  @Test
  public void testGetRepositoryConnections_Organization() {
    Organization org = tempEntity.newOrganization();
    testGetRepositoryConnections(org.getId(), OwnerType.ORGANIZATION);
  }

  @Test
  public void testGetRepositoryConnections_Application() {
    Application app = tempEntity.newApplicationWithParent();
    testGetRepositoryConnections(app.getId(), OwnerType.APPLICATION);
  }

  private void testGetRepositoryConnections(final String id, final OwnerType ownerType) {
    tempEntity.newRepositoryConnection(id, "url1", "user1", "pass1".toCharArray());
    tempEntity.newRepositoryConnection(id, "url2", "user2", "pass2".toCharArray());

    List<ApiRepositoryConnectionDTO> connections =
        repositoryConnectionService.getRepositoryConnections(ownerType, id);
    assertThat(connections).hasSize(2).extracting("baseUrl", "username")
        .containsExactlyInAnyOrder(tuple("url1", "user1"), tuple("url2", "user2"));
  }

  @Test
  public void testAddRepositoryConnection_Organization() {
    Organization org = tempEntity.newOrganization();
    testAddRepositoryConnection(OwnerType.ORGANIZATION, org.getId());
  }

  @Test
  public void testAddRepositoryConnection_Application() {
    Application app = tempEntity.newApplicationWithParent();
    testAddRepositoryConnection(OwnerType.APPLICATION, app.getId());
  }

  private void testAddRepositoryConnection(OwnerType ownerType, String id) {
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.baseUrl = "baseUrl";
    dto.username = "user";
    dto.password = "pass";

    ApiRepositoryConnectionDTO createdDto =
        repositoryConnectionService.addRepositoryConnection(ownerType, id, dto);

    assertThat(createdDto.repositoryConnectionId).isNotNull();
    assertThat(createdDto.password).isNull();
    RepositoryConnection savedConfig = dao.getByOwnerIdAndBaseUrl(id, dto.baseUrl);
    assertThat(passwordHandler.decryptPassword(savedConfig.getPassword())).isEqualTo(dto.password.toCharArray());
  }

  @Test
  public void testAddRepositoryConnection_MissingPayload() {
    String orgId = tempEntity.newOrganization().getId();
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> repositoryConnectionService.addRepositoryConnection(OwnerType.ORGANIZATION, orgId, null))
        .withMessage("missing repository base URL");
  }

  @Test
  public void testAddRepositoryConnection_MissingBaseUrl() {
    String orgId = tempEntity.newOrganization().getId();
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> repositoryConnectionService.addRepositoryConnection(OwnerType.ORGANIZATION, orgId, dto))
        .withMessage("missing repository base URL");
  }

  @Test
  public void testAddRepositoryConnection_MissingUsername() {
    String orgId = tempEntity.newOrganization().getId();
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.baseUrl = "baseUrl";
    dto.password = "pass";
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> repositoryConnectionService.addRepositoryConnection(OwnerType.ORGANIZATION, orgId, dto))
        .withMessage("missing username/password for repository connection");
  }

  @Test
  public void testAddRepositoryConnection_MissingPassword() {
    String orgId = tempEntity.newOrganization().getId();
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.baseUrl = "baseUrl";
    dto.username = "user";
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> repositoryConnectionService.addRepositoryConnection(OwnerType.ORGANIZATION, orgId, dto))
        .withMessage("missing username/password for repository connection");
  }

  @Test
  public void testAddRepositoryConnection_ConfigurationAlreadyExists() {
    String orgId = tempEntity.newOrganization().getId();
    tempEntity.newRepositoryConnection(orgId, "baseUrl", "user", "pass".toCharArray());
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.baseUrl = "baseUrl";
    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(() -> repositoryConnectionService.addRepositoryConnection(OwnerType.ORGANIZATION, orgId, dto))
        .withMessage("base URL configuration already exist for organization with id: " + orgId);
  }

  @Test
  public void testUpdateRepositoryConnection_Organization() {
    Organization org = tempEntity.newOrganization();
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.baseUrl = "updated baseUrl";
    dto.username = "user2";
    dto.password = "pass2";
    testUpdateRepositoryConnection(org.getId(), OwnerType.ORGANIZATION, dto, false);
  }

  @Test
  public void testUpdateRepositoryConnection_Application() {
    Application app = tempEntity.newApplicationWithParent();
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.baseUrl = "updated baseUrl";
    dto.username = "user2";
    dto.password = "pass2";
    testUpdateRepositoryConnection(app.getId(), OwnerType.APPLICATION, dto, false);
  }

  @Test
  public void testUpdateRepositoryConnection_BaseUrlOnly_ExistingAnonymousConnection() {
    Application app = tempEntity.newApplicationWithParent();
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.baseUrl = "updated baseUrl";
    testUpdateRepositoryConnection(app.getId(), OwnerType.APPLICATION, dto, true);
  }

  @Test
  public void testUpdateRepositoryConnection_BaseUrlOnly_ForExistingConnectionWithCredentials() {
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.baseUrl = "updated baseUrl";
    testUpdateRepositoryConnectionMissingAuthData(dto);
  }

  @Test
  public void testUpdateRepositoryConnection_MissingPassword() {
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.username = "user2";
    testUpdateRepositoryConnectionMissingAuthData(dto);
  }

  @Test
  public void testUpdateRepositoryConnection_MissingUsername() {
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.password = "pass";
    testUpdateRepositoryConnectionMissingAuthData(dto);
  }

  private void testUpdateRepositoryConnectionMissingAuthData(final ApiRepositoryConnectionDTO dto) {
    Application app = tempEntity.newApplicationWithParent();
    String connectionId = tempEntity.newRepositoryConnection(app.getId(), "baseUrl", "user",
        passwordHandler.encryptPassword("pass".toCharArray())).getId();
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> repositoryConnectionService.updateRepositoryConnection(OwnerType.APPLICATION, app.getId(),
            connectionId, dto))
        .withMessage("missing username/password for repository connection");
  }

  private void testUpdateRepositoryConnection(
      String id,
      OwnerType ownerType,
      ApiRepositoryConnectionDTO dto,
      boolean anonymous)
  {
    RepositoryConnection existingConnection;
    if (anonymous) {
      existingConnection = tempEntity.newRepositoryConnection(id, "baseUrl", null, null);
    }
    else {
      existingConnection = tempEntity.newRepositoryConnection(id, "baseUrl", "user",
          passwordHandler.encryptPassword("pass".toCharArray()));
    }

    ApiRepositoryConnectionDTO updated =
        repositoryConnectionService.updateRepositoryConnection(ownerType, id,
            existingConnection.getId(), dto);
    RepositoryConnection storedConnection = dao.getById(existingConnection.getId());

    assertThat(updated.baseUrl).isEqualTo(dto.baseUrl != null ? dto.baseUrl : storedConnection.getBaseUrl());
    assertThat(updated.username).isEqualTo(dto.username != null ? dto.username : storedConnection.getUsername());
    assertThat(updated.password).isNull();

    if (anonymous) {
      assertThat(storedConnection.getPassword()).isNull();
    }
    else {
      String expectedStoredPassword = dto.password != null ? dto.password : "pass";
      assertThat(Arrays.equals(passwordHandler.decryptPassword(storedConnection.getPassword()),
          expectedStoredPassword.toCharArray())).isTrue();
    }
  }

  @Test
  public void testUpdateRepositoryConnection_MissingPayload() {
    String orgId = tempEntity.newOrganization().getId();
    RepositoryConnection existingConnection =
        tempEntity.newRepositoryConnection(orgId, "baseUrl", "user", "pass".toCharArray());
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> repositoryConnectionService.updateRepositoryConnection(OwnerType.ORGANIZATION, orgId,
            existingConnection.getId(), null))
        .withMessage("missing repository connection data for update");
  }

  @Test
  public void testUpdateRepositoryConnection_MissingUpdateData() {
    String orgId = tempEntity.newOrganization().getId();
    RepositoryConnection existingConnection =
        tempEntity.newRepositoryConnection(orgId, "baseUrl", "user", "pass".toCharArray());
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> repositoryConnectionService.updateRepositoryConnection(OwnerType.ORGANIZATION, orgId,
            existingConnection.getId(), dto))
        .withMessage("missing repository connection data for update");
  }

  @Test
  public void testUpdateRepositoryConnection_InvalidOwnerId() {
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.baseUrl = "url";
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> repositoryConnectionService.updateRepositoryConnection(OwnerType.ORGANIZATION, "blahblah",
            "someId", dto))
        .withMessage("no repository connections found with connection id: someId for organization having id: blahblah");
  }

  @Test
  public void testUpdateRepositoryConnection_ConfigurationAlreadyExists() {
    String orgId = tempEntity.newOrganization().getId();
    tempEntity.newRepositoryConnection(orgId, "baseUrl", "user", "pass".toCharArray());
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.repositoryConnectionId = "anotherId";
    dto.baseUrl = "baseUrl";
    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(() -> repositoryConnectionService.updateRepositoryConnection(OwnerType.ORGANIZATION, orgId,
            dto.repositoryConnectionId, dto))
        .withMessage("repository connection URL configuration exist for organization with id: " + orgId);
  }

  @Test
  public void testUpdateRepositoryConnection_ConfigurationNotFound() {
    String orgId = tempEntity.newOrganization().getId();
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.repositoryConnectionId = "nonExistentId";
    dto.baseUrl = "baseUrl";
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> repositoryConnectionService.updateRepositoryConnection(OwnerType.ORGANIZATION, orgId,
            dto.repositoryConnectionId, dto))
        .withMessage(
            "no repository connections found with connection id: nonExistentId for organization having id: " + orgId);
  }

  @Test
  public void testDeleteRepositoryConnection_Organization() {
    Organization org = tempEntity.newOrganization();
    testDeleteRepositoryConnection(org.getId(), OwnerType.ORGANIZATION);
  }

  @Test
  public void testDeleteRepositoryConnection_Application() {
    Application app = tempEntity.newApplicationWithParent();
    testDeleteRepositoryConnection(app.getId(), OwnerType.APPLICATION);
  }

  private void testDeleteRepositoryConnection(final String id, final OwnerType ownerType) {
    RepositoryConnection connection =
        tempEntity.newRepositoryConnection(id, "baseUrl", "user", "pass".toCharArray());

    repositoryConnectionService.deleteRepositoryConnection(ownerType, id, connection.getId());
    assertThat(dao.getById(connection.getId())).isNull();
  }

  @Test
  public void testDeleteRepositoryConnection_InvalidOwnerId() {
    Application app = tempEntity.newApplicationWithParent();
    String connectionId =
        tempEntity.newRepositoryConnection(app.getId(), "baseUrl", "user", "pass".toCharArray()).getId();

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> repositoryConnectionService.deleteRepositoryConnection(OwnerType.APPLICATION, "blahblah",
            connectionId))
        .withMessage("no repository connections found with connection id: " + connectionId
            + " for application having id: blahblah");
  }

  @Test
  public void testDeleteRepositoryConnection_InvalidConnectionId() {
    String appId = tempEntity.newApplicationWithParent().getId();
    tempEntity.newRepositoryConnection(appId, "baseUrl", "user", "pass".toCharArray());

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> repositoryConnectionService.deleteRepositoryConnection(OwnerType.APPLICATION, appId,
            "blahblah"))
        .withMessage("no repository connections found with connection id: blahblah" +
            " for application having id: " + appId);
  }

  @Test
  public void testDeleteRepositoryConnection_ConnectionAndOwnerMismatch() {
    String app1Id = tempEntity.newApplicationWithParent().getId();
    String app2Id = tempEntity.newApplicationWithParent().getId();
    RepositoryConnection connection =
        tempEntity.newRepositoryConnection(app1Id, "baseUrl", "user", "pass".toCharArray());

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> repositoryConnectionService.deleteRepositoryConnection(OwnerType.APPLICATION, app2Id,
            connection.getId()))
        .withMessage("no repository connections found with connection id: " + connection.getId()
            + " for application having id: " + app2Id);
  }
}
