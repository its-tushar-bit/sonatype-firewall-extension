/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanResultDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.api.v2.dto.IdeUsersOverviewDTO;
import com.sonatype.insight.brain.api.v2.service.ApiThirdPartyScanService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.scan.file.SbomFormat;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.75
 */
@Named
@Timed
@Path(PublicApiPaths.THIRD_PARTY_SCAN_PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.APPLICATION_EVALUATION)
@Tag(name = "Third-Party Analysis", description = "Use this REST API to scan SBOMs for your applications.")
public class ApiThirdPartyScanResource
{
  public static final String SCAN_COMPONENTS = "{applicationId}/sources/{source}";

  public static final String SCAN_STATUS = "{applicationId}/status/{scanRequestId}";

  public static final String IDE_USER_OVERVIEW = "ideUser/overview";

  public static final String SINCE_UTC_TIMESTAMP = "sinceUtcTimestamp";

  private final ApiThirdPartyScanService thirdPartyScanService;

  @Inject
  public ApiThirdPartyScanResource(final ApiThirdPartyScanService thirdPartyScanService) {
    this.thirdPartyScanService = thirdPartyScanService;
  }

  @Path(SCAN_COMPONENTS)
  @POST
  @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EVALUATE_THIRD_PARTY)
  @Operation(description = "Use this method to perform an analysis of an SBOM." +
      "\n" +
      "\n" +
      "Permissions required: Evaluate Applications",
      responses = {
        @ApiResponse(responseCode = "202",
            description = "The response contains a `statusUrl` containing the applicationId and statusId " +
                "that can be used to check the progress of the SBOM evaluation.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiThirdPartyScanTicketDTO.class)))
      })
  public Response scanComponents(
      @Parameter(description = "Enter the application internal id. Use the Applications REST API to retrieve the" +
          "application internal id.", required = true) @PathParam("applicationId") final String applicationId,
      @Parameter(description = "Specify the specification name of the SBOM to be evaluated. " +
          "Allowed values are `cyclonedx` and `spdx`", required = true) @PathParam("source") final String source,
      @Parameter(description = "Enter the stageId to run the evaluation for. The policy actions will be determined " +
          "by the stage selected. Allowed values are `develop`, `build`, `stage-release`, `release` and `operate`") @DefaultValue("build") @QueryParam("stageId") final String stageId,
      @Context final HttpServletRequest request,
      @RequestBody(description = "Select the request header content-type from the dropdown, depending on whether " +
          "the SBOM is in XML or JSON format." +
          "\n" +
          "\n" +
          "Embed the contents of the SBOM here or enter the file path for the SBOM. A component in the SBOM can " +
          "be identified by: " +
          "<ol>" +
          "<li>packageUrl</li>" +
          "<li>Component hash</li>" +
          "<li>Component name and version</li></ol>" +
          "\n" +
          "\n" +
          "Any SPE and SWID tags for the component will be preserved in the evaluation report.") final String sbom)
  {
    SbomFormat format =
        request.getContentType().equalsIgnoreCase(MediaType.APPLICATION_XML) ? SbomFormat.XML : SbomFormat.JSON;
    ApiThirdPartyScanTicketDTO ticket = thirdPartyScanService.scanComponents(applicationId, source, stageId, sbom,
        HdsClient.getClientUserAgent(request), format);
    return Response.status(Response.Status.ACCEPTED).entity(ticket).build();
  }

  @GET
  @Path(SCAN_STATUS)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "SBOM evaluation is an asynchronous operation. Use this method to check on the status " +
      "of the SBOM evaluation." +
      "\n" +
      "\n" +
      "Permissions required: Evaluate Applications",
      responses = {
        @ApiResponse(
            responseCode = "404",
            description = "Evaluation report is not ready."),
        @ApiResponse(
            responseCode = "200",
            description = "The response contains summarized results of the SBOM evaluation and the URLs for detailed " +
                "evaluation reports in HTML, pdf and raw formats." +
                "\n" +
                "\n" +
                "`policyAction` indicates the policy actions determined by the `stageId` selected while " +
                "submitting the evaluation using the POST method.",
            useReturnTypeSchema = true)
      })
  public ApiThirdPartyScanResultDTO getScanStatus(
      @Parameter(description = "Enter the application internal id for the SBOM to be evaluated.",
          required = true) @PathParam("applicationId") String applicationId,
      @Parameter(description = "Enter the statusId from the statusUrl generated as a response to the POST request to " +
          "perform the evaluation.", required = true) @PathParam("scanRequestId") String scanRequestId)
  {
    return thirdPartyScanService.getScanStatus(applicationId, scanRequestId);
  }

  @GET
  @Path(IDE_USER_OVERVIEW)
  @Produces(MediaType.APPLICATION_JSON)
  @Hidden
  public IdeUsersOverviewDTO getIdeUsersOverview(
      @QueryParam(SINCE_UTC_TIMESTAMP) final Long sinceUtcTimestamp)
  {
    return thirdPartyScanService.getIdeUsersOverview(sinceUtcTimestamp);
  }
}
