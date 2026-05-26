/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.development.prioritization;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiDependencyDataDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.PrioritizedComponent;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.api.v2.service.ApiComponentRemediationService;
import com.sonatype.insight.brain.callflow.ComponentReachabilityService;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.development.prioritization.DevelopmentPrioritizationComponentInfoDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.development.prioritization.dto.PrioritizationRemediationVersionDTO;
import com.sonatype.insight.brain.features.FeaturesService;
import com.sonatype.insight.brain.innersource.InnerSourceService;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.ReachabilityStatus;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.prioritization.DevelopmentPrioritizationComponentInfo;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.PolicyEvaluationDiffService;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats.Component;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats.PolicyViolation;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.insight.brain.report.CompositeComparableVersion;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotAuthorizedException;
import com.sonatype.insight.license.model.Feature;
import com.sonatype.insight.license.model.LicensedFeature;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.api.v2.dto.PrioritizedComponent.DEPENDENCY_TYPE_DIRECT;
import static com.sonatype.insight.brain.api.v2.dto.PrioritizedComponent.DEPENDENCY_TYPE_INNER_SOURCE_DIRECT;
import static com.sonatype.insight.brain.api.v2.dto.PrioritizedComponent.DEPENDENCY_TYPE_INNER_SOURCE_TRANSITIVE;
import static com.sonatype.insight.brain.api.v2.dto.PrioritizedComponent.DEPENDENCY_TYPE_TRANSITIVE;
import static com.sonatype.insight.brain.api.v2.dto.PrioritizedComponent.DEPENDENCY_TYPE_UNKNOWN;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.SECURITY;
import static com.sonatype.insight.brain.report.InnerSourceUtils.createCompositeComparableVersion;

@Named
public class DevelopmentPrioritiesService
{
  private static final Logger log = LoggerFactory.getLogger(DevelopmentPrioritiesService.class);

  private static final int MINIMUM_THREAT_LEVEL = 1;

  private final FeaturesService featuresService;

  private final DevelopmentPrioritiesReportService developmentPrioritiesReportService;

  private final DevelopmentPrioritizationComponentInfoDAO prioritizationComponentInfoDAO;

  private final ReportService reportService;

  private final ComponentReachabilityService componentReachabilityService;

  private final ApiComponentRemediationService apiComponentRemediationService;

  private final DevelopmentPrioritiesUtilsService developmentPrioritiesUtilsService;

  private final PolicyEvaluationDiffService policyEvaluationDiffService;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final ApplicationDAO applicationDAO;

  private final PolicyWaiverDAO policyWaiverDAO;

  private final InnerSourceService innerSourceService;

  private final AutoPolicyWaiverDAO autoPolicyWaiverDAO;

  @Inject
  public DevelopmentPrioritiesService(
      final FeaturesService featuresService,
      final DevelopmentPrioritiesReportService developmentPrioritiesReportService,
      final DevelopmentPrioritizationComponentInfoDAO prioritizationComponentInfoDAO,
      final ReportService reportService,
      final ComponentReachabilityService componentReachabilityService,
      final ApiComponentRemediationService apiComponentRemediationService,
      final DevelopmentPrioritiesUtilsService developmentPrioritiesUtilsService,
      final PolicyEvaluationDiffService policyEvaluationDiffService,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final ApplicationDAO applicationDAO,
      final PolicyWaiverDAO policyWaiverDAO,
      final InnerSourceService innerSourceService,
      final AutoPolicyWaiverDAO autoPolicyWaiverDAO)
  {
    this.featuresService = featuresService;
    this.developmentPrioritiesReportService = developmentPrioritiesReportService;
    this.prioritizationComponentInfoDAO = prioritizationComponentInfoDAO;
    this.reportService = reportService;
    this.componentReachabilityService = componentReachabilityService;
    this.apiComponentRemediationService = apiComponentRemediationService;
    this.developmentPrioritiesUtilsService = developmentPrioritiesUtilsService;
    this.policyEvaluationDiffService = policyEvaluationDiffService;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.applicationDAO = applicationDAO;
    this.policyWaiverDAO = policyWaiverDAO;
    this.innerSourceService = innerSourceService;
    this.autoPolicyWaiverDAO = autoPolicyWaiverDAO;
  }

