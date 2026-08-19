/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

/**
 * @since 1.51
 */
public class ViolationsByCategoryDTO
{
  public String timePeriodName;

  public Integer security;

  public Integer license;

  public Integer quality;

  public Integer other;

  // for deserialization
  public ViolationsByCategoryDTO() {
  }

  public ViolationsByCategoryDTO(
      String timePeriodName,
      Integer security,
      Integer license,
      Integer quality,
      Integer other)
  {
    this.timePeriodName = timePeriodName;
    this.security = security;
    this.license = license;
    this.quality = quality;
    this.other = other;
  }
}
