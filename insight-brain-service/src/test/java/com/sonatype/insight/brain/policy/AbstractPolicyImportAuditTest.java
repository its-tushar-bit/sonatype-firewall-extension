/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.actions.WarnActionType;
import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.notifications.JiraNotification;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.RoleNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractPolicyImportAuditTest
    extends AbstractAuditTest
{
  protected PolicyDAO policyDAO;

  protected RoleDAO roleDAO;

  @Before
  public void setUp() {
    roleDAO = lookup(RoleDAO.class);
    policyDAO = lookup(PolicyDAO.class);
  }

  protected Policy policy() {
    Policy policy = new Policy();
    policy.setId(TemporaryEntity.uuid());
    policy.setName(TemporaryEntity.uuid());
    policy.setPolicyActionsOverrideAllowed(true);
    Constraint constraint = new Constraint();
    constraint.setName("constraintName");
    Condition condition = new Condition(ConditionTypes.MatchStateConditionType.getId(), "is", "exact");
    constraint.setConditions(Collections.singletonList(condition));
    policy.setConstraints(Collections.singletonList(constraint));
    return policy;
  }

  protected Policy aComplexPolicy() {
    Policy policy = policy();
    policy.setConstraints(new ArrayList<>(Arrays.asList(
        constraint("c1", LogicalOperator.AND,
            condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"),
            condition(ConditionTypes.MatchStateConditionType.getId(), "is", "exact")),
        constraint("c2", LogicalOperator.OR,
            condition(AgeInDaysConditionType.ID, "older than", "1"),
            condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "7")))));
    policy.setAction(Stage.ID_BUILD, WarnActionType.ID);
    policy.setAction(Stage.ID_RELEASE, FailActionType.ID);
    Role role = roleDAO.getById(Role.DEVELOPER_ROLE_ID);
    policy.setNotifications(new Notifications(
        new UserNotification("name@email.com", Stage.ID_BUILD, Stage.ID_STAGE_RELEASE, Stage.ID_OPERATE),
        new RoleNotification(role.getId(), role.getName(), Stage.ID_BUILD),
        new JiraNotification("p1", 123L, Stage.ID_DEVELOP)));
    return policy;
  }

  protected Label label() {
    Label label = new Label();
    label.setId(TemporaryEntity.uuid());
    label.setLabel(TemporaryEntity.uuid());
    return label;
  }

  protected LicenseThreatGroup licenseThreatGroup() {
    LicenseThreatGroup licenseThreatGroup = new LicenseThreatGroup();
    licenseThreatGroup.setId(TemporaryEntity.uuid());
    licenseThreatGroup.setName(TemporaryEntity.uuid());
    return licenseThreatGroup;
  }

  protected Tag tag() {
    Tag tag = new Tag();
    tag.setId(TemporaryEntity.uuid());
    tag.setName(TemporaryEntity.uuid());
    tag.setDescription("tagDescription");
    return tag;
  }

  protected void assertPolicyImportData(
      AuditDTO auditDTO,
      Integer policyCount,
      Integer componentLabelCount,
      Integer licenseThreatGroupCount,
      Integer applicationCategoryCount)
  {
    assertCustomData(auditDTO, "policyCount", policyCount);
    assertCustomData(auditDTO, "componentLabelCount", componentLabelCount);
    assertCustomData(auditDTO, "licenseThreatGroupCount", licenseThreatGroupCount);
    assertCustomData(auditDTO, "applicationCategoryCount", applicationCategoryCount);
  }

  protected void assertPolicyOverrideData(
      final AuditDTO auditDTO,
      final Policy policy,
      final String overridingOwnerId,
      boolean actionsOverrideAdded,
      boolean notificationsOverrideAdded)
  {
    String auditedPolicyId = (String) auditDTO.data.get("policyId");
    String auditedOverridingOwnerId = (String) auditDTO.data.get("overridingOwnerId");

    assertThat(auditedPolicyId).isNotNull();
    assertThat(auditedOverridingOwnerId).isNotNull().isEqualTo(overridingOwnerId);
    assertThat(auditedPolicyId).isEqualTo(policy.getId());
    assertThat(policyDAO.getById(auditedPolicyId)).isNotNull();
    if (actionsOverrideAdded) {
      assertThat(auditDTO.data.get("actionsOverride")).isNotNull();
    }
    if (notificationsOverrideAdded) {
      assertThat(auditDTO.data.get("notificationsOverride")).isNotNull();
    }
  }

  protected void assertImportedPolicies(
      final List<Policy> policies,
      String organizationId,
      String organizationName,
      String username)
  {
    List<AuditDTO> auditLogs = assertAuditLogs(AuditEvent.IMPORT_POLICY, policies.size(), null, username);
    for (int i = 0; i < policies.size(); i++) {
      assertOrganizationData(auditLogs.get(i), organizationId, organizationName);
      assertPolicyData(auditLogs.get(i), policies.get(i), false);
    }
  }

  protected Condition condition(final String conditionTypeId, final String operator, final String value) {
    return new Condition(conditionTypeId, operator, value);
  }

  protected Constraint constraint(final String name, final LogicalOperator logicalOperator, Condition... conditions) {
    Constraint constraint = new Constraint(TemporaryEntity.uuid(), name, logicalOperator);
    constraint.setConditions(Arrays.asList(conditions));
    return constraint;
  }
}
