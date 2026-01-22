/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.development.prioritization;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.development.prioritization.dto.PrioritizationRemediationVersionDTO;
import com.sonatype.insight.brain.features.FeaturesService;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.license.model.Feature;
import com.sonatype.insight.license.model.LicensedFeature;

@Named
@Singleton
public class DevelopmentPrioritiesUtilsService
{
  private final FeaturesService featuresService;

  @Inject
  public DevelopmentPrioritiesUtilsService(FeaturesService featuresService) {
    this.featuresService = featuresService;
  }

  public boolean arePrioritiesFeaturesEnabled() {
    Set<Feature> features = featuresService.getFeatures();
    final boolean isPrioritizedFindingsEnabled = features
        .contains(SystemConfigurationPropertyFeature.PRIORITIZED_FINDINGS_REPORT);
    final boolean isDeveloperDashboardEnabled = features
        .contains(LicensedFeature.DEVELOPER_DASHBOARD);

    return isPrioritizedFindingsEnabled && isDeveloperDashboardEnabled;
  }

  public PrioritizationRemediationVersionDTO getPrioritizationRemediation(
      ApiComponentRemediationValueDTO remediation,
      String currentVersion)
  {
    if (remediation == null) {
      return null;
    }

    List<ApiVersionChangeOptionDTO> filteredVersions = remediation.versionChanges.stream().filter(
        versionChange -> !versionChange.getData().getComponent().componentIdentifier.getCoordinates()
            .get(ComponentIdentifier.VERSION).equals(currentVersion)
    ).toList();

    if (remediation.suggestedVersionChange != null && remediation.suggestedVersionChange.getData() != null) {
      String version =
          getVersionFromComponent(remediation.suggestedVersionChange.getData().getComponent());
      return new PrioritizationRemediationVersionDTO(version, remediation.suggestedVersionChange.getType());
    }

    return Stream.of(
        extractRemediationDto(filteredVersions, ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES),
        extractRemediationDto(filteredVersions, ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS),
        extractRemediationDto(filteredVersions, ApiVersionChangeOptionType.NEXT_NON_FAILING_WITH_DEPENDENCIES),
        extractRemediationDto(filteredVersions, ApiVersionChangeOptionType.NEXT_NON_FAILING)
    ).map(Supplier::get).filter(Objects::nonNull).findFirst().orElse(null);
  }

  private Supplier<PrioritizationRemediationVersionDTO> extractRemediationDto(
      List<ApiVersionChangeOptionDTO> filteredVersions,
      ApiVersionChangeOptionType type)
  {

    return () -> {
      ApiVersionChangeOptionDTO optionByType = findVersionByType(filteredVersions,
          type);
      if (optionByType != null && optionByType.getData() != null) {
        String version = getVersionFromComponent(optionByType.getData().getComponent());
        return new PrioritizationRemediationVersionDTO(version, optionByType.getType());
      }

      return null;
    };
  }

  public String getVersionFromComponent(ApiComponentDTOV2 component) {
    if (component != null &&
        component.componentIdentifier != null &&
        component.componentIdentifier.getCoordinates() != null) {
      return component.componentIdentifier.toComponentIdentifier().get(ComponentIdentifier.VERSION);
    }
    return null;
  }

  public ApiVersionChangeOptionDTO findVersionByType(List<ApiVersionChangeOptionDTO> versions,
                                                     ApiVersionChangeOptionType type)
  {
    return versions.stream().filter(version -> version.getType().equals(type)).findFirst().orElse(null);
  }
}
