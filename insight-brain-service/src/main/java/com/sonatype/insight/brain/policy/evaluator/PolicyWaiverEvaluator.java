/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
public class PolicyWaiverEvaluator
{
  private static final Logger log = LoggerFactory.getLogger(PolicyWaiverEvaluator.class);

  /**
   * Returns all those facts from the input list that are not subject to a policy waiver for the specified application.
   */
  public List<MatchFact> applyWaivers(String applicationId, List<MatchFact> facts) {
    List<PolicyWaiver> policyWaivers = new PolicyWaiverDAO().getByOwnerId(applicationId, true /* inherit */);
    log.debug("Applying {} waivers to {} facts for application ID {}", policyWaivers.size(), facts.size(),
        applicationId);
    Set<String> policyWaiverKeys = new LinkedHashSet<String>();
    for (PolicyWaiver policyWaiver : policyWaivers) {
      if (policyWaiver.getHash() == null) {
        policyWaiverKeys.add(policyWaiver.getPolicyId());
      }
      else {
        policyWaiverKeys.add(policyWaiver.getPolicyId() + "_" + policyWaiver.getHash());
      }
    }

    List<MatchFact> result = new ArrayList<MatchFact>();
    for (MatchFact fact : facts) {
      if (!isWaived(fact, policyWaiverKeys)) {
        result.add(fact);
      }
    }
    return result;
  }

  private boolean isWaived(MatchFact fact, Set<String> policyWaiverKeys) {
    String key = fact.getPolicyId();
    if (policyWaiverKeys.contains(key)) {
      return true;
    }
    key += "_" + fact.getComponent().getHash();
    if (policyWaiverKeys.contains(key)) {
      return true;
    }
    return false;
  }
}
