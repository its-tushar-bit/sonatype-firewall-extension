/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.enterprise.reporting;

import java.util.List;

public class DashboardGroupMetadataDTO
{
  public String groupId;

  public String description;

  public List<String> features;

  public String previewImageIcon;

  public boolean spotlight;

  public String spotlightColor;

  public String spotlightText;

  public String sinceIQVersion;

  public String title;

  public DashboardGroupMetadataDTO() {
    // for jackson;
  }

  public DashboardGroupMetadataDTO(
      final String groupId,
      final String description,
      final List<String> features,
      final String previewImageIcon,
      final boolean spotlight,
      final String spotlightColor,
      final String spotlightText,
      final String title)
  {
    this.groupId = groupId;
    this.description = description;
    this.features = features;
    this.previewImageIcon = previewImageIcon;
    this.spotlight = spotlight;
    this.spotlightColor = spotlightColor;
    this.spotlightText = spotlightText;
    this.title = title;
  }
}
