/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import java.util.List;

/**
 * @since 1.33
 */
public class SuccessMetricsAveragesDTO
{
  public int activeApplicationCount;
  public List<AverageDiscoveredPolicyViolationsDTO> averageDiscoveredPolicyViolations;

  public SuccessMetricsAveragesDTO() {
  }

  public SuccessMetricsAveragesDTO(int activeApplicationCount,
                        List<AverageDiscoveredPolicyViolationsDTO> averageDiscoveredPolicyViolations)
  {
    this.activeApplicationCount = activeApplicationCount;
    this.averageDiscoveredPolicyViolations = averageDiscoveredPolicyViolations;
  }
}
