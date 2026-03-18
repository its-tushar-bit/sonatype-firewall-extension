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
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiPullRequestResults;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlService;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.97
 */
@Named
@Timed
@Path(value = PublicApiPaths.SOURCE_CONTROL_METRICS_PATH_V2)
@ProductLicenseEnforcementPoint(LicensedFeature.SOURCE_CONTROL)
@Tag(name = "Source Control Metrics",
    description = "Use this REST API to view the response times of a source control evaluation.")
public class ApiSourceControlMetricsResource
{
  private static final String OWNER_TYPE = "{ownerType:application|organization}";

  private static final String OWNER_ID = "{internalOwnerId}";

  /* paths are package private for use in tests */
  static final String BY_OWNER = OWNER_TYPE + "/" + OWNER_ID;

  private final ApiSourceControlService sourceControlService;

  @Inject
  public ApiSourceControlMetricsResource(final ApiSourceControlService apiSourceControlService) {
    this.sourceControlService = apiSourceControlService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(BY_OWNER)
  @HasFeature(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED)
  @Operation(description = "Use this method to view the source control pull request metrics." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "The response contains `results` which is a list of elements, each including: " +
                "<ul>" +
                "<li>`startTime` indicates the start time of the pull request.</li>" +
                "<li>`title` indicates the title of the pull request.</li>" +
                "<li>`exceptionThrown` indicates if the pull request caused an exception.</li>" +
                "<li>`successful` indicates if the pull request was successful.</li>" +
                "<li>`totalTime` indicates the total time taken to complete the pull request.</li>" +
                "<li>`reasoning` indicates the summary of the outcome of the pull request.</li>" +
                "</ul>",
            useReturnTypeSchema = true)
      })
  public ApiPullRequestResults getSourceControl(
      @Parameter(
          description = "Select the ownerType for the pull requests.") @PathParam("ownerType") OwnerType ownerType,
      @Parameter(
          description = "Enter the ownerId corresponding to the ownerType.") @PathParam("internalOwnerId") String internalOwnerId)
  {
    return sourceControlService.getSourceControlMetricsForApplication(ownerType, internalOwnerId);
  }
}
