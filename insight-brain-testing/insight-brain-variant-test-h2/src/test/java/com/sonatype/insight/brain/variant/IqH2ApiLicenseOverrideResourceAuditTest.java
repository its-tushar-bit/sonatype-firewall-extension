/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseOverrideDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.license.LicenseOverrideUtil;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ApiLicenseOverrideResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private ApiLicenseOverrideDTO licenseOverride;

  private Application app;

  private User unauthorizedUser;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void setup() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();

    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    licenseOverride = new ApiLicenseOverrideDTO(null /* ownerId */,
        "",
        new HashSet<>(Arrays.asList("Apache-2.0", "GPL-2.0")),
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier),
        LicenseOverrideStatus.OVERRIDDEN);
    app = ctx.tempEntity().newApplicationWithParent();
  }

  @AfterEach
  void after() {
    logOutput.tearDown();
  }

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }

  @Override
  public String getUnauthorizedUsername() {
    return unauthorizedUser.getUsername();
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private java.util.function.Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  private HttpRequest restRequest(OwnerType ownerType, String ownerId) {
    return ctx.restRequest().path(PublicApiPaths.LICENSE_OVERRIDE_RESOURCE_PATH_V2).parameter(ownerType, ownerId);
  }

  private void assertOverrideData(
      AuditDTO auditDTO,
      ApiLicenseOverrideDTO override,
      String... selectedOverriddenLicenseNames)
  {
    assertOverrideData(auditDTO, override, false, selectedOverriddenLicenseNames);
  }

  @SuppressWarnings("unchecked")
  private void assertOverrideData(
      AuditDTO auditDTO,
      ApiLicenseOverrideDTO override,
      boolean isDelete,
      String... selectedOverriddenLicenseNames)
  {
    assertCustomObject(auditDTO, "componentIdentifier", override.componentIdentifier);
    assertCustomData(auditDTO, "status", isDelete ? "inherited" : override.status.name().toLowerCase(Locale.ROOT));
    assertCustomData(auditDTO, "comment", isDelete ? null : override.comment);
    if (selectedOverriddenLicenseNames.length > 0) {
      assertThat(auditDTO.data).containsKey("licenseNames");
      assertThat((List<String>) auditDTO.data.get("licenseNames"))
          .containsExactlyInAnyOrder(selectedOverriddenLicenseNames);
    }
    else {
      assertThat(auditDTO.data).doesNotContainKey("licenseNames");
    }
  }

  @Test
  void testAddLicenseOverride_Application() throws Exception {
    licenseOverride.comment = "My comment";
    ApiLicenseOverrideDTO response =
        restRequest(OwnerType.APPLICATION, app.getPublicId()).body(licenseOverride)
            .post()
            .getBody(ApiLicenseOverrideDTO.class);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_COMPONENT_LICENSE, null);
    assertOverrideData(auditDTO, response, "Apache-2.0", "GPL-2.0");
    assertApplicationData(auditDTO, app);
  }

  @Test
  void testAddLicenseOverride_Organization() throws Exception {
    Organization org = ctx.tempEntity().newOrganization("LicenseOverrideResourceAuditTest");

    licenseOverride.licenseIds = null;
    licenseOverride.status = LicenseOverrideStatus.OPEN;
    ApiLicenseOverrideDTO response =
        restRequest(OwnerType.ORGANIZATION, org.getPublicId()).body(licenseOverride)
            .post()
            .getBody(ApiLicenseOverrideDTO.class);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_COMPONENT_LICENSE, null);
    assertOverrideData(auditDTO, response);
    assertOrganizationData(auditDTO, org);
  }

  @Test
  void testAddLicenseOverride_Repository() throws Exception {
    Repository repo = ctx.tempEntity().newRepository();

    ApiLicenseOverrideDTO response = restRequest(OwnerType.REPOSITORY, repo.getId()).body(licenseOverride)
        .post()
        .getBody(ApiLicenseOverrideDTO.class);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_COMPONENT_LICENSE, null);
    assertOverrideData(auditDTO, response, "Apache-2.0", "GPL-2.0");
    assertRepositoryData(auditDTO, repo);
  }

  @Test
  void testAddLicenseOverride_RepositoryContainer() throws Exception {
    ApiLicenseOverrideDTO response = restRequest(
        OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID).body(licenseOverride)
            .post()
            .getBody(ApiLicenseOverrideDTO.class);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_COMPONENT_LICENSE, null);
    assertOverrideData(auditDTO, response, "Apache-2.0", "GPL-2.0");
    assertRepositoryContainerData(auditDTO);
  }

  @Test
  void testAddLicenseOverride_Unauthorized() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();

    restRequest(OwnerType.APPLICATION, app.getPublicId()).body(licenseOverride).with(unauthorizedUser()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_COMPONENT_LICENSE, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  @Test
  void testDeleteLicenseOverride() throws Exception {
    LicenseOverride toBeDeleted = ctx.tempEntity()
        .newLicenseOverride(app.getId(), licenseOverride.componentIdentifier.toComponentIdentifier(),
            licenseOverride.status, licenseOverride.licenseIds, "Existing comment");

    restRequest(OwnerType.APPLICATION, app.getPublicId()).path(toBeDeleted.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_COMPONENT_LICENSE, null);
    assertOverrideData(auditDTO, LicenseOverrideUtil.toApiLicenseOverrideDTO(toBeDeleted), true);
    assertApplicationData(auditDTO, app);
  }

  @Test
  void testDeleteLicenseOverride_Unauthorized() throws Exception {
    LicenseOverride toBeDeleted = ctx.tempEntity()
        .newLicenseOverride(app.getId(), licenseOverride.componentIdentifier.toComponentIdentifier(),
            licenseOverride.status, licenseOverride.licenseIds, "Existing comment");

    restRequest(OwnerType.APPLICATION, app.getPublicId()).path(toBeDeleted.getId()).with(unauthorizedUser()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_COMPONENT_LICENSE, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  private static final class TestLogOutput
      extends LogOutput
  {
    TestLogOutput(String... loggerNames) {
      super(loggerNames);
    }

    void tearDown() {
      after();
    }
  }
}
