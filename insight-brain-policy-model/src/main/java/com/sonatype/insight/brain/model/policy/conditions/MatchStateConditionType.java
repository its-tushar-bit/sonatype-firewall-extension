/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.MatchStateValueType;

public class MatchStateConditionType
    extends AbstractConditionType<String>
{
  public static final String ID = "MatchState";

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
    return "Match State";
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
    return "Match State was " + component.getMatchState().getId();
  }

  @Override
  public String getValueTypeId() {
    return MatchStateValueType.ID;
  }

  @Override
  public void validateCondition(Condition condition, String ownerId) throws InvalidConditionException {
    super.validateCondition(condition, ownerId);

    if (MatchState.getById(condition.getValue()) == null) {
      throw new InvalidConditionException(condition, "Value not supported: " + condition.getValue());
    }
  }

  @Override
  protected boolean internalEvaluateCondition(Component component, String operator, String value) {
    boolean result = component.getMatchState().getId().equals(value);
    return "is".equals(operator) ? result : !result;
  }
}
