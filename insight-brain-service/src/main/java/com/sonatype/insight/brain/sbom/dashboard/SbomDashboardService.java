/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.dashboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.SbomsAnalyzedMetricsDTO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.thirdpartyscans.ApiSbomApplicationsHistoryMetricDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.RecentImportedSbomsDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.RecentVulnerabilitiesDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ReleaseStatusDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.VulnerabilitiesThreadLevelMetricDTO;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.product.license.ProductLicense;

import org.apache.commons.lang3.tuple.Pair;

@Named
public class SbomDashboardService
{
  private final ApplicationService applicationService;

  private final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final ProductLicense productLicense;

  @Inject
  public SbomDashboardService(
      final ApplicationService applicationService,
      final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO,
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final ProductLicense productLicense)
  {
    this.applicationService = applicationService;
    this.thirdPartyCoordinateSecurityDAO = thirdPartyCoordinateSecurityDAO;
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.productLicense = productLicense;
  }

  public List<RecentVulnerabilitiesDTO> getRecentHighPriorityVulnerabilities() {
    List<Application> applications = applicationService.getApplications();
    Set<String> applicationIds = applications.stream().map(Application::getId).collect(Collectors.toSet());
    if (applicationIds.isEmpty()) {
      return new ArrayList<>();
    }
    return thirdPartyCoordinateSecurityDAO.getRecentHighPriorityVulnerabilities(applicationIds);
  }

  public ReleaseStatusDTO getSbomReleaseStatus() {
    List<Application> applications = applicationService.getApplications();
    Set<String> applicationIds = applications.stream().map(Application::getId).collect(Collectors.toSet());
    if (applicationIds.isEmpty()) {
      return new ReleaseStatusDTO();
    }
    long needsAttentionCount = thirdPartyCoordinateSecurityDAO.getSbomReleaseStatusNeedsAttention(applicationIds);
    long partiallyReadyCount = thirdPartyCoordinateSecurityDAO.getSbomReleaseStatusPartiallyReady(applicationIds);
    long releaseReadyCount = thirdPartyCoordinateSecurityDAO.getSbomReleaseStatusReleaseReady(applicationIds);

    ReleaseStatusDTO result = new ReleaseStatusDTO(releaseReadyCount, partiallyReadyCount, needsAttentionCount);
    return result;
  }

  public List<RecentImportedSbomsDTO> getRecentSbomsImported() {
    List<Application> applications = applicationService.getApplications();
    Set<String> applicationIds = applications.stream().map(Application::getId).collect(Collectors.toSet());
    if (applicationIds.isEmpty()) {
      return new ArrayList<>();
    }
    List<RecentImportedSbomsDTO> sbomsDTOList = thirdPartyCoordinateSecurityDAO.getRecentImportedSboms(applicationIds);
    Map<String, Application> applicationsMap = applications.stream()
        .collect(Collectors.toMap(Application::getId, Function.identity()));
    sbomsDTOList.forEach(recentImported -> {
      Application application = applicationsMap.get(recentImported.getApplicationId());
      recentImported.setApplicationName(application.getName());
      recentImported.setPublicApplicationId(application.getPublicId());
    });
    return sbomsDTOList;
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
