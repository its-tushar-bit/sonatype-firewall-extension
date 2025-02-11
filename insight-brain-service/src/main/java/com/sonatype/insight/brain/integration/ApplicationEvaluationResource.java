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
import com.sonatype.insight.brain.policy.componentanalysis.ComponentAnalysisService;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateService;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationPollingResultDTO;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.scan.model.ClientScanType;

import com.codahale.metrics.annotation.Timed;

/**
 * Resource for integrations points to conform to Application Evaluation.
 *
 * @since 1.69
 */
@Path(ApplicationEvaluationResource.RESOURCE_PATH)
@Named
@Timed
@ProductLicenseEnforcementPoint(LicensedFeature.APPLICATION_EVALUATION)
public class ApplicationEvaluationResource
{
  static final String RESOURCE_PATH = "rest/integration/applications/{applicationPublicId}/evaluations";

  static final String EVALUATE_PATH = "{integrationType: ci|cli|rm}/stages/{stageId}";

  static final String COMPONENT_ANALYSIS_PATH = EVALUATE_PATH + "/component-analysis";

  static final String STATUS_PATH = "status/{statusId}";

  private final PolicyEvaluateService policyEvaluateService;

  private final ComponentAnalysisService componentAnalysisService;

  @Inject
  public ApplicationEvaluationResource(
      PolicyEvaluateService policyEvaluateService,
      ComponentAnalysisService componentAnalysisService)
  {
    this.policyEvaluateService = policyEvaluateService;
    this.componentAnalysisService = componentAnalysisService;
  }

  /**
   * Starts the evaluation of an scanned file for an application, integration, type and stage. After
   * starting will return a {@link PolicyEvaluationReceipt} for requester to use to check on results
   * via {@link #pollEvaluationResult(String, String)}
   *
   * @param applicationPublicId public shared id
   * @param integrationType {@link IntegrationType}
   * @param stage {@link Stage}
   * @param clientScanType {@link ClientScanType}
   * @param req {@link HttpServletRequest}
   * @return PolicyEvaluationReceipt
   * @throws IOException when the scan file, uploaded via the request, is unable to be read or processed
   */
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
    return policyEvaluateService
        .evaluateWithPolling(integrationType, applicationPublicId, clientScanType, req, stage);
  }

  /**
   * Starts the component analysis for an application, integration, type and stage. After
   * starting will return a {@link PolicyEvaluationReceipt} for requester to use to check on results
   * via {@link #pollEvaluationResult(String, String)}
   *
   * @param applicationPublicId public shared id
   * @param integrationType {@link IntegrationType}
   * @param stage {@link Stage}
   * @param clientScanType {@link ClientScanType}
   * @param request {@link HttpServletRequest}
   * @return PolicyEvaluationReceipt
   * @throws IOException when the scan file, uploaded via the request, is unable to be read or processed
   */
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path(COMPONENT_ANALYSIS_PATH)
  @Audited(AuditEvent.EVALUATE_APPLICATION)
  public PolicyEvaluationReceipt analyzeComponentsWithPolling(
      @PathParam("applicationPublicId") final String applicationPublicId,
      @PathParam("integrationType") final IntegrationType integrationType,
      @PathParam("stageId") final Stage stage,
      @QueryParam("scanType") ClientScanType clientScanType,
      @Context HttpServletRequest request) throws IOException
  {
    return componentAnalysisService
        .analyzeComponentsWithPolling(integrationType, applicationPublicId, clientScanType, request, stage);
  }

  /**
   * Retrieve the {@link PolicyEvaluationPollingResult} for an existing request, made
   * through the {@link #evaluateWithPolling(String, IntegrationType, Stage, ClientScanType, HttpServletRequest)} or
   * {@link #analyzeComponentsWithPolling(String, IntegrationType, Stage, ClientScanType, HttpServletRequest)}.
   *
   * @param applicationPublicId public shared id
   * @param statusId id from status, normally gotten from {@link PolicyEvaluationReceipt}
   * @return PolicyEvaluationReceipt
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(STATUS_PATH)
  public PolicyEvaluationPollingResultDTO pollEvaluationResult(
      @PathParam("applicationPublicId") final String applicationPublicId,
      @PathParam("statusId") final String statusId)
  {
    return policyEvaluateService.pollEvaluationResult(applicationPublicId, statusId);
  }
}
