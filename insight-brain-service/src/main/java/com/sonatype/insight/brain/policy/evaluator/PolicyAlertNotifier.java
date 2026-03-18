/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.jira.JiraPolicyAlertNotifier;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
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

  private final JiraPolicyAlertNotifier jiraPolicyAlertNotifier;

  private final PolicyAlertScmNotifier policyAlertScmNotifier;

  private final PolicyNotificationUtil policyNotificationUtil;

  @Inject
  public PolicyAlertNotifier(
      final PolicyAlertEmailer policyAlertEmailer,
      final JiraPolicyAlertNotifier jiraPolicyAlertNotifier,
      final PolicyAlertScmNotifier policyAlertScmNotifier,
      final PolicyNotificationUtil policyNotificationUtil)
  {
    this.policyAlertEmailer = policyAlertEmailer;
    this.jiraPolicyAlertNotifier = jiraPolicyAlertNotifier;
    this.policyAlertScmNotifier = policyAlertScmNotifier;
    this.policyNotificationUtil = policyNotificationUtil;
  }

  /**
   * Sends notifications in case of a difference between the current and previous policy violations for a given
   * application and stage.
   */
  public void sendNotifications(
      final Application app,
      final ScanPolicyEvaluatorResults results)
  {
    if (!results.notifiableViolations.isEmpty()) {
      List<PolicyNotification> policyNotifications = policyNotificationUtil
          .createPolicyNotifications(app, results.notifiableViolations, results.evaluation.getStageTypeId(),
              results.evaluation.isForMonitoring());

      // sort the alerts by threat-level, which is common means to represent in most notifiers
      policyNotifications.sort((o1, o2) -> {
        int t1 = o1.getPolicyFact().getThreatLevel();
        int t2 = o2.getPolicyFact().getThreatLevel();
        int r = t2 - t1;
        if (r == 0) {
          r = o1.getPolicyFact().getPolicyName().compareToIgnoreCase(o2.getPolicyFact().getPolicyName());
        }
        return r;
      });

      final String scanId = results.evaluation.getScanId();
      final Stage stage = makeStage(results.evaluation.getStageTypeId());

      int legacyViolationPolicyCount = getLegacyPolicyViolationCount(results);

      try {
        policyAlertEmailer
            .sendNotifications(app, scanId, stage, policyNotifications, legacyViolationPolicyCount,
                results.evaluation.isForMonitoring());
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

      try {
        policyAlertScmNotifier.sendNotifications(app, scanId, stage, policyNotifications);
      }
      catch (Exception e) {
        log.error("Source Control notification failed", e);
      }
    }
    else {
      log.debug("Not sending notifications for application {} and scan {} in stage {}, " +
          "no new policy violations for policies configured to send notifications since last evaluation.",
          app.getPublicId(), results.evaluation.getScanId(), results.evaluation.getStageTypeId());
    }
  }

  private int getLegacyPolicyViolationCount(final ScanPolicyEvaluatorResults results) {
    int legacyViolationPolicyCount = 0;
    for (PolicyViolation policyViolation : results.allViolations) {
      if (policyViolation.isLegacyViolation()) {
        legacyViolationPolicyCount++;
      }
    }
    return legacyViolationPolicyCount;
  }

  /**
   * Construct a stage with filled in type-id and name details.
   */
  private static Stage makeStage(final String stageTypeId) {
    return new Stage(stageTypeId, StageTypes.getById(stageTypeId).getName());
  }
}
