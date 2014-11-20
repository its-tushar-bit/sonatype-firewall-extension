/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import java.io.File;

import javax.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dto.audit.BomAudit;
import com.sonatype.insight.brain.dto.audit.LicenseOverrideAudit;
import com.sonatype.insight.brain.license.LicenseOverrideResource.AppliedLicenseOverrides;
import com.sonatype.insight.brain.license.LicenseOverrideResource.LicenseOverrideByOwner;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
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
  @Test
  public void testCRUD_Application() throws Exception {
    String appPublicId = "LicenseOverrideResourceTest";
    Application application = tempEntity.newApplicationWithParent(appPublicId);

    testCRUD(IdUtils.TYPE_APPLICATION, appPublicId, application.getId());
  }

  @Test
  public void testCRUD_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization("LicenseOverrideResourceTest");

    testCRUD(IdUtils.TYPE_ORGANIZATION, organization.getId(), organization.getId());
  }

  @Test
  public void testCRUD_Nuget_Application() throws Exception {
    String appPublicId = "LicenseOverrideResourceTest";
    Application application = tempEntity.newApplicationWithParent(appPublicId);

    testCRUD_Nuget(IdUtils.TYPE_APPLICATION, appPublicId, application.getId());
  }

  @Test
  public void testCRUD_Nuget_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization("LicenseOverrideResourceTest");

    testCRUD_Nuget(IdUtils.TYPE_ORGANIZATION, organization.getId(), organization.getId());
  }

  private void testCRUD_Nuget(String ownerType, String ownerPublicId, String ownerId) throws Exception {
    String user = "admin";
    String where = "EdgeOfSpace";

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNugetCoordinates("p1", "v1");
    // Create
    LicenseOverride licenseOverride = new LicenseOverride(null /* ownerId */,
        componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0",
        "My comment");
    Response response = AuthedRestAccess.post(getServiceURL(ownerType, ownerPublicId) + "?where=" + where,
        JsonHelpers.asJson(licenseOverride));
    assertResponseStatus(200, response);
    licenseOverride = JsonHelpers.fromJson(response.getResponseBody(), LicenseOverride.class);
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
        componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0", "My comment updated");
    response = AuthedRestAccess.post(getServiceURL(ownerType, ownerPublicId) + "?where=" + where,
        JsonHelpers.asJson(licenseOverride));
    assertResponseStatus(200, response);
    licenseOverride = JsonHelpers.fromJson(response.getResponseBody(), LicenseOverride.class);
    assertLicenseOverride(ownerId, componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0", "My comment updated",
        licenseOverride);
    assertAuditLog(ownerId, user, where, false /* isDelete */, licenseOverride);

    // Get
    licenseOverride = licenseOverrideDAO.getByIdNotNull(licenseOverride.getId());
    assertLicenseOverride(ownerId, componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0", "My comment updated",
        licenseOverride);

    // Delete
    response = AuthedRestAccess.delete(getServiceURL(ownerType, ownerPublicId) + "/" + licenseOverride.getId() + "?user="
        + user + "&where=" + where);
    assertResponseStatus(204, response);
    assertAuditLog(ownerId, user, where, true /* isDelete */, licenseOverride);

    // Get
    licenseOverride = licenseOverrideDAO.getById(licenseOverride.getId());
    assertNull(licenseOverride);
  }

  private void testCRUD(String ownerType, String ownerPublicId, String ownerId) throws Exception {
    String user = "admin";
    String where = "EdgeOfSpace";

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    // Create
    LicenseOverride licenseOverride = new LicenseOverride(null /* ownerId */,
        componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0",
      "My comment");
    Response response = AuthedRestAccess.post(getServiceURL(ownerType, ownerPublicId) + "?where=" + where,
        JsonHelpers.asJson(licenseOverride));
    assertResponseStatus(200, response);
    licenseOverride = JsonHelpers.fromJson(response.getResponseBody(), LicenseOverride.class);
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
    response = AuthedRestAccess.post(getServiceURL(ownerType, ownerPublicId) + "?where=" + where,
        JsonHelpers.asJson(licenseOverride));
    assertResponseStatus(200, response);
    licenseOverride = JsonHelpers.fromJson(response.getResponseBody(), LicenseOverride.class);
    assertLicenseOverride(ownerId, componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0", "My comment updated",
        licenseOverride);
    assertAuditLog(ownerId, user, where, false /* isDelete */, licenseOverride);

    // Get
    licenseOverride = licenseOverrideDAO.getByIdNotNull(licenseOverride.getId());
    assertLicenseOverride(ownerId, componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0", "My comment updated",
        licenseOverride);

    // Delete
    response = AuthedRestAccess.delete(getServiceURL(ownerType, ownerPublicId) + "/" + licenseOverride.getId() + "?user="
        + user + "&where=" + where);
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
    assertEquals(expected.getLicenseId(), license.getId());

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

    Response response = AuthedRestAccess.delete(getServiceURL(IdUtils.TYPE_APPLICATION, appPublicId) + "/YettiId");
    assertResponseStatus(404, response);
    assertEquals("Cannot find a license override with ID YettiId.", response.getResponseBody());
  }

  @Test
  public void testDelete_Nonexistent_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization("LicenseOverrideResourceTest");

    Response response = AuthedRestAccess.delete(getServiceURL(IdUtils.TYPE_ORGANIZATION, organization.getId()) + "/YettiId");
    assertResponseStatus(404, response);
    assertEquals("Cannot find a license override with ID YettiId.", response.getResponseBody());
  }

  @Test
  public void testGetAppliedLicenseOverrides_NoComponentIdentifier() throws Exception {
    // Create an organization and an application
    String orgName = "testGetAppliedLicenseOverrides";
    Organization organization = tempEntity.newOrganization(orgName);
    String appPublicId = "testGetAppliedLicenseOverrides";
    tempEntity.newApplication(appPublicId, appPublicId, organization.getId());
    Response response = AuthedRestAccess.get(getServiceURL(IdUtils.TYPE_APPLICATION, appPublicId, null));
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), is("componentIdentifier is required"));
  }

  @Test
  public void testGetAppliedLicenseOverrides() throws Exception {
    // Create an organization and an application
    String orgName = "testGetAppliedLicenseOverrides";
    Organization organization = tempEntity.newOrganization(orgName);
    String orgId = organization.getId();
    String appName = "testGetAppliedLicenseOverrides";
    String appPublicId = "testGetAppliedLicenseOverrides";
    tempEntity.newApplication(appPublicId, appPublicId, organization.getId());

    // Verify the applied license overrides for the application
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    Response response = AuthedRestAccess.get(getServiceURL(IdUtils.TYPE_APPLICATION, appPublicId, componentIdentifier));
    assertResponseStatus(200, response);
    AppliedLicenseOverrides appliedLicenseOverrides = JsonHelpers.fromJson(response.getResponseBody(),
        AppliedLicenseOverrides.class);
    assertNotNull(appliedLicenseOverrides);
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner, hasSize(2));
    assertLicenseOverrideByOwner(appPublicId, appName, "application", false,
        appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(orgId, orgName, "organization", false,
        appliedLicenseOverrides.licenseOverridesByOwner.get(1));

    // Verify the applied license overrides for the organization
    response = AuthedRestAccess.get(getServiceURL(IdUtils.TYPE_ORGANIZATION, orgId, componentIdentifier));
    assertResponseStatus(200, response);
    appliedLicenseOverrides = JsonHelpers.fromJson(response.getResponseBody(), AppliedLicenseOverrides.class);
    assertNotNull(appliedLicenseOverrides);
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner, hasSize(1));
    assertLicenseOverrideByOwner(orgId, orgName, "organization", false,
        appliedLicenseOverrides.licenseOverridesByOwner.get(0));

    // Create a license override for the application
    LicenseOverride appLicenseOverride = new LicenseOverride(null /* ownerId */,
      componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0",
      "My comment");
    response = AuthedRestAccess.post(getServiceURL(IdUtils.TYPE_APPLICATION, appPublicId),
        JsonHelpers.asJson(appLicenseOverride));
    appLicenseOverride = JsonHelpers.fromJson(response.getResponseBody(), LicenseOverride.class);

    // Verify the applied license overrides for the application
    response = AuthedRestAccess.get(getServiceURL(IdUtils.TYPE_APPLICATION, appPublicId, componentIdentifier));
    assertResponseStatus(200, response);
    appliedLicenseOverrides = JsonHelpers.fromJson(response.getResponseBody(), AppliedLicenseOverrides.class);
    assertNotNull(appliedLicenseOverrides);
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner, hasSize(2));
    assertLicenseOverrideByOwner(appPublicId, appName, "application", true,
        appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(orgId, orgName, "organization", false,
        appliedLicenseOverrides.licenseOverridesByOwner.get(1));
    assertEquals(appLicenseOverride.getId(),
        appliedLicenseOverrides.licenseOverridesByOwner.get(0).licenseOverride.getId());

    // Verify the applied license overrides for the organization
    response = AuthedRestAccess.get(getServiceURL(IdUtils.TYPE_ORGANIZATION, orgId, componentIdentifier));
    assertResponseStatus(200, response);
    appliedLicenseOverrides = JsonHelpers.fromJson(response.getResponseBody(), AppliedLicenseOverrides.class);
    assertNotNull(appliedLicenseOverrides);
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner, hasSize(1));
    assertLicenseOverrideByOwner(orgId, orgName, "organization", false,
        appliedLicenseOverrides.licenseOverridesByOwner.get(0));

    // Create a license override for the organization
    LicenseOverride orgLicenseOverride = new LicenseOverride(null /* ownerId */,
        componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0",
      "My comment");
    response = AuthedRestAccess.post(getServiceURL(IdUtils.TYPE_ORGANIZATION, orgId), JsonHelpers.asJson(orgLicenseOverride));
    orgLicenseOverride = JsonHelpers.fromJson(response.getResponseBody(), LicenseOverride.class);

    // Verify the applied license overrides for the application
    response = AuthedRestAccess.get(getServiceURL(IdUtils.TYPE_APPLICATION, appPublicId, componentIdentifier));
    assertResponseStatus(200, response);
    appliedLicenseOverrides = JsonHelpers.fromJson(response.getResponseBody(), AppliedLicenseOverrides.class);
    assertNotNull(appliedLicenseOverrides);
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner, hasSize(2));
    assertLicenseOverrideByOwner(appPublicId, appName, "application", true,
        appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverrideByOwner(orgId, orgName, "organization", true,
        appliedLicenseOverrides.licenseOverridesByOwner.get(1));
    assertEquals(appLicenseOverride.getId(),
        appliedLicenseOverrides.licenseOverridesByOwner.get(0).licenseOverride.getId());
    assertEquals(orgLicenseOverride.getId(),
        appliedLicenseOverrides.licenseOverridesByOwner.get(1).licenseOverride.getId());

    // Verify the applied license overrides for the organization
    response = AuthedRestAccess.get(getServiceURL(IdUtils.TYPE_ORGANIZATION, orgId, componentIdentifier));
    assertResponseStatus(200, response);
    appliedLicenseOverrides = JsonHelpers.fromJson(response.getResponseBody(), AppliedLicenseOverrides.class);
    assertNotNull(appliedLicenseOverrides);
    assertThat(appliedLicenseOverrides.licenseOverridesByOwner, hasSize(1));
    assertLicenseOverrideByOwner(orgId, orgName, "organization", true,
        appliedLicenseOverrides.licenseOverridesByOwner.get(0));
    assertEquals(orgLicenseOverride.getId(),
        appliedLicenseOverrides.licenseOverridesByOwner.get(0).licenseOverride.getId());
  }

  @Test
  public void testDelete_OwnerIdMismatch_Application() throws Exception {
    String appPublicId1 = "LicenseOverrideResourceTest1";
    Application application1 = tempEntity.newApplicationWithParent(appPublicId1);
    String appPublicId2 = "LicenseOverrideResourceTest2";
    tempEntity.newApplicationWithParent(appPublicId2);

    testDelete_OwnerIdMismatch(IdUtils.TYPE_APPLICATION, appPublicId1, application1.getId(), appPublicId2);
  }

  @Test
  public void testDelete_OwnerIdMismatch_Organization() throws Exception {
    Organization organization1 = tempEntity.newOrganization("LicenseOverrideResourceTest1");
    Organization organization2 = tempEntity.newOrganization("LicenseOverrideResourceTest2");

    testDelete_OwnerIdMismatch(IdUtils.TYPE_ORGANIZATION, organization1.getId(), organization1.getId(),
        organization2.getId());
  }

  private void testDelete_OwnerIdMismatch(String ownerType, String ownerPublicId1, String ownerId1,
      String ownerPublicId2) throws Exception
  {
    LicenseOverride licenseOverride = new LicenseOverride(null /* ownerId */,
      ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0",
      "My comment");
    Response response = AuthedRestAccess.post(getServiceURL(ownerType, ownerPublicId1), JsonHelpers.asJson(licenseOverride));
    assertResponseStatus(200, response);
    licenseOverride = JsonHelpers.fromJson(response.getResponseBody(), LicenseOverride.class);

    response = AuthedRestAccess.delete(getServiceURL(ownerType, ownerPublicId2) + "/" + licenseOverride.getId());
    assertResponseStatus(404, response);
    assertEquals("Cannot find a license override with ID " + licenseOverride.getId() + " for " + ownerType + " ID "
        + ownerPublicId2, response.getResponseBody());
    // Verify that the license override was not deleted
    new LicenseOverrideDAO().getByIdNotNull(licenseOverride.getId());
  }

  private void assertLicenseOverrideByOwner(String ownerId, String ownerName, String ownerType,
      boolean hasLicenseOverride, LicenseOverrideByOwner actual)
  {
    assertEquals(ownerId, actual.ownerId);
    assertEquals(ownerName, actual.ownerName);
    assertEquals(ownerType, actual.ownerType);
    if (hasLicenseOverride) {
      assertNotNull(actual.licenseOverride);
    }
    else {
      assertNull(actual.licenseOverride);
    }
  }

  private String getServiceURL(final String ownerType, final String ownerId) {
    return getServiceURL(ownerType, ownerId, null);
  }

  private String getServiceURL(final String ownerType, final String ownerId, final ComponentIdentifier componentIdentifier) {
    UriBuilder builder = UriBuilder.fromUri(getRestUrl(LicenseOverrideResource.SERVICE_PATH, ownerType, ownerId));
    if (componentIdentifier != null) {
      builder.queryParam("componentIdentifier", ComponentIdentifierAdapter.toJson(componentIdentifier));
    }
    return builder.build().toString();
  }

  private void assertLicenseOverride(String ownerId, ComponentIdentifier componentIdentifier,
      LicenseOverrideStatus status, String licenseId, String comment, LicenseOverride actual)
  {
    assertEquals(ownerId, actual.getOwnerId());
    assertEquals(componentIdentifier, actual.getComponentIdentifier());
    assertEquals(status, actual.getStatus());
    assertEquals(licenseId, actual.getLicenseId());
    assertEquals(comment, actual.getComment());
  }

  @Test
  public void testAddLicenseOverride_ValidateComponentIdentifier() throws Exception {
    String where = "EdgeOfSpace";
    String appPublicId = "LicenseOverrideResourceTest";
    tempEntity.newApplicationWithParent(appPublicId);

    LicenseOverride licenseOverride = new LicenseOverride(null /* ownerId */, null /* componentIdentifier */,
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment");
    Response response = AuthedRestAccess.post(getServiceURL("application", appPublicId) + "?where=" + where,
        JsonHelpers.asJson(licenseOverride));
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), is("The component identifier cannot be null."));
  }
}
