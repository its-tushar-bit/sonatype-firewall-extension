/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import java.util.Date;
import java.util.List;

/**
 * @since 1.37
 */
public class SuccessMetricsChartDataDTO
{
  public List<MttrDTO> mttrs;

  public AverageDiscoveredPolicyViolationsDTO averages;

  public ApplicationCountsDTO applicationCounts;

  public List<ViolationCountsDTO> violationCounts;

  // Ordered list of up to 12 weeks in chronologically increasing order
  public List<ViolationsByCategoryDTO> violationsByCategoryWeeks;

  public Date lastUpdated;

  public int monthCount;
}
