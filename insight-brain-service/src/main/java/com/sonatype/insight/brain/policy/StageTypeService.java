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

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
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

  // Filters whose decision does not depend on the HOSTED_REPOSITORY_EVALUATION feature flag are
  // cached here. The flag-dependent ALL_CONTEXT and LIFECYCLE_CONTEXT filters are built per
  // request inside getLicensedStageTypes(...) so the flag is read at most once per call rather
  // than once per stream element (the predicate's test() method).
  private final Map<String, Predicate<StageType>> flagIndependentFilterMap = new HashMap<>();

  @Inject
  public StageTypeService(final ProductLicense productLicense) {
    this.productLicense = productLicense;
    // HOSTED is excluded from every classic context until hosted-repository synchronous
    // enforcement (CLM-39870) ships its own dedicated context. The HOSTED stage exists in the
    // registry (StageTypes.getAll()) and may be added to a license set explicitly, but it never
    // appears in CI/Maven/Dashboard enumerations alongside the classic stages.
    flagIndependentFilterMap.put(CI_CONTEXT, new BuildFilter());
    flagIndependentFilterMap.put(CLI_CONTEXT, new BuildFilter());
    flagIndependentFilterMap.put(QA_CONTEXT, new RMFilter());
    flagIndependentFilterMap.put(RM_CONTEXT, new RMFilter());
    flagIndependentFilterMap.put(MAVEN_CONTEXT, new BuildFilter());
    flagIndependentFilterMap.put(DASHBOARD_CONTEXT, new DashboardFilter());
    flagIndependentFilterMap.put(SBOM_CONTEXT, new SbomFilter());
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
    Predicate<StageType> filter = resolveFilter(context);
    if (filter == null) {
      throw new IllegalArgumentException("Invalid context " + context);
    }
    Collection<StageType> allowed = orderStages(productLicense.getStageTypes());
    allowed = allowed.stream().filter(filter).collect(Collectors.toList());
    return Collections.unmodifiableCollection(allowed);
  }

  /**
   * Returns the filter for the given context. For ALL_CONTEXT and LIFECYCLE_CONTEXT the filter
   * depends on {@code HOSTED_REPOSITORY_EVALUATION} — its value is read here once per call so
   * the predicate's {@code test(...)} method does not open a database transaction per stream
   * element. Other contexts return cached, flag-independent filter instances.
   */
  private Predicate<StageType> resolveFilter(final String context) {
    if (ALL_CONTEXT.equals(context)) {
      return new ClassicStagesFilter(
          SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.isEnabled());
    }
    if (LIFECYCLE_CONTEXT.equals(context)) {
      return new LifecycleFilter(
          SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.isEnabled());
    }
    return flagIndependentFilterMap.get(context);
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
   * Keeps everything; used for {@link #ALL_CONTEXT}.
   *
   * <p>
   * HOSTED is gated on {@code HOSTED_REPOSITORY_EVALUATION} so customers who have not
   * opted into hosted-repository synchronous enforcement (CLM-39870) see the unchanged
   * pre-feature stage list, while customers who enable the flag see HOSTED alongside the
   * classic stages — matching PMQ-HRE-001's "Option 1 / keep distinct stage" assumption.
   *
   * <p>
   * The flag value is captured at construction time by the caller of {@link #resolveFilter}
   * so {@link #test} is a pure boolean comparison with no I/O.
   */
  private static class ClassicStagesFilter
      implements Predicate<StageType>
  {
    private final boolean hostedEnforcementEnabled;

    ClassicStagesFilter(final boolean hostedEnforcementEnabled) {
      this.hostedEnforcementEnabled = hostedEnforcementEnabled;
    }

    @Override
    public boolean test(StageType input) {
      if (HostedStageType.ID.equals(input.getId())) {
        return hostedEnforcementEnabled;
      }
      return true;
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

  /**
   * Keeps everything except {@link ComplianceStageType}; used for {@link #LIFECYCLE_CONTEXT}.
   *
   * <p>
   * HOSTED is gated on {@code HOSTED_REPOSITORY_EVALUATION} so the policy-editor
   * Actions matrix shows the Hosted column only for customers who have enabled
   * synchronous hosted-repository enforcement (CLM-39870). With the flag off, the
   * matrix matches its pre-feature shape and customers see no surprise column.
   *
   * <p>
   * The flag value is captured at construction time by the caller of {@link #resolveFilter}
   * so {@link #test} is a pure boolean comparison with no I/O.
   */
  private static class LifecycleFilter
      implements Predicate<StageType>
  {
    private final boolean hostedEnforcementEnabled;

    LifecycleFilter(final boolean hostedEnforcementEnabled) {
      this.hostedEnforcementEnabled = hostedEnforcementEnabled;
    }

    @Override
    public boolean test(StageType input) {
      if (HostedStageType.ID.equals(input.getId())) {
        return hostedEnforcementEnabled;
      }
      return !ComplianceStageType.ID.equals(input.getId());
    }
  }
}
