/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import com.sonatype.insight.brain.TestLicenseFingerprinter;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.product.license.CLMLicenseManager.LicenseSummary;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.license.model.CLMEnforcementPoint;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.sonatype.licensing.LicensingException;

import org.junit.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
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

  @Inject
  private TestLicenseFingerprinter licenseFingerprinter;

  @Inject
  private TestProductLicenseManager licenseManager;

  private void installLicense() throws IOException, LicensingException {
    clmLicenseManager.installLicense(new ByteArrayInputStream(new byte[0]));
  }

  @Test
  public void testLicenseLacksClmFeatureAndFirewallFeature() throws Exception {
    clmLicenseManager.uninstallLicense();
    licenseManager.setForceVerificationFailure(true);
    try {
      installLicense();
      fail("Expected LicensingException");
    }
    catch (LicensingException e) {
      assertThat(e.getMessage(), is("License does not permit use of feature '" + CLMFeature.ID + "' or '"
          + FirewallFeature.ID + "'"));
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
    assertEquals(true, clmLicenseManager.hasDashboard());
    assertEquals(true, clmLicenseManager.hasQuality());

    // now change the value and make sure the cache is still stale
    licenseManager.setApplicationLimit(10);
    assertEquals(100, clmLicenseManager.getApplicationCountLimit());
    licenseManager.setProducts("");
    assertEquals(true, clmLicenseManager.hasPolicyMonitoring());
    assertEquals(true, clmLicenseManager.hasDashboard());
    assertEquals(true, clmLicenseManager.hasQuality());
    licenseManager.setEnforcementPoints(CLMEnforcementPoint.StageRelease, CLMEnforcementPoint.Release);

    // now install the license (which causes the cache to be cleared) and make sure the cache is no longer stale
    installLicense();
    assertEquals(10, clmLicenseManager.getApplicationCountLimit());
    assertEquals(false, clmLicenseManager.hasPolicyMonitoring());
  }

  @Test
  public void testHasDashboard_NexusClmLicense_Legacy() throws Exception {
    licenseManager.setVersion(0);
    licenseManager.setProducts();
    licenseManager.setEnforcementPoints(CLMEnforcementPoint.StageRelease, CLMEnforcementPoint.Release);
    installLicense();
    assertThat(clmLicenseManager.hasDashboard(), is(false));
  }

  @Test
  public void testHasDashboard_FullClmLicense_Legacy() throws Exception {
    licenseManager.setVersion(0);
    licenseManager.setProducts();
    installLicense();
    assertThat(clmLicenseManager.hasDashboard(), is(true));
  }

  @Test
  public void testHasDashboard_NexusClmLicense() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    licenseManager.setEnforcementPoints(CLMEnforcementPoint.StageRelease, CLMEnforcementPoint.Release);
    installLicense();
    assertThat(clmLicenseManager.hasDashboard(), is(false));
  }

  @Test
  public void testHasDashboard_FullClmLicense() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    installLicense();
    assertThat(clmLicenseManager.hasDashboard(), is(true));
  }

  @Test
  public void testHasQuality_RiskAndRemediation() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    installLicense();
    assertThat(clmLicenseManager.hasQuality(), is(true));
  }

  @Test
  public void testHasQuality_NoRiskAndRemediation() throws Exception {
    Set<String> productSet = new HashSet<>(ProductLicenseDetails.PRODUCTS);
    productSet.remove(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    String[] products = productSet.toArray(new String[ProductLicenseDetails.PRODUCTS.size()]);
    licenseManager.setProducts(products);
    installLicense();
    assertThat(clmLicenseManager.hasQuality(), is(false));
  }

  @Test
  public void testHasQuality_LegacyNoBuildStage() throws Exception {
    licenseManager.setVersion(0);
    licenseManager.setProducts();
    licenseManager.setEnforcementPoints(CLMEnforcementPoint.Release);
    installLicense();
    assertThat(clmLicenseManager.hasQuality(), is(false));
  }

  @Test
  public void testHasQuality_LegacyWithBuildStage() throws Exception {
    licenseManager.setVersion(0);
    licenseManager.setProducts();
    licenseManager.setEnforcementPoints(CLMEnforcementPoint.Build);
    installLicense();
    assertThat(clmLicenseManager.hasQuality(), is(true));
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

  @Test
  public void testHasRepositoryFirewall_NexusLifecycle() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    installLicense();
    assertThat(clmLicenseManager.hasRepositoryFirewall(), is(false));
  }

  @Test
  public void testHasRepositoryFirewall_NexusProPlus() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    installLicense();
    assertThat(clmLicenseManager.hasRepositoryFirewall(), is(false));
  }

  @Test
  public void testHasRepositoryFirewall_NexusAuditor() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    installLicense();
    assertThat(clmLicenseManager.hasRepositoryFirewall(), is(false));
  }

  @Test
  public void testHasRepositoryFirewall_Firewall() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL);
    installLicense();
    assertThat(clmLicenseManager.hasRepositoryFirewall(), is(true));
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
  public void testNotifyListener_InstallLicense() throws Exception {
    LicenseListener listener = mock(LicenseListener.class);
    clmLicenseManager.addListener(listener);
    installLicense();
    verify(listener).licenseChanged();

    clmLicenseManager.removeListener(listener);
    installLicense();
    verify(listener).licenseChanged();
  }

  @Test
  public void testNotifyListener_UninstallLicense() throws Exception {
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

  @Test
  public void testGetLicenseSummary_IncludesFingerprint() throws Exception {
    String fingerprint = "test-passed";
    licenseFingerprinter.setDummyLicenseFingerprint(fingerprint);
    installLicense();
    LicenseSummary summary = clmLicenseManager.getLicenseSummary();
    assertThat(summary, is(notNullValue()));
    assertThat(summary.fingerprint, is(fingerprint));
  }

  @Test
  public void testGetLicenseSummary_ProductEditionNone() throws Exception {
    clmLicenseManager.uninstallLicense();
    LicenseSummary summary = clmLicenseManager.getLicenseSummary();
    assertThat(summary, is(notNullValue()));
    assertThat(summary.productEdition, is(""));
  }

  @Test
  public void testGetLicenseSummary_ProductEditionAuditor() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    installLicense();
    LicenseSummary summary = clmLicenseManager.getLicenseSummary();
    assertThat(summary, is(notNullValue()));
    assertThat(summary.productEdition, is(CLMLicenseManager.PRODUCT_AUDITOR));
  }

  @Test
  public void testGetLicenseSummary_ProductEditionNexusPro() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    installLicense();
    LicenseSummary summary = clmLicenseManager.getLicenseSummary();
    assertThat(summary, is(notNullValue()));
    assertThat(summary.productEdition, is(CLMLicenseManager.PRODUCT_PRO_PLUS));
  }

  @Test
  public void testGetLicenseSummary_ProductEditionLifecycle() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    installLicense();
    LicenseSummary summary = clmLicenseManager.getLicenseSummary();
    assertThat(summary, is(notNullValue()));
    assertThat(summary.productEdition, is(CLMLicenseManager.PRODUCT_LIFECYCLE));
  }

  @Test
  public void testGetLicenseSummary_ProductEditionFirewall() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL);
    installLicense();
    LicenseSummary summary = clmLicenseManager.getLicenseSummary();
    assertThat(summary, is(notNullValue()));
    assertThat(summary.productEdition, is(CLMLicenseManager.PRODUCT_FIREWALL));
  }
}
