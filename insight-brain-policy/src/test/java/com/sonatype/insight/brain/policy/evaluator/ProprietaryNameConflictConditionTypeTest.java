/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.ProprietaryComponentName;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.ProprietaryNameConflictConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProprietaryNameConflictConditionTypeTest
    extends AbstractPolicyEvaluationTest
{
  private Constraint newConstraint(String operator) {
    return createConstraint("ConstraintId1", "Constraint Name 1", ProprietaryNameConflictConditionType.ID, operator,
        null /* value */);
  }

  private Policy newPolicy(Constraint constraint) {
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.addConstraint(constraint);
    policy.setAction(BuildStageType.ID, FailActionType.ID);
    return policy;
  }

  @Test
  public void testEvaluate_OperatorIsPresent() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository =
        tempEntity.newRepository(repositoryManager, "repo-id", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);

    Constraint constraint = newConstraint(ProprietaryNameConflictConditionType.OP_IS_PRESENT);
    Policy policy = newPolicy(constraint);

    List<Component> components = new ArrayList<>();

    Component component1 = new Component(ComponentIdentifier.createNpmCoordinates("@sonatype/cli", "1.0"));
    component1.setConflictingProprietaryName(new ProprietaryComponentName("@sonatype/*", repository.getId()));
    components.add(component1);

    Component component2 = new Component(ComponentIdentifier.createNpmCoordinates("cli", "1.1"));
    component2.setConflictingProprietaryName(null);
    components.add(component2);

    Component component3 = new Component();
    components.add(component3);

    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    List<ConditionFact> conditionFacts = assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID,
        ProprietaryNameConflictConditionType.ID, policyAlerts);
    assertThat(conditionFacts).hasSize(1);
    assertThat(conditionFacts.get(0).getReason())
        .isEqualTo("Component name conflicts with proprietary component @sonatype/* from repo-id");
    assertNotContainsPolicyAlert(component2, policy, constraint, FailActionType.ID,
        ProprietaryNameConflictConditionType.ID, policyAlerts);
    assertNotContainsPolicyAlert(component3, policy, constraint, FailActionType.ID,
        ProprietaryNameConflictConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluate_OperatorIsNotPresent() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository =
        tempEntity.newRepository(repositoryManager, "repo-id", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);

    Constraint constraint = newConstraint(ProprietaryNameConflictConditionType.OP_IS_NOT_PRESENT);
    Policy policy = newPolicy(constraint);

    List<Component> components = new ArrayList<>();

    Component component1 = new Component(ComponentIdentifier.createNpmCoordinates("@sonatype/cli", "1.0"));
    component1.setConflictingProprietaryName(new ProprietaryComponentName("@sonatype/*", repository.getId()));
    components.add(component1);

    Component component2 = new Component(ComponentIdentifier.createNpmCoordinates("cli", "1.1"));
    component2.setConflictingProprietaryName(null);
    components.add(component2);

    Component component3 = new Component();
    components.add(component3);

    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    List<ConditionFact> conditionFacts = assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID,
        ProprietaryNameConflictConditionType.ID, policyAlerts);
    assertThat(conditionFacts).hasSize(1);
    assertThat(conditionFacts.get(0).getReason())
        .isEqualTo("Component name does not conflict with any proprietary component");
    assertNotContainsPolicyAlert(component1, policy, constraint, FailActionType.ID,
        ProprietaryNameConflictConditionType.ID, policyAlerts);
    assertNotContainsPolicyAlert(component3, policy, constraint, FailActionType.ID,
        ProprietaryNameConflictConditionType.ID, policyAlerts);
  }
}
