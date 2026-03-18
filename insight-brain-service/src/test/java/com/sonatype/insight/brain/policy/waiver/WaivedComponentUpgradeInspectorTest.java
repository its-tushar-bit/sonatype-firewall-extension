/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.waiver;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.actions.ApiComponentChangeActionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.api.v2.service.ApiComponentRemediationService;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.test.LogOutput;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WaivedComponentUpgradeInspectorTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(WaivedComponentUpgradeInspector.class);

  @Inject
  private PolicyWaiverDAO policyWaiverDAO;

  @Inject
  private WaivedComponentUpgradeInspector waivedComponentUpgradeInspector;

  @Mock
  private ComponentInfoService componentInfoServiceMock;

  @Mock
  private TelemetrySender telemetrySenderMock;

  @Mock
  private ThirdPartyComponentDAO thirdPartyComponentDAO;

  @Mock
  HdsClient hdsClientMock;

  @Mock
  private ProductLicense productLicense;

  @Mock
  private ApiComponentRemediationService apiComponentRemediationService;

  @Mock
  private Configuration configurationMock;

  private static final String DUMMY_PURL = "pkg:maven/g1/a1@v1?type=jar";

  @Override
  public void configure(Binder binder) {
    binder.bind(ComponentInfoService.class).toInstance(componentInfoServiceMock);
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
    binder.bind(HdsClient.class).toInstance(hdsClientMock);
    binder.bind(ProductLicense.class).toInstance(productLicense);
    binder.bind(ThirdPartyComponentDAO.class).toInstance(thirdPartyComponentDAO);
    binder.bind(ApiComponentRemediationService.class).toInstance(apiComponentRemediationService);
    binder.bind(Configuration.class).toInstance(configurationMock);
    lenient().doReturn(ComponentSummary.create(true))
        .when(hdsClientMock)
        .get(eq(ComponentSummary.class),
            eq("rest/component/summary"), anyMap());
    super.configure(binder);
  }

  @Before
  public void beforeEachTest() {
    when(configurationMock.getWaivedComponentUpgradeMonitoringEnabled()).thenReturn(true);
  }

  @Test
  public void testIgnoreProcess_monitoringIsOff() {
    when(configurationMock.getWaivedComponentUpgradeMonitoringEnabled()).thenReturn(false);

    waivedComponentUpgradeInspector.run();
    List<String> infoMessages = logOutput.getInfoMessages(WaivedComponentUpgradeInspector.class.getName());
    assertThat(infoMessages).contains(
        "Could not run Waived Component Upgrade Inspector as upgrade monitoring is turned off");
  }

  @Test
  public void testOnlyProcessWaiversThatHaveKnownComponents() {
    Organization org = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplication("Application 1", "Application-1", org.getId());
    Policy policy = tempEntity.newPolicy(org);
    PolicyWaiver waiver =
        new PolicyWaiver().setPolicyId(policy.getId())
            .setOwnerId(app1.getId())
            .setComponentMatchStrategy(ComponentMatcherStrategyForWaiver.EXACT_COMPONENT)
            .setComponentUpgradeAvailable(null)
            .setAssociatedPackageUrl(null);
    tempEntity.newWaiver(waiver);

    waivedComponentUpgradeInspector.run();

    verify(apiComponentRemediationService, Mockito.times(0)).getSuggestedRemediationForComponentNoAuthz(
        any(ApiComponentDTOV2.class), eq(OwnerType.APPLICATION), eq(app1.getId()), isNull(), isNull(), isNull(),
        isNull(), anyBoolean());
  }

  @Test
  public void testOnlyProcessWaiversThatAreExactMatchWaivers() {
    Organization org = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplication("Application 1", "Application-1", org.getId());
    Policy policy = tempEntity.newPolicy(org);
    PolicyWaiver waiverAllVersions =
        new PolicyWaiver().setPolicyId(policy.getId())
            .setOwnerId(app1.getId())
            .setComponentMatchStrategy(
                ComponentMatcherStrategyForWaiver.ALL_VERSIONS)
            .setAssociatedPackageUrl(DUMMY_PURL);
    PolicyWaiver waiverAllComponents =
        new PolicyWaiver().setPolicyId(policy.getId())
            .setOwnerId(app1.getId())
            .setComponentMatchStrategy(
                ComponentMatcherStrategyForWaiver.ALL_COMPONENTS)
            .setAssociatedPackageUrl(DUMMY_PURL);
    PolicyWaiver waiverExactComponent =
        new PolicyWaiver().setPolicyId(policy.getId())
            .setOwnerId(app1.getId())
            .setComponentMatchStrategy(
                ComponentMatcherStrategyForWaiver.EXACT_COMPONENT)
            .setAssociatedPackageUrl(DUMMY_PURL);
    tempEntity.newWaiver(waiverAllVersions);
    tempEntity.newWaiver(waiverAllComponents);
    tempEntity.newWaiver(waiverExactComponent);

    doReturn(getSimpleRemediationResponseWithSuggestion()).when(apiComponentRemediationService)
        .getSuggestedRemediationForComponentNoAuthz(any(ApiComponentDTOV2.class), eq(OwnerType.APPLICATION),
            eq(app1.getId()), isNull(), isNull(), isNull(), isNull(), anyBoolean());

    waivedComponentUpgradeInspector.run();

    List<PolicyWaiver> activeByPolicyId = policyWaiverDAO.getActiveByPolicyId(policy.getId());
    assertThat(activeByPolicyId).hasSize(3);
    List<Boolean> upgradeableMarks =
        activeByPolicyId.stream().map(PolicyWaiver::isComponentUpgradeAvailable).collect(Collectors.toList());
    assertThat(upgradeableMarks).containsExactly(null, null, true);
  }

  @Test
  public void testOnlyProcessWaiversThatHaveNoComponentUpgradeAvailable() {
    Organization org = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplication("Application 1", "Application-1", org.getId());
    Policy policy = tempEntity.newPolicy(org);
    PolicyWaiver waiver =
        new PolicyWaiver().setPolicyId(policy.getId())
            .setOwnerId(app1.getId())
            .setComponentMatchStrategy(ComponentMatcherStrategyForWaiver.EXACT_COMPONENT)
            .setComponentUpgradeAvailable(true);
    tempEntity.newWaiver(waiver);

    waivedComponentUpgradeInspector.run();

    verify(apiComponentRemediationService, Mockito.times(0)).getSuggestedRemediationForComponentNoAuthz(
        any(ApiComponentDTOV2.class), eq(OwnerType.APPLICATION), eq(app1.getId()), isNull(), isNull(), isNull(),
        isNull(), anyBoolean());
  }

  @Test
  public void testDoesNotMarkWaiverIfNoUpgradeAvailable_noVersionToRemediate() {
    Organization org = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplication("Application 1", "Application-1", org.getId());
    Policy policy = tempEntity.newPolicy(org);
    PolicyWaiver waiver =
        new PolicyWaiver().setPolicyId(policy.getId())
            .setOwnerId(app1.getId())
            .setComponentMatchStrategy(ComponentMatcherStrategyForWaiver.EXACT_COMPONENT)
            .setAssociatedPackageUrl(DUMMY_PURL);
    tempEntity.newWaiver(waiver);

    ApiComponentRemediationDTO simpleRemediationResponseWithSuggestion = getSimpleRemediationResponseWithSuggestion();
    simpleRemediationResponseWithSuggestion.remediation.versionChanges.clear();

    doReturn(simpleRemediationResponseWithSuggestion).when(apiComponentRemediationService)
        .getSuggestedRemediationForComponentNoAuthz(any(ApiComponentDTOV2.class), any(OwnerType.class),
            eq(app1.getId()), isNull(), isNull(), isNull(), isNull(), anyBoolean());

    waivedComponentUpgradeInspector.run();

    verify(apiComponentRemediationService, Mockito.times(1)).getSuggestedRemediationForComponentNoAuthz(
        any(ApiComponentDTOV2.class), eq(OwnerType.APPLICATION), eq(app1.getId()), isNull(), isNull(), isNull(),
        isNull(), anyBoolean());
    waiver = policyWaiverDAO.getActiveByPolicyId(policy.getId()).get(0);
    assertThat(waiver.isComponentUpgradeAvailable()).isNull();
  }

  @Test
  public void testDoesNotMarkWaiverIfNoUpgradeAvailable_remediationIsNull() {
    Organization org = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplication("Application 1", "Application-1", org.getId());
    Policy policy = tempEntity.newPolicy(org);
    PolicyWaiver waiver =
        new PolicyWaiver().setPolicyId(policy.getId())
            .setOwnerId(app1.getId())
            .setComponentMatchStrategy(ComponentMatcherStrategyForWaiver.EXACT_COMPONENT)
            .setAssociatedPackageUrl(DUMMY_PURL);
    tempEntity.newWaiver(waiver);

    doReturn(null).when(apiComponentRemediationService)
        .getSuggestedRemediationForComponentNoAuthz(any(ApiComponentDTOV2.class), eq(OwnerType.APPLICATION),
            eq(app1.getId()), isNull(), isNull(), isNull(), isNull(), anyBoolean());

    waivedComponentUpgradeInspector.run();

    verify(apiComponentRemediationService, Mockito.times(1)).getSuggestedRemediationForComponentNoAuthz(
        any(ApiComponentDTOV2.class), eq(OwnerType.APPLICATION), eq(app1.getId()), isNull(), isNull(), isNull(),
        isNull(), anyBoolean());
    waiver = policyWaiverDAO.getActiveByPolicyId(policy.getId()).get(0);
    assertThat(waiver.isComponentUpgradeAvailable()).isNull();
  }

  @Test
  public void testDoesNotMarkWaiverIfNoUpgradeAvailable_sameComponentSuggested_CLM_22331() {
    Organization org = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplication("Application 1", "Application-1", org.getId());
    Policy policy = tempEntity.newPolicy(org);
    PolicyWaiver waiver =
        new PolicyWaiver().setPolicyId(policy.getId())
            .setOwnerId(app1.getId())
            .setComponentMatchStrategy(ComponentMatcherStrategyForWaiver.EXACT_COMPONENT)
            .setAssociatedPackageUrl(DUMMY_PURL);
    tempEntity.newWaiver(waiver);

    ApiComponentRemediationDTO simpleRemediationResponseWithSuggestion = getSimpleRemediationResponseWithSuggestion();
    ApiVersionChangeOptionDTO versionChangeOptionDTO =
        simpleRemediationResponseWithSuggestion.remediation.versionChanges.get(0);
    versionChangeOptionDTO.getData().getComponent().packageUrl = DUMMY_PURL;

    doReturn(simpleRemediationResponseWithSuggestion).when(apiComponentRemediationService)
        .getSuggestedRemediationForComponentNoAuthz(any(ApiComponentDTOV2.class), eq(OwnerType.APPLICATION),
            eq(app1.getId()), isNull(), isNull(), isNull(), isNull(), anyBoolean());

    waivedComponentUpgradeInspector.run();

    verify(apiComponentRemediationService, Mockito.times(1)).getSuggestedRemediationForComponentNoAuthz(
        any(ApiComponentDTOV2.class), any(OwnerType.class), eq(app1.getId()), isNull(), isNull(), isNull(), isNull(),
        anyBoolean());
    waiver = policyWaiverDAO.getActiveByPolicyId(policy.getId()).get(0);
    assertThat(waiver.isComponentUpgradeAvailable()).isNull();
  }

  @Test
  public void testErrorDuringSingleWaiverDoesNotPropagateAndStopProcess() {
    Organization org = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplication("Application 1", "Application-1", org.getId());
    Policy policy = tempEntity.newPolicy(org);
    PolicyWaiver waiver1 =
        new PolicyWaiver().setPolicyId(policy.getId())
            .setOwnerId(app1.getId())
            .setComponentMatchStrategy(ComponentMatcherStrategyForWaiver.EXACT_COMPONENT)
            .setAssociatedPackageUrl(DUMMY_PURL);
    tempEntity.newWaiver(waiver1);
    PolicyWaiver waiver2 =
        new PolicyWaiver().setPolicyId(policy.getId())
            .setOwnerId(org.getId())
            .setComponentMatchStrategy(ComponentMatcherStrategyForWaiver.EXACT_COMPONENT)
            .setAssociatedPackageUrl(DUMMY_PURL);
    tempEntity.newWaiver(waiver2);
    doThrow(new BadRequestException("Something happened while processing waiver")).when(apiComponentRemediationService)
        .getSuggestedRemediationForComponentNoAuthz(any(ApiComponentDTOV2.class), eq(OwnerType.APPLICATION),
            eq(app1.getId()), isNull(), isNull(), isNull(), isNull(), anyBoolean());
    doThrow(new BadRequestException("Something happened while processing waiver")).when(apiComponentRemediationService)
        .getSuggestedRemediationForComponentNoAuthz(any(ApiComponentDTOV2.class), eq(OwnerType.ORGANIZATION),
            eq(org.getId()), isNull(), isNull(), isNull(), isNull(), anyBoolean());

    waivedComponentUpgradeInspector.run();

    final String expectedMessage1 =
        "Error when marking waiver as having component upgrade available. Waiver id: " + waiver1.getId();
    final String expectedMessage2 =
        "Error when marking waiver as having component upgrade available. Waiver id: " + waiver2.getId();
    List<String> errorMessages = logOutput.getErrorMessages(WaivedComponentUpgradeInspector.class.getName());
    assertThat(errorMessages).hasSize(2).contains(expectedMessage1, expectedMessage2);
  }

  @Test
  public void testOnlySuggestUpgradeForNextNonViolatingRemediationStrategies() {
    Organization org = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplication("Application 1", "Application-1", org.getId());
    Policy policy = tempEntity.newPolicy(org);
    PolicyWaiver waiver =
        new PolicyWaiver().setPolicyId(policy.getId())
            .setOwnerId(app1.getId())
            .setComponentMatchStrategy(
                ComponentMatcherStrategyForWaiver.EXACT_COMPONENT)
            .setAssociatedPackageUrl(DUMMY_PURL);
    tempEntity.newWaiver(waiver);

    ApiComponentRemediationDTO simpleRemediationResponseWithSuggestion = getSimpleRemediationResponseWithSuggestion();
    doReturn(simpleRemediationResponseWithSuggestion).when(apiComponentRemediationService)
        .getSuggestedRemediationForComponentNoAuthz(any(ApiComponentDTOV2.class), eq(OwnerType.APPLICATION),
            eq(app1.getId()), isNull(), isNull(), isNull(), isNull(), anyBoolean());

    modifyRemediationStrategyForResponse(simpleRemediationResponseWithSuggestion,
        ApiVersionChangeOptionType.NEXT_NON_FAILING_WITH_DEPENDENCIES);
    waivedComponentUpgradeInspector.run();

    List<PolicyWaiver> activeByPolicyId = policyWaiverDAO.getActiveByPolicyId(policy.getId());
    assertThat(activeByPolicyId.get(0).isComponentUpgradeAvailable()).isNull();

    modifyRemediationStrategyForResponse(simpleRemediationResponseWithSuggestion,
        ApiVersionChangeOptionType.NEXT_NON_FAILING);
    waivedComponentUpgradeInspector.run();

    activeByPolicyId = policyWaiverDAO.getActiveByPolicyId(policy.getId());
    assertThat(activeByPolicyId.get(0).isComponentUpgradeAvailable()).isNull();

    modifyRemediationStrategyForResponse(simpleRemediationResponseWithSuggestion,
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS);
    waivedComponentUpgradeInspector.run();

    activeByPolicyId = policyWaiverDAO.getActiveByPolicyId(policy.getId());
    assertThat(activeByPolicyId.get(0).isComponentUpgradeAvailable()).isTrue();
  }

  @Test
  public void testOnlyExpireExpireWhenRemediationAvailableWaiverIfUpgradeAvailable() {
    SystemConfigurationPropertyFeature.EXPIRE_WAIVER_WHEN_REMEDIATION_AVAILABLE.setEnabled(true);
    Organization org = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplication("Application 1", "Application-1", org.getId());
    Policy policy = tempEntity.newPolicy(org);
    PolicyWaiver waiver =
        new PolicyWaiver().setPolicyId(policy.getId())
            .setOwnerId(app1.getId())
            .setComponentMatchStrategy(
                ComponentMatcherStrategyForWaiver.EXACT_COMPONENT)
            .setAssociatedPackageUrl(DUMMY_PURL);
    waiver.setExpireWhenRemediationAvailable(true);
    tempEntity.newWaiver(waiver);
    ApiComponentRemediationDTO simpleRemediationResponseWithSuggestion = getSimpleRemediationResponseWithSuggestion();
    doReturn(simpleRemediationResponseWithSuggestion).when(apiComponentRemediationService)
        .getSuggestedRemediationForComponentNoAuthz(any(ApiComponentDTOV2.class), eq(OwnerType.APPLICATION),
            eq(app1.getId()), isNull(), isNull(), isNull(), isNull(), anyBoolean());

    modifyRemediationStrategyForResponse(simpleRemediationResponseWithSuggestion,
        ApiVersionChangeOptionType.NEXT_NON_FAILING_WITH_DEPENDENCIES);
    waivedComponentUpgradeInspector.run();

    List<PolicyWaiver> activeByPolicyId = policyWaiverDAO.getActiveByPolicyId(policy.getId());
    assertThat(activeByPolicyId.get(0).isComponentUpgradeAvailable()).isNull();

    modifyRemediationStrategyForResponse(simpleRemediationResponseWithSuggestion,
        ApiVersionChangeOptionType.NEXT_NON_FAILING);
    waivedComponentUpgradeInspector.run();

    activeByPolicyId = policyWaiverDAO.getActiveByPolicyId(policy.getId());
    assertThat(activeByPolicyId.get(0).isComponentUpgradeAvailable()).isNull();
    assertThat(activeByPolicyId.get(0).getExpiryTime()).isNull();

    modifyRemediationStrategyForResponse(simpleRemediationResponseWithSuggestion,
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS);
    waivedComponentUpgradeInspector.run();

    // waiver will no longer be active
    List<PolicyWaiver> policyWaiversById = policyWaiverDAO.getByPolicyId(policy.getId());
    assertThat(policyWaiversById.get(0).isComponentUpgradeAvailable()).isTrue();
    assertThat(policyWaiversById.get(0).getExpiryTime()).isNotNull();
    SystemConfigurationPropertyFeature.EXPIRE_WAIVER_WHEN_REMEDIATION_AVAILABLE.setEnabled(false);
  }

  private void modifyRemediationStrategyForResponse(
      ApiComponentRemediationDTO simpleRemediationResponseWithSuggestion,
      ApiVersionChangeOptionType strategy)
  {
    ApiComponentChangeActionDTO apiComponentChangeActionDTO =
        simpleRemediationResponseWithSuggestion.remediation.versionChanges.get(0).getData();
    ApiVersionChangeOptionDTO versionChangeOptionDTO =
        new ApiVersionChangeOptionDTO(strategy, apiComponentChangeActionDTO);
    simpleRemediationResponseWithSuggestion.remediation.versionChanges.clear();
    simpleRemediationResponseWithSuggestion.remediation.versionChanges.add(versionChangeOptionDTO);
  }

  private ApiComponentRemediationDTO getSimpleRemediationResponseWithSuggestion() {
    ApiComponentDTOV2 apiComponentDTOV2 = new ApiComponentDTOV2();
    apiComponentDTOV2.packageUrl = "packageUrlValue";
    ApiComponentChangeActionDTO apiComponentChangeActionDTO = new ApiComponentChangeActionDTO(apiComponentDTOV2);
    ApiVersionChangeOptionDTO versionChangeOptionDTO =
        new ApiVersionChangeOptionDTO(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS, apiComponentChangeActionDTO);

    ApiComponentRemediationDTO dto = new ApiComponentRemediationDTO();
    dto.remediation.versionChanges.add(versionChangeOptionDTO);
    return dto;
  }
}
