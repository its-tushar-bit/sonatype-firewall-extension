/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.assertThat;

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
    catch (AssertionError expected) {
      assertThat(expected.getMessage(), endsWith("expected:<200> but was:<400>"));
    }

    // IE is expecting a 200 response back, so we need to validate the error
    String result = installLicenseAsIE();

    Assert.assertEquals("An error occurred", result);
  }
}
