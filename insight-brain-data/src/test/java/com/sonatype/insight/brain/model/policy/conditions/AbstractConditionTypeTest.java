/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.List;

import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractConditionTypeTest
{
  @Test
  public void testConvertIfNeeded_ReturnsPassedValue() {
    final AbstractConditionType abstractConditionType = new AbstractConditionType()
    {
      @Override
      protected String generateDroolsConditionValue(final TransactionContext tx, final String value) {
        return null;
      }

      @Override
      public String getId() {
        return null;
      }

      @Override
      public String getName() {
        return null;
      }

      @Override
      public List<String> getSupportedOperators() {
        return null;
      }

      @Override
      public String getValueTypeId() {
        return null;
      }

      @Override
      public String generateDroolsConditionCode(final TransactionContext tx, final Condition condition) {
        return null;
      }

      @Override
      public String explainMatch(final Condition condition, final MatchFact matchFact) {
        return null;
      }

      @Override
      public boolean isAutoUnquarantineSupported() {
        return false;
      }
    };
    assertThat(abstractConditionType.convertIfNeeded("value")).isEqualTo("value");
  }
}
