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

  public String category;

  public String description;

  public List<String> features;

  public String accessButtonText;

  public String previewImage;

  public String previewImageIcon;

  public Integer priority;

  public boolean spotlight;

  public String spotlightColor;

  public String spotlightText;

  public String dashboardPath;

  public String sinceIQVersion;

  public String groupId;

  public DashboardMetadataDTO() {
    // for jackson;
  }

  public DashboardMetadataDTO(
      final String dashboardId,
      final String groupId,
      final String title,
      final String category,
      final String description,
      final List<String> features,
      final String accessButtonText,
      final String previewImage,
      final String previewImageIcon,
      final Integer priority,
      final boolean spotlight,
      final String dashboardPath,
      final String spotlightColor,
      final String spotlightText)
  {
    this.dashboardId = dashboardId;
    this.groupId = groupId;
    this.title = title;
    this.category = category;
    this.description = description;
    this.features = features;
    this.accessButtonText = accessButtonText;
    this.previewImage = previewImage;
    this.previewImageIcon = previewImageIcon;
    this.priority = priority;
    this.spotlight = spotlight;
    this.dashboardPath = dashboardPath;
    this.spotlightColor = spotlightColor;
    this.spotlightText = spotlightText;
  }

  // for testing
  public DashboardMetadataDTO(
      final String dashboardId,
      final String groupId,
      final String title,
      final String category,
      final String description,
      final List<String> features,
      final String accessButtonText,
      final String previewImage,
      final String previewImageIcon,
      final Integer priority,
      final boolean spotlight,
      final String dashboardPath,
      final String spotlightColor,
      final String spotlightText,
      final String sinceIQVersion)
  {
    this.dashboardId = dashboardId;
    this.groupId = groupId;
    this.title = title;
    this.category = category;
    this.description = description;
    this.features = features;
    this.accessButtonText = accessButtonText;
    this.previewImage = previewImage;
    this.previewImageIcon = previewImageIcon;
    this.priority = priority;
    this.spotlight = spotlight;
    this.dashboardPath = dashboardPath;
    this.spotlightColor = spotlightColor;
    this.spotlightText = spotlightText;
    this.sinceIQVersion = sinceIQVersion;
  }
}
