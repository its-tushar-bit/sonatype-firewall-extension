/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.jira.JiraPolicyAlertNotifier;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.notifications.Notification;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class used to generate policy alerts and send notifications for them.
 *
 * @since 1.8
 */
@Named
public class PolicyAlertNotifier
{
  private static final Logger log = LoggerFactory.getLogger(PolicyAlertNotifier.class);

  private final PolicyAlertEmailer policyAlertEmailer;

  private final PolicyViolationDAO policyViolationDAO;

  private final JiraPolicyAlertNotifier jiraPolicyAlertNotifier;

  @Inject
  public PolicyAlertNotifier(final PolicyAlertEmailer policyAlertEmailer,
                             final PolicyViolationDAO policyViolationDAO,
                             final JiraPolicyAlertNotifier jiraPolicyAlertNotifier)
  {
    this.policyAlertEmailer = policyAlertEmailer;
    this.policyViolationDAO = policyViolationDAO;
    this.jiraPolicyAlertNotifier = jiraPolicyAlertNotifier;
  }

  /**
   * Sends notifications in case of a difference between the current and previous policy violations for a given
   * application and stage.
   */
  public void sendNotifications(final Application app,
                                final PolicyEvaluation currentEvaluation,
                                final PolicyEvaluation previousEvaluation)
  {
    PolicyViolationDiff<PolicyViolation> diff = createPolicyViolationDiff(previousEvaluation, currentEvaluation);

    if (diff.hasAppeared()) {
      List<PolicyNotification> policyNotifications = PolicyNotificationUtil
          .createPolicyNotifications(diff.getAppeared(), currentEvaluation.getStageTypeId(),
              currentEvaluation.isForMonitoring());
      addNotificationsToPolicyViolations(diff.getAppeared(), policyNotifications);

      // sort the alerts by threat-level, which is common means to represent in most notifiers
      Collections.sort(policyNotifications, new Comparator<PolicyNotification>()
      {
        @Override
        public int compare(PolicyNotification o1, PolicyNotification o2) {
          int t1 = o1.getPolicyFact().getThreatLevel();
          int t2 = o2.getPolicyFact().getThreatLevel();
          int r = t2 - t1;
          if (r == 0) {
            r = String.CASE_INSENSITIVE_ORDER
                .compare(o1.getPolicyFact().getPolicyName(), o2.getPolicyFact().getPolicyName());
          }
          return r;
        }
      });

      final String scanId = currentEvaluation.getScanId();
      final Stage stage = makeStage(currentEvaluation.getStageTypeId());

      try {
        policyAlertEmailer.sendNotifications(app, scanId, stage, policyNotifications);
      }
      catch (Exception e) {
        log.error("Email notification failed", e);
      }

      try {
        jiraPolicyAlertNotifier.sendNotifications(app, scanId, stage, policyNotifications);
      }
      catch (Exception e) {
        log.error("JIRA notification failed", e);
      }
    }
    else {
      log.debug("Not sending notifications for application {} and scan {} in stage {}"
          + ", no new policy violations since last evaluation", app.getPublicId(), currentEvaluation.getScanId(),
          currentEvaluation.getStageTypeId());
    }
  }

  /**
   * Construct a stage with filled in type-id and name details.
   */
  private static Stage makeStage(final String stageTypeId) {
    return new Stage(stageTypeId, StageTypes.getById(stageTypeId).getName());
  }

  private PolicyViolationDiff<PolicyViolation> createPolicyViolationDiff(final PolicyEvaluation previousEvaluation,
                                                        final PolicyEvaluation currentEvaluation)
  {
    List<PolicyViolation> currentViolations = policyViolationDAO.getActiveByEvaluationId(currentEvaluation.getId());
    List<PolicyViolation> previousViolations = null;
    if (previousEvaluation != null) {
      previousViolations = policyViolationDAO.getActiveByEvaluationId(previousEvaluation.getId());
    }
    return PolicyViolationDigester.digestPolicyViolations(previousViolations, currentViolations);
  }

  private void addNotificationsToPolicyViolations(List<PolicyViolation> policyViolations,
                                                  List<PolicyNotification> policyNotifications)
  {
    Map<String, PolicyNotification> policyNotificationsByPolicyId = new HashMap<>();
    for (PolicyNotification policyNotification : policyNotifications) {
      policyNotificationsByPolicyId.put(policyNotification.getPolicyFact().getPolicyId(), policyNotification);
    }

    for (PolicyViolation policyViolation : policyViolations) {
      PolicyNotification policyNotification = policyNotificationsByPolicyId.get(policyViolation.getPolicyId());
      List<String> notifications = new ArrayList<>();
      for (Notification notification : policyNotification.getNotifications().getAllNotifications()) {
        Action action = notification.toAction();
        notifications.add(action.getTarget());
      }
      policyViolation.setNotifications(notifications);
      policyViolationDAO.update(policyViolation);
    }
  }
}
