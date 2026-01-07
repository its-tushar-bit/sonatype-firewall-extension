/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.actions.ApiComponentChangeActionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiSuggestedVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.innersource.InnerSourceService;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.DependencyType;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.policy.evaluator.ComponentPolicyEvaluator;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.report.CompositeComparableVersion;
import com.sonatype.insight.brain.telemetry.NonBreakingRecommendationTelemetryMetrics;
import com.sonatype.insight.brain.telemetry.NonBreakingRecommendationTelemetryStats.SourceEndpoint;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.InvalidPackageURLException;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.report.InnerSourceUtils.createCompositeComparableVersion;

/**
 * @since 1.83
 * This code was formerly in ApiComponentRemediationService but was split out to avoid a circular dependency
 */
@Named
public class ComponentRemediationService
{
  private static final Logger log = LoggerFactory.getLogger(ComponentRemediationService.class);

  private static final int SEVERITY_THRESHOLD = 2;

  public static final List<ApiVersionChangeOptionType> PREFERABLE_TYPE_ORDER = List.of(
      ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES,
      ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING,
      // The above two are here to be logically consistent.
      // InnerSource specific remediation types
      ApiVersionChangeOptionType.INNER_SOURCE_LATEST_NON_BREAKING,
      ApiVersionChangeOptionType.INNER_SOURCE_LATEST,
      // The below are the actual types to be in the `versionChanges` list.
      ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES,
      ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS,
      ApiVersionChangeOptionType.NEXT_NON_FAILING_WITH_DEPENDENCIES,
      ApiVersionChangeOptionType.NEXT_NON_FAILING
  );

  private static final String OWNER_TYPE_ATTR = "owner_type";

  private static final String OWNER_ID_ATTR = "owner_id";

  private static final String COMPONENT_ATTR = "component";

  private static final String OPTION_NEXT_NO_VIOLATIONS_ATTR = "option_next_no_violations";

  private static final String OPTION_NEXT_NON_FAILING_ATTR = "option_next_non_failing";

  private static final String OPTION_NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES_ATTR =
      "option_next_no_violations_with_dependencies";

  private static final String OPTION_NEXT_NON_FAILING_WITH_DEPENDENCIES_ATTR =
      "option_next_non_failing_with_dependencies";

  private final TelemetrySender telemetrySender;

  private final HdsClient hdsClient;

  private final ComponentPolicyEvaluator componentPolicyEvaluator;

  private final ProductLicense productLicense;

  private final TelemetryUtils telemetryUtils;

  private final VersionScoringService versionScoringService;

  private final NonBreakingRecommendationTelemetryMetrics nonBreakingRecommendationTelemetryMetrics;
  
  private final InnerSourceService innerSourceService;

  @Inject
  public ComponentRemediationService(
      TelemetrySender telemetrySender,
      HdsClient hdsClient,
      ComponentPolicyEvaluator componentPolicyEvaluator,
      ProductLicense productLicense,
      TelemetryUtils telemetryUtils,
      VersionScoringService versionScoringService,
      NonBreakingRecommendationTelemetryMetrics nonBreakingRecommendationTelemetryMetrics,
      InnerSourceService innerSourceService)
  {
    this.telemetrySender = telemetrySender;
    this.hdsClient = hdsClient;
    this.componentPolicyEvaluator = componentPolicyEvaluator;
    this.productLicense = productLicense;
    this.telemetryUtils = telemetryUtils;
    this.versionScoringService = versionScoringService;
    this.nonBreakingRecommendationTelemetryMetrics = nonBreakingRecommendationTelemetryMetrics;
    this.innerSourceService = innerSourceService;
  }

  private ComponentIdentifier ensureCompleteIfNeeded(ComponentIdentifier componentIdentifier) {
    if (ComponentIdentifier.FORMAT_CONAN.equals(componentIdentifier.getFormat())) {
      try {
        componentIdentifier.ensureComplete();
      }
      catch (InvalidComponentIdentifierException e) {
        throw new BadRequestException(e.getMessage(), e);
      }
    }
    return componentIdentifier;
  }

