/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.nio.charset.StandardCharsets;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyInternal;
import com.sonatype.insight.brain.dataaccess.policy.PolicyInternalDAO;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.notifications.Notification;
import com.sonatype.insight.brain.model.policy.notifications.RoleNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class PolicyJsonMigratorTest
    extends AbstractComponentH2Test
{
  @Inject
  private PolicyJsonMigrator migrator;

  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  private PolicyInternalDAO policyInternalDAO;

  @Inject
  private PolicyDAO policyDAO;

  @BeforeEach
  public void init() {
    // fake version, tests here are written for version 0
    MigrationTracker migrationTracker = new MigrationTracker(PolicyJsonMigrator.MIGRATION_ID);
    migrationTracker.setVersion(0);
    migrationTrackerDAO.update(migrationTracker);
  }

  private String getPolicyContent(String filename) throws Exception {
    return IOUtils.toString(getClass().getResourceAsStream("/PolicyJsonMigratorTest/" + filename),
        StandardCharsets.UTF_8);
  }

  @Test
  public void testMigrate() throws Exception {
    String policyId = tempEntity.newPolicy().getId();
    PolicyInternal policyInternal = policyInternalDAO.getById(policyId);
    policyInternal.setContent(getPolicyContent("policy1.json"));
    policyInternalDAO.update(policyInternal);

    migrator.migrate();

    assertThat(migrationTrackerDAO.getById(PolicyJsonMigrator.MIGRATION_ID).getVersion())
        .isEqualTo(PolicyJsonMigrator.POLICY_JSON_VERSION);

    Policy policy = policyDAO.getById(policyId);
    assertThat(policy.getConstraints()).hasSize(1);
    assertThat(policy.getActions()).containsOnlyKeys(Stage.ID_BUILD, Stage.ID_DEVELOP);
    assertThat(policy.getActions().get(Stage.ID_BUILD)).isEqualTo(Action.ID_FAIL);
    assertThat(policy.getActions().get(Stage.ID_DEVELOP)).isEqualTo(Action.ID_WARN);

    assertThat(policy.getNotifications().getUserNotifications()).hasSize(1);
    UserNotification userNotification = policy.getNotifications().getUserNotifications().get(0);
    assertThat(userNotification.getEmailAddress()).isEqualTo("nobody@sonatype.com");
    assertThat(userNotification.getStageIds()).containsExactlyInAnyOrder(Notification.CONTINUOUS_MONITORING,
        Stage.ID_BUILD);

    assertThat(policy.getNotifications().getRoleNotifications()).hasSize(1);
    RoleNotification roleNotification = policy.getNotifications().getRoleNotifications().get(0);
    assertThat(roleNotification.getRoleId()).isEqualTo(Role.OWNER_ROLE_ID);
    assertThat(roleNotification.getStageIds()).containsExactlyInAnyOrder(Notification.CONTINUOUS_MONITORING,
        Stage.ID_BUILD);
  }

  @Test
  public void testMigrate_DeprecatedConditionForSecurityVulnerabilities() throws Exception {
    // Verifies that the deprecated condition for security vulnerabilities can be migrated.
    // The migrator should not fail when it encounters this policy condition type.
    String policyId = tempEntity.newPolicy().getId();
    PolicyInternal policyInternal = policyInternalDAO.getById(policyId);
    policyInternal.setContent(getPolicyContent("policy_deprecated_security_vulnerability_condition.json"));
    policyInternalDAO.update(policyInternal);

    migrator.migrate();

    assertThat(migrationTrackerDAO.getById(PolicyJsonMigrator.MIGRATION_ID).getVersion())
        .isEqualTo(PolicyJsonMigrator.POLICY_JSON_VERSION);

    Policy policy = policyDAO.getById(policyId);
    Condition deprecatedCondition = policy.getConstraints().get(0).getConditions().get(0);
    assertThat(deprecatedCondition.getConditionTypeId()).isEqualTo("SecurityVulnerability");
    assertThat(deprecatedCondition.getOperator()).isEqualTo("present");
    assertThat(deprecatedCondition.getValue()).isNull();
  }
}
