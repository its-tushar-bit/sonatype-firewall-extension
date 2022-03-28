/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

public class AttributionReportApplicationDTO
{
  public String applicationId;

  public String applicationPublicId;

  public String stageTypeName;

  public AttributionReportApplicationDTO() {
    // for Jackson
  }

  public AttributionReportApplicationDTO(String applicationId, String applicationPublicId, String stageTypeName) {
    this.applicationId = applicationId;
    this.applicationPublicId = applicationPublicId;
    this.stageTypeName = stageTypeName;
  }
}
