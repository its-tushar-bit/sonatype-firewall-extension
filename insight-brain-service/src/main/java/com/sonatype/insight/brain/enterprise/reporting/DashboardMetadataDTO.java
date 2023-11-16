/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.enterprise.reporting;

import java.util.List;

public class DashboardMetadataDTO
{
  public String dashboardId;

  public String title;

  public String description;

  public List<String> features;

  public String accessButtonText;

  public String previewImage;

  public Integer priority;

  public boolean spotlight;

  public DashboardMetadataDTO() {
    //for jackson;
  }

  public DashboardMetadataDTO(final String dashboardId,
                              final String title,
                              final String description,
                              final List<String> features,
                              final String accessButtonText,
                              final String previewImage,
                              final Integer priority,
                              final boolean spotlight)
  {
    this.dashboardId = dashboardId;
    this.title = title;
    this.description = description;
    this.features = features;
    this.accessButtonText = accessButtonText;
    this.previewImage = previewImage;
    this.priority = priority;
    this.spotlight = spotlight;
  }
}
