/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.policy.utils.ConstraintFactsUtil;
import com.sonatype.insight.brain.policy.utils.ConstraintFactsUtil.CveData;
import com.sonatype.insight.purl.PackageUrlIdentifier;

public class PolicyViolationTelemetry
{
  private final String actionTypeId;

  private final Integer threatLevel;

  private final String threatCategory;

  private final List<ConstraintTelemetry> constraints = new ArrayList<>();

  // New CVE-related fields
  private String cveNumber;

  private Object cvssScore;

  private String componentName;

  private String attackVector;

  private String vulnerabilityCategory;

  // Component namespace field
  private String namespace;

  // Policy name field
  private String policyName;

  public PolicyViolationTelemetry(final AbstractPolicyViolation policyViolation) {
    this(policyViolation.getConstraintFacts(), policyViolation.getActionTypeId(),
        policyViolation.getThreatCategory().getName(), policyViolation.getThreatLevel(),
        policyViolation.getComponentIdentifier(), policyViolation.getPolicyName());
  }

  public PolicyViolationTelemetry(final List<ConstraintFact> constraintFacts) {
    this(constraintFacts, null, null, null, null, null);
  }

  PolicyViolationTelemetry(
      final List<ConstraintFact> constraintFacts,
      final String actionTypeId,
      final String threatCategory,
      final Integer threatLevel)
  {
    this(constraintFacts, actionTypeId, threatCategory, threatLevel, null, null);
  }

  PolicyViolationTelemetry(
      final List<ConstraintFact> constraintFacts,
      final String actionTypeId,
      final String threatCategory,
      final Integer threatLevel,
      final ComponentIdentifier componentIdentifier)
  {
    this(constraintFacts, actionTypeId, threatCategory, threatLevel, componentIdentifier, null);
  }

  PolicyViolationTelemetry(
      final List<ConstraintFact> constraintFacts,
      final String actionTypeId,
      final String threatCategory,
      final Integer threatLevel,
      final ComponentIdentifier componentIdentifier,
      final String policyName)
  {
    this.actionTypeId = actionTypeId;
    this.threatCategory = threatCategory;
    this.threatLevel = threatLevel;
    this.policyName = policyName;

    // Extract component name and namespace from ComponentIdentifier
    if (componentIdentifier != null) {
      PackageUrlIdentifier purl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);
      this.componentName = purl.getName();
      this.namespace = purl.getNamespace();
    }

    // Extract CVE data from constraint facts using ConstraintFactsUtil
    if (constraintFacts != null) {
      constraints.addAll(constraintFacts.stream().map(ConstraintTelemetry::new).collect(Collectors.toList()));

      CveData cveData = ConstraintFactsUtil.extractCveData(constraintFacts);
      this.cveNumber = cveData.cveNumber();
      this.cvssScore = cveData.cvssScore();
      this.vulnerabilityCategory = cveData.vulnerabilityCategory();
      this.attackVector = cveData.attackVector();
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

  public String getCveNumber() {
    return cveNumber;
  }

  public Object getCvssScore() {
    return cvssScore;
  }

  public String getComponentName() {
    return componentName;
  }

  public String getAttackVector() {
    return attackVector;
  }

  public String getVulnerabilityCategory() {
    return vulnerabilityCategory;
  }

  public String getNamespace() {
    return namespace;
  }

  public String getPolicyName() {
    return policyName;
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
        conditions.addAll(constraintFact.getConditionFacts().stream()
            .map(ConditionTelemetry::new)
            .collect(Collectors.toList()));
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
