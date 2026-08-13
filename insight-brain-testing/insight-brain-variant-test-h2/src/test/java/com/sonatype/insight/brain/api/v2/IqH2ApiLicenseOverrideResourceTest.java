/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiAppliedLicenseOverridesDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseOverrideDTO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dto.audit.LicenseOverrideAudit;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ApiLicenseOverrideResourceTest
{
  private IqTestContext ctx;

  private LicenseOverrideDAO licenseOverrideDAO;

  private LicenseDAO licenseDAO;

  @BeforeEach
  void setUp() {
    licenseOverrideDAO = ctx.lookup(LicenseOverrideDAO.class);
    licenseDAO = ctx.lookup(LicenseDAO.class);
  }

  private HttpRequest restRequest(OwnerType ownerType, String ownerId, String... paths) {
    return ctx.restRequest()
        .path(PublicApiPaths.LICENSE_OVERRIDE_RESOURCE_PATH_V2)
        .path(paths)
        .parameter(ownerType,
            ownerId);
  }

  private HttpRequest restRequest(
      OwnerType ownerType,
      String ownerId,
      ComponentIdentifier componentIdentifier,
      String... paths)
  {
    return ctx.restRequest()
        .path(PublicApiPaths.LICENSE_OVERRIDE_RESOURCE_PATH_V2)
        .path(paths)
        .query("componentIdentifier", componentIdentifier)
        .parameter(ownerType, ownerId);
  }

  @Test
  void testCRUD_Application() throws Exception {
    String appPublicId = "LicenseOverrideResourceTest";
    Application application = ctx.tempEntity().newApplicationWithParent(appPublicId);

    testCRUD(OwnerType.APPLICATION, appPublicId, application.getId(), false, false);
    testCRUD(OwnerType.APPLICATION, appPublicId, application.getId(), true, false);
  }

  @Test
  void testCRUD_Organization() throws Exception {
    Organization organization = ctx.tempEntity().newOrganization("LicenseOverrideResourceTest");

    testCRUD(OwnerType.ORGANIZATION, organization.getId(), organization.getId(), false, false);
    testCRUD(OwnerType.ORGANIZATION, organization.getId(), organization.getId(), true, false);
  }

  @Test
  void testCRUD_Repository() throws Exception {
    final Repository repository = ctx.tempEntity().newRepository();
    testCRUD(OwnerType.REPOSITORY, repository.getId(), repository.getId(), false, false);
    testCRUD(OwnerType.REPOSITORY, repository.getId(), repository.getId(), true, false);
  }

  @Test
  void testCRUD_RepositoryManager() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    testCRUD(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId(), repositoryManager.getId(),
        false, false);
    testCRUD(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId(), repositoryManager.getId(),
        true, false);
  }

  @Test
  void testCRUD_RepositoryContainer() throws Exception {
    testCRUD(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID,
        RepositoryContainer.REPOSITORY_CONTAINER_ID, false, false);
    testCRUD(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID,
        RepositoryContainer.REPOSITORY_CONTAINER_ID, true, false);
  }

  @Test
  void testCRUD_Nuget_Application() throws Exception {
    String appPublicId = "LicenseOverrideResourceTest";
    Application application = ctx.tempEntity().newApplicationWithParent(appPublicId);

    testCRUD(OwnerType.APPLICATION, appPublicId, application.getId(), false, true);
    testCRUD(OwnerType.APPLICATION, appPublicId, application.getId(), true, true);
  }

  @Test
  void testCRUD_Nuget_Organization() throws Exception {
    Organization organization = ctx.tempEntity().newOrganization("LicenseOverrideResourceTest");

    testCRUD(OwnerType.ORGANIZATION, organization.getId(), organization.getId(), false, true);
    testCRUD(OwnerType.ORGANIZATION, organization.getId(), organization.getId(), true, true);
  }

  @Test
  void testCRUD_Nuget_Repository() throws Exception {
    final Repository repository = ctx.tempEntity().newRepository();
    testCRUD(OwnerType.REPOSITORY, repository.getId(), repository.getId(), false, true);
    testCRUD(OwnerType.REPOSITORY, repository.getId(), repository.getId(), true, true);
  }

  @Test
  void testCRUD_Nuget_RepositoryContainer() throws Exception {
    testCRUD(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID,
        RepositoryContainer.REPOSITORY_CONTAINER_ID, false, true);
    testCRUD(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID,
        RepositoryContainer.REPOSITORY_CONTAINER_ID, true, true);
  }

  private void testCRUD(
      OwnerType ownerType,
      String ownerPublicId,
      String ownerId,
      boolean isLegalReviewer,
      boolean isNugetCoordinate) throws Exception
  {
    String user = "admin";
    String where = "EdgeOfSpace";
    String path = isLegalReviewer ? ApiLicenseOverrideResource.LEGAL_REVIEWER_PATH : "";
    ComponentIdentifier componentIdentifier =
        isNugetCoordinate
            ? ComponentIdentifier.createNugetCoordinates("p1", "v1")
            : ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");

    HttpRequest getRequest =
        restRequest(ownerType, ownerPublicId, componentIdentifier, path).query("where", where);
    HttpRequest postRequest = restRequest(ownerType, ownerPublicId).query("where", where);

    // Create
    ApiLicenseOverrideDTO licenseOverrideReq = new ApiLicenseOverrideDTO(ownerPublicId,
        "My comment",
        "Apache-2.0",
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier), LicenseOverrideStatus.OVERRIDDEN);

    HttpResponse postResponse = postRequest.body(licenseOverrideReq).post();
    ctx.assertResponseStatus(200, postResponse);
    ApiLicenseOverrideDTO licenseOverrideCreateRes =
        postResponse.getBody(ApiLicenseOverrideDTO.class);
    assertLicenseOverride(ownerId, componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment",
        licenseOverrideCreateRes);
    assertAuditLog(ownerId, user, where, false /* isDelete */, licenseOverrideCreateRes);

    // Get
    HttpResponse getResponse = getRequest.get();
    ctx.assertResponseStatus(200, getResponse);
    ApiAppliedLicenseOverridesDTO appliedLicenseOverrides =
        getResponse.getBody(ApiAppliedLicenseOverridesDTO.class);

    LicenseOverride licenseOverride =
        getLicenseOverrideFromApplied(appliedLicenseOverrides, licenseOverrideCreateRes.id);
    assertLicenseOverride(ownerId, componentIdentifier, LicenseOverrideStatus.OVERRIDDEN,
        "Apache-2.0", "My comment", licenseOverride);

    // Update (i.e. add again)
    licenseOverrideReq = new ApiLicenseOverrideDTO(
        ownerId, "My comment updated", "GPL-2.0",
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier), LicenseOverrideStatus.OVERRIDDEN);
    postResponse = postRequest.body(licenseOverrideReq).post();
    ctx.assertResponseStatus(200, postResponse);
    ApiLicenseOverrideDTO licenseOverrideUpdateRes = postResponse.getBody(ApiLicenseOverrideDTO.class);
    assertLicenseOverride(ownerId, componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0",
        "My comment updated", licenseOverrideUpdateRes);
    assertAuditLog(ownerId, user, where, false /* isDelete */, licenseOverrideUpdateRes);

    // Get
    getResponse = getRequest.get();
    ctx.assertResponseStatus(200, getResponse);
    appliedLicenseOverrides =
        getResponse.getBody(ApiAppliedLicenseOverridesDTO.class);

    licenseOverride = getLicenseOverrideFromApplied(appliedLicenseOverrides, licenseOverrideUpdateRes.id);
    assertLicenseOverride(ownerId, componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0",
        "My comment updated", licenseOverride);

    // Delete
    postResponse = postRequest.subpath(licenseOverrideUpdateRes.id).delete();
    ctx.assertResponseStatus(204, postResponse);
    assertAuditLog(ownerId, user, where, true /* isDelete */, licenseOverrideUpdateRes);

    // Get
    licenseOverride = licenseOverrideDAO.getById(licenseOverrideCreateRes.id);
    ctx.assertResponseStatus(200, getResponse);
    assertThat(licenseOverride).isNull();

    getResponse = getRequest.get();
    appliedLicenseOverrides =
        getResponse.getBody(ApiAppliedLicenseOverridesDTO.class);

    licenseOverride = getLicenseOverrideFromApplied(appliedLicenseOverrides, licenseOverrideUpdateRes.id);
    assertThat(licenseOverride).isNull();
  }

  private void assertAuditLog(
      String ownerId,
      String user,
      String where,
      boolean isDelete,
      ApiLicenseOverrideDTO expected) throws Exception
  {
    // Verify the license override audit
    File logFile = new File(ctx.lookup(InsightWork.class).getAuditDir(ownerId), "licenses.json");
    assertThat(logFile).isFile();

    ArrayNode allLogJsonData = JsonUtils.read(logFile);
    assertThat(allLogJsonData).isNotEmpty();
    JsonNode logJsonData = allLogJsonData.get(0);
    assertThat(logJsonData).isNotNull();
    assertThat(logJsonData.get("user").asText()).isEqualTo(user);
    assertThat(logJsonData.get("where").asText()).isEqualTo(where);
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

  private void assertLicenseOverride(
      String ownerId,
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

  private void assertLicenseOverride(
      String ownerId,
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

  private LicenseOverride getLicenseOverrideFromApplied(
      ApiAppliedLicenseOverridesDTO appliedLicenseOverrides,
      final String licenseOverrideId)
  {
    return appliedLicenseOverrides.licenseOverridesByOwner.stream()
        .filter(
            licenseOverrideByOwner -> licenseOverrideByOwner.licenseOverride != null &&
                licenseOverrideByOwner.licenseOverride.getId().equals(licenseOverrideId))
        .findFirst()
        .map(
            licenseOverrideByOwner -> licenseOverrideByOwner.licenseOverride)
        .orElse(null);
  }
}
