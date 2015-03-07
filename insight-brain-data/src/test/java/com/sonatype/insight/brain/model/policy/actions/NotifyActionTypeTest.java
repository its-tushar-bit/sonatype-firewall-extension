/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.actions;

import com.sonatype.clm.dto.model.policy.NotifyAction;
import com.sonatype.insight.brain.model.ValidationResult;
import com.sonatype.insight.brain.model.policy.ActionType;
import com.sonatype.insight.brain.model.security.Role;

import org.junit.Test;

import static com.sonatype.insight.brain.model.policy.ValidationAssert.assertValidationResultHasErrors;
import static com.sonatype.insight.brain.model.policy.ValidationAssert.assertValidationResultHasNoErrors;

public class NotifyActionTypeTest
{
  private ActionType actionType = ActionTypes.getById(NotifyActionType.ID);

  @Test
  public void testValidateAction_WithBrain1_6ConcatenatedAddresses() {
    NotifyAction action = new NotifyAction("one@1.com,two@2.com", null /* targetType */);
    ValidationResult result = actionType.validateAction(action);
    assertValidationResultHasErrors(result,
        "Invalid action 'Notify': A valid e-mail address is required instead of: one@1.com,two@2.com");
  }

  @Test
  public void testValidateAction_WithNullEmail() {
    NotifyAction action = new NotifyAction(null /* email */, null /* targetType */);
    ValidationResult result = actionType.validateAction(action);
    assertValidationResultHasErrors(result, "Invalid action 'Notify': A valid e-mail address is required");

    // Fix the action and validate again
    action.setTarget("tester@sonatype.com");
    result = actionType.validateAction(action);
    assertValidationResultHasNoErrors(result);
  }

  @Test
  public void testValidateAction_WithEmptyEmail() {
    NotifyAction action = new NotifyAction("  " /* email */, null /* targetType */);
    ValidationResult result = actionType.validateAction(action);
    assertValidationResultHasErrors(result, "Invalid action 'Notify': A valid e-mail address is required");

    // Fix the action and validate again
    action.setTarget("tester@sonatype.com");
    result = actionType.validateAction(action);
    assertValidationResultHasNoErrors(result);
  }

  @Test
  public void testValidateAction_WithInvalidEmail() {
    NotifyAction action = new NotifyAction("bad email address", null /* targetType */);
    ValidationResult result = actionType.validateAction(action);
    assertValidationResultHasErrors(result,
        "Invalid action 'Notify': A valid e-mail address is required instead of: bad email address");

    // Fix the action and validate again
    action.setTarget("tester@sonatype.com");
    result = actionType.validateAction(action);
    assertValidationResultHasNoErrors(result);
  }

  @Test
  public void testValidateAction_WithNullTargetType() {
    NotifyAction action = new NotifyAction("tester@sonatype.com", null /* targetType */);
    ValidationResult result = actionType.validateAction(action);
    assertValidationResultHasNoErrors(result);
  }

  @Test
  public void testValidateAction_WithEmptyTargetType() {
    NotifyAction action = new NotifyAction("tester@sonatype.com", " " /* targetType */);
    ValidationResult result = actionType.validateAction(action);
    assertValidationResultHasNoErrors(result);
  }

  @Test
  public void testValidateAction_WithInvalidTargetType() {
    NotifyAction action = new NotifyAction("hello", "Not a target type");
    ValidationResult result = actionType.validateAction(action);
    assertValidationResultHasErrors(result, "Invalid action 'Notify': Invalid target type: 'Not a target type'");

    // Fix the action and validate again
    action.setTarget(Role.ADMIN_ROLE_ID);
    action.setTargetType(NotifyActionType.TARGET_TYPE_ROLE);
    result = actionType.validateAction(action);
    assertValidationResultHasNoErrors(result);
  }

  @Test
  public void testValidateAction_WithNullRole() {
    NotifyAction action = new NotifyAction(null, NotifyActionType.TARGET_TYPE_ROLE);
    ValidationResult result = actionType.validateAction(action);
    assertValidationResultHasErrors(result, "Invalid action 'Notify': A valid role ID is required");

    // Fix the action and validate again
    action.setTarget(Role.ADMIN_ROLE_ID);
    result = actionType.validateAction(action);
    assertValidationResultHasNoErrors(result);
  }

  @Test
  public void testValidateAction_WithEmptyRole() {
    NotifyAction action = new NotifyAction(" ", NotifyActionType.TARGET_TYPE_ROLE);
    ValidationResult result = actionType.validateAction(action);
    assertValidationResultHasErrors(result, "Invalid action 'Notify': A valid role ID is required");

    // Fix the action and validate again
    action.setTarget(Role.ADMIN_ROLE_ID);
    result = actionType.validateAction(action);
    assertValidationResultHasNoErrors(result);
  }

  @Test
  public void testValidateAction_WithInvalidRole() {
    NotifyAction action = new NotifyAction("Hamlet", NotifyActionType.TARGET_TYPE_ROLE);
    ValidationResult result = actionType.validateAction(action);
    assertValidationResultHasErrors(result, "Invalid action 'Notify': A valid role ID is required instead of: Hamlet");

    // Fix the action and validate again
    action.setTarget(Role.ADMIN_ROLE_ID);
    result = actionType.validateAction(action);
    assertValidationResultHasNoErrors(result);
  }
}
