/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Map;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.banning.BlockIfMultiTenant;
import com.sonatype.insight.brain.product.license.UnlicensedPath;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.138
 */
@Named
@Timed
@Path(value = PublicApiPaths.CONFIG_RESOURCE_PATH_V2)
@Tag(name = "Configuration",
    description = "Use this REST API to configure the IQ Server system properties." +
        "\n" +
        "\n" +
        "We strongly recommend using this REST API instead of config.yml for versions 142 and higher.")
@UnlicensedPath
public class ApiConfigurationResource
{
  private final ApiConfigurationService service;

  @Inject
  public ApiConfigurationResource(ApiConfigurationService service) {
    this.service = service;
  }

  @GET
  @Operation(description = "Use this method to retrieve the configured value for an IQ Server system property." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users or system property dependent",
      responses = {
        @ApiResponse(
            responseCode = "400",
            description = "Bad request, check for invalid property name."),
        @ApiResponse(
            responseCode = "200",
            description = "The response contains all the requested properties and the corresponding values.",
            useReturnTypeSchema = true)
      })
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, Object> getConfiguration(
      @Parameter(
          description = "Enter the names of the system properties. Values provided for name are case-sensitive.") @QueryParam("property") Set<String> properties)
  {
    return service.getConfiguration(properties);
  }

  @PUT
  @Audited(AuditEvent.CONFIGURE_PROPERTIES)
  @Consumes(MediaType.APPLICATION_JSON)
  @BlockIfMultiTenant
  @Operation(description = "Use this method to configure one or more IQ Server system properties. The property names " +
      "are case-sensitive." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users",
      responses = {
        @ApiResponse(
            responseCode = "400",
            description = "Bad request, check for invalid property name."),
        @ApiResponse(
            responseCode = "204",
            description = "The specified IQ server configuration property has been set successfully.")
      })
  public void setConfiguration(
      @RequestBody(description = "Enter the property names and the corresponding values.",
          required = true) Map<String, Object> properties)
  {
    service.setConfiguration(properties);
  }

  @DELETE
  @Operation(description = "Use this method to disable one or more IQ Server system properties. " +
      "The property names are case-sensitive." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users",
      responses = {
        @ApiResponse(
            responseCode = "400",
            description = "Bad request, check for invalid property name."),
        @ApiResponse(
            responseCode = "204",
            description = "The IQ Server system properties specified have been successfully disabled.")
      })
  @Audited(AuditEvent.DELETE_PROPERTIES)
  @BlockIfMultiTenant
  public void deleteConfiguration(
      @Parameter(
          description = "Enter the names of the system properties. Values provided for name are case-sensitive.") @QueryParam("property") Set<String> properties)
  {
    service.deleteConfiguration(properties);
  }
}
