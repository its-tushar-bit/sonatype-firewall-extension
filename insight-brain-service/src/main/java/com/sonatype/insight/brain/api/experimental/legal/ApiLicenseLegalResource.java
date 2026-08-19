/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationDashboardResultDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentDashboardResultDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalObligationDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentCopyrightDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentCopyrightWithOwnerDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentLegalFileDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentObligationAttributionDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentSourceLinkDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.CopyrightFilePathsDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LicenseLegalApplicationComponentsFilterDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LicenseLegalFilterDTO;
import com.sonatype.insight.brain.api.v2.service.legal.ApiLicenseLegalService;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.legal.LegalFileType;
import com.sonatype.insight.error.exception.BadRequestException;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Named
@Timed
@Path(PublicApiPaths.LICENSE_LEGAL_RESOURCE_PATH)
@Tag(name = "License Legal Metadata",
    description = "These are experimental REST APIs for the Advanced Legal Pack (ALP).")
public class ApiLicenseLegalResource
{
  public static final String DASHBOARD_APPLICATIONS_PATH = "dashboard/applications";

  public static final String DASHBOARD_COMPONENTS_PATH = "dashboard/components";

  public static final String DASHBOARD_APPLICATION_PATH = "dashboard/application/{applicationPublicId}";

  public static final String COMPONENT_PATH = "{ownerType: application|organization}/{ownerId}/component";

  public static final String COMPONENT_COPYRIGHT_PATH = COMPONENT_PATH + "/copyright";

  public static final String COMPONENT_SOURCE_LINK_PATH = COMPONENT_PATH + "/sourceLink";

  public static final String COMPONENT_LEGAL_FILE_PATH = COMPONENT_PATH + "/legalFile";

  public static final String COMPONENT_OBLIGATION_PATH = COMPONENT_PATH + "/obligation";

  public static final String COMPONENT_OBLIGATIONS_PATH = COMPONENT_PATH + "/obligations";

  public static final String COMPONENT_OBLIGATION_DELETE_PATH = "/component/obligation";

  public static final String COMPONENT_OBLIGATION_ATTRIBUTION_PATH = COMPONENT_OBLIGATION_PATH + "/attribution";

  public static final String COMPONENT_OBLIGATION_ATTRIBUTION_DELETE_PATH =
      "/component/obligation/attribution/{componentObligationAttributionId}";

  public static final String COMPONENT_COPYRIGHT_FILEPATHS =
      COMPONENT_PATH + "/{componentHash}/copyright/{copyrightContentHash}/filePaths";

  public static final String COMPONENT_COPYRIGHT_FILEPATH_CONTEXT =
      COMPONENT_PATH + "/{componentHash}/copyright/{copyrightContentHash}/context";

  public static final String COMPONENT_COPYRIGHT_FILE_COUNT = COMPONENT_PATH + "/{componentHash}/copyright/fileCount";

  public static final int OBLIGATION_COMMENT_MAX_CHARACTER = 1000;

  private final ApiLicenseLegalService apiLicenseLegalService;

  private final ComponentLegalService componentLegalService;

  private final ApiLegalCopyrightService apiLegalCopyrightService;

  private final LegalApplicationDashboardService legalApplicationDashboardService;

  @Inject
  public ApiLicenseLegalResource(
      final ApiLicenseLegalService apiLicenseLegalService,
      final ComponentLegalService componentLegalService,
      final ApiLegalCopyrightService apiLegalCopyrightService,
      final LegalApplicationDashboardService legalApplicationDashboardService)
  {
    this.apiLicenseLegalService = apiLicenseLegalService;
    this.componentLegalService = componentLegalService;
    this.apiLegalCopyrightService = apiLegalCopyrightService;
    this.legalApplicationDashboardService = legalApplicationDashboardService;
  }