  /**
   * Get the suggested component remediations, specifically including "advanced" strategies which include
   * non-violating and non-failing components when taking their dependencies into account.
   */
  public ApiComponentRemediationValueDTO getSuggestedRemediation(
      final ComponentIdentifier currentComponent,
      final List<ComponentDetailsDTO> allVersions,
      final Owner owner,
      final String stageId,
      final ComponentDetailsLoader componentDetailsLoader,
      final SourceEndpoint source
  )
  {
    ApiComponentRemediationValueDTO suggestedRemediation = getSuggestedSelectedRemediation(
        currentComponent,
        allVersions,
        owner,
        stageId,
        componentDetailsLoader,
        shouldIncludeAdvancedStrategies(currentComponent));
    // Collecting telemetry for non-breaking version suggestions can be expensive sometimes.
    // It can be disabled by setting the feature flag to false.
    if (SystemConfigurationPropertyFeature.NON_BREAKING_VERSION_SUGGESTION_TELEMETRY.isEnabled()) {
      nonBreakingRecommendationTelemetryMetrics.collect(suggestedRemediation.suggestedVersionChange, owner, source);
    }
    return suggestedRemediation;
  }

  public ApiComponentRemediationValueDTO getSuggestedSelectedRemediation(
      final ComponentIdentifier currentComponent,
      final List<ComponentDetailsDTO> allVersions,
      final Owner owner,
      final String stageId,
      ComponentDetailsLoader componentDetailsLoader,
      final boolean advancedStrategies)
  {
    ApiComponentRemediationValueDTO componentRemediationDto = new ApiComponentRemediationValueDTO();
    ensureCompleteIfNeeded(currentComponent);

    if (innerSourceService.isInnerSourceComponent(currentComponent)) {
      try {
        String latestVersion =
            innerSourceService.getComponentLatestVersionByStage(currentComponent, StageTypes.RELEASE.getId());

        String currentVersion = currentComponent.get(ComponentIdentifier.VERSION);

        if (isNewerVersion(currentVersion, latestVersion, currentComponent.getFormat())) {
          ComponentIdentifier latestComponentIdentifier = currentComponent.createAlternativeVersion(latestVersion);
          ApiVersionChangeOptionType remediationType =
              determineInnerSourceRemediationType(currentVersion, latestVersion, currentComponent.getFormat());
          componentRemediationDto.suggestedVersionChange =
              createSuggestedVersionChangeOption(latestComponentIdentifier, remediationType, false);
        }
      }
      catch (Exception e) {
        log.warn("Failed to get remediation for InnerSource component: {}", currentComponent, e);
        throw new BadRequestException(e.getMessage(), e);
      }
    }
    
    int currentIndex = findCurrentIndex(allVersions, currentComponent);

    Map<String, Object> telemetryAttributes = new HashMap<>();

    if (currentIndex >= 0) { // only process if we find a current version of the component
      // find non-violating and non-failing versions
      List<ComponentDetailsDTO> nonViolatingVersions = nonViolatingVersions(currentIndex, allVersions);

      // find first non violating version
      nonViolatingVersions.stream()
          .findFirst()
          .ifPresent(dto -> {
            componentRemediationDto.versionChanges.add(
                createVersionChangeOption(dto.componentIdentifier, ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS,
                    dto.breakingChangesCount));
            telemetryAttributes.put(OPTION_NEXT_NO_VIOLATIONS_ATTR, String.valueOf(true));
          });

      // nonFailingVersions will be empty if stage is not specified
      // and we won't add non-failing/non-failing with dependencies remedies.
      List<ComponentDetailsDTO> nonFailingVersions = (stageId == null) ?
          Collections.emptyList() :
          nonFailingVersions(currentIndex, allVersions);

      // find first non failing version
      nonFailingVersions.stream()
          .findFirst()
          .ifPresent(dto -> {
            componentRemediationDto.versionChanges.add(
                createVersionChangeOption(dto.componentIdentifier, ApiVersionChangeOptionType.NEXT_NON_FAILING,
                    dto.breakingChangesCount));
            telemetryAttributes.put(OPTION_NEXT_NON_FAILING_ATTR, String.valueOf(true));
          });

      // Defined outside the feature-flagged block to avoid duplicate computation.
      List<String> nonBreakingVersionsSortedByScore = new ArrayList<>();
      if (SystemConfigurationPropertyFeature.DEVELOPER_SUGGEST_NON_BREAKING_VERSION.isEnabled()) {
        log.debug("Fetching non-breaking versions for component: {}", currentComponent);
        nonBreakingVersionsSortedByScore.addAll(versionScoringService.getSortedNonBreakingVersionsNoAuth(
            List.of(currentComponent)).getOrDefault(currentComponent, Collections.emptyList()));
        Set<ComponentIdentifier> nonViolatingVersionsSet =
            findNoViolatingAboveSeverityThreshold(currentIndex, allVersions, SEVERITY_THRESHOLD)
                .map(dto -> dto.componentIdentifier)
                .collect(Collectors.toSet());
        Optional<ComponentIdentifier> topScoreNonViolatingNonBreakingVersion = nonBreakingVersionsSortedByScore.stream()
            .map(currentComponent::createAlternativeVersion)
            .filter(nonViolatingVersionsSet::contains)
            .findFirst();
        topScoreNonViolatingNonBreakingVersion.ifPresent(topScore ->
            // the non-golden version suggestion should happen before (to be overridden by) the golden suggestion.
            componentRemediationDto.suggestedVersionChange = createSuggestedVersionChangeOption(
            topScore, ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING, false));
      }

      if (advancedStrategies) {
        boolean includeAdvancedStrategies = shouldIncludeAdvancedStrategies(currentComponent);

        if (includeAdvancedStrategies) {
          final ComponentDependenciesDTO componentDependencies =
              fetchDependencyInformation(
                  allVersions,
                  currentIndex
              );
          Map<PackageUrlIdentifier, List<PolicyAlert>> dependencyAlerts = getDependencyAlerts(componentDependencies,
              owner, stageId, componentDetailsLoader);

          // find first non violating where dependencies have no violations
          nonViolatingWithDependencies(nonViolatingVersions, dependencyAlerts)
              .ifPresent(dto -> {
                componentRemediationDto.versionChanges.add(
                    createVersionChangeOption(dto.componentIdentifier,
                        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES, dto.breakingChangesCount)
                );
                telemetryAttributes.put(OPTION_NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES_ATTR, String.valueOf(true));
              });

          // find first non failing where dependencies have no violations
          nonFailingWithDependencies(nonFailingVersions, dependencyAlerts)
              .ifPresent(dto -> {
                componentRemediationDto.versionChanges.add(
                    createVersionChangeOption(dto.componentIdentifier,
                        ApiVersionChangeOptionType.NEXT_NON_FAILING_WITH_DEPENDENCIES, dto.breakingChangesCount)
                );
                telemetryAttributes.put(OPTION_NEXT_NON_FAILING_WITH_DEPENDENCIES_ATTR, String.valueOf(true));
              });

          if (SystemConfigurationPropertyFeature.DEVELOPER_SUGGEST_NON_BREAKING_VERSION.isEnabled()) {
            log.debug("Fetching non-breaking versions with dependencies for component: {}", currentComponent);
            Set<ComponentDetailsDTO> nonViolatingVersionsAboveThreshold =
                findNoViolatingAboveSeverityThreshold(currentIndex, allVersions, SEVERITY_THRESHOLD)
                .collect(Collectors.toSet());

            List<ComponentIdentifier> nonViolatingVersionsWithDependenciesSet =
                nonViolatingWithDependenciesAboveThreshold(
                nonViolatingVersionsAboveThreshold,
                dependencyAlerts,
                SEVERITY_THRESHOLD
            ).map(dto -> dto.componentIdentifier).toList();

            Optional<ComponentIdentifier> topScoreNonViolatingNonBreakingWithDependenciesVersion =
                nonBreakingVersionsSortedByScore.stream()
                .map(currentComponent::createAlternativeVersion)
                .filter(nonViolatingVersionsWithDependenciesSet::contains)
                .findFirst();
            topScoreNonViolatingNonBreakingWithDependenciesVersion.ifPresent(topScore ->
                // the golden version suggestion should happen after (override) the non-golden version suggestions.
                componentRemediationDto.suggestedVersionChange = createSuggestedVersionChangeOption(
                    topScore, ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES, true));
          }
        }
      }
    }
    componentRemediationDto.versionChanges = sortAndDeduplicateVersionChanges(componentRemediationDto.versionChanges);
    log.debug("Created {} component remediation version changes", componentRemediationDto.versionChanges.size());
    sendTelemetry(owner, currentComponent, telemetryAttributes);
    return componentRemediationDto;
  }

