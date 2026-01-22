/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ComponentCategoryDAO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.ComponentCategory;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.ComponentCategoryValueType;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.85
 */
@Singleton
@Named
public class ComponentCategoryConditionType
    extends AbstractComponentConditionType<String>
{
  public static final String ID = "ComponentCategory";

  private static List<String> supportedOperators = new ArrayList<>();

  static {
    supportedOperators.add("is");
    supportedOperators.add("is not");
  }

  private final ComponentCategoryDAO componentCategoryDAO;

  @Inject
  public ComponentCategoryConditionType(final ComponentCategoryDAO componentCategoryDAO) {
    this.componentCategoryDAO = componentCategoryDAO;
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "Component Category";
  }

  @Override
  public List<String> getSupportedOperators() {
    return supportedOperators;
  }

  @Override
  public String generateDroolsConditionValue(TransactionContext tx, String value) {
    return asDroolsString(value);
  }

  @Override
  public String explainCondition(final Condition condition) {
    return getName() + ' ' + condition.getOperator() + ' ' +
        componentCategoryDAO.getById(condition.getValue()).getName();
  }

  @Override
  public String explainMatch(final Condition condition, final MatchFact matchFact) {
    String categoriesFromMatchFact =
        matchFact.getComponent().getComponentCategories().stream().map(ComponentCategory::getPath)
            .collect(Collectors.joining(", "));
    return "Component Category was " + categoriesFromMatchFact + ("is not".equals(condition.getOperator()) ?
        ", not " + componentCategoryDAO.getById(condition.getValue()).getPath() : "");
  }

  @Override
  public String getValueTypeId() {
    return ComponentCategoryValueType.ID;
  }

  @Override
  public void validateCondition(TransactionContext tx, Condition condition, String ownerId)
      throws InvalidConditionException
  {
    super.validateCondition(tx, condition, ownerId);

    if (componentCategoryDAO.getById(condition.getValue()) == null) {
      throw new InvalidConditionException(condition, "Value not supported: " + condition.getValue());
    }
  }

  @Override
  public boolean isAutoUnquarantineSupported() {
    return false;
  }

  @Override
  protected boolean internalEvaluateCondition(Component component, String operator, String value) {
    if (component.getComponentCategories() == null) {
      return false;
    }
    List<ComponentCategory> children = componentCategoryDAO.getChildren(value);
    boolean result = component.getComponentCategories().stream()
        .anyMatch(componentCategory -> value.equals(componentCategory.getId()) || children.contains(componentCategory));
    return "is".equals(operator) ? result : !result;
  }
}
