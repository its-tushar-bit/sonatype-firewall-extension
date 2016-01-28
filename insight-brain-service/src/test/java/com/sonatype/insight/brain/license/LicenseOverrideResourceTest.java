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
import com.sonatype.insight.brain.dto.audit.BomAudit;
import com.sonatype.insight.brain.dto.audit.LicenseOverrideAudit;
import com.sonatype.insight.brain.license.LicenseOverrideService.AppliedLicenseOverrides;
import com.sonatype.insight.brain.license.LicenseOverrideService.LicenseOverrideByOwner;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class LicenseOverrideResourceTest
    extends AbstractResourceTest
{
  private HttpRequest restRequest(OwnerType ownerType, String ownerId) {
    return restRequest().path(LicenseOverrideResource.RESOURCE_PATH).parameter(ownerType, ownerId);
  }

  private HttpRequest restRequest(OwnerType ownerType, String ownerId, ComponentIdentifier componentIdentifier) {
    return restRequest().path(LicenseOverrideResource.RESOURCE_PATH).query("componentIdentifier", componentIdentifier)
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
    assertNull(licenseOverride);
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
    licenseOverride = new LicenseOverride(null /* ownerId */,
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0",
        "My comment updated");
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
    assertNull(licenseOverride);
  }

  private void assertAuditLog(String ownerId, String user, String where, boolean isDelete, LicenseOverride expected)
      throws Exception
  {
    // Verify the license override audit
    File logFile = new File(getCLMServer().getAuditDir(ownerId), "licenses.json");
    assertTrue(logFile.getAbsolutePath() + " does not exist", logFile.exists());

    ArrayNode allLogJsonData = JsonUtils.read(logFile);
    assertTrue(allLogJsonData.size() > 0);
    JsonNode logJsonData = allLogJsonData.get(0);
    assertNotNull(logJsonData);
    assertEquals(user, logJsonData.get("user").asText());
    assertEquals(where, logJsonData.get("where").asText());
    LicenseOverrideAudit licenseOverrideAudit = JsonUtils.asPojo(logJsonData.get("data"), LicenseOverrideAudit.class);
    assertNotNull(licenseOverrideAudit);
    assertEquals(expected.getComponentIdentifier(), licenseOverrideAudit.getComponentIdentifier());
    if (isDelete) {
      assertEquals("Deleted", licenseOverrideAudit.getStatus());
      assertNull(licenseOverrideAudit.getComment());
    }
    else {
      assertEquals(expected.getStatus().getName(), licenseOverrideAudit.getStatus());
      assertEquals(expected.getComment(), licenseOverrideAudit.getComment());
    }
    String licenseName = licenseOverrideAudit.getOverriddenLicenses().get(0);
    License license = new LicenseDAO().getByNameNotNull(licenseName);
    assertEquals(expected.getLicenseIds().iterator().next(), license.getId());

    // Verify the BOM audit
    logFile = new File(getCLMServer().getAuditDir(ownerId), "bom.json");
    assertTrue(logFile.getAbsolutePath() + " does not exist", logFile.exists());

    allLogJsonData = JsonUtils.read(logFile);
    assertTrue(allLogJsonData.size() > 0);
    logJsonData = allLogJsonData.get(0);
    assertNotNull(logJsonData);
    assertEquals(user, logJsonData.get("user").asText());
    assertEquals(where, logJsonData.get("where").asText());
    BomAudit bomAudit = JsonUtils.asPojo(logJsonData.get("data"), BomAudit.class);
    assertNotNull(bomAudit);
    assertThat(expected.getComponentIdentifier(), is(bomAudit.getComponentIdentifier()));
    assertEquals(!isDelete, bomAudit.isModified());
  }

  @Test
  public void testDelete_Nonexistent_Application() throws Exception {
    String appPublicId = "LicenseOverrideResourceTest";
    tempEntity.newApplicationWithParent(appPublicId);

    HttpResponse response = restRequest(OwnerType.APPLICATION, appPublicId).path("YettiId").delete();
    assertResponseStatus(404, response);
    assertEquals("Cannot find a license override with ID YettiId.", response.getBodyText());
  }

  @Test
  public void testDelete_Nonexistent_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization("LicenseOverrideResourceTest");

    HttpResponse response = restRequest(OwnerType.ORGANIZATION, organization.getId()).path("YettiId").delete();
    assertResponseStatus(404, response);
    assertEquals("Cannot find a license override with ID YettiId.", response.getBodyText());
  }

  @Test
  public void testDelete_Nonexistent_Repository() throws Exception {
    final Repository repository = tempEntity.newRepository();
    final HttpResponse response = restRequest(OwnerType.REPOSITORY, repository.getId()).path("YettiId").delete();
    assertResponseStatus(404, response);
    assertEquals("Cannot find a license override with ID YettiId.", response.getBodyText());
  }

  @Test
  public void testGetAppliedLicenseOverrides_NoComponentIdentifier() throws Exception {
    // Create an organization and an application
    String orgName = "testGetAppliedLicenseOverrides";
    Organization organization = tempEntity.newOrganization(orgName);
    String appPublicId = "testGetAppliedLicenseOverrides";
    tempEntity.newApplication(appPublicId, appPublicId, organization.getId());
    HttpResponse response = restRequest(OwnerType.APPLICATION, appPublicId).get();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText(), is("componentIdentifier is required"));
  }

  @Test
  public void testGetAppliedLicenseOverrides() throws Exception {
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
    HttpResponse response = restRequest(OwnerType.APPLICATION, appPublicId, componentIdentifier).get();
    assertResponseStatus(200, response);
    AppliedLicenseOverrides appliedLicenseOverrides = response.getBody(AppliedLicenseOverrides.class);
    assertNotNull(appliedLicenseOverrides);
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner, hasSize(3));
    assertLicenseOverrideByOwner(app, false, appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(organization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(1));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(2));

    // Verify the applied license overrides for the organization
    response = restRequest(OwnerType.ORGANIZATION, orgId, componentIdentifier).get();
    assertResponseStatus(200, response);
    appliedLicenseOverrides = response.getBody(AppliedLicenseOverrides.class);
    assertNotNull(appliedLicenseOverrides);
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner, hasSize(2));
    assertLicenseOverrideByOwner(organization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(1));

    // Verify the applied license overrides for the repository
    response = restRequest(OwnerType.REPOSITORY, repoId, componentIdentifier).get();
    assertResponseStatus(200, response);
    appliedLicenseOverrides = response.getBody(AppliedLicenseOverrides.class);
    assertNotNull(appliedLicenseOverrides);
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner, hasSize(3));
    assertLicenseOverrideByOwner(repository, false, appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(RepositoryContainer.SINGLETON, false, appliedLicenseOverrides.licenseOverridesByOwner.get(1));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(2));

    // Verify the applied license overrides for the repository_container
    response = restRequest(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID,
        componentIdentifier).get();
    assertResponseStatus(200, response);
    appliedLicenseOverrides = response.getBody(AppliedLicenseOverrides.class);
    assertNotNull(appliedLicenseOverrides);
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner, hasSize(2));
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
    assertNotNull(appliedLicenseOverrides);
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner, hasSize(3));
    assertLicenseOverrideByOwner(app, true, appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(organization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(1));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(2));
    assertEquals(appLicenseOverride.getId(),
        appliedLicenseOverrides.licenseOverridesByOwner.get(0).licenseOverride.getId());

    // Verify the applied license overrides for the organization
    response = restRequest(OwnerType.ORGANIZATION, orgId, componentIdentifier).get();
    assertResponseStatus(200, response);
    appliedLicenseOverrides = response.getBody(AppliedLicenseOverrides.class);
    assertNotNull(appliedLicenseOverrides);
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner, hasSize(2));
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
    assertNotNull(appliedLicenseOverrides);
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner, hasSize(3));
    assertLicenseOverrideByOwner(app, true, appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(organization, true, appliedLicenseOverrides.licenseOverridesByOwner.get(1));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(2));
    assertEquals(appLicenseOverride.getId(),
        appliedLicenseOverrides.licenseOverridesByOwner.get(0).licenseOverride.getId());
    assertEquals(orgLicenseOverride.getId(),
        appliedLicenseOverrides.licenseOverridesByOwner.get(1).licenseOverride.getId());

    // Verify the applied license overrides for the organization
    response = restRequest(OwnerType.ORGANIZATION, orgId, componentIdentifier).get();
    assertResponseStatus(200, response);
    appliedLicenseOverrides = response.getBody(AppliedLicenseOverrides.class);
    assertNotNull(appliedLicenseOverrides);
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner, hasSize(2));
    assertLicenseOverrideByOwner(organization, true, appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(1));
    assertEquals(orgLicenseOverride.getId(),
        appliedLicenseOverrides.licenseOverridesByOwner.get(0).licenseOverride.getId());

    // Verify the applied license overrides for the repository
    response = restRequest(OwnerType.REPOSITORY, repoId, componentIdentifier).get();
    assertResponseStatus(200, response);
    appliedLicenseOverrides = response.getBody(AppliedLicenseOverrides.class);
    assertNotNull(appliedLicenseOverrides);
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner, hasSize(3));
    assertLicenseOverrideByOwner(repository, false, appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(RepositoryContainer.SINGLETON, false, appliedLicenseOverrides.licenseOverridesByOwner.get(1));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(2));

    // Create a license override for the repository container
    response = restRequest(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID)
        .body(new LicenseOverride(null /* ownerId */, componentIdentifier,
            LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment2")).post();
    final LicenseOverride repoContainerLicenseOverride = response.getBody(LicenseOverride.class);
    tempEntity.register(repoContainerLicenseOverride);

    // Verify the applied root org license overrides for the repository
    response = restRequest(OwnerType.REPOSITORY, repoId, componentIdentifier).get();
    assertResponseStatus(200, response);
    appliedLicenseOverrides = response.getBody(AppliedLicenseOverrides.class);
    assertNotNull(appliedLicenseOverrides);
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner, hasSize(3));
    assertLicenseOverrideByOwner(repository, false, appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(RepositoryContainer.SINGLETON, true, appliedLicenseOverrides.licenseOverridesByOwner.get(1));
    assertLicenseOverrideByOwner(rootOrganization, false, appliedLicenseOverrides.licenseOverridesByOwner.get(2));
    assertEquals(repoContainerLicenseOverride.getId(),
        appliedLicenseOverrides.licenseOverridesByOwner.get(1).licenseOverride.getId());

    // Create a license override for the root organization
    response = restRequest(OwnerType.ORGANIZATION, rootOrganization.getId())
        .body(new LicenseOverride(null /* ownerId */, componentIdentifier,
            LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment2")).post();
    final LicenseOverride rootOrgLicenseOverride = response.getBody(LicenseOverride.class);
    tempEntity.register(rootOrgLicenseOverride);

    // Verify the applied root org license overrides for the repository
    response = restRequest(OwnerType.REPOSITORY, repoId, componentIdentifier).get();
    assertResponseStatus(200, response);
    appliedLicenseOverrides = response.getBody(AppliedLicenseOverrides.class);
    assertNotNull(appliedLicenseOverrides);
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner, hasSize(3));
    assertLicenseOverrideByOwner(repository, false, appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(RepositoryContainer.SINGLETON, true, appliedLicenseOverrides.licenseOverridesByOwner.get(1));
    assertLicenseOverrideByOwner(rootOrganization, true, appliedLicenseOverrides.licenseOverridesByOwner.get(2));
    assertEquals(rootOrgLicenseOverride.getId(),
        appliedLicenseOverrides.licenseOverridesByOwner.get(2).licenseOverride.getId());
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

  private void testDelete_OwnerIdMismatch(OwnerType ownerType, String ownerPublicId1,
      String ownerPublicId2) throws Exception
  {
    LicenseOverride licenseOverride = new LicenseOverride(null /* ownerId */,
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0",
        "My comment");
    HttpResponse response = restRequest(ownerType, ownerPublicId1).body(licenseOverride).post();
    assertResponseStatus(200, response);
    licenseOverride = response.getBody(LicenseOverride.class);

    response = restRequest(ownerType, ownerPublicId2).path(licenseOverride.getId()).delete();
    assertResponseStatus(404, response);
    assertEquals("Cannot find a license override with ID " + licenseOverride.getId() + " for " + ownerType + " ID "
        + ownerPublicId2, response.getBodyText());
    // Verify that the license override was not deleted
    new LicenseOverrideDAO().getByIdNotNull(licenseOverride.getId());
  }

  private void assertLicenseOverrideByOwner(Owner owner, boolean hasLicenseOverride, LicenseOverrideByOwner actual) {
    if (owner instanceof Repository) {
      assertEquals(owner.getId(), actual.ownerId);
    }
    else {
      assertEquals(owner.getPublicId(), actual.ownerId);
    }

    assertEquals(owner.getName(), actual.ownerName);
    assertEquals(owner.getType(), actual.ownerType);
    if (hasLicenseOverride) {
      assertNotNull(actual.licenseOverride);
    }
    else {
      assertNull(actual.licenseOverride);
    }
  }

  private void assertLicenseOverride(String ownerId, ComponentIdentifier componentIdentifier,
      LicenseOverrideStatus status, String licenseId, String comment, LicenseOverride actual)
  {
    assertEquals(ownerId, actual.getOwnerId());
    assertEquals(componentIdentifier, actual.getComponentIdentifier());
    assertEquals(status, actual.getStatus());
    assertEquals(licenseId, actual.getLicenseIds().iterator().next());
    assertEquals(comment, actual.getComment());
  }

  @Test
  public void testAddLicenseOverride_ValidateComponentIdentifier() throws Exception {
    String appPublicId = "LicenseOverrideResourceTest";
    tempEntity.newApplicationWithParent(appPublicId);

    LicenseOverride licenseOverride = new LicenseOverride(null /* ownerId */, null /* componentIdentifier */,
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment");
    HttpResponse response = restRequest(OwnerType.APPLICATION, appPublicId).body(licenseOverride).post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText(), is("The component identifier cannot be null."));
  }
}
