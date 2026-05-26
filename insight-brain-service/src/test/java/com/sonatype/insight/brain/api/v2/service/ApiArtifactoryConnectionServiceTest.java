/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import static com.sonatype.insight.brain.api.v2.service.ApiArtifactoryConnectionService.ARTIFACTORY_CONNECTION_NOT_FOUND_ERROR;
import static com.sonatype.insight.brain.api.v2.service.ApiArtifactoryConnectionService.MISSING_CONNECTION_DATA_ERROR;
import static com.sonatype.insight.brain.api.v2.service.ApiArtifactoryConnectionService.MISSING_CREDENTIALS_ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.api.v2.dto.ApiArtifactoryConnectionStatusRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiArtifactoryConnectionStatusResponseDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiArtifactoryConnectionDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiOwnerArtifactoryConnectionDTO;
import com.sonatype.insight.brain.artifactory.ArtifactoryClient;
import com.sonatype.insight.brain.artifactory.ArtifactoryClientFactory;
import com.sonatype.insight.brain.artifactory.ArtifactoryClientFactory.ArtifactoryClientBuilder;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.artifactory.ArtifactoryConnectionDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.artifactory.ArtifactoryConnection;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.error.exception.NotFoundException;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response.StatusType;
import java.io.IOException;
import java.util.Arrays;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mock;

