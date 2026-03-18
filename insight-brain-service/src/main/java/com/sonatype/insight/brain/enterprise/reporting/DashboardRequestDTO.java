/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.enterprise.reporting;

public class DashboardRequestDTO
{
  public String dashboard;

  public DashboardRequestDTO() {
    // for jackson;
  }

  public DashboardRequestDTO(final String dashboard) {
    this.dashboard = dashboard;
  }
}
