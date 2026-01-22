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

import com.sonatype.clm.dto.model.component.AiModelContentType;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.AiModelContentValueType;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.dataaccess.TransactionContext;

@Singleton
@Named
public class AiModelContentConditionType
    extends AbstractComponentConditionType<String>
{
  public static final String ID = "AiModelContent";

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
    return "AI Content";
  }

  @Override
  public List<String> getSupportedOperators() {
    return supportedOperators;
  }

  @Override
  public String generateDroolsConditionValue(TransactionContext tx, String value) {
    return '"' + value + '"';
  }

  @Override
  public String explainMatch(Condition condition, MatchFact matchFact) {
    return "AI model " + condition.getOperator() + " " + AiModelContentType.getById(condition.getValue()).getName();
  }

  @Override
  public boolean isAutoUnquarantineSupported() {
    return false;
  }

  @Override
  public String getValueTypeId() {
    return AiModelContentValueType.ID;
  }

  @Override
  protected boolean internalEvaluateCondition(Component component, String operator, String value) {
    AiModelContentType aiModelContentType = AiModelContentType.getById(value);
    if (component.getAiModelContentTypes().contains(aiModelContentType)) {
      return "is".equals(operator);
    }
    else {
      return "is not".equals(operator);
    }
  }

  @Override
  public PolicyThreatCategory getThreatCategory() {
    return PolicyThreatCategory.QUALITY;
  }

  @Override
  public String generateDroolsTriggerCode(Condition condition, int conditionIndex) {
    return "$conditionTriggers.add(new ConditionTrigger(" + conditionIndex
        + ", new TriggerAiModelContentType(\"" + condition.getValue() + "\")));";
  }

  @Override
  protected boolean isApplicable(Component component) {
    if (!super.isApplicable(component)) {
      return false;
    }

    ComponentIdentifier componentIdentifier = component.getComponentIdentifier();
    return componentIdentifier != null
        && ComponentIdentifier.FORMAT_HUGGINGFACE_MODEL.equals(componentIdentifier.getFormat());
  }
}
