/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.service.ApiCycloneDxServiceV2;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.thirdparty.ThirdPartyUtils;

import com.codahale.metrics.annotation.Timed;
import org.apache.commons.collections4.CollectionUtils;
import org.cyclonedx.CycloneDxSchema.Version;

/**
 * @since 1.70
 */
@Named
@Timed
@Singleton
@Path(PublicApiPaths.CYCLONE_DX_RESOURCE_PATH)
public class ApiCycloneDxResourceV2
{
  static final String GET_BY_STAGE_PATH = "{applicationId}/stages/{stageId}";

  static final String GET_BY_STAGE_PATH_WITH_VERSION = "{cdxVersion: 1.1|1.2|1.3|1.4}/{applicationId}/stages/{stageId}";

  static final String GET_BY_REPORT_PATH = "{applicationId}/reports/{reportId}";

  /**
   * When adding a new version or changing this path, please update
   * {@link UserInterfaceLinksResource#linkToSbom(String, String)} as well.
   */
  static final String GET_BY_REPORT_PATH_WITH_VERSION =
      "{cdxVersion: 1.1|1.2|1.3|1.4}/{applicationId}/reports/{reportId}";

  private final ApiCycloneDxServiceV2 apiCycloneDxService;

  @Inject
  public ApiCycloneDxResourceV2(ApiCycloneDxServiceV2 apiCycloneDxService) {
    this.apiCycloneDxService = apiCycloneDxService;
  }

  @GET
  @Path(GET_BY_STAGE_PATH)
  @Produces(MediaType.APPLICATION_XML)
  @Audited(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT)
  public Response getLatest(
      @PathParam("applicationId") String applicationId,
      @PathParam("stageId") String stageId)
  {
    return apiCycloneDxService.getLatest(applicationId, stageId, MediaType.APPLICATION_XML, Version.VERSION_11);
  }

  @GET
  @Path(GET_BY_STAGE_PATH_WITH_VERSION)
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  @Audited(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT)
  public Response getLatest(
      @PathParam("applicationId") String applicationId,
      @PathParam("stageId") String stageId,
      @PathParam("cdxVersion") String cycloneDxVersion,
      @Context HttpHeaders headers)
  {
    String acceptType = determineAcceptableMediaType(headers);
    return apiCycloneDxService.getLatest(applicationId, stageId, acceptType,
        ThirdPartyUtils.getSchemaVersion(cycloneDxVersion));
  }

  @GET
  @Path(GET_BY_REPORT_PATH)
  @Produces(MediaType.APPLICATION_XML)
  @Audited(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT)
  public Response getByReportId(
      @PathParam("applicationId") String applicationId,
      @PathParam("reportId") String reportId)
  {
    return apiCycloneDxService
        .getByScanId(applicationId, reportId, MediaType.APPLICATION_XML, Version.VERSION_11);
  }

  @GET
  @Path(GET_BY_REPORT_PATH_WITH_VERSION)
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  @Audited(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT)
  public Response getByReportId(
      @PathParam("applicationId") String applicationId,
      @PathParam("reportId") String reportId,
      @PathParam("cdxVersion") String cycloneDxVersion,
      @Context HttpHeaders headers)
  {
    String acceptType = determineAcceptableMediaType(headers);
    return apiCycloneDxService.getByScanId(applicationId, reportId, acceptType,
        ThirdPartyUtils.getSchemaVersion(cycloneDxVersion));
  }

  private String determineAcceptableMediaType(final HttpHeaders headers) {
    if (headers != null && CollectionUtils.isNotEmpty(headers.getAcceptableMediaTypes())) {
      if (MediaType.APPLICATION_JSON_TYPE.equals(headers.getAcceptableMediaTypes().get(0))) {
        return MediaType.APPLICATION_JSON_TYPE.toString();
      }
    }
    return MediaType.APPLICATION_XML_TYPE.toString();
  }
}