  public static List<ApiVersionChangeOptionDTO> sortAndDeduplicateVersionChanges(
      final List<ApiVersionChangeOptionDTO> versionChanges)
  {
    Comparator<ApiVersionChangeOptionDTO> comparatorWithTypeOrder =
        Comparator.comparingInt(dto -> PREFERABLE_TYPE_ORDER.indexOf(dto.getType()));
    List<ApiVersionChangeOptionDTO> sortedVersionChanges = versionChanges.stream()
        .sorted(comparatorWithTypeOrder)
        .toList();

    Set<String> seenPackageUrls = new HashSet<>();
    List<ApiVersionChangeOptionDTO> deduplicatedVersionChanges = new ArrayList<>();
    for (ApiVersionChangeOptionDTO versionChange : sortedVersionChanges) {
      String packageUrl = versionChange.getData().getComponent().packageUrl;
      if (!seenPackageUrls.contains(packageUrl)) {
        deduplicatedVersionChanges.add(versionChange);
      }
      seenPackageUrls.add(packageUrl);
    }

    return deduplicatedVersionChanges;
  }

  public ApiComponentRemediationValueDTO getSuggestedRemediationForTransitive(
      final Map<ComponentIdentifier, List<ComponentDetailsDTO>> componentIdentifierToAllVersionMap,
      final ApiComponentIdentifierDTOV2 transitiveComponent,
      final Owner owner,
      final String stageId,
      final ComponentDetailsLoader componentDetailsLoader)
  {

    ApiComponentRemediationValueDTO transitiveComponentRemediationValueDTO = new ApiComponentRemediationValueDTO();
    int currentIndex = -1;
    Map<String, Object> telemetryAttributes = new HashMap<>();

    for (Map.Entry<ComponentIdentifier, List<ComponentDetailsDTO>> entry
        : componentIdentifierToAllVersionMap.entrySet()) {

      ComponentIdentifier directComponentIdentifier = entry.getKey();
      List<ComponentDetailsDTO> allVersions = entry.getValue();

      currentIndex = findCurrentIndex(allVersions, directComponentIdentifier);

      if (currentIndex < 0) {
        continue;
      }

      List<ComponentDetailsDTO> nonViolatingVersions = nonViolatingVersions(currentIndex, allVersions);
      List<ComponentDetailsDTO> nonFailingVersions =
          (stageId == null) ? Collections.emptyList() : nonFailingVersions(currentIndex, allVersions);

      boolean includeAdvancedStrategies = shouldIncludeAdvancedStrategies(directComponentIdentifier);

      if (includeAdvancedStrategies) {
        final ComponentDependenciesDTO componentDependencies = fetchDependencyInformation(allVersions, currentIndex);
        Map<PackageUrlIdentifier, List<PolicyAlert>> dependencyAlerts =
            getDependencyAlerts(componentDependencies, owner, stageId, componentDetailsLoader);

        if (!nonViolatingVersions.isEmpty()) {
          nonViolatingWithDependencies(nonViolatingVersions, dependencyAlerts).ifPresent(dto -> {
            ApiVersionChangeOptionDTO versionChangeOption =
                createVersionChangeOptionForTransitiveComponent(transitiveComponent, dto,
                    ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES);
            telemetryAttributes.put(OPTION_NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES_ATTR, String.valueOf(true));
            transitiveComponentRemediationValueDTO.versionChanges.add(versionChangeOption);
          });
        }

        if (!nonFailingVersions.isEmpty()) {
          nonFailingWithDependencies(nonFailingVersions, dependencyAlerts).ifPresent(dto -> {
            ApiVersionChangeOptionDTO versionChangeOption =
                createVersionChangeOptionForTransitiveComponent(transitiveComponent, dto,
                    ApiVersionChangeOptionType.NEXT_NON_FAILING_WITH_DEPENDENCIES);
            telemetryAttributes.put(OPTION_NEXT_NON_FAILING_WITH_DEPENDENCIES_ATTR, String.valueOf(true));
            transitiveComponentRemediationValueDTO.versionChanges.add(versionChangeOption);
          });
        }
      }

      sendTelemetry(owner, directComponentIdentifier, telemetryAttributes);
    }

    return transitiveComponentRemediationValueDTO;
  }

