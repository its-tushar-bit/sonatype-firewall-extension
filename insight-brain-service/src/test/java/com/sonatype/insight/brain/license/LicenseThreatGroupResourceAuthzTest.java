/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.brain.utils.IdUtils;

import com.ning.http.client.Response;
import org.junit.Test;

public class LicenseThreatGroupResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testGetLicenseThreatGroups() throws Exception {
    grantReadPermission(app.getId());

    String url = getRestUrl(LicenseThreatGroupResource.SERVICE_PATH, IdUtils.TYPE_APPLICATION, app.getPublicId());
    testAuthzGet(url);

    grantReadPermission(org.getId());

    url = getRestUrl(LicenseThreatGroupResource.SERVICE_PATH, IdUtils.TYPE_ORGANIZATION, org.getId());
    testAuthzGet(url);
  }

  @Test
  public void testGetApplicableLicenseThreatGroups() throws Exception {
    grantReadPermission(app.getId());

    String url = getRestUrl(LicenseThreatGroupResource.SERVICE_PATH + "/applicable", IdUtils.TYPE_APPLICATION,
        app.getPublicId());
    testAuthzGet(url);

    grantReadPermission(org.getId());

    url = getRestUrl(LicenseThreatGroupResource.SERVICE_PATH + "/applicable", IdUtils.TYPE_ORGANIZATION, org.getId());
    testAuthzGet(url);
  }

  @Test
  public void testAddLicenseThreatGroup() throws Exception {
    grantWritePermission(app.getId());

    LicenseThreatGroup ltg = new LicenseThreatGroup(null, "Test LTG", 5);

    String url = getRestUrl(LicenseThreatGroupResource.SERVICE_PATH, IdUtils.TYPE_APPLICATION, app.getPublicId());
    Response response = testAuthzPost(url, toJson(ltg));
    ltg = fromJson(response, LicenseThreatGroup.class);
    new LicenseThreatGroupDAO().delete(ltg);

    grantWritePermission(org.getId());

    url = getRestUrl(LicenseThreatGroupResource.SERVICE_PATH, IdUtils.TYPE_ORGANIZATION, org.getId());
    response = testAuthzPost(url, toJson(ltg));
    ltg = fromJson(response, LicenseThreatGroup.class);
    new LicenseThreatGroupDAO().delete(ltg);
  }

  @Test
  public void testUpdateLicenseThreatGroup() throws Exception {
    grantWritePermission(app.getId());

    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(app.getId());

    String url = getRestUrl(LicenseThreatGroupResource.SERVICE_PATH, IdUtils.TYPE_APPLICATION, app.getPublicId());
    testAuthzPut(url, toJson(ltg));

    grantWritePermission(org.getId());
    ltg = tempEntity.newLicenseThreatGroup(org.getId());

    url = getRestUrl(LicenseThreatGroupResource.SERVICE_PATH, IdUtils.TYPE_ORGANIZATION, org.getId());
    testAuthzPut(url, toJson(ltg));
  }

  @Test
  public void testDeleteLicenseThreatGroup() throws Exception {
    grantWritePermission(app.getId());

    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(app.getId());

    String url = getRestUrl(LicenseThreatGroupResource.SERVICE_PATH + "/{ltgId}", IdUtils.TYPE_APPLICATION,
        app.getPublicId(), ltg.getId());
    testAuthzDelete(url);

    grantWritePermission(org.getId());
    ltg = tempEntity.newLicenseThreatGroup(org.getId());

    url = getRestUrl(LicenseThreatGroupResource.SERVICE_PATH + "/{ltgId}", IdUtils.TYPE_ORGANIZATION, org.getId(),
        ltg.getId());
    testAuthzDelete(url);
  }
}
