/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentReportDTO;
import com.sonatype.insight.brain.api.v2.service.legal.ApiLicenseLegalService;
import com.sonatype.insight.brain.api.v2.service.legal.report.ApplicationAttributionReportBuilder;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.utils.IdUtils;

import com.codahale.metrics.annotation.Timed;

@Named
@Timed
@Path(PublicApiPaths.LICENSE_LEGAL_RESOURCE_PATH_V2)
public class DefaultApiLicenseLegalResourceV2
    implements ApiLicenseLegalResourceV2
{
  public static final String APPLICATION_PATH = "application/{applicationId}";

  public static final String APPLICATION_REPORT_PATH = APPLICATION_PATH + "/stage/{stageId}/report";

  public static final String COMPONENT_PATH = "{ownerType: application|organization}/{ownerId}/component";

  private final ApiLicenseLegalService apiLicenseLegalServiceV2;

  private final ApplicationAttributionReportBuilder applicationAttributionReportBuilder;

  @Context
  private HttpServletRequest httpRequest;

  @Inject
  public DefaultApiLicenseLegalResourceV2(
      ApiLicenseLegalService apiLicenseLegalService,
      ApplicationAttributionReportBuilder applicationAttributionReportBuilder)
  {
    this.apiLicenseLegalServiceV2 = apiLicenseLegalService;
    this.applicationAttributionReportBuilder = applicationAttributionReportBuilder;
  }

  @Override
  @GET
  @Path(APPLICATION_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public ApiLicenseLegalApplicationReportDTO getLicenseLegalApplicationReport(
      @PathParam("applicationId") String applicationId)
  {
    return apiLicenseLegalServiceV2
        .getLicenseLegalApplicationReport(IdUtils.getOwnerNotNull(OwnerType.APPLICATION, applicationId));
  }

  @Override
  @GET
  @Path(APPLICATION_REPORT_PATH)
  @Produces(MediaType.TEXT_HTML)
  public String getLicenseLegalApplicationHTMLReport(
      @PathParam("applicationId") String applicationId, @PathParam("stageId") String stageId)
  {
    return applicationAttributionReportBuilder
        .generateLegalApplicationAttributionReport(
            IdUtils.getOwnerNotNull(OwnerType.APPLICATION, applicationId), stageId);
  }

  @Override
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
    return apiLicenseLegalServiceV2.getLicenseLegalComponentReport(ownerType, ownerId, componentIdentifier, packageUrl,
        hash, httpRequest, identificationSource, scanId);
  }
}
