/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.dashboard;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.model.thirdpartyscans.RecentVulnerabilitiesDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ReleaseStatusDTO;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;

@Named
@Timed
@Singleton
@Path(SbomDashboardResource.RESOURCE_BASE_PATH)
public class SbomDashboardResource
{
  public static final String RESOURCE_BASE_PATH = "rest/sbom/dashboard";

  static final String SBOMS_HIGH_PRIORITY_VULNERABILITIES = "highPriorityVulnerabilities";

  static final String SBOM_RELEASE_STATUS = "sbomReleaseStatus";

  private final SbomDashboardService service;

  @Inject
  public SbomDashboardResource(SbomDashboardService service) {
    this.service = service;
  }

  @GET
  @Path(SBOMS_HIGH_PRIORITY_VULNERABILITIES)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  @Produces(MediaType.APPLICATION_JSON)
  public List<RecentVulnerabilitiesDTO> getRecentHighPriorityVulnerabilities() {
    return service.getRecentHighPriorityVulnerabilities();
  }

  @GET
  @Path(SBOM_RELEASE_STATUS)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  @Produces(MediaType.APPLICATION_JSON)
  public ReleaseStatusDTO getSbomReleaseStatus() {
    return service.getSbomReleaseStatus();
  }
}
