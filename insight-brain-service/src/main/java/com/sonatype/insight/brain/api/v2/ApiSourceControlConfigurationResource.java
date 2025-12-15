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
import com.sonatype.insight.brain.api.v2.dto.ApiSourceControlConfigurationDTO;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlConfigurationService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.140
 */
@Named
@IqOnlyEndpoint
@Timed
@Path(value = PublicApiPaths.SOURCE_CONTROL_CONFIG_RESOURCE_PATH_V2)
@Tag(name = "Config Source Control",
    description =
        "Use this REST API to manage the configuration of IQ Server with your Source Control Management (SCM) " +
            "system (e.g. GitHub).")
@ProductLicenseEnforcementPoint(LicensedFeature.SOURCE_CONTROL)
public class ApiSourceControlConfigurationResource
{
  private final ApiSourceControlConfigurationService service;

  @Inject
  public ApiSourceControlConfigurationResource(ApiSourceControlConfigurationService service) {
    this.service = service;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @HasFeature(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED)
  @Operation(description = "Use this method to retrieve an existing SCM configuration." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users",
      responses = {
          @ApiResponse(responseCode = "404",
              description = "No SCM configuration found"),
          @ApiResponse(responseCode = "200",
              description = "The response contains: " +
                  "<ul>" +
                  "<li>`cloneDirectory` is the location of the cloned repository that will be used by the IQ server." +
                  " If a relative path is provided, then that path will be created inside the " +
                  " `sonatype-work` directory and your repository will be created within this. A return value " +
                  "`source-control` indicates that this setting is not configured.</li>" +
                  "<li>`gitImplementation` will have the value `java` for JGit or `native` for a native git " +
                  "client.</li>" +
                  "<li>`prCommentPurgeWindow` is the number of days until the comments of a Pull Request (PR) " +
                  "are allowed to be purged.</li>" +
                  "<li>`prEventPurgeWindow` is the number of days until PR events are allowed to be purged.</li>" +
                  "<li>`gitExecutable` is the absolute path to a native client. No value indicates the native git " +
                  "client is on the system path.</li>" +
                  "`gitTimeoutSeconds` is the number of seconds a git command can execute before timing out.</li>" +
                  "`commitUsername` is the username that will be used for the SCM features. The value `NexusIQ` " +
                  "indicates the default value.</li>" +
                  "`commitEmail` is the commit email that will be used for the SCM features." +
                  "`useUsernameInRepositoryCloneUrl` indicates if the username will be added to the URL for the" +
                  " cloned repository. This can be used in conjunction with `commitEmail` to support the " +
                  " 'Verified Committer' feature of Bitbucket.</li>" +
                  "`defaultBranchMonitoringStartTime` has a default value between 00:00 and 00:10. It is the time " +
                  "at which the default branch monitoring will start for the first time.</li>" +
                  "`defaultBranchMonitoringIntervalHours` is the number of hours elapsed between the executions " +
                  "of default branch monitoring by the IQ Server. The default value is 24 hours.</li>" +
                  "<li>`pullRequestMonitoringIntervalSeconds` is the time in seconds between consecutive execution " +
                  "of PR monitoring. The default value is 60 seconds.</li>" +
                  "</ul> ",
              useReturnTypeSchema = true)
      }
  )
  public ApiSourceControlConfigurationDTO getConfiguration() {
    return service.getConfiguration();
  }

  @PUT
  @Audited(AuditEvent.CONFIGURE_SOURCE_CONTROL_CONFIGURATION)
  @Consumes(MediaType.APPLICATION_JSON)
  @HasFeature(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED)
  @Operation(description = "Use this method to set an SCM Configuration with the IQ Server." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users",
      responses = {
          @ApiResponse(responseCode = "204",
              description = "SCM configuration successfully updated.")
      })
  public void setConfiguration(
      @RequestBody(description = "Provide the settings for the SCM configuration as below: " +
          "<ul>" +
          "<li>`cloneDirectory` is the location of the cloned repository that will be used by the IQ server. If a " +
          "relative path is provided, then that path will be created inside the " +
          " `sonatype-work` directory and your repository will be created within this. A return value " +
          "`source-control` indicates that this setting is not configured.</li>" +
          "<li>`gitImplementation` will have the value `java` for JGit or `native` for a native git client.</li>" +
          "<li>`prCommentPurgeWindow` is the number of days until the comments of a Pull Request (PR) " +
          "are allowed to be purged.</li>" +
          "<li>`prEventPurgeWindow` is the number of days until PR events are allowed to be purged.</li>" +
          "<li>`gitExecutable` is the absolute path to a native client. No value indicates the native git client is " +
          "on the system path.</li>" +
          "`gitTimeoutSeconds` is the number of seconds a git command can execute before timing out.</li>" +
          "`commitUsername` is the username that will be used for the SCM features. The value `NexusIQ` indicates " +
          "the default value.</li>" +
          "`commitEmail` is the commit email that will be used for the SCM features." +
          "`useUsernameInRepositoryCloneUrl` indicates if the username will be added to the URL for the cloned" +
          "repository. This can be used in conjunction with `commitEmail` to support the 'Verified Committer' " +
          "feature of Bitbucket.</li>" +
          "`defaultBranchMonitoringStartTime` has a default value between 00:00 and 00:10. It is the time at which " +
          "the default branch monitoring will start for the first time.</li>" +
          "`defaultBranchMonitoringIntervalHours` is the number of hours elapsed between the executions of default " +
          "branch monitoring by the IQ Server. The default value is 24 hours.</li>" +
          "<li>`pullRequestMonitoringIntervalSeconds` is the time in seconds between consecutive execution of PR " +
          "monitoring. The default value is 60 seconds.</li>" +
          "</ul>",
          content = @Content(schema = @Schema(implementation = ApiSourceControlConfigurationDTO.class)))
      JsonNode jsonNode)
  {
    service.setConfiguration(jsonNode);
  }

  @DELETE
  @Audited(AuditEvent.DELETE_SOURCE_CONTROL_CONFIGURATION)
  @HasFeature(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED)
  @Operation(description = "Use this method to delete an existing SCM configuration." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users",
      responses = {
          @ApiResponse(responseCode = "204",
              description = "SCM configuration deleted successfully.")
      })
  public void deleteConfiguration() {
    service.deleteConfiguration();
  }
}
