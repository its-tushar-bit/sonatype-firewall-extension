/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.experimental.legal.report.ApplicationAttributionReportBuilder;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationDashboardResultDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentDashboardDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalObligationDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentCopyrightDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentCopyrightWithOwnerDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentLegalFileDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentObligationAttributionDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.CopyrightFilePathsDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LicenseLegalApplicationComponentsFilterDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LicenseLegalFilterDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.legal.LegalFileType;

import com.codahale.metrics.annotation.Timed;

@Named
@Timed
@Path(PublicApiPaths.LICENSE_LEGAL_RESOURCE_PATH)
public class ApiLicenseLegalResource
{
  public static final String DASHBOARD_APPLICATIONS_PATH = "dashboard/applications";

  public static final String DASHBOARD_COMPONENTS_PATH = "dashboard/components";

  public static final String DASHBOARD_APPLICATION_PATH = "dashboard/application/{applicationPublicId}";

  public static final String APPLICATION_PATH = "application/{applicationPublicId}";

  public static final String APPLICATION_REPORT_PATH = APPLICATION_PATH + "/report";

  public static final String COMPONENT_PATH = "{ownerType: application|organization}/{ownerId}/component";

  public static final String COMPONENT_COPYRIGHT_PATH = COMPONENT_PATH + "/copyright";

  public static final String COMPONENT_LEGAL_FILE_PATH = COMPONENT_PATH + "/legalFile";

  public static final String COMPONENT_OBLIGATION_PATH = COMPONENT_PATH + "/obligation";

  public static final String COMPONENT_OBLIGATION_DELETE_PATH = "/component/obligation/{componentObligationId}";

  public static final String COMPONENT_OBLIGATION_ATTRIBUTION_PATH = COMPONENT_OBLIGATION_PATH + "/attribution";

  public static final String COMPONENT_OBLIGATION_ATTRIBUTION_DELETE_PATH =
      "/component/obligation/attribution/{componentObligationAttributionId}";

  public static final String COMPONENT_COPYRIGHT_FILEPATHS =
      COMPONENT_PATH + "{componentHash}/copyright/{copyrightContentHash}/filepaths";

  public static final String COMPONENT_COPYRIGHT_FILEPATH_CONTEXT =
      COMPONENT_PATH + "{componentHash}/copyright/{copyrightContentHash}/file/{filePath}/context";

  public static final String COMPONENT_COPYRIGHT_FILE_COUNT = COMPONENT_PATH + "{componentHash}/copyright/fileCount";

  private final ApiLicenseLegalService apiLicenseLegalService;

  private final ComponentLegalService componentLegalService;

  private final ApiLegalCopyrightService apiLegalCopyrightService;

  private final ApplicationAttributionReportBuilder applicationAttributionReportBuilder;

  private final LegalApplicationDashboardService legalApplicationDashboardService;

  @Context
  private HttpServletRequest httpRequest;

  @Inject
  public ApiLicenseLegalResource(
      final ApiLicenseLegalService apiLicenseLegalService,
      final ComponentLegalService componentLegalService,
      final ApiLegalCopyrightService apiLegalCopyrightService,
      final LegalApplicationDashboardService legalApplicationDashboardService,
      final ApplicationAttributionReportBuilder applicationAttributionReportBuilder)
  {
    this.apiLicenseLegalService = apiLicenseLegalService;
    this.componentLegalService = componentLegalService;
    this.apiLegalCopyrightService = apiLegalCopyrightService;
    this.legalApplicationDashboardService = legalApplicationDashboardService;
    this.applicationAttributionReportBuilder = applicationAttributionReportBuilder;
  }

