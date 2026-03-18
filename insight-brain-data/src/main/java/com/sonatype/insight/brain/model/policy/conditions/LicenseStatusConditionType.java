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
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.LicenseStatusValueType;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.dataaccess.TransactionContext;

@Singleton
@Named
public class LicenseStatusConditionType
    extends AbstractComponentConditionType<String>
{
  public static final String ID = "LicenseStatus";

  private static List<String> supportedOperators = new ArrayList<>();

  static {
    supportedOperators.add("is");
    supportedOperators.add("is not");
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "License Status";
  }

  @Override
  public List<String> getSupportedOperators() {
    return supportedOperators;
  }

  @Override
  public String generateDroolsConditionValue(TransactionContext tx, String value) {
    return asDroolsString(value);
  }

  @Override
  public String explainMatch(final Condition condition, final MatchFact matchFact) {
    return "License status was " + matchFact.getComponent().getLicenseOverrideStatus().getName()
        + ("is not".equals(condition.getOperator())
            ? ", not " + LicenseOverrideStatus.valueOf(condition.getValue()).getName()
            : "");
  }

  @Override
  public String getValueTypeId() {
    return LicenseStatusValueType.ID;
  }

  @Override
  public PolicyThreatCategory getThreatCategory() {
    return PolicyThreatCategory.LICENSE;
  }

  @Override
  public void validateCondition(
      TransactionContext tx,
      Condition condition,
      String ownerId) throws InvalidConditionException
  {
    super.validateCondition(tx, condition, ownerId);

    try {
      LicenseOverrideStatus.valueOf(condition.getValue());
    }
    catch (IllegalArgumentException e) {
      throw new InvalidConditionException(condition, "Value not supported: " + condition.getValue());
    }
  }

  @Override
  public boolean isAutoUnquarantineSupported() {
    return false;
  }

  @Override
  protected boolean internalEvaluateCondition(Component component, String operator, String value) {
    boolean result = component.getLicenseOverrideStatus().getId().equals(value);
    return "is".equals(operator) ? result : !result;
  }

  @Override
  public String generateDroolsTriggerCode(Condition condition, int conditionIndex) {
    return "$conditionTriggers.add(new ConditionTrigger(" + conditionIndex + ", new TriggerLicenseStatus("
        + asDroolsString(condition.getValue()) + ")));";
  }
}
