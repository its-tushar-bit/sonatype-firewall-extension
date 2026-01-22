/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.IOException;
import java.util.Set;
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
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationCategoriesListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiMoveApplicationResponseDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiApplicationService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.dataaccess.InvalidApplicationException;
import com.sonatype.insight.brain.organization.ApplicationCloneService;
import com.sonatype.insight.brain.organization.ApplicationMoveService;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;

@Tag(name = "Applications",
    description = "Use this REST API to manage applications." +
        "\n" +
        "\n" +
        "In addition to the primary functions of create, update and delete, you can also move applications from one " +
        "organization to other."
)
/**
 *
 * @since 1.11.0
 */
@Named
@Timed
@Path(PublicApiPaths.APP_RESOURCE_PATH)
public class ApiApplicationResourceV2
{
  /**
   * Internal application Id
   */
  public static final String APPLICATION_ID = "{applicationId}";

  public static final String ORGANIZATION_PATH = "organization/{organizationId}";

  // NOTE: more specific path param name than applicationId to avoid default handling by AuditContainerRequestFilter
  public static final String CLONE_PATH = "{sourceApplicationId}/clone";

  public static final String MOVE_PATH = APPLICATION_ID + "/move/" + ORGANIZATION_PATH;

  private final ApiApplicationService apiApplicationService;

  private final ApplicationCloneService applicationCloneService;

  private final ApplicationMoveService applicationMoveService;

  @Inject
  public ApiApplicationResourceV2(
      final ApiApplicationService apiApplicationService,
      final ApplicationCloneService applicationCloneService,
      final ApplicationMoveService applicationMoveService)
  {
    this.apiApplicationService = apiApplicationService;
    this.applicationCloneService = applicationCloneService;
    this.applicationMoveService = applicationMoveService;
  }

