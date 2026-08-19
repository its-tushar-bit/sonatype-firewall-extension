/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDetailsRequestDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDetailsResultDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiComponentDetailsServiceV2;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.16.0
 */
@Named
@Timed
@Path(PublicApiPaths.COMPONENT_DETAILS_PATH_V2)
@Tag(name = "Components",
    description = "Use this REST API to retrieve a component's security vulnerability data, license data, age and " +
        "popularity.")
@ProductLicenseEnforcementPoint(LicensedFeature.COMPONENT_EVALUATION)
public class ApiComponentDetailsResourceV2
{
  private final ApiComponentDetailsServiceV2 componentDetailsService;

  @Inject
  public ApiComponentDetailsResourceV2(final ApiComponentDetailsServiceV2 componentDetailsService) {
    this.componentDetailsService = componentDetailsService;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve data related to a component.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "The response contains a detailed description of the component. The hash value returned here "
                +
                "is truncated and not intended to be used as a checksum. It can be used as an identifier " +
                "to pass to other REST API calls.",
            useReturnTypeSchema = true)
      })
  public ApiComponentDetailsResultDTOV2 getComponentDetails(
      @Parameter(description = "You can retrieve component data in any one of the 3 ways via:\n" +
          "1. Component identifier\n" +
          "2. Package URL\n" +
          "3. Hash",
          required = true) ApiComponentDetailsRequestDTOV2 componentDetailsRequest)
  {
    return componentDetailsService.getComponentDetails(componentDetailsRequest);
  }
}
