/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.development.prioritization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.Comparator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.api.v2.dto.*;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.api.v2.service.ApiComponentRemediationService;
import com.sonatype.insight.brain.callflow.ComponentReachabilityService;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.development.prioritization.DevelopmentPrioritizationComponentInfoDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.development.prioritization.dto.PrioritizationRemediationVersionDTO;
import com.sonatype.insight.brain.features.FeaturesService;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.prioritization.DevelopmentPrioritizationComponentInfo;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats.Component;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats.PolicyViolation;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotAuthorizedException;
import com.sonatype.insight.license.model.Feature;
import com.sonatype.insight.license.model.LicensedFeature;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.api.v2.dto.PrioritizedComponent.DEPENDENCY_TYPE_DIRECT;
import static com.sonatype.insight.brain.api.v2.dto.PrioritizedComponent.DEPENDENCY_TYPE_INNER_SOURCE;
import static com.sonatype.insight.brain.api.v2.dto.PrioritizedComponent.DEPENDENCY_TYPE_TRANSITIVE;
import static com.sonatype.insight.brain.api.v2.dto.PrioritizedComponent.DEPENDENCY_TYPE_UNKNOWN;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.SECURITY;

@Named
public class DevelopmentPrioritiesService
{
  private static final Logger log = LoggerFactory.getLogger(DevelopmentPrioritiesService.class);

  private final FeaturesService featuresService;

  private final DevelopmentPrioritiesReportService developmentPrioritiesReportService;

  private final DevelopmentPrioritizationComponentInfoDAO prioritizationComponentInfoDAO;

  private final ReportService reportService;

  private final ComponentReachabilityService componentReachabilityService;

  private boolean isBulkRecommendationsEnabled;

  private final ApiComponentRemediationService apiComponentRemediationService;

  private final DevelopmentPrioritiesUtilsService developmentPrioritiesUtilsService;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final ApplicationDAO applicationDAO;

  @Inject
  public DevelopmentPrioritiesService(
      final FeaturesService featuresService,
      final DevelopmentPrioritiesReportService developmentPrioritiesReportService,
      final DevelopmentPrioritizationComponentInfoDAO prioritizationComponentInfoDAO,
      final ReportService reportService,
      final ComponentReachabilityService componentReachabilityService,
      final ApiComponentRemediationService apiComponentRemediationService,
      final DevelopmentPrioritiesUtilsService developmentPrioritiesUtilsService,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final ApplicationDAO applicationDAO)
  {
    this.featuresService = featuresService;
    this.developmentPrioritiesReportService = developmentPrioritiesReportService;
    this.prioritizationComponentInfoDAO = prioritizationComponentInfoDAO;
    this.reportService = reportService;
    this.componentReachabilityService = componentReachabilityService;
    this.apiComponentRemediationService = apiComponentRemediationService;
    this.developmentPrioritiesUtilsService = developmentPrioritiesUtilsService;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.applicationDAO = applicationDAO;
  }

