/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.actions;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.model.policy.ActionType;
import com.sonatype.insight.brain.model.policy.ValidationResult;

abstract class AbstractActionType
    implements ActionType
{
  @Override
  public ValidationResult validateAction(Action action) {
    if (isRequiresTarget()) {
      if (action.getTarget() == null || action.getTarget().trim().isEmpty()) {
        return new ValidationResult("Invalid action '" + getName() + "': A target is required");
      }
    }
    else {
      if (action.getTarget() != null) {
        return new ValidationResult("Invalid action '" + getName() + "': This action does not support targets");
      }
    }

    return null;
  }
}
