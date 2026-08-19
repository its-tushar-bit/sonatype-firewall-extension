/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.IqOnlyEndpoint;
import com.sonatype.insight.brain.api.v2.dto.SsoConfigurationDTO;
import com.sonatype.insight.brain.api.v2.service.ApiOidcConfigurationService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.198
 */
@Named
@IqOnlyEndpoint
@Timed
@Path(value = PublicApiPaths.OIDC_CONFIG_RESOURCE_PATH_V2)
@Tag(name = "Config OIDC",
    description = "Use this REST API to manage the OIDC configuration for IQ Server.")
public class ApiOidcConfigurationResource
{
  private final ApiOidcConfigurationService apiOidcConfigurationService;

  @Inject
  public ApiOidcConfigurationResource(ApiOidcConfigurationService apiOidcConfigurationService) {
    this.apiOidcConfigurationService = apiOidcConfigurationService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve the OIDC configuration." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users",
      responses = {
        @ApiResponse(responseCode = "404",
            description = "OIDC is not configured."),
        @ApiResponse(
            responseCode = "200",
            description = "The response contains:" +
                "\n" +
                " - `oidcConfiguration` field that contains all the oidc configuration data " +
                "\n" +
                " - `oAuth2Configuration` field that contains the OAuth2 configuration required for oidc",
            useReturnTypeSchema = true)
      })
  public SsoConfigurationDTO getOidcConfiguration() {
    return apiOidcConfigurationService.getOidcConfiguration();
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_OIDC)
  @Operation(
      description = "Use this method to enable SSO using OpenID Connect (OIDC). This request uses the content type " +
          "application/json to transmit the configuration to IQ Server." +
          "\n" +
          "\n" +
          "Permissions required: Edit System Configuration and Users",
      responses = {
        @ApiResponse(
            responseCode = "400",
            description = "Invalid configuration."),
        @ApiResponse(
            responseCode = "204",
            description = "Configuration successful.")
      })
  public void insertOrUpdateOidcConfiguration(SsoConfigurationDTO oidcConfiguration) {
    apiOidcConfigurationService.insertOrUpdateOidcConfiguration(oidcConfiguration);
  }

  @DELETE
  @Audited(AuditEvent.DELETE_OIDC)
  @Operation(description = "Use this method to delete the OIDC configuration." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users",
      responses = {
        @ApiResponse(
            responseCode = "404",
            description = "OIDC is not configured."),
        @ApiResponse(
            responseCode = "204")
      })
  public void deleteOidcConfiguration() {
    apiOidcConfigurationService.deleteOidcConfiguration();
  }
}
