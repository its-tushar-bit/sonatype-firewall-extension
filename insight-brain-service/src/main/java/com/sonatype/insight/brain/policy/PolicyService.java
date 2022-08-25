/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class PolicyService
{
  private final PolicyDAO policyDAO = new PolicyDAO();

  public void removeOverrides(String internalOwnerId, TransactionContext tx) {
    List<Policy> collect = policyDAO.getAll(tx).stream()
        .filter(policy -> policy.getPolicyActionsOverrides() != null
            && policy.getPolicyActionsOverrides().containsKey(internalOwnerId))
        .collect(Collectors.toList());
    for (Policy policy : collect) {
      policy.getPolicyActionsOverrides().remove(internalOwnerId);
      policyDAO.update(tx, policy);
    }
  }

  public void removeOverrides(TransactionContext tx, String internalOwnerId, String applicationId) {
    policyDAO.getByOwnerId(tx, internalOwnerId).stream()
        .filter(policy -> policy.getPolicyActionsOverrides() != null
            && policy.getPolicyActionsOverrides().containsKey(applicationId))
        .forEach(policy -> {
          policy.getPolicyActionsOverrides().remove(applicationId);
          policyDAO.update(tx, policy);
        });
  }

  public Policy removeOverride(String internalOwnerId, String policyId) {
    Policy policy = policyDAO.getByIdNotNull(policyId);
    Map<String, Map<String, String>> overrides = policy.getPolicyActionsOverrides();

    if (overrides != null && !overrides.isEmpty() && overrides.containsKey(internalOwnerId)) {
      overrides.remove(internalOwnerId);
      policy.setPolicyActionsOverrides(overrides);
      policyDAO.update(policy);
    }
    return policy;
  }
}
