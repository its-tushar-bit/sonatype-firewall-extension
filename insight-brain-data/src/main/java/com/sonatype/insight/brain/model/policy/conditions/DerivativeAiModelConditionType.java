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

import com.sonatype.clm.dto.model.DerivedFromAiModel;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.dataaccess.TransactionContext;

@Singleton
@Named
public class DerivativeAiModelConditionType
    extends AbstractComponentConditionType<String>
{
  public static final String ID = "DerivativeAiModel";

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
    return "Derivative AI Model";
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
  public String explainMatch(Condition condition, MatchFact matchFact) {
    if (isDerivativeAiModel(matchFact.getComponent())) {
      DerivedFromAiModel derivedFromAiModel = matchFact.getComponent().getDerivedFromAiModel();
      return "AI model is derived from "
          + ComponentDisplayNameUtil.fromIdentifier(derivedFromAiModel.getComponentIdentifier());
    }
    else {
      return "AI model is not derived from another AI model";
    }
  }

  private boolean isDerivativeAiModel(Component component) {
    DerivedFromAiModel derivedFromAiModel = component.getDerivedFromAiModel();
    return derivedFromAiModel != null
        && !derivedFromAiModel.getComponentIdentifier().equals(component.getComponentIdentifier());
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
    boolean isTrue = isDerivativeAiModel(component);
    return "is true".equals(operator) == isTrue;
  }

  @Override
  public PolicyThreatCategory getThreatCategory() {
    return PolicyThreatCategory.QUALITY;
  }

  @Override
  public String generateDroolsTriggerCode(Condition condition, int conditionIndex) {
    return "$conditionTriggers.add(new ConditionTrigger(" + conditionIndex
        + ", new TriggerDerivedFromAiModel($component.getDerivedFromAiModel())));";
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
