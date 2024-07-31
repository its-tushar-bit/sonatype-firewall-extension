/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.development.prioritization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.api.experimental.development.prioritization.PrioritizedComponent;
import com.sonatype.insight.brain.api.v2.dto.ApiDependencyDataDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.dataaccess.development.prioritization.DevelopmentPrioritizationComponentInfoDAO;
import com.sonatype.insight.brain.features.FeaturesService;
import com.sonatype.insight.brain.label.ComponentLabelService;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.prioritization.DevelopmentPrioritizationComponentInfo;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats.Component;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats.PolicyViolation;
import com.sonatype.insight.error.exception.NotAuthorizedException;
import com.sonatype.insight.license.model.Feature;
import com.sonatype.insight.license.model.LicensedFeature;

import static com.sonatype.insight.brain.api.experimental.ApiVulnerabilitySignatureService.SECURITY_REACHABLE_LABEL;
import static com.sonatype.insight.brain.api.experimental.development.prioritization.PrioritizedComponent.DEPENDENCY_TYPE_DIRECT;
import static com.sonatype.insight.brain.api.experimental.development.prioritization.PrioritizedComponent.DEPENDENCY_TYPE_TRANSITIVE;
import static com.sonatype.insight.brain.api.experimental.development.prioritization.PrioritizedComponent.DEPENDENCY_TYPE_UNKNOWN;
import static com.sonatype.insight.brain.api.experimental.development.prioritization.PrioritizedComponent.DEPENDENCY_TYPE_INNER_SOURCE;

@Named
public class DevelopmentPrioritiesService
{
  private final FeaturesService featuresService;

  private final DevelopmentPrioritiesReportService developmentPrioritiesReportService;

  private final ComponentLabelService componentLabelService;

  private final DevelopmentPrioritizationComponentInfoDAO prioritizationComponentInfoDAO;

  private boolean isBulkRecommendationsEnabled;

  @Inject
  public DevelopmentPrioritiesService(
      final FeaturesService featuresService,
      final DevelopmentPrioritiesReportService developmentPrioritiesReportService,
      final ComponentLabelService componentLabelService,
      final DevelopmentPrioritizationComponentInfoDAO prioritizationComponentInfoDAO)
  {
    this.featuresService = featuresService;
    this.developmentPrioritiesReportService = developmentPrioritiesReportService;
    this.componentLabelService = componentLabelService;
    this.prioritizationComponentInfoDAO = prioritizationComponentInfoDAO;
  }

