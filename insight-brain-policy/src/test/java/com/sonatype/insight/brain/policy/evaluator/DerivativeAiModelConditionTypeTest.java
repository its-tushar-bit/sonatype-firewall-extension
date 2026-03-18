/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.dto.model.DerivedFromAiModel;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.DerivativeAiModelConditionType;
import com.sonatype.insight.brain.model.policy.facts.ConditionTrigger;
import com.sonatype.insight.brain.model.policy.facts.TriggerDerivedFromAiModel;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DerivativeAiModelConditionTypeTest
    extends AbstractPolicyEvaluationTest
{
  private Constraint createConstraint(String operator) {
    return createConstraint("ConstraintId1", "Constraint Name 1", DerivativeAiModelConditionType.ID, operator,
        null /* value */);
  }

  @Test
  public void testEvaluateIsTrue() {
    // Create policy constraints
    Constraint constraint = createConstraint("is true");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    // component1 is a derivative AI model
    Component component1 = new Component(ComponentIdentifier.createHuggingfaceModelCoordinates("repoId1", "model1",
        "version1", "modelFormat1", "modelExtension1"));
    ComponentIdentifier derivedFromAiModelComponentIdentifier1 = ComponentIdentifier
        .createHuggingfaceModelCoordinates("repoIdD1", "modelD1", "versionD1", "modelFormatD1", "modelExtensionD1");
    component1.setDerivedFromAiModel(new DerivedFromAiModel(derivedFromAiModelComponentIdentifier1, 0.5));
    component1.setMatchState(MatchState.EXACT);
    components.add(component1);
    // component2 is not a derivative AI model
    Component component2 = new Component(ComponentIdentifier.createHuggingfaceModelCoordinates("repoId2", "model2",
        "version2", "modelFormat2", "modelExtension2"));
    // Same coordinates as component2 - so component2 is not a derivative AI model
    component2.setDerivedFromAiModel(new DerivedFromAiModel(component2.getComponentIdentifier(), 1.0));
    component2.setMatchState(MatchState.EXACT);
    components.add(component2);
    // component3 is not a derivative AI model because it does not have a derivedFromAiModel
    Component component3 = new Component(ComponentIdentifier.createHuggingfaceModelCoordinates("repoId3", "model3",
        "version3", "modelFormat3", "modelExtension3"));
    component3.setMatchState(MatchState.EXACT);
    components.add(component3);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    ConditionTrigger expectedConditionTrigger = new ConditionTrigger(0,
        new TriggerDerivedFromAiModel(new DerivedFromAiModel(derivedFromAiModelComponentIdentifier1, 0.5)));
    assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID, DerivativeAiModelConditionType.ID,
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
    assertThat(actualReason)
        .isEqualTo("AI model is derived from repoIdD1 : modelD1 : version : modelFormatD1 : modelExtensionD1");
  }

  @Test
  public void testEvaluateIsFalse() {
    // Create policy constraints
    Constraint constraint = createConstraint("is false");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    // component1 is a derivative AI model
    Component component1 = new Component(ComponentIdentifier.createHuggingfaceModelCoordinates("repoId1", "model1",
        "version1", "modelFormat1", "modelExtension1"));
    ComponentIdentifier derivedFromAiModelComponentIdentifier1 = ComponentIdentifier
        .createHuggingfaceModelCoordinates("repoIdD1", "modelD1", "versionD1", "modelFormatD1", "modelExtensionD1");
    component1.setDerivedFromAiModel(new DerivedFromAiModel(derivedFromAiModelComponentIdentifier1, 0.5));
    component1.setMatchState(MatchState.EXACT);
    components.add(component1);
    // component2 is not a derivative AI model
    Component component2 = new Component(ComponentIdentifier.createHuggingfaceModelCoordinates("repoId2", "model2",
        "version2", "modelFormat2", "modelExtension2"));
    // Same coordinates as component2 - so component2 is not a derivative AI model
    component2.setDerivedFromAiModel(new DerivedFromAiModel(component2.getComponentIdentifier(), 1.0));
    component2.setMatchState(MatchState.EXACT);
    components.add(component2);
    // component3 is not a derivative AI model because it does not have a derivedFromAiModel
    Component component3 = new Component(ComponentIdentifier.createHuggingfaceModelCoordinates("repoId3", "model3",
        "version3", "modelFormat3", "modelExtension3"));
    component3.setMatchState(MatchState.EXACT);
    components.add(component3);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(2);

    assertFactCounts(1, 1, policyAlerts.get(0));
    ConditionTrigger expectedConditionTrigger = new ConditionTrigger(0,
        new TriggerDerivedFromAiModel(new DerivedFromAiModel(component2.getComponentIdentifier(), 1.0)));
    assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID, DerivativeAiModelConditionType.ID,
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
    assertThat(actualReason).isEqualTo("AI model is not derived from another AI model");

    assertFactCounts(1, 1, policyAlerts.get(0));
    expectedConditionTrigger = new ConditionTrigger(0, new TriggerDerivedFromAiModel(null));
    assertContainsPolicyAlert(component3, policy, constraint, FailActionType.ID, DerivativeAiModelConditionType.ID,
        expectedConditionTrigger, policyAlerts);
    actualReason = policyAlerts.get(1)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason).isEqualTo("AI model is not derived from another AI model");
  }

  @Test
  public void testEvaluate_DerivedFromAiModelNull() {
    // Create policy constraints
    Constraint constraint = createConstraint("is true");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component = new Component(ComponentIdentifier.createHuggingfaceModelCoordinates("repoId1", "model1",
        "version1", "modelFormat1", "modelExtension1"));
    component.setMatchState(MatchState.EXACT);
    components.add(component);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).isEmpty();
  }

  @Test
  public void testEvaluate_UnknownComponent() {
    // Create policy constraints
    Constraint constraint = createConstraint("is true");
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
