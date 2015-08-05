/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import com.sonatype.insight.brain.license.LicenseThreatGroupService.ApplicableLicenseThreatGroups;
import com.sonatype.insight.brain.license.LicenseThreatGroupService.LicenseThreatGroupWithLicenses;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;

import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class OrganizationLicenseThreatGroupResourceTest
    extends AbstractLicenseThreatGroupResourceTest
{
  @Test
  public void testCRUD() throws Exception {
    Organization organization = tempEntity.newOrganization("testCRUD-Organization");
    testCRUD(organization.getId(), organization.getId());
  }

  @Test
  public void testDelete_OwnerIdMismatch() throws Exception {
    Organization organization1 = tempEntity.newOrganization("testDeleteOwnerIdMismatch1");
    Organization organization2 = tempEntity.newOrganization("testDeleteOwnerIdMismatch2");
    testDelete_OwnerIdMismatch(organization1.getId(), organization1.getId(), organization2.getId(),
        organization2.getId());
  }

  @Test
  public void testDelete_InUseByPolicy() throws Exception {
    Organization org = tempEntity.newOrganization("test");
    testDelete_InUseByPolicy(org.getId(), org.getId(), org.getId());
  }

  @Test
  public void testDelete_InUseByPolicyInChildApp() throws Exception {
    Application app = tempEntity.newApplicationWithParent("appPublicId", "appName");
    testDelete_InUseByPolicy(app.getOrganizationId(), app.getOrganizationId(), app.getId(), "in application 'appName'");
  }

  @Test
  public void testDelete_InUseByPolicyInGrandChildApp() throws Exception {
    Organization org = tempEntity.newOrganization("orgName");
    Application app = tempEntity.newApplication("appName", "appPublicId", org.getId());
    testDelete_InUseByPolicy(org.getParentOrganizationId(), org.getParentOrganizationId(), app.getId(),
        "in application 'appName'");
  }

  @Test
  public void testDelete_InUseByPolicyInChildOrg() throws Exception {
    Organization org = tempEntity.newOrganization("orgName");
    testDelete_InUseByPolicy(org.getParentOrganizationId(), org.getParentOrganizationId(), org.getId(),
        "in organization 'orgName'");
  }

  @Test
  public void testGetApplicable() throws Exception {
    Organization org = tempEntity.newOrganization("orgName");
    tempEntity.newLicenseThreatGroup(org.getId(), "LTG-0", 5, "Apache-2.0");
    tempEntity.newLicenseThreatGroup(org.getId(), "LTG-1", 5, "EPL-1.0");

    Organization parentOrg = orgDAO.getById(org.getParentOrganizationId());
    tempEntity.newLicenseThreatGroup(parentOrg.getId(), "LTG-3", 5, "GPL-2.0", "GPL-3.0");

    ApplicableLicenseThreatGroups altgs = getApplicableLicenseThreatGroups(org.getId());
    assertNotNull(altgs);
    assertNotNull(altgs.licenseThreatGroupsByOwner);
    assertEquals(2, altgs.licenseThreatGroupsByOwner.size());
    assertLicenseThreatGroupsByOwner(org.getId(), org.getName(), OwnerType.ORGANIZATION, 2,
        altgs.licenseThreatGroupsByOwner.get(0));
    for (LicenseThreatGroupWithLicenses ltgwl : altgs.licenseThreatGroupsByOwner.get(0).licenseThreatGroups) {
      assertThat(ltgwl.licenses, hasSize(1));
    }
    // Expect 5 LTGs (4 default + 1 created above)
    assertLicenseThreatGroupsByOwner(parentOrg.getId(), parentOrg.getName(), OwnerType.ORGANIZATION, 5,
        altgs.licenseThreatGroupsByOwner.get(1));
    for (LicenseThreatGroupWithLicenses ltgwl : altgs.licenseThreatGroupsByOwner.get(0).licenseThreatGroups) {
      assertThat(ltgwl.licenses, hasSize(1));
    }
  }

  @Override
  protected String getOwnerType() {
    return "organization";
  }
}
