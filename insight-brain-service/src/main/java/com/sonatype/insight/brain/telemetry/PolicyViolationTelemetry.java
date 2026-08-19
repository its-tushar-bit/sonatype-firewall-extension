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
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.policy.utils.ConstraintFactsUtil;
import com.sonatype.insight.brain.policy.utils.ConstraintFactsUtil.CveData;

/**
 * Telemetry data for policy violations on repository components.
 * <p>
 * Captures policy violation details including action type, threat information, CVE data,
 * and malware classifications for components evaluated against policies. Used for both
 * Firewall quarantine/audit operations and Lifecycle policy evaluations.
 * </p>
 */
public class PolicyViolationTelemetry
{
  /**
   * The action type ID from the policy (e.g., "FAIL" for quarantine, "WARN" for warning).
   *
   * <p>
   * <b>Note:</b> This field can be null for Firewall policies that have only NOTIFY actions.
   * Firewall intentionally excludes NOTIFY-only actions as they don't represent quarantine or
   * warning behavior for repository components. This is expected and valid behavior.
   * </p>
   */
  private final String actionTypeId;

  private final Integer threatLevel;

  private final String threatCategory;

  private final List<ConstraintTelemetry> constraints = new ArrayList<>();

  // CVE and malware related fields
  private String cveNumber;

  private Object cvssScore;

  private String vulnerabilityCategory; // CVE category (e.g., "ARBITRARY_CODE_EXECUTION")

  private String malwareSeverity; // Malware severity (e.g., "Malicious", "Moderate")

  private String malwareAttackVector; // Malware attack vector (e.g., "Trojan", "Brandjack", "Hijack")

  private String cvssAttackVector; // CVSS attack vector (e.g., "Network", "Adjacent", "Local", "Physical")

  // Threat type classifications (e.g., ["secrets_exfiltration", "backdoor", "crypto_miner"])
  private List<String> threatTypes;

  // Policy name field
  private String policyName;

  public PolicyViolationTelemetry(final AbstractPolicyViolation policyViolation) {
    this(policyViolation.getConstraintFacts(), policyViolation.getActionTypeId(),
        policyViolation.getThreatCategory().getName(), policyViolation.getThreatLevel(),
        policyViolation.getPolicyName());
  }

  public PolicyViolationTelemetry(final List<ConstraintFact> constraintFacts) {
    this(constraintFacts, null, null, null, null);
  }

  PolicyViolationTelemetry(
      final List<ConstraintFact> constraintFacts,
      final String actionTypeId,
      final String threatCategory,
      final Integer threatLevel)
  {
    this(constraintFacts, actionTypeId, threatCategory, threatLevel, null);
  }

  PolicyViolationTelemetry(
      final List<ConstraintFact> constraintFacts,
      final String actionTypeId,
      final String threatCategory,
      final Integer threatLevel,
      final String policyName)
  {
    this.actionTypeId = actionTypeId;
    this.threatCategory = threatCategory;
    this.threatLevel = threatLevel;
    this.policyName = policyName;

    // Extract CVE and malware data from constraint facts using ConstraintFactsUtil
    if (constraintFacts != null) {
      constraints.addAll(constraintFacts.stream().map(ConstraintTelemetry::new).collect(Collectors.toList()));

      CveData cveData = ConstraintFactsUtil.extractCveData(constraintFacts);
      this.cveNumber = cveData.cveNumber();
      this.cvssScore = cveData.cvssScore();
      this.vulnerabilityCategory = cveData.vulnerabilityCategory();
      this.malwareSeverity = cveData.malwareSeverity();
      this.malwareAttackVector = cveData.malwareAttackVector();
      this.cvssAttackVector = cveData.cvssAttackVector();
      this.threatTypes = cveData.threatTypes();
    }
  }

  /**
   * Factory method to create PolicyViolationTelemetry from policy violation and component.
   * Extracts CVE data from constraint facts and enriches missing fields from Component data.
   * Also extracts threat types from Component's SecurityVulnerability list.
   *
   * @param policyViolation The repository policy violation
   * @param component The component (used to enrich CVE data and extract threat types)
   * @return PolicyViolationTelemetry with CVE fields enriched and threat types populated
   */
  public static PolicyViolationTelemetry createWithComponent(
      final AbstractPolicyViolation policyViolation,
      final Component component)
  {
    // Create telemetry with data from constraint facts
    PolicyViolationTelemetry telemetry = new PolicyViolationTelemetry(policyViolation);

    // Enrich missing CVE fields and threat types from Component's SecurityVulnerability list
    if (component != null && telemetry.cveNumber != null &&
        component.getSecurityVulnerabilities() != null)
    {
      enrichCveDataFromComponent(telemetry, component);
    }

    return telemetry;
  }

  /**
   * Enriches telemetry CVE data from Component's SecurityVulnerability list.
   * Only fills in fields that are null in the telemetry.
   *
   * @param telemetry The telemetry to enrich
   * @param component The component with SecurityVulnerability data
   */
  private static void enrichCveDataFromComponent(PolicyViolationTelemetry telemetry, Component component) {
    component.getSecurityVulnerabilities()
        .stream()
        .filter(vuln -> telemetry.cveNumber.equalsIgnoreCase(vuln.getRefId()))
        .findFirst()
        .ifPresent(vuln -> {
          // Enrich cvssAttackVector if missing
          if (telemetry.cvssAttackVector == null && vuln.getVector() != null) {
            telemetry.cvssAttackVector = ConstraintFactsUtil.parseAttackVectorFromCvssVector(vuln.getVector());
          }

          // Enrich malwareAttackVector if missing
          if (telemetry.malwareAttackVector == null && vuln.getAttackVector() != null) {
            telemetry.malwareAttackVector = vuln.getAttackVector();
          }

          // Enrich malware severity if missing
          if (telemetry.malwareSeverity == null && vuln.getSeverityDescription() != null) {
            telemetry.malwareSeverity = vuln.getSeverityDescription();
          }

          // Enrich vulnerabilityCategory if missing
          if (telemetry.vulnerabilityCategory == null && vuln.getVulnerabilityCategories() != null &&
              !vuln.getVulnerabilityCategories().isEmpty())
        {
            // Use the first category's ID (uppercased to match constraint facts format)
            telemetry.vulnerabilityCategory =
                vuln.getVulnerabilityCategories().get(0).getId().toUpperCase();
          }

          // Enrich threat types if missing
          if (telemetry.threatTypes == null && vuln.getThreatTypes() != null &&
              !vuln.getThreatTypes().isEmpty())
        {
            telemetry.threatTypes = vuln.getThreatTypes();
          }
        });
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

  public String getVulnerabilityCategory() {
    return vulnerabilityCategory;
  }

  public String getMalwareSeverity() {
    return malwareSeverity;
  }

  public String getMalwareAttackVector() {
    return malwareAttackVector;
  }

  public String getCvssAttackVector() {
    return cvssAttackVector;
  }

  public List<String> getThreatTypes() {
    return threatTypes;
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
        conditions.addAll(constraintFact.getConditionFacts()
            .stream()
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
