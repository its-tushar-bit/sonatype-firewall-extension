/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.aggregation;

import java.util.Date;

/**
 * @since 1.33
 */
public class MttrDTO
{
  public Date timePeriodStart;

  public Integer mttrInSeconds;

  public Integer criticalMttrInSeconds;

  // mainly present to help debug automated tests
  @Override
  public String toString() {
    return "[MttrDTO timePeriodStart=" + timePeriodStart + "; mttrInSeconds=" + mttrInSeconds
        + "; criticalMttrInSeconds=" + criticalMttrInSeconds + "]";
  }
}
