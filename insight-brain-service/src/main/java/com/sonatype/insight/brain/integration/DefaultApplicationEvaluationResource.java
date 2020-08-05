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

/**
 * @since 1.69
 */
@Path(ApplicationEvaluationResourceConstants.RESOURCE_PATH)
@Named
@Timed
public class DefaultApplicationEvaluationResource
    implements ApplicationEvaluationResource
{
  private final PolicyEvaluateService policyEvaluateService;

  @Inject
  public DefaultApplicationEvaluationResource(PolicyEvaluateService policyEvaluateService) {
    this.policyEvaluateService = policyEvaluateService;
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path(ApplicationEvaluationResourceConstants.EVALUATE_PATH)
  @Audited(AuditEvent.EVALUATE_APPLICATION)
  @Override
  public PolicyEvaluationReceipt evaluateWithPolling(
      @PathParam("applicationPublicId") final String applicationPublicId,
      @PathParam("integrationType") final IntegrationType integrationType,
      @PathParam("stageId") final Stage stage,
      @QueryParam("scanType") ClientScanType clientScanType,
      @Context HttpServletRequest req) throws IOException
  {
    return policyEvaluateService
        .evaluateWithPolling(integrationType, applicationPublicId, clientScanType, req, stage);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(ApplicationEvaluationResourceConstants.STATUS_PATH)
  @Override
  public PolicyEvaluationPollingResult pollEvaluationResult(
      @PathParam("applicationPublicId") final String applicationPublicId,
      @PathParam("statusId") final String statusId)
  {
    return policyEvaluateService.pollEvaluationResult(applicationPublicId, statusId);
  }
}
