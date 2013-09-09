/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.io.InputStream;
import java.util.Date;

import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.service.AbstractLicenseTest;
import com.sonatype.insight.brain.service.TestInsightBrainService;

import org.sonatype.licensing.LicenseKey;
import org.sonatype.licensing.LicensingException;
import org.sonatype.licensing.feature.Feature;
import org.sonatype.licensing.feature.FeatureValidator;
import org.sonatype.licensing.internal.DefaultFeatureValidator;
import org.sonatype.licensing.product.ProductLicenseManager;

import com.google.inject.AbstractModule;
import org.codehaus.plexus.util.IOUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class CLMLicenseManagerTest
    extends AbstractLicenseTest
{
  private static class NegativeFeatureValidator
      extends DefaultFeatureValidator
  {
    @Override
    public boolean isValid(Feature feature, LicenseKey licenseKey) {
      return false;
    }
  }

  @Override
  protected void configureBrain(TestInsightBrainService brain) {
    if ("testLicenseLacksClmFeature".equals(testName.getMethodName())) {
      brain.addModule(new AbstractModule()
      {
        @Override
        protected void configure() {
          bind(FeatureValidator.class).toInstance(new NegativeFeatureValidator());
        }
      });
    } else {
      super.configureBrain(brain);
    }
  }

  @Test
  public void testLicenseLacksClmFeature() throws Exception {
    CLMLicenseManager clmLicenseManager = brain.getInjector().getInstance(CLMLicenseManager.class);
    InputStream licenseStream = this.getClass().getResourceAsStream("/productlicense/license.lic");
    try {
      clmLicenseManager.installLicense(licenseStream);
      fail("Expected LicensingException");
    }
    catch (LicensingException expected) {
      assertEquals("License does not permit use of feature 'SonatypeCLM'", expected.getMessage());
    }
    finally {
      IOUtil.close(licenseStream);
    }

    assertNull(clmLicenseManager.getLicenseFingerprint());
  }

  @Test
  public void testLicenseCache() throws Exception {
    TestProductLicenseManager licenseManager = TestProductLicenseManager.class.cast(brain.getInjector().getInstance(
        ProductLicenseManager.class));

    CLMLicenseManager clmLicenseManager = brain.getInjector().getInstance(CLMLicenseManager.class);

    assertEquals(true, clmLicenseManager.isValid());
    assertEquals(100, clmLicenseManager.getApplicationCountLimit());

    // now change the value and make sure the cache is still stale
    licenseManager.setApplicationLimit(10);
    assertEquals(100, clmLicenseManager.getApplicationCountLimit());

    // now install the license (which causes the cache to be cleared) and make sure the cache is no longer stale
    installLicense();
    assertEquals(10, clmLicenseManager.getApplicationCountLimit());
  }

  @Test
  public void testLicenseExpiration() throws Exception {
    TestProductLicenseManager licenseManager = TestProductLicenseManager.class.cast(brain.getInjector().getInstance(
        ProductLicenseManager.class));

    CLMLicenseManager clmLicenseManager = brain.getInjector().getInstance(CLMLicenseManager.class);

    licenseManager.setExpirationDate(new Date(System.currentTimeMillis() + 500));
    installLicense();

    assertEquals(true, clmLicenseManager.isValid());

    Thread.sleep(600);

    assertEquals(false, clmLicenseManager.isValid());
  }
}
