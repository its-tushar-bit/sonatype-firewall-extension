/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
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
import com.sonatype.insight.test.LogOutput;

import org.sonatype.licensing.LicensingException;

import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CLMLicenseManagerTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(CLMLicenseManager.class);
  
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
    assertThatThrownBy(() -> {
      installLicense();
    }).isInstanceOf(LicensingException.class)
        .hasMessage("License does not permit use of feature '" + CLMFeature.ID + "' or '" + FirewallFeature.ID + "'");

    assertThat(clmLicenseManager.getLicenseFingerprint()).isNull();
  }

  @Test
  public void testLicenseExpiration() throws Exception {
    licenseManager.setExpirationDate(new Date(System.currentTimeMillis() + 2000));
    long before = System.currentTimeMillis();
    installLicense();

    assertThat(clmLicenseManager.isValid()).isTrue();

    Thread.sleep(2100 - (System.currentTimeMillis() - before));

    assertThat(clmLicenseManager.isValid()).isFalse();
  }

  @Test
  public void testLicenseCache() throws Exception {
    assertThat(clmLicenseManager.isValid()).isTrue();
    assertThat(clmLicenseManager.getApplicationCountLimit()).isEqualTo(100);
    assertThat(clmLicenseManager.hasPolicyMonitoring()).isTrue();
    assertThat(clmLicenseManager.hasDashboard()).isTrue();
    assertThat(clmLicenseManager.hasQuality()).isTrue();

    // now change the value and make sure the cache is still stale
    licenseManager.setApplicationLimit(10);
    assertThat(clmLicenseManager.getApplicationCountLimit()).isEqualTo(100);
    licenseManager.setProducts("");
    assertThat(clmLicenseManager.hasPolicyMonitoring()).isTrue();
    assertThat(clmLicenseManager.hasDashboard()).isTrue();
    assertThat(clmLicenseManager.hasQuality()).isTrue();
    licenseManager.setEnforcementPoints(CLMEnforcementPoint.StageRelease, CLMEnforcementPoint.Release);

    // now install the license (which causes the cache to be cleared) and make sure the cache is no longer stale
    installLicense();
    assertThat(clmLicenseManager.getApplicationCountLimit()).isEqualTo(10);
    assertThat(clmLicenseManager.hasPolicyMonitoring()).isFalse();
  }

  @Test
  public void testHasDashboard_NexusClmLicense_Legacy() throws Exception {
    licenseManager.setVersion(0);
    licenseManager.setProducts();
    licenseManager.setEnforcementPoints(CLMEnforcementPoint.StageRelease, CLMEnforcementPoint.Release);
    installLicense();
    assertThat(clmLicenseManager.hasDashboard()).isFalse();
  }

  @Test
  public void testHasDashboard_FullClmLicense_Legacy() throws Exception {
    licenseManager.setVersion(0);
    licenseManager.setProducts();
    installLicense();
    assertThat(clmLicenseManager.hasDashboard()).isTrue();
  }

  @Test
  public void testHasDashboard_NexusProPlusLicense() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    licenseManager.setEnforcementPoints(CLMEnforcementPoint.StageRelease, CLMEnforcementPoint.Release);
    installLicense();
    assertThat(clmLicenseManager.hasDashboard()).isFalse();
  }

  @Test
  public void testHasDashboard_NexusAuditorLicense() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    installLicense();
    assertThat(clmLicenseManager.hasDashboard()).isTrue();
  }

  @Test
  public void testHasQuality_NexusLifecycleLicense() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    installLicense();
    assertThat(clmLicenseManager.hasQuality()).isTrue();
    assertThat(clmLicenseManager.hasPolicyMonitoring()).isTrue();
    assertThat(clmLicenseManager.hasEnforcement()).isTrue();
    assertThat(clmLicenseManager.hasNotifications()).isTrue();
    assertThat(clmLicenseManager.hasPolicyGrandfathering()).isTrue();
    assertThat(clmLicenseManager.hasWebhooks()).isTrue();
  }

  @Test
  public void testHasQuality_NoNexusLifecycle() throws Exception {
    Set<String> productSet = new HashSet<>(ProductLicenseDetails.PRODUCTS);
    productSet.remove(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    String[] products = productSet.toArray(new String[ProductLicenseDetails.PRODUCTS.size()]);
    licenseManager.setProducts(products);
    installLicense();
    assertThat(clmLicenseManager.hasQuality()).isFalse();
  }

  @Test
  public void testHasQuality_LegacyNoBuildStage() throws Exception {
    licenseManager.setVersion(0);
    licenseManager.setProducts();
    licenseManager.setEnforcementPoints(CLMEnforcementPoint.Release);
    installLicense();
    assertThat(clmLicenseManager.hasQuality()).isFalse();
  }

  @Test
  public void testHasQuality_LegacyWithBuildStage() throws Exception {
    licenseManager.setVersion(0);
    licenseManager.setProducts();
    licenseManager.setEnforcementPoints(CLMEnforcementPoint.Build);
    installLicense();
    assertThat(clmLicenseManager.hasQuality()).isTrue();
  }

  @Test
  public void testHasPolicyMonitoring_NexusClmLicense_Legacy() throws Exception {
    licenseManager.setVersion(0);
    licenseManager.setProducts();
    licenseManager.setEnforcementPoints(CLMEnforcementPoint.StageRelease, CLMEnforcementPoint.Release);
    installLicense();
    assertThat(clmLicenseManager.hasPolicyMonitoring()).isFalse();
  }

  @Test
  public void testHasPolicyMonitoring_FullClmLicense_Legacy() throws Exception {
    licenseManager.setVersion(0);
    licenseManager.setProducts();
    installLicense();
    assertThat(clmLicenseManager.hasPolicyMonitoring()).isTrue();
  }

  @Test
  public void testHasPolicyMonitoring_NexusProPlusLicense() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    licenseManager.setEnforcementPoints(CLMEnforcementPoint.StageRelease, CLMEnforcementPoint.Release);
    installLicense();
    assertThat(clmLicenseManager.hasPolicyMonitoring()).isFalse();
  }

  @Test
  public void testHasPolicyMonitoring_NexusAuditorLicense() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    installLicense();
    assertThat(clmLicenseManager.hasPolicyMonitoring()).isTrue();
  }

  @Test
  public void testHasRepositoryFirewall_NexusLifecycleLicense() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    installLicense();
    assertThat(clmLicenseManager.hasPolicyMonitoring()).isTrue();
    assertThat(clmLicenseManager.hasRepositoryFirewall()).isFalse();
    assertThat(clmLicenseManager.hasEnforcement()).isTrue();
    assertThat(clmLicenseManager.hasNotifications()).isTrue();
    assertThat(clmLicenseManager.hasPolicyGrandfathering()).isTrue();
    assertThat(clmLicenseManager.hasWebhooks()).isTrue();
  }

  @Test
  public void testHasRepositoryFirewall_NexusProPlusLicense() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    installLicense();
    assertThat(clmLicenseManager.hasRepositoryFirewall()).isFalse();
  }

  @Test
  public void testHasRepositoryFirewall_NexusAuditorLicense() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    installLicense();
    assertThat(clmLicenseManager.hasRepositoryFirewall()).isFalse();
  }

  @Test
  public void testHasRepositoryFirewall_NexusFirewallLicense() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL);
    installLicense();
    assertThat(clmLicenseManager.hasRepositoryFirewall()).isTrue();
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
    assertThatThrownBy(() -> {
      licenseManager.setProperty(ProductLicenseDetails.PROPERTY_MAX_FIREWALL_USERS, "Invalid");
      installLicense();
    }).isInstanceOf(LicensingException.class).hasMessage("Invalid value for max firewall users: Invalid");
  }

  @Test
  public void testInstallLicense_BadMaxUsers() throws Exception {
    assertThatThrownBy(() -> {
      licenseManager.setProperty(ProductLicenseDetails.PROPERTY_MAX_USERS, "Invalid");
      installLicense();
    }).isInstanceOf(LicensingException.class).hasMessage("Invalid value for max users: Invalid");
  }

  @Test
  public void testInstallLicense_UnknownEnforcementPointIsIgnored() throws Exception {
    licenseManager.setProperty(ProductLicenseDetails.PROPERTY_ENFORCEMENT_POINTS, "Invalid,Build");
    installLicense();
    assertThat(clmLicenseManager.getEnforcementPoints()).containsExactlyInAnyOrder(CLMEnforcementPoint.Build);
  }

  @Test
  public void testInstallLicense_DeprecatedEnforcementPointIsIgnored() throws Exception {
    licenseManager.setProperty(ProductLicenseDetails.PROPERTY_ENFORCEMENT_POINTS, "Build,Procure");
    installLicense();
    assertThat(clmLicenseManager.getEnforcementPoints()).containsExactlyInAnyOrder(CLMEnforcementPoint.Build);
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
    assertThat(summary).isNotNull();
    assertThat(summary.productEdition).isEqualTo("");
  }

  @Test
  public void testGetLicenseSummary_ProductEditionAuditor() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    installLicense();
    LicenseSummary summary = clmLicenseManager.getLicenseSummary();
    assertThat(summary).isNotNull();
    assertThat(summary.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_AUDITOR);
  }

  @Test
  public void testGetLicenseSummary_ProductEditionNexusProPlus() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    installLicense();
    LicenseSummary summary = clmLicenseManager.getLicenseSummary();
    assertThat(summary).isNotNull();
    assertThat(summary.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_PRO_PLUS);
  }

  @Test
  public void testGetLicenseSummary_ProductEditionLifecycle() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    installLicense();
    LicenseSummary summary = clmLicenseManager.getLicenseSummary();
    assertThat(summary).isNotNull();
    assertThat(summary.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_LIFECYCLE);
  }

  @Test
  public void testGetLicenseSummary_ProductEditionFirewall() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL);
    installLicense();
    LicenseSummary summary = clmLicenseManager.getLicenseSummary();
    assertThat(summary).isNotNull();
    assertThat(summary.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_FIREWALL);
  }

  @Test
  public void testGetLicenseInfo_IncludesFingerprint() throws Exception {
    String fingerprint = "test-passed";
    licenseFingerprinter.setDummyLicenseFingerprint(fingerprint);
    installLicense();
    LicenseInfo summary = clmLicenseManager.getLicenseInfo();
    assertThat(summary).isNotNull();
    assertThat(summary.fingerprint).isEqualTo(fingerprint);
  }

  @Test
  public void testGetLicenseInfo_ProductEditionNone() throws Exception {
    clmLicenseManager.uninstallLicense();
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info).isNotNull();
    assertThat(info.productEdition).isEqualTo("");
  }

  @Test
  public void testGetLicenseInfo_ProductEditionAuditor() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    installLicense();
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info).isNotNull();
    assertThat(info.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_AUDITOR);
  }

  @Test
  public void testGetLicenseInfo_ProductEditionNexusProPlus() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    installLicense();
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info).isNotNull();
    assertThat(info.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_PRO_PLUS);
  }

  @Test
  public void testGetLicenseInfo_ProductEditionLifecycle() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    installLicense();
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info).isNotNull();
    assertThat(info.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_LIFECYCLE);
  }

  @Test
  public void testGetLicenseInfo_ProductEditionFirewall() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL);
    installLicense();
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info).isNotNull();
    assertThat(info.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_FIREWALL);
  }

  @Test
  public void testGetLicenseInfo_LicensedUsersToDisplay() throws Exception {
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info.licensedUsersToDisplay).isEqualTo(50);

    // should be null when product is auditor
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.licensedUsersToDisplay).isNull();

    // should also be null when it is just Firewall
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.licensedUsersToDisplay).isNull();

    // should not be null when it is Pro+
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.licensedUsersToDisplay).isEqualTo(50);
  }

  @Test
  public void testGetLicenseInfo_FirewallUsersToDisplay() throws Exception {
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info.firewallUsersToDisplay).isEqualTo(45);

    // should be null when product is auditor
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.firewallUsersToDisplay).isNull();

    // should not be null when it is just Firewall
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.firewallUsersToDisplay).isEqualTo(45);

    // should be null when Lifecycle but with null maxFirewallUsers
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    licenseManager.setMaxFirewallUsers(null);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.firewallUsersToDisplay).isNull();
  }

  @Test
  public void testGetLicenseInfo_ApplicationLimitToDisplay() throws Exception {
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info.applicationLimitToDisplay).isNull();

    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.applicationLimitToDisplay).isEqualTo(100);

    // should also be null when it is just Firewall
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.applicationLimitToDisplay).isNull();
  }

  @Test
  public void testGetLicenseInfo_Products() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL, ProductLicenseDetails.PRODUCT_RISK,
          ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION, "foo", ProductLicenseDetails.PRODUCT_NEXUS);

    installLicense();
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info).isNotNull();
    assertThat(info.products).containsExactly("Nexus Firewall", "Nexus Auditor", "Nexus Lifecycle", "Nexus Pro+");
  }

  @Test
  public void testInstallLicenseIfUnlicensed_Null_DoesNothing() throws Exception {
    clmLicenseManager.uninstallLicense();

    clmLicenseManager.installLicenseIfUnlicensed(null);

    assertThat(clmLicenseManager.getLicenseFingerprint()).isNull();
  }

  @Test
  public void testInstallLicenseIfUnlicensed_LicenseAlreadyInstalled_Warn() throws Exception {
    installLicense();
    String licenseFilePath = "path/to/license/file";

    clmLicenseManager.installLicenseIfUnlicensed(licenseFilePath);

    assertThat(logOutput).atWarnLevel().contains(licenseFilePath);
  }

  @Test
  public void testInstallLicenseIfUnlicensed() throws Exception {
    clmLicenseManager.uninstallLicense();
    String licenseFilePath = getClass().getClassLoader().getResource("CLMLicenseManagerTest/license.lic").getFile();

    clmLicenseManager.installLicenseIfUnlicensed(licenseFilePath);

    assertThat(logOutput).atInfoLevel().contains(licenseFilePath);
    assertThat(clmLicenseManager.getLicenseFingerprint()).isNotNull();
  }

  @Test
  public void testInstallLicenseIfUnlicensed_FileNotFoundException() throws Exception {
    clmLicenseManager.uninstallLicense();
    String licenseFilePath = "path/to/license/file";
    assertThatThrownBy(() -> {
      clmLicenseManager.installLicenseIfUnlicensed(licenseFilePath);
    }).isInstanceOf(FileNotFoundException.class).hasMessageContaining(new File(licenseFilePath).getPath());
    assertThat(logOutput).atInfoLevel().contains(licenseFilePath);
    assertThat(clmLicenseManager.getLicenseFingerprint()).isNull();
  }

  @Test
  public void testInstallLicenseIfUnlicensed_LicensingException() throws Exception {
    licenseManager.setForceVerificationFailure(true);
    clmLicenseManager.uninstallLicense();
    String licenseFilePath = getClass().getClassLoader().getResource("CLMLicenseManagerTest/license.lic").getFile();
    assertThatThrownBy(() -> {
      clmLicenseManager.installLicenseIfUnlicensed(licenseFilePath);
    }).isInstanceOf(LicensingException.class);
    assertThat(logOutput).atInfoLevel().contains(licenseFilePath);
    assertThat(clmLicenseManager.getLicenseFingerprint()).isNull();
  }

  @Test
  public void testFoundationLicense() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    clmLicenseManager.installLicense(null);
    assertThat(clmLicenseManager.hasPolicyMonitoring()).isFalse();
    assertThat(clmLicenseManager.hasEnforcement()).isFalse();
    assertThat(clmLicenseManager.hasNotifications()).isFalse();
    assertThat(clmLicenseManager.hasPolicyGrandfathering()).isFalse();
    assertThat(clmLicenseManager.hasWebhooks()).isFalse();
  }

  @Test
  public void testFoundationLicense_WithFirewall() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FOUNDATION, ProductLicenseDetails.PRODUCT_FIREWALL);
    clmLicenseManager.installLicense(null);
    assertThat(clmLicenseManager.hasRepositoryFirewall()).isTrue();
    assertThat(clmLicenseManager.hasPolicyMonitoring()).isFalse();
    assertThat(clmLicenseManager.hasEnforcement()).isFalse();
    assertThat(clmLicenseManager.hasNotifications()).isFalse();
    assertThat(clmLicenseManager.hasPolicyGrandfathering()).isFalse();
    assertThat(clmLicenseManager.hasWebhooks()).isFalse();
  }
}
