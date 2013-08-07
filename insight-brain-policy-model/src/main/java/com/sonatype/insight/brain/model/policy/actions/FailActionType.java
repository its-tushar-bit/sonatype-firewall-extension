/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.actions;

import java.util.List;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.model.policy.ActionType;

public class FailActionType
    extends AbstractActionType
    implements ActionType
{
  public static final String ID = Action.ID_FAIL;

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "Fail";
  }

  @Override
  public List<String> getAvailableTargets() {
    return null;
  }

  @Override
  public boolean isRequiresTarget() {
    return false;
  }

  @Override
  public String getSummary() {
    return "Build Failed";
  }
}
