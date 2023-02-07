/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import com.sonatype.insight.brain.api.v2.dto.ApiApplicationEvaluationStatusDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiSourceControlEvaluationRequestDTO;

public interface ApiSourceControlEvaluationService
{
  ApiApplicationEvaluationStatusDTOV2 evaluateSourceControl(
      String applicationId,
      ApiSourceControlEvaluationRequestDTO sourceControlEvaluationRequest, String clientUserAgent);
}
