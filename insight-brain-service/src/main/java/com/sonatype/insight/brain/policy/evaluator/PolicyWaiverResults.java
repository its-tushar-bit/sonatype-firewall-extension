/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.model.policy.facts.MatchFact;

/**
 * Carries the results from {@link PolicyWaiverEvaluator}.
 * 
 * @since 1.9
 */
class PolicyWaiverResults
{
  private List<MatchFact> activeFacts = new ArrayList<>();

  private List<MatchFact> waivedFacts = new ArrayList<>();

  void addFact(MatchFact fact, boolean waived) {
    (waived ? waivedFacts : activeFacts).add(fact);
  }

  /**
   * Gets the facts that have not been waived.
   */
  public List<MatchFact> getActiveFacts() {
    return activeFacts;
  }

  /**
   * Gets the facts that have been waived.
   */
  public List<MatchFact> getWaivedFacts() {
    return waivedFacts;
  }
}
