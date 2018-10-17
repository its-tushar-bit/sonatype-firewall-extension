/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

public class LicenseOverrideResourceAuditTest
    extends AbstractAuditTest
{
  private LicenseOverride licenseOverride;

  private HttpRequest restRequest(OwnerType ownerType, String ownerId) {
    return restRequest().path(LicenseOverrideResource.RESOURCE_PATH).parameter(ownerType, ownerId);
  }

  private AuditDTO assertAuditLog(String error) {
    AuditDTO auditDTO = awaitLogEntries(AuditEvent.UPDATE_COMPONENT_LICENSE, 1).get(0);
    assertStandardData(auditDTO, AuditEvent.UPDATE_COMPONENT_LICENSE, error);
    return auditDTO;
  }

  @Before
  public void setup() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    licenseOverride = new LicenseOverride(null /* ownerId */, componentIdentifier, LicenseOverrideStatus.OVERRIDDEN,
        new HashSet<>(Arrays.asList("Apache-2.0", "GPL-2.0")), null);
  }

  @SuppressWarnings("unchecked")
  private void assertOverrideData(AuditDTO auditDTO, LicenseOverride override, String... selectedOverriddenLicenseNames)
  {
    assertCustomObject(auditDTO, "componentIdentifier", override.getComponentIdentifier());
    assertCustomData(auditDTO, "status", override.getStatus().name().toLowerCase(Locale.ROOT));
    assertCustomData(auditDTO, "comment", override.getComment());
    if (selectedOverriddenLicenseNames.length > 0) {
      assertThat(auditDTO.data, hasKey("licenseNames"));
      assertThat((List<String>) auditDTO.data.get("licenseNames"), containsInAnyOrder(selectedOverriddenLicenseNames));
    }
    else {
      assertThat(auditDTO.data, not(hasKey("licenseNames")));
    }
  }

  @Test
  public void testAddLicenseOverride_Application() throws Exception {
    Application app = tempEntity.newApplicationWithParent("LicenseOverrideResourceAuditTest");

    licenseOverride.setComment("My comment");
    LicenseOverride response = restRequest(OwnerType.APPLICATION, app.getPublicId()).body(licenseOverride).post()
        .getBody(LicenseOverride.class);

    AuditDTO auditDTO = assertAuditLog(null);
    assertOverrideData(auditDTO, response, "Apache-2.0", "GPL-2.0");
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testAddLicenseOverride_Organization() throws Exception {
    Organization org = tempEntity.newOrganization("LicenseOverrideResourceAuditTest");

    licenseOverride.setLicenseIds(null);
    licenseOverride.setStatus(LicenseOverrideStatus.OPEN);
    LicenseOverride response = restRequest(OwnerType.ORGANIZATION, org.getPublicId()).body(licenseOverride).post()
        .getBody(LicenseOverride.class);

    AuditDTO auditDTO = assertAuditLog(null);
    assertOverrideData(auditDTO, response);
    assertOrganizationData(auditDTO, org);
  }

  @Test
  public void testAddLicenseOverride_Repository() throws Exception {
    Repository repo = tempEntity.newRepository();

    LicenseOverride response = restRequest(OwnerType.REPOSITORY, repo.getId()).body(licenseOverride).post()
        .getBody(LicenseOverride.class);

    AuditDTO auditDTO = assertAuditLog(null);
    assertOverrideData(auditDTO, response, "Apache-2.0", "GPL-2.0");
    assertRepositoryData(auditDTO, repo);
  }

  @Test
  public void testAddLicenseOverride_RepositoryContainer() throws Exception {
    LicenseOverride response = restRequest(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID)
        .body(licenseOverride).post().getBody(LicenseOverride.class);

    tempEntity.register(response);

    AuditDTO auditDTO = assertAuditLog(null);
    assertOverrideData(auditDTO, response, "Apache-2.0", "GPL-2.0");
    assertRepositoryContainerData(auditDTO);
  }

  @Test
  public void testAddLicenseOverride_Unauthorized() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    restRequest(OwnerType.APPLICATION, app.getPublicId()).body(licenseOverride)
        .auth(unauthorizedUser.getUsername(), unauthorizedUser.getPassword()).post();

    AuditDTO auditDTO = assertAuditLog("unauthorized");
    assertApplicationData(auditDTO, app);
  }
}
