/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentPolicyViolationsDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportPolicyDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.service.ApiComponentRemediationService;
import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dto.ApplicationComponentMatchDTO;
import com.sonatype.insight.brain.hds.AffectedComponentDTO;
import com.sonatype.insight.brain.hds.ComponentRemediationService;
import com.sonatype.insight.brain.hds.CveAffectedComponentsService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.utils.CsvWritable;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.singleton;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;

@Named
@Singleton
public class CveAffectedComponentSearchService
{
  private static final Logger log = LoggerFactory.getLogger(CveAffectedComponentSearchService.class);

  private final CveAffectedComponentsService cveAffectedComponentsService;

  private final ApplicationService applicationService;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final ApiReportDataServiceV2 apiReportDataService;

  private final ApiComponentRemediationService apiComponentRemediationService;

  private final ComponentRemediationService componentRemediationService;

  @Inject
  public CveAffectedComponentSearchService(
      final CveAffectedComponentsService cveAffectedComponentsService,
      final ApplicationService applicationService,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final ApiReportDataServiceV2 apiReportDataService,
      final ApiComponentRemediationService apiComponentRemediationService,
      final ComponentRemediationService componentRemediationService)
  {
    this.cveAffectedComponentsService = cveAffectedComponentsService;
    this.applicationService = applicationService;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.apiReportDataService = apiReportDataService;
    this.apiComponentRemediationService = apiComponentRemediationService;
    this.componentRemediationService = componentRemediationService;
  }

  /**
   * Finds all applications containing components affected by the given CVE. Uses a shared cache to fetch remediation
   * data on-demand as the stream is consumed.
   *
   * @param cveId the CVE identifier
   * @param baseUrl the base URL for generating links (optional, can be null)
   * @return Stream of matches with remediation data
   */
  public Stream<ApplicationComponentMatchDTO> find(final String cveId, final String baseUrl) {
    List<AffectedComponentDTO> affectedComponents = cveAffectedComponentsService.getAffectedComponents(cveId);

    if (affectedComponents.isEmpty()) {
      return Stream.empty();
    }

    Map<String, String> remediationCache = new ConcurrentHashMap<>();

    return applicationService
        .getApplications()
        .stream()
        .flatMap(application -> findMatchesInApplication(application, affectedComponents, cveId))
        .map(match -> enrichMatchWithRemediation(match, remediationCache))
        .map(match -> setBaseUrl(match, baseUrl));
  }

  private Stream<ApplicationComponentMatchDTO> findMatchesInApplication(
      final Application application,
      final List<AffectedComponentDTO> affectedComponents,
      final String cveId)
  {
    PolicyEvaluation latestEvaluation = policyEvaluationDAO.getLastByApplicationIds(singleton(application.getId()))
        .stream()
        .max(comparing(PolicyEvaluation::getTime))
        .orElse(null);

    if (latestEvaluation == null) {
      return Stream.empty();
    }

    ApiReportRawDataDTOV2 reportRawData = getLastRawApplicationReport(application.getPublicId(), latestEvaluation);

    if (reportRawData == null || reportRawData.components == null || reportRawData.components.isEmpty()) {
      return Stream.empty();
    }

    ApiReportPolicyDataDTOV2 policyData = getPolicyViolationsData(application, latestEvaluation);

    return reportRawData
        .components
        .stream()
        .map(reportComponent ->
            buildMatch(application, latestEvaluation, reportComponent, affectedComponents, policyData, cveId)
        )
        .filter(Objects::nonNull);
  }

  private ApiReportPolicyDataDTOV2 getPolicyViolationsData(
      final Application application,
      final PolicyEvaluation latestEvaluation)
  {
    try {
      return apiReportDataService.getPolicyViolationsDataNoAuth(
          application.getPublicId(),
          latestEvaluation.getScanId(),
          false);
    }
    catch (IOException e) {
      log.error("Failed to fetch policy violation data for application {} and scan id {}: {}",
          application.getPublicId(),
          latestEvaluation.getScanId(), e.getMessage());
    }
    return null;
  }

  private ApplicationComponentMatchDTO buildMatch(
      final Application application,
      final PolicyEvaluation evaluation,
      final ApiReportComponentDTOV2 reportComponent,
      final List<AffectedComponentDTO> affectedComponents,
      final ApiReportPolicyDataDTOV2 policyData,
      final String cveId)
  {
    AffectedComponentDTO affectedComponent = findMatchingAffectedComponent(reportComponent, affectedComponents);
    if (affectedComponent == null) {
      return null;
    }

    // Build vulnerability IDs
    String vulnerabilityIds = "";
    if (reportComponent.securityData != null && reportComponent.securityData.securityIssues != null) {
      vulnerabilityIds = reportComponent.securityData.securityIssues.stream()
          .map(issue -> issue.reference)
          .filter(ref -> ref != null && !ref.isEmpty())
          .collect(joining(", "));
    }

    String activeWaiver = hasActiveWaiver(reportComponent, policyData) ? "True" : "False";
    String implicatedFiles = hasImplicatedFiles(reportComponent, cveId) ? "True" : "False";

    String evaluationDate = evaluation.getTime() != null
        ? CsvWritable.dateFormatter.format(evaluation.getTime().toInstant())
        : "";

    return new ApplicationComponentMatchDTO(
        application.getPublicId(),
        application.getName(),
        application.getId(),
        evaluation.getStageTypeId(),
        evaluationDate,
        reportComponent.packageUrl != null ? reportComponent.packageUrl : "",
        stripVersionFromDisplayName(reportComponent.displayName),
        reportComponent.hash != null ? reportComponent.hash : "",
        affectedComponent.getName(),
        affectedComponent.getVersion(),
        vulnerabilityIds,
        "",
        "",
        activeWaiver,
        implicatedFiles,
        evaluation.getScanId()
    );
  }

