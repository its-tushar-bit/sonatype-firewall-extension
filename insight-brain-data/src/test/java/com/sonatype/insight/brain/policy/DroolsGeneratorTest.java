/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.io.ResourceFactory;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class DroolsGeneratorTest
{
  @Test
  public void testGenerate() {
    final List<Constraint> constraints = new ArrayList<Constraint>();
    final Constraint constraint1 = new Constraint("ConstraintId1", "Constraint Name 1", LogicalOperator.AND);
    Condition condition1 = new Condition(SecurityVulnerabilityConditionType.ID, "present");
    constraint1.addCondition(condition1);
    Condition condition2 = new Condition(LicenseConditionType.ID, "is", "Apache-2.0");
    constraint1.addCondition(condition2);
    constraints.add(constraint1);
    final Constraint constraint2 = new Constraint("ConstraintId2", "Constraint Name 2", LogicalOperator.OR);
    condition1 = new Condition(SecurityVulnerabilityConditionType.ID, "absent");
    constraint2.addCondition(condition1);
    condition2 = new Condition(LicenseConditionType.ID, "is not", "GPL-2.0");
    constraint2.addCondition(condition2);
    constraints.add(constraint2);

    final Policy policy = new Policy();
    policy.setId("PolicyId1");
    policy.setName("Policy Name 1");
    policy.setConstraints(constraints);
    Action action = new Action();
    action.setActionTypeId(FailActionType.ID);
    policy.addAction(BuildStageType.ID, action);

    DroolsGenerator.generate(policy);
    System.out.println(policy.getDroolsCode());
    // TODO Add asserts - for now it's good if we get no exceptions :)

    final KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
    // this will parse and compile in one step
    kbuilder.add(ResourceFactory.newReaderResource(new StringReader(policy.getDroolsCode())), ResourceType.DRL);
    Assert.assertFalse(kbuilder.getErrors().toString(), kbuilder.hasErrors());
  }

  @Test
  public void testGet_NoPolicies() {
    List<Policy> policies = Collections.emptyList();
    String droolsCode = DroolsGenerator.get(policies);
    assertThat(droolsCode, is(""));
  }

  @Test
  public void testGet_OnePolicy() {
    Policy policy = new Policy("PolicyId", "Policy name");
    policy.setDroolsCode("abc");
    List<Policy> policies = Collections.singletonList(policy);
    String droolsCode = DroolsGenerator.get(policies);
    assertThat(droolsCode, is("abc\n"));
  }

  @Test
  public void testGet_TwoPolicies() {
    Policy policy1 = new Policy("PolicyId1", "Policy name 1");
    policy1.setDroolsCode("abc");
    Policy policy2 = new Policy("PolicyId2", "Policy name 2");
    policy2.setDroolsCode("def");
    List<Policy> policies = Arrays.asList(policy1, policy2);
    String droolsCode = DroolsGenerator.get(policies);
    assertThat(droolsCode, is("abc\ndef\n"));
  }
}
