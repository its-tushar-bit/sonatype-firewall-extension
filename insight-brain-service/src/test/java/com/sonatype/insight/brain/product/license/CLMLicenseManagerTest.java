/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.inject.Inject;

import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.sonatype.licensing.LicenseKey;
import org.sonatype.licensing.LicensingException;
import org.sonatype.licensing.feature.Feature;
import org.sonatype.licensing.feature.FeatureValidator;
import org.sonatype.licensing.internal.DefaultFeatureValidator;
import org.sonatype.licensing.product.ProductLicenseManager;

import com.google.inject.Binder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class CLMLicenseManagerTest
    extends AbstractComponentTest
{
  @Inject
  private CLMLicenseManager clmLicenseManager;

  private TestProductLicenseManager licenseManager = new TestProductLicenseManager(true);

  private static class NegativeFeatureValidator
      extends DefaultFeatureValidator
  {
    @Override
    public boolean isValid(Feature feature, LicenseKey licenseKey) {
      return false;
    }
  }

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    if ("testLicenseLacksClmFeature".equals(testName.getMethodName())) {
      binder.bind(FeatureValidator.class).toInstance(new NegativeFeatureValidator());
    }
    else {
      binder.bind(ProductLicenseManager.class).toInstance(licenseManager);
    }
  }

  private void installLicense() throws IOException, LicensingException {
    try (InputStream licenseStream = getClass().getResourceAsStream("/productlicense/license.lic")) {
      clmLicenseManager.installLicense(licenseStream);
    }
  }

  @Test
  public void testLicenseLacksClmFeature() throws Exception {
    try {
      installLicense();
      fail("Expected LicensingException");
    }
    catch (LicensingException expected) {
      assertEquals("License does not permit use of feature 'SonatypeCLM'", expected.getMessage());
    }

    assertNull(clmLicenseManager.getLicenseFingerprint());
  }

  @Test
  public void testLicenseExpiration() throws Exception {
    licenseManager.setExpirationDate(new Date(System.currentTimeMillis() + 2000));
    long before = System.currentTimeMillis();
    installLicense();

    assertEquals(true, clmLicenseManager.isValid());

    Thread.sleep(2100 - (System.currentTimeMillis() - before));

    assertEquals(false, clmLicenseManager.isValid());
  }
}
