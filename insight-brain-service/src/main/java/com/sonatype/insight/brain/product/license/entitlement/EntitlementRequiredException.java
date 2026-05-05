/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license.entitlement;

import com.sonatype.insight.license.model.LicensedFeature;

/**
 * Thrown when a tier-gated entitlement is not available.
 * Carries feature identity and upsell metadata for a rich 402 JSON response.
 * <p>
 * Separate from {@link com.sonatype.insight.brain.product.license.InvalidLicenseException}
 * which handles general license validation (expired, missing, etc.).
 */
public class EntitlementRequiredException
    extends RuntimeException
{
  private static final long serialVersionUID = 1L;

  private final LicensedFeature feature;

  private final UpsellInfo upsellInfo;

  public EntitlementRequiredException(LicensedFeature feature) {
    this(feature, EntitlementUpsellRegistry.getUpsellInfo(feature));
  }

  private EntitlementRequiredException(LicensedFeature feature, UpsellInfo upsellInfo) {
    super(upsellInfo.getMessage());
    this.feature = feature;
    this.upsellInfo = upsellInfo;
  }

  public LicensedFeature getFeature() {
    return feature;
  }

  public UpsellInfo getUpsellInfo() {
    return upsellInfo;
  }
}
