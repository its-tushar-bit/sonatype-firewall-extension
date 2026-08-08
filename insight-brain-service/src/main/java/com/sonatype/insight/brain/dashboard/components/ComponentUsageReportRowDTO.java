/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

/**
 * One policy-evaluation report (scan) where the requested component hash appears for an application.
 */
public class ComponentUsageReportRowDTO
{
  /**
   * Policy-evaluation scan id used in report URLs ({@code /applications/{publicId}/report/{reportId}}),
   * not an {@code owner_component} / report-row primary key.
   */
  public String reportId;

  public String stageTypeId;

  /** Evaluation time for this report, epoch millis. */
  public Long evaluationTime;
}