  /**
   * This method will return an `ApiVersionChangeOptionDTO` representing the `suggestedVersionChange` if it is not null.
   * Otherwise, it will return a `NEXT_NO_VIOLATING_WITH_DEPENDENCIES` or `NEXT_NON_VIOLATIONS` entry from the
   * versionChange if one is present, or an empty `Optional` otherwise.
   */
  public Optional<ApiVersionChangeOptionDTO> getApplicableVersionChange(
      ApiSuggestedVersionChangeOptionDTO suggestedVersionChange,
      List<ApiVersionChangeOptionDTO> versionChanges)
  {
    if (versionChanges.isEmpty() && suggestedVersionChange == null) {
      return Optional.empty();
    }

    if (suggestedVersionChange != null) {
      ApiVersionChangeOptionDTO changeOption = new ApiVersionChangeOptionDTO(
          suggestedVersionChange.getType(),
          suggestedVersionChange.getData()
      );
      return Optional.of(changeOption);
    }

    Optional<ApiVersionChangeOptionDTO> versionChange = versionChanges.stream().filter(
        vChange -> vChange.getType() ==
            ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES).findFirst();

    if (!versionChange.isPresent()) {
      versionChange = versionChanges.stream().filter(
          vChange -> vChange.getType() ==
              ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS).findFirst();
    }

    return versionChange;
  }

