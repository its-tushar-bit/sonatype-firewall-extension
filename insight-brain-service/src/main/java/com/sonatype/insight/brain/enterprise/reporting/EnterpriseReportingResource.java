/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.enterprise.reporting;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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

  public static final String SAVED_FILTERS_PATH = "filters";

  public static final String DELETE_FILTERS_PATH = SAVED_FILTERS_PATH + "/{filterId}";

  public static final String DEFAULT_FILTER_PATH = SAVED_FILTERS_PATH + "/default";

  public static final String UPDATE_DEFAULT_FILTERS_PATH = DEFAULT_FILTER_PATH + "/{filterId}";

  private final EnterpriseReportingService enterpriseReportingService;

  private final EnterpriseReportingFilterService enterpriseReportingFilterService;

  @Inject
  public EnterpriseReportingResource(
      final EnterpriseReportingService enterpriseReportingService,
      final EnterpriseReportingFilterService enterpriseReportingFilterService)
  {
    this.enterpriseReportingService = enterpriseReportingService;
    this.enterpriseReportingFilterService = enterpriseReportingFilterService;
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
      @QueryParam("embedDomain") String encodedEmbedDomain)
  {
    return enterpriseReportingService.acquireEmbedSession(dashboardId, encodedEmbedDomain, clientUserAgent);
  }

  @PUT
  @Path(GENERATE_EMBED_TOKENS)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public EmbedCookielessSessionGenerateTokensResponse generateEmbedTokens(
      @HeaderParam(USER_AGENT) String clientUserAgent,
      EmbedCookielessSessionGenerateTokens embedCookielessSessionGenerateTokens)
  {
    return enterpriseReportingService.generateEmbedTokens(embedCookielessSessionGenerateTokens, clientUserAgent);
  }

  @GET
  @Path(SAVED_FILTERS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public List<EnterpriseReportingDashboardFilterDTO> getFiltersForCurrentUser() {
    return enterpriseReportingFilterService.getFiltersForCurrentUser();
  }

  @POST
  @Path(SAVED_FILTERS_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public EnterpriseReportingDashboardFilterDTO createFilterForCurrentUser(
      EnterpriseReportingDashboardFilterDTO filterDTO)
  {
    return enterpriseReportingFilterService.upsertFilterForCurrentUser(filterDTO);
  }

  @PUT
  @Path(SAVED_FILTERS_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public EnterpriseReportingDashboardFilterDTO updateFilterForCurrentUser(
      EnterpriseReportingDashboardFilterDTO filterDTO)
  {
    return enterpriseReportingFilterService.upsertFilterForCurrentUser(filterDTO);
  }

  @DELETE
  @Path(DELETE_FILTERS_PATH)
  public void deleteDashboardFilterForCurrentUser(@PathParam("filterId") String filterId) {
    enterpriseReportingFilterService.deleteFilterForCurrentUser(filterId);
  }

  @GET
  @Path(DEFAULT_FILTER_PATH)
  @Produces(MediaType.TEXT_PLAIN)
  public String getDefaultFilterForCurrentUser() {
    return enterpriseReportingFilterService.getDefaultFilterForCurrentUser();
  }

  @DELETE
  @Path(DEFAULT_FILTER_PATH)
  public void deleteDefaultFilterForCurrentUser() {
    enterpriseReportingFilterService.deleteDefaultFilterForCurrentUser();
  }

  @PUT
  @Path(UPDATE_DEFAULT_FILTERS_PATH)
  @Produces(MediaType.TEXT_PLAIN)
  public String setDefaultFilterForCurrentUser(@PathParam("filterId") String filterId) {
    return enterpriseReportingFilterService.setDefaultFilterForCurrentUser(filterId);
  }
}
