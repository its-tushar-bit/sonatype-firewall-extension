/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

/**
 * @since 1.37
 */
public class SuccessMetricsReportDTO
{
  public String id;

  public String name;

  public SuccessMetricsReportScopeDTO scope;

  public boolean includeLatestData;

  public SuccessMetricsReportDTO() {
  }

  public SuccessMetricsReportDTO(final String name, final SuccessMetricsReportScopeDTO scope) {
    this(name, scope, false);
  }

  public SuccessMetricsReportDTO(
      final String name,
      final SuccessMetricsReportScopeDTO scope,
      final boolean includeLatestData)
  {
    this.name = name;
    this.scope = scope;
    this.includeLatestData = includeLatestData;
  }
}
