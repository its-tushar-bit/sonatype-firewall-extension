package com.sonatype.insight.brain.product.license;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sonatype.licensing.product.ProductLicenseManager;

import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.service.AbstractLicenseTest;

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

    // now change the value and make sure the cache is still stale
    licenseManager.setApplicationLimit(10);
    assertEquals(100, clmLicenseManager.getApplicationCountLimit());

    // now install the license (which causes the cache to be cleared) and make sure the cache is no longer stale
    installLicense();
    assertEquals(10, clmLicenseManager.getApplicationCountLimit());
  }
}
