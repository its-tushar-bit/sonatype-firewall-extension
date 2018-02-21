/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

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
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.policy.DroolsGenerator;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.codehaus.plexus.util.StringUtils;
import org.junit.Assert;

import static org.junit.Assert.fail;

public abstract class AbstractPolicyEvaluationTest
    extends AbstractComponentTest
{
  @Inject
  protected ComponentPolicyEvaluator componentPolicyEvaluator;

  @Override
  public void setUpTestLicenseThreatGroups() {
    // blank slate please
  }

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
    Assert.assertEquals("Incorrect number of component facts:" + componentFacts, expectedComponentFactCount,
        componentFacts.size());

    int actualConstraintFactCount = 0;
    Set<String> observeredConstraints = new HashSet<>();
    for (ComponentFact componentFact : componentFacts) {
      for (ConstraintFact constraintFact : componentFact.getConstraintFacts()) {
        if (observeredConstraints.add(constraintFact.getConstraintId())) {
          actualConstraintFactCount++;
        }
      }
    }
    Assert.assertEquals("Incorrect number of constraint facts", expectedConstraintFactCount, actualConstraintFactCount);
  }

  private static ConditionFact findConditionFactInPolicyAlerts(Component expectedComponent,
                                                               Policy expectedPolicy,
                                                               Constraint expectedConstraint,
                                                               String expectedActionTypeId,
                                                               String expectedConditionTypeId,
                                                               List<PolicyAlert> actual)
  {
    for (PolicyAlert actualPolicyAlert : actual) {
      PolicyFact policyFact = actualPolicyAlert.getTrigger();
      if (expectedPolicy.getId().equals(policyFact.getPolicyId())
          && expectedPolicy.getName().equals(policyFact.getPolicyName())
          && policyAlertContainsAction(actualPolicyAlert, expectedActionTypeId)) {
        for (ComponentFact componentFact : policyFact.getComponentFacts()) {
          if (Objects.equals(expectedComponent.getComponentIdentifier(), componentFact.getComponentIdentifier())
              && StringUtils.equals(expectedComponent.getHash(), componentFact.getHash())) {
            for (ConstraintFact constraintFact : componentFact.getConstraintFacts()) {
              if (expectedConstraint.getId().equals(constraintFact.getConstraintId())
                  && expectedConstraint.getName().equals(constraintFact.getConstraintName())) {
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

    return null;
  }

  public static ConditionFact assertContainsPolicyAlert(Component expectedComponent,
                                                        Policy expectedPolicy,
                                                        Constraint expectedConstraint,
                                                        String expectedActionTypeId,
                                                        String expectedConditionTypeId,
                                                        List<PolicyAlert> actual)
  {
    ConditionFact conditionFact = findConditionFactInPolicyAlerts(expectedComponent, expectedPolicy, expectedConstraint,
        expectedActionTypeId, expectedConditionTypeId, actual);
    if (conditionFact == null) {
      fail("Cannot find expected policy alert in:" + toString(actual));
    }
    return conditionFact;
  }

  public static void assertNotContainsPolicyAlert(Component expectedComponent,
                                                  Policy expectedPolicy,
                                                  Constraint expectedConstraint,
                                                  String expectedActionTypeId,
                                                  String expectedConditionTypeId,
                                                  List<PolicyAlert> actual)
  {
    ConditionFact conditionFact = findConditionFactInPolicyAlerts(expectedComponent, expectedPolicy, expectedConstraint,
        expectedActionTypeId, expectedConditionTypeId, actual);
    if (conditionFact != null) {
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
