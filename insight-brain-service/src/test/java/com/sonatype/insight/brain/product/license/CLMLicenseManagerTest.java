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
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Stream;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.TestLicenseFingerprinter;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.DatabaseConfig;
import com.sonatype.insight.brain.service.HdsMockServerRule;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.license.model.SignedProductLicenseDetailsDTO;
import com.sonatype.insight.test.productlicense.ProductLicenseConfig;
import com.sonatype.insight.test.productlicense.ProductLicenseSigner;
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

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsNewTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
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
  private CreditAwareProductLicense creditAwareProductLicense;

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
  protected TaskScheduler taskSchedulerMock;

  @Before
  public void before() throws Exception {
    try (InputStream in = getClass().getResourceAsStream("/productlicense/licensing-keystore-hds.p12")) {
      assert in != null;
      Files.copy(in, new File(tempDir.getRoot(), "hds.p12").toPath());
    }
    hdsMockServer.reset();
    setHdsUrl(hdsMockServer.getHttpUrl());
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

  private void mockHdsProductLicenseDetails() {
    mockHdsProductLicenseDetails(null);
  }

  private Consumer<SignedProductLicenseDetailsDTO> withInvalidSignature() {
    return licenseDetails -> licenseDetails.signature = new byte[256];
  }

  private Consumer<SignedProductLicenseDetailsDTO> withMaxApplications(Integer maxApplications) {
    return licenseDetails -> licenseDetails.maxApplications = maxApplications;
  }

  private Consumer<SignedProductLicenseDetailsDTO> withMaxSboms(Integer maxSboms) {
    return licenseDetails -> licenseDetails.maxSboms = maxSboms;
  }

  private Consumer<SignedProductLicenseDetailsDTO> withCreditAmount(BigDecimal creditAmount) {
    return licenseDetails -> licenseDetails.creditAmount = creditAmount;
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
  public void testMissingLicense_BasicLicenseInformationCanStillBeQueried() {
    clmLicenseManager.uninstallLicense();
    assertThat(productLicense.getFingerprint()).isNull();
    assertThat(productLicense.getFeatures()).isEmpty();
    assertThat(productLicense.getStageTypes()).isEmpty();
    assertThat(clmLicenseManager.getLicenseSummary()).isNotNull();
    assertThat(clmLicenseManager.getLicenseInfo()).isNotNull();
  }

  @Test
  public void testLicenseLacksClmFeatureAndFirewallFeature() {
    clmLicenseManager.uninstallLicense();
    licenseManager.setForceVerificationFailure(true);
    assertThatExceptionOfType(LicensingException.class).isThrownBy(this::installLicense)
        .withMessage("License does not permit use of feature '" + CLMFeature.ID + "', '"
            + FirewallFeature.ID + "', or '" + GuideFeature.ID + "'");

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
        LicensedFeature.DASHBOARD, //
        LicensedFeature.CLI_INTEGRATION, //
        LicensedFeature.DATA_RETENTION, //
        LicensedFeature.ENFORCEMENT, //
        LicensedFeature.INNER_SOURCE_REPOSITORIES, //
        LicensedFeature.NOTIFICATIONS, //
        LicensedFeature.ORGS_AND_APPS, //
        LicensedFeature.POLICY_MONITORING, //
        LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS, //
        LicensedFeature.POLICY_GRANDFATHERING, //
        LicensedFeature.PROPRIETARY_COMPONENTS, //
        LicensedFeature.RM_STAGING_INTEGRATION, //
        LicensedFeature.WAIVERS_DASHBOARD, //
        LicensedFeature.WEBHOOKS_FOR_APPLICATIONS,
        LicensedFeature.APPLICATION_REPORTS,
        LicensedFeature.APPLICATION_EVALUATION,
        LicensedFeature.CALL_FLOW_ANALYSIS,
        LicensedFeature.COMPONENT_EVALUATION,
        LicensedFeature.COMPONENT_LABELS,
        LicensedFeature.COMPONENT_SEARCH,
        LicensedFeature.POLICY_MANAGEMENT,
        LicensedFeature.POLICY_READ_ONLY,
        LicensedFeature.POLICY_VIOLATIONS,
        LicensedFeature.POLICY_WAIVERS,
        LicensedFeature.REPOSITORY_EVALUATION,
        LicensedFeature.REPOSITORY_REPORTS,
        LicensedFeature.SBOM_EVALUATION,
        LicensedFeature.SBOM_REPORTS,
        LicensedFeature.SOURCE_CONTROL,
        LicensedFeature.SUCCESS_METRICS,
        LicensedFeature.VULNERABILITY_CUSTOMIZATION,
        LicensedFeature.WAIVER_REPORTS);
  }

  @Test
  public void testGetFeatures_AuditorSaaS() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_AUDITOR_SAAS);
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).containsExactlyInAnyOrder( //
        LicensedFeature.DASHBOARD, //
        LicensedFeature.CLI_INTEGRATION, //
        LicensedFeature.DATA_RETENTION, //
        LicensedFeature.ENFORCEMENT, //
        LicensedFeature.INNER_SOURCE_REPOSITORIES, //
        LicensedFeature.NOTIFICATIONS, //
        LicensedFeature.ORGS_AND_APPS, //
        LicensedFeature.POLICY_GRANDFATHERING, //
        LicensedFeature.POLICY_MONITORING, //
        LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS, //
        LicensedFeature.PROPRIETARY_COMPONENTS, //
        LicensedFeature.RM_STAGING_INTEGRATION, //
        LicensedFeature.WAIVERS_DASHBOARD, //
        LicensedFeature.WEBHOOKS_FOR_APPLICATIONS,
        LicensedFeature.APPLICATION_REPORTS,
        LicensedFeature.APPLICATION_EVALUATION,
        LicensedFeature.CALL_FLOW_ANALYSIS,
        LicensedFeature.COMPONENT_EVALUATION,
        LicensedFeature.COMPONENT_LABELS,
        LicensedFeature.COMPONENT_SEARCH,
        LicensedFeature.POLICY_MANAGEMENT,
        LicensedFeature.POLICY_READ_ONLY,
        LicensedFeature.POLICY_VIOLATIONS,
        LicensedFeature.POLICY_WAIVERS,
        LicensedFeature.REPOSITORY_EVALUATION,
        LicensedFeature.REPOSITORY_REPORTS,
        LicensedFeature.SBOM_EVALUATION,
        LicensedFeature.SBOM_REPORTS,
        LicensedFeature.SOURCE_CONTROL,
        LicensedFeature.SUCCESS_METRICS,
        LicensedFeature.VULNERABILITY_CUSTOMIZATION,
        LicensedFeature.WAIVER_REPORTS);
  }

  @Test
  public void testGetFeatures_Lifecycle() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);

    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).containsExactlyInAnyOrder( //
        LicensedFeature.AUTOMATION, //
        LicensedFeature.ADVANCED_RECOMMENDATION_STRATEGIES, //
        LicensedFeature.BREAKING_CHANGE, //
        LicensedFeature.CI_INTEGRATION, //
        LicensedFeature.CLI_INTEGRATION, //
        LicensedFeature.DASHBOARD, //
        LicensedFeature.DATA_RETENTION, //
        LicensedFeature.ENFORCEMENT, //
        LicensedFeature.HYGIENE, //
        LicensedFeature.IDE_INTEGRATION, //
        LicensedFeature.INNER_SOURCE_REPOSITORIES, //
        LicensedFeature.NOTIFICATIONS, //
        LicensedFeature.ORGS_AND_APPS, //
        LicensedFeature.POLICY_MONITORING, //
        LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS, //
        LicensedFeature.POLICY_GRANDFATHERING, //
        LicensedFeature.PROPRIETARY_COMPONENTS, //
        LicensedFeature.QUALITY, //
        LicensedFeature.RELEASE_INTEGRITY, //
        LicensedFeature.RM_STAGING_INTEGRATION, //
        LicensedFeature.WAIVERS_DASHBOARD, //
        LicensedFeature.WEBHOOKS_FOR_APPLICATIONS,
        LicensedFeature.API_PAGE,
        LicensedFeature.APPLICATION_REPORTS,
        LicensedFeature.APPLICATION_EVALUATION,
        LicensedFeature.CALL_FLOW_ANALYSIS,
        LicensedFeature.COMPONENT_EVALUATION,
        LicensedFeature.COMPONENT_LABELS,
        LicensedFeature.COMPONENT_SEARCH,
        LicensedFeature.POLICY_MANAGEMENT,
        LicensedFeature.POLICY_READ_ONLY,
        LicensedFeature.POLICY_VIOLATIONS,
        LicensedFeature.POLICY_WAIVERS,
        LicensedFeature.REPOSITORY_EVALUATION,
        LicensedFeature.REPOSITORY_REPORTS,
        LicensedFeature.SBOM_EVALUATION,
        LicensedFeature.SBOM_REPORTS,
        LicensedFeature.SOURCE_CONTROL,
        LicensedFeature.SUCCESS_METRICS,
        LicensedFeature.VULNERABILITY_CUSTOMIZATION,
        LicensedFeature.WAIVER_REPORTS,
        LicensedFeature.ROI_CONFIGURATION,
        LicensedFeature.CONTAINER_IMAGES_EVALUATION,
        LicensedFeature.CUSTOM_POLICIES,
        LicensedFeature.CUSTOM_APPLICATION_CATEGORIES,
        LicensedFeature.CUSTOM_COMPONENT_LABELS,
        LicensedFeature.CUSTOM_LICENSE_THREAT_GROUPS,
        LicensedFeature.AUTO_WAIVER_MANAGEMENT,
        LicensedFeature.WAIVER_REQUEST_WORKFLOW,
        LicensedFeature.BULK_WAIVERS);
  }

  @Test
  public void testGetFeatures_LifecycleCloud() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD);

    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).containsExactlyInAnyOrder( //
        LicensedFeature.AUTOMATION, //
        LicensedFeature.CI_INTEGRATION, //
        LicensedFeature.CLI_INTEGRATION, //
        LicensedFeature.DASHBOARD, //
        LicensedFeature.DATA_RETENTION, //
        LicensedFeature.ENFORCEMENT, //
        LicensedFeature.IDE_INTEGRATION, //
        LicensedFeature.INNER_SOURCE_REPOSITORIES, //
        LicensedFeature.IP_ALLOWLIST, //
        LicensedFeature.NOTIFICATIONS, //
        LicensedFeature.ORGS_AND_APPS, //
        LicensedFeature.POLICY_MONITORING, //
        LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS, //
        LicensedFeature.POLICY_GRANDFATHERING, //
        LicensedFeature.PROPRIETARY_COMPONENTS, //
        LicensedFeature.QUALITY, //
        LicensedFeature.RM_STAGING_INTEGRATION, //
        LicensedFeature.WAIVERS_DASHBOARD, //
        LicensedFeature.WEBHOOKS_FOR_APPLICATIONS,
        LicensedFeature.API_PAGE,
        LicensedFeature.APPLICATION_REPORTS,
        LicensedFeature.APPLICATION_EVALUATION,
        LicensedFeature.CALL_FLOW_ANALYSIS,
        LicensedFeature.COMPONENT_EVALUATION,
        LicensedFeature.COMPONENT_LABELS,
        LicensedFeature.COMPONENT_SEARCH,
        LicensedFeature.POLICY_MANAGEMENT,
        LicensedFeature.POLICY_READ_ONLY,
        LicensedFeature.POLICY_VIOLATIONS,
        LicensedFeature.POLICY_WAIVERS,
        LicensedFeature.REPOSITORY_EVALUATION,
        LicensedFeature.REPOSITORY_REPORTS,
        LicensedFeature.SBOM_EVALUATION,
        LicensedFeature.SBOM_REPORTS,
        LicensedFeature.SOURCE_CONTROL,
        LicensedFeature.SUCCESS_METRICS,
        LicensedFeature.VULNERABILITY_CUSTOMIZATION,
        LicensedFeature.WAIVER_REPORTS,
        LicensedFeature.ROI_CONFIGURATION,
        LicensedFeature.CONTAINER_IMAGES_EVALUATION,
        LicensedFeature.CUSTOM_POLICIES,
        LicensedFeature.CUSTOM_APPLICATION_CATEGORIES,
        LicensedFeature.CUSTOM_COMPONENT_LABELS,
        LicensedFeature.CUSTOM_LICENSE_THREAT_GROUPS,
        LicensedFeature.AUTO_WAIVER_MANAGEMENT,
        LicensedFeature.WAIVER_REQUEST_WORKFLOW,
        LicensedFeature.BULK_WAIVERS);
  }

  @Test
  public void testGetFeatures_LifecycleSaas() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);

    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).containsExactlyInAnyOrder( //
        LicensedFeature.AUTOMATION,
        LicensedFeature.ADVANCED_RECOMMENDATION_STRATEGIES,
        LicensedFeature.BREAKING_CHANGE, //
        LicensedFeature.CI_INTEGRATION, //
        LicensedFeature.CLI_INTEGRATION, //
        LicensedFeature.DASHBOARD, //
        LicensedFeature.DATA_RETENTION, //
        LicensedFeature.ENFORCEMENT, //
        LicensedFeature.HYGIENE,
        LicensedFeature.IDE_INTEGRATION, //
        LicensedFeature.INNER_SOURCE_REPOSITORIES, //
        LicensedFeature.NOTIFICATIONS, //
        LicensedFeature.ORGS_AND_APPS, //
        LicensedFeature.POLICY_GRANDFATHERING, //
        LicensedFeature.POLICY_MONITORING, //
        LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS, //
        LicensedFeature.PROPRIETARY_COMPONENTS, //
        LicensedFeature.QUALITY, //
        LicensedFeature.RELEASE_INTEGRITY,
        LicensedFeature.RM_STAGING_INTEGRATION, //
        LicensedFeature.WAIVERS_DASHBOARD, //
        LicensedFeature.WEBHOOKS_FOR_APPLICATIONS,
        LicensedFeature.API_PAGE,
        LicensedFeature.APPLICATION_REPORTS,
        LicensedFeature.APPLICATION_EVALUATION,
        LicensedFeature.CALL_FLOW_ANALYSIS,
        LicensedFeature.COMPONENT_EVALUATION,
        LicensedFeature.COMPONENT_LABELS,
        LicensedFeature.COMPONENT_SEARCH,
        LicensedFeature.POLICY_MANAGEMENT,
        LicensedFeature.POLICY_READ_ONLY,
        LicensedFeature.POLICY_VIOLATIONS,
        LicensedFeature.POLICY_WAIVERS,
        LicensedFeature.REPOSITORY_EVALUATION,
        LicensedFeature.REPOSITORY_REPORTS,
        LicensedFeature.SBOM_EVALUATION,
        LicensedFeature.SBOM_REPORTS,
        LicensedFeature.SOURCE_CONTROL,
        LicensedFeature.SUCCESS_METRICS,
        LicensedFeature.VULNERABILITY_CUSTOMIZATION,
        LicensedFeature.WAIVER_REPORTS,
        LicensedFeature.ROI_CONFIGURATION,
        LicensedFeature.CONTAINER_IMAGES_EVALUATION,
        LicensedFeature.CUSTOM_POLICIES,
        LicensedFeature.CUSTOM_APPLICATION_CATEGORIES,
        LicensedFeature.CUSTOM_COMPONENT_LABELS,
        LicensedFeature.CUSTOM_LICENSE_THREAT_GROUPS,
        LicensedFeature.AUTO_WAIVER_MANAGEMENT,
        LicensedFeature.WAIVER_REQUEST_WORKFLOW,
        LicensedFeature.BULK_WAIVERS);
  }

  @Test
  public void testGetFeatures_Firewall() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL);
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).containsExactlyInAnyOrder( //
        LicensedFeature.FIREWALL, //
        LicensedFeature.FIREWALL_FOR_ARTIFACTORY,
        LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES, //
        LicensedFeature.RM_STAGING_INTEGRATION, //
        LicensedFeature.WAIVERS_DASHBOARD, //
        LicensedFeature.WEBHOOKS_FOR_REPOSITORIES,
        LicensedFeature.API_PAGE,
        LicensedFeature.APPLICATION_REPORTS,
        LicensedFeature.APPLICATION_EVALUATION,
        LicensedFeature.CALL_FLOW_ANALYSIS,
        LicensedFeature.COMPONENT_EVALUATION,
        LicensedFeature.COMPONENT_LABELS,
        LicensedFeature.COMPONENT_SEARCH,
        LicensedFeature.POLICY_MANAGEMENT,
        LicensedFeature.POLICY_READ_ONLY,
        LicensedFeature.POLICY_VIOLATIONS,
        LicensedFeature.POLICY_WAIVERS,
        LicensedFeature.REPOSITORY_EVALUATION,
        LicensedFeature.REPOSITORY_REPORTS,
        LicensedFeature.SBOM_EVALUATION,
        LicensedFeature.SBOM_REPORTS,
        LicensedFeature.SOURCE_CONTROL,
        LicensedFeature.SUCCESS_METRICS,
        LicensedFeature.VULNERABILITY_CUSTOMIZATION,
        LicensedFeature.WAIVER_REPORTS,
        LicensedFeature.ROI_CONFIGURATION);
  }

  @Test
  public void testGetFeatures_FirewallForArtifactory() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL_FOR_ARTIFACTORY);
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).containsExactlyInAnyOrder( //
        LicensedFeature.FIREWALL,
        LicensedFeature.FIREWALL_FOR_ARTIFACTORY, //
        LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES, //
        LicensedFeature.RM_STAGING_INTEGRATION, //
        LicensedFeature.WAIVERS_DASHBOARD, //
        LicensedFeature.WEBHOOKS_FOR_REPOSITORIES,
        LicensedFeature.API_PAGE,
        LicensedFeature.APPLICATION_REPORTS,
        LicensedFeature.APPLICATION_EVALUATION,
        LicensedFeature.CALL_FLOW_ANALYSIS,
        LicensedFeature.COMPONENT_EVALUATION,
        LicensedFeature.COMPONENT_LABELS,
        LicensedFeature.COMPONENT_SEARCH,
        LicensedFeature.POLICY_MANAGEMENT,
        LicensedFeature.POLICY_READ_ONLY,
        LicensedFeature.POLICY_VIOLATIONS,
        LicensedFeature.POLICY_WAIVERS,
        LicensedFeature.REPOSITORY_EVALUATION,
        LicensedFeature.REPOSITORY_REPORTS,
        LicensedFeature.SBOM_EVALUATION,
        LicensedFeature.SBOM_REPORTS,
        LicensedFeature.SOURCE_CONTROL,
        LicensedFeature.SUCCESS_METRICS,
        LicensedFeature.VULNERABILITY_CUSTOMIZATION,
        LicensedFeature.WAIVER_REPORTS,
        LicensedFeature.ROI_CONFIGURATION);
  }

  @Test
  public void testGetFeatures_FirewallForArtifactory_V2() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL_FOR_ARTIFACTORY_V2);
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).containsExactlyInAnyOrder( //
        LicensedFeature.FIREWALL,
        LicensedFeature.FIREWALL_AUTO_UNQUARANTINE,
        LicensedFeature.FIREWALL_FOR_ARTIFACTORY, //
        LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES, //
        LicensedFeature.RELEASE_INTEGRITY,
        LicensedFeature.RM_STAGING_INTEGRATION, //
        LicensedFeature.WAIVERS_DASHBOARD, //
        LicensedFeature.WEBHOOKS_FOR_REPOSITORIES,
        LicensedFeature.API_PAGE,
        LicensedFeature.APPLICATION_REPORTS,
        LicensedFeature.APPLICATION_EVALUATION,
        LicensedFeature.CALL_FLOW_ANALYSIS,
        LicensedFeature.COMPONENT_EVALUATION,
        LicensedFeature.COMPONENT_LABELS,
        LicensedFeature.COMPONENT_SEARCH,
        LicensedFeature.POLICY_MANAGEMENT,
        LicensedFeature.POLICY_READ_ONLY,
        LicensedFeature.POLICY_VIOLATIONS,
        LicensedFeature.POLICY_WAIVERS,
        LicensedFeature.REPOSITORY_EVALUATION,
        LicensedFeature.REPOSITORY_REPORTS,
        LicensedFeature.SBOM_EVALUATION,
        LicensedFeature.SBOM_REPORTS,
        LicensedFeature.SOURCE_CONTROL,
        LicensedFeature.SUCCESS_METRICS,
        LicensedFeature.VULNERABILITY_CUSTOMIZATION,
        LicensedFeature.WAIVER_REPORTS,
        LicensedFeature.ROI_CONFIGURATION,
        LicensedFeature.MALWARE_DEFENSE_EVALUATION,
        LicensedFeature.CONTAINER_IMAGES_EVALUATION,
        LicensedFeature.NOTIFICATIONS);
  }

  @Test
  public void testGetFeatures_FirewallV2() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL_V2);
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).containsExactlyInAnyOrder( //
        LicensedFeature.FIREWALL, //
        LicensedFeature.FIREWALL_AUTO_UNQUARANTINE, //
        LicensedFeature.FIREWALL_FOR_ARTIFACTORY,
        LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES, //
        LicensedFeature.RELEASE_INTEGRITY, //
        LicensedFeature.RM_STAGING_INTEGRATION, //
        LicensedFeature.WAIVERS_DASHBOARD, //
        LicensedFeature.WEBHOOKS_FOR_REPOSITORIES,
        LicensedFeature.API_PAGE,
        LicensedFeature.APPLICATION_REPORTS,
        LicensedFeature.APPLICATION_EVALUATION,
        LicensedFeature.CALL_FLOW_ANALYSIS,
        LicensedFeature.COMPONENT_EVALUATION,
        LicensedFeature.COMPONENT_LABELS,
        LicensedFeature.COMPONENT_SEARCH,
        LicensedFeature.POLICY_MANAGEMENT,
        LicensedFeature.POLICY_READ_ONLY,
        LicensedFeature.POLICY_VIOLATIONS,
        LicensedFeature.POLICY_WAIVERS,
        LicensedFeature.REPOSITORY_EVALUATION,
        LicensedFeature.REPOSITORY_REPORTS,
        LicensedFeature.SBOM_EVALUATION,
        LicensedFeature.SBOM_REPORTS,
        LicensedFeature.SOURCE_CONTROL,
        LicensedFeature.SUCCESS_METRICS,
        LicensedFeature.VULNERABILITY_CUSTOMIZATION,
        LicensedFeature.WAIVER_REPORTS,
        LicensedFeature.ROI_CONFIGURATION,
        LicensedFeature.MALWARE_DEFENSE_EVALUATION,
        LicensedFeature.CONTAINER_IMAGES_EVALUATION,
        LicensedFeature.NOTIFICATIONS);
  }

  @Test
  public void testGetFeatures_LifecycleFirewallCloud() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_CLOUD);
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).containsExactlyInAnyOrder( //
        LicensedFeature.FIREWALL, //
        LicensedFeature.FIREWALL_AUTO_UNQUARANTINE, //
        LicensedFeature.FIREWALL_FOR_ARTIFACTORY,
        LicensedFeature.IP_ALLOWLIST, //
        LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES, //
        LicensedFeature.RELEASE_INTEGRITY, //
        LicensedFeature.RM_STAGING_INTEGRATION, //
        LicensedFeature.WAIVERS_DASHBOARD, //
        LicensedFeature.WEBHOOKS_FOR_REPOSITORIES,
        LicensedFeature.API_PAGE,
        LicensedFeature.APPLICATION_REPORTS,
        LicensedFeature.APPLICATION_EVALUATION,
        LicensedFeature.CALL_FLOW_ANALYSIS,
        LicensedFeature.COMPONENT_EVALUATION,
        LicensedFeature.COMPONENT_LABELS,
        LicensedFeature.COMPONENT_SEARCH,
        LicensedFeature.POLICY_MANAGEMENT,
        LicensedFeature.POLICY_READ_ONLY,
        LicensedFeature.POLICY_VIOLATIONS,
        LicensedFeature.POLICY_WAIVERS,
        LicensedFeature.REPOSITORY_EVALUATION,
        LicensedFeature.REPOSITORY_REPORTS,
        LicensedFeature.SBOM_EVALUATION,
        LicensedFeature.SBOM_REPORTS,
        LicensedFeature.SOURCE_CONTROL,
        LicensedFeature.SUCCESS_METRICS,
        LicensedFeature.VULNERABILITY_CUSTOMIZATION,
        LicensedFeature.WAIVER_REPORTS,
        LicensedFeature.ROI_CONFIGURATION,
        LicensedFeature.MALWARE_DEFENSE_EVALUATION,
        LicensedFeature.CONTAINER_IMAGES_EVALUATION);
  }

  /**
   * @deprecated This tested code is deprecated
   */
  @Deprecated
  @Test
  public void testGetFeatures_LifecycleFirewallSaas() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_SAAS);
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).containsExactlyInAnyOrder(//
        LicensedFeature.FIREWALL, //
        LicensedFeature.FIREWALL_AUTO_UNQUARANTINE, //
        LicensedFeature.FIREWALL_FOR_ARTIFACTORY,
        LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES, //
        LicensedFeature.RELEASE_INTEGRITY, //
        LicensedFeature.RM_STAGING_INTEGRATION, //
        LicensedFeature.WAIVERS_DASHBOARD, //
        LicensedFeature.WEBHOOKS_FOR_REPOSITORIES,
        LicensedFeature.API_PAGE,
        LicensedFeature.APPLICATION_REPORTS,
        LicensedFeature.APPLICATION_EVALUATION,
        LicensedFeature.CALL_FLOW_ANALYSIS,
        LicensedFeature.COMPONENT_EVALUATION,
        LicensedFeature.COMPONENT_LABELS,
        LicensedFeature.COMPONENT_SEARCH,
        LicensedFeature.POLICY_MANAGEMENT,
        LicensedFeature.POLICY_READ_ONLY,
        LicensedFeature.POLICY_VIOLATIONS,
        LicensedFeature.POLICY_WAIVERS,
        LicensedFeature.REPOSITORY_EVALUATION,
        LicensedFeature.REPOSITORY_REPORTS,
        LicensedFeature.SBOM_EVALUATION,
        LicensedFeature.SBOM_REPORTS,
        LicensedFeature.SOURCE_CONTROL,
        LicensedFeature.SUCCESS_METRICS,
        LicensedFeature.VULNERABILITY_CUSTOMIZATION,
        LicensedFeature.WAIVER_REPORTS,
        LicensedFeature.ROI_CONFIGURATION,
        LicensedFeature.MALWARE_DEFENSE_EVALUATION,
        LicensedFeature.CONTAINER_IMAGES_EVALUATION,
        LicensedFeature.NOTIFICATIONS);
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
        LicensedFeature.DATA_RETENTION, //
        LicensedFeature.INNER_SOURCE_REPOSITORIES, //
        LicensedFeature.ORGS_AND_APPS, //
        LicensedFeature.PROPRIETARY_COMPONENTS, //
        LicensedFeature.QUALITY, //
        LicensedFeature.RM_STAGING_INTEGRATION, //
        LicensedFeature.WAIVERS_DASHBOARD,
        LicensedFeature.API_PAGE,
        LicensedFeature.APPLICATION_REPORTS,
        LicensedFeature.APPLICATION_EVALUATION,
        LicensedFeature.CALL_FLOW_ANALYSIS,
        LicensedFeature.COMPONENT_EVALUATION,
        LicensedFeature.COMPONENT_LABELS,
        LicensedFeature.COMPONENT_SEARCH,
        LicensedFeature.POLICY_MANAGEMENT,
        LicensedFeature.POLICY_READ_ONLY,
        LicensedFeature.POLICY_VIOLATIONS,
        LicensedFeature.POLICY_WAIVERS,
        LicensedFeature.REPOSITORY_EVALUATION,
        LicensedFeature.REPOSITORY_REPORTS,
        LicensedFeature.SBOM_EVALUATION,
        LicensedFeature.SBOM_REPORTS,
        LicensedFeature.SOURCE_CONTROL,
        LicensedFeature.SUCCESS_METRICS,
        LicensedFeature.VULNERABILITY_CUSTOMIZATION,
        LicensedFeature.WAIVER_REPORTS,
        LicensedFeature.CONTAINER_IMAGES_EVALUATION);
  }

  @Test
  public void testGetFeatures_FoundationSaaS() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_FOUNDATION_SAAS);
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).containsExactlyInAnyOrder( //
        LicensedFeature.CI_INTEGRATION, //
        LicensedFeature.CLI_INTEGRATION, //
        LicensedFeature.DASHBOARD, //
        LicensedFeature.DATA_RETENTION, //
        LicensedFeature.INNER_SOURCE_REPOSITORIES, //
        LicensedFeature.ORGS_AND_APPS, //
        LicensedFeature.PROPRIETARY_COMPONENTS, //
        LicensedFeature.QUALITY, //
        LicensedFeature.RM_STAGING_INTEGRATION, //
        LicensedFeature.WAIVERS_DASHBOARD,
        LicensedFeature.API_PAGE,
        LicensedFeature.APPLICATION_REPORTS,
        LicensedFeature.APPLICATION_EVALUATION,
        LicensedFeature.CALL_FLOW_ANALYSIS,
        LicensedFeature.COMPONENT_EVALUATION,
        LicensedFeature.COMPONENT_LABELS,
        LicensedFeature.COMPONENT_SEARCH,
        LicensedFeature.POLICY_MANAGEMENT,
        LicensedFeature.POLICY_READ_ONLY,
        LicensedFeature.POLICY_VIOLATIONS,
        LicensedFeature.POLICY_WAIVERS,
        LicensedFeature.REPOSITORY_EVALUATION,
        LicensedFeature.REPOSITORY_REPORTS,
        LicensedFeature.SBOM_EVALUATION,
        LicensedFeature.SBOM_REPORTS,
        LicensedFeature.SOURCE_CONTROL,
        LicensedFeature.SUCCESS_METRICS,
        LicensedFeature.VULNERABILITY_CUSTOMIZATION,
        LicensedFeature.WAIVER_REPORTS,
        LicensedFeature.CONTAINER_IMAGES_EVALUATION);
  }

  @Test
  public void testGetFeatures_SbomManager() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).containsExactlyInAnyOrder( //
        LicensedFeature.API_PAGE,
        LicensedFeature.SBOM_MANAGER,
        LicensedFeature.POLICY_MONITORING,
        LicensedFeature.POLICY_READ_ONLY,
        LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS,
        LicensedFeature.CLI_INTEGRATION,
        LicensedFeature.NOTIFICATIONS,
        LicensedFeature.DATA_RETENTION,
        LicensedFeature.ORGS_AND_APPS,
        LicensedFeature.ENFORCEMENT,
        LicensedFeature.APPLICATION_EVALUATION,
        LicensedFeature.CONTAINER_IMAGES_EVALUATION);
  }

  @Test
  public void testGetFeatures_SbomManagerSaas() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).containsExactlyInAnyOrder( //
        LicensedFeature.API_PAGE,
        LicensedFeature.SBOM_MANAGER,
        LicensedFeature.POLICY_MONITORING,
        LicensedFeature.POLICY_READ_ONLY,
        LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS,
        LicensedFeature.CLI_INTEGRATION,
        LicensedFeature.NOTIFICATIONS,
        LicensedFeature.DATA_RETENTION,
        LicensedFeature.ORGS_AND_APPS,
        LicensedFeature.ENFORCEMENT,
        LicensedFeature.APPLICATION_EVALUATION,
        LicensedFeature.CONTAINER_IMAGES_EVALUATION);
  }

  @Test
  public void testGetFeatures_TeamsEdition() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_TEAMS_EDITION);
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).containsExactlyInAnyOrder(
        LicensedFeature.AUTOMATION,
        LicensedFeature.ADVANCED_RECOMMENDATION_STRATEGIES,
        LicensedFeature.BREAKING_CHANGE,
        LicensedFeature.CI_INTEGRATION,
        LicensedFeature.CLI_INTEGRATION,
        LicensedFeature.DASHBOARD,
        LicensedFeature.DATA_RETENTION,
        LicensedFeature.ENFORCEMENT,
        LicensedFeature.HYGIENE,
        LicensedFeature.IDE_INTEGRATION,
        LicensedFeature.INNER_SOURCE_REPOSITORIES,
        LicensedFeature.NOTIFICATIONS,
        LicensedFeature.ORGS_AND_APPS,
        LicensedFeature.POLICY_GRANDFATHERING,
        LicensedFeature.POLICY_MONITORING,
        LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS,
        LicensedFeature.PROPRIETARY_COMPONENTS,
        LicensedFeature.QUALITY,
        LicensedFeature.RELEASE_INTEGRITY,
        LicensedFeature.RM_STAGING_INTEGRATION,
        LicensedFeature.WAIVERS_DASHBOARD,
        LicensedFeature.WEBHOOKS_FOR_APPLICATIONS,
        LicensedFeature.DEVELOPER_DASHBOARD,
        LicensedFeature.API_PAGE,
        LicensedFeature.APPLICATION_REPORTS,
        LicensedFeature.APPLICATION_EVALUATION,
        LicensedFeature.CALL_FLOW_ANALYSIS,
        LicensedFeature.COMPONENT_EVALUATION,
        LicensedFeature.COMPONENT_LABELS,
        LicensedFeature.COMPONENT_SEARCH,
        LicensedFeature.POLICY_MANAGEMENT,
        LicensedFeature.POLICY_READ_ONLY,
        LicensedFeature.POLICY_VIOLATIONS,
        LicensedFeature.POLICY_WAIVERS,
        LicensedFeature.REPOSITORY_EVALUATION,
        LicensedFeature.REPOSITORY_REPORTS,
        LicensedFeature.SBOM_EVALUATION,
        LicensedFeature.SBOM_REPORTS,
        LicensedFeature.SOURCE_CONTROL,
        LicensedFeature.SUCCESS_METRICS,
        LicensedFeature.VULNERABILITY_CUSTOMIZATION,
        LicensedFeature.WAIVER_REPORTS,
        LicensedFeature.ROI_CONFIGURATION,
        LicensedFeature.CONTAINER_IMAGES_EVALUATION,
        LicensedFeature.CUSTOM_POLICIES,
        LicensedFeature.CUSTOM_APPLICATION_CATEGORIES,
        LicensedFeature.CUSTOM_COMPONENT_LABELS,
        LicensedFeature.CUSTOM_LICENSE_THREAT_GROUPS,
        LicensedFeature.AUTO_WAIVER_MANAGEMENT,
        LicensedFeature.WAIVER_REQUEST_WORKFLOW,
        LicensedFeature.BULK_WAIVERS);
  }

  @Test
  public void testGetFeatures_Developer() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_SONATYPE_DEVELOPMENT);
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).containsExactlyInAnyOrder(
        LicensedFeature.API_PAGE,
        LicensedFeature.DEVELOPER_DASHBOARD);
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
  public void testGetStageTypes_AuditorSaas() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_AUDITOR_SAAS);
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
        StageTypes.PROXY,
        StageTypes.STAGE_RELEASE,
        StageTypes.RELEASE);
  }

  @Test
  public void testGetStageTypes_FirewallForArtifactory_V2() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL_FOR_ARTIFACTORY_V2);
    mockHdsProductLicenseDetails(withStages());
    installLicense();

    assertThat(productLicense.getStageTypes()).containsExactlyInAnyOrder( //
        StageTypes.PROXY,
        StageTypes.RELEASE,
        StageTypes.STAGE_RELEASE);
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

  /**
   * @deprecated This tested code is deprecated
   */
  @Deprecated
  @Test
  public void testGetStageTypes_LifeCycleFirewallSaas() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_SAAS);
    mockHdsProductLicenseDetails(withStages());
    installLicense();

    assertThat(productLicense.getStageTypes()).containsExactlyInAnyOrder(
        StageTypes.STAGE_RELEASE,
        StageTypes.RELEASE,
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
  public void testGetStageTypes_LifecycleSaas() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    mockHdsProductLicenseDetails(withStages());
    installLicense();

    assertThat(productLicense.getStageTypes()).containsExactlyInAnyOrder(
        StageTypes.DEVELOP,
        StageTypes.SOURCE,
        StageTypes.BUILD,
        StageTypes.STAGE_RELEASE,
        StageTypes.RELEASE,
        StageTypes.OPERATE,
        StageTypes.PROXY);
  }

  @Test
  public void testGetStageTypes_TeamsEdition() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_TEAMS_EDITION);
    mockHdsProductLicenseDetails(withStages());
    installLicense();

    assertThat(productLicense.getStageTypes()).containsExactlyInAnyOrder(
        StageTypes.DEVELOP,
        StageTypes.SOURCE,
        StageTypes.BUILD,
        StageTypes.STAGE_RELEASE,
        StageTypes.RELEASE,
        StageTypes.OPERATE,
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
  public void testGetStageTypes_FoundationSaas() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_FOUNDATION_SAAS);
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
  public void testGetStageTypes_SbomManager() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
    mockHdsProductLicenseDetails(withStages());
    installLicense();

    assertThat(productLicense.getStageTypes()).containsOnly(StageTypes.COMPLIANCE, StageTypes.PROXY);
  }

  @Test
  public void testGetStageTypes_SbomManagerSaas() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    mockHdsProductLicenseDetails(withStages());
    installLicense();

    assertThat(productLicense.getStageTypes()).containsOnly(StageTypes.COMPLIANCE, StageTypes.PROXY);
  }

  @Test
  public void testUpdateLicenseCacheFromDatabase() {
    CLMLicenseManager clmLicenseManagerSpy = spy(clmLicenseManager);

    // before
    assertThat(productLicense.getMaxApplications()).isEqualTo(100);
    assertThat(productLicense.getMaxSboms()).isEqualTo(50);

    // set database license
    SignedProductLicenseDetailsDTO licenseDetails = new SignedProductLicenseDetailsDTO();
    licenseDetails.features = new TreeSet<>();
    licenseDetails.stageIds = new TreeSet<>();
    licenseDetails.maxApplications = 12345;
    licenseDetails.maxSboms = 100;
    productLicenseSigner.sign(licenseDetails, licenseFingerprinter.calculate());
    productLicenseDetailsCache.setProductLicenseDetails(licenseDetails);

    clmLicenseManagerSpy.updateLicenseCacheFromDatabase();

    // after
    assertThat(productLicense.getMaxApplications()).isEqualTo(12345);
    assertThat(productLicense.getMaxSboms()).isEqualTo(100);
    verify(clmLicenseManagerSpy, never()).loadLicense();
    verify(clmLicenseManagerSpy, never()).loadProductLicenseOnAllOtherClusterNodes();
  }

  @Test
  public void testUpdateLicenseCacheFromDatabase_ClearsCacheNoDatabaseRecord() {
    CLMLicenseManager clmLicenseManagerSpy = spy(clmLicenseManager);

    // before
    assertThat(productLicense.isValid()).isTrue();
    assertThat(productLicense.getMaxApplications()).isEqualTo(100);
    assertThat(productLicense.getMaxSboms()).isEqualTo(50);

    productLicenseDetailsCache.saveJson(null);
    clmLicenseManagerSpy.updateLicenseCacheFromDatabase();

    // after
    assertThat(productLicense.isValid()).isFalse();
    assertThat(productLicense.getMaxApplications()).isZero();
    assertThat(productLicense.getMaxSboms()).isZero();
    verify(clmLicenseManagerSpy, never()).loadProductLicenseOnAllOtherClusterNodes();
  }

  @Test
  public void testLoadLicense() {
    CLMLicenseManager clmLicenseManagerSpy = spy(clmLicenseManager);
    mockHdsProductLicenseDetails(withFeatures(LicensedFeature.CI_INTEGRATION, LicensedFeature.DASHBOARD)
        .andThen(withStages(StageTypes.BUILD, StageTypes.RELEASE).andThen(withMaxApplications(12345))));

    clmLicenseManagerSpy.loadLicense();

    assertThat(productLicense.isValid()).isTrue();
    SignedProductLicenseDetailsDTO licenseDetails = productLicenseDetailsCache.getProductLicenseDetails();
    assertThat(licenseDetails).isNotNull();
    assertThat(licenseDetails.features).containsExactly(LicensedFeature.CI_INTEGRATION.name(),
        LicensedFeature.DASHBOARD.name());
    assertThat(licenseDetails.stageIds).contains(StageTypes.BUILD.getId(), StageTypes.RELEASE.getId());
    assertThat(licenseDetails.maxApplications).isEqualTo(12345);
    verify(clmLicenseManagerSpy, times(1)).loadProductLicenseOnAllOtherClusterNodes();
  }

  @Test
  public void testLoadLicense_MaxSboms() {
    CLMLicenseManager clmLicenseManagerSpy = spy(clmLicenseManager);
    config.setDatabase(new DatabaseConfig());
    mockHdsProductLicenseDetails(withFeatures(LicensedFeature.EXTERNAL_DATABASE, LicensedFeature.SBOM_MANAGER)
        .andThen(withStages(StageTypes.RELEASE).andThen(withMaxSboms(50))));

    clmLicenseManagerSpy.loadLicense();

    assertThat(productLicense.isValid()).isTrue();
    SignedProductLicenseDetailsDTO licenseDetails = productLicenseDetailsCache.getProductLicenseDetails();
    assertThat(licenseDetails).isNotNull();
    assertThat(licenseDetails.features).containsExactly(LicensedFeature.EXTERNAL_DATABASE.name(),
        LicensedFeature.SBOM_MANAGER.name());
    assertThat(licenseDetails.stageIds).contains(StageTypes.RELEASE.getId());
    assertThat(licenseDetails.maxSboms).isEqualTo(50);
    verify(clmLicenseManagerSpy, times(1)).loadProductLicenseOnAllOtherClusterNodes();
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
    licenseDetails.maxSboms = 50;
    productLicenseSigner.sign(licenseDetails, licenseFingerprinter.calculate());
    productLicenseDetailsCache.setProductLicenseDetails(licenseDetails);

    clmLicenseManager.loadLicense();

    assertThat(productLicense.isValid()).isTrue();
    licenseDetails = productLicenseDetailsCache.getProductLicenseDetails();
    assertThat(licenseDetails).isNotNull();
    assertThat(licenseDetails.features).containsExactly("featureA", "featureB");
    assertThat(licenseDetails.stageIds).contains("stageA", "stageB");
    assertThat(licenseDetails.maxApplications).isEqualTo(12345);
    assertThat(licenseDetails.maxSboms).isEqualTo(50);
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

    assertThatExceptionOfType(LicensingException.class)
        .isThrownBy(() -> clmLicenseManager.loadLicense())
        .withMessageContaining("license does not support use of an external database");

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
  public void testLoadLicense_SbomManagerFeature_ExternalDatabaseNotAllowed() {
    mockHdsProductLicenseDetails(withFeatures(LicensedFeature.SBOM_MANAGER));

    assertThatExceptionOfType(LicensingException.class)
        .isThrownBy(() -> clmLicenseManager.loadLicense())
        .withMessageContaining(
            "SBOM Manager feature requires use of an external database, please retry using an external database.");
  }

  @Test
  public void testLoadLicense_GuideFeature_ExternalDatabaseNotAllowed() {
    mockHdsProductLicenseDetails(withFeatures(LicensedFeature.GUIDE));

    assertThatExceptionOfType(LicensingException.class)
        .isThrownBy(() -> clmLicenseManager.loadLicense())
        .withMessageContaining(
            "Guide feature requires use of an external database, please retry using an external database.");
  }

  @Test
  public void testInstallLicense_LegacyVersion() {
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
  public void testInstallLicense_BadMaxFirewallUsers() {
    assertThatExceptionOfType(LicensingException.class).isThrownBy(() -> {
      licenseManager.setProperty(ProductLicenseDetails.PROPERTY_MAX_FIREWALL_USERS, "Invalid");
      installLicense();
    }).withMessage("Invalid value for max firewall users: Invalid");
  }

  @Test
  public void testInstallLicense_BadMaxUsers() {
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
  public void testInstallLicense_LicenseDetailsFromHds_MaxSboms() throws Exception {
    config.setDatabase(new DatabaseConfig());
    mockHdsProductLicenseDetails(withFeatures(LicensedFeature.EXTERNAL_DATABASE, LicensedFeature.SBOM_MANAGER)
        .andThen(withStages(StageTypes.RELEASE).andThen(withMaxSboms(50))));
    installLicense();
    SignedProductLicenseDetailsDTO licenseDetails = productLicenseDetailsCache.getProductLicenseDetails();
    assertThat(licenseDetails).isNotNull();
    assertThat(licenseDetails.features).containsExactly(LicensedFeature.EXTERNAL_DATABASE.name(),
        LicensedFeature.SBOM_MANAGER.name());
    assertThat(licenseDetails.stageIds).contains(StageTypes.RELEASE.getId());
    assertThat(licenseDetails.maxSboms).isEqualTo(50);
  }

  @Test
  public void testInstallLicense_LicenseDetailsFromHds_InvalidSignature() {
    clmLicenseManager.uninstallLicense();
    mockHdsProductLicenseDetails(withInvalidSignature());
    assertThatExceptionOfType(LicensingException.class).isThrownBy(this::installLicense)
        .withMessageStartingWith("Could not verify signature of license details");
    assertThat(licenseManager.isValid()).isFalse();
    assertThat(productLicense.isValid()).isFalse();
  }

  @Test
  public void testInstallLicense_LicenseDetailsFromHds_RequestFailure() {
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
  public void testInstallLicense_ExternalDatabaseNotAllowedButCurrentlyUsed() {
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
  public void testInstallLicense_SbomManagerFeature_ExternalDatabaseNotAllowed() {
    mockHdsProductLicenseDetails(withFeatures(LicensedFeature.SBOM_MANAGER));
    clmLicenseManager.uninstallLicense();

    assertThatExceptionOfType(LicensingException.class).isThrownBy(this::installLicense)
        .withMessageContaining(
            "SBOM Manager feature requires use of an external database, please retry using an external database.");
    assertThat(productLicense.isValid()).isFalse();
  }

  @Test
  public void testInstallLicense_GuideFeature_ExternalDatabaseNotAllowed() {
    mockHdsProductLicenseDetails(withFeatures(LicensedFeature.GUIDE));
    clmLicenseManager.uninstallLicense();

    assertThatExceptionOfType(LicensingException.class).isThrownBy(this::installLicense)
        .withMessageContaining(
            "Guide feature requires use of an external database, please retry using an external database.");
    assertThat(productLicense.isValid()).isFalse();
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
  public void testInstallLicense_IntegratedEnterpriseReportingFeatureFromHds() throws Exception {
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).doesNotContain(LicensedFeature.INTEGRATED_ENTERPRISE_REPORTING);

    mockHdsProductLicenseDetails(withFeatures(LicensedFeature.INTEGRATED_ENTERPRISE_REPORTING));
    installLicense();
    assertThat(productLicense.getFeatures()).contains(LicensedFeature.INTEGRATED_ENTERPRISE_REPORTING);
  }

  @Test
  public void testInstallLicense_CpeMatchingFeatureFromHds() throws Exception {
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).doesNotContain(LicensedFeature.CPE_MATCHING);

    mockHdsProductLicenseDetails(withFeatures(LicensedFeature.CPE_MATCHING));
    installLicense();
    assertThat(productLicense.getFeatures()).contains(LicensedFeature.CPE_MATCHING);
  }

  @Test
  public void testInstallLicense_MaliciousUrlsPartnerAccessFeatureFromHds() throws Exception {
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).doesNotContain(LicensedFeature.MALICIOUS_URLS_PARTNER_ACCESS);

    mockHdsProductLicenseDetails(withFeatures(LicensedFeature.MALICIOUS_URLS_PARTNER_ACCESS));
    installLicense();
    assertThat(productLicense.getFeatures()).contains(LicensedFeature.MALICIOUS_URLS_PARTNER_ACCESS);
  }

  @Test
  public void testNotifyListener_LoadLicense() {
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
  public void testGetLicenseSummary_ProductEditionNone() {
    clmLicenseManager.uninstallLicense();
    LicenseSummary summary = clmLicenseManager.getLicenseSummary();
    assertThat(summary).isNotNull();
    assertThat(summary.productEdition).isEqualTo("");
    assertThat(summary.products).isEmpty();
  }

  @Test
  public void testGetLicenseSummary_ProductEditionAuditor() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    installLicense();
    LicenseSummary summary = clmLicenseManager.getLicenseSummary();
    assertThat(summary).isNotNull();
    assertThat(summary.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_AUDITOR);
    assertThat(summary.products).contains("Sonatype " + CLMLicenseManager.PRODUCT_AUDITOR);
  }

  @Test
  public void testGetLicenseSummary_ProductEditionNexusProPlus() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    installLicense();
    LicenseSummary summary = clmLicenseManager.getLicenseSummary();
    assertThat(summary).isNotNull();
    assertThat(summary.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_PRO_PLUS);
    assertThat(summary.products).contains("Sonatype " + CLMLicenseManager.PRODUCT_PRO_PLUS);
  }

  @Test
  public void testGetLicenseSummary_ProductEditionLifecycle() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    installLicense();
    LicenseSummary summary = clmLicenseManager.getLicenseSummary();
    assertThat(summary).isNotNull();
    assertThat(summary.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_LIFECYCLE);
    assertThat(summary.products).contains("Sonatype " + CLMLicenseManager.PRODUCT_LIFECYCLE);
  }

  @Test
  public void testGetLicenseSummary_ProductEditionLifecycleCloud() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD);
    installLicense();
    LicenseSummary summary = clmLicenseManager.getLicenseSummary();
    assertThat(summary).isNotNull();
    assertThat(summary.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_LIFECYCLE_CLOUD);
    assertThat(summary.products).contains("Sonatype " + CLMLicenseManager.PRODUCT_LIFECYCLE_CLOUD);
  }

  @Test
  public void testGetLicenseSummary_ProductEditionLifecycleSaas() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    installLicense();
    LicenseSummary summary = clmLicenseManager.getLicenseSummary();
    assertThat(summary).isNotNull();
    assertThat(summary.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_LIFECYCLE_SAAS);
    assertThat(summary.products).contains("Sonatype " + CLMLicenseManager.PRODUCT_LIFECYCLE_SAAS);
  }

  @Test
  public void testGetLicenseSummary_ProductEditionFirewall() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL);
    installLicense();
    LicenseSummary summary = clmLicenseManager.getLicenseSummary();
    assertThat(summary).isNotNull();
    assertThat(summary.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_FIREWALL);
    assertThat(summary.products).contains("Sonatype " + CLMLicenseManager.PRODUCT_FIREWALL);
  }

  @Test
  public void testGetLicenseSummary_ProductEditionFirewallV2() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL_V2);
    installLicense();
    LicenseSummary summary = clmLicenseManager.getLicenseSummary();
    assertThat(summary).isNotNull();
    assertThat(summary.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_FIREWALL);
    assertThat(summary.products).contains("Sonatype " + CLMLicenseManager.PRODUCT_FIREWALL);
  }

  @Test
  public void testGetLicenseSummary_ProductEditionLifecycleFirewallCloud() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_CLOUD);
    installLicense();
    LicenseSummary summary = clmLicenseManager.getLicenseSummary();
    assertThat(summary).isNotNull();
    assertThat(summary.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_LIFECYCLE_FIREWALL_CLOUD);
    assertThat(summary.products).contains("Sonatype " + CLMLicenseManager.PRODUCT_LIFECYCLE_FIREWALL_CLOUD);
  }

  /**
   * @deprecated This tested code is deprecated
   */
  @Deprecated
  @Test
  public void testGetLicenseSummary_ProductEditionLifecycleFirewallSaas() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_SAAS);
    installLicense();
    LicenseSummary summary = clmLicenseManager.getLicenseSummary();
    assertThat(summary).isNotNull();
    assertThat(summary.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_LIFECYCLE_FIREWALL_SAAS);
    assertThat(summary.products).contains("Sonatype " + CLMLicenseManager.PRODUCT_LIFECYCLE_FIREWALL_SAAS);
  }

  @Test
  public void testGetLicenseSummary_ProductEditionFirewallForArtifactoryV2() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL_FOR_ARTIFACTORY_V2);
    installLicense();
    LicenseSummary summary = clmLicenseManager.getLicenseSummary();
    assertThat(summary).isNotNull();
    assertThat(summary.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_FIREWALL_FOR_ARTIFACTORY);
    assertThat(summary.products).contains("Sonatype " + CLMLicenseManager.PRODUCT_FIREWALL_FOR_ARTIFACTORY);
  }

  @Test
  public void testGetLicenseSummary_ProductEditionLifecycleFoundation() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    installLicense();
    LicenseSummary summary = clmLicenseManager.getLicenseSummary();
    assertThat(summary).isNotNull();
    assertThat(summary.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_LIFECYCLE_FOUNDATION);
    assertThat(summary.products).contains("Sonatype " + CLMLicenseManager.PRODUCT_LIFECYCLE_FOUNDATION);
  }

  @Test
  public void testGetLicenseSummary_ProductEditionTeamsEdition() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_TEAMS_EDITION);
    installLicense();
    LicenseSummary summary = clmLicenseManager.getLicenseSummary();
    assertThat(summary).isNotNull();
    assertThat(summary.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_TEAMS_EDITION);
    assertThat(summary.products).contains("Sonatype " + CLMLicenseManager.PRODUCT_TEAMS_EDITION);
  }

  @Test
  public void testGetLicenseSummary_ProductEditionSbomManager() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
    installLicense();
    LicenseSummary summary = clmLicenseManager.getLicenseSummary();
    assertThat(summary).isNotNull();
    assertThat(summary.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_SBOM_MANAGER);
    assertThat(summary.products).contains("Sonatype " + CLMLicenseManager.PRODUCT_SBOM_MANAGER);
  }

  @Test
  public void testGetLicenseSummary_ProductEditionSbomManagerSaas() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    installLicense();
    LicenseSummary summary = clmLicenseManager.getLicenseSummary();
    assertThat(summary).isNotNull();
    assertThat(summary.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_SBOM_MANAGER_SAAS);
    assertThat(summary.products).contains("Sonatype " + CLMLicenseManager.PRODUCT_SBOM_MANAGER_SAAS);
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
  public void testGetLicenseInfo_ProductEditionNone() {
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
    assertThat(info.products)
        .containsExactlyInAnyOrder(suffix(CLMLicenseManager.PRODUCT_AUDITOR));
  }

  @Test
  public void testGetLicenseInfo_ProductEditionNexusProPlus() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    installLicense();
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info).isNotNull();
    assertThat(info.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_PRO_PLUS);
    assertThat(info.products)
        .containsExactlyInAnyOrder(suffix(CLMLicenseManager.PRODUCT_PRO_PLUS));
  }

  @Test
  public void testGetLicenseInfo_ProductEditionLifecycle() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    installLicense();
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info).isNotNull();
    assertThat(info.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_LIFECYCLE);
    assertThat(info.products)
        .containsExactlyInAnyOrder(suffix(CLMLicenseManager.PRODUCT_LIFECYCLE),
            suffix(CLMLicenseManager.PRODUCT_SONATYPE_DEVELOPMENT));
  }

  @Test
  public void testGetLicenseInfo_ProductEditionLifecycleCloud() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD);
    installLicense();
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info).isNotNull();
    assertThat(info.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_LIFECYCLE_CLOUD);
    assertThat(info.products)
        .containsExactlyInAnyOrder(suffix(CLMLicenseManager.PRODUCT_LIFECYCLE_CLOUD),
            suffix(CLMLicenseManager.PRODUCT_SONATYPE_DEVELOPMENT));
  }

  @Test
  public void testGetLicenseInfo_ProductEditionLifecycleSaas() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    installLicense();
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info).isNotNull();
    assertThat(info.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_LIFECYCLE_SAAS);
    assertThat(info.products)
        .containsExactlyInAnyOrder(suffix(CLMLicenseManager.PRODUCT_LIFECYCLE_SAAS),
            suffix(CLMLicenseManager.PRODUCT_SONATYPE_DEVELOPMENT));
  }

  @Test
  public void testGetLicenseInfo_ProductEditionFirewall() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL);
    installLicense();
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info).isNotNull();
    assertThat(info.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_FIREWALL);
    assertThat(info.products)
        .containsExactlyInAnyOrder(suffix(CLMLicenseManager.PRODUCT_FIREWALL));
  }

  @Test
  public void testGetLicenseInfo_ProductEditionFirewallV2() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL_V2);
    installLicense();
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info).isNotNull();
    assertThat(info.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_FIREWALL);
    assertThat(info.products)
        .containsExactlyInAnyOrder(suffix(CLMLicenseManager.PRODUCT_FIREWALL));
  }

  @Test
  public void testGetLicenseInfo_ProductEditionLifecycleFirewallCloud() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_CLOUD);
    installLicense();
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info).isNotNull();
    assertThat(info.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_LIFECYCLE_FIREWALL_CLOUD);
    assertThat(info.products)
        .containsExactlyInAnyOrder(suffix(CLMLicenseManager.PRODUCT_LIFECYCLE_FIREWALL_CLOUD));
  }

  /**
   * @deprecated This tested code is deprecated
   */
  @Deprecated
  @Test
  public void testGetLicenseInfo_ProductEditionLifecycleFirewallSaas() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_SAAS);
    installLicense();
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info).isNotNull();
    assertThat(info.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_LIFECYCLE_FIREWALL_SAAS);
  }

  @Test
  public void testGetLicenseInfo_ProductEditionFirewallForArtifactoryV2() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL_FOR_ARTIFACTORY_V2);
    installLicense();
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info).isNotNull();
    assertThat(info.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_FIREWALL_FOR_ARTIFACTORY);
    assertThat(info.products)
        .containsExactlyInAnyOrder(suffix(CLMLicenseManager.PRODUCT_FIREWALL_FOR_ARTIFACTORY));
  }

  @Test
  public void testGetLicenseInfo_ProductEditionLifecycleFoundation() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    installLicense();
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info).isNotNull();
    assertThat(info.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_LIFECYCLE_FOUNDATION);
    assertThat(info.products)
        .containsExactlyInAnyOrder(suffix(CLMLicenseManager.PRODUCT_LIFECYCLE_FOUNDATION));
  }

  @Test
  public void testGetLicenseInfo_ProductEditionTeamsEdition() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_TEAMS_EDITION);
    installLicense();
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info).isNotNull();
    assertThat(info.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_TEAMS_EDITION);
    assertThat(info.products)
        .containsExactlyInAnyOrder(suffix(CLMLicenseManager.PRODUCT_TEAMS_EDITION),
            suffix(CLMLicenseManager.PRODUCT_SONATYPE_DEVELOPMENT));
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

    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
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

    // should also be null when it is just Lifecycle Firewall Saas
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_SAAS);
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
        ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_CLOUD, ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_SAAS);
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

    // should not be null when it is just Lifecycle Firewall Saas
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_SAAS);
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
        ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_CLOUD, ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_SAAS);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.firewallUsersToDisplay).isEqualTo(45);

    // should be null when Lifecycle but with null maxFirewallUsers
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    licenseManager.setMaxFirewallUsers(null);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.firewallUsersToDisplay).isNull();

    // should be null when Lifecycle Cloud but with null maxFirewallUsers
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD);
    licenseManager.setMaxFirewallUsers(null);
    installLicense();
    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.firewallUsersToDisplay).isNull();

    // should be null when Lifecycle Saas but with null maxFirewallUsers
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
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

    // should also be null when it is just Lifecycle Firewall Saas
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_SAAS);
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
    licenseManager.setMaxSboms(1234);
    installLicense();

    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info.applicationLimitToDisplay).isEqualTo(100);
    assertThat(info.applicationCountToDisplay).isEqualTo(0);
    assertThat(info.licensedUsersToDisplay).isNull();
    assertThat(info.firewallUsersToDisplay).isNull();
    assertThat(info.sbomLimitToDisplay).isNull();
  }

  @Test
  public void testGetLicenseInfo_LimitsToDisplay_SbomBasedLicensing() throws Exception {
    licenseManager.setProperty(ProductLicenseDetails.PROPERTY_LICENSING_MODEL,
        ProductLicenseDetails.LICENSING_SBOM_BASED);
    licenseManager.setApplicationLimit(100);
    licenseManager.setMaxUsers(8765);
    licenseManager.setMaxFirewallUsers(4321);
    licenseManager.setMaxSboms(50);
    installLicense();

    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info.applicationLimitToDisplay).isNull();
    assertThat(info.applicationCountToDisplay).isNull();
    assertThat(info.licensedUsersToDisplay).isNull();
    assertThat(info.firewallUsersToDisplay).isNull();
    assertThat(info.sbomLimitToDisplay).isEqualTo(50);
  }

  @Test
  public void testGetLicenseInfo_LimitsToDisplay_MultipleLicensing() throws Exception {
    List<String> licensingModels = Arrays.asList(
        ProductLicenseDetails.LICENSING_SBOM_BASED,
        ProductLicenseDetails.LICENSING_APP_BASED,
        ProductLicenseDetails.LICENSING_USER_BASED);

    String licensingModelsString = String.join(",", licensingModels);
    licenseManager.setProperty(ProductLicenseDetails.PROPERTY_LICENSING_MODEL, licensingModelsString);
    licenseManager.setApplicationLimit(100);
    licenseManager.setMaxUsers(8765);
    licenseManager.setMaxFirewallUsers(4321);
    licenseManager.setMaxSboms(50);
    installLicense();

    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info.applicationLimitToDisplay).isEqualTo(100);
    assertThat(info.applicationCountToDisplay).isEqualTo(0);
    assertThat(info.licensedUsersToDisplay).isNotNull();
    assertThat(info.firewallUsersToDisplay).isEqualTo(4321);
    assertThat(info.properties.getProperty("licensingModel")).isEqualTo("sbom-based,app-based,user-based");
    assertThat(info.sbomLimitToDisplay).isEqualTo(50);
  }

  @Test
  public void testGetLicenseInfo_LimitsToDisplay_UserBasedLicensing() throws Exception {
    licenseManager.setProperty(ProductLicenseDetails.PROPERTY_LICENSING_MODEL,
        ProductLicenseDetails.LICENSING_USER_BASED);
    licenseManager.setApplicationLimit(100);
    licenseManager.setMaxUsers(8765);
    licenseManager.setMaxFirewallUsers(null);
    licenseManager.setMaxSboms(1234);
    installLicense();

    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info.applicationLimitToDisplay).isNull();
    assertThat(info.applicationCountToDisplay).isNull();
    assertThat(info.licensedUsersToDisplay).isEqualTo(8765);
    assertThat(info.firewallUsersToDisplay).isNull();
    assertThat(info.sbomLimitToDisplay).isNull();

    licenseManager.setMaxFirewallUsers(4321);
    installLicense();

    info = clmLicenseManager.getLicenseInfo();
    assertThat(info.applicationLimitToDisplay).isNull();
    assertThat(info.applicationCountToDisplay).isNull();
    assertThat(info.licensedUsersToDisplay).isEqualTo(8765);
    assertThat(info.firewallUsersToDisplay).isEqualTo(4321);
    assertThat(info.sbomLimitToDisplay).isNull();
  }

  @Test
  public void testGetLicenseInfo_Products() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL, ProductLicenseDetails.PRODUCT_RISK,
        ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION, "foo", ProductLicenseDetails.PRODUCT_NEXUS,
        ProductLicenseDetails.PRODUCT_ADVANCED_LEGAL_PACK, ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD);

    installLicense();
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info).isNotNull();
    assertThat(info.products).containsExactlyInAnyOrder("Sonatype Repository Firewall", "Sonatype Auditor",
        "Sonatype Lifecycle", "Sonatype Nexus Pro+", "Sonatype Advanced Legal Pack", "Sonatype Lifecycle Cloud",
        "Sonatype Developer");
    Map<String, String> expected = new HashMap<>();
    expected.put("clm.licenseVersion", "1");
    expected.put("clm.maxActiveApplications", "100");
    expected.put("clm.maxFirewallUsers", "45");
    expected.put("clm.maxUsers", "50");
    expected.put("clm.products",
        "Firewall,Risk,RiskAndRemediation,foo,Nexus,AdvancedLegalPack,LifecycleCloud,Development");
    assertThat(info.properties).containsAllEntriesOf(expected);
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
  public void testInstallLicenseIfUnlicensed_FileNotFoundException() {
    clmLicenseManager.uninstallLicense();
    String licenseFilePath = "path/to/license/file";
    assertThatExceptionOfType(FileNotFoundException.class)
        .isThrownBy(() -> clmLicenseManager.installLicenseIfUnlicensed(licenseFilePath))
        .withMessageContaining(new File(licenseFilePath).getPath());
    assertThat(logOutput).atInfoLevel().contains(licenseFilePath);
    assertThat(productLicense.getFingerprint()).isNull();
  }

  @Test
  public void testInstallLicenseIfUnlicensed_LicensingException() {
    licenseManager.setForceVerificationFailure(true);
    clmLicenseManager.uninstallLicense();
    String licenseFilePath = getClass().getClassLoader().getResource("CLMLicenseManagerTest/license.lic").getFile();
    assertThatExceptionOfType(LicensingException.class)
        .isThrownBy(() -> clmLicenseManager.installLicenseIfUnlicensed(licenseFilePath));
    assertThat(logOutput).atInfoLevel().contains(licenseFilePath);
    assertThat(productLicense.getFingerprint()).isNull();
  }

  @Test
  public void testExecute() {
    CLMLicenseManager clmLicenseManagerSpy = spy(clmLicenseManager);
    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(clmLicenseManagerSpy).updateLicenseCacheFromDatabase();

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      clmLicenseManagerSpy.execute(mock(JobExecutionContext.class));
    }

    verify(clmLicenseManagerSpy).updateLicenseCacheFromDatabase();
  }

  @Test
  public void testLoadProductLicenseOnAllOtherClusterNodes() {
    CLMLicenseManager clmLicenseManagerSpy = spy(clmLicenseManager);

    clmLicenseManagerSpy.loadProductLicenseOnAllOtherClusterNodes();

    verify(taskSchedulerMock).scheduleOneTimeTaskForAllOtherNodes(clmLicenseManagerSpy);
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

  @Test
  public void testTenantManagedLicenseListenersAreNotCalled_whenGlobalTenant() {
    ProductLicenseListener listener = mock(TestTenantManagedProductLicenseListener.class);
    clmLicenseManager.addListener(listener);
    try {
      testAsTenant(GLOBAL_TENANT, t -> {
        clmLicenseManager.loadLicense();
        verify(listener, never()).productLicenseChanged();
      });

      testAsNewTenant(testName, t -> {
        clmLicenseManager.loadLicense();
        verify(listener).productLicenseChanged();
      });
    }
    finally {
      clmLicenseManager.removeListener(listener);
    }
  }

  @Test
  public void testGetLicenseInfo_EligibleLifecycleEditionsIncludeDeveloperProduct() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);

    installLicense();
    final LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info).isNotNull();
    assertThat(info.products).containsExactlyInAnyOrder("Sonatype Lifecycle", "Sonatype Developer");
    assertThat(info.properties.getProperty(ProductLicenseDetails.PROPERTY_PRODUCTS).split(","))
        .containsExactlyInAnyOrder("RiskAndRemediation", "Development");
  }

  @Test
  public void testGetLicenseInfo_IneligibleLifecycleEditionsDoNotIncludeDeveloperProduct() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);

    installLicense();
    final LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info).isNotNull();
    assertThat(info.products).containsExactlyInAnyOrder("Sonatype Lifecycle Foundation");
    assertThat(info.properties.getProperty(ProductLicenseDetails.PROPERTY_PRODUCTS).split(","))
        .containsExactlyInAnyOrder("Foundation");
  }

  @Test
  public void testGetLicenseInfo_DoesNotIncludeDuplicateProducts() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL, ProductLicenseDetails.PRODUCT_FIREWALL_V2);

    installLicense();
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info).isNotNull();
    assertThat(info.products).containsExactlyInAnyOrder("Sonatype Repository Firewall");
    assertThat(info.properties.getProperty(ProductLicenseDetails.PROPERTY_PRODUCTS).split(","))
        .containsExactlyInAnyOrder("Firewall", "FirewallV2");
  }

  @Test
  public void testInstallLicense_CurrentLicenseExpired() throws Exception {
    // Install a license that expires in 2 seconds.
    licenseManager.setExpirationDate(new Date(System.currentTimeMillis() + 2000));
    long before = System.currentTimeMillis();
    installLicense();
    // Sanity check
    assertThat(productLicense.isValid()).isTrue();
    // Wait for the license to expire.
    Thread.sleep(2100 - (System.currentTimeMillis() - before));
    // Check that the license expired.
    assertThat(productLicense.isValid()).isFalse();

    // Install a valid license. There should be no errors (even though the current license is expired).
    licenseManager.setExpirationDate(new Date(System.currentTimeMillis() + 20000));
    installLicense();
    assertThat(productLicense.isValid()).isTrue();
  }

  @Test
  public void testHasLifecycleProduct() {
    Stream.of(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION,
        ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS, ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD,
        ProductLicenseDetails.PRODUCT_TEAMS_EDITION)
        .forEach(product -> {
          licenseManager.setProducts(product);
          assertThat(CLMLicenseManager.hasLifecycleProduct(productLicense)).isTrue();
        });

    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    assertThat(CLMLicenseManager.hasLifecycleProduct(productLicense)).isFalse();
  }

  @Test
  public void testHasSbomManagerProduct() {
    Stream.of(ProductLicenseDetails.PRODUCT_SBOM_MANAGER,
        ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS)
        .forEach(product -> {
          licenseManager.setProducts(product);
          assertThat(CLMLicenseManager.hasSbomManagerProduct(productLicense)).isTrue();
        });

    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    assertThat(CLMLicenseManager.hasSbomManagerProduct(productLicense)).isFalse();
  }

  // ---- Tier-based enterprise features tests ----

  @Test
  public void testGetFeatures_Lifecycle_ProTier_NoEnterpriseFeatures() throws Exception {
    systemConfigurationPropertyDAO.insert(
        new SystemConfigurationProperty(SystemConfigurationProperty.LIFECYCLE_TIER, "Pro"));
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).doesNotContain(
        LicensedFeature.CUSTOM_POLICIES,
        LicensedFeature.CUSTOM_APPLICATION_CATEGORIES,
        LicensedFeature.CUSTOM_COMPONENT_LABELS,
        LicensedFeature.CUSTOM_LICENSE_THREAT_GROUPS,
        LicensedFeature.AUTO_WAIVER_MANAGEMENT,
        LicensedFeature.WAIVER_REQUEST_WORKFLOW,
        LicensedFeature.BULK_WAIVERS);
  }

  @Test
  public void testGetFeatures_Lifecycle_EnterpriseTier_HasEnterpriseFeatures() throws Exception {
    systemConfigurationPropertyDAO.insert(
        new SystemConfigurationProperty(SystemConfigurationProperty.LIFECYCLE_TIER, "Enterprise"));
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).contains(
        LicensedFeature.CUSTOM_POLICIES,
        LicensedFeature.CUSTOM_APPLICATION_CATEGORIES,
        LicensedFeature.CUSTOM_COMPONENT_LABELS,
        LicensedFeature.CUSTOM_LICENSE_THREAT_GROUPS,
        LicensedFeature.AUTO_WAIVER_MANAGEMENT,
        LicensedFeature.WAIVER_REQUEST_WORKFLOW,
        LicensedFeature.BULK_WAIVERS);
  }

  @Test
  public void testGetFeatures_Lifecycle_LegacyTier_HasEnterpriseFeatures() throws Exception {
    systemConfigurationPropertyDAO.insert(
        new SystemConfigurationProperty(SystemConfigurationProperty.LIFECYCLE_TIER, "Legacy"));
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).contains(
        LicensedFeature.CUSTOM_POLICIES,
        LicensedFeature.CUSTOM_APPLICATION_CATEGORIES,
        LicensedFeature.CUSTOM_COMPONENT_LABELS,
        LicensedFeature.CUSTOM_LICENSE_THREAT_GROUPS,
        LicensedFeature.AUTO_WAIVER_MANAGEMENT,
        LicensedFeature.WAIVER_REQUEST_WORKFLOW,
        LicensedFeature.BULK_WAIVERS);
  }

  @Test
  public void testGetFeatures_Lifecycle_NullTier_HasEnterpriseFeatures() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).contains(
        LicensedFeature.CUSTOM_POLICIES,
        LicensedFeature.CUSTOM_APPLICATION_CATEGORIES,
        LicensedFeature.CUSTOM_COMPONENT_LABELS,
        LicensedFeature.CUSTOM_LICENSE_THREAT_GROUPS,
        LicensedFeature.AUTO_WAIVER_MANAGEMENT,
        LicensedFeature.WAIVER_REQUEST_WORKFLOW,
        LicensedFeature.BULK_WAIVERS);
  }

  @Test
  public void testGetFeatures_Lifecycle_ProTier_HdsOverride_GrantsEnterpriseFeature() throws Exception {
    systemConfigurationPropertyDAO.insert(
        new SystemConfigurationProperty(SystemConfigurationProperty.LIFECYCLE_TIER, "Pro"));
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    mockHdsProductLicenseDetails(withFeatures(LicensedFeature.CUSTOM_POLICIES));
    installLicense();
    assertThat(productLicense.getFeatures()).contains(LicensedFeature.CUSTOM_POLICIES);
    assertThat(productLicense.getFeatures()).doesNotContain(
        LicensedFeature.CUSTOM_APPLICATION_CATEGORIES,
        LicensedFeature.AUTO_WAIVER_MANAGEMENT);
  }

  @Test
  public void testGetFeatures_Firewall_NotAffectedByTier() throws Exception {
    systemConfigurationPropertyDAO.insert(
        new SystemConfigurationProperty(SystemConfigurationProperty.LIFECYCLE_TIER, "Pro"));
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL);
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).doesNotContain(
        LicensedFeature.CUSTOM_POLICIES,
        LicensedFeature.BULK_WAIVERS);
    assertThat(productLicense.getFeatures()).contains(LicensedFeature.FIREWALL);
  }

  @Test
  public void testGetLicenseSummary_ProTier_ProductEdition() throws Exception {
    systemConfigurationPropertyDAO.insert(
        new SystemConfigurationProperty(SystemConfigurationProperty.LIFECYCLE_TIER, "Pro"));
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    installLicense();
    LicenseSummary summary = clmLicenseManager.getLicenseSummary();
    assertThat(summary.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_LIFECYCLE_PRO);
  }

  @Test
  public void testGetLicenseSummary_EnterpriseTier_ProductEdition() throws Exception {
    systemConfigurationPropertyDAO.insert(
        new SystemConfigurationProperty(SystemConfigurationProperty.LIFECYCLE_TIER, "Enterprise"));
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    installLicense();
    LicenseSummary summary = clmLicenseManager.getLicenseSummary();
    assertThat(summary.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_LIFECYCLE_ENTERPRISE);
  }

  @Test
  public void testGetLicenseSummary_NullTier_ProductEditionIsLegacy() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    installLicense();
    LicenseSummary summary = clmLicenseManager.getLicenseSummary();
    assertThat(summary.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_LIFECYCLE);
  }

  @Test
  public void testHasGuideProduct() {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_GUIDE_SELF_HOSTED);
    assertThat(CLMLicenseManager.hasGuideProduct(productLicense)).isTrue();

    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    assertThat(CLMLicenseManager.hasGuideProduct(productLicense)).isFalse();
  }

  @Test
  public void testGetFeatures_GuideSelfHosted() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_GUIDE_SELF_HOSTED);
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).containsExactlyInAnyOrder(
        LicensedFeature.GUIDE,
        LicensedFeature.GUIDE_MCP,
        LicensedFeature.GUIDE_SEARCH);
  }

  @Test
  public void testGetFeatures_GuideSelfHosted_viaGuideProductsProperty() throws Exception {
    licenseManager.setProducts("");
    licenseManager.setProperty(ProductLicenseDetails.PROPERTY_GUIDE_PRODUCTS,
        ProductLicenseDetails.PRODUCT_GUIDE_SELF_HOSTED);
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).containsExactlyInAnyOrder(
        LicensedFeature.GUIDE,
        LicensedFeature.GUIDE_MCP,
        LicensedFeature.GUIDE_SEARCH);
  }

  @Test
  public void testGetLicenseSummary_ProductEditionGuide() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_GUIDE_SELF_HOSTED);
    installLicense();
    LicenseSummary summary = clmLicenseManager.getLicenseSummary();
    assertThat(summary).isNotNull();
    assertThat(summary.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_GUIDE);
    assertThat(summary.products).contains("Sonatype " + CLMLicenseManager.PRODUCT_GUIDE);
  }

  @Test
  public void testGetLicenseInfo_ProductEditionGuide() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_GUIDE_SELF_HOSTED);
    installLicense();
    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info).isNotNull();
    assertThat(info.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_GUIDE);
    assertThat(info.products).containsExactlyInAnyOrder(suffix(CLMLicenseManager.PRODUCT_GUIDE));
  }

  /**
   * Tests the defensive LEGACY model path for Guide licenses.
   * Guide licenses should always be CREDIT_BASED, but the LEGACY switch handles this defensively.
   */
  @Test
  public void testGetLicenseInfo_GuideWithLegacyModel_surfacesCreditAmount() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_GUIDE_SELF_HOSTED);
    // No licensing model set - defaults to LEGACY
    mockHdsProductLicenseDetails(withCreditAmount(new BigDecimal("500")));
    installLicense();

    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info).isNotNull();
    assertThat(info.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_GUIDE);
    // Defensive LEGACY path should still surface creditAmount
    assertThat(info.creditAmountToDisplay).isEqualTo(new BigDecimal("500"));
  }

  @Test
  public void testGetLicenseInfo_CreditBasedLicensing_withNullCreditAmount() throws Exception {
    licenseManager.setProperty(ProductLicenseDetails.PROPERTY_LICENSING_MODEL,
        ProductLicenseDetails.LICENSING_CREDIT_BASED);
    licenseManager.setApplicationLimit(100);
    installLicense();

    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info).isNotNull();
    assertThat(info.creditAmountToDisplay).isNull();
    assertThat(info.applicationLimitToDisplay).isNull();
    assertThat(info.licensedUsersToDisplay).isNull();
    assertThat(info.firewallUsersToDisplay).isNull();
    assertThat(info.sbomLimitToDisplay).isNull();
  }

  @Test
  public void testGetLicenseInfo_GuideSelfHostedWithCreditBased() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_GUIDE_SELF_HOSTED);
    licenseManager.setProperty(ProductLicenseDetails.PROPERTY_LICENSING_MODEL,
        ProductLicenseDetails.LICENSING_CREDIT_BASED);
    mockHdsProductLicenseDetails(withCreditAmount(new BigDecimal("1000")));
    installLicense();

    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info).isNotNull();
    assertThat(info.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_GUIDE);
    assertThat(info.creditAmountToDisplay).isEqualTo(new BigDecimal("1000"));
    assertThat(info.applicationLimitToDisplay).isNull();
    assertThat(info.licensedUsersToDisplay).isNull();
    assertThat(info.firewallUsersToDisplay).isNull();
    assertThat(info.sbomLimitToDisplay).isNull();
  }

  @Test
  public void testGetLicenseInfo_GuideSelfHostedWithAppAndSbomBased_surfacesCreditAmount() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_GUIDE_SELF_HOSTED);
    List<String> licensingModels = Arrays.asList(
        ProductLicenseDetails.LICENSING_APP_BASED,
        ProductLicenseDetails.LICENSING_SBOM_BASED);
    licenseManager.setProperty(ProductLicenseDetails.PROPERTY_LICENSING_MODEL, String.join(",", licensingModels));
    licenseManager.setApplicationLimit(100);
    licenseManager.setMaxSboms(50);
    mockHdsProductLicenseDetails(withCreditAmount(new BigDecimal("5000")));
    installLicense();

    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info).isNotNull();
    assertThat(info.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_GUIDE);
    assertThat(info.creditAmountToDisplay).isEqualTo(new BigDecimal("5000"));
    assertThat(info.applicationLimitToDisplay).isEqualTo(100);
    assertThat(info.sbomLimitToDisplay).isEqualTo(50);
  }

  @Test
  public void testGetLicenseInfo_GuideSelfHostedWithAppAndSbomBased_noCreditAmount() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_GUIDE_SELF_HOSTED);
    List<String> licensingModels = Arrays.asList(
        ProductLicenseDetails.LICENSING_APP_BASED,
        ProductLicenseDetails.LICENSING_SBOM_BASED);
    licenseManager.setProperty(ProductLicenseDetails.PROPERTY_LICENSING_MODEL, String.join(",", licensingModels));
    licenseManager.setApplicationLimit(100);
    licenseManager.setMaxSboms(50);
    mockHdsProductLicenseDetails();
    installLicense();

    LicenseInfo info = clmLicenseManager.getLicenseInfo();
    assertThat(info).isNotNull();
    assertThat(info.productEdition).isEqualTo(CLMLicenseManager.PRODUCT_GUIDE);
    assertThat(info.creditAmountToDisplay).isNull();
    assertThat(info.applicationLimitToDisplay).isEqualTo(100);
    assertThat(info.sbomLimitToDisplay).isEqualTo(50);
  }

  @Test
  public void testGetCreditAmount() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_GUIDE_SELF_HOSTED);
    mockHdsProductLicenseDetails(withCreditAmount(new BigDecimal("1000")));
    installLicense();
    assertThat(creditAwareProductLicense.getCreditAmount()).isEqualTo(new BigDecimal("1000"));
  }

  @Test
  public void testGetStageTypes_GuideSelfHosted() throws Exception {
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_GUIDE_SELF_HOSTED);
    mockHdsProductLicenseDetails(withStages());
    installLicense();

    assertThat(productLicense.getStageTypes()).containsExactlyInAnyOrder(
        StageTypes.DEVELOP,
        StageTypes.PROXY);
  }

  @Test
  public void testGuideSelfHosted_allFeaturesRejected_throwsException() {
    clmLicenseManager.uninstallLicense();
    licenseManager.setForceVerificationFailure(true);
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_GUIDE_SELF_HOSTED);
    assertThatExceptionOfType(LicensingException.class).isThrownBy(this::installLicense)
        .withMessage("License does not permit use of feature '" + CLMFeature.ID + "', '"
            + FirewallFeature.ID + "', or '" + GuideFeature.ID + "'");
  }

  @Test
  public void testGuideSelfHosted_guideOnlyFeature_accepted() throws Exception {
    licenseManager.setAllowedFeatureIds(GuideFeature.ID);
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_GUIDE_SELF_HOSTED);
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).containsExactlyInAnyOrder(
        LicensedFeature.GUIDE,
        LicensedFeature.GUIDE_MCP,
        LicensedFeature.GUIDE_SEARCH);
  }

  @Test
  public void testGuideSelfHosted_clmFeature_accepted() throws Exception {
    licenseManager.setAllowedFeatureIds(CLMFeature.ID);
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_GUIDE_SELF_HOSTED);
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).containsExactlyInAnyOrder(
        LicensedFeature.GUIDE,
        LicensedFeature.GUIDE_MCP,
        LicensedFeature.GUIDE_SEARCH);
  }

  @Test
  public void testGuideSelfHosted_firewallFeature_accepted() throws Exception {
    licenseManager.setAllowedFeatureIds(FirewallFeature.ID);
    licenseManager.setProducts(ProductLicenseDetails.PRODUCT_GUIDE_SELF_HOSTED);
    mockHdsProductLicenseDetails(withFeatures());
    installLicense();
    assertThat(productLicense.getFeatures()).containsExactlyInAnyOrder(
        LicensedFeature.GUIDE,
        LicensedFeature.GUIDE_MCP,
        LicensedFeature.GUIDE_SEARCH);
  }

  private static String suffix(final String suffix) {
    return "Sonatype " + suffix;
  }

  private static class TestTenantManagedProductLicenseListener
      implements TenantManaged, ProductLicenseListener
  {
    @Override
    public void productLicenseChanged() {
      // no-op
    }
  }
}
