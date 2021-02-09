/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import java.io.IOException;
import java.util.List;

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
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationDashboardDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentDashboardDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentCopyrightDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentObligationAttributionDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LicenseLegalFilterDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;

import com.codahale.metrics.annotation.Timed;

@Named
@Timed
@Path(PublicApiPaths.LICENSE_LEGAL_RESOURCE_PATH)
public class ApiLicenseLegalResource
{
  public static final String DASHBOARD_APPLICATIONS_PATH = "dashboard/applications";

  public static final String DASHBOARD_COMPONENTS_PATH = "dashboard/components";

  public static final String APPLICATION_PATH = "application/{applicationPublicId}";

  public static final String COMPONENT_PATH = "{ownerType: application|organization}/{ownerId}/component";

  public static final String COMPONENT_COPYRIGHT_PATH =
      "{ownerType: application|organization}/{ownerId}/component/copyright";

  public static final String COMPONENT_OBLIGATION_PATH =
      "{ownerType: application|organization}/{ownerId}/component/obligation";

  public static final String COMPONENT_OBLIGATION_ATTRIBUTION_PATH = COMPONENT_OBLIGATION_PATH + "/attribution";

  public static final String COMPONENT_OBLIGATION_ATTRIBUTION_DELETE_PATH =
      "/component/obligation/attribution/{componentObligationAttributionId}";

  private final ApiLicenseLegalService apiLicenseLegalService;

  private final ComponentLegalService componentLegalService;

  @Context
  private HttpServletRequest httpRequest;

  @Inject
  public ApiLicenseLegalResource(
      final ApiLicenseLegalService apiLicenseLegalService,
      final ComponentLegalService componentLegalService)
  {
    this.apiLicenseLegalService = apiLicenseLegalService;
    this.componentLegalService = componentLegalService;
  }

  @POST
  @Path(DASHBOARD_APPLICATIONS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public List<ApiLicenseLegalApplicationDashboardDTO> getLicenseLegalApplicationsDashboard(
      LicenseLegalFilterDTO filter)
  {
    return apiLicenseLegalService.getLicenseLegalApplicationsDashboard(filter.organizationIds, filter.applicationIds,
        filter.tagIds, filter.stageTypeIds, filter.licenseIds, filter.order, filter.page, filter.pageSize);
  }

  @POST
  @Path(DASHBOARD_COMPONENTS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public List<ApiLicenseLegalComponentDashboardDTO> getLicenseLegalComponentsDashboard(LicenseLegalFilterDTO filter) {
    return apiLicenseLegalService.getLicenseLegalComponentsDashboard(filter.organizationIds, filter.applicationIds,
        filter.tagIds, filter.stageTypeIds, filter.licenseIds);
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
}
