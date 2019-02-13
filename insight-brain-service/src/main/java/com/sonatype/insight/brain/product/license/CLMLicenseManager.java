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
import com.sonatype.insight.brain.features.Feature;
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

  private final class CachedLicenseData
  {
    private final String fingerprint;

    private final long expirationTimestamp;

    private final String contactName;

    private final String contactCompany;

    private final String contactEmail;

    private final Set<String> products;

    private final Set<Feature> features;

    private final Integer applicationLimit;

    private final Integer maxUsers;

    private final Integer maxFirewallUsers;

    private final Integer maxFirewallForArtifactoryServers;

    public CachedLicenseData(String fingerprint,
                             long expirationTimestamp,
                             String contactName,
                             String contactCompany,
                             String contactEmail,
                             Set<String> products,
                             Set<Feature> features,
                             Integer applicationLimit,
                             Integer maxUsers,
                             Integer maxFirewallUsers,
                             Integer maxFirewallForArtifactoryServers)
    {
      this.fingerprint = fingerprint;
      this.expirationTimestamp = expirationTimestamp;
      this.contactName = contactName;
      this.contactCompany = contactCompany;
      this.contactEmail = contactEmail;
      this.products = products;
      this.features = features;
      this.applicationLimit = applicationLimit;
      this.maxUsers = maxUsers;
      this.maxFirewallUsers = maxFirewallUsers;
      this.maxFirewallForArtifactoryServers = maxFirewallForArtifactoryServers;
    }
  }

  private final ProductLicenseManager licenseManager;

  private final LicenseFingerprinter licenseFingerprinter;

  private static final Logger log = LoggerFactory.getLogger(CLMLicenseManager.class);

  private volatile CachedLicenseData licenseCache;

  private final List<LicenseListener> listeners = new CopyOnWriteArrayList<>();
  
  private final AuditRecorder auditRecorder;

  @Inject
  public CLMLicenseManager(final ProductLicenseManager licenseManager,
                           final LicenseFingerprinter licenseFingerprinter,
                           final AuditRecorder auditRecorder)
  {
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
    if (getLicenseFingerprint() != null) {
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
        .ofInstant(Instant.ofEpochMilli(getLicenseInfo().expiryTimestamp), ZoneId.systemDefault())
        .format(DateTimeFormatter.ISO_LOCAL_DATE);
    AuditData.get().setData("productLicenseFingerprint", getLicenseFingerprint())
        .setData("productLicenseFilename", filename).setData("productLicenseExpiry", productLicenseExpiry);
  }

  /**
   * Get a license fingerprint, if there is no license, null will be returned
   */
  public String getLicenseFingerprint() {
    return licenseCache.fingerprint;
  }

  /**
   * Get the application limit in the license, if no license, 0 will be returned
   */
  public Integer getApplicationCountLimit() {
    return licenseCache.applicationLimit;
  }

  public boolean hasFeature(Feature feature) {
    return getFeatures().contains(feature);
  }

  public Set<Feature> getFeatures() {
    return EnumSet.copyOf(licenseCache.features);
  }

  public boolean hasProduct(String productId) {
    return licenseCache.products.contains(productId);
  }

  /**
   * Get whether the license is currently valid
   *
   * @return the validity
   */
  public boolean isValid() {
    return getLicenseFingerprint() != null && licenseCache.expirationTimestamp > System.currentTimeMillis();
  }

  /**
   * Validate that a license is installed
   *
   * @throws InvalidLicenseException when no license is installed or the installed license is not valid
   */
  public void validate() throws InvalidLicenseException {
    if (!isValid()) {
      String msg = "The product license has expired.";
      if (getLicenseFingerprint() == null) {
        msg = "No valid product license installed.";
      }
      throw new InvalidLicenseException(msg);
    }
  }

  public void validateFeature(Feature feature) {
    if (!hasFeature(feature)) {
      throw new InvalidLicenseException();
    }
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
    String[] products = licenseCache.products.stream() //
        .map(CLMLicenseManager::getProductMarketingName) //
        .filter(Objects::nonNull) //
        .toArray(String[]::new);

    String productEdition = getProductEdition();
    Integer applicationLimitToDisplay = null;
    Integer licensedUsersToDisplay = null;
    Integer firewallUsersToDisplay = null;
    Integer firewallForArtifactoryServersToDisplay = null;

    switch (productEdition) {
      case PRODUCT_AUDITOR:
        applicationLimitToDisplay = licenseCache.applicationLimit;
        break;
      case PRODUCT_PRO_PLUS:
        licensedUsersToDisplay = licenseCache.maxUsers;
        break;
      case PRODUCT_LIFECYCLE:
        // fallthrough
      case PRODUCT_LIFECYCLE_FOUNDATION:
        licensedUsersToDisplay = licenseCache.maxUsers;
        // fallthrough
      case PRODUCT_FIREWALL:
        firewallUsersToDisplay = licenseCache.maxFirewallUsers;
        // fallthrough
      case PRODUCT_FIREWALL_FOR_ARTIFACTORY:
        firewallForArtifactoryServersToDisplay = licenseCache.maxFirewallForArtifactoryServers;
        break;
      default:
        // no limits to display
    }

    return new LicenseInfo(licenseCache.fingerprint, licenseCache.expirationTimestamp, licensedUsersToDisplay,
        firewallUsersToDisplay, firewallForArtifactoryServersToDisplay, applicationLimitToDisplay,
        licenseCache.contactName, licenseCache.contactCompany, licenseCache.contactEmail, products, productEdition);
  }

  private String getProductEdition() {
    if (hasProduct(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION)) {
      return PRODUCT_LIFECYCLE;
    }
    else if (hasProduct(ProductLicenseDetails.PRODUCT_FOUNDATION)) {
      return PRODUCT_LIFECYCLE_FOUNDATION;
    }
    else if (hasProduct(ProductLicenseDetails.PRODUCT_FIREWALL)) {
      return PRODUCT_FIREWALL;
    }
    else if (hasProduct(ProductLicenseDetails.PRODUCT_FIREWALL_FOR_ARTIFACTORY)) {
      return PRODUCT_FIREWALL_FOR_ARTIFACTORY;
    }
    else if (hasProduct(ProductLicenseDetails.PRODUCT_NEXUS)) {
      return PRODUCT_PRO_PLUS;
    }
    else if (hasProduct(ProductLicenseDetails.PRODUCT_RISK)) {
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
    Integer maxFirewallForArtifactoryServers = getMaxFirewallForArtifactoryServers(key);
    Integer maxUsers = getMaxUsers(key);

    Set<String> products = getProducts(key);

    Set<Feature> features = EnumSet.noneOf(Feature.class);
    if (products.contains(ProductLicenseDetails.PRODUCT_RISK)) {
      features.add(Feature.POLICY_MONITORING);
      features.add(Feature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS);
      features.add(Feature.DASHBOARD);
      features.add(Feature.CLI_INTEGRATION);
      features.add(Feature.ENFORCEMENT);
      features.add(Feature.NOTIFICATIONS);
      features.add(Feature.POLICY_GRANDFATHERING);
      features.add(Feature.WEBHOOKS_FOR_APPLICATIONS);
      features.add(Feature.RM_STAGING_INTEGRATION);
    }
    if (products.contains(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION)) {
      features.add(Feature.QUALITY);
      features.add(Feature.POLICY_MONITORING);
      features.add(Feature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS);
      features.add(Feature.DASHBOARD);
      features.add(Feature.CLI_INTEGRATION);
      features.add(Feature.ENFORCEMENT);
      features.add(Feature.NOTIFICATIONS);
      features.add(Feature.POLICY_GRANDFATHERING);
      features.add(Feature.WEBHOOKS_FOR_APPLICATIONS);
      features.add(Feature.IDE_INTEGRATION);
      features.add(Feature.CI_INTEGRATION);
      features.add(Feature.RM_STAGING_INTEGRATION);
    }
    if (products.contains(ProductLicenseDetails.PRODUCT_NEXUS)) {
      features.add(Feature.ENFORCEMENT);
      features.add(Feature.NOTIFICATIONS);
      features.add(Feature.POLICY_GRANDFATHERING);
      features.add(Feature.WEBHOOKS_FOR_APPLICATIONS);
      features.add(Feature.RM_STAGING_INTEGRATION);
    }
    if (products.contains(ProductLicenseDetails.PRODUCT_FOUNDATION)) {
      features.add(Feature.DASHBOARD);
      features.add(Feature.CLI_INTEGRATION);
      features.add(Feature.CI_INTEGRATION);
      features.add(Feature.RM_STAGING_INTEGRATION);
      features.add(Feature.QUALITY);
    }
    if (products.contains(ProductLicenseDetails.PRODUCT_FIREWALL)) {
      features.add(Feature.FIREWALL);
      features.add(Feature.RM_STAGING_INTEGRATION);
      features.add(Feature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES);
      features.add(Feature.WEBHOOKS_FOR_REPOSITORIES);
    }
    if (products.contains(ProductLicenseDetails.PRODUCT_FIREWALL_FOR_ARTIFACTORY)) {
      features.add(Feature.FIREWALL_FOR_ARTIFACTORY);
      features.add(Feature.RM_STAGING_INTEGRATION);
      features.add(Feature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES);
    }

    licenseCache = new CachedLicenseData(licenseFingerprint, key.getExpirationDate().getTime(), key.getContactName(),
        key.getContactCompany(), key.getContactEmailAddress(), products, features, applicationCount, maxUsers,
        maxFirewallUsers, maxFirewallForArtifactoryServers);
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

  private Integer getMaxFirewallForArtifactoryServers(ProductLicenseKey key) throws LicensingException {
    String prop = getProperty(key, ProductLicenseDetails.PROPERTY_MAX_FIREWALL_FOR_ARTIFACTORY_SERVERS);
    try {
      return prop != null ? Integer.decode(prop) : null;
    }
    catch (IllegalArgumentException e) {
      throw new LicensingException("Invalid value for max firewall for artifactory servers: " + prop, e);
    }
  }

  private void clearLicenseCache() {
    licenseCache = new CachedLicenseData(null, 0, null, null, null, Collections.emptySet(), Collections.emptySet(), 0,
        0, 0, 0);
    notifyListeners();
  }

  /**
   * Registers the specified listener to be notified of changes to the license.
   *
   * @since 1.9
   */
  public void addListener(LicenseListener listener) {
    if (listener == null) {
      throw new IllegalArgumentException("listener not specified");
    }
    listeners.add(listener);
  }

  /**
   * Unregisters the specified listener.
   *
   * @since 1.9
   */
  public void removeListener(LicenseListener listener) {
    listeners.remove(listener);
  }

  private void notifyListeners() {
    for (LicenseListener listener : listeners) {
      try {
        listener.licenseChanged();
      }
      catch (RuntimeException e) {
        log.warn("Failed to notify {} of license update", listener, e);
      }
    }
  }
}
