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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import com.codahale.metrics.annotation.Timed;

@Named
@Timed
@Path(EnterpriseReportingResource.RESOURCE_PATH)
public class EnterpriseReportingResource
{
  public static final String RESOURCE_PATH = "rest/enterpriseReporting";

  public static final String SSO_EMBED_URL_PATH = "ssoEmbedUrl";

  public static final String DASHBOARDS_METADATA_PATH = "dashboards";

  public static final String GET_IER_ICON_PATH = "dashboard/icons/{iconName}";

  private final EnterpriseReportingService enterpriseReportingService;

  @Inject
  public EnterpriseReportingResource(final EnterpriseReportingService enterpriseReportingService) {
    this.enterpriseReportingService = enterpriseReportingService;
  }

  @POST
  @Path(SSO_EMBED_URL_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_INTEGRATED_ENTERPRISE_REPORTING_DASHBOARD)
  public SSOEmbedUrlDTO createSSOEmbedUrl(DashboardRequestDTO dashboardRequestDTO) {
    return enterpriseReportingService.createSSOEmbedUrl(dashboardRequestDTO);
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
}
