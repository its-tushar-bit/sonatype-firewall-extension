/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiReportHistoryDTO;
import com.sonatype.insight.brain.api.v2.service.ApiReportServiceV2;
import com.sonatype.insight.brain.dataaccess.repository.HostedRepositoryComponentDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2 + ApiHostedRepositoryComponentReportResourceV2.PATH)
@Named
@Singleton
@Timed
@ProductLicenseEnforcementPoint(LicensedFeature.APPLICATION_REPORTS)
@HasFeature(SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION)
@Tag(name = "Reports")
public class ApiHostedRepositoryComponentReportResourceV2
{
  public static final String PATH = "/hostedRepositoryComponent";

  private final ApiReportServiceV2 reportService;

  private final HostedRepositoryComponentDAO hostedRepositoryComponentDAO;

  @Inject
  public ApiHostedRepositoryComponentReportResourceV2(
      final ApiReportServiceV2 reportService,
      final HostedRepositoryComponentDAO hostedRepositoryComponentDAO)
  {
    this.reportService = reportService;
    this.hostedRepositoryComponentDAO = hostedRepositoryComponentDAO;
  }

  @GET
  @Path("{hrcId}/history")
  @Produces(MediaType.APPLICATION_JSON)
  public ApiReportHistoryDTO getReportHistory(
      @PathParam("hrcId") final String hrcId,
      @QueryParam("stage") final String stage,
      @QueryParam("limit") final Integer limit)
  {
    return reportService.getReportHistoryForOwner(hostedRepositoryComponentDAO.getByIdNotNull(hrcId), stage, limit);
  }
}
