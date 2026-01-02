/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import java.util.Collections;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Test;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class LicenseThreatGroupLicenseResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(LicenseThreatGroupLicenseResource.RESOURCE_PATH);
  }

  @Test
  public void testGetLicenseThreatGroupLicenses() throws Exception {
    grantReadPermission(app.getId());

    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(app.getId());

    testAuthzGet(restRequest().parameter(OwnerType.APPLICATION, app.getPublicId(), ltg.getId()));

    grantReadPermission(org.getId());

    testAuthzGet(restRequest().parameter(OwnerType.ORGANIZATION, org.getId(), ltg.getId()));
  }

  @Test
  public void testSetLicenseThreatGroupLicenses() throws Exception {
    grantWritePermission(app.getId());

    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(app.getId());

    HttpRequest request = restRequest().body(Collections.singletonList("MIT"));
    testAuthzPut(request.parameter(OwnerType.APPLICATION, app.getPublicId(), ltg.getId()));

    grantWritePermission(org.getId());

    testAuthzPut(request.parameter(OwnerType.ORGANIZATION, org.getId(), ltg.getId()));
  }
}
