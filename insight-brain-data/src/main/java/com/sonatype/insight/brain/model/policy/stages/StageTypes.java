/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
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

  public static final StageType BUILD = new BuildStageType();

  public static final StageType STAGE_RELEASE = new StageReleaseStageType();

  public static final StageType RELEASE = new ReleaseStageType();

  public static final StageType OPERATE = new OperateStageType();

  private static final Map<String, StageType> allStageTypes = new LinkedHashMap<String, StageType>();

  static {
    add(DEVELOP);
    add(BUILD);
    add(STAGE_RELEASE);
    add(RELEASE);
    add(OPERATE);
  }

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

  public static boolean isIgnoredForDashboard(String stageTypeId) {
    return DevelopStageType.ID.equals(stageTypeId);
  }
}
