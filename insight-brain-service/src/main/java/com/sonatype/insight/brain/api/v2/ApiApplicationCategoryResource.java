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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationCategoryDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.brain.tag.ApplicableTagsDTO;
import com.sonatype.insight.brain.tag.AppliedTagsDTO;
import com.sonatype.insight.brain.tag.TagService;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.9
 */
@Named
@Timed
@Path(ApiApplicationCategoryResource.RESOURCE_PATH)
@Tag(name = "Application Categories",
    description = "Use the Application Categories REST API to manage " +
        "the application categories or tags assigned to the applications in an organization. ")
@ProductLicenseEnforcementPoint(LicensedFeature.POLICY_MANAGEMENT)
public class ApiApplicationCategoryResource
{
  public static final String RESOURCE_PATH = PublicApiPaths.APPLICATION_CATEGORY_RESOURCE_PATH;

  public static final String USED_BY_APPLICATION_PATH = "application";

  public static final String APPLICATION_PATH = "application/{applicationPublicId}";

  public static final String ORGANIZATION_PATH = "organization/{organizationId}";

  public static final String ORGANIZATION_APPLICABLE_TAGS_PATH = "organization/{organizationId}/applicable";

  private final TagService service;

  @Inject
  public ApiApplicationCategoryResource(TagService service) {
    this.service = service;
  }

  @GET
  @Path(USED_BY_APPLICATION_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description =
      "Grouping applications with similar characteristics into categories makes policy management easier. " +
          "You can then create a policy that applies to a specific category. Use this method to retrieve " +
          "a list of application categories.")
  @ApiResponse(responseCode = "200",
      description = "A list of application categories or tags applied to applications. " +
          "Each application category or tag consists of an id, name, description and color.",
      useReturnTypeSchema = true)
  public List<ApiApplicationCategoryDTO> getTagsUsedByApplications() {
    return service.getTagsUsedByApplications();
  }

  @GET
  @Path(APPLICATION_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description =
      "Grouping applications with similar characteristics into categories makes policy management easier. " +
          "You can then create a policy that applies to a specific category. " +
          "Use this method to retrieve a list of application " +
          "categories available to applications in this organization.")
  @ApiResponse(responseCode = "200",
      description = "A list of application categories that can be applied to the specified application. " +
          "Each application category or tag consists of an id, name, description and color. ",
      useReturnTypeSchema = true)
  @ProductLicenseEnforcementPoint(LicensedFeature.POLICY_READ_ONLY)
  public ApplicableTagsDTO getApplicationApplicableTags(
      @Parameter(description = "The application public ID ", required = true)
      @PathParam("applicationPublicId") String applicationPublicId)
  {
    return service.getApplicableTags(OwnerType.APPLICATION, applicationPublicId);
  }

  @GET
  @Path(ORGANIZATION_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description =
      "Grouping applications with similar characteristics into categories makes policy management easier. " +
          "You can then create a policy that applies to a specific category." +
          " Use this method to retrieve a list of application " +
          "categories in use by applications in this organization.")
  @ApiResponse(responseCode = "200",
      description = "A list of application categories or tags that can be used by applications in this organization. " +
          "Each application category consists of an id, name, description and color. ",
      useReturnTypeSchema = true)
  public List<ApiApplicationCategoryDTO> getTags(
      @Parameter(description = "The organizationId assigned by IQ Server.", required = true)
      @PathParam("organizationId") String organizationId)
  {
    return service.getTags(organizationId);
  }

  @GET
  @Path(ORGANIZATION_APPLICABLE_TAGS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description =
      "Grouping applications with similar characteristics into categories makes policy management easier. " +
          "You can then create a policy that applies to a specific category. " +
          "Use this method to retrieve a list of application " +
          "categories that can be applied to applications in this organization.")
  @ApiResponse(responseCode = "200",
      description = "A list of application categories or tags that can be applied to applications in this" +
          " organization." +
          " Each application category or tag consists of an id, name, description and color. ",
      useReturnTypeSchema = true)
  @ProductLicenseEnforcementPoint(LicensedFeature.POLICY_READ_ONLY)
  public ApplicableTagsDTO getApplicableTags(
      @Parameter(description = "The organizationId assigned by IQ Server," +
          " for which you want to retrieve the applicable tags or application categories.",
          required = true) @PathParam("organizationId") String organizationId)
  {
    return service.getApplicableTags(OwnerType.ORGANIZATION, organizationId);
  }

