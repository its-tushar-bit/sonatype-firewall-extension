/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

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

  private final class CachedLicenseData
      extends ProductLicenseDetails
  {
    private final String fingerprint;

    private final long expirationTimestamp;

    public CachedLicenseData(final String fingerprint, final int version, Integer applicationLimit,
        final Set<String> products, final String[] features, final Set<CLMEnforcementPoint> enforcementPoints,
        final long expirationTimestamp)
    {
      this.fingerprint = fingerprint;
      this.expirationTimestamp = expirationTimestamp;
      setVersion(version);
      super.setApplicationLimit(applicationLimit);
      super.setEnforcementPoints(enforcementPoints.toArray(new CLMEnforcementPoint[0]));
      super.setFeatures(features);
      setProducts(products);
    }

    public String getFingerprint() {
      return fingerprint;
    }
  }

  public final class LicenseSummary
  {
    public final long expiryTimestamp;
    public final String[] features;

    public LicenseSummary(long timestamp, String[] features) {
      this.expiryTimestamp = timestamp;
      this.features = features;
    }
  }

  private final ProductLicenseManager licenseManager;

  private final LicenseFingerprinter licenseFingerprinter;

  private static final Logger log = LoggerFactory.getLogger(CLMLicenseManager.class);

  private volatile CachedLicenseData licenseCache;

  @Inject
  public CLMLicenseManager(final ProductLicenseManager licenseManager, final LicenseFingerprinter licenseFingerprinter)
  {
    this.licenseManager = licenseManager;
    this.licenseFingerprinter = licenseFingerprinter;
    try {
      populateLicenseCache();
    }
    catch (LicensingException e) {
      log.debug("Unable to load license details", e);
      clearLicenseCache();
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

  /**
   * Get a license fingerprint, if there is no license, null will be returned
   */
  public String getLicenseFingerprint() {
    return licenseCache.getFingerprint();
  }

  /**
   * Get the application limit in the license, if no license, 0 will be returned
   */
  public int getApplicationCountLimit() {
    return licenseCache.getApplicationLimit();
  }

  public boolean hasPolicyMonitoring() {
    String[] features = licenseCache.getFeatures();
    if (features != null) {
      for (String feature : features) {
        if (FEATURE_POLICY_MONITORING.equals(feature)) {
          return true;
        }
      }
    }
    return false;
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
      String msg = "CLM is not licensed!";
      log.error(msg);
      throw new InvalidLicenseException(msg);
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

    if (enforcementPoints.size() == 1) {
      throw new InvalidLicenseException("The enforcement point " + enforcementPoints.iterator().next()
          + " is not licensed!");
    }

    throw new InvalidLicenseException("None of the enforcement points " + enforcementPoints + " is licensed!");
  }

  Set<CLMEnforcementPoint> getEnforcementPoints() {
    Set<CLMEnforcementPoint> enforcementPoints = EnumSet.noneOf(CLMEnforcementPoint.class);
    Collections.addAll(enforcementPoints, licenseCache.getEnforcementPoints());
    return enforcementPoints;
  }

  public LicenseSummary getLicenseSummary() {
    return new LicenseSummary(this.licenseCache.expirationTimestamp, this.licenseCache.getFeatures());
  }

  private void populateLicenseCache() throws LicensingException {
    ProductLicenseKey key = licenseManager.getLicenseDetails();

    licenseManager.verifyFeature(key, new CLMFeature());

    String licenseFingerprint = licenseFingerprinter.calculate(key);

    int version = getVersion(key);

    Integer applicationCount = getApplicationLimit(key);

    Set<CLMEnforcementPoint> enforcementPoints = EnumSet.noneOf(CLMEnforcementPoint.class);
    String[] enforcementPointIds = getPropertyNotNull(key, ProductLicenseDetails.PROPERTY_ENFORCEMENT_POINTS)
        .split(",");
    for (String enforcementPointId : enforcementPointIds) {
      enforcementPointId = enforcementPointId.trim();
      try {
        enforcementPoints.add(CLMEnforcementPoint.valueOf(enforcementPointId));
      }
      catch (IllegalArgumentException e) {
        log.warn("License enables unknown enforcement point {}, ignored", enforcementPointId);
      }
    }

    Set<String> products = getProducts(key);

    Set<String> features = new LinkedHashSet<String>();
    if (version < 1) {
      // legacy license without product info
      if (!isNexusClmLicense(enforcementPoints)) {
        features.add(FEATURE_POLICY_MONITORING);
      }
    }
    else {
      // new license with product info
      if (products.contains(ProductLicenseDetails.PRODUCT_RISK)
          || products.contains(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION)) {
        features.add(FEATURE_POLICY_MONITORING);
      }
    }

    licenseCache = new CachedLicenseData(licenseFingerprint, version, applicationCount, products,
        features.toArray(new String[features.size()]), enforcementPoints, key.getExpirationDate().getTime());
  }

  private static boolean isNexusClmLicense(Set<CLMEnforcementPoint> enforcementPoints) {
    enforcementPoints = EnumSet.copyOf(enforcementPoints);
    enforcementPoints.remove(CLMEnforcementPoint.StageRelease);
    enforcementPoints.remove(CLMEnforcementPoint.Release);
    enforcementPoints.remove(CLMEnforcementPoint.Procure);
    return enforcementPoints.isEmpty();
  }

  private String getPropertyNotNull(ProductLicenseKey key, String property) throws LicensingException {
    String value = getProperty(key, property);
    if (value == null) {
      throw new LicensingException(key, "License lacks property " + property, null);
    }
    return value;
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
    String prop = getPropertyNotNull(key, ProductLicenseDetails.PROPERTY_APPLICATION_LIMIT);
    try {
      return Integer.decode(prop);
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

  private void clearLicenseCache() {
    licenseCache = new CachedLicenseData(null, 0, 0, Collections.<String> emptySet(), new String[0],
        Collections.<CLMEnforcementPoint> emptySet(), 0);
  }
}
