/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions.valuetype;

import java.util.List;

import com.sonatype.insight.brain.model.policy.ConditionValueType;

public class LicenseStatusValueType
    implements ConditionValueType<LicenseOverrideStatus>
{
  public static final String ID = "LicenseStatusValueType";

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getDataType() {
    return "LicenseStatus";
  }

  @Override
  public boolean isAllowMultiple() {
    return false;
  }

  @Override
  public List<LicenseOverrideStatus> getAvailableValues() {
    return LicenseOverrideStatus.getAll();
  }
}