  @GET
  @Path(APPLICATION_PATH + "/applicable")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description =
      "Grouping applications with similar characteristics into categories makes policy management easier. " +
          "You can then create a policy that applies to a specific category. " +
          "Use this method to retrieve a list of application " +
          "categories that can be applied to applications in this organization.")
  @ApiResponse(responseCode = "200",
      description = "Returns all application categories or tags that can be applied to this application,  " +
          "by providing the application public ID.",
      useReturnTypeSchema = true)
  @ProductLicenseEnforcementPoint(LicensedFeature.POLICY_READ_ONLY)
  public List<ApiApplicationCategoryDTO> getApplicableTagsByApplicationPublicId(
      @Parameter(description = "Provide the application public ID assigned by IQ Server.", required = true)
      @PathParam("applicationPublicId") String applicationPublicId)
  {
    return service.getApplicableTagsByApplicationPublicId(applicationPublicId);
  }

  @GET
  @Path(ORGANIZATION_PATH + "/applied")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description =
      "Grouping applications with similar characteristics into categories makes policy management easier. " +
          "You can then create a policy that applies to a specific category." +
          " Use this method to retrieve a list of application " +
          "categories that can be applied to applications in this organization.")
  @ApiResponse(responseCode = "200",
      description = "Get all application categories or tags that can be applied to an application, " +
          "belonging to the organization specified by the organization id.",
      useReturnTypeSchema = true)
  public AppliedTagsDTO getAppliedTags(
      @Parameter(description = "The organizationId assigned by IQ Server.", required = true)
      @PathParam("organizationId") String organizationId)
  {
    return service.getAppliedTags(organizationId);
  }

  @GET
  @Path(ORGANIZATION_PATH + "/policy")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description =
      "Grouping applications with similar characteristics into categories makes policy management easier. " +
          "You can then create a policy that applies to a specific category. " +
          "Use this method to retrieve a list of application " +
          "categories that are applied to applications in this organization.")
  @ApiResponse(responseCode = "200",
      description = "Get all policy application categories or tags that are applied to applications " +
          " in this organization.",
      useReturnTypeSchema = true)
  public List<PolicyTag> getAppliedPolicyTags(
      @Parameter(description = "The organizationId assigned by IQ Server.", required = true)
      @PathParam("organizationId") String organizationId)
  {
    return service.getAppliedPolicyTags(organizationId);
  }

  @POST
  @Path(ORGANIZATION_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_APPLICATION_CATEGORY)
  @Operation(description =
      "Grouping applications with similar characteristics into categories makes policy management easier. " +
          "You can then create a policy that applies to a specific category. " +
          "Use this method to add a new application category or tag.")
  @ApiResponse(responseCode = "200",
      description = "Successful creation of the new application category and its details.",
      useReturnTypeSchema = true)
  public ApiApplicationCategoryDTO addTag(
      @Parameter(description = "The organizationId assigned by IQ Server, " +
          "for which you want to create the application category.",
          required = true) @PathParam("organizationId") String organizationId,
      @RequestBody(description = "Specify the the name, description and color for the new application category to be " +
          " created. The application category id is not required to create a new application category " +
          " and should not be included.",
          required = true) ApiApplicationCategoryDTO tag)
  {
    return service.addTag(organizationId, tag);
  }

  @PUT
  @Path(ORGANIZATION_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_APPLICATION_CATEGORY)
  @Operation(description =
      "Grouping applications with similar characteristics into categories makes policy management easier. " +
          "You can then create a policy that applies to a specific category. " +
          "Use this method to update an existing application category.")
  @ApiResponse(responseCode = "200",
      description = "Successful update echoing the updated application category details.",
      useReturnTypeSchema = true)
  public ApiApplicationCategoryDTO updateTag(
      @Parameter(description = "The organizationId assigned by IQ Server.", required = true)
      @PathParam("organizationId") String organizationId,
      @RequestBody(description = "Specify the id (application category id) and id of the organization that owns this " +
          " application category, to update the name, description and color.",
          required = true) ApiApplicationCategoryDTO tag)
  {
    return service.updateTag(organizationId, tag);
  }

  @DELETE
  @Path(ORGANIZATION_PATH + "/{tagId}")
  @Audited(AuditEvent.DELETE_APPLICATION_CATEGORY)
  @Operation(description =
      "Grouping applications with similar characteristics into categories makes policy management easier. " +
          "You can then create a policy that applies to a specific category. Use this method to update an existing " +
          "application category." +
          "Use this method to delete an existing application category.")
  @ApiResponse(responseCode = "204", description = "Successful deletion of the application category.")
  public void deleteTag(
      @Parameter(description = "The organizationId assigned by IQ Server, corresponding to the application" +
          " category tag you want to delete.",
          required = true) @PathParam("organizationId") String organizationId,
      @Parameter(description = "The application category ID assigned by IQ Server, to be deleted.", required = true)
      @PathParam("tagId") String tagId)
  {
    service.deleteTag(organizationId, tagId);
  }
}