  @Authorize(permission = Permission.READ)
  public DevelopmentPrioritizationResults getPrioritizedFindings(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) final String applicationPublicId,
      final String scanId,
      final int page,
      final int pageSize,
      final String componentNameFilter,
      final boolean includeRemediation,
      final boolean filterOnPolicyActions
  )
  {
    final int skipCount = (page - 1) * pageSize;

    final List<PrioritizedComponent> allPrioritizedFindings =
        includeRemediation ?
            getAllPrioritizedFindings(applicationPublicId, scanId, skipCount, pageSize) :
            getAllPrioritizedFindings(applicationPublicId, scanId, null, null);

    final List<PrioritizedComponent> filteredByNameAndAction = allPrioritizedFindings.stream()
        .filter(prioritizedComponent -> StringUtils.isEmpty(componentNameFilter) ||
            matchesFilter(prioritizedComponent.getDisplayName(), componentNameFilter))
        .filter(prioritizedComponent ->
            !filterOnPolicyActions || Action.ID_FAIL.equals(prioritizedComponent.getAction()) ||
                Action.ID_WARN.equals(prioritizedComponent.getAction())).toList();

    // get total size before for pagination
    final long totalSize = filteredByNameAndAction.size();

    final List<PrioritizedComponent> prioritizedFindingsForPagination = filteredByNameAndAction
        .stream()
        .skip(skipCount)
        .limit(pageSize)
        .toList();

    return new DevelopmentPrioritizationResults(new ApiPageResult<>(totalSize, page, pageSize,
        prioritizedFindingsForPagination));
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
      final Integer remediationLimit
  )
  {
    throwErrorIfDevelopmentNotEnabledByLicense();

    // checks for read permissions on the app, making this an authorized function
    final ApiReportRawDataDTOV2 apiReportRawDataDTOV2 =
        developmentPrioritiesReportService.getDependencyInformation(applicationPublicId, scanId);
    final PolicyThreats policyThreats = reportService.getPolicyThreats(applicationPublicId, scanId);

    PolicyEvaluation policyEvaluation =
        policyEvaluationDAO.getLastByApplicationIdAndScanId(
            applicationDAO.getByPublicId(applicationPublicId).getId(),
            scanId);

    isBulkRecommendationsEnabled = isBulkRecommendationsEnabled();

    final List<UnprioritizedComponent> sortedComponents = apiReportRawDataDTOV2.components
        .stream()
        .map(component -> {
          final List<PolicyViolation> policyViolations =
              getMatchingViolations(policyThreats.aaData, component);

          // Component identifier can be null for unknown components
          final ComponentIdentifier componentIdentifier = component.componentIdentifier != null ?
              component.componentIdentifier.toComponentIdentifier() :
              null;
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

          final boolean securityReachable = hasSecurityViolations(policyViolations)
              && isSecurityReachable(applicationPublicId, scanId, component.hash);

          ApiVersionChangeOptionType remediationType = null;
          String remediationVersion = null;
          if (securityReachable) {
            final PolicyViolation highestReachablePolicyViolation = getHighestThreat(policyViolations, true);
            if (highestReachablePolicyViolation != null) {
              highestReachableThreatLevel = highestReachablePolicyViolation.policyThreatLevel;
            }
          }

          if (isBulkRecommendationsEnabled && componentIdentifier != null) {
            // component.hash and componentIdentifier.toSyntheticHash() have different values. The synthetic hash
            // from the component identifier (does not use the binary) is what is stored in the database
            DevelopmentPrioritizationComponentInfo prioritizationComponentInfo =
                prioritizationComponentInfoDAO.getByScanIdAndComponentHash(scanId,
                    componentIdentifier.toSyntheticHash());

            if (prioritizationComponentInfo != null) {
              remediationType = prioritizationComponentInfo.getRemediationType();
              remediationVersion = prioritizationComponentInfo.getRemediationVersion();
            }
          }

          return new UnprioritizedComponent(
              component,
              getDependencyType(component),
              hasFailActionOnComponent(policyViolations),
              getAction(policyViolations),
              highestThreatLevel,
              policyName,
              highestThreatConstraintName,
              securityReachable,
              remediationType,
              remediationVersion,
              highestReachableThreatLevel
          );
        })
        .filter(unprioritizedComponent -> unprioritizedComponent.highestThreat > 0)
        .sorted(Comparator.comparingInt(this::getScore).thenComparingInt(this::getHighestThreat).reversed())
        .toList();

    List<UnprioritizedComponent> sortedComponentsWithRemediation = setRemediationForComponents(
        sortedComponents,
        applicationPublicId,
        scanId,
        policyEvaluation != null ? policyEvaluation.getStageTypeId() : null,
        remediationSkip,
        remediationLimit
    );

    // If skipCount and limit are not provided, return all sorted components without remediation
    return addPrioritiesToSortedList(sortedComponentsWithRemediation);
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
      final Integer remediationLimit)
  {
    if (remediationSkip != null && remediationLimit != null) {
      return IntStream.range(0, sortedComponents.size())
          .mapToObj(index -> {
            UnprioritizedComponent unprioritizedComponent = sortedComponents.get(index);

            if (index >= remediationSkip && index < remediationSkip + remediationLimit) {
              return loadRemediation(unprioritizedComponent, applicationPublicId, scanId, stageId);
            }

            return unprioritizedComponent;
          })
          .toList();
    }

    return sortedComponents;
  }

  private UnprioritizedComponent loadRemediation(UnprioritizedComponent unprioritizedComponent,
                                                 String applicationPublicId, String scanId, String stageId)
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
                true
            );

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
      return DEPENDENCY_TYPE_INNER_SOURCE;
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
        .stream().filter(policyViolation -> {
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
          violationsStream.filter(policyViolation ->
              SECURITY.getName().equalsIgnoreCase(policyViolation.policyThreatCategory));
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
        .anyMatch(policyViolation ->
            SECURITY.getName().equalsIgnoreCase(policyViolation.policyThreatCategory));
  }

  private List<PolicyThreats.PolicyViolation> getMatchingViolations(
      final List<Component> policyThreatsComponents,
      final ApiReportComponentDTOV2 component
  )
  {
    return policyThreatsComponents.stream()
        .filter(policyThreatComponent -> policyThreatComponent.hash.equals(component.hash))
        .map(comp -> comp.activeViolations)
        .flatMap(Collection::stream)
        .filter(violation -> !violation.legacyViolation)
        .collect(Collectors.toList());
  }

  private List<PrioritizedComponent> addPrioritiesToSortedList(
      final List<UnprioritizedComponent> sortedComponents
  )
  {
    final List<PrioritizedComponent> prioritizedComponents = new ArrayList<>();

    if (sortedComponents.isEmpty()) {
      return prioritizedComponents;
    }

    for (int i = 0; i < sortedComponents.size(); i++) {
      final UnprioritizedComponent componentToPrioritize = sortedComponents.get(i);
      prioritizedComponents.add(componentToPrioritize.toPrioritizedComponent(i + 1));
    }

    return prioritizedComponents;
  }

  private int getScore(final UnprioritizedComponent unprioritizedComponent) {
    return getActionNumber(unprioritizedComponent.action) * 100000 +
        getRecommendationNumber(unprioritizedComponent) * 100 +
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

  private int getRecommendationNumber(final UnprioritizedComponent unprioritizedComponent) {
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

  private void throwErrorIfDevelopmentNotEnabledByLicense() {
    if (!isDevelopmentFeatureEnabled()) {
      throw new NotAuthorizedException("This server is not licensed for Sonatype Developer.");
    }
  }

  private boolean isDevelopmentFeatureEnabled() {
    final Set<Feature> features = featuresService.getFeatures();

    return features.contains(LicensedFeature.DEVELOPER_DASHBOARD);
  }

  private boolean isSecurityReachable(
      final String applicationId,
      final String scanId,
      final String componentHash)
  {
    return componentReachabilityService.isComponentReachable(applicationId, scanId, componentHash);
  }

  private boolean isBulkRecommendationsEnabled() {
    final Set<Feature> features = featuresService.getFeatures();

    return features.contains(SystemConfigurationPropertyFeature.DEVELOPER_BULK_RECOMMENDATIONS);
  }

  private static boolean matchesFilter(final String componentName, final String filter) {
    return componentName.toLowerCase(Locale.ROOT)
        .matches(String.format(".*%s.*", Pattern.quote(filter.toLowerCase(Locale.ROOT))));
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

    public final boolean securityReachable;

    public ApiVersionChangeOptionType remediationType;

    public String remediationVersion;

    public final int highestReachableThreat;

    public UnprioritizedComponent(
        final ApiComponentDTOV2 component,
        final String dependencyType,
        final Boolean hasFailActionOnComponent,
        final String action,
        final int highestThreat,
        final String highestThreatPolicyName,
        final String highestThreatPolicyConstraintName,
        final boolean securityReachable,
        final ApiVersionChangeOptionType remediationType,
        final String remediationVersion,
        final int highestReachableThreat
    )
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
          highestReachableThreat);
    }

    private ComponentIdentifier getComponentIdentifier() {
      return component.componentIdentifier != null ?
          component.componentIdentifier.toComponentIdentifier() :
          null;
    }

    private String getComponentVersion() {
      ComponentIdentifier componentIdentifier = getComponentIdentifier();
      return componentIdentifier != null ?
          componentIdentifier.get(ComponentIdentifier.VERSION) : null;
    }
  }
}
