/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.componentsearch.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.sonatype.clm.dto.model.component.ComponentNearestFixedVersions;
import com.sonatype.insight.brain.api.experimental.ApiComponentNearestFixedVersionsRequestListDto;
import com.sonatype.insight.brain.api.experimental.ApiComponentNearestFixedVersionsRequestListDto.ApiComponentNearestFixedVersionsRequestDto;
import com.sonatype.insight.brain.api.experimental.ApiComponentNearestFixedVersionsService;
import com.sonatype.insight.brain.componentsearch.dto.ApplicationComponentMatchDTO;
import com.sonatype.insight.brain.service.BaseUrl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enriches component vulnerability match results with remediation guidance from HDS.
 * Queries HDS to find the nearest fixed version for each vulnerable component and adds
 * actionable recommendations like "Upgrade to 4.17.21" instead of just "Upgrade recommended".
 * Also sets the base URL for generating report links.
 */
@Named
@Singleton
public class ComponentMatchEnrichmentService
{
  private static final Logger log = LoggerFactory.getLogger(ComponentMatchEnrichmentService.class);

  private final ApiComponentNearestFixedVersionsService nearestFixedVersionsService;

  private final BaseUrl baseUrl;

  @Inject
  public ComponentMatchEnrichmentService(
      final ApiComponentNearestFixedVersionsService nearestFixedVersionsService,
      final BaseUrl baseUrl)
  {
    this.nearestFixedVersionsService = nearestFixedVersionsService;
    this.baseUrl = baseUrl;
  }

  public List<ApplicationComponentMatchDTO> enrichWithRemediationBulk(
      final List<ApplicationComponentMatchDTO> matches,
      final Set<String> cveIds)
  {
    if (matches.isEmpty()) {
      return matches;
    }

    Set<String> uniquePackageUrls = matches.stream()
        .map(ApplicationComponentMatchDTO::getPackageUrl)
        .filter(purl -> purl != null && !purl.isEmpty())
        .collect(Collectors.toSet());

    if (uniquePackageUrls.isEmpty()) {
      log.warn("No valid package URLs found in matches");
      return matches;
    }

    ApiComponentNearestFixedVersionsRequestListDto request = new ApiComponentNearestFixedVersionsRequestListDto();
    for (String packageUrl : uniquePackageUrls) {
      ApiComponentNearestFixedVersionsRequestDto dto = new ApiComponentNearestFixedVersionsRequestDto();
      dto.setPackageUrl(packageUrl);
      request.getComponents().add(dto);
    }

    List<ComponentNearestFixedVersions> fixedVersionsList;
    try {
      fixedVersionsList = nearestFixedVersionsService.getNearestFixedVersions(request);
    }
    catch (Exception e) {
      log.error("Failed to fetch nearest fixed versions from HDS: {}", e.getMessage(), e);
      return matches;
    }

    Table<String, String, String> packageUrlToCveToFixedVersion = buildFixedVersionTable(fixedVersionsList, cveIds);

    return matches.stream()
        .map(match -> enrichMatchWithCveVersion(match, packageUrlToCveToFixedVersion))
        .collect(Collectors.toList());
  }

  private Table<String, String, String> buildFixedVersionTable(
      final List<ComponentNearestFixedVersions> fixedVersionsList,
      final Set<String> cveIds)
  {
    return fixedVersionsList.stream()
        .filter(componentFixedVersions -> componentFixedVersions.getPackageUrl() != null)
        .flatMap(componentFixedVersions -> componentFixedVersions.getSecurityIssues().stream()
            .filter(range -> cveIds.contains(range.getIdentifier()))
            .filter(range -> range.getNearestFixedUpgrade() != null && !range.getNearestFixedUpgrade().isEmpty())
            .map(range -> new String[] {
                componentFixedVersions.getPackageUrl(),
                range.getIdentifier(),
                range.getNearestFixedUpgrade()
            }))
        .collect(Tables.toTable(
            row -> row[0],
            row -> row[1],
            row -> row[2],
            HashBasedTable::create
        ));
  }

  public ApplicationComponentMatchDTO enrichMatchWithCache(
      final ApplicationComponentMatchDTO match,
      final Table<String, String, String> remediationCache,
      final Set<String> cveIds)
  {
    String packageUrl = match.getPackageUrl();
    String cveId = match.getCveId();

    String recommendedVersion = null;
    if (packageUrl != null && cveId != null) {
      recommendedVersion = remediationCache.get(packageUrl, cveId);

      if (recommendedVersion == null && !remediationCache.contains(packageUrl, cveId)) {
        fetchRemedianFromHDS(packageUrl, cveIds, remediationCache);
        recommendedVersion = remediationCache.get(packageUrl, cveId);
      }
    }

    String recommendedAction = (recommendedVersion != null && !recommendedVersion.isEmpty())
        ? "Upgrade to " + recommendedVersion
        : "";

    ApplicationComponentMatchDTO enriched = new ApplicationComponentMatchDTO(
        match.getApplicationPublicId(),
        match.getApplicationName(),
        match.getApplicationInternalId(),
        match.getStage(),
        match.getEvaluationDate(),
        match.getPackageUrl(),
        match.getComponentDisplayName(),
        match.getHash(),
        match.getCveId(),
        recommendedAction,
        match.getActiveWaiver(),
        match.getViolating(),
        match.getReportId()
    );
    enriched.setBaseUrl(baseUrl.get());
    return enriched;
  }

  private void fetchRemedianFromHDS(
      final String packageUrl,
      final Set<String> cveIds,
      final Table<String, String, String> remediationCache)
  {
    try {
      ApiComponentNearestFixedVersionsRequestListDto request = new ApiComponentNearestFixedVersionsRequestListDto();
      ApiComponentNearestFixedVersionsRequestDto dto = new ApiComponentNearestFixedVersionsRequestDto();
      dto.setPackageUrl(packageUrl);
      request.getComponents().add(dto);

      List<ComponentNearestFixedVersions> fixedVersionsList =
          nearestFixedVersionsService.getNearestFixedVersions(request);

      Table<String, String, String> result = buildFixedVersionTable(fixedVersionsList, cveIds);

      for (String cveId : cveIds) {
        String version = result.get(packageUrl, cveId);
        remediationCache.put(packageUrl, cveId, version != null ? version : "");
      }
    }
    catch (Exception e) {
      log.error("Failed to fetch remediation for {}: {}", packageUrl, e.getMessage());
    }
  }

  private ApplicationComponentMatchDTO enrichMatchWithCveVersion(
      final ApplicationComponentMatchDTO match,
      final Table<String, String, String> packageUrlToCveToFixedVersion)
  {
    String packageUrl = match.getPackageUrl();
    String cveId = match.getCveId();

    String recommendedVersion = null;
    if (packageUrl != null && cveId != null) {
      recommendedVersion = packageUrlToCveToFixedVersion.get(packageUrl, cveId);
    }

    String recommendedAction;
    if (recommendedVersion != null) {
      recommendedAction = "Upgrade to " + recommendedVersion;
    }
    else {
      recommendedAction = "";
    }

    ApplicationComponentMatchDTO enriched = new ApplicationComponentMatchDTO(
        match.getApplicationPublicId(),
        match.getApplicationName(),
        match.getApplicationInternalId(),
        match.getStage(),
        match.getEvaluationDate(),
        match.getPackageUrl(),
        match.getComponentDisplayName(),
        match.getHash(),
        match.getCveId(),
        recommendedAction,
        match.getActiveWaiver(),
        match.getViolating(),
        match.getReportId()
    );
    enriched.setBaseUrl(baseUrl.get());
    return enriched;
  }
}