  public DevelopmentPrioritizationResults getPrioritizedFindings(
      final String applicationPublicId,
      final String scanId,
      final int page,
      final int pageSize
  )
  {
    throwErrorIfDevelopmentNotEnabledByLicense();

    // checks for read permissions on the app, making this an authorized function
    final ApiReportRawDataDTOV2 apiReportRawDataDTOV2 =
        developmentPrioritiesReportService.getDependencyInformation(applicationPublicId, scanId);
    final PolicyThreats policyThreats =
        this.developmentPrioritiesReportService.getPolicyThreatsNoAuth(applicationPublicId, scanId);

    final int skipCount = (page - 1) * pageSize;
    isBulkRecommendationsEnabled = isBulkRecommendationsEnabled();

    final List<UnprioritizedComponent> sortedComponents = apiReportRawDataDTOV2.components
        .stream()
        .map(component -> {
          final List<PolicyViolation> policyViolations =
              getMatchingViolations(policyThreats.aaData, component);

          // Component identifier can be null for unknown components
          final ComponentIdentifier componentIdentifier =  component.componentIdentifier != null ?
              component.componentIdentifier.toComponentIdentifier() :
              null;
          final PolicyViolation highestPolicyViolation = getHighestThreat(policyViolations);
          final int highestThreatLevel;
          final String policyName;
          final String highestThreatConstraintName;

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
              && isSecurityReachable(applicationPublicId, component.hash);

          DevelopmentPrioritizationComponentInfo prioritizationComponentInfo = null;
          if (isBulkRecommendationsEnabled && componentIdentifier != null) {
            // component.hash and componentIdentifier.toSyntheticHash() have different values. The synthetic hash
            // from the component identifier (does not use the binary) is what is stored in the database
            prioritizationComponentInfo = prioritizationComponentInfoDAO.getByScanIdAndComponentHash(scanId,
                componentIdentifier.toSyntheticHash());
          }

          return new UnprioritizedComponent(
              component.displayName,
              componentIdentifier,
              getDependencyType(component),
              hasFailActionOnComponent(policyViolations),
              getAction(policyViolations),
              highestThreatLevel,
              policyName,
              highestThreatConstraintName,
              component.hash,
              securityReachable,
              prioritizationComponentInfo
          );
        })
        .filter(unprioritizedComponent -> unprioritizedComponent.highestThreat > 0)
        .sorted(this::compareScoreDescending)
        .collect(Collectors.toList());

    final List<PrioritizedComponent> allPrioritizedFindings = addPrioritiesToSortedList(sortedComponents, 1);

    // pluck off top 3
    final int top3Bound = Math.min(allPrioritizedFindings.size(), 3);
    final List<PrioritizedComponent> top3Priorities = allPrioritizedFindings.subList(0, top3Bound);

    final List<PrioritizedComponent> remainingPrioritiesAll;

    if (allPrioritizedFindings.size() > 3) {
      remainingPrioritiesAll = allPrioritizedFindings.subList(3, allPrioritizedFindings.size());
    }
    else {
      remainingPrioritiesAll = new ArrayList<>();
    }

    // get total size before adjusting for pagination (pagination is only over remaining non-top 3 components
    final long totalSize = remainingPrioritiesAll.size();

    // take a page from remaining priorities
    final List<PrioritizedComponent> remainingPriorities = remainingPrioritiesAll
        .stream()
        .skip(skipCount)
        .limit(pageSize)
        .collect(Collectors.toList());

    return new DevelopmentPrioritizationResults(
        top3Priorities,
        new ApiPageResult<>(totalSize, page, pageSize, remainingPriorities));
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
        .collect(Collectors.toList());

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
        .collect(Collectors.toList());

    return !violationsWithFails.isEmpty();
  }

  final PolicyViolation getHighestThreat(List<PolicyThreats.PolicyViolation> activeViolations) {
    if (activeViolations.isEmpty()) {
      return null;
    }

    final List<PolicyThreats.PolicyViolation> violation = activeViolations
        .stream()
        .sorted((violationA, violationB) -> {
          if (violationA.policyThreatLevel != violationB.policyThreatLevel) {
            return Integer.compare(violationB.policyThreatLevel, violationA.policyThreatLevel);
          }
          else {
            // apply a secondary sort on ID so we always find the same Policy Violation
            return String.CASE_INSENSITIVE_ORDER.compare(violationA.policyViolationId, violationB.policyViolationId);
          }
        })
        .collect(Collectors.toList());

    return violation.get(0);
  }

  final boolean hasSecurityViolations(List<PolicyThreats.PolicyViolation> activeViolations) {
    if (activeViolations.isEmpty()) {
      return false;
    }

    return activeViolations
        .stream()
        .anyMatch(policyViolation -> "SECURITY".equals(policyViolation.policyThreatCategory));
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
      final List<UnprioritizedComponent> sortedComponents,
      final int firstPriority
  )
  {
    final List<PrioritizedComponent> prioritizedComponents = new ArrayList<>();

    if (sortedComponents.isEmpty()) {
      return prioritizedComponents;
    }

    prioritizedComponents.add(sortedComponents.get(0).toPrioritizedComponent(firstPriority));
    int priority = firstPriority;

    for (int i = 1; i < sortedComponents.size(); i++) {
      final UnprioritizedComponent componentToPrioritize = sortedComponents.get(i);

      final int currentScore = getScore(componentToPrioritize);
      final int lastScore = getScore(sortedComponents.get(i - 1));

      final boolean sameAsLastScore = lastScore == currentScore;

      if (sameAsLastScore) {
        prioritizedComponents.add(
            componentToPrioritize.toPrioritizedComponent(priority)
        );
      }
      else {
        prioritizedComponents.add(
            componentToPrioritize.toPrioritizedComponent(++priority)
        );
      }
    }

    return prioritizedComponents;
  }

