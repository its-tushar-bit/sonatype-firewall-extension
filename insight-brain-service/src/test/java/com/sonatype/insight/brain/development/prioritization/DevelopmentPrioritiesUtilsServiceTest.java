/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.development.prioritization;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.actions.ApiComponentChangeActionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiSuggestedVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.development.prioritization.dto.PrioritizationRemediationVersionDTO;
import com.sonatype.insight.brain.features.FeaturesService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.google.common.collect.Sets.newHashSet;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature.PRIORITIZED_FINDINGS_REPORT;
import static com.sonatype.insight.license.model.LicensedFeature.DEVELOPER_DASHBOARD;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DevelopmentPrioritiesUtilsServiceTest
{
  @Mock
  private FeaturesService featuresService;

  private DevelopmentPrioritiesUtilsService developmentPrioritiesUtilsService;

  @BeforeEach
  public void setup() {
    developmentPrioritiesUtilsService = new DevelopmentPrioritiesUtilsService(featuresService);
  }

  @Test
  public void noFeaturesAvailable() {
    assertThat(developmentPrioritiesUtilsService.arePrioritiesFeaturesEnabled()).isFalse();
  }

  @Test
  public void onlyPrioritizedFindingsEnabled() {
    when(featuresService.getFeatures())
        .thenReturn(newHashSet(PRIORITIZED_FINDINGS_REPORT));
    assertThat(developmentPrioritiesUtilsService.arePrioritiesFeaturesEnabled()).isFalse();
  }

  @Test
  public void onlyDeveloperDashboardEnabled() {
    when(featuresService.getFeatures())
        .thenReturn(newHashSet(DEVELOPER_DASHBOARD));
    assertThat(developmentPrioritiesUtilsService.arePrioritiesFeaturesEnabled()).isFalse();
  }

  @Test
  public void bothFeaturesEnabled() {
    when(featuresService.getFeatures())
        .thenReturn(newHashSet(DEVELOPER_DASHBOARD, PRIORITIZED_FINDINGS_REPORT));
    assertThat(developmentPrioritiesUtilsService.arePrioritiesFeaturesEnabled()).isTrue();
  }

  @Test
  public void testGetPrioritizationRemediation_nullRemediation() {
    assertThat(developmentPrioritiesUtilsService.getPrioritizationRemediation(null, "1.0.0")).isNull();
  }

  @Test
  public void testGetPrioritizationRemediation_suggestedVersionIsPreferred() {
    final ApiComponentRemediationValueDTO remediation = new ApiComponentRemediationValueDTO();
    final ApiSuggestedVersionChangeOptionDTO suggestedVersion =
        new ApiSuggestedVersionChangeOptionDTO();
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1",
        "a1", "v1");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g1",
        "a1", "v2");
    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createMavenCoordinates("g1",
        "a1", "v3");
    remediation.versionChanges = List.of(
        createVersionChangeOption(ApiVersionChangeOptionType.NEXT_NON_FAILING_WITH_DEPENDENCIES,
            componentIdentifier1),
        createVersionChangeOption(ApiVersionChangeOptionType.NEXT_NON_FAILING, componentIdentifier2),
        createVersionChangeOption(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES,
            componentIdentifier3));

    ComponentIdentifier componentIdentifier5 = ComponentIdentifier.createMavenCoordinates("g4",
        "a4", "v4");
    suggestedVersion.setData(createChangeAction(componentIdentifier5));
    suggestedVersion.setType(ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING);
    remediation.suggestedVersionChange = suggestedVersion;

    assertThat(developmentPrioritiesUtilsService.getPrioritizationRemediation(remediation, "1.0" +
        ".0")).isEqualTo(new PrioritizationRemediationVersionDTO(
            "v4", ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING));

    suggestedVersion.setType(ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES);
    remediation.suggestedVersionChange = suggestedVersion;

    assertThat(developmentPrioritiesUtilsService.getPrioritizationRemediation(remediation, "1.0" +
        ".0")).isEqualTo(new PrioritizationRemediationVersionDTO("v4",
            ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES));
  }

  @Test
  public void testGetPrioritizationRemediation_nextNoViolationWithDependenciesIsPreferred() {
    final ApiComponentRemediationValueDTO remediation = new ApiComponentRemediationValueDTO();
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1",
        "a1", "v1");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g1",
        "a1", "v2");
    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createMavenCoordinates("g1",
        "a1", "v3");
    remediation.versionChanges = List.of(
        createVersionChangeOption(ApiVersionChangeOptionType.NEXT_NON_FAILING_WITH_DEPENDENCIES,
            componentIdentifier1),
        createVersionChangeOption(ApiVersionChangeOptionType.NEXT_NON_FAILING, componentIdentifier2),
        createVersionChangeOption(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES,
            componentIdentifier3));

    assertThat(developmentPrioritiesUtilsService.getPrioritizationRemediation(remediation, "1.0" +
        ".0")).isEqualTo(new PrioritizationRemediationVersionDTO("v3",
            ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES));
  }

  @Test
  public void testGetPrioritizationRemediation_nextNoViolationsIsPreferred() {
    final ApiComponentRemediationValueDTO remediation = new ApiComponentRemediationValueDTO();
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1",
        "a1", "v1");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g1",
        "a1", "v2");
    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createMavenCoordinates("g1",
        "a1", "v3");
    remediation.versionChanges = List.of(
        createVersionChangeOption(ApiVersionChangeOptionType.NEXT_NON_FAILING_WITH_DEPENDENCIES,
            componentIdentifier1),
        createVersionChangeOption(ApiVersionChangeOptionType.NEXT_NON_FAILING, componentIdentifier2),
        createVersionChangeOption(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS,
            componentIdentifier3));

    assertThat(developmentPrioritiesUtilsService.getPrioritizationRemediation(remediation, "1.0" +
        ".0")).isEqualTo(new PrioritizationRemediationVersionDTO("v3",
            ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS));
  }

  @Test
  public void testGetPrioritizationRemediation_nextNonFailingWithDependenciesIsPreferred() {
    final ApiComponentRemediationValueDTO remediation = new ApiComponentRemediationValueDTO();
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1",
        "a1", "v1");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g1",
        "a1", "v2");
    remediation.versionChanges = List.of(
        createVersionChangeOption(ApiVersionChangeOptionType.NEXT_NON_FAILING_WITH_DEPENDENCIES,
            componentIdentifier1),
        createVersionChangeOption(ApiVersionChangeOptionType.NEXT_NON_FAILING, componentIdentifier2));

    assertThat(developmentPrioritiesUtilsService.getPrioritizationRemediation(remediation, "1.0" +
        ".0")).isEqualTo(new PrioritizationRemediationVersionDTO("v1",
            ApiVersionChangeOptionType.NEXT_NON_FAILING_WITH_DEPENDENCIES));
  }

  @Test
  public void testGetPrioritizationRemediation_nextNonFailingIsPreferred() {
    final ApiComponentRemediationValueDTO remediation = new ApiComponentRemediationValueDTO();

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1",
        "a1", "v1");

    remediation.versionChanges = List.of(
        createVersionChangeOption(ApiVersionChangeOptionType.NEXT_NON_FAILING, componentIdentifier));

    assertThat(developmentPrioritiesUtilsService.getPrioritizationRemediation(remediation, "1.0" +
        ".0")).isEqualTo(new PrioritizationRemediationVersionDTO("v1",
            ApiVersionChangeOptionType.NEXT_NON_FAILING));
  }

  private ApiVersionChangeOptionDTO createVersionChangeOption(
      final ApiVersionChangeOptionType type,
      final ComponentIdentifier componentIdentifier)
  {
    final ApiVersionChangeOptionDTO versionChangeOption = new ApiVersionChangeOptionDTO();
    versionChangeOption.setType(type);
    versionChangeOption.setData(createChangeAction(componentIdentifier));
    return versionChangeOption;
  }

  private ApiComponentChangeActionDTO createChangeAction(ComponentIdentifier componentIdentifier) {
    final ApiComponentChangeActionDTO changeAction = new ApiComponentChangeActionDTO();
    changeAction.setComponent(createComponent(componentIdentifier));
    return changeAction;
  }

  private ApiComponentDTOV2 createComponent(ComponentIdentifier componentIdentifier) {
    ApiComponentDTOV2 component = new ApiComponentDTOV2();

    component.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);
    return component;
  }
}