  /**
   * This method is similar to `getApplicableVersionChange`, but it also considers the case where
   * `NEXT_NON_FAILING` and `NEXT_NON_FAILING_WITH_DEPENDENCIES` are presented
   */
  public Optional<ApiVersionChangeOptionDTO> getApplicableVersionChangeFromAllType(
      ApiSuggestedVersionChangeOptionDTO suggestedVersionChange,
      List<ApiVersionChangeOptionDTO> versionChanges)
  {
    if (versionChanges.isEmpty() && suggestedVersionChange == null) {
      return Optional.empty();
    }

    if (suggestedVersionChange != null) {
      ApiVersionChangeOptionDTO changeOption = new ApiVersionChangeOptionDTO(
          suggestedVersionChange.getType(),
          suggestedVersionChange.getData()
      );
      return Optional.of(changeOption);
    }

    Optional<ApiVersionChangeOptionDTO> versionChange = versionChanges.stream()
        .sorted(Comparator.comparingInt(v -> PREFERABLE_TYPE_ORDER.indexOf(v.getType())))
        .findFirst();

    return versionChange;
  }

  /**
   * evaluates dependencies and returns a map of each version's component identifier to its dependencies policy alerts
   */
  private Map<PackageUrlIdentifier, List<PolicyAlert>> getDependencyAlerts(
      final ComponentDependenciesDTO dependenciesDto,
      final Owner owner,
      final String stageId,
      ComponentDetailsLoader componentDetailsLoader)
  {
    Map<PackageUrlIdentifier, List<PolicyAlert>> dependencyAlerts = new HashMap<>();
    final Collection<ComponentDetails> componentDetailsList = dependenciesDto.getDetailsMap().values();
    final Stage stage = new Stage(stageId != null ? stageId : BuildStageType.ID);

    // evaluate flattened dependencies
    // Fix match state to exact as there's no point propagating it to other versions.
    // Assume the dependencies are only transitive
    List<Component> components = componentDetailsLoader
        .augmentComponentDetails(componentDetailsList, MatchState.EXACT.getId(), DependencyType.TRANSITIVE);
    Map<PackageUrlIdentifier, List<PolicyAlert>> policyAlertsByComponent =
        evaluateAndGetPolicyAlertsByComponent(owner.getId(), stage, components);

    // map parent components to their dependency alerts
    for (Map.Entry<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> entry :
        dependenciesDto.getDependenciesMap().entrySet())
    {
      dependencyAlerts.put(entry.getKey(),
          entry.getValue().stream()
              .flatMap(purl -> {
                List<PolicyAlert> alert = policyAlertsByComponent.get(purl);
                return (alert == null) ? Stream.empty() : alert.stream();
              })
              .collect(Collectors.toList()));
    }

    return dependencyAlerts;
  }

