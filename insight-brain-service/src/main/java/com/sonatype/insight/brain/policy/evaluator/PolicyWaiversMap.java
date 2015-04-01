/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.model.policy.PolicyWaiver;

public class PolicyWaiversMap
{
  private Map<String, PolicyWaiver> policyWaiversByKey = new LinkedHashMap<>();

  public PolicyWaiversMap(List<PolicyWaiver> policyWaivers) {
    for (PolicyWaiver policyWaiver : policyWaivers) {
      String policyWaiverKey = policyWaiver.getPolicyId();
      if (policyWaiver.getHash() != null) {
        policyWaiverKey += "_" + policyWaiver.getHash();
      }
      policyWaiversByKey.put(policyWaiverKey, policyWaiver);
    }
  }

  public PolicyWaiver get(String policyId, String hash) {
    PolicyWaiver policyWaiver = policyWaiversByKey.get(policyId);
    if (policyWaiver == null) {
      policyWaiver = policyWaiversByKey.get(policyId + "_" + hash);
    }
    return policyWaiver;
  }
}
