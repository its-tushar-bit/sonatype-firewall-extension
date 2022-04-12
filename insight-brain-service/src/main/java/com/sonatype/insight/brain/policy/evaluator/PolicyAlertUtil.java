/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.*;
import java.util.function.Function;

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

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

public class PolicyAlertUtil
{
  public static List<PolicyAlert> createPolicyAlerts(List<PolicyViolation> policyViolations,
                                                     String stageTypeId,
                                                     boolean forMonitoring,
                                                     boolean enableActions)
  {
    Map<String, Policy> policiesById =
        new PolicyDAO().getByIds(policyViolations.stream().map(PolicyViolation::getPolicyId).collect(toSet())).stream()
            .collect(toMap(Policy::getId, Function.identity()));
    List<PolicyAlert> result = new ArrayList<>();
    for (PolicyViolation policyViolation : policyViolations) {
      String policyId = policyViolation.getPolicyId();
      PolicyFact policyFact = new PolicyFact(policyId, policyViolation.getPolicyName(),
          policyViolation.getThreatLevel(), policyViolation.getId());
      Policy policy = policiesById.get(policyId);
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
      final PolicyEvaluation policyEvaluation,
      final List<PolicyAlert> allPolicyAlerts)
  {
    return getPolicyViolationsFromAlertsAndEvaluation(policyEvaluation, allPolicyAlerts, 0);
  }

  public static List<PolicyViolation> getPolicyViolationsFromAlertsAndEvaluation(
      final PolicyEvaluation policyEvaluation,
      final List<PolicyAlert> allPolicyAlerts,
      final int minimumThreatLevel)
  {
    List<PolicyViolation> allViolations = new ArrayList<>();
    for (PolicyAlert policyAlert : allPolicyAlerts) {
      PolicyFact policyFact = policyAlert.getTrigger();
      if (policyFact.getThreatLevel() >= minimumThreatLevel) {
        for (ComponentFact componentFact : policyFact.getComponentFacts()) {
          PolicyViolation policyViolation =
              new PolicyViolation(policyEvaluation, policyFact.getPolicyId(), policyFact.getPolicyName(),
                  policyFact.getThreatLevel(), null, componentFact.getHash(), componentFact.getComponentIdentifier(),
                  componentFact.getConstraintFacts(), getFilename(componentFact));
          policyViolation.setId(policyFact.getPolicyViolationId());
          allViolations.add(policyViolation);
        }
      }
    }
    return allViolations;
  }

  private static String getFilename(ComponentFact componentFact) {
    return new ComponentDisplayFilename().addPathnames(componentFact.getPathnames()).getFilename().orElse(null);
  }

  public static List<PolicyViolation> getDummyPolicyViolationsFromPolicyThreatsForCounts(PolicyThreats policyThreats) {
    List<PolicyViolation> allViolations = new ArrayList<>();
    for (PolicyThreats.Component component : policyThreats.aaData) {
      for (PolicyThreats.PolicyViolation violation : component.allViolations) {
        // We only need the threat level and the fix/waive/grandfather times to be set or not
        // (doesn't matter what their times are) to get accurate counts
        PolicyViolation policyViolation = new PolicyViolation();
        policyViolation.setThreatLevel(violation.policyThreatLevel);
        policyViolation.setHash(component.hash);
        boolean waived = violation.waived;
        boolean grandfathered = violation.grandfathered;
        boolean fixed = !waived && !grandfathered && !isActive(component.activeViolations, violation.policyViolationId);
        if (fixed) {
          policyViolation.setFixTime(new Date());
        }
        if (waived) {
          policyViolation.setWaiveTime(new Date());
        }
        if (grandfathered) {
          policyViolation.setGrandfatherTime(new Date());
        }
        allViolations.add(policyViolation);
      }
    }
    return allViolations;
  }

  private static boolean isActive(List<PolicyThreats.PolicyViolation> activeViolations, String policyViolationId) {
    return activeViolations.stream().anyMatch(v -> v.policyViolationId.equals(policyViolationId));
  }
}

