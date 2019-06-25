/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationReceipt;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateService;
import com.sonatype.insight.scan.model.ClientScanType;

import com.codahale.metrics.annotation.Timed;

@Path(ApplicationEvaluationResource.RESOURCE_PATH)
@Named
@Timed
public class ApplicationEvaluationResource
{
  public static final String RESOURCE_PATH = "rest/integration/applications/{applicationPublicId}/evaluations";

  public static final String EVALUATE_PATH = "{integrationType: ci|cli|rm}/stages/{stageId}";

  public static final String STATUS_PATH = "status/{statusId}";

  private final PolicyEvaluateService policyEvaluateService;

  @Inject
  public ApplicationEvaluationResource(PolicyEvaluateService policyEvaluateService) {
    this.policyEvaluateService = policyEvaluateService;
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path(EVALUATE_PATH)
  @Audited(AuditEvent.EVALUATE_APPLICATION)
  public PolicyEvaluationReceipt evaluateWithPolling(
      @PathParam("applicationPublicId") final String applicationPublicId,
      @PathParam("integrationType") final IntegrationType integrationType,
      @PathParam("stageId") final Stage stage,
      @QueryParam("scanType") ClientScanType clientScanType,
      @Context HttpServletRequest req) throws IOException
  {

    return policyEvaluateService.evaluateWithPolling(integrationType, applicationPublicId, clientScanType, req, stage);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(STATUS_PATH)
  public PolicyEvaluationPollingResult pollEvaluationResult(
      @PathParam("applicationPublicId") final String applicationPublicId,
      @PathParam("statusId") final String statusId)
  {
    return policyEvaluateService.pollEvaluationResult(applicationPublicId, statusId);
  }
}
