/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.core.Response.Status;

import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryConnectionDTO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryConnectionDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.model.repository.RepositoryFormat;
import com.sonatype.insight.brain.repository.RepositoryClient;
import com.sonatype.insight.brain.repository.client.RepositoryClientFactory;
import com.sonatype.insight.brain.repository.client.RepositoryClientFactory.RepositoryClientBuilder;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.inject.Binder;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ApiRepositoryConnectionServiceTest
    extends AbstractComponentTest
{
  @Rule
  public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  @Inject
  private ApiRepositoryConnectionService repositoryConnectionService;

  @Inject
  private OwnerDAO ownerDAO;
  
  @Inject
  private RepositoryConnectionDAO dao;

  @Inject
  private PasswordHandler passwordHandler;

  @Mock
  private RepositoryClientFactory mockFactory;

  @Mock
  private RepositoryClientBuilder mockBuilder;

  @Mock
  private RepositoryClient client;

  @Override
  public void configure(final Binder binder) {
    binder.bind(RepositoryClientFactory.class).toInstance(mockFactory);
    super.configure(binder);
  }

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
    tempEntity.newRepositoryConnection(id, "url2", RepositoryFormat.MAVEN, "user2", "pass2".toCharArray());

    Owner owner = ownerDAO.getById(id);
    List<ApiRepositoryConnectionDTO> connections =
        repositoryConnectionService.getRepositoryConnections(ownerType, id, false);
    assertThat(connections).hasSize(2).extracting("baseUrl", "username", "ownerType", "ownerId")
        .containsExactlyInAnyOrder(tuple("url1", "user1", owner.getType(), owner.getId()),
            tuple("url2", "user2", owner.getType(), owner.getId()));
  }

  @Test
  public void testGetRepositoryConnections_InheritTrue_Application() {
    tempEntity.newRepositoryConnection("other");
    Application application = tempEntity.newApplicationWithParent();
    String rootOrgId = Organization.ROOT_ORGANIZATION_ID;
    String orgId = application.getParentOwnerId();
    String appId = application.getId();

    // None
    assertThat(repositoryConnectionService.getRepositoryConnections(OwnerType.APPLICATION, appId, true)).isEmpty();

    // Only root org
    RepositoryConnection rootOrgRepositoryConnection = tempEntity.newRepositoryConnection(rootOrgId);
    assertThat(repositoryConnectionService.getRepositoryConnections(OwnerType.APPLICATION, appId, true))
        .extracting("ownerType", "ownerId").containsExactly(tuple(OwnerType.ORGANIZATION, rootOrgId));

    // Root org and org
    RepositoryConnection orgRepositoryConnection = tempEntity.newRepositoryConnection(orgId);
    assertThat(repositoryConnectionService.getRepositoryConnections(OwnerType.APPLICATION, appId, true))
        .extracting("ownerType", "ownerId").containsExactly(tuple(OwnerType.ORGANIZATION, orgId));

    // Root org, org, and app
    RepositoryConnection appRepositoryConnection = tempEntity.newRepositoryConnection(appId);
    assertThat(repositoryConnectionService.getRepositoryConnections(OwnerType.APPLICATION, appId, true))
        .extracting("ownerType", "ownerId").containsExactly(tuple(OwnerType.APPLICATION, appId));

    // Org and app
    dao.delete(rootOrgRepositoryConnection);
    assertThat(repositoryConnectionService.getRepositoryConnections(OwnerType.APPLICATION, appId, true))
        .extracting("ownerType", "ownerId").containsExactly(tuple(OwnerType.APPLICATION, appId));

    // Only app
    dao.delete(orgRepositoryConnection);
    assertThat(repositoryConnectionService.getRepositoryConnections(OwnerType.APPLICATION, appId, true))
        .extracting("ownerType", "ownerId").containsExactly(tuple(OwnerType.APPLICATION, appId));

    // Root org and app
    rootOrgRepositoryConnection = tempEntity.newRepositoryConnection(rootOrgId);
    assertThat(repositoryConnectionService.getRepositoryConnections(OwnerType.APPLICATION, appId, true))
        .extracting("ownerType", "ownerId").containsExactly(tuple(OwnerType.APPLICATION, appId));

    // Only org
    dao.delete(rootOrgRepositoryConnection);
    dao.delete(appRepositoryConnection);
    tempEntity.newRepositoryConnection(orgId);
    assertThat(repositoryConnectionService.getRepositoryConnections(OwnerType.APPLICATION, appId, true))
        .extracting("ownerType", "ownerId").containsExactly(tuple(OwnerType.ORGANIZATION, orgId));
  }

  @Test
  public void testGetRepositoryConnections_InheritTrue_Organization() {
    tempEntity.newRepositoryConnection("other");
    Organization organization = tempEntity.newOrganization();
    String rootOrgId = Organization.ROOT_ORGANIZATION_ID;
    String orgId = organization.getId();

    // None
    assertThat(repositoryConnectionService.getRepositoryConnections(OwnerType.ORGANIZATION, orgId, true))
        .isEmpty();

    // Only root org
    RepositoryConnection rootOrgRepositoryConnection = tempEntity.newRepositoryConnection(rootOrgId);
    assertThat(repositoryConnectionService.getRepositoryConnections(OwnerType.ORGANIZATION, orgId, true))
        .extracting("ownerType", "ownerId").containsExactly(tuple(OwnerType.ORGANIZATION, rootOrgId));

    // Root org and org
    tempEntity.newRepositoryConnection(orgId);
    assertThat(repositoryConnectionService.getRepositoryConnections(OwnerType.ORGANIZATION, orgId, true))
        .extracting("ownerType", "ownerId").containsExactly(tuple(OwnerType.ORGANIZATION, orgId));

    // Only org
    dao.delete(rootOrgRepositoryConnection);
    assertThat(repositoryConnectionService.getRepositoryConnections(OwnerType.ORGANIZATION, orgId, true))
        .extracting("ownerType", "ownerId").containsExactly(tuple(OwnerType.ORGANIZATION, orgId));
  }

  @Test
  public void testGetRepositoryConnections_InheritTrue_RootOrganization() {
    tempEntity.newRepositoryConnection("other");
    String rootOrgId = Organization.ROOT_ORGANIZATION_ID;

    // None
    assertThat(repositoryConnectionService.getRepositoryConnections(OwnerType.ORGANIZATION, rootOrgId, true))
        .isEmpty();

    // Only root org
    tempEntity.newRepositoryConnection(rootOrgId);
    assertThat(repositoryConnectionService.getRepositoryConnections(OwnerType.ORGANIZATION, rootOrgId, true))
        .extracting("ownerType", "ownerId").containsExactly(tuple(OwnerType.ORGANIZATION, rootOrgId));
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
    dto.format = RepositoryFormat.MAVEN;
    dto.username = "user";
    dto.password = "pass";

    ApiRepositoryConnectionDTO createdDto =
        repositoryConnectionService.addRepositoryConnection(ownerType, id, dto);

    assertThat(createdDto.repositoryConnectionId).isNotNull();
    assertThat(createdDto.password).isNull();
    assertThat(createdDto.format).isEqualTo(dto.format);
    assertThat(createdDto.ownerType).isEqualTo(ownerType);
    assertThat(createdDto.ownerId).isEqualTo(id);
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
        .withMessage("repository connection format generic configuration exists for organization with id: " + orgId);
  }

  @Test
  public void testAddRepositoryConnection_DefaultFormat() {
    Application app = tempEntity.newApplicationWithParent();
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.baseUrl = "baseUrl";
    dto.username = "user";
    dto.password = "pass";

    ApiRepositoryConnectionDTO createdDto =
        repositoryConnectionService.addRepositoryConnection(OwnerType.APPLICATION, app.getId(), dto);

    assertThat(createdDto.repositoryConnectionId).isNotNull();
    assertThat(createdDto.format).isEqualTo(RepositoryFormat.GENERIC);
    assertThat(createdDto.ownerType).isEqualTo(OwnerType.APPLICATION);
    assertThat(createdDto.ownerId).isEqualTo(app.getId());
    RepositoryConnection savedConfig = dao.getByOwnerIdAndBaseUrl(app.getId(), dto.baseUrl);
    assertThat(savedConfig.getFormat()).isEqualTo(RepositoryFormat.GENERIC);
  }

  @Test
  public void testUpdateRepositoryConnection_Organization() {
    Organization org = tempEntity.newOrganization();
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.baseUrl = "updated baseUrl";
    dto.username = "user2";
    dto.password = "pass2";
    dto.format = RepositoryFormat.MAVEN;
    testUpdateRepositoryConnection(org.getId(), OwnerType.ORGANIZATION, dto, false);
  }

  @Test
  public void testUpdateRepositoryConnection_Application() {
    Application app = tempEntity.newApplicationWithParent();
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.baseUrl = "updated baseUrl";
    dto.username = "user2";
    dto.password = "pass2";
    dto.format = RepositoryFormat.MAVEN;
    testUpdateRepositoryConnection(app.getId(), OwnerType.APPLICATION, dto, false);
  }

  @Test
  public void testUpdateRepositoryConnection_BaseUrlOnly_ExistingAnonymousConnection() {
    Application app = tempEntity.newApplicationWithParent();
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.baseUrl = "updated baseUrl";
    dto.format = RepositoryFormat.MAVEN;
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
    assertThat(updated.baseUrl).isEqualTo(dto.baseUrl);
    assertThat(updated.username).isEqualTo(dto.username);
    assertThat(updated.password).isNull();
    assertThat(updated.ownerId).isEqualTo(id);
    assertThat(updated.ownerType).isEqualTo(ownerType);

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
    RepositoryConnection toUpdate =
        tempEntity.newRepositoryConnection(orgId, "baseUrl", RepositoryFormat.MAVEN, "user", "pass".toCharArray());
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.repositoryConnectionId = toUpdate.getId();
    dto.baseUrl = toUpdate.getBaseUrl();
    dto.format = RepositoryFormat.GENERIC;
    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(() -> repositoryConnectionService.updateRepositoryConnection(OwnerType.ORGANIZATION, orgId,
            dto.repositoryConnectionId, dto))
        .withMessage("repository connection format generic configuration exists for organization with id: " + orgId);
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
  public void testUpdateRepositoryConnection_NoFormat() {
    Application app = tempEntity.newApplicationWithParent();
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.baseUrl = "updated baseUrl";
    dto.username = "user2";
    dto.password = "pass2";
    RepositoryConnection existingConnection = tempEntity.newRepositoryConnection(app.getId());

    ApiRepositoryConnectionDTO updated =
        repositoryConnectionService.updateRepositoryConnection(OwnerType.APPLICATION, app.getId(),
            existingConnection.getId(), dto);

    assertThat(updated.baseUrl).isEqualTo(dto.baseUrl);
    assertThat(updated.format).isEqualTo(existingConnection.getFormat());
    assertThat(updated.ownerType).isEqualTo(OwnerType.APPLICATION);
    assertThat(updated.ownerId).isEqualTo(app.getId());
    RepositoryConnection storedConnection = dao.getById(existingConnection.getId());
    assertThat(updated.baseUrl).isEqualTo(storedConnection.getBaseUrl());
    assertThat(updated.format).isEqualTo(storedConnection.getFormat());
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

  @Test
  public void testTestRepositoryConnection_Success() throws Exception {
    setupMocks();
    when(client.getServerStatus()).thenReturn(Status.OK);
    testTestRepositoryConnection(Status.OK);
  }

  @Test
  public void testTestRepositoryConnection_Unauthorized() throws Exception {
    setupMocks();
    when(client.getServerStatus()).thenReturn(Status.UNAUTHORIZED);
    testTestRepositoryConnection(Status.UNAUTHORIZED);
  }

  @Test
  public void testTestRepositoryConnection_Exception() throws Exception {
    setupMocks();
    when(client.getServerStatus()).thenThrow(new IOException("error"));
    testTestRepositoryConnection(Status.BAD_GATEWAY);
  }

  private void testTestRepositoryConnection(Status status) {
    String appId = tempEntity.newApplicationWithParent().getId();

    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.baseUrl = "baseUrl";
    dto.username = "user";
    dto.password = "pass";
    Status response = repositoryConnectionService.testRepositoryConnection(OwnerType.APPLICATION, appId, dto);

    assertThat(response).isEqualTo(status);
  }

  @Test
  public void testTestRepositoryConnection_MissingBaseUrl() {
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    testTestRepositoryConnection_MissingData(dto, "missing repository base URL");
  }

  @Test
  public void testTestRepositoryConnection_MissingPassword() {
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.baseUrl = "baseUrl";
    dto.username = "user";
    testTestRepositoryConnection_MissingData(dto, "missing username/password for repository connection");
  }

  @Test
  public void testTestRepositoryConnection_MissingUsername() {
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.baseUrl = "baseUrl";
    dto.password = "pass";
    testTestRepositoryConnection_MissingData(dto, "missing username/password for repository connection");
  }

  private void testTestRepositoryConnection_MissingData(
      final ApiRepositoryConnectionDTO dto,
      final String exectedMessage)
  {
    String appId = tempEntity.newApplicationWithParent().getId();
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> repositoryConnectionService.testRepositoryConnection(OwnerType.APPLICATION, appId, dto))
        .withMessage(exectedMessage);
  }

  private void setupMocks() {
    when(mockFactory.create()).thenReturn(mockBuilder);
    when(mockBuilder.forNexus3(any(), any(), any())).thenReturn(client);
  }
}
