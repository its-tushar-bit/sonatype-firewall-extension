/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.actions.ApiComponentChangeActionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.hds.ComponentDetailsDTO;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.hds.ComponentRemediationService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PullRequestCommentingRemediationServiceTest
    extends AbstractComponentTest
{
  @Mock
  private ComponentInfoService mockComponentInfoService;

  @Mock
  private ComponentRemediationService mockComponentRemediationService;

  @Mock
  private ProductLicense mockProductLicense;

  private Application application;

  // Subject
  PullRequestCommentingRemediationService service;

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    application = tempEntity.newApplicationWithParent();
  }

  @Test
  public void testGetRemediationVersionMap_remediationFound_withoutDependencyInfo_adpEnabled_featureFlagDisabled() {
    testGetRemediationVersionMap_remediationFound(true, false, false);
  }

  @Test
  public void testGetRemediationVersionMap_remediationFound_withDependencyInfo_adpEnabled_featureFlagDisabled() {
    testGetRemediationVersionMap_remediationFound(true, true, false);
  }

  @Test
  public void testGetRemediationVersionMap_remediationFound_adpDisabled_featureFlagDisabled() {
    testGetRemediationVersionMap_remediationFound(false, false, false);
  }

  @Test
  public void testGetRemediationVersionMap_remediationFound_withoutDependencyInfo_adpEnabled_featureFlagEnabled() {
    testGetRemediationVersionMap_remediationFound(true, false, true);
  }

  @Test
  public void testGetRemediationVersionMap_remediationFound_withDependencyInfo_adpEnabled_featureFlagEnabled() {
    testGetRemediationVersionMap_remediationFound(true, true, true);
  }

  @Test
  public void testGetRemediationVersionMap_remediationFound_adpDisabled_featureFlagEnabled() {
    testGetRemediationVersionMap_remediationFound(false, false, true);
  }

  /**
   * If remediationWithDependenciesAvailable is false adpEnabled must be false
   */
  private void testGetRemediationVersionMap_remediationFound(
      boolean adpEnabled,
      boolean remediationWithDependenciesAvailable,
      boolean experimentalFeatureFlagEnabled)
  {

    // given:
    service = new PullRequestCommentingRemediationService(new ApplicationDAO(), mockComponentInfoService,
        mockComponentRemediationService, mockProductLicense, getInsightConfig(experimentalFeatureFlagEnabled));

    ComponentIdentifier id1 = ComponentIdentifier.createNpmCoordinates("artifact-1", "1.0.0");

    List<PolicyViolation> violationList = new LinkedList<>();
    PolicyViolation violation = new PolicyViolation();
    violation.setComponentIdentifier(id1);
    violationList.add(violation);

    // and: there is a remediation version for the component identifier with specific types
    componentInfoServiceSetup(adpEnabled);
    componentRemediationServiceSetup(ComponentIdentifier.createNpmCoordinates("artifact-1", "1.2.0"),
        remediationWithDependenciesAvailable);

    // when:
    Map<ComponentIdentifier, RemediationVersionDTO> versionMap =
        service.getRemediationVersionMap(violationList, application.getId());

    // then: remediation version returned in map
    assertThat(versionMap.containsKey(id1)).isTrue();
    assertThat(versionMap.get(id1).getVersion()).isEqualTo("1.2.0");

    if (adpEnabled) {
      assertThat(versionMap.get(id1).getBreakingChangesCount()).isEqualTo(7);
      if (experimentalFeatureFlagEnabled) {
        if (remediationWithDependenciesAvailable) {
          assertThat(versionMap.get(id1).getRemediationType())
              .isEqualTo(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES);
        }
        else {
          assertThat(versionMap.get(id1).getRemediationType()).isEqualTo(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS);
        }
      }
    }
    else {
      assertThat(versionMap.get(id1).getBreakingChangesCount()).isNull();
      assertThat(versionMap.get(id1).getRemediationType()).isEqualTo(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS);
    }
  }

  //This method will be removed when experimental feature flag is removed
  @Test
  public void testGetRemediationVersionMap_remediationNotFound_featureFlagDisabled() {
    // given:
    service = new PullRequestCommentingRemediationService(new ApplicationDAO(), mockComponentInfoService,
        mockComponentRemediationService, mockProductLicense, getInsightConfig(false));

    ComponentIdentifier id2 = ComponentIdentifier.createNpmCoordinates("artifact-2", "2.0.0");

    List<PolicyViolation> violationList = new LinkedList<>();
    PolicyViolation violation = new PolicyViolation();
    violation.setComponentIdentifier(id2);
    violationList.add(violation);

    // and: there is no remediation version for the component identifier
    componentInfoServiceSetup(true);
    componentRemediationServiceSetup(null, false);

    // when:
    Map<ComponentIdentifier, RemediationVersionDTO> versionMap =
        service.getRemediationVersionMap(violationList, application.getId());

    // then: remediation version returned in map
    assertThat(versionMap.containsKey(id2)).isFalse();
  }

  @Test
  public void testGetRemediationVersionMap_remediationNotFound_featureFlagEnabled() {
    // given:
    service = new PullRequestCommentingRemediationService(new ApplicationDAO(), mockComponentInfoService,
        mockComponentRemediationService, mockProductLicense, getInsightConfig(true));

    ComponentIdentifier id2 = ComponentIdentifier.createNpmCoordinates("artifact-2", "2.0.0");

    List<PolicyViolation> violationList = new LinkedList<>();
    PolicyViolation violation = new PolicyViolation();
    violation.setComponentIdentifier(id2);
    violationList.add(violation);

    // and: there is no remediation version for the component identifier
    componentInfoServiceSetup(true);
    componentRemediationServiceSetup(null, false);

    // when:
    Map<ComponentIdentifier, RemediationVersionDTO> versionMap =
        service.getRemediationVersionMap(violationList, application.getId());

    // then: remediation version returned in map
    assertThat(versionMap.containsKey(id2)).isFalse();
  }

  private void componentInfoServiceSetup(boolean adpEnabled) {
    lenient().when(mockProductLicense.hasFeature(LicensedFeature.BREAKING_CHANGE)).thenReturn(adpEnabled);

    List<ComponentDetailsDTO> componentDetailsDTOs = new LinkedList<>();
    ComponentDetailsDTO componentDetailsDTO = new ComponentDetailsDTO();
    componentDetailsDTO.componentIdentifier = ComponentIdentifier.createNpmCoordinates("artifact-1", "1.0.0");
    componentDetailsDTO.breakingChangesCount = adpEnabled ? 0 : null;
    componentDetailsDTOs.add(componentDetailsDTO);

    componentDetailsDTO = new ComponentDetailsDTO();
    componentDetailsDTO.componentIdentifier = ComponentIdentifier.createNpmCoordinates("artifact-1", "1.1.0");
    componentDetailsDTO.breakingChangesCount = adpEnabled ? 2 : null;
    componentDetailsDTOs.add(componentDetailsDTO);

    componentDetailsDTO = new ComponentDetailsDTO();
    componentDetailsDTO.componentIdentifier = ComponentIdentifier.createNpmCoordinates("artifact-1", "1.2.0");
    componentDetailsDTO.breakingChangesCount = adpEnabled ? 7 : null;
    componentDetailsDTOs.add(componentDetailsDTO);

    when(mockComponentInfoService.getComponentDetailsForAllVersionsNoAuth(
        any(), any(), any(), any(), any(), any(), any())).thenReturn(componentDetailsDTOs);
  }

  private void componentRemediationServiceSetup(
      ComponentIdentifier componentIdentifier,
      boolean noViolationsWithDependenciesAvailable)
  {
    ApiComponentRemediationValueDTO remediationValueDto = new ApiComponentRemediationValueDTO();
    remediationValueDto.versionChanges = new LinkedList<>();
    if (componentIdentifier != null) {
      remediationValueDto.versionChanges
          .add(getApiVersionChangeOptionDTO(componentIdentifier, ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS));
    }
    if (noViolationsWithDependenciesAvailable) {
      remediationValueDto.versionChanges.add(getApiVersionChangeOptionDTO(componentIdentifier,
          ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES));
    }
    when(mockComponentRemediationService.getSuggestedRemediation(
        any(), any(), any(), any(), any())).thenReturn(remediationValueDto);
  }

  private ApiVersionChangeOptionDTO getApiVersionChangeOptionDTO(
      ComponentIdentifier componentIdentifier,
      ApiVersionChangeOptionType remediationType)
  {
    ApiVersionChangeOptionDTO versionChangeOptionDTO = new ApiVersionChangeOptionDTO();
    ApiComponentDTOV2 componentDTOV2 = new ApiComponentDTOV2();
    componentDTOV2.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);
    ApiComponentChangeActionDTO data = new ApiComponentChangeActionDTO(componentDTOV2);
    versionChangeOptionDTO.setType(remediationType);
    versionChangeOptionDTO.setData(data);

    return versionChangeOptionDTO;
  }

  private InsightConfig getInsightConfig(boolean featureFlagEnabled) {
    InsightConfig config = new InsightConfig();
    Map<String, Boolean> experimentalFeatures = new HashMap<>();
    if (featureFlagEnabled) {
      experimentalFeatures.put(PullRequestCommentingRemediationService.ADVANCED_REMEDIATION_IN_IQ_FOR_SCM, true);
    }
    config.setExperimentalFeatures(experimentalFeatures);
    return config;
  }
}
