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
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.policy.stages.HostedStageType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.product.license.ProductLicense;

/**
 * @since 1.11
 */
@Named
public class StageTypeService
{
  public static final String ALL_CONTEXT = "all";

  public static final String LIFECYCLE_CONTEXT = "lifecycle";

  public static final String CI_CONTEXT = "ci";

  public static final String CLI_CONTEXT = "cli";

  public static final String QA_CONTEXT = "qa";

  public static final String RM_CONTEXT = "rm";

  public static final String MAVEN_CONTEXT = "maven";

  public static final String DASHBOARD_CONTEXT = "dashboard";

  public static final String SBOM_CONTEXT = "sbom";

  private final ProductLicense productLicense;

  private final Map<String, Predicate<StageType>> contextFilterMap = new HashMap<>();

  @Inject
  public StageTypeService(final ProductLicense productLicense) {
    this.productLicense = productLicense;
    // HOSTED is excluded from every classic context until hosted-repository synchronous
    // enforcement (CLM-39870) ships its own dedicated context. The HOSTED stage exists in the
    // registry (StageTypes.getAll()) and may be added to a license set explicitly, but it never
    // appears in CI/Maven/Dashboard/Lifecycle/All enumerations alongside the classic stages.
    contextFilterMap.put(ALL_CONTEXT, new ClassicStagesFilter());
    contextFilterMap.put(CI_CONTEXT, new BuildFilter());
    contextFilterMap.put(CLI_CONTEXT, new BuildFilter());
    contextFilterMap.put(QA_CONTEXT, new RMFilter());
    contextFilterMap.put(RM_CONTEXT, new RMFilter());
    contextFilterMap.put(MAVEN_CONTEXT, new BuildFilter());
    contextFilterMap.put(DASHBOARD_CONTEXT, new DashboardFilter());
    contextFilterMap.put(SBOM_CONTEXT, new SbomFilter());
    contextFilterMap.put(LIFECYCLE_CONTEXT, new LifecycleFilter());
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
    Collection<StageType> allowed = orderStages(productLicense.getStageTypes());
    allowed = allowed.stream().filter(filter).collect(Collectors.toList());
    return Collections.unmodifiableCollection(allowed);
  }

  public Set<String> getValidSuccessMetricsStageTypeIds() {
    return this.getLicensedStageTypes()
        .stream()
        .map(StageType::getId)
        .filter(stageTypeId -> !StageTypes.isIgnoredForPolicyViolationAggregation(stageTypeId))
        .collect(Collectors.toSet());
  }

  /**
   * Orders the given stages by their natural chronological order. This is the same order as {@link StageTypes#getAll()}
   * .
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

  /**
   * Keeps everything except {@link HostedStageType}; used for {@link #ALL_CONTEXT}.
   */
  private static class ClassicStagesFilter
      implements Predicate<StageType>
  {
    @Override
    public boolean test(StageType input) {
      return !HostedStageType.ID.equals(input.getId());
    }
  }

  class RMFilter
      implements Predicate<StageType>
  {
    @Override
    public boolean test(@Nullable final StageType input) {
      return !DevelopStageType.ID.equals(input.getId()) && !ProxyStageType.ID.equals(input.getId()) &&
          !ComplianceStageType.ID.equals(input.getId()) && !HostedStageType.ID.equals(input.getId());
    }
  }

  private static class BuildFilter
      implements Predicate<StageType>
  {
    @Override
    public boolean test(StageType input) {
      return !ProxyStageType.ID.equals(input.getId()) && !ComplianceStageType.ID.equals(input.getId()) &&
          !HostedStageType.ID.equals(input.getId());
    }
  }

  private static class SbomFilter
      implements Predicate<StageType>
  {
    @Override
    public boolean test(StageType input) {
      return ComplianceStageType.ID.equals(input.getId());
    }
  }

  /**
   * Note: deliberate split with {@link StageTypes#isIgnoredForDashboard(String)}.
   * <p>
   * {@code isIgnoredForDashboard} is a low-level predicate (DEVELOP / PROXY / COMPLIANCE) that
   * does not encode CLM-39870 hosted-stage suppression. {@code DashboardFilter} is the
   * authoritative gate for the dashboard context until a dedicated {@code HOSTED_CONTEXT}
   * is wired up — it adds the {@link HostedStageType} exclusion explicitly. Direct callers of
   * {@code isIgnoredForDashboard} that bypass {@link StageTypeService#getLicensedStageTypes}
   * will <b>not</b> get HOSTED stripped automatically; route through the service instead.
   */
  private static class DashboardFilter
      implements Predicate<StageType>
  {
    @Override
    public boolean test(StageType input) {
      return !StageTypes.isIgnoredForDashboard(input.getId()) && !HostedStageType.ID.equals(input.getId());
    }
  }

  private static class LifecycleFilter
      implements Predicate<StageType>
  {
    @Override
    public boolean test(StageType input) {
      return !ComplianceStageType.ID.equals(input.getId()) && !HostedStageType.ID.equals(input.getId());
    }
  }
}
