/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.SchemaInfoDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyInternal;
import com.sonatype.insight.brain.dataaccess.policy.PolicyInternalDAO;
import com.sonatype.insight.brain.model.SchemaInfo;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.notifications.Notification;
import com.sonatype.insight.brain.model.policy.notifications.RoleNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.codehaus.plexus.util.IOUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class PolicyJsonMigratorTest
    extends AbstractComponentTest
{
  @Inject
  private PolicyJsonMigrator migrator;

  @Inject
  private SchemaInfoDAO schemaInfoDAO;

  @Inject
  private PolicyInternalDAO policyInternalDAO;

  @Inject
  private PolicyDAO policyDAO;

  @Before
  public void init() {
    SchemaInfo schemaInfo = schemaInfoDAO.get();
    schemaInfo.setPolicyJsonVersion(0);
    schemaInfoDAO.update(schemaInfo);
  }

  @After
  public void exit() {
    SchemaInfo schemaInfo = schemaInfoDAO.get();
    schemaInfo.setPolicyJsonVersion(PolicyJsonMigrator.POLICY_JSON_VERSION);
    schemaInfoDAO.update(schemaInfo);
  }

  private String getPolicyContent(String filename) throws Exception {
    return IOUtil.toString(getClass().getResourceAsStream("/PolicyJsonMigratorTest/" + filename), "UTF-8");
  }

  @Test
  public void testMigrate() throws Exception {
    String policyId = tempEntity.newPolicy("Test").getId();
    PolicyInternal policyInternal = policyInternalDAO.getById(policyId);
    policyInternal.setContent(getPolicyContent("policy1.json"));
    policyInternalDAO.update(policyInternal);

    migrator.migrate();

    assertThat(schemaInfoDAO.get().getPolicyJsonVersion(), is(PolicyJsonMigrator.POLICY_JSON_VERSION));

    Policy policy = policyDAO.getById(policyId);
    assertThat(policy.getConstraints(), hasSize(1));
    assertThat(policy.getActions().keySet(), containsInAnyOrder(Stage.ID_BUILD, Stage.ID_DEVELOP));
    assertThat(policy.getActions().get(Stage.ID_BUILD), is(Action.ID_FAIL));
    assertThat(policy.getActions().get(Stage.ID_DEVELOP), is(Action.ID_WARN));

    assertThat(policy.getNotifications().getUserNotifications(), hasSize(1));
    UserNotification userNotification = policy.getNotifications().getUserNotifications().get(0);
    assertThat(userNotification.getEmailAddress(), is("nobody@sonatype.com"));
    assertThat(userNotification.getStageIds(), containsInAnyOrder(Notification.CONTINUOUS_MONITORING, Stage.ID_BUILD));

    assertThat(policy.getNotifications().getRoleNotifications(), hasSize(1));
    RoleNotification roleNotification = policy.getNotifications().getRoleNotifications().get(0);
    assertThat(roleNotification.getRoleId(), is(Role.OWNER_ROLE_ID));
    assertThat(roleNotification.getStageIds(), containsInAnyOrder(Notification.CONTINUOUS_MONITORING, Stage.ID_BUILD));
  }

  @Test
  public void testMigrate_DeprecatedConditionForSecurityVulnerabilities() throws Exception {
    // Verifies that the deprecated condition for security vulnerabilities can be migrated.
    // The migrator should not fail when it encounters this policy condition type.
    String policyId = tempEntity.newPolicy("Test").getId();
    PolicyInternal policyInternal = policyInternalDAO.getById(policyId);
    policyInternal.setContent(getPolicyContent("policy_deprecated_security_vulnerability_condition.json"));
    policyInternalDAO.update(policyInternal);

    migrator.migrate();

    assertThat(schemaInfoDAO.get().getPolicyJsonVersion(), is(PolicyJsonMigrator.POLICY_JSON_VERSION));

    Policy policy = policyDAO.getById(policyId);
    Condition deprecatedCondition = policy.getConstraints().get(0).getConditions().get(0);
    assertThat(deprecatedCondition.getConditionTypeId(), is("SecurityVulnerability"));
    assertThat(deprecatedCondition.getOperator(), is("present"));
    assertThat(deprecatedCondition.getValue(), is(nullValue()));
  }
}
