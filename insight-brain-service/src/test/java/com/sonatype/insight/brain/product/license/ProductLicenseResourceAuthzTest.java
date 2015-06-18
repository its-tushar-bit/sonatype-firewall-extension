/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Test;

public class ProductLicenseResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testInstallLicense() throws Exception {
    grantConfigureSystemPermission();

    HttpRequest request = licenseRequest();
    HttpResponse response = request.auth(unauthorized.getUsername(), unauthorized.getPassword()).post();
    assertResponseStatus(403, response);

    response = request.auth(authorized.getUsername(), authorized.getPassword()).post();
    assertResponseStatus(200, response);
  }

  @Test
  public void testUninstallLicense() throws Exception {
    installLicense();

    grantConfigureSystemPermission();

    testAuthzDelete(restRequest().path(ProductLicenseResource.SERVICE_PATH));
  }
}
