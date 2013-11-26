/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.actions.NotifyActionType;
import com.sonatype.insight.brain.model.policy.actions.WarnActionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;

public class PolicyValidationTest
{
  private String applicationId = "PolicyTest_AppId";

  @Test
  public void testValidate_NameNull() {
    Policy policy = new Policy();
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);
    ValidationResult result = policy.validate(applicationId);
    assertValidationResultHasErrors(result, "The policy name is required.");
  }

  @Test
  public void testValidate_NameEmpty() {
    Policy policy = new Policy();
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);
    policy.setName(" ");
    ValidationResult result = policy.validate(applicationId);
    assertValidationResultHasErrors(result, "The policy name is required.");
  }

  @Test
  public void testValidate_NameWhitespace() {
    Policy policy = new Policy();
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);
    policy.setName(" Leading Space");
    ValidationResult result = policy.validate(applicationId);
    assertValidationResultHasErrors(result,
        "The policy name must not have leading or trailing spaces, or have two spaces in a row.");
    policy.setName("Trailing Space ");
    result = policy.validate(applicationId);
    assertValidationResultHasErrors(result,
        "The policy name must not have leading or trailing spaces, or have two spaces in a row.");
    policy.setName("Multiple  Spaces");
    result = policy.validate(applicationId);
    assertValidationResultHasErrors(result,
        "The policy name must not have leading or trailing spaces, or have two spaces in a row.");
  }

  @Test
  public void testValidate_NameInvalidChar() {
    Policy policy = new Policy();
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);
    String[] invalidAlphaNumericNames = { "!", "@", "#", "$", "%", "^", "&", "*", "(", "_", "+" };
    for (String name : invalidAlphaNumericNames) {
      policy.setName(name);
      ValidationResult result = policy.validate(applicationId);
      assertValidationResultHasErrors(result, "The policy name must be alpha numeric.");
    }
  }

  @Test
  public void testValidate_NameLength() {
    Policy policy = new Policy();
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    String name = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH);
    policy.addConstraint(constraint);

    policy.setName(name + "a");
    ValidationResult result = policy.validate(applicationId);
    assertValidationResultHasErrors(result, "The policy name must be " + NameHelper.MAX_NAME_LENGTH + " characters or less.");

    policy.setName(name);
    result = policy.validate(applicationId);
    Assert.assertTrue(result.isValid());
  }

  @Test
  public void testValidate_NoConstraints() {
    Policy policy = new Policy();
    policy.setName("Policy Name");
    ValidationResult result = policy.validate(applicationId);
    assertValidationResultHasErrors(result, "Policy 'Policy Name' has no constraints");
  }

  @Test
  public void testValidate_ConstraintNameNull() {
    Policy policy = new Policy("PolicyId", "Policy Name");
    Constraint constraint = new Constraint("Constraint Id", null /* name */, LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);
    ValidationResult result = policy.validate(applicationId);
    assertValidationResultHasErrors(result, "Policy 'Policy Name' has invalid constraints:",
        "The constraint name must not be null or empty");
  }

  @Test
  public void testValidate_ConstraintNameEmpty() {
    Policy policy = new Policy("PolicyId", "Policy Name");
    Constraint constraint = new Constraint("Constraint Id", " " /* name */, LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);
    ValidationResult result = policy.validate(applicationId);
    assertValidationResultHasErrors(result, "Policy 'Policy Name' has invalid constraints:",
        "The constraint name must not be null or empty");
  }

  @Test
  public void testValidate_ConstraintNameDuplicate() {
    Policy policy = new Policy("PolicyId", "Policy Name");
    Constraint constraint1 = new Constraint("Constraint Id 1", "Constraint Name", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint1);
    Constraint constraint2 = new Constraint("Constraint Id 2", "Constraint Name", LogicalOperator.AND);
    constraint2.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint2);
    ValidationResult result = policy.validate(applicationId);
    assertValidationResultHasErrors(result, "Policy 'Policy Name' has invalid constraints:",
        "Duplicate constraint name 'Constraint Name'");
  }

  @Test
  public void testValidate_ConstraintNoConditions() {
    Policy policy = new Policy("PolicyId", "Policy Name");
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    policy.addConstraint(constraint);
    ValidationResult result = policy.validate(applicationId);
    assertValidationResultHasErrors(result, "Policy 'Policy Name' has invalid constraints:",
        "Constraint 'Constraint Name' has no conditions");
  }

  @Test
  public void testValidate_ConditionTypeIdNull() {
    Policy policy = new Policy("PolicyId", "Policy Name");
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(null /* conditionTypeId */, "present"));
    policy.addConstraint(constraint);
    ValidationResult result = policy.validate(applicationId);
    assertConditionValidationResult("Invalid condition type id: 'null'", result);
  }

  @Test
  public void testValidate_ConditionTypeIdEmpty() {
    Policy policy = new Policy("PolicyId", "Policy Name");
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(" " /* conditionTypeId */, "present"));
    policy.addConstraint(constraint);
    ValidationResult result = policy.validate(applicationId);
    assertConditionValidationResult("Invalid condition type id: ' '", result);
  }

  @Test
  public void testValidate_ConditionTypeIdInvalid() {
    Policy policy = new Policy("PolicyId", "Policy Name");
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition("abc" /* conditionTypeId */, "present"));
    policy.addConstraint(constraint);
    ValidationResult result = policy.validate(applicationId);
    assertConditionValidationResult("Invalid condition type id: 'abc'", result);
  }

  @Test
  public void testValidate_ConditionNoOperator() {
    Policy policy = new Policy("PolicyId", "Policy Name");
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, null /* operator */));
    policy.addConstraint(constraint);
    ValidationResult result = policy.validate(applicationId);
    assertConditionValidationResult("Invalid condition 'SecurityVulnerability null null', Operator is null", result);
  }

  @Test
  public void testValidate_UnknownStageType() {
    Policy policy = new Policy("PolicyId", "Policy Name");
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);
    
    Action action = new Action(FailActionType.ID);
    HashMap<String, List<Action>> invalidStage = new HashMap<String, List<Action>>();
    invalidStage.put("unknown stage type", Arrays.asList(action));
    policy.setActions(invalidStage);

    ValidationResult result = policy.validate(applicationId);
    assertValidationResultHasErrors(result, "Policy 'Policy Name' has invalid actions:",
        "Invalid stage type id: 'unknown stage type'");

    // Fix the stage and validate again
    HashMap<String, List<Action>> validStage = new HashMap<String, List<Action>>();
    validStage.put(BuildStageType.ID, Arrays.asList(action));
    policy.setActions(validStage);
    result = policy.validate(applicationId);
    Assert.assertTrue(result.isValid());
  }
  
  @Test
  public void testValidate_UnknownActionType() {
    Policy policy = new Policy("PolicyId", "Policy Name");
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);
    Action action = new Action("unknown action type");
    policy.addAction(BuildStageType.ID, action);
    ValidationResult result = policy.validate(applicationId);
    assertValidationResultHasErrors(result, "Policy 'Policy Name' has invalid actions:",
        "Invalid action type id: 'unknown action type'");

    // Fix the action and validate again
    action.setActionTypeId(FailActionType.ID);
    result = policy.validate(applicationId);
    Assert.assertTrue(result.isValid());
  }

  @Test
  public void testValidate_NotifyActionTypeWithBrain1_6ConcatenatedAddresses() {
      Policy policy = new Policy("PolicyId", "Policy Name");
      Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
      constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
      policy.addConstraint(constraint);
      Action action = new Action(NotifyActionType.ID);
      action.setTarget("one@1.com,two@2.com");
      policy.addAction(BuildStageType.ID, action);
      ValidationResult result = policy.validate(applicationId);
      assertValidationResultHasErrors(result, "Policy 'Policy Name' has invalid actions:",
          "Invalid action 'Notify': A valid email target is required");
  }

  @Test
  public void testValidate_NotifyActionType() {
    Policy policy = new Policy("PolicyId", "Policy Name");
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);
    Action action = new Action(NotifyActionType.ID);
    policy.addAction(BuildStageType.ID, action);
    ValidationResult result = policy.validate(applicationId);
    assertValidationResultHasErrors(result, "Policy 'Policy Name' has invalid actions:",
        "Invalid action 'Notify': A target is required");

    // Fix the action and validate again
    action.setTarget("tester@sonatype.com");
    result = policy.validate(applicationId);
    Assert.assertTrue(result.isValid());
  }

  @Test
  public void testValidate_FailActionType() {
    Policy policy = new Policy("PolicyId", "Policy Name");
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);
    Action action = new Action(FailActionType.ID);
    action.setTarget("abc");
    policy.addAction(BuildStageType.ID, action);
    ValidationResult result = policy.validate(applicationId);
    assertValidationResultHasErrors(result, "Policy 'Policy Name' has invalid actions:",
        "Invalid action 'Fail': This action does not support targets");

    // Fix the action and validate again
    action.setTarget(null);
    result = policy.validate(applicationId);
    Assert.assertTrue(result.isValid());
  }

  @Test
  public void testValidate_WarnActionType() {
    Policy policy = new Policy("PolicyId", "Policy Name");
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);
    Action action = new Action(WarnActionType.ID);
    action.setTarget("abc");
    policy.addAction(BuildStageType.ID, action);
    ValidationResult result = policy.validate(applicationId);
    assertValidationResultHasErrors(result, "Policy 'Policy Name' has invalid actions:",
        "Invalid action 'Warn': This action does not support targets");

    // Fix the action and validate again
    action.setTarget(null);
    result = policy.validate(applicationId);
    Assert.assertTrue(result.isValid());
  }

  private void assertValidationResultHasErrors(ValidationResult result, String... errors) {
    Assert.assertNotNull(result);
    Assert.assertFalse(result.isValid());
    Assert.assertEquals(result.toMessageString(), errors.length, result.getErrors().size());
    for (int i = 0; i < errors.length; i++) {
      Assert.assertEquals(errors[i], result.getErrors().get(i));
    }
  }

  private void assertConditionValidationResult(String error, ValidationResult result) {
    Assert.assertNotNull(result);
    Assert.assertFalse(result.isValid());
    Assert.assertEquals(3, result.getErrors().size());
    Assert.assertEquals("Policy 'Policy Name' has invalid constraints:", result.getErrors().get(0));
    Assert.assertEquals("Constraint 'Constraint Name' has invalid conditions:", result.getErrors().get(1));
    Assert.assertEquals(error, result.getErrors().get(2));
  }
}
