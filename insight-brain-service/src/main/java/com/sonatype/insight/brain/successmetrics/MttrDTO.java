/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

/**
 * @since 1.33
 */
public class MttrDTO
{
  public String timePeriodName;

  public Integer mttrInSeconds;

  public Integer criticalMttrInSeconds;

  public MttrDTO() {
  }

  public MttrDTO(String timePeriodName, Integer mttrInSeconds, Integer criticalMttrInSeconds) {
    this.timePeriodName = timePeriodName;
    this.mttrInSeconds = mttrInSeconds;
    this.criticalMttrInSeconds = criticalMttrInSeconds;
  }

  // mainly present to help debug automated tests
  @Override
  public String toString() {
    return "[MttrDTO timePeriodName=" + timePeriodName + "; mttrInSeconds=" + mttrInSeconds + "; criticalMttrInSeconds="
        + criticalMttrInSeconds + "]";
  }
}
