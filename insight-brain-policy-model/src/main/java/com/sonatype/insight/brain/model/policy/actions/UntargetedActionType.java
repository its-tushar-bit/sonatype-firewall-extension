/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.actions;

import java.util.List;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.model.policy.ActionType;
import com.sonatype.insight.brain.model.policy.ValidationResult;

/**
 * An {@link ActionType} that does not support targets.
 * 
 * @since 1.7
 */
abstract class UntargetedActionType
    implements ActionType
{
  @Override
  public boolean isRequiresTarget() {
    return false;
  }

  @Override
  public List<String> getAvailableTargets() {
    return null;
  }

  @Override
  public ValidationResult validateAction(Action action) {
    ValidationResult result = new ValidationResult();
    
    if (action.getTarget() != null) {
      result.addError("Invalid action '" + getName() + "': This action does not support targets");
    }

    return result;
  }
}
