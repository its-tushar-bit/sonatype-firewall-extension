/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.List;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.dataaccess.TransactionContext;

public interface ConditionType
{
  String getId();

  String getName();

  List<String> getSupportedOperators();

  /**
   * @return The ID of a ConditionValueType that defines the value type for this condition type or null if the
   *         condition type does not require or support values.
   */
  String getValueTypeId();

  String getValueHint();

  PolicyThreatCategory getThreatCategory();

  String generateDroolsCode(TransactionContext tx, Condition condition);

  String explainCondition(Condition condition);

  String explainMatch(Condition condition, Component component);

  void validateCondition(TransactionContext tx, Condition condition, String ownerId) throws InvalidConditionException;

  /**
   * @since 1.32.0
   */
  String convertIfNeeded(String value);
}
