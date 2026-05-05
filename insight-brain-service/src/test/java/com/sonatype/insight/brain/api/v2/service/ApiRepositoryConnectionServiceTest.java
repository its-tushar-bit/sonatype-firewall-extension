/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response.StatusType;

import com.sonatype.insight.brain.api.v2.dto.ApiOwnerRepositoryConnectionsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryConnectionDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryConnectionStatusRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryConnectionStatusResponseDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
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
import org.junit.After;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class ApiRepositoryConnectionServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiRepositoryConnectionService repositoryConnectionService;

  @Inject
  private OwnerDAO ownerDAO;

  @Inject
  private RepositoryConnectionDAO dao;

  @Inject
  private OrganizationDAO organizationDAO;

  @Inject
  private ApplicationDAO applicationDAO;

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

  @After
  public void after() {
    Organization rootOrganization = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    rootOrganization.setRepositoryConnectionEnabled(true);
    rootOrganization.setAllowRepositoryConnectionOverride(true);
    organizationDAO.update(rootOrganization);
  }

  @Test
  public void testGetRepositoryConnection_Organization() {
    Organization org = tempEntity.newOrganization();
    RepositoryConnection repositoryConnection = tempEntity.newRepositoryConnection(org.getId());

    ApiRepositoryConnectionDTO dto =
        repositoryConnectionService.getRepositoryConnection(org.getType(), org.getId(), repositoryConnection.getId());

    assertRepositoryConnectionDTO(repositoryConnection, dto);
  }

  @Test
  public void testGetRepositoryConnection_Application() {
    Application app = tempEntity.newApplicationWithParent();
    RepositoryConnection repositoryConnection = tempEntity.newRepositoryConnection(app.getId());

    ApiRepositoryConnectionDTO dto =
        repositoryConnectionService.getRepositoryConnection(app.getType(), app.getId(), repositoryConnection.getId());

    assertRepositoryConnectionDTO(repositoryConnection, dto);
  }

  @Test
  public void testGetRepositoryConnection_DoesNotExist() {
    Application app = tempEntity.newApplicationWithParent();
    String repositoryConnectionId = "doesNotExist";

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> repositoryConnectionService
        .getRepositoryConnection(app.getType(), app.getId(), repositoryConnectionId))
        .withMessageContaining(
            String.format(ApiRepositoryConnectionService.REPOSITORY_CONNECTION_NOT_FOUND_ERROR, repositoryConnectionId,
                app.getType(), app.getId()));
  }

  private void assertRepositoryConnectionDTO(RepositoryConnection expected, ApiRepositoryConnectionDTO actual) {
    assertThat(actual.repositoryConnectionId).isEqualTo(expected.getId());
    assertThat(actual.ownerType).isEqualTo(ownerDAO.getById(expected.getOwnerId()).getType());
    assertThat(actual.ownerId).isEqualTo(expected.getOwnerId());
    assertThat(actual.format).isEqualTo(expected.getFormat());
    assertThat(actual.isAnonymous).isEqualTo(expected.getUsername() == null);
    assertThat(actual.baseUrl).isEqualTo(expected.getBaseUrl());
    assertThat(actual.username).isEqualTo(expected.getUsername());
    assertThat(actual.password).isNull();
  }

  @Test
  public void testGetOwnerRepositoryConnections_Organization() {
    Organization org = tempEntity.newOrganization();
    org.setRepositoryConnectionEnabled(true);
    organizationDAO.update(org);
    testGetOwnerRepositoryConnections(org.getId(), OwnerType.ORGANIZATION);
  }

  @Test
  public void testGetOwnerRepositoryConnections_Organization_Inherit() {
    Organization org = tempEntity.newOrganization();
    tempEntity.newRepositoryConnection(org.getId(), "url1", "user1", "pass1".toCharArray());
    // add one to the ROOT which will be the effective connection
    tempEntity.newRepositoryConnection(Organization.ROOT_ORGANIZATION_ID, "url2", "user2", "pass2".toCharArray());
    List<ApiRepositoryConnectionDTO> connections = repositoryConnectionService
        .getOwnerRepositoryConnections(OwnerType.ORGANIZATION, org.getId(), true).repositoryConnections;
    assertThat(connections).hasSize(1)
        .extracting("baseUrl", "username", "ownerType", "ownerId")
        .containsExactly(tuple("url2", "user2", OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID));
  }

  @Test
  public void testGetOwnerRepositoryConnections_Organization_Inherit_Disabled() {
    Organization org = tempEntity.newOrganization();
    tempEntity.newRepositoryConnection(org.getId(), "url1", "user1", "pass1".toCharArray());
    org.setRepositoryConnectionEnabled(false);
    organizationDAO.update(org);

    List<ApiRepositoryConnectionDTO> connections = repositoryConnectionService
        .getOwnerRepositoryConnections(OwnerType.ORGANIZATION, org.getId(), true).repositoryConnections;
    assertThat(connections).hasSize(1)
        .extracting("baseUrl", "username", "ownerType", "ownerId")
        .containsExactly(tuple("url1", "user1", OwnerType.ORGANIZATION, org.getId()));
  }

  @Test
  public void testGetOwnerRepositoryConnections_Root_Inherit_Disabled() {
    Organization rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newRepositoryConnection(rootOrg.getId(), "url2", "user2", "pass2".toCharArray());
    rootOrg.setRepositoryConnectionEnabled(null);
    organizationDAO.update(rootOrg);

    List<ApiRepositoryConnectionDTO> connections = repositoryConnectionService
        .getOwnerRepositoryConnections(OwnerType.ORGANIZATION, rootOrg.getId(), true).repositoryConnections;
    assertThat(connections).hasSize(1)
        .extracting("baseUrl", "username", "ownerType", "ownerId")
        .containsExactly(tuple("url2", "user2", OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID));
  }

  @Test
  public void testGetOwnerRepositoryConnections_Application() {
    Application app = tempEntity.newApplicationWithParent();
    app.setRepositoryConnectionEnabled(true);
    applicationDAO.update(app);
    testGetOwnerRepositoryConnections(app.getId(), OwnerType.APPLICATION);
  }

  @Test
  public void testGetOwnerRepositoryConnections_Application_Inherit() {
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newRepositoryConnection(app.getId(), "url1", "user1", "pass1".toCharArray());
    // add one to the ROOT which will be the effective connection
    tempEntity.newRepositoryConnection(Organization.ROOT_ORGANIZATION_ID, "url2", "user2", "pass2".toCharArray());
    List<ApiRepositoryConnectionDTO> connections = repositoryConnectionService
        .getOwnerRepositoryConnections(OwnerType.APPLICATION, app.getId(), true).repositoryConnections;
    assertThat(connections).hasSize(1)
        .extracting("baseUrl", "username", "ownerType", "ownerId")
        .containsExactly(tuple("url2", "user2", OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID));
  }

  @Test
  public void testGetOwnerRepositoryConnections_Application_Inherit_Disabled() {
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newRepositoryConnection(app.getId(), "url1", "user1", "pass1".toCharArray());
    app.setRepositoryConnectionEnabled(false);
    applicationDAO.update(app);

    List<ApiRepositoryConnectionDTO> connections = repositoryConnectionService
        .getOwnerRepositoryConnections(OwnerType.APPLICATION, app.getId(), true).repositoryConnections;
    assertThat(connections).hasSize(1)
        .extracting("baseUrl", "username", "ownerType", "ownerId")
        .containsExactly(tuple("url1", "user1", OwnerType.APPLICATION, app.getId()));
  }

  private void testGetOwnerRepositoryConnections(final String id, final OwnerType ownerType) {
    tempEntity.newRepositoryConnection(id, "url1", "user1", "pass1".toCharArray());
    tempEntity.newRepositoryConnection(id, "url2", RepositoryFormat.MAVEN, "user2", "pass2".toCharArray());

    Owner owner = ownerDAO.getById(id);
    List<ApiRepositoryConnectionDTO> connections =
        repositoryConnectionService.getOwnerRepositoryConnections(ownerType, id, false).repositoryConnections;
    assertThat(connections).hasSize(2)
        .extracting("baseUrl", "username", "ownerType", "ownerId")
        .containsExactlyInAnyOrder(tuple("url1", "user1", owner.getType(), owner.getId()),
            tuple("url2", "user2", owner.getType(), owner.getId()));
  }

  @Test
  public void testGetOwnerRepositoryConnections_InheritTrue_Application() {
    tempEntity.newRepositoryConnection("other");
    Organization rootOrganization = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    String rootOrgId = rootOrganization.getId();
    String orgId = organization.getId();
    String appId = application.getId();

    // None
    assertThat(repositoryConnectionService.getOwnerRepositoryConnections(OwnerType.APPLICATION, appId,
        true).repositoryConnections).isEmpty();

    // Only root org
    rootOrganization.setAllowRepositoryConnectionOverride(false);
    organizationDAO.update(rootOrganization);
    RepositoryConnection rootOrgRepositoryConnection = tempEntity.newRepositoryConnection(rootOrgId);
    assertThat(repositoryConnectionService.getOwnerRepositoryConnections(OwnerType.APPLICATION, appId,
        true).repositoryConnections)
            .extracting("ownerType", "ownerId")
            .containsExactly(tuple(OwnerType.ORGANIZATION, rootOrgId));

    // Root org and org
    rootOrganization.setAllowRepositoryConnectionOverride(true);
    organizationDAO.update(rootOrganization);
    organization.setAllowRepositoryConnectionOverride(false);
    organization.setRepositoryConnectionEnabled(true);
    organizationDAO.update(organization);
    RepositoryConnection orgRepositoryConnection = tempEntity.newRepositoryConnection(orgId);
    assertThat(repositoryConnectionService.getOwnerRepositoryConnections(OwnerType.APPLICATION, appId,
        true).repositoryConnections)
            .extracting("ownerType", "ownerId")
            .containsExactly(tuple(OwnerType.ORGANIZATION, organization.getId()));

    // Root org, org, and app
    rootOrganization.setAllowRepositoryConnectionOverride(true);
    organizationDAO.update(rootOrganization);
    organization.setAllowRepositoryConnectionOverride(true);
    organizationDAO.update(organization);
    RepositoryConnection appRepositoryConnection = tempEntity.newRepositoryConnection(appId);
    application.setRepositoryConnectionEnabled(true);
    applicationDAO.update(application);
    assertThat(repositoryConnectionService.getOwnerRepositoryConnections(OwnerType.APPLICATION, appId,
        true).repositoryConnections)
            .extracting("ownerType", "ownerId")
            .containsExactly(tuple(OwnerType.APPLICATION, appId));

    // Org and app
    dao.delete(rootOrgRepositoryConnection);
    assertThat(repositoryConnectionService.getOwnerRepositoryConnections(OwnerType.APPLICATION, appId,
        true).repositoryConnections)
            .extracting("ownerType", "ownerId")
            .containsExactly(tuple(OwnerType.APPLICATION, appId));

    // Only app
    dao.delete(orgRepositoryConnection);
    assertThat(repositoryConnectionService.getOwnerRepositoryConnections(OwnerType.APPLICATION, appId,
        true).repositoryConnections)
            .extracting("ownerType", "ownerId")
            .containsExactly(tuple(OwnerType.APPLICATION, appId));

    // Root org and app
    rootOrgRepositoryConnection = tempEntity.newRepositoryConnection(rootOrgId);
    assertThat(repositoryConnectionService.getOwnerRepositoryConnections(OwnerType.APPLICATION, appId,
        true).repositoryConnections)
            .extracting("ownerType", "ownerId")
            .containsExactly(tuple(OwnerType.APPLICATION, appId));

    // Only org
    dao.delete(rootOrgRepositoryConnection);
    dao.delete(appRepositoryConnection);
    tempEntity.newRepositoryConnection(orgId);
    organization.setAllowRepositoryConnectionOverride(false);
    organizationDAO.update(organization);
    assertThat(repositoryConnectionService.getOwnerRepositoryConnections(OwnerType.APPLICATION, appId,
        true).repositoryConnections)
            .extracting("ownerType", "ownerId")
            .containsExactly(tuple(OwnerType.ORGANIZATION, orgId));
  }

  @Test
  public void testGetOwnerRepositoryConnections_InheritTrue_Organization() {
    tempEntity.newRepositoryConnection("other");
    Organization rootOrganization = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    Organization organization = tempEntity.newOrganization();
    String rootOrgId = rootOrganization.getId();
    String orgId = organization.getId();

    // None
    assertThat(repositoryConnectionService.getOwnerRepositoryConnections(OwnerType.ORGANIZATION, orgId,
        true).repositoryConnections)
            .isEmpty();

    // Only root org
    rootOrganization.setAllowRepositoryConnectionOverride(false);
    organizationDAO.update(rootOrganization);
    RepositoryConnection rootOrgRepositoryConnection = tempEntity.newRepositoryConnection(rootOrgId);
    assertThat(repositoryConnectionService.getOwnerRepositoryConnections(OwnerType.ORGANIZATION, orgId,
        true).repositoryConnections)
            .extracting("ownerType", "ownerId")
            .containsExactly(tuple(OwnerType.ORGANIZATION, rootOrgId));

    // Root org and org
    rootOrganization.setAllowRepositoryConnectionOverride(true);
    organizationDAO.update(rootOrganization);
    tempEntity.newRepositoryConnection(orgId);
    organization.setRepositoryConnectionEnabled(true);
    organizationDAO.update(organization);
    assertThat(repositoryConnectionService.getOwnerRepositoryConnections(OwnerType.ORGANIZATION, orgId,
        true).repositoryConnections)
            .extracting("ownerType", "ownerId")
            .containsExactly(tuple(OwnerType.ORGANIZATION, orgId));

    // Only org
    dao.delete(rootOrgRepositoryConnection);
    assertThat(repositoryConnectionService.getOwnerRepositoryConnections(OwnerType.ORGANIZATION, orgId,
        true).repositoryConnections)
            .extracting("ownerType", "ownerId")
            .containsExactly(tuple(OwnerType.ORGANIZATION, orgId));
  }

  @Test
  public void testGetOwnerRepositoryConnections_InheritTrue_RootOrganization() {
    tempEntity.newRepositoryConnection("other");
    String rootOrgId = Organization.ROOT_ORGANIZATION_ID;

    // None
    assertThat(repositoryConnectionService.getOwnerRepositoryConnections(OwnerType.ORGANIZATION, rootOrgId,
        true).repositoryConnections)
            .isEmpty();

    // Only root org
    tempEntity.newRepositoryConnection(rootOrgId);
    assertThat(repositoryConnectionService.getOwnerRepositoryConnections(OwnerType.ORGANIZATION, rootOrgId,
        true).repositoryConnections)
            .extracting("ownerType", "ownerId")
            .containsExactly(tuple(OwnerType.ORGANIZATION, rootOrgId));
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
  public void testUpdateRepositoryConnection_ToAnonymous() {
    String orgId = tempEntity.newOrganization().getId();
    RepositoryConnection repositoryConnection =
        tempEntity.newRepositoryConnection(orgId, "baseUrl", "user", "pass".toCharArray());
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.isAnonymous = true;

    ApiRepositoryConnectionDTO result =
        repositoryConnectionService.updateRepositoryConnection(OwnerType.ORGANIZATION, orgId,
            repositoryConnection.getId(), dto);

    RepositoryConnection storedRepositoryConnection = dao.getById(result.repositoryConnectionId);
    assertThat(storedRepositoryConnection.getOwnerId()).isEqualTo(repositoryConnection.getOwnerId());
    assertThat(storedRepositoryConnection.getBaseUrl()).isEqualTo(repositoryConnection.getBaseUrl());
    assertThat(storedRepositoryConnection.getFormat()).isEqualTo(repositoryConnection.getFormat());
    assertThat(storedRepositoryConnection.getUsername()).isNull();
    assertThat(storedRepositoryConnection.getPassword()).isNull();
  }

  @Test
  public void testUpdateRepositoryConnection_ToAnonymousAndDifferentBaseUrl() {
    String orgId = tempEntity.newOrganization().getId();
    RepositoryConnection repositoryConnection =
        tempEntity.newRepositoryConnection(orgId, "baseUrl", "user", "pass".toCharArray());
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.baseUrl = repositoryConnection.getBaseUrl() + "2";
    dto.isAnonymous = true;

    ApiRepositoryConnectionDTO result =
        repositoryConnectionService.updateRepositoryConnection(OwnerType.ORGANIZATION, orgId,
            repositoryConnection.getId(), dto);

    RepositoryConnection storedRepositoryConnection = dao.getById(result.repositoryConnectionId);
    assertThat(storedRepositoryConnection.getOwnerId()).isEqualTo(repositoryConnection.getOwnerId());
    assertThat(storedRepositoryConnection.getBaseUrl()).isEqualTo(dto.baseUrl);
    assertThat(storedRepositoryConnection.getFormat()).isEqualTo(repositoryConnection.getFormat());
    assertThat(storedRepositoryConnection.getUsername()).isNull();
    assertThat(storedRepositoryConnection.getPassword()).isNull();
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
    StatusType response = repositoryConnectionService.testRepositoryConnection(OwnerType.APPLICATION, appId, dto);

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

  @Test
  public void testGetOwnerRepositoryConnectionStatus() {
    Owner root = ownerDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());

    setupOwner(root, true, true);
    setupOwner(org, true, true);
    setupOwner(app, true, null);
    testGetOwnerRepositoryConnectionStatus(root, true, null, true, true, null);
    testGetOwnerRepositoryConnectionStatus(org, true, null, true, true, null);
    testGetOwnerRepositoryConnectionStatus(app, true, null, false, true, null);

    setupOwner(root, true, true);
    setupOwner(org, true, true);
    setupOwner(app, false, null);
    testGetOwnerRepositoryConnectionStatus(root, true, null, true, true, null);
    testGetOwnerRepositoryConnectionStatus(org, true, null, true, true, null);
    testGetOwnerRepositoryConnectionStatus(app, false, null, false, true, null);

    setupOwner(root, true, false);
    setupOwner(org, true, true);
    setupOwner(app, true, null);
    testGetOwnerRepositoryConnectionStatus(root, true, null, false, true, null);
    testGetOwnerRepositoryConnectionStatus(org, true, root.getName(), true, false, true);
    testGetOwnerRepositoryConnectionStatus(app, true, root.getName(), false, false, true);

    setupOwner(root, false, false);
    setupOwner(org, true, true);
    setupOwner(app, true, null);
    testGetOwnerRepositoryConnectionStatus(root, false, null, false, true, null);
    testGetOwnerRepositoryConnectionStatus(org, true, root.getName(), true, false, false);
    testGetOwnerRepositoryConnectionStatus(app, true, root.getName(), false, false, false);

    setupOwner(root, true, true);
    setupOwner(org, true, false);
    setupOwner(app, true, null);
    testGetOwnerRepositoryConnectionStatus(root, true, null, true, true, null);
    testGetOwnerRepositoryConnectionStatus(org, true, null, false, true, null);
    testGetOwnerRepositoryConnectionStatus(app, true, org.getName(), false, false, true);

    setupOwner(root, true, true);
    setupOwner(org, false, false);
    setupOwner(app, true, null);
    testGetOwnerRepositoryConnectionStatus(root, true, null, true, true, null);
    testGetOwnerRepositoryConnectionStatus(org, false, null, false, true, null);
    testGetOwnerRepositoryConnectionStatus(app, true, org.getName(), false, false, false);

    setupOwner(root, true, true);
    setupOwner(org, null, true);
    setupOwner(app, null, null);
    testGetOwnerRepositoryConnectionStatus(root, true, null, true, true, null);
    testGetOwnerRepositoryConnectionStatus(org, null, root.getName(), true, true, true);
    testGetOwnerRepositoryConnectionStatus(app, null, root.getName(), false, true, true);

    setupOwner(root, true, true);
    setupOwner(org, false, false);
    setupOwner(app, null, null);
    testGetOwnerRepositoryConnectionStatus(root, true, null, true, true, null);
    testGetOwnerRepositoryConnectionStatus(org, false, null, false, true, null);
    testGetOwnerRepositoryConnectionStatus(app, null, org.getName(), false, false, false);

    setupOwner(root, false, false);
    setupOwner(org, false, false);
    setupOwner(app, null, null);
    testGetOwnerRepositoryConnectionStatus(root, false, null, false, true, null);
    testGetOwnerRepositoryConnectionStatus(org, false, root.getName(), false, false, false);
    testGetOwnerRepositoryConnectionStatus(app, null, root.getName(), false, false, false);
    setupOwner(org, null, false);
    testGetOwnerRepositoryConnectionStatus(app, null, root.getName(), false, false, false);
    setupOwner(org, null, true);
    testGetOwnerRepositoryConnectionStatus(app, null, root.getName(), false, false, false);

    setupOwner(root, null, true);
    setupOwner(org, null, true);
    setupOwner(app, null, null);
    testGetOwnerRepositoryConnectionStatus(root, null, null, true, true, null);
    testGetOwnerRepositoryConnectionStatus(org, null, root.getName(), true, true, null);
    testGetOwnerRepositoryConnectionStatus(app, null, root.getName(), false, true, null);
  }

  private void setupOwner(Owner owner, Boolean enabled, Boolean allowOverride) {
    switch (owner.getType()) {
      case APPLICATION: {
        Application application = (Application) owner;
        application.setRepositoryConnectionEnabled(enabled);
        applicationDAO.update(application);
        break;
      }
      case ORGANIZATION: {
        Organization organization = (Organization) owner;
        organization.setRepositoryConnectionEnabled(enabled);
        organization.setAllowRepositoryConnectionOverride(allowOverride);
        organizationDAO.update(organization);
        break;
      }
      default: {
        fail("Unrecognized owner type");
      }
    }
  }

  private void testGetOwnerRepositoryConnectionStatus(
      Owner owner,
      Boolean expectedEnabled,
      String expectedInheritedFromOrganizationName,
      boolean expectedAllowOverride,
      boolean expectedAllowChange,
      Boolean expectedInheritedEnabled)
  {
    ApiRepositoryConnectionStatusResponseDTO result =
        repositoryConnectionService.getOwnerRepositoryConnectionStatus(owner.getType(), owner.getId());
    assertThat(result).isNotNull();
    assertThat(result.enabled).isEqualTo(expectedEnabled);
    assertThat(result.inheritedFromOrganizationName).isEqualTo(expectedInheritedFromOrganizationName);
    assertThat(result.allowOverride).isEqualTo(expectedAllowOverride);
    assertThat(result.allowChange).isEqualTo(expectedAllowChange);
    assertThat(result.inheritedFromOrgEnabled).isEqualTo(expectedInheritedEnabled);
  }

  @Test
  public void testUpdateOwnerRepositoryConnectionStatus_Organization() {
    ApiRepositoryConnectionStatusRequestDTO dto = new ApiRepositoryConnectionStatusRequestDTO();
    dto.allowOverride = false;
    dto.enabled = false;
    testUpdateOwnerRepositoryConnectionStatus_Organization(dto);
  }

  @Test
  public void testUpdateOwnerRepositoryConnectionStatus_Organization_Inherit() {
    ApiRepositoryConnectionStatusRequestDTO dto = new ApiRepositoryConnectionStatusRequestDTO();
    dto.allowOverride = false;
    dto.enabled = null;
    testUpdateOwnerRepositoryConnectionStatus_Organization(dto);
  }

  private void testUpdateOwnerRepositoryConnectionStatus_Organization(ApiRepositoryConnectionStatusRequestDTO dto) {
    String orgId = tempEntity.newOrganization().getId();
    repositoryConnectionService.updateOwnerRepositoryConnectionStatus(OwnerType.ORGANIZATION, orgId, dto);

    Organization org = organizationDAO.getByIdNotNull(orgId);
    assertThat(org.isAllowRepositoryConnectionOverride()).isEqualTo(dto.allowOverride);
    assertThat(org.isRepositoryConnectionEnabled()).isEqualTo(dto.enabled);
  }

  @Test
  public void testUpdateOwnerRepositoryConnectionStatus_Application() {
    ApiRepositoryConnectionStatusRequestDTO dto = new ApiRepositoryConnectionStatusRequestDTO();
    dto.enabled = false;
    testUpdateOwnerRepositoryConnectionStatus_Application(dto, false);
  }

  @Test
  public void testUpdateOwnerRepositoryConnectionStatus_Application_Inherit() {
    ApiRepositoryConnectionStatusRequestDTO dto = new ApiRepositoryConnectionStatusRequestDTO();
    dto.enabled = null;
    testUpdateOwnerRepositoryConnectionStatus_Application(dto, null);
  }

  private void testUpdateOwnerRepositoryConnectionStatus_Application(
      final ApiRepositoryConnectionStatusRequestDTO dto,
      final Boolean expectedEnabled)
  {
    Application app = tempEntity.newApplicationWithParent();

    repositoryConnectionService.updateOwnerRepositoryConnectionStatus(OwnerType.APPLICATION, app.getId(), dto);

    Application application = applicationDAO.getByIdNotNull(app.getId());
    assertThat(application.isRepositoryConnectionEnabled()).isEqualTo(expectedEnabled);
  }

  @Test
  public void testUpdateOwnerRepositoryConnectionStatus_MissingPayload() {
    String orgId = tempEntity.newOrganization().getId();
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> repositoryConnectionService
                .updateOwnerRepositoryConnectionStatus(OwnerType.ORGANIZATION, orgId, null))
        .withMessage("missing repository connection configuration data for update");
  }

  @Test
  public void testUpdateOwnerRepositoryConnectionStatus_AllowedForOwner_InvalidOrgId() {
    ApiRepositoryConnectionStatusRequestDTO dto = new ApiRepositoryConnectionStatusRequestDTO();
    dto.enabled = true;
    dto.allowOverride = false;
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> repositoryConnectionService
            .updateOwnerRepositoryConnectionStatus(OwnerType.ORGANIZATION, "irrelevant", dto))
        .withMessage(
            "Organization with ID irrelevant does not exist.");
  }

  @Test
  public void testUpdateOwnerRepositoryConnectionStatus_AllowedForOwner_InvalidAppId() {
    ApiRepositoryConnectionStatusRequestDTO dto = new ApiRepositoryConnectionStatusRequestDTO();
    dto.enabled = true;
    dto.allowOverride = false;
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> repositoryConnectionService
            .updateOwnerRepositoryConnectionStatus(OwnerType.APPLICATION, "irrelevant", dto))
        .withMessage("Application with ID irrelevant does not exist.");
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

  @Test
  public void testTestRepositoryConnection_ByRepositoryId_DoesNotExist() {
    Organization org = tempEntity.newOrganization();
    String repositoryConnectionId = "doesNotExist";

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> repositoryConnectionService
        .testRepositoryConnection(org.getType(), org.getId(), repositoryConnectionId))
        .withMessageContaining(
            String.format(ApiRepositoryConnectionService.REPOSITORY_CONNECTION_NOT_FOUND_ERROR, repositoryConnectionId,
                org.getType(), org.getId()));
  }

  @Test
  public void testTestRepositoryConnection_ByRepositoryId_WrongOwner() {
    Organization org1 = tempEntity.newOrganization();
    RepositoryConnection repositoryConnection = tempEntity.newRepositoryConnection(org1.getId());
    Organization org2 = tempEntity.newOrganization();

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> repositoryConnectionService
        .testRepositoryConnection(org2.getType(), org2.getId(), repositoryConnection.getId()))
        .withMessageContaining(String.format(ApiRepositoryConnectionService.REPOSITORY_CONNECTION_NOT_FOUND_ERROR,
            repositoryConnection.getId(), org2.getType(), org2.getId()));
  }

  @Test
  public void testTestRepositoryConnection_ByRepositoryId() throws Exception {
    setupMocks();
    when(client.getServerStatus()).thenThrow(new IOException("error"));
    String appId = tempEntity.newApplicationWithParent().getId();
    RepositoryConnection repositoryConnection = tempEntity.newRepositoryConnection(appId);
    repositoryConnection.setPassword(passwordHandler.encryptPassword(repositoryConnection.getPassword()));
    dao.update(repositoryConnection);

    StatusType response = repositoryConnectionService.testRepositoryConnection(OwnerType.APPLICATION, appId,
        repositoryConnection.getId());

    assertThat(response).isEqualTo(Status.BAD_GATEWAY);
  }

  @Test
  public void testGetOwnerRepositoryConnections() {
    Application application = tempEntity.newApplicationWithParent();
    application.setRepositoryConnectionEnabled(true);
    applicationDAO.update(application);
    RepositoryConnection repositoryConnection = tempEntity.newRepositoryConnection(application.getId());

    ApiOwnerRepositoryConnectionsDTO dto =
        repositoryConnectionService.getOwnerRepositoryConnections(application.getType(), application.getId(),
            false);

    assertThat(dto.repositoryConnections).hasSize(1);
    ApiRepositoryConnectionDTO connectionDTO = dto.repositoryConnections.get(0);
    assertRepositoryConnectionDTO(repositoryConnection, connectionDTO);
    assertThat(dto.repositoryConnectionStatus.enabled).isTrue();
    assertThat(dto.repositoryConnectionStatus.inheritedFromOrganizationName).isNull();
    assertThat(dto.repositoryConnectionStatus.allowOverride).isFalse();
    assertThat(dto.repositoryConnectionStatus.allowChange).isTrue();
    assertThat(dto.ownerDTO.ownerId).isEqualTo(application.getId());
    assertThat(dto.ownerDTO.ownerName).isEqualTo(application.getName());
    assertThat(dto.ownerDTO.ownerPublicId).isEqualTo(application.getPublicId());
  }

  @Test
  public void testUpdateOwnerRepositoryConnectionStatus_RootOrgCannotInherit() {
    ApiRepositoryConnectionStatusRequestDTO dto = new ApiRepositoryConnectionStatusRequestDTO();

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> repositoryConnectionService.updateOwnerRepositoryConnectionStatus(OwnerType.ORGANIZATION,
            Organization.ROOT_ORGANIZATION_ID, dto))
        .withMessageContaining("root organization cannot inherit configuration");
  }

  private void setupMocks() {
    when(mockFactory.create()).thenReturn(mockBuilder);
    when(mockBuilder.forNexus3(any(), any(), any())).thenReturn(client);
  }
}
