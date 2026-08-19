/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.KevStatusValueType;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.brain.model.policy.facts.TriggerSecurityVulnerabilityWithKev;
import com.sonatype.insight.brain.model.vulnerability.KevStatus;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
@Named
public class KevStatusConditionType
    extends AbstractVulnerabilityConditionType<String>
{
  public static final String ID = "KevStatus";

  private static List<String> supportedOperators = new ArrayList<>();

  static {
    supportedOperators.add("is");
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "KEV Status";
  }

  @Override
  public List<String> getSupportedOperators() {
    return supportedOperators;
  }

  @Override
  public String generateDroolsConditionValue(final TransactionContext tx, final String value) {
    return asDroolsString(value);
  }

  @Override
  public String explainMatch(final Condition condition, final MatchFact matchFact) {
    TriggerSecurityVulnerabilityWithKev conditionTrigger = (TriggerSecurityVulnerabilityWithKev) matchFact
        .getConditionTriggerByConditionIndex(condition.getConditionIndex())
        .getTrigger();
    if (conditionTrigger.isKev) {
      return "Vulnerability " + conditionTrigger.refId
          + " listed in the Known Exploited Vulnerabilities (KEV) database.";
    }
    else {
      return "Vulnerability " + conditionTrigger.refId
          + " not listed in the Known Exploited Vulnerabilities (KEV) database.";
    }
  }

  @Override
  public String getValueTypeId() {
    return KevStatusValueType.ID;
  }

  @Override
  public String explainCondition(final Condition condition) {
    return getName() + " " + condition.getOperator() + " " +
        KevStatus.getById(condition.getValue()).getName();
  }

  @Override
  public void validateCondition(
      TransactionContext tx,
      Condition condition,
      String ownerId) throws InvalidConditionException
  {
    super.validateCondition(tx, condition, ownerId);

    if (KevStatus.getById(condition.getValue()) == null) {
      throw new InvalidConditionException(condition, "Value not supported: " + condition.getValue());
    }
  }

  @Override
  public boolean isAutoUnquarantineSupported() {
    return false;
  }

  @Override
  public PolicyThreatCategory getThreatCategory() {
    return PolicyThreatCategory.SECURITY;
  }

  @Override
  public boolean evaluateCondition(
      Component component,
      SecurityVulnerability vulnerability,
      String operator,
      String value)
  {
    if (vulnerability.getKevData() == null || vulnerability.getKevData().getIsKev() == null) {
      return false;
    }

    return vulnerability.getKevData().getIsKev() == KevStatus.KNOWN_TO_BE_EXPLOITED.getId().equals(value);
  }

  @Override
  public String generateDroolsTriggerCode(Condition condition, int conditionIndex) {
    return "$conditionTriggers.add(new ConditionTrigger(" + conditionIndex
        + ", new TriggerSecurityVulnerabilityWithKev($securityVulnerability)));";
  }
}
