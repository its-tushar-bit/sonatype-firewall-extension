/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.model.policy.stages.StageTypes;

import static java.util.stream.Collectors.toList;

public class ActionDTO
{
  public String stageId;

  public String actionType;

  public ActionDTO(final String stageId, final String actionType) {
    this.stageId = stageId;
    this.actionType = actionType;
  }

  public static List<ActionDTO> transcribe(final Map<String, String> actions) {
    return StageTypes.getAll()
        .stream()
        .map(stageType -> new ActionDTO(stageType.getId(), actions.getOrDefault(stageType.getId(), "none")))
        .collect(toList());
  }
}
