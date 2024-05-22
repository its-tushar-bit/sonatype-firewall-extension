/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.SbomsAnalyzedMetricsDTO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.thirdpartyscans.ApiSbomApplicationsHistoryMetricDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.VulnerabilitiesThreadLevelMetricDTO;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.product.license.ProductLicense;

import org.apache.commons.lang3.tuple.Pair;

@Named
public class ApiSbomDashboardService
{
  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  private final ApplicationService applicationService;

  private final ProductLicense productLicense;

  @Inject
  public ApiSbomDashboardService(
      ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO,
      ApplicationService applicationService,
      ProductLicense productLicense)
  {
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.thirdPartyCoordinateSecurityDAO = thirdPartyCoordinateSecurityDAO;
    this.applicationService = applicationService;
    this.productLicense = productLicense;
  }

  public SbomsAnalyzedMetricsDTO getSbomsAnalyzedMetrics() {
    return new SbomsAnalyzedMetricsDTO(thirdPartySbomMetadataDAO.getActiveSbomCount(), productLicense.getMaxSboms());
  }

  public ApiSbomApplicationsHistoryMetricDTO getApplicationsHistoryMetric() {
    return thirdPartySbomMetadataDAO.getSbomsHistoryMetrics();
  }

  public VulnerabilitiesThreadLevelMetricDTO getVulnerabilitiesByThreatLevel() {
    Pair<List<Application>, Boolean> pair = applicationService.getApplicationsAndCheckIfAll();
    List<Application> applications = pair.getLeft();
    boolean hasPermissionInAllApps = pair.getRight();

    if (applications.isEmpty()) {
      return new VulnerabilitiesThreadLevelMetricDTO();
    }

    Set<String> applicationIds = hasPermissionInAllApps
        ? Collections.emptySet()
        : applications.stream().map(Application::getId).collect(Collectors.toSet());

    return thirdPartyCoordinateSecurityDAO.getVulnerabilitiesByThreatLevel(applicationIds);
  }
}