  @POST
  @Path(DASHBOARD_APPLICATIONS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public ApiLicenseLegalApplicationDashboardResultDTO getLicenseLegalApplicationsDashboard(
      LicenseLegalFilterDTO filter)
  {
    return apiLicenseLegalService.getLicenseLegalApplicationsDashboard(filter.organizationIds, filter.applicationIds,
        filter.tagIds, filter.stageTypeIds, filter.licenseIds, filter.reviewStatus, filter.order, filter.page,
        filter.pageSize);
  }

  @POST
  @Path(DASHBOARD_COMPONENTS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public List<ApiLicenseLegalComponentDashboardDTO> getLicenseLegalComponentsDashboard(LicenseLegalFilterDTO filter) {
    return apiLicenseLegalService.getLicenseLegalComponentsDashboard(filter.organizationIds, filter.applicationIds,
        filter.tagIds, filter.stageTypeIds, filter.licenseIds);
  }

  @POST
  @Path(DASHBOARD_APPLICATION_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public List<ApiLicenseLegalApplicationComponentDTO> getLicenseLegalApplicationDashboard(
      @PathParam("applicationPublicId") String applicationPublicId,
      LicenseLegalApplicationComponentsFilterDTO filter)
  {
    return legalApplicationDashboardService.getLicenseLegalApplicationDashboard(applicationPublicId, filter);
  }

  @GET
  @Path(APPLICATION_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public ApiLicenseLegalApplicationReportDTO getLicenseLegalApplicationReport(
      @PathParam("applicationPublicId") String applicationPublicId)
  {
    return apiLicenseLegalService.getLicenseLegalApplicationReport(applicationPublicId);
  }

  @GET
  @Path(APPLICATION_REPORT_PATH)
  @Produces(MediaType.TEXT_HTML)
  public String getLicenseLegalApplicationHTMLReport(
      @PathParam("applicationPublicId") String applicationPublicId)
  {
    return applicationAttributionReportBuilder.generateLegalApplicationAttributionReport(applicationPublicId);
  }

  @GET
  @Path(COMPONENT_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public ApiLicenseLegalComponentReportDTO getLicenseLegalComponentReport(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId,
      @QueryParam("componentIdentifier") ComponentIdentifier componentIdentifier,
      @QueryParam("packageUrl") String packageUrl,
      @QueryParam("hash") String hash,
      @QueryParam("identificationSource") String identificationSource,
      @QueryParam("scanId") String scanId) throws IOException
  {
    return apiLicenseLegalService.getLicenseLegalComponentReport(ownerType, ownerId, componentIdentifier, packageUrl,
        hash, httpRequest, identificationSource, scanId);
  }

  @POST
  @Path(COMPONENT_COPYRIGHT_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_COMPONENT_COPYRIGHT)
  public ComponentCopyrightDTO saveComponentCopyright(
      ComponentCopyrightDTO componentCopyrightDTO,
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId)
  {
    return componentLegalService.saveComponentCopyright(ownerType, ownerId, componentCopyrightDTO);
  }

  @GET
  @Path(COMPONENT_COPYRIGHT_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public ComponentCopyrightWithOwnerDTO getComponentCopyright(
      @QueryParam("componentIdentifier") ComponentIdentifier componentIdentifier,
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId)
  {
    return componentLegalService.getComponentCopyrightWithHierarchy(ownerType, ownerId, componentIdentifier);
  }

  /**
   * @since 1.107
   */
  @POST
  @Path(COMPONENT_LEGAL_FILE_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_COMPONENT_LEGAL_FILE)
  public ComponentLegalFileDTO saveComponentLegalFile(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId,
      ComponentLegalFileDTO componentLegalFileDTO)
  {
    return componentLegalService.saveComponentLegalFile(ownerType, ownerId, componentLegalFileDTO);
  }

  /**
   * @since 1.107
   */
  @GET
  @Path(COMPONENT_LEGAL_FILE_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public ComponentLegalFileDTO getComponentLegalFile(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId,
      @QueryParam("componentIdentifier") ComponentIdentifier componentIdentifier,
      @QueryParam("legalFileType") LegalFileType legalFileType)
  {
    return componentLegalService.getComponentLegalFile(ownerType, ownerId, componentIdentifier, legalFileType);
  }

  /**
   * @since 1.106
   */
  @GET
  @Path(COMPONENT_OBLIGATION_ATTRIBUTION_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public List<ComponentObligationAttributionDTO> getComponentObligationAttribution(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId,
      @QueryParam("componentIdentifier") ComponentIdentifier componentIdentifier,
      @QueryParam("obligationName") String obligationName)
  {
    return componentLegalService
        .getComponentObligationAttributions(ownerType, ownerId, componentIdentifier, obligationName);
  }

  /**
   * @since 1.106
   */
  @POST
  @Path(COMPONENT_OBLIGATION_ATTRIBUTION_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_COMPONENT_OBLIGATION_ATTRIBUTION)
  public ComponentObligationAttributionDTO saveComponentObligationAttribution(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId,
      ComponentObligationAttributionDTO componentObligationAttributionDTO)
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
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.DELETE_COMPONENT_OBLIGATION_ATTRIBUTION)
  public void deleteComponentObligationAttribution(
      @PathParam("componentObligationAttributionId") String componentObligationAttributionId)
  {
    componentLegalService.deleteComponentObligationAttribution(componentObligationAttributionId);
  }

  /**
   * @since 1.106
   */
  @GET
  @Path(COMPONENT_OBLIGATION_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public ApiLicenseLegalObligationDTO getComponentObligation(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId,
      @QueryParam("componentIdentifier") ComponentIdentifier componentIdentifier,
      @QueryParam("obligationName") String obligationName)
  {
    return componentLegalService.getComponentObligation(ownerType, ownerId, componentIdentifier, obligationName);
  }

  /**
   * @since 1.106
   */
  @POST
  @Path(COMPONENT_OBLIGATION_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_COMPONENT_OBLIGATION)
  public ApiLicenseLegalObligationDTO saveComponentObligation(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId,
      ApiLicenseLegalObligationDTO componentObligationDTO)
  {
    if (componentObligationDTO.getId() != null) {
      AuditData.get().setEvent(AuditEvent.UPDATE_COMPONENT_OBLIGATION);
    }
    return componentLegalService.saveComponentObligation(ownerType, ownerId, componentObligationDTO);
  }

  /**
   * @since 1.106
   */
  @DELETE
  @Path(COMPONENT_OBLIGATION_DELETE_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.DELETE_COMPONENT_OBLIGATION)
  public void deleteComponentObligation(@PathParam("componentObligationId") String componentObligationId) {
    componentLegalService.deleteComponentObligation(componentObligationId);
  }

  /**
   * @since 1.108
   */
  @GET
  @Path(COMPONENT_COPYRIGHT_FILEPATHS)
  @Produces(MediaType.APPLICATION_JSON)
  public CopyrightFilePathsDTO getCopyrightContexts(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId,
      @PathParam("componentHash") String componentHash,
      @PathParam("copyrightContentHash") String copyrightContentHash,
      @QueryParam("componentIdentifier") ComponentIdentifier componentIdentifier,
      @QueryParam("pageStart") int pageStart,
      @QueryParam("pageLength") int pageLength)
  {
    return apiLegalCopyrightService.getCopyrightFilePaths(
        ownerType,
        ownerId,
        componentIdentifier,
        componentHash,
        copyrightContentHash,
        pageStart, pageLength);
  }

  /**
   * @since 1.108
   */
  @GET
  @Path(COMPONENT_COPYRIGHT_FILEPATH_CONTEXT)
  @Produces(MediaType.APPLICATION_JSON)
  public List<String> getCopyrightContexts(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId,
      @PathParam("componentHash") String componentHash,
      @PathParam("copyrightContentHash") String copyrightContentHash,
      @PathParam("filePath") String filePath,
      @QueryParam("componentIdentifier") ComponentIdentifier componentIdentifier)
  {
    return apiLegalCopyrightService.getCopyrightContextContent(
        ownerType,
        ownerId,
        componentIdentifier,
        componentHash,
        copyrightContentHash,
        filePath);
  }

  /**
   * @since 1.108
   */
  @GET
  @Path(COMPONENT_COPYRIGHT_FILE_COUNT)
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, Integer> getCopyrightFileCount(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId,
      @PathParam("componentHash") String componentHash,
      @QueryParam("componentIdentifier") ComponentIdentifier componentIdentifier)
  {
    return apiLegalCopyrightService.getCopyrightFileCount(
        ownerType,
        ownerId,
        componentIdentifier,
        componentHash);
  }
}
