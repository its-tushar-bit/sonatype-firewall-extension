/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.NotNull;

import com.sonatype.insight.brain.developer.integrationdashboard.DeveloperEnablementService;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import org.sonatype.licensing.product.ProductLicenseKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes the currently installed product license.
 */
@Named
@Singleton
public class DefaultProductLicense
    implements ProductLicense, CreditAwareProductLicense
{
  private static final Logger log = LoggerFactory.getLogger(DefaultProductLicense.class);

  private final DeveloperEnablementService developerEnablementService;

  @Inject
  public DefaultProductLicense(final DeveloperEnablementService developerEnablementService) {
    this.developerEnablementService = developerEnablementService;
  }

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

    private final Set<ProductLicensingModel> licensingModels;

    private final Integer maxApplications;

    private final Integer maxUsers;

    private final Integer maxFirewallUsers;

    private final Integer maxSboms;

    private final BigDecimal creditAmount;

    public ProductLicenseData(
        String fingerprint,
        long expirationTimestamp,
        String contactName,
        String contactCompany,
        String contactEmail,
        Set<String> products,
        Set<LicensedFeature> features,
        Set<StageType> stageTypes,
        Set<ProductLicensingModel> licensingModels,
        Integer maxApplications,
        Integer maxUsers,
        Integer maxFirewallUsers,
        Integer maxSboms,
        BigDecimal creditAmount)
    {
      this.fingerprint = fingerprint;
      this.expirationTimestamp = expirationTimestamp;
      this.contactName = contactName;
      this.contactCompany = contactCompany;
      this.contactEmail = contactEmail;
      this.products = products;
      this.features = features;
      this.stageTypes = stageTypes;
      this.licensingModels = licensingModels;
      this.maxApplications = maxApplications;
      this.maxUsers = maxUsers;
      this.maxFirewallUsers = maxFirewallUsers;
      this.maxSboms = maxSboms;
      this.creditAmount = creditAmount;
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
      Set<ProductLicensingModel> licensingModels,
      Integer maxApplications,
      Integer maxUsers,
      Integer maxFirewallUsers,
      Integer maxSboms)
  {
    productLicenseData = new ProductLicenseData(fingerprint, productLicenseKey.getExpirationDate().getTime(),
        productLicenseKey.getContactName(), productLicenseKey.getContactCompany(),
        productLicenseKey.getContactEmailAddress(), Collections.unmodifiableSet(products),
        Collections.unmodifiableSet(features), Collections.unmodifiableSet(stageTypes), licensingModels,
        maxApplications, maxUsers, maxFirewallUsers, maxSboms, null);
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
    final Set<String> products = getProductLicenseData().products;
    log.trace("Fetched license products: [{}]", products);
    return products;
  }

  @Override
  public Set<LicensedFeature> getFeatures() {
    return getProductLicenseData().features;
  }

  @Override
  public boolean hasFeature(LicensedFeature feature) {
    if (LicensedFeature.DEVELOPER_DASHBOARD.equals(feature)) {
      return developerEnablementService.shouldEnableDeveloperProduct();
    }
    return getFeatures().contains(feature);
  }

  @Override
  public boolean hasProduct(String product) {
    if (ProductLicenseDetails.PRODUCT_SONATYPE_DEVELOPMENT.equals(product)) {
      return developerEnablementService.shouldEnableDeveloperProduct();
    }
    return getProducts().contains(product);
  }

  @Override
  public void validateFeature(LicensedFeature feature) {
    if (!hasFeature(feature)) {
      log.error("Product license is missing license feature " + feature.name());
      throw new InvalidLicenseException();
    }
  }

  @Override
  public void validateFeatures(final LicensedFeature... features) {
    for (LicensedFeature feature : features) {
      if (hasFeature(feature)) {
        return;
      }
    }
    log.error("Product license is missing any one of " + Arrays.stream(features)
        .map(LicensedFeature::name)
        .collect(
            Collectors.joining(", ")));
    throw new InvalidLicenseException();
  }

  @Override
  public Set<StageType> getStageTypes() {
    return getProductLicenseData().stageTypes;
  }

  @Override
  public Set<ProductLicensingModel> getLicensingModels() {
    return getProductLicenseData().licensingModels;
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

  @Override
  public Integer getMaxSboms() {
    return getProductLicenseData().maxSboms;
  }

  @Override
  public void setCreditAmount(BigDecimal creditAmount) {
    ProductLicenseData current = getProductLicenseData();
    productLicenseData = new ProductLicenseData(
        current.fingerprint, current.expirationTimestamp, current.contactName, current.contactCompany,
        current.contactEmail, current.products, current.features, current.stageTypes, current.licensingModels,
        current.maxApplications, current.maxUsers, current.maxFirewallUsers, current.maxSboms, creditAmount);
  }

  @Override
  public BigDecimal getCreditAmount() {
    return getProductLicenseData().creditAmount;
  }

  ProductLicenseData getProductLicenseData() {
    return productLicenseData;
  }

  @NotNull
  static ProductLicenseData initialProductLicenseData() {
    return new ProductLicenseData(null, 0, null, null, null, Collections.emptySet(), Collections.emptySet(),
        Collections.emptySet(), Collections.singleton(ProductLicensingModel.APP_BASED), 0, 0, 0, 0, null);
  }
}
