/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.codahale.metrics.annotation.Timed;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.PrioritizedComponent;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.development.prioritization.DevelopmentPrioritiesService;
import com.sonatype.insight.brain.development.prioritization.DevelopmentPrioritizationResults;
import com.sonatype.insight.brain.utils.Csv;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * @since 1.183.0
 */
@Path(ApiDeveloperPrioritiesResourceV2.RESOURCE_PATH)
@Named
@Timed
@Tag(name = "Developer Priorities",
    description = "Use this REST API to export Sonatype Developer component priorities data, " +
        "including security reachability data.")
public class ApiDeveloperPrioritiesResourceV2
{
  public static final String PRIORITIES_PATH = "/priorities/{applicationId}/{scanId}";

  public static final String RESOURCE_PATH =
      PublicApiPaths.DEVELOPER_PATH + ApiDeveloperPrioritiesResourceV2.PRIORITIES_PATH;

  public static final String EXPORT_PATH = "export";

  static final String DEFAULT_PAGE = "1";

  static final String DEFAULT_PAGE_SIZE = "10";

  private DevelopmentPrioritiesService developmentPrioritiesService;

  @Inject
  public ApiDeveloperPrioritiesResourceV2(final DevelopmentPrioritiesService developmentPrioritiesService) {
    this.developmentPrioritiesService = developmentPrioritiesService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = """
      Use this method to retrieve all priorities by providing the application ID and scan ID.

      Permissions required: View IQ Elements""",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = """
                The response field `priorities` returns prioritized components for the specified
                application ID and scan ID. Each result has relevant component information, reachability
                information, policy information, and a priority number, sorted by priority in descending order.
                Pagination is supported, and the default page size is 10.
                The parameter `includeRemediation` is required for the paginated result to
                include remediation information.""",
            useReturnTypeSchema = true)
      })
  public DevelopmentPrioritizationResults getPriorities(
      @Parameter(description = "Enter the applicationId.") @PathParam("applicationId") final String applicationId,
      @Parameter(description = "Enter the scanId.") @PathParam("scanId") final String scanId,
      @Parameter(
          description = "Whether to include remediation type and version for the component or not") @DefaultValue("false") @QueryParam("includeRemediation") boolean includeRemediation,
      @Parameter(description = "Current page number.") @DefaultValue(DEFAULT_PAGE) @QueryParam("page") final int page,
      @Parameter(
          description = "Enter the no. of results that should be visible per page.") @DefaultValue(DEFAULT_PAGE_SIZE) @QueryParam("pageSize") final int pageSize,
      @Parameter(
          description = "Component name to filter by") @QueryParam("componentNameFilter") final String componentNameFilter,
      @Parameter(
          description = "Whether to enable Fail/Warn policy action filter or not") @QueryParam("filterOnPolicyActions") @DefaultValue("true") final boolean filterOnPolicyActions)
  {
    return developmentPrioritiesService
        .getPrioritizedFindings(applicationId, scanId, page, pageSize,
            componentNameFilter, includeRemediation, filterOnPolicyActions);
  }

  @GET
  @Path(EXPORT_PATH)
  @Produces("text/csv")
  @Operation(description = """
      Use this method to retrieve the priorities, by providing the applicationId and scanId.

      Permissions required: View IQ Elements""",
      responses = {
        @ApiResponse(responseCode = "200",
            description = """
                The response is a CSV that contains all the prioritized components for the specified
                applicationId and scanId. Each line has all relevant component information, reachability
                information, policy information, and the priority assigned to it.""")
      })
  @Audited(AuditEvent.EXPORT_DEVELOPER_PRIORITIES)
  public Response getPrioritiesExport(
      @Parameter(description = "Enter the applicationId.") @PathParam("applicationId") final String applicationId,
      @Parameter(description = "Enter the scanId.") @PathParam("scanId") final String scanId)
  {
    List<PrioritizedComponent> results =
        developmentPrioritiesService.getAllPrioritizedFindings(applicationId, scanId, null, null);
    String fileNamePrefix = applicationId + "-" + scanId + "-priorities";
    return Csv.generate(Response.ok(), fileNamePrefix, PrioritizedComponent.getCsvHeader(), results).build();
  }
}
