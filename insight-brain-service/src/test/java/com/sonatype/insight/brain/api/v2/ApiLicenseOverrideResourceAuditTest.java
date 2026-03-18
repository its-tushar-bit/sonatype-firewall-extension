/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseOverrideDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.license.LicenseOverrideUtil;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiLicenseOverrideResourceAuditTest
    extends AbstractAuditTest
{
  private ApiLicenseOverrideDTO licenseOverride;

  private Application app;

  private HttpRequest restRequest(OwnerType ownerType, String ownerId) {
    return restRequest().path(PublicApiPaths.LICENSE_OVERRIDE_RESOURCE_PATH_V2).parameter(ownerType, ownerId);
  }

  @Before
  public void setup() {
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    licenseOverride = new ApiLicenseOverrideDTO(null /* ownerId */,
        "",
        new HashSet<>(Arrays.asList("Apache-2.0", "GPL-2.0")),
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier),
        LicenseOverrideStatus.OVERRIDDEN);
    app = tempEntity.newApplicationWithParent();
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
  public void testAddLicenseOverride_Application() throws Exception {
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
  public void testAddLicenseOverride_Organization() throws Exception {
    Organization org = tempEntity.newOrganization("LicenseOverrideResourceAuditTest");

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
  public void testAddLicenseOverride_Repository() throws Exception {
    Repository repo = tempEntity.newRepository();

    ApiLicenseOverrideDTO response = restRequest(OwnerType.REPOSITORY, repo.getId()).body(licenseOverride)
        .post()
        .getBody(ApiLicenseOverrideDTO.class);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_COMPONENT_LICENSE, null);
    assertOverrideData(auditDTO, response, "Apache-2.0", "GPL-2.0");
    assertRepositoryData(auditDTO, repo);
  }

  @Test
  public void testAddLicenseOverride_RepositoryContainer() throws Exception {
    ApiLicenseOverrideDTO response = restRequest(
        OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID).body(licenseOverride)
            .post()
            .getBody(ApiLicenseOverrideDTO.class);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_COMPONENT_LICENSE, null);
    assertOverrideData(auditDTO, response, "Apache-2.0", "GPL-2.0");
    assertRepositoryContainerData(auditDTO);
  }

  @Test
  public void testAddLicenseOverride_Unauthorized() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    restRequest(OwnerType.APPLICATION, app.getPublicId()).body(licenseOverride).with(unauthorizedUser()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_COMPONENT_LICENSE, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testDeleteLicenseOverride() throws Exception {
    LicenseOverride toBeDeleted = tempEntity
        .newLicenseOverride(app.getId(), licenseOverride.componentIdentifier.toComponentIdentifier(),
            licenseOverride.status, licenseOverride.licenseIds, "Existing comment");

    restRequest(OwnerType.APPLICATION, app.getPublicId()).path(toBeDeleted.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_COMPONENT_LICENSE, null);
    assertOverrideData(auditDTO, LicenseOverrideUtil.toApiLicenseOverrideDTO(toBeDeleted), true);
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testDeleteLicenseOverride_Unauthorized() throws Exception {
    LicenseOverride toBeDeleted = tempEntity
        .newLicenseOverride(app.getId(), licenseOverride.componentIdentifier.toComponentIdentifier(),
            licenseOverride.status, licenseOverride.licenseIds, "Existing comment");

    restRequest(OwnerType.APPLICATION, app.getPublicId()).path(toBeDeleted.getId()).with(unauthorizedUser()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_COMPONENT_LICENSE, "unauthorized");
    assertApplicationData(auditDTO, app);
  }
}
