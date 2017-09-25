/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import java.util.Set;

/**
 * @since 1.37
 */
public class SuccessMetricsReportScopeDTO
{
  public Set<String> applicationIds;

  public Set<String> organizationIds;

  public SuccessMetricsReportScopeDTO() {
  }

  public SuccessMetricsReportScopeDTO(final Set<String> applicationIds, final Set<String> organizationIds) {
    this.applicationIds = applicationIds;
    this.organizationIds = organizationIds;
  }
}
