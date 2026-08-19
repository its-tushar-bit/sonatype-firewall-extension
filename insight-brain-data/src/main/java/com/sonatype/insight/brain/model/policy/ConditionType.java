/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.List;

import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
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

  /**
   * Generates Drools code for this condition. The generated code is included in the "when" part of the Drools rule (aka
   * the left-hand-side).
   */
  String generateDroolsConditionCode(TransactionContext tx, Condition condition);

  /**
   * Generates Drools code that adds trigger data to the Drools results. The generated code is included in the "then"
   * part of the Drools rule (aka the right-hand-side).
   *
   * Not all condition types report back trigger data.
   *
   * @param conditionIndex The condition index in the policy constraint.
   *
   * @since 1.50
   */
  default String generateDroolsTriggerCode(@SuppressWarnings("unused") Condition condition, int conditionIndex) {
    return null;
  }

  String explainCondition(Condition condition);

  String explainMatch(Condition condition, MatchFact matchFact);

  /**
   * @since 1.67
   */
  TriggerReference getTriggerReference(Condition condition, MatchFact matchFact);

  void validateCondition(TransactionContext tx, Condition condition, String ownerId) throws InvalidConditionException;

  /**
   * @since 1.32.0
   */
  String convertIfNeeded(String value);

  /**
   * @since 1.87
   */
  void setEnabled(boolean enabled);

  /**
   * @since 1.87
   */
  boolean isEnabled();

  /**
   * @since 1.107
   */
  boolean isAutoUnquarantineSupported();
}
