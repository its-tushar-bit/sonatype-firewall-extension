/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.NameHelperTest;
import com.sonatype.insight.brain.model.ValidationResult;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.conditions.RelativePopularityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.notifications.JiraNotification;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.RoleNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.policy.notifications.WebhookNotification;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.json.store.JsonUtils;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static com.sonatype.clm.dto.model.policy.Action.ID_FAIL;
import static com.sonatype.clm.dto.model.policy.Action.ID_NOTIFY;
import static com.sonatype.clm.dto.model.policy.Action.ID_WARN;
import static com.sonatype.insight.brain.model.policy.ValidationAssert.assertValidationResultHasErrors;
import static com.sonatype.insight.brain.model.policy.ValidationAssert.assertValidationResultHasNoErrors;
import static org.assertj.core.api.Assertions.assertThat;

public class PolicyTest
{
  private final String applicationId = "PolicyTest_AppId";

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
    constraint4.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint4);

    assertThat(policy.getThreatCategory()).isEqualTo(PolicyThreatCategory.SECURITY);
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

    assertThat(policy.getThreatCategory()).isEqualTo(PolicyThreatCategory.LICENSE);
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

    assertThat(policy.getThreatCategory()).isEqualTo(PolicyThreatCategory.QUALITY);
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

    assertThat(policy.getThreatCategory()).isEqualTo(PolicyThreatCategory.QUALITY);
  }

  @Test
  public void testGetThreatCategory_Other() {
    Policy policy = new Policy(null, "PolicyTest");
    Constraint constraint1 = new Constraint(null, "Constraint1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(MatchStateConditionType.ID, "is", "exact"));
    policy.addConstraint(constraint1);

    assertThat(policy.getThreatCategory()).isEqualTo(PolicyThreatCategory.OTHER);
  }

  @Test
  public void testValidate_NameNull() {
    Policy policy = new Policy();
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);
    ValidationResult result = policy.validate(null, applicationId);
    assertValidationResultHasErrors(result, "The policy name is required.");
  }

  @Test
  public void testValidate_NameEmpty() {
    Policy policy = new Policy();
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);
    policy.setName(" ");
    ValidationResult result = policy.validate(null, applicationId);
    assertValidationResultHasErrors(result, "The policy name is required.");
  }

  @Test
  public void testValidate_NameWhitespace() {
    Policy policy = new Policy();
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);
    policy.setName(" Leading Space");
    ValidationResult result = policy.validate(null, applicationId);
    assertValidationResultHasErrors(result,
        "The policy name must not have leading or trailing spaces, or have two spaces in a row.");
    policy.setName("Trailing Space ");
    result = policy.validate(null, applicationId);
    assertValidationResultHasErrors(result,
        "The policy name must not have leading or trailing spaces, or have two spaces in a row.");
    policy.setName("Multiple  Spaces");
    result = policy.validate(null, applicationId);
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
      ValidationResult result = policy.validate(null, applicationId);
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
      ValidationResult result = policy.validate(null, applicationId);
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
    ValidationResult result = policy.validate(null, applicationId);
    assertValidationResultHasErrors(result, "The policy name must be " + NameHelper.MAX_NAME_LENGTH
        + " characters or less.");

    policy.setName(name);
    result = policy.validate(null, applicationId);
    assertValidationResultHasNoErrors(result);
  }

  @Test
  public void testValidate_NoConstraints() {
    Policy policy = new Policy();
    policy.setName("Policy Name");
    ValidationResult result = policy.validate(null, applicationId);
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
    ValidationResult result = policy.validate(null, applicationId);
    assertValidationResultHasErrors(result, "Policy 'Policy Name' has invalid constraints:",
        "Duplicate constraint name 'Constraint Name'");
  }

  @Test
  public void testValidate_ConstraintInvalid() {
    Policy policy = new Policy("PolicyId", "Policy Name");
    Constraint constraint = new Constraint("Constraint Id 1", null, LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);
    ValidationResult result = policy.validate(null, applicationId);
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

    ValidationResult result = policy.validate(null, applicationId);
    assertValidationResultHasErrors(result, "Policy 'Policy Name' has invalid actions:",
        "Invalid stage: 'unknown stage type'");

    // Fix the stage and validate again
    HashMap<String, String> validStage = new HashMap<>();
    validStage.put(BuildStageType.ID, FailActionType.ID);
    policy.setActions(validStage);
    result = policy.validate(null, applicationId);
    assertValidationResultHasNoErrors(result);
  }

  @Test
  public void testValidate_ActionTypeUnknown() {
    Policy policy = new Policy("PolicyId", "Policy Name");
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);
    policy.setAction(BuildStageType.ID, "unknown action type");
    ValidationResult result = policy.validate(null, applicationId);
    assertValidationResultHasErrors(result, "Policy 'Policy Name' has invalid actions:",
        "Invalid action for stage 'build': 'unknown action type'");

    // Fix the action and validate again
    policy.setAction(BuildStageType.ID, FailActionType.ID);
    result = policy.validate(null, applicationId);
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
    ValidationResult result = policy.validate(null, applicationId);
    assertValidationResultHasErrors(result, "Policy 'Policy Name' has invalid notifications:",
        "Invalid notification: A valid role ID is required");

    // Change to inexistant role id and validate again
    notification.setRoleId("NoSuchRole");
    result = policy.validate(null, applicationId);
    assertValidationResultHasErrors(result, "Policy 'Policy Name' has invalid notifications:",
        "Invalid notification: 'NoSuchRole' is not a valid role");

    // Fix the notification and validate again
    notification.setRoleId(Role.OWNER_ROLE_ID);
    result = policy.validate(null, applicationId);
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
    ValidationResult result = policy.validate(null, applicationId);
    assertValidationResultHasErrors(result, "Policy 'Policy Name' has invalid notifications:",
        "Invalid notification: A valid e-mail address is required");

    // Fix the notification and validate again
    notification.setEmailAddress("tester@sonatype.com");
    result = policy.validate(null, applicationId);
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
    ValidationResult result = policy.validate(null, applicationId);
    assertValidationResultHasErrors(result, "Policy 'Policy Name' has invalid notifications:",
        "Invalid JIRA notification: A valid project key is required",
        "Invalid JIRA notification: A valid issue type id is required");

    // Fix the notification and validate again
    jiraNotification.setProjectKey("key");
    jiraNotification.setIssueTypeId(1);
    result = policy.validate(null, applicationId);
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
    ValidationResult result = policy.validate(null, applicationId);
    assertValidationResultHasErrors(result, "Policy 'Policy Name' has invalid notifications:",
        "Invalid Webhook notification: A valid webhook id is required");

    // Fix the notification and validate again
    notification.setWebhookId("http://localhost");
    result = policy.validate(null, applicationId);
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
    ValidationResult result = policy.validate(null, applicationId);
    assertValidationResultHasErrors(result,
        "Policy 'testValidate_threatLevelInvalid' has threat level outside of valid range 0-10: -1");

    policy.setThreatLevel(11);
    result = policy.validate(null, applicationId);
    assertValidationResultHasErrors(result,
        "Policy 'testValidate_threatLevelInvalid' has threat level outside of valid range 0-10: 11");

    policy.setThreatLevel(0);
    result = policy.validate(null, applicationId);
    assertValidationResultHasNoErrors(result);

    policy.setThreatLevel(10);
    result = policy.validate(null, applicationId);
    assertValidationResultHasNoErrors(result);
  }

  @Test
  public void testGetDroolsCode_ExcludedFromJson() throws Exception {
    Policy policy = new Policy("PolicyId", "Policy Name");
    policy.setDroolsCode("unwanted");
    policy = JsonUtils.parse(JsonUtils.format(policy), Policy.class);
    assertThat(policy.getDroolsCode()).isNull();
  }

  @Test
  public void testToActions() {
    Policy policy = new Policy("PolicyId", "PolicyName");
    policy.setPolicyActionsOverrideAllowed(true);
    policy.setAction("Deploy", ID_NOTIFY);
    List<String> ownerIds = new ArrayList<>();
    ownerIds.add("OwnerId");
    ownerIds.add("Root");

    Map<String, Map<String, String>> policyActionsOverrides = new HashMap<>();
    Map<String, String> values = new HashMap<>();
    values.put("Deploy", ID_WARN);
    policyActionsOverrides.put("OwnerId", values);
    policy.setPolicyActionsOverrides(policyActionsOverrides);

    List<Action> deploy = policy.toActions("Deploy", false, ownerIds);

    assertThat(deploy.size()).isOne();
    assertThat(deploy.get(0).getActionTypeId()).isEqualTo(ID_WARN);
  }

  @Test
  public void testGetEffectiveNotifications_NullOverrides() {
    Policy policy = createPolicyWithOverrides("org1", "org2");
    policy.setPolicyNotificationsOverrideAllowed(true);
    policy.setPolicyNotificationsOverrides(null);

    assertThat(policy.getEffectiveNotifications(Arrays.asList("app", "org2", "org1"))).isSameAs(
        policy.getNotifications());
  }

  @Test
  public void testGetEffectiveNotifications_EmptyOverrides() {
    Policy policy = createPolicyWithOverrides("org1", "org2");
    policy.setPolicyNotificationsOverrideAllowed(true);
    policy.setPolicyNotificationsOverrides(new LinkedHashMap<>());

    assertThat(policy.getEffectiveNotifications(Arrays.asList("app", "org2", "org1"))).isSameAs(
        policy.getNotifications());
  }

  @Test
  public void testGetEffectiveNotifications_NullOwnerIds() {
    Policy policy = createPolicyWithOverrides("org1", "org2");
    policy.setPolicyNotificationsOverrideAllowed(true);

    assertThat(policy.getEffectiveNotifications(null)).isSameAs(policy.getNotifications());
  }

  @Test
  public void testGetEffectiveNotifications_NotificationsOverrideNotAllowed() {
    Policy policy = createPolicyWithOverrides("org1", "org2");
    policy.setPolicyNotificationsOverrideAllowed(false);

    assertThat(policy.getEffectiveNotifications(Arrays.asList("app", "org2", "org1"))).isSameAs(
        policy.getNotifications());
  }

  @Test
  public void testGetEffectiveNotifications_NotificationsOverrideAllowed_OrgLevel() {
    testGetEffectiveNotifications_NotificationsOverrideAllowed("org2");
  }

  @Test
  public void testGetEffectiveNotifications_NotificationsOverrideAllowed_AppLevel() {
    testGetEffectiveNotifications_NotificationsOverrideAllowed("app");
  }

  @Test
  public void testGetEffectiveNotifications_NotificationsOverrideAllowed_AppAndOrgLevel() {
    testGetEffectiveNotifications_NotificationsOverrideAllowed("org2", "app");
  }

  private void testGetEffectiveNotifications_NotificationsOverrideAllowed(String... overrideOwnerIds) {
    Policy policy = createPolicyWithOverrides("org1", overrideOwnerIds);
    policy.setPolicyNotificationsOverrideAllowed(true);

    assertThat(policy.getEffectiveNotifications(Arrays.asList("app", "org2", "org1"))).isSameAs(
        policy.getPolicyNotificationsOverrides().get(overrideOwnerIds[overrideOwnerIds.length - 1]));
  }

  private Policy createPolicyWithOverrides(String ownerId, String... overrideOwnerIds) {
    Policy policy = new Policy();
    policy.setOwnerId(ownerId);
    policy.setNotifications(new Notifications());
    Map<String, Notifications> policyNotificationsOverrides = new LinkedHashMap<>();
    for (String overrideOwnerId : overrideOwnerIds) {
      policyNotificationsOverrides.put(overrideOwnerId, new Notifications());
    }
    policy.setPolicyNotificationsOverrides(policyNotificationsOverrides);
    return policy;
  }

  @Test
  public void testToActions_WithSeveralOwners() {
    Policy policy = new Policy("PolicyId", "PolicyName");
    policy.setPolicyActionsOverrideAllowed(true);
    policy.setAction("Deploy", ID_NOTIFY);
    List<String> ownerIds = new ArrayList<>();
    ownerIds.add("AppOwner");
    ownerIds.add("OrganizationOwner");

    Map<String, Map<String, String>> policyActionsOverrides = new HashMap<>();
    Map<String, String> organizationValues = new HashMap<>();
    organizationValues.put("Deploy", ID_WARN);
    Map<String, String> appValues = new HashMap<>();
    appValues.put("Deploy", ID_FAIL);
    policyActionsOverrides.put("OrganizationOwner", organizationValues);
    policyActionsOverrides.put("AppOwner", appValues);
    policy.setPolicyActionsOverrides(policyActionsOverrides);

    List<Action> deploy = policy.toActions("Deploy", false, ownerIds);

    assertThat(deploy.size()).isOne();
    assertThat(deploy.get(0).getActionTypeId()).isEqualTo(ID_FAIL);
  }

  @Test
  public void testToActions_OverrideFirstParentValue() {
    Policy policy = new Policy("PolicyId", "PolicyName");
    policy.setPolicyActionsOverrideAllowed(true);
    policy.setAction("Deploy", ID_NOTIFY);
    List<String> ownerIds = new ArrayList<>();
    ownerIds.add("AppOwner");
    ownerIds.add("OrganizationOwner");
    ownerIds.add("Parent");
    ownerIds.add("Root");

    Map<String, Map<String, String>> policyActionsOverrides = new HashMap<>();
    Map<String, String> orgValues = new HashMap<>();
    orgValues.put("Deploy", ID_FAIL);
    Map<String, String> values = new HashMap<>();
    values.put("Deploy", ID_WARN);
    policyActionsOverrides.put("OrganizationOwner", orgValues);
    policyActionsOverrides.put("Parent", values);
    policy.setPolicyActionsOverrides(policyActionsOverrides);

    List<Action> deploy = policy.toActions("Deploy", false, ownerIds);

    assertThat(deploy.size()).isOne();
    assertThat(deploy.get(0).getActionTypeId()).isEqualTo(ID_FAIL);
  }

  @Test
  public void testToActions_OverrideNotAllowed() {
    Policy policy = new Policy("PolicyId", "PolicyName");
    policy.setPolicyActionsOverrideAllowed(false);
    policy.setAction("Deploy", ID_NOTIFY);
    List<String> ownerIds = new ArrayList<>();
    ownerIds.add("OwnerId");
    ownerIds.add("Root");

    Map<String, Map<String, String>> policyActionsOverrides = new HashMap<>();
    Map<String, String> values = new HashMap<>();
    values.put("Deploy", ID_WARN);
    policyActionsOverrides.put("OwnerId", values);
    policy.setPolicyActionsOverrides(policyActionsOverrides);

    List<Action> deploy = policy.toActions("Deploy", false, ownerIds);

    assertThat(deploy.size()).isOne();
    assertThat(deploy.get(0).getActionTypeId()).isEqualTo(ID_NOTIFY);
  }

  @Test
  public void testToActions_HasWrongStageId() {
    Policy policy = new Policy("PolicyId", "PolicyName");
    policy.setPolicyActionsOverrideAllowed(true);
    List<String> ownerIds = new ArrayList<>();
    ownerIds.add("OwnerId");

    Map<String, Map<String, String>> policyActionsOverrides = new HashMap<>();
    Map<String, String> values = new HashMap<>();
    values.put("WrongStage", "warn");
    policyActionsOverrides.put("OwnerId", values);
    policy.setPolicyActionsOverrides(policyActionsOverrides);

    List<Action> deploy = policy.toActions("Deploy", false, ownerIds);

    assertThat(deploy).isEmpty();
  }

  @Test
  public void testToActions_ContinuesMonitoringIsTrue() {
    Policy policy = new Policy("PolicyId", "PolicyName");
    List<String> ownerIds = new ArrayList<>();
    ownerIds.add("OwnerId");

    List<UserNotification> notifications = new ArrayList<>();
    UserNotification userNotification = new UserNotification();
    Notifications notification = new Notifications();
    Set<String> set = new HashSet<>();
    set.add("continuous-monitoring");
    userNotification.setStageIds(set);
    notifications.add(userNotification);
    notification.setUserNotifications(notifications);
    policy.setNotifications(notification);

    List<Action> deploy = policy.toActions("Deploy", true, ownerIds);

    assertThat(deploy.size()).isOne();
    assertThat(deploy.get(0).getActionTypeId()).isEqualTo("notify");
  }

  @Test
  public void testToActions_OwnerIdIsNull() {
    Policy policy = new Policy("PolicyId", "PolicyName");
    policy.setAction("Deploy", ID_NOTIFY);
    policy.setPolicyActionsOverrideAllowed(true);
    List<Action> deploy = policy.toActions("Deploy", false, null);

    assertThat(deploy.size()).isOne();
    assertThat(deploy.get(0).getActionTypeId()).isEqualTo(ID_NOTIFY);
  }

  @Test
  public void testToActions_ActionOverridesIsEmpty() {
    Policy policy = new Policy("PolicyId", "PolicyName");
    policy.setPolicyActionsOverrideAllowed(true);
    policy.setAction("Deploy", ID_NOTIFY);
    List<String> ownerIds = new ArrayList<>();
    ownerIds.add("OwnerId");

    Map<String, Map<String, String>> policyActionsOverrides = new HashMap<>();
    policy.setPolicyActionsOverrides(policyActionsOverrides);

    List<Action> deploy = policy.toActions("Deploy", false, ownerIds);

    assertThat(deploy.size()).isOne();
    assertThat(deploy.get(0).getActionTypeId()).isEqualTo(ID_NOTIFY);
  }

  @Test
  public void testToActions_ActionOverridesIsNull() {
    Policy policy = new Policy("PolicyId", "PolicyName");
    policy.setPolicyActionsOverrideAllowed(true);
    policy.setAction("Deploy", ID_NOTIFY);
    List<String> ownerIds = new ArrayList<>();
    ownerIds.add("OwnerId");

    policy.setPolicyActionsOverrides(null);

    List<Action> deploy = policy.toActions("Deploy", false, ownerIds);

    assertThat(deploy.size()).isOne();
    assertThat(deploy.get(0).getActionTypeId()).isEqualTo(ID_NOTIFY);
  }

  @Test
  public void testToActions_OverrideForPolicyOwnerIsIgnored() {
    Policy policy = new Policy("PolicyId", "PolicyName");
    policy.setOwnerId("ParentOrg");
    policy.setPolicyActionsOverrideAllowed(true);
    policy.setAction("Deploy", ID_NOTIFY);
    List<String> ownerIds = new ArrayList<>();
    ownerIds.add("AppOwner");
    ownerIds.add("ParentOrg");
    ownerIds.add("OrganizationOwner");
    ownerIds.add("Root");
    Map<String, Map<String, String>> policyActionsOverrides = new HashMap<>();
    Map<String, String> values = new HashMap<>();
    values.put("Deploy", ID_FAIL);
    policyActionsOverrides.put("OrganizationOwner", values);
    policyActionsOverrides.put("ParentOrg", values);
    policy.setPolicyActionsOverrides(policyActionsOverrides);

    List<Action> deploy = policy.toActions("Deploy", false, ownerIds);

    assertThat(deploy.size()).isOne();
    assertThat(deploy.get(0).getActionTypeId()).isEqualTo(ID_NOTIFY);
  }

  @Test
  public void testToActions_OverrideWithNoAction() {
    Policy policy = new Policy();
    policy.setOwnerId("ParentOrg");
    policy.setPolicyActionsOverrideAllowed(true);
    policy.setAction("Deploy", ID_FAIL);

    List<String> ownerIds = new ArrayList<>();
    ownerIds.add("App");
    ownerIds.add("ParentOrg");

    Map<String, Map<String, String>> policyActionsOverrides = new HashMap<>();
    policyActionsOverrides.put("App", new HashMap<>());
    policy.setPolicyActionsOverrides(policyActionsOverrides);

    List<Action> effectiveActions = policy.toActions("Deploy", false, ownerIds);

    assertThat(effectiveActions).isEmpty();
  }
}
