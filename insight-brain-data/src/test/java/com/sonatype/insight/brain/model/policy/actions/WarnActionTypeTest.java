/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.actions;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.model.ValidationResult;
import com.sonatype.insight.brain.model.policy.ActionType;

import org.junit.Test;

import static com.sonatype.insight.brain.model.policy.ValidationAssert.assertValidationResultHasErrors;
import static com.sonatype.insight.brain.model.policy.ValidationAssert.assertValidationResultHasNoErrors;

public class WarnActionTypeTest
{
  private ActionType actionType = ActionTypes.getById(WarnActionType.ID);

  @Test
  public void testValidateAction_TypeWithTarget() {
    Action action = new Action(actionType.getId());
    action.setTarget("abc");
    ValidationResult result = actionType.validateAction(action);
    assertValidationResultHasErrors(result, "Invalid action 'Warn': This action does not support targets");

    // Fix the action and validate again
    action.setTarget(null);
    result = actionType.validateAction(action);
    assertValidationResultHasNoErrors(result);
  }

  @Test
  public void testValidateAction_WithTargetType() {
    Action action = new Action(actionType.getId());
    action.setTargetType("abc");
    ValidationResult result = actionType.validateAction(action);
    assertValidationResultHasErrors(result, "Invalid action 'Warn': This action does not support target types");

    // Fix the action and validate again
    action.setTargetType(null);
    result = actionType.validateAction(action);
    assertValidationResultHasNoErrors(result);
  }
}
