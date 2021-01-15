/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.facts.ConditionTrigger;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.policy.DroolsGenerator;
import com.sonatype.insight.json.store.JsonUtils;

import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public abstract class AbstractPolicyEvaluationTest
    extends InjectedTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Inject
  protected ComponentPolicyEvaluator componentPolicyEvaluator;

  protected List<PolicyAlert> evaluate(Policy policy, List<Component> components) {
    return evaluate(new Stage(BuildStageType.ID), policy, components);
  }

  protected List<PolicyAlert> evaluate(Stage stage, Policy policy, List<Component> components) {
    DroolsGenerator.generate(policy);
    return componentPolicyEvaluator.evaluate(null /* applicationId */, stage, Arrays.asList(policy), components)
        .getActiveAlerts();
  }

  protected Constraint createConstraint(String constraintId,
                                        String constraintName,
                                        String conditionTypeId,
                                        String operator,
                                        String value)
  {
    Condition condition = new Condition(conditionTypeId, operator, value);
    Constraint constraint = new Constraint(constraintId, constraintName, LogicalOperator.AND);
    constraint.addCondition(condition);
    return constraint;
  }

  public static void assertFactCounts(int expectedConstraintFactCount,
                                      int expectedComponentFactCount,
                                      PolicyAlert actualPolicyAlert)
  {
    List<ComponentFact> componentFacts = actualPolicyAlert.getTrigger().getComponentFacts();
    assertThat(componentFacts).hasSize(expectedComponentFactCount);

    int actualConstraintFactCount = 0;
    Set<String> observeredConstraints = new HashSet<>();
    for (ComponentFact componentFact : componentFacts) {
      for (ConstraintFact constraintFact : componentFact.getConstraintFacts()) {
        if (observeredConstraints.add(constraintFact.getConstraintId())) {
          actualConstraintFactCount++;
        }
      }
    }
    assertThat(actualConstraintFactCount).as("Incorrect number of constraint facts")
        .isEqualTo(expectedConstraintFactCount);
  }

  private static List<ConditionFact> findConditionFactsInPolicyAlerts(Component expectedComponent,
                                                                      Policy expectedPolicy,
                                                                      Constraint expectedConstraint,
                                                                      String expectedActionTypeId,
                                                                      String expectedConditionTypeId,
                                                                      List<PolicyAlert> actual)
  {
    List<ConditionFact> result = new ArrayList<>();

    for (PolicyAlert actualPolicyAlert : actual) {
      PolicyFact policyFact = actualPolicyAlert.getTrigger();
      if (expectedPolicy.getId().equals(policyFact.getPolicyId())
          && expectedPolicy.getName().equals(policyFact.getPolicyName())
          && policyAlertContainsAction(actualPolicyAlert, expectedActionTypeId)) {
        for (ComponentFact componentFact : policyFact.getComponentFacts()) {
          if (Objects.equals(expectedComponent.getComponentIdentifier(), componentFact.getComponentIdentifier())
              && Objects.equals(expectedComponent.getHash(), componentFact.getHash())) {
            for (ConstraintFact constraintFact : componentFact.getConstraintFacts()) {
              if (expectedConstraint.getId().equals(constraintFact.getConstraintId())
                  && expectedConstraint.getName().equals(constraintFact.getConstraintName())) {
                for (ConditionFact conditionFact : constraintFact.getConditionFacts()) {
                  if (expectedConditionTypeId.equals(conditionFact.getConditionTypeId())) {
                    result.add(conditionFact);
                  }
                }
              }
            }
          }
        }
      }
    }

    return result;
  }

  public static List<ConditionFact> assertContainsPolicyAlert(Component expectedComponent,
                                                              Policy expectedPolicy,
                                                              Constraint expectedConstraint,
                                                              String expectedActionTypeId,
                                                              String expectedConditionTypeId,
                                                              List<PolicyAlert> actual)
  {
    List<ConditionFact> conditionFacts = findConditionFactsInPolicyAlerts(expectedComponent, expectedPolicy,
        expectedConstraint, expectedActionTypeId, expectedConditionTypeId, actual);
    if (conditionFacts.isEmpty()) {
      fail("Cannot find expected policy alert in:" + toString(actual));
    }

    return conditionFacts;
  }

  public static List<ConditionFact> assertContainsPolicyAlert(Component expectedComponent,
                                                              Policy expectedPolicy,
                                                              Constraint expectedConstraint,
                                                              String expectedActionTypeId,
                                                              String expectedConditionTypeId,
                                                              ConditionTrigger expectedConditionTrigger,
                                                              List<PolicyAlert> actual)
  {
    List<ConditionFact> conditionFacts = findConditionFactsInPolicyAlerts(expectedComponent, expectedPolicy,
        expectedConstraint, expectedActionTypeId, expectedConditionTypeId, actual);
    if (conditionFacts.isEmpty()) {
      fail("Cannot find expected policy alert in:" + toString(actual));
    }

    for (ConditionFact conditionFact : conditionFacts) {
      if (conditionFact.getTriggerJson().equals(JsonUtils.writeUnformatted(expectedConditionTrigger))) {
        return conditionFacts;
      }
    }
    fail("Cannot find expected policy alert with condition trigger in:" + toString(actual));
    return null; // unreachable, only needed to avoid warnings
  }

  public static void assertNotContainsPolicyAlert(Component expectedComponent,
                                                  Policy expectedPolicy,
                                                  Constraint expectedConstraint,
                                                  String expectedActionTypeId,
                                                  String expectedConditionTypeId,
                                                  List<PolicyAlert> actual)
  {
    List<ConditionFact> conditionFacts = findConditionFactsInPolicyAlerts(expectedComponent, expectedPolicy,
        expectedConstraint, expectedActionTypeId, expectedConditionTypeId, actual);
    if (!conditionFacts.isEmpty()) {
      fail("Found unexpected policy alert in:" + toString(actual));
    }
  }

  private static String toString(List<PolicyAlert> policyAlerts) {
    StringBuilder result = new StringBuilder();
    for (PolicyAlert policyAlert : policyAlerts) {
      result.append(policyAlert.getTrigger().toString());
    }
    return result.toString();
  }

  private static boolean policyAlertContainsAction(PolicyAlert actualPolicyAlert, String actionTypeId) {
    for (Action action : actualPolicyAlert.getActions()) {
      if (actionTypeId.equals(action.getActionTypeId())) {
        return true;
      }
    }
    return false;
  }
}
