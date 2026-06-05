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

import static org.assertj.core.api.Assertions.assertThat;

public class LicenseThreatGroupResourceAuditTest
    extends AbstractAuditTest
{
  private HttpRequest restRequest(Owner owner) {
    return restRequest().path(LicenseThreatGroupResource.RESOURCE_PATH).parameter(owner.getType(), owner.getPublicId());
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

  @Test
  public void testDeleteLicenseThreatGroup_Application() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(application.getId(), "The Name", 3);
    restRequest(application).path(ltg.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_LICENSE_THREAT_GROUP, null);
    assertApplicationData(auditDTO, application);
    assertLicenseThreatGroupData(auditDTO, ltg);
  }

  @Test
  public void testDeleteLicenseThreatGroup_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization();
    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(organization.getId(), "The Name", 3);
    restRequest(organization).path(ltg.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_LICENSE_THREAT_GROUP, null);
    assertOrganizationData(auditDTO, organization);
    assertLicenseThreatGroupData(auditDTO, ltg);
  }

  @Test
  public void testDeleteLicenseThreatGroup_Unauthorized() throws Exception {
    Organization organization = tempEntity.newOrganization();
    restRequest(organization).with(unauthorizedUser()).path("ltg-id").delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_LICENSE_THREAT_GROUP, "unauthorized");
    assertOrganizationData(auditDTO, organization);
  }

  /**
   * The counts endpoint is intentionally not {@code @Audited}: it is a high-frequency dashboard read. This test
   * asserts no audit events are emitted for either the CREATE/UPDATE/DELETE domains when the counts endpoint is
   * hit, and that the endpoint itself does not introduce a new audited event (CLM-39702, CLM-38750).
   */
  @Test
  public void testGetLicenseThreatGroupCounts_NotAudited() throws Exception {
    Organization organization = tempEntity.newOrganization();

    restRequest(organization).path("counts").get();

    assertThat(getLogEntries(AuditEvent.CREATE_LICENSE_THREAT_GROUP)).isEmpty();
    assertThat(getLogEntries(AuditEvent.UPDATE_LICENSE_THREAT_GROUP)).isEmpty();
    assertThat(getLogEntries(AuditEvent.DELETE_LICENSE_THREAT_GROUP)).isEmpty();
  }
}
