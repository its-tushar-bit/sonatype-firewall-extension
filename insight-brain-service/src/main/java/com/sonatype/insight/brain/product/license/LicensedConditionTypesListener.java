/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.license.model.LicensedFeature;

@Named
@Singleton
public class LicensedConditionTypesListener
    implements ProductLicenseListener
{
  private final ProductLicense productLicense;

  @Inject
  public LicensedConditionTypesListener(final ProductLicense productLicense) {
    this.productLicense = productLicense;
  }

  @Override
  public void productLicenseChanged() {
    if (productLicense.hasFeature(LicensedFeature.HYGIENE)) {
      ConditionTypes.enableConditionType(ConditionTypes.HygieneRatingConditionType);
    }
    else {
      ConditionTypes.disableConditionType(ConditionTypes.HygieneRatingConditionType);
    }
    if (productLicense.hasFeature(LicensedFeature.RELEASE_INTEGRITY)) {
      ConditionTypes.enableConditionType(ConditionTypes.IntegrityRatingConditionType);
    }
    else {
      ConditionTypes.disableConditionType(ConditionTypes.IntegrityRatingConditionType);
    }
    if (productLicense.hasFeature(LicensedFeature.INFRASTRUCTURE_AS_CODE_PACK)) {
      ConditionTypes.enableConditionType(ConditionTypes.IacControlConditionType);
    }
    else {
      ConditionTypes.disableConditionType(ConditionTypes.IacControlConditionType);
    }
  }
}
