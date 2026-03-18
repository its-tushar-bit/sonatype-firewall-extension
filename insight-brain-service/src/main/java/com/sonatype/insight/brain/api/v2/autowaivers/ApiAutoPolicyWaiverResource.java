/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.autowaivers;

import java.util.List;
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
import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverStatusDTO;
import com.sonatype.insight.brain.api.v2.service.autowaivers.ApiAutoPolicyWaiverService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

//This resource is behind a feature flag, so we need to check if the feature is enabled before using it
@Named
@Timed
@Path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH)
@Tag(name = "Auto Policy Waivers",
    description = "Use this REST API to create, modify and retrieve auto policy waivers.")
@ProductLicenseEnforcementPoint(LicensedFeature.DEVELOPER_DASHBOARD)
public class ApiAutoPolicyWaiverResource
{
  private final ApiAutoPolicyWaiverService apiAutoPolicyWaiverService;

  static final String OWNERS_PATH = "{ownerType: application|organization}/{ownerId}";

  static final String AUTO_WAIVER_STATUS_PATH = OWNERS_PATH + "/status";

  static final String BY_AUTO_POLICY_WAIVER_ID_PATH = OWNERS_PATH + "/{autoPolicyWaiverId}";

  static final String APPLICABLE_WAIVERS_PATH = "/v2/" + OWNERS_PATH + "/applicableAutoWaivers";

  @Inject
  public ApiAutoPolicyWaiverResource(ApiAutoPolicyWaiverService apiAutoPolicyWaiverService) {
    this.apiAutoPolicyWaiverService = apiAutoPolicyWaiverService;
  }

