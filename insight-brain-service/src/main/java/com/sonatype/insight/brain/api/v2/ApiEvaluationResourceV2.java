/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationEvaluationResultDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationEvaluationStatusDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationRequestDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationResultDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationTicketDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiPromoteScanRequestDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiComponentEvaluationServiceV2;
import com.sonatype.insight.brain.api.v2.service.ApiPromoteScanServiceV2;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.hds.HdsClient;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.13.0
 */
@Named
@Timed
@Path(PublicApiPaths.APPLICATION_EVALUATION_PATH_V2)
public class ApiEvaluationResourceV2
{
  public static final String PROMOTE_SCAN_PATH = "{applicationId}/promoteScan";

  private final ApiComponentEvaluationServiceV2 componentEvaluationService;

  private final ApiPromoteScanServiceV2 promoteScanService;

  @Inject
  public ApiEvaluationResourceV2(
      final ApiComponentEvaluationServiceV2 componentEvaluationService,
      final ApiPromoteScanServiceV2 apiPromoteScanServiceV2)
  {
    this.componentEvaluationService = componentEvaluationService;
    this.promoteScanService = apiPromoteScanServiceV2;
  }

  @Path("{applicationId}")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EVALUATE_AD_HOC)
  public ApiComponentEvaluationTicketDTOV2 evaluateComponents(
      @PathParam("applicationId") final String applicationId,
      final ApiComponentEvaluationRequestDTOV2 evaluationRequest)
  {
    return componentEvaluationService.evaluateComponents(applicationId, evaluationRequest);
  }

  @Path("{applicationId}/results/{resultId}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EXPORT_EVALUATION_AD_HOC)
  public ApiComponentEvaluationResultDTOV2 getComponentEvaluation(
      @PathParam("applicationId") final String applicationId,
      @PathParam("resultId") final String resultId) throws IOException
  {
    return componentEvaluationService.getComponentEvaluation(applicationId, resultId);
  }

  @Path(PROMOTE_SCAN_PATH)
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(value = AuditEvent.EVALUATE_APPLICATION)
  public ApiApplicationEvaluationStatusDTOV2 promoteScan(
      @PathParam("applicationId") final String applicationId,
      final ApiPromoteScanRequestDTOV2 promoteScanRequest,
      @Context HttpServletRequest request)
  {
    return promoteScanService.promoteScan(applicationId, promoteScanRequest, HdsClient.getClientUserAgent(request));
  }

  @GET
  @Path("{applicationId}/status/{statusId}")
  @Produces(MediaType.APPLICATION_JSON)
  public ApiApplicationEvaluationResultDTOV2 getApplicationEvaluationStatus(
      @PathParam("applicationId") String applicationId,
      @PathParam("statusId") String statusId)
  {
    return promoteScanService.getApplicationEvaluationStatus(applicationId, statusId);
  }
}
