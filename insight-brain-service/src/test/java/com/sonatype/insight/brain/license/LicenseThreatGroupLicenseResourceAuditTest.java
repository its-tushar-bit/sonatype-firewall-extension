/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import java.util.Arrays;
import java.util.Collections;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

public class LicenseThreatGroupLicenseResourceAuditTest
    extends AbstractAuditTest
{
  private HttpRequest restRequest(Owner owner, String ltgId) {
    return restRequest().path(LicenseThreatGroupLicenseResource.RESOURCE_PATH)
        .parameter(owner.getType(),
            owner.getPublicId(), ltgId);
  }

  private void assertLicenseData(AuditDTO auditDTO, LicenseThreatGroup ltg, String... licenseNames) {
    assertCustomData(auditDTO, "licenseThreatGroupId", ltg.getId());
    assertCustomData(auditDTO, "licenseThreatGroupName", ltg.getName());
    assertCustomData(auditDTO, "licenseNames", Arrays.asList(licenseNames));
  }

  @Test
  public void testSetLicenseThreatGroupLicenses_Application() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(application.getId());
    restRequest(application, ltg.getId()).body(Collections.emptyList()).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_LICENSE_THREAT_GROUP_LICENSES, null);
    assertApplicationData(auditDTO, application);
    assertLicenseData(auditDTO, ltg);
  }

  @Test
  public void testSetLicenseThreatGroupLicenses_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization();
    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(organization.getId());
    restRequest(organization, ltg.getId()).body(Arrays.asList("Apache-UNSPECIFIED", "PUBLIC-DOMAIN")).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_LICENSE_THREAT_GROUP_LICENSES, null);
    assertOrganizationData(auditDTO, organization);
    assertLicenseData(auditDTO, ltg, "Apache", "Public Domain");
  }

  @Test
  public void testSetLicenseThreatGroupLicenses_Unauthorized() throws Exception {
    Organization organization = tempEntity.newOrganization();
    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(organization.getId());
    restRequest(organization, ltg.getId())
        .with(unauthorizedUser())
        .body(Collections.singletonList("Not-Declared"))
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_LICENSE_THREAT_GROUP_LICENSES, "unauthorized");
    assertOrganizationData(auditDTO, organization);
  }
}
