/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationDiffDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportPolicyDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;

/**
 * Resource for API Report Data
 */
public interface ApiReportDataResourceV2
{
  /**
   * Gets the JSON data for the report of the given application and scan.
   *
   * @since 1.63
   */
  ApiReportRawDataDTOV2 getRawData(String applicationPublicId, String scanId) throws Exception;

  /**
   * Gets the JSON data for the policy violations in the report of the given application and scan.
   *
   * @since 1.64
   */
  ApiReportPolicyDataDTOV2 getPolicyViolations(String applicationPublicId, String scanId) throws Exception;

  ApiPolicyViolationDiffDTO getPolicyViolationDiff(String applicationPublicId,
                                                   String fromCommit,
                                                   String toCommit,
                                                   String fromPolicyEvaluationId,
                                                   String toPolicyEvaluationId);
}
