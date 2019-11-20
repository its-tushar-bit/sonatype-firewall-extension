/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.LicenseValueType;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.dataaccess.TransactionContext;

public class LicenseConditionType
    extends AbstractComponentConditionType<String>
{
  public static final String ID = "License";

  private static List<String> supportedOperators = new ArrayList<>();

  static {
    supportedOperators.add("is");
    supportedOperators.add("is not");
  }

  @Override
  public List<String> getSupportedOperators() {
    return supportedOperators;
  }

  @Override
  public String getValueTypeId() {
    return LicenseValueType.ID;
  }

  @Override
  public void validateCondition(TransactionContext tx, Condition condition, String ownerId)
      throws InvalidConditionException
  {
    super.validateCondition(tx, condition, ownerId);

    String licenseId = condition.getValue();
    if (LicenseValueType.getLicenseById(licenseId) == null) {
      throw new InvalidConditionException(condition, "Invalid license id: " + licenseId);
    }
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "License";
  }

  @Override
  public String generateDroolsConditionValue(TransactionContext tx, String value) {
    return asDroolsString(value);
  }

  @Override
  public String explainCondition(final Condition condition) {
    return getName() + ' ' + condition.getOperator() + " '"
        + new LicenseDAO().getById(condition.getValue()).getShortDisplayName() + '\'';
  }

  @Override
  public String explainMatch(final Condition condition, final MatchFact matchFact) {
    License license = new LicenseDAO().getById(condition.getValue());
    String licenseName = license != null ? license.getShortDisplayName() : condition.getValue();

    return ("is".equals(condition.getOperator()) ? "Found" : "Did not find") + " '" + licenseName + "' license";
  }

  @Override
  protected boolean internalEvaluateCondition(Component component, String operator, String value) {
    boolean hasLicense = component.hasLicenseId(value);
    return "is".equals(operator) ? hasLicense : !hasLicense;
  }

  @Override
  public PolicyThreatCategory getThreatCategory() {
    return PolicyThreatCategory.LICENSE;
  }

  @Override
  public String generateDroolsTriggerCode(Condition condition, int conditionIndex) {
    return "$conditionTriggers.add(new ConditionTrigger(" + conditionIndex + ", new TriggerLicense("
        + asDroolsString(condition.getValue()) + ")));";
  }
}
