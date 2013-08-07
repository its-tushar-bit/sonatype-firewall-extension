/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Condition;

public class ProprietaryConditionType
    extends AbstractConditionType<String>
{
  public static final String ID = "Proprietary";

  private static List<String> supportedOperators = new ArrayList<String>();

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
    return "Proprietary";
  }

  @Override
  public List<String> getSupportedOperators() {
    return supportedOperators;
  }

  @Override
  public String generateDroolsConditionValue(String value) {
    return null;
  }

  @Override
  public String explainMatch(final Condition condition, final Component component) {
    if (component.isProprietary()) {
      return "Component contains proprietary packages";
    }
    else {
      return "Component does not contain proprietary packages";
    }
  }

  @Override
  public String getValueTypeId() {
    return null;
  }

  @Override
  protected boolean internalEvaluateCondition(Component component, String operator, String value) {
    return "is true".equals(operator) ? component.isProprietary() : !component.isProprietary();
  }
}
