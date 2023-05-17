/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;

import com.sonatype.insight.brain.api.v2.dto.ApiApplicationReportDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportHistoryDTO;

/**
 * Resource for API Report
 */
public interface ApiReportResourceV2
{
  List<ApiApplicationReportDTOV2> getByApplicationId(String applicationId);

  List<ApiApplicationReportDTOV2> getAll();

  ApiReportHistoryDTO getReportHistoryForApplication(String applicationId, String stage, Integer limit);
}
