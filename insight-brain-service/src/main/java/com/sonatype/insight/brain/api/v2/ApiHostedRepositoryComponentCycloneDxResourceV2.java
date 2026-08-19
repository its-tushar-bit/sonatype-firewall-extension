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
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.service.ApiCycloneDxServiceV2;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.dataaccess.repository.HostedRepositoryComponentDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.scan.file.ThirdPartyUtils;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.core.Context;
import org.apache.commons.collections4.CollectionUtils;
import org.cyclonedx.Version;

@Named
@Timed
@Singleton
@Path(PublicApiPaths.CYCLONE_DX_RESOURCE_PATH)
@Tag(name = "CycloneDX")
@ProductLicenseEnforcementPoint(LicensedFeature.SBOM_REPORTS)
@HasFeature(SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION)
public class ApiHostedRepositoryComponentCycloneDxResourceV2
{
  static final String GET_BY_STAGE_PATH = "hostedRepositoryComponent/{hrcId}/stages/{stageId}";

  static final String GET_BY_STAGE_PATH_WITH_VERSION =
      "{cdxVersion: 1.1|1.2|1.3|1.4|1.5|1.6|1.7}/hostedRepositoryComponent/{hrcId}/stages/{stageId}";

  static final String GET_BY_REPORT_PATH = "hostedRepositoryComponent/{hrcId}/reports/{reportId}";

  static final String GET_BY_REPORT_PATH_WITH_VERSION =
      "{cdxVersion: 1.1|1.2|1.3|1.4|1.5|1.6|1.7}/hostedRepositoryComponent/{hrcId}/reports/{reportId}";

  private final ApiCycloneDxServiceV2 apiCycloneDxService;

  private final HostedRepositoryComponentDAO hostedRepositoryComponentDAO;

  @Inject
  public ApiHostedRepositoryComponentCycloneDxResourceV2(
      ApiCycloneDxServiceV2 apiCycloneDxService,
      HostedRepositoryComponentDAO hostedRepositoryComponentDAO)
  {
    this.apiCycloneDxService = apiCycloneDxService;
    this.hostedRepositoryComponentDAO = hostedRepositoryComponentDAO;
  }

  @GET
  @Path(GET_BY_STAGE_PATH)
  @Produces(MediaType.APPLICATION_XML)
  @Audited(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT)
  public Response getLatest(
      @PathParam("hrcId") String hrcId,
      @PathParam("stageId") String stageId)
  {
    return apiCycloneDxService.getLatest(hostedRepositoryComponentDAO.getByIdNotNull(hrcId), stageId,
        MediaType.APPLICATION_XML, Version.VERSION_11);
  }

  @GET
  @Path(GET_BY_STAGE_PATH_WITH_VERSION)
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  @Audited(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT)
  public Response getLatestWithVersion(
      @PathParam("hrcId") String hrcId,
      @PathParam("stageId") String stageId,
      @PathParam("cdxVersion") String cycloneDxVersion,
      @Context HttpHeaders headers)
  {
    return apiCycloneDxService.getLatest(hostedRepositoryComponentDAO.getByIdNotNull(hrcId), stageId,
        determineAcceptableMediaType(headers), ThirdPartyUtils.getCycloneDxSchemaVersion(cycloneDxVersion));
  }

  @GET
  @Path(GET_BY_REPORT_PATH)
  @Produces(MediaType.APPLICATION_XML)
  @Audited(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT)
  public Response getByReportId(
      @PathParam("hrcId") String hrcId,
      @PathParam("reportId") String reportId)
  {
    return apiCycloneDxService.getByScanId(hostedRepositoryComponentDAO.getByIdNotNull(hrcId), reportId,
        MediaType.APPLICATION_XML, Version.VERSION_11);
  }

  @GET
  @Path(GET_BY_REPORT_PATH_WITH_VERSION)
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  @Audited(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT)
  public Response getByReportIdWithVersion(
      @PathParam("hrcId") String hrcId,
      @PathParam("reportId") String reportId,
      @PathParam("cdxVersion") String cycloneDxVersion,
      @Context HttpHeaders headers)
  {
    return apiCycloneDxService.getByScanId(hostedRepositoryComponentDAO.getByIdNotNull(hrcId), reportId,
        determineAcceptableMediaType(headers), ThirdPartyUtils.getCycloneDxSchemaVersion(cycloneDxVersion));
  }

  private String determineAcceptableMediaType(final HttpHeaders headers) {
    if (headers != null && CollectionUtils.isNotEmpty(headers.getAcceptableMediaTypes())
        && MediaType.APPLICATION_JSON_TYPE.equals(headers.getAcceptableMediaTypes().get(0)))
    {
      return MediaType.APPLICATION_JSON_TYPE.toString();
    }
    return MediaType.APPLICATION_XML_TYPE.toString();
  }
}
