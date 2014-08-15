/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Assists the {@link PolicyEvaluator} by factoring in policy waivers.
 * 
 * @since 1.9
 */
class PolicyWaiverEvaluator
{
  private static final Logger log = LoggerFactory.getLogger(PolicyWaiverEvaluator.class);

  /**
   * Splits the facts from the input list into those that are subject to a policy waiver for the specified application
   * and those that are not.
   */
  public PolicyWaiverResults applyWaivers(String applicationId, List<MatchFact> facts) {
    List<PolicyWaiver> policyWaivers = new PolicyWaiverDAO().getByOwnerId(applicationId, true /* inherit */);
    log.debug("Applying {} waivers to {} facts for application ID {}", policyWaivers.size(), facts.size(),
        applicationId);
    Map<String, PolicyWaiver> policyWaiversByKey = new LinkedHashMap<>();
    for (PolicyWaiver policyWaiver : policyWaivers) {
      String policyWaiverKey = policyWaiver.getPolicyId();
      if (policyWaiver.getHash() != null) {
        policyWaiverKey += "_" + policyWaiver.getHash();
      }
      policyWaiversByKey.put(policyWaiverKey, policyWaiver);
    }

    PolicyWaiverResults results = new PolicyWaiverResults();
    for (MatchFact fact : facts) {
      PolicyWaiver policyWaiver = findPolicyWaiverForMatchFact(fact, policyWaiversByKey);
      fact.setPolicyWaiver(policyWaiver);
      results.addFact(fact);
    }
    return results;
  }

  private PolicyWaiver findPolicyWaiverForMatchFact(MatchFact fact, Map<String, PolicyWaiver> policyWaiversByKey) {
    String key = fact.getPolicyId();
    PolicyWaiver policyWaiver = policyWaiversByKey.get(key);
    if (policyWaiver == null) {
      key += "_" + fact.getComponent().getHash();
      policyWaiver = policyWaiversByKey.get(key);
    }

    return policyWaiver;
  }
}
