/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.NotifyAction;
import com.sonatype.insight.brain.dataaccess.SchemaInfoDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyInternal;
import com.sonatype.insight.brain.dataaccess.policy.PolicyInternalDAO;
import com.sonatype.insight.brain.model.SchemaInfo;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.NotifyActionType;
import com.sonatype.insight.brain.model.policy.notifications.Notification;
import com.sonatype.insight.brain.model.policy.notifications.RoleNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Migrates the persisted JSON blob for policies to the latest version.
 * 
 * @since 1.21
 */
@Named
public class PolicyJsonMigrator
{
  private static final Logger log = LoggerFactory.getLogger(PolicyJsonMigrator.class);

  static final int POLICY_JSON_VERSION = 1;

  private final SchemaInfoDAO schemaInfoDAO;

  private final PolicyInternalDAO policyInternalDAO;

  private final ProcureRemovalMigrator procureRemovalMigrator;

  @Inject
  public PolicyJsonMigrator(SchemaInfoDAO schemaInfoDAO,
                            PolicyInternalDAO policyInternalDAO,
                            ProcureRemovalMigrator procureRemovalMigrator)
  {
    this.schemaInfoDAO = schemaInfoDAO;
    this.policyInternalDAO = policyInternalDAO;
    this.procureRemovalMigrator = procureRemovalMigrator;
  }

  public void migrate() throws IOException {
    SchemaInfo schemaInfo = schemaInfoDAO.get();

    int policyJsonVersion = schemaInfo.getPolicyJsonVersion();
    if (policyJsonVersion >= POLICY_JSON_VERSION) {
      log.debug("Policy definitions already up to date");
      return;
    }

    try (TransactionContext tx = policyInternalDAO.createTransactionContext()) {
      tx.begin();

      List<PolicyInternal> policies = policyInternalDAO.getAll(tx);
      log.info("Migrating definitions for {} policies", policies.size());
      for (PolicyInternal policy : policies) {
        policy.setContent(JsonUtils.format(migrate(policy.getContent())));
        policyInternalDAO.update(tx, policy);
      }

      schemaInfo.setPolicyJsonVersion(POLICY_JSON_VERSION);
      schemaInfoDAO.update(tx, schemaInfo);

      tx.commit();
    }
  }

  private Policy migrate(String policyJson) throws IOException {
    return migrate(JsonUtils.parse(policyJson));
  }

  Policy migrate(JsonNode policyJson) throws IOException {
    PolicyV0 legacyPolicy = JsonUtils.asPojo(policyJson, PolicyV0.class);
    Policy policy = new Policy(legacyPolicy.id, legacyPolicy.name);
    policy.setOwnerId(legacyPolicy.ownerId);
    policy.setEnabled(legacyPolicy.enabled);
    policy.setThreatLevel(legacyPolicy.threatLevel);
    policy.setConstraints(legacyPolicy.constraints);
    Map<String, Notification> notificationsByTarget = new HashMap<>();
    if (legacyPolicy.actions != null) {
      for (Map.Entry<String, List<Action>> entry : legacyPolicy.actions.entrySet()) {
        String stageId = entry.getKey();
        for (Action action : entry.getValue()) {
          if (Action.ID_NOTIFY.equals(action.getActionTypeId())) {
            migrateNotification(policy, stageId, action, notificationsByTarget);
          }
          else {
            policy.setAction(stageId, action.getActionTypeId());
          }
        }
      }
      procureRemovalMigrator.pruneProcurement(policy);
    }
    if (legacyPolicy.monitorNotifyActions != null) {
      for (Action action : legacyPolicy.monitorNotifyActions) {
        migrateNotification(policy, Notification.CONTINUOUS_MONITORING, action, notificationsByTarget);
      }
    }
    return policy;
  }

  private void migrateNotification(Policy policy,
                                   String stageId,
                                   Action action,
                                   Map<String, Notification> notificationsByTarget)
  {
    String key = action.getTargetType() + ":" + action.getTarget();
    Notification notification = notificationsByTarget.get(key);
    if (notification == null) {
      if (NotifyActionType.TARGET_TYPE_ROLE.equals(action.getTargetType())) {
        notification = new RoleNotification(action.getTarget());
      }
      else {
        notification = new UserNotification(action.getTarget());
      }
      policy.getNotifications().add(notification);
      notificationsByTarget.put(key, notification);
    }
    notification.getStageIds().add(stageId);
  }

  private static class PolicyV0
  {
    public String id;

    public String name;

    public String ownerId;

    public boolean enabled = true;

    public int threatLevel = 5;

    public List<Constraint> constraints;

    public Map<String, List<Action>> actions;

    public List<NotifyAction> monitorNotifyActions;
  }
}
