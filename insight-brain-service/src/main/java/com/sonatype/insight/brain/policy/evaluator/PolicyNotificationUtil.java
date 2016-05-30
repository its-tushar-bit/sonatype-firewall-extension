/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;

/**
 * @since 1.21.0
 */
public class PolicyNotificationUtil
{
  public static List<PolicyNotification> createPolicyNotifications(List<PolicyViolation> policyViolations,
                                                                   String stageTypeId,
                                                                   boolean forMonitoring)
  {
    List<PolicyNotification> result = new ArrayList<>();
    PolicyDAO policyDAO = new PolicyDAO();

    Map<String, PolicyFact> policyFactsByPolicyId = new LinkedHashMap<>();
    for (PolicyViolation policyViolation : policyViolations) {
      String policyId = policyViolation.getPolicyId();
      PolicyFact policyFact = policyFactsByPolicyId.get(policyId);
      if (policyFact == null) {
        policyFact = new PolicyFact(policyId, policyViolation.getPolicyName(), policyViolation.getThreatLevel());
        policyFactsByPolicyId.put(policyId, policyFact);

        Policy policy = policyDAO.getByIdNotNull(policyId);
        Notifications notifications = policy.getNotifications().getApplicable(stageTypeId, forMonitoring);
        PolicyNotification policyNotification = new PolicyNotification(policyFact, notifications);
        result.add(policyNotification);
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
