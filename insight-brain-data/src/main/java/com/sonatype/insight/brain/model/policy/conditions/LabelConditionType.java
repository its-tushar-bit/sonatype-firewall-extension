/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.ArrayList;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.LabelValueType;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.brain.model.policy.facts.TriggerLabel;
import com.sonatype.insight.dataaccess.TransactionContext;

@Singleton
@Named
public class LabelConditionType
    extends AbstractComponentConditionType<String>
{
  public static final String ID = "Label";

  private static List<String> supportedOperators = new ArrayList<>();

  private final LabelDAO labelDAO;

  static {
    supportedOperators.add("is");
    supportedOperators.add("is not");
  }

  @Inject
  public LabelConditionType(final LabelDAO labelDAO) {
    this.labelDAO = labelDAO;
  }

  @Override
  public List<String> getSupportedOperators() {
    return supportedOperators;
  }

  @Override
  public String getValueTypeId() {
    return LabelValueType.ID;
  }

  @Override
  public void validateCondition(
      TransactionContext tx,
      Condition condition,
      String ownerId) throws InvalidConditionException
  {
    super.validateCondition(tx, condition, ownerId);

    String labelId = condition.getValue();
    LabelValueType labelValueType = new LabelValueType(tx, ownerId, labelDAO);
    for (Label label : labelValueType.getAvailableValues()) {
      if (label.getId().equals(labelId)) {
        return;
      }
    }
    throw new InvalidConditionException(condition, "Invalid label id: " + labelId);
  }

  @Override
  public boolean isAutoUnquarantineSupported() {
    return false;
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "Label";
  }

  @Override
  public String generateDroolsConditionValue(TransactionContext tx, String value) {
    Label label = labelDAO.getById(tx, value);
    return asDroolsString(value) + asDroolsComment("label: " + label.getLabel());
  }

  @Override
  public String explainCondition(final Condition condition) {
    return getName() + ' ' + condition.getOperator() + " '" + labelDAO.getById(condition.getValue()).getLabel()
        + '\'';
  }

  @Override
  public String explainMatch(final Condition condition, final MatchFact matchFact) {
    TriggerLabel conditionTrigger = (TriggerLabel) matchFact
        .getConditionTriggerByConditionIndex(condition.getConditionIndex())
        .getTrigger();
    Label label = labelDAO.getById(conditionTrigger.id);
    if ("is".equals(condition.getOperator())) {
      return "Found label '" + label.getLabel() + "'";
    }
    else {
      return "Did not find label '" + label.getLabel() + "'";
    }
  }

  @Override
  protected boolean internalEvaluateCondition(Component component, String operator, String value) {
    boolean hasLabel = component.hasLabelId(value);
    return "is".equals(operator) ? hasLabel : !hasLabel;
  }

  @Override
  public String generateDroolsTriggerCode(Condition condition, int conditionIndex) {
    return "$conditionTriggers.add(new ConditionTrigger(" + conditionIndex + ", new TriggerLabel("
        + asDroolsString(condition.getValue()) + ")));";
  }
}
