/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import static org.junit.Assert.*;

import org.junit.Test;

import com.sonatype.insight.brain.license.LicenseThreatGroupResource.ApplicableLicenseThreatGroups;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.utils.IdUtils;

public class OrganizationLicenseThreatGroupResourceTest
    extends AbstractLicenseThreatGroupResourceTest
{
  @Test
  public void testCRUD() throws Exception {
    Organization organization = createOrganization("testCRUD-Organization", true /* createLicenseThreatGroups */);
    testCRUD(organization.getId(), organization.getId());
  }

  @Test
  public void testDelete_OwnerIdMismatch() throws Exception {
    Organization organization1 = createOrganization("testDeleteOwnerIdMismatch1", false /* createLicenseThreatGroups */);
    Organization organization2 = createOrganization("testDeleteOwnerIdMismatch2", false /* createLicenseThreatGroups */);
    testDelete_OwnerIdMismatch(organization1.getId(), organization1.getId(), organization2.getId(),
        organization2.getId());
  }

  @Test
  public void testDelete_InUseByPolicy() throws Exception {
    Organization org = createOrganization("test", false);
    testDelete_InUseByPolicy(org.getId(), org.getId(), org.getId());
  }

  @Test
  public void testDelete_InUseByPolicyInChildApp() throws Exception {
    Application app = createApplication("appPublicId", "appName");
    testDelete_InUseByPolicy(app.getOrganizationId(), app.getOrganizationId(), app.getId(), "in application 'appName'");
  }

  @Test
  public void testGetApplicable() throws Exception {
    Organization org = createOrganization("orgName", false);
    createLicenseThreatGroup("LTG-0", org.getId());
    createLicenseThreatGroup("LTG-1", org.getId());

    ApplicableLicenseThreatGroups altgs = getApplicableLicenseThreatGroups(org.getId());
    assertNotNull(altgs);
    assertNotNull(altgs.licenseThreatGroupsByOwner);
    assertEquals(1, altgs.licenseThreatGroupsByOwner.size());
    assertLicenseThreatGroupsByOwner(org.getId(), org.getName(), IdUtils.TYPE_ORGANIZATION, 2,
        altgs.licenseThreatGroupsByOwner.get(0));
  }

  @Override
  protected String getOwnerType() {
    return "organization";
  }
}
