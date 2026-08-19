/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
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
import com.sonatype.insight.brain.AbstractDataTest;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.api.io.ResourceType;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class DroolsGeneratorTest
    extends AbstractDataTest
{
  private LabelDAO labelDAO;

  @BeforeEach
  public void setUp() {
    labelDAO = daoFactory.createLabelDAO();
  }

  @Test
  public void testGenerate() {
    final List<Constraint> constraints = new ArrayList<>();
    final Constraint constraint1 = new Constraint("ConstraintId1", "Constraint Name 1", LogicalOperator.AND);
    Condition condition1 = new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0");
    constraint1.addCondition(condition1);
    Condition condition2 = new Condition(LicenseConditionType.ID, "is", "Apache-2.0");
    constraint1.addCondition(condition2);
    constraints.add(constraint1);
    final Constraint constraint2 = new Constraint("ConstraintId2", "Constraint Name 2", LogicalOperator.OR);
    condition1 = new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "5");
    constraint2.addCondition(condition1);
    condition2 = new Condition(LicenseConditionType.ID, "is not", "GPL-2.0");
    constraint2.addCondition(condition2);
    constraints.add(constraint2);

    final Policy policy = new Policy();
    policy.setId("PolicyId1");
    policy.setName("Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, Action.ID_FAIL);

    DroolsGenerator.generate(policy, labelDAO);
    System.out.println(policy.getDroolsCode());
    // TODO Add asserts - for now it's good if we get no exceptions :)

    final KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
    // this will parse and compile in one step
    kbuilder.add(ResourceFactory.newReaderResource(new StringReader(policy.getDroolsCode())), ResourceType.DRL);
    assertThat(kbuilder.getErrors()).isEmpty();
  }

  @Test
  public void testGet_NoPolicies() {
    List<Policy> policies = Collections.emptyList();
    String droolsCode = DroolsGenerator.get(policies);
    assertThat(droolsCode).isEqualTo("");
  }

  @Test
  public void testGet_OnePolicy() {
    Policy policy = new Policy("PolicyId", "Policy name");
    policy.setDroolsCode("abc");
    List<Policy> policies = Collections.singletonList(policy);
    String droolsCode = DroolsGenerator.get(policies);
    assertThat(droolsCode).isEqualTo("abc\n");
  }

  @Test
  public void testGet_TwoPolicies() {
    Policy policy1 = new Policy("PolicyId1", "Policy name 1");
    policy1.setDroolsCode("abc");
    Policy policy2 = new Policy("PolicyId2", "Policy name 2");
    policy2.setDroolsCode("def");
    List<Policy> policies = Arrays.asList(policy1, policy2);
    String droolsCode = DroolsGenerator.get(policies);
    assertThat(droolsCode).isEqualTo("abc\ndef\n");
  }
}
