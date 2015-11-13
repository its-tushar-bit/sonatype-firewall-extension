/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.license.model.CLMEnforcementPoint;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;

/**
 * @since 1.11
 */
@Named
public class StageTypeService
{
  public static final String ALL_CONTEXT = "all";

  public static final String CI_CONTEXT = "ci";

  public static final String CLI_CONTEXT = "cli";

  public static final String QA_CONTEXT = "qa";

  public static final String RM_CONTEXT = "rm";

  public static final String MAVEN_CONTEXT = "maven";

  public static final String DASHBOARD_CONTEXT = "dashboard";

  private final CLMLicenseManager licenseManager;

  private final Map<String, Predicate<StageType>> contextFilterMap = new HashMap<>();

  @Inject
  public StageTypeService(final CLMLicenseManager licenseManager) {
    this.licenseManager = licenseManager;
    contextFilterMap.put(ALL_CONTEXT, Predicates.<StageType>alwaysTrue());
    contextFilterMap.put(CI_CONTEXT, new CiFilter());
    contextFilterMap.put(CLI_CONTEXT, new BuildFilter());
    contextFilterMap.put(QA_CONTEXT, new CiFilter());
    contextFilterMap.put(RM_CONTEXT, new CiFilter());
    contextFilterMap.put(MAVEN_CONTEXT, new BuildFilter());
    contextFilterMap.put(DASHBOARD_CONTEXT, new DashboardFilter());
  }

  /**
   * Using details here https://docs.sonatype.com/display/ProdMgmt/Product+License+Matrix to map the product to
   * available StageTypes
   *
   * @return all StageType objects allowed by the current license in natural order of occurrence during the component
   *         lifecycle.
   */
  public Collection<StageType> getLicensedStageTypes() {
    return getLicensedStageTypes(ALL_CONTEXT);
  }

  /**
   * Using details here https://docs.sonatype.com/display/ProdMgmt/Product+Licensing to map the product to
   * available StageTypes
   * 
   * @return all StageType objects allowed by the current license in natural order of occurrence during the component
   *         lifecycle filtered by the supplied context.
   * @since 1.13
   */
  public Collection<StageType> getLicensedStageTypes(final String context) {
    Predicate<StageType> filter = contextFilterMap.get(context);
    if (filter == null) {
      throw new IllegalArgumentException("Invalid context " + context);
    }
    Collection<StageType> allowed = orderStages(calculateLicensedStages());
    allowed = Collections2.filter(allowed, filter);
    return Collections.unmodifiableCollection(allowed);
  }

  private Collection<StageType> calculateLicensedStages() {
    Collection<StageType> allowed = new HashSet<>();

    if (licenseManager.hasProduct(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION)) {
      // all allowed
      allowed.addAll(StageTypes.getAll());
    }

    if (licenseManager.hasProduct(ProductLicenseDetails.PRODUCT_RISK)) {
      allowed.add(StageTypes.RELEASE);
    }

    if (licenseManager.hasProduct(ProductLicenseDetails.PRODUCT_NEXUS)) {
      allowed.add(StageTypes.STAGE_RELEASE);
      allowed.add(StageTypes.RELEASE);
    }

    if (licenseManager.hasProduct(ProductLicenseDetails.PRODUCT_FIREWALL)) {
      allowed.add(StageTypes.STAGE_RELEASE);
      allowed.add(StageTypes.RELEASE);
    }

    if (allowed.isEmpty()) {
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
    allowed.add(StageTypes.PROXY);

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

  class CiFilter
      implements Predicate<StageType>
  {
    @Override
    public boolean apply(@Nullable final StageType input) {
      if (input == null) {
        return false;
      }
      return !DevelopStageType.ID.equals(input.getId()) && !ProxyStageType.ID.equals(input.getId());
    }
  }

  private static class BuildFilter
      implements Predicate<StageType>
  {

    @Override
    public boolean apply(StageType input) {
      if (input == null) {
        return false;
      }
      return !ProxyStageType.ID.equals(input.getId());
    }
  }

  private static class DashboardFilter
      implements Predicate<StageType>
  {

    @Override
    public boolean apply(StageType input) {
      if (input == null) {
        return false;
      }
      return !StageTypes.isIgnoredForDashboard(input.getId());
    }
  }
}