  @GET
  @Path(BY_AUTO_POLICY_WAIVER_ID_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      description = "Use this method to retrieve auto policy waiver details for the autoPolicyWaiverId specified." +
          "\n" +
          "\n" +
          "Permissions required: View IQ Elements",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "The response contains waiver details corresponding to the auto policy waiver id specified.",
            useReturnTypeSchema = true)
      })
  public ApiAutoPolicyWaiverDTO getAutoPolicyWaiver(
      @Parameter(description = "Enter the ownerType to specify the scope. The response will contain the details " +
          "for waivers within the scope.", required = true) @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the corresponding id for the ownerType specified above.",
          required = true) @PathParam("ownerId") String ownerId,
      @Parameter(
          description = "Enter the autoPolicyWaiverId for which you want to retrieve the auto policy waiver details.",
          required = true) @PathParam("autoPolicyWaiverId") String autoPolicyWaiverId)
  {
    return apiAutoPolicyWaiverService.getAutoPolicyWaiver(ownerType, ownerId, autoPolicyWaiverId);
  }

  @GET
  @Path(OWNERS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve waiver details for all auto policy waivers for the " +
      "scope specified. You can specify the scope by using the parameters ownerType and ownerId." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "The response contains waiver details for the specified ownerType and the corresponding " +
                "ownerId, grouped by the autoPolicyWaiverId.",
            useReturnTypeSchema = true)
      })
  public List<ApiAutoPolicyWaiverDTO> getAutoPolicyWaivers(
      @Parameter(description = "Enter the ownerType to specify the scope. The response will contain " +
          "waivers that are within the scope specified.", required = true) @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the corresponding id for the ownerType specified above.",
          required = true) @PathParam("ownerId") String ownerId)
  {
    return apiAutoPolicyWaiverService.getAutoPolicyWaivers(ownerType, ownerId);
  }

  @POST
  @Path("/v2/" + OWNERS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_AUTO_WAIVER)
  @Operation(
      description = "Use this method to create an auto policy waiver configuration. Only three configurations can " +
          " exist at a time for a given application or organization. With different combinations for" +
          " reachable/pathForward" +
          "\n" +
          "\n" +
          "Permissions required: Waive Policy Violations",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Auto waiver has been created successfully.",
            useReturnTypeSchema = true)
      })
  public List<ApiAutoPolicyWaiverDTO> addAutoPolicyWaivers(
      @Parameter(description = "Enter the ownerType to specify the scope. The response will contain the details " +
          "for waivers within the scope.", required = true) @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the corresponding id for the ownerType specified above.",
          required = true) @PathParam("ownerId") String ownerId,
      @RequestBody(
          description = "The request JSON can be an array that include the fields" +
              "<ol>" +
              "<li>threatLevel</li>" +
              "<li>pathForward</li>" +
              "<li>reachable</li>" +
              "</ol>",
          required = true) final List<ApiAutoPolicyWaiverDTO> autoPolicyWaivers)
  {
    return apiAutoPolicyWaiverService.addAutoPolicyWaivers(ownerType, ownerId, autoPolicyWaivers);
  }

  @POST
  @Path(OWNERS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_AUTO_WAIVER)
  @Operation(
      description = "Use this method to create an auto policy waiver configuration. Only one configuration can exist" +
          " at a time for a given application or organization." +
          "\n" +
          "\n" +
          "Permissions required: Waive Policy Violations",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Auto waiver has been created successfully.",
            useReturnTypeSchema = true)
      })
  public ApiAutoPolicyWaiverDTO addAutoPolicyWaiver(
      @Parameter(description = "Enter the ownerType to specify the scope. The response will contain the details " +
          "for waivers within the scope.", required = true) @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the corresponding id for the ownerType specified above.",
          required = true) @PathParam("ownerId") String ownerId,
      @RequestBody(
          description = "The request JSON can include the fields" +
              "<ol>" +
              "<li>threatLevel</li>" +
              "<li>pathForward</li>" +
              "<li>reachable</li>" +
              "</ol>",
          required = true) final ApiAutoPolicyWaiverDTO autoPolicyWaiverDTO)
  {
    return apiAutoPolicyWaiverService.addAutoPolicyWaiver(ownerType, ownerId, autoPolicyWaiverDTO);
  }

  @PUT
  @Path(BY_AUTO_POLICY_WAIVER_ID_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_AUTO_WAIVER)
  @Operation(description = "Use this method to update an auto policy waiver, specified by the autoPolicyWaiverId." +
      "\n" +
      "\n" +
      "Permissions required: Write IQ Elements",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Auto Policy Waiver has been updated successfully.",
            useReturnTypeSchema = true)
      })
  public ApiAutoPolicyWaiverDTO updateAutoPolicyWaiver(
      @Parameter(description = "Enter the ownerType to specify the scope. The response will contain the details " +
          "for waivers within the scope.", required = true) @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the corresponding id for the ownerType specified above.",
          required = true) @PathParam("ownerId") String ownerId,
      @Parameter(
          description = "Enter the autoPolicyWaiverId to be updated.") @PathParam("autoPolicyWaiverId") String autoPolicyWaiverId,
      @RequestBody(
          description = "The request JSON can include the fields" +
              "<ol>" +
              "<li>autoPolicyWaiverId</li>" +
              "<li>threatLevel</li>" +
              "<li>pathForward</li>" +
              "<li>reachable</li>" +
              "</ol>",
          required = true) final ApiAutoPolicyWaiverDTO autoPolicyWaiverDTO)
  {
    return apiAutoPolicyWaiverService.updateAutoPolicyWaiver(ownerType, ownerId, autoPolicyWaiverId,
        autoPolicyWaiverDTO);
  }

  @DELETE
  @Path(BY_AUTO_POLICY_WAIVER_ID_PATH)
  @Audited(AuditEvent.DELETE_AUTO_WAIVER)
  @Operation(description = "Use this method to delete an auto policy waiver, specified by the autoPolicyWaiverId." +
      "\n" +
      "\n" +
      "Permissions required: Waive Policy Violations",
      responses = {
        @ApiResponse(
            responseCode = "204",
            description = "Auto Policy Waiver has been deleted successfully.",
            useReturnTypeSchema = true)
      })
  public void deleteAutoPolicyWaiver(
      @Parameter(description = "Enter the ownerType to specify the scope. A waiver corresponding to the " +
          "autoPolicyWaiverId provided and within the scope specified will be deleted.",
          required = true) @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the corresponding id for the ownerType specified above.",
          required = true) @PathParam("ownerId") String ownerId,
      @Parameter(
          description = "Enter the autoPolicyWaiverId to be deleted") @PathParam("autoPolicyWaiverId") String autoPolicyWaiverId)
  {
    apiAutoPolicyWaiverService.deleteAutoPolicyWaiver(ownerType, ownerId, autoPolicyWaiverId);
  }

  @GET
  @Path(AUTO_WAIVER_STATUS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve status details for any auto policy waiver enabled " +
      "for the scope specified. You can specify the scope by using the parameters ownerType and ownerId." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "The response contains auto policy waiver status details for the specified ownerType and the "
                +
                "corresponding ownerId.",
            useReturnTypeSchema = true)
      })
  public ApiAutoPolicyWaiverStatusDTO getAutoPolicyWaiverStatus(
      @Parameter(description = "Enter the ownerType to specify the scope. The response will contain " +
          "status details for the active auto policy waiver, if any, that is within the scope specified.",
          required = true) @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the corresponding id for the ownerType specified above.",
          required = true) @PathParam("ownerId") String ownerId)
  {
    return apiAutoPolicyWaiverService.getAutoPolicyWaiverStatus(ownerType, ownerId);
  }

  @GET
  @Path(APPLICABLE_WAIVERS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve all applicable auto waivers for the scope specified. " +
      "You can specify the scope by using the parameters ownerType and ownerId." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "The response contains applicable auto policy waivers for the specified ownerType and the " +
                "corresponding ownerId.",
            useReturnTypeSchema = true)
      })
  public List<ApiAutoPolicyWaiverStatusDTO> getApplicableAutoWaivers(
      @Parameter(description = "Enter the ownerType to specify the scope. The response will contain " +
          "applicable auto policy waivers, if any, that are within the scope specified.",
          required = true) @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the corresponding id for the ownerType.",
          required = true) @PathParam("ownerId") String ownerId)
  {
    return apiAutoPolicyWaiverService.getApplicableAutoWaivers(ownerType, ownerId);
  }
}
