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
import javax.validation.constraints.NotNull;

import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.license.model.LicensedFeature;
import org.sonatype.licensing.product.ProductLicenseKey;

/**
 * Describes the currently installed product license.
 */
@Named
@Singleton
public class DefaultProductLicense
    implements ProductLicense
{
  static class ProductLicenseData
  {
    private final String fingerprint;

    private final long expirationTimestamp;

    private final String contactName;

    private final String contactCompany;

    private final String contactEmail;

    private final Set<String> products;

    private final Set<LicensedFeature> features;

    private final Set<StageType> stageTypes;

    private final ProductLicensingModel licensingModel;

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
        ProductLicensingModel licensingModel,
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
      this.licensingModel = licensingModel;
      this.maxApplications = maxApplications;
      this.maxUsers = maxUsers;
      this.maxFirewallUsers = maxFirewallUsers;
    }
  }

  private volatile ProductLicenseData productLicenseData = initialProductLicenseData();

  @Override
  public void clear() {
    productLicenseData = initialProductLicenseData();
  }

  @Override
  public void set(
      ProductLicenseKey productLicenseKey,
      String fingerprint,
      Set<String> products,
      Set<LicensedFeature> features,
      Set<StageType> stageTypes,
      ProductLicensingModel licensingModel,
      Integer maxApplications,
      Integer maxUsers,
      Integer maxFirewallUsers)
  {
    productLicenseData = new ProductLicenseData(fingerprint, productLicenseKey.getExpirationDate().getTime(),
        productLicenseKey.getContactName(), productLicenseKey.getContactCompany(),
        productLicenseKey.getContactEmailAddress(), Collections.unmodifiableSet(products),
        Collections.unmodifiableSet(features), Collections.unmodifiableSet(stageTypes), licensingModel, maxApplications,
        maxUsers, maxFirewallUsers);
  }

  /**
   * Get whether the license is currently valid and not expired.
   */
  @Override
  public boolean isValid() {
    return getFingerprint() != null && getExpirationTimestamp() > System.currentTimeMillis();
  }

  @Override
  public void validate() {
    if (!isValid()) {
      String msg = "The product license has expired.";
      if (getFingerprint() == null) {
        msg = "No valid product license installed.";
      }
      throw new InvalidLicenseException(msg);
    }
  }

  @Override
  public long getExpirationTimestamp() {
    return getProductLicenseData().expirationTimestamp;
  }

  /**
   * Get the fingerprint of the license or {@code null} if there is no license.
   */
  @Override
  public String getFingerprint() {
    return getProductLicenseData().fingerprint;
  }

  @Override
  public String getContactName() {
    return getProductLicenseData().contactName;
  }

  @Override
  public String getContactCompany() {
    return getProductLicenseData().contactCompany;
  }

  @Override
  public String getContactEmail() {
    return getProductLicenseData().contactEmail;
  }

  /**
   * Gets the {@code ProductLicenseDetails.PRODUCT_*} values in the license.
   */
  @Override
  public Set<String> getProducts() {
    return getProductLicenseData().products;
  }

  @Override
  public Set<LicensedFeature> getFeatures() {
    return getProductLicenseData().features;
  }

  @Override
  public boolean hasFeature(LicensedFeature feature) {
    return getFeatures().contains(feature);
  }

  @Override
  public boolean hasProduct(String product) {
    return getProducts().contains(product);
  }

  @Override
  public void validateFeature(LicensedFeature feature) {
    if (!hasFeature(feature)) {
      throw new InvalidLicenseException();
    }
  }

  @Override
  public Set<StageType> getStageTypes() {
    return getProductLicenseData().stageTypes;
  }

  @Override
  public ProductLicensingModel getLicensingModel() {
    return getProductLicenseData().licensingModel;
  }

  /**
   * Get the application limit (if any) in the license or 0 if there is no license.
   */
  @Override
  public Integer getMaxApplications() {
    return getProductLicenseData().maxApplications;
  }

  @Override
  public Integer getMaxUsers() {
    return getProductLicenseData().maxUsers;
  }

  @Override
  public Integer getMaxFirewallUsers() {
    return getProductLicenseData().maxFirewallUsers;
  }

  ProductLicenseData getProductLicenseData() {
    return productLicenseData;
  }

  @NotNull
  static ProductLicenseData initialProductLicenseData() {
    return new ProductLicenseData(null, 0, null, null, null, Collections.emptySet(),
        Collections.emptySet(), Collections.emptySet(), ProductLicensingModel.APP_BASED, 0, 0, 0);
  }
}
