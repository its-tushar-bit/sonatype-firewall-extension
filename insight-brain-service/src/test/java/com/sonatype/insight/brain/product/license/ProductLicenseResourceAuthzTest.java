/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Test;

public class ProductLicenseResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testInstallLicense() throws Exception {
    grantConfigureSystemPermission();

    HttpResponse response = uploadLicense(false, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = uploadLicense(false, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(200, response);
  }

  @Test
  public void testUninstallLicense() throws Exception {
    installLicense();

    grantConfigureSystemPermission();

    testAuthzDelete(restRequest().path(ProductLicenseResource.SERVICE_PATH));
  }
}
