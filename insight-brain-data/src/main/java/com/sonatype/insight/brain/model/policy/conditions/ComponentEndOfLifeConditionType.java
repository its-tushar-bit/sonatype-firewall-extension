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

import com.sonatype.clm.dto.model.ComponentEndOfLifeStatus;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.dataaccess.TransactionContext;

@Singleton
@Named
public class ComponentEndOfLifeConditionType
    extends AbstractComponentConditionType<String>
{
  public static final String ID = "ComponentEndOfLife";

  private static List<String> supportedOperators = new ArrayList<>();

  static {
    supportedOperators.add("is true");
    supportedOperators.add("is false");
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "End of Life";
  }

  @Override
  public List<String> getSupportedOperators() {
    return supportedOperators;
  }

  @Override
  public String generateDroolsConditionValue(TransactionContext tx, String value) {
    return null;
  }

  @Override
  public String explainMatch(final Condition condition, final MatchFact matchFact) {
    boolean isEndOfLife = ComponentEndOfLifeStatus.END_OF_LIFE_TRUE.equals(matchFact.getComponent().getEndOfLife());

    if (isEndOfLife) {
      return "Component status is End-of-Life (EOL)";
    }
    else {
      return "Component status is not End-of-Life (EOL)";
    }
  }

  @Override
  public boolean isAutoUnquarantineSupported() {
    return false;
  }

  @Override
  public String getValueTypeId() {
    return null;
  }

  @Override
  protected boolean internalEvaluateCondition(Component component, String operator, String value) {
    boolean isTrue = ComponentEndOfLifeStatus.END_OF_LIFE_TRUE.equals(component.getEndOfLife());
    return "is true".equals(operator) == isTrue;
  }

  @Override
  public PolicyThreatCategory getThreatCategory() {
    return PolicyThreatCategory.QUALITY;
  }
}
