/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageReleaseStageType;
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
   * Using details here https://docs.sonatype.com/display/ProdMgmt/Product+License+Matrix
   * to map the product to available StageTypes
   *
   * @return all StageType objects allowed by the current license.
   */
  public Collection<StageType> getLicensedStageTypes() {
    if (licenseManager.hasProduct(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION)) {
      return StageTypes.getAll();
    }
    else if (licenseManager.hasProduct(ProductLicenseDetails.PRODUCT_RISK)) {
      return Collections.singleton(StageTypes.getById(ReleaseStageType.ID));
    }
    else if (licenseManager.hasProduct(ProductLicenseDetails.PRODUCT_NEXUS)) {
      ArrayList<StageType> types = new ArrayList<>();
      types.add(StageTypes.getById(ReleaseStageType.ID));
      types.add(StageTypes.getById(StageReleaseStageType.ID));
      return types;
    }

    //if no product is defined, we are dealing with legacy license
    return getLegacy();
  }

  //simply converting the old enforcement points to stages, fortunately 1-1 mapping
  private Collection<StageType> getLegacy() {
    ArrayList<StageType> types = new ArrayList<>();
    if (licenseManager.hasEnforcementPoint(CLMEnforcementPoint.Build)) {
      types.add(StageTypes.getById(BuildStageType.ID));
    }
    if (licenseManager.hasEnforcementPoint(CLMEnforcementPoint.Develop)) {
      types.add(StageTypes.getById(DevelopStageType.ID));
    }
    if (licenseManager.hasEnforcementPoint(CLMEnforcementPoint.Release)) {
      types.add(StageTypes.getById(ReleaseStageType.ID));
    }
    if (licenseManager.hasEnforcementPoint(CLMEnforcementPoint.StageRelease)) {
      types.add(StageTypes.getById(StageReleaseStageType.ID));
    }

    return types;
  }
}
