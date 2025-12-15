/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.IqOnlyEndpoint;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiCrowdConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiStatusDTO;
import com.sonatype.insight.brain.api.v2.service.ApiCrowdConfigurationService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.error.exception.NotAuthorizedException;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Named
@IqOnlyEndpoint
@Timed
@Path(value = PublicApiPaths.CROWD_CONFIG_RESOURCE_PATH_V2)
@Tag(name = "Config Crowd",
    description = "Use this REST API to manage the configuration of an existing Atlassian Crowd Server that is being " +
        "used to authenticate users for IQ Server.")
public class ApiCrowdConfigurationResource
{
  public static final String TEST_PATH = "test";

  private final ApiCrowdConfigurationService apiCrowdConfigurationService;

  @Inject
  public ApiCrowdConfigurationResource(ApiCrowdConfigurationService apiCrowdConfigurationService) {
    this.apiCrowdConfigurationService = apiCrowdConfigurationService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve the configuration details for the Atlassian Crowd Server." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users",
      responses = {
          @ApiResponse(responseCode = "200",
              description =
                  "The response contains the `serverUrl` and `applicationName` provided " +
                      "at the time of setting up the Crowd Server.",
              useReturnTypeSchema = true),
      })
  public ApiCrowdConfigurationDTO getCrowdConfiguration() {
    checkCrowdEnabled();
    return apiCrowdConfigurationService.getCrowdConfiguration();
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_CROWD)
  @Operation(description = "Use this method to create a new or update an existing Atlassian Crowd Server " +
      "configuration." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users",
      responses = {
          @ApiResponse(responseCode = "204",
              description = "Update or create successful")
      })
  public void insertOrUpdateCrowdConfiguration(
      @RequestBody(description = "The request JSON should include the `serverUrl`, `applicationName`, " +
          "and the `applicationPassword` which will be " +
          "used for authentication against the Atlassian Crowd Server." +
          "\n" +
          "\n" +
          "If updating the `serverUrl`, the `applicationPassword` field is required.")
      ApiCrowdConfigurationDTO crowdConfiguration)
  {
    checkCrowdEnabled();
    apiCrowdConfigurationService.insertOrUpdateCrowdConfiguration(crowdConfiguration);
  }

  @DELETE
  @Audited(AuditEvent.DELETE_CROWD)
  @Operation(description = "Use this method to remove an existing Atlassian Crowd Configuration." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users",
      responses = {
          @ApiResponse(responseCode = "204",
              description = "The Atlassian Crowd Server configuration has been deleted.")
      })
  public void deleteCrowdConfiguration() {
    checkCrowdEnabled();
    apiCrowdConfigurationService.deleteCrowdConfiguration();
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to test a new or an existing Atlassian Crowd Server configuration.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Test performed, results will be in the response message string.",
              useReturnTypeSchema = true)
      })
  @Path(TEST_PATH)
  public ApiStatusDTO testCrowdConfiguration(
      @RequestBody(description = "To test an existing configuration, the request body is not required." +
          "\n" +
          "\n" +
          "To test a new configuration, provide the `serverURl`, `applicationName`, and `applicationPassword` for " +
          "the configuration.")
      ApiCrowdConfigurationDTO dto)
  {
    checkCrowdEnabled();
    return apiCrowdConfigurationService.testCrowdConfiguration(dto);
  }

  private void checkCrowdEnabled() {
    if (!SystemConfigurationPropertyFeature.CROWD_INTEGRATION.isEnabled()) {
      throw new NotAuthorizedException(
          SystemConfigurationPropertyFeature.CROWD_INTEGRATION.getId() + " feature is disabled.");
    }
  }
}
