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
import com.sonatype.insight.brain.api.v2.dto.ApiReverseProxyAuthenticationConfigurationDTO;
import com.sonatype.insight.brain.api.v2.service.ApiReverseProxyAuthenticationConfigurationService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.138
 */
@Named
@Timed
@Path(value = PublicApiPaths.REVERSE_PROXY_AUTHENTICATION_CONFIG_RESOURCE_PATH_V2)
@Tag(name = "Config Reverse Proxy Authentication",
    description = "Use this REST API to manage the configuration of a reverse proxy server.")
public class ApiReverseProxyAuthenticationConfigurationResource
{
  private final ApiReverseProxyAuthenticationConfigurationService service;

  @Inject
  public ApiReverseProxyAuthenticationConfigurationResource(
      ApiReverseProxyAuthenticationConfigurationService service)
  {
    this.service = service;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to inspect an existing reverse proxy server configuration." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users",
      responses = {
          @ApiResponse(responseCode = "404",
              description = "No reverse proxy server configuration exists."),
          @ApiResponse(responseCode = "200",
              description = "The response contains:" +
                  "<ul>" +
                  "<li>`enabled` indicates if the configuration is enabled.</li>" +
                  "<li>`usernameHeader` is the name of the HTTP request header field that contains the username. " +
                  "The default value is `REMOTE_USER`.</li>" +
                  "<li>`csrfProtectionDisabled` indicates if Cross-Site Request Forgery (CSRF) protection is " +
                  "disabled. Used for backward compatibility with old client plugins.</li>" +
                  "<li>`logoutUrl` is the redirect URL when a user logs out. If set to `null` the " +
                  "user will not be redirected.</li>" +
                  "</ul>",
              useReturnTypeSchema = true)
      }
  )
  public ApiReverseProxyAuthenticationConfigurationDTO getConfiguration() {
    return service.getConfiguration();
  }

  @PUT
  @Audited(AuditEvent.CONFIGURE_REVERSE_PROXY_AUTHENTICATION)
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to configure the reverse proxy server." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users",
      responses = {
          @ApiResponse(responseCode = "204",
              description = "Reverse proxy server configuration successful."),
          @ApiResponse(responseCode = "400",
              description = "Missing or invalid values."
          )
      }
  )
  public void setConfiguration(
      @RequestBody(
          description = "The request JSON could include: " +
              "<ul>" +
              "<li>`enabled` indicates if the configuration is enabled.</li>" +
              "<li>`usernameHeader` is the name of the HTTP request header field that contains the username. " +
              "The default value is `REMOTE_USER`.</li>" +
              "<li>`csrfProtectionDisabled` indicates if Cross-Site Request Forgery (CSRF) protection is " +
              "disabled. Used for backward compatibility with old client plugins.</li>" +
              "<li>`logoutUrl` is the redirect URL when a user logs out. If set to `null` the " +
              "user will not be redirected.</li>" +
              "</ul>"
      )
      ApiReverseProxyAuthenticationConfigurationDTO dto)
  {
    service.setConfiguration(dto);
  }

  @DELETE
  @Operation(description = "Use this method to remove an existing reverse proxy server configuration." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users",
      responses = {
          @ApiResponse(responseCode = "204",
              description = "Reverse proxy server configuration deleted successfully.")
      })
  @Audited(AuditEvent.DELETE_REVERSE_PROXY_AUTHENTICATION)
  public void deleteConfiguration() {
    service.deleteConfiguration();
  }
}
