/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.insight.brain.api.v2.dto.*;

/**
 * Resource for API Evaluation
 */
public interface ApiEvaluationResourceV2
{
  ApiComponentEvaluationTicketDTOV2 evaluateComponents(String applicationId,
                                                       ApiComponentEvaluationRequestDTOV2 evaluationRequest);

  ApiComponentEvaluationResultDTOV2 getComponentEvaluation(String applicationId, String resultId) throws IOException;

  ApiApplicationEvaluationStatusDTOV2 promoteScan(String applicationId,
                                                  ApiPromoteScanRequestDTOV2 promoteScanRequest,
                                                  HttpServletRequest request);

  /**
   * @since 1.101
   */
  ApiApplicationEvaluationStatusDTOV2 doManifestEvaluation(
      String applicationId,
      ApiManifestEvaluationRequestDTO manifestEvaluationRequest,
      HttpServletRequest request);

  ApiApplicationEvaluationResultDTOV2 getApplicationEvaluationStatus(String applicationId, String statusId);
}
