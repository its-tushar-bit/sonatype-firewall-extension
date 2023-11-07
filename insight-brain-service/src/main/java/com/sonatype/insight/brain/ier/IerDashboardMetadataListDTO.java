/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ier;

import java.util.List;

public class IerDashboardMetadataListDTO
{
  public List<IerDashboardMetadataDTO> dashboardMetadata;

  public IerDashboardMetadataListDTO() {
    //for jackson
  }

  public IerDashboardMetadataListDTO(List<IerDashboardMetadataDTO> dashboardMetadata) {
    this.dashboardMetadata = dashboardMetadata;
  }
}
