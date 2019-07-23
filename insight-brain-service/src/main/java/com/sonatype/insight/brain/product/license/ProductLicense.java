/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.util.Collections;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.license.model.LicensedFeature;

import org.sonatype.licensing.product.ProductLicenseKey;

/**
 * Describes the currently installed product license.
 */
@Named
@Singleton
public class ProductLicense
{
  private static class ProductLicenseData
  {
    private final String fingerprint;

    private final long expirationTimestamp;

    private final String contactName;

    private final String contactCompany;

    private final String contactEmail;

    private final Set<String> products;

    private final Set<LicensedFeature> features;

    private final Set<StageType> stageTypes;

    private final Integer maxApplications;

    private final Integer maxUsers;

    private final Integer maxFirewallUsers;

    public ProductLicenseData(
        String fingerprint,
        long expirationTimestamp,
        String contactName,
        String contactCompany,
        String contactEmail,
        Set<String> products,
        Set<LicensedFeature> features,
        Set<StageType> stageTypes,
        Integer maxApplications,
        Integer maxUsers,
        Integer maxFirewallUsers)
    {
      this.fingerprint = fingerprint;
      this.expirationTimestamp = expirationTimestamp;
      this.contactName = contactName;
      this.contactCompany = contactCompany;
      this.contactEmail = contactEmail;
      this.products = products;
      this.features = features;
      this.stageTypes = stageTypes;
      this.maxApplications = maxApplications;
      this.maxUsers = maxUsers;
      this.maxFirewallUsers = maxFirewallUsers;
    }
  }

  private volatile ProductLicenseData productLicenseData;

  public ProductLicense() {
    clear();
  }

  void clear() {
    productLicenseData = new ProductLicenseData(null, 0, null, null, null, Collections.emptySet(),
        Collections.emptySet(), Collections.emptySet(), 0, 0, 0);
  }

  void set(
      ProductLicenseKey productLicenseKey,
      String fingerprint,
      Set<String> products,
      Set<LicensedFeature> features,
      Set<StageType> stageTypes,
      Integer maxApplications,
      Integer maxUsers,
      Integer maxFirewallUsers)
  {
    productLicenseData = new ProductLicenseData(fingerprint, productLicenseKey.getExpirationDate().getTime(),
        productLicenseKey.getContactName(), productLicenseKey.getContactCompany(),
        productLicenseKey.getContactEmailAddress(), Collections.unmodifiableSet(products),
        Collections.unmodifiableSet(features), Collections.unmodifiableSet(stageTypes), maxApplications, maxUsers,
        maxFirewallUsers);
  }

  /**
   * Get whether the license is currently valid and not expired.
   */
  public boolean isValid() {
    return getFingerprint() != null && getExpirationTimestamp() > System.currentTimeMillis();
  }

  public void validate() {
    if (!isValid()) {
      String msg = "The product license has expired.";
      if (getFingerprint() == null) {
        msg = "No valid product license installed.";
      }
      throw new InvalidLicenseException(msg);
    }
  }

  public long getExpirationTimestamp() {
    return productLicenseData.expirationTimestamp;
  }

  /**
   * Get the fingerprint of the license or {@code null} if there is no license.
   */
  public String getFingerprint() {
    return productLicenseData.fingerprint;
  }

  public String getContactName() {
    return productLicenseData.contactName;
  }

  public String getContactCompany() {
    return productLicenseData.contactCompany;
  }

  public String getContactEmail() {
    return productLicenseData.contactEmail;
  }

  /**
   * Gets the {@code ProductLicenseDetails.PRODUCT_*} values in the license.
   */
  public Set<String> getProducts() {
    return productLicenseData.products;
  }

  public Set<LicensedFeature> getFeatures() {
    return productLicenseData.features;
  }

  public boolean hasFeature(LicensedFeature feature) {
    return getFeatures().contains(feature);
  }

  public void validateFeature(LicensedFeature feature) {
    if (!hasFeature(feature)) {
      throw new InvalidLicenseException();
    }
  }

  public Set<StageType> getStageTypes() {
    return productLicenseData.stageTypes;
  }

  /**
   * Get the application limit (if any) in the license or 0 if there is no license.
   */
  public Integer getMaxApplications() {
    return productLicenseData.maxApplications;
  }

  public Integer getMaxUsers() {
    return productLicenseData.maxUsers;
  }

  public Integer getMaxFirewallUsers() {
    return productLicenseData.maxFirewallUsers;
  }
}
