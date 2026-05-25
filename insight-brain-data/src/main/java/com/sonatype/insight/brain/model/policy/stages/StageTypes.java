/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.stages;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.sonatype.insight.brain.model.policy.StageType;

public class StageTypes
{
  public static final StageType DEVELOP = new DevelopStageType();

  public static final StageType SOURCE = new SourceStageType();

  public static final StageType BUILD = new BuildStageType();

  public static final StageType STAGE_RELEASE = new StageReleaseStageType();

  public static final StageType RELEASE = new ReleaseStageType();

  public static final StageType OPERATE = new OperateStageType();

  public static final StageType PROXY = new ProxyStageType();

  public static final StageType HOSTED = new HostedStageType();

  public static final StageType COMPLIANCE = new ComplianceStageType();

  private static final Map<String, StageType> allStageTypes = new LinkedHashMap<>();

  static {
    add(PROXY);
    add(HOSTED);
    add(DEVELOP);
    add(SOURCE);
    add(BUILD);
    add(STAGE_RELEASE);
    add(RELEASE);
    add(OPERATE);
    add(COMPLIANCE);
  }

  /**
   * Gets all stage types in chronological order of appearance during the component lifecycle.
   */
  public static Collection<StageType> getAll() {
    return Collections.unmodifiableCollection(allStageTypes.values());
  }

  private static void add(final StageType stageType) {
    if (allStageTypes.keySet().contains(stageType.getId())) {
      throw new IllegalStateException("Duplicate stage type id: " + stageType.getId());
    }
    allStageTypes.put(stageType.getId(), stageType);
  }

  public static StageType getById(final String stageTypeId) {
    // TODO throw exception if stageTypeId is unknown
    return allStageTypes.get(stageTypeId);
  }

  /**
   * Low-level predicate: returns {@code true} for the classic stages that the dashboard
   * historically suppresses (DEVELOP / PROXY / COMPLIANCE).
   * <p>
   * Note: this predicate does <b>not</b> encode CLM-39870 hosted-stage suppression. Callers
   * that want the full dashboard-context filter should route through
   * {@code StageTypeService.getLicensedStageTypes(DASHBOARD_CONTEXT)}, which composes this
   * predicate with the explicit {@link HostedStageType} exclusion until a dedicated hosted
   * dashboard context is wired up.
   */
  public static boolean isIgnoredForDashboard(String stageTypeId) {
    return DevelopStageType.ID.equals(stageTypeId) || ProxyStageType.ID.equals(stageTypeId) ||
        ComplianceStageType.ID.equals(stageTypeId);
  }

  public static boolean isIgnoredForPolicyViolationAggregation(String stageTypeId) {
    return DevelopStageType.ID.equals(stageTypeId) ||
        ComplianceStageType.ID.equals(stageTypeId) ||
        ProxyStageType.ID.equals(stageTypeId);
  }
}
