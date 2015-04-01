/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions.valuetype;

import java.util.List;

import com.sonatype.insight.brain.model.policy.ConditionValueType;

public class IntegerValueType
    implements ConditionValueType<Integer>
{
  public static final String ID = "IntegerValueType";

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getDataType() {
    return "Integer";
  }

  @Override
  public boolean isAllowMultiple() {
    return false;
  }

  @Override
  public List<Integer> getAvailableValues() {
    return null;
  }
}
