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
import com.sonatype.insight.brain.api.v2.dto.legal.LicenseLegalFilterDTO;
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

  private final ApiLicenseLegalService apiLicenseLegalService;

  @Context
  private HttpServletRequest httpRequest;

  @Inject
  public ApiLicenseLegalResource(ApiLicenseLegalService apiLicenseLegalService) {
    this.apiLicenseLegalService = apiLicenseLegalService;
  }

  @POST
  @Path(DASHBOARD_APPLICATIONS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public List<ApiLicenseLegalApplicationDashboardDTO> getLicenseLegalApplicationsDashboard(
      LicenseLegalFilterDTO filter)
  {
    return apiLicenseLegalService.getLicenseLegalApplicationsDashboard(filter.organizationIds, filter.applicationIds,
        filter.tagIds, filter.stageTypeIds);
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
}
