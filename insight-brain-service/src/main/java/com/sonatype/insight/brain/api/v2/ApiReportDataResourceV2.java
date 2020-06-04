/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.net.URI;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationDiffDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportPolicyDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import com.sonatype.insight.brain.api.v2.service.ApiReportViolationsDiffService;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.service.BaseUrl;

import com.codahale.metrics.annotation.Timed;

/**
 * Provides data from an application's composition report in a format suitable for consumption by 3rd-party clients.
 * 
 * @since 1.13.0
 */
@Named
@Timed
@Path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2)
public class ApiReportDataResourceV2
{
  public static final String SCAN_PATH = "{scanId}";

  public static final String RAW_DATA_PATH = "raw";

  public static final String POLICY_DATA_PATH = "policy";

  public static final String VIOLATION_DIFF_PATH = "policyViolations/diff";

  private final ApiReportDataServiceV2 reportDataService;

  private final BaseUrl baseUrl;

  private final ApiReportViolationsDiffService apiReportViolationsDiffService;

  @Inject
  public ApiReportDataResourceV2(
      final ApiReportDataServiceV2 reportDataService,
      final BaseUrl baseUrl,
      final ApiReportViolationsDiffService apiReportViolationsDiffService)
  {
    this.reportDataService = reportDataService;
    this.baseUrl = baseUrl;
    this.apiReportViolationsDiffService = apiReportViolationsDiffService;
  }

  /**
   * NOTE: prior to IQ 63, this endpoint was the actual implementation that is now at the RAW_DATA_PATH, rather
   * than a redirect
   */
  @GET
  @Path(SCAN_PATH)
  public Response getData(@PathParam("applicationPublicId") String applicationPublicId,
                          @PathParam("scanId") String scanId) throws Exception
  {
    return Response.temporaryRedirect(new URI(baseUrl.get()).resolve(getDataUrl(applicationPublicId, scanId))).build();
  }

  /**
   * Gets the JSON data for the report of the given application and scan.
   * @since 1.63
   */
  @GET
  @Path(SCAN_PATH + "/" + RAW_DATA_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT)
  public ApiReportRawDataDTOV2 getRawData(@PathParam("applicationPublicId") String applicationPublicId,
                                          @PathParam("scanId") String scanId) throws Exception
  {
    AuditData.get().setReportId(scanId);
    return reportDataService.getRawData(applicationPublicId, scanId);
  }

  /**
   * Gets the JSON data for the policy violations in the report of the given application and scan.
   * @since 1.64
   */
  @GET
  @Path(SCAN_PATH + "/" + POLICY_DATA_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT)
  public ApiReportPolicyDataDTOV2 getPolicyViolations(@PathParam("applicationPublicId") String applicationPublicId,
                                                      @PathParam("scanId") String scanId) throws Exception
  {
    AuditData.get().setReportId(scanId);
    return reportDataService.getPolicyViolationsData(applicationPublicId, scanId);
  }

  /**
   * Gets the relative URL to this REST resource for the given application and scan.
   */
  public static String getDataUrl(String applicationPublicId, String scanId) {
    return UriBuilder.fromPath(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2)
        .path(ApiReportDataResourceV2.SCAN_PATH)
        .path(ApiReportDataResourceV2.RAW_DATA_PATH)
        .build(applicationPublicId, scanId).toString();
  }

  @GET
  @Path(VIOLATION_DIFF_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public ApiPolicyViolationDiffDTO getPolicyViolationDiff(
      @PathParam("applicationPublicId") final String applicationPublicId,
      @QueryParam("fromCommit") final String fromCommit,
      @QueryParam("toCommit") final String toCommit,
      @QueryParam("fromPolicyEvaluationId") final String fromPolicyEvaluationId,
      @QueryParam("toPolicyEvaluationId") final String toPolicyEvaluationId)
  {
    return apiReportViolationsDiffService
        .getPolicyViolationDiff(applicationPublicId, fromCommit, toCommit, fromPolicyEvaluationId,
            toPolicyEvaluationId);
  }
}
