/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.nio.charset.StandardCharsets;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyInternal;
import com.sonatype.insight.brain.dataaccess.policy.PolicyInternalDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.ValidationResult;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.ConditionValidator;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.ConstraintValidator;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyValidator;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.policy.notifications.JiraNotificationValidator;
import com.sonatype.insight.brain.model.policy.notifications.NotificationsValidator;
import com.sonatype.insight.brain.model.policy.notifications.RoleNotificationValidator;
import com.sonatype.insight.brain.model.policy.notifications.UserNotificationValidator;
import com.sonatype.insight.brain.model.policy.notifications.WebhookNotificationValidator;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyDroolsCodeMigratorTest
    extends AbstractComponentTest
{
  @Inject
  private PolicyInternalDAO policyInternalDAO;

  @Inject
  private PolicyDroolsCodeMigrator migrator;

  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  private PolicyDAO policyDAO;

  @Inject
  private LicenseThreatGroupDAO licenseThreatGroupDAO;

  @Inject
  private RoleDAO roleDAO;

  @Test
  public void testMigrate_GracefullyHandleInvalidPolicy() {
    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(Organization.ROOT_ORGANIZATION_ID);
    Organization org = tempEntity.newOrganization();
    Policy policy = new Policy(null, "Test Policy");
    policy.setOwnerId(org.getId());
    Constraint constraint = new Constraint(null, "Test Constraint", LogicalOperator.OR);
    constraint.addCondition(new Condition(LicenseThreatGroupConditionType.ID, "is", ltg.getId()));
    policy.addConstraint(constraint);
    tempEntity.newPolicy(policy);

    licenseThreatGroupDAO.delete(ltg);

    ConditionValidator conditionValidator = new ConditionValidator();
    ConstraintValidator constraintValidator = new ConstraintValidator(conditionValidator);
    UserNotificationValidator userNotificationValidator = new UserNotificationValidator();
    RoleNotificationValidator roleNotificationValidator =
        new RoleNotificationValidator(() -> roleDAO);
    JiraNotificationValidator jiraNotificationValidator = new JiraNotificationValidator();
    WebhookNotificationValidator webhookNotificationValidator = new WebhookNotificationValidator();
    NotificationsValidator notificationsValidator =
        new NotificationsValidator(userNotificationValidator, roleNotificationValidator, jiraNotificationValidator,
            webhookNotificationValidator);
    PolicyValidator policyValidator = new PolicyValidator(constraintValidator, notificationsValidator);

    ValidationResult validationResult = policyValidator.validate(null, policy, policy.getOwnerId());
    assertThat(validationResult.isValid()).isFalse();

    fakeDroolsCodeVersion(2);

    migrator.migrate();

    assertThat(migrationTrackerDAO.getById(PolicyDroolsCodeMigrator.MIGRATION_ID).getVersion())
        .isEqualTo(PolicyDroolsCodeMigrator.DROOLS_CODE_VERSION);
  }

  @Test
  public void testMigrate_DeprecatedConditionForSecurityVulnerabilities() throws Exception {
    // Verifies that the deprecated condition for security vulnerabilities can be migrated.
    // The migrator should not fail when it encounters this policy condition type.
    String policyId = tempEntity.newPolicy().getId();
    PolicyInternal policyInternal = policyInternalDAO.getById(policyId);
    policyInternal.setContent(getPolicyContent("policy_deprecated_security_vulnerability_condition.json"));
    policyInternalDAO.update(policyInternal);

    fakeDroolsCodeVersion(2);

    migrator.migrate();
    Policy policy = policyDAO.getById(policyId);
    Condition deprecatedCondition = policy.getConstraints().get(0).getConditions().get(0);
    assertThat(deprecatedCondition.getConditionTypeId()).isEqualTo("SecurityVulnerability");
    assertThat(deprecatedCondition.getOperator()).isEqualTo("present");
    assertThat(deprecatedCondition.getValue()).isNull();
  }

  @Test
  public void testMigrate_FromVersion3() {
    String policyId = tempEntity.newPolicy().getId();
    PolicyInternal policyInternal = policyInternalDAO.getById(policyId);
    policyInternal.setDroolsCode("");
    policyInternalDAO.update(policyInternal);
    Policy policy = policyDAO.getById(policyId);
    assertThat(policy.getDroolsCode()).isEqualTo("");

    fakeDroolsCodeVersion(3);

    migrator.migrate();
    policy = policyDAO.getById(policyId);
    assertThat(policy.getDroolsCode()).contains("$conditionTriggers");

    assertThat(migrationTrackerDAO.getById(PolicyDroolsCodeMigrator.MIGRATION_ID).getVersion())
        .isEqualTo(PolicyDroolsCodeMigrator.DROOLS_CODE_VERSION);
  }

  private String getPolicyContent(String filename) throws Exception {
    return IOUtils.toString(getClass().getResourceAsStream("/PolicyDroolsCodeMigratorTest/" + filename),
        StandardCharsets.UTF_8);
  }

  private void fakeDroolsCodeVersion(int version) {
    MigrationTracker migrationTracker = new MigrationTracker(PolicyDroolsCodeMigrator.MIGRATION_ID);
    migrationTracker.setVersion(version);
    migrationTrackerDAO.update(migrationTracker);
  }
}
