/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationReportDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportHistoryDTO;
import com.sonatype.insight.brain.api.v2.service.ApiReportServiceV2;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2 + ApiReportResourceV2.PATH)
@Named
@Timed
@ProductLicenseEnforcementPoint(LicensedFeature.APPLICATION_REPORTS)
@Tag(name = "Reports",
    description = "Use this REST API to view application scan reports, generate a list of stale waivers, view existing "
        +
        "policy waivers on components, view quarantined components and retrieve additional metrics data. ")
public class ApiReportResourceV2
{
  public static final String PATH = "/applications";

  private final ApiReportServiceV2 reportService;

  @Inject
  public ApiReportResourceV2(final ApiReportServiceV2 searchService) {
    this.reportService = searchService;
  }

  @GET
  @Operation(description = "Use this method to retrieve the application reports for the specified " +
      "application Id. You can view application reports only for applications to which you have access. " +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements ",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "The response JSON contains the URLs to access the latest scan report for the " +
                "applicationId provided. " +
                "\n" +
                "\n" +
                "The response field `stage` indicates the stage at which the policy evaluation " +
                "was executed, such as 'develop', 'build', 'release'.  " +
                "The response field `latestReportHtmlUrl` is a relative link to view the most recent report. " +
                "Response fields `reportPdfURL` and `reportHtmlURL` are links to view the pdf version " +
                "of the report. The response field `reportDataUrl` is a link to view the most recent report data. ",
            useReturnTypeSchema = true)
      })
  @Path("{applicationId}")
  @Produces(MediaType.APPLICATION_JSON)
  public List<ApiApplicationReportDTOV2> getByApplicationId(
      @Parameter(description = "Enter the internal application Id. You can use the Applications REST API to get " +
          "the internal application Id. ") @PathParam("applicationId") String applicationId)
  {
    return reportService.getByApplicationId(applicationId);
  }

  @GET
  @Operation(description = "Use this method to view all application reports for applications to which  " +
      "you have access. " +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements ",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "The response JSON contains URLs to view the report data in html and pdf format, for each " +
                "application to which you have access." +
                "\n" +
                "\n" +
                "The response field stage indicates the stage at which the policy evaluation" +
                " was executed, such as 'develop', 'build' and 'release' " +
                "The response field latestReportHtmlUrl is a relative link to view the most recent report. " +
                "Response fields reportPdfUrl and reportHtmlUrl are links to view the pdf version " +
                "of the report." +
                "The response field reportDataUrl is a link to view the most recent report data. ",
            useReturnTypeSchema = true)
      })
  @Produces(MediaType.APPLICATION_JSON)
  public List<ApiApplicationReportDTOV2> getAll() {
    return reportService.getAll();
  }

  @Path("{applicationId}/history")
  @Operation(description = "Use this method to retrieve previous application scan reports (100 max.) for the " +
      "specified application. You can view application reports only for applications to which you have access.  " +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements ",
      responses = {
        @ApiResponse(
            responseCode = "400",
            description = "Error in request. Check for missing or invalid parameter."),
        @ApiResponse(
            responseCode = "404",
            description = "Scan report history could not be found."),
        @ApiResponse(
            responseCode = "200",
            description = "The response contains evaluation details, embeddable link and URLs to view the " +
                "reports in pdf and html formats. " +
                "\n" +
                "\n" +
                "<ul>" +
                "<li><code>stage</code> indicates the stage at which policy evaluation was performed, such as " +
                "'develop', 'build' and 'release'.</li>" +
                "<li><code>latestReportHtmlUrl</code> is a relative link to view the most recent evaluation " +
                "report.</li>" +
                "<li><code>reportPdfUrl</code> and <code>reportHtmlUrl</code> are links to view the pdf version " +
                "of the report.</li>" +
                "<li><code>reportDataUrl</code> is a link to view the most recent report data.</li>" +
                "<li><code>scanId</code> is the Id associated with the evaluation report.</li>" +
                "<li><code>isReevaluation</code> indicates whether this policy evaluation is a re-evaluation of " +
                "an older policy evaluation.</li>" +
                "<li><code>isForMonitoring</code> indicates whether this policy evaluation was triggered by " +
                "continuous monitoring.</li>" +
                "<li><code>commitHash</code> is the hash string that identifies a specific commit in the " +
                "source control system.</li>" +
                "<li><code>scanTriggerType</code> indicates the type of scan used for this evaluation, " +
                "such as WEB_UI.</li>" +
                "<li><code>affectedComponentCount</code> is the number of components identified in this " +
                "evaluation.</li>" +
                "<li><code>criticalComponentCount</code>, <code>severeComponentCount</code>, " +
                "<code>moderateComponentCount</code> indicate the " +
                "number of components with critical, severe and moderate policy violations respectively.</li>" +
                "<li><code>criticalPolicyViolationCount</code>, <code>severePolicyViolationCount</code>, " +
                "<code>moderatePolicyViolationCount</code>" +
                " indicate the number of critical, severe and moderate policy violations respectively.</li>" +
                "<li><code>policyEvaluationResult</code> contains details on the policy violation such as, " +
                "coordinates of the violating component and the specific policy constraints that are violated.</li>" +
                "</ul>",
            useReturnTypeSchema = true)
      })
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ApiReportHistoryDTO getReportHistoryForApplication(
      @Parameter(description = "Enter the internal application Id. You can use the Applications REST API to get " +
          "the internal application Id. ", required = true) @PathParam("applicationId") final String applicationId,
      @Parameter(description = "Enter the specific stage, for which you want retrieve the scan history, e.g." +
          " 'build' ") @QueryParam("stage") String stage,
      @Parameter(
          description = "Enter the exact no. of most recent reports to retrieve (maximum 100; larger values are " +
              "clamped).") @QueryParam("limit") Integer limit)
  {
    return reportService.getReportHistoryForApplication(applicationId, stage, limit);
  }
}
