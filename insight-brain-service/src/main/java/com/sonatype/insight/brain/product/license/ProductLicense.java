/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.util.Set;

import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.license.model.LicensedFeature;
import org.sonatype.licensing.product.ProductLicenseKey;

public interface ProductLicense
{
  void clear();

  void set(
      ProductLicenseKey productLicenseKey,
      String fingerprint,
      Set<String> products,
      Set<LicensedFeature> features,
      Set<StageType> stageTypes,
      Set<ProductLicensingModel> licensingModels,
      Integer maxApplications,
      Integer maxUsers,
      Integer maxFirewallUsers,
      Integer maxSboms);

  /**
   * Get whether the license is currently valid and not expired.
   */
  boolean isValid();

  void validate();

  long getExpirationTimestamp();

  /**
   * Get the fingerprint of the license or {@code null} if there is no license.
   */
  String getFingerprint();

  String getContactName();

  String getContactCompany();

  String getContactEmail();

  /**
   * Gets the {@code ProductLicenseDetails.PRODUCT_*} values in the license.
   */
  Set<String> getProducts();

  Set<LicensedFeature> getFeatures();

  boolean hasFeature(LicensedFeature feature);

  boolean hasProduct(String product);

  void validateFeature(LicensedFeature feature);

  void validateFeatures(LicensedFeature... features);

  Set<StageType> getStageTypes();

  Set<ProductLicensingModel> getLicensingModels();

  /**
   * Get the application limit (if any) in the license or 0 if there is no license.
   */
  Integer getMaxApplications();

  Integer getMaxUsers();

  Integer getMaxFirewallUsers();

  Integer getMaxSboms();
}