  @Authorize(permission = Permission.READ)
  public DevelopmentPrioritizationResults getPrioritizedFindings(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) final String applicationPublicId,
      final String scanId,
      final int page,
      final int pageSize,
      final String componentNameFilter,
      final boolean includeRemediation,
      final boolean filterOnPolicyActions)
  {
    final int skipCount = (page - 1) * pageSize;

    PolicyEvaluation latestBuildStageEvaluation =
        policyEvaluationDAO.getLastByApplicationIdAndStageId(
            applicationDAO.getByPublicId(applicationPublicId).getId(), "build");

    String scanIdFromLatestBuildStageEvaluation =
        latestBuildStageEvaluation != null ? latestBuildStageEvaluation.getScanId() : "";

    boolean hasAutoWaiversConfigured = autoPolicyWaiverDAO.getCount() > 0 ? true : false;

    final List<PrioritizedComponent> allPrioritizedFindings =
        includeRemediation
            ? getAllPrioritizedFindings(applicationPublicId, scanId, skipCount, pageSize)
            : getAllPrioritizedFindings(applicationPublicId, scanId, null, null);

    final List<PrioritizedComponent> filteredByNameAndAction = allPrioritizedFindings.stream()
        .filter(prioritizedComponent -> StringUtils.isEmpty(componentNameFilter) ||
            matchesFilter(prioritizedComponent.getDisplayName(), componentNameFilter))
        .filter(
            prioritizedComponent -> !filterOnPolicyActions || Action.ID_FAIL.equals(prioritizedComponent.getAction()) ||
                Action.ID_WARN.equals(prioritizedComponent.getAction()))
        .toList();

    // get total size before for pagination
    final long totalSize = filteredByNameAndAction.size();

    final List<PrioritizedComponent> prioritizedFindingsForPagination = filteredByNameAndAction
        .stream()
        .skip(skipCount)
        .limit(pageSize)
        .toList();

    return new DevelopmentPrioritizationResults(scanIdFromLatestBuildStageEvaluation, hasAutoWaiversConfigured,
        new ApiPageResult<>(totalSize, page, pageSize, prioritizedFindingsForPagination));
  }

  /**
   * This method is used to get all prioritized findings for the specified application Id and scan Id.
   * If the skipCount and limit are provided, it will set remediation for the components inside the skip and limit
   * remediation range (page size).
   **/
  @Authorize(permission = Permission.READ)
  public List<PrioritizedComponent> getAllPrioritizedFindings(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) final String applicationPublicId,
      final String scanId,
      final Integer remediationSkip,
      final Integer remediationLimit)
  {
    throwErrorIfDevelopmentNotEnabledByLicense();

    // checks for read permissions on the app, making this an authorized function
    final ApiReportRawDataDTOV2 apiReportRawDataDTOV2 =
        developmentPrioritiesReportService.getDependencyInformation(applicationPublicId, scanId);
    final PolicyThreats policyThreats = reportService.getPolicyThreats(applicationPublicId, scanId);

    String applicationId = applicationDAO.getByPublicId(applicationPublicId).getId();

    PolicyEvaluation policyEvaluation =
        policyEvaluationDAO.getLastByApplicationIdAndScanId(applicationId, scanId);

    String policyEvaluationStage = policyEvaluation != null ? policyEvaluation.getStageTypeId() : null;

    PolicyEvaluation mainPolicyEvaluation =
        policyEvaluationDAO.getLastByApplicationIdAndStageId(applicationId, Stage.ID_BUILD);

    Set<String> componentsHashesOnBothEvaluations = Stage.ID_DEVELOP.equals(policyEvaluationStage)
        ? getSameComponentHashesInBothEvaluations(policyEvaluation, mainPolicyEvaluation)
        : Collections.emptySet();

    boolean isBulkRecommendationsEnabled = isBulkRecommendationsEnabled();

    // Batch fetch policy waivers and component info before stream processing
    Set<String> componentHashes = apiReportRawDataDTOV2.components.stream()
        .map(component -> component.hash)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    Map<String, List<PolicyWaiver>> policyWaiversByHash;
    try (TransactionContext tx = policyWaiverDAO.createTransactionContext()) {
      policyWaiversByHash = policyWaiverDAO.getByOwnerIdAndHashes(tx, applicationId, componentHashes);
    }

    final Map<String, DevelopmentPrioritizationComponentInfo> componentInfoByHash;
    if (isBulkRecommendationsEnabled) {
      Set<String> syntheticHashes = apiReportRawDataDTOV2.components.stream()
          .filter(c -> c.componentIdentifier != null)
          .map(c -> c.componentIdentifier.toComponentIdentifier().toSyntheticHash())
          .filter(Objects::nonNull)
          .collect(Collectors.toSet());
      componentInfoByHash = prioritizationComponentInfoDAO.getByScanIdAndComponentHashes(scanId, syntheticHashes);
    }
    else {
      componentInfoByHash = Collections.emptyMap();
    }

    final List<UnprioritizedComponent> sortedComponents = apiReportRawDataDTOV2.components
        .stream()
        .map(component -> {
          final List<PolicyViolation> policyViolations =
              getMatchingViolations(policyThreats.aaData, component);

          // Component identifier can be null for unknown components
          final ComponentIdentifier componentIdentifier =
              component.componentIdentifier != null ? component.componentIdentifier.toComponentIdentifier() : null;
          final PolicyViolation highestPolicyViolation = getHighestThreat(policyViolations, false);
          final int highestThreatLevel;
          final String policyName;
          final String highestThreatConstraintName;
          int highestReachableThreatLevel = 0;

          if (highestPolicyViolation == null) {
            highestThreatLevel = 0;
            policyName = null;
            highestThreatConstraintName = null;
          }
          else {
            highestThreatLevel = highestPolicyViolation.policyThreatLevel;
            policyName = highestPolicyViolation.policyName;

            if (!highestPolicyViolation.constraints.isEmpty()) {
              highestThreatConstraintName = highestPolicyViolation.constraints.get(0).constraintName;
            }
            else {
              highestThreatConstraintName = null;
            }
          }

          final ReachabilityStatus securityReachable;
          if (hasSecurityViolations(policyViolations)) {
            securityReachable = isSecurityReachable(policyThreats, component.hash);
          }
          else {
            securityReachable = ReachabilityStatus.combine(Stream.of());
          }

          ApiVersionChangeOptionType remediationType = null;
          String remediationVersion = null;
          if (securityReachable == ReachabilityStatus.REACHABLE) {
            final PolicyViolation highestReachablePolicyViolation = getHighestThreat(policyViolations, true);
            if (highestReachablePolicyViolation != null) {
              highestReachableThreatLevel = highestReachablePolicyViolation.policyThreatLevel;
            }
          }

          if (isBulkRecommendationsEnabled && componentIdentifier != null) {
            DevelopmentPrioritizationComponentInfo prioritizationComponentInfo =
                componentInfoByHash.get(componentIdentifier.toSyntheticHash());

            if (prioritizationComponentInfo != null) {
              remediationType = prioritizationComponentInfo.getRemediationType();
              remediationVersion = prioritizationComponentInfo.getRemediationVersion();
            }
          }

          boolean hasExpiredWaiver = false;
          boolean hasSoonToExpireWaiver = false;
          boolean isAllViolationsWaived = false;
          String waiverExpirationDetails = "";
          long soonestToExpireDays = Integer.MAX_VALUE;
          long oldestHasExpiredDays = Integer.MIN_VALUE;
          int waivedViolationsCount = 0;
          boolean hasAutoWaiver = false;
          boolean hasSameViolationsOnMain = componentsHashesOnBothEvaluations.contains(component.hash);

          Component policyThreatComponent = getPolicyThreatComponent(policyThreats.aaData, component);
          if (policyThreatComponent != null) {
            waivedViolationsCount = policyThreatComponent.waivedViolations.size();
            isAllViolationsWaived = !policyThreatComponent.allViolations.isEmpty()
                && policyThreatComponent.allViolations.size() == waivedViolationsCount;
            for (PolicyViolation policyViolation : policyThreatComponent.allViolations) {
              if (policyViolation.waivedWithAutoWaiver) {
                hasAutoWaiver = true;
                break;
              }
            }
          }

          List<PolicyWaiver> policyWaivers = policyWaiversByHash.getOrDefault(component.hash, List.of());
          for (PolicyWaiver policyWaiver : policyWaivers) {
            if (Objects.nonNull(policyWaiver.getExpiryTime())) {
              ZonedDateTime waiverExpiryTime = policyWaiver.getExpiryTime().toInstant().atZone(ZoneId.systemDefault());
              boolean isWaiverExpired = waiverExpiryTime.toLocalDateTime().isBefore(LocalDateTime.now());
              long daysFromNowToExpiry = ChronoUnit.DAYS.between(LocalDate.now(), waiverExpiryTime.toLocalDate());
              if (!isWaiverExpired && daysFromNowToExpiry < 10) {
                hasSoonToExpireWaiver = true;
                soonestToExpireDays = Math.min(soonestToExpireDays, daysFromNowToExpiry);
              }
              // daysFromNowToExpiry is negative if the waiver has expired
              else if (isWaiverExpired && daysFromNowToExpiry > -10) {
                hasExpiredWaiver = true;
                oldestHasExpiredDays = Math.max(oldestHasExpiredDays, Math.abs(daysFromNowToExpiry));
              }
            }
          }

          if (isAllViolationsWaived && hasSoonToExpireWaiver) {
            waiverExpirationDetails = soonestToExpireDays == 0
                ? "Applied waiver will expire today"
                : String.format("Applied waiver will expire in %d %s",
                    soonestToExpireDays, soonestToExpireDays == 1 ? "day" : "days");
          }

          else if (!isAllViolationsWaived && hasExpiredWaiver) {
            waiverExpirationDetails = oldestHasExpiredDays == 0
                ? "Applied waiver expired today"
                : String.format("Applied waiver expired %d %s ago",
                    oldestHasExpiredDays, oldestHasExpiredDays == 1 ? "day" : "days");
          }

          return new UnprioritizedComponent(
              component,
              getDependencyType(component),
              hasFailActionOnComponent(policyViolations),
              getAction(policyViolations),
              highestThreatLevel,
              policyName,
              highestThreatConstraintName,
              securityReachable.toBoolean(),
              remediationType,
              remediationVersion,
              highestReachableThreatLevel,
              hasSameViolationsOnMain,
              hasExpiredWaiver,
              hasSoonToExpireWaiver,
              isAllViolationsWaived,
              waiverExpirationDetails,
              waivedViolationsCount,
              hasAutoWaiver);
        })
        .filter(unprioritizedComponent -> unprioritizedComponent.isAllViolationsWaived
            || unprioritizedComponent.highestThreat > 0 || hasUpgradePathOfInnerSource(unprioritizedComponent))
        .sorted(Comparator.comparingInt((UnprioritizedComponent c) -> getScore(c, isBulkRecommendationsEnabled))
            .thenComparingInt(this::getHighestThreat)
            .reversed())
        .toList();

    List<UnprioritizedComponent> sortedComponentsWithRemediation = setRemediationForComponents(
        sortedComponents,
        applicationPublicId,
        scanId,
        policyEvaluationStage,
        remediationSkip,
        remediationLimit,
        isBulkRecommendationsEnabled);

    // If skipCount and limit are not provided, return all sorted components without remediation
    return addPrioritiesToSortedList(sortedComponentsWithRemediation);
  }

  /**
   * Retrieves a set of component hashes that are present in both policy evaluations.
   * <p>
   * This method compares two `PolicyEvaluation` objects and identifies components that have the same
   * violations in both evaluations. It uses the `PolicyEvaluationDiffService` to compute the differences
   * and extracts the hashes of components with matching violations.
   *
   * @param policyEvaluation The primary `PolicyEvaluation` object to compare.
   * @param mainPolicyEvaluation The secondary `PolicyEvaluation` object to compare against.
   * @return A `Set` of component hashes that are present in both evaluations. If the evaluations are
   *         identical or the secondary evaluation is null, an empty set is returned.
   */
  private Set<String> getSameComponentHashesInBothEvaluations(
      PolicyEvaluation policyEvaluation,
      PolicyEvaluation mainPolicyEvaluation)
  {
    if (mainPolicyEvaluation != null && !Objects.equals(policyEvaluation.getId(), mainPolicyEvaluation.getId())) {
      return policyEvaluationDiffService.createPolicyViolationDiff(
          policyEvaluation, mainPolicyEvaluation, MINIMUM_THREAT_LEVEL)
          .map(PolicyViolationDiff::getSame)
          .map(same -> same.keySet()
              .stream()
              .map(com.sonatype.insight.brain.model.policy.PolicyViolation::getHash)
              .collect(Collectors.toSet()))
          .orElse(Collections.emptySet());
    }
    else {
      return Collections.emptySet();
    }
  }

  /**
   * This method is used to set remediation for components based on the skipCount and limit provided.
   * If skipCount and limit are provided, it will set remediation for the components inside the skip and limit range
   * (page size).
   **/
  private List<UnprioritizedComponent> setRemediationForComponents(
      final List<UnprioritizedComponent> sortedComponents,
      final String applicationPublicId,
      final String scanId,
      final String stageId,
      final Integer remediationSkip,
      final Integer remediationLimit,
      final boolean isBulkRecommendationsEnabled)
  {
    if (remediationSkip != null && remediationLimit != null) {
      return IntStream.range(0, sortedComponents.size())
          .mapToObj(index -> {
            UnprioritizedComponent unprioritizedComponent = sortedComponents.get(index);

            if (index >= remediationSkip && index < remediationSkip + remediationLimit) {
              return loadRemediation(unprioritizedComponent, applicationPublicId, scanId, stageId,
                  isBulkRecommendationsEnabled);
            }

            return unprioritizedComponent;
          })
          .toList();
    }

    return sortedComponents;
  }

  private UnprioritizedComponent loadRemediation(
      UnprioritizedComponent unprioritizedComponent,
      String applicationPublicId,
      String scanId,
      String stageId,
      boolean isBulkRecommendationsEnabled)
  {

    ComponentIdentifier componentIdentifier = unprioritizedComponent.getComponentIdentifier();
    if (!isBulkRecommendationsEnabled && componentIdentifier != null) {
      try {
        ApiComponentRemediationDTO apiComponentRemediationDTO =
            apiComponentRemediationService.getSuggestedRemediationForComponentNoAuthz(
                unprioritizedComponent.component,
                OwnerType.APPLICATION,
                applicationPublicId,
                stageId,
                null,
                scanId,
                false,
                true);

        if (apiComponentRemediationDTO != null) {
          PrioritizationRemediationVersionDTO remediationVersionDTO =
              developmentPrioritiesUtilsService.getPrioritizationRemediation(
                  apiComponentRemediationDTO.remediation,
                  componentIdentifier.get("version"));

          if (remediationVersionDTO != null) {
            unprioritizedComponent.remediationType = remediationVersionDTO.getRemediationType();
            unprioritizedComponent.remediationVersion = remediationVersionDTO.getVersion();
          }
        }
      }
      catch (BadRequestException e) {
        log.warn("Failed to get remediation for component: {}", componentIdentifier, e);
      }
    }

    return unprioritizedComponent;
  }

  private String getDependencyType(ApiReportComponentDTOV2 reportBomComponent) {
    final ApiDependencyDataDTO dependencyData = reportBomComponent.dependencyData;

    if (dependencyData == null) {
      return DEPENDENCY_TYPE_UNKNOWN;
    }

    if (dependencyData.innerSource != null && dependencyData.innerSource) {
      if (dependencyData.directDependency != null && dependencyData.directDependency) {
        return DEPENDENCY_TYPE_INNER_SOURCE_DIRECT;
      }
      else {
        return DEPENDENCY_TYPE_INNER_SOURCE_TRANSITIVE;
      }
    }
    else if (dependencyData.directDependency != null && dependencyData.directDependency) {
      return DEPENDENCY_TYPE_DIRECT;
    }
    else {
      return DEPENDENCY_TYPE_TRANSITIVE;
    }
  }

  private String getAction(List<PolicyThreats.PolicyViolation> activeViolations) {
    if (hasFailActionOnComponent(activeViolations)) {
      return Action.ID_FAIL;
    }
    else if (hasWarnAction(activeViolations)) {
      return Action.ID_WARN;
    }
    else {
      return "none";
    }
  }

  private Boolean hasWarnAction(List<PolicyThreats.PolicyViolation> activeViolations) {
    if (activeViolations.isEmpty()) {
      return false;
    }

    final List<PolicyThreats.PolicyViolation> violationsWithWarns = activeViolations
        .stream()
        .filter(policyViolation -> {
          if (policyViolation.actions == null) {
            return false;
          }

          return policyViolation.actions.stream().anyMatch(action -> Action.ID_WARN.equals(action.actionType));
        })
        .toList();

    return !violationsWithWarns.isEmpty();
  }

  private boolean hasFailActionOnComponent(List<PolicyThreats.PolicyViolation> activeViolations) {
    if (activeViolations.isEmpty()) {
      return false;
    }

    final List<PolicyThreats.PolicyViolation> violationsWithFails = activeViolations
        .stream()
        .filter(policyViolation -> {
          if (policyViolation.actions == null) {
            return false;
          }

          return policyViolation.actions.stream().anyMatch(action -> Action.ID_FAIL.equals(action.actionType));
        })
        .toList();

    return !violationsWithFails.isEmpty();
  }

  final PolicyViolation getHighestThreat(
      List<PolicyThreats.PolicyViolation> activeViolations,
      boolean securityViolationsOnly)
  {
    if (activeViolations.isEmpty()) {
      return null;
    }

    Stream<PolicyViolation> violationsStream = activeViolations.stream();
    if (securityViolationsOnly) {
      violationsStream =
          violationsStream
              .filter(policyViolation -> SECURITY.getName().equalsIgnoreCase(policyViolation.policyThreatCategory));
    }
    final List<PolicyThreats.PolicyViolation> violations = violationsStream
        .sorted((violationA, violationB) -> {
          if (violationA.policyThreatLevel != violationB.policyThreatLevel) {
            return Integer.compare(violationB.policyThreatLevel, violationA.policyThreatLevel);
          }
          else {
            // apply a secondary sort on ID so we always find the same Policy Violation
            return String.CASE_INSENSITIVE_ORDER.compare(violationA.policyViolationId, violationB.policyViolationId);
          }
        })
        .toList();

    return violations.get(0);
  }

  final boolean hasSecurityViolations(List<PolicyThreats.PolicyViolation> activeViolations) {
    if (activeViolations.isEmpty()) {
      return false;
    }

    return activeViolations
        .stream()
        .anyMatch(policyViolation -> SECURITY.getName().equalsIgnoreCase(policyViolation.policyThreatCategory));
  }

  private List<PolicyThreats.PolicyViolation> getMatchingViolations(
      final List<Component> policyThreatsComponents,
      final ApiReportComponentDTOV2 component)
  {
    return policyThreatsComponents.stream()
        .filter(policyThreatComponent -> policyThreatComponent.hash.equals(component.hash))
        .map(comp -> comp.activeViolations)
        .flatMap(Collection::stream)
        .filter(violation -> !violation.legacyViolation)
        .collect(Collectors.toList());
  }

  private List<PrioritizedComponent> addPrioritiesToSortedList(
      final List<UnprioritizedComponent> sortedComponents)
  {
    final List<PrioritizedComponent> prioritizedComponents = new ArrayList<>();

    if (sortedComponents.isEmpty()) {
      return prioritizedComponents;
    }

    for (int i = 0; i < sortedComponents.size(); i++) {
      final UnprioritizedComponent componentToPrioritize = sortedComponents.get(i);
      // If we detect an expired waiver, isAllViolationsWaived should be set to false
      if (componentToPrioritize.isAllViolationsWaived && componentToPrioritize.hasExpiredWaiver) {
        componentToPrioritize.isAllViolationsWaived = false;
      }
      prioritizedComponents.add(componentToPrioritize.toPrioritizedComponent(i + 1));
    }

    return prioritizedComponents;
  }

  private int getScore(
      final UnprioritizedComponent unprioritizedComponent,
      final boolean isBulkRecommendationsEnabled)
  {
    return getActionNumber(unprioritizedComponent.action) * 100000 +
        getRecommendationNumber(unprioritizedComponent, isBulkRecommendationsEnabled) * 100 +
        unprioritizedComponent.highestReachableThreat;
  }

  private int getHighestThreat(final UnprioritizedComponent unprioritizedComponent) {
    return unprioritizedComponent.highestThreat;
  }

  private int getActionNumber(final String action) {
    if (Action.ID_FAIL.equals(action)) {
      return 2;
    }
    else if (Action.ID_WARN.equals(action)) {
      return 1;
    }
    else {
      return 0;
    }
  }

  private int getRecommendationNumber(
      final UnprioritizedComponent unprioritizedComponent,
      final boolean isBulkRecommendationsEnabled)
  {
    if (isBulkRecommendationsEnabled && Objects.nonNull(unprioritizedComponent) &&
        Objects.nonNull(unprioritizedComponent.remediationVersion))
    {
      final String originalComponentVersion = unprioritizedComponent.getComponentVersion();
      final String remediationVersion = unprioritizedComponent.remediationVersion;

      // Remove from consideration situations where IQ recommends the same version because it isn't failing policy
      if (originalComponentVersion != null && !originalComponentVersion.equals(remediationVersion)) {
        return 1;
      }
    }
    return 0;
  }

  private Component getPolicyThreatComponent(
      final List<Component> policyThreatsComponents,
      final ApiReportComponentDTOV2 component)
  {
    return policyThreatsComponents.stream()
        .filter(policyThreatComponent -> policyThreatComponent.hash.equals(component.hash))
        .findFirst()
        .orElse(null);
  }

  private void throwErrorIfDevelopmentNotEnabledByLicense() {
    if (!isDevelopmentFeatureEnabled()) {
      throw new NotAuthorizedException("This server is not licensed for Sonatype Developer.");
    }
  }

  private boolean isDevelopmentFeatureEnabled() {
    final Set<Feature> features = featuresService.getFeatures();

    return features.contains(LicensedFeature.DEVELOPER_DASHBOARD);
  }

  private ReachabilityStatus isSecurityReachable(
      final PolicyThreats policyThreats,
      final String componentHash)
  {
    return componentReachabilityService.isComponentReachable(policyThreats, componentHash);
  }

  private boolean isBulkRecommendationsEnabled() {
    final Set<Feature> features = featuresService.getFeatures();

    return features.contains(SystemConfigurationPropertyFeature.DEVELOPER_BULK_RECOMMENDATIONS);
  }

  private static boolean matchesFilter(final String componentName, final String filter) {
    return componentName.toLowerCase(Locale.ROOT)
        .matches(String.format(".*%s.*", Pattern.quote(filter.toLowerCase(Locale.ROOT))));
  }

  /**
   * Checks if the component has an upgrade path of direct Inner Source.
   */
  private boolean hasUpgradePathOfInnerSource(UnprioritizedComponent unprioritizedComponent) {
    if (!DEPENDENCY_TYPE_INNER_SOURCE_DIRECT.equals(unprioritizedComponent.dependencyType)) {
      return false;
    }

    String currentVersion = unprioritizedComponent.getComponentVersion();
    if (currentVersion == null) {
      return false;
    }

    ComponentIdentifier componentId = unprioritizedComponent.component.componentIdentifier.toComponentIdentifier();
    if (componentId == null) {
      return false;
    }

    String latestVersionByStage = innerSourceService.getComponentLatestVersionByStage(
        componentId, StageTypes.RELEASE.getId());
    if (latestVersionByStage == null) {
      return false;
    }

    CompositeComparableVersion currentComparableVersion = createCompositeComparableVersion(
        currentVersion,
        componentId.getFormat());
    CompositeComparableVersion latestComparableVersion = createCompositeComparableVersion(
        latestVersionByStage,
        componentId.getFormat());

    return currentComparableVersion.compareTo(latestComparableVersion) < 0;
  }

  private static class UnprioritizedComponent
  {
    public ApiComponentDTOV2 component;

    public final String dependencyType;

    public final Boolean hasFailActionOnComponent;

    public final String action;

    public final int highestThreat;

    public final String highestThreatPolicyName;

    public final String highestThreatPolicyConstraintName;

    public final Boolean securityReachable;

    public ApiVersionChangeOptionType remediationType;

    public String remediationVersion;

    public final int highestReachableThreat;

    public final boolean hasSameViolationsOnMain;

    public final boolean hasExpiredWaiver;

    public final boolean hasSoonToExpireWaiver;

    public boolean isAllViolationsWaived;

    public final String waiverExpirationDetails;

    public final int waivedViolationsCount;

    public final boolean hasAutoWaiver;

    public UnprioritizedComponent(
        final ApiComponentDTOV2 component,
        final String dependencyType,
        final Boolean hasFailActionOnComponent,
        final String action,
        final int highestThreat,
        final String highestThreatPolicyName,
        final String highestThreatPolicyConstraintName,
        final Boolean securityReachable,
        final ApiVersionChangeOptionType remediationType,
        final String remediationVersion,
        final int highestReachableThreat,
        final boolean hasSameViolationsOnMain,
        final boolean hasExpiredWaiver,
        final boolean hasSoonToExpireWaiver,
        boolean isAllViolationsWaived,
        final String waiverExpirationDetails,
        final int waivedViolationsCount,
        final boolean hasAutoWaiver)
    {
      this.dependencyType = dependencyType;
      this.hasFailActionOnComponent = hasFailActionOnComponent;
      this.action = action;
      this.highestThreat = highestThreat;
      this.highestThreatPolicyName = highestThreatPolicyName;
      this.highestThreatPolicyConstraintName = highestThreatPolicyConstraintName;
      this.securityReachable = securityReachable;
      this.remediationType = remediationType;
      this.remediationVersion = remediationVersion;
      this.component = component;
      this.highestReachableThreat = highestReachableThreat;
      this.hasSameViolationsOnMain = hasSameViolationsOnMain;
      this.hasExpiredWaiver = hasExpiredWaiver;
      this.hasSoonToExpireWaiver = hasSoonToExpireWaiver;
      this.isAllViolationsWaived = isAllViolationsWaived;
      this.waiverExpirationDetails = waiverExpirationDetails;
      this.waivedViolationsCount = waivedViolationsCount;
      this.hasAutoWaiver = hasAutoWaiver;
    }

    public PrioritizedComponent toPrioritizedComponent(final int priority) {
      return new PrioritizedComponent(
          component.displayName,
          getComponentIdentifier(),
          component.hash,
          dependencyType,
          hasFailActionOnComponent,
          action,
          highestThreat,
          highestThreatPolicyName,
          highestThreatPolicyConstraintName,
          securityReachable,
          priority,
          remediationType,
          remediationVersion,
          highestReachableThreat,
          hasSameViolationsOnMain,
          hasExpiredWaiver,
          hasSoonToExpireWaiver,
          isAllViolationsWaived,
          waiverExpirationDetails,
          waivedViolationsCount,
          hasAutoWaiver);
    }

    private ComponentIdentifier getComponentIdentifier() {
      return component.componentIdentifier != null ? component.componentIdentifier.toComponentIdentifier() : null;
    }

    private String getComponentVersion() {
      ComponentIdentifier componentIdentifier = getComponentIdentifier();
      return componentIdentifier != null ? componentIdentifier.get(ComponentIdentifier.VERSION) : null;
    }
  }
}
