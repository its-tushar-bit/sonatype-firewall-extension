/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationTicketDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiThirdPartyEvaluationService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.service.InsightConfig;

import com.codahale.metrics.annotation.Timed;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * @since 1.75
 */
@Named
@Timed
@Path(PublicApiPaths.APPLICATION_EVALUATION_PATH_V2)
public class ApiThirdPartyResource
{
  public static final String EVALUATE_COMPONENTS_SBOM = "{applicationId}/sbom/{source}";

  private final ApiThirdPartyEvaluationService thirdPartyEvaluationService;

  private final InsightConfig config;

  @Inject
  public ApiThirdPartyResource(
      final ApiThirdPartyEvaluationService thirdPartyEvaluationService,
      final InsightConfig config)
  {
    this.thirdPartyEvaluationService = thirdPartyEvaluationService;
    this.config = config;
  }

  @Path(EVALUATE_COMPONENTS_SBOM)
  @POST
  @Consumes(MediaType.APPLICATION_XML)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EVALUATE_THIRD_PARTY)
  public Response evaluateComponents(
      @PathParam("applicationId") final String applicationId,
      @PathParam("source") final String source,
      @DefaultValue("build") @QueryParam("stageId") final String stageId,
      @NotNull @NotEmpty String sbom)
  {
    if (config.isThirdPartyEvaluationApiEnabled()) {
      ApiComponentEvaluationTicketDTOV2 ticket =
          thirdPartyEvaluationService.evaluateComponents(applicationId, source, stageId, sbom);
      return Response.status(Response.Status.OK).entity(ticket).build();
    }
    else {
      return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }
  }
}
