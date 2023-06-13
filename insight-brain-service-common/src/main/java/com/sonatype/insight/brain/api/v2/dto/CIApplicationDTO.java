/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.api.v2.dto;

public class CIApplicationDTO
{
  public String applicationPublicId;

  public String applicationName;

  public int totalRisk;

  public CIApplicationDTO() {
  }

  public CIApplicationDTO(final String applicationPublicId, final String applicationName, final int totalRisk) {
    this.applicationPublicId = applicationPublicId;
    this.applicationName = applicationName;
    this.totalRisk = totalRisk;
  }
}
