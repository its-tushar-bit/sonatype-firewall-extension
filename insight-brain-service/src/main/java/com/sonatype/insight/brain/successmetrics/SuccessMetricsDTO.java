/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

/**
 * @since 1.37
 */
public class SuccessMetricsDTO
{
  public String id;

  public String name;

  public SuccessMetricsScopeDTO scope;

  public SuccessMetricsDTO() {
  }

  public SuccessMetricsDTO(final String name, final SuccessMetricsScopeDTO scope) {
    this.name = name;
    this.scope = scope;
  }
}