import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class ApiArtifactoryConnectionServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiArtifactoryConnectionService artifactoryConnectionService;

  @Inject
  private OwnerDAO ownerDAO;

  @Inject
  private ArtifactoryConnectionDAO dao;

  @Inject
  private OrganizationDAO organizationDAO;

  @Inject
  private ApplicationDAO applicationDAO;

  @Inject
  private PasswordHandler passwordHandler;

  @Mock
  private ArtifactoryClientFactory mockFactory;

  @Mock
  private ArtifactoryClientBuilder mockBuilder;

  @Mock
  private ArtifactoryClient client;

  @After
  public void after() {
    Organization rootOrganization = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    rootOrganization.setArtifactoryConnectionEnabled(true);
    rootOrganization.setAllowArtifactoryConnectionOverride(true);
    organizationDAO.update(rootOrganization);
  }

  @Test
  public void testGetArtifactoryConnection_Organization() {
    Organization org = tempEntity.newOrganization();
    ArtifactoryConnection artifactoryConnection = tempEntity.newArtifactoryConnection(
        org.getId(), "url1", "user1", "pass1".toCharArray());

    ApiArtifactoryConnectionDTO dto =
        artifactoryConnectionService.getArtifactoryConnection(
            org.getType(), org.getId(), artifactoryConnection.getId());

    assertArtifactoryConnectionDTO(artifactoryConnection, dto);
  }

  @Test
  public void testGetArtifactoryConnection_Application() {
    Application app = tempEntity.newApplicationWithParent();
    ArtifactoryConnection artifactoryConnection = tempEntity.newArtifactoryConnection(
        app.getId(), "url1", "user1", "pass1".toCharArray());

    ApiArtifactoryConnectionDTO dto =
        artifactoryConnectionService.getArtifactoryConnection(
            app.getType(), app.getId(), artifactoryConnection.getId());

    assertArtifactoryConnectionDTO(artifactoryConnection, dto);
  }

  @Test
  public void testGetArtifactoryConnection_DoesNotExist() {
    Application app = tempEntity.newApplicationWithParent();
    String appId = app.getId();
    String artifactoryConnectionId = "doesNotExist";

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> artifactoryConnectionService
        .getArtifactoryConnection(OwnerType.APPLICATION, appId, artifactoryConnectionId))
        .withMessageContaining(
            String.format(ARTIFACTORY_CONNECTION_NOT_FOUND_ERROR, artifactoryConnectionId, OwnerType.APPLICATION,
                appId));
  }

  private void assertArtifactoryConnectionDTO(ArtifactoryConnection expected, ApiArtifactoryConnectionDTO actual) {
    assertThat(actual.artifactoryConnectionId).isEqualTo(expected.getId());
    assertThat(actual.ownerType).isEqualTo(ownerDAO.getById(expected.getOwnerId()).getType());
    assertThat(actual.ownerId).isEqualTo(expected.getOwnerId());
    assertThat(actual.isAnonymous).isEqualTo(expected.getUsername() == null);
    assertThat(actual.baseUrl).isEqualTo(expected.getBaseUrl());
    assertThat(actual.username).isEqualTo(expected.getUsername());
    assertThat(actual.password).isNull();
  }

  @Test
  public void testGetOwnerArtifactoryConnection_Organization() {
    Organization org = tempEntity.newOrganization();
    org.setArtifactoryConnectionEnabled(true);
    organizationDAO.update(org);
    testGetOwnerArtifactoryConnections(org.getId(), OwnerType.ORGANIZATION);
  }

  @Test
  public void testGetOwnerArtifactoryConnection_Organization_Inherit() {
    Organization org = tempEntity.newOrganization();
    tempEntity.newArtifactoryConnection(org.getId(), "url1", "user1", "pass1".toCharArray());
    // add one to the ROOT which will be the effective connection
    tempEntity.newArtifactoryConnection(
        Organization.ROOT_ORGANIZATION_ID, "url2", "user2", "pass2".toCharArray());
    ApiArtifactoryConnectionDTO connection = artifactoryConnectionService
        .getOwnerArtifactoryConnection(OwnerType.ORGANIZATION, org.getId(), true).artifactoryConnection;
    assertThat(connection).extracting("baseUrl", "username", "ownerType", "ownerId")
        .containsExactly("url2", "user2", OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testGetOwnerArtifactoryConnection_Organization_Inherit_Disabled() {
    Organization org = tempEntity.newOrganization();
    tempEntity.newArtifactoryConnection(org.getId(), "url1", "user1", "pass1".toCharArray());
    org.setArtifactoryConnectionEnabled(false);
    organizationDAO.update(org);

    ApiArtifactoryConnectionDTO connection = artifactoryConnectionService
        .getOwnerArtifactoryConnection(OwnerType.ORGANIZATION, org.getId(), true).artifactoryConnection;
    assertThat(connection).extracting("baseUrl", "username", "ownerType", "ownerId")
        .containsExactly("url1", "user1", OwnerType.ORGANIZATION, org.getId());
  }

  @Test
  public void testGetOwnerArtifactoryConnection_Root_Inherit_Disabled() {
    Organization rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newArtifactoryConnection(rootOrg.getId(), "url2", "user2", "pass2".toCharArray());
    rootOrg.setArtifactoryConnectionEnabled(null);
    organizationDAO.update(rootOrg);

    ApiArtifactoryConnectionDTO connection = artifactoryConnectionService
        .getOwnerArtifactoryConnection(OwnerType.ORGANIZATION, rootOrg.getId(), true).artifactoryConnection;
    assertThat(connection).extracting("baseUrl", "username", "ownerType", "ownerId")
        .containsExactly("url2", "user2", OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testGetOwnerArtifactoryConnection_Application() {
    Application app = tempEntity.newApplicationWithParent();
    app.setArtifactoryConnectionEnabled(true);
    applicationDAO.update(app);
    testGetOwnerArtifactoryConnections(app.getId(), OwnerType.APPLICATION);
  }

  @Test
  public void testGetOwnerArtifactoryConnection_Application_Inherit() {
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newArtifactoryConnection(app.getId(), "url1", "user1", "pass1".toCharArray());
    // add one to the ROOT which will be the effective connection
    tempEntity.newArtifactoryConnection(
        Organization.ROOT_ORGANIZATION_ID, "url2", "user2", "pass2".toCharArray());
    ApiArtifactoryConnectionDTO connection = artifactoryConnectionService
        .getOwnerArtifactoryConnection(OwnerType.APPLICATION, app.getId(), true).artifactoryConnection;
    assertThat(connection).extracting("baseUrl", "username", "ownerType", "ownerId")
        .containsExactly("url2", "user2", OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testGetOwnerArtifactoryConnection_Application_Inherit_Disabled() {
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newArtifactoryConnection(app.getId(), "url1", "user1", "pass1".toCharArray());
    app.setArtifactoryConnectionEnabled(false);
    applicationDAO.update(app);

    ApiArtifactoryConnectionDTO connection = artifactoryConnectionService
        .getOwnerArtifactoryConnection(OwnerType.APPLICATION, app.getId(), true).artifactoryConnection;
    assertThat(connection).extracting("baseUrl", "username", "ownerType", "ownerId")
        .containsExactly("url1", "user1", OwnerType.APPLICATION, app.getId());
  }

  private void testGetOwnerArtifactoryConnections(final String id, final OwnerType ownerType) {
    tempEntity.newArtifactoryConnection(id, "url1", "user1", "pass1".toCharArray());

    Owner owner = ownerDAO.getById(id);
    ApiArtifactoryConnectionDTO connection =
        artifactoryConnectionService.getOwnerArtifactoryConnection(ownerType, id, false).artifactoryConnection;
    assertThat(connection).extracting("baseUrl", "username", "ownerType", "ownerId")
        .containsExactlyInAnyOrder("url1", "user1", owner.getType(), owner.getId());
  }

  @Test
  public void testGetOwnerArtifactoryConnection_InheritTrue_Application() {
    tempEntity.newArtifactoryConnection("other", "url1", "user1", "pass1".toCharArray());
    Organization rootOrganization = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    String rootOrgId = rootOrganization.getId();
    String orgId = organization.getId();
    String appId = application.getId();

    // None
    assertThat(artifactoryConnectionService.getOwnerArtifactoryConnection(OwnerType.APPLICATION, appId,
        true).artifactoryConnection).isNull();

    // Only root org
    rootOrganization.setAllowArtifactoryConnectionOverride(false);
    organizationDAO.update(rootOrganization);
    ArtifactoryConnection rootOrgArtifactoryConnection = tempEntity.newArtifactoryConnection(
        rootOrgId, "url1", "user1", "pass1".toCharArray());
    assertThat(artifactoryConnectionService.getOwnerArtifactoryConnection(OwnerType.APPLICATION, appId,
        true).artifactoryConnection)
            .extracting("ownerType", "ownerId")
            .containsExactly(OwnerType.ORGANIZATION, rootOrgId);

    // Root org and org
    rootOrganization.setAllowArtifactoryConnectionOverride(true);
    organizationDAO.update(rootOrganization);
    organization.setAllowArtifactoryConnectionOverride(false);
    organization.setArtifactoryConnectionEnabled(true);
    organizationDAO.update(organization);
    ArtifactoryConnection orgArtifactoryConnection = tempEntity.newArtifactoryConnection(
        orgId, "url1", "user1", "pass1".toCharArray());
    assertThat(artifactoryConnectionService.getOwnerArtifactoryConnection(OwnerType.APPLICATION, appId,
        true).artifactoryConnection)
            .extracting("ownerType", "ownerId")
            .containsExactly(OwnerType.ORGANIZATION, organization.getId());

    // Root org, org, and app
    rootOrganization.setAllowArtifactoryConnectionOverride(true);
    organizationDAO.update(rootOrganization);
    organization.setAllowArtifactoryConnectionOverride(true);
    organizationDAO.update(organization);
    ArtifactoryConnection appArtifactoryConnection = tempEntity.newArtifactoryConnection(
        appId, "url1", "user1", "pass1".toCharArray());
    application.setArtifactoryConnectionEnabled(true);
    applicationDAO.update(application);
    assertThat(artifactoryConnectionService.getOwnerArtifactoryConnection(OwnerType.APPLICATION, appId,
        true).artifactoryConnection)
            .extracting("ownerType", "ownerId")
            .containsExactly(OwnerType.APPLICATION, appId);

    // Org and app
    dao.delete(rootOrgArtifactoryConnection);
    assertThat(artifactoryConnectionService.getOwnerArtifactoryConnection(OwnerType.APPLICATION, appId,
        true).artifactoryConnection)
            .extracting("ownerType", "ownerId")
            .containsExactly(OwnerType.APPLICATION, appId);

    // Only app
    dao.delete(orgArtifactoryConnection);
    assertThat(artifactoryConnectionService.getOwnerArtifactoryConnection(OwnerType.APPLICATION, appId,
        true).artifactoryConnection)
            .extracting("ownerType", "ownerId")
            .containsExactly(OwnerType.APPLICATION, appId);

    // Root org and app
    rootOrgArtifactoryConnection = tempEntity.newArtifactoryConnection(
        rootOrgId, "url1", "user1", "pass1".toCharArray());
    assertThat(artifactoryConnectionService.getOwnerArtifactoryConnection(OwnerType.APPLICATION, appId,
        true).artifactoryConnection)
            .extracting("ownerType", "ownerId")
            .containsExactly(OwnerType.APPLICATION, appId);

    // Only org
    dao.delete(rootOrgArtifactoryConnection);
    dao.delete(appArtifactoryConnection);
    tempEntity.newArtifactoryConnection(
        orgId, "url1", "user1", "pass1".toCharArray());
    organization.setAllowArtifactoryConnectionOverride(false);
    organizationDAO.update(organization);
    assertThat(artifactoryConnectionService.getOwnerArtifactoryConnection(OwnerType.APPLICATION, appId,
        true).artifactoryConnection)
            .extracting("ownerType", "ownerId")
            .containsExactly(OwnerType.ORGANIZATION, orgId);
  }

  @Test
  public void testGetOwnerArtifactoryConnection_InheritTrue_Organization() {
    tempEntity.newArtifactoryConnection("other", "url1", "user1", "pass1".toCharArray());
    Organization rootOrganization = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    Organization organization = tempEntity.newOrganization();
    String rootOrgId = rootOrganization.getId();
    String orgId = organization.getId();

    // None
    assertThat(artifactoryConnectionService.getOwnerArtifactoryConnection(OwnerType.ORGANIZATION, orgId,
        true).artifactoryConnection)
            .isNull();

    // Only root org
    rootOrganization.setAllowArtifactoryConnectionOverride(false);
    organizationDAO.update(rootOrganization);
    ArtifactoryConnection rootOrgArtifactoryConnection = tempEntity.newArtifactoryConnection(
        rootOrgId, "url1", "user1", "pass1".toCharArray());
    assertThat(artifactoryConnectionService.getOwnerArtifactoryConnection(OwnerType.ORGANIZATION, orgId,
        true).artifactoryConnection)
            .extracting("ownerType", "ownerId")
            .containsExactly(OwnerType.ORGANIZATION, rootOrgId);

    // Root org and org
    rootOrganization.setAllowArtifactoryConnectionOverride(true);
    organizationDAO.update(rootOrganization);
    tempEntity.newArtifactoryConnection(orgId, "url1", "user1", "pass1".toCharArray());
    organization.setArtifactoryConnectionEnabled(true);
    organizationDAO.update(organization);
    assertThat(artifactoryConnectionService.getOwnerArtifactoryConnection(OwnerType.ORGANIZATION, orgId,
        true).artifactoryConnection)
            .extracting("ownerType", "ownerId")
            .containsExactly(OwnerType.ORGANIZATION, orgId);

    // Only org
    dao.delete(rootOrgArtifactoryConnection);
    assertThat(artifactoryConnectionService.getOwnerArtifactoryConnection(OwnerType.ORGANIZATION, orgId,
        true).artifactoryConnection)
            .extracting("ownerType", "ownerId")
            .containsExactly(OwnerType.ORGANIZATION, orgId);
  }

  @Test
  public void testGetOwnerArtifactoryConnection_InheritTrue_RootOrganization() {
    tempEntity.newArtifactoryConnection("other", "url1", "user1", "pass1".toCharArray());
    String rootOrgId = Organization.ROOT_ORGANIZATION_ID;

    // None
    assertThat(artifactoryConnectionService.getOwnerArtifactoryConnection(OwnerType.ORGANIZATION, rootOrgId,
        true).artifactoryConnection)
            .isNull();

    // Only root org
    tempEntity.newArtifactoryConnection(rootOrgId, "url1", "user1", "pass1".toCharArray());
    assertThat(artifactoryConnectionService.getOwnerArtifactoryConnection(OwnerType.ORGANIZATION, rootOrgId,
        true).artifactoryConnection)
            .extracting("ownerType", "ownerId")
            .containsExactly(OwnerType.ORGANIZATION, rootOrgId);
  }

  @Test
  public void testAddArtifactoryConnection_Organization() {
    Organization org = tempEntity.newOrganization();
    testAddArtifactoryConnection(OwnerType.ORGANIZATION, org.getId());
  }

  @Test
  public void testAddArtifactoryConnection_Application() {
    Application app = tempEntity.newApplicationWithParent();
    testAddArtifactoryConnection(OwnerType.APPLICATION, app.getId());
  }

  private void testAddArtifactoryConnection(OwnerType ownerType, String id) {
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.baseUrl = "baseUrl";
    dto.username = "user";
    dto.password = "pass";

    ApiArtifactoryConnectionDTO createdDto =
        artifactoryConnectionService.addArtifactoryConnection(ownerType, id, dto);

    assertThat(createdDto.artifactoryConnectionId).isNotNull();
    assertThat(createdDto.password).isNull();
    assertThat(createdDto.ownerType).isEqualTo(ownerType);
    assertThat(createdDto.ownerId).isEqualTo(id);
    ArtifactoryConnection savedConfig = dao.getByIdAndOwnerId(createdDto.artifactoryConnectionId, id);
    assertThat(passwordHandler.decryptPassword(savedConfig.getPassword())).isEqualTo(dto.password.toCharArray());
  }

  @Test
  public void testAddArtifactoryConnection_MissingPayload() {
    String orgId = tempEntity.newOrganization().getId();
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> artifactoryConnectionService.addArtifactoryConnection(OwnerType.ORGANIZATION, orgId, null))
        .withMessage("missing artifactory base URL");
  }

  @Test
  public void testAddArtifactoryConnection_MissingBaseUrl() {
    String orgId = tempEntity.newOrganization().getId();
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> artifactoryConnectionService.addArtifactoryConnection(OwnerType.ORGANIZATION, orgId, dto))
        .withMessage("missing artifactory base URL");
  }

  @Test
  public void testAddArtifactoryConnection_MissingUsername() {
    String orgId = tempEntity.newOrganization().getId();
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.baseUrl = "baseUrl";
    dto.password = "pass";
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> artifactoryConnectionService.addArtifactoryConnection(OwnerType.ORGANIZATION, orgId, dto))
        .withMessage(MISSING_CREDENTIALS_ERROR);
  }

  @Test
  public void testAddArtifactoryConnection_MissingPassword() {
    String orgId = tempEntity.newOrganization().getId();
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.baseUrl = "baseUrl";
    dto.username = "user";
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> artifactoryConnectionService.addArtifactoryConnection(OwnerType.ORGANIZATION, orgId, dto))
        .withMessage(MISSING_CREDENTIALS_ERROR);
  }

  @Test
  public void testAddArtifactoryConnection_ConfigurationAlreadyExists() {
    String orgId = tempEntity.newOrganization().getId();
    tempEntity.newArtifactoryConnection(orgId, "baseUrl", "user", "pass".toCharArray());
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.baseUrl = "baseUrl";
    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(() -> artifactoryConnectionService.addArtifactoryConnection(OwnerType.ORGANIZATION, orgId, dto))
        .withMessage("artifactory connection configuration exists for organization with id: " + orgId);
  }

  @Test
  public void testUpdateArtifactoryConnection_Organization() {
    Organization org = tempEntity.newOrganization();
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.baseUrl = "updated baseUrl";
    dto.username = "user2";
    dto.password = "pass2";
    testUpdateArtifactoryConnection(org.getId(), OwnerType.ORGANIZATION, dto, false);
  }

  @Test
  public void testUpdateArtifactoryConnection_Application() {
    Application app = tempEntity.newApplicationWithParent();
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.baseUrl = "updated baseUrl";
    dto.username = "user2";
    dto.password = "pass2";
    testUpdateArtifactoryConnection(app.getId(), OwnerType.APPLICATION, dto, false);
  }

  @Test
  public void testUpdateArtifactoryConnection_BaseUrlOnly_ExistingAnonymousConnection() {
    Application app = tempEntity.newApplicationWithParent();
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.baseUrl = "updated baseUrl";
    testUpdateArtifactoryConnection(app.getId(), OwnerType.APPLICATION, dto, true);
  }

  @Test
  public void testUpdateArtifactoryConnection_BaseUrlOnly_ForExistingConnectionWithCredentials() {
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.baseUrl = "updated baseUrl";
    testUpdateArtifactoryConnectionMissingAuthData(dto);
  }

  @Test
  public void testUpdateArtifactoryConnection_MissingPassword() {
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.username = "user2";
    testUpdateArtifactoryConnectionMissingAuthData(dto);
  }

  @Test
  public void testUpdateArtifactoryConnection_MissingUsername() {
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.password = "pass";
    testUpdateArtifactoryConnectionMissingAuthData(dto);
  }

  private void testUpdateArtifactoryConnectionMissingAuthData(final ApiArtifactoryConnectionDTO dto) {
    Application app = tempEntity.newApplicationWithParent();
    String appId = app.getId();
    String connectionId = tempEntity.newArtifactoryConnection(appId, "baseUrl", "user",
        passwordHandler.encryptPassword("pass".toCharArray())).getId();
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> artifactoryConnectionService.updateArtifactoryConnection(OwnerType.APPLICATION, appId,
            connectionId, dto))
        .withMessage(MISSING_CREDENTIALS_ERROR);
  }

  private void testUpdateArtifactoryConnection(
      String id,
      OwnerType ownerType,
      ApiArtifactoryConnectionDTO dto,
      boolean anonymous)
  {
    ArtifactoryConnection existingConnection;
    if (anonymous) {
      existingConnection = tempEntity.newArtifactoryConnection(id, "baseUrl", null, null);
    }
    else {
      existingConnection = tempEntity.newArtifactoryConnection(id, "baseUrl", "user",
          passwordHandler.encryptPassword("pass".toCharArray()));
    }

    ApiArtifactoryConnectionDTO updated =
        artifactoryConnectionService.updateArtifactoryConnection(ownerType, id,
            existingConnection.getId(), dto);
    assertThat(updated.baseUrl).isEqualTo(dto.baseUrl);
    assertThat(updated.username).isEqualTo(dto.username);
    assertThat(updated.password).isNull();
    assertThat(updated.ownerId).isEqualTo(id);
    assertThat(updated.ownerType).isEqualTo(ownerType);

    ArtifactoryConnection storedConnection = dao.getById(existingConnection.getId());

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
  public void testUpdateArtifactoryConnection_ToAnonymous() {
    String orgId = tempEntity.newOrganization().getId();
    ArtifactoryConnection artifactoryConnection =
        tempEntity.newArtifactoryConnection(orgId, "baseUrl", "user", "pass".toCharArray());
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.isAnonymous = true;

    ApiArtifactoryConnectionDTO result =
        artifactoryConnectionService.updateArtifactoryConnection(OwnerType.ORGANIZATION, orgId,
            artifactoryConnection.getId(), dto);

    ArtifactoryConnection storedArtifactoryConnection = dao.getById(result.artifactoryConnectionId);
    assertThat(storedArtifactoryConnection.getOwnerId()).isEqualTo(artifactoryConnection.getOwnerId());
    assertThat(storedArtifactoryConnection.getBaseUrl()).isEqualTo(artifactoryConnection.getBaseUrl());
    assertThat(storedArtifactoryConnection.getUsername()).isNull();
    assertThat(storedArtifactoryConnection.getPassword()).isNull();
  }

  @Test
  public void testUpdateArtifactoryConnection_ToAnonymousAndDifferentBaseUrl() {
    String orgId = tempEntity.newOrganization().getId();
    ArtifactoryConnection artifactoryConnection =
        tempEntity.newArtifactoryConnection(orgId, "baseUrl", "user", "pass".toCharArray());
    String artifactoryConnectionId = artifactoryConnection.getId();
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.baseUrl = artifactoryConnection.getBaseUrl() + "2";
    dto.isAnonymous = true;

    ApiArtifactoryConnectionDTO result =
        artifactoryConnectionService.updateArtifactoryConnection(OwnerType.ORGANIZATION, orgId,
            artifactoryConnectionId, dto);

    ArtifactoryConnection storedArtifactoryConnection = dao.getById(result.artifactoryConnectionId);
    assertThat(storedArtifactoryConnection.getOwnerId()).isEqualTo(artifactoryConnection.getOwnerId());
    assertThat(storedArtifactoryConnection.getBaseUrl()).isEqualTo(dto.baseUrl);
    assertThat(storedArtifactoryConnection.getUsername()).isNull();
    assertThat(storedArtifactoryConnection.getPassword()).isNull();
  }

  @Test
  public void testUpdateArtifactoryConnection_MissingPayload() {
    String orgId = tempEntity.newOrganization().getId();
    ArtifactoryConnection existingConnection =
        tempEntity.newArtifactoryConnection(orgId, "baseUrl", "user", "pass".toCharArray());
    String existingConnectionId = existingConnection.getId();
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> artifactoryConnectionService.updateArtifactoryConnection(OwnerType.ORGANIZATION, orgId,
            existingConnectionId, null))
        .withMessage(MISSING_CONNECTION_DATA_ERROR);
  }

  @Test
  public void testUpdateArtifactoryConnection_MissingUpdateData() {
    String orgId = tempEntity.newOrganization().getId();
    ArtifactoryConnection existingConnection =
        tempEntity.newArtifactoryConnection(orgId, "baseUrl", "user", "pass".toCharArray());
    String existingConnectionId = existingConnection.getId();
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> artifactoryConnectionService.updateArtifactoryConnection(OwnerType.ORGANIZATION, orgId,
            existingConnectionId, dto))
        .withMessage(MISSING_CONNECTION_DATA_ERROR);
  }

  @Test
  public void testUpdateArtifactoryConnection_InvalidOwnerId() {
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.baseUrl = "url";
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> artifactoryConnectionService.updateArtifactoryConnection(OwnerType.ORGANIZATION, "blah",
            "someId", dto))
        .withMessage("no artifactory connections found with connection id: someId for organization having id: blah");
  }

  @Test
  public void testUpdateArtifactoryConnection_ConfigurationNotFound() {
    String orgId = tempEntity.newOrganization().getId();
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.artifactoryConnectionId = "nonExistentId";
    dto.baseUrl = "baseUrl";
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> artifactoryConnectionService.updateArtifactoryConnection(OwnerType.ORGANIZATION, orgId,
            dto.artifactoryConnectionId, dto))
        .withMessage(
            "no artifactory connections found with connection id: nonExistentId for organization having id: " + orgId);
  }

  @Test
  public void testDeleteArtifactoryConnection_Organization() {
    Organization org = tempEntity.newOrganization();
    testDeleteArtifactoryConnection(org.getId(), OwnerType.ORGANIZATION);
  }

  @Test
  public void testDeleteArtifactoryConnection_Application() {
    Application app = tempEntity.newApplicationWithParent();
    testDeleteArtifactoryConnection(app.getId(), OwnerType.APPLICATION);
  }

  private void testDeleteArtifactoryConnection(final String id, final OwnerType ownerType) {
    ArtifactoryConnection connection =
        tempEntity.newArtifactoryConnection(id, "baseUrl", "user", "pass".toCharArray());

    artifactoryConnectionService.deleteArtifactoryConnection(ownerType, id, connection.getId());
    assertThat(dao.getById(connection.getId())).isNull();
  }

  @Test
  public void testDeleteArtifactoryConnection_InvalidOwnerId() {
    Application app = tempEntity.newApplicationWithParent();
    String connectionId =
        tempEntity.newArtifactoryConnection(app.getId(), "baseUrl", "user", "pass".toCharArray()).getId();

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> artifactoryConnectionService.deleteArtifactoryConnection(OwnerType.APPLICATION, "blah",
            connectionId))
        .withMessage("no artifactory connections found with connection id: " + connectionId
            + " for application having id: blah");
  }

  @Test
  public void testDeleteArtifactoryConnection_InvalidConnectionId() {
    String appId = tempEntity.newApplicationWithParent().getId();
    tempEntity.newArtifactoryConnection(appId, "baseUrl", "user", "pass".toCharArray());

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> artifactoryConnectionService.deleteArtifactoryConnection(OwnerType.APPLICATION, appId,
            "blah"))
        .withMessage("no artifactory connections found with connection id: blah" +
            " for application having id: " + appId);
  }

  @Test
  public void testDeleteArtifactoryConnection_ConnectionAndOwnerMismatch() {
    String app1Id = tempEntity.newApplicationWithParent().getId();
    String app2Id = tempEntity.newApplicationWithParent().getId();
    ArtifactoryConnection connection =
        tempEntity.newArtifactoryConnection(app1Id, "baseUrl", "user", "pass".toCharArray());
    String connectionId = connection.getId();

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> artifactoryConnectionService.deleteArtifactoryConnection(OwnerType.APPLICATION, app2Id,
            connectionId))
        .withMessage("no artifactory connections found with connection id: " + connectionId
            + " for application having id: " + app2Id);
  }

  @Test
  public void testTestArtifactoryConnection_Anonymous_Success() throws Exception {
    setupMocks();
    when(client.getServerStatusViaQueryParam()).thenReturn(Status.OK);
    testTestArtifactoryConnection(Status.OK, true);
  }

  @Test
  public void testTestArtifactoryConnection_Anonymous_Unauthorized() throws Exception {
    setupMocks();
    when(client.getServerStatusViaQueryParam()).thenReturn(Status.UNAUTHORIZED);
    testTestArtifactoryConnection(Status.UNAUTHORIZED, true);
  }

  @Test
  public void testTestArtifactoryConnection_Anonymous_Exception() throws Exception {
    setupMocks();
    when(client.getServerStatusViaQueryParam()).thenThrow(new IOException("error"));
    testTestArtifactoryConnection(Status.BAD_GATEWAY, true);
  }

  @Test
  public void testTestArtifactoryConnection_NonAnonymous_Success() throws Exception {
    setupMocks();
    when(client.getServerStatusViaAQL()).thenReturn(Status.OK);
    testTestArtifactoryConnection(Status.OK, false);
  }

  @Test
  public void testTestArtifactoryConnection_NonAnonymous_Unauthorized() throws Exception {
    setupMocks();
    when(client.getServerStatusViaAQL()).thenReturn(Status.UNAUTHORIZED);
    testTestArtifactoryConnection(Status.UNAUTHORIZED, false);
  }

  @Test
  public void testTestArtifactoryConnection_NonAnonymous_Exception() throws Exception {
    setupMocks();
    when(client.getServerStatusViaAQL()).thenThrow(new IOException("error"));
    testTestArtifactoryConnection(Status.BAD_GATEWAY, false);
  }

  private void testTestArtifactoryConnection(Status status, boolean anonymous) {
    String appId = tempEntity.newApplicationWithParent().getId();

    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.baseUrl = "baseUrl";
    if (!anonymous) {
      dto.username = "user";
      dto.password = "pass";
    }
    StatusType response = artifactoryConnectionService.testArtifactoryConnection(OwnerType.APPLICATION, appId, dto);

    assertThat(response).isEqualTo(status);
  }

  @Test
  public void testTestArtifactoryConnection_MissingBaseUrl() {
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    testTestArtifactoryConnection_MissingData(dto, "missing artifactory base URL");
  }

  @Test
  public void testTestArtifactoryConnection_MissingPassword() {
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.baseUrl = "baseUrl";
    dto.username = "user";
    testTestArtifactoryConnection_MissingData(dto, MISSING_CREDENTIALS_ERROR);
  }

  @Test
  public void testTestArtifactoryConnection_MissingUsername() {
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.baseUrl = "baseUrl";
    dto.password = "pass";
    testTestArtifactoryConnection_MissingData(dto, MISSING_CREDENTIALS_ERROR);
  }

  @Test
  public void testGetOwnerArtifactoryConnectionStatus() {
    Owner root = ownerDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());

    setupOwner(root, true, true);
    setupOwner(org, true, true);
    setupOwner(app, true, null);
    testGetOwnerArtifactoryConnectionStatus(root, true, null, true, true, null);
    testGetOwnerArtifactoryConnectionStatus(org, true, null, true, true, null);
    testGetOwnerArtifactoryConnectionStatus(app, true, null, false, true, null);

    setupOwner(root, true, true);
    setupOwner(org, true, true);
    setupOwner(app, false, null);
    testGetOwnerArtifactoryConnectionStatus(root, true, null, true, true, null);
    testGetOwnerArtifactoryConnectionStatus(org, true, null, true, true, null);
    testGetOwnerArtifactoryConnectionStatus(app, false, null, false, true, null);

    setupOwner(root, true, false);
    setupOwner(org, true, true);
    setupOwner(app, true, null);
    testGetOwnerArtifactoryConnectionStatus(root, true, null, false, true, null);
    testGetOwnerArtifactoryConnectionStatus(org, true, root.getName(), true, false, true);
    testGetOwnerArtifactoryConnectionStatus(app, true, root.getName(), false, false, true);

    setupOwner(root, false, false);
    setupOwner(org, true, true);
    setupOwner(app, true, null);
    testGetOwnerArtifactoryConnectionStatus(root, false, null, false, true, null);
    testGetOwnerArtifactoryConnectionStatus(org, true, root.getName(), true, false, false);
    testGetOwnerArtifactoryConnectionStatus(app, true, root.getName(), false, false, false);

    setupOwner(root, true, true);
    setupOwner(org, true, false);
    setupOwner(app, true, null);
    testGetOwnerArtifactoryConnectionStatus(root, true, null, true, true, null);
    testGetOwnerArtifactoryConnectionStatus(org, true, null, false, true, null);
    testGetOwnerArtifactoryConnectionStatus(app, true, org.getName(), false, false, true);

    setupOwner(root, true, true);
    setupOwner(org, false, false);
    setupOwner(app, true, null);
    testGetOwnerArtifactoryConnectionStatus(root, true, null, true, true, null);
    testGetOwnerArtifactoryConnectionStatus(org, false, null, false, true, null);
    testGetOwnerArtifactoryConnectionStatus(app, true, org.getName(), false, false, false);

    setupOwner(root, true, true);
    setupOwner(org, null, true);
    setupOwner(app, null, null);
    testGetOwnerArtifactoryConnectionStatus(root, true, null, true, true, null);
    testGetOwnerArtifactoryConnectionStatus(org, null, root.getName(), true, true, true);
    testGetOwnerArtifactoryConnectionStatus(app, null, root.getName(), false, true, true);

    setupOwner(root, true, true);
    setupOwner(org, false, false);
    setupOwner(app, null, null);
    testGetOwnerArtifactoryConnectionStatus(root, true, null, true, true, null);
    testGetOwnerArtifactoryConnectionStatus(org, false, null, false, true, null);
    testGetOwnerArtifactoryConnectionStatus(app, null, org.getName(), false, false, false);

    setupOwner(root, false, false);
    setupOwner(org, false, false);
    setupOwner(app, null, null);
    testGetOwnerArtifactoryConnectionStatus(root, false, null, false, true, null);
    testGetOwnerArtifactoryConnectionStatus(org, false, root.getName(), false, false, false);
    testGetOwnerArtifactoryConnectionStatus(app, null, root.getName(), false, false, false);
    setupOwner(org, null, false);
    testGetOwnerArtifactoryConnectionStatus(app, null, root.getName(), false, false, false);
    setupOwner(org, null, true);
    testGetOwnerArtifactoryConnectionStatus(app, null, root.getName(), false, false, false);

    setupOwner(root, null, true);
    setupOwner(org, null, true);
    setupOwner(app, null, null);
    testGetOwnerArtifactoryConnectionStatus(root, null, null, true, true, null);
    testGetOwnerArtifactoryConnectionStatus(org, null, root.getName(), true, true, null);
    testGetOwnerArtifactoryConnectionStatus(app, null, root.getName(), false, true, null);
  }

  private void setupOwner(Owner owner, Boolean enabled, Boolean allowOverride) {
    switch (owner.getType()) {
      case APPLICATION: {
        Application application = (Application) owner;
        application.setArtifactoryConnectionEnabled(enabled);
        applicationDAO.update(application);
        break;
      }
      case ORGANIZATION: {
        Organization organization = (Organization) owner;
        organization.setArtifactoryConnectionEnabled(enabled);
        organization.setAllowArtifactoryConnectionOverride(allowOverride);
        organizationDAO.update(organization);
        break;
      }
      default: {
        fail("Unrecognized owner type");
      }
    }
  }

  private void testGetOwnerArtifactoryConnectionStatus(
      Owner owner,
      Boolean expectedEnabled,
      String expectedInheritedFromOrganizationName,
      boolean expectedAllowOverride,
      boolean expectedAllowChange,
      Boolean expectedInheritedEnabled)
  {
    ApiArtifactoryConnectionStatusResponseDTO result =
        artifactoryConnectionService.getOwnerArtifactoryConnectionStatus(owner.getType(), owner.getId());
    assertThat(result).isNotNull();
    assertThat(result.enabled).isEqualTo(expectedEnabled);
    assertThat(result.inheritedFromOrganizationName).isEqualTo(expectedInheritedFromOrganizationName);
    assertThat(result.allowOverride).isEqualTo(expectedAllowOverride);
    assertThat(result.allowChange).isEqualTo(expectedAllowChange);
    assertThat(result.inheritedFromOrgEnabled).isEqualTo(expectedInheritedEnabled);
  }

  @Test
  public void testUpdateOwnerArtifactoryConnectionStatus_Organization() {
    ApiArtifactoryConnectionStatusRequestDTO dto = new ApiArtifactoryConnectionStatusRequestDTO();
    dto.allowOverride = false;
    dto.enabled = false;
    testUpdateOwnerArtifactoryConnectionStatus_Organization(dto);
  }

  @Test
  public void testUpdateOwnerArtifactoryConnectionStatus_Organization_Inherit() {
    ApiArtifactoryConnectionStatusRequestDTO dto = new ApiArtifactoryConnectionStatusRequestDTO();
    dto.allowOverride = false;
    dto.enabled = null;
    testUpdateOwnerArtifactoryConnectionStatus_Organization(dto);
  }

  private void testUpdateOwnerArtifactoryConnectionStatus_Organization(ApiArtifactoryConnectionStatusRequestDTO dto) {
    String orgId = tempEntity.newOrganization().getId();
    artifactoryConnectionService.updateOwnerArtifactoryConnectionStatus(OwnerType.ORGANIZATION, orgId, dto);

    Organization org = organizationDAO.getByIdNotNull(orgId);
    assertThat(org.isAllowArtifactoryConnectionOverride()).isEqualTo(dto.allowOverride);
    assertThat(org.isArtifactoryConnectionEnabled()).isEqualTo(dto.enabled);
  }

  @Test
  public void testUpdateOwnerArtifactoryConnectionStatus_Application() {
    ApiArtifactoryConnectionStatusRequestDTO dto = new ApiArtifactoryConnectionStatusRequestDTO();
    dto.enabled = false;
    testUpdateOwnerArtifactoryConnectionStatus_Application(dto, false);
  }

  @Test
  public void testUpdateOwnerArtifactoryConnectionStatus_Application_Inherit() {
    ApiArtifactoryConnectionStatusRequestDTO dto = new ApiArtifactoryConnectionStatusRequestDTO();
    dto.enabled = null;
    testUpdateOwnerArtifactoryConnectionStatus_Application(dto, null);
  }

  private void testUpdateOwnerArtifactoryConnectionStatus_Application(
      final ApiArtifactoryConnectionStatusRequestDTO dto,
      final Boolean expectedEnabled)
  {
    Application app = tempEntity.newApplicationWithParent();

    artifactoryConnectionService.updateOwnerArtifactoryConnectionStatus(OwnerType.APPLICATION, app.getId(), dto);

    Application application = applicationDAO.getByIdNotNull(app.getId());
    assertThat(application.isArtifactoryConnectionEnabled()).isEqualTo(expectedEnabled);
  }

  @Test
  public void testUpdateOwnerArtifactoryConnectionStatus_MissingPayload() {
    String orgId = tempEntity.newOrganization().getId();
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> artifactoryConnectionService
                .updateOwnerArtifactoryConnectionStatus(OwnerType.ORGANIZATION, orgId, null))
        .withMessage("missing artifactory connection configuration data for update");
  }

  @Test
  public void testUpdateOwnerArtifactoryConnectionStatus_AllowedForOwner_InvalidOrgId() {
    ApiArtifactoryConnectionStatusRequestDTO dto = new ApiArtifactoryConnectionStatusRequestDTO();
    dto.enabled = true;
    dto.allowOverride = false;
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> artifactoryConnectionService
            .updateOwnerArtifactoryConnectionStatus(OwnerType.ORGANIZATION, "irrelevant", dto))
        .withMessage(
            "Organization with ID irrelevant does not exist.");
  }

  @Test
  public void testUpdateOwnerArtifactoryConnectionStatus_AllowedForOwner_InvalidAppId() {
    ApiArtifactoryConnectionStatusRequestDTO dto = new ApiArtifactoryConnectionStatusRequestDTO();
    dto.enabled = true;
    dto.allowOverride = false;
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> artifactoryConnectionService
            .updateOwnerArtifactoryConnectionStatus(OwnerType.APPLICATION, "irrelevant", dto))
        .withMessage("Application with ID irrelevant does not exist.");
  }

  private void testTestArtifactoryConnection_MissingData(
      final ApiArtifactoryConnectionDTO dto,
      final String exectedMessage)
  {
    String appId = tempEntity.newApplicationWithParent().getId();
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> artifactoryConnectionService.testArtifactoryConnection(OwnerType.APPLICATION, appId, dto))
        .withMessage(exectedMessage);
  }

  @Test
  public void testTestArtifactoryConnection_ByArtifactoryId_DoesNotExist() {
    Organization org = tempEntity.newOrganization();
    String artifactoryConnectionId = "doesNotExist";
    String orgId = org.getId();

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> artifactoryConnectionService
        .testArtifactoryConnection(OwnerType.ORGANIZATION, orgId, artifactoryConnectionId))
        .withMessageContaining(
            String.format(ARTIFACTORY_CONNECTION_NOT_FOUND_ERROR,
                artifactoryConnectionId, OwnerType.ORGANIZATION, orgId));
  }

  @Test
  public void testTestArtifactoryConnection_ByArtifactoryId_WrongOwner() {
    Organization org1 = tempEntity.newOrganization();
    ArtifactoryConnection artifactoryConnection = tempEntity.newArtifactoryConnection(
        org1.getId(), "url1", "user1", "pass1".toCharArray());
    String artifactoryConnectionId = artifactoryConnection.getId();
    Organization org2 = tempEntity.newOrganization();
    String org2Id = org2.getId();

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> artifactoryConnectionService
        .testArtifactoryConnection(OwnerType.ORGANIZATION, org2Id, artifactoryConnectionId))
        .withMessageContaining(String.format(ARTIFACTORY_CONNECTION_NOT_FOUND_ERROR,
            artifactoryConnectionId, OwnerType.ORGANIZATION, org2Id));
  }

  @Test
  public void testTestArtifactoryConnection_ByArtifactoryId_ViaQueryParam() throws Exception {
    setupMocks();
    when(client.getServerStatusViaQueryParam()).thenThrow(new IOException("error"));
    testTestArtifactoryConnection_ByArtifactoryId(true);
  }

  @Test
  public void testTestArtifactoryConnection_ByArtifactoryId_ViaAQL() throws Exception {
    setupMocks();
    when(client.getServerStatusViaAQL()).thenThrow(new IOException("error"));
    testTestArtifactoryConnection_ByArtifactoryId(false);
  }

  private void testTestArtifactoryConnection_ByArtifactoryId(boolean anonymous) {
    String appId = tempEntity.newApplicationWithParent().getId();
    ArtifactoryConnection artifactoryConnection = tempEntity.newArtifactoryConnection(
        appId, "url1", anonymous ? null : "user1", anonymous ? null : "pass1".toCharArray());
    artifactoryConnection.setPassword(passwordHandler.encryptPassword(artifactoryConnection.getPassword()));
    dao.update(artifactoryConnection);

    StatusType response = artifactoryConnectionService.testArtifactoryConnection(OwnerType.APPLICATION, appId,
        artifactoryConnection.getId());

    assertThat(response).isEqualTo(Status.BAD_GATEWAY);
  }

  @Test
  public void testGetOwnerArtifactoryConnections() {
    Application application = tempEntity.newApplicationWithParent();
    application.setArtifactoryConnectionEnabled(true);
    applicationDAO.update(application);
    ArtifactoryConnection artifactoryConnection = tempEntity.newArtifactoryConnection(
        application.getId(), "url1", "user1", "pass1".toCharArray());

    ApiOwnerArtifactoryConnectionDTO dto =
        artifactoryConnectionService.getOwnerArtifactoryConnection(application.getType(), application.getId(),
            false);

    assertThat(dto.artifactoryConnection).isNotNull();
    ApiArtifactoryConnectionDTO connectionDTO = dto.artifactoryConnection;
    assertArtifactoryConnectionDTO(artifactoryConnection, connectionDTO);
    assertThat(dto.artifactoryConnectionStatus.enabled).isTrue();
    assertThat(dto.artifactoryConnectionStatus.inheritedFromOrganizationName).isNull();
    assertThat(dto.artifactoryConnectionStatus.allowOverride).isFalse();
    assertThat(dto.artifactoryConnectionStatus.allowChange).isTrue();
    assertThat(dto.ownerDTO.ownerId).isEqualTo(application.getId());
    assertThat(dto.ownerDTO.ownerName).isEqualTo(application.getName());
    assertThat(dto.ownerDTO.ownerPublicId).isEqualTo(application.getPublicId());
  }

  @Test
  public void testUpdateOwnerArtifactoryConnectionStatus_RootOrgCannotInherit() {
    ApiArtifactoryConnectionStatusRequestDTO dto = new ApiArtifactoryConnectionStatusRequestDTO();

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> artifactoryConnectionService.updateOwnerArtifactoryConnectionStatus(OwnerType.ORGANIZATION,
            Organization.ROOT_ORGANIZATION_ID, dto))
        .withMessageContaining("root organization cannot inherit configuration");
  }

  private void setupMocks() {
    when(mockFactory.create()).thenReturn(mockBuilder);
    when(mockBuilder.forArtifactory(any(), any(), any())).thenReturn(client);
  }
}
