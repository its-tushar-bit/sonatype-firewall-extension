/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import com.sonatype.insight.brain.features.LicensedFeature;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.sonatype.licensing.LicensingException;
import org.sonatype.licensing.product.ProductLicenseKey;
import org.sonatype.licensing.product.ProductLicenseManager;
import org.sonatype.licensing.product.util.LicenseFingerprinter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class CLMLicenseManager
{
  public static final String PRODUCT_PRO_PLUS = "Pro+";

  public static final String PRODUCT_LIFECYCLE = "Lifecycle";

  public static final String PRODUCT_LIFECYCLE_FOUNDATION = "Lifecycle Foundation";

  public static final String PRODUCT_FIREWALL = "Firewall";

  public static final String PRODUCT_FIREWALL_FOR_ARTIFACTORY = "Firewall for Artifactory";

  public static final String PRODUCT_AUDITOR = "Auditor";

  private final ProductLicense productLicense;

  private final ProductLicenseManager licenseManager;

  private final LicenseFingerprinter licenseFingerprinter;

  private static final Logger log = LoggerFactory.getLogger(CLMLicenseManager.class);

  private final List<ProductLicenseListener> listeners = new CopyOnWriteArrayList<>();
  
  private final AuditRecorder auditRecorder;

  @Inject
  public CLMLicenseManager(
      final ProductLicense productLicense,
      final ProductLicenseManager licenseManager,
      final LicenseFingerprinter licenseFingerprinter,
      final AuditRecorder auditRecorder)
  {
    this.productLicense = productLicense;
    this.licenseManager = licenseManager;
    this.licenseFingerprinter = licenseFingerprinter;
    this.auditRecorder = auditRecorder;
    try {
      populateLicenseCache();
    }
    catch (LicensingException e) {
      log.debug("Unable to load license details", e);
      clearLicenseCache();
    }
  }

  public void installLicenseIfUnlicensed(String licenseFilePath) throws IOException, LicensingException {
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

  public synchronized void installLicense(InputStream is) throws IOException, LicensingException {
    licenseManager.installLicense(is);
    populateLicenseCache();
    log.info("License installed successfully");
  }

  public synchronized void uninstallLicense() throws LicensingException {
    licenseManager.uninstallLicense();
    clearLicenseCache();
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
   * A function to map from product names stored in the license to product names suitable for
   * display to the end-user
   */
  private static String getProductMarketingName(String internalName) {
    String marketingNameSuffix;

    switch (internalName) {
      case ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION:
        marketingNameSuffix = PRODUCT_LIFECYCLE;
        break;
      case ProductLicenseDetails.PRODUCT_FOUNDATION:
        marketingNameSuffix = PRODUCT_LIFECYCLE_FOUNDATION;
        break;
      case ProductLicenseDetails.PRODUCT_FIREWALL:
        marketingNameSuffix = PRODUCT_FIREWALL;
        break;
      case ProductLicenseDetails.PRODUCT_FIREWALL_FOR_ARTIFACTORY:
        marketingNameSuffix = PRODUCT_FIREWALL_FOR_ARTIFACTORY;
        break;
      case ProductLicenseDetails.PRODUCT_NEXUS:
        marketingNameSuffix = PRODUCT_PRO_PLUS;
        break;
      case ProductLicenseDetails.PRODUCT_RISK:
        marketingNameSuffix = PRODUCT_AUDITOR;
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
    Integer applicationLimitToDisplay = null;
    Integer licensedUsersToDisplay = null;
    Integer firewallUsersToDisplay = null;

    switch (productEdition) {
      case PRODUCT_AUDITOR:
        applicationLimitToDisplay = productLicense.getMaxApplications();
        break;
      case PRODUCT_PRO_PLUS:
        licensedUsersToDisplay = productLicense.getMaxUsers();
        break;
      case PRODUCT_LIFECYCLE:
        // fallthrough
      case PRODUCT_LIFECYCLE_FOUNDATION:
        licensedUsersToDisplay = productLicense.getMaxUsers();
        // fallthrough
      case PRODUCT_FIREWALL:
        firewallUsersToDisplay = productLicense.getMaxFirewallUsers();
        break;
      default:
        // no limits to display
    }

    return new LicenseInfo(productLicense.getFingerprint(), productLicense.getExpirationTimestamp(),
        licensedUsersToDisplay, firewallUsersToDisplay, applicationLimitToDisplay, productLicense.getContactName(),
        productLicense.getContactCompany(), productLicense.getContactEmail(), products, productEdition);
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
    else if (products.contains(ProductLicenseDetails.PRODUCT_NEXUS)) {
      return PRODUCT_PRO_PLUS;
    }
    else if (products.contains(ProductLicenseDetails.PRODUCT_RISK)) {
      return PRODUCT_AUDITOR;
    }

    return "";
  }

  private void validateFeatures(final ProductLicenseKey key) throws LicensingException {
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

  private void populateLicenseCache() throws LicensingException {
    ProductLicenseKey key = licenseManager.getLicenseDetails();

    validateFeatures(key);

    String licenseFingerprint = licenseFingerprinter.calculate(key);

    int version = getVersion(key);
    if (version < 1) {
      // legacy license without product info
      throw new LicensingException("Invalid license version: " + version);
    }

    Integer applicationCount = getApplicationLimit(key);
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
    if (products.contains(ProductLicenseDetails.PRODUCT_FIREWALL)) {
      features.add(LicensedFeature.FIREWALL);
      features.add(LicensedFeature.RM_STAGING_INTEGRATION);
      features.add(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES);
      features.add(LicensedFeature.WEBHOOKS_FOR_REPOSITORIES);
      stageTypes.add(StageTypes.STAGE_RELEASE);
      stageTypes.add(StageTypes.RELEASE);
    }
    if (products.contains(ProductLicenseDetails.PRODUCT_FIREWALL_FOR_ARTIFACTORY)) {
      features.add(LicensedFeature.FIREWALL_FOR_ARTIFACTORY);
      features.add(LicensedFeature.RM_STAGING_INTEGRATION);
      features.add(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES);
    }
    stageTypes.add(StageTypes.PROXY);

    productLicense.set(key, licenseFingerprint, products, features, stageTypes, applicationCount,
        maxUsers, maxFirewallUsers);
    notifyListeners();
  }

  private String getProperty(ProductLicenseKey key, String property) {
    return key.getProperties().getProperty(property);
  }

  private int getVersion(ProductLicenseKey key) throws LicensingException {
    String prop = getProperty(key, ProductLicenseDetails.PROPERTY_VERSION);
    try {
      return Integer.parseInt(prop);
    }
    catch (IllegalArgumentException e) {
      throw new LicensingException("Invalid license version: " + prop, e);
    }
  }

  private Integer getApplicationLimit(ProductLicenseKey key) throws LicensingException {
    String prop = getProperty(key, ProductLicenseDetails.PROPERTY_APPLICATION_LIMIT);
    try {
      return prop != null ? Integer.decode(prop) : null;
    }
    catch (IllegalArgumentException e) {
      throw new LicensingException("Invalid application limit: " + prop, e);
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

  private Integer getMaxUsers(ProductLicenseKey key) throws LicensingException {
    String prop = getProperty(key, ProductLicenseDetails.PROPERTY_MAX_USERS);
    try {
      return prop != null ? Integer.decode(prop) : null;
    }
    catch (IllegalArgumentException e) {
      throw new LicensingException("Invalid value for max users: " + prop, e);
    }
  }

  private Integer getMaxFirewallUsers(ProductLicenseKey key) throws LicensingException {
    String prop = getProperty(key, ProductLicenseDetails.PROPERTY_MAX_FIREWALL_USERS);
    try {
      return prop != null ? Integer.decode(prop) : null;
    }
    catch (IllegalArgumentException e) {
      throw new LicensingException("Invalid value for max firewall users: " + prop, e);
    }
  }

  private void clearLicenseCache() {
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
        log.debug("Notifying listener {}", listener);
        listener.productLicenseChanged();
      }
      catch (RuntimeException e) {
        log.warn("Failed to notify {} of license update", listener, e);
      }
    }
  }
}
