/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.service.AbstractLicenseTest;

import org.sonatype.licensing.product.ProductLicenseManager;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CLMLicenseCacheTest
    extends AbstractLicenseTest
{
  @Test
  public void testLicenseCache() throws Exception {
    TestProductLicenseManager licenseManager = TestProductLicenseManager.class.cast(brain.getInjector().getInstance(
        ProductLicenseManager.class));

    CLMLicenseManager clmLicenseManager = brain.getInjector().getInstance(CLMLicenseManager.class);

    assertEquals(true, clmLicenseManager.isValid());
    assertEquals(100, clmLicenseManager.getApplicationCountLimit());
    assertEquals(true, clmLicenseManager.hasPolicyMonitoring());

    // now change the value and make sure the cache is still stale
    licenseManager.setApplicationLimit(10);
    assertEquals(100, clmLicenseManager.getApplicationCountLimit());
    licenseManager.setFeatures(new String[0]);
    assertEquals(true, clmLicenseManager.hasPolicyMonitoring());

    // now install the license (which causes the cache to be cleared) and make sure the cache is no longer stale
    installLicense();
    assertEquals(10, clmLicenseManager.getApplicationCountLimit());
    assertEquals(false, clmLicenseManager.hasPolicyMonitoring());
  }
}
