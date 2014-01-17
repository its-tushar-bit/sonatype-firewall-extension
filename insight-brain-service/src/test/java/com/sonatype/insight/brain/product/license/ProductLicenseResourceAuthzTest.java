/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import com.ning.http.client.Response;
import org.junit.Test;

public class ProductLicenseResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testInstallLicense() throws Exception {
    grantAdminPermission();

    Response response = uploadLicense(null /* queryParams */, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = uploadLicense(null /* queryParams */, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(200, response);
  }

  @Test
  public void testUninstallLicense() throws Exception {
    installLicense();

    grantAdminPermission();

    String url = getRestUrl(ProductLicenseResource.SERVICE_PATH);
    testAuthzDelete(url);
  }
}