  private String stripVersionFromDisplayName(final String displayName) {
    if (displayName == null || displayName.isEmpty()) {
      return "";
    }

    int lastColonIndex = displayName.lastIndexOf(':');
    if (lastColonIndex > 0) {
      return displayName.substring(0, lastColonIndex);
    }

    return displayName;
  }

  private boolean hasActiveWaiver(
      final ApiReportComponentDTOV2 reportComponent,
      final ApiReportPolicyDataDTOV2 policyData)
  {
    if (policyData == null || policyData.components == null || reportComponent.hash == null) {
      return false;
    }

    for (ApiReportComponentPolicyViolationsDTOV2 compViolation : policyData.components) {
      if (reportComponent.hash.equals(compViolation.hash)) {
        boolean hasWaiver = compViolation.violations != null &&
            compViolation.violations.stream().anyMatch(v -> v.waived);

        if (hasWaiver) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean hasImplicatedFiles(
      final ApiReportComponentDTOV2 reportComponent,
      final String cveId)
  {
    if (reportComponent.securityData == null || reportComponent.securityData.securityIssues == null) {
      return false;
    }

    return reportComponent.securityData.securityIssues.stream()
        .anyMatch(issue -> cveId.equals(issue.reference));
  }

  private ApplicationComponentMatchDTO enrichMatchWithRemediation(
      final ApplicationComponentMatchDTO match,
      final Map<String, String> remediationCache)
  {
    String packageUrl = match.getPackageUrl();

    String recommendedVersion = remediationCache.computeIfAbsent(packageUrl, key -> {
      try {
        ApiComponentDTOV2 componentDTO = new ApiComponentDTOV2();
        componentDTO.packageUrl = key;

        ApiComponentRemediationDTO remediation =
            apiComponentRemediationService.getSuggestedRemediationForComponentNoAuthz(
                componentDTO,
                OwnerType.APPLICATION,
                match.getApplicationInternalId(),
                null,
                null,
                match.getReportId(),
                false,
                true
            );

        if (remediation != null && remediation.remediation != null) {
          Optional<ApiVersionChangeOptionDTO> versionChange =
              componentRemediationService.getApplicableVersionChangeFromAllType(
                  remediation.remediation.suggestedVersionChange,
                  remediation.remediation.versionChanges
              );

          if (versionChange.isPresent()) {
            String version = extractVersionFromVersionChange(versionChange.get());
            if (!version.isEmpty()) {
              return version;
            }
          }
        }
      }
      catch (Exception e) {
        log.error("Failed to fetch remediation for {}: {}", key, e.getMessage());
      }
      return "";
    });

    String recommendedAction = "";

    if (!recommendedVersion.isEmpty()) {
      recommendedAction = "Upgrade to " + recommendedVersion;
    }
    else if (match.getVulnerabilityIds() != null && !match.getVulnerabilityIds().isEmpty()) {
      recommendedAction = "Upgrade recommended";
    }

    return new ApplicationComponentMatchDTO(
        match.getApplicationPublicId(),
        match.getApplicationName(),
        match.getApplicationInternalId(),
        match.getStage(),
        match.getEvaluationDate(),
        match.getPackageUrl(),
        match.getComponentDisplayName(),
        match.getHash(),
        match.getMatchedName(),
        match.getMatchedVersion(),
        match.getVulnerabilityIds(),
        recommendedAction,
        recommendedVersion,
        match.getActiveWaiver(),
        match.getImplicatedFiles(),
        match.getReportId()
    );
  }

  private String extractVersionFromVersionChange(final ApiVersionChangeOptionDTO versionChange) {
    try {
      if (versionChange.getData() != null &&
          versionChange.getData().getComponent() != null &&
          versionChange.getData().getComponent().componentIdentifier != null) {
        PackageUrlIdentifier purlIdentifier =
            PackageUrlIdentifier.fromComponentIdentifier(
                versionChange.getData().getComponent().componentIdentifier.toComponentIdentifier()
            );
        String version = purlIdentifier.getVersion();
        return version != null ? version : "";
      }
    }
    catch (Exception e) {
      log.debug("Failed to extract version from version change: {}", e.getMessage());
    }
    return "";
  }

  private AffectedComponentDTO findMatchingAffectedComponent(
      final ApiReportComponentDTOV2 reportComponent,
      final List<AffectedComponentDTO> affectedComponents)
  {
    if (reportComponent != null && reportComponent.componentIdentifier != null) {
      ComponentIdentifier componentIdentifier = reportComponent.componentIdentifier.toComponentIdentifier();
      for (AffectedComponentDTO affectedComponent : affectedComponents) {
        if (affectedComponent.equalByComponentIdentifier(componentIdentifier)) {
          return affectedComponent;
        }
      }
    }

    return null;
  }

  private ApiReportRawDataDTOV2 getLastRawApplicationReport(
      final String applicationPublicId,
      final PolicyEvaluation lastPolicyEvaluation)
  {
    try {
      return apiReportDataService.getDataNoAuth(applicationPublicId, lastPolicyEvaluation.getScanId());
    }
    catch (Exception e) {
      // this mostly happens if we don't have a report file while we do have an application
      log.warn("Failed to fetch last application report for {}: {}", applicationPublicId, e.getMessage());

      return null;
    }
  }

  private ApplicationComponentMatchDTO setBaseUrl(
      final ApplicationComponentMatchDTO match,
      final String baseUrl)
  {
    if (baseUrl != null && !baseUrl.isEmpty()) {
      match.setBaseUrl(baseUrl);
    }
    return match;
  }
}
