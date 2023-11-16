/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.enterprise.reporting;

import java.util.List;

public class DashboardMetadataListDTO
{
  public List<DashboardMetadataDTO> dashboardMetadata;

  public DashboardMetadataListDTO() {
    //for jackson
  }

  public DashboardMetadataListDTO(List<DashboardMetadataDTO> dashboardMetadata) {
    this.dashboardMetadata = dashboardMetadata;
  }
}
