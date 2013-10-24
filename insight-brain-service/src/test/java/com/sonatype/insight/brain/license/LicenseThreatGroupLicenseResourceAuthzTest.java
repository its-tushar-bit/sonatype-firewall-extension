/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import java.util.Arrays;

import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.brain.utils.IdUtils;

import org.junit.Test;

public class LicenseThreatGroupLicenseResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testGetLicenseThreatGroupLicenses() throws Exception {
    Role role = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());

    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(app.getId());

    String url = getRestUrl(LicenseThreatGroupLicenseResource.SERVICE_PATH, IdUtils.TYPE_APPLICATION,
        app.getPublicId(), ltg.getId());
    testAuthzGet(url);

    tempEntity.newMembershipMapping(org.getId(), role.getId(), authorized.getUsername());

    url = getRestUrl(LicenseThreatGroupLicenseResource.SERVICE_PATH, IdUtils.TYPE_ORGANIZATION, org.getId(),
        ltg.getId());
    testAuthzGet(url);
  }

  @Test
  public void testSetLicenseThreatGroupLicenses() throws Exception {
    Role role = tempEntity.newRole(false, Permission.WRITE);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());

    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(app.getId());
    String json = toJson(Arrays.asList("MIT"));

    String url = getRestUrl(LicenseThreatGroupLicenseResource.SERVICE_PATH, IdUtils.TYPE_APPLICATION,
        app.getPublicId(), ltg.getId());
    testAuthzPut(url, json);

    tempEntity.newMembershipMapping(org.getId(), role.getId(), authorized.getUsername());

    url = getRestUrl(LicenseThreatGroupLicenseResource.SERVICE_PATH, IdUtils.TYPE_ORGANIZATION, org.getId(),
        ltg.getId());
    testAuthzPut(url, json);
  }
}
