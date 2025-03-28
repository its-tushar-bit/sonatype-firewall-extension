/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiAppliedLicenseOverridesDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseOverrideDTO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dto.audit.LicenseOverrideAudit;
import com.sonatype.insight.brain.license.LicenseOverrideResource;
import com.sonatype.insight.brain.license.LicenseOverrideService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiLicenseOverrideResourceTest extends AbstractResourceTest
{
  private LicenseOverrideDAO licenseOverrideDAO;

  private OwnerDAO ownerDAO;

  private RepositoryManagerDAO repositoryManagerDAO;

  private LicenseDAO licenseDAO;

  @Before
  public void setUp() {
    licenseOverrideDAO = lookup(LicenseOverrideDAO.class);
    ownerDAO = lookup(OwnerDAO.class);
    repositoryManagerDAO = lookup(RepositoryManagerDAO.class);
    licenseDAO = lookup(LicenseDAO.class);
  }

  private HttpRequest restRequest(OwnerType ownerType, String ownerId, String... paths) {
    return restRequest().path(PublicApiPaths.LICENSE_OVERRIDE_RESOURCE_PATH_V2).path(paths).parameter(ownerType,
        ownerId);
  }

  private HttpRequest restRequest(
      OwnerType ownerType,
      String ownerId,
      ComponentIdentifier componentIdentifier,
      String... paths)
  {
    return restRequest().path(LicenseOverrideResource.RESOURCE_PATH).path(paths)
        .query("componentIdentifier", componentIdentifier)
        .parameter(ownerType, ownerId);
  }

  @Test
  public void testCRUD_Application() throws Exception {
    String appPublicId = "LicenseOverrideResourceTest";
    Application application = tempEntity.newApplicationWithParent(appPublicId);

    testCRUD(OwnerType.APPLICATION, appPublicId, application.getId());
  }

  @Test
  public void testCRUD_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization("LicenseOverrideResourceTest");

    testCRUD(OwnerType.ORGANIZATION, organization.getId(), organization.getId());
  }

  @Test
  public void testCRUD_Repository() throws Exception {
    final Repository repository = tempEntity.newRepository();
    testCRUD(OwnerType.REPOSITORY, repository.getId(), repository.getId());
  }

  @Test
  public void testCRUD_RepositoryManager() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    testCRUD(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId(), repositoryManager.getId());
  }

  @Test
  public void testCRUD_RepositoryContainer() throws Exception {
    testCRUD(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID,
        RepositoryContainer.REPOSITORY_CONTAINER_ID);
  }

  @Test
  public void testCRUD_Nuget_Application() throws Exception {
    String appPublicId = "LicenseOverrideResourceTest";
    Application application = tempEntity.newApplicationWithParent(appPublicId);

    testCRUD_Nuget(OwnerType.APPLICATION, appPublicId, application.getId());
  }

  @Test
  public void testCRUD_Nuget_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization("LicenseOverrideResourceTest");

    testCRUD_Nuget(OwnerType.ORGANIZATION, organization.getId(), organization.getId());
  }

  @Test
  public void testCRUD_Nuget_Repository() throws Exception {
    final Repository repository = tempEntity.newRepository();
    testCRUD_Nuget(OwnerType.REPOSITORY, repository.getId(), repository.getId());
  }

  @Test
  public void testCRUD_Nuget_RepositoryContainer() throws Exception {
    testCRUD_Nuget(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID,
        RepositoryContainer.REPOSITORY_CONTAINER_ID);
  }

  private void testCRUD_Nuget(OwnerType ownerType, String ownerPublicId, String ownerId) throws Exception {
    String user = "admin";
    HttpRequest request = restRequest(ownerType, ownerPublicId);

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNugetCoordinates("p1", "v1");
    // Create
    ApiLicenseOverrideDTO licenseOverrideReq = new ApiLicenseOverrideDTO(ownerPublicId,
        "My comment",
        "Apache-2.0",
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier), LicenseOverrideStatus.OVERRIDDEN);

    HttpResponse response = request.body(licenseOverrideReq).post();
    assertResponseStatus(200, response);
    ApiLicenseOverrideDTO licenseOverrideRes = response.getBody(ApiLicenseOverrideDTO.class);
    assertLicenseOverride(ownerId, componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment",
        licenseOverrideRes);
    assertAuditLog(ownerId, user, false /* isDelete */, licenseOverrideRes);

    // Get
    LicenseOverride licenseOverride = licenseOverrideDAO.getByIdNotNull(licenseOverrideRes.id);
    assertLicenseOverride(ownerId, componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment",
        licenseOverride);

    // Update (i.e. add again)
    licenseOverrideReq = new ApiLicenseOverrideDTO(
        ownerId, "My comment updated", "GPL-2.0",
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier), LicenseOverrideStatus.OVERRIDDEN);
    response = request.body(licenseOverrideReq).post();
    assertResponseStatus(200, response);
    licenseOverrideRes = response.getBody(ApiLicenseOverrideDTO.class);
    assertLicenseOverride(ownerId, componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0",
        "My comment updated", licenseOverrideRes);
    assertAuditLog(ownerId, user, false /* isDelete */, licenseOverrideRes);

    // Get
    licenseOverride = licenseOverrideDAO.getByIdNotNull(licenseOverrideRes.id);
    assertLicenseOverride(ownerId, componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0",
        "My comment updated", licenseOverride);

    // Delete
    response = request.subpath(licenseOverrideRes.id).delete();
    assertResponseStatus(204, response);
    assertAuditLog(ownerId, user, true /* isDelete */, licenseOverrideRes);

    // Get
    licenseOverride = licenseOverrideDAO.getById(licenseOverrideRes.id);
    assertThat(licenseOverride).isNull();
  }

  private void testCRUD(OwnerType ownerType, String ownerPublicId, String ownerId) throws Exception {
    String user = "admin";
    HttpRequest request = restRequest(ownerType, ownerPublicId);

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    // Create
    ApiLicenseOverrideDTO licenseOverrideReq = new ApiLicenseOverrideDTO(
        ownerId, "My comment", "Apache-2.0",
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier), LicenseOverrideStatus.OVERRIDDEN);
    HttpResponse response = request.body(licenseOverrideReq).post();
    assertResponseStatus(200, response);
    ApiLicenseOverrideDTO licenseOverrideRes = response.getBody(ApiLicenseOverrideDTO.class);
    assertLicenseOverride(ownerId, componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment",
        licenseOverrideRes);
    assertAuditLog(ownerId, user, false /* isDelete */, licenseOverrideRes);

    // Get
    LicenseOverride licenseOverride = licenseOverrideDAO.getByIdNotNull(licenseOverrideRes.id);
    assertLicenseOverride(ownerId, componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment",
        licenseOverride);

    // Update (i.e. add again)
    licenseOverrideReq = new ApiLicenseOverrideDTO(
        ownerId, "My comment updated", "GPL-2.0",
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier), LicenseOverrideStatus.OVERRIDDEN);
    response = request.body(licenseOverrideReq).post();
    assertResponseStatus(200, response);
    licenseOverrideRes = response.getBody(ApiLicenseOverrideDTO.class);
    assertLicenseOverride(ownerId, componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0",
        "My comment updated", licenseOverrideRes);
    assertAuditLog(ownerId, user, false /* isDelete */, licenseOverrideRes);

    // Get
    licenseOverride = licenseOverrideDAO.getByIdNotNull(licenseOverrideRes.id);
    assertLicenseOverride(ownerId, componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0",
        "My comment updated", licenseOverride);

    // Delete
    response = request.subpath(licenseOverrideRes.id).delete();
    assertResponseStatus(204, response);
    assertAuditLog(ownerId, user, true /* isDelete */, licenseOverrideRes);

    // Get
    licenseOverride = licenseOverrideDAO.getById(licenseOverrideRes.id);
    assertThat(licenseOverride).isNull();
  }

  private void assertAuditLog(String ownerId, String user, boolean isDelete, ApiLicenseOverrideDTO expected)
      throws Exception
  {
    // Verify the license override audit
    File logFile = new File(getCLMServer().getInstance(InsightWork.class).getAuditDir(ownerId), "licenses.json");
    assertThat(logFile).isFile();

    ArrayNode allLogJsonData = JsonUtils.read(logFile);
    assertThat(allLogJsonData).isNotEmpty();
    JsonNode logJsonData = allLogJsonData.get(0);
    assertThat(logJsonData).isNotNull();
    assertThat(logJsonData.get("user").asText()).isEqualTo(user);
    LicenseOverrideAudit licenseOverrideAudit = JsonUtils.asPojo(logJsonData.get("data"), LicenseOverrideAudit.class);
    assertThat(licenseOverrideAudit).isNotNull();
    assertThat(licenseOverrideAudit.getComponentIdentifier())
        .isEqualTo(expected.componentIdentifier.toComponentIdentifier());
    if (isDelete) {
      assertThat(licenseOverrideAudit.getStatus()).isEqualTo("Deleted");
      assertThat(licenseOverrideAudit.getComment()).isNull();
    }
    else {
      assertThat(licenseOverrideAudit.getStatus()).isEqualTo(expected.status.getName());
      assertThat(licenseOverrideAudit.getComment()).isEqualTo(expected.comment);
    }
    assertThat(licenseOverrideAudit.getOverriddenLicenses())
        .extracting(licenseName -> licenseDAO.getByNameNotNull(licenseName).getId())
        .containsExactlyInAnyOrderElementsOf(expected.licenseIds);
  }

  @Test
  public void testDelete_Nonexistent_Application() throws Exception {
    String appPublicId = "LicenseOverrideResourceTest";
    tempEntity.newApplicationWithParent(appPublicId);

    HttpResponse response = restRequest(OwnerType.APPLICATION, appPublicId).path("YettiId").delete();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Cannot find a license override with ID YettiId.");
  }

  @Test
  public void testDelete_Nonexistent_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization("LicenseOverrideResourceTest");

    HttpResponse response = restRequest(OwnerType.ORGANIZATION, organization.getId()).path("YettiId").delete();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Cannot find a license override with ID YettiId.");
  }

  @Test
  public void testDelete_Nonexistent_Repository() throws Exception {
    final Repository repository = tempEntity.newRepository();
    final HttpResponse response = restRequest(OwnerType.REPOSITORY, repository.getId()).path("YettiId").delete();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Cannot find a license override with ID YettiId.");
  }

  @Test
  public void testGetAppliedLicenseOverrides_NoComponentIdentifier() throws Exception {
    // Create an organization and an application
    String orgName = "testGetAppliedLicenseOverrides";
    Organization organization = tempEntity.newOrganization(orgName);
    String appPublicId = "testGetAppliedLicenseOverrides";
    tempEntity.newApplication(appPublicId, appPublicId, organization.getId());
    HttpResponse response = restRequest(OwnerType.APPLICATION, appPublicId, "").get();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("componentIdentifier is required");
  }

  @Test
  public void testGetAppliedLicenseOverrides() throws Exception {
    // Create an organization, an application, and a repository
    String orgName = "testGetAppliedLicenseOverrides";
    Organization organization = tempEntity.newOrganization(orgName);
    Owner rootOrganization = ownerDAO.getParentOwner(organization);
    String orgId = organization.getId();
    String appPublicId = "testGetAppliedLicenseOverrides";
    Application app = tempEntity.newApplication(appPublicId, appPublicId, organization.getId());
    final Repository repository = tempEntity.newRepository();
    final String repoId = repository.getId();
    RepositoryManager repositoryManager = repositoryManagerDAO.getById(repository.getRepositoryManagerId());

    // Verify the applied license overrides for the application
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    HttpResponse response = restRequest(OwnerType.APPLICATION, appPublicId, componentIdentifier, "").get();
    assertResponseStatus(200, response);
    ApiAppliedLicenseOverridesDTO appliedLicenseOverrides =
        response.getBody(ApiAppliedLicenseOverridesDTO.class);
    assertThat(appliedLicenseOverrides).isNotNull();
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner).hasSize(3);
    assertLicenseOverrideByOwner(app, false, appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(organization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(1));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(2));

    // Verify the applied license overrides for the organization
    response = restRequest(OwnerType.ORGANIZATION, orgId, componentIdentifier).get();
    assertResponseStatus(200, response);
    appliedLicenseOverrides = response.getBody(ApiAppliedLicenseOverridesDTO.class);
    assertThat(appliedLicenseOverrides).isNotNull();
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner).hasSize(2);
    assertLicenseOverrideByOwner(organization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(1));

    // Verify the applied license overrides for the repository
    response = restRequest(OwnerType.REPOSITORY, repoId, componentIdentifier).get();
    assertResponseStatus(200, response);
    appliedLicenseOverrides = response.getBody(ApiAppliedLicenseOverridesDTO.class);
    assertThat(appliedLicenseOverrides).isNotNull();
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner).hasSize(4);
    assertLicenseOverrideByOwner(repository, false, appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(repositoryManager, false, appliedLicenseOverrides.licenseOverridesByOwner.get(1));
    assertLicenseOverrideByOwner(RepositoryContainer.SINGLETON, false,
        appliedLicenseOverrides.licenseOverridesByOwner.get(2));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(3));

    // Verify the applied license overrides for the repository_container
    response = restRequest(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID,
        componentIdentifier).get();
    assertResponseStatus(200, response);
    appliedLicenseOverrides = response.getBody(ApiAppliedLicenseOverridesDTO.class);
    assertThat(appliedLicenseOverrides).isNotNull();
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner).hasSize(2);
    assertLicenseOverrideByOwner(RepositoryContainer.SINGLETON, false,
        appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(1));

    // Create a license override for the application
    ApiLicenseOverrideDTO licenseOverrideReq = new ApiLicenseOverrideDTO(
        appPublicId, "My comment", "Apache-2.0",
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier), LicenseOverrideStatus.OVERRIDDEN);
    response = restRequest(OwnerType.APPLICATION, appPublicId).body(licenseOverrideReq).post();
    ApiLicenseOverrideDTO appLicenseOverrideRes = response.getBody(ApiLicenseOverrideDTO.class);

    // Verify the applied license overrides for the application
    response = restRequest(OwnerType.APPLICATION, appPublicId, componentIdentifier).get();
    assertResponseStatus(200, response);
    appliedLicenseOverrides = response.getBody(ApiAppliedLicenseOverridesDTO.class);
    assertThat(appliedLicenseOverrides).isNotNull();
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner).hasSize(3);
    assertLicenseOverrideByOwner(app, true, appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(organization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(1));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(2));
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner.get(0).licenseOverride.getId())
        .isEqualTo(appLicenseOverrideRes.id);

    // Verify the applied license overrides for the organization
    response = restRequest(OwnerType.ORGANIZATION, orgId, componentIdentifier).get();
    assertResponseStatus(200, response);
    appliedLicenseOverrides = response.getBody(ApiAppliedLicenseOverridesDTO.class);
    assertThat(appliedLicenseOverrides).isNotNull();
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner).hasSize(2);
    assertLicenseOverrideByOwner(organization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(1));

    // Create a license override for the organization
    ApiLicenseOverrideDTO orgLicenseOverrideReq = new ApiLicenseOverrideDTO(
        orgId,"My comment", "Apache-2.0",
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier), LicenseOverrideStatus.OVERRIDDEN);
    response = restRequest(OwnerType.ORGANIZATION, orgId).body(orgLicenseOverrideReq).post();
    ApiLicenseOverrideDTO orgLicenseOverrideRes = response.getBody(ApiLicenseOverrideDTO.class);

    // Verify the applied license overrides for the application
    response = restRequest(OwnerType.APPLICATION, appPublicId, componentIdentifier).get();
    assertResponseStatus(200, response);
    appliedLicenseOverrides = response.getBody(ApiAppliedLicenseOverridesDTO.class);
    assertThat(appliedLicenseOverrides).isNotNull();
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner).hasSize(3);
    assertLicenseOverrideByOwner(app, true, appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(organization, true, appliedLicenseOverrides.licenseOverridesByOwner.get(1));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(2));
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner.get(0).licenseOverride.getId())
        .isEqualTo(appLicenseOverrideRes.id);
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner.get(1).licenseOverride.getId())
        .isEqualTo(orgLicenseOverrideRes.id);

    // Verify the applied license overrides for the organization
    response = restRequest(OwnerType.ORGANIZATION, orgId, componentIdentifier).get();
    assertResponseStatus(200, response);
    appliedLicenseOverrides = response.getBody(ApiAppliedLicenseOverridesDTO.class);
    assertThat(appliedLicenseOverrides).isNotNull();
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner).hasSize(2);
    assertLicenseOverrideByOwner(organization, true, appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(1));
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner.get(0).licenseOverride.getId())
        .isEqualTo(orgLicenseOverrideRes.id);

    // Verify the applied license overrides for the repository
    response = restRequest(OwnerType.REPOSITORY, repoId, componentIdentifier).get();
    assertResponseStatus(200, response);
    appliedLicenseOverrides = response.getBody(ApiAppliedLicenseOverridesDTO.class);
    assertThat(appliedLicenseOverrides).isNotNull();
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner).hasSize(4);
    assertLicenseOverrideByOwner(repository, false, appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(repositoryManager, false, appliedLicenseOverrides.licenseOverridesByOwner.get(1));
    assertLicenseOverrideByOwner(RepositoryContainer.SINGLETON, false,
        appliedLicenseOverrides.licenseOverridesByOwner.get(2));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(3));

    // Create a license override for the repository container
    licenseOverrideReq = new ApiLicenseOverrideDTO(
        repoId, "My comment2", "Apache-2.0",
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier), LicenseOverrideStatus.OVERRIDDEN);
    response = restRequest(OwnerType.REPOSITORY_CONTAINER,
        RepositoryContainer.REPOSITORY_CONTAINER_ID).body(licenseOverrideReq).post();
    final ApiLicenseOverrideDTO repoContainerLicenseOverride = response.getBody(ApiLicenseOverrideDTO.class);

    // Verify the applied root org license overrides for the repository
    response = restRequest(OwnerType.REPOSITORY, repoId, componentIdentifier).get();
    assertResponseStatus(200, response);
    appliedLicenseOverrides = response.getBody(ApiAppliedLicenseOverridesDTO.class);
    assertThat(appliedLicenseOverrides).isNotNull();
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner).hasSize(4);
    assertLicenseOverrideByOwner(repository, false, appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(repositoryManager, false, appliedLicenseOverrides.licenseOverridesByOwner.get(1));
    assertLicenseOverrideByOwner(RepositoryContainer.SINGLETON, true,
        appliedLicenseOverrides.licenseOverridesByOwner.get(2));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(3));
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner.get(2).licenseOverride.getId())
        .isEqualTo(repoContainerLicenseOverride.id);

    // Create a license override for the root organization
    ApiLicenseOverrideDTO rootOrgLicenseOverrideReq = new ApiLicenseOverrideDTO(
        rootOrganization.getId(), "My comment2", "Apache-2.0",
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier), LicenseOverrideStatus.OVERRIDDEN);
    response = restRequest(OwnerType.ORGANIZATION, rootOrganization.getId()).body(rootOrgLicenseOverrideReq).post();
    final ApiLicenseOverrideDTO rootOrgLicenseOverrideRes = response.getBody(ApiLicenseOverrideDTO.class);

    // Verify the applied root org license overrides for the repository
    response = restRequest(OwnerType.REPOSITORY, repoId, componentIdentifier).get();
    assertResponseStatus(200, response);
    appliedLicenseOverrides = response.getBody(ApiAppliedLicenseOverridesDTO.class);
    assertThat(appliedLicenseOverrides).isNotNull();
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner).hasSize(4);
    assertLicenseOverrideByOwner(repository, false, appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(repositoryManager, false, appliedLicenseOverrides.licenseOverridesByOwner.get(1));
    assertLicenseOverrideByOwner(RepositoryContainer.SINGLETON, true,
        appliedLicenseOverrides.licenseOverridesByOwner.get(2));
    assertLicenseOverrideByOwner(rootOrganization, true, appliedLicenseOverrides.licenseOverridesByOwner.get(3));
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner.get(3).licenseOverride.getId())
        .isEqualTo(rootOrgLicenseOverrideRes.id);
  }

  @Test
  public void testDelete_OwnerIdMismatch_Application() throws Exception {
    String appPublicId1 = "LicenseOverrideResourceTest1";
    tempEntity.newApplicationWithParent(appPublicId1);
    String appPublicId2 = "LicenseOverrideResourceTest2";
    tempEntity.newApplicationWithParent(appPublicId2);

    testDelete_OwnerIdMismatch(OwnerType.APPLICATION, appPublicId1, appPublicId2);
  }

  @Test
  public void testDelete_OwnerIdMismatch_Organization() throws Exception {
    Organization organization1 = tempEntity.newOrganization("LicenseOverrideResourceTest1");
    Organization organization2 = tempEntity.newOrganization("LicenseOverrideResourceTest2");

    testDelete_OwnerIdMismatch(OwnerType.ORGANIZATION, organization1.getId(), organization2.getId());
  }

  @Test
  public void testDelete_OwnerIdMismatch_Repository() throws Exception {
    final Repository repository = tempEntity.newRepository();
    final Repository repository2 = tempEntity.newRepository();
    testDelete_OwnerIdMismatch(OwnerType.REPOSITORY, repository.getId(), repository2.getId());
  }

  @Test
  public void testDelete_OwnerIdMismatch_RepositoryManager() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    RepositoryManager repositoryManager2 = tempEntity.newRepositoryManager();
    testDelete_OwnerIdMismatch(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId(), repositoryManager2.getId());
  }

  private void testDelete_OwnerIdMismatch(OwnerType ownerType, String ownerPublicId1, String ownerPublicId2)
      throws Exception
  {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1",
        "a1", "v1");
    ApiLicenseOverrideDTO licenseOverrideReq = new ApiLicenseOverrideDTO(
        ownerPublicId1, "My comment", "Apache-2.0",
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier), LicenseOverrideStatus.OVERRIDDEN);
    HttpResponse response = restRequest(ownerType, ownerPublicId1).body(licenseOverrideReq).post();
    assertResponseStatus(200, response);
    ApiLicenseOverrideDTO licenseOverrideRes = response.getBody(ApiLicenseOverrideDTO.class);

    response = restRequest(ownerType, ownerPublicId2).path(licenseOverrideRes.id).delete();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Cannot find a license override with ID "
        + licenseOverrideRes.id + " for " + ownerType + " ID " + ownerPublicId2);
    // Verify that the license override was not deleted
    licenseOverrideDAO.getByIdNotNull(licenseOverrideRes.id);
  }

  private void assertLicenseOverrideByOwner(Owner owner,
      boolean hasLicenseOverride, LicenseOverrideService.LicenseOverrideByOwner actual)
  {
    assertThat(actual.ownerId).isEqualTo(owner instanceof Repository ? owner.getId() : owner.getPublicId());
    assertThat(actual.ownerName).isEqualTo(owner.getName());
    assertThat(actual.ownerType).isEqualTo(owner.getType());
    if (hasLicenseOverride) {
      assertThat(actual.licenseOverride).isNotNull();
    }
    else {
      assertThat(actual.licenseOverride).isNull();
    }
  }

  private void assertLicenseOverride(String ownerId,
                                     ComponentIdentifier componentIdentifier,
                                     LicenseOverrideStatus status,
                                     String licenseId,
                                     String comment,
                                     LicenseOverride actual)
  {
    assertThat(actual.getOwnerId()).isEqualTo(ownerId);
    assertThat(actual.getComponentIdentifier()).isEqualTo(componentIdentifier);
    assertThat(actual.getStatus()).isEqualTo(status);
    assertThat(actual.getLicenseIds()).containsExactlyInAnyOrder(licenseId);
    assertThat(actual.getComment()).isEqualTo(comment);
  }

  private void assertLicenseOverride(String ownerId,
                                     ComponentIdentifier componentIdentifier,
                                     LicenseOverrideStatus status,
                                     String licenseId,
                                     String comment,
                                     ApiLicenseOverrideDTO actual)
  {
    assertThat(actual.ownerId).isEqualTo(ownerId);
    assertThat(actual.componentIdentifier.toComponentIdentifier()).isEqualTo(componentIdentifier);
    assertThat(actual.status).isEqualTo(status);
    assertThat(actual.licenseIds).containsExactlyInAnyOrder(licenseId);
    assertThat(actual.comment).isEqualTo(comment);
  }

  @Test
  public void testAddLicenseOverride_ValidateComponentIdentifier() throws Exception {
    String appPublicId = "LicenseOverrideResourceTest";
    tempEntity.newApplicationWithParent(appPublicId);

    ApiLicenseOverrideDTO licenseOverrideReq = new ApiLicenseOverrideDTO(
        appPublicId, "My comment", "Apache-2.0",
        null, LicenseOverrideStatus.OVERRIDDEN);
    HttpResponse response = restRequest(OwnerType.APPLICATION, appPublicId).body(licenseOverrideReq).post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("The component identifier cannot be null.");
  }
}