  @POST
  @Path(DASHBOARD_APPLICATIONS_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve a dashboard view of legal review status " +
      "for a page of applications. The filter criteria for application IDs, organization IDs, stage type IDs, " +
      "application categories (tag IDs), and the review progress can be specified in the request body." +
      "\n" +
      "\n" +
      "Permissions required: Review Legal Obligations For Components Licenses",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "The response contains the application name and their corresponding application categories, "
                +
                "last scan time, stage, number of component obligations reviewed and the total number of " +
                "components in the application.",
            useReturnTypeSchema = true)
      })
  public ApiLicenseLegalApplicationDashboardResultDTO getLicenseLegalApplicationsDashboard(
      @RequestBody(description = "Enter values for the filter criteria for the dashboard results." +
          "<ul>" +
          "<li>Enter values for organization IDs.</li>" +
          "<li>Enter values for application IDs.</li>" +
          "<li>Possible values for stage type IDs are `source`, `build`, `stage release`,`release` and " +
          " `operate`.</li>" +
          "<li>Enter values for application categories (tag IDs).</li>" +
          "<li>Possible values for review status are `OPEN` or `NOT_STARTED`.</li>" +
          "</ul>") LicenseLegalFilterDTO filter)
  {
    return apiLicenseLegalService.getLicenseLegalApplicationsDashboard(filter.organizationIds, filter.applicationIds,
        filter.tagIds, filter.stageTypeIds, filter.reviewStatus, filter.order, filter.page, filter.pageSize);
  }

  @POST
  @Path(DASHBOARD_COMPONENTS_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      description = "This method retrieves a detailed dashboard view of components and the corresponding legal " +
          "obligations, based on the filter selection in the request body." +
          "\n" +
          "\n" +
          "Permissions required: Review Legal Obligations For Components Licenses",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "The response contains results for the components dashboard view based on the filter criteria. "
                +
                "It contains component details, licenses, number of applications containing the components, " +
                "and a comparison of the number of obligations reviewed to the total number of obligations.",
            useReturnTypeSchema = true)
      })
  public ApiLicenseLegalComponentDashboardResultDTO getLicenseLegalComponentsDashboard(
      @RequestBody(description = "Enter values for the filter criteria for the dashboard results." +
          "<ul>" +
          "<li>Enter values for organization IDs.</li>" +
          "<li>Enter values for application IDs.</li>" +
          "<li>Possible values for stage type IDs are `source`, `build`, `stage release`,`release` and " +
          " `operate`.</li>" +
          "<li>Enter values for application categories (tag IDs).</li>" +
          "<li>Possible values for review status are `OPEN` or `NOT_STARTED`.</li>" +
          "</ul>") LicenseLegalFilterDTO filter)
  {
    return apiLicenseLegalService.getLicenseLegalComponentsDashboard(filter);
  }

  @POST
  @Path(DASHBOARD_APPLICATION_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "This method retrieves a detailed dashboard view of legal obligations and review status " +
      "for the components of the specified application. The filter criteria for review status and license " +
      "threat group names can be specified in the request body." +
      "\n" +
      "\n" +
      "Permissions required: Review Legal Obligations For Components Licenses",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "The response contains results for the dashboard view based on the filter criteria specified. "
                +
                "It contains:" +
                "<ul>" +
                "<li>The license IDs and corresponding license names, license threat groups and number " +
                "of obligations reviewed to the total number of obligations for each component.</li>" +
                "<li>The review status can be `FLAGGED`,`IN_PROGRESS`, `UNREVIEWED` and `COMPLETED`.</li>" +
                "</ul>",
            useReturnTypeSchema = true)
      })
  public List<ApiLicenseLegalApplicationComponentDTO> getLicenseLegalApplicationDashboard(
      @Parameter(
          description = "Enter the application public ID.") @PathParam("applicationPublicId") String applicationPublicId,
      @RequestBody(description = "Enter values for the filter criteria: " +
          "<ul>" +
          "<li>Possible values for stage type IDs are `source`, `build`, `stage release`, `release`, " +
          "and `operate`.</li>" +
          "<li>Possible values for review statuses are `FLAGGED`,`IN_PROGRESS`, `UNREVIEWED` and `COMPLETED`.</li>" +
          "<li>Possible values for license threat group names are the same as those already setup.</li>" +
          "</ul>") LicenseLegalApplicationComponentsFilterDTO filter)
  {
    return legalApplicationDashboardService.getLicenseLegalApplicationDashboard(applicationPublicId, filter);
  }

  @POST
  @Path(COMPONENT_COPYRIGHT_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_COMPONENT_COPYRIGHT)
  @Operation(description = "Use this method to update the copyright text for a component." +
      "\n" +
      "\n" +
      "Permissions required: Review Legal Obligations For Components Licenses",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "The response contains details for the component and the updated copyright text.",
            useReturnTypeSchema = true)
      })
  public ComponentCopyrightDTO saveComponentCopyright(
      @RequestBody(
          description = "The request JSON should include the component identifier (format and coordinates) or " +
              "the packageUrl, the content hash of the original copyright (if updating), new content for the " +
              "copyright, and status indicating if the copyright content appears on the attribution report.") ComponentCopyrightDTO componentCopyrightDTO,
      @Parameter(description = "Select the owner type.") @PathParam("ownerType") OwnerType ownerType,
      @Parameter(
          description = "Enter the owner ID corresponding to the owner type selected.") @PathParam("ownerId") String ownerId)
  {
    return componentLegalService.saveComponentCopyright(ownerType, ownerId,
        componentCopyrightDTO);
  }

  @GET
  @Path(COMPONENT_COPYRIGHT_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve all copyrights for the specified component identifier. You " +
      "can specify the component identifier in one of the 2 ways:" +
      "<ul>" +
      "<li>Component identifier object containing the coordinates of the component and its format</li>" +
      "<li>packageUrl string</li>" +
      "</ul>" +
      "\n" +
      "\n" +
      "Permissions required: Review Legal Obligations For Components Licenses",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "The response contains the component identifier (format and coordinates), " +
                "the packageUrl, the component copyrights, timestamp when the copyright was last updated, " +
                "last updated by username. " +
                "Each copyright contains the content hash of the original  copyright, the content for the " +
                "copyright, and the status indicating if the copyright content appears on the attribution report.",
            useReturnTypeSchema = true)
      })
  public ComponentCopyrightWithOwnerDTO getComponentCopyright(
      @Parameter(
          description = "Enter the `format` and `coordinates` for the component identifier.") @QueryParam("componentIdentifier") ComponentIdentifier componentIdentifier,
      @Parameter(description = "Enter the packageUrl for the component.") @QueryParam("packageUrl") String packageUrl,
      @Parameter(description = "Select the ownerType.") @PathParam("ownerType") OwnerType ownerType,
      @Parameter(
          description = "Enter the ID corresponding to the ownerType selected above.") @PathParam("ownerId") String ownerId)
  {
    return componentLegalService.getComponentCopyrightWithHierarchy(ownerType, ownerId,
        apiLicenseLegalService.getComponentIdentifier(componentIdentifier, packageUrl));
  }

  /**
   * @since 1.107
   */
  @POST
  @Path(COMPONENT_LEGAL_FILE_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_COMPONENT_LEGAL_FILE)
  @Operation(description = "Use this method to update the legal contents (notice or license) for a component via " +
      "its component identifier." +
      "\n" +
      "\n" +
      "Permissions required: Review Legal Obligations For Components Licenses",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "The response contains the updated legal contents for the component.")
      })
  public ComponentLegalFileDTO saveComponentLegalFile(
      @Parameter(description = "Select the ownerType.") @PathParam("ownerType") OwnerType ownerType,
      @Parameter(
          description = "Enter the corresponding ID for the ownerType specified above.") @PathParam("ownerId") String ownerId,
      @RequestBody(description = "The request JSON should include:" +
          "<ul>" +
          "<li>The component identifier (format and coordinates) or the packageUrl for the component.</li>" +
          "<li>The legal content type being updated - `notice` or `license`.</li>" +
          "<li>The content for the legal override.</li>" +
          "<li>The status indicating if the legal override appears on the attribution report.</li>" +
          "</ul>") ComponentLegalFileDTO componentLegalFileDTO)
  {
    return componentLegalService.saveComponentLegalFile(ownerType, ownerId, componentLegalFileDTO);
  }

  /**
   * @since 1.107
   */
  @GET
  @Path(COMPONENT_LEGAL_FILE_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve the legal file overrides for the specified component " +
      "identifier. You can select the type of the legal file i.e. `notice` (legal requirements " +
      "and attribution related to the project dependencies) or `license` (rights and obligations for the users.) " +
      "You can specify the component identifier in the following ways:" +
      "<ul>" +
      "<li>Component identifier object containing the coordinates of the component and its format</li>" +
      "<li>packageUrl string</li>" +
      "</ul>" +
      "\n" +
      "\n" +
      "Permissions required: Review Legal Obligations For Components Licenses",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "The response contains the component identifier (format and coordinates), " +
                "the packageUrl, the legal file type (`notice` or `license`), legal overrides, timestamp when this " +
                "was last updated, and the last updated by username. " +
                "Each legal file override contains the content hash of the original legal file, the content for " +
                "the legal override, and the status indicating if the content appears on the " +
                "attribution report.",
            useReturnTypeSchema = true)
      })
  public ComponentLegalFileDTO getComponentLegalFile(
      @Parameter(description = "Select the ownerType.") @PathParam("ownerType") OwnerType ownerType,
      @Parameter(
          description = "Select the owner ID corresponding to the owner type.") @PathParam("ownerId") String ownerId,
      @Parameter(
          description = "Enter the component identifier.") @QueryParam("componentIdentifier") ComponentIdentifier componentIdentifier,
      @Parameter(description = "Enter the packageUrl.") @QueryParam("packageUrl") String packageUrl,
      @Parameter(description = "Select the legal file type.") @QueryParam("legalFileType") LegalFileType legalFileType)
  {
    return componentLegalService.getComponentLegalFile(ownerType, ownerId,
        apiLicenseLegalService.getComponentIdentifier(componentIdentifier, packageUrl), legalFileType);
  }

  /**
   * @since 1.106
   */
  @GET
  @Path(COMPONENT_OBLIGATION_ATTRIBUTION_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve the attributions for a component by specifying the component " +
      "identifier and the obligation name. The component identifier can be specified using the component coordinates " +
      "and format or the packageUrl." +
      "\n" +
      "\n" +
      "Permissions required: Review Legal Obligations For Components Licenses",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "The response contains the attributions for the specified component obligation.",
            useReturnTypeSchema = true)
      })
  public List<ComponentObligationAttributionDTO> getComponentObligationAttribution(
      @Parameter(description = "Select the ownerType.") @PathParam("ownerType") OwnerType ownerType,
      @Parameter(
          description = "Enter the owner ID corresponding to the ownerType selected above.") @PathParam("ownerId") String ownerId,
      @Parameter(
          description = "Enter the component coordinates and format.") @QueryParam("componentIdentifier") ComponentIdentifier componentIdentifier,
      @Parameter(description = "Enter the packageUrl for the component.") @QueryParam("packageUrl") String packageUrl,
      @Parameter(description = "Enter the obligation name.",
          required = true) @QueryParam("obligationName") String obligationName)
  {
    return componentLegalService
        .getComponentObligationAttributions(ownerType, ownerId,
            apiLicenseLegalService.getComponentIdentifier(componentIdentifier, packageUrl), obligationName);
  }

  /**
   * @since 1.106
   */
  @POST
  @Path(COMPONENT_OBLIGATION_ATTRIBUTION_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_COMPONENT_OBLIGATION_ATTRIBUTION)
  @Operation(description = "Use this method to create or update an attribution for a component obligation." +
      "\n" +
      "\n" +
      "Permissions required: Review Legal Obligations For Components Licenses",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "The response contains the details for the component obligation attribution created.",
            useReturnTypeSchema = true)
      })
  public ComponentObligationAttributionDTO saveComponentObligationAttribution(
      @Parameter(description = "Select the ownerType.") @PathParam("ownerType") OwnerType ownerType,
      @Parameter(
          description = "Enter the owner ID corresponding to the ownerType selected above.") @PathParam("ownerId") String ownerId,
      @RequestBody(description = "Enter the details for the component obligation attribution including component " +
          "coordinates, the attribution content, and attribution ID if updating.") ComponentObligationAttributionDTO componentObligationAttributionDTO)
  {
    if (componentObligationAttributionDTO.getId() != null) {
      AuditData.get().setEvent(AuditEvent.UPDATE_COMPONENT_OBLIGATION_ATTRIBUTION);
    }
    return componentLegalService
        .saveComponentObligationAttribution(ownerType, ownerId, componentObligationAttributionDTO);
  }

  /**
   * @since 1.106
   */
  @DELETE
  @Path(COMPONENT_OBLIGATION_ATTRIBUTION_DELETE_PATH)
  @Audited(AuditEvent.DELETE_COMPONENT_OBLIGATION_ATTRIBUTION)
  @Operation(description = "Use this method to permanently delete an obligation attribution for a component." +
      "\n" +
      "\n" +
      "Permissions required: Review Legal Obligations For Components Licenses",
      responses = {
        @ApiResponse(
            responseCode = "204",
            description = "Attribution deleted successfully.")
      })
  public void deleteComponentObligationAttribution(
      @Parameter(
          description = "Enter the attribution ID for the component obligation to be deleted.") @PathParam("componentObligationAttributionId") String componentObligationAttributionId)
  {
    componentLegalService.deleteComponentObligationAttribution(componentObligationAttributionId);
  }

  /**
   * @since 1.106
   */
  @GET
  @Path(COMPONENT_OBLIGATION_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve the obligation details for a component by specifying the " +
      "component identifier and the obligation name. The component identifier can be specified using the component " +
      "coordinates and format or the packageUrl." +
      "\n" +
      "\n" +
      "Permissions required: Review Legal Obligations For Components Licenses",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "The response contains the obligation status, comment, and last modification details (date and "
                +
                "user) for the obligation specified.",
            useReturnTypeSchema = true)
      })
  public ApiLicenseLegalObligationDTO getComponentObligation(
      @Parameter(description = "Select the ownerType.") @PathParam("ownerType") OwnerType ownerType,
      @Parameter(
          description = "Enter the owner ID corresponding to the ownerType selected above.") @PathParam("ownerId") String ownerId,
      @Parameter(
          description = "Enter the component coordinates and format.") @QueryParam("componentIdentifier") ComponentIdentifier componentIdentifier,
      @Parameter(description = "Enter the packageUrl for the component.") @QueryParam("packageUrl") String packageUrl,
      @Parameter(description = "Enter the obligation name.",
          required = true) @QueryParam("obligationName") String obligationName)
  {
    return componentLegalService.getComponentObligation(ownerType, ownerId,
        apiLicenseLegalService.getComponentIdentifier(componentIdentifier, packageUrl), obligationName);
  }

  /**
   * @since 1.106
   */
  @POST
  @Path(COMPONENT_OBLIGATION_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.SAVE_COMPONENT_OBLIGATIONS)
  @Operation(description = "Use this method to update the legal obligation status and comments for a " +
      "component, by specifying the component identifier and obligation name." +
      "\n" +
      "\n" +
      "Permissions required: Review Legal Obligations For Components Licenses",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "The response contains the updated legal obligation status and comments.",
            useReturnTypeSchema = true)
      })
  public ApiLicenseLegalObligationDTO saveComponentObligation(
      @Parameter(description = "Select the ownerType") @PathParam("ownerType") OwnerType ownerType,
      @Parameter(
          description = "Enter the owner ID for the ownerType selected above.") @PathParam("ownerId") String ownerId,
      @RequestBody(description = "Enter the component identifier (coordinates and format or packageUrl), " +
          "obligation status and comments. The allowed values for the field status are `FULFILLED`, " +
          "`FLAGGED`, `IGNORED`, and `OPEN`.") ApiLicenseLegalObligationDTO componentObligationDTO)
  {
    if (componentObligationDTO.getComment() != null &&
        componentObligationDTO.getComment().length() > OBLIGATION_COMMENT_MAX_CHARACTER)
    {
      throw new BadRequestException(String.format(
          "ComponentObligationAttribution content must be less than %s characters", OBLIGATION_COMMENT_MAX_CHARACTER));
    }
    return componentLegalService
        .saveComponentObligations(ownerType, ownerId, Collections.singletonList(componentObligationDTO))
        .get(0);
  }

  /**
   * @since 1.109
   */
  @POST
  @Path(COMPONENT_OBLIGATIONS_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.SAVE_COMPONENT_OBLIGATIONS)
  @Operation(description = "Use this method to update the legal obligation status and comments for components by " +
      "specifying the obligation name and the component coordinates. The component coordinates can be the packageURL " +
      "or the component identifier." +
      "\n" +
      "\n" +
      "Permissions required: Review Legal Obligations For Components Licenses",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "The response contains component details and the updated obligation status for " +
                "each component.",
            useReturnTypeSchema = true)
      })
  public List<ApiLicenseLegalObligationDTO> saveComponentObligations(
      @Parameter(description = "Select the owner type.") @PathParam("ownerType") OwnerType ownerType,
      @Parameter(
          description = "Enter the owner ID corresponding to the selected owner type.") @PathParam("ownerId") String ownerId,
      @RequestBody(description = "Enter values for component coordinates, obligation names and review status for " +
          "each component." +
          "\n" +
          "\n" +
          "The review status can be `FLAGGED`,`IN_PROGRESS`, `UNREVIEWED` and `COMPLETED`.") List<ApiLicenseLegalObligationDTO> componentObligationDTOs)
  {
    return componentLegalService.saveComponentObligations(ownerType, ownerId, componentObligationDTOs);
  }

  /**
   * @since 1.106
   */
  @DELETE
  @Path(COMPONENT_OBLIGATION_DELETE_PATH)
  @Audited(AuditEvent.DELETE_COMPONENT_OBLIGATION)
  @Operation(description = "Use this method to permanently delete multiple obligations for a component." +
      "\n" +
      "\n" +
      "Permissions Required: Review Legal Obligations For Components Licenses",
      responses = {
        @ApiResponse(
            responseCode = "204",
            description = "Obligation(s) deleted successfully.")
      })
  public void deleteComponentObligations(
      @Parameter(
          description = "Enter the component obligation ID(s).") @QueryParam("componentObligationId") List<String> componentObligationIds)
  {
    componentLegalService.deleteComponentObligations(componentObligationIds);
  }

  /**
   * @since 1.109
   */
  @GET
  @Path(COMPONENT_COPYRIGHT_FILEPATHS)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve the file paths for a component copyright by specifying the " +
      "component format and coordinates or the packageUrl for the component, the component hash " +
      "and the copyright content hash." +
      "\n" +
      "\n" +
      "Permissions required: Review Legal Obligations For Components Licenses",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "The response contains copyright file paths and the number of times each file path has occurred "
                +
                "and the total number of distinct file paths.",
            useReturnTypeSchema = true)
      })
  public CopyrightFilePathsDTO getCopyrightFilePaths(
      @Parameter(description = "Select the ownerType.") @PathParam("ownerType") OwnerType ownerType,
      @Parameter(
          description = "Enter the ownerId corresponding to the owner type selected above.") @PathParam("ownerId") String ownerId,
      @Parameter(description = "Enter the component hash.") @PathParam("componentHash") String componentHash,
      @Parameter(
          description = "Enter the copyright hash.") @PathParam("copyrightContentHash") String copyrightContentHash,
      @Parameter(
          description = "Enter the component format and coordinates.") @QueryParam("componentIdentifier") ComponentIdentifier componentIdentifier,
      @Parameter(description = "Enter the package URL.") @QueryParam("packageUrl") String packageUrl,
      @Parameter(description = "Enter the page number for the query results.") @QueryParam("pageStart") int pageStart,
      @Parameter(description = "Enter the page length of the query results.") @QueryParam("pageLength") int pageLength)
  {
    return apiLegalCopyrightService.getCopyrightFilePaths(
        ownerType,
        ownerId,
        apiLicenseLegalService.getComponentIdentifier(componentIdentifier, packageUrl),
        componentHash,
        copyrightContentHash,
        pageStart, pageLength);
  }

  /**
   * @since 1.109
   */
  @GET
  @Path(COMPONENT_COPYRIGHT_FILEPATH_CONTEXT)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve the content for a component's copyright within a particular " +
      "file, by specifying the " +
      "component hash and coordinates, copyright content hash, and the file path." +
      "\n" +
      "\n" +
      "Permissions required: Review Legal Obligations For Components Licenses",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "The response contains the context for the specified component.",
            useReturnTypeSchema = true)
      })
  public List<String> getCopyrightContexts(
      @Parameter(description = "Select the ownerType.") @PathParam("ownerType") OwnerType ownerType,
      @Parameter(
          description = "Enter the owner ID corresponding to the owner type selected above.") @PathParam("ownerId") String ownerId,
      @Parameter(description = "Enter the component hash.") @PathParam("componentHash") String componentHash,
      @Parameter(
          description = "Enter the copyright content hash.") @PathParam("copyrightContentHash") String copyrightContentHash,
      @Parameter(description = "Enter the filepath.", required = true) @QueryParam("filePath") String filePath,
      @Parameter(
          description = "Enter the component identifier.") @QueryParam("componentIdentifier") ComponentIdentifier componentIdentifier,
      @Parameter(description = "Enter the packageUrl.") @QueryParam("packageUrl") String packageUrl)
  {
    return apiLegalCopyrightService.getCopyrightContextContent(
        ownerType,
        ownerId,
        apiLicenseLegalService.getComponentIdentifier(componentIdentifier, packageUrl),
        componentHash,
        copyrightContentHash,
        filePath);
  }

  /**
   * @since 1.109
   */
  @GET
  @Path(COMPONENT_COPYRIGHT_FILE_COUNT)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve the number of files associated with each copyright by " +
      "specifying the component hash and coordinates." +
      "\n" +
      "\n" +
      "Permissions required: Review Legal Obligations For Components Licenses",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "The response contains a map of copyright hashes to file counts.",
            useReturnTypeSchema = true)
      })
  public Map<String, Integer> getCopyrightFileCount(
      @Parameter(description = "Select the ownerType.") @PathParam("ownerType") OwnerType ownerType,
      @Parameter(
          description = "Enter the owner ID for the owner type selected above.") @PathParam("ownerId") String ownerId,
      @Parameter(description = "Enter the component hash.") @PathParam("componentHash") String componentHash,
      @Parameter(
          description = "Enter the component coordinates and format.") @QueryParam("componentIdentifier") ComponentIdentifier componentIdentifier,
      @Parameter(description = "Enter the packageUrl.") @QueryParam("packageUrl") String packageUrl)
  {
    return apiLegalCopyrightService.getCopyrightFileCount(
        ownerType,
        ownerId,
        apiLicenseLegalService.getComponentIdentifier(componentIdentifier, packageUrl),
        componentHash);
  }

  /**
   * @since 1.133
   */
  @POST
  @Path(COMPONENT_SOURCE_LINK_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_COMPONENT_SOURCE_LINK)
  @Operation(description = "Use this method to add links to the source code for a component." +
      "\n" +
      "\n" +
      "Permissions required: Review Legal Obligations For Components Licenses",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "The response contains the component details and the source code links for " +
                "the component.",
            useReturnTypeSchema = true)
      })
  public ComponentSourceLinkDTO saveComponentSourceLink(
      @RequestBody(description = "Enter values for the component coordinates or packageURL." +
          "\n" +
          "\n" +
          "If adding new source code links, `sourceLinkOverrides` should contain the content and status. " +
          "If updating, `sourceLinkOverrides` should contain the ID, the original content, content (to be updated) " +
          "and status. " +
          "Status `enabled` will allow the source code links to be included in the attribution report.") ComponentSourceLinkDTO componentSourceLinkDTO,
      @Parameter(description = "Select the owner type.") @PathParam("ownerType") OwnerType ownerType,
      @Parameter(
          description = "Enter the owner ID corresponding to the selected owner type.") @PathParam("ownerId") String ownerId)
  {
    return componentLegalService.saveComponentSourceLink(ownerType, ownerId, componentSourceLinkDTO);
  }
}
