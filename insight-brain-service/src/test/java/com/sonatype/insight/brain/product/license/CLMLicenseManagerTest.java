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
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.TreeSet;

import javax.inject.Inject;

import com.sonatype.insight.brain.TestLicenseFingerprinter;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.HdsMockServerRule;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.license.model.SignedProductLicenseDetailsDTO;
import com.sonatype.insight.productlicense.ProductLicenseSigner;
import com.sonatype.insight.test.LogOutput;

import org.sonatype.licensing.LicensingException;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CLMLicenseManagerTest
    extends AbstractComponentTest
{
  @ClassRule
  public static HdsMockServerRule hdsMockServer = new HdsMockServerRule();

  @Rule
  public LogOutput logOutput = new LogOutput(CLMLicenseManager.class);
  
  @Inject
  private CLMLicenseManager clmLicenseManager;

  @Inject
  private ProductLicense productLicense;

  @Inject
  private ProductLicenseDetailsCache productLicenseDetailsCache;

  @Inject
  private TestLicenseFingerprinter licenseFingerprinter;

  @Inject
  private TestProductLicenseManager licenseManager;

  @Inject
  private ProductLicenseSigner productLicenseSigner;

  @Before
  public void before() throws Exception {
    Files.copy(getClass().getResourceAsStream("/CLMLicenseManagerTest/licensing-keystore-hds.p12"),
        new File(tempDir.getRoot(), "hds.p12").toPath());
    hdsMockServer.reset();
  }

  @Override
  public void configure(Properties properties) {
    super.configure(properties);
    properties.setProperty("licensing.keystore.path", new File(tempDir.getRoot(), "hds.p12").getAbsolutePath());
    properties.setProperty("licensing.keystore.aliasgroup", "licensing-key-test");
  }

  @Override
  protected void customizeConfig(InsightConfig config) {
    config.setHdsUrl(hdsMockServer.getHttpUrl());
  }

  private void mockHdsProductLicenseDetails() {
    SignedProductLicenseDetailsDTO licenseDetails = new SignedProductLicenseDetailsDTO();
    licenseDetails.version = 1;
    licenseDetails.features = new TreeSet<>();
    licenseDetails.stageIds = new TreeSet<>();
    licenseDetails.maxApplications = 100;
    mockHdsProductLicenseDetails(licenseDetails);
  }

  private void mockHdsProductLicenseDetails(SignedProductLicenseDetailsDTO licenseDetails) {
    if (licenseDetails.signature == null) {
      productLicenseSigner.sign(licenseDetails, licenseFingerprinter.calculate());
    }
    hdsMockServer.respondWith(licenseDetails).atUri("/rest/productLicense/v1").withoutLicense();
  }

  private void installLicense() throws IOException, LicensingException {
    clmLicenseManager.installLicense(new ByteArrayInputStream(new byte[1]));
  }

  @Test
  public void testMissingLicense_BasicLicenseInformationCanStillBeQueried() throws Exception {
    clmLicenseManager.uninstallLicense();
    assertThat(productLicense.getFingerprint()).isNull();
    assertThat(productLicense.getFeatures()).isEmpty();
    assertThat(productLicense.getStageTypes()).isEmpty();
    assertThat(clmLicenseManager.getLicenseSummary()).isNotNull();
    assertThat(clmLicenseManager.getLicenseInfo()).isNotNull();
  }

  @Test
  public void testLicenseLacksClmFeatureAndFirewallFeature() throws Exception {
    clmLicenseManager.uninstallLicense();
    licenseManager.setForceVerificationFailure(true);
    assertThatThrownBy(() -> {
      installLicense();
    }).isInstanceOf(LicensingException.class)
        .hasMessage("License does not permit use of feature '" + CLMFeature.ID + "' or '" + FirewallFeature.ID + "'");

    assertThat(productLicense.getFingerprint()).isNull();
  }

  @Test
  public void testLicenseExpiration() throws Exception {
    licenseManager.setExpirationDate(new Date(System.currentTimeMillis() + 2000));
    long before = System.currentTimeMillis();
    installLicense();

    assertThat(productLicense.isValid()).isTrue();

    Thread.sleep(2100 - (System.currentTimeMillis() - before));

    assertThat(productLicense.isValid()).isFalse();
  }

  @Test
  public void testLicenseCache() throws Exception {
    assertThat(productLicense.isValid()).isTrue();
    assertThat(productLicense.hasFeature(LicensedFeature.POLICY_MONITORING)).isTrue();

    // now change the value and make sure the cache is still stale
    licenseManager.setProducts("");
    assertThat(productLicense.hasFeature(LicensedFeature.POLICY_MONITORING)).isTrue();

    // now install the license (which causes the cache to be cleared) and make sure the cache is no longer stale
    installLicense();
    assertThat(productLicense.hasFeature(LicensedFeature.POLICY_MONITORING)).isFalse();
  }

  @Test
  public void testGetFeatures_StagePropertyFromLicenseIsIgnored() throws Exception {
    licenseManager.setProperty(ProductLicenseDetails.PROPERTY_ENFORCEMENT_POINTS, "Invalid,Build,Procure");
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    installLicense();
    assertThat(productLicense.getFeatures()).contains(LicensedFeature.CI_INTEGRATION,
        LicensedFeature.IDE_INTEGRATION, LicensedFeature.RM_STAGING_INTEGRATION);
  }

  @Test
  public void testGetFeatures_NexusProPlus() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    installLicense();
    assertThat(productLicense.getFeatures()).containsExactlyInAnyOrder( //
        LicensedFeature.RM_STAGING_INTEGRATION, //
        LicensedFeature.ENFORCEMENT, //
        LicensedFeature.NOTIFICATIONS, //
        LicensedFeature.POLICY_GRANDFATHERING, //
        LicensedFeature.WEBHOOKS_FOR_APPLICATIONS);
  }

  @Test
  public void testGetFeatures_Auditor() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    installLicense();
    assertThat(productLicense.getFeatures()).containsExactlyInAnyOrder( //
        LicensedFeature.RM_STAGING_INTEGRATION, //
        LicensedFeature.DASHBOARD, //
        LicensedFeature.POLICY_MONITORING, //
        LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS, //
        LicensedFeature.CLI_INTEGRATION, //
        LicensedFeature.ENFORCEMENT, //
        LicensedFeature.NOTIFICATIONS, //
        LicensedFeature.POLICY_GRANDFATHERING, //
        LicensedFeature.WEBHOOKS_FOR_APPLICATIONS);
  }

  @Test
  public void testGetFeatures_Lifecycle() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    installLicense();
    assertThat(productLicense.getFeatures()).containsExactlyInAnyOrder( //
        LicensedFeature.IDE_INTEGRATION, //
        LicensedFeature.CI_INTEGRATION, //
        LicensedFeature.RM_STAGING_INTEGRATION, //
        LicensedFeature.DASHBOARD, //
        LicensedFeature.POLICY_MONITORING, //
        LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS, //
        LicensedFeature.CLI_INTEGRATION, //
        LicensedFeature.QUALITY, //
        LicensedFeature.ENFORCEMENT, //
        LicensedFeature.NOTIFICATIONS, //
        LicensedFeature.POLICY_GRANDFATHERING, //
        LicensedFeature.WEBHOOKS_FOR_APPLICATIONS);
  }

  @Test
  public void testGetFeatures_Firewall() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL);
    installLicense();
    assertThat(productLicense.getFeatures()).containsExactlyInAnyOrder( //
        LicensedFeature.FIREWALL, //
        LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES, //
        LicensedFeature.RM_STAGING_INTEGRATION, //
        LicensedFeature.WEBHOOKS_FOR_REPOSITORIES);
  }

  @Test
  public void testGetFeatures_FirewallForArtifactory() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL_FOR_ARTIFACTORY);
    installLicense();
    assertThat(productLicense.getFeatures()).containsExactlyInAnyOrder( //
        LicensedFeature.FIREWALL_FOR_ARTIFACTORY, //
        LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES, //
        LicensedFeature.RM_STAGING_INTEGRATION);
  }

  @Test
  public void testGetFeatures_Foundation() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    installLicense();
    assertThat(productLicense.getFeatures()).containsExactlyInAnyOrder( //
        LicensedFeature.DASHBOARD, //
        LicensedFeature.CI_INTEGRATION, //
        LicensedFeature.CLI_INTEGRATION, //
        LicensedFeature.RM_STAGING_INTEGRATION, //
        LicensedFeature.QUALITY);
  }

  @Test
  public void testGetStageTypes_Auditor() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    installLicense();

    assertThat(productLicense.getStageTypes()).containsExactlyInAnyOrder( //
        StageTypes.RELEASE, //
        StageTypes.PROXY);
  }

  @Test
  public void testGetStageTypes_NexusProPlus() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    installLicense();

    assertThat(productLicense.getStageTypes()).containsExactlyInAnyOrder( //
        StageTypes.STAGE_RELEASE, //
        StageTypes.RELEASE, //
        StageTypes.PROXY);
  }

  @Test
  public void testGetStageTypes_Firewall() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL);
    installLicense();

    assertThat(productLicense.getStageTypes()).containsExactlyInAnyOrder( //
        StageTypes.STAGE_RELEASE, //
        StageTypes.RELEASE, //
        StageTypes.PROXY);
  }

  @Test
  public void testGetStageTypes_FirewallForArtifactory() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL_FOR_ARTIFACTORY);
    installLicense();

    assertThat(productLicense.getStageTypes()).containsExactlyInAnyOrder( //
        StageTypes.PROXY);
  }

  @Test
  public void testGetStageTypes_Lifecycle() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    installLicense();

    assertThat(productLicense.getStageTypes()).containsExactlyInAnyOrder( //
        StageTypes.DEVELOP, //
        StageTypes.BUILD, //
        StageTypes.STAGE_RELEASE, //
        StageTypes.RELEASE, //
        StageTypes.OPERATE, //
        StageTypes.PROXY);
  }

  @Test
  public void testGetStageTypes_Foundation() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    installLicense();

    assertThat(productLicense.getStageTypes()).containsExactlyInAnyOrder( //
        StageTypes.DEVELOP, //
        StageTypes.BUILD, //
        StageTypes.STAGE_RELEASE, //
        StageTypes.RELEASE, //
        StageTypes.OPERATE, //
        StageTypes.PROXY);
  }

  @Test
  public void testLoadLicense() {
    SignedProductLicenseDetailsDTO licenseDetails = new SignedProductLicenseDetailsDTO();
    licenseDetails.features = new TreeSet<>(Arrays.asList("featureA", "featureB"));
    licenseDetails.stageIds = new TreeSet<>(Arrays.asList("stageA", "stageB"));
    licenseDetails.maxApplications = 12345;
    mockHdsProductLicenseDetails(licenseDetails);

    clmLicenseManager.loadLicense();

    assertThat(productLicense.isValid());
    licenseDetails = productLicenseDetailsCache.getProductLicenseDetails();
    assertThat(licenseDetails).isNotNull();
    assertThat(licenseDetails.features).containsExactly("featureA", "featureB");
    assertThat(licenseDetails.stageIds).contains("stageA", "stageB");
    assertThat(licenseDetails.maxApplications).isEqualTo(12345);
  }

  @Test
  public void testLoadLicense_InvalidSignatureFromHdsAndNoLocalCache() {
    SignedProductLicenseDetailsDTO licenseDetails = new SignedProductLicenseDetailsDTO();
    licenseDetails.features = new TreeSet<>();
    licenseDetails.stageIds = new TreeSet<>();
    licenseDetails.signature = new byte[256];
    mockHdsProductLicenseDetails(licenseDetails);
    productLicenseDetailsCache.saveJson(null);

    clmLicenseManager.loadLicense();

    assertThat(productLicense.isValid()).isFalse();
    assertThat(productLicenseDetailsCache.getProductLicenseDetails()).isNull();
  }

  @Test
  public void testLoadLicense_HdsRequestFailureAndNoLocalCache() {
    hdsMockServer.respondWith("error").andStatus(503).atUri("/rest/productLicense/v1").withoutLicense();
    productLicenseDetailsCache.saveJson(null);

    clmLicenseManager.loadLicense();

    assertThat(productLicense.isValid()).isFalse();
    assertThat(productLicenseDetailsCache.getProductLicenseDetails()).isNull();
  }

  @Test
  public void testLoadLicense_HdsRequestFailureAndValidLocalCache() {
    hdsMockServer.respondWith("error").andStatus(503).atUri("/rest/productLicense/v1").withoutLicense();
    SignedProductLicenseDetailsDTO licenseDetails = new SignedProductLicenseDetailsDTO();
    licenseDetails.features = new TreeSet<>(Arrays.asList("featureA", "featureB"));
    licenseDetails.stageIds = new TreeSet<>(Arrays.asList("stageA", "stageB"));
    licenseDetails.maxApplications = 12345;
    productLicenseSigner.sign(licenseDetails, licenseFingerprinter.calculate());
    productLicenseDetailsCache.setProductLicenseDetails(licenseDetails);

    clmLicenseManager.loadLicense();

    assertThat(productLicense.isValid()).isTrue();
    licenseDetails = productLicenseDetailsCache.getProductLicenseDetails();
    assertThat(licenseDetails).isNotNull();
    assertThat(licenseDetails.features).containsExactly("featureA", "featureB");
    assertThat(licenseDetails.stageIds).contains("stageA", "stageB");
    assertThat(licenseDetails.maxApplications).isEqualTo(12345);
  }

  @Test
  public void testLoadLicense_HdsRequestFailureAndInvalidLocalSignature() {
    hdsMockServer.respondWith("error").andStatus(503).atUri("/rest/productLicense/v1").withoutLicense();
    SignedProductLicenseDetailsDTO licenseDetails = new SignedProductLicenseDetailsDTO();
    licenseDetails.features = new TreeSet<>();
    licenseDetails.stageIds = new TreeSet<>();
    licenseDetails.signature = new byte[256];
    productLicenseDetailsCache.setProductLicenseDetails(licenseDetails);

    clmLicenseManager.loadLicense();

    assertThat(productLicense.isValid()).isFalse();
  }

  @Test
  public void testInstallLicense_LegacyVersion() throws Exception {
    licenseManager.setVersion(0);
    assertThatThrownBy(() -> {
      installLicense();
    }).isInstanceOf(LicensingException.class).hasMessage("Invalid license version: 0");
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
  public void testInstallLicense_LicenseDetailsFromHds() throws Exception {
    SignedProductLicenseDetailsDTO licenseDetails = new SignedProductLicenseDetailsDTO();
    licenseDetails.features = new TreeSet<>(Arrays.asList("featureA", "featureB"));
    licenseDetails.stageIds = new TreeSet<>(Arrays.asList("stageA", "stageB"));
    licenseDetails.maxApplications = 12345;
    mockHdsProductLicenseDetails(licenseDetails);
    installLicense();
    licenseDetails = productLicenseDetailsCache.getProductLicenseDetails();
    assertThat(licenseDetails).isNotNull();
    assertThat(licenseDetails.features).containsExactly("featureA", "featureB");
    assertThat(licenseDetails.stageIds).contains("stageA", "stageB");
    assertThat(licenseDetails.maxApplications).isEqualTo(12345);
  }

  @Test
  public void testInstallLicense_LicenseDetailsFromHds_InvalidSignature() throws Exception {
    clmLicenseManager.uninstallLicense();
    SignedProductLicenseDetailsDTO licenseDetails = new SignedProductLicenseDetailsDTO();
    licenseDetails.features = new TreeSet<>();
    licenseDetails.stageIds = new TreeSet<>();
    licenseDetails.signature = new byte[256];
    mockHdsProductLicenseDetails(licenseDetails);
    assertThatExceptionOfType(LicensingException.class).isThrownBy(() -> {
      installLicense();
    }).withMessage("Could not verify signature of license details");
    assertThat(licenseManager.isValid()).isFalse();
    assertThat(productLicense.isValid()).isFalse();
  }

  @Test
  public void testInstallLicense_LicenseDetailsFromHds_RequestFailure() throws Exception {
    clmLicenseManager.uninstallLicense();
    hdsMockServer.respondWith("error").andStatus(503).atUri("/rest/productLicense/v1").withoutLicense();
    assertThatExceptionOfType(BadGatewayException.class).isThrownBy(() -> {
      installLicense();
    }).withMessageContaining("Data Services are currently out of service");
    assertThat(licenseManager.isValid()).isFalse();
    assertThat(productLicense.isValid()).isFalse();
  }

  public void testNotifyListener_LoadLicense() throws Exception {
    ProductLicenseListener listener = mock(ProductLicenseListener.class);
    clmLicenseManager.addListener(listener);
    clmLicenseManager.loadLicense();
    verify(listener).productLicenseChanged();

    clmLicenseManager.removeListener(listener);
    clmLicenseManager.loadLicense();
    verify(listener).productLicenseChanged();
  }

  @Test
  public void testNotifyListener_InstallLicense() throws Exception {
    ProductLicenseListener listener = mock(ProductLicenseListener.class);
    clmLicenseManager.addListener(listener);
    installLicense();
    verify(listener).productLicenseChanged();

    clmLicenseManager.removeListener(listener);
    installLicense();
    verify(listener).productLicenseChanged();
  }

  @Test
  public void testNotifyListener_UninstallLicense() throws Exception {
    installLicense();
    ProductLicenseListener listener = mock(ProductLicenseListener.class);
    clmLicenseManager.addListener(listener);
    clmLicenseManager.uninstallLicense();
    verify(listener).productLicenseChanged();

    clmLicenseManager.removeListener(listener);
    installLicense();
    clmLicenseManager.uninstallLicense();
    verify(listener).productLicenseChanged();
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
  public void testGetLicenseSummary_ProductEditionLifecycleFoundation() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    installLicense();
    LicenseSummary summary = clmLicenseManager.getLicenseSummary();
    assertThat(summary).isNotNull();
    assertThat(summary.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_LIFECYCLE_FOUNDATION);
  }

  @Test
  public void testGetLicenseInfo_IncludesFingerprint() throws Exception {
    String fingerprint = "test-passed";
    licenseFingerprinter.setDummyLicenseFingerprint(fingerprint);
    mockHdsProductLicenseDetails();
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
  public void testGetLicenseInfo_ProductEditionLifecycleFoundation() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    installLicense();
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info).isNotNull();
    assertThat(info.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_LIFECYCLE_FOUNDATION);
  }

  @Test
  public void testGetLicenseInfo_LicensedUsersToDisplay() throws Exception {
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info.licensedUsersToDisplay).isEqualTo(50);

    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
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

    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.licensedUsersToDisplay).isEqualTo(50);

    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FOUNDATION, ProductLicenseDetails.PRODUCT_FIREWALL);
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

    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FOUNDATION, ProductLicenseDetails.PRODUCT_FIREWALL);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.firewallUsersToDisplay).isEqualTo(45);

    // should be null when Lifecycle but with null maxFirewallUsers
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    licenseManager.setMaxFirewallUsers(null);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.firewallUsersToDisplay).isNull();

    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
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

    assertThat(productLicense.getFingerprint()).isNull();
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
    assertThat(productLicense.getFingerprint()).isNotNull();
  }

  @Test
  public void testInstallLicenseIfUnlicensed_FileNotFoundException() throws Exception {
    clmLicenseManager.uninstallLicense();
    String licenseFilePath = "path/to/license/file";
    assertThatThrownBy(() -> {
      clmLicenseManager.installLicenseIfUnlicensed(licenseFilePath);
    }).isInstanceOf(FileNotFoundException.class).hasMessageContaining(new File(licenseFilePath).getPath());
    assertThat(logOutput).atInfoLevel().contains(licenseFilePath);
    assertThat(productLicense.getFingerprint()).isNull();
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
    assertThat(productLicense.getFingerprint()).isNull();
  }
}
