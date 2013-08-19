/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import java.io.File;

import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dto.audit.BomAudit;
import com.sonatype.insight.brain.dto.audit.LicenseOverrideAudit;
import com.sonatype.insight.brain.license.LicenseOverrideResource.ApplicableLicenseOverrides;
import com.sonatype.insight.brain.license.LicenseOverrideResource.LicenseOverridesByOwner;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.test.RestAccess;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
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
    Application application = createApplication(appPublicId);

    testCRUD(IdUtils.TYPE_APPLICATION, appPublicId, application.getId());
  }

  @Test
  public void testCRUD_Organization() throws Exception {
    Organization organization = createOrganization("LicenseOverrideResourceTest");

    testCRUD(IdUtils.TYPE_ORGANIZATION, organization.getId(), organization.getId());
  }

  private void testCRUD(String ownerType, String ownerPublicId, String ownerId) throws Exception {
    String user = "Picard";
    String where = "EdgeOfSpace";

    // Create
    LicenseOverride licenseOverride = new LicenseOverride(null /* ownerId */, "g1", "a1", "v1",
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment");
    Response response = RestAccess.post(getServiceURL(ownerType, ownerPublicId) + "?user=" + user + "&where=" + where,
        JsonHelpers.asJson(licenseOverride));
    assertResponseStatus(200, response);
    licenseOverride = JsonHelpers.fromJson(response.getResponseBody(), LicenseOverride.class);
    assertLicenseOverride(ownerId, "g1", "a1", "v1", LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment",
        licenseOverride);
    assertAuditLog(ownerId, user, where, false /* isDelete */, licenseOverride);

    // Get
    LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();
    licenseOverride = licenseOverrideDAO.getByIdNotNull(licenseOverride.getId());
    assertLicenseOverride(ownerId, "g1", "a1", "v1", LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment",
        licenseOverride);

    // Delete
    response = RestAccess.delete(getServiceURL(ownerType, ownerPublicId) + "/" + licenseOverride.getId() + "?user="
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
    File logFile = new File(brain.getAuditDir(ownerId), "licenses.json");
    assertTrue(logFile.getAbsolutePath() + " does not exist", logFile.exists());

    ArrayNode allLogJsonData = (ArrayNode) JsonUtils.read(logFile);
    assertTrue(allLogJsonData.size() > 0);
    JsonNode logJsonData = allLogJsonData.get(0);
    assertNotNull(logJsonData);
    assertEquals(user, logJsonData.get("user").asText());
    assertEquals(where, logJsonData.get("where").asText());
    LicenseOverrideAudit licenseOverrideAudit = JsonUtils.asPojo(logJsonData.get("data"), LicenseOverrideAudit.class);
    assertNotNull(licenseOverrideAudit);
    assertEquals(expected.getGroupId(), licenseOverrideAudit.getGroupId());
    assertEquals(expected.getArtifactId(), licenseOverrideAudit.getArtifactId());
    assertEquals(expected.getVersion(), licenseOverrideAudit.getVersion());
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
    logFile = new File(brain.getAuditDir(ownerId), "bom.json");
    assertTrue(logFile.getAbsolutePath() + " does not exist", logFile.exists());

    allLogJsonData = (ArrayNode) JsonUtils.read(logFile);
    assertTrue(allLogJsonData.size() > 0);
    logJsonData = allLogJsonData.get(0);
    assertNotNull(logJsonData);
    assertEquals(user, logJsonData.get("user").asText());
    assertEquals(where, logJsonData.get("where").asText());
    BomAudit bomAudit = JsonUtils.asPojo(logJsonData.get("data"), BomAudit.class);
    assertNotNull(bomAudit);
    assertEquals(expected.getGroupId(), bomAudit.getGroupId());
    assertEquals(expected.getArtifactId(), bomAudit.getArtifactId());
    assertEquals(expected.getVersion(), bomAudit.getVersion());
    assertEquals(!isDelete, bomAudit.isModified());
  }

  @Test
  public void testDelete_Nonexistant_Application() throws Exception {
    String appPublicId = "LicenseOverrideResourceTest";
    createApplication(appPublicId);

    Response response = RestAccess.delete(getServiceURL(IdUtils.TYPE_APPLICATION, appPublicId) + "/YettiId");
    assertResponseStatus(404, response);
    assertEquals("Cannot find a license override with id YettiId", response.getResponseBody());
  }

  @Test
  public void testDelete_Nonexistant_Organization() throws Exception {
    Organization organization = createOrganization("LicenseOverrideResourceTest");

    Response response = RestAccess.delete(getServiceURL(IdUtils.TYPE_ORGANIZATION, organization.getId()) + "/YettiId");
    assertResponseStatus(404, response);
    assertEquals("Cannot find a license override with id YettiId", response.getResponseBody());
  }

  @Test
  public void testGetApplicableLicenseOverrides() throws Exception {
    // Create an organization and an application
    String orgName = "testGetApplicableLicenseOverrides";
    Organization organization = createOrganization(orgName);
    String orgId = organization.getId();
    String appName = "testGetApplicableLicenseOverrides";
    String appPublicId = "testGetApplicableLicenseOverrides";
    Application app = super.createApplication(appPublicId, appPublicId, organization);
    String appId = app.getId();

    // Verify the applicable license overrides for the application
    Response response = RestAccess.get(getServiceURL(IdUtils.TYPE_APPLICATION, appPublicId) + "/applicable/g1/a1/v1");
    assertResponseStatus(200, response);
    ApplicableLicenseOverrides applicableLicenseOverrides = JsonHelpers.fromJson(response.getResponseBody(),
        ApplicableLicenseOverrides.class);
    assertNotNull(applicableLicenseOverrides);
    assertThat(applicableLicenseOverrides.licenseOverridesByOwner, hasSize(2));
    assertLicenseOverridesByOwner(appId, appName, "application", 0,
        applicableLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverridesByOwner(orgId, orgName, "organization", 0,
        applicableLicenseOverrides.licenseOverridesByOwner.get(1));

    // Verify the applicable license overrides for the organization
    response = RestAccess.get(getServiceURL(IdUtils.TYPE_ORGANIZATION, orgId) + "/applicable/g1/a1/v1");
    assertResponseStatus(200, response);
    applicableLicenseOverrides = JsonHelpers.fromJson(response.getResponseBody(), ApplicableLicenseOverrides.class);
    assertNotNull(applicableLicenseOverrides);
    assertThat(applicableLicenseOverrides.licenseOverridesByOwner, hasSize(1));
    assertLicenseOverridesByOwner(orgId, orgName, "organization", 0,
        applicableLicenseOverrides.licenseOverridesByOwner.get(0));

    // Create a license override for the application
    LicenseOverride appLicenseOverride = new LicenseOverride(null /* ownerId */, "g1", "a1", "v1",
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment");
    response = RestAccess.post(getServiceURL(IdUtils.TYPE_APPLICATION, appPublicId),
        JsonHelpers.asJson(appLicenseOverride));
    appLicenseOverride = JsonHelpers.fromJson(response.getResponseBody(), LicenseOverride.class);

    // Verify the applicable license overrides for the application
    response = RestAccess.get(getServiceURL(IdUtils.TYPE_APPLICATION, appPublicId) + "/applicable/g1/a1/v1");
    assertResponseStatus(200, response);
    applicableLicenseOverrides = JsonHelpers.fromJson(response.getResponseBody(), ApplicableLicenseOverrides.class);
    assertNotNull(applicableLicenseOverrides);
    assertThat(applicableLicenseOverrides.licenseOverridesByOwner, hasSize(2));
    assertLicenseOverridesByOwner(appId, appName, "application", 1,
        applicableLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverridesByOwner(orgId, orgName, "organization", 0,
        applicableLicenseOverrides.licenseOverridesByOwner.get(1));
    assertEquals(appLicenseOverride.getId(), applicableLicenseOverrides.licenseOverridesByOwner.get(0).licenseOverrides
        .get(0).getId());

    // Verify the applicable license overrides for the organization
    response = RestAccess.get(getServiceURL(IdUtils.TYPE_ORGANIZATION, orgId) + "/applicable/g1/a1/v1");
    assertResponseStatus(200, response);
    applicableLicenseOverrides = JsonHelpers.fromJson(response.getResponseBody(), ApplicableLicenseOverrides.class);
    assertNotNull(applicableLicenseOverrides);
    assertThat(applicableLicenseOverrides.licenseOverridesByOwner, hasSize(1));
    assertLicenseOverridesByOwner(orgId, orgName, "organization", 0,
        applicableLicenseOverrides.licenseOverridesByOwner.get(0));

    // Create a license override for the organization
    LicenseOverride orgLicenseOverride = new LicenseOverride(null /* ownerId */, "g1", "a1", "v1",
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment");
    response = RestAccess.post(getServiceURL(IdUtils.TYPE_ORGANIZATION, orgId), JsonHelpers.asJson(orgLicenseOverride));
    orgLicenseOverride = JsonHelpers.fromJson(response.getResponseBody(), LicenseOverride.class);

    // Verify the applicable license overrides for the application
    response = RestAccess.get(getServiceURL(IdUtils.TYPE_APPLICATION, appPublicId) + "/applicable/g1/a1/v1");
    assertResponseStatus(200, response);
    applicableLicenseOverrides = JsonHelpers.fromJson(response.getResponseBody(), ApplicableLicenseOverrides.class);
    assertNotNull(applicableLicenseOverrides);
    assertThat(applicableLicenseOverrides.licenseOverridesByOwner, hasSize(2));
    assertLicenseOverridesByOwner(appId, appName, "application", 1,
        applicableLicenseOverrides.licenseOverridesByOwner.get(0));
    assertLicenseOverridesByOwner(orgId, orgName, "organization", 1,
        applicableLicenseOverrides.licenseOverridesByOwner.get(1));
    assertEquals(appLicenseOverride.getId(), applicableLicenseOverrides.licenseOverridesByOwner.get(0).licenseOverrides
        .get(0).getId());
    assertEquals(orgLicenseOverride.getId(), applicableLicenseOverrides.licenseOverridesByOwner.get(1).licenseOverrides
        .get(0).getId());

    // Verify the applicable license overrides for the organization
    response = RestAccess.get(getServiceURL(IdUtils.TYPE_ORGANIZATION, orgId) + "/applicable/g1/a1/v1");
    assertResponseStatus(200, response);
    applicableLicenseOverrides = JsonHelpers.fromJson(response.getResponseBody(), ApplicableLicenseOverrides.class);
    assertNotNull(applicableLicenseOverrides);
    assertThat(applicableLicenseOverrides.licenseOverridesByOwner, hasSize(1));
    assertLicenseOverridesByOwner(orgId, orgName, "organization", 1,
        applicableLicenseOverrides.licenseOverridesByOwner.get(0));
    assertEquals(orgLicenseOverride.getId(), applicableLicenseOverrides.licenseOverridesByOwner.get(0).licenseOverrides
        .get(0).getId());
  }

  @Test
  public void testDelete_OwnerIdMismatch_Application() throws Exception {
    String appPublicId1 = "LicenseOverrideResourceTest1";
    Application application1 = createApplication(appPublicId1);
    String appPublicId2 = "LicenseOverrideResourceTest2";
    createApplication(appPublicId2);

    testDelete_OwnerIdMismatch(IdUtils.TYPE_APPLICATION, appPublicId1, application1.getId(), appPublicId2);
  }

  @Test
  public void testDelete_OwnerIdMismatch_Organization() throws Exception {
    Organization organization1 = createOrganization("LicenseOverrideResourceTest1");
    Organization organization2 = createOrganization("LicenseOverrideResourceTest2");

    testDelete_OwnerIdMismatch(IdUtils.TYPE_ORGANIZATION, organization1.getId(), organization1.getId(),
        organization2.getId());
  }

  private void testDelete_OwnerIdMismatch(String ownerType, String ownerPublicId1, String ownerId1,
      String ownerPublicId2) throws Exception
  {
    LicenseOverride licenseOverride = new LicenseOverride(null /* ownerId */, "g1", "a1", "v1",
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment");
    Response response = RestAccess.post(getServiceURL(ownerType, ownerPublicId1), JsonHelpers.asJson(licenseOverride));
    assertResponseStatus(200, response);
    licenseOverride = JsonHelpers.fromJson(response.getResponseBody(), LicenseOverride.class);

    response = RestAccess.delete(getServiceURL(ownerType, ownerPublicId2) + "/" + licenseOverride.getId());
    assertResponseStatus(404, response);
    assertEquals("Cannot find a license override with id " + licenseOverride.getId() + " for " + ownerType + " id "
        + ownerPublicId2, response.getResponseBody());
    // Verify that the license override was not deleted
    LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();
    licenseOverride = licenseOverrideDAO.getById(licenseOverride.getId());
  }

  private void assertLicenseOverridesByOwner(String ownerId, String ownerName, String ownerType,
      int licenseOverridesCount, LicenseOverridesByOwner actual)
  {
    assertEquals(ownerId, actual.ownerId);
    assertEquals(ownerName, actual.ownerName);
    assertEquals(ownerType, actual.ownerType);
    assertThat(actual.licenseOverrides, hasSize(licenseOverridesCount));
  }

  private String getServiceURL(final String ownerType, final String ownerId) {
    return getRestBaseUrl() + LicenseOverrideResource.SERVICE_BASEPATH + ownerType + "/" + ownerId;
  }

  private void assertLicenseOverride(String ownerId, String groupId, String artifactId, String version,
      LicenseOverrideStatus status, String licenseId, String comment, LicenseOverride actual)
  {
    assertEquals(ownerId, actual.getOwnerId());
    assertEquals(groupId, actual.getGroupId());
    assertEquals(artifactId, actual.getArtifactId());
    assertEquals(version, actual.getVersion());
    assertEquals(status, actual.getStatus());
    assertEquals(licenseId, actual.getLicenseId());
    assertEquals(comment, actual.getComment());
  }
}
