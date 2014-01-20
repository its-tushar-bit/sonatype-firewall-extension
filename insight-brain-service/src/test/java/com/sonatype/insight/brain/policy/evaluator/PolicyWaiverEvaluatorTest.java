/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.TemporaryEntity;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;

import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

public class PolicyWaiverEvaluatorTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  private PolicyWaiverEvaluator evaluator = new PolicyWaiverEvaluator();

  private String appId = "some-app-id";

  private Component newComponent(String hash) {
    Component component = new Component();
    component.setHash(hash);
    return component;
  }

  @Test
  public void testApplyWaivers_SpecificComponent() {
    tempEntity.newWaiver("aaaaaaaaaaaaaaaaaaa0", "policy-0", appId);
    MatchFact fact1 = new MatchFact(newComponent("aaaaaaaaaaaaaaaaaaa0"), "policy-0", "constraint-0");
    MatchFact fact2 = new MatchFact(newComponent("aaaaaaaaaaaaaaaaaaa0"), "policy-0", "constraint-1");
    MatchFact fact3 = new MatchFact(newComponent("aaaaaaaaaaaaaaaaaaa0"), "policy-1", "constraint-0");
    MatchFact fact4 = new MatchFact(newComponent("aaaaaaaaaaaaaaaaaaa1"), "policy-0", "constraint-0");
    List<MatchFact> facts = evaluator.applyWaivers(appId, Arrays.asList(fact1, fact2, fact3, fact4));
    assertThat(facts, contains(fact3, fact4));
  }

  @Test
  public void testApplyWaivers_EntirePolicy() {
    tempEntity.newWaiver("policy-0", appId);
    MatchFact fact1 = new MatchFact(newComponent("aaaaaaaaaaaaaaaaaaa0"), "policy-0", "constraint-0");
    MatchFact fact2 = new MatchFact(newComponent("aaaaaaaaaaaaaaaaaaa0"), "policy-0", "constraint-1");
    MatchFact fact3 = new MatchFact(newComponent("aaaaaaaaaaaaaaaaaaa0"), "policy-1", "constraint-0");
    MatchFact fact4 = new MatchFact(newComponent("aaaaaaaaaaaaaaaaaaa1"), "policy-0", "constraint-0");
    List<MatchFact> facts = evaluator.applyWaivers(appId, Arrays.asList(fact1, fact2, fact3, fact4));
    assertThat(facts, contains(fact3));
  }
}