  private int getScore(final UnprioritizedComponent unprioritizedComponent) {
    return getActionNumber(unprioritizedComponent.action) * 100000 +
        getSecurityReachableNumber(unprioritizedComponent) * 1000 +
        getRecommendationNumber(unprioritizedComponent) * 100 +
        unprioritizedComponent.highestThreat;
  }

  private int getSecurityReachableNumber(final UnprioritizedComponent unprioritizedComponent) {
    if (unprioritizedComponent.securityReachable) {
      return 1;
    }
    else {
      return 0;
    }
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
    if (isBulkRecommendationsEnabled && Objects.nonNull(unprioritizedComponent.prioritizationComponentInfo)) {
      final String originalComponentVersion =
          unprioritizedComponent.componentIdentifier.get(ComponentIdentifier.VERSION);
      final String remediationVersion = unprioritizedComponent.prioritizationComponentInfo.getRemediationVersion();

      // Remove from consideration situations where IQ recommends the same version because it isn't failing policy
      if (!originalComponentVersion.equals(remediationVersion)) {
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

  private int compareScoreDescending(final UnprioritizedComponent a, final UnprioritizedComponent b) {
    final int scoreA = getScore(a);
    final int scoreB = getScore(b);

    if (scoreA != scoreB) {
      return scoreB - scoreA;
    }
    else {
      // fall back to hash comparison to preserve order when there are score collisions
      return String.CASE_INSENSITIVE_ORDER.compare(a.hash, b.hash);
    }
  }

  private boolean isDevelopmentFeatureEnabled() {
    final Set<Feature> features = featuresService.getFeatures();

    return features.contains(LicensedFeature.DEVELOPER_DASHBOARD);
  }

  private boolean isSecurityReachable(final String applicationPublicId, final String componentHash) {
    return this.componentLabelService.getComponentLabelsNoAuth(
        OwnerType.APPLICATION,
        applicationPublicId,
        componentHash).labelsByOwner.stream()
        .anyMatch(label -> {
          return label.labels.stream().anyMatch(label1 -> label1.getLabel().equals(SECURITY_REACHABLE_LABEL));
        });
  }

  private boolean isBulkRecommendationsEnabled() {
    final Set<Feature> features = featuresService.getFeatures();

    return features.contains(SystemConfigurationPropertyFeature.DEVELOPER_BULK_RECOMMENDATIONS);
  }

  private static class UnprioritizedComponent
  {
    public final String displayName;

    public final ComponentIdentifier componentIdentifier;

    public final String dependencyType;

    public final Boolean hasFailActionOnComponent;

    public final String action;

    public final int highestThreat;

    public final String highestThreatPolicyName;

    public final String highestThreatPolicyConstraintName;

    public final String hash;

    public final boolean securityReachable;

    public DevelopmentPrioritizationComponentInfo prioritizationComponentInfo;

    public UnprioritizedComponent(
        final String displayName,
        final ComponentIdentifier componentIdentifier,
        final String dependencyType,
        final Boolean hasFailActionOnComponent,
        final String action,
        final int highestThreat,
        final String highestThreatPolicyName,
        final String highestThreatPolicyConstraintName,
        final String hash,
        final boolean securityReachable,
        final DevelopmentPrioritizationComponentInfo prioritizationComponentInfo
    )
    {
      this.displayName = displayName;
      this.componentIdentifier = componentIdentifier;
      this.dependencyType = dependencyType;
      this.hasFailActionOnComponent = hasFailActionOnComponent;
      this.action = action;
      this.highestThreat = highestThreat;
      this.highestThreatPolicyName = highestThreatPolicyName;
      this.highestThreatPolicyConstraintName = highestThreatPolicyConstraintName;
      this.hash = hash;
      this.securityReachable = securityReachable;
      this.prioritizationComponentInfo = prioritizationComponentInfo;
    }

    public PrioritizedComponent toPrioritizedComponent(final int priority) {
      return new PrioritizedComponent(
          displayName,
          componentIdentifier,
          hash,
          dependencyType,
          hasFailActionOnComponent,
          action,
          highestThreat,
          highestThreatPolicyName,
          highestThreatPolicyConstraintName,
          securityReachable,
          priority,
          prioritizationComponentInfo);
    }
  }
}
