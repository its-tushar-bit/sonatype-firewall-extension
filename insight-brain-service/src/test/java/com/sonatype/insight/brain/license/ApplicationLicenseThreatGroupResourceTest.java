/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.license.LicenseThreatGroupResource.ApplicableLicenseThreatGroups;
import com.sonatype.insight.brain.license.LicenseThreatGroupResource.LicenseThreatGroupWithLicenses;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.utils.IdUtils;

import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class ApplicationLicenseThreatGroupResourceTest
    extends AbstractLicenseThreatGroupResourceTest
{
  @Test
  public void testCRUD() throws Exception {
    String appPublicId = "LicenseThreatGroupResourceTest_AppId";
    Application application = tempEntity.newApplicationWithParent(appPublicId);
    testCRUD(appPublicId, application.getId());
  }

  @Test
  public void testDelete_OwnerIdMismatch() throws Exception {
    String appPublicId1 = "LicenseThreatGroupResourceTest_AppId1";
    Application application1 = tempEntity.newApplicationWithParent(appPublicId1);
    String appPublicId2 = "LicenseThreatGroupResourceTest_AppId2";
    Application application2 = tempEntity.newApplicationWithParent(appPublicId2);
    testDelete_OwnerIdMismatch(appPublicId1, application1.getId(), appPublicId2, application2.getId());
  }

  @Test
  public void testDelete_InUseByPolicy() throws Exception {
    Application app = tempEntity.newApplicationWithParent("appPublicId");
    testDelete_InUseByPolicy(app.getPublicId(), app.getId(), app.getId());
  }

  @Test
  public void testGetApplicable() throws Exception {
    Organization org = tempEntity.newOrganization("orgName", false);
    tempEntity.newLicenseThreatGroup(org.getId(), "LTG-2", 5, "GPL-2.0", "GPL-3.0");
    Application app = tempEntity.newApplication("appName", "appPublicId", org.getId());
    app.setOrganizationId(org.getId());
    new ApplicationDAO().update(app);
    tempEntity.newLicenseThreatGroup(app.getId(), "LTG-0", 5, "Apache-2.0");
    tempEntity.newLicenseThreatGroup(app.getId(), "LTG-1", 5, "EPL-1.0");

    ApplicableLicenseThreatGroups altgs = getApplicableLicenseThreatGroups(app.getPublicId());
    assertNotNull(altgs);
    assertNotNull(altgs.licenseThreatGroupsByOwner);
    assertEquals(2, altgs.licenseThreatGroupsByOwner.size());
    assertLicenseThreatGroupsByOwner(app.getId(), app.getName(), IdUtils.TYPE_APPLICATION, 2,
        altgs.licenseThreatGroupsByOwner.get(0));
    for (LicenseThreatGroupWithLicenses ltgwl : altgs.licenseThreatGroupsByOwner.get(0).licenseThreatGroups) {
      assertThat(ltgwl.licenses, hasSize(1));
    }
    assertLicenseThreatGroupsByOwner(org.getId(), org.getName(), IdUtils.TYPE_ORGANIZATION, 1,
        altgs.licenseThreatGroupsByOwner.get(1));
    for (LicenseThreatGroupWithLicenses ltgwl : altgs.licenseThreatGroupsByOwner.get(1).licenseThreatGroups) {
      assertThat(ltgwl.licenses, hasSize(2));
    }
  }

  @Override
  protected String getOwnerType() {
    return "application";
  }
}
