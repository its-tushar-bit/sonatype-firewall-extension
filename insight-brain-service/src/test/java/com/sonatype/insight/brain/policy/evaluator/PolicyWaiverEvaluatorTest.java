/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.Arrays;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class PolicyWaiverEvaluatorTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  private PolicyWaiverEvaluator evaluator = new PolicyWaiverEvaluator();

  private Application app;

  private Policy policy0;

  private Policy policy1;

  private Component newComponent(String hash) {
    Component component = new Component();
    component.setHash(hash);
    return component;
  }

  @Before
  public void init() {
    Organization org = tempEntity.newOrganization("PolicyWaiverEvaluatorTest");
    app = tempEntity.newApplication(org.getId());
    policy0 = tempEntity.newPolicy(app.getId(), "PolicyWaiverEvaluatorTest0");
    policy1 = tempEntity.newPolicy(app.getId(), "PolicyWaiverEvaluatorTest1");
  }

  @Test
  public void testApplyWaivers_SpecificComponent() {
    PolicyWaiver policyWaiver = tempEntity.newWaiver("aaaaaaaaaaaaaaaaaaa0", policy0.getId(), app.getId());
    MatchFact fact1 = new MatchFact(newComponent("aaaaaaaaaaaaaaaaaaa0"), policy0.getId(), "constraint-0");
    MatchFact fact2 = new MatchFact(newComponent("aaaaaaaaaaaaaaaaaaa0"), policy0.getId(), "constraint-1");
    MatchFact fact3 = new MatchFact(newComponent("aaaaaaaaaaaaaaaaaaa0"), policy1.getId(), "constraint-0");
    MatchFact fact4 = new MatchFact(newComponent("aaaaaaaaaaaaaaaaaaa1"), policy0.getId(), "constraint-0");
    evaluator.applyWaivers(app.getId(), Arrays.asList(fact1, fact2, fact3, fact4));
    assertThat(fact1.getPolicyWaiver().getId(), is(policyWaiver.getId()));
    assertThat(fact2.getPolicyWaiver().getId(), is(policyWaiver.getId()));
    assertThat(fact3.getPolicyWaiver(), nullValue());
    assertThat(fact4.getPolicyWaiver(), nullValue());
  }

  @Test
  public void testApplyWaivers_EntirePolicy() {
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy0.getId(), app.getId());
    MatchFact fact1 = new MatchFact(newComponent("aaaaaaaaaaaaaaaaaaa0"), policy0.getId(), "constraint-0");
    MatchFact fact2 = new MatchFact(newComponent("aaaaaaaaaaaaaaaaaaa0"), policy0.getId(), "constraint-1");
    MatchFact fact3 = new MatchFact(newComponent("aaaaaaaaaaaaaaaaaaa0"), policy1.getId(), "constraint-0");
    MatchFact fact4 = new MatchFact(newComponent("aaaaaaaaaaaaaaaaaaa1"), policy0.getId(), "constraint-0");
    evaluator.applyWaivers(app.getId(), Arrays.asList(fact1, fact2, fact3, fact4));
    assertThat(fact1.getPolicyWaiver().getId(), is(policyWaiver.getId()));
    assertThat(fact2.getPolicyWaiver().getId(), is(policyWaiver.getId()));
    assertThat(fact3.getPolicyWaiver(), nullValue());
    assertThat(fact4.getPolicyWaiver().getId(), is(policyWaiver.getId()));
  }
}
