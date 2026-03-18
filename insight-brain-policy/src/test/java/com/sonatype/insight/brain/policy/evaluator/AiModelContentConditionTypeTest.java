/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.sonatype.clm.dto.model.component.AiModelContentType;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.AiModelContentConditionType;
import com.sonatype.insight.brain.model.policy.facts.ConditionTrigger;
import com.sonatype.insight.brain.model.policy.facts.TriggerAiModelContentType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AiModelContentConditionTypeTest
    extends AbstractPolicyEvaluationTest
{
  private Constraint createConstraint(String operator) {
    return createConstraint("ConstraintId1", "Constraint Name 1", AiModelContentConditionType.ID, operator,
        AiModelContentType.OBJECTIONABLE.getId());
  }

  @Test
  public void testEvaluate_Is() {
    // Create policy constraints
    Constraint constraint = createConstraint("is");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    // component1 has OBJECTIONABLE AI content type
    Component component1 = new Component(ComponentIdentifier.createHuggingfaceModelCoordinates("repoId1", "model1",
        "version1", "modelFormat1", "modelExtension1"));
    component1.setAiModelContentTypes(Set.of(AiModelContentType.OBJECTIONABLE));
    component1.setMatchState(MatchState.EXACT);
    components.add(component1);
    // component2 doesn't have AI content types
    Component component2 = new Component(ComponentIdentifier.createHuggingfaceModelCoordinates("repoId2", "model2",
        "version2", "modelFormat2", "modelExtension2"));
    component2.setMatchState(MatchState.EXACT);
    components.add(component2);
    // component3 is not an AI model
    Component component3 = new Component(ComponentIdentifier.createNpmCoordinates("packageId1", "version1"));
    component3.setMatchState(MatchState.EXACT);
    components.add(component3);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    ConditionTrigger expectedConditionTrigger =
        new ConditionTrigger(0, new TriggerAiModelContentType(AiModelContentType.OBJECTIONABLE.getId()));
    assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID, AiModelContentConditionType.ID,
        expectedConditionTrigger, policyAlerts);
    String actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason).isEqualTo("AI model is Objectionable");
  }

  @Test
  public void testEvaluate_IsNot() {
    // Create policy constraints
    Constraint constraint = createConstraint("is not");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    // component1 has OBJECTIONABLE AI content type
    Component component1 = new Component(ComponentIdentifier.createHuggingfaceModelCoordinates("repoId1", "model1",
        "version1", "modelFormat1", "modelExtension1"));
    component1.setAiModelContentTypes(Set.of(AiModelContentType.OBJECTIONABLE));
    component1.setMatchState(MatchState.EXACT);
    components.add(component1);
    // component2 doesn't have AI content types
    Component component2 = new Component(ComponentIdentifier.createHuggingfaceModelCoordinates("repoId2", "model2",
        "version2", "modelFormat2", "modelExtension2"));
    component2.setMatchState(MatchState.EXACT);
    components.add(component2);
    // component3 is not an AI model
    Component component3 = new Component(ComponentIdentifier.createNpmCoordinates("packageId1", "version1"));
    component3.setMatchState(MatchState.EXACT);
    components.add(component3);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    ConditionTrigger expectedConditionTrigger =
        new ConditionTrigger(0, new TriggerAiModelContentType(AiModelContentType.OBJECTIONABLE.getId()));
    assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID, AiModelContentConditionType.ID,
        expectedConditionTrigger, policyAlerts);
    String actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason).isEqualTo("AI model is not Objectionable");
  }

  @Test
  public void testEvaluate_UnknownComponent() {
    // Create policy constraints
    Constraint constraint = createConstraint("is");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component = new Component(ComponentIdentifier.createHuggingfaceModelCoordinates("repoId1", "model1",
        "version1", "modelFormat1", "modelExtension1"));
    component.setMatchState(MatchState.UNKNOWN);
    components.add(component);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).isEmpty();
  }
}
