/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.NotifyAction;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.NameHelperTest;
import com.sonatype.insight.brain.model.ValidationResult;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.actions.NotifyActionType;
import com.sonatype.insight.brain.model.policy.actions.WarnActionType;
import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.conditions.RelativePopularityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.json.store.JsonUtils;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import static com.sonatype.insight.brain.model.policy.ValidationAssert.assertValidationResultHasErrors;
import static com.sonatype.insight.brain.model.policy.ValidationAssert.assertValidationResultHasNoErrors;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class PolicyTest
{
  private String applicationId = "PolicyTest_AppId";

  @Test
  public void testGetThreatCategory_Security() {
    Policy policy = new Policy(null, "PolicyTest");
    Constraint constraint1 = new Constraint(null, "Constraint1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(MatchStateConditionType.ID, "is", "exact"));
    policy.addConstraint(constraint1);
    Constraint constraint2 = new Constraint(null, "Constraint2", LogicalOperator.AND);
    constraint2.addCondition(new Condition(AgeInDaysConditionType.ID, "older than", "1"));
    policy.addConstraint(constraint2);
    Constraint constraint3 = new Constraint(null, "Constraint3", LogicalOperator.AND);
    constraint3.addCondition(new Condition(LicenseConditionType.ID, "is", "Apache-2.0"));
    policy.addConstraint(constraint3);
    Constraint constraint4 = new Constraint(null, "Constraint4", LogicalOperator.AND);
    constraint4.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint4);

    assertThat(policy.getThreatCategory(), is(PolicyThreatCategory.SECURITY));
  }

  @Test
  public void testGetThreatCategory_License() {
    Policy policy = new Policy(null, "PolicyTest");
    Constraint constraint1 = new Constraint(null, "Constraint1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(MatchStateConditionType.ID, "is", "exact"));
    policy.addConstraint(constraint1);
    Constraint constraint2 = new Constraint(null, "Constraint2", LogicalOperator.AND);
    constraint2.addCondition(new Condition(AgeInDaysConditionType.ID, "older than", "1"));
    policy.addConstraint(constraint2);
    Constraint constraint3 = new Constraint(null, "Constraint3", LogicalOperator.AND);
    constraint3.addCondition(new Condition(LicenseConditionType.ID, "is", "Apache-2.0"));
    policy.addConstraint(constraint3);

    assertThat(policy.getThreatCategory(), is(PolicyThreatCategory.LICENSE));
  }

  @Test
  public void testGetThreatCategory_Quality_Age() {
    Policy policy = new Policy(null, "PolicyTest");
    Constraint constraint1 = new Constraint(null, "Constraint1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(MatchStateConditionType.ID, "is", "exact"));
    policy.addConstraint(constraint1);
    Constraint constraint2 = new Constraint(null, "Constraint2", LogicalOperator.AND);
    constraint2.addCondition(new Condition(AgeInDaysConditionType.ID, "older than", "1"));
    policy.addConstraint(constraint2);

    assertThat(policy.getThreatCategory(), is(PolicyThreatCategory.QUALITY));
  }

  @Test
  public void testGetThreatCategory_Quality_Popularity() {
    Policy policy = new Policy(null, "PolicyTest");
    Constraint constraint1 = new Constraint(null, "Constraint1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(MatchStateConditionType.ID, "is", "exact"));
    policy.addConstraint(constraint1);
    Constraint constraint2 = new Constraint(null, "Constraint2", LogicalOperator.AND);
    constraint2.addCondition(new Condition(RelativePopularityConditionType.ID, "=", "10"));
    policy.addConstraint(constraint2);

    assertThat(policy.getThreatCategory(), is(PolicyThreatCategory.QUALITY));
  }

  @Test
  public void testGetThreatCategory_Other() {
    Policy policy = new Policy(null, "PolicyTest");
    Constraint constraint1 = new Constraint(null, "Constraint1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(MatchStateConditionType.ID, "is", "exact"));
    policy.addConstraint(constraint1);

    assertThat(policy.getThreatCategory(), is(PolicyThreatCategory.OTHER));
  }

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
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      policy.setName(name);
      ValidationResult result = policy.validate(applicationId);
      assertValidationResultHasErrors(result,
          String.format(NameHelper.INVALID_CHAR_MESSAGE, "The policy name", name.charAt(0)));
    }
  }

  @Test
  public void testValidate_NameValidChars() {
    Policy policy = new Policy();
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);
    for (String name : NameHelperTest.VALID_NAMES) {
      policy.setName(name);
      ValidationResult result = policy.validate(applicationId);
      assertValidationResultHasNoErrors(result);
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
    assertValidationResultHasErrors(result, "The policy name must be " + NameHelper.MAX_NAME_LENGTH
        + " characters or less.");

    policy.setName(name);
    result = policy.validate(applicationId);
    assertValidationResultHasNoErrors(result);
  }

  @Test
  public void testValidate_NoConstraints() {
    Policy policy = new Policy();
    policy.setName("Policy Name");
    ValidationResult result = policy.validate(applicationId);
    assertValidationResultHasErrors(result, "Policy 'Policy Name' has no constraints");
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
  public void testValidate_ConstraintInvalid() {
    Policy policy = new Policy("PolicyId", "Policy Name");
    Constraint constraint = new Constraint("Constraint Id 1", null, LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);
    ValidationResult result = policy.validate(applicationId);
    assertValidationResultHasErrors(result, "Policy 'Policy Name' has invalid constraints:",
        "The constraint name must not be null or empty");
  }

  @Test
  public void testValidate_StageTypeUnknown() {
    Policy policy = new Policy("PolicyId", "Policy Name");
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);

    Action action = new Action(FailActionType.ID);
    HashMap<String, List<Action>> invalidStage = new HashMap<>();
    invalidStage.put("unknown stage type", Arrays.asList(action));
    policy.setActions(invalidStage);

    ValidationResult result = policy.validate(applicationId);
    assertValidationResultHasErrors(result, "Policy 'Policy Name' has invalid actions:",
        "Invalid stage type id: 'unknown stage type'");

    // Fix the stage and validate again
    HashMap<String, List<Action>> validStage = new HashMap<>();
    validStage.put(BuildStageType.ID, Arrays.asList(action));
    policy.setActions(validStage);
    result = policy.validate(applicationId);
    assertValidationResultHasNoErrors(result);
  }

  @Test
  public void testValidate_ActionTypeUnknown() {
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
    assertValidationResultHasNoErrors(result);
  }

  @Test
  public void testValidate_ActionTypeFailVsWarn() {
    Policy policy = new Policy("PolicyId", "Policy Name");
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);
    policy.addAction(BuildStageType.ID, new Action(WarnActionType.ID));
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));
    policy.addAction(BuildStageType.ID, new Action(NotifyActionType.ID, "foo@bar.com"));
    ValidationResult result = policy.validate(applicationId);
    assertValidationResultHasErrors(result, "Policy 'Policy Name' has invalid actions:",
        "Ambiguous action types: [warn, fail]");

    // Fix the actions and validate again
    policy.getActions(BuildStageType.ID).remove(0);
    result = policy.validate(applicationId);
    assertValidationResultHasNoErrors(result);
  }

  @Test
  public void testValidate_ActionTypeInvalid() {
    Policy policy = new Policy("PolicyId", "Policy Name");
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);
    Action action = new Action(FailActionType.ID);
    action.setTarget("invalid");
    policy.addAction(BuildStageType.ID, action);
    ValidationResult result = policy.validate(applicationId);
    assertValidationResultHasErrors(result, "Policy 'Policy Name' has invalid actions:",
        "Invalid action 'Fail': This action does not support targets");

    // Fix the actions and validate again
    action.setTarget(null);
    result = policy.validate(applicationId);
    assertValidationResultHasNoErrors(result);
  }

  @Test
  public void testValidate_MonitorNotifyActionInvalid() {
    Policy policy = new Policy("PolicyId", "Policy Name");
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);
    NotifyAction notifyAction = new NotifyAction("  " /* email */, null /* targetType */);
    policy.addMonitorNotifyAction(notifyAction);
    ValidationResult result = policy.validate(applicationId);
    assertValidationResultHasErrors(result, "Policy 'Policy Name' has invalid monitor notification actions:",
        "Invalid action 'Notify': A valid e-mail address is required");

    // Fix the action and validate again
    notifyAction.setTarget("tester@sonatype.com");
    result = policy.validate(applicationId);
    assertValidationResultHasNoErrors(result);
  }

  @Test
  public void testValidate_threatLevelValidation() {
    Policy policy = new Policy();
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);

    policy.setName("testValidate_threatLevelInvalid");
    policy.setThreatLevel(-1);
    ValidationResult result = policy.validate(applicationId);
    assertValidationResultHasErrors(result,
        "Policy 'testValidate_threatLevelInvalid' has threat level outside of valid range 0-10: -1");

    policy.setThreatLevel(11);
    result = policy.validate(applicationId);
    assertValidationResultHasErrors(result,
        "Policy 'testValidate_threatLevelInvalid' has threat level outside of valid range 0-10: 11");

    policy.setThreatLevel(0);
    result = policy.validate(applicationId);
    assertValidationResultHasNoErrors(result);

    policy.setThreatLevel(10);
    result = policy.validate(applicationId);
    assertValidationResultHasNoErrors(result);
  }

  @Test
  public void testGetDroolsCode_ExcludedFromJson() throws Exception {
    Policy policy = new Policy("PolicyId", "Policy Name");
    policy.setDroolsCode("unwanted");
    policy = JsonUtils.parse(JsonUtils.format(policy), Policy.class);
    assertThat(policy.getDroolsCode(), is(nullValue()));
  }
}
