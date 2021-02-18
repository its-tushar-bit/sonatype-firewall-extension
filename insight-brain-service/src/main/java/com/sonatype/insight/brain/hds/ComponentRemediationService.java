/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.actions.ApiComponentChangeActionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.DependencyType;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.policy.evaluator.ComponentPolicyEvaluator;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.apache.commons.collections.CollectionUtils;

/**
 * @since 1.83
 * This code was formerly in ApiComponentRemediationService but was split out to avoid a circular dependency
 */
@Named
public class ComponentRemediationService
{
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

  private final ComponentDetailsLoaderFactory componentDetailsLoaderFactory;

  private final ProductLicense productLicense;

  @Inject
  public ComponentRemediationService(
      TelemetrySender telemetrySender,
      HdsClient hdsClient,
      ComponentPolicyEvaluator componentPolicyEvaluator,
      ComponentDetailsLoaderFactory componentDetailsLoaderFactory,
      ProductLicense productLicense)
  {
    this.telemetrySender = telemetrySender;
    this.hdsClient = hdsClient;
    this.componentPolicyEvaluator = componentPolicyEvaluator;
    this.componentDetailsLoaderFactory = componentDetailsLoaderFactory;
    this.productLicense = productLicense;
  }

  public ApiComponentRemediationValueDTO getSuggestedRemediation(
      final ComponentIdentifier currentComponent,
      final List<ComponentDetailsDTO> allVersions,
      final OwnerType ownerType,
      final String ownerId,
      final String stageId)
  {
    if (ownerType == OwnerType.REPOSITORY || ownerType == OwnerType.REPOSITORY_CONTAINER) {
      return null;
    }

    ApiComponentRemediationValueDTO componentRemediationDto = new ApiComponentRemediationValueDTO();

    int currentIndex = IntStream.range(0, allVersions.size())
        .filter(i -> allVersions.get(i).componentIdentifier.equals(currentComponent))
        .findFirst()
        .orElse(-1);

    Map<String, Object> telemetryAttributes = new HashMap<>();

    if (currentIndex >= 0) { // only process if we find a current version of the component
      // find non-violating and non-failing versions
      List<ComponentIdentifier> nonViolatingVersions = nonViolatingVersions(currentIndex, allVersions);

      // find first non violating version
      nonViolatingVersions.stream()
          .findFirst()
          .ifPresent(identifier -> {
            componentRemediationDto.versionChanges.add(
                createVersionChangeOption(identifier, ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS));
            telemetryAttributes.put(OPTION_NEXT_NO_VIOLATIONS_ATTR, String.valueOf(true));
          });

      // nonFailingVersions will be empty if stage is not specified
      // and we won't add non-failing/non-failing with dependencies remedies.
      List<ComponentIdentifier> nonFailingVersions = (stageId == null) ?
          Collections.emptyList() :
          nonFailingVersions(currentIndex, allVersions);

      // find first non failing version
      nonFailingVersions.stream()
          .findFirst()
          .ifPresent(identifier -> {
            componentRemediationDto.versionChanges.add(
                createVersionChangeOption(identifier, ApiVersionChangeOptionType.NEXT_NON_FAILING));
            telemetryAttributes.put(OPTION_NEXT_NON_FAILING_ATTR, String.valueOf(true));
          });

      boolean includeAdvancedStrategies = currentComponent.isMaven() &&
          productLicense.hasFeature(LicensedFeature.ADVANCED_RECOMMENDATION_STRATEGIES);
      if (includeAdvancedStrategies) {
        // non-violating/non-failing with dependencies
        Collection<PackageUrlIdentifier> nonFailingVersionsPurls = nonFailingVersions.stream()
            .map(PackageUrlIdentifier::fromComponentIdentifier)
            .collect(Collectors.toList());

        Collection<PackageUrlIdentifier> nonViolatingVersionsPurls = nonViolatingVersions.stream()
            .map(PackageUrlIdentifier::fromComponentIdentifier)
            .collect(Collectors.toList());

        // create collection of purls of all non violating, non failing versions
        // since nonFailingVersions is a super set which includes nonViolatingVersions, use that if calculated
        Collection<PackageUrlIdentifier> candidatePurls = CollectionUtils.isNotEmpty(nonFailingVersionsPurls) ?
            nonFailingVersionsPurls :
            nonViolatingVersionsPurls;

        Map<PackageUrlIdentifier, List<PolicyAlert>> dependencyAlerts = getDependencyAlerts(candidatePurls,
            ownerType, ownerId, stageId);

        // find first non violating where dependencies have no violations
        nonViolatingWithDependencies(nonViolatingVersionsPurls, dependencyAlerts)
            .ifPresent(identifier -> {
              componentRemediationDto.versionChanges.add(
                  createVersionChangeOption(identifier,
                      ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES)
              );
              telemetryAttributes.put(OPTION_NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES_ATTR, String.valueOf(true));
            });

        // find first non failing where dependencies have no violations
        nonFailingWithDependencies(nonFailingVersionsPurls, dependencyAlerts)
            .ifPresent(identifier -> {
              componentRemediationDto.versionChanges.add(
                  createVersionChangeOption(identifier, ApiVersionChangeOptionType.NEXT_NON_FAILING_WITH_DEPENDENCIES)
              );
              telemetryAttributes.put(OPTION_NEXT_NON_FAILING_WITH_DEPENDENCIES_ATTR, String.valueOf(true));
            });
      }
    }

    sendTelemetry(ownerType, ownerId, currentComponent, telemetryAttributes);
    return componentRemediationDto;
  }

