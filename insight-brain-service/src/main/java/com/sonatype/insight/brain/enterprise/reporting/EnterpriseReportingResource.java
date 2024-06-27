/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.enterprise.reporting;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sonatype.clm.dto.model.looker.EmbedCookielessSessionAcquire;
import com.sonatype.clm.dto.model.looker.EmbedCookielessSessionGenerateTokens;
import com.sonatype.clm.dto.model.looker.EmbedCookielessSessionGenerateTokensResponse;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import com.codahale.metrics.annotation.Timed;

import static org.apache.http.HttpHeaders.USER_AGENT;

@Named
@Timed
@Path(EnterpriseReportingResource.RESOURCE_PATH)
public class EnterpriseReportingResource
{
  public static final String RESOURCE_PATH = "rest/enterpriseReporting";

  public static final String GET_BASE_URL = "getBaseUrl";

  public static final String DASHBOARDS_METADATA_PATH = "dashboards";

  public static final String GET_IER_ICON_PATH = "dashboard/icons/{iconName}";

  public static final String ACQUIRE_EMBED_SESSION = "acquireEmbedSession";

  public static final String GENERATE_EMBED_TOKENS = "generateEmbedTokens";

  private final EnterpriseReportingService enterpriseReportingService;

  @Inject
  public EnterpriseReportingResource(final EnterpriseReportingService enterpriseReportingService) {
    this.enterpriseReportingService = enterpriseReportingService;
  }

  @GET
  @Path(GET_BASE_URL)
  @Produces(MediaType.TEXT_PLAIN)
  public String getBaseUrl() {
    return enterpriseReportingService.getEnterpriseReportingConfigDTOBaseUrl();
  }

  @GET
  @Path(DASHBOARDS_METADATA_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public DashboardMetadataListDTO getDashboardMetadata() {
    return enterpriseReportingService.getDashboardMetadata();
  }

  @GET
  @Path(GET_IER_ICON_PATH)
  @Produces("image/svg+xml")
  public Response getIcon(@PathParam("iconName") final String iconName) {
    return Response.ok(enterpriseReportingService.getIcon(iconName)).build();
  }

  @GET
  @Path(ACQUIRE_EMBED_SESSION)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_INTEGRATED_ENTERPRISE_REPORTING_DASHBOARD)
  public EmbedCookielessSessionAcquire acquireEmbedSession(
      @HeaderParam(USER_AGENT) String clientUserAgent,
      @QueryParam("dashboardId") String dashboardId,
      @QueryParam("embedDomain") String encodedEmbedDomain
  )
  {
    return enterpriseReportingService.acquireEmbedSession(dashboardId, encodedEmbedDomain, clientUserAgent);
  }

  @PUT
  @Path(GENERATE_EMBED_TOKENS)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public EmbedCookielessSessionGenerateTokensResponse generateEmbedTokens(
      @HeaderParam(USER_AGENT) String clientUserAgent,
      EmbedCookielessSessionGenerateTokens embedCookielessSessionGenerateTokens
  )
  {
    return enterpriseReportingService.generateEmbedTokens(embedCookielessSessionGenerateTokens, clientUserAgent);
  }
}
