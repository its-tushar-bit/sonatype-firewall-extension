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
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.brain.zscaler.ApiZScalerConfigurationDTO;
import com.sonatype.insight.brain.zscaler.ApiZScalerConfigurationService;
import com.sonatype.insight.brain.zscaler.ApiZScalerService;
import com.sonatype.insight.brain.zscaler.ApiZScalerService.ApiZScalerQuotaDTO;
import com.sonatype.insight.brain.zscaler.ZScalerSupportedFormat;
import com.sonatype.insight.brain.zscaler.ZScalerUpdater;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Named
@Timed
@Path(value = PublicApiPaths.ZSCALER_CONFIG_RESOURCE_PATH_V2)
@HasFeature(SystemConfigurationPropertyFeature.ZSCALER)
@Tag(name = "Config Zscaler",
    description = "Use this REST API to manage the configuration of a Zscaler service.")
@ProductLicenseEnforcementPoint(LicensedFeature.FIREWALL)
public class ApiZScalerConfigurationResource
{
  private final ApiZScalerConfigurationService zScalerConfigurationService;

  private final ZScalerUpdater zScalerUpdater;

  private final ApiZScalerService zScalerService;

  @Inject
  public ApiZScalerConfigurationResource(
      final ApiZScalerConfigurationService zScalerConfigurationService,
      final ZScalerUpdater zScalerUpdater,
      final ApiZScalerService zScalerService)
  {
    this.zScalerConfigurationService = zScalerConfigurationService;
    this.zScalerUpdater = zScalerUpdater;
    this.zScalerService = zScalerService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to review the configuration for a Zscaler server." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users")
  @ApiResponse(
      responseCode = "404",
      description = "No Zscaler server is currently configured with the IQ Server."
  )
  @ApiResponse(
      responseCode = "200",
      description = "The response contains:" +
          "<ul>" +
          "<li>`username` is the username to authenticate with the Zscaler server.</li>" +
          "<li>`password` is always null, never included for security purposes for this method.</Li>" +
          "<li>`hostname` is the hostname or IP address of the Zscaler server.</li>" +
          "<li>`apiKey` is the apiKey used for communicating with the Zscaler service.</li>" +
          "</ul>",
      useReturnTypeSchema = true
  )
  public ApiZScalerConfigurationDTO getConfiguration() {
    return zScalerConfigurationService.getConfiguration();
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.TEXT_HTML)
  @Operation(description = "Use this method to configure or update an existing Zscaler server configuration." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users")
  @ApiResponse(
      responseCode = "400",
      description = "Missing or invalid values provided."
  )
  @ApiResponse(
      responseCode = "204",
      description = "Zscaler server configuration was updated successfully.",
      useReturnTypeSchema = true
  )
  public String setConfiguration(
      @RequestBody(description = "Provide one or more values for the following in the JSON payload:" +
          "<ul>" +
          "<li>`username` - is the username for the Zscaler server.</li>" +
          "<li>`password` - is the password for the Zscaler server.</li>" +
          "<li>`hostname` - is the hostname or IP address of the Zscaler server.</li>" +
          "<li>`apiKey` - is the apiKey for the Zscaler Server.</li>" +
          "<li>`eulaAgreed` - is the agreement to the Sonatype's end user license agreement.</li>" +
          "<li>`mavenFormatEnabled` - is the flag to enable or disable the Maven format for Zscaler.</li>" +
          "<li>`npmFormatEnabled` - is the flag to enable or disable the Npm format for Zscaler.</li>" +
          "<li>`pypiFormatEnabled` - is the flag to enable or disable the Pypi format for Zscaler.</li>" +
          "<li>`nugetFormatEnabled` - is the flag to enable or disable the Nuget format for Zscaler.</li>" +
          "</ul>",
          useParameterTypeSchema = true)
      ApiZScalerConfigurationDTO configurationDTO)
  {
    return zScalerConfigurationService.setConfiguration(configurationDTO);
  }

  @DELETE
  @Operation(description = "Use this method to disable or remove the Zscaler configuration." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users")
  @ApiResponse(
      responseCode = "204",
      description = "Zscaler configuration was removed successfully.",
      useReturnTypeSchema = true
  )
  @ApiResponse(
      responseCode = "404",
      description = "Zscaler server configuration does not exist.",
      useReturnTypeSchema = true
  )
  public void deleteConfiguration() {
    zScalerConfigurationService.deleteConfiguration();
  }

  @POST
  @Path("testConfig")
  @Operation(description = "Use this method to test Zscaler server configuration." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users")
  @ApiResponse(
      responseCode = "400",
      description = "Missing or invalid values provided."
  )
  @ApiResponse(
      responseCode = "204",
      description = "Test Zscaler server configuration was successful."
  )
  public void testConfiguration(
      @RequestBody(description = "Provide one or more values for the following in the JSON payload:" +
          "<ul>" +
          "<li>`username` - is the username for the Zscaler server.</li>" +
          "<li>`password` - is the password for the Zscaler server.</li>" +
          "<li>`hostname` - is the hostname or IP address of the Zscaler server.</li>" +
          "<li>`apiKey` - is the apiKey for the Zscaler Server.</li>" +
          "</ul>",
          useParameterTypeSchema = true)
      ApiZScalerConfigurationDTO configurationDTO)
  {
    zScalerService.authenticateAndValidatePermissions(configurationDTO.getHostname(),
        configurationDTO.getUsername(), configurationDTO.getPassword(), configurationDTO.getApiKey());
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("zscalerLimits")
  @Operation(
      description = "Returns the current Zscaler quota." +
          "\n" +
          "\n" +
          "Permissions required: None"
  )
  @ApiResponse(
      responseCode = "200",
      description = "Successfully retrieved the Zscaler quota.",
      useReturnTypeSchema = true
  )
  public ApiZScalerQuotaDTO getQuota() {
    return zScalerService.getQuota();
  }

  @POST
  @Path("/update/{format}")
  @Operation(description = "Use this endpoint to trigger an update to your Zscaler instance" +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users")
  @ApiResponse(
      responseCode = "204",
      description = "Zscaler updated custom urls for format.",
      useReturnTypeSchema = true
  )
  @ApiResponse(
      responseCode = "404",
      description = "Zscaler unable to update custom urls for format.",
      useReturnTypeSchema = true
  )
  public void triggerUpdate(@PathParam("format") ZScalerSupportedFormat format) {
    zScalerUpdater.updateZScalerMaliciousCategory(format);
  }

  @POST
  @Path("/update")
  @Operation(description = "Use this endpoint to trigger an update of all configured formats to your Zscaler instance" +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users")
  @ApiResponse(
      responseCode = "204",
      description = "Zscaler updated custom urls successfully.",
      useReturnTypeSchema = true
  )
  @ApiResponse(
      responseCode = "404",
      description = "Zscaler unable to update custom urls",
      useReturnTypeSchema = true
  )
  public void triggerUpdateAll() {
    zScalerUpdater.updateAllZScalerMaliciousCategories();
  }

  @DELETE
  @Path("/update")
  @Operation(description = "Use this endpoint to delete all configured formats from your Zscaler instance" +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users")
  @ApiResponse(
      responseCode = "204",
      description = "Zscaler deleted custom urls.",
      useReturnTypeSchema = true
  )
  @ApiResponse(
      responseCode = "404",
      description = "Zscaler unable to remove custom urls",
      useReturnTypeSchema = true
  )
  public void deleteAllCategories() {
    zScalerUpdater.deleteAllZScalerMaliciousUrlCategories();
  }

  @DELETE
  @Path("/update/{format}")
  @Operation(description = "Use this endpoint to delete a specific format from your Zscaler instance" +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users")
  @ApiResponse(
      responseCode = "204",
      description = "Zscaler updating a format was successful.",
      useReturnTypeSchema = true
  )
  @ApiResponse(
      responseCode = "404",
      description = "Zscaler updating a format was not successful",
      useReturnTypeSchema = true
  )
  public void deleteCategory(@PathParam("format") ZScalerSupportedFormat format) {
    zScalerUpdater.deleteZScalerMaliciousUrlCategory(format);
  }
}
