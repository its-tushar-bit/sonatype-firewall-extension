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
import com.sonatype.insight.brain.product.license.CLMLicenseManager.LicenseInfo;
import com.sonatype.insight.brain.product.license.CLMLicenseManager.LicenseSummary;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.license.model.CLMEnforcementPoint;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.sonatype.licensing.LicensingException;

import org.junit.Test;

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
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
    assertEquals(Integer.valueOf(100), clmLicenseManager.getApplicationCountLimit());
    assertEquals(true, clmLicenseManager.hasPolicyMonitoring());
    assertEquals(true, clmLicenseManager.hasDashboard());
    assertEquals(true, clmLicenseManager.hasQuality());

    // now change the value and make sure the cache is still stale
    licenseManager.setApplicationLimit(10);
    assertEquals(Integer.valueOf(100), clmLicenseManager.getApplicationCountLimit());
    licenseManager.setProducts("");
    assertEquals(true, clmLicenseManager.hasPolicyMonitoring());
    assertEquals(true, clmLicenseManager.hasDashboard());
    assertEquals(true, clmLicenseManager.hasQuality());
    licenseManager.setEnforcementPoints(CLMEnforcementPoint.StageRelease, CLMEnforcementPoint.Release);

    // now install the license (which causes the cache to be cleared) and make sure the cache is no longer stale
    installLicense();
    assertEquals(Integer.valueOf(10), clmLicenseManager.getApplicationCountLimit());
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
  public void testHasDashboard_NexusProPlusLicense() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    licenseManager.setEnforcementPoints(CLMEnforcementPoint.StageRelease, CLMEnforcementPoint.Release);
    installLicense();
    assertThat(clmLicenseManager.hasDashboard(), is(false));
  }

  @Test
  public void testHasDashboard_NexusAuditorLicense() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    installLicense();
    assertThat(clmLicenseManager.hasDashboard(), is(true));
  }

  @Test
  public void testHasQuality_NexusLifecycleLicense() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    installLicense();
    assertThat(clmLicenseManager.hasQuality(), is(true));
  }

  @Test
  public void testHasQuality_NoNexusLifecycle() throws Exception {
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
  public void testHasPolicyMonitoring_NexusProPlusLicense() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    licenseManager.setEnforcementPoints(CLMEnforcementPoint.StageRelease, CLMEnforcementPoint.Release);
    installLicense();
    assertThat(clmLicenseManager.hasPolicyMonitoring(), is(false));
  }

  @Test
  public void testHasPolicyMonitoring_NexusAuditorLicense() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    installLicense();
    assertThat(clmLicenseManager.hasPolicyMonitoring(), is(true));
  }

  @Test
  public void testHasRepositoryFirewall_NexusLifecycleLicense() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    installLicense();
    assertThat(clmLicenseManager.hasRepositoryFirewall(), is(false));
  }

  @Test
  public void testHasRepositoryFirewall_NexusProPlusLicense() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    installLicense();
    assertThat(clmLicenseManager.hasRepositoryFirewall(), is(false));
  }

  @Test
  public void testHasRepositoryFirewall_NexusAuditorLicense() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    installLicense();
    assertThat(clmLicenseManager.hasRepositoryFirewall(), is(false));
  }

  @Test
  public void testHasRepositoryFirewall_NexusFirewallLicense() throws Exception {
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
  public void testInstallLicense_BadMaxFirewallUsers() throws Exception {
    try {
      licenseManager.setProperty(ProductLicenseDetails.PROPERTY_MAX_FIREWALL_USERS, "Invalid");
      installLicense();
      fail("Expected LicensingException");
    }
    catch (LicensingException e) {
      assertThat(e.getMessage(), is("Invalid value for max firewall users: Invalid"));
    }
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
  public void testGetLicenseSummary_ProductEditionNexusProPlus() throws Exception {
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

  @Test
  public void testGetLicenseInfo_IncludesFingerprint() throws Exception {
    String fingerprint = "test-passed";
    licenseFingerprinter.setDummyLicenseFingerprint(fingerprint);
    installLicense();
    LicenseInfo summary = clmLicenseManager.getLicenseInfo();
    assertThat(summary, is(notNullValue()));
    assertThat(summary.fingerprint, is(fingerprint));
  }

  @Test
  public void testGetLicenseInfo_ProductEditionNone() throws Exception {
    clmLicenseManager.uninstallLicense();
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info, is(notNullValue()));
    assertThat(info.productEdition, is(""));
  }

  @Test
  public void testGetLicenseInfo_ProductEditionAuditor() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    installLicense();
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info, is(notNullValue()));
    assertThat(info.productEdition, is(CLMLicenseManager.PRODUCT_AUDITOR));
  }

  @Test
  public void testGetLicenseInfo_ProductEditionNexusProPlus() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    installLicense();
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info, is(notNullValue()));
    assertThat(info.productEdition, is(CLMLicenseManager.PRODUCT_PRO_PLUS));
  }

  @Test
  public void testGetLicenseInfo_ProductEditionLifecycle() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    installLicense();
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info, is(notNullValue()));
    assertThat(info.productEdition, is(CLMLicenseManager.PRODUCT_LIFECYCLE));
  }

  @Test
  public void testGetLicenseInfo_ProductEditionFirewall() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL);
    installLicense();
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info, is(notNullValue()));
    assertThat(info.productEdition, is(CLMLicenseManager.PRODUCT_FIREWALL));
  }

  @Test
  public void testGetLicenseInfo_LicensedUsersToDisplay() throws Exception {
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info.licensedUsersToDisplay, is(50));

    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.licensedUsersToDisplay, is(nullValue()));
  }

  @Test
  public void testGetLicenseInfo_ApplicationLimitToDisplay() throws Exception {
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info.applicationLimitToDisplay, is(nullValue()));

    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.applicationLimitToDisplay, is(100));
  }

  @Test
  public void testGetLicenseInfo_Products() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL, ProductLicenseDetails.PRODUCT_RISK,
          ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION, "foo", ProductLicenseDetails.PRODUCT_NEXUS);

    installLicense();
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info, is(notNullValue()));
    assertThat(info.products, is(arrayContaining("Nexus Firewall", "Nexus Auditor", "Nexus Lifecycle", "Nexus Pro+")));
  }
}
