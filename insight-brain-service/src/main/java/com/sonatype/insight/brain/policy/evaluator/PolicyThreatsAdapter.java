/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.actions.ActionTypes;

/**
 * 
 * <p>
 * This adapter replaces the older {@link PolicyThreats} generator that used {@link PolicyAlert}s.
 * {@link PolicyViolation}s are directly adapted to {@link PolicyThreats}.
 * </p>
 * 
 * @since 1.13.0
 */
@Named
@Singleton
public class PolicyThreatsAdapter
{
  public PolicyThreats createPolicyThreats(List<PolicyViolation> policyViolations) {
    Map<String, PolicyThreats.Component> components = processPolicyViolations(policyViolations);

    PolicyThreats policyThreats = new PolicyThreats();
    policyThreats.version = 5;
    policyThreats.aaData = new ArrayList<>(components.values());

    return policyThreats;
  }

  private Map<String, PolicyThreats.Component> processPolicyViolations(List<PolicyViolation> policyViolations) {
    Map<String, PolicyThreats.Component> components = new LinkedHashMap<>();

    if (policyViolations != null) {
      for (PolicyViolation violation : policyViolations) {
        PolicyThreats.Component component = components.get(violation.getHash());
        if (component == null) {
          component = new PolicyThreats.Component();
          component.hash = violation.getHash();
          component.componentIdentifier = violation.getComponentIdentifier();
          component.policyThreatLevel = 0;
          component.policyName = "None";
          components.put(component.hash, component);
        }

        PolicyThreats.PolicyViolation policyThreatsPolicyViolation = toPolicyThreatsPolicyViolation(violation);
        component.allViolations.add(policyThreatsPolicyViolation);

        if (!violation.isWaived()) {
          component.activeViolations.add(policyThreatsPolicyViolation);
          if (violation.getThreatLevel() > component.policyThreatLevel || component.policyId == null) {
            component.policyId = violation.getPolicyId();
            component.policyName = violation.getPolicyName();
            component.policyThreatLevel = violation.getThreatLevel();
          }
        }
        else {
          component.waivedViolations.add(policyThreatsPolicyViolation);
        }
      }
    }

    return components;
  }

  private PolicyThreats.PolicyViolation toPolicyThreatsPolicyViolation(PolicyViolation violation) {
    PolicyThreats.PolicyViolation result = new PolicyThreats.PolicyViolation();
    result.policyId = violation.getPolicyId();
    result.policyViolationId = violation.getId();
    result.policyName = violation.getPolicyName();
    result.policyThreatLevel = violation.getThreatLevel();
    result.waived = violation.isWaived();
    result.grandfathered = violation.isGrandfathered();
    result.actions.addAll(toPolicyThreatsPolicyActions(violation));
    result.constraints.addAll(toPolicyThreatsPolicyConstraints(violation.getConstraintFacts()));
    result.constraintFactsJson = violation.getConstraintFactsJson();
    result.policyThreatCategory = violation.getThreatCategory().toString();
    return result;
  }

  private List<PolicyThreats.PolicyAction> toPolicyThreatsPolicyActions(PolicyViolation violation) {
    List<PolicyThreats.PolicyAction> result = new ArrayList<>();

    if (violation.getActionTypeId() != null) {
      PolicyThreats.PolicyAction action = new PolicyThreats.PolicyAction();
      action.actionType = violation.getActionTypeId();
      action.actionSummary = ActionTypes.getById(violation.getActionTypeId()).getSummary();
      result.add(action);
    }

    return result;
  }

  public List<PolicyThreats.PolicyConstraint> toPolicyThreatsPolicyConstraints(List<ConstraintFact> constraintFacts) {
    List<PolicyThreats.PolicyConstraint> result = new ArrayList<>();
    for (ConstraintFact fact : constraintFacts) {
      PolicyThreats.PolicyConstraint constraint = new PolicyThreats.PolicyConstraint();
      constraint.constraintId = fact.getConstraintId();
      constraint.constraintName = fact.getConstraintName();
      constraint.constraintOperator = fact.getOperatorName();
      constraint.conditions.addAll(toPolicyThreatsPolicyConditions(fact));
      result.add(constraint);
    }
    return result;
  }

  private List<PolicyThreats.PolicyCondition> toPolicyThreatsPolicyConditions(ConstraintFact fact) {
    List<PolicyThreats.PolicyCondition> result = new ArrayList<>();
    for (ConditionFact conditionFact : fact.getConditionFacts()) {
      TriggerReference conditionReference = conditionFact.getReference();

      PolicyThreats.PolicyCondition condition = new PolicyThreats.PolicyCondition();
      condition.conditionReason = conditionFact.getReason();
      condition.conditionSummary = conditionFact.getSummary();
      condition.conditionType = conditionFact.getConditionTypeId();

      if (conditionReference != null) {
        condition.conditionTriggerReference = new PolicyThreats.PolicyConditionTriggerReference();
        condition.conditionTriggerReference.value = conditionReference.getValue();
        condition.conditionTriggerReference.type = conditionReference.getType();
      }

      result.add(condition);
    }
    return result;
  }
}
