/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.service.ApiComponentRemediationService;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dto.ApplicationComponentMatchDTO;
import com.sonatype.insight.brain.hds.AffectedComponentDTO;
import com.sonatype.insight.brain.hds.ComponentRemediationService;
import com.sonatype.insight.brain.hds.CveAffectedComponentsService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.utils.CsvWritable;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private final ApplicationComponentDAO applicationComponentDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final ApiComponentRemediationService apiComponentRemediationService;

  private final ComponentRemediationService componentRemediationService;

  private final BaseUrl baseUrl;

  @Inject
  public CveAffectedComponentSearchService(
      final CveAffectedComponentsService cveAffectedComponentsService,
      final ApplicationService applicationService,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final ApplicationComponentDAO applicationComponentDAO,
      final PolicyViolationDAO policyViolationDAO,
      final ApiComponentRemediationService apiComponentRemediationService,
      final ComponentRemediationService componentRemediationService,
      final BaseUrl baseUrl)
  {
    this.cveAffectedComponentsService = cveAffectedComponentsService;
    this.applicationService = applicationService;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.applicationComponentDAO = applicationComponentDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.apiComponentRemediationService = apiComponentRemediationService;
    this.componentRemediationService = componentRemediationService;
    this.baseUrl = baseUrl;
  }

  /**
   * Finds all applications containing components affected by the given CVE. Uses database queries instead of
   * loading report files for better performance.
   *
   * @param cveId the CVE identifier
   * @return Stream of matches with remediation data
   */
  public Stream<ApplicationComponentMatchDTO> find(final String cveId) {
    // Step 1: Get affected component coordinates from HDS
    List<AffectedComponentDTO> affectedComponentsList = cveAffectedComponentsService.getAffectedComponents(cveId);

    log.debug("CVE {}: Found {} affected components from HDS", cveId, affectedComponentsList.size());
    if (log.isTraceEnabled()) {
      affectedComponentsList.forEach(dto ->
          log.trace("  Affected: format={}, namespace={}, name={}, version={}",
              dto.format(), dto.namespace(), dto.name(), dto.version()));
    }

    if (affectedComponentsList.isEmpty()) {
      return Stream.empty();
    }

    // Convert to Set for O(1) lookup instead of O(n) iteration
    Set<AffectedComponentDTO> affectedComponents = Set.copyOf(affectedComponentsList);

    // Step 2: Get all applications
    List<Application> applications = applicationService.getApplications();
    if (applications.isEmpty()) {
      return Stream.empty();
    }

    // Step 3: Batch get latest PolicyEvaluations for ALL applications
    Set<String> applicationIds = applications.stream()
        .map(Application::getId)
        .collect(Collectors.toSet());

    Map<String, PolicyEvaluation> latestEvaluationByAppId =
        policyEvaluationDAO.getLastByApplicationIds(applicationIds)
            .stream()
            .collect(Collectors.groupingBy(
                PolicyEvaluation::getApplicationId,
                Collectors.collectingAndThen(
                    Collectors.maxBy(comparing(PolicyEvaluation::getTime)),
                    opt -> opt.orElse(null)
                )
            ));

    // Step 4: Build app->stage mapping
    Map<String, String> appIdToStageType = new HashMap<>();
    latestEvaluationByAppId.forEach((appId, evaluation) -> {
      if (evaluation != null) {
        appIdToStageType.put(appId, evaluation.getStageTypeId());
      }
    });

    if (appIdToStageType.isEmpty()) {
      return Stream.empty();
    }

    Map<String, String> remediationCache = new ConcurrentHashMap<>();

    // Step 5: Process each application, querying components and violations per-app to avoid row limits
    return applications.stream()
        .flatMap(application -> {
          PolicyEvaluation evaluation = latestEvaluationByAppId.get(application.getId());
          if (evaluation == null) {
            return Stream.empty();
          }

          // Query per-application to avoid hitting global database row limits
          List<ApplicationComponent> appComponents = applicationComponentDAO.getByApplicationIdAndStageTypeId(
                  application.getId(),
                  evaluation.getStageTypeId()
          );
          List<PolicyViolation> appViolations =
              policyViolationDAO.getActiveByApplicationIdAndStageId(application.getId(), evaluation.getStageTypeId());

          // First, check if any components in this app match affected components
          List<ApplicationComponent> matchingComponents = appComponents.stream()
              .filter(component -> {
                ComponentIdentifier componentIdentifier = component.getComponentIdentifier();
                if (componentIdentifier == null) {
                  return false;
                }
                AffectedComponentDTO affectedComponent =
                    AffectedComponentDTO.fromComponentIdentifier(componentIdentifier);
                return affectedComponents.contains(affectedComponent);
              })
              .toList();

          if (matchingComponents.isEmpty()) {
            return Stream.empty();
          }

          // Only load constraint facts if we have matching components
          policyViolationDAO.loadConstraintFacts(appViolations);

          // Now process the matching components
          return matchingComponents.stream()
              .map(component -> buildMatchFromDatabase(
                  application,
                  evaluation,
                  component,
                  affectedComponents,
                  appViolations,
                  cveId
              ))
              .filter(Objects::nonNull);
        })
        .map(match -> enrichMatchWithRemediation(match, remediationCache));
  }

  private ApplicationComponentMatchDTO buildMatchFromDatabase(
      final Application application,
      final PolicyEvaluation evaluation,
      final ApplicationComponent component,
      final Set<AffectedComponentDTO> affectedComponents,
      final List<PolicyViolation> appViolations,
      final String cveId)
  {
    // Step 1: Check if this component matches any affected component from HDS using O(1) set lookup
    ComponentIdentifier componentIdentifier = component.getComponentIdentifier();
    if (componentIdentifier == null) {
      log.trace("Skipping component without ComponentIdentifier: hash={}", component.getHash());
      return null;
    }

    AffectedComponentDTO affectedComponent = AffectedComponentDTO.fromComponentIdentifier(componentIdentifier);

    log.trace("Checking component: format={}, namespace={}, name={}, version={}",
        affectedComponent.format(), affectedComponent.namespace(),
        affectedComponent.name(), affectedComponent.version());

    if (!affectedComponents.contains(affectedComponent)) {
      log.trace("  No match in affected components set");
      return null;
    }

    log.debug("MATCH FOUND: app={}, component={}", application.getName(), affectedComponent);

    // Step 2: Find PolicyViolations for this component
    String componentHash = component.getHash();
    List<PolicyViolation> componentViolations = appViolations.stream()
        .filter(v -> componentHash.equals(v.getHash()))
        .toList();

    // Step 3: Extract vulnerability IDs and check for implicated/waived status
    Set<String> vulnerabilityIds = new HashSet<>();
    boolean implicated = false;
    boolean hasWaiver = false;

    for (PolicyViolation violation : componentViolations) {
      if (violation.isWaived()) {
        hasWaiver = true;
      }

      List<ConstraintFact> constraintFacts = violation.getConstraintFacts();
      if (constraintFacts != null) {
        for (ConstraintFact constraintFact : constraintFacts) {
          if (constraintFact.getConditionFacts() != null) {
            for (var conditionFact : constraintFact.getConditionFacts()) {
              if (conditionFact.getReference() != null &&
                  TriggerReference.Type.SECURITY_VULNERABILITY_REFID == conditionFact.getReference().getType()) {
                String vulnId = conditionFact.getReference().getValue();
                if (vulnId != null) {
                  vulnerabilityIds.add(vulnId);
                  if (cveId.equals(vulnId)) {
                    implicated = true;
                  }
                }
              }
            }
          }
        }
      }
    }

    String vulnerabilityIdsString = vulnerabilityIds.stream()
        .sorted()
        .collect(joining(", "));

    String evaluationDate = evaluation.getTime() != null
        ? CsvWritable.dateFormatter.format(evaluation.getTime().toInstant())
        : "";

    PackageUrlIdentifier purlIdentifier = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);
    String packageUrl = purlIdentifier.getPackageUrl();
    String fullDisplayName = ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString();
    String displayName = stripVersionFromDisplayName(fullDisplayName);

    ApplicationComponentMatchDTO match = new ApplicationComponentMatchDTO(
        application.getPublicId(),
        application.getName(),
        application.getId(),
        evaluation.getStageTypeId(),
        evaluationDate,
        packageUrl != null ? packageUrl : "",
        displayName,
        componentHash != null ? componentHash : "",
        affectedComponent.name(),
        affectedComponent.version(),
        vulnerabilityIdsString,
        "",
        "",
        hasWaiver ? "True" : "False",
        implicated ? "True" : "False",
        evaluation.getScanId()
    );

    match.setBaseUrl(baseUrl.get());
    return match;
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

    ApplicationComponentMatchDTO enriched = new ApplicationComponentMatchDTO(
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
    enriched.setBaseUrl(baseUrl.get());
    return enriched;
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
}
