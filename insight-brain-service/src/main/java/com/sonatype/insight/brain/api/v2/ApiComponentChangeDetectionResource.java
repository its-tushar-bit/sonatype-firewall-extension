/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.service.ApiComponentChangeDetectionService;
import com.sonatype.insight.brain.dataaccess.ComponentChangeDetectionConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.ComponentChangeDetectionEventDAO;
import com.sonatype.insight.brain.malware.defense.ApiMalwareComponentEvaluationRequestList.ApiMalwareComponentEvaluationRequest;
import com.sonatype.insight.brain.model.ComponentChangeDetectionConfiguration;
import com.sonatype.insight.brain.model.ComponentChangeDetectionEvent;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@Timed
@ProductLicenseEnforcementPoint(LicensedFeature.FIREWALL)
@HasFeature(SystemConfigurationPropertyFeature.COMPONENT_CHANGE_DETECTION_API)
@Path(PublicApiPaths.COMPONENT_CHANGE_DETECTION_RESOURCE_PATH)
@Tag(name = "Component Change Detection",
    description = "Use this REST API to populate and evaluate the catalog of components " +
        "and retrieve change detection events.")
public class ApiComponentChangeDetectionResource
{
  public static final String CONFIGURATION_PATH = "configuration";

  public static final String EVENT_PATH = "event";

  private final ApiComponentChangeDetectionService apiComponentChangeDetectionService;

  private final ComponentChangeDetectionEventDAO componentChangeDetectionEventDAO;

  Logger log = LoggerFactory.getLogger(ApiComponentChangeDetectionResource.class);

  @Inject
  public ApiComponentChangeDetectionResource(
      final ApiComponentChangeDetectionService apiComponentChangeDetectionService,
      final ComponentChangeDetectionEventDAO componentChangeDetectionEventDAO)
  {
    this.apiComponentChangeDetectionService = apiComponentChangeDetectionService;
    this.componentChangeDetectionEventDAO = componentChangeDetectionEventDAO;
  }

  @POST
  @Path(CONFIGURATION_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  @Operation(
      description = "Add a list of components for component change detection." +
          "<p>" +
          "Use this endpoint to add component configurations for component change detection. " +
          "A list of hash and package URL for each component can be supplied.",
      responses = {
        @ApiResponse(
            responseCode = "204",
            description = "Components added successfully",
            useReturnTypeSchema = true),
        @ApiResponse(
            responseCode = "200",
            description = "The response contains a list of hash and package URL for each component that have " +
                "been removed from the configuration when the maximum number of components has been exceeded.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = ApiMalwareComponentEvaluationRequest.class),
                array = @ArraySchema(schema = @Schema(implementation = ApiMalwareComponentEvaluationRequest.class))))
      })
  public Response addComponents(final List<ApiMalwareComponentEvaluationRequest> components) {
    List<ComponentChangeDetectionConfiguration> componentChangeDetectionConfigurationList =
        buildAndValidateConfigurationList(components);

    List<ApiMalwareComponentEvaluationRequest> result =
        apiComponentChangeDetectionService.addItemsToConfiguration(componentChangeDetectionConfigurationList)
            .stream()
            .map(config -> new ApiMalwareComponentEvaluationRequest(config.getComponentHash(), config.getPurl()))
            .collect(Collectors.toList());

    if (result.isEmpty()) {
      return Response.noContent().build();
    }
    return Response.ok(result).build();
  }

  private List<ComponentChangeDetectionConfiguration> buildAndValidateConfigurationList(
      final List<ApiMalwareComponentEvaluationRequest> components)
  {
    List<ComponentChangeDetectionConfiguration> componentChangeConfigurationList = new ArrayList<>();
    Map<String, String> malFormedUrls = new HashMap<>();

    for (ApiMalwareComponentEvaluationRequest component : components) {
      try {
        PackageURL packageURL = new PackageURL(component.packageUrl);
        checkPythonPackageUrl(packageURL);
        componentChangeConfigurationList.add(
            new ComponentChangeDetectionConfiguration(
                ComponentChangeDetectionConfigurationDAO.COMPONENT_CHANGE_DETECTION_VERSION, component.packageUrl,
                component.hash, null, new Date()));
      }
      catch (MalformedPackageURLException e) {
        malFormedUrls.put(component.packageUrl, e.getMessage());
      }
    }

    if (!malFormedUrls.isEmpty()) {
      log.warn("Malformed URLs were found during the load of data: {} ", malFormedUrls);
    }

    return componentChangeConfigurationList;
  }

  @GET
  @Path(EVENT_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  @Operation(
      description = "The response contains a list of component change detection events." +
          "<p>" +
          "Use this endpoint to get a list of component change detection events.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Component change detection events.",
            useReturnTypeSchema = true)
      })
  public List<ComponentChangeDetectionEvent> getComponentChangeDetectionEvents() {
    return componentChangeDetectionEventDAO.getAll();
  }

  @POST
  @Path(EVENT_PATH)
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  @Operation(
      description = "Delete component change detection events older than the provided timestamp." +
          "<p>" +
          "Use this endpoint to delete component change detection events that have a timestamp " +
          "older than the provided timestamp.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Events deleted successfully")
      })
  public Response acknowledgeEventsOlderThan(
      @Parameter(description = "Enter the timestamp in `yyyy-MM-dd'T'HH:mm:ss'Z'` format",
          required = true) @QueryParam("timestamp") String timestamp)
  {
    apiComponentChangeDetectionService.acknowledgeEventsOlderThan(parse(timestamp));
    return Response.ok().build();
  }

  private Date parse(String timestamp) {
    if (timestamp == null) {
      return null;
    }
    try {
      return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(timestamp);
    }
    catch (ParseException e) {
      throw new BadRequestException(
          String.format("Could not parse: %s. Expected format is: yyyy-MM-dd'T'HH:mm:ss'Z'.", timestamp));
    }
  }

  private void checkPythonPackageUrl(final PackageURL packageURL) throws MalformedPackageURLException {
    if (packageURL.getType().equals("pypi")) {
      if (packageURL.getQualifiers() == null || !packageURL.getQualifiers().containsKey("filename")) {
        throw new MalformedPackageURLException("Python package URL must have filename qualifier");
      }
    }
  }
}
