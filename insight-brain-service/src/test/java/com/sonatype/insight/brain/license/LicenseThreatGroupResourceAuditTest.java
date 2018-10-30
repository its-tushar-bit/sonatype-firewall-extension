/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

public class LicenseThreatGroupResourceAuditTest
    extends AbstractAuditTest
{
  private HttpRequest restRequest(Owner owner) {
    return restRequest().path(LicenseThreatGroupResource.RESOURCE_PATH).parameter(owner.getType(), owner.getPublicId());
  }

  private AuditDTO assertAuditLog(AuditEvent event, String error) {
    AuditDTO auditDTO = awaitLogEntries(event, 1).get(0);
    assertStandardData(auditDTO, event, error);
    return auditDTO;
  }

  private void assertLicenseThreatGroupData(AuditDTO auditDTO, LicenseThreatGroup ltg) {
    assertCustomData(auditDTO, "licenseThreatGroupId", ltg.getId());
    assertCustomData(auditDTO, "licenseThreatGroupName", ltg.getName());
    assertCustomData(auditDTO, "licenseThreatGroupThreatLevel", ltg.getThreatLevel());
  }

  @Test
  public void testAddLicenseThreatGroup_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization();
    LicenseThreatGroup ltg = new LicenseThreatGroup(null, "Lawyer Nightmare", 7);
    ltg = restRequest(organization).body(ltg).post().getBody(LicenseThreatGroup.class);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_LICENSE_THREAT_GROUP, null);
    assertOrganizationData(auditDTO, organization);
    assertLicenseThreatGroupData(auditDTO, ltg);
  }

  @Test
  public void testAddLicenseThreatGroup_Unauthorized() throws Exception {
    Organization organization = tempEntity.newOrganization();
    LicenseThreatGroup ltg = new LicenseThreatGroup(null, "Lawyer Nightmare", 7);
    restRequest(organization).with(unauthorizedUser()).body(ltg).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_LICENSE_THREAT_GROUP, "unauthorized");
    assertOrganizationData(auditDTO, organization);
  }

  @Test
  public void testUpdateLicenseThreatGroup_Application() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    LicenseThreatGroup ltg = new LicenseThreatGroup(null, "Lawyer Nightmare", 7);
    ltg.setId(tempEntity.newLicenseThreatGroup(application.getId(), "Old Name", 5).getId());
    restRequest(application).body(ltg).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_LICENSE_THREAT_GROUP, null);
    assertApplicationData(auditDTO, application);
    assertLicenseThreatGroupData(auditDTO, ltg);
  }

  @Test
  public void testUpdateLicenseThreatGroup_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization();
    LicenseThreatGroup ltg = new LicenseThreatGroup(null, "Lawyer Nightmare", 7);
    ltg.setId(tempEntity.newLicenseThreatGroup(organization.getId(), "Old Name", 5).getId());
    restRequest(organization).body(ltg).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_LICENSE_THREAT_GROUP, null);
    assertOrganizationData(auditDTO, organization);
    assertLicenseThreatGroupData(auditDTO, ltg);
  }

  @Test
  public void testUpdateLicenseThreatGroup_Unauthorized() throws Exception {
    Organization organization = tempEntity.newOrganization();
    LicenseThreatGroup ltg = new LicenseThreatGroup(null, "Lawyer Nightmare", 7);
    ltg.setId(tempEntity.newLicenseThreatGroup(organization.getId(), "Old Name", 5).getId());
    restRequest(organization).with(unauthorizedUser()).body(ltg).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_LICENSE_THREAT_GROUP, "unauthorized");
    assertOrganizationData(auditDTO, organization);
  }
}
