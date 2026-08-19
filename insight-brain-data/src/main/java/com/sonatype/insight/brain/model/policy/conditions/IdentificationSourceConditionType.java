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

import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.IdentificationSourceValueType;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.dataaccess.TransactionContext;

@Singleton
@Named
public class IdentificationSourceConditionType
    extends AbstractComponentConditionType<String>
{
  public static final String ID = "IdentificationSource";

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
    return "Identification Source";
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
    return "Identification Source was " + matchFact.getComponent().getIdentificationSource().getId()
        + ("is not".equals(condition.getOperator()) ? ", not " + condition.getValue() : "");
  }

  @Override
  public String getValueTypeId() {
    return IdentificationSourceValueType.ID;
  }

  @Override
  public void validateCondition(
      TransactionContext tx,
      Condition condition,
      String ownerId) throws InvalidConditionException
  {
    super.validateCondition(tx, condition, ownerId);

    if (IdentificationSource.getById(condition.getValue()) == null) {
      throw new InvalidConditionException(condition, "Value not supported: " + condition.getValue());
    }
  }

  @Override
  public boolean isAutoUnquarantineSupported() {
    return false;
  }

  @Override
  protected boolean internalEvaluateCondition(Component component, String operator, String value) {
    boolean result = component.getIdentificationSource().getId().equals(value);
    return "is".equals(operator) ? result : !result;
  }
}
