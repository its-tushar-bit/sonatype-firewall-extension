/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.actions;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.sonatype.insight.brain.model.policy.ActionType;

public class ActionTypes
{
  private static final Map<String, ActionType> allActionTypes = new LinkedHashMap<>();

  static {
    add(new FailActionType());
    add(new WarnActionType());
    add(new NotifyActionType());
  }

  public static Collection<ActionType> getAll() {
    return allActionTypes.values();
  }

  private static void add(final ActionType actionType) {
    if (allActionTypes.keySet().contains(actionType.getId())) {
      throw new IllegalStateException("Duplicate action type id: " + actionType.getId());
    }
    allActionTypes.put(actionType.getId(), actionType);
  }

  public static ActionType getById(final String actionTypeId) {
    // TODO throw exception if actionTypeId is unknown
    return allActionTypes.get(actionTypeId);
  }
}
