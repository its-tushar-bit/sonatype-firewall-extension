/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

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

  @Inject
  public PolicyAlertNotifier(final PolicyAlertEmailer policyAlertEmailer,
                             final JiraPolicyAlertNotifier jiraPolicyAlertNotifier)
  {
    this.policyAlertEmailer = policyAlertEmailer;
    this.jiraPolicyAlertNotifier = jiraPolicyAlertNotifier;
  }

  /**
   * Sends notifications in case of a difference between the current and previous policy violations for a given
   * application and stage.
   */
  public void sendNotifications(final Application app,
                                final ScanPolicyEvaluatorResults results)
  {
    if (!results.notifiableViolations.isEmpty()) {
      List<PolicyNotification> policyNotifications = PolicyNotificationUtil
          .createPolicyNotifications(results.notifiableViolations, results.evaluation.getStageTypeId(),
              results.evaluation.isForMonitoring());

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

      final String scanId = results.evaluation.getScanId();
      final Stage stage = makeStage(results.evaluation.getStageTypeId());

      int grandfatheredPolicyViolationCount = getGrandfatheredPolicyViolationCount(results);

      try {
        policyAlertEmailer
            .sendNotifications(app, scanId, stage, policyNotifications, grandfatheredPolicyViolationCount);
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
          + ", no new policy violations since last evaluation", app.getPublicId(), results.evaluation.getScanId(),
          results.evaluation.getStageTypeId());
    }
  }

  private int getGrandfatheredPolicyViolationCount(final ScanPolicyEvaluatorResults results) {
    int grandfatheredPolicyViolationCount = 0;
    for (PolicyViolation policyViolation : results.allViolations) {
      if (policyViolation.isGrandfathered()) {
        grandfatheredPolicyViolationCount++;
      }
    }
    return grandfatheredPolicyViolationCount;
  }

  /**
   * Construct a stage with filled in type-id and name details.
   */
  private static Stage makeStage(final String stageTypeId) {
    return new Stage(stageTypeId, StageTypes.getById(stageTypeId).getName());
  }
}
