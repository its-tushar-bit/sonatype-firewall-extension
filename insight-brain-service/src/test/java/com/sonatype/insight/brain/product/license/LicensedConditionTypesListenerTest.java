/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LicensedConditionTypesListenerTest
    extends AbstractComponentTest
{
  @Inject
  private TestProductLicense testProductLicense;

  @Inject
  private LicensedConditionTypesListener licensedConditionTypesListener;

  @Test
  public void test_HygieneConditionType() {
    testProductLicense.setFeatures(LicensedFeature.HYGIENE);
    licensedConditionTypesListener.productLicenseChanged();
    assertThat(ConditionTypes.getAll()).contains(ConditionTypes.HygieneRatingConditionType);

    testProductLicense.setMissingFeatures(LicensedFeature.HYGIENE);
    licensedConditionTypesListener.productLicenseChanged();
    assertThat(ConditionTypes.getAll()).doesNotContain(ConditionTypes.HygieneRatingConditionType);
  }
}
