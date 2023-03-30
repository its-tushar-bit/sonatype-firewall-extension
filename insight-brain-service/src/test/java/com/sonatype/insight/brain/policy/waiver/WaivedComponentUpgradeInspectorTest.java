/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.waiver;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.actions.ApiComponentChangeActionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.api.v2.service.ApiComponentRemediationService;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.TestPolicyWaiverBuilder;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.test.LogOutput;

import com.google.inject.Binder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

public class WaivedComponentUpgradeInspectorTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(WaivedComponentUpgradeInspector.class);

  @Inject
  private OrganizationDAO organizationDAO;

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

  private String waivedComponentUpgradeStageTypeId;

  private static final String DUMMY_PURL = "pkg:maven/g1/a1@v1?type=jar";

  @Override
  public void configure(Binder binder) {
    binder.bind(ComponentInfoService.class).toInstance(componentInfoServiceMock);
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
    binder.bind(HdsClient.class).toInstance(hdsClientMock);
    binder.bind(ProductLicense.class).toInstance(productLicense);
    binder.bind(ThirdPartyComponentDAO.class).toInstance(thirdPartyComponentDAO);
    binder.bind(ApiComponentRemediationService.class).toInstance(apiComponentRemediationService);
    lenient().doReturn(ComponentSummary.create(true)).when(hdsClientMock).get(eq(ComponentSummary.class),
        eq("rest/component/summary"), anyMap());
    super.configure(binder);
  }

  @Before
  public void before() {
    // Capture the original root org waived component upgrade stage id, so we can restore it after the tests.
    Organization rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    waivedComponentUpgradeStageTypeId = rootOrg.getWaivedComponentUpgradeStageTypeId();
    rootOrg.setWaivedComponentUpgradeStageTypeId(ReleaseStageType.ID);
    organizationDAO.update(rootOrg);
  }

  @After
  public void restoreRootOrganizationState() {
    Organization rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    rootOrg.setWaivedComponentUpgradeStageTypeId(waivedComponentUpgradeStageTypeId);
    organizationDAO.update(rootOrg);
  }

  @Test
  public void testIgnoreProcess_noStageConfigured() {
    Organization rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    rootOrg.setWaivedComponentUpgradeStageTypeId(null);
    organizationDAO.update(rootOrg);

    waivedComponentUpgradeInspector.run();
    List<String> infoMessages = logOutput.getInfoMessages(WaivedComponentUpgradeInspector.class.getName());
    assertThat(infoMessages).contains("Could not run WaivedComponentUpgradeInspector as stage is not configured");
  }

  @Test
  public void testOnlyProcessWaiversThatHaveKnownComponents() {
    Organization org = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplication("Application 1", "Application-1", org.getId());
    Policy policy = tempEntity.newPolicy(org);
    PolicyWaiver waiver =
        new TestPolicyWaiverBuilder().withPolicyId(policy.getId()).withOwnerId(app1.getId())
            .withComponentMatcherStrategyForWaiver(ComponentMatcherStrategyForWaiver.EXACT_COMPONENT)
            .withComponentUpgradeAvailable(null).withAssociatedPackageUrl(null).build();
    tempEntity.newWaiver(waiver);

    waivedComponentUpgradeInspector.run();

    verify(apiComponentRemediationService, Mockito.times(0)).getSuggestedRemediationForComponentNoAuthz(
        any(ApiComponentDTOV2.class), any(OwnerType.class), anyString(),
        anyString(), eq(null), eq(null));
  }

  @Test
  public void testOnlyProcessWaiversThatAreExactMatchWaivers() {
    Organization org = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplication("Application 1", "Application-1", org.getId());
    Policy policy = tempEntity.newPolicy(org);
    PolicyWaiver waiverAllVersions =
        new TestPolicyWaiverBuilder().withPolicyId(policy.getId()).withOwnerId(app1.getId())
            .withComponentMatcherStrategyForWaiver(
                ComponentMatcherStrategyForWaiver.ALL_VERSIONS).withAssociatedPackageUrl(DUMMY_PURL).build();
    PolicyWaiver waiverAllComponents =
        new TestPolicyWaiverBuilder().withPolicyId(policy.getId()).withOwnerId(app1.getId())
            .withComponentMatcherStrategyForWaiver(
                ComponentMatcherStrategyForWaiver.ALL_COMPONENTS).withAssociatedPackageUrl(DUMMY_PURL).build();
    PolicyWaiver waiverExactComponent =
        new TestPolicyWaiverBuilder().withPolicyId(policy.getId()).withOwnerId(app1.getId())
            .withComponentMatcherStrategyForWaiver(
                ComponentMatcherStrategyForWaiver.EXACT_COMPONENT).withAssociatedPackageUrl(DUMMY_PURL).build();
    tempEntity.newWaiver(waiverAllVersions);
    tempEntity.newWaiver(waiverAllComponents);
    tempEntity.newWaiver(waiverExactComponent);

    doReturn(getSimpleRemediationResponseWithSuggestion()).when(apiComponentRemediationService)
        .getSuggestedRemediationForComponentNoAuthz(any(ApiComponentDTOV2.class), any(OwnerType.class), anyString(),
            anyString(), eq(null), eq(null));

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
        new TestPolicyWaiverBuilder().withPolicyId(policy.getId()).withOwnerId(app1.getId())
            .withComponentMatcherStrategyForWaiver(ComponentMatcherStrategyForWaiver.EXACT_COMPONENT)
            .withComponentUpgradeAvailable(true).build();
    tempEntity.newWaiver(waiver);

    waivedComponentUpgradeInspector.run();

    verify(apiComponentRemediationService, Mockito.times(0)).getSuggestedRemediationForComponentNoAuthz(
        any(ApiComponentDTOV2.class), any(OwnerType.class), anyString(),
        anyString(), eq(null), eq(null));
  }

  @Test
  public void testDoesNotMarkWaiverIfNoUpgradeAvailable_noVersionToRemediate() {
    Organization org = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplication("Application 1", "Application-1", org.getId());
    Policy policy = tempEntity.newPolicy(org);
    PolicyWaiver waiver =
        new TestPolicyWaiverBuilder().withPolicyId(policy.getId()).withOwnerId(app1.getId())
            .withComponentMatcherStrategyForWaiver(ComponentMatcherStrategyForWaiver.EXACT_COMPONENT)
            .withAssociatedPackageUrl(DUMMY_PURL)
            .build();
    tempEntity.newWaiver(waiver);

    ApiComponentRemediationDTO simpleRemediationResponseWithSuggestion = getSimpleRemediationResponseWithSuggestion();
    simpleRemediationResponseWithSuggestion.remediation.versionChanges.clear();

    doReturn(simpleRemediationResponseWithSuggestion).when(apiComponentRemediationService)
        .getSuggestedRemediationForComponentNoAuthz(any(ApiComponentDTOV2.class), any(OwnerType.class), anyString(),
            anyString(), eq(null), eq(null));

    waivedComponentUpgradeInspector.run();

    verify(apiComponentRemediationService, Mockito.times(1)).getSuggestedRemediationForComponentNoAuthz(
        any(ApiComponentDTOV2.class), any(OwnerType.class), anyString(),
        anyString(), eq(null), eq(null));
    waiver = policyWaiverDAO.getActiveByPolicyId(policy.getId()).get(0);
    assertThat(waiver.isComponentUpgradeAvailable()).isNull();
  }

  @Test
  public void testDoesNotMarkWaiverIfNoUpgradeAvailable_remediationIsNull() {
    Organization org = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplication("Application 1", "Application-1", org.getId());
    Policy policy = tempEntity.newPolicy(org);
    PolicyWaiver waiver =
        new TestPolicyWaiverBuilder().withPolicyId(policy.getId()).withOwnerId(app1.getId())
            .withComponentMatcherStrategyForWaiver(ComponentMatcherStrategyForWaiver.EXACT_COMPONENT)
            .withAssociatedPackageUrl(DUMMY_PURL)
            .build();
    tempEntity.newWaiver(waiver);

    doReturn(null).when(apiComponentRemediationService)
        .getSuggestedRemediationForComponentNoAuthz(any(ApiComponentDTOV2.class), any(OwnerType.class), anyString(),
            anyString(), eq(null), eq(null));

    waivedComponentUpgradeInspector.run();

    verify(apiComponentRemediationService, Mockito.times(1)).getSuggestedRemediationForComponentNoAuthz(
        any(ApiComponentDTOV2.class), any(OwnerType.class), anyString(),
        anyString(), eq(null), eq(null));
    waiver = policyWaiverDAO.getActiveByPolicyId(policy.getId()).get(0);
    assertThat(waiver.isComponentUpgradeAvailable()).isNull();
  }

  @Test
  public void testDoesNotMarkWaiverIfNoUpgradeAvailable_sameComponentSuggested_CLM_22331() {
    Organization org = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplication("Application 1", "Application-1", org.getId());
    Policy policy = tempEntity.newPolicy(org);
    PolicyWaiver waiver =
        new TestPolicyWaiverBuilder().withPolicyId(policy.getId()).withOwnerId(app1.getId())
            .withComponentMatcherStrategyForWaiver(ComponentMatcherStrategyForWaiver.EXACT_COMPONENT)
            .withAssociatedPackageUrl(DUMMY_PURL)
            .build();
    tempEntity.newWaiver(waiver);

    ApiComponentRemediationDTO simpleRemediationResponseWithSuggestion = getSimpleRemediationResponseWithSuggestion();
    ApiVersionChangeOptionDTO versionChangeOptionDTO =
        simpleRemediationResponseWithSuggestion.remediation.versionChanges.get(0);
    versionChangeOptionDTO.getData().getComponent().packageUrl = DUMMY_PURL;

    doReturn(simpleRemediationResponseWithSuggestion).when(apiComponentRemediationService)
        .getSuggestedRemediationForComponentNoAuthz(any(ApiComponentDTOV2.class), any(OwnerType.class), anyString(),
            anyString(), eq(null), eq(null));

    waivedComponentUpgradeInspector.run();

    verify(apiComponentRemediationService, Mockito.times(1)).getSuggestedRemediationForComponentNoAuthz(
        any(ApiComponentDTOV2.class), any(OwnerType.class), anyString(),
        anyString(), eq(null), eq(null));
    waiver = policyWaiverDAO.getActiveByPolicyId(policy.getId()).get(0);
    assertThat(waiver.isComponentUpgradeAvailable()).isNull();
  }

  @Test
  public void testRepositoryWaiversAreTiedToProxyStage() {
    Organization rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    Repository repo = tempEntity.newRepository();
    Policy policy = tempEntity.newPolicy(rootOrg);
    PolicyWaiver waiverRepository =
        new TestPolicyWaiverBuilder().withPolicyId(policy.getId()).withOwnerId(repo.getId())
            .withComponentMatcherStrategyForWaiver(ComponentMatcherStrategyForWaiver.EXACT_COMPONENT)
            .withAssociatedPackageUrl(DUMMY_PURL)
            .build();
    tempEntity.newWaiver(waiverRepository);
    PolicyWaiver waiverRepositoryContainer =
        new TestPolicyWaiverBuilder().withPolicyId(policy.getId())
            .withOwnerId(RepositoryContainer.REPOSITORY_CONTAINER_ID)
            .withComponentMatcherStrategyForWaiver(ComponentMatcherStrategyForWaiver.EXACT_COMPONENT)
            .withAssociatedPackageUrl(DUMMY_PURL)
            .build();
    tempEntity.newWaiver(waiverRepositoryContainer);
    doReturn(getSimpleRemediationResponseWithSuggestion()).when(apiComponentRemediationService)
        .getSuggestedRemediationForComponentNoAuthz(any(ApiComponentDTOV2.class), any(OwnerType.class), anyString(),
            eq(ProxyStageType.ID), eq(null), eq(null));

    waivedComponentUpgradeInspector.run();

    verify(apiComponentRemediationService, Mockito.times(2)).getSuggestedRemediationForComponentNoAuthz(
        any(ApiComponentDTOV2.class), any(OwnerType.class), anyString(),
        eq(ProxyStageType.ID), eq(null), eq(null));
  }

  @Test
  public void testErrorDuringSingleWaiverDoesNotPropagateAndStopProcess() {
    Organization org = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplication("Application 1", "Application-1", org.getId());
    Policy policy = tempEntity.newPolicy(org);
    PolicyWaiver waiver1 =
        new TestPolicyWaiverBuilder().withPolicyId(policy.getId()).withOwnerId(app1.getId())
            .withComponentMatcherStrategyForWaiver(ComponentMatcherStrategyForWaiver.EXACT_COMPONENT)
            .withAssociatedPackageUrl(DUMMY_PURL)
            .build();
    tempEntity.newWaiver(waiver1);
    PolicyWaiver waiver2 =
        new TestPolicyWaiverBuilder().withPolicyId(policy.getId()).withOwnerId(org.getId())
            .withComponentMatcherStrategyForWaiver(ComponentMatcherStrategyForWaiver.EXACT_COMPONENT)
            .withAssociatedPackageUrl(DUMMY_PURL)
            .build();
    tempEntity.newWaiver(waiver2);
    doThrow(new BadRequestException("Something happened while processing waiver")).when(apiComponentRemediationService)
        .getSuggestedRemediationForComponentNoAuthz(any(ApiComponentDTOV2.class), any(OwnerType.class), anyString(),
            anyString(), eq(null), eq(null));

    waivedComponentUpgradeInspector.run();

    final String expectedMessage1 =
        "Error when marking waiver as having component upgrade available. Waiver id: " + waiver1.getId();
    final String expectedMessage2 =
        "Error when marking waiver as having component upgrade available. Waiver id: " + waiver2.getId();
    List<String> errorMessages = logOutput.getErrorMessages(WaivedComponentUpgradeInspector.class.getName());
    assertThat(errorMessages).hasSize(2).contains(expectedMessage1, expectedMessage2);
  }

  private ApiComponentRemediationDTO getSimpleRemediationResponseWithSuggestion() {
    ApiComponentDTOV2 apiComponentDTOV2 = new ApiComponentDTOV2();
    apiComponentDTOV2.packageUrl = "packageUrlValue";
    ApiComponentChangeActionDTO apiComponentChangeActionDTO = new ApiComponentChangeActionDTO(apiComponentDTOV2);
    ApiVersionChangeOptionDTO versionChangeOptionDTO =
        new ApiVersionChangeOptionDTO(ApiVersionChangeOptionType.NEXT_NON_FAILING, apiComponentChangeActionDTO);

    ApiComponentRemediationDTO dto = new ApiComponentRemediationDTO();
    dto.remediation.versionChanges.add(versionChangeOptionDTO);
    return dto;
  }
}
