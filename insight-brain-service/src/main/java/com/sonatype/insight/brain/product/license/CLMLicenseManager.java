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
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.Signature;
import java.security.cert.Certificate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.developer.integrationdashboard.DeveloperEnablementService;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.FIPSModeDetector;
import com.sonatype.insight.brain.security.certificate.CertificateFactory;
import com.sonatype.insight.brain.security.keystore.KeyStoreFactory;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.tenancy.GlobalTenantJob;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.license.model.SignedProductLicenseDetailsDTO;
import org.sonatype.licensing.LicensingException;
import org.sonatype.licensing.feature.AbstractFeature;
import org.sonatype.licensing.product.ProductLicenseKey;
import org.sonatype.licensing.product.ProductLicenseManager;
import org.sonatype.licensing.product.util.LicenseContent;
import org.sonatype.licensing.product.util.LicenseFingerprinter;
import org.sonatype.licensing.util.LicensingUtil;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import de.schlichtherle.license.NoLicenseInstalledException;
import org.apache.commons.lang3.StringUtils;
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
  public static final String PRODUCT_PRO_PLUS = "Nexus Pro+";

  public static final String PRODUCT_LIFECYCLE = "Lifecycle";

  public static final String PRODUCT_LIFECYCLE_FOUNDATION = "Lifecycle Foundation";

  public static final String PRODUCT_FIREWALL = "Repository Firewall";

  public static final String PRODUCT_FIREWALL_FOR_ARTIFACTORY = "Firewall for Artifactory";

  public static final String PRODUCT_AUDITOR = "Auditor";

  public static final String MIGRATION_TRACKER_EXTERNAL_DB = "external-database";

  public static final String PRODUCT_ADVANCED_DEVELOPMENT_PACK = "Advanced Development Pack";

  public static final String PRODUCT_ADVANCED_LEGAL_PACK = "Advanced Legal Pack";

  public static final String PRODUCT_INFRASTRUCTURE_AS_CODE_PACK = "Infrastructure as Code Pack";

  public static final String PRODUCT_LIFECYCLE_CLOUD = "Lifecycle Cloud";

  public static final String PRODUCT_LIFECYCLE_FIREWALL_CLOUD = "Lifecycle Firewall Cloud";

  public static final String PRODUCT_LIFECYCLE_SAAS = "Lifecycle SaaS";

  public static final String PRODUCT_LIFECYCLE_FIREWALL_SAAS = "Lifecycle Firewall SaaS";

  public static final String PRODUCT_LIFECYCLE_FOUNDATION_SAAS = "Lifecycle Foundation SaaS";

  public static final String PRODUCT_AUDITOR_SAAS = "Auditor SaaS";

  public static final String PRODUCT_SBOM_MANAGER = "SBOM Manager";

  public static final String PRODUCT_SBOM_MANAGER_SAAS = "SBOM Manager SaaS";

  public static final String PRODUCT_SONATYPE_DEVELOPMENT = "Developer";

  public static final String PRODUCT_TEAMS_EDITION = "Teams Edition";

  public static final String PRODUCT_LIFECYCLE_PRO = "Lifecycle Pro";

  public static final String PRODUCT_LIFECYCLE_ENTERPRISE = "Lifecycle Enterprise";

  static final String TIER_PRO = "Pro";

  private static final String TIER_ENTERPRISE = "Enterprise";

  public static final String PRODUCT_GUIDE = "Guide";

  private static final Set<String> LIFECYCLE_PRODUCTS = Set.of(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION,
      ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS, ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD,
      ProductLicenseDetails.PRODUCT_TEAMS_EDITION);

  private static final Set<String> SBOM_MANAGER_PRODUCTS = Set.of(ProductLicenseDetails.PRODUCT_SBOM_MANAGER,
      ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);

  private static final Set<String> LEGAL_PACK_PRODUCTS = Set.of(ProductLicenseDetails.PRODUCT_ADVANCED_LEGAL_PACK);

  private static final Set<String> GUIDE_PRODUCTS = Set.of(ProductLicenseDetails.PRODUCT_GUIDE_SELF_HOSTED);

  // Visible for testing
  static final String TASK_NAME = "ProductLicenseLoad";

  private static final String LICENSE_LOADING_ERROR = "Error when loading the product license";

  // no-op
  private static final String FIPS_LICENSE_KEYSTORE_EXTENSION = ".bcfks";

  private static final String LEGACY_LICENSE_KEYSTORE_EXTENSION = ".p12";

  private static final String LICENSING_KEYSTORE_NAME = "licensing-keystore";

  private final InsightConfig config;

  private final MigrationTrackerDAO migrationTrackerDAO;

  private final ApplicationDAO applicationDAO;

  private final ProductLicense productLicense;

  private final CreditAwareProductLicense creditAwareProductLicense;

  private final ProductLicenseDetailsCache productLicenseDetailsCache;

  private final ProductLicenseManager licenseManager;

  private final LicenseFingerprinter licenseFingerprinter;

  private final LicenseContent licenseContent;

  private final HdsClient hdsClient;

  private static final Logger log = LoggerFactory.getLogger(CLMLicenseManager.class);

  private final List<ProductLicenseListener> listeners = new CopyOnWriteArrayList<>();

  private final AuditRecorder auditRecorder;

  private final TaskScheduler taskScheduler;

  private final DeveloperEnablementService developerEnablementService;

  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @Inject
  public CLMLicenseManager(
      final InsightConfig config,
      final MigrationTrackerDAO migrationTrackerDAO,
      final ApplicationDAO applicationDAO,
      final ProductLicense productLicense,
      final CreditAwareProductLicense creditAwareProductLicense,
      final ProductLicenseDetailsCache productLicenseDetailsCache,
      final ProductLicenseManager licenseManager,
      final LicenseFingerprinter licenseFingerprinter,
      final LicenseContent licenseContent,
      final HdsClient hdsClient,
      final AuditRecorder auditRecorder,
      final TaskScheduler taskScheduler,
      final DeveloperEnablementService developerEnablementService,
      final Set<ProductLicenseListener> productLicenseListeners,
      final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO)
  {
    this.config = config;
    this.migrationTrackerDAO = migrationTrackerDAO;
    this.applicationDAO = applicationDAO;
    this.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
    this.productLicense = productLicense;
    this.creditAwareProductLicense = creditAwareProductLicense;
    this.productLicenseDetailsCache = productLicenseDetailsCache;
    this.licenseManager = licenseManager;
    this.licenseFingerprinter = licenseFingerprinter;
    this.licenseContent = licenseContent;
    this.hdsClient = hdsClient;
    this.auditRecorder = auditRecorder;
    this.taskScheduler = taskScheduler;
    this.developerEnablementService = developerEnablementService;

    // Register all ProductLicenseListener implementations
    productLicenseListeners.forEach(this::addListener);
  }

  public void loadLicense() {
    SignedProductLicenseDetailsDTO licenseDetails;
    try {
      ProductLicenseKey licenseKey = licenseManager.getLicenseDetails();
      String licenseFingerprint = licenseFingerprinter.calculate(licenseKey);
      byte[] licenseData = licenseContent.raw();

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
    validateExternalDatabaseForFeature(licenseDetails);
  }

  private void validateExternalDatabaseForFeature(SignedProductLicenseDetailsDTO licenseDetails) {
    if (!config.isDatabaseEmbedded()) {
      return;
    }
    if (licenseDetails.features.contains(LicensedFeature.SBOM_MANAGER.name())) {
      throw new ExternalDatabaseNotSupportedException(
          "SBOM Manager feature requires use of an external database, please retry using an external database.");
    }
    // Each Guide entitlement (GUIDE umbrella, GUIDE_MCP, GUIDE_SEARCH) requires external DB on its
    // own. SearchLicenseFilter and McpLicenseFilter gate on the child features independently of the
    // umbrella, so HDS signing only a child feature must still be rejected on embedded DB.
    for (LicensedFeature feature : EnumSet.of(LicensedFeature.GUIDE, LicensedFeature.GUIDE_MCP,
        LicensedFeature.GUIDE_SEARCH))
    {
      if (licenseDetails.features.contains(feature.name())) {
        throw new ExternalDatabaseNotSupportedException(
            "Guide feature requires use of an external database, please retry using an external database.");
      }
    }
  }

  private void updatePropertiesWithDeveloperProduct(final Properties properties) {
    if (properties == null) {
      return;
    }

    final String productsString = properties.getProperty(ProductLicenseDetails.PROPERTY_PRODUCTS);
    if (StringUtils.isEmpty(productsString)) {
      return;
    }

    final List<String> products = Arrays.asList(productsString.split(","));
    if (shouldAddDeveloperProduct(products)) {
      final String productsProperty = properties.getProperty(ProductLicenseDetails.PROPERTY_PRODUCTS) +
          "," + ProductLicenseDetails.PRODUCT_SONATYPE_DEVELOPMENT;
      properties.setProperty(ProductLicenseDetails.PROPERTY_PRODUCTS, productsProperty);
    }
  }

  private boolean shouldAddDeveloperProduct(final List<String> products) {
    return developerEnablementService.shouldEnableDeveloperProduct() &&
        !products.contains(ProductLicenseDetails.PRODUCT_SONATYPE_DEVELOPMENT);
  }

  private Set<String> getProducts() {
    final List<String> productList = Lists.newArrayList(productLicense.getProducts());
    if (shouldAddDeveloperProduct(productList)) {
      productList.add(ProductLicenseDetails.PRODUCT_SONATYPE_DEVELOPMENT);
    }
    return Sets.newHashSet(productList);
  }

  // visible for testing
  void updateLicenseCacheFromDatabase() {
    try {
      SignedProductLicenseDetailsDTO licenseDetails = productLicenseDetailsCache.getProductLicenseDetails();
      if (licenseDetails == null) {
        clearLicenseCache(false);
      }
      else {
        ProductLicenseKey licenseKey = licenseManager.getLicenseDetails();
        // not verifying signature intentionally. This is expected to be used only to update the cache
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
        && !migrationTrackerDAO.isTrackerPresent(MIGRATION_TRACKER_EXTERNAL_DB))
    {
      throw new ExternalDatabaseNotSupportedException("The product license does not support use of an external database"
          + ", please reconfigure IQ Server to use the embedded database before installing the license.");
    }
    validateExternalDatabaseForFeature(licenseDetails);
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
        hdsClient.post(SignedProductLicenseDetailsDTO.class, HdsClient.GET_PRODUCT_LICENSE_DETAILS_HDS_PATH,
            licenseData);
    verifySignature(licenseDetails, licenseFingerprint);
    return licenseDetails;
  }

  private void verifySignature(SignedProductLicenseDetailsDTO licenseDetails, String licenseFingerprint) {
    try {
      Signature signature = Signature.getInstance(CertificateFactory.getSignatureAlgorithm());
      signature.initVerify(loadCertificateForSignatureVerification(licenseDetails.signatureKeyAlias));
      for (String feature : licenseDetails.features) {
        signature.update(feature.getBytes(StandardCharsets.UTF_8));
      }
      for (String stageId : licenseDetails.stageIds) {
        signature.update(stageId.getBytes(StandardCharsets.UTF_8));
      }
      signature.update((licenseDetails.maxApplications == null ? "0" : licenseDetails.maxApplications.toString())
          .getBytes(StandardCharsets.UTF_8));

      if (licenseDetails.maxSboms != null) {
        signature.update(licenseDetails.maxSboms.toString().getBytes(StandardCharsets.UTF_8));
      }

      if (licenseDetails.creditAmount != null) {
        signature.update(licenseDetails.creditAmount.stripTrailingZeros()
            .toPlainString()
            .getBytes(StandardCharsets.UTF_8));
      }

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
      KeyStore keyStore = KeyStoreFactory.createKeyStore();
      keyStore.load(getResourceForLicensingKeystore(), getUnobfuscatedLicensingKeysPassword());
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
    AuditData.get()
        .setData("productLicenseFingerprint", productLicense.getFingerprint())
        .setData("productLicenseFilename", filename)
        .setData("productLicenseExpiry", productLicenseExpiry);
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
      case ProductLicenseDetails.PRODUCT_AUDITOR_SAAS:
        marketingNameSuffix = PRODUCT_AUDITOR_SAAS;
        break;
      case ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD:
        marketingNameSuffix = PRODUCT_LIFECYCLE_CLOUD;
        break;
      case ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS:
        marketingNameSuffix = PRODUCT_LIFECYCLE_SAAS;
        break;
      case ProductLicenseDetails.PRODUCT_FOUNDATION:
        marketingNameSuffix = PRODUCT_LIFECYCLE_FOUNDATION;
        break;
      case ProductLicenseDetails.PRODUCT_LIFECYCLE_FOUNDATION_SAAS:
        marketingNameSuffix = PRODUCT_LIFECYCLE_FOUNDATION_SAAS;
        break;
      case ProductLicenseDetails.PRODUCT_FIREWALL:
      case ProductLicenseDetails.PRODUCT_FIREWALL_V2:
        marketingNameSuffix = PRODUCT_FIREWALL;
        break;
      case ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_CLOUD:
        marketingNameSuffix = PRODUCT_LIFECYCLE_FIREWALL_CLOUD;
        break;
      case ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_SAAS:
        marketingNameSuffix = PRODUCT_LIFECYCLE_FIREWALL_SAAS;
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
      case ProductLicenseDetails.PRODUCT_SBOM_MANAGER:
        marketingNameSuffix = PRODUCT_SBOM_MANAGER;
        break;
      case ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS:
        marketingNameSuffix = PRODUCT_SBOM_MANAGER_SAAS;
        break;
      case ProductLicenseDetails.PRODUCT_SONATYPE_DEVELOPMENT:
        marketingNameSuffix = PRODUCT_SONATYPE_DEVELOPMENT;
        break;
      case ProductLicenseDetails.PRODUCT_TEAMS_EDITION:
        marketingNameSuffix = PRODUCT_TEAMS_EDITION;
        break;
      case ProductLicenseDetails.PRODUCT_GUIDE_SELF_HOSTED:
        marketingNameSuffix = PRODUCT_GUIDE;
        break;
      default:
        return null;
    }

    return "Sonatype " + marketingNameSuffix;
  }

  public LicenseSummary getLicenseSummary() {
    return new LicenseSummary(getProductEdition(), getProductLicenseProductsMarketingNames());
  }

  public LicenseInfo getLicenseInfo() {
    String[] products = getProductLicenseProductsMarketingNames();
    String productEdition = getProductEdition();
    Set<ProductLicensingModel> licensingModels = productLicense.getLicensingModels();
    Integer applicationLimitToDisplay = null;
    Integer applicationCountToDisplay = null;
    Integer licensedUsersToDisplay = null;
    Integer firewallUsersToDisplay = null;
    Integer sbomLimitToDisplay = null;
    BigDecimal creditAmountToDisplay = null;

    for (ProductLicensingModel model : licensingModels) {
      switch (model) {
        case LEGACY:
          switch (productEdition) {
            case PRODUCT_AUDITOR:
            case PRODUCT_AUDITOR_SAAS:
              applicationLimitToDisplay = productLicense.getMaxApplications();
              break;
            case PRODUCT_PRO_PLUS:
              licensedUsersToDisplay = productLicense.getMaxUsers();
              break;
            case PRODUCT_LIFECYCLE:
            case PRODUCT_LIFECYCLE_CLOUD:
            case PRODUCT_LIFECYCLE_SAAS:
            case PRODUCT_LIFECYCLE_FOUNDATION:
            case PRODUCT_LIFECYCLE_FOUNDATION_SAAS:
              licensedUsersToDisplay = productLicense.getMaxUsers();
              //$FALL-THROUGH$ fallthrough
            case PRODUCT_FIREWALL:
            case PRODUCT_LIFECYCLE_FIREWALL_CLOUD:
            case PRODUCT_LIFECYCLE_FIREWALL_SAAS:
              firewallUsersToDisplay = productLicense.getMaxFirewallUsers();
              break;
            case PRODUCT_SBOM_MANAGER:
            case PRODUCT_SBOM_MANAGER_SAAS:
              sbomLimitToDisplay = productLicense.getMaxSboms();
              break;
            case PRODUCT_GUIDE:
              // Defensive: Guide licenses should always be CREDIT_BASED (tested by
              // testGetLicenseInfo_GuideWithLegacyModel_surfacesCreditAmount), but handle LEGACY just in case.
              creditAmountToDisplay = creditAwareProductLicense.getCreditAmount();
              break;
            default:
              // no limits to display
          }
          break;
        case APP_BASED:
          applicationLimitToDisplay = productLicense.getMaxApplications();
          break;
        case SBOM_BASED:
          sbomLimitToDisplay = productLicense.getMaxSboms();
          break;
        case USER_BASED:
          licensedUsersToDisplay = productLicense.getMaxUsers();
          firewallUsersToDisplay = productLicense.getMaxFirewallUsers();
          break;
        case CREDIT_BASED:
          creditAmountToDisplay = creditAwareProductLicense.getCreditAmount();
          break;
        default:
          throw new IllegalStateException("Unknown licensing model: " + model);
      }
    }

    if (creditAmountToDisplay == null && hasGuideProduct(productLicense)) {
      creditAmountToDisplay = creditAwareProductLicense.getCreditAmount();
    }

    if (applicationLimitToDisplay != null) {
      applicationCountToDisplay = (int) applicationDAO.getCountWithoutRelatedRepositories();
    }

    Properties properties = productLicense.isValid() ? licenseManager.getLicenseDetails().getProperties() : null;
    updatePropertiesWithDeveloperProduct(properties);

    return new LicenseInfo(productLicense.getFingerprint(), productLicense.getExpirationTimestamp(),
        licensedUsersToDisplay, firewallUsersToDisplay, applicationLimitToDisplay, applicationCountToDisplay,
        sbomLimitToDisplay, creditAmountToDisplay, productLicense.getContactName(), productLicense.getContactCompany(),
        productLicense.getContactEmail(), products, properties, productEdition);
  }

  public static boolean hasLifecycleProduct(ProductLicense productLicense) {
    return LIFECYCLE_PRODUCTS.stream().anyMatch(productLicense::hasProduct);
  }

  static boolean hasAnyLifecycleProduct(Set<String> products) {
    return !Collections.disjoint(LIFECYCLE_PRODUCTS, products);
  }

  public static boolean hasSbomManagerProduct(ProductLicense productLicense) {
    return SBOM_MANAGER_PRODUCTS.stream().anyMatch(productLicense::hasProduct);
  }

  public static boolean hasAdvancedLegalPackProduct(ProductLicense productLicense) {
    return LEGAL_PACK_PRODUCTS.stream().anyMatch(productLicense::hasProduct);
  }

  public static boolean hasGuideProduct(ProductLicense productLicense) {
    return GUIDE_PRODUCTS.stream().anyMatch(productLicense::hasProduct);
  }

  private String[] getProductLicenseProductsMarketingNames() {
    return getProducts().stream()
        .map(CLMLicenseManager::getProductMarketingName)
        .filter(Objects::nonNull)
        .distinct()
        .toArray(String[]::new);
  }

  private String getProductEdition() {
    Set<String> products = getProducts();

    // Check tier for lifecycle products
    if (hasAnyLifecycleProduct(products)) {
      String normalizedTier = normalizeTier(getLifecycleTier());
      if (TIER_PRO.equalsIgnoreCase(normalizedTier)) {
        return PRODUCT_LIFECYCLE_PRO;
      }
      else if (TIER_ENTERPRISE.equalsIgnoreCase(normalizedTier)) {
        return PRODUCT_LIFECYCLE_ENTERPRISE;
      }
    }

    // Legacy/null tier or non-lifecycle products — return original product edition
    if (products.contains(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION)) {
      return PRODUCT_LIFECYCLE;
    }
    else if (products.contains(ProductLicenseDetails.PRODUCT_FOUNDATION)) {
      return PRODUCT_LIFECYCLE_FOUNDATION;
    }
    else if (products.contains(ProductLicenseDetails.PRODUCT_LIFECYCLE_FOUNDATION_SAAS)) {
      return PRODUCT_LIFECYCLE_FOUNDATION_SAAS;
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
    else if (products.contains(ProductLicenseDetails.PRODUCT_AUDITOR_SAAS)) {
      return PRODUCT_AUDITOR_SAAS;
    }
    else if (products.contains(ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD)) {
      return PRODUCT_LIFECYCLE_CLOUD;
    }
    else if (products.contains(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS)) {
      return PRODUCT_LIFECYCLE_SAAS;
    }
    else if (products.contains(ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_CLOUD)) {
      return PRODUCT_LIFECYCLE_FIREWALL_CLOUD;
    }
    else if (products.contains(ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_SAAS)) {
      return PRODUCT_LIFECYCLE_FIREWALL_SAAS;
    }
    else if (products.contains(ProductLicenseDetails.PRODUCT_SBOM_MANAGER)) {
      return PRODUCT_SBOM_MANAGER;
    }
    else if (products.contains(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS)) {
      return PRODUCT_SBOM_MANAGER_SAAS;
    }
    else if (products.contains(ProductLicenseDetails.PRODUCT_TEAMS_EDITION)) {
      return PRODUCT_TEAMS_EDITION;
    }
    else if (products.contains(ProductLicenseDetails.PRODUCT_GUIDE_SELF_HOSTED)) {
      return PRODUCT_GUIDE;
    }
    // Keep this last since we do not have a true standalone edition of Developer yet
    else if (products.contains(ProductLicenseDetails.PRODUCT_SONATYPE_DEVELOPMENT)) {
      return PRODUCT_SONATYPE_DEVELOPMENT;
    }

    return "";
  }

  private void validateFeatures(final ProductLicenseKey key) {
    if (!verifyAnyFeature(key, new CLMFeature(), new FirewallFeature(), new GuideFeature())) {
      throw new LicensingException("License does not permit use of feature '" + CLMFeature.ID + "', '"
          + FirewallFeature.ID + "', or '" + GuideFeature.ID + "'");
    }
  }

  private boolean verifyAnyFeature(final ProductLicenseKey key, AbstractFeature... features) {
    for (AbstractFeature feature : features) {
      try {
        licenseManager.verifyFeature(key, feature);
        return true;
      }
      catch (LicensingException e) {
        // Try next feature
      }
    }
    return false;
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

    Set<ProductLicensingModel> licensingModels = getLicensingModels(key);
    Integer applicationCount = licenseDetails.maxApplications;
    Integer maxFirewallUsers = getMaxFirewallUsers(key);
    Integer maxUsers = getMaxUsers(key);
    Integer maxSboms = licenseDetails.maxSboms;
    BigDecimal creditAmount = licenseDetails.creditAmount;

    Set<String> products = getProducts(key);

    Set<LicensedFeature> features = EnumSet.noneOf(LicensedFeature.class);
    Set<StageType> stageTypes = new LinkedHashSet<>();
    // The classic stage set used by Lifecycle/Risk/Foundation product tiers. Excludes
    // COMPLIANCE (SBOM-Manager-only stage, added explicitly when that product is present).
    Collection<StageType> allClassicStageTypes =
        StageTypes.getAll()
            .stream()
            .filter(stageType -> !StageTypes.COMPLIANCE.equals(stageType))
            .collect(Collectors.toSet());
    if (products.contains(ProductLicenseDetails.PRODUCT_RISK)
        || products.contains(ProductLicenseDetails.PRODUCT_AUDITOR_SAAS))
    {
      features.add(LicensedFeature.POLICY_MONITORING);
      features.add(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS);
      features.add(LicensedFeature.DASHBOARD);
      features.add(LicensedFeature.WAIVERS_DASHBOARD);
      features.add(LicensedFeature.CLI_INTEGRATION);
      features.add(LicensedFeature.ENFORCEMENT);
      features.add(LicensedFeature.NOTIFICATIONS);
      features.add(LicensedFeature.POLICY_GRANDFATHERING);
      features.add(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS);
      features.add(LicensedFeature.RM_STAGING_INTEGRATION);

      features.add(LicensedFeature.DATA_RETENTION);
      features.add(LicensedFeature.INNER_SOURCE_REPOSITORIES);
      features.add(LicensedFeature.ORGS_AND_APPS);
      features.add(LicensedFeature.PROPRIETARY_COMPONENTS);

      features.add(LicensedFeature.APPLICATION_REPORTS);
      features.add(LicensedFeature.APPLICATION_EVALUATION);
      features.add(LicensedFeature.CALL_FLOW_ANALYSIS);
      features.add(LicensedFeature.COMPONENT_EVALUATION);
      features.add(LicensedFeature.COMPONENT_LABELS);
      features.add(LicensedFeature.COMPONENT_SEARCH);
      features.add(LicensedFeature.POLICY_MANAGEMENT);
      features.add(LicensedFeature.POLICY_READ_ONLY);
      features.add(LicensedFeature.POLICY_VIOLATIONS);
      features.add(LicensedFeature.POLICY_WAIVERS);
      features.add(LicensedFeature.REPOSITORY_EVALUATION);
      features.add(LicensedFeature.REPOSITORY_REPORTS);
      features.add(LicensedFeature.SBOM_EVALUATION);
      features.add(LicensedFeature.SBOM_REPORTS);
      features.add(LicensedFeature.SOURCE_CONTROL);
      features.add(LicensedFeature.SUCCESS_METRICS);
      features.add(LicensedFeature.VULNERABILITY_CUSTOMIZATION);
      features.add(LicensedFeature.WAIVER_REPORTS);

      stageTypes.add(StageTypes.RELEASE);
    }
    if (products.contains(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION)
        || products.contains(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS))
    {
      addLifecycleFeatures(features);
      stageTypes.addAll(allClassicStageTypes);
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
    if (products.contains(ProductLicenseDetails.PRODUCT_FOUNDATION)
        || products.contains(ProductLicenseDetails.PRODUCT_LIFECYCLE_FOUNDATION_SAAS))
    {
      features.add(LicensedFeature.DASHBOARD);
      features.add(LicensedFeature.WAIVERS_DASHBOARD);
      features.add(LicensedFeature.CLI_INTEGRATION);
      features.add(LicensedFeature.CI_INTEGRATION);
      features.add(LicensedFeature.RM_STAGING_INTEGRATION);
      features.add(LicensedFeature.QUALITY);

      features.add(LicensedFeature.DATA_RETENTION);
      features.add(LicensedFeature.INNER_SOURCE_REPOSITORIES);
      features.add(LicensedFeature.ORGS_AND_APPS);
      features.add(LicensedFeature.PROPRIETARY_COMPONENTS);

      features.add(LicensedFeature.API_PAGE);
      features.add(LicensedFeature.APPLICATION_REPORTS);
      features.add(LicensedFeature.APPLICATION_EVALUATION);
      features.add(LicensedFeature.CALL_FLOW_ANALYSIS);
      features.add(LicensedFeature.COMPONENT_EVALUATION);
      features.add(LicensedFeature.COMPONENT_LABELS);
      features.add(LicensedFeature.COMPONENT_SEARCH);
      features.add(LicensedFeature.POLICY_MANAGEMENT);
      features.add(LicensedFeature.POLICY_READ_ONLY);
      features.add(LicensedFeature.POLICY_VIOLATIONS);
      features.add(LicensedFeature.POLICY_WAIVERS);
      features.add(LicensedFeature.REPOSITORY_EVALUATION);
      features.add(LicensedFeature.REPOSITORY_REPORTS);
      features.add(LicensedFeature.SBOM_EVALUATION);
      features.add(LicensedFeature.SBOM_REPORTS);
      features.add(LicensedFeature.SOURCE_CONTROL);
      features.add(LicensedFeature.SUCCESS_METRICS);
      features.add(LicensedFeature.VULNERABILITY_CUSTOMIZATION);
      features.add(LicensedFeature.WAIVER_REPORTS);
      features.add(LicensedFeature.CONTAINER_IMAGES_EVALUATION);

      stageTypes.addAll(allClassicStageTypes);
    }
    if (products.contains(ProductLicenseDetails.PRODUCT_FIREWALL) ||
        products.contains(ProductLicenseDetails.PRODUCT_FIREWALL_FOR_ARTIFACTORY))
    {
      features.add(LicensedFeature.FIREWALL);
      features.add(LicensedFeature.FIREWALL_FOR_ARTIFACTORY);
      features.add(LicensedFeature.RM_STAGING_INTEGRATION);
      features.add(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES);
      features.add(LicensedFeature.WEBHOOKS_FOR_REPOSITORIES);
      features.add(LicensedFeature.WAIVERS_DASHBOARD);

      features.add(LicensedFeature.API_PAGE);
      features.add(LicensedFeature.APPLICATION_REPORTS);
      features.add(LicensedFeature.APPLICATION_EVALUATION);
      features.add(LicensedFeature.CALL_FLOW_ANALYSIS);
      features.add(LicensedFeature.COMPONENT_EVALUATION);
      features.add(LicensedFeature.COMPONENT_LABELS);
      features.add(LicensedFeature.COMPONENT_SEARCH);
      features.add(LicensedFeature.POLICY_MANAGEMENT);
      features.add(LicensedFeature.POLICY_READ_ONLY);
      features.add(LicensedFeature.POLICY_VIOLATIONS);
      features.add(LicensedFeature.POLICY_WAIVERS);
      features.add(LicensedFeature.REPOSITORY_EVALUATION);
      features.add(LicensedFeature.REPOSITORY_REPORTS);
      features.add(LicensedFeature.SBOM_EVALUATION);
      features.add(LicensedFeature.SBOM_REPORTS);
      features.add(LicensedFeature.SOURCE_CONTROL);
      features.add(LicensedFeature.SUCCESS_METRICS);
      features.add(LicensedFeature.VULNERABILITY_CUSTOMIZATION);
      features.add(LicensedFeature.WAIVER_REPORTS);
      features.add(LicensedFeature.ROI_CONFIGURATION);

      stageTypes.add(StageTypes.STAGE_RELEASE);
      stageTypes.add(StageTypes.RELEASE);
    }
    if (products.contains(ProductLicenseDetails.PRODUCT_FIREWALL_V2) ||
        products.contains(ProductLicenseDetails.PRODUCT_FIREWALL_FOR_ARTIFACTORY_V2) ||
        products.contains(ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_SAAS))
    {
      features.add(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE);
      features.add(LicensedFeature.RELEASE_INTEGRITY);
      features.add(LicensedFeature.FIREWALL);
      features.add(LicensedFeature.FIREWALL_FOR_ARTIFACTORY);
      features.add(LicensedFeature.RM_STAGING_INTEGRATION);
      features.add(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES);
      features.add(LicensedFeature.WEBHOOKS_FOR_REPOSITORIES);
      features.add(LicensedFeature.WAIVERS_DASHBOARD);

      features.add(LicensedFeature.API_PAGE);
      features.add(LicensedFeature.APPLICATION_REPORTS);
      features.add(LicensedFeature.APPLICATION_EVALUATION);
      features.add(LicensedFeature.CALL_FLOW_ANALYSIS);
      features.add(LicensedFeature.COMPONENT_EVALUATION);
      features.add(LicensedFeature.COMPONENT_LABELS);
      features.add(LicensedFeature.COMPONENT_SEARCH);
      features.add(LicensedFeature.POLICY_MANAGEMENT);
      features.add(LicensedFeature.POLICY_READ_ONLY);
      features.add(LicensedFeature.POLICY_VIOLATIONS);
      features.add(LicensedFeature.POLICY_WAIVERS);
      features.add(LicensedFeature.REPOSITORY_EVALUATION);
      features.add(LicensedFeature.REPOSITORY_REPORTS);
      features.add(LicensedFeature.SBOM_EVALUATION);
      features.add(LicensedFeature.SBOM_REPORTS);
      features.add(LicensedFeature.SOURCE_CONTROL);
      features.add(LicensedFeature.SUCCESS_METRICS);
      features.add(LicensedFeature.VULNERABILITY_CUSTOMIZATION);
      features.add(LicensedFeature.WAIVER_REPORTS);
      features.add(LicensedFeature.ROI_CONFIGURATION);
      features.add(LicensedFeature.MALWARE_DEFENSE_EVALUATION);
      features.add(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
      features.add(LicensedFeature.NOTIFICATIONS);

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
      features.add(LicensedFeature.WAIVERS_DASHBOARD);
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

      features.add(LicensedFeature.DATA_RETENTION);
      features.add(LicensedFeature.INNER_SOURCE_REPOSITORIES);
      features.add(LicensedFeature.ORGS_AND_APPS);
      features.add(LicensedFeature.PROPRIETARY_COMPONENTS);

      features.add(LicensedFeature.API_PAGE);
      features.add(LicensedFeature.APPLICATION_REPORTS);
      features.add(LicensedFeature.APPLICATION_EVALUATION);
      features.add(LicensedFeature.CALL_FLOW_ANALYSIS);
      features.add(LicensedFeature.COMPONENT_EVALUATION);
      features.add(LicensedFeature.COMPONENT_LABELS);
      features.add(LicensedFeature.COMPONENT_SEARCH);
      features.add(LicensedFeature.POLICY_MANAGEMENT);
      features.add(LicensedFeature.POLICY_READ_ONLY);
      features.add(LicensedFeature.POLICY_VIOLATIONS);
      features.add(LicensedFeature.POLICY_WAIVERS);
      features.add(LicensedFeature.REPOSITORY_EVALUATION);
      features.add(LicensedFeature.REPOSITORY_REPORTS);
      features.add(LicensedFeature.SBOM_EVALUATION);
      features.add(LicensedFeature.SBOM_REPORTS);
      features.add(LicensedFeature.SOURCE_CONTROL);
      features.add(LicensedFeature.SUCCESS_METRICS);
      features.add(LicensedFeature.VULNERABILITY_CUSTOMIZATION);
      features.add(LicensedFeature.WAIVER_REPORTS);
      features.add(LicensedFeature.ROI_CONFIGURATION);
      features.add(LicensedFeature.CONTAINER_IMAGES_EVALUATION);

      stageTypes.addAll(allClassicStageTypes);
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
      features.add(LicensedFeature.WAIVERS_DASHBOARD);

      features.add(LicensedFeature.API_PAGE);
      features.add(LicensedFeature.APPLICATION_REPORTS);
      features.add(LicensedFeature.APPLICATION_EVALUATION);
      features.add(LicensedFeature.CALL_FLOW_ANALYSIS);
      features.add(LicensedFeature.COMPONENT_EVALUATION);
      features.add(LicensedFeature.COMPONENT_LABELS);
      features.add(LicensedFeature.COMPONENT_SEARCH);
      features.add(LicensedFeature.POLICY_MANAGEMENT);
      features.add(LicensedFeature.POLICY_READ_ONLY);
      features.add(LicensedFeature.POLICY_VIOLATIONS);
      features.add(LicensedFeature.POLICY_WAIVERS);
      features.add(LicensedFeature.REPOSITORY_EVALUATION);
      features.add(LicensedFeature.REPOSITORY_REPORTS);
      features.add(LicensedFeature.SBOM_EVALUATION);
      features.add(LicensedFeature.SBOM_REPORTS);
      features.add(LicensedFeature.SOURCE_CONTROL);
      features.add(LicensedFeature.SUCCESS_METRICS);
      features.add(LicensedFeature.VULNERABILITY_CUSTOMIZATION);
      features.add(LicensedFeature.WAIVER_REPORTS);
      features.add(LicensedFeature.ROI_CONFIGURATION);
      features.add(LicensedFeature.MALWARE_DEFENSE_EVALUATION);
      features.add(LicensedFeature.CONTAINER_IMAGES_EVALUATION);

      stageTypes.add(StageTypes.STAGE_RELEASE);
      stageTypes.add(StageTypes.RELEASE);
    }
    if (products.contains(ProductLicenseDetails.PRODUCT_SBOM_MANAGER) ||
        products.contains(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS))
    {
      features.add(LicensedFeature.API_PAGE);
      features.add(LicensedFeature.SBOM_MANAGER);
      features.add(LicensedFeature.POLICY_MONITORING);
      features.add(LicensedFeature.POLICY_READ_ONLY);
      features.add(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS);
      features.add(LicensedFeature.CLI_INTEGRATION);
      features.add(LicensedFeature.NOTIFICATIONS);
      features.add(LicensedFeature.DATA_RETENTION);
      features.add(LicensedFeature.ORGS_AND_APPS);
      features.add(LicensedFeature.ENFORCEMENT);
      features.add(LicensedFeature.APPLICATION_EVALUATION);
      features.add(LicensedFeature.CONTAINER_IMAGES_EVALUATION);

      stageTypes.add(StageTypes.COMPLIANCE);
    }
    if (products.contains(ProductLicenseDetails.PRODUCT_GUIDE_SELF_HOSTED)) {
      // GUIDE, GUIDE_MCP, GUIDE_SEARCH are HDS-controlled (added below from
      // licenseDetails.features) so ops can disable Guide via SQL on the product_license
      // features_csv column without reissuing the license file.
      stageTypes.add(StageTypes.DEVELOP);
    }

    stageTypes.add(StageTypes.PROXY);

    if (products.contains(ProductLicenseDetails.PRODUCT_SONATYPE_DEVELOPMENT)) {
      addDevelopmentFeatures(features);
    }

    if (products.contains(ProductLicenseDetails.PRODUCT_TEAMS_EDITION)) {
      addLifecycleFeatures(features);
      addDevelopmentFeatures(features);
      stageTypes.addAll(allClassicStageTypes);
    }

    // Tier-controlled features are reconciled at read time in DefaultProductLicense. They remain
    // in this HDS overlay so HDS-granted entries land in the cache and survive the tier check.
    Set<LicensedFeature> hdsControlledFeatures = EnumSet.of( //
        LicensedFeature.GUIDE, //
        LicensedFeature.GUIDE_MCP, //
        LicensedFeature.GUIDE_SEARCH, //
        LicensedFeature.ADVANCED_RECOMMENDATION_STRATEGIES, //
        LicensedFeature.EXTERNAL_DATABASE, //
        LicensedFeature.HYGIENE, //
        LicensedFeature.RELEASE_INTEGRITY, //
        LicensedFeature.NODE_CLUSTERING, //
        LicensedFeature.ADVANCED_LEGAL_PACK, //
        LicensedFeature.DATA_INSIGHTS, //
        LicensedFeature.INFRASTRUCTURE_AS_CODE_PACK, //
        LicensedFeature.BREAKING_CHANGE, //
        LicensedFeature.DEVELOPER_DASHBOARD, //
        LicensedFeature.INTEGRATED_ENTERPRISE_REPORTING, //
        LicensedFeature.ALLOW_SCM_ON_PUBLIC_REPOS, //
        LicensedFeature.CPE_MATCHING, //
        LicensedFeature.MALICIOUS_URLS_PARTNER_ACCESS, //
        LicensedFeature.CUSTOM_POLICIES, //
        LicensedFeature.CUSTOM_APPLICATION_CATEGORIES, //
        LicensedFeature.CUSTOM_COMPONENT_LABELS, //
        LicensedFeature.CUSTOM_LICENSE_THREAT_GROUPS, //
        LicensedFeature.AUTO_WAIVER_MANAGEMENT, //
        LicensedFeature.WAIVER_REQUEST_WORKFLOW, //
        LicensedFeature.BULK_WAIVERS //
    );
    for (LicensedFeature feature : hdsControlledFeatures) {
      if (licenseDetails.features.contains(feature.name())) {
        features.add(feature);
      }
    }

    if (triggerOnOtherNodes) {
      loadProductLicenseOnAllOtherClusterNodes();
    }
    productLicense.set(key, licenseFingerprint, products, features, stageTypes, licensingModels, applicationCount,
        maxUsers, maxFirewallUsers, maxSboms);
    creditAwareProductLicense.setCreditAmount(creditAmount);
    notifyListeners();
  }

  private static void addDevelopmentFeatures(final Set<LicensedFeature> features) {
    features.add(LicensedFeature.API_PAGE);
    features.add(LicensedFeature.DEVELOPER_DASHBOARD);
  }

  private String getLifecycleTier() {
    try {
      SystemConfigurationProperty prop =
          systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.LIFECYCLE_TIER);
      return prop != null ? prop.getValue() : null;
    }
    catch (Exception e) {
      log.warn("Could not read LifecycleTier config, defaulting to Legacy", e);
      return null;
    }
  }

  private static String normalizeTier(String tier) {
    return tier != null ? tier.trim() : null;
  }

  private static void applyTierFeatures(String tier, Set<LicensedFeature> features) {
    String normalizedTier = normalizeTier(tier);

    if (TIER_PRO.equalsIgnoreCase(normalizedTier)) {
      return;
    }

    if (TIER_ENTERPRISE.equalsIgnoreCase(normalizedTier)) {
      addEnterpriseTierFeatures(features);
      return;
    }

    // Legacy / null / unknown — currently same as Enterprise, can diverge later
    if (normalizedTier != null) {
      log.info("Unrecognized lifecycle tier '{}', treating as Legacy (Enterprise features granted)", normalizedTier);
    }
    addEnterpriseTierFeatures(features);
  }

  private static void addEnterpriseTierFeatures(final Set<LicensedFeature> features) {
    features.add(LicensedFeature.CUSTOM_POLICIES);
    features.add(LicensedFeature.CUSTOM_APPLICATION_CATEGORIES);
    features.add(LicensedFeature.CUSTOM_COMPONENT_LABELS);
    features.add(LicensedFeature.CUSTOM_LICENSE_THREAT_GROUPS);
    features.add(LicensedFeature.AUTO_WAIVER_MANAGEMENT);
    features.add(LicensedFeature.WAIVER_REQUEST_WORKFLOW);
    features.add(LicensedFeature.BULK_WAIVERS);
  }

  private static void addLifecycleFeatures(final Set<LicensedFeature> features) {
    features.add(LicensedFeature.QUALITY);
    features.add(LicensedFeature.POLICY_MONITORING);
    features.add(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS);
    features.add(LicensedFeature.DASHBOARD);
    features.add(LicensedFeature.WAIVERS_DASHBOARD);
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

    features.add(LicensedFeature.DATA_RETENTION);
    features.add(LicensedFeature.INNER_SOURCE_REPOSITORIES);
    features.add(LicensedFeature.ORGS_AND_APPS);
    features.add(LicensedFeature.PROPRIETARY_COMPONENTS);

    features.add(LicensedFeature.API_PAGE);
    features.add(LicensedFeature.APPLICATION_REPORTS);
    features.add(LicensedFeature.APPLICATION_EVALUATION);
    features.add(LicensedFeature.CALL_FLOW_ANALYSIS);
    features.add(LicensedFeature.COMPONENT_EVALUATION);
    features.add(LicensedFeature.COMPONENT_LABELS);
    features.add(LicensedFeature.COMPONENT_SEARCH);
    features.add(LicensedFeature.POLICY_MANAGEMENT);
    features.add(LicensedFeature.POLICY_READ_ONLY);
    features.add(LicensedFeature.POLICY_VIOLATIONS);
    features.add(LicensedFeature.POLICY_WAIVERS);
    features.add(LicensedFeature.REPOSITORY_EVALUATION);
    features.add(LicensedFeature.REPOSITORY_REPORTS);
    features.add(LicensedFeature.SBOM_EVALUATION);
    features.add(LicensedFeature.SBOM_REPORTS);
    features.add(LicensedFeature.SOURCE_CONTROL);
    features.add(LicensedFeature.SUCCESS_METRICS);
    features.add(LicensedFeature.VULNERABILITY_CUSTOMIZATION);
    features.add(LicensedFeature.WAIVER_REPORTS);
    features.add(LicensedFeature.ROI_CONFIGURATION);
    features.add(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
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
    String clmProducts = getProperty(key, ProductLicenseDetails.PROPERTY_PRODUCTS);
    if (clmProducts != null && !clmProducts.isBlank()) {
      Collections.addAll(products, clmProducts.split("\\s*,\\s*"));
    }
    String guideProducts = getProperty(key, ProductLicenseDetails.PROPERTY_GUIDE_PRODUCTS);
    if (guideProducts != null && !guideProducts.isBlank()) {
      Collections.addAll(products, guideProducts.split("\\s*,\\s*"));
    }
    return products;
  }

  private Set<ProductLicensingModel> getLicensingModels(ProductLicenseKey key) {
    String prop = getProperty(key, ProductLicenseDetails.PROPERTY_LICENSING_MODEL);
    Set<ProductLicensingModel> models = new HashSet<>();
    if (prop == null) {
      models.add(ProductLicensingModel.LEGACY);
    }
    else {
      String[] props = prop.split(",");
      for (String p : props) {
        switch (p) {
          case ProductLicenseDetails.LICENSING_APP_BASED:
            models.add(ProductLicensingModel.APP_BASED);
            break;
          case ProductLicenseDetails.LICENSING_USER_BASED:
            models.add(ProductLicensingModel.USER_BASED);
            break;
          case ProductLicenseDetails.LICENSING_SBOM_BASED:
            models.add(ProductLicensingModel.SBOM_BASED);
            break;
          case ProductLicenseDetails.LICENSING_CREDIT_BASED:
            models.add(ProductLicensingModel.CREDIT_BASED);
            break;
          default:
            throw new LicensingException("Invalid licensing model: " + p);
        }
      }
    }
    return models;
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
    taskScheduler.scheduleOneTimeTaskForAllOtherNodes(this);
  }

  @Override
  public void execute(JobExecutionContext context) {
    execute(this::updateLicenseCacheFromDatabase, log, LICENSE_LOADING_ERROR);
  }

  @Override
  public String getJobName() {
    return TASK_NAME;
  }

  private InputStream getResourceForLicensingKeystore() {
    String extension =
        FIPSModeDetector.isEnabled() ? FIPS_LICENSE_KEYSTORE_EXTENSION : LEGACY_LICENSE_KEYSTORE_EXTENSION;
    return getClass().getResourceAsStream(LICENSING_KEYSTORE_NAME.concat(extension));
  }

  private char[] getUnobfuscatedLicensingKeysPassword() {
    return LicensingUtil
        .unobfuscate(new long[]{0xA8874A6C58A5CD5BL, 0xDADEE6943E19F478L, 0x34D18D0FE23233C2L})
        .toCharArray();
  }
}
