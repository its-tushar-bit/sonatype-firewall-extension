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
import com.sonatype.insight.brain.model.component.IntegrityRating;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.IntegrityRatingValueType;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.99
 */
@Singleton
@Named
public class IntegrityRatingConditionType
    extends AbstractComponentConditionType<String>
{
  public static final String ID = "IntegrityRating";

  private static final List<String> supportedOperators = new ArrayList<>();

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
    return "Integrity Rating";
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
  public String explainCondition(final Condition condition) {
    return getName() + ' ' + condition.getOperator() + ' ' + IntegrityRating.getById(condition.getValue()).getName();
  }

  @Override
  public String explainMatch(final Condition condition, final MatchFact matchFact) {
    return "Integrity Rating was " + matchFact.getComponent().getIntegrityRating().getName()
        + ("is not".equals(condition.getOperator()) ?
        ", not " + IntegrityRating.getById(condition.getValue()).getName() : "");
  }

  @Override
  public String getValueTypeId() {
    return IntegrityRatingValueType.ID;
  }

  @Override
  public void validateCondition(TransactionContext tx, Condition condition, String ownerId)
      throws InvalidConditionException
  {
    super.validateCondition(tx, condition, ownerId);

    if (IntegrityRating.getById(condition.getValue()) == null) {
      throw new InvalidConditionException(condition, "Value not supported: " + condition.getValue());
    }
  }

  @Override
  protected boolean internalEvaluateCondition(Component component, String operator, String value) {
    if (component.getIntegrityRating() == null) {
      return false;
    }
    boolean result = component.getIntegrityRating().getId().equals(value);
    return "is".equals(operator) ? result : !result;
  }

  @Override
  public PolicyThreatCategory getThreatCategory() {
    return PolicyThreatCategory.QUALITY;
  }

  @Override
  public boolean isAutoUnquarantineSupported() {
    return true;
  }
}
