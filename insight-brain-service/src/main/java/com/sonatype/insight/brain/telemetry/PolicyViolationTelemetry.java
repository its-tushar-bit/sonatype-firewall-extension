/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;

public class PolicyViolationTelemetry
{
  private final String actionTypeId;

  private final Integer threatLevel;

  private final String threatCategory;

  private final List<ConstraintTelemetry> constraints = new ArrayList<>();

  public PolicyViolationTelemetry(final AbstractPolicyViolation policyViolation) {
    this(policyViolation.getConstraintFacts(), policyViolation.getActionTypeId(),
        policyViolation.getThreatCategory().getName(), policyViolation.getThreatLevel());
  }

  public PolicyViolationTelemetry(final List<ConstraintFact> constraintFacts) {
    this(constraintFacts, null, null, null);
  }

  PolicyViolationTelemetry(
      final List<ConstraintFact> constraintFacts,
      final String actionTypeId,
      final String threatCategory,
      final Integer threatLevel)
  {
    this.actionTypeId = actionTypeId;
    this.threatCategory = threatCategory;
    this.threatLevel = threatLevel;
    if (constraintFacts != null) {
      constraints.addAll(constraintFacts.stream().map(ConstraintTelemetry::new).collect(Collectors.toList()));
    }
  }

  public String getActionTypeId() {
    return actionTypeId;
  }

  public Integer getThreatLevel() {
    return threatLevel;
  }

  public String getThreatCategory() {
    return threatCategory;
  }

  public List<ConstraintTelemetry> getConstraints() {
    return constraints;
  }

  public static class ConditionTelemetry
  {
    private final String conditionType;

    private final String conditionSummary;

    ConditionTelemetry(final ConditionFact conditionFact) {
      conditionSummary = conditionFact.getSummary();
      conditionType = conditionFact.getConditionTypeId();
    }

    public String getConditionSummary() {
      return conditionSummary;
    }

    public String getConditionType() {
      return conditionType;
    }
  }

  public static class ConstraintTelemetry
  {
    private final List<ConditionTelemetry> conditions = new ArrayList<>();

    private final String constraintOperator;

    ConstraintTelemetry(final ConstraintFact constraintFact) {
      constraintOperator = constraintFact.getOperatorName();
      if (constraintFact.getConditionFacts() != null) {
        conditions.addAll(
            constraintFact.getConditionFacts().stream().map(ConditionTelemetry::new).collect(Collectors.toList()));
      }
    }

    public List<ConditionTelemetry> getConditions() {
      return conditions;
    }

    public String getConstraintOperator() {
      return constraintOperator;
    }
  }
}
