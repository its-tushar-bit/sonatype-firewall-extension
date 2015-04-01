/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.List;

public interface ConditionValueType<T>
{
  String getId();

  String getDataType();

  boolean isAllowMultiple();

  List<T> getAvailableValues();
}
