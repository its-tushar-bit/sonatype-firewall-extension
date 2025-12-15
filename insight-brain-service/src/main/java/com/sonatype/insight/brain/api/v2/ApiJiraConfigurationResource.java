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
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.IqOnlyEndpoint;
import com.sonatype.insight.brain.api.v2.dto.ApiJiraConfigurationDTO;
import com.sonatype.insight.brain.api.v2.service.ApiJiraConfigurationService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.139
 */
@Named
@IqOnlyEndpoint
@Timed
@Path(value = PublicApiPaths.JIRA_CONFIG_RESOURCE_PATH_V2)
@Tag(name = "Config Jira",
    description = "Use this REST API to manage Jira configurations to receive notifications from Lifecycle." +
        "\n" +
        "\n" +
        "It is supported for Jira Cloud, Jira Server, and Jira Data Center.")
public class ApiJiraConfigurationResource
{
  private final ApiJiraConfigurationService service;

  @Inject
  public ApiJiraConfigurationResource(ApiJiraConfigurationService service) {
    this.service = service;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve an existing configuration for Jira." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users",
      responses = {
          @ApiResponse(responseCode = "404",
              description = "No saved Jira configuration exists."),
          @ApiResponse(responseCode = "200",
              description = "The response contains:" +
                  "<ol>" +
                  "<li>`url` is the Jira server address.</li>" +
                  "<li>`username` is the username used to connect to the Jira server.</li>" +
                  "<li>`password` is the password used to authenticate on the Jira server.</li>" +
                  "<li>`customFields` are any project issue type required fields defined in Jira.</li>" +
                  "</ol>",
              useReturnTypeSchema = true)
      }
  )
  public ApiJiraConfigurationDTO getConfiguration() {
    return service.getConfiguration();
  }

  @PUT
  @Audited(AuditEvent.CONFIGURE_JIRA)
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to set a Jira configuration. If a Jira configuration already exists, " +
      "the values will be updated with the ones provided here. If the server URL is being changed, then the " +
      "password (if any) will be required." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users",
      responses = {
          @ApiResponse(
              responseCode = "204",
              description = "Set Jira configuration successfully."
          ),
      })
  public void setConfiguration(
      @RequestBody(description = "Enter the Jira configuration details here.",
          content = @Content(schema = @Schema(implementation = ApiJiraConfigurationDTO.class)))
      JsonNode jsonNode)
  {
    service.setConfiguration(jsonNode);
  }

  @DELETE
  @Audited(AuditEvent.DELETE_JIRA)
  @Operation(description = "Use this method to delete a Jira configuration." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users",
      responses = {
          @ApiResponse(
              responseCode = "204",
              description = "Jira configuration deleted."
          )
      }
  )
  public void deleteConfiguration() {
    service.deleteConfiguration();
  }
}
