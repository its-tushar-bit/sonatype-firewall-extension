/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentWaiversDTO;
import com.sonatype.insight.brain.api.v2.service.ApiComponentsWithWaiversReportingService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.76
 */
@Named
@Timed
@Path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2 + ApiComponentsWithWaiversReportingResource.PATH)
@Consumes(MediaType.APPLICATION_JSON)
@ProductLicenseEnforcementPoint(LicensedFeature.WAIVER_REPORTS)
public class ApiComponentsWithWaiversReportingResource
{
  public static final String PATH = "/components/waivers";

  private final ApiComponentsWithWaiversReportingService componentsWithWaiversReportingService;

  @Inject
  public ApiComponentsWithWaiversReportingResource(
      ApiComponentsWithWaiversReportingService componentsWithWaiversReportingService)
  {
    this.componentsWithWaiversReportingService = componentsWithWaiversReportingService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_COMPONENTS_WITH_WAIVERS)
  public ApiComponentWaiversDTO getComponentsWithWaivers(@QueryParam("format") String format) {
    return componentsWithWaiversReportingService.getComponentsWithWaivers(format);
  }
}
