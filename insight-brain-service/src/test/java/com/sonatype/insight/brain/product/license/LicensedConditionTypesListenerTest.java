/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.conditions.HygieneRatingConditionType;
import com.sonatype.insight.brain.model.policy.conditions.IntegrityRatingConditionType;
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
    assertThat(ConditionTypes.getById(HygieneRatingConditionType.ID).isEnabled()).isTrue();

    testProductLicense.setMissingFeatures(LicensedFeature.HYGIENE);
    licensedConditionTypesListener.productLicenseChanged();
    assertThat(ConditionTypes.getById(HygieneRatingConditionType.ID).isEnabled()).isFalse();
  }

  @Test
  public void test_IntegrityRatingConditionType() {
    testProductLicense.setFeatures(LicensedFeature.RELEASE_INTEGRITY);
    licensedConditionTypesListener.productLicenseChanged();
    assertThat(ConditionTypes.getById(IntegrityRatingConditionType.ID).isEnabled()).isTrue();

    testProductLicense.setMissingFeatures(LicensedFeature.RELEASE_INTEGRITY);
    licensedConditionTypesListener.productLicenseChanged();
    assertThat(ConditionTypes.getById(IntegrityRatingConditionType.ID).isEnabled()).isFalse();
  }
}
