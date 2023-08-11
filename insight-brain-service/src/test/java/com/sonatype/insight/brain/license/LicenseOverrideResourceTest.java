/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import java.io.File;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dto.audit.LicenseOverrideAudit;
import com.sonatype.insight.brain.license.LicenseOverrideService.AppliedLicenseOverrides;
import com.sonatype.insight.brain.license.LicenseOverrideService.LicenseOverrideByOwner;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LicenseOverrideResourceTest
    extends AbstractResourceTest
{
  private HttpRequest restRequest(OwnerType ownerType, String ownerId, String... paths) {
    return restRequest().path(LicenseOverrideResource.RESOURCE_PATH).path(paths).parameter(ownerType, ownerId);
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
    String where = "EdgeOfSpace";
    HttpRequest request = restRequest(ownerType, ownerPublicId).query("where", where);

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNugetCoordinates("p1", "v1");
    // Create
    LicenseOverride licenseOverride = new LicenseOverride(null /* ownerId */, componentIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment");
    HttpResponse response = request.body(licenseOverride).post();
    assertResponseStatus(200, response);
    licenseOverride = response.getBody(LicenseOverride.class);
    assertLicenseOverride(ownerId, componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment",
        licenseOverride);
    assertAuditLog(ownerId, user, where, false /* isDelete */, licenseOverride);

    // Get
    LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();
    licenseOverride = licenseOverrideDAO.getByIdNotNull(licenseOverride.getId());
    assertLicenseOverride(ownerId, componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment",
        licenseOverride);

    // Update (i.e. add again)
    licenseOverride = new LicenseOverride(null /* ownerId */, componentIdentifier, LicenseOverrideStatus.OVERRIDDEN,
        "GPL-2.0", "My comment updated");
    response = request.body(licenseOverride).post();
    assertResponseStatus(200, response);
    licenseOverride = response.getBody(LicenseOverride.class);
    assertLicenseOverride(ownerId, componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0",
        "My comment updated", licenseOverride);
    assertAuditLog(ownerId, user, where, false /* isDelete */, licenseOverride);

    // Get
    licenseOverride = licenseOverrideDAO.getByIdNotNull(licenseOverride.getId());
    assertLicenseOverride(ownerId, componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0",
        "My comment updated", licenseOverride);

    // Delete
    response = request.subpath(licenseOverride.getId()).delete();
    assertResponseStatus(204, response);
    assertAuditLog(ownerId, user, where, true /* isDelete */, licenseOverride);

    // Get
    licenseOverride = licenseOverrideDAO.getById(licenseOverride.getId());
    assertThat(licenseOverride).isNull();
  }

  private void testCRUD(OwnerType ownerType, String ownerPublicId, String ownerId) throws Exception {
    String user = "admin";
    String where = "EdgeOfSpace";
    HttpRequest request = restRequest(ownerType, ownerPublicId).query("where", where);

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    // Create
    LicenseOverride licenseOverride = new LicenseOverride(null /* ownerId */, componentIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment");
    HttpResponse response = request.body(licenseOverride).post();
    assertResponseStatus(200, response);
    licenseOverride = response.getBody(LicenseOverride.class);
    assertLicenseOverride(ownerId, componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment",
        licenseOverride);
    assertAuditLog(ownerId, user, where, false /* isDelete */, licenseOverride);

    // Get
    LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();
    licenseOverride = licenseOverrideDAO.getByIdNotNull(licenseOverride.getId());
    assertLicenseOverride(ownerId, componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment",
        licenseOverride);

    // Update (i.e. add again)
    licenseOverride = new LicenseOverride(null /* ownerId */, ComponentIdentifier.createMavenCoordinates("g1", "a1",
        "v1"), LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0", "My comment updated");
    response = request.body(licenseOverride).post();
    assertResponseStatus(200, response);
    licenseOverride = response.getBody(LicenseOverride.class);
    assertLicenseOverride(ownerId, componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0",
        "My comment updated", licenseOverride);
    assertAuditLog(ownerId, user, where, false /* isDelete */, licenseOverride);

    // Get
    licenseOverride = licenseOverrideDAO.getByIdNotNull(licenseOverride.getId());
    assertLicenseOverride(ownerId, componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0",
        "My comment updated", licenseOverride);

    // Delete
    response = request.subpath(licenseOverride.getId()).delete();
    assertResponseStatus(204, response);
    assertAuditLog(ownerId, user, where, true /* isDelete */, licenseOverride);

    // Get
    licenseOverride = licenseOverrideDAO.getById(licenseOverride.getId());
    assertThat(licenseOverride).isNull();
  }

  private void assertAuditLog(String ownerId, String user, String where, boolean isDelete, LicenseOverride expected)
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
    assertThat(logJsonData.get("where").asText()).isEqualTo(where);
    LicenseOverrideAudit licenseOverrideAudit = JsonUtils.asPojo(logJsonData.get("data"), LicenseOverrideAudit.class);
    assertThat(licenseOverrideAudit).isNotNull();
    assertThat(licenseOverrideAudit.getComponentIdentifier()).isEqualTo(expected.getComponentIdentifier());
    if (isDelete) {
      assertThat(licenseOverrideAudit.getStatus()).isEqualTo("Deleted");
      assertThat(licenseOverrideAudit.getComment()).isNull();
    }
    else {
      assertThat(licenseOverrideAudit.getStatus()).isEqualTo(expected.getStatus().getName());
      assertThat(licenseOverrideAudit.getComment()).isEqualTo(expected.getComment());
    }
    assertThat(licenseOverrideAudit.getOverriddenLicenses())
        .extracting(licenseName -> new LicenseDAO().getByNameNotNull(licenseName).getId())
        .containsExactlyInAnyOrderElementsOf(expected.getLicenseIds());
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
    doTestGetAppliedLicenseOverrides_NoComponentIdentifier("");
  }

  @Test
  public void testGetAppliedLicenseOverridesForLegalReviewer_NoComponentIdentifier() throws Exception {
    doTestGetAppliedLicenseOverrides_NoComponentIdentifier(LicenseOverrideResource.LEGAL_REVIEWER_PATH);
  }

  private void doTestGetAppliedLicenseOverrides_NoComponentIdentifier(String path) throws Exception {
    // Create an organization and an application
    String orgName = "testGetAppliedLicenseOverrides";
    Organization organization = tempEntity.newOrganization(orgName);
    String appPublicId = "testGetAppliedLicenseOverrides";
    tempEntity.newApplication(appPublicId, appPublicId, organization.getId());
    HttpResponse response = restRequest(OwnerType.APPLICATION, appPublicId, path).get();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("componentIdentifier is required");
  }

  @Test
  public void testGetAppliedLicenseOverrides() throws Exception {
    doTestGetAppliedLicenseOverrides("");
  }

  @Test
  public void testGetAppliedLicenseOverridesForLegalReviewer() throws Exception {
    doTestGetAppliedLicenseOverrides(LicenseOverrideResource.LEGAL_REVIEWER_PATH);
  }

  private void doTestGetAppliedLicenseOverrides(String path) throws Exception {
    // Create an organization, an application, and a repository
    String orgName = "testGetAppliedLicenseOverrides";
    Organization organization = tempEntity.newOrganization(orgName);
    Owner rootOrganization = new OwnerDAO().getParentOwner(organization);
    String orgId = organization.getId();
    String appPublicId = "testGetAppliedLicenseOverrides";
    Application app = tempEntity.newApplication(appPublicId, appPublicId, organization.getId());
    final Repository repository = tempEntity.newRepository();
    final String repoId = repository.getId();

    // Verify the applied license overrides for the application
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    HttpResponse response = restRequest(OwnerType.APPLICATION, appPublicId, componentIdentifier, path).get();
    assertResponseStatus(200, response);
    AppliedLicenseOverrides appliedLicenseOverrides = response.getBody(AppliedLicenseOverrides.class);
    assertThat(appliedLicenseOverrides).isNotNull();
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner).hasSize(3);
    assertLicenseOverrideByOwner(app, false, appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(organization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(1));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(2));

    // Verify the applied license overrides for the organization
    response = restRequest(OwnerType.ORGANIZATION, orgId, componentIdentifier).get();
    assertResponseStatus(200, response);
    appliedLicenseOverrides = response.getBody(AppliedLicenseOverrides.class);
    assertThat(appliedLicenseOverrides).isNotNull();
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner).hasSize(2);
    assertLicenseOverrideByOwner(organization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(1));

    // Verify the applied license overrides for the repository
    response = restRequest(OwnerType.REPOSITORY, repoId, componentIdentifier).get();
    assertResponseStatus(200, response);
    appliedLicenseOverrides = response.getBody(AppliedLicenseOverrides.class);
    assertThat(appliedLicenseOverrides).isNotNull();
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner).hasSize(3);
    assertLicenseOverrideByOwner(repository, false, appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(RepositoryContainer.SINGLETON, false,
        appliedLicenseOverrides.licenseOverridesByOwner.get(1));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(2));

    // Verify the applied license overrides for the repository_container
    response = restRequest(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID,
        componentIdentifier).get();
    assertResponseStatus(200, response);
    appliedLicenseOverrides = response.getBody(AppliedLicenseOverrides.class);
    assertThat(appliedLicenseOverrides).isNotNull();
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner).hasSize(2);
    assertLicenseOverrideByOwner(RepositoryContainer.SINGLETON, false,
        appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(1));

    // Create a license override for the application
    LicenseOverride appLicenseOverride = new LicenseOverride(null /* ownerId */, componentIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment");
    response = restRequest(OwnerType.APPLICATION, appPublicId).body(appLicenseOverride).post();
    appLicenseOverride = response.getBody(LicenseOverride.class);

    // Verify the applied license overrides for the application
    response = restRequest(OwnerType.APPLICATION, appPublicId, componentIdentifier).get();
    assertResponseStatus(200, response);
    appliedLicenseOverrides = response.getBody(AppliedLicenseOverrides.class);
    assertThat(appliedLicenseOverrides).isNotNull();
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner).hasSize(3);
    assertLicenseOverrideByOwner(app, true, appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(organization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(1));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(2));
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner.get(0).licenseOverride.getId())
        .isEqualTo(appLicenseOverride.getId());

    // Verify the applied license overrides for the organization
    response = restRequest(OwnerType.ORGANIZATION, orgId, componentIdentifier).get();
    assertResponseStatus(200, response);
    appliedLicenseOverrides = response.getBody(AppliedLicenseOverrides.class);
    assertThat(appliedLicenseOverrides).isNotNull();
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner).hasSize(2);
    assertLicenseOverrideByOwner(organization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(1));

    // Create a license override for the organization
    LicenseOverride orgLicenseOverride = new LicenseOverride(null /* ownerId */, componentIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment");
    response = restRequest(OwnerType.ORGANIZATION, orgId).body(orgLicenseOverride).post();
    orgLicenseOverride = response.getBody(LicenseOverride.class);

    // Verify the applied license overrides for the application
    response = restRequest(OwnerType.APPLICATION, appPublicId, componentIdentifier).get();
    assertResponseStatus(200, response);
    appliedLicenseOverrides = response.getBody(AppliedLicenseOverrides.class);
    assertThat(appliedLicenseOverrides).isNotNull();
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner).hasSize(3);
    assertLicenseOverrideByOwner(app, true, appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(organization, true, appliedLicenseOverrides.licenseOverridesByOwner.get(1));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(2));
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner.get(0).licenseOverride.getId())
        .isEqualTo(appLicenseOverride.getId());
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner.get(1).licenseOverride.getId())
        .isEqualTo(orgLicenseOverride.getId());

    // Verify the applied license overrides for the organization
    response = restRequest(OwnerType.ORGANIZATION, orgId, componentIdentifier).get();
    assertResponseStatus(200, response);
    appliedLicenseOverrides = response.getBody(AppliedLicenseOverrides.class);
    assertThat(appliedLicenseOverrides).isNotNull();
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner).hasSize(2);
    assertLicenseOverrideByOwner(organization, true, appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(1));
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner.get(0).licenseOverride.getId())
        .isEqualTo(orgLicenseOverride.getId());

    // Verify the applied license overrides for the repository
    response = restRequest(OwnerType.REPOSITORY, repoId, componentIdentifier).get();
    assertResponseStatus(200, response);
    appliedLicenseOverrides = response.getBody(AppliedLicenseOverrides.class);
    assertThat(appliedLicenseOverrides).isNotNull();
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner).hasSize(3);
    assertLicenseOverrideByOwner(repository, false, appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(RepositoryContainer.SINGLETON, false,
        appliedLicenseOverrides.licenseOverridesByOwner.get(1));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(2));

    // Create a license override for the repository container
    response = restRequest(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID).body(
        new LicenseOverride(null /* ownerId */, componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0",
            "My comment2")).post();
    final LicenseOverride repoContainerLicenseOverride = response.getBody(LicenseOverride.class);

    // Verify the applied root org license overrides for the repository
    response = restRequest(OwnerType.REPOSITORY, repoId, componentIdentifier).get();
    assertResponseStatus(200, response);
    appliedLicenseOverrides = response.getBody(AppliedLicenseOverrides.class);
    assertThat(appliedLicenseOverrides).isNotNull();
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner).hasSize(3);
    assertLicenseOverrideByOwner(repository, false, appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(RepositoryContainer.SINGLETON, true,
        appliedLicenseOverrides.licenseOverridesByOwner.get(1));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(2));
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner.get(1).licenseOverride.getId())
        .isEqualTo(repoContainerLicenseOverride.getId());

    // Create a license override for the root organization
    response = restRequest(OwnerType.ORGANIZATION, rootOrganization.getId()).body(
        new LicenseOverride(null /* ownerId */, componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0",
            "My comment2")).post();
    final LicenseOverride rootOrgLicenseOverride = response.getBody(LicenseOverride.class);

    // Verify the applied root org license overrides for the repository
    response = restRequest(OwnerType.REPOSITORY, repoId, componentIdentifier).get();
    assertResponseStatus(200, response);
    appliedLicenseOverrides = response.getBody(AppliedLicenseOverrides.class);
    assertThat(appliedLicenseOverrides).isNotNull();
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner).hasSize(3);
    assertLicenseOverrideByOwner(repository, false, appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(RepositoryContainer.SINGLETON, true,
        appliedLicenseOverrides.licenseOverridesByOwner.get(1));
    assertLicenseOverrideByOwner(rootOrganization, true, appliedLicenseOverrides.licenseOverridesByOwner.get(2));
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner.get(2).licenseOverride.getId())
        .isEqualTo(rootOrgLicenseOverride.getId());
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

  private void testDelete_OwnerIdMismatch(OwnerType ownerType, String ownerPublicId1, String ownerPublicId2)
      throws Exception
  {
    LicenseOverride licenseOverride = new LicenseOverride(null /* ownerId */,
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0",
        "My comment");
    HttpResponse response = restRequest(ownerType, ownerPublicId1).body(licenseOverride).post();
    assertResponseStatus(200, response);
    licenseOverride = response.getBody(LicenseOverride.class);

    response = restRequest(ownerType, ownerPublicId2).path(licenseOverride.getId()).delete();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Cannot find a license override with ID " + licenseOverride.getId()
        + " for " + ownerType + " ID " + ownerPublicId2);
    // Verify that the license override was not deleted
    new LicenseOverrideDAO().getByIdNotNull(licenseOverride.getId());
  }

  private void assertLicenseOverrideByOwner(Owner owner, boolean hasLicenseOverride, LicenseOverrideByOwner actual) {
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

  @Test
  public void testAddLicenseOverride_ValidateComponentIdentifier() throws Exception {
    String appPublicId = "LicenseOverrideResourceTest";
    tempEntity.newApplicationWithParent(appPublicId);

    LicenseOverride licenseOverride = new LicenseOverride(null /* ownerId */, null /* componentIdentifier */,
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment");
    HttpResponse response = restRequest(OwnerType.APPLICATION, appPublicId).body(licenseOverride).post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("The component identifier cannot be null.");
  }
}
