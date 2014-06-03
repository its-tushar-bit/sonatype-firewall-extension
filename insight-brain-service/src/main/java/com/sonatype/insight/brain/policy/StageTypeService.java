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
    Collection<StageType> allowed = orderStages(calculateLicensedStages());
    return Collections.unmodifiableCollection(allowed);
  }

  private Collection<StageType> calculateLicensedStages() {
    Collection<StageType> allowed = new HashSet<>();

    if (licenseManager.hasProduct(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION)) {
      // all allowed
      allowed.addAll(StageTypes.getAll());
    }
    else if (licenseManager.hasProduct(ProductLicenseDetails.PRODUCT_RISK)) {
      allowed.add(StageTypes.RELEASE);
    }
    else if (licenseManager.hasProduct(ProductLicenseDetails.PRODUCT_NEXUS)) {
      allowed.add(StageTypes.STAGE_RELEASE);
      allowed.add(StageTypes.RELEASE);
    }
    else {
      // if no product is defined, we are dealing with legacy license
      if (licenseManager.hasEnforcementPoint(CLMEnforcementPoint.Build)) {
        allowed.add(StageTypes.BUILD);
      }
      if (licenseManager.hasEnforcementPoint(CLMEnforcementPoint.Develop)) {
        allowed.add(StageTypes.DEVELOP);
      }
      if (licenseManager.hasEnforcementPoint(CLMEnforcementPoint.Release)) {
        allowed.add(StageTypes.RELEASE);
      }
      if (licenseManager.hasEnforcementPoint(CLMEnforcementPoint.StageRelease)) {
        allowed.add(StageTypes.STAGE_RELEASE);
      }
      if (!licenseManager.isLegacyNexusClmLicense()) {
        allowed.add(StageTypes.OPERATE);
      }
    }
    return allowed;
  }

  /**
   * Orders the given stages by their natural chronological order.  This is the same order as 
   * {@link StageTypes#getAll()}.
   */
  private Collection<StageType> orderStages(Collection<StageType> stagesToOrder) {
    Collection<StageType> ordered = new ArrayList<>();

    for (StageType stageType : StageTypes.getAll()) {
      if (stagesToOrder.contains(stageType)) {
        ordered.add(stageType);
      }
    }

    return ordered;
  }
}
