/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.ArrayList;
import java.util.List;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.IntegerValueType;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.brain.model.policy.facts.TriggerLicenseThreatGroupWithThreatLevel;
import com.sonatype.insight.brain.policy.DroolsGenerator;
import com.sonatype.insight.dataaccess.TransactionContext;

@Singleton
@Named
public class LicenseThreatGroupLevelConditionType
    extends AbstractLicenseThreatGroupConditionType<Integer>
{
  public static final String ID = "License Threat Group Level";

  private static List<String> supportedOperators = new ArrayList<>();

  static {
    supportedOperators.add("<=");
    supportedOperators.add(">=");
  }

  @Override
  public List<String> getSupportedOperators() {
    return supportedOperators;
  }

  @Override
  public String getValueTypeId() {
    return IntegerValueType.ID;
  }

  @Override
  public void validateCondition(TransactionContext tx, Condition condition, String ownerId)
      throws InvalidConditionException
  {
    super.validateCondition(tx, condition, ownerId);

    try {
      int value = Integer.parseInt(condition.getValue());
      if (value < 0 || value > 10) {
        throw new InvalidConditionException(condition, "The license threat group level must be between 0 and 10");
      }
    }
    catch (NumberFormatException e) {
      throw new InvalidConditionException(condition, "Invalid license threat group level: " + condition.getValue());
    }
  }

  @Override
  public boolean isAutoUnquarantineSupported() {
    return false;
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "License Threat Group Level";
  }

  @Override
  public String generateDroolsConditionValue(TransactionContext tx, String value) {
    return asDroolsInteger(value);
  }

  @Override
  public String explainMatch(final Condition condition, final MatchFact matchFact) {
    TriggerLicenseThreatGroupWithThreatLevel conditionTrigger = (TriggerLicenseThreatGroupWithThreatLevel) matchFact
        .getConditionTriggerByConditionIndex(condition.getConditionIndex()).getTrigger();
    LicenseThreatGroup licenseThreatGroup = getLicenseThreatGroupById(matchFact.getComponent(), conditionTrigger.id);
    return "Found license threat group '" + licenseThreatGroup.getName() + "' with level "
        + condition.getOperator() + " " + condition.getValue() + " (level = " + conditionTrigger.threatLevel + ")";
  }

  @Override
  public boolean evaluateCondition(
      Component component,
      LicenseThreatGroup licenseThreatGroup,
      String operator,
      Integer licenseThreatGroupLevel)
  {
    if (">=".equals(operator)) {
      return licenseThreatGroup.getThreatLevel() >= licenseThreatGroupLevel;
    }
    if ("<=".equals(operator)) {
      return licenseThreatGroup.getThreatLevel() <= licenseThreatGroupLevel;
    }
    throw new IllegalArgumentException("Unsupported condition operator:" + operator);
  }

  private LicenseThreatGroup getLicenseThreatGroupById(Component component, String licenseThreatGroupId) {
    return component.getLicenseThreatGroups().stream().filter(ltg -> ltg.getId().equals(licenseThreatGroupId))
        .findFirst().get();
  }

  @Override
  public PolicyThreatCategory getThreatCategory() {
    return PolicyThreatCategory.LICENSE;
  }

  @Override
  public String generateDroolsTriggerCode(Condition condition, int conditionIndex) {
    return "$conditionTriggers.add(new ConditionTrigger(" + conditionIndex
        + ", new TriggerLicenseThreatGroupWithThreatLevel(" + DroolsGenerator.LICENSE_THREAT_GROUP_VARIABLE + ")));";
  }
}
