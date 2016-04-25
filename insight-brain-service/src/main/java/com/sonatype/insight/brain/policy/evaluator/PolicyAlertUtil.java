/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;

public class PolicyAlertUtil
{
  public static List<PolicyAlert> createPolicyAlerts(PolicyEvaluation policyEvaluation) {
    if (policyEvaluation == null) {
      return Collections.emptyList();
    }

    List<PolicyViolation> policyViolations = new PolicyViolationDAO().getActiveByEvaluationId(policyEvaluation.getId());
    return createPolicyAlerts(policyViolations, policyEvaluation.getStageTypeId(), policyEvaluation.isForMonitoring());
  }

  public static List<PolicyAlert> createPolicyAlerts(List<PolicyViolation> policyViolations,
                                                     String stageTypeId,
                                                     boolean forMonitoring)
  {
    List<PolicyAlert> result = new ArrayList<>();
    PolicyDAO policyDAO = new PolicyDAO();
    Map<String, PolicyFact> policyFactsByPolicyId = new LinkedHashMap<>();
    for (PolicyViolation policyViolation : policyViolations) {
      String policyId = policyViolation.getPolicyId();
      PolicyFact policyFact = policyFactsByPolicyId.get(policyId);
      if (policyFact == null) {
        policyFact = new PolicyFact(policyId, policyViolation.getPolicyName(), policyViolation.getThreatLevel());
        policyFactsByPolicyId.put(policyId, policyFact);

        Policy policy = policyDAO.getById(policyId);
        List<? extends Action> actions;
        if (policy == null) {
          actions = Collections.emptyList();
        }
        else {
          actions = policy.toActions(stageTypeId, forMonitoring);
        }
        PolicyAlert policyAlert = new PolicyAlert(policyFact, actions);
        result.add(policyAlert);
      }

      ComponentFact componentFact = new ComponentFact(policyViolation.getComponentIdentifier(),
          policyViolation.getHash());
      ComponentDisplayNameUtil.injectDisplayName(componentFact);
      for (ConstraintFact constraintFact : policyViolation.getConstraintFacts()) {
        componentFact.addConstraintFact(constraintFact);
      }
      policyFact.addComponentFact(componentFact);
    }

    return result;
  }
}