  /**
   * evaluates dependencies and returns a map of each version's component identifier to its dependencies policy alerts
   */
  private Map<PackageUrlIdentifier, List<PolicyAlert>> getDependencyAlerts(
      Collection<PackageUrlIdentifier> candidatePurls,
      final OwnerType ownerType,
      final String ownerId,
      final String stageId)
  {
    // get dependencies of all non violating, non failing versions
    ComponentDependenciesDTO dependenciesDto = getComponentDependencies(candidatePurls);

    Map<PackageUrlIdentifier, List<PolicyAlert>> dependencyAlerts = new HashMap<>();
    final Owner owner = IdUtils.getOwnerNotNull(ownerType, ownerId);
    final Collection<ComponentDetails> componentDetailsList = dependenciesDto.getDetailsMap().values();
    final Stage stage = new Stage(stageId != null ? stageId : BuildStageType.ID);

    // evaluate flattened dependencies
    // Fix match state to exact as there's no point propagating it to other versions.
    // Assume the dependencies are only transitive
    List<Component> components = componentDetailsLoaderFactory.newInstance(owner)
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

  public Map<PackageUrlIdentifier, List<PolicyAlert>> evaluateAndGetPolicyAlertsByComponent(
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

  private List<ComponentIdentifier> nonViolatingVersions(int startingIndex, List<ComponentDetailsDTO> dtos) {
    return dtos.stream()
        .skip(startingIndex)
        .filter(dto -> dto.violatedPolicyCount == 0)
        .map(dto -> dto.componentIdentifier)
        .collect(Collectors.toList());
  }

  private List<ComponentIdentifier> nonFailingVersions(int startingIndex, List<ComponentDetailsDTO> dtos) {
    return dtos.stream()
        .skip(startingIndex)
        .filter(dto -> !hasFailAction(dto.policyAlerts))
        .map(dto -> dto.componentIdentifier)
        .collect(Collectors.toList());
  }

  private Optional<ComponentIdentifier> nonViolatingWithDependencies(
      Collection<PackageUrlIdentifier> nonViolatingVersions,
      Map<PackageUrlIdentifier, List<PolicyAlert>> dependencyAlerts)
  {
    for (PackageUrlIdentifier versionPurl : nonViolatingVersions) {
      if (CollectionUtils.isEmpty(dependencyAlerts.get(versionPurl)))
      {
        return Optional.of(versionPurl.toComponentIdentifier());
      }
    }
    return Optional.empty();
  }

  private Optional<ComponentIdentifier> nonFailingWithDependencies(
      Collection<PackageUrlIdentifier> nonFailingVersions,
      Map<PackageUrlIdentifier, List<PolicyAlert>> dependencyAlerts)
  {
    for (PackageUrlIdentifier versionPurl : nonFailingVersions) {
      List<PolicyAlert> policyAlerts = dependencyAlerts.get(versionPurl);
      if (policyAlerts == null || !hasFailAction(policyAlerts))
      {
        return Optional.of(versionPurl.toComponentIdentifier());
      }
    }
    return Optional.empty();
  }

  private boolean hasFailAction(List<PolicyAlert> alerts) {
    return alerts.stream().anyMatch(
        alert -> Optional.ofNullable(alert.getActions()).map(Collection::stream).orElseGet(Stream::empty)
            .anyMatch(action -> Action.ID_FAIL.equals(action.getActionTypeId())));
  }

  private void sendTelemetry(final OwnerType ownerType,
                             final String ownerId,
                             final ComponentIdentifier componentIdentifier,
                             final Map<String, Object> attributes)
  {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.COMPONENT_REMEDIATION);
    attributes.put(COMPONENT_ATTR, HdsClientAnalytics.obfuscate(JsonUtils.writeUnformatted(componentIdentifier)));
    attributes.put(OWNER_TYPE_ATTR, ownerType.toString());
    attributes.put(OWNER_ID_ATTR, HdsClientAnalytics.obfuscate(ownerId));
    attributes.putIfAbsent(OPTION_NEXT_NO_VIOLATIONS_ATTR, String.valueOf(false));
    attributes.putIfAbsent(OPTION_NEXT_NON_FAILING_ATTR, String.valueOf(false));
    attributes.putIfAbsent(OPTION_NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES_ATTR, String.valueOf(false));
    attributes.putIfAbsent(OPTION_NEXT_NON_FAILING_WITH_DEPENDENCIES_ATTR, String.valueOf(false));
    telemetryData.setAttributes(attributes);
    telemetrySender.send(telemetryData);
  }

  private ApiVersionChangeOptionDTO createVersionChangeOption(ComponentIdentifier componentIdentifier,
                                                              ApiVersionChangeOptionType apiVersionChangeOptionType)
  {
    ApiComponentDTOV2 componentDTOV2 = new ApiComponentDTOV2();
    componentDTOV2.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);
    componentDTOV2.packageUrl = PackageUrlIdentifier.toPackageUrl(componentIdentifier);
    ComponentDisplayName componentDisplayName =
        ComponentDisplayNameUtil.fromIdentifier(componentIdentifier);
    componentDTOV2.displayName = componentDisplayName != null ? componentDisplayName.toString() : null;
    componentDTOV2.proprietary = null; // not applicable
    return new ApiVersionChangeOptionDTO(apiVersionChangeOptionType, new ApiComponentChangeActionDTO(componentDTOV2));
  }

  private ComponentDependenciesDTO getComponentDependencies(
      final Collection<PackageUrlIdentifier> componentIdentifiers)
  {
    return hdsClient.post(ComponentDependenciesDTO.class, "rest/component/dependencies", componentIdentifiers);
  }
}
