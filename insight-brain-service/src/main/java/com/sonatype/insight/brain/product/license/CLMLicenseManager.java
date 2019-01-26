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

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.license.model.CLMEnforcementPoint;
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
  private static final String FEATURE_POLICY_MONITORING = "PolicyMonitoring";

  private static final String FEATURE_DASHBOARD = "DASHBOARD";

  private static final String FEATURE_CLI_SCAN = "CLI_SCAN";

  private static final String FEATURE_QUALITY = "QUALITY";

  private static final String FEATURE_REPOSITORY_FIREWALL = "REPOSITORY_FIREWALL";

  private static final String FEATURE_ENFORCEMENT = "ENFORCEMENT";

  private static final String FEATURE_NOTIFICATIONS = "NOTIFICATIONS";

  private static final String FEATURE_POLICY_GRANDFATHERING = "POLICY_GRANDFATHERING";

  private static final String FEATURE_WEBHOOKS = "WEBHOOKS";

  public static final String PRODUCT_PRO_PLUS = "Pro+";

  public static final String PRODUCT_LIFECYCLE = "Lifecycle";

  public static final String PRODUCT_LIFECYCLE_FOUNDATION = "Lifecycle Foundation";

  public static final String PRODUCT_FIREWALL = "Firewall";

  public static final String PRODUCT_AUDITOR = "Auditor";

  private final class CachedLicenseData
      extends ProductLicenseDetails
  {
    private final String fingerprint;

    private final long expirationTimestamp;

    private final String contactName;

    private final String contactCompany;

    private final String contactEmail;

    public CachedLicenseData(final String fingerprint,
                             final int version,
                             Integer applicationLimit,
                             final Set<String> products,
                             final String[] features,
                             final Set<CLMEnforcementPoint> enforcementPoints,
                             final long expirationTimestamp,
                             final Integer maxUsers,
                             final Integer maxFirewallUsers,
                             final String contactName,
                             final String contactCompany,
                             final String contactEmail)
    {
      this.fingerprint = fingerprint;
      this.expirationTimestamp = expirationTimestamp;
      this.contactName = contactName;
      this.contactCompany = contactCompany;
      this.contactEmail = contactEmail;

      setVersion(version);
      super.setApplicationLimit(applicationLimit);
      super.setMaxFirewallUsers(maxFirewallUsers);
      super.setMaxUsers(maxUsers);
      super.setEnforcementPoints(enforcementPoints.toArray(new CLMEnforcementPoint[enforcementPoints.size()]));
      super.setFeatures(features);
      setProducts(products);
    }

    public String getFingerprint() {
      return fingerprint;
    }
  }

  public static class LicenseSummary
  {
    public String productEdition;

    public LicenseSummary() {
    }

    public LicenseSummary(String productEdition) {
      this.productEdition = productEdition;
    }
  }

  public static final class LicenseInfo
      extends LicenseSummary
  {
    public String fingerprint;

    public long expiryTimestamp;

    /*
     * NOTE: The next two fields aren't necessarily the real limits, they're just the limits that we want
     * to show to users in the License info page. In particular, Lifecycle licenses aren't sold by application limit,
     * so we don't want to display it for those licenses. However, they do still technically have an application
     * limit, which will not be reflected in the value of this property. Similarly, Auditor licenses don't really use
     * the licensedUsers field, but it still has a value in the license simply because it isn't nullable.
     */
    public Integer licensedUsersToDisplay;

    public Integer applicationLimitToDisplay;

    public Integer firewallUsersToDisplay;

    public String contactName;

    public String contactCompany;

    public String contactEmail;

    public String[] products;

    public LicenseInfo() {
    }

    public LicenseInfo(String fingerprint,
                       long expiryTimestamp,
                       Integer licensedUsersToDisplay,
                       Integer firewallUsersToDisplay,
                       Integer applicationLimitToDisplay,
                       String contactName,
                       String contactCompany,
                       String contactEmail,
                       String[] products,
                       String productEdition)
    {
      super(productEdition);

      this.fingerprint = fingerprint;
      this.expiryTimestamp = expiryTimestamp;
      this.licensedUsersToDisplay = licensedUsersToDisplay;
      this.firewallUsersToDisplay = firewallUsersToDisplay;
      this.applicationLimitToDisplay = applicationLimitToDisplay;
      this.contactName = contactName;
      this.contactCompany = contactCompany;
      this.contactEmail = contactEmail;
      this.products = products;
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
    return licenseCache.getFingerprint();
  }

  /**
   * Get the application limit in the license, if no license, 0 will be returned
   */
  public Integer getApplicationCountLimit() {
    return licenseCache.getApplicationLimit();
  }

  public boolean hasPolicyMonitoring() {
    return hasFeature(FEATURE_POLICY_MONITORING);
  }

  public boolean hasDashboard() {
    return hasFeature(FEATURE_DASHBOARD);
  }

  public boolean hasCLIScanning() {
    return hasFeature(FEATURE_CLI_SCAN);
  }

  /**
   * Checks to see if the license enables the quality feature
   *
   * @since 1.11.0
   */
  public boolean hasQuality() {
    return hasFeature(FEATURE_QUALITY);
  }

  /**
   * @since 1.17
   */
  public boolean hasRepositoryFirewall() {
    return hasFeature(FEATURE_REPOSITORY_FIREWALL);
  }

  /**
   * @since 1.59
   */
  public boolean hasEnforcement() {
    return hasFeature(FEATURE_ENFORCEMENT);
  }

  /**
   * @since 1.59
   */
  public boolean hasEnforcement(String stageTypeId) {
    return hasEnforcement() || (hasRepositoryFirewall() && Stage.ID_PROXY.equals(stageTypeId));
  }

  /**
   * @since 1.59
   */
  public boolean hasNotifications() {
    return hasFeature(FEATURE_NOTIFICATIONS);
  }

  /**
   * For lifecycle foundation notifications are turned off, except when hasFirewall and is proxy stage
   *
   * @since 1.59
   */
  public boolean hasNotifications(String stageTypeId) {
    return hasNotifications() || (hasRepositoryFirewall() && Stage.ID_PROXY.equals(stageTypeId));
  }

  /**
   * @since 1.59
   */
  public boolean hasPolicyGrandfathering() {
    return hasFeature(FEATURE_POLICY_GRANDFATHERING);
  }

  /**
   * @since 1.59
   */
  public boolean hasWebhooks() {
    return hasFeature(FEATURE_WEBHOOKS);
  }

  private boolean hasFeature(String feature) {
    String[] licensedFeatures = licenseCache.getFeatures();
    if (licensedFeatures != null) {
      for (String licensedFeature : licensedFeatures) {
        if (licensedFeature.equals(feature)) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean hasProduct(String productId) {
    Set<String> products = licenseCache.getProducts();
    return products != null && products.contains(productId);
  }

  public boolean hasEnforcementPoint(CLMEnforcementPoint enforcementPoint) {
    return getEnforcementPoints().contains(enforcementPoint);
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
      String msg = "Nexus IQ license has expired!";
      if (getLicenseFingerprint() == null) {
        msg = "Nexus IQ is not licensed!";
      }
      log.error(msg);
      throw new InvalidLicenseException();
    }
  }

  /**
   * Validates that the license is installed and contains any of the requested enforcement points.
   *
   * @throws InvalidLicenseException If none of the enforcement points is licensed.
   */
  public void validateAnyEnforcementPoint(Set<CLMEnforcementPoint> enforcementPoints) {
    if (enforcementPoints.isEmpty()) {
      return;
    }

    Set<CLMEnforcementPoint> licensed = getEnforcementPoints();
    for (CLMEnforcementPoint requested : enforcementPoints) {
      if (licensed.contains(requested)) {
        return;
      }
    }

    throw new InvalidLicenseException();
  }

  public Set<String> getProducts() {
    return licenseCache.getProducts();
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

  public Set<CLMEnforcementPoint> getEnforcementPoints() {
    Set<CLMEnforcementPoint> enforcementPoints = EnumSet.noneOf(CLMEnforcementPoint.class);
    Collections.addAll(enforcementPoints, licenseCache.getEnforcementPoints());
    return enforcementPoints;
  }

  public LicenseSummary getLicenseSummary() {
    return new LicenseSummary(getProductEdition());
  }

  public LicenseInfo getLicenseInfo() {
    String[] products = licenseCache.getProducts().stream() //
        .map(CLMLicenseManager::getProductMarketingName) //
        .filter(Objects::nonNull) //
        .toArray(String[]::new);

    String productEdition = getProductEdition();
    Integer applicationLimitToDisplay = null;
    Integer licensedUsersToDisplay = null;
    Integer firewallUsersToDisplay = null;

    switch (productEdition) {
      case PRODUCT_AUDITOR:
        applicationLimitToDisplay = licenseCache.getApplicationLimit();
        break;
      case PRODUCT_PRO_PLUS:
        licensedUsersToDisplay = licenseCache.getMaxUsers();
        break;
      case PRODUCT_LIFECYCLE:
        // no break
      case PRODUCT_LIFECYCLE_FOUNDATION:
        licensedUsersToDisplay = licenseCache.getMaxUsers();
        // no break
      case PRODUCT_FIREWALL:
        firewallUsersToDisplay = licenseCache.getMaxFirewallUsers();
        break;
    }

    return new LicenseInfo(licenseCache.getFingerprint(), licenseCache.expirationTimestamp, licensedUsersToDisplay,
        firewallUsersToDisplay, applicationLimitToDisplay, licenseCache.contactName, licenseCache.contactCompany,
        licenseCache.contactEmail, products, productEdition);
  }

  private String getProductEdition() {
    if (hasProduct(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION)) {
      return (PRODUCT_LIFECYCLE);
    }
    else if (hasProduct(ProductLicenseDetails.PRODUCT_FOUNDATION)) {
      return (PRODUCT_LIFECYCLE_FOUNDATION);
    }
    else if (hasProduct(ProductLicenseDetails.PRODUCT_FIREWALL)) {
      return (PRODUCT_FIREWALL);
    }
    else if (hasProduct(ProductLicenseDetails.PRODUCT_NEXUS)) {
      return (PRODUCT_PRO_PLUS);
    }
    else if (hasProduct(ProductLicenseDetails.PRODUCT_RISK)) {
      return (PRODUCT_AUDITOR);
    }

    return "";
  }

  private void validateFeatures(final ProductLicenseKey key) throws LicensingException {
    try {
      licenseManager.verifyFeature(key, new CLMFeature());
    }
    catch (LicensingException e1) {
      try {
        licenseManager.verifyFeature(key, new FirewallFeature());
      }
      catch (LicensingException e2) {
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

    Set<CLMEnforcementPoint> enforcementPoints = EnumSet.noneOf(CLMEnforcementPoint.class);
    Set<String> features = new LinkedHashSet<>();
    if (products.contains(ProductLicenseDetails.PRODUCT_RISK)) {
      features.add(FEATURE_POLICY_MONITORING);
      features.add(FEATURE_DASHBOARD);
      features.add(FEATURE_CLI_SCAN);
      features.add(FEATURE_ENFORCEMENT);
      features.add(FEATURE_NOTIFICATIONS);
      features.add(FEATURE_POLICY_GRANDFATHERING);
      features.add(FEATURE_WEBHOOKS);
      enforcementPoints.add(CLMEnforcementPoint.Release);
    }
    if (products.contains(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION)) {
      features.add(FEATURE_QUALITY);
      features.add(FEATURE_POLICY_MONITORING);
      features.add(FEATURE_DASHBOARD);
      features.add(FEATURE_CLI_SCAN);
      features.add(FEATURE_ENFORCEMENT);
      features.add(FEATURE_NOTIFICATIONS);
      features.add(FEATURE_POLICY_GRANDFATHERING);
      features.add(FEATURE_WEBHOOKS);
      enforcementPoints.add(CLMEnforcementPoint.Develop);
      enforcementPoints.add(CLMEnforcementPoint.Build);
      enforcementPoints.add(CLMEnforcementPoint.StageRelease);
      enforcementPoints.add(CLMEnforcementPoint.Release);
    }
    if (products.contains(ProductLicenseDetails.PRODUCT_NEXUS)) {
      features.add(FEATURE_ENFORCEMENT);
      features.add(FEATURE_NOTIFICATIONS);
      features.add(FEATURE_POLICY_GRANDFATHERING);
      features.add(FEATURE_WEBHOOKS);
      enforcementPoints.add(CLMEnforcementPoint.StageRelease);
      enforcementPoints.add(CLMEnforcementPoint.Release);
    }
    if (products.contains(ProductLicenseDetails.PRODUCT_FOUNDATION)) {
      features.add(FEATURE_DASHBOARD);
      features.add(FEATURE_CLI_SCAN);
      enforcementPoints.add(CLMEnforcementPoint.Build);
      enforcementPoints.add(CLMEnforcementPoint.StageRelease);
      enforcementPoints.add(CLMEnforcementPoint.Release);
    }
    if (products.contains(ProductLicenseDetails.PRODUCT_FIREWALL)) {
      features.add(FEATURE_REPOSITORY_FIREWALL);
      enforcementPoints.add(CLMEnforcementPoint.StageRelease);
      enforcementPoints.add(CLMEnforcementPoint.Release);
    }

    licenseCache = new CachedLicenseData(licenseFingerprint, version, applicationCount, products,
        features.toArray(new String[features.size()]), enforcementPoints, key.getExpirationDate().getTime(), maxUsers,
        maxFirewallUsers, key.getContactName(), key.getContactCompany(), key.getContactEmailAddress());
    notifyListeners();
  }

  private String getProperty(ProductLicenseKey key, String property) {
    return key.getProperties().getProperty(property);
  }

  private int getVersion(ProductLicenseKey key) throws LicensingException {
    String prop = getProperty(key, ProductLicenseDetails.PROPERTY_VERSION);
    if (prop == null) {
      // legacy license
      return 0;
    }
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

    if (prop != null) {
      try {
        return Integer.decode(prop);
      }
      catch (IllegalArgumentException e) {
        throw new LicensingException("Invalid value for max users: " + prop, e);
      }
    }
    else {
      return null;
    }
  }

  private Integer getMaxFirewallUsers(ProductLicenseKey key) throws LicensingException {
    String prop = getProperty(key, ProductLicenseDetails.PROPERTY_MAX_FIREWALL_USERS);

    if (prop != null) {
      try {
        return Integer.decode(prop);
      }
      catch (IllegalArgumentException e) {
        throw new LicensingException("Invalid value for max firewall users: " + prop, e);
      }
    }
    else {
      return null;
    }
  }

  private void clearLicenseCache() {
    licenseCache = new CachedLicenseData(null, 0, 0, Collections.<String> emptySet(), new String[0],
        Collections.<CLMEnforcementPoint> emptySet(), 0, 0, null, null, null, null);
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
