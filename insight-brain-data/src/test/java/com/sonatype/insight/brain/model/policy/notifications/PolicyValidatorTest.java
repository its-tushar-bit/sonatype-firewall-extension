/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.notifications;

import java.util.HashMap;

import com.sonatype.insight.brain.AbstractDataTest;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.NameHelperTest;
import com.sonatype.insight.brain.model.ValidationResult;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.ConditionValidator;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.ConstraintValidator;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyValidator;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.security.Role;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.policy.ValidationAssert.assertValidationResultHasErrors;
import static com.sonatype.insight.brain.model.policy.ValidationAssert.assertValidationResultHasNoErrors;

public class PolicyValidatorTest
    extends AbstractDataTest
{
  private PolicyValidator policyValidator;

  private final String applicationId = "PolicyTest_AppId";

  @BeforeEach
  public void before() {
    UserNotificationValidator userNotificationValidator = new UserNotificationValidator();
    RoleNotificationValidator roleNotificationValidator =
        new RoleNotificationValidator(() -> daoFactory.createRoleDAO());
    JiraNotificationValidator jiraNotificationValidator = new JiraNotificationValidator();
    WebhookNotificationValidator webhookNotificationValidator = new WebhookNotificationValidator();
    NotificationsValidator notificationsValidator =
        new NotificationsValidator(userNotificationValidator, roleNotificationValidator, jiraNotificationValidator,
            webhookNotificationValidator);
    ConstraintValidator constraintValidator = new ConstraintValidator(new ConditionValidator());
    policyValidator = new PolicyValidator(constraintValidator, notificationsValidator);
  }

  @Test
  public void testValidate_NameNull() {
    Policy policy = new Policy();
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);
    ValidationResult result = policyValidator.validate(null, policy, applicationId);
    assertValidationResultHasErrors(result, "The policy name is required.");
  }

  @Test
  public void testValidate_NameEmpty() {
    Policy policy = new Policy();
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);
    policy.setName(" ");
    ValidationResult result = policyValidator.validate(null, policy, applicationId);
    assertValidationResultHasErrors(result, "The policy name is required.");
  }

  @Test
  public void testValidate_NameWhitespace() {
    Policy policy = new Policy();
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);
    policy.setName(" Leading Space");
    ValidationResult result = policyValidator.validate(null, policy, applicationId);
    assertValidationResultHasErrors(result,
        "The policy name must not have leading or trailing spaces, or have two spaces in a row.");
    policy.setName("Trailing Space ");
    result = policyValidator.validate(null, policy, applicationId);
    assertValidationResultHasErrors(result,
        "The policy name must not have leading or trailing spaces, or have two spaces in a row.");
    policy.setName("Multiple  Spaces");
    result = policyValidator.validate(null, policy, applicationId);
    assertValidationResultHasErrors(result,
        "The policy name must not have leading or trailing spaces, or have two spaces in a row.");
  }

  @Test
  public void testValidate_NameInvalidChar() {
    Policy policy = new Policy();
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      policy.setName(name);
      ValidationResult result = policyValidator.validate(null, policy, applicationId);
      assertValidationResultHasErrors(result,
          String.format(NameHelper.INVALID_CHAR_MESSAGE, "The policy name", name.charAt(0)));
    }
  }

  @Test
  public void testValidate_NameValidChars() {
    Policy policy = new Policy();
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);
    for (String name : NameHelperTest.VALID_NAMES) {
      policy.setName(name);
      ValidationResult result = policyValidator.validate(null, policy, applicationId);
      assertValidationResultHasNoErrors(result);
    }
  }

  @Test
  public void testValidate_NameLength() {
    Policy policy = new Policy();
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    String name = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH);
    policy.addConstraint(constraint);

    policy.setName(name + "a");
    ValidationResult result = policyValidator.validate(null, policy, applicationId);
    assertValidationResultHasErrors(result, "The policy name must be " + NameHelper.MAX_NAME_LENGTH
        + " characters or less.");

    policy.setName(name);
    result = policyValidator.validate(null, policy, applicationId);
    assertValidationResultHasNoErrors(result);
  }

  @Test
  public void testValidate_NoConstraints() {
    Policy policy = new Policy();
    policy.setName("Policy Name");
    ValidationResult result = policyValidator.validate(null, policy, applicationId);
    assertValidationResultHasErrors(result, "Policy 'Policy Name' has no constraints");
  }

  @Test
  public void testValidate_ConstraintNameDuplicate() {
    Policy policy = new Policy("PolicyId", "Policy Name");
    Constraint constraint1 = new Constraint("Constraint Id 1", "Constraint Name", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint1);
    Constraint constraint2 = new Constraint("Constraint Id 2", "Constraint Name", LogicalOperator.AND);
    constraint2.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint2);
    ValidationResult result = policyValidator.validate(null, policy, applicationId);
    assertValidationResultHasErrors(result, "Policy 'Policy Name' has invalid constraints:",
        "Duplicate constraint name 'Constraint Name'");
  }

  @Test
  public void testValidate_ConstraintInvalid() {
    Policy policy = new Policy("PolicyId", "Policy Name");
    Constraint constraint = new Constraint("Constraint Id 1", null, LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);
    ValidationResult result = policyValidator.validate(null, policy, applicationId);
    assertValidationResultHasErrors(result, "Policy 'Policy Name' has invalid constraints:",
        "The constraint name must not be null or empty");
  }

  @Test
  public void testValidate_StageTypeUnknown() {
    Policy policy = new Policy("PolicyId", "Policy Name");
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);

    HashMap<String, String> invalidStage = new HashMap<>();
    invalidStage.put("unknown stage type", FailActionType.ID);
    policy.setActions(invalidStage);

    ValidationResult result = policyValidator.validate(null, policy, applicationId);
    assertValidationResultHasErrors(result, "Policy 'Policy Name' has invalid actions:",
        "Invalid stage: 'unknown stage type'");

    // Fix the stage and validate again
    HashMap<String, String> validStage = new HashMap<>();
    validStage.put(BuildStageType.ID, FailActionType.ID);
    policy.setActions(validStage);
    result = policyValidator.validate(null, policy, applicationId);
    assertValidationResultHasNoErrors(result);
  }

  @Test
  public void testValidate_ActionTypeUnknown() {
    Policy policy = new Policy("PolicyId", "Policy Name");
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);
    policy.setAction(BuildStageType.ID, "unknown action type");
    ValidationResult result = policyValidator.validate(null, policy, applicationId);
    assertValidationResultHasErrors(result, "Policy 'Policy Name' has invalid actions:",
        "Invalid action for stage 'build': 'unknown action type'");

    // Fix the action and validate again
    policy.setAction(BuildStageType.ID, FailActionType.ID);
    result = policyValidator.validate(null, policy, applicationId);
    assertValidationResultHasNoErrors(result);
  }

  @Test
  public void testValidate_RoleNotificationInvalid() {
    Policy policy = new Policy("PolicyId", "Policy Name");
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);

    // Add invalid notification (no role id) and validate
    RoleNotification notification = new RoleNotification();
    policy.getNotifications().add(notification);
    ValidationResult result = policyValidator.validate(null, policy, applicationId);
    assertValidationResultHasErrors(result, "Policy 'Policy Name' has invalid notifications:",
        "Invalid notification: A valid role ID is required");

    // Change to inexistant role id and validate again
    notification.setRoleId("NoSuchRole");
    result = policyValidator.validate(null, policy, applicationId);
    assertValidationResultHasErrors(result, "Policy 'Policy Name' has invalid notifications:",
        "Invalid notification: 'NoSuchRole' is not a valid role");

    // Fix the notification and validate again
    notification.setRoleId(Role.OWNER_ROLE_ID);
    result = policyValidator.validate(null, policy, applicationId);
    assertValidationResultHasNoErrors(result);
  }

  @Test
  public void testValidate_UserNotificationInvalid() {
    Policy policy = new Policy("PolicyId", "Policy Name");
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);

    // Add invalid notification and validate
    UserNotification notification = new UserNotification();
    policy.getNotifications().add(notification);
    ValidationResult result = policyValidator.validate(null, policy, applicationId);
    assertValidationResultHasErrors(result, "Policy 'Policy Name' has invalid notifications:",
        "Invalid notification: A valid e-mail address is required");

    // Fix the notification and validate again
    notification.setEmailAddress("tester@sonatype.com");
    result = policyValidator.validate(null, policy, applicationId);
    assertValidationResultHasNoErrors(result);
  }

  @Test
  public void testValidate_JiraNotificationInvalid() {
    Policy policy = new Policy("PolicyId", "Policy Name");
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);

    // Add invalid notification and validate
    JiraNotification jiraNotification = new JiraNotification();
    policy.getNotifications().add(jiraNotification);
    ValidationResult result = policyValidator.validate(null, policy, applicationId);
    assertValidationResultHasErrors(result, "Policy 'Policy Name' has invalid notifications:",
        "Invalid JIRA notification: A valid project key is required",
        "Invalid JIRA notification: A valid issue type id is required");

    // Fix the notification and validate again
    jiraNotification.setProjectKey("key");
    jiraNotification.setIssueTypeId(1);
    result = policyValidator.validate(null, policy, applicationId);
    assertValidationResultHasNoErrors(result);
  }

  @Test
  public void testValidate_WebhookNotificationInvalid() {
    Policy policy = new Policy("PolicyId", "Policy Name");
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);

    // Add invalid notification and validate
    WebhookNotification notification = new WebhookNotification();
    policy.getNotifications().add(notification);
    ValidationResult result = policyValidator.validate(null, policy, applicationId);
    assertValidationResultHasErrors(result, "Policy 'Policy Name' has invalid notifications:",
        "Invalid Webhook notification: A valid webhook id is required");

    // Fix the notification and validate again
    notification.setWebhookId("http://localhost");
    result = policyValidator.validate(null, policy, applicationId);
    assertValidationResultHasNoErrors(result);
  }

  @Test
  public void testValidate_threatLevelValidation() {
    Policy policy = new Policy();
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);

    policy.setName("testValidate_threatLevelInvalid");
    policy.setThreatLevel(-1);
    ValidationResult result = policyValidator.validate(null, policy, applicationId);
    assertValidationResultHasErrors(result,
        "Policy 'testValidate_threatLevelInvalid' has threat level outside of valid range 0-10: -1");

    policy.setThreatLevel(11);
    result = policyValidator.validate(null, policy, applicationId);
    assertValidationResultHasErrors(result,
        "Policy 'testValidate_threatLevelInvalid' has threat level outside of valid range 0-10: 11");

    policy.setThreatLevel(0);
    result = policyValidator.validate(null, policy, applicationId);
    assertValidationResultHasNoErrors(result);

    policy.setThreatLevel(10);
    result = policyValidator.validate(null, policy, applicationId);
    assertValidationResultHasNoErrors(result);
  }
}