  private ComponentDependenciesDTO fetchDependencyInformation(
      final Collection<ComponentDetailsDTO> allVersions,
      final int currentComponentIndex
  )
  {
    final Collection<PackageUrlIdentifier> candidatePurls = allVersions.stream()
        .skip(currentComponentIndex)
        .map(version -> version.componentIdentifier)
        .map(PackageUrlIdentifier::fromComponentIdentifier)
        .collect(Collectors.toList());

    return getComponentDependencies(candidatePurls);
  }

  private Map<PackageUrlIdentifier, List<PolicyAlert>> evaluateAndGetPolicyAlertsByComponent(
      String ownerId,
      Stage stage,
      List<Component> components)
  {
    List<PolicyAlert> allPolicyAlerts = componentPolicyEvaluator.evaluate(ownerId, stage, components);

    Map<PackageUrlIdentifier, List<PolicyAlert>> policyAlertsByComponent = new HashMap<>();
    for (PolicyAlert policyAlert : allPolicyAlerts) {
      for (ComponentFact componentFact : policyAlert.getTrigger().getComponentFacts()) {
        policyAlertsByComponent.computeIfAbsent(
            PackageUrlIdentifier.fromComponentIdentifier(componentFact.getComponentIdentifier()),
            key -> new ArrayList<>()
        ).add(policyAlert);
      }
    }

    return policyAlertsByComponent;
  }

  private List<ComponentDetailsDTO> nonViolatingVersions(int startingIndex, List<ComponentDetailsDTO> dtos) {
    return dtos.stream().parallel()
        .skip(startingIndex)
        .filter(dto -> dto.violatedPolicyCount == 0)
        .toList();
  }

  private List<ComponentDetailsDTO> nonFailingVersions(int startingIndex, List<ComponentDetailsDTO> dtos) {
    return dtos.stream().parallel()
        .skip(startingIndex)
        .filter(dto -> !hasFailAction(dto.policyAlerts))
        .toList();
  }

  private Stream<ComponentDetailsDTO> findNoViolatingAboveSeverityThreshold(int startingIndex,
                                                                            List<ComponentDetailsDTO> dtos,
                                                                            int severityThreshold)
  {
    return dtos.stream().parallel()
        .skip(startingIndex)
        .filter(dto -> {
          Stream<Integer> stream;
          if (dto.policyMaxThreatLevelsByCategory == null) {
            stream = Stream.empty();
          }
          else {
            stream = dto.policyMaxThreatLevelsByCategory.values().stream();
          }
          return stream.max(Integer::compareTo).orElse(0) < severityThreshold;
        });
  }

  private ComponentIdentifier tryEnsureCompleteIdentifier(PackageUrlIdentifier versionPurl) {
    try {
      return versionPurl.ensureCompleteIdentifier();
    }
    catch (InvalidPackageURLException e) {
      throw new BadRequestException(e.getMessage(), e);
    }
  }

  private Optional<ComponentDetailsDTO> nonViolatingWithDependencies(
      Collection<ComponentDetailsDTO> nonViolatingVersions,
      Map<PackageUrlIdentifier, List<PolicyAlert>> dependencyAlerts)
  {
    for (ComponentDetailsDTO dto : nonViolatingVersions) {
      final PackageUrlIdentifier versionPurl = PackageUrlIdentifier.fromComponentIdentifier(dto.componentIdentifier);
      if (CollectionUtils.isEmpty(dependencyAlerts.get(versionPurl)))
      {
        dto.componentIdentifier = tryEnsureCompleteIdentifier(versionPurl);
        return Optional.of(dto);
      }
    }
    return Optional.empty();
  }

  private Stream<ComponentDetailsDTO> nonViolatingWithDependenciesAboveThreshold(
      final Collection<ComponentDetailsDTO> nonViolatingVersions,
      final Map<PackageUrlIdentifier, List<PolicyAlert>> dependencyAlerts,
      final int severityThreshold)
  {
    return nonViolatingVersions.stream().filter(dto -> {
      final PackageUrlIdentifier versionPurl = PackageUrlIdentifier.fromComponentIdentifier(dto.componentIdentifier);
      if (CollectionUtils.isEmpty(dependencyAlerts.get(versionPurl)))
      {
        dto.componentIdentifier = tryEnsureCompleteIdentifier(versionPurl);
        return true;
      }

      long dependenciesAboveThresholdCount =
          dependencyAlerts.get(versionPurl).stream().filter(dependencyAlert ->
              dependencyAlert.getTrigger().getThreatLevel() > severityThreshold).count();

      return dependenciesAboveThresholdCount == 0;
    });
  }

