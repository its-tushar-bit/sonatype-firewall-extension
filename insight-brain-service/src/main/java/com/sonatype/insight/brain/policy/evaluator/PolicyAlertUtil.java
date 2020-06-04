/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.component.ComponentDisplayFilename;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.utils.ComponentFactUtil;

public class PolicyAlertUtil
{
  public static List<PolicyAlert> createPolicyAlerts(List<PolicyViolation> policyViolations,
                                                     String stageTypeId,
                                                     boolean forMonitoring,
                                                     boolean enableActions)
  {
    List<PolicyAlert> result = new ArrayList<>();
    PolicyDAO policyDAO = new PolicyDAO();
    for (PolicyViolation policyViolation : policyViolations) {
      String policyId = policyViolation.getPolicyId();
      PolicyFact policyFact = new PolicyFact(policyId, policyViolation.getPolicyName(),
          policyViolation.getThreatLevel(), policyViolation.getId());
      Policy policy = policyDAO.getById(policyId);
      List<Action> actions;
      if (policy == null || !enableActions) {
        actions = Collections.emptyList();
      }
      else {
        actions = policy.toActions(stageTypeId, forMonitoring);
      }
      PolicyAlert policyAlert = new PolicyAlert(policyFact, actions);
      result.add(policyAlert);

      ComponentFact componentFact = new ComponentFact(policyViolation.getComponentIdentifier(),
          policyViolation.getHash());
      ComponentFactUtil.injectDisplayName(componentFact);
      for (ConstraintFact constraintFact : policyViolation.getConstraintFacts()) {
        removeDataUnnecessaryForPolicyAlert(constraintFact);
        componentFact.addConstraintFact(constraintFact);
      }
      policyFact.addComponentFact(componentFact);
    }

    return result;
  }

  private static void removeDataUnnecessaryForPolicyAlert(ConstraintFact constraintFact) {
    for (ConditionFact conditionFact : constraintFact.getConditionFacts()) {
      conditionFact.setConditionIndex(0);
      conditionFact.setTriggerJson(null);
    }
  }

  public static List<PolicyViolation> getPolicyViolationsFromAlertsAndEvaluation(
      PolicyEvaluation policyEvaluation,
      List<PolicyAlert> allPolicyAlerts)
  {
    List<PolicyViolation> allViolations = new ArrayList<>();
    for (PolicyAlert policyAlert : allPolicyAlerts) {
      PolicyFact policyFact = policyAlert.getTrigger();
      for (ComponentFact componentFact : policyFact.getComponentFacts()) {
        PolicyViolation policyViolation =
            new PolicyViolation(policyEvaluation, policyFact.getPolicyId(), policyFact.getPolicyName(),
                policyFact.getThreatLevel(), null, componentFact.getHash(), componentFact.getComponentIdentifier(),
                componentFact.getConstraintFacts(), getFilename(componentFact));
        policyViolation.setId(policyFact.getPolicyViolationId());
        allViolations.add(policyViolation);
      }
    }
    return allViolations;
  }

  private static String getFilename(ComponentFact componentFact) {
    return new ComponentDisplayFilename().addPathnames(componentFact.getPathnames()).getFilename().orElse(null);
  }
}
