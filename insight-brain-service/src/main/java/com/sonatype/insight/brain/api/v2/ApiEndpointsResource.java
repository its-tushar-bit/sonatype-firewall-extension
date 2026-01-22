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
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiType;
import com.sonatype.insight.brain.api.v2.service.ApiEndpointsService;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.143.0
 */
@Named
@Timed
@Path(PublicApiPaths.ENDPOINTS_RESOURCE_PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.API_PAGE)
@Tag(name = "Endpoints",
    description = "This REST API returns the OpenAPI documentation for the specified IQ Server REST API.")
@HasFeature(SystemConfigurationPropertyFeature.API_PAGE)
public class ApiEndpointsResource
{
  public static final String ENDPOINT_TYPE_RESOURCE_PATH = "{apiType: public|experimental}";

  private final ApiEndpointsService apiEndpointsService;

  @Inject
  public ApiEndpointsResource(final ApiEndpointsService apiEndpointsService) {
    this.apiEndpointsService = apiEndpointsService;
  }

  @GET
  @Path(ENDPOINT_TYPE_RESOURCE_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description =
      "Use this method to retrieve the OpenAPI documentation for the specified type of IQ Server " +
          "REST API.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "The response contains the OpenAPI documentation.",
              useReturnTypeSchema = true)
      })
  public String getOpenAPI(
      @Context final Application application,
      @Parameter(description = "Select the type of the API." +
          "<ul>" +
          "<li> `public` APIs are Generally Available and fully supported by Sonatype.</li>" +
          "<li> `experimental` APIs are not production " +
          "ready, may change, and are not intended to be used in critical workloads.</li>" +
          "</ul>")
      @PathParam("apiType") final ApiType apiType)
  {
    return apiEndpointsService.getOpenAPI(application, apiType);
  }
}
