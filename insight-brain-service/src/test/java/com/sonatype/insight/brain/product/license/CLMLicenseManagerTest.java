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
import com.sonatype.insight.license.model.CLMEnforcementPoint;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.sonatype.licensing.LicenseKey;
import org.sonatype.licensing.LicensingException;
import org.sonatype.licensing.feature.Feature;
import org.sonatype.licensing.feature.FeatureValidator;
import org.sonatype.licensing.internal.DefaultFeatureValidator;
import org.sonatype.licensing.product.ProductLicenseManager;

import com.google.inject.Binder;
import org.junit.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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

  @Test
  public void testLicenseCache() throws Exception {
    assertEquals(true, clmLicenseManager.isValid());
    assertEquals(100, clmLicenseManager.getApplicationCountLimit());
    assertEquals(true, clmLicenseManager.hasPolicyMonitoring());

    // now change the value and make sure the cache is still stale
    licenseManager.setApplicationLimit(10);
    assertEquals(100, clmLicenseManager.getApplicationCountLimit());
    licenseManager.setProducts(new String[0]);
    assertEquals(true, clmLicenseManager.hasPolicyMonitoring());
    licenseManager.setEnforcementPoints(CLMEnforcementPoint.StageRelease, CLMEnforcementPoint.Release);

    // now install the license (which causes the cache to be cleared) and make sure the cache is no longer stale
    installLicense();
    assertEquals(10, clmLicenseManager.getApplicationCountLimit());
    assertEquals(false, clmLicenseManager.hasPolicyMonitoring());
  }

  @Test
  public void testHasPolicyMonitoring_NexusClmLicense_Legacy() throws Exception {
    licenseManager.setVersion(0);
    licenseManager.setProducts();
    licenseManager.setEnforcementPoints(CLMEnforcementPoint.StageRelease, CLMEnforcementPoint.Release);
    installLicense();
    assertThat(clmLicenseManager.hasPolicyMonitoring(), is(false));
  }

  @Test
  public void testHasPolicyMonitoring_FullClmLicense_Legacy() throws Exception {
    licenseManager.setVersion(0);
    licenseManager.setProducts();
    installLicense();
    assertThat(clmLicenseManager.hasPolicyMonitoring(), is(true));
  }

  @Test
  public void testHasPolicyMonitoring_NexusClmLicense() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    licenseManager.setEnforcementPoints(CLMEnforcementPoint.StageRelease, CLMEnforcementPoint.Release);
    installLicense();
    assertThat(clmLicenseManager.hasPolicyMonitoring(), is(false));
  }

  @Test
  public void testHasPolicyMonitoring_FullClmLicense() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    installLicense();
    assertThat(clmLicenseManager.hasPolicyMonitoring(), is(true));
  }

  @Test(expected = LicensingException.class)
  public void testInstallLicense_BadVersion() throws Exception {
    licenseManager.setProperty(ProductLicenseDetails.PROPERTY_VERSION, "Invalid");
    installLicense();
  }

  @Test(expected = LicensingException.class)
  public void testInstallLicense_BadAppLimit() throws Exception {
    licenseManager.setProperty(ProductLicenseDetails.PROPERTY_APPLICATION_LIMIT, "Invalid");
    installLicense();
  }

  @Test
  public void testInstallLicense_UnknownEnforcementPointIsIgnored() throws Exception {
    licenseManager.setProperty(ProductLicenseDetails.PROPERTY_ENFORCEMENT_POINTS, "Invalid,Build");
    installLicense();
    assertThat(clmLicenseManager.getEnforcementPoints(), containsInAnyOrder(CLMEnforcementPoint.Build));
  }

  @Test
  public void testInstallLicense_DeprecatedEnforcementPointIsIgnored() throws Exception {
    licenseManager.setProperty(ProductLicenseDetails.PROPERTY_ENFORCEMENT_POINTS, "Build,Procure");
    installLicense();
    assertThat(clmLicenseManager.getEnforcementPoints(), containsInAnyOrder(CLMEnforcementPoint.Build));
  }

  @Test
  public void testNotifiyListener_InstallLicense() throws Exception {
    LicenseListener listener = mock(LicenseListener.class);
    clmLicenseManager.addListener(listener);
    installLicense();
    verify(listener).licenseChanged();

    clmLicenseManager.removeListener(listener);
    installLicense();
    verify(listener).licenseChanged();
  }

  @Test
  public void testNotifiyListener_UninstallLicense() throws Exception {
    installLicense();
    LicenseListener listener = mock(LicenseListener.class);
    clmLicenseManager.addListener(listener);
    clmLicenseManager.uninstallLicense();
    verify(listener).licenseChanged();

    clmLicenseManager.removeListener(listener);
    installLicense();
    clmLicenseManager.uninstallLicense();
    verify(listener).licenseChanged();
  }
}