  private Optional<ComponentDetailsDTO> nonFailingWithDependencies(
      Collection<ComponentDetailsDTO> nonFailingVersions,
      Map<PackageUrlIdentifier, List<PolicyAlert>> dependencyAlerts)
  {
    for (ComponentDetailsDTO dto : nonFailingVersions) {
      final PackageUrlIdentifier versionPurl = PackageUrlIdentifier.fromComponentIdentifier(dto.componentIdentifier);
      List<PolicyAlert> policyAlerts = dependencyAlerts.get(versionPurl);
      if (policyAlerts == null || !hasFailAction(policyAlerts))
      {
        dto.componentIdentifier = tryEnsureCompleteIdentifier(versionPurl);
        return Optional.of(dto);
      }
    }
    return Optional.empty();
  }

  private boolean hasFailAction(List<PolicyAlert> alerts) {
    return alerts.stream().anyMatch(
        alert -> Optional.ofNullable(alert.getActions()).map(Collection::stream).orElseGet(Stream::empty)
            .anyMatch(action -> Action.ID_FAIL.equals(action.getActionTypeId())));
  }

  private void sendTelemetry(
      final Owner owner,
      final ComponentIdentifier componentIdentifier,
      final Map<String, Object> attributes)
  {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.COMPONENT_REMEDIATION);
    attributes.put(COMPONENT_ATTR, HdsClientAnalytics.obfuscate(JsonUtils.writeUnformatted(componentIdentifier)));
    attributes.put(OWNER_TYPE_ATTR, owner.getType().toString());
    attributes.put(OWNER_ID_ATTR, HdsClientAnalytics.obfuscate(owner.getId()));
    attributes.putIfAbsent(OPTION_NEXT_NO_VIOLATIONS_ATTR, String.valueOf(false));
    attributes.putIfAbsent(OPTION_NEXT_NON_FAILING_ATTR, String.valueOf(false));
    attributes.putIfAbsent(OPTION_NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES_ATTR, String.valueOf(false));
    attributes.putIfAbsent(OPTION_NEXT_NON_FAILING_WITH_DEPENDENCIES_ATTR, String.valueOf(false));

    telemetryUtils.includeRealOwnerId(attributes, owner.getId());

