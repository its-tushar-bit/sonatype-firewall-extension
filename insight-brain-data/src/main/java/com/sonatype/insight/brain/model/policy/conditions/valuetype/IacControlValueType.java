/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions.valuetype;

import java.util.List;

import com.sonatype.insight.brain.model.component.IacControl;
import com.sonatype.insight.brain.model.policy.ConditionValueType;

public class IacControlValueType
    implements ConditionValueType<IacControl>
{
  public static final String ID = "IacControlValueType";

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getDataType() {
    return "IacControl";
  }

  @Override
  public boolean isAllowMultiple() {
    return true;
  }

  @Override
  public List<IacControl> getAvailableValues() {
    return IacControl.getAll();
  }
}
