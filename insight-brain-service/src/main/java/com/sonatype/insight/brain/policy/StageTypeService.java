/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.license.model.CLMEnforcementPoint;
import com.sonatype.insight.license.model.ProductLicenseDetails;

/**
 * @since 1.11
 */
@Named
public class StageTypeService
{
  private final CLMLicenseManager licenseManager;

  @Inject
  public StageTypeService(final CLMLicenseManager licenseManager) {
    this.licenseManager = licenseManager;
  }

  /**
   * Using details here https://docs.sonatype.com/display/ProdMgmt/Product+License+Matrix to map the product to
   * available StageTypes
   * 
   * @return all StageType objects allowed by the current license in natural order of occurrence during the component
   *         lifecycle.
   */
  public Collection<StageType> getLicensedStageTypes() {
    Collection<StageType> stageTypes = StageTypes.getAll();
    Collection<String> allowed = new HashSet<>();

    if (licenseManager.hasProduct(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION)) {
      // all allowed
    }
    else if (licenseManager.hasProduct(ProductLicenseDetails.PRODUCT_RISK)) {
      allowed.add(StageTypes.RELEASE.getId());
    }
    else if (licenseManager.hasProduct(ProductLicenseDetails.PRODUCT_NEXUS)) {
      allowed.add(StageTypes.STAGE_RELEASE.getId());
      allowed.add(StageTypes.RELEASE.getId());
    }
    else {
      // if no product is defined, we are dealing with legacy license
      if (licenseManager.hasEnforcementPoint(CLMEnforcementPoint.Build)) {
        allowed.add(StageTypes.BUILD.getId());
      }
      if (licenseManager.hasEnforcementPoint(CLMEnforcementPoint.Develop)) {
        allowed.add(StageTypes.DEVELOP.getId());
      }
      if (licenseManager.hasEnforcementPoint(CLMEnforcementPoint.Release)) {
        allowed.add(StageTypes.RELEASE.getId());
      }
      if (licenseManager.hasEnforcementPoint(CLMEnforcementPoint.StageRelease)) {
        allowed.add(StageTypes.STAGE_RELEASE.getId());
      }
      if (!licenseManager.isLegacyNexusClmLicense()) {
        allowed.add(StageTypes.OPERATE.getId());
      }
    }

    if (!allowed.isEmpty()) {
      Collection<StageType> filtered = new ArrayList<>();
      for (StageType stageType : stageTypes) {
        if (allowed.contains(stageType.getId())) {
          filtered.add(stageType);
        }
      }
      stageTypes = Collections.unmodifiableCollection(filtered);
    }

    return stageTypes;
  }
}
