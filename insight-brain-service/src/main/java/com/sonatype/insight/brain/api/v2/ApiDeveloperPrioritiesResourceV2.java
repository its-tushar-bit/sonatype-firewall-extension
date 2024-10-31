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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @since 1.183.0
 */
@Path(ApiDeveloperPrioritiesResourceV2.RESOURCE_PATH)
@Named
@Timed
@Tag(name = "Developer Priorities",
        description =
                "Use this REST API to export Sonatype Developer component priorities data, " +
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
  @Operation(description = "Use this method to retrieve the priorities for the specified application Id and scan Id" +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements ",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "The response field `topPriorities` returns the first 3 prioritized components and the " +
                  "`additionalPriorities` field returns the remaining prioritized components for the specified " +
                  "application Id and scan Id. Each result has all relevant component information, reachability" +
                  " information, policy information, and the priority assigned to it." +
                  "It has pagination support, and the default page size is 10.",
              useReturnTypeSchema = true
          )
      }
  )
  @Produces(MediaType.APPLICATION_JSON)
  public DevelopmentPrioritizationResults getPriorities(
          @PathParam("applicationId") final String applicationId,
          @PathParam("scanId") final String scanId,
          @DefaultValue(DEFAULT_PAGE) @QueryParam("page") final int page,
          @DefaultValue(DEFAULT_PAGE_SIZE) @QueryParam("pageSize") final int pageSize,
          @QueryParam("optionalComponentNameFilter") final String optionalComponentNameFilter
  )
  {
    return developmentPrioritiesService
            .getPrioritizedFindings(applicationId, scanId, page, pageSize, optionalComponentNameFilter);
  }

  @GET
  @Path(EXPORT_PATH)
  @Operation(description = "Use this method to retrieve the priorities for the specified application Id and scan Id" +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements ",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "The response is a CSV that contains all the prioritized components for the specified " +
                  "application Id and scan Id. Each line has all relevant component information, reachability " +
                  "information, policy information, and the priority assigned to it.",
              useReturnTypeSchema = true
          )
      }
  )
  @Produces("text/csv")
  @Audited(AuditEvent.EXPORT_DEVELOPER_PRIORITIES)
  public Response getPrioritiesExport(
          @PathParam("applicationId") final String applicationId,
          @PathParam("scanId") final String scanId)
  {
    List<PrioritizedComponent> results = developmentPrioritiesService.getAllPrioritizedFindings(applicationId, scanId);
    String fileNamePrefix = applicationId + "-" + scanId + "-priorities";
    return Csv.generate(Response.ok(), fileNamePrefix, PrioritizedComponent.getCsvHeader(), results).build();
  }
}