  @GET
  @Path(APPLICATION_ID)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = """
      Use this method to retrieve the application details, by providing the applicationId.

      Permissions required: View IQ Elements""",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "The response contains the details of the application corresponding to the applicationId.",
              useReturnTypeSchema = true)
      }
  )
  public ApiApplicationDTO getApplication(
      @Parameter(description = "Enter the applicationId.")
      @PathParam("applicationId") final String applicationId)
  {
    return apiApplicationService.getApplicationById(applicationId);
  }

  /**
   * Get the application DTO list filtered by the set of publicIds. If the publicIds is empty then all applications are
   * returned.
   *
   * @param publicIds The set of public ids to filter on (cannot be null)
   * @return The application DTO list found
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve the application details for the applicationId(s) provided." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Returns either a list of applications or a list of applications with category tags " +
                  "depending on the `includeCategories` parameter.",
              content = {
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON,
                      schema = @Schema(oneOf = {
                          ApiApplicationListDTO.class,
                          ApiApplicationCategoriesListDTO.class
                      })
                  )
              }
          ),
      })
  public Response getApplications(
      @Parameter(description = "Enter the applicationId.")
      @QueryParam("publicId") final Set<String> publicIds,
      @Parameter(description = "Set this parameter to `true` to obtain the application tags (application categories) " +
          "in the response.")
      @QueryParam("includeCategories") @DefaultValue("false") final boolean includeCategories)
  {
    if (includeCategories) {
      return Response.ok(apiApplicationService.getApplicationsWithAppliedCategories(publicIds),
          MediaType.APPLICATION_JSON_TYPE).build();
    }
    else {
      return Response.ok(apiApplicationService.getApplicationDTOs(publicIds), MediaType.APPLICATION_JSON_TYPE).build();
    }
  }

  /**
   * @since 1.102
   */
  @GET
  @Path(ORGANIZATION_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve application details for all applications under the " +
      "organizationId provided." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "The response contains the details of all applications found under the " +
                  "organizationId provided.",
              useReturnTypeSchema = true)
      }
  )
  public ApiApplicationListDTO getApplicationsByOrganizationId(
      @Parameter(description = "Enter the organizationId.")
      @PathParam("organizationId") String organizationId)
  {
    return apiApplicationService.getApplicationsByOrganizationId(organizationId);
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_APPLICATION)
  @Operation(description = "Use this method to create an application under an organization. Use the Organization " +
      "REST API to obtain organizationId." +
      "\n" +
      "\n" +
      "Permissions required: Add Application (on parent organization)",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "The response contains application details for the application created using this method.",
              useReturnTypeSchema = true
          )
      })
  public ApiApplicationDTO addApplication(
      @Parameter(description = "Specify the applicationId, application name and the organizationId under which the " +
          "application should be created. `contactUserName` corresponds to the 'contact' field in the UI and " +
          "represents the user name. If LDAP is used for authentication, you can use LDAP usernames." +
          "`tagId` is the internal identifier for the Application Category that you want to apply to the " +
          "application. " +
          "Use the Application Categories REST API for the available categories and the corresponding tagIds.")
      final ApiApplicationDTO applicationDTO)
  {
    return apiApplicationService.addApplication(applicationDTO);
  }

  @PUT
  @Path(APPLICATION_ID)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_APPLICATION)
  @Operation(description = "Use this method to update the application name, application tags or " +
      "the contact user name for an existing application by providing the applicationId. " +
      "\n" +
      "\n" +
      "NOTE: This method cannot be used to change the organizationId of an application." +
      "\n" +
      "\n" +
      "Permissions required: Edit IQ Elements",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "The response contains the updated application name, contact user name and " +
                  "application tags,  for the applicationId provided",
              useReturnTypeSchema = true)
      }
  )
  public ApiApplicationDTO updateApplication(
      @Parameter(description = "Specify the applicationId, application name and the organizationId under which  " +
          "the application exists. `contactUserName` corresponds to the 'contact' field in the UI and " +
          "represents the user name. If LDAP is used for authentication, you can use LDAP usernames." +
          "`tagId` is the internal identifier for the Application Category that you want to apply to " +
          "the application. . Use the Application Categories REST API for the available categories " +
          "and the corresponding tagIds.") final ApiApplicationDTO applicationDTO,
      @PathParam("applicationId") final String applicationId)
  {
    if (StringUtils.isBlank(applicationDTO.id)) {
      applicationDTO.id = applicationId;
    }

    if (!applicationId.equals(applicationDTO.id)) {
      throw new InvalidApplicationException("The applicationId=" + applicationId
          + " provided in the url did not match the id=" + applicationDTO.id + " provided in the json.");
    }
    return apiApplicationService.updateApplication(applicationDTO);
  }

  @DELETE
  @Path(APPLICATION_ID)
  @Audited(AuditEvent.DELETE_APPLICATION)
  @Operation(description = "Use this method to permanently delete an existing application and all data " +
      "associated with it. This action cannot be un-done. Before deleting, confirm that the application being " +
      "deleted does not impact any integrations that could depend on it." +
      "\n" +
      "\n" +
      "Permissions required: Edit IQ Elements",
      responses = {
          @ApiResponse(responseCode = "204",
              description = "Application deleted successfully"
          )
      })
  public void deleteApplication(
      @Parameter(description = "Enter the applicationId to be deleted.")
      @PathParam("applicationId") final String applicationId) throws IOException
  {
    apiApplicationService.deleteApplication(applicationId);
  }

  @POST
  @Path(CLONE_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_APPLICATION)
  @Operation(description = "Use this method to clone an existing application." +
      "\n" +
      "\n" +
      "Permissions required: Add Application (on the parent organization)",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "The response contains application details of the cloned application.",
              useReturnTypeSchema = true)
      })
  public ApiApplicationDTO cloneApplication(
      @Parameter(description = "Enter the applicationId for the application to be cloned.", required = true)
      @PathParam("sourceApplicationId") String sourceApplicationId,
      @Parameter(description = "Enter the application name for the new cloned application.")
      @QueryParam("clonedApplicationName") String clonedApplicationName,
      @Parameter(description = "Enter the applicationPublicId for the cloned application.")
      @QueryParam("clonedApplicationPublicId") String clonedApplicationPublicId)
  {
    return applicationCloneService.cloneApplication(sourceApplicationId, clonedApplicationName,
        clonedApplicationPublicId);
  }

  @POST
  @Path(MOVE_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.MOVE_APPLICATION)
  @Operation(
      description = "Use this method to move an application from one organization to another." +
          "\n" +
          "\n" +
          "Permissions required: Edit IQ Elements",
      responses = {
          @ApiResponse(responseCode = "200", description = "Application moved successfully, with/without warnings. " +
              "Warnings, if any, will appear in the response body.", useReturnTypeSchema = true),
          @ApiResponse(responseCode = "409", description = "Moving the application failed due " +
              "to conflicts between the organizations."),
          @ApiResponse(responseCode = "404", description = "Moving the application failed because " +
              "either an application with the provided applicationId or the organizationId for the organization " +
              "where it is to be moved is not found."),
      })
  public ApiMoveApplicationResponseDTOV2 moveApplication(
      @Parameter(description = "Enter the applicationId of the application to be moved.", required = true)
      @PathParam("applicationId") String applicationId,
      @Parameter(description = "Enter the organizationId of the destination organization.", required = true)
      @PathParam("organizationId") String organizationId)
  {
    return applicationMoveService.moveApplication(applicationId, organizationId);
  }
}
