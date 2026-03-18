/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions.valuetype;

import com.sonatype.insight.brain.model.policy.ConditionValueType;
import com.sonatype.insight.brain.model.vulnerability.KevStatus;

import java.util.List;

public class KevStatusValueType
    implements ConditionValueType<KevStatus>
{
  public static final String ID = "KevStatusValueType";

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getDataType() {
    return "KevStatus";
  }

  @Override
  public boolean isAllowMultiple() {
    return false;
  }

  @Override
  public List<KevStatus> getAvailableValues() {
    return KevStatus.getAll();
  }
}
