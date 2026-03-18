/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.enterprise.reporting;

import java.util.List;

public class DashboardMetadataListDTO
{
  public DashboardsVersionDTO version;

  public List<DashboardMetadataDTO> dashboardMetadata;

  public List<DashboardGroupMetadataDTO> dashboardGroupMetadata;

  public DashboardMetadataListDTO() {
    // for jackson
  }

  public DashboardMetadataListDTO(
      DashboardsVersionDTO version,
      List<DashboardMetadataDTO> dashboardMetadata,
      List<DashboardGroupMetadataDTO> dashboardGroupMetadata)
  {
    this.version = version;
    this.dashboardMetadata = dashboardMetadata;
    this.dashboardGroupMetadata = dashboardGroupMetadata;
  }
}
