/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.actions;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.model.policy.ActionType;

public class WarnActionType
    implements ActionType
{
  public static final String ID = Action.ID_WARN;

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "Warn";
  }

  @Override
  public String getSummary(String stageTypeId) {
    if ("proxy".equalsIgnoreCase(stageTypeId)) {
      return "Proxy Warning";
    }
    return "Build Warning";
  }
}
