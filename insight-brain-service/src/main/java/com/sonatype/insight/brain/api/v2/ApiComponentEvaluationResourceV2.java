/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationRequestDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationResultDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationTicketDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiComponentEvaluationServiceV2;

/**
 * @since 1.13.0
 */
@Named
@Path(PublicApiPaths.APPLICATION_EVALUATION_PATH_V2)
public class ApiComponentEvaluationResourceV2
{
  private final ApiComponentEvaluationServiceV2 componentEvaluationService;

  @Inject
  public ApiComponentEvaluationResourceV2(final ApiComponentEvaluationServiceV2 componentEvaluationService) {
    this.componentEvaluationService = componentEvaluationService;
  }

  @Path("{applicationId}")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ApiComponentEvaluationTicketDTOV2 evaluateComponents(@PathParam("applicationId") final String applicationId,
                                                              final ApiComponentEvaluationRequestDTOV2 evaluationRequest)
  {
    return componentEvaluationService.evaluateComponents(applicationId, evaluationRequest);
  }

  @Path("{applicationId}/results/{resultId}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ApiComponentEvaluationResultDTOV2 getComponentEvaluation(@PathParam("applicationId") final String applicationId,
                                                                  @PathParam("resultId") final String resultId)
      throws IOException
  {
    return componentEvaluationService.getComponentEvaluation(applicationId, resultId);
  }
}