    telemetryData.setAttributes(attributes);
    telemetrySender.send(telemetryData);
  }

  private ApiSuggestedVersionChangeOptionDTO createSuggestedVersionChangeOption(ComponentIdentifier componentIdentifier,
                                                              ApiVersionChangeOptionType apiVersionChangeOptionType,
                                                              boolean isGolden)
  {
    ApiComponentDTOV2 componentDTOV2 = createComponentDtoFromIdentifier(componentIdentifier);
    componentDTOV2.breakingChangesCount = 0;
    return new ApiSuggestedVersionChangeOptionDTO(
        apiVersionChangeOptionType, isGolden, new ApiComponentChangeActionDTO(componentDTOV2));
  }

  public static ApiComponentDTOV2 createComponentDtoFromIdentifier(final ComponentIdentifier componentIdentifier) {
    ApiComponentDTOV2 componentDTOV2 = new ApiComponentDTOV2();
    componentDTOV2.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);
    componentDTOV2.packageUrl = PackageUrlIdentifier.toPackageUrl(componentIdentifier);
    ComponentDisplayName componentDisplayName =
        ComponentDisplayNameUtil.fromIdentifier(componentIdentifier);
    componentDTOV2.displayName = componentDisplayName != null ? componentDisplayName.toString() : null;
    componentDTOV2.proprietary = null; // not applicable
    return componentDTOV2;
  }

  private ApiVersionChangeOptionDTO createVersionChangeOption(ComponentIdentifier componentIdentifier,
                                                              ApiVersionChangeOptionType apiVersionChangeOptionType,
                                                              Integer breakingChangesCount)
  {
    ApiComponentDTOV2 componentDTOV2 = createComponentDtoFromIdentifier(componentIdentifier);
    componentDTOV2.breakingChangesCount = breakingChangesCount;
    return new ApiVersionChangeOptionDTO(apiVersionChangeOptionType, new ApiComponentChangeActionDTO(componentDTOV2));
  }

  private ApiVersionChangeOptionDTO createVersionChangeOptionForTransitiveComponent(
      final ApiComponentIdentifierDTOV2 transitiveComponent,
      final ComponentDetailsDTO dto,
      final ApiVersionChangeOptionType apiVersionChangeOptionType)
  {
    ApiComponentDTOV2 directApiComponentDTOV2 = new ApiComponentDTOV2();
    directApiComponentDTOV2.componentIdentifier =
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(dto.componentIdentifier);
    directApiComponentDTOV2.packageUrl = PackageUrlIdentifier.toPackageUrl(dto.componentIdentifier);
    ComponentDisplayName componentDisplayName =
        ComponentDisplayNameUtil.fromIdentifier(dto.componentIdentifier);
    directApiComponentDTOV2.displayName = componentDisplayName != null ? componentDisplayName.toString() : null;
    directApiComponentDTOV2.proprietary = null; // not applicable
    directApiComponentDTOV2.breakingChangesCount = dto.breakingChangesCount;
    ApiComponentChangeActionDTO parentAction = new ApiComponentChangeActionDTO(directApiComponentDTOV2);
    ApiVersionChangeOptionDTO versionChangeOption =
        createVersionChangeOption(transitiveComponent.toComponentIdentifier(),
            apiVersionChangeOptionType, dto.breakingChangesCount
        );
    versionChangeOption.setDirectDependency(false);
    versionChangeOption.getDirectDependencyData().add(parentAction);
    return versionChangeOption;
  }

  private ComponentDependenciesDTO getComponentDependencies(
      final Collection<PackageUrlIdentifier> componentIdentifiers)
  {
    return hdsClient.post(ComponentDependenciesDTO.class, "rest/component/dependencies", componentIdentifiers);
  }

  private int findCurrentIndex(List<ComponentDetailsDTO> allVersions, ComponentIdentifier componentIdentifier) {
    return IntStream.range(0, allVersions.size())
        .filter(i -> ensureCompleteIfNeeded(allVersions.get(i).componentIdentifier)
            .equals(ensureCompleteIfNeeded(componentIdentifier)))
        .findFirst()
        .orElse(-1);
  }

  private boolean shouldIncludeAdvancedStrategies(ComponentIdentifier componentIdentifier) {
    return componentIdentifier.isMaven() &&
        productLicense.hasFeature(LicensedFeature.ADVANCED_RECOMMENDATION_STRATEGIES) &&
        SystemConfigurationPropertyFeature.TRANSITIVE_SOLVER.isEnabled();
  }

  /**
   * Determines the appropriate remediation type for InnerSource components by comparing versions.
   * If the major version has changed, returns INNER_SOURCE_LATEST.
   * If only a minor or patch version has changed, returns INNER_SOURCE_LATEST_NON_BREAKING.
   */
  private ApiVersionChangeOptionType determineInnerSourceRemediationType(
      String currentVersion,
      String latestVersion,
      String format)
  {
    try {
      CompositeComparableVersion currentComparableVersion =
          createCompositeComparableVersion(currentVersion, format);
      CompositeComparableVersion latestComparableVersion = createCompositeComparableVersion(latestVersion, format);

      Boolean isMajorJump = currentComparableVersion.isMajorJump(latestComparableVersion);

      if (isMajorJump == null || isMajorJump) {
        return ApiVersionChangeOptionType.INNER_SOURCE_LATEST;
      }
      else {
        return ApiVersionChangeOptionType.INNER_SOURCE_LATEST_NON_BREAKING;
      }
    }
    catch (Exception e) {
      // In case of any parsing errors, default to considering it a major change
      log.warn("Error comparing versions {} and {}: {}", currentVersion, latestVersion, e.getMessage());
      return ApiVersionChangeOptionType.INNER_SOURCE_LATEST;
    }
  }

  private boolean isNewerVersion(String oldVersion, String newVersion, final String format) {
    CompositeComparableVersion oldComparableVersion = createCompositeComparableVersion(oldVersion, format);
    CompositeComparableVersion newComparableVersion = createCompositeComparableVersion(newVersion, format);

    return oldComparableVersion.compareTo(newComparableVersion) < 0;
  }
}
