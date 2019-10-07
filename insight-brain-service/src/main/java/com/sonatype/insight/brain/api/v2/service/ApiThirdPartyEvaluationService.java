/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Date;
import java.util.UUID;

import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationTicketDTOV2;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;

/**
 * @since 1.75
 */
@Named
@Singleton
public class ApiThirdPartyEvaluationService
{
  @Authorize(permission = Permission.READ)
  public ApiComponentEvaluationTicketDTOV2 scanComponents(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId,
      final String source,
      final String stageId,
      final String sbom)
  {
    ApiComponentEvaluationTicketDTOV2 evaluationTicketDTO = createScanTicket(applicationId);
    return evaluationTicketDTO;
  }

  private ApiComponentEvaluationTicketDTOV2 createScanTicket(final String applicationId) {
    ApiComponentEvaluationTicketDTOV2 evaluationTicketDTO = new ApiComponentEvaluationTicketDTOV2();
    evaluationTicketDTO.resultId = UUID.randomUUID().toString().replace("-", "");
    evaluationTicketDTO.submittedDate = new Date();
    evaluationTicketDTO.applicationId = applicationId;
    evaluationTicketDTO.resultsUrl = PublicApiPaths.APPLICATION_EVALUATION_PATH_V2 + "/" + applicationId + "/results/"
        + evaluationTicketDTO.resultId;

    return evaluationTicketDTO;
  }
}
