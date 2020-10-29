/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.legal.dto;

import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;

/**
 * @since 1.101
 */
public class ApplicationReportRawDataDTO
{
  public String applicationPublicId;

  public ApiReportRawDataDTOV2 apiReportRawDataDTOV2;

  public ApplicationReportRawDataDTO(String applicationPublicId, ApiReportRawDataDTOV2 apiReportRawDataDTOV2) {
    this.applicationPublicId = applicationPublicId;
    this.apiReportRawDataDTOV2 = apiReportRawDataDTOV2;
  }
}
