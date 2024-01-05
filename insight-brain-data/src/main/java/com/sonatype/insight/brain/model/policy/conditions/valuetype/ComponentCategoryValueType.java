/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions.valuetype;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.ComponentCategoryDAO;
import com.sonatype.insight.brain.model.component.ComponentCategory;
import com.sonatype.insight.brain.model.policy.ConditionValueType;

public class ComponentCategoryValueType
    implements ConditionValueType<ComponentCategory>
{
  public static final String ID = "ComponentCategoryValueType";

  private final ComponentCategoryDAO componentCategoryDAO;

  public ComponentCategoryValueType(final ComponentCategoryDAO componentCategoryDAO) {
    this.componentCategoryDAO = componentCategoryDAO;
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getDataType() {
    return "ComponentCategory";
  }

  @Override
  public boolean isAllowMultiple() {
    return false;
  }

  @Override
  public List<ComponentCategory> getAvailableValues() {
    return componentCategoryDAO.getAll();
  }
}
