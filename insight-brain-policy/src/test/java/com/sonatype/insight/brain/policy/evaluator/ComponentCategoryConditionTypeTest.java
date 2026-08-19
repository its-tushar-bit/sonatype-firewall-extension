/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.ComponentCategory;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.ComponentCategoryConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ComponentCategoryConditionTypeTest
    extends AbstractPolicyEvaluationTest
{
  private static final List<Component> COMPONENTS = new ArrayList<>();

  private static final Component COMPONENT_1 = ComponentFactory.forGav("g1", "a1", "1", MatchState.EXACT);

  private static final Component COMPONENT_2 = ComponentFactory.forGav("g2", "a2", "1", MatchState.EXACT);

  private static final Component COMPONENT_3 = ComponentFactory.forGav("g3", "a3", "1", MatchState.EXACT);

  private static final String WEB_FRAMEWORKS_ID = "110";

  private static final String WEB_FRAMEWORKS_REST_ID = "170";

  private static final String WEB_FRAMEWORKS_REST_SERVER_ID = "171";

  static {
    // Add multiple categories
    COMPONENT_1
        .addComponentCategory(new ComponentCategory(WEB_FRAMEWORKS_REST_SERVER_ID, "Web Frameworks/Rest/Server"));
    COMPONENT_1.addComponentCategory(new ComponentCategory("154", "Mail/Server"));

    // Add single category
    COMPONENT_2.addComponentCategory(new ComponentCategory(WEB_FRAMEWORKS_ID, "Web Frameworks"));

    // Add category unrelated to "Web Frameworks/Rest/Server", but with a similar name
    COMPONENT_3.addComponentCategory(new ComponentCategory("151", "Server"));

    COMPONENTS.add(COMPONENT_1);
    COMPONENTS.add(COMPONENT_2);
    COMPONENTS.add(COMPONENT_3);
  }

  private Constraint createConstraint(String operator, String value) {
    return createConstraint("ConstraintId1", "Constraint Name 1", ComponentCategoryConditionType.ID, operator, value);
  }

  private Policy createPolicyWithConstraint(Constraint constraint) {
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);
    return policy;
  }

  @Test
  public void testEvaluateIs_withExactCategoryMatch() {
    // Use exact category from COMPONENT_1
    Constraint constraint = createConstraint("is", WEB_FRAMEWORKS_REST_SERVER_ID); // Component1 category
    Policy policy = createPolicyWithConstraint(constraint);

    List<PolicyAlert> policyAlerts = evaluate(policy, COMPONENTS);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(COMPONENT_1, policy, constraint, FailActionType.ID, ComponentCategoryConditionType.ID,
        policyAlerts);
    String actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason).isEqualTo("Component Category was Web Frameworks/Rest/Server, Mail/Server");
  }

  @Test
  public void testEvaluateIs_withParentCategoryMatch() {
    // Use category one level up from COMPONENT_1 category
    Constraint constraint = createConstraint("is", WEB_FRAMEWORKS_REST_ID);
    Policy policy = createPolicyWithConstraint(constraint);

    List<PolicyAlert> policyAlerts = evaluate(policy, COMPONENTS);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(COMPONENT_1, policy, constraint, FailActionType.ID, ComponentCategoryConditionType.ID,
        policyAlerts);
    String actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason).isEqualTo("Component Category was Web Frameworks/Rest/Server, Mail/Server");
  }

  @Test
  public void testEvaluateIs_withGrandparentCategoryMatch() {
    // Use category two levels up from COMPONENT_1 category
    Constraint constraint = createConstraint("is", WEB_FRAMEWORKS_ID);
    Policy policy = createPolicyWithConstraint(constraint);

    List<PolicyAlert> policyAlerts = evaluate(policy, COMPONENTS);
    assertThat(policyAlerts).hasSize(2);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertFactCounts(1, 1, policyAlerts.get(1));
    assertContainsPolicyAlert(COMPONENT_1, policy, constraint, FailActionType.ID, ComponentCategoryConditionType.ID,
        policyAlerts);
    assertContainsPolicyAlert(COMPONENT_2, policy, constraint, FailActionType.ID, ComponentCategoryConditionType.ID,
        policyAlerts);
    String actualReason1 = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason1).isEqualTo("Component Category was Web Frameworks/Rest/Server, Mail/Server");
    String actualReason2 = policyAlerts.get(1)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason2).isEqualTo("Component Category was Web Frameworks");
  }

  @Test
  public void testEvaluateIsNot_withExactCategoryMatch() {
    // Use exact category from COMPONENT_1
    Constraint constraint = createConstraint("is not", WEB_FRAMEWORKS_REST_SERVER_ID);
    Policy policy = createPolicyWithConstraint(constraint);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, COMPONENTS);
    assertThat(policyAlerts).hasSize(2);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertFactCounts(1, 1, policyAlerts.get(1));
    assertContainsPolicyAlert(COMPONENT_2, policy, constraint, FailActionType.ID, ComponentCategoryConditionType.ID,
        policyAlerts);
    assertContainsPolicyAlert(COMPONENT_3, policy, constraint, FailActionType.ID, ComponentCategoryConditionType.ID,
        policyAlerts);
    String actualReason1 = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason1).isEqualTo("Component Category was Web Frameworks, not Web Frameworks/Rest/Server");
    String actualReason2 = policyAlerts.get(1)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason2).isEqualTo("Component Category was Server, not Web Frameworks/Rest/Server");
  }

  @Test
  public void testEvaluateIsNot_withParentCategoryMatch() {
    // Use category one level up from COMPONENT_1 category
    Constraint constraint = createConstraint("is not", WEB_FRAMEWORKS_REST_ID);
    Policy policy = createPolicyWithConstraint(constraint);

    List<PolicyAlert> policyAlerts = evaluate(policy, COMPONENTS);
    assertThat(policyAlerts).hasSize(2);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertFactCounts(1, 1, policyAlerts.get(1));
    assertContainsPolicyAlert(COMPONENT_2, policy, constraint, FailActionType.ID, ComponentCategoryConditionType.ID,
        policyAlerts);
    assertContainsPolicyAlert(COMPONENT_3, policy, constraint, FailActionType.ID, ComponentCategoryConditionType.ID,
        policyAlerts);
    String actualReason1 = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason1).isEqualTo("Component Category was Web Frameworks, not Web Frameworks/Rest");
    String actualReason2 = policyAlerts.get(1)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason2).isEqualTo("Component Category was Server, not Web Frameworks/Rest");
  }

  @Test
  public void testEvaluateIsNot_withGrandparentCategoryMatch() {
    // Use category two levels up from COMPONENT_1 category, which is also exact category of COMPONENT_2
    Constraint constraint = createConstraint("is not", WEB_FRAMEWORKS_ID);
    Policy policy = createPolicyWithConstraint(constraint);

    List<PolicyAlert> policyAlerts = evaluate(policy, COMPONENTS);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(COMPONENT_3, policy, constraint, FailActionType.ID, ComponentCategoryConditionType.ID,
        policyAlerts);
    String actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason).isEqualTo("Component Category was Server, not Web Frameworks");
  }

  @Test
  public void testValidateCondition_InvalidValue() {
    Condition condition = new Condition(ComponentCategoryConditionType.ID, "is", "abc");
    assertThatThrownBy(() -> ConditionTypes.ComponentCategoryConditionType
        .validateCondition(null, condition, null /* applicationId */)).isInstanceOf(InvalidConditionException.class)
            .hasMessageEndingWith("Value not supported: abc");
  }
}
