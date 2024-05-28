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
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.SbomsAnalyzedMetricsDTO;
import com.sonatype.insight.brain.api.v2.service.ApiSbomDashboardService;
import com.sonatype.insight.brain.model.thirdpartyscans.ApiSbomApplicationsHistoryMetricDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.VulnerabilitiesThreadLevelMetricDTO;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Named
@Timed
@Singleton
@Path(PublicApiPaths.SBOM_DASHBOARD_RESOURCE_PATH)
public class ApiSbomDashboardResource
{
  static final String SBOMS_ANALYZED_PATH = "sbomsAnalyzed";

  static final String SBOMS_HISTORY_METRICS_PATH = "sbomsHistoryMetrics";

  static final String SBOMS_VULNERABILITES_BY_THREAT_LEVEL_PATH = "vulnerabilitiesByThreatLevel";

  private final ApiSbomDashboardService service;

  @Inject
  public ApiSbomDashboardResource(ApiSbomDashboardService service) {
    this.service = service;
  }

  @Operation(summary = "Gets total of SBOMs analyzed and the threshold in the product license",
      tags = {"sbom dashboard"},
      description = "Queries how many SBOMs have been analyzed and the threshold in the product license",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Total of SBOMs analyzed and the threshold in the product license",
              content = @Content(mediaType = "application/json"))
      })

  @GET
  @Path(SBOMS_ANALYZED_PATH)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  @Produces(MediaType.APPLICATION_JSON)
  public SbomsAnalyzedMetricsDTO getSbomsAnalyzedMetrics() {
    return service.getSbomsAnalyzedMetrics();
  }

  @Operation(summary = "Gets application history metrics",
      tags = {"sbom dashboard"},
      description = "Queries how many SBOMs applications have been analyzed",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Total of SBOMs applications analyzed",
              content = @Content(mediaType = "application/json"))
      })

  @GET
  @Path(SBOMS_HISTORY_METRICS_PATH)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  @Produces(MediaType.APPLICATION_JSON)
  public ApiSbomApplicationsHistoryMetricDTO getApplicationsHistoryMetric() {
    return service.getApplicationsHistoryMetric();
  }

  @Operation(summary = "Gets counters of vulnerabilities and annotations by threat level",
      tags = {"sbom dashboard"},
      description = "Queries how many vulnerabilities and annotations have been found by each threat level",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Counters of vulnerabilities and annotations by threat level",
              content = @Content(mediaType = "application/json"))
      })

  @GET
  @Path(SBOMS_VULNERABILITES_BY_THREAT_LEVEL_PATH)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  @Produces(MediaType.APPLICATION_JSON)
  public VulnerabilitiesThreadLevelMetricDTO getVulnerabilitiesByThreatLevel() {
    return service.getVulnerabilitiesByThreatLevel();
  }
}
