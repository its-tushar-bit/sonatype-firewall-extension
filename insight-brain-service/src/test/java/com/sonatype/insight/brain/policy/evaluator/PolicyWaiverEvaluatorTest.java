/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
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

  private Organization org;
  
  private Application app;

  private Policy policyApp;

  private Policy policyOrg;

  private Component newComponent(String hash) {
    Component component = new Component();
    component.setHash(hash);
    return component;
  }

  @Before
  public void init() {
    org = tempEntity.newOrganization("PolicyWaiverEvaluatorTest");
    app = tempEntity.newApplication(org.getId());
    policyApp = tempEntity.newPolicy(app.getId(), "PolicyWaiverEvaluatorTest0");
    policyOrg = tempEntity.newPolicy(org.getId(), "PolicyWaiverEvaluatorTest1");
  }

  @Test
  public void testApplyWaivers_SpecificComponent() {
    PolicyWaiver policyWaiver = tempEntity.newWaiver("aaaaaaaaaaaaaaaaaaa0", policyApp.getId(), app.getId());
    MatchFact fact1 = new MatchFact(newComponent("aaaaaaaaaaaaaaaaaaa0"), policyApp.getId(), "constraint-0");
    MatchFact fact2 = new MatchFact(newComponent("aaaaaaaaaaaaaaaaaaa0"), policyApp.getId(), "constraint-1");
    MatchFact fact3 = new MatchFact(newComponent("aaaaaaaaaaaaaaaaaaa0"), policyOrg.getId(), "constraint-0");
    MatchFact fact4 = new MatchFact(newComponent("aaaaaaaaaaaaaaaaaaa1"), policyApp.getId(), "constraint-0");
    evaluator.applyWaivers(app.getId(), Arrays.asList(fact1, fact2, fact3, fact4));
    assertThat(fact1.getPolicyWaiver().getId(), is(policyWaiver.getId()));
    assertThat(fact2.getPolicyWaiver().getId(), is(policyWaiver.getId()));
    assertThat(fact3.getPolicyWaiver(), nullValue());
    assertThat(fact4.getPolicyWaiver(), nullValue());
  }

  @Test
  public void testApplyWaivers_EntirePolicy() {
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policyApp.getId(), app.getId());
    MatchFact fact1 = new MatchFact(newComponent("aaaaaaaaaaaaaaaaaaa0"), policyApp.getId(), "constraint-0");
    MatchFact fact2 = new MatchFact(newComponent("aaaaaaaaaaaaaaaaaaa0"), policyApp.getId(), "constraint-1");
    MatchFact fact3 = new MatchFact(newComponent("aaaaaaaaaaaaaaaaaaa0"), policyOrg.getId(), "constraint-0");
    MatchFact fact4 = new MatchFact(newComponent("aaaaaaaaaaaaaaaaaaa1"), policyApp.getId(), "constraint-0");
    evaluator.applyWaivers(app.getId(), Arrays.asList(fact1, fact2, fact3, fact4));
    assertThat(fact1.getPolicyWaiver().getId(), is(policyWaiver.getId()));
    assertThat(fact2.getPolicyWaiver().getId(), is(policyWaiver.getId()));
    assertThat(fact3.getPolicyWaiver(), nullValue());
    assertThat(fact4.getPolicyWaiver().getId(), is(policyWaiver.getId()));
  }
  
  @Test
  public void testApplyWaivers_Inheritance() {
    Policy policyParentOrg = tempEntity.newPolicy(org.getParentOrganizationId(), "PolicyWaiverEvaluatorTest2");
    PolicyWaiver policyWaiverApp = tempEntity.newWaiver("aaaaaaaaaaaaaaaaaaa0", policyApp.getId(), app.getId());
    PolicyWaiver policyWaiverOrg = tempEntity.newWaiver("aaaaaaaaaaaaaaaaaaa0", policyOrg.getId(), org.getId());
    PolicyWaiver policyWaiverParentOrg = tempEntity.newWaiver("aaaaaaaaaaaaaaaaaaa0", policyParentOrg.getId(),
        org.getParentOrganizationId());
    MatchFact fact1 = new MatchFact(newComponent("aaaaaaaaaaaaaaaaaaa0"), policyApp.getId(), "constraint-0");
    MatchFact fact2 = new MatchFact(newComponent("aaaaaaaaaaaaaaaaaaa0"), policyOrg.getId(), "constraint-1");
    MatchFact fact3 = new MatchFact(newComponent("aaaaaaaaaaaaaaaaaaa0"), policyParentOrg.getId(), "constraint-0");

    evaluator.applyWaivers(app.getId(), Arrays.asList(fact1, fact2, fact3));
    assertThat(fact1.getPolicyWaiver().getId(), is(policyWaiverApp.getId()));
    assertThat(fact2.getPolicyWaiver().getId(), is(policyWaiverOrg.getId()));
    assertThat(fact3.getPolicyWaiver().getId(), is(policyWaiverParentOrg.getId()));
  }
}
