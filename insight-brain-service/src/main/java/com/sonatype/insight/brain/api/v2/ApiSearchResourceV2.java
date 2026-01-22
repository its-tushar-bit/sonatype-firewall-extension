/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiSearchResultsDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiSearchServiceV2;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Enables end users to search for components within their applications. This REST API is exposed directly to users.
 *
 * @since 1.13.0
 */
@Path(PublicApiPaths.SEARCH_RESOURCE_PATH_V2)
@Named
@Timed
@ProductLicenseEnforcementPoint(LicensedFeature.COMPONENT_SEARCH)
@Tag(name = "Component Search",
    description = "Use this REST API to search for components in application evaluation reports.")
public class ApiSearchResourceV2
{
  private final ApiSearchServiceV2 searchService;

  @Inject
  public ApiSearchResourceV2(final ApiSearchServiceV2 searchService) {
    this.searchService = searchService;
  }

  /**
   * Searches all currently registered applications for a component matching the given search criteria. A component can
   * be searched for by its hash or its coordinates (or its equivalent packageUrl format), the latter supporting wild
   * cards like the equivalent policy condition. The mandatory stageId parameter restricts which scans/reports of the
   * applications are inspected for the component.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.SEARCH_COMPONENT_USES)
  @Operation(description = "Use this method to retrieve the component details from the application " +
      "evaluation reports by specifying the component search parameters, format and evaluation stage. " +
      "You can specify the component search parameters in any one of the 3 ways:" +
      "<ul>" +
      "<li>SHA1 hash of the component</li>" +
      "<li>Component identifier object containing the coordinates of the component and its format</li>" +
      "<li>packageUrl string</li>" +
      "</ul>" +
      "Use of wildcards when searching using the GAVEC(coordinates) is supported." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "The response contains (a) criteria (the search criteria in the request), and " +
                  "(b) results (list of applications with the component specified)." +
                  "\n" +
                  "\n" +
                  "Each result includes applicationId and application name containing the component, the relative " +
                  "and absoluteURLs of the report, component " +
                  "metadata, threat level, and dependency data indicating if the component is a " +
                  "direct/transitive/InnerSource dependency.",
              useReturnTypeSchema = true)
      })
  public ApiSearchResultsDTOV2 searchComponent(
      @Parameter(description = "Specify the evaluation report stage.", required = true)
      @QueryParam("stageId") String stageId,
      @Parameter(description = "Enter the component hash.")
      @QueryParam("hash") String hash,
      @Parameter(description = "Specify the componentIdentifier object containing the format and coordinates.")
      @QueryParam("componentIdentifier") ComponentIdentifier componentIdentifier,
      @Parameter(description = "Enter the packageUrl.")
      @QueryParam("packageUrl") String packageUrl)
  {
    return searchService.searchComponent(stageId, hash, componentIdentifier, packageUrl);
  }
}
