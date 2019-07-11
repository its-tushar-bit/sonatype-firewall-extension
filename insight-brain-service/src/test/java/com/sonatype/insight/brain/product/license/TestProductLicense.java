/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.features.LicensedFeature;
import com.sonatype.insight.brain.model.policy.StageType;

@Named
@Singleton
public class TestProductLicense
    extends ProductLicense
{
  private final TestProductLicenseManager testProductLicenseManager;

  @Inject
  public TestProductLicense(TestProductLicenseManager testProductLicenseManager) {
    this.testProductLicenseManager = testProductLicenseManager;
  }

  @Override
  public Set<LicensedFeature> getFeatures() {
    // features are normally derived based on the products
    // for precise testing, we allow them to be manually overridden to a specific set
    Set<LicensedFeature> features = testProductLicenseManager.getFeatures();
    if (features != null) {
      return features;
    }
    return super.getFeatures();
  }

  @Override
  public Set<StageType> getStageTypes() {
    // stage types are normally derived based on the products
    // for precise testing, we allow them to be manually overridden to a specific set
    Set<StageType> stageTypes = testProductLicenseManager.getStageTypes();
    if (stageTypes != null) {
      return stageTypes;
    }
    return super.getStageTypes();
  }
}
