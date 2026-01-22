/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.development.prioritization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.actions.ApiComponentChangeActionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.development.prioritization.DevelopmentPrioritizationComponentInfoDAO;
import com.sonatype.insight.brain.dataaccess.development.prioritization.DevelopmentPrioritizationDAO;
import com.sonatype.insight.brain.development.prioritization.dto.PrioritizationRemediationVersionDTO;
import com.sonatype.insight.brain.hds.ComponentDetailsDTO;
import com.sonatype.insight.brain.hds.ComponentDetailsLoaderFactory;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.hds.ComponentRemediationService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.prioritization.DevelopmentPrioritization;
import com.sonatype.insight.brain.model.prioritization.DevelopmentPrioritizationComponentInfo;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType.NEXT_NON_FAILING;
import static com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType.NEXT_NON_FAILING_WITH_DEPENDENCIES;
import static com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS;
import static com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DevelopmentPrioritizationRemediationServiceTest extends AbstractComponentTest
{
  private Application application;

  private DevelopmentPrioritizationRemediationService service;

  @Inject
  private ApplicationDAO applicationDAO;

  @Inject
  private DevelopmentPrioritizationComponentInfoDAO developmentPrioritizationComponentInfoDAO;

  @Inject
  private DevelopmentPrioritizationDAO developmentPrioritizationDAO;

  @Mock
  private ComponentInfoService mockComponentInfoService;

  @Mock
  private ComponentRemediationService mockComponentRemediationService;

  @Inject
  private ComponentDetailsLoaderFactory componentDetailsLoaderFactory;

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    application = tempEntity.newApplicationWithParent();
    service = new DevelopmentPrioritizationRemediationService(
        applicationDAO, componentDetailsLoaderFactory, mockComponentInfoService,
        mockComponentRemediationService, developmentPrioritizationComponentInfoDAO, developmentPrioritizationDAO);
  }

  @Test
  public void testFetchAndPersistRemediationRecommendations_insertsAllFoundRecommendations() {
    ComponentIdentifier componentIdentifiers1 = ComponentIdentifier.createMavenCoordinates("foo1", "bar1", "2.0.0");
    ComponentIdentifier componentIdentifiers2 = ComponentIdentifier.createMavenCoordinates("foo2", "bar2", "3.1.1");

    String scanId = "scan1";
    Map<ComponentIdentifier, PrioritizationRemediationVersionDTO> recommendationMap = ImmutableMap.of(
        componentIdentifiers1, new PrioritizationRemediationVersionDTO(
            "2.1.1", ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES),
        componentIdentifiers2, new PrioritizationRemediationVersionDTO(
            "3.2.2", ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS)
    );

    service.persistRemediationRecommendations(recommendationMap, scanId);

    DevelopmentPrioritization developmentPrioritizationList = developmentPrioritizationDAO.getByScanId(scanId);
    assertThat(developmentPrioritizationList)
        .isNotNull()
        .extracting(DevelopmentPrioritization::getScanId)
        .isEqualTo(scanId);

    List<DevelopmentPrioritizationComponentInfo> developmentPrioritizationComponentInfoList =
        developmentPrioritizationComponentInfoDAO.getAllByScanId(scanId);
    assertThat(developmentPrioritizationComponentInfoList)
        .hasSize(2)
        .extracting(DevelopmentPrioritizationComponentInfo::getScanId,
            DevelopmentPrioritizationComponentInfo::getComponentHash,
            DevelopmentPrioritizationComponentInfo::getRemediationType,
            DevelopmentPrioritizationComponentInfo::getRemediationVersion)
        .containsExactlyInAnyOrder(
            tuple(scanId, componentIdentifiers1.toSyntheticHash(),
                ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES, "2.1.1"),
            tuple(scanId, componentIdentifiers2.toSyntheticHash(),
                ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS, "3.2.2"));
  }

  @Test
  public void testGetRemediationVersions_noDetails() {
    Map<String, String> coordinates = Map.of("artifactId", "Artifact1", "groupId", "Group1", "version", "1.2.3");
    ComponentIdentifier componentIdentifier = new ComponentIdentifier("maven", coordinates);

    List<ComponentIdentifier> componentIdentifiers = Collections.singletonList(componentIdentifier);

    when(mockComponentInfoService.getComponentDetailsForAllVersionsNoAuthBulk(
            any(), eq(componentIdentifiers), eq("myStage"), eq("scanId"), any(), anyBoolean()))
            .thenReturn(Collections.emptyMap());

    Map<ComponentIdentifier, PrioritizationRemediationVersionDTO> remediationList =
            service.getRemediationVersions(componentIdentifiers, application.getId(), "myStage", "scanId");

    assertThat(remediationList).isEmpty();
  }

  @Test
  public void testGetRemediationVersions_remediationNotFound() {
    Map<String, String> coordinates = Map.of("artifactId", "Artifact1", "groupId", "Group1", "version", "1.2.3");
    ComponentIdentifier componentIdentifier = new ComponentIdentifier("maven", coordinates);

    ComponentDetailsDTO componentDetailsDTO = new ComponentDetailsDTO();
    componentDetailsDTO.componentIdentifier = componentIdentifier;
    componentDetailsDTO.violatedPolicyCount = 0;
    List<ComponentDetailsDTO> componentDetailsDTOs = Collections.singletonList(componentDetailsDTO);

    List<ComponentIdentifier> componentIdentifiers = Collections.singletonList(componentIdentifier);

    when(mockComponentInfoService.getComponentDetailsForAllVersionsNoAuthBulk(
            any(), eq(componentIdentifiers), eq("myStage"), eq("scanId"), any(), anyBoolean()))
            .thenReturn(Collections.singletonMap(
                componentIdentifier.createAlternativeVersion(null), componentDetailsDTOs));

    when(mockComponentRemediationService.getSuggestedSelectedRemediation(
            eq(componentIdentifier), eq(componentDetailsDTOs),
            any(), any(), any(), anyBoolean())).thenReturn(null);

    Map<ComponentIdentifier, PrioritizationRemediationVersionDTO> remediationList =
            service.getRemediationVersions(componentIdentifiers, application.getId(), "myStage", "scanId");

    assertThat(remediationList).isEmpty();
  }

  @Test
  public void testGetRemediationVersions_remediationFound() {
    Map<String, String> coordinates = Map.of("artifactId", "Artifact1", "groupId", "Group1", "version", "1.2.3");
    ComponentIdentifier componentIdentifier = new ComponentIdentifier("maven", coordinates);

    ComponentDetailsDTO componentDetailsDTO = new ComponentDetailsDTO();
    componentDetailsDTO.componentIdentifier = componentIdentifier;
    componentDetailsDTO.violatedPolicyCount = 0;
    List<ComponentDetailsDTO> componentDetailsDTOs = Collections.singletonList(componentDetailsDTO);

    List<ComponentIdentifier> componentIdentifiers = Collections.singletonList(componentIdentifier);

    when(mockComponentInfoService.getComponentDetailsForAllVersionsNoAuthBulk(
            any(), eq(componentIdentifiers), eq("myStage"), eq("scanId"), any(), anyBoolean()))
            .thenReturn(Collections.singletonMap(componentIdentifier.createAlternativeVersion(null),
                componentDetailsDTOs));

    componentRemediationServiceSetup(componentIdentifier, componentDetailsDTOs);

    Map<ComponentIdentifier, PrioritizationRemediationVersionDTO> remediationList =
            service.getRemediationVersions(componentIdentifiers, application.getId(), "myStage", "scanId");

    assertThat(remediationList).hasSize(1);
    PrioritizationRemediationVersionDTO prioritizationRemediationVersionDTO = remediationList.get(componentIdentifier);
    assertThat(prioritizationRemediationVersionDTO).isNotNull();
    assertThat(prioritizationRemediationVersionDTO.getVersion()).isEqualTo("1.2.3");
    assertThat(prioritizationRemediationVersionDTO.getRemediationType())
            .isEqualTo(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS);
  }

  @Test
  public void testGetRemediationVersions_multipleRemediationFound() {
    Map<String, String> coordinates = Map.of("artifactId", "Artifact1", "groupId", "Group1", "version", "1.2.1");
    ComponentIdentifier componentIdentifier = new ComponentIdentifier("maven", coordinates);

    ComponentDetailsDTO componentDetailsDTO = new ComponentDetailsDTO();
    componentDetailsDTO.componentIdentifier = componentIdentifier;
    componentDetailsDTO.violatedPolicyCount = 0;

    Map<String, String> coordinates1 = Map.of("artifactId", "Artifact2", "groupId", "Group2", "version", "1.2.2");
    ComponentIdentifier componentIdentifier1 = new ComponentIdentifier("maven", coordinates1);

    ComponentDetailsDTO componentDetailsDTO1 = new ComponentDetailsDTO();
    componentDetailsDTO1.componentIdentifier = componentIdentifier1;
    componentDetailsDTO1.violatedPolicyCount = 0;

    Map<String, String> coordinates2 = Map.of("artifactId", "Artifact3", "groupId", "Group3", "version", "1.2.3");
    ComponentIdentifier componentIdentifier2a = new ComponentIdentifier("maven", coordinates2);
    ComponentIdentifier componentIdentifier2b = componentIdentifier2a.createAlternativeVersion("1.2.4");

    ComponentDetailsDTO componentDetailsDTO2a = new ComponentDetailsDTO();
    componentDetailsDTO2a.componentIdentifier = componentIdentifier2a;
    componentDetailsDTO2a.violatedPolicyCount = 0;

    ComponentDetailsDTO componentDetailsDTO2b = new ComponentDetailsDTO();
    componentDetailsDTO2b.componentIdentifier = componentIdentifier2b;
    componentDetailsDTO2b.violatedPolicyCount = 0;

    List<ComponentDetailsDTO> componentDetailsDTOs = Collections.singletonList(componentDetailsDTO);
    List<ComponentDetailsDTO> componentDetailsDTOs1 = Collections.singletonList(componentDetailsDTO1);

    // Two possible upgrades for test "2". This is used to ensure we don't choose an incorrect version,
    // and also that we don't make recommendations for versions that we do not need recommendations for.
    List<ComponentDetailsDTO> componentDetailsDTOs2 = new ArrayList<>();
    componentDetailsDTOs2.add(componentDetailsDTO2a);
    componentDetailsDTOs2.add(componentDetailsDTO2b);

    List<ComponentIdentifier> componentIdentifiers =
        Arrays.asList(componentIdentifier, componentIdentifier1, componentIdentifier2a);

    when(mockComponentInfoService.getComponentDetailsForAllVersionsNoAuthBulk(
        any(), eq(componentIdentifiers), eq("myStage"), eq("scanId"), any(), anyBoolean()))
        .thenReturn(ImmutableMap.of(
            componentIdentifier.createAlternativeVersion(null), componentDetailsDTOs,
            componentIdentifier1.createAlternativeVersion(null), componentDetailsDTOs1,
            componentIdentifier2a.createAlternativeVersion(null), componentDetailsDTOs2));

    componentRemediationServiceSetup(componentIdentifier, componentDetailsDTOs);
    componentRemediationServiceSetup(componentIdentifier1, componentDetailsDTOs1);
    componentRemediationServiceSetup(componentIdentifier2a, componentDetailsDTOs2);

    Map<ComponentIdentifier, PrioritizationRemediationVersionDTO> remediationList =
        service.getRemediationVersions(componentIdentifiers, application.getId(), "myStage", "scanId");

    assertThat(remediationList).hasSize(3);
    PrioritizationRemediationVersionDTO prioritizationRemediationVersionDTO = remediationList.get(componentIdentifier);
    assertThat(prioritizationRemediationVersionDTO).isNotNull();
    assertThat(prioritizationRemediationVersionDTO.getVersion()).isEqualTo("1.2.1");
    assertThat(prioritizationRemediationVersionDTO.getRemediationType())
            .isEqualTo(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS);

    prioritizationRemediationVersionDTO = remediationList.get(componentIdentifier1);
    assertThat(prioritizationRemediationVersionDTO).isNotNull();
    assertThat(prioritizationRemediationVersionDTO.getVersion()).isEqualTo("1.2.2");
    assertThat(prioritizationRemediationVersionDTO.getRemediationType())
            .isEqualTo(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS);

    prioritizationRemediationVersionDTO = remediationList.get(componentIdentifier2a);
    assertThat(prioritizationRemediationVersionDTO).isNotNull();
    assertThat(prioritizationRemediationVersionDTO.getVersion()).isEqualTo("1.2.3");
    assertThat(prioritizationRemediationVersionDTO.getRemediationType())
            .isEqualTo(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS);
  }

  @Test
  public void getRecommendedVersionChange_noResults() {
    Optional<ApiVersionChangeOptionDTO> recommendedVersionChange =
        service.getRecommendedVersionChange(Collections.emptyList());
    assertThat(recommendedVersionChange).isEmpty();
  }

  @Test
  public void getRecommendedVersionChange_firstNextNoViolationsWithDependencies() {
    ApiVersionChangeOptionDTO apiVersionChangeOptionDTO =
        new ApiVersionChangeOptionDTO(NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES, null);
    ApiVersionChangeOptionDTO apiVersionChangeOptionDTO1 =
        new ApiVersionChangeOptionDTO(NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES, null);
    ApiVersionChangeOptionDTO apiVersionChangeOptionDTO2 =
        new ApiVersionChangeOptionDTO(NEXT_NO_VIOLATIONS, null);

    Optional<ApiVersionChangeOptionDTO> recommendedVersionChange =
        service.getRecommendedVersionChange(asList(
            apiVersionChangeOptionDTO, apiVersionChangeOptionDTO1, apiVersionChangeOptionDTO2));

    assertThat(recommendedVersionChange).isNotEmpty();
    assertThat(recommendedVersionChange.get()).isEqualTo(apiVersionChangeOptionDTO);
  }

  @Test
  public void getRecommendedVersionChange_firstNextNoViolations() {
    ApiVersionChangeOptionDTO apiVersionChangeOptionDTO =
        new ApiVersionChangeOptionDTO(NEXT_NO_VIOLATIONS, null);
    ApiVersionChangeOptionDTO apiVersionChangeOptionDTO1 =
        new ApiVersionChangeOptionDTO(NEXT_NO_VIOLATIONS, null);
    ApiVersionChangeOptionDTO apiVersionChangeOptionDTO2 =
        new ApiVersionChangeOptionDTO(NEXT_NON_FAILING_WITH_DEPENDENCIES, null);

    Optional<ApiVersionChangeOptionDTO> recommendedVersionChange =
        service.getRecommendedVersionChange(asList(
            apiVersionChangeOptionDTO, apiVersionChangeOptionDTO1, apiVersionChangeOptionDTO2));

    assertThat(recommendedVersionChange).isNotEmpty();
    assertThat(recommendedVersionChange.get()).isEqualTo(apiVersionChangeOptionDTO);
  }

  @Test
  public void getRecommendedVersionChange_firstNextNoFailingWithDependencies() {
    ApiVersionChangeOptionDTO apiVersionChangeOptionDTO =
        new ApiVersionChangeOptionDTO(NEXT_NON_FAILING_WITH_DEPENDENCIES, null);
    ApiVersionChangeOptionDTO apiVersionChangeOptionDTO1 =
        new ApiVersionChangeOptionDTO(NEXT_NON_FAILING_WITH_DEPENDENCIES, null);
    ApiVersionChangeOptionDTO apiVersionChangeOptionDTO2 =
        new ApiVersionChangeOptionDTO(NEXT_NON_FAILING, null);

    Optional<ApiVersionChangeOptionDTO> recommendedVersionChange =
        service.getRecommendedVersionChange(asList(
            apiVersionChangeOptionDTO, apiVersionChangeOptionDTO1, apiVersionChangeOptionDTO2));

    assertThat(recommendedVersionChange).isNotEmpty();
    assertThat(recommendedVersionChange.get()).isEqualTo(apiVersionChangeOptionDTO);
  }

  @Test
  public void getRecommendedVersionChange_firstNextNoFailing() {
    ApiVersionChangeOptionDTO apiVersionChangeOptionDTO =
        new ApiVersionChangeOptionDTO(NEXT_NON_FAILING, null);
    ApiVersionChangeOptionDTO apiVersionChangeOptionDTO1 =
        new ApiVersionChangeOptionDTO(NEXT_NON_FAILING, null);

    Optional<ApiVersionChangeOptionDTO> recommendedVersionChange =
        service.getRecommendedVersionChange(asList(
            apiVersionChangeOptionDTO, apiVersionChangeOptionDTO1));

    assertThat(recommendedVersionChange).isNotEmpty();
    assertThat(recommendedVersionChange.get()).isEqualTo(apiVersionChangeOptionDTO);
  }

  private void componentRemediationServiceSetup(
      ComponentIdentifier componentIdentifier, List<ComponentDetailsDTO> componentDetailsDTOList)
  {
    ApiComponentRemediationValueDTO remediationValueDto = new ApiComponentRemediationValueDTO();
    remediationValueDto.versionChanges = new LinkedList<>();
    if (componentIdentifier != null) {
      remediationValueDto.versionChanges
              .add(getApiVersionChangeOptionDTO(componentIdentifier));
    }
    when(mockComponentRemediationService.getSuggestedSelectedRemediation(
        eq(componentIdentifier), eq(componentDetailsDTOList),
        any(), any(), any(), anyBoolean())).thenReturn(remediationValueDto);
  }

  private ApiVersionChangeOptionDTO getApiVersionChangeOptionDTO(ComponentIdentifier componentIdentifier) {
    ApiVersionChangeOptionDTO versionChangeOptionDTO = new ApiVersionChangeOptionDTO();
    ApiComponentDTOV2 componentDTOV2 = new ApiComponentDTOV2();
    componentDTOV2.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);
    ApiComponentChangeActionDTO data = new ApiComponentChangeActionDTO(componentDTOV2);
    versionChangeOptionDTO.setType(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS);
    versionChangeOptionDTO.setData(data);

    return versionChangeOptionDTO;
  }
}
