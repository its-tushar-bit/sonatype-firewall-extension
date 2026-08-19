/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO describing the security and license data (raw data) in an application composition report.
 *
 * @since 1.13.0
 */
public class ApiReportRawDataDTOV2
{
  // components in app, in no particular order
  public List<ApiReportComponentDTOV2> components = new ArrayList<>();

  /**
   * @since 1.14.1
   */
  public ApiMatchStateSummaryDTOV2 matchSummary = new ApiMatchStateSummaryDTOV2();

  public ApiGlobalInformationDTOV2 globalInformation = new ApiGlobalInformationDTOV2();
}
