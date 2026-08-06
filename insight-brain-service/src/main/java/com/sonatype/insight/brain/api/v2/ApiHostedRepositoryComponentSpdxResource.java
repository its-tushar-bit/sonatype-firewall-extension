/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.service.ApiSpdxService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.dataaccess.repository.HostedRepositoryComponentDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.tags.Tag;

@Named
@Timed
@Singleton
@Path(PublicApiPaths.SPDX_RESOURCE_PATH)
@Tag(name = "SPDX")
@ProductLicenseEnforcementPoint(LicensedFeature.SBOM_REPORTS)
@HasFeature(SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION)
public class ApiHostedRepositoryComponentSpdxResource
{
  static final String GET_BY_STAGE_PATH = "hostedRepositoryComponent/{hrcId}/stages/{stageId}";

  static final String GET_BY_REPORT_PATH = "hostedRepositoryComponent/{hrcId}/reports/{scanId}";

  static final String DEFAULT_SPDX_FORMAT = "json";

  static final String DEFAULT_SPDX_VERSION = "2.3";

  private final ApiSpdxService apiSpdxService;

  private final HostedRepositoryComponentDAO hostedRepositoryComponentDAO;

  @Inject
  public ApiHostedRepositoryComponentSpdxResource(
      ApiSpdxService apiSpdxService,
      HostedRepositoryComponentDAO hostedRepositoryComponentDAO)
  {
    this.apiSpdxService = apiSpdxService;
    this.hostedRepositoryComponentDAO = hostedRepositoryComponentDAO;
  }

  @GET
  @Path(GET_BY_STAGE_PATH)
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM})
  @Audited(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT)
  public Response getLatestForStage(
      @PathParam("hrcId") String hrcId,
      @PathParam("stageId") String stageId,
      @DefaultValue(DEFAULT_SPDX_FORMAT) @QueryParam("format") String format,
      @DefaultValue("false") @QueryParam("generateCycloneDx") boolean generateCycloneDx,
      @DefaultValue(DEFAULT_SPDX_VERSION) @QueryParam("spdxVersion") String spdxVersion)
  {
    return apiSpdxService.getLatestForStage(hostedRepositoryComponentDAO.getByIdNotNull(hrcId), stageId, format,
        generateCycloneDx, spdxVersion);
  }

  @GET
  @Path(GET_BY_REPORT_PATH)
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM})
  @Audited(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT)
  public Response getByScanId(
      @PathParam("hrcId") String hrcId,
      @PathParam("scanId") String scanId,
      @DefaultValue(DEFAULT_SPDX_FORMAT) @QueryParam("format") String format,
      @DefaultValue("false") @QueryParam("generateCycloneDx") boolean generateCycloneDx,
      @DefaultValue(DEFAULT_SPDX_VERSION) @QueryParam("spdxVersion") String spdxVersion)
  {
    return apiSpdxService.getByScanId(hostedRepositoryComponentDAO.getByIdNotNull(hrcId), scanId, format,
        generateCycloneDx, spdxVersion);
  }
}
