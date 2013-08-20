/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.sun.jersey.api.client.UniformInterfaceException;
import org.junit.Assert;
import org.junit.Test;

public class ProductLicenseResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testInstallUninstallLicense() throws Exception {
    installLicense();
    uninstallLicense();
  }

  @Test
  public void testInstallFailedWithIE() throws Exception {
    getLicenseManager().forceInstallFailure(true);

    try {
      installLicense();
      Assert.fail("License installation should have failed");
    }
    catch (UniformInterfaceException e) {
      Assert.assertEquals(400, e.getResponse().getStatus());
    }

    // IE is expecting a 200 response back, so we need to validate the error
    String result = installLicenseAsIE();

    Assert.assertEquals("An error occurred", result);
  }
}
