/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.codahale.metrics.annotation.Timed;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiWaiverExpirationNotificationConfigDTO;
import com.sonatype.insight.brain.api.v2.service.ApiWaiverExpirationNotificationConfigService;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST resource for waiver expiration notification configuration.
 */
@Named
@Timed
@Path(PublicApiPaths.WAIVER_EXPIRATION_NOTIFICATION_CONFIG_PATH_V2)
@Tag(name = "Waiver Expiration Notification Config",
    description = "Configure when and to whom notifications are sent before waivers expire.")
public class ApiWaiverExpirationNotificationConfigResource
{
  private final ApiWaiverExpirationNotificationConfigService service;

  @Inject
  public ApiWaiverExpirationNotificationConfigResource(
      final ApiWaiverExpirationNotificationConfigService service)
  {
    this.service = service;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      description = "Returns the effective waiver expiration notification configuration for the given owner. " +
          "If the owner has no custom configuration, the inherited configuration from the parent is returned " +
          "with inheritConfig=true." +
          "<p>" +
          "Permissions required: View IQ Elements",
      responses = {
        @ApiResponse(responseCode = "200", description = "The effective notification configuration.",
            useReturnTypeSchema = true)
      })
  public ApiWaiverExpirationNotificationConfigDTO getConfig(
      @Parameter(description = "The owner type (organization, repository_manager, or repository_container).",
          required = true) @PathParam("ownerType") final String ownerType,
      @Parameter(description = "The internal owner ID assigned by IQ Server.",
          required = true) @PathParam("ownerId") final String ownerId)
  {
    return service.getConfig(ownerId);
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_WAIVER_EXPIRATION_NOTIFICATION)
  @Operation(
      description = "Sets the waiver expiration notification configuration for the given owner. " +
          "Set inheritConfig=true to inherit the configuration from the parent." +
          "<p>" +
          "Permissions required: Edit IQ Elements",
      responses = {
        @ApiResponse(responseCode = "204", description = "Configuration saved successfully.")
      })
  public void saveConfig(
      @Parameter(description = "The owner type (organization, repository_manager, or repository_container).",
          required = true) @PathParam("ownerType") final String ownerType,
      @Parameter(description = "The internal owner ID assigned by IQ Server.",
          required = true) @PathParam("ownerId") final String ownerId,
      @RequestBody(description = "The notification configuration to save.",
          required = true) final ApiWaiverExpirationNotificationConfigDTO dto)
  {
    AuditData.get().setData("waiverExpirationNotificationConfig", dto);
    service.saveConfig(ownerId, dto);
  }
}
