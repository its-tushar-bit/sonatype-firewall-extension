/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions.valuetype;

import java.util.List;

import com.sonatype.clm.dto.model.component.AiModelContentType;
import com.sonatype.insight.brain.model.policy.ConditionValueType;

public class AiModelContentValueType
    implements ConditionValueType<AiModelContentType>
{
  public static final String ID = "AiModelContentValueType";

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getDataType() {
    return "AiModelContent";
  }

  @Override
  public boolean isAllowMultiple() {
    return false;
  }

  @Override
  public List<AiModelContentType> getAvailableValues() {
    return AiModelContentType.getAll();
  }
}
