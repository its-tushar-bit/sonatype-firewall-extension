/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import com.sonatype.insight.brain.api.IqOnlyEndpoint;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Named
@IqOnlyEndpoint
@Path(PublicApiPaths.CONFIG_FEATURES_PATH)
@Tag(name = "Feature Configuration",
    description = "Use this REST API to enable/disable the IQ Server features.")
public class ApiConfigFeaturesResource
{
  public static final String FEATURE = "{feature}";

  private final ApiConfigFeaturesService apiConfigFeaturesService;

  @Inject
  public ApiConfigFeaturesResource(ApiConfigFeaturesService apiConfigFeaturesService) {
    this.apiConfigFeaturesService = apiConfigFeaturesService;
  }

  @POST
  @Audited(AuditEvent.SET_FEATURES)
  @Path(FEATURE)
  @Operation(description = "Use this method to enable an IQ Server feature." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users")
  @ApiResponse(
      responseCode = "204",
      description = "The specified feature has been enabled successfully.")
  @ApiResponse(
      responseCode = "400",
      description = "Bad request, check for invalid feature name.")
  public void enabledFeature(
      @Parameter(description = "Enter the name of the feature to be enabled.",
          required = true) @PathParam("feature") String feature)
  {
    apiConfigFeaturesService.enableFeature(feature);
  }

  @DELETE
  @Audited(AuditEvent.UNSET_FEATURES)
  @Path(FEATURE)
  @Operation(description = "Use this method to disable an IQ Server feature." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users")
  @ApiResponse(
      responseCode = "204",
      description = "The IQ Server feature has been successfully disabled.")
  @ApiResponse(
      responseCode = "400",
      description = "Bad request, check for invalid feature name.")
  public void disableFeature(
      @Parameter(description = "Enter the name of the IQ Server feature to be disabled.",
          required = true) @PathParam("feature") String feature)
  {
    apiConfigFeaturesService.disableFeature(feature);
  }
}
