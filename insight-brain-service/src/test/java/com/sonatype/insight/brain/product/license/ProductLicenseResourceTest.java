/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ProductLicenseResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testInstallUninstallLicense() throws Exception {
    installLicense();
    uninstallLicense();
  }

  @Test
  public void testInstallFailedWithNormalBrowser() throws Exception {
    getLicenseManager().forceInstallFailure(true);

    Response response = installLicense(false);
    assertResponseStatus(400, response);

    assertEquals("The provided license file is invalid. Please verify you selected the correct file."
        + " If the problem persists, please contact our support team.", response.getResponseBody());
  }

  @Test
  public void testInstallFailedWithIE() throws Exception {
    getLicenseManager().forceInstallFailure(true);

    // IE is expecting a 200 response back, so we need to validate the error
    Response response = installLicense(true);
    assertResponseStatus(200, response);

    assertEquals("The provided license file is invalid. Please verify you selected the correct file."
        + " If the problem persists, please contact our support team.", response.getResponseBody());
  }
}
