/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.brain.utils.IdUtils;

import com.ning.http.client.Response;
import org.junit.Test;

public class LicenseThreatGroupResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(LicenseThreatGroupResource.SERVICE_PATH);
  }

  @Test
  public void testGetLicenseThreatGroups() throws Exception {
    grantReadPermission(app.getId());

    testAuthzGet(restRequest().parameter(IdUtils.TYPE_APPLICATION, app.getPublicId()));

    grantReadPermission(org.getId());

    testAuthzGet(restRequest().parameter(IdUtils.TYPE_ORGANIZATION, org.getId()));
  }

  @Test
  public void testGetApplicableLicenseThreatGroups() throws Exception {
    grantReadPermission(app.getId());

    HttpRequest request = restRequest().path("applicable");
    testAuthzGet(request.parameter(IdUtils.TYPE_APPLICATION, app.getPublicId()));

    grantReadPermission(org.getId());

    testAuthzGet(request.parameter(IdUtils.TYPE_ORGANIZATION, org.getId()));
  }

  @Test
  public void testAddLicenseThreatGroup() throws Exception {
    grantWritePermission(app.getId());

    HttpRequest request = restRequest().body(new LicenseThreatGroup(null, "Test LTG", 5));
    Response response = testAuthzPost(request.parameter(IdUtils.TYPE_APPLICATION, app.getPublicId()));
    new LicenseThreatGroupDAO().delete(fromJson(response, LicenseThreatGroup.class));

    grantWritePermission(org.getId());

    response = testAuthzPost(request.parameter(IdUtils.TYPE_ORGANIZATION, org.getId()));
    new LicenseThreatGroupDAO().delete(fromJson(response, LicenseThreatGroup.class));
  }

  @Test
  public void testUpdateLicenseThreatGroup() throws Exception {
    grantWritePermission(app.getId());

    HttpRequest request = restRequest().body(tempEntity.newLicenseThreatGroup(app.getId()));
    testAuthzPut(request.parameter(IdUtils.TYPE_APPLICATION, app.getPublicId()));

    grantWritePermission(org.getId());

    request.body(tempEntity.newLicenseThreatGroup(org.getId()));
    testAuthzPut(request.parameter(IdUtils.TYPE_ORGANIZATION, org.getId()));
  }

  @Test
  public void testDeleteLicenseThreatGroup() throws Exception {
    grantWritePermission(app.getId());

    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(app.getId());

    HttpRequest request = restRequest().path("{ltgId}");
    testAuthzDelete(request.parameter(IdUtils.TYPE_APPLICATION, app.getPublicId(), ltg.getId()));

    grantWritePermission(org.getId());
    ltg = tempEntity.newLicenseThreatGroup(org.getId());

    testAuthzDelete(request.parameter(IdUtils.TYPE_ORGANIZATION, org.getId(), ltg.getId()));
  }
}
