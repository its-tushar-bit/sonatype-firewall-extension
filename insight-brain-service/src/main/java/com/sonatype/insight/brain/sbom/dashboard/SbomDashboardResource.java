/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.dashboard;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.v2.dto.SbomsAnalyzedMetricsDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ApiSbomApplicationsHistoryMetricDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.RecentImportedSbomsDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.RecentVulnerabilitiesDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ReleaseStatusDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.VulnerabilitiesThreadLevelMetricDTO;
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

  static final String SBOMS_RECENTLY_IMPORTED = "recentlyImportedSboms";

  static final String SBOM_RELEASE_STATUS = "sbomReleaseStatus";

  static final String SBOMS_ANALYZED_PATH = "sbomsAnalyzed";

  static final String SBOMS_HISTORY_METRICS_PATH = "sbomsHistoryMetrics";

  static final String SBOMS_VULNERABILITES_BY_THREAT_LEVEL_PATH = "vulnerabilitiesByThreatLevel";

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
  @Path(SBOMS_RECENTLY_IMPORTED)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  @Produces(MediaType.APPLICATION_JSON)
  public List<RecentImportedSbomsDTO> getRecentSbomsImported() {
    return service.getRecentSbomsImported();
  }

  @GET
  @Path(SBOM_RELEASE_STATUS)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  @Produces(MediaType.APPLICATION_JSON)
  public ReleaseStatusDTO getSbomReleaseStatus() {
    return service.getSbomReleaseStatus();
  }

  @GET
  @Path(SBOMS_ANALYZED_PATH)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  @Produces(MediaType.APPLICATION_JSON)
  public SbomsAnalyzedMetricsDTO getSbomsAnalyzedMetrics() {
    return service.getSbomsAnalyzedMetrics();
  }

  @GET
  @Path(SBOMS_HISTORY_METRICS_PATH)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  @Produces(MediaType.APPLICATION_JSON)
  public ApiSbomApplicationsHistoryMetricDTO getApplicationsHistoryMetric() {
    return service.getApplicationsHistoryMetric();
  }

  @GET
  @Path(SBOMS_VULNERABILITES_BY_THREAT_LEVEL_PATH)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  @Produces(MediaType.APPLICATION_JSON)
  public VulnerabilitiesThreadLevelMetricDTO getVulnerabilitiesByThreatLevel() {
    return service.getVulnerabilitiesByThreatLevel();
  }
}
