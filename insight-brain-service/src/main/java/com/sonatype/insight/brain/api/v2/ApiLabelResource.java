/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiLabelDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.dto.ApplicableContext;
import com.sonatype.insight.brain.label.ApplicableLabels;
import com.sonatype.insight.brain.label.LabelService;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.product.license.RequiresEntitlement;
import com.sonatype.insight.brain.repository.hosted.HrcOwnerTypeFeatureGuard;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.90
 */
@Named
@Timed
@Path(PublicApiPaths.LABEL_RESOURCE_PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.COMPONENT_LABELS)
@Tag(name = "Component Labels",
    description = "Use this REST API to manage component labels for applications, organizations and repositories." +
        "\n" +
        "\n" +
        "Component Labels can be used as attributes of a component at the time of creating policies. " +
        "A policy violation can be triggered based on the component label.")
public class ApiLabelResource
{
  private final LabelService labelService;

  @Inject
  public ApiLabelResource(final LabelService labelService) {
    this.labelService = labelService;
  }

  /**
   * @param inherit boolean if {@code true} the returned list will include labels inherited from organization hierarchy,
   *          default is {@code false}
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve the details for component labels for an application, " +
      "organization or a repository." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "The response contains an array of component label descriptions for the application, " +
                "organization or repository, as selected in the request. Each label description contains:" +
                "<ul>" +
                "<li>`id` is the internal identifier assigned to the label.</li>" +
                "<li>`label` is the identifying name of the label, for e.g. 'Architecture-Deprecated'.</li>" +
                "<li>`description` is additional information describing the label.</li>" +
                "<li>`color` is the color assigned to the component label.</li>" +
                "<li>`ownerId` is the identifier for the ownerType selected in the request.</li>" +
                "<li>`ownerType` indicates if the label is for the application, organization or repository,  " +
                "as selected in the request.</li>" +
                "</ul>" +
                "If the request parameter `inherit` is set to `true` the response contains a description " +
                "of component labels that are inherited from the parent. The inherited labels can be identified " +
                "by the value of `ownerId` and `ownerType`.",
            useReturnTypeSchema = true)
      })

  public List<ApiLabelDTO> getLabels(
      @Parameter(description = "Select the `ownerType` for which you want to retrieve the component label " +
          "information.",
          required = true) @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the id of the application, organization or the repository.",
          required = true) @PathParam("ownerId") String ownerId,
      @Parameter(
          description = "Set to `true` to retrieve inherited component labels.") @QueryParam("inherit") @DefaultValue("false") boolean inherit)
  {
    HrcOwnerTypeFeatureGuard.requireHrcFeatureIfHrc(ownerType);
    return labelService.getLabels(ownerType, ownerId, inherit);
  }

  /**
   * Returns all the labels associated with an ownerId. The labels are grouped by ownerId and the owner name and type
   * are returned.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("applicable")
  @Operation(description = "Use this method to retrieve all component labels that are applicable to the specified " +
      "application, organization or repository." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "The response contains descriptions for all component labels that are applicable to the specified "
                +
                "owner. These include all component labels that are assigned and inherited. " +
                "The response includes:" +
                "<ul>" +
                "<li>`ownerId` is the identifier for the owner.</li>" +
                "<li>`ownerName` is the name for the owner.</li>" +
                "<li>`ownerType` indicates if the labels are for an application, organization or " +
                "repository.</li> " +
                "<li>`labels` is the component labels for this owner.</li>" +
                "</ul>" +
                "Each label includes " +
                "<ul>" +
                "<li>`id` is the internal identifier assigned to the label.</li>" +
                "<li>`label` is the identifying name of the label, for e.g. 'Architecture-Deprecated'.</li>" +
                "<li>`description` is additional information describing the label.</li>" +
                "<li>`color` is the color assigned to the component label.</li>" +
                "<li>`ownerId` is the identifier for the ownerType selected in the request.</li>" +
                "<li>`ownerType` indicates if the label is for the application, organization or repository, " +
                "as selected in the request.</li>" +
                "</ul>",
            useReturnTypeSchema = true)
      })
  public ApplicableLabels getApplicableLabels(
      @Parameter(description = "Select the ownerType to retrieve the component label information for.",
          required = true) @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the id for the application, organization or repository",
          required = true) @PathParam("ownerId") String ownerId)
  {
    HrcOwnerTypeFeatureGuard.requireHrcFeatureIfHrc(ownerType);
    return labelService.getApplicableLabels(ownerType, ownerId);
  }

  /**
   * Enumerates the contexts (org/app) in which the given label could be applied.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("applicable/context/{labelId}")
  @Operation(description = "Use this method to retrieve the hierarchy of owners (applications, organizations, " +
      "repositories) in which the label can be applied." +
      "\n" +
      "\n" +
      "Permissions required: Edit IQ Elements",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "The response contains:" +
                "<ul>" +
                "<li>`id` is the id of the selected owner.</li>" +
                "<li>`name` is the name of the selected owner.</li>" +
                "<li>`type` is the type of the selected owner e.g. application, organization or repository.</li>" +
                "<li>`children` is an array of the child owners in the hierarchy.</li>",
            useReturnTypeSchema = true)
      })
  public ApplicableContext getApplicableContexts(
      @Parameter(description = "Select the ownerType.") @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the ownerId") @PathParam("ownerId") String ownerId,
      @Parameter(description = "Enter the labelId") @PathParam("labelId") String labelId)
  {
    HrcOwnerTypeFeatureGuard.requireHrcFeatureIfHrc(ownerType);
    return labelService.getApplicableContexts(ownerType, ownerId, labelId);
  }

  @RequiresEntitlement(LicensedFeature.CUSTOM_COMPONENT_LABELS)
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_LABEL)
  @Operation(description = "Use this method to create and assign a component label to an application, organization " +
      "or repository." +
      "\n" +
      "\n" +
      "Permissions required: Edit IQ Elements",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "The response contains label details sent in the request and the `id` for the label " +
                "created.",
            useReturnTypeSchema = true)
      })
  public ApiLabelDTO addLabel(
      @Parameter(
          description = "Select the ownerType to which the label will be assigned.") @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the id for the selected ownerType.") @PathParam("ownerId") String ownerId,
      @RequestBody(description = "Specify a label name, description and color for the label. Valid values for " +
          "color are `light-red` , `light-green` , `light-blue` , `light-purple`, `dark-red` , `dark-green` , " +
          "`dark-blue` , `dark-purple` , `orange` , `yellow`. Do not enter value for the `id` field.") ApiLabelDTO labelDTO)
  {
    HrcOwnerTypeFeatureGuard.requireHrcFeatureIfHrc(ownerType);
    return labelService.addLabel(ownerType, ownerId, labelDTO);
  }

  @RequiresEntitlement(LicensedFeature.CUSTOM_COMPONENT_LABELS)
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_LABEL)
  @Operation(description = "Use this method to update an existing component label for an application, organization " +
      "or repository." +
      "\n" +
      "\n" +
      "Permissions required: Edit IQ Elements",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "The response contains the label details sent in the update request.",
            useReturnTypeSchema = true)
      })
  public ApiLabelDTO updateLabel(
      @Parameter(
          description = "Select the ownerType for which the label will be updated.") @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the id for the selected ownerType.") @PathParam("ownerId") String ownerId,
      @RequestBody(description = "Specify the new values for label name, description, color and the corresponding " +
          "label id for the component label to be updated. Valid values for color are `light-red` , " +
          "`light-green` , `light-blue` , `light--purple`, `dark-red` , `dark-green` ,`dark-blue` , `dark-purple` ," +
          "`orange` , `yellow`.") ApiLabelDTO labelDTO)
  {
    HrcOwnerTypeFeatureGuard.requireHrcFeatureIfHrc(ownerType);
    return labelService.updateLabel(ownerType, ownerId, labelDTO);
  }

  @RequiresEntitlement(LicensedFeature.CUSTOM_COMPONENT_LABELS)
  @DELETE
  @Path("{labelId}")
  @Audited(AuditEvent.DELETE_LABEL)
  @Operation(description = "Use this method to delete an existing component label." +
      "\n" +
      "\n" +
      "Permissions required: Edit IQ Elements",
      responses = {
        @ApiResponse(responseCode = "204",
            description = "Component label deleted successfully.")
      })
  public void deleteLabel(
      @Parameter(
          description = "Select the ownerType for which the label will be deleted.") @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the id for the selected ownerType.") @PathParam("ownerId") String ownerId,
      @Parameter(description = "Enter the id for the label to be deleted.") @PathParam("labelId") String labelId)
  {
    HrcOwnerTypeFeatureGuard.requireHrcFeatureIfHrc(ownerType);
    labelService.deleteLabel(ownerType, ownerId, labelId);
  }
}
