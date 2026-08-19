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
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiMailConfigurationDTO;
import com.sonatype.insight.brain.api.v2.service.ApiMailConfigurationService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.83
 */
@Named
@Timed
@Path(value = PublicApiPaths.MAIL_CONFIG_RESOURCE_PATH_V2)
@Tag(name = "Config Mail",
    description = "Use this REST API to manage the configuration of an SMTP server, to receive email notifications.")
public class ApiMailConfigurationResource
{
  private final ApiMailConfigurationService mailConfigurationService;

  static final String TEST_CONFIGURATION = "test/{recipientEmail}";

  @Inject
  public ApiMailConfigurationResource(ApiMailConfigurationService mailConfigurationService) {
    this.mailConfigurationService = mailConfigurationService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to review the configuration for an SMTP server." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users")
  @ApiResponse(
      responseCode = "404",
      description = "No SMTP server is currently configured with the IQ Server.")
  @ApiResponse(
      responseCode = "200",
      description = "The response contains:" +
          "<ul>" +
          "<li>`hostname` is the hostname or IP address of the SMTP server used for outgoing mail.</li>" +
          "<li>`port` is the port number on which the SMTP server accepts email requests.</li>" +
          "<li>`username` is the username to authenticate users on the SMTP server.</li>" +
          "<li>`password` is always null, never included for security purposes for this method.</Li>" +
          "<li>`passwordIsIncluded` is always FALSE for this method.</li>" +
          "<li>`sslEnabled` is a boolean flag indicating if the connection to the SMTP server should use SSL/TLS " +
          "right from the start.</li>" +
          "<li>`startIsEnabled` is a boolean flag indicating if the connection to the SMTP server should attempt to " +
          "upgrade to SSL/TLS using the STARTTLS command.</li>" +
          "<li>`systemEmail` is the email address used for the FROM header in emails sent by the IQ Server.</li>" +
          "</ul>",
      useReturnTypeSchema = true)
  public ApiMailConfigurationDTO getConfiguration() {
    return mailConfigurationService.getConfiguration();
  }

  @PUT
  @Audited(AuditEvent.CONFIGURE_MAIL)
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to configure or update an existing SMTP server configuration." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users")
  @ApiResponse(
      responseCode = "400",
      description = "Missing or invalid values provided.")
  @ApiResponse(
      responseCode = "204",
      description = "SMTP server configuration was updated successfully.",
      useReturnTypeSchema = true)

  public void setConfiguration(
      @RequestBody(description = "Provide one or more values for the following in the JSON payload:" +
          "<ul>" +
          "<li>`hostname` - is the hostname or IP address of the SMTP server used for outgoing mail.</li>" +
          "<li>`port` - is the port number on which the SMTP server accepts email requests.</li>" +
          "<li>`password` - depends upon the value of `passwordIsIncluded`.</li>" +
          "<li>`passwordIsIncluded` - if set to true, value must be provided for `password`, null is allowed." +
          "If set to false, the previous value will remain unchanged, provided that `hostname` and `port` are not " +
          "changed." +
          "<li>`sslEnabled` - is a boolean flag indicating if the connection to the SMTP server should use SSL/TLS" +
          "right from the start.</li>" +
          "<li>`startIsEnabled`- is a boolean flag indicating if the connection to the SMTP server should attempt to" +
          "upgrade to SSL/TLS using the STARTTLS command." +
          "<li>`systemEmail` - is the email address used for the FROM header in emails sent by the IQ Server.</li>" +
          "</ul>",
          useParameterTypeSchema = true) ApiMailConfigurationDTO configurationDTO)
  {
    mailConfigurationService.setConfiguration(configurationDTO);
  }

  @DELETE
  @Audited(AuditEvent.DELETE_MAIL)
  @Operation(description = "Use this method to disable or remove an SMTP configuration." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users")
  @ApiResponse(
      responseCode = "204",
      description = "SMTP configuration was removed successfully.",
      useReturnTypeSchema = true)
  @ApiResponse(
      responseCode = "404",
      description = "SMTP server configuration does not exist.",
      useReturnTypeSchema = true)
  public void deleteConfiguration() {
    mailConfigurationService.deleteConfiguration();
  }

  @POST
  @Path(TEST_CONFIGURATION)
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to test an SMTP configuration, without affecting the current " +
      "configuration." +
      "\n" +
      "\n" +
      "Permissions required:Edit System Configuration and Users")
  @ApiResponse(
      responseCode = "204",
      description = "The test email was sent successfully.")
  @ApiResponse(
      responseCode = "400",
      description = "Missing or invalid values provided.")
  public void testConfiguration(
      @Parameter(
          description = "Enter the test recipient email address.") @PathParam("recipientEmail") String recipientEmail,
      @RequestBody(description = "Provide one or more values for the following in the JSON payload:" +
          "<ul>" +
          "<li>`hostname` - is the hostname or IP address of the SMTP server used for outgoing mail.</li>" +
          "<li>`port` - is the port number on which the SMTP server accepts email requests.</li>" +
          "<li>`password` - depends upon the value of `passwordIsIncluded`.</li>" +
          "<li>`passwordIsIncluded` - if set to true, value must be provided for `password`, null is allowed." +
          "If set to false, the previous value will remain unchanged, provided that `hostname` and `port` are not " +
          "changed." +
          "<li>`sslEnabled` - is a boolean flag indicating if the connection to the SMTP server should use SSL/TLS" +
          "right from the start.</li>" +
          "<li>`startIsEnabled`- is a boolean flag indicating if the connection to the SMTP server should attempt to" +
          "upgrade to SSL/TLS using the STARTTLS command." +
          "<li>`systemEmail` - is the email address used for the FROM header in emails sent by the IQ Server.</li>" +
          "</ul>",
          useParameterTypeSchema = true) ApiMailConfigurationDTO configurationDTO)
  {
    mailConfigurationService.testConfiguration(recipientEmail, configurationDTO);
  }
}
