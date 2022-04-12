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
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.sonatype.insight.brain.TestLicenseFingerprinter;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.DatabaseConfig;
import com.sonatype.insight.brain.service.HdsMockServerRule;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.license.model.SignedProductLicenseDetailsDTO;
import com.sonatype.insight.productlicense.ProductLicenseConfig;
import com.sonatype.insight.productlicense.ProductLicenseSigner;
import com.sonatype.insight.test.LogOutput;

import org.sonatype.licensing.LicensingException;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
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

  @Inject
  private InsightConfig config;

  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Mock
  private TaskScheduler taskSchedulerMock;

  @Before
  public void before() throws Exception {
    Files.copy(getClass().getResourceAsStream("/productlicense/licensing-keystore-hds.p12"),
        new File(tempDir.getRoot(), "hds.p12").toPath());
    hdsMockServer.reset();
  }

  @Override
  public void configure(Binder binder) {
    ProductLicenseConfig productLicenseConfig = new ProductLicenseConfig();
    productLicenseConfig.setKeyStorePath(new File(tempDir.getRoot(), "hds.p12").getAbsolutePath());
    productLicenseConfig.setKeyStoreAliasGroup("licensing-key-test");
    binder.bind(ProductLicenseConfig.class).toInstance(productLicenseConfig);
    binder.bind(TaskScheduler.class).toInstance(taskSchedulerMock);
    super.configure(binder);
  }

  @Override
  protected void customizeConfig(InsightConfig config) {
    config.setHdsUrl(hdsMockServer.getHttpUrl());
  }

  private void mockHdsProductLicenseDetails() {
    mockHdsProductLicenseDetails(null);
  }

  private Consumer<SignedProductLicenseDetailsDTO> withInvalidSignature() {
    return licenseDetails -> licenseDetails.signature = new byte[256];
  }

  private Consumer<SignedProductLicenseDetailsDTO> withMaxApplications(Integer maxApplications) {
    return licenseDetails -> licenseDetails.maxApplications = maxApplications;
  }

  private Consumer<SignedProductLicenseDetailsDTO> withStages(StageType... stages) {
    return licenseDetails -> Stream.of(stages).forEach(stage -> licenseDetails.stageIds.add(stage.getId()));
  }

  private Consumer<SignedProductLicenseDetailsDTO> withFeatures(LicensedFeature... features) {
    return licenseDetails -> Stream.of(features).forEach(feature -> licenseDetails.features.add(feature.name()));
  }

  private void mockHdsProductLicenseDetails(Consumer<SignedProductLicenseDetailsDTO> licenseDetailsCustomization) {
    SignedProductLicenseDetailsDTO licenseDetails = new SignedProductLicenseDetailsDTO();
    licenseDetails.version = 1;
    licenseDetails.features = new TreeSet<>();
    licenseDetails.stageIds = new TreeSet<>();
    licenseDetails.maxApplications = 100;
    if (licenseDetailsCustomization != null) {
      licenseDetailsCustomization.accept(licenseDetails);
    }
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
    assertThatExceptionOfType(LicensingException.class).isThrownBy(this::installLicense)
        .withMessage("License does not permit use of feature '" + CLMFeature.ID + "' or '" + FirewallFeature.ID + "'");

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
  public void testGetFeatures_StagePropertyFromLicenseIsIgnored_LifecycleCloud() throws Exception {
    licenseManager.setProperty(ProductLicenseDetails.PROPERTY_ENFORCEMENT_POINTS, "Invalid,Build,Procure");
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD);
    installLicense();
    assertThat(productLicense.getFeatures()).contains(LicensedFeature.CI_INTEGRATION,
        LicensedFeature.IDE_INTEGRATION, LicensedFeature.RM_STAGING_INTEGRATION);
  }

  @Test
  public void testGetFeatures_NexusProPlus() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    mockHdsProductLicenseDetails(withFeatures());
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
    mockHdsProductLicenseDetails(withFeatures());
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
    mockHdsProductLicenseDetails(withFeatures());
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
        LicensedFeature.WEBHOOKS_FOR_APPLICATIONS,
        LicensedFeature.AUTOMATION,
        LicensedFeature.ADVANCED_RECOMMENDATION_STRATEGIES,
        LicensedFeature.HYGIENE,
        LicensedFeature.RELEASE_INTEGRITY,
        LicensedFeature.BREAKING_CHANGE);
  }

  @Test
  public void testGetFeatures_LifecycleCloud() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD);
    mockHdsProductLicenseDetails(withFeatures());
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
        LicensedFeature.WEBHOOKS_FOR_APPLICATIONS,
        LicensedFeature.AUTOMATION);
  }

  @Test
  public void testGetFeatures_Firewall() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL);
    mockHdsProductLicenseDetails(withFeatures());
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
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).containsExactlyInAnyOrder( //
        LicensedFeature.FIREWALL_FOR_ARTIFACTORY, //
        LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES, //
        LicensedFeature.RM_STAGING_INTEGRATION);
  }

  @Test
  public void testGetFeatures_FirewallForArtifactory_V2() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL_FOR_ARTIFACTORY_V2);
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).containsExactlyInAnyOrder( //
        LicensedFeature.FIREWALL_AUTO_UNQUARANTINE,
        LicensedFeature.RELEASE_INTEGRITY,
        LicensedFeature.FIREWALL_FOR_ARTIFACTORY, //
        LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES, //
        LicensedFeature.RM_STAGING_INTEGRATION);
  }

  @Test
  public void testGetFeatures_FirewallV2() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL_V2);
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).containsExactlyInAnyOrder( //
        LicensedFeature.FIREWALL_AUTO_UNQUARANTINE, //
        LicensedFeature.RELEASE_INTEGRITY, //
        LicensedFeature.FIREWALL, //
        LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES, //
        LicensedFeature.RM_STAGING_INTEGRATION, //
        LicensedFeature.WEBHOOKS_FOR_REPOSITORIES);
  }

  @Test
  public void testGetFeatures_LifecycleFirewallCloud() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_CLOUD);
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).containsExactlyInAnyOrder( //
        LicensedFeature.FIREWALL_AUTO_UNQUARANTINE, //
        LicensedFeature.RELEASE_INTEGRITY, //
        LicensedFeature.FIREWALL, //
        LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES, //
        LicensedFeature.RM_STAGING_INTEGRATION, //
        LicensedFeature.WEBHOOKS_FOR_REPOSITORIES);
  }

  @Test
  public void testGetFeatures_Foundation() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    mockHdsProductLicenseDetails(withFeatures());
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
    mockHdsProductLicenseDetails(withStages());
    installLicense();

    assertThat(productLicense.getStageTypes()).containsExactlyInAnyOrder( //
        StageTypes.RELEASE, //
        StageTypes.PROXY);
  }

  @Test
  public void testGetStageTypes_NexusProPlus() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    mockHdsProductLicenseDetails(withStages());
    installLicense();

    assertThat(productLicense.getStageTypes()).containsExactlyInAnyOrder( //
        StageTypes.STAGE_RELEASE, //
        StageTypes.RELEASE, //
        StageTypes.PROXY);
  }

  @Test
  public void testGetStageTypes_Firewall() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL);
    mockHdsProductLicenseDetails(withStages());
    installLicense();

    assertThat(productLicense.getStageTypes()).containsExactlyInAnyOrder( //
        StageTypes.STAGE_RELEASE, //
        StageTypes.RELEASE, //
        StageTypes.PROXY);
  }

  @Test
  public void testGetStageTypes_FirewallForArtifactory() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL_FOR_ARTIFACTORY);
    mockHdsProductLicenseDetails(withStages());
    installLicense();

    assertThat(productLicense.getStageTypes()).containsExactlyInAnyOrder( //
        StageTypes.PROXY);
  }

  @Test
  public void testGetStageTypes_FirewallForArtifactory_V2() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL_FOR_ARTIFACTORY_V2);
    mockHdsProductLicenseDetails(withStages());
    installLicense();

    assertThat(productLicense.getStageTypes()).containsExactlyInAnyOrder( //
        StageTypes.PROXY);
  }

  @Test
  public void testGetStageTypes_FirewallV2() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL_V2);
    mockHdsProductLicenseDetails(withStages());
    installLicense();

    assertThat(productLicense.getStageTypes()).containsExactlyInAnyOrder( //
        StageTypes.STAGE_RELEASE, //
        StageTypes.RELEASE, //
        StageTypes.PROXY);
  }

  @Test
  public void testGetStageTypes_LifeCycleFirewallCloud() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_CLOUD);
    mockHdsProductLicenseDetails(withStages());
    installLicense();

    assertThat(productLicense.getStageTypes()).containsExactlyInAnyOrder( //
        StageTypes.STAGE_RELEASE, //
        StageTypes.RELEASE, //
        StageTypes.PROXY);
  }

  @Test
  public void testGetStageTypes_Lifecycle() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    mockHdsProductLicenseDetails(withStages());
    installLicense();

    assertThat(productLicense.getStageTypes()).containsExactlyInAnyOrder( //
        StageTypes.DEVELOP, //
        StageTypes.SOURCE, //
        StageTypes.BUILD, //
        StageTypes.STAGE_RELEASE, //
        StageTypes.RELEASE, //
        StageTypes.OPERATE, //
        StageTypes.PROXY);
  }

  @Test
  public void testGetStageTypes_LifecycleCloud() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD);
    mockHdsProductLicenseDetails(withStages());
    installLicense();

    assertThat(productLicense.getStageTypes()).containsExactlyInAnyOrder( //
        StageTypes.DEVELOP, //
        StageTypes.SOURCE, //
        StageTypes.BUILD, //
        StageTypes.STAGE_RELEASE, //
        StageTypes.RELEASE, //
        StageTypes.OPERATE, //
        StageTypes.PROXY);
  }

  @Test
  public void testGetStageTypes_Foundation() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    mockHdsProductLicenseDetails(withStages());
    installLicense();

    assertThat(productLicense.getStageTypes()).containsExactlyInAnyOrder( //
        StageTypes.DEVELOP, //
        StageTypes.SOURCE, //
        StageTypes.BUILD, //
        StageTypes.STAGE_RELEASE, //
        StageTypes.RELEASE, //
        StageTypes.OPERATE, //
        StageTypes.PROXY);
  }

  @Test
  public void testLoadLicense() {
    mockHdsProductLicenseDetails(withFeatures(LicensedFeature.CI_INTEGRATION, LicensedFeature.DASHBOARD)
        .andThen(withStages(StageTypes.BUILD, StageTypes.RELEASE).andThen(withMaxApplications(12345))));

    clmLicenseManager.loadLicense();

    assertThat(productLicense.isValid());
    SignedProductLicenseDetailsDTO licenseDetails = productLicenseDetailsCache.getProductLicenseDetails();
    assertThat(licenseDetails).isNotNull();
    assertThat(licenseDetails.features).containsExactly(LicensedFeature.CI_INTEGRATION.name(),
        LicensedFeature.DASHBOARD.name());
    assertThat(licenseDetails.stageIds).contains(StageTypes.BUILD.getId(), StageTypes.RELEASE.getId());
    assertThat(licenseDetails.maxApplications).isEqualTo(12345);
  }

  @Test
  public void testLoadLicense_InvalidSignatureFromHdsAndNoLocalCache() {
    mockHdsProductLicenseDetails(withInvalidSignature());
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
  public void testLoadLicense_ExternalDatabaseAllowedAndNotCurrentlyUsed() {
    mockHdsProductLicenseDetails(withFeatures(LicensedFeature.EXTERNAL_DATABASE));

    clmLicenseManager.loadLicense();

    assertThat(migrationTrackerDAO.isTrackerPresent(CLMLicenseManager.MIGRATION_TRACKER_EXTERNAL_DB)).isFalse();
  }

  @Test
  public void testLoadLicense_ExternalDatabaseAllowedAndCurrentlyUsed() {
    config.setDatabase(new DatabaseConfig());
    mockHdsProductLicenseDetails(withFeatures(LicensedFeature.EXTERNAL_DATABASE));

    clmLicenseManager.loadLicense();

    assertThat(migrationTrackerDAO.isTrackerPresent(CLMLicenseManager.MIGRATION_TRACKER_EXTERNAL_DB)).isTrue();
  }

  @Test
  public void testLoadLicense_ExternalDatabaseNotAllowedButCurrentlyUsed() {
    config.setDatabase(new DatabaseConfig());
    mockHdsProductLicenseDetails(withFeatures());

    assertThatExceptionOfType(LicensingException.class).isThrownBy(() -> {
      clmLicenseManager.loadLicense();
    }).withMessageContaining("license does not support use of an external database");

    assertThat(migrationTrackerDAO.isTrackerPresent(CLMLicenseManager.MIGRATION_TRACKER_EXTERNAL_DB)).isFalse();
  }

  @Test
  public void testLoadLicense_ExternalDatabaseCurrentlyUsedAndUnlicensed() {
    clmLicenseManager.uninstallLicense();
    config.setDatabase(new DatabaseConfig());

    clmLicenseManager.loadLicense();

    assertThat(migrationTrackerDAO.isTrackerPresent(CLMLicenseManager.MIGRATION_TRACKER_EXTERNAL_DB)).isFalse();
  }

  @Test
  public void testInstallLicense_LegacyVersion() throws Exception {
    licenseManager.setVersion(0);
    assertThatExceptionOfType(LicensingException.class).isThrownBy(this::installLicense)
        .withMessage("Invalid license version: 0");
  }

  @Test(expected = LicensingException.class)
  public void testInstallLicense_BadVersion() throws Exception {
    licenseManager.setProperty(ProductLicenseDetails.PROPERTY_VERSION, "Invalid");
    installLicense();
  }

  @Test
  public void testInstallLicense_BadMaxFirewallUsers() throws Exception {
    assertThatExceptionOfType(LicensingException.class).isThrownBy(() -> {
      licenseManager.setProperty(ProductLicenseDetails.PROPERTY_MAX_FIREWALL_USERS, "Invalid");
      installLicense();
    }).withMessage("Invalid value for max firewall users: Invalid");
  }

  @Test
  public void testInstallLicense_BadMaxUsers() throws Exception {
    assertThatExceptionOfType(LicensingException.class).isThrownBy(() -> {
      licenseManager.setProperty(ProductLicenseDetails.PROPERTY_MAX_USERS, "Invalid");
      installLicense();
    }).withMessage("Invalid value for max users: Invalid");
  }

  @Test
  public void testInstallLicense_LicenseDetailsFromHds() throws Exception {
    mockHdsProductLicenseDetails(withFeatures(LicensedFeature.CI_INTEGRATION, LicensedFeature.DASHBOARD)
        .andThen(withStages(StageTypes.BUILD, StageTypes.RELEASE).andThen(withMaxApplications(12345))));
    installLicense();
    SignedProductLicenseDetailsDTO licenseDetails = productLicenseDetailsCache.getProductLicenseDetails();
    assertThat(licenseDetails).isNotNull();
    assertThat(licenseDetails.features).containsExactly(LicensedFeature.CI_INTEGRATION.name(),
        LicensedFeature.DASHBOARD.name());
    assertThat(licenseDetails.stageIds).contains(StageTypes.BUILD.getId(), StageTypes.RELEASE.getId());
    assertThat(licenseDetails.maxApplications).isEqualTo(12345);
  }

  @Test
  public void testInstallLicense_LicenseDetailsFromHds_InvalidSignature() throws Exception {
    clmLicenseManager.uninstallLicense();
    mockHdsProductLicenseDetails(withInvalidSignature());
    assertThatExceptionOfType(LicensingException.class).isThrownBy(this::installLicense)
        .withMessageStartingWith("Could not verify signature of license details");
    assertThat(licenseManager.isValid()).isFalse();
    assertThat(productLicense.isValid()).isFalse();
  }

  @Test
  public void testInstallLicense_LicenseDetailsFromHds_RequestFailure() throws Exception {
    clmLicenseManager.uninstallLicense();
    hdsMockServer.respondWith("error").andStatus(503).atUri("/rest/productLicense/v1").withoutLicense();
    assertThatExceptionOfType(BadGatewayException.class).isThrownBy(this::installLicense)
        .withMessageContaining("Data Services are currently out of service");
    assertThat(licenseManager.isValid()).isFalse();
    assertThat(productLicense.isValid()).isFalse();
  }

  @Test
  public void testInstallLicense_MaxApplicationsFromHdsAndNotLicenseKey() throws Exception {
    licenseManager.setProperty(ProductLicenseDetails.PROPERTY_APPLICATION_LIMIT, "123");
    mockHdsProductLicenseDetails(withMaxApplications(12345));
    installLicense();
    assertThat(productLicense.getMaxApplications()).isEqualTo(12345);
  }

  @Test
  public void testInstallLicense_ExternalDatabaseFeatureFromHds() throws Exception {
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).doesNotContain(LicensedFeature.EXTERNAL_DATABASE);

    mockHdsProductLicenseDetails(withFeatures(LicensedFeature.EXTERNAL_DATABASE));
    installLicense();
    assertThat(productLicense.getFeatures()).contains(LicensedFeature.EXTERNAL_DATABASE);
  }

  @Test
  public void testInstallLicense_ExternalDatabaseNotAllowedButCurrentlyUsed() throws Exception {
    config.setDatabase(new DatabaseConfig());
    mockHdsProductLicenseDetails(withFeatures());
    clmLicenseManager.uninstallLicense();

    assertThatExceptionOfType(LicensingException.class).isThrownBy(this::installLicense)
        .withMessageContaining("license does not support use of an external database");
    assertThat(productLicense.isValid()).isFalse();
    assertThat(migrationTrackerDAO.isTrackerPresent(CLMLicenseManager.MIGRATION_TRACKER_EXTERNAL_DB)).isFalse();
  }

  @Test
  public void testInstallLicense_ExternalDatabaseNotAllowedButPreviouslyAllowed() throws Exception {
    config.setDatabase(new DatabaseConfig());
    migrationTrackerDAO.insert(new MigrationTracker(CLMLicenseManager.MIGRATION_TRACKER_EXTERNAL_DB));
    mockHdsProductLicenseDetails(withFeatures());

    installLicense();
    assertThat(productLicense.isValid()).isTrue();
    assertThat(migrationTrackerDAO.isTrackerPresent(CLMLicenseManager.MIGRATION_TRACKER_EXTERNAL_DB)).isTrue();
  }

  @Test
  public void testInstallLicense_ExternalDatabaseAllowedAndCurrentlyUsed() throws Exception {
    config.setDatabase(new DatabaseConfig());
    mockHdsProductLicenseDetails(withFeatures(LicensedFeature.EXTERNAL_DATABASE));

    installLicense();
    assertThat(productLicense.isValid()).isTrue();
    assertThat(migrationTrackerDAO.isTrackerPresent(CLMLicenseManager.MIGRATION_TRACKER_EXTERNAL_DB)).isTrue();
  }

  @Test
  public void testInstallLicense_ExternalDatabaseAllowedAndNotCurrentlyUsed() throws Exception {
    mockHdsProductLicenseDetails(withFeatures(LicensedFeature.EXTERNAL_DATABASE));

    installLicense();
    assertThat(productLicense.isValid()).isTrue();
    assertThat(migrationTrackerDAO.isTrackerPresent(CLMLicenseManager.MIGRATION_TRACKER_EXTERNAL_DB)).isFalse();
  }

  @Test
  public void testInstallLicense_HygieneFeatureFromHds() throws Exception {
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).contains(LicensedFeature.HYGIENE);

    mockHdsProductLicenseDetails(withFeatures(LicensedFeature.HYGIENE));
    installLicense();
    assertThat(productLicense.getFeatures()).contains(LicensedFeature.HYGIENE);
  }

  @Test
  public void testInstallLicense_AdvancedRecommendationStrategiesFeatureFromHds() throws Exception {
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).contains(LicensedFeature.ADVANCED_RECOMMENDATION_STRATEGIES);

    mockHdsProductLicenseDetails(withFeatures(LicensedFeature.ADVANCED_RECOMMENDATION_STRATEGIES));
    installLicense();
    assertThat(productLicense.getFeatures()).contains(LicensedFeature.ADVANCED_RECOMMENDATION_STRATEGIES);
  }

  @Test
  public void testInstallLicense_BreakingChangeFeatureFromHds() throws Exception {
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).contains(LicensedFeature.BREAKING_CHANGE);

    mockHdsProductLicenseDetails(withFeatures(LicensedFeature.BREAKING_CHANGE));
    installLicense();
    assertThat(productLicense.getFeatures()).contains(LicensedFeature.BREAKING_CHANGE);
  }

  @Test
  public void testInstallLicense_NodeClusteringFeatureFromHds() throws Exception {
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).doesNotContain(LicensedFeature.NODE_CLUSTERING);

    mockHdsProductLicenseDetails(withFeatures(LicensedFeature.NODE_CLUSTERING));
    installLicense();
    assertThat(productLicense.getFeatures()).contains(LicensedFeature.NODE_CLUSTERING);
  }

  @Test
  public void testInstallLicense_AdvancedLegalPackFromHds() throws Exception {
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).doesNotContain(LicensedFeature.ADVANCED_LEGAL_PACK);

    mockHdsProductLicenseDetails(withFeatures(LicensedFeature.ADVANCED_LEGAL_PACK));
    installLicense();
    assertThat(productLicense.getFeatures()).contains(LicensedFeature.ADVANCED_LEGAL_PACK);
  }

  @Test
  public void testInstallLicense_DataInsightsFeatureFromHds() throws Exception {
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).doesNotContain(LicensedFeature.DATA_INSIGHTS);

    mockHdsProductLicenseDetails(withFeatures(LicensedFeature.DATA_INSIGHTS));
    installLicense();
    assertThat(productLicense.getFeatures()).contains(LicensedFeature.DATA_INSIGHTS);
  }

  @Test
  public void testInstallLicense_SamlUserTokensFeatureFromHds() throws Exception {
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).doesNotContain(LicensedFeature.SAML_USER_TOKENS);

    mockHdsProductLicenseDetails(withFeatures(LicensedFeature.SAML_USER_TOKENS));
    installLicense();
    assertThat(productLicense.getFeatures()).contains(LicensedFeature.SAML_USER_TOKENS);
  }

  @Test
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
  public void testGetLicenseSummary_ProductEditionLifecycleCloud() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD);
    installLicense();
    LicenseSummary summary = clmLicenseManager.getLicenseSummary();
    assertThat(summary).isNotNull();
    assertThat(summary.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_LIFECYCLE_CLOUD);
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
  public void testGetLicenseSummary_ProductEditionFirewallV2() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL_V2);
    installLicense();
    LicenseSummary summary = clmLicenseManager.getLicenseSummary();
    assertThat(summary).isNotNull();
    assertThat(summary.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_FIREWALL);
  }

  @Test
  public void testGetLicenseSummary_ProductEditionLifecycleFirewallCloud() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_CLOUD);
    installLicense();
    LicenseSummary summary = clmLicenseManager.getLicenseSummary();
    assertThat(summary).isNotNull();
    assertThat(summary.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_LIFECYCLE_FIREWALL_CLOUD);
  }

  @Test
  public void testGetLicenseSummary_ProductEditionFirewallForArtifactoryV2() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL_FOR_ARTIFACTORY_V2);
    installLicense();
    LicenseSummary summary = clmLicenseManager.getLicenseSummary();
    assertThat(summary).isNotNull();
    assertThat(summary.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_FIREWALL_FOR_ARTIFACTORY);
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
  public void testGetLicenseInfo_ProductEditionLifecycleCloud() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD);
    installLicense();
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info).isNotNull();
    assertThat(info.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_LIFECYCLE_CLOUD);
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
  public void testGetLicenseInfo_ProductEditionFirewallV2() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL_V2);
    installLicense();
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info).isNotNull();
    assertThat(info.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_FIREWALL);
  }

  @Test
  public void testGetLicenseInfo_ProductEditionLifecycleFirewallCloud() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_CLOUD);
    installLicense();
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info).isNotNull();
    assertThat(info.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_LIFECYCLE_FIREWALL_CLOUD);
  }

  @Test
  public void testGetLicenseInfo_ProductEditionFirewallForArtifactoryV2() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL_FOR_ARTIFACTORY_V2);
    installLicense();
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info).isNotNull();
    assertThat(info.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_FIREWALL_FOR_ARTIFACTORY);
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
  public void testGetLicenseInfo_LicensedUsersToDisplay_LegacyLicensing() throws Exception {
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info.licensedUsersToDisplay).isEqualTo(50);

    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.licensedUsersToDisplay).isEqualTo(50);

    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD);
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

    // should also be null when it is just Firewall V2
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL_V2);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.licensedUsersToDisplay).isNull();

    // should also be null when it is just Firewall for Artifactory V2
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL_FOR_ARTIFACTORY_V2);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.licensedUsersToDisplay).isNull();

    // should also be null when it is just Lifecycle Firewall Cloud
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_CLOUD);
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

    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FOUNDATION, ProductLicenseDetails.PRODUCT_FIREWALL_V2);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.licensedUsersToDisplay).isEqualTo(50);

    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FOUNDATION,
        ProductLicenseDetails.PRODUCT_FIREWALL_FOR_ARTIFACTORY_V2);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.licensedUsersToDisplay).isEqualTo(50);

    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FOUNDATION,
        ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_CLOUD);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.licensedUsersToDisplay).isEqualTo(50);
  }

  @Test
  public void testGetLicenseInfo_FirewallUsersToDisplay_LegacyLicensing() throws Exception {
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

    // should not be null when it is just Firewall V2
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL_V2);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.firewallUsersToDisplay).isEqualTo(45);

    // should not be null when it is just Lifecycle Firewall Cloud
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_CLOUD);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.firewallUsersToDisplay).isEqualTo(45);

    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FOUNDATION, ProductLicenseDetails.PRODUCT_FIREWALL);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.firewallUsersToDisplay).isEqualTo(45);

    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FOUNDATION, ProductLicenseDetails.PRODUCT_FIREWALL_V2);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.firewallUsersToDisplay).isEqualTo(45);

    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FOUNDATION,
        ProductLicenseDetails.PRODUCT_FIREWALL_FOR_ARTIFACTORY_V2);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.firewallUsersToDisplay).isEqualTo(45);

    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FOUNDATION,
        ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_CLOUD);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.firewallUsersToDisplay).isEqualTo(45);

    // should be null when Lifecycle but with null maxFirewallUsers
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    licenseManager.setMaxFirewallUsers(null);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.firewallUsersToDisplay).isNull();

    // should be null when Lifecycle but with null maxFirewallUsers
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD);
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
  public void testGetLicenseInfo_ApplicationLimitToDisplay_LegacyLicensing() throws Exception {
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info.applicationLimitToDisplay).isNull();

    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.applicationLimitToDisplay).isEqualTo(100);
    assertThat(info.applicationCountToDisplay).isEqualTo(0);

    // should also be null when it is just Firewall
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.applicationLimitToDisplay).isNull();
    assertThat(info.applicationCountToDisplay).isNull();

    // should also be null when it is just Firewall V2
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL_V2);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.applicationLimitToDisplay).isNull();
    assertThat(info.applicationCountToDisplay).isNull();

    // should also be null when it is just Firewall for Artifactory V2
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL_FOR_ARTIFACTORY_V2);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.applicationLimitToDisplay).isNull();
    assertThat(info.applicationCountToDisplay).isNull();

    // should also be null when it is just Lifecycle Firewall Cloud
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_CLOUD);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.applicationLimitToDisplay).isNull();
    assertThat(info.applicationCountToDisplay).isNull();
  }

  @Test
  public void testGetLicenseInfo_LimitsToDisplay_AppBasedLicensing() throws Exception {
    licenseManager.setProperty(ProductLicenseDetails.PROPERTY_LICENSING_MODEL,
        ProductLicenseDetails.LICENSING_APP_BASED);
    licenseManager.setApplicationLimit(100);
    licenseManager.setMaxUsers(8765);
    licenseManager.setMaxFirewallUsers(4321);
    installLicense();

    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info.applicationLimitToDisplay).isEqualTo(100);
    assertThat(info.applicationCountToDisplay).isEqualTo(0);
    assertThat(info.licensedUsersToDisplay).isNull();
    assertThat(info.firewallUsersToDisplay).isNull();
  }

  @Test
  public void testGetLicenseInfo_LimitsToDisplay_UserBasedLicensing() throws Exception {
    licenseManager.setProperty(ProductLicenseDetails.PROPERTY_LICENSING_MODEL,
        ProductLicenseDetails.LICENSING_USER_BASED);
    licenseManager.setApplicationLimit(100);
    licenseManager.setMaxUsers(8765);
    licenseManager.setMaxFirewallUsers(null);
    installLicense();

    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info.applicationLimitToDisplay).isNull();
    assertThat(info.applicationCountToDisplay).isNull();
    assertThat(info.licensedUsersToDisplay).isEqualTo(8765);
    assertThat(info.firewallUsersToDisplay).isNull();

    licenseManager.setMaxFirewallUsers(4321);
    installLicense();

    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.applicationLimitToDisplay).isNull();
    assertThat(info.applicationCountToDisplay).isNull();
    assertThat(info.licensedUsersToDisplay).isEqualTo(8765);
    assertThat(info.firewallUsersToDisplay).isEqualTo(4321);
  }

  @Test
  public void testGetLicenseInfo_Products() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL, ProductLicenseDetails.PRODUCT_RISK,
        ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION, "foo", ProductLicenseDetails.PRODUCT_NEXUS,
        ProductLicenseDetails.PRODUCT_ADVANCED_LEGAL_PACK, ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD);

    installLicense();
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info).isNotNull();
    assertThat(info.products).containsExactlyInAnyOrder("Nexus Firewall", "Nexus Auditor", "Nexus Lifecycle",
        "Nexus Pro+", "Nexus Advanced Legal Pack", "Nexus Lifecycle Cloud");
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
    assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(() -> {
      clmLicenseManager.installLicenseIfUnlicensed(licenseFilePath);
    }).withMessageContaining(new File(licenseFilePath).getPath());
    assertThat(logOutput).atInfoLevel().contains(licenseFilePath);
    assertThat(productLicense.getFingerprint()).isNull();
  }

  @Test
  public void testInstallLicenseIfUnlicensed_LicensingException() throws Exception {
    licenseManager.setForceVerificationFailure(true);
    clmLicenseManager.uninstallLicense();
    String licenseFilePath = getClass().getClassLoader().getResource("CLMLicenseManagerTest/license.lic").getFile();
    assertThatExceptionOfType(LicensingException.class).isThrownBy(() -> {
      clmLicenseManager.installLicenseIfUnlicensed(licenseFilePath);
    });
    assertThat(logOutput).atInfoLevel().contains(licenseFilePath);
    assertThat(productLicense.getFingerprint()).isNull();
  }

  @Test
  public void testExecute() {
    CLMLicenseManager clmLicenseManagerSpy = spy(clmLicenseManager);
    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(clmLicenseManagerSpy).loadLicense();

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      clmLicenseManagerSpy.execute(mock(JobExecutionContext.class));
    }

    verify(clmLicenseManagerSpy).loadLicense();
  }

  @Test
  public void testLoadProductLicenseOnAllOtherClusterNodes() {
    CLMLicenseManager clmLicenseManagerSpy = spy(clmLicenseManager);

    clmLicenseManagerSpy.loadProductLicenseOnAllOtherClusterNodes();

    verify(taskSchedulerMock)
        .scheduleOneTimeTaskForAllOtherNodes(clmLicenseManagerSpy.getClass(), CLMLicenseManager.TASK_NAME);
  }

  @Test
  public void testInstallLicense_UpdatesOtherNodes() throws Exception {
    clmLicenseManager.uninstallLicense();
    CLMLicenseManager clmLicenseManagerSpy = spy(clmLicenseManager);

    clmLicenseManagerSpy.installLicense(new ByteArrayInputStream(new byte[1]));

    verify(clmLicenseManagerSpy).loadProductLicenseOnAllOtherClusterNodes();
  }

  @Test
  public void testInstallLicense_FailureDoesNotUpdateOtherNodes() {
    licenseManager.setForceVerificationFailure(true);
    clmLicenseManager.uninstallLicense();
    CLMLicenseManager clmLicenseManagerSpy = spy(clmLicenseManager);

    assertThatExceptionOfType(LicensingException.class)
        .isThrownBy(() -> clmLicenseManagerSpy.installLicense(new ByteArrayInputStream(new byte[1])));

    verify(clmLicenseManagerSpy, never()).loadProductLicenseOnAllOtherClusterNodes();
  }

  @Test
  public void testInstallLicenseIfUnlicensed_UpdatesOtherNodes() throws Exception {
    clmLicenseManager.uninstallLicense();
    CLMLicenseManager clmLicenseManagerSpy = spy(clmLicenseManager);
    String licenseFilePath = getClass().getClassLoader().getResource("CLMLicenseManagerTest/license.lic").getFile();

    clmLicenseManagerSpy.installLicenseIfUnlicensed(licenseFilePath);

    verify(clmLicenseManagerSpy).loadProductLicenseOnAllOtherClusterNodes();
  }

  @Test
  public void testInstallLicenseIfUnlicensed_FailureDoesNotUpdateOtherNodes() {
    licenseManager.setForceVerificationFailure(true);
    clmLicenseManager.uninstallLicense();
    CLMLicenseManager clmLicenseManagerSpy = spy(clmLicenseManager);
    String licenseFilePath = getClass().getClassLoader().getResource("CLMLicenseManagerTest/license.lic").getFile();

    assertThatExceptionOfType(LicensingException.class)
        .isThrownBy(() -> clmLicenseManagerSpy.installLicenseIfUnlicensed(licenseFilePath));

    verify(clmLicenseManagerSpy, never()).loadProductLicenseOnAllOtherClusterNodes();
  }

  @Test
  public void testUninstallLicense_UpdatesOtherNodes() {
    CLMLicenseManager clmLicenseManagerSpy = spy(clmLicenseManager);

    clmLicenseManagerSpy.uninstallLicense();

    verify(clmLicenseManagerSpy).loadProductLicenseOnAllOtherClusterNodes();
  }

  @Test
  public void testUninstallLicense_FailureDoesNotUpdateOtherNodes() {
    licenseManager.setForceUninstallFailure(true);
    CLMLicenseManager clmLicenseManagerSpy = spy(clmLicenseManager);

    assertThatExceptionOfType(RuntimeException.class).isThrownBy(clmLicenseManagerSpy::uninstallLicense);

    verify(clmLicenseManagerSpy, never()).loadProductLicenseOnAllOtherClusterNodes();
  }

  @Test
  public void testInstallLicense_InfrastructureAsCodePackFeatureFromHds() throws Exception {
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).doesNotContain(LicensedFeature.INFRASTRUCTURE_AS_CODE_PACK);

    mockHdsProductLicenseDetails(withFeatures(LicensedFeature.INFRASTRUCTURE_AS_CODE_PACK));
    installLicense();
    assertThat(productLicense.getFeatures()).contains(LicensedFeature.INFRASTRUCTURE_AS_CODE_PACK);
  }

  @Test
  public void testGetLicenseInfo_AdvancedLegalPackProduct() throws Exception {
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).doesNotContain(LicensedFeature.ADVANCED_LEGAL_PACK);

    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_ADVANCED_LEGAL_PACK);
    installLicense();
    assertThat(productLicense.getFeatures()).contains(LicensedFeature.ADVANCED_LEGAL_PACK);
  }
}
