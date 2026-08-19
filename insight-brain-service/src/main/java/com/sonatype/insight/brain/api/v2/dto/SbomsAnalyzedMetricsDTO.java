/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

public class SbomsAnalyzedMetricsDTO
{
  private long total;

  private long threshold;

  public SbomsAnalyzedMetricsDTO() {
    // for Jackson
  }

  public SbomsAnalyzedMetricsDTO(long total, long threshold) {
    this.total = total;
    this.threshold = threshold;
  }

  public long getTotal() {
    return total;
  }

  public long getThreshold() {
    return threshold;
  }
}
