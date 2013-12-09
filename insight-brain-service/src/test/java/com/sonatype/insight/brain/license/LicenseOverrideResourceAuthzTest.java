/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.brain.utils.IdUtils;

import com.ning.http.client.Response;
import org.junit.Test;

public class LicenseOverrideResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testAddLicenseOverride() throws Exception {
    grantWritePermission(app.getId());
    LicenseOverride override = new LicenseOverride(null, "g", "a", "1", LicenseOverrideStatus.CONFIRMED, null, "test");

    String url = getRestUrl(LicenseOverrideResource.SERVICE_PATH, IdUtils.TYPE_APPLICATION, app.getPublicId());
    Response response = testAuthzPost(url, toJson(override));
    override = fromJson(response, LicenseOverride.class);
    new LicenseOverrideDAO().delete(override);

    grantWritePermission(org.getId());
    override = new LicenseOverride(null, "g", "a", "1", LicenseOverrideStatus.CONFIRMED, null, "test");

    url = getRestUrl(LicenseOverrideResource.SERVICE_PATH, IdUtils.TYPE_ORGANIZATION, org.getId());
    response = testAuthzPost(url, toJson(override));
    override = fromJson(response, LicenseOverride.class);
    new LicenseOverrideDAO().delete(override);
  }

  @Test
  public void testDeleteLicenseOverride() throws Exception {
    grantWritePermission(app.getId());
    LicenseOverride override = tempEntity.newLicenseOverride(app.getId(), "g", "a", "1",
        LicenseOverrideStatus.CONFIRMED, null);

    String url = getRestUrl(LicenseOverrideResource.SERVICE_PATH + "/{overrideId}", IdUtils.TYPE_APPLICATION,
        app.getPublicId(), override.getId());
    testAuthzDelete(url);

    grantWritePermission(org.getId());
    override = tempEntity.newLicenseOverride(org.getId(), "g", "a", "1", LicenseOverrideStatus.CONFIRMED, null);

    url = getRestUrl(LicenseOverrideResource.SERVICE_PATH + "/{overrideId}", IdUtils.TYPE_ORGANIZATION, org.getId(),
        override.getId());
    testAuthzDelete(url);
  }

  @Test
  public void testGetAppliedLicenseOverrides() throws Exception {
    grantReadPermission(app.getId());

    String url = getRestUrl(LicenseOverrideResource.SERVICE_PATH + "/applied/{g}/{a}/{v}", IdUtils.TYPE_APPLICATION,
        app.getPublicId(), "g", "a", "1");
    testAuthzGet(url);

    grantReadPermission(org.getId());

    url = getRestUrl(LicenseOverrideResource.SERVICE_PATH + "/applied/{g}/{a}/{v}", IdUtils.TYPE_ORGANIZATION,
        org.getId(), "g", "a", "1");
    testAuthzGet(url);
  }
}
