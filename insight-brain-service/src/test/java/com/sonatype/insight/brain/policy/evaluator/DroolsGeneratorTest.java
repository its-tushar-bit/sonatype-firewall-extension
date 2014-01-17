/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidPolicyException;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.ValidationResult;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.io.ResourceFactory;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class DroolsGeneratorTest
{
  @Test
  public void testGenerate() {
    final List<Constraint> constraints = new ArrayList<Constraint>();
    final Constraint constraint1 = new Constraint("ConstraintId1", "Constraint Name 1", LogicalOperator.AND);
    Condition condition1 = new Condition();
    condition1.setConditionTypeId(SecurityVulnerabilityConditionType.ID);
    condition1.setOperator("present");
    constraint1.addCondition(condition1);
    Condition condition2 = new Condition();
    condition2.setConditionTypeId(LicenseConditionType.ID);
    condition2.setOperator("is");
    condition2.setValue("Apache-2.0");
    constraint1.addCondition(condition2);
    constraints.add(constraint1);
    final Constraint constraint2 = new Constraint();
    constraint2.setId("ConstraintId2");
    constraint2.setName("Constraint Name 2");
    constraint2.setOperator(LogicalOperator.OR);
    condition1 = new Condition();
    condition1.setConditionTypeId(SecurityVulnerabilityConditionType.ID);
    condition1.setOperator("absent");
    constraint2.addCondition(condition1);
    condition2 = new Condition();
    condition2.setConditionTypeId(LicenseConditionType.ID);
    condition2.setOperator("is not");
    condition2.setValue("GPL-2.0");
    constraint2.addCondition(condition2);
    constraints.add(constraint2);

    final Policy policy = new Policy();
    policy.setId("PolicyId1");
    policy.setName("Policy Name 1");
    policy.setConstraints(constraints);
    Action action = new Action();
    action.setActionTypeId(FailActionType.ID);
    policy.addAction(BuildStageType.ID, action);

    final DroolsGenerator generator = new DroolsGenerator();
    final String droolsCode = generator.generate(null /* applicationId */, Arrays.asList(policy));
    System.out.println(droolsCode);
    // TODO Add asserts - for now it's good if we get no exceptions :)

    final KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
    // this will parse and compile in one step
    kbuilder.add(ResourceFactory.newReaderResource(new StringReader(droolsCode)), ResourceType.DRL);
    Assert.assertFalse(kbuilder.getErrors().toString(), kbuilder.hasErrors());
  }

  @Test
  public void testConditionWithoutOperator() {
    final List<Constraint> constraints = new ArrayList<Constraint>();
    final Constraint constraint1 = new Constraint("ConstraintId1", "Constraint Name 1", LogicalOperator.AND);
    Condition condition1 = new Condition();
    condition1.setConditionTypeId(SecurityVulnerabilityConditionType.ID);
    constraint1.addCondition(condition1);
    constraints.add(constraint1);

    final Policy policy = new Policy();
    policy.setId("PolicyId1");
    policy.setName("Policy Name 1");
    policy.setConstraints(constraints);
    Action action = new Action();
    action.setActionTypeId(FailActionType.ID);
    policy.addAction(BuildStageType.ID, action);

    final DroolsGenerator generator = new DroolsGenerator();
    try {
      generator.generate(null /* applicationId */, Arrays.asList(policy));
      Assert.fail("Expected InvalidPolicyException");
    }
    catch (InvalidPolicyException expected) {
      if (!expected.getMessage().endsWith("Operator is null")) {
        throw expected;
      }
    }
  }

  @Test
  public void testConditionWithUnsupportedOperator() {
    final List<Constraint> constraints = new ArrayList<Constraint>();
    final Constraint constraint1 = new Constraint("ConstraintId1", "Constraint Name 1", LogicalOperator.AND);
    Condition condition1 = new Condition();
    condition1.setConditionTypeId(SecurityVulnerabilityConditionType.ID);
    condition1.setOperator("Verdi");
    constraint1.addCondition(condition1);
    constraints.add(constraint1);

    final Policy policy = new Policy();
    policy.setId("PolicyId1");
    policy.setName("Policy Name 1");
    policy.setConstraints(constraints);
    Action action = new Action();
    action.setActionTypeId(FailActionType.ID);
    policy.addAction(BuildStageType.ID, action);

    final DroolsGenerator generator = new DroolsGenerator();
    try {
      generator.generate(null /* applicationId */, Arrays.asList(policy));
      Assert.fail("Expected InvalidPolicyException");
    }
    catch (InvalidPolicyException expected) {
      if (!expected.getMessage().endsWith("Operator is not supported")) {
        throw expected;
      }
    }
  }

  @Test
  public void testConditionWithoutValue_Valid() {
    final List<Constraint> constraints = new ArrayList<Constraint>();
    final Constraint constraint1 = new Constraint("ConstraintId1", "Constraint Name 1", LogicalOperator.AND);
    Condition condition1 = new Condition();
    condition1.setConditionTypeId(SecurityVulnerabilityConditionType.ID);
    condition1.setOperator("present");
    constraint1.addCondition(condition1);
    constraints.add(constraint1);

    final Policy policy = new Policy();
    policy.setId("PolicyId1");
    policy.setName("Policy Name 1");
    policy.setConstraints(constraints);
    Action action = new Action();
    action.setActionTypeId(FailActionType.ID);
    policy.addAction(BuildStageType.ID, action);

    final DroolsGenerator generator = new DroolsGenerator();
    generator.generate(null /* applicationId */, Arrays.asList(policy));
  }

  @Test
  public void testConditionWithoutValue_Invalid() {
    final List<Constraint> constraints = new ArrayList<Constraint>();
    final Constraint constraint1 = new Constraint("ConstraintId1", "Constraint Name 1", LogicalOperator.AND);
    Condition condition1 = new Condition();
    condition1.setConditionTypeId(SecurityVulnerabilitySeverityConditionType.ID);
    condition1.setOperator(">");
    constraint1.addCondition(condition1);
    constraints.add(constraint1);

    final Policy policy = new Policy();
    policy.setId("PolicyId1");
    policy.setName("Policy Name 1");
    policy.setConstraints(constraints);
    Action action = new Action();
    action.setActionTypeId(FailActionType.ID);
    policy.addAction(BuildStageType.ID, action);

    final DroolsGenerator generator = new DroolsGenerator();
    try {
      generator.generate(null /* applicationId */, Arrays.asList(policy));
      Assert.fail("Expected InvalidPolicyException");
    }
    catch (InvalidPolicyException expected) {
      if (!expected.getMessage().endsWith("Value is null")) {
        throw expected;
      }
    }
  }

  @Test
  public void testLegacyPolicyWithInvalidNameCanStillBeEvaluated() {
    final Constraint constraint1 = new Constraint("ConstraintId1", "Constraint Name 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">", "0"));

    final Policy policy = new Policy();
    policy.setId("PolicyId1");
    policy.setName(" Invalid  Policy Name ! ");
    policy.addConstraint(constraint1);
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));

    ValidationResult validationResult = policy.validate(null /* ownerId */);
    assertTrue(validationResult.getErrors().contains("The policy name must be alpha numeric."));

    final DroolsGenerator generator = new DroolsGenerator();
    Assert.assertNotNull(generator.generate(null /* applicationId */, Arrays.asList(policy)));
  }

  // @Test
  // public void test()
  // {
  // final KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
  // // this will parse and compile in one step
  // kbuilder.add( ResourceFactory.newFileResource( "c:/temp/test.drl" ), ResourceType.DRL );
  // Assert.assertFalse( kbuilder.getErrors().toString(), kbuilder.hasErrors() );
  // }
}
