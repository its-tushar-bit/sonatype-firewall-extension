/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class used to send (email) notifications for policy alerts.
 * 
 * @since 1.8
 */
@Named
public class PolicyAlertNotifier
{
  private static final Logger log = LoggerFactory.getLogger(PolicyAlertNotifier.class);

  private final PolicyAlertEmailer policyAlertEmailer;

  private final PolicyViolationDAO policyViolationDAO;

  @Inject
  public PolicyAlertNotifier(final PolicyAlertEmailer policyAlertEmailer, final PolicyViolationDAO policyViolationDAO)
  {
    this.policyAlertEmailer = policyAlertEmailer;
    this.policyViolationDAO = policyViolationDAO;
  }

  /**
   * Sends notifications in case of a difference between the current and previous policy violations for a given
   * application and stage.
   */
  public void sendNotifications(final Application app, final PolicyEvaluation currentEvaluation,
      final PolicyEvaluation previousEvaluation)
  {
    List<PolicyViolation> currentViolations = policyViolationDAO.getActiveByEvaluationId(currentEvaluation.getId());
    List<PolicyViolation> previousViolations = null;
    if (previousEvaluation != null) {
      previousViolations = policyViolationDAO.getActiveByEvaluationId(previousEvaluation.getId());
    }
    PolicyViolationDiff diff = PolicyViolationDigester.digestPolicyViolations(previousViolations, currentViolations);

    if (!diff.getAppeared().isEmpty()) {
      List<PolicyAlert> policyAlerts = PolicyAlertUtil.createPolicyAlerts(diff.getAppeared(),
          currentEvaluation.getStageTypeId(), currentEvaluation.isForMonitoring());
      updatePolicyViolations(diff.getAppeared(), policyAlerts);
      policyAlertEmailer.sendNotifications(app, currentEvaluation.getScanId(),
          new Stage(currentEvaluation.getStageTypeId()), policyAlerts);
    }
    else {
      log.debug("Not sending notification emails for application {} and scan {} in stage {}"
          + ", no new policy violations since last evaluation", app.getPublicId(), currentEvaluation.getScanId(),
          currentEvaluation.getStageTypeId());
    }
  }

  private void updatePolicyViolations(List<PolicyViolation> policyViolations, List<PolicyAlert> policyAlerts) {
    Map<String, PolicyAlert> policyAlertsByPolicyId = new HashMap<>();
    for (PolicyAlert policyAlert : policyAlerts) {
      policyAlertsByPolicyId.put(policyAlert.getTrigger().getPolicyId(), policyAlert);
    }

    for (PolicyViolation policyViolation : policyViolations) {
      PolicyAlert policyAlert = policyAlertsByPolicyId.get(policyViolation.getPolicyId());
      List<String> notifications = new ArrayList<>();
      for (Action action : policyAlert.getActions()) {
        if (Action.ID_NOTIFY.equals(action.getActionTypeId())) {
          notifications.add(action.getTarget());
        }
      }
      policyViolation.setNotifications(notifications);
      policyViolationDAO.update(policyViolation);
    }
  }
}
