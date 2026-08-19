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
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.utils.ComponentFactUtil;

/**
 * @since 1.21.0
 */
@Named
@Singleton
public class PolicyNotificationUtil
{
  private final OwnerDAO ownerDAO;

  private final PolicyDAO policyDAO;

  @Inject
  public PolicyNotificationUtil(final OwnerDAO ownerDAO, final PolicyDAO policyDAO) {
    this.ownerDAO = ownerDAO;
    this.policyDAO = policyDAO;
  }

  public List<PolicyNotification> createPolicyNotifications(
      Owner owner,
      List<PolicyViolation> policyViolations,
      String stageTypeId,
      boolean forMonitoring)
  {
    List<PolicyNotification> result = new ArrayList<>();
    List<String> ownerIds = ownerDAO.getOwnerIds(owner);

    Map<String, PolicyFact> policyFactsByPolicyId = new LinkedHashMap<>();
    for (PolicyViolation policyViolation : policyViolations) {
      String policyId = policyViolation.getPolicyId();
      PolicyFact policyFact = policyFactsByPolicyId.get(policyId);
      if (policyFact == null) {
        policyFact = new PolicyFact(policyId, policyViolation.getPolicyName(), policyViolation.getThreatLevel());
        policyFactsByPolicyId.put(policyId, policyFact);

        Policy policy = policyDAO.getById(policyId);
        Notifications notifications;
        if (policy == null) {
          // The policy has since been deleted, so there are no configured notifications to consider sending
          notifications = new Notifications();
        }
        else {
          notifications =
              policy.getEffectiveNotifications(ownerIds).getApplicable(stageTypeId, forMonitoring);
        }
        PolicyNotification policyNotification = new PolicyNotification(policyFact, notifications);
        result.add(policyNotification);
      }

      ComponentFact componentFact = new ComponentFact(policyViolation.getComponentIdentifier(),
          policyViolation.getHash());
      ComponentFactUtil.injectDisplayName(componentFact);
      for (ConstraintFact constraintFact : policyViolation.getConstraintFacts()) {
        componentFact.addConstraintFact(constraintFact);
      }
      policyFact.addComponentFact(componentFact);
    }

    return result;
  }
}
