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
public class AverageDiscoveredPolicyViolationsDTO
{
  public Date timePeriodStart;

  public AverageDiscoveredThreatCategoryPolicyViolationsDTO security;
  public AverageDiscoveredThreatCategoryPolicyViolationsDTO license;
  public AverageDiscoveredThreatCategoryPolicyViolationsDTO quality;
  public AverageDiscoveredThreatCategoryPolicyViolationsDTO other;
  public int evaluationCount;

  static class AverageDiscoveredThreatCategoryPolicyViolationsDTO
  {
    public double averageDiscoveredLow;
    public double averageDiscoveredModerate;
    public double averageDiscoveredSevere;
    public double averageDiscoveredCritical;

    public AverageDiscoveredThreatCategoryPolicyViolationsDTO() {
      // for jackson
    }

    public AverageDiscoveredThreatCategoryPolicyViolationsDTO(double averageDiscoveredLow,
                                                              double averageDiscoveredModerate,
                                                              double averageDiscoveredSevere,
                                                              double averageDiscoveredCritical)
    {
      this.averageDiscoveredLow = averageDiscoveredLow;
      this.averageDiscoveredModerate = averageDiscoveredModerate;
      this.averageDiscoveredSevere = averageDiscoveredSevere;
      this.averageDiscoveredCritical = averageDiscoveredCritical;
    }
  }
}
