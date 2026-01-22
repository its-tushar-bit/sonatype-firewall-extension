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
import com.sonatype.insight.brain.api.v2.dto.ApiProxyServerConfigurationDTO;
import com.sonatype.insight.brain.api.v2.service.ApiProxyServerConfigurationService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.product.license.UnlicensedPath;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.84
 */
@Named
@IqOnlyEndpoint
@Timed
@Path(value = PublicApiPaths.PROXY_SERVER_CONFIG_PATH_V2)
@UnlicensedPath
@Tag(name = "Config Proxy Server",
    description = "Use this REST API to manage the configuration of IQ Server with an existing HTTP proxy server.")
public class ApiProxyServerConfigurationResource
{
  private final ApiProxyServerConfigurationService proxyServerConfigurationService;

  @Inject
  public ApiProxyServerConfigurationResource(
      ApiProxyServerConfigurationService proxyServerConfigurationService)
  {
    this.proxyServerConfigurationService = proxyServerConfigurationService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to inspect an existing HTTP proxy server configuration." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users",
      responses = {
          @ApiResponse(responseCode = "404",
              description = "No HTTP proxy server configuration exists."),
          @ApiResponse(responseCode = "200",
              description = "The response contains:" +
                  "<ul>" +
                  "<li>`hostname` is host name or IP address of the HTTP proxy server to use for outgoing HTTP " +
                  "connections.</li>" +
                  "<li>`port` is the port number for the HTTP proxy server.</li>" +
                  "<li>`username` is the username needed to authenticate with the HTTP proxy server.</li>" +
                  "<li>`password` is always null, never included for security purposes.</li>" +
                  "<li>`passwordIsIncluded` is always FALSE </li>" +
                  "<li>`excludeHosts` is a list of host names that are to be excluded from using the HTTP proxy " +
                  "server.</li>" +
                  "</ul>",
              useReturnTypeSchema = true)
      }
  )

  public ApiProxyServerConfigurationDTO getConfiguration() {
    return proxyServerConfigurationService.getConfiguration();
  }

  @PUT
  @Audited(AuditEvent.CONFIGURE_PROXY)
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to create or update an existing HTTP proxy server configuration." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users",
      responses = {
          @ApiResponse(responseCode = "204",
              description = "HTTP proxy server configuration successful."),
          @ApiResponse(responseCode = "400",
              description = "Missing or invalid values."
          )
      }
  )
  public void setConfiguration(
      @RequestBody(
          description = "The request JSON could include: " +
              "<ul>" +
              "<li>`hostname` is host name or IP address of the HTTP proxy server to use for outgoing HTTP " +
              "connections.</li>" +
              "<li>`port` is the port number for the HTTP proxy server.</li>" +
              "<li>`username` is the username used to authenticate with the HTTP proxy server.</li>" +
              "<li>`password` is the password used for authentication with the HTTP proxy server.</li>" +
              "<li>`passwordIsIncluded` should be `true` if password is included in the request." +
              "<ul>" +
              "<li>If `true` but the password is not included the password will be considered as `null`.</li>" +
              "<li>Can be `false` for update operations that do not a require password change. Note that updating " +
              "the hostname and port requires a password to be provided.</li> " +
              "</ul>" +
              "<li>`excludeHosts` is a list of host names that are to be excluded from using the HTTP proxy " +
              "server.</li>" +
              "</ul>"
      )
      ApiProxyServerConfigurationDTO configurationDTO)
  {
    proxyServerConfigurationService.setConfiguration(configurationDTO);
  }

  @DELETE
  @Audited(AuditEvent.DELETE_PROXY)
  @Operation(description = "Use this method to remove an existing HTTP proxy server configuration." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users",
      responses = {
          @ApiResponse(responseCode = "204",
              description = "HTTP proxy server configuration deleted successfully."),
          @ApiResponse(responseCode = "404",
              description = "No HTTP server configuration was found.")
      })
  public void deleteConfiguration() {
    proxyServerConfigurationService.deleteConfiguration();
  }
}
