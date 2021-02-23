/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.dataaccess.TransactionContext;

public class ProprietaryNameConflictConditionType
    extends AbstractComponentConditionType<String>
{
  public static final String ID = "ProprietaryNameConflict";

  public static final String OP_IS_PRESENT = "is present";

  public static final String OP_IS_NOT_PRESENT = "is not present";

  private static List<String> supportedOperators =
      Collections.unmodifiableList(Arrays.asList(OP_IS_PRESENT, OP_IS_NOT_PRESENT));

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "Proprietary Name Conflict";
  }

  @Override
  public PolicyThreatCategory getThreatCategory() {
    return PolicyThreatCategory.SECURITY;
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
    String conflict = matchFact.getComponent().getConflictingProprietaryName();
    if (conflict.isEmpty()) {
      return "Component name does not conflict with any proprietary component";
    }
    else {
      return "Component name conflicts with proprietary component " + conflict;
    }
  }

  @Override
  public String getValueTypeId() {
    return null;
  }

  @Override
  protected boolean isApplicable(Component component) {
    return component.getConflictingProprietaryName() != null;
  }

  @Override
  protected boolean internalEvaluateCondition(Component component, String operator, String value) {
    boolean conflictPresent = !component.getConflictingProprietaryName().isEmpty();
    return OP_IS_PRESENT.equals(operator) ? conflictPresent : !conflictPresent;
  }
}
