/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * Condition types reasoning about the component in general.
 *
 * @param <T> The type of the condition value.
 */
public abstract class AbstractComponentConditionType<T>
    extends AbstractConditionType
{
  @Override
  public final String generateDroolsConditionCode(TransactionContext tx, Condition condition) {
    return "ConditionTypes." + getClass().getSimpleName() + ".evaluateCondition(this, \"" + condition.getOperator()
        + "\", " + generateDroolsConditionValue(tx, condition.getValue()) + ")";
  }

  protected abstract boolean internalEvaluateCondition(Component component, String operator, T value);

  protected boolean isApplicable(Component component) {
    return !MatchState.UNKNOWN.equals(component.getMatchState());
  }

  public final boolean evaluateCondition(Component component, String operator, T value) {
    if (!isApplicable(component)) {
      return false;
    }
    return internalEvaluateCondition(component, operator, value);
  }
}
