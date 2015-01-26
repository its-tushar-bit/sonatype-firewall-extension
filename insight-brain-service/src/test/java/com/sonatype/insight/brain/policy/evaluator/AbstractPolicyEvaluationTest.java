/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.policy.DroolsGenerator;

import org.codehaus.plexus.util.StringUtils;
import org.junit.Assert;
import org.junit.Rule;

public abstract class AbstractPolicyEvaluationTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  protected PolicyEvaluator evaluator = new PolicyEvaluator();

  protected List<PolicyAlert> evaluate(Policy policy, List<Component> components) {
    DroolsGenerator.generate(policy);
    return evaluate(new Stage(BuildStageType.ID), policy, components);
  }

  protected List<PolicyAlert> evaluate(Stage stage, Policy policy, List<Component> components) {
    DroolsGenerator.generate(policy);
    return evaluator.evaluate(null /* applicationId */, stage, Arrays.asList(policy), components).getActiveAlerts();
  }

  protected Constraint createConstraint(String constraintId, String constraintName, String conditionTypeId,
      String operator, String value)
  {
    Condition condition = new Condition(conditionTypeId, operator, value);
    Constraint constraint = new Constraint(constraintId, constraintName, LogicalOperator.AND);
    constraint.addCondition(condition);
    return constraint;
  }

  public static void assertFactCounts(int expectedConstraintFactCount, int expectedComponentFactCount,
      PolicyAlert actualPolicyAlert)
  {
    List<ComponentFact> componentFacts = actualPolicyAlert.getTrigger().getComponentFacts();
    Assert.assertEquals("Incorrect number of component facts:" + componentFacts, expectedComponentFactCount,
        componentFacts.size());

    int actualConstraintFactCount = 0;
    Set<String> observeredConstraints = new HashSet<String>();
    for (ComponentFact componentFact : componentFacts) {
      for (ConstraintFact constraintFact : componentFact.getConstraintFacts()) {
        if (observeredConstraints.add(constraintFact.getConstraintId())) {
          actualConstraintFactCount++;
        }
      }
    }
    Assert.assertEquals("Incorrect number of constraint facts", expectedConstraintFactCount, actualConstraintFactCount);
  }

  public static ConditionFact assertContainsPolicyAlert(Component expectedComponent, String expectedPolicyId,
      String expectedPolicyName, String actionTypeId, String expectedConstraintId, String expectedConstraintName,
      String expectedConditionTypeId, List<PolicyAlert> actual)
  {
    for (PolicyAlert actualPolicyAlert : actual) {
      PolicyFact policyFact = actualPolicyAlert.getTrigger();
      if (expectedPolicyId.equals(policyFact.getPolicyId()) && expectedPolicyName.equals(policyFact.getPolicyName())
          && policyAlertContainsAction(actualPolicyAlert, actionTypeId)) {
        for (ComponentFact componentFact : policyFact.getComponentFacts()) {
          if (Objects.equals(expectedComponent.getComponentIdentifier(), componentFact.getComponentIdentifier())
              && StringUtils.equals(expectedComponent.getHash(), componentFact.getHash())) {
            for (ConstraintFact constraintFact : componentFact.getConstraintFacts()) {
              if (expectedConstraintId.equals(constraintFact.getConstraintId())
                  && expectedConstraintName.equals(constraintFact.getConstraintName())) {
                for (ConditionFact conditionFact : constraintFact.getConditionFacts()) {
                  if (expectedConditionTypeId.equals(conditionFact.getConditionTypeId())) {
                    return conditionFact;
                  }
                }
              }
            }
          }
        }
      }
    }

    Assert.fail(toString(actual));
    return null;
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
