/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.Signature;
import java.security.cert.Certificate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.tenancy.GlobalTenantJob;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.license.model.SignedProductLicenseDetailsDTO;
import org.sonatype.licensing.LicensingException;
import org.sonatype.licensing.product.ProductLicenseKey;
import org.sonatype.licensing.product.ProductLicenseManager;
import org.sonatype.licensing.product.util.LicenseContent;
import org.sonatype.licensing.product.util.LicenseFingerprinter;
import org.sonatype.licensing.util.LicensingUtil;

import com.google.common.io.ByteStreams;
import de.schlichtherle.license.NoLicenseInstalledException;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@DisallowConcurrentExecution
public class CLMLicenseManager
    implements InsightJob, GlobalTenantJob
{
  public static final String PRODUCT_PRO_PLUS = "Pro+";

  public static final String PRODUCT_LIFECYCLE = "Lifecycle";

  public static final String PRODUCT_LIFECYCLE_FOUNDATION = "Lifecycle Foundation";

  public static final String PRODUCT_FIREWALL = "Firewall";

  public static final String PRODUCT_FIREWALL_FOR_ARTIFACTORY = "Firewall for Artifactory";

  public static final String PRODUCT_AUDITOR = "Auditor";

  public static final String MIGRATION_TRACKER_EXTERNAL_DB = "external-database";

  public static final String PRODUCT_ADVANCED_DEVELOPMENT_PACK = "Advanced Development Pack";

  public static final String PRODUCT_ADVANCED_LEGAL_PACK = "Advanced Legal Pack";

  public static final String PRODUCT_INFRASTRUCTURE_AS_CODE_PACK = "Infrastructure as Code Pack";

  public static final String PRODUCT_LIFECYCLE_CLOUD = "Lifecycle Cloud";

  public static final String PRODUCT_LIFECYCLE_FIREWALL_CLOUD = "Lifecycle Firewall Cloud";

  // Visible for testing
  static final String TASK_NAME = "ProductLicenseLoad";

  private static final String LICENSE_LOADING_ERROR = "Error when loading the product license";

  private final InsightConfig config;

  private final MigrationTrackerDAO migrationTrackerDAO;

  private final ProductLicense productLicense;

  private final ProductLicenseDetailsCache productLicenseDetailsCache;

  private final ProductLicenseManager licenseManager;

  private final LicenseFingerprinter licenseFingerprinter;

  private final LicenseContent licenseContent;

  private final HdsClient hdsClient;

  private static final Logger log = LoggerFactory.getLogger(CLMLicenseManager.class);

  private final List<ProductLicenseListener> listeners = new CopyOnWriteArrayList<>();

  private final AuditRecorder auditRecorder;

  private final TaskScheduler taskScheduler;

  @Inject
  public CLMLicenseManager(
      final InsightConfig config,
      final MigrationTrackerDAO migrationTrackerDAO,
      final ProductLicense productLicense,
      final ProductLicenseDetailsCache productLicenseDetailsCache,
      final ProductLicenseManager licenseManager,
      final LicenseFingerprinter licenseFingerprinter,
      final LicenseContent licenseContent,
      final HdsClient hdsClient,
      final AuditRecorder auditRecorder,
      final TaskScheduler taskScheduler)
  {
    this.config = config;
    this.migrationTrackerDAO = migrationTrackerDAO;
    this.productLicense = productLicense;
    this.productLicenseDetailsCache = productLicenseDetailsCache;
    this.licenseManager = licenseManager;
    this.licenseFingerprinter = licenseFingerprinter;
    this.licenseContent = licenseContent;
    this.hdsClient = hdsClient;
    this.auditRecorder = auditRecorder;
    this.taskScheduler = taskScheduler;
  }

  public void loadLicense() {
    try {
      ProductLicenseKey licenseKey = licenseManager.getLicenseDetails();
      String licenseFingerprint = licenseFingerprinter.calculate(licenseKey);
      byte[] licenseData = licenseContent.raw();
      SignedProductLicenseDetailsDTO licenseDetails;
      try {
        licenseDetails = queryLicenseDetailsFromHds(licenseData, licenseFingerprint);
        productLicenseDetailsCache.setProductLicenseDetails(licenseDetails);
      }
      catch (RuntimeException hdsException) {
        log.info("Could not retrieve current license details, falling back to local cache: {}",
            hdsException.getMessage(), log.isDebugEnabled() ? hdsException : null);
        try {
          licenseDetails = productLicenseDetailsCache.getProductLicenseDetails();
          verifySignature(licenseDetails, licenseFingerprint);
        }
        catch (LicensingException cacheException) {
          hdsException.addSuppressed(cacheException);
          throw hdsException;
        }
      }
      populateLicenseCache(licenseKey, licenseDetails, true);
    }
    catch (RuntimeException e) {
      if (e.getCause() instanceof NoLicenseInstalledException) {
        log.info("No license installed", log.isDebugEnabled() ? e : null);
      }
      else {
        log.error("Unable to load license details, a valid license needs to be installed", e);
      }
      clearLicenseCache(true);
      return;
    }
    if (!config.isDatabaseEmbedded() && !migrationTrackerDAO.isTrackerPresent(MIGRATION_TRACKER_EXTERNAL_DB)) {
      if (!productLicense.hasFeature(LicensedFeature.EXTERNAL_DATABASE)) {
        throw new ExternalDatabaseNotSupportedException(
            "The product license does not support use of an external database"
                + ", please reconfigure IQ Server to use the embedded database.");
      }
      recordSupportForExternalDatabase();
    }
  }

  //visible for testing
  void updateLicenseCacheFromDatabase() {
    try {
      SignedProductLicenseDetailsDTO licenseDetails = productLicenseDetailsCache.getProductLicenseDetails();
      if (licenseDetails == null) {
        clearLicenseCache(false);
      }
      else {
        ProductLicenseKey licenseKey = licenseManager.getLicenseDetails();
        //not verifying signature intentionally. This is expected to be used only to update the cache
        // from the database which is already verified.
        populateLicenseCache(licenseKey, licenseDetails, false);
      }
    }
    catch (RuntimeException e) {
      log.error("Unable to update product license cache from the database", e);
    }
  }

  private void recordSupportForExternalDatabase() {
    MigrationTracker tracker = new MigrationTracker(MIGRATION_TRACKER_EXTERNAL_DB);
    tracker.setConfiguration(productLicense.getFingerprint()); // pointer to the license that enabled it
    migrationTrackerDAO.insert(tracker);
  }

  public void installLicenseIfUnlicensed(String licenseFilePath) throws IOException {
    if (licenseFilePath == null) {
      return;
    }
    if (productLicense.getFingerprint() != null) {
      log.warn("A license is already installed, ignoring {}.", licenseFilePath);
      return;
    }
    log.info("Installing license {}.", licenseFilePath);
    try (AuditSession auditSession = auditRecorder.recordSystemEvent(AuditEvent.INSTALL_LICENSE)) {
      try (FileInputStream fileInputStream = new FileInputStream(licenseFilePath)) {
        installLicense(fileInputStream);
        auditLicense(new File(licenseFilePath).getName());
      }
      catch (Throwable t) {
        AuditData.get().setException(t);
        throw t;
      }
    }
  }

  public synchronized void installLicense(InputStream is) throws IOException {
    byte[] licenseData = ByteStreams.toByteArray(is);
    ProductLicenseKey licenseKey = licenseManager.getLicenseDetails(new ByteArrayInputStream(licenseData));
    String licenseFingerprint = licenseFingerprinter.calculate(licenseKey);
    SignedProductLicenseDetailsDTO licenseDetails = queryLicenseDetailsFromHds(licenseData, licenseFingerprint);
    if (!config.isDatabaseEmbedded() && !licenseDetails.features.contains(LicensedFeature.EXTERNAL_DATABASE.name())
        && !migrationTrackerDAO.isTrackerPresent(MIGRATION_TRACKER_EXTERNAL_DB)) {
      throw new ExternalDatabaseNotSupportedException("The product license does not support use of an external database"
          + ", please reconfigure IQ Server to use the embedded database before installing the license.");
    }
    licenseManager.installLicense(new ByteArrayInputStream(licenseData));
    productLicenseDetailsCache.setProductLicenseDetails(licenseDetails);
    populateLicenseCache(licenseKey, licenseDetails, true);
    log.info("License installed successfully");
    if (!config.isDatabaseEmbedded() && !migrationTrackerDAO.isTrackerPresent(MIGRATION_TRACKER_EXTERNAL_DB)) {
      recordSupportForExternalDatabase();
    }
  }

  private SignedProductLicenseDetailsDTO queryLicenseDetailsFromHds(byte[] licenseData, String licenseFingerprint) {
    SignedProductLicenseDetailsDTO licenseDetails =
        hdsClient.post(SignedProductLicenseDetailsDTO.class, "rest/productLicense/v1", licenseData);
    verifySignature(licenseDetails, licenseFingerprint);
    return licenseDetails;
  }

  private void verifySignature(SignedProductLicenseDetailsDTO licenseDetails, String licenseFingerprint) {
    try {
      Signature signature = Signature.getInstance("SHA256withRSA");
      signature.initVerify(loadCertificateForSignatureVerification(licenseDetails.signatureKeyAlias));
      for (String feature : licenseDetails.features) {
        signature.update(feature.getBytes(StandardCharsets.UTF_8));
      }
      for (String stageId : licenseDetails.stageIds) {
        signature.update(stageId.getBytes(StandardCharsets.UTF_8));
      }
      signature.update((licenseDetails.maxApplications == null ? "0" : licenseDetails.maxApplications.toString())
          .getBytes(StandardCharsets.UTF_8));
      signature.update(licenseFingerprint.getBytes(StandardCharsets.UTF_8));
      if (!signature.verify(licenseDetails.signature)) {
        throw new Exception("Signature mismatch");
      }
    }
    catch (Exception e) {
      throw new LicensingException(
          "Could not verify signature of license details with fingerprint " + licenseFingerprint, e);
    }
  }

  private Certificate loadCertificateForSignatureVerification(String keyAlias) {
    Certificate certificate;
    try {
      KeyStore keyStore = KeyStore.getInstance("pkcs12");
      keyStore.load(getClass().getResourceAsStream("licensing-keystore.p12"), LicensingUtil
          .unobfuscate(new long[]{0xA8874A6C58A5CD5BL, 0xDADEE6943E19F478L, 0x34D18D0FE23233C2L}).toCharArray());
      certificate = keyStore.getCertificate(keyAlias);
    }
    catch (Exception e) {
      throw new IllegalStateException("Could not load certificates for signature verification", e);
    }
    if (certificate == null) {
      throw new IllegalStateException("Could not load certificate " + keyAlias + " for signature verification");
    }
    return certificate;
  }

  public synchronized void uninstallLicense() {
    licenseManager.uninstallLicense();
    clearLicenseCache(true);
    log.info("License uninstalled successfully");
  }

  void auditLicense(String filename) {
    String productLicenseExpiry = ZonedDateTime
        .ofInstant(Instant.ofEpochMilli(productLicense.getExpirationTimestamp()), ZoneId.systemDefault())
        .format(DateTimeFormatter.ISO_LOCAL_DATE);
    AuditData.get().setData("productLicenseFingerprint", productLicense.getFingerprint())
        .setData("productLicenseFilename", filename).setData("productLicenseExpiry", productLicenseExpiry);
  }

  /**
   * A function to map from product names stored in the license to product names suitable for display to the end-user
   */
  private static String getProductMarketingName(String internalName) {
    String marketingNameSuffix;

    switch (internalName) {
      case ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION:
        marketingNameSuffix = PRODUCT_LIFECYCLE;
        break;
      case ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD:
        marketingNameSuffix = PRODUCT_LIFECYCLE_CLOUD;
        break;
      case ProductLicenseDetails.PRODUCT_FOUNDATION:
        marketingNameSuffix = PRODUCT_LIFECYCLE_FOUNDATION;
        break;
      case ProductLicenseDetails.PRODUCT_FIREWALL:
      case ProductLicenseDetails.PRODUCT_FIREWALL_V2:
        marketingNameSuffix = PRODUCT_FIREWALL;
        break;
      case ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_CLOUD:
        marketingNameSuffix = PRODUCT_LIFECYCLE_FIREWALL_CLOUD;
        break;
      case ProductLicenseDetails.PRODUCT_FIREWALL_FOR_ARTIFACTORY:
      case ProductLicenseDetails.PRODUCT_FIREWALL_FOR_ARTIFACTORY_V2:
        marketingNameSuffix = PRODUCT_FIREWALL_FOR_ARTIFACTORY;
        break;
      case ProductLicenseDetails.PRODUCT_NEXUS:
        marketingNameSuffix = PRODUCT_PRO_PLUS;
        break;
      case ProductLicenseDetails.PRODUCT_RISK:
        marketingNameSuffix = PRODUCT_AUDITOR;
        break;
      case ProductLicenseDetails.PRODUCT_ADVANCED_DEVELOPMENT_PACK:
        marketingNameSuffix = PRODUCT_ADVANCED_DEVELOPMENT_PACK;
        break;
      case ProductLicenseDetails.PRODUCT_INFRASTRUCTURE_AS_CODE_PACK:
        marketingNameSuffix = PRODUCT_INFRASTRUCTURE_AS_CODE_PACK;
        break;
      case ProductLicenseDetails.PRODUCT_ADVANCED_LEGAL_PACK:
        marketingNameSuffix = PRODUCT_ADVANCED_LEGAL_PACK;
        break;
      default:
        return null;
    }

    return "Nexus " + marketingNameSuffix;
  }

  public LicenseSummary getLicenseSummary() {
    return new LicenseSummary(getProductEdition());
  }

  public LicenseInfo getLicenseInfo() {
    String[] products = productLicense.getProducts().stream() //
        .map(CLMLicenseManager::getProductMarketingName) //
        .filter(Objects::nonNull) //
        .toArray(String[]::new);

    String productEdition = getProductEdition();
    ProductLicensingModel licensingModel = productLicense.getLicensingModel();
    Integer applicationLimitToDisplay = null;
    Integer applicationCountToDisplay = null;
    Integer licensedUsersToDisplay = null;
    Integer firewallUsersToDisplay = null;

    switch (licensingModel) {
      case LEGACY:
        switch (productEdition) {
          case PRODUCT_AUDITOR:
            applicationLimitToDisplay = productLicense.getMaxApplications();
            break;
          case PRODUCT_PRO_PLUS:
            licensedUsersToDisplay = productLicense.getMaxUsers();
            break;
          case PRODUCT_LIFECYCLE:
          case PRODUCT_LIFECYCLE_CLOUD:
            // fallthrough
          case PRODUCT_LIFECYCLE_FOUNDATION:
            licensedUsersToDisplay = productLicense.getMaxUsers();
            // fallthrough
          case PRODUCT_FIREWALL:
          case PRODUCT_LIFECYCLE_FIREWALL_CLOUD:
            firewallUsersToDisplay = productLicense.getMaxFirewallUsers();
            break;
          default:
            // no limits to display
        }
        break;
      case APP_BASED:
        applicationLimitToDisplay = productLicense.getMaxApplications();
        break;
      case USER_BASED:
        licensedUsersToDisplay = productLicense.getMaxUsers();
        firewallUsersToDisplay = productLicense.getMaxFirewallUsers();
        break;
      default:
        throw new IllegalStateException("Unknown licensing model: " + licensingModel);
    }

    if (applicationLimitToDisplay != null) {
      applicationCountToDisplay = new ApplicationDAO().getCount();
    }

    return new LicenseInfo(productLicense.getFingerprint(), productLicense.getExpirationTimestamp(),
        licensedUsersToDisplay, firewallUsersToDisplay, applicationLimitToDisplay, applicationCountToDisplay,
        productLicense.getContactName(), productLicense.getContactCompany(), productLicense.getContactEmail(), products,
        productEdition);
  }

  private String getProductEdition() {
    Set<String> products = productLicense.getProducts();
    if (products.contains(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION)) {
      return PRODUCT_LIFECYCLE;
    }
    else if (products.contains(ProductLicenseDetails.PRODUCT_FOUNDATION)) {
      return PRODUCT_LIFECYCLE_FOUNDATION;
    }
    else if (products.contains(ProductLicenseDetails.PRODUCT_FIREWALL)) {
      return PRODUCT_FIREWALL;
    }
    else if (products.contains(ProductLicenseDetails.PRODUCT_FIREWALL_FOR_ARTIFACTORY)) {
      return PRODUCT_FIREWALL_FOR_ARTIFACTORY;
    }
    else if (products.contains(ProductLicenseDetails.PRODUCT_FIREWALL_V2)) {
      return PRODUCT_FIREWALL;
    }
    else if (products.contains(ProductLicenseDetails.PRODUCT_FIREWALL_FOR_ARTIFACTORY_V2)) {
      return PRODUCT_FIREWALL_FOR_ARTIFACTORY;
    }
    else if (products.contains(ProductLicenseDetails.PRODUCT_NEXUS)) {
      return PRODUCT_PRO_PLUS;
    }
    else if (products.contains(ProductLicenseDetails.PRODUCT_RISK)) {
      return PRODUCT_AUDITOR;
    }
    else if (products.contains(ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD)) {
      return PRODUCT_LIFECYCLE_CLOUD;
    }
    else if (products.contains(ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_CLOUD)) {
      return PRODUCT_LIFECYCLE_FIREWALL_CLOUD;
    }

    return "";
  }

  private void validateFeatures(final ProductLicenseKey key) {
    try {
      licenseManager.verifyFeature(key, new CLMFeature());
    }
    catch (LicensingException ex) {
      try {
        licenseManager.verifyFeature(key, new FirewallFeature());
      }
      catch (LicensingException nestedEx) {
        throw new LicensingException("License does not permit use of feature '" + CLMFeature.ID + "' or '"
            + FirewallFeature.ID + "'");
      }
    }
  }

  private void populateLicenseCache(
      ProductLicenseKey key,
      SignedProductLicenseDetailsDTO licenseDetails,
      boolean triggerOnOtherNodes)
  {
    validateFeatures(key);

    String licenseFingerprint = licenseFingerprinter.calculate(key);

    int version = getVersion(key);
    if (version < 1) {
      // legacy license without product info
      throw new LicensingException("Invalid license version: " + version);
    }

    ProductLicensingModel licensingModel = getLicensingModel(key);
    Integer applicationCount = licenseDetails.maxApplications;
    Integer maxFirewallUsers = getMaxFirewallUsers(key);
    Integer maxUsers = getMaxUsers(key);

    Set<String> products = getProducts(key);

    Set<LicensedFeature> features = EnumSet.noneOf(LicensedFeature.class);
    Set<StageType> stageTypes = new LinkedHashSet<>();
    if (products.contains(ProductLicenseDetails.PRODUCT_RISK)) {
      features.add(LicensedFeature.POLICY_MONITORING);
      features.add(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS);
      features.add(LicensedFeature.DASHBOARD);
      features.add(LicensedFeature.CLI_INTEGRATION);
      features.add(LicensedFeature.ENFORCEMENT);
      features.add(LicensedFeature.NOTIFICATIONS);
      features.add(LicensedFeature.POLICY_GRANDFATHERING);
      features.add(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS);
      features.add(LicensedFeature.RM_STAGING_INTEGRATION);
      stageTypes.add(StageTypes.RELEASE);
    }
    if (products.contains(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION)) {
      features.add(LicensedFeature.QUALITY);
      features.add(LicensedFeature.POLICY_MONITORING);
      features.add(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS);
      features.add(LicensedFeature.DASHBOARD);
      features.add(LicensedFeature.CLI_INTEGRATION);
      features.add(LicensedFeature.ENFORCEMENT);
      features.add(LicensedFeature.NOTIFICATIONS);
      features.add(LicensedFeature.POLICY_GRANDFATHERING);
      features.add(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS);
      features.add(LicensedFeature.IDE_INTEGRATION);
      features.add(LicensedFeature.CI_INTEGRATION);
      features.add(LicensedFeature.RM_STAGING_INTEGRATION);
      features.add(LicensedFeature.AUTOMATION);
      features.add(LicensedFeature.HYGIENE);
      features.add(LicensedFeature.BREAKING_CHANGE);
      features.add(LicensedFeature.RELEASE_INTEGRITY);
      features.add(LicensedFeature.ADVANCED_RECOMMENDATION_STRATEGIES);
      stageTypes.addAll(StageTypes.getAll());
    }
    if (products.contains(ProductLicenseDetails.PRODUCT_NEXUS)) {
      features.add(LicensedFeature.ENFORCEMENT);
      features.add(LicensedFeature.NOTIFICATIONS);
      features.add(LicensedFeature.POLICY_GRANDFATHERING);
      features.add(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS);
      features.add(LicensedFeature.RM_STAGING_INTEGRATION);
      stageTypes.add(StageTypes.STAGE_RELEASE);
      stageTypes.add(StageTypes.RELEASE);
    }
    if (products.contains(ProductLicenseDetails.PRODUCT_FOUNDATION)) {
      features.add(LicensedFeature.DASHBOARD);
      features.add(LicensedFeature.CLI_INTEGRATION);
      features.add(LicensedFeature.CI_INTEGRATION);
      features.add(LicensedFeature.RM_STAGING_INTEGRATION);
      features.add(LicensedFeature.QUALITY);
      stageTypes.addAll(StageTypes.getAll());
    }
    if (products.contains(ProductLicenseDetails.PRODUCT_FIREWALL) ||
        products.contains(ProductLicenseDetails.PRODUCT_FIREWALL_FOR_ARTIFACTORY)) {
      features.add(LicensedFeature.FIREWALL);
      features.add(LicensedFeature.FIREWALL_FOR_ARTIFACTORY);
      features.add(LicensedFeature.RM_STAGING_INTEGRATION);
      features.add(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES);
      features.add(LicensedFeature.WEBHOOKS_FOR_REPOSITORIES);
      stageTypes.add(StageTypes.STAGE_RELEASE);
      stageTypes.add(StageTypes.RELEASE);
    }
    if (products.contains(ProductLicenseDetails.PRODUCT_FIREWALL_V2) ||
        products.contains(ProductLicenseDetails.PRODUCT_FIREWALL_FOR_ARTIFACTORY_V2)) {
      features.add(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE);
      features.add(LicensedFeature.RELEASE_INTEGRITY);
      features.add(LicensedFeature.FIREWALL);
      features.add(LicensedFeature.FIREWALL_FOR_ARTIFACTORY);
      features.add(LicensedFeature.RM_STAGING_INTEGRATION);
      features.add(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES);
      features.add(LicensedFeature.WEBHOOKS_FOR_REPOSITORIES);
      stageTypes.add(StageTypes.STAGE_RELEASE);
      stageTypes.add(StageTypes.RELEASE);
    }
    if (products.contains(ProductLicenseDetails.PRODUCT_ADVANCED_LEGAL_PACK)) {
      features.add(LicensedFeature.ADVANCED_LEGAL_PACK);
    }
    if (products.contains(ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD)) {
      features.add(LicensedFeature.QUALITY);
      features.add(LicensedFeature.POLICY_MONITORING);
      features.add(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS);
      features.add(LicensedFeature.DASHBOARD);
      features.add(LicensedFeature.CLI_INTEGRATION);
      features.add(LicensedFeature.ENFORCEMENT);
      features.add(LicensedFeature.NOTIFICATIONS);
      features.add(LicensedFeature.POLICY_GRANDFATHERING);
      features.add(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS);
      features.add(LicensedFeature.IDE_INTEGRATION);
      features.add(LicensedFeature.CI_INTEGRATION);
      features.add(LicensedFeature.RM_STAGING_INTEGRATION);
      features.add(LicensedFeature.AUTOMATION);
      features.add(LicensedFeature.IP_ALLOWLIST);
      stageTypes.addAll(StageTypes.getAll());
    }
    if (products.contains(ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_CLOUD)) {
      features.add(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE);
      features.add(LicensedFeature.RELEASE_INTEGRITY);
      features.add(LicensedFeature.FIREWALL);
      features.add(LicensedFeature.FIREWALL_FOR_ARTIFACTORY);
      features.add(LicensedFeature.RM_STAGING_INTEGRATION);
      features.add(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES);
      features.add(LicensedFeature.WEBHOOKS_FOR_REPOSITORIES);
      features.add(LicensedFeature.IP_ALLOWLIST);
      stageTypes.add(StageTypes.STAGE_RELEASE);
      stageTypes.add(StageTypes.RELEASE);
    }

    stageTypes.add(StageTypes.PROXY);

    Set<LicensedFeature> hdsControlledFeatures = EnumSet.of( //
        LicensedFeature.ADVANCED_RECOMMENDATION_STRATEGIES, //
        LicensedFeature.EXTERNAL_DATABASE, //
        LicensedFeature.HYGIENE, //
        LicensedFeature.RELEASE_INTEGRITY, //
        LicensedFeature.NODE_CLUSTERING, //
        LicensedFeature.ADVANCED_LEGAL_PACK, //
        LicensedFeature.DATA_INSIGHTS, //
        LicensedFeature.INFRASTRUCTURE_AS_CODE_PACK, //
        LicensedFeature.BREAKING_CHANGE
    );
    for (LicensedFeature feature : hdsControlledFeatures) {
      if (licenseDetails.features.contains(feature.name())) {
        features.add(feature);
      }
    }

    if (triggerOnOtherNodes) {
      loadProductLicenseOnAllOtherClusterNodes();
    }
    productLicense.set(key, licenseFingerprint, products, features, stageTypes, licensingModel, applicationCount,
        maxUsers, maxFirewallUsers);
    notifyListeners();
  }

  private String getProperty(ProductLicenseKey key, String property) {
    return key.getProperties().getProperty(property);
  }

  private int getVersion(ProductLicenseKey key) {
    String prop = getProperty(key, ProductLicenseDetails.PROPERTY_VERSION);
    try {
      return Integer.parseInt(prop);
    }
    catch (IllegalArgumentException e) {
      throw new LicensingException("Invalid license version: " + prop, e);
    }
  }

  private Set<String> getProducts(ProductLicenseKey key) {
    Set<String> products = new LinkedHashSet<>();
    String value = getProperty(key, ProductLicenseDetails.PROPERTY_PRODUCTS);
    if (value != null) {
      Collections.addAll(products, value.split("\\s*,\\s*"));
    }
    return products;
  }

  private ProductLicensingModel getLicensingModel(ProductLicenseKey key) {
    String prop = getProperty(key, ProductLicenseDetails.PROPERTY_LICENSING_MODEL);
    if (ProductLicenseDetails.LICENSING_APP_BASED.equals(prop)) {
      return ProductLicensingModel.APP_BASED;
    }
    else if (ProductLicenseDetails.LICENSING_USER_BASED.equals(prop)) {
      return ProductLicensingModel.USER_BASED;
    }
    else if (prop == null) {
      return ProductLicensingModel.LEGACY;
    }
    throw new LicensingException("Invalid licensing model: " + prop);
  }

  private Integer getMaxUsers(ProductLicenseKey key) {
    String prop = getProperty(key, ProductLicenseDetails.PROPERTY_MAX_USERS);
    try {
      return prop != null ? Integer.decode(prop) : null;
    }
    catch (IllegalArgumentException e) {
      throw new LicensingException("Invalid value for max users: " + prop, e);
    }
  }

  private Integer getMaxFirewallUsers(ProductLicenseKey key) {
    String prop = getProperty(key, ProductLicenseDetails.PROPERTY_MAX_FIREWALL_USERS);
    try {
      return prop != null ? Integer.decode(prop) : null;
    }
    catch (IllegalArgumentException e) {
      throw new LicensingException("Invalid value for max firewall users: " + prop, e);
    }
  }

  private void clearLicenseCache(boolean triggerOnOtherNodes) {
    if (triggerOnOtherNodes) {
      loadProductLicenseOnAllOtherClusterNodes();
    }
    productLicense.clear();
    notifyListeners();
  }

  /**
   * Registers the specified listener to be notified of changes to the license.
   *
   * @since 1.9
   */
  public void addListener(ProductLicenseListener listener) {
    if (listener == null) {
      throw new IllegalArgumentException("listener not specified");
    }
    listeners.add(listener);
    log.debug("Added listener {}", listener);
  }

  /**
   * Unregisters the specified listener.
   *
   * @since 1.9
   */
  public void removeListener(ProductLicenseListener listener) {
    listeners.remove(listener);
    log.debug("Removed listener {}", listener);
  }

  private void notifyListeners() {
    for (ProductLicenseListener listener : listeners) {
      try {
        if (listener instanceof TenantManaged && new TenantUtil().isGlobalTenant()) {
          // TenantManaged listeners should not be called in the context of the Global tenant
          continue;
        }

        log.debug("Notifying listener {}", listener);
        listener.productLicenseChanged();
      }
      catch (RuntimeException e) {
        log.warn("Failed to notify {} of license update", listener, e);
      }
    }
  }

  // Visible for testing
  void loadProductLicenseOnAllOtherClusterNodes() {
    taskScheduler.scheduleOneTimeTaskForAllOtherNodes(getClass(), TASK_NAME);
  }

  @Override
  public void execute(JobExecutionContext context) {
    execute(this::updateLicenseCacheFromDatabase, log, LICENSE_LOADING_ERROR);
  }
}
