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
import com.sonatype.insight.brain.api.v2.service.ApiCompositeSourceControlConfigValidatorService;
import com.sonatype.insight.brain.git.ConfigurationValidationResult;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Provides an endpoint for the SCM Validator Service to perform basic validation on a given configuration
 *
 * @since 1.96
 */
@Named
@Timed
@Path(value = PublicApiPaths.COMPOSITE_SOURCE_CONTROL_CONFIG_VALIDATOR_PATH_V2)
@ProductLicenseEnforcementPoint(LicensedFeature.SOURCE_CONTROL)
@Tag(name = "Composite Source Control Validator",
    description = "Use this REST API to validate the composite source control management (SCM) configuration." +
        "\n" +
        "\n" +
        "Composite source control configuration is defined as the configuration that is inherited from the " +
        "parent or is directly assigned.")
public class ApiCompositeSourceControlConfigValidatorResource
{
  private final ApiCompositeSourceControlConfigValidatorService service;

  @Inject
  public ApiCompositeSourceControlConfigValidatorResource(
      ApiCompositeSourceControlConfigValidatorService service)
  {
    this.service = service;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @HasFeature(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED)
  @Operation(description = "Use this method to validate the composite source control configuration." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "The response shows if the composite source control configuration for the application " +
                  "is valid.",
              useReturnTypeSchema = true
          )
      })
  public ConfigurationValidationResult validateSourceControlConfig(
      @Parameter(description = "Enter the applicationId for which you want to validate the composite source " +
          "control configuration.", required = true)
      @PathParam("applicationId") String applicationId)
  {
    return service.validateSourceControlConfig(applicationId);
  }
}
