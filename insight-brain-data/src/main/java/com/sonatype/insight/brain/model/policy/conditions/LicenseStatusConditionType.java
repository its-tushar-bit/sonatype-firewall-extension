/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.LicenseStatusValueType;

public class LicenseStatusConditionType
    extends AbstractConditionType<String>
{
  public static final String ID = "LicenseStatus";

  private static List<String> supportedOperators = new ArrayList<String>();

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
  public String generateDroolsConditionValue(String value) {
    return "\"" + value + "\"";
  }

  @Override
  public String explainMatch(final Condition condition, final Component component) {
    return "License Status was " + component.getLicenseOverrideStatus().getId();
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
  public void validateCondition(Condition condition, String ownerId) throws InvalidConditionException {
    super.validateCondition(condition, ownerId);

    try {
      LicenseOverrideStatus.valueOf(condition.getValue());
    }
    catch (IllegalArgumentException e) {
      throw new InvalidConditionException(condition, "Value not supported: " + condition.getValue());
    }
  }

  @Override
  protected boolean internalEvaluateCondition(Component component, String operator, String value) {
    boolean result = component.getLicenseOverrideStatus().getId().equals(value);
    return "is".equals(operator) ? result : !result;
  }
}
