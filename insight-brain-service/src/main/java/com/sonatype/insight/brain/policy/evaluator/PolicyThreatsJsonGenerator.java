/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.model.policy.actions.ActionTypes;
import com.sonatype.insight.json.store.JsonUtils;

/**
 * Generates the content of the policythreats.json file included in CLM reports.
 */
class PolicyThreatsJsonGenerator
{
  static byte[] generate(PolicyResults policyResults) throws IOException {
    return JsonUtils.generate(toPolicyThreats(policyResults));
  }

  static PolicyThreats toPolicyThreats(final PolicyResults policyResults) {
    final Map<String, PolicyThreats.Component> components = new LinkedHashMap<>();
    processAlerts(components, policyResults.getActiveAlerts(), false);
    processAlerts(components, policyResults.getWaivedAlerts(), true);
    for (PolicyThreats.Component component : components.values()) {
      if (component.policyThreatLevel < 0) {
        component.policyThreatLevel = 0;
        component.policyName = "None";
      }
    }
    PolicyThreats policyThreats = new PolicyThreats();
    policyThreats.version = 1;
    policyThreats.aaData = new ArrayList<>(components.values());
    return policyThreats;
  }

  private static void processAlerts(Map<String, PolicyThreats.Component> components, List<PolicyAlert> alerts,
      boolean waived)
  {
    for (final PolicyAlert alert : alerts) {
      final PolicyFact trigger = alert.getTrigger();
      final int threatLevel = trigger.getThreatLevel();
      for (final ComponentFact componentFact : trigger.getComponentFacts()) {
        final String id = componentFact.getComponentId();
        PolicyThreats.Component component = components.get(id);
        if (component == null) {
          component = new PolicyThreats.Component();
          component.hash = componentFact.getHash();
          ComponentIdentifier componentIdentifier = componentFact.getComponentIdentifier();
          // TODO Fix the maven specific code as part of CLM-3709
          if (componentIdentifier != null && componentIdentifier.isMaven()) {
            component.groupId = componentIdentifier.get(ComponentIdentifier.MAVEN_GROUP_ID);
            component.artifactId = componentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID);
            component.version = componentIdentifier.get(ComponentIdentifier.VERSION);
          }
          component.policyThreatLevel = -1;
          components.put(id, component);
        }
        PolicyThreats.PolicyViolation violation = toPolicyViolation(alert, componentFact);
        if (!waived) {
          component.activeViolations.add(violation);
          if (threatLevel > component.policyThreatLevel) {
            component.policyId = trigger.getPolicyId();
            component.policyName = trigger.getPolicyName();
            component.policyThreatLevel = threatLevel;
          }
        }
        else {
          component.waivedViolations.add(violation);
        }
      }
    }
  }

  private static PolicyThreats.PolicyViolation toPolicyViolation(PolicyAlert alert, ComponentFact componentFact) {
    PolicyThreats.PolicyViolation violation = new PolicyThreats.PolicyViolation();
    violation.policyId = alert.getTrigger().getPolicyId();
    violation.policyName = alert.getTrigger().getPolicyName();
    violation.policyThreatLevel = alert.getTrigger().getThreatLevel();
    for (Action action : alert.getActions()) {
      PolicyThreats.PolicyAction act = new PolicyThreats.PolicyAction();
      act.actionType = action.getActionTypeId();
      act.actionSummary = ActionTypes.getById(action.getActionTypeId()).getSummary();
      violation.actions.add(act);
    }
    for (ConstraintFact constraintFact : componentFact.getConstraintFacts()) {
      PolicyThreats.PolicyConstraint constraint = new PolicyThreats.PolicyConstraint();
      constraint.constraintId = constraintFact.getConstraintId();
      constraint.constraintName = constraintFact.getConstraintName();
      constraint.constraintOperator = constraintFact.getOperatorName();
      for (ConditionFact conditionFact : constraintFact.getConditionFacts()) {
        PolicyThreats.PolicyCondition condition = new PolicyThreats.PolicyCondition();
        condition.conditionType = conditionFact.getConditionTypeId();
        condition.conditionSummary = conditionFact.getSummary();
        condition.conditionReason = conditionFact.getReason();
        constraint.conditions.add(condition);
      }
      violation.constraints.add(constraint);
    }
    return violation;
  }

}
