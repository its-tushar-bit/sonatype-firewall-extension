/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import static com.sonatype.insight.brain.report.ReportTestUtils.zipReportDir;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationViolationListDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentTransitivePolicyViolationsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintViolationReasonDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiEnhancedPolicyViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiStagePolicyViolationComponentDTO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerComponent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.InnerSourceData;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.InvalidPackageURLException;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class ApiPolicyViolationServiceV2Test
    extends AbstractComponentTest
{
  private static final String PACKAGE_URL_MAVEN_V1 = "pkg:maven/g1/a1@v1";

  private static final String PACKAGE_URL_MAVEN_V2 = "pkg:maven/g2/a2@v2";

  private static final String PACKAGE_URL_NUGET = "pkg:nuget/nuget1@v1";

  @Inject
  private ApiPolicyViolationServiceV2 apiPolicyViolationService;

  @Inject
  private InsightWork insightWork;

  @Inject
  private PolicyViolationDAO policyViolationDAO;

  @Test
  public void testGetPolicyViolations_MalformedAfterDate() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> apiPolicyViolationService.getPolicyViolations(Collections.emptySet(), "invalid-date", null,
            Collections.emptySet()))
        .withMessageContaining("Provided value for openTimeAfter is not a valid date.");
  }

  @Test
  public void testGetPolicyViolations_MalformedBeforeDate() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> apiPolicyViolationService.getPolicyViolations(Collections.emptySet(), null, "invalid-date",
            Collections.emptySet()))
        .withMessageContaining("Provided value for openTimeBefore is not a valid date.");
  }

  @Test
  public void testGetPolicyViolations_noPolicyIds() {
    createPolicyTestData("scanId1App1",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), "h1", "r1", "f1.jar");

    ApiApplicationViolationListDTOV2 apiApplicationViolationListDTO = apiPolicyViolationService
        .getPolicyViolations(Collections.emptySet(), null, null, Collections.emptySet());
    assertThat(apiApplicationViolationListDTO).isNotNull();
    assertThat(apiApplicationViolationListDTO.applicationViolations).isEmpty();
  }

  @Test
  public void testGetPolicyViolations_filteredByPolicyId() {
    PolicyData appPolicyData1 = createPolicyTestData("scanId1App1",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), "h1", "r1", "f1.jar");
    PolicyData appPolicyData2 = createPolicyTestData("scanId1App2",
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"), "h2", "r2", "f2.jar");
    createPolicyTestData("scanId1App3",
        ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"), "h3", "r3", "f3.jar");

    // Get the policy violations for two (out of three) policies
    Set<String> policyIds = Sets.newHashSet(appPolicyData1.orgPolicy.getId(), appPolicyData2.orgPolicy.getId());
    ApiApplicationViolationListDTOV2 apiApplicationViolationListDTO = apiPolicyViolationService
        .getPolicyViolations(policyIds, null, null, Collections.emptySet());

    assertThat(apiApplicationViolationListDTO).isNotNull();
    assertThat(apiApplicationViolationListDTO.applicationViolations).hasSize(2);
    ApiApplicationViolationDTOV2 apiApplicationViolationDTO1 = apiApplicationViolationListDTO.applicationViolations
        .get(0);
    ApiApplicationViolationDTOV2 apiApplicationViolationDTO2 = apiApplicationViolationListDTO.applicationViolations
        .get(1);
    if (appPolicyData1.application.getId().equals(apiApplicationViolationDTO1.application.id)) {
      assertPolicyViolation(apiApplicationViolationDTO1, appPolicyData1, PACKAGE_URL_MAVEN_V1);
      assertPolicyViolation(apiApplicationViolationDTO2, appPolicyData2, PACKAGE_URL_MAVEN_V2);
    }
    else {
      assertPolicyViolation(apiApplicationViolationDTO1, appPolicyData2, PACKAGE_URL_MAVEN_V2);
      assertPolicyViolation(apiApplicationViolationDTO2, appPolicyData1, PACKAGE_URL_MAVEN_V1);
    }

    // Assert violations not returned due to after date
    apiApplicationViolationListDTO =
        apiPolicyViolationService.getPolicyViolations(policyIds, "2040-01-01", null, Collections.emptySet());
    assertThat(apiApplicationViolationListDTO).isNotNull();
    assertThat(apiApplicationViolationListDTO.applicationViolations).isEmpty();

    // Assert violations not returned due to before date
    apiApplicationViolationListDTO =
        apiPolicyViolationService.getPolicyViolations(policyIds, null, "2010-12-31", Collections.emptySet());
    assertThat(apiApplicationViolationListDTO).isNotNull();
    assertThat(apiApplicationViolationListDTO.applicationViolations).isEmpty();

    // Assert violations loaded when provided dates are in range
    apiApplicationViolationListDTO =
        apiPolicyViolationService.getPolicyViolations(policyIds, LocalDate.now().minusDays(1).toString(),
            LocalDate.now().plusDays(1).toString(), Collections.emptySet());

    assertThat(apiApplicationViolationListDTO).isNotNull();
    assertThat(apiApplicationViolationListDTO.applicationViolations).hasSize(2);
    apiApplicationViolationDTO1 = apiApplicationViolationListDTO.applicationViolations.get(0);
    apiApplicationViolationDTO2 = apiApplicationViolationListDTO.applicationViolations.get(1);
    if (appPolicyData1.application.getId().equals(apiApplicationViolationDTO1.application.id)) {
      assertPolicyViolation(apiApplicationViolationDTO1, appPolicyData1, PACKAGE_URL_MAVEN_V1);
      assertPolicyViolation(apiApplicationViolationDTO2, appPolicyData2, PACKAGE_URL_MAVEN_V2);
    }
    else {
      assertPolicyViolation(apiApplicationViolationDTO1, appPolicyData2, PACKAGE_URL_MAVEN_V2);
      assertPolicyViolation(apiApplicationViolationDTO2, appPolicyData1, PACKAGE_URL_MAVEN_V1);
    }
  }

  @Test
  public void testGetPolicyViolations_nuGetFilteredByPolicyId() {
    PolicyData appPolicyData1 = createPolicyTestData("scanId1App1",
        ComponentIdentifier.createNugetCoordinates("nuget1", "v1"), "h3", "r4", "f4.jar");
    createPolicyTestData("scanId1App2",
        ComponentIdentifier.createNugetCoordinates("nuget2", "v1"), "h4", "r5", "f5.jar");

    Set<String> policyIds = Sets.newHashSet(appPolicyData1.orgPolicy.getId());
    ApiApplicationViolationListDTOV2 apiApplicationViolationListDTO = apiPolicyViolationService
        .getPolicyViolations(policyIds, null, null, Collections.emptySet());

    assertThat(apiApplicationViolationListDTO).isNotNull();
    assertThat(apiApplicationViolationListDTO.applicationViolations).hasSize(1);
    ApiApplicationViolationDTOV2 apiApplicationViolationDTO1 = apiApplicationViolationListDTO.applicationViolations
        .get(0);
    assertPolicyViolation(apiApplicationViolationDTO1, appPolicyData1, PACKAGE_URL_NUGET);
  }

  @Test
  public void testGetPolicyViolations_unknownComponent() {
    PolicyData appPolicyData =
        createPolicyTestData("scanId", null /* componentIdentifier */, "testhash", "testreason", "test.jar");

    Set<String> policyIds = Collections.singleton(appPolicyData.orgPolicy.getId());
    ApiApplicationViolationListDTOV2 apiApplicationViolationListDTO = apiPolicyViolationService
        .getPolicyViolations(policyIds, null, null, Collections.emptySet());

    assertThat(apiApplicationViolationListDTO.applicationViolations).hasSize(1);
    ApiApplicationViolationDTOV2 apiApplicationViolationDTO = apiApplicationViolationListDTO.applicationViolations
        .get(0);
    assertPolicyViolation(apiApplicationViolationDTO, appPolicyData, null);
  }

  @Test
  public void testGetPolicyViolations_ExcludeWaivedViolations() {
    PolicyData policyData =
        createPolicyTestData("scanId", null /* componentIdentifier */, "testhash", "testreason", "test.jar");
    tempEntity.newWaivedPolicyViolation(policyData.policyEvaluation1, policyData.orgPolicy,
        tempEntity.newWaiver(policyData.orgPolicy.getId(), policyData.organization.getId()));

    Set<String> policyIds = Collections.singleton(policyData.orgPolicy.getId());
    ApiApplicationViolationListDTOV2 apiApplicationViolationListDTO = apiPolicyViolationService
        .getPolicyViolations(policyIds, null, null, Collections.singleton(PolicyViolationType.ACTIVE));

    assertThat(apiApplicationViolationListDTO.applicationViolations).hasSize(1);
    ApiApplicationViolationDTOV2 apiApplicationViolationDTO = apiApplicationViolationListDTO.applicationViolations
        .get(0);
    assertPolicyViolation(apiApplicationViolationDTO, policyData, null);
  }

  @Test
  public void testGetPolicyViolations_includesOpenTime() {
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);
    var policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scanId");
    PolicyViolation v1 = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyViolation v2 = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyViolation v3 = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyViolation v4 = tempEntity.newPolicyViolation(policyEvaluation, policy);
    v2.setWaiveTime(DateUtils.addDays(v2.getOpenTime(), 1));
    v3.setLegacyViolationTime(DateUtils.addDays(v3.getOpenTime(), 1));
    v4.setFixTime(DateUtils.addDays(v4.getOpenTime(), 1));
    policyViolationDAO.update(v2);
    policyViolationDAO.update(v3);
    policyViolationDAO.update(v4);

    var result = apiPolicyViolationService.getPolicyViolations(Sets.newHashSet(policy.getId()), null, null,
        Collections.singleton(PolicyViolationType.ACTIVE));

    assertThat(result).isNotNull();
    assertThat(result.applicationViolations).hasSize(1);
    // getPolicyViolations only includes active violations so the other times should be null
    assertThat(result.applicationViolations.get(0).policyViolations).hasSize(1);
    var violation = result.applicationViolations.get(0).policyViolations.get(0);
    assertThat(violation.policyViolationId).isEqualTo(v1.getId());
    assertThat(violation.openTime).isNotNull().isEqualTo(v1.getOpenTime());
    assertThat(violation.waiveTime).isNull();
    assertThat(violation.fixTime).isNull();
    assertThat(violation.legacyViolationTime).isNull();
  }

  @Test
  public void testGetTransitivePolicyViolationsByOwnerStageComponent_InvalidStageId() {
    String invalidStageId = "InVaLiD";

    assertThatExceptionOfType(InvalidStageException.class)
        .isThrownBy(() -> apiPolicyViolationService.getTransitivePolicyViolationsByOwnerStageComponent(
            OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, invalidStageId, null,
            "pkg:maven/g/other@v?type=e", null))
        .withMessageContaining("Invalid stage id=" + invalidStageId);
  }

  @Test
  public void testGetTransitivePolicyViolationsForLastEvaluation_NoPolicyEvaluation() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> apiPolicyViolationService.getTransitivePolicyViolationsForLastEvaluation(
            "appId", "scanId", null, null, null))
        .withMessageContaining("Evaluation not found with application appId and scan scanId");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testGetTransitivePolicyViolationsForLastEvaluation_WithPolicyEvaluation() {
    Application application = tempEntity.newApplicationWithParent();
    String scanId = "scanId";
    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scanId);
    PolicyEvaluation latestPolicyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID,
        scanId, new Date(System.currentTimeMillis() + 1000));
    ComponentIdentifier component = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    String packageUrl = PackageUrlIdentifier.fromComponentIdentifier(component).getPackageUrl();

    ApiPolicyViolationServiceV2 spyService = spy(apiPolicyViolationService);
    ArgumentCaptor<List<PolicyEvaluation>> captor = ArgumentCaptor.forClass(List.class);

    doReturn(null).when(spyService)
        .getTransitivePolicyViolationsByComponent(eq(BuildStageType.ID), eq(component),
            eq(packageUrl), eq("hash"), captor.capture());

    spyService.getTransitivePolicyViolationsForLastEvaluation(application.getId(), scanId, component, packageUrl,
        "hash");

    verify(spyService).getTransitivePolicyViolationsByComponent(eq(BuildStageType.ID), eq(component), eq(packageUrl),
        eq("hash"), captor.capture());
    assertThat(captor.getValue().get(0).getId()).isEqualTo(latestPolicyEvaluation.getId());
  }

  @Test
  public void testGetTransitivePolicyViolationsByComponent_StageTypeNotLicensed() {
    StageTypeService mockStageTypeService = mock(StageTypeService.class);
    applyBeanFieldOverride(ApiPolicyViolationServiceV2.class, "stageTypeService", mockStageTypeService);
    when(mockStageTypeService.getLicensedStageTypes()).thenReturn(Collections.emptyList());

    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> apiPolicyViolationService.getTransitivePolicyViolationsByComponent(BuildStageType.ID, null,
            null, null, null))
        .withMessageContaining("Stage '" + BuildStageType.ID + "' is not supported by your license.");
  }

  @Test
  public void testGetTransitivePolicyViolationsByComponent_NoComponentIdentifierAndNoPackageUrlAndNoHash() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiPolicyViolationService.getTransitivePolicyViolationsByComponent(BuildStageType.ID, null,
            null, null, null))
        .withMessageContaining("componentIdentifier or packageUrl or hash must be specified.");
  }

  @Test
  public void testGetTransitivePolicyViolationsByComponent_UnknownHash() {
    Application application = tempEntity.newApplicationWithParent();
    String scanId = "scanId";
    List<PolicyEvaluation> policyEvaluations =
        Collections.singletonList(tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scanId));

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> apiPolicyViolationService.getTransitivePolicyViolationsByComponent(BuildStageType.ID, null,
            null, "unknown", policyEvaluations))
        .withMessageContaining("Component not found.");
  }

  @Test
  public void testGetTransitivePolicyViolationsByComponent_InvalidPackageUrl() {
    assertThatExceptionOfType(InvalidPackageURLException.class)
        .isThrownBy(() -> apiPolicyViolationService.getTransitivePolicyViolationsByComponent(BuildStageType.ID, null,
            "invalidPackageUrl", null, null))
        .withMessageContaining("Invalid package url");
  }

  @Test
  public void testGetTransitivePolicyViolationsByComponent_IncompletePackageUrl() {
    List<PolicyEvaluation> policyEvaluations = Collections.emptyList();
    assertThatExceptionOfType(InvalidPackageURLException.class)
        .isThrownBy(() -> apiPolicyViolationService.getTransitivePolicyViolationsByComponent(BuildStageType.ID, null,
            "pkg:maven/g/a@v", null, policyEvaluations))
        .withMessageContaining("The following coordinates are missing for given format: [type]");
  }

  @Test
  public void testGetTransitivePolicyViolationsByComponent_IncompleteComponentIdentifier() {
    List<PolicyEvaluation> policyEvaluations = Collections.emptyList();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", null, null);
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiPolicyViolationService.getTransitivePolicyViolationsByComponent(BuildStageType.ID,
            componentIdentifier, null, null, policyEvaluations))
        .withMessageContaining("The following coordinates are missing for given format: [extension]");
  }

  @Test
  public void testGetTransitivePolicyViolationsByComponent_MissingReport() {
    Application application = tempEntity.newApplicationWithParent();
    String scanId = "scanId";
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scanId);
    ComponentIdentifier direct = ComponentIdentifier.createMavenCoordinates("g", "direct2", "v", "", "e");

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> apiPolicyViolationService
        .getTransitivePolicyViolationsByComponent(BuildStageType.ID, direct, null, null,
            Collections.singletonList(policyEvaluation)))
        .withMessageContaining("Component not found.");
  }

  @Test
  public void testGetTransitivePolicyViolationsByComponent_ComponentNotFound_Application() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    String scanId = "scanId";
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scanId);
    ReportTestUtils.createReportFile(application.getId(), scanId,
        zipReportDir("/ApiPolicyViolationServiceV2Test/report", tempDir), insightWork);

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> apiPolicyViolationService
        .getTransitivePolicyViolationsByComponent(BuildStageType.ID, null, "pkg:maven/g/other@v?type=e", null,
            Collections.singletonList(policyEvaluation)))
        .withMessageContaining("Component not found.");
  }

  @Test
  public void testGetTransitivePolicyViolationsByAppScanComponent_RootOrganization() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> apiPolicyViolationService
        .getTransitivePolicyViolationsByAppScanComponent(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
            "scanId", null, "pkg:maven/g/other@v?type=e", null))
        .withMessageContaining("scanId can only be specified for an application.");
  }

  @Test
  public void testGetTransitivePolicyViolationsByAppScanComponent_Organization_ScanId() {
    Organization organization = tempEntity.newOrganization();
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> apiPolicyViolationService
        .getTransitivePolicyViolationsByAppScanComponent(organization.getType(), organization.getPublicId(), "scanId",
            null, "pkg:maven/g/other@v?type=e", null))
        .withMessageContaining("scanId can only be specified for an application.");
  }

  @Test
  public void testGetTransitivePolicyViolationsByAppScanComponent_Application_ScanId_NotFound() {
    Application app = tempEntity.newApplicationWithParent();
    String scanId = "someScanId";
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> apiPolicyViolationService
        .getTransitivePolicyViolationsByAppScanComponent(OwnerType.APPLICATION, app.getPublicId(), scanId,
            null, "pkg:maven/g/other@v?type=e", null))
        .withMessageContaining("scanId " + scanId + " not found for application " + app.getPublicId());
  }

  @Test
  public void testGetTransitivePolicyViolationsByAppScanComponent_Application() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    String oldScanId = "oldScanId";
    PolicyEvaluation oldPolicyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, oldScanId);
    Policy policy = tempEntity.newPolicy();
    ComponentIdentifier direct2 = ComponentIdentifier.createMavenCoordinates("g", "direct2", "v", "", "e");
    ComponentIdentifier transitive2 = ComponentIdentifier.createMavenCoordinates("g", "transitive2", "v", "", "e");
    ComponentIdentifier transitive22 = ComponentIdentifier.createMavenCoordinates("g", "transitive22", "v", "", "e");
    PolicyViolation direct2PolicyViolation =
        tempEntity.newPolicyViolation(oldPolicyEvaluation, policy, direct2, "hash2");
    PolicyViolation transitive2PolicyViolation =
        tempEntity.newPolicyViolation(oldPolicyEvaluation, policy, transitive2, "hash22");
    PolicyViolation transitive22PolicyViolation =
        tempEntity.newPolicyViolation(oldPolicyEvaluation, policy, transitive22, "hash222");
    ReportTestUtils.createReportFile(application.getId(), oldScanId,
        zipReportDir("/ApiPolicyViolationServiceV2Test/report", tempDir), insightWork);
    ReportHelper.createPolicyThreats(insightWork, application.getId(), oldScanId,
        Arrays.asList(direct2PolicyViolation, transitive2PolicyViolation, transitive22PolicyViolation));
    String newScanId = "newScanId";
    PolicyEvaluation newPolicyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, newScanId);
    ComponentIdentifier transitive1 = ComponentIdentifier.createMavenCoordinates("g", "transitive1", "v", "", "e");
    PolicyViolation transitive1PolicyViolation =
        tempEntity.newPolicyViolation(newPolicyEvaluation, policy, transitive1, "hash21");
    ReportTestUtils.createReportFile(application.getId(), newScanId,
        zipReportDir("/ApiPolicyViolationServiceV2Test/report", tempDir), insightWork);
    ReportHelper.createPolicyThreats(insightWork, application.getId(), newScanId,
        Collections.singletonList(transitive1PolicyViolation));

    ApiComponentTransitivePolicyViolationsDTO result = apiPolicyViolationService
        .getTransitivePolicyViolationsByAppScanComponent(OwnerType.APPLICATION, application.getId(), oldScanId, null,
            null, "hash2");

    assertThat(result).isNotNull();
    assertThat(result.componentIdentifier).usingRecursiveComparison()
        .isEqualTo(ApiComponentIdentifierDTOV2.fromComponentIdentifier(direct2));
    assertThat(result.packageUrl).isEqualTo(PackageUrlIdentifier.fromComponentIdentifier(direct2).getPackageUrl());
    assertThat(result.hash).isEqualTo("hash2");
    assertThat(result.displayName).isEqualTo("g : direct2 : v");
    assertThat(result.isInnerSource).isFalse();
    Component expectedTransitiveComponent2 = new Component();
    expectedTransitiveComponent2.setHash("hash22");
    expectedTransitiveComponent2.setDisplayName("g : transitive2 : v");
    Component expectedTransitiveComponent22 = new Component();
    expectedTransitiveComponent22.setHash("hash222");
    expectedTransitiveComponent22.setDisplayName("g : transitive22 : v");
    assertThat(result.transitivePolicyViolations).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(
            ApiStagePolicyViolationComponentDTO
                .fromPolicyViolationAndComponent(Pair.of(transitive2PolicyViolation, expectedTransitiveComponent2)),
            ApiStagePolicyViolationComponentDTO
                .fromPolicyViolationAndComponent(Pair.of(transitive22PolicyViolation, expectedTransitiveComponent22)));

    result = apiPolicyViolationService.getTransitivePolicyViolationsByAppScanComponent(OwnerType.APPLICATION,
        application.getId(), newScanId, null, null, "hash2");
    assertThat(result).isNotNull();
    assertThat(result.componentIdentifier).usingRecursiveComparison()
        .isEqualTo(ApiComponentIdentifierDTOV2.fromComponentIdentifier(direct2));
    assertThat(result.packageUrl).isEqualTo(PackageUrlIdentifier.fromComponentIdentifier(direct2).getPackageUrl());
    assertThat(result.hash).isEqualTo("hash2");
    assertThat(result.displayName).isEqualTo("g : direct2 : v");
    assertThat(result.isInnerSource).isFalse();
    Component expectedTransitiveComponent1 = new Component();
    expectedTransitiveComponent1.setHash("hash21");
    expectedTransitiveComponent1.setDisplayName("g : transitive1 : v");
    assertThat(result.transitivePolicyViolations).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(
            ApiStagePolicyViolationComponentDTO
                .fromPolicyViolationAndComponent(Pair.of(transitive1PolicyViolation, expectedTransitiveComponent1)));
  }

  @Test
  public void testGetTransitivePolicyViolationsByComponent_ComponentNotFound_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    String scanId = "scanId";
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scanId);
    ReportTestUtils.createReportFile(application.getId(), scanId,
        zipReportDir("/ApiPolicyViolationServiceV2Test/report", tempDir), insightWork);

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> apiPolicyViolationService
        .getTransitivePolicyViolationsByComponent(BuildStageType.ID, null, "pkg:maven/g/other@v?type=e", null,
            Collections.singletonList(policyEvaluation)))
        .withMessageContaining("Component not found.");
  }

  @Test
  public void testGetTransitivePolicyViolations_ByComponentIdentifier() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    testGetTransitivePolicyViolations(application,
        ComponentIdentifier.createMavenCoordinates("g", "direct2", "v", "", "e"), null, null);
  }

  @Test
  public void testGetTransitivePolicyViolationsByComponent_ByComponentIdentifier() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    testGetTransitivePolicyViolationsByComponent(application,
        ComponentIdentifier.createMavenCoordinates("g", "direct2", "v", "", "e"), null, null);
  }

  @Test
  public void testGetTransitivePolicyViolations_ByPackageUrl() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    testGetTransitivePolicyViolations(application, null, "pkg:maven/g/direct2@v?type=e", null);
  }

  @Test
  public void testGetTransitivePolicyViolationsByComponent_ByPackageUrl() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    testGetTransitivePolicyViolationsByComponent(application, null, "pkg:maven/g/direct2@v?type=e", null);
  }

  @Test
  public void testGetTransitivePolicyViolations_ByHash() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    testGetTransitivePolicyViolations(application, null, null, "hash2");
  }

  @Test
  public void testGetTransitivePolicyViolationsByComponent_ByHash() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    testGetTransitivePolicyViolationsByComponent(application, null, null, "hash2");
  }

  @Test
  public void testGetTransitivePolicyViolations_ByComponentIdentifierAndPackageUrl() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    testGetTransitivePolicyViolations(application,
        ComponentIdentifier.createMavenCoordinates("g", "direct2", "v", "", "e"), "invalidPackageUrl", null);
  }

  @Test
  public void testGetTransitivePolicyViolationsByComponent_ByComponentIdentifierAndPackageUrl() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    testGetTransitivePolicyViolationsByComponent(application,
        ComponentIdentifier.createMavenCoordinates("g", "direct2", "v", "", "e"), "invalidPackageUrl", null);
  }

  @Test
  public void testGetTransitivePolicyViolations_ByComponentIdentifierAndHash() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    testGetTransitivePolicyViolations(application,
        ComponentIdentifier.createMavenCoordinates("g", "direct2", "v", "", "e"), null, "unknown");
  }

  @Test
  public void testGetTransitivePolicyViolationsByComponent_ByComponentIdentifierAndHash() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    testGetTransitivePolicyViolationsByComponent(application,
        ComponentIdentifier.createMavenCoordinates("g", "direct2", "v", "", "e"), null, "unknown");
  }

  @Test
  public void testGetTransitivePolicyViolations_ByPackageUrlAndHash() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    testGetTransitivePolicyViolations(application, null, "pkg:maven/g/direct2@v?type=e", "unknown");
  }

  @Test
  public void testGetTransitivePolicyViolationsByComponent_ByPackageUrlAndHash() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    testGetTransitivePolicyViolationsByComponent(application, null, "pkg:maven/g/direct2@v?type=e", "unknown");
  }

  @Test
  public void testGetTransitivePolicyViolations_ByComponentIdentifierAndPackageUrlAndHash() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    testGetTransitivePolicyViolations(application,
        ComponentIdentifier.createMavenCoordinates("g", "direct2", "v", "", "e"), "invalidPackageUrl", "unknown");
  }

  @Test
  public void testGetTransitivePolicyViolationsByComponent_ByComponentIdentifierAndPackageUrlAndHash() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    testGetTransitivePolicyViolationsByComponent(application,
        ComponentIdentifier.createMavenCoordinates("g", "direct2", "v", "", "e"), "invalidPackageUrl", "unknown");
  }

  private void testGetTransitivePolicyViolations(
      Application application,
      ComponentIdentifier componentIdentifier,
      String packageUrl,
      String hash) throws Exception
  {
    String scanId = "scanId";
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scanId);
    Policy policy = tempEntity.newPolicy();
    ComponentIdentifier direct2 = ComponentIdentifier.createMavenCoordinates("g", "direct2", "v", "", "e");
    ComponentIdentifier transitive2 = ComponentIdentifier.createMavenCoordinates("g", "transitive2", "v", "", "e");
    ComponentIdentifier transitive22 = ComponentIdentifier.createMavenCoordinates("g", "transitive22", "v", "", "e");
    PolicyViolation direct2PolicyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy, direct2, "hash2");
    PolicyViolation transitive2PolicyViolation =
        tempEntity.newPolicyViolation(policyEvaluation, policy, transitive2, "hash22");
    PolicyViolation transitive22PolicyViolation =
        tempEntity.newPolicyViolation(policyEvaluation, policy, transitive22, "hash222");
    ReportTestUtils.createReportFile(application.getId(), scanId,
        zipReportDir("/ApiPolicyViolationServiceV2Test/report", tempDir), insightWork);
    ReportHelper.createPolicyThreats(insightWork, application.getId(), scanId,
        Arrays.asList(direct2PolicyViolation, transitive2PolicyViolation, transitive22PolicyViolation));

    ApiComponentTransitivePolicyViolationsDTO result = apiPolicyViolationService.getTransitivePolicyViolations(
        BuildStageType.ID, componentIdentifier, packageUrl, hash, Collections.singletonList(policyEvaluation));

    assertThat(result).isNotNull();
    assertThat(result.componentIdentifier).usingRecursiveComparison()
        .isEqualTo(ApiComponentIdentifierDTOV2.fromComponentIdentifier(direct2));
    assertThat(result.packageUrl).isEqualTo(PackageUrlIdentifier.fromComponentIdentifier(direct2).getPackageUrl());
    assertThat(result.hash).isEqualTo("hash2");
    assertThat(result.displayName).isEqualTo("g : direct2 : v");
    assertThat(result.isInnerSource).isFalse();
    Component expectedTransitiveComponent2 = new Component();
    expectedTransitiveComponent2.setHash("hash22");
    expectedTransitiveComponent2.setDisplayName("g : transitive2 : v");
    Component expectedTransitiveComponent22 = new Component();
    expectedTransitiveComponent22.setHash("hash222");
    expectedTransitiveComponent22.setDisplayName("g : transitive22 : v");
    assertThat(result.transitivePolicyViolations).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(
            ApiStagePolicyViolationComponentDTO
                .fromPolicyViolationAndComponent(Pair.of(transitive2PolicyViolation, expectedTransitiveComponent2)),
            ApiStagePolicyViolationComponentDTO
                .fromPolicyViolationAndComponent(Pair.of(transitive22PolicyViolation, expectedTransitiveComponent22)));
  }

  private void testGetTransitivePolicyViolationsByComponent(
      Application application,
      ComponentIdentifier componentIdentifier,
      String packageUrl,
      String hash) throws Exception
  {
    String scanId = "scanId";
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scanId);
    Policy policy = tempEntity.newPolicy();
    ComponentIdentifier direct2 = ComponentIdentifier.createMavenCoordinates("g", "direct2", "v", "", "e");
    ComponentIdentifier transitive2 = ComponentIdentifier.createMavenCoordinates("g", "transitive2", "v", "", "e");
    ComponentIdentifier transitive22 = ComponentIdentifier.createMavenCoordinates("g", "transitive22", "v", "", "e");
    PolicyViolation direct2PolicyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy, direct2, "hash2");
    PolicyViolation transitive2PolicyViolation =
        tempEntity.newPolicyViolation(policyEvaluation, policy, transitive2, "hash22");
    PolicyViolation transitive22PolicyViolation =
        tempEntity.newPolicyViolation(policyEvaluation, policy, transitive22, "hash222");
    ReportTestUtils.createReportFile(application.getId(), scanId,
        zipReportDir("/ApiPolicyViolationServiceV2Test/report", tempDir), insightWork);
    ReportHelper.createPolicyThreats(insightWork, application.getId(), scanId,
        Arrays.asList(direct2PolicyViolation, transitive2PolicyViolation, transitive22PolicyViolation));

    Pair<Component, List<Pair<PolicyViolation, Component>>> result =
        apiPolicyViolationService.getTransitivePolicyViolationsByComponent(BuildStageType.ID, componentIdentifier,
            packageUrl, hash, Collections.singletonList(policyEvaluation));

    assertThat(result).isNotNull();

    Component component = result.getLeft();
    List<Pair<PolicyViolation, Component>> violations = result.getRight();

    assertThat(component.getComponentIdentifier()).isEqualTo(direct2);
    assertThat(component.getHash()).isEqualTo("hash2");
    assertThat(component.getDisplayName()).isEqualTo("g : direct2 : v");
    assertThat(BooleanUtils.toBoolean(component.getInnerSource())).isFalse();
    Component expectedTransitiveComponent2 = new Component();
    expectedTransitiveComponent2.setHash("hash22");
    expectedTransitiveComponent2.setDisplayName("g : transitive2 : v");
    Component expectedTransitiveComponent22 = new Component();
    expectedTransitiveComponent22.setHash("hash222");
    expectedTransitiveComponent22.setDisplayName("g : transitive22 : v");

    assertThat(violations.get(0).getLeft().getId()).isEqualTo(transitive2PolicyViolation.getId());
    assertThat(violations.get(0).getRight().getHash()).isEqualTo(expectedTransitiveComponent2.getHash());
    assertThat(violations.get(0).getRight().getDisplayName()).isEqualTo(expectedTransitiveComponent2.getDisplayName());

    assertThat(violations.get(1).getLeft().getId()).isEqualTo(transitive22PolicyViolation.getId());
    assertThat(violations.get(1).getRight().getHash()).isEqualTo(expectedTransitiveComponent22.getHash());
    assertThat(violations.get(1).getRight().getDisplayName()).isEqualTo(expectedTransitiveComponent22.getDisplayName());
  }

  @Test
  public void testGetTransitivePolicyViolationsByOwnerStageComponent_MultipleApplications() throws Exception {
    Organization organization = tempEntity.newOrganization();
    Application application1 = tempEntity.newApplication(organization.getId());
    Application application2 = tempEntity.newApplication(organization.getId());
    String scanId = "scanId";
    PolicyEvaluation policyEvaluation1 =
        tempEntity.newPolicyEvaluation(application1.getId(), BuildStageType.ID, scanId);
    PolicyEvaluation policyEvaluation2 =
        tempEntity.newPolicyEvaluation(application2.getId(), BuildStageType.ID, scanId);
    Policy policy1 = tempEntity.newPolicy(organization.getId(), "policy1", 9);
    Policy policy2 = tempEntity.newPolicy(organization.getId(), "policy2", 5);
    ComponentIdentifier direct2 = ComponentIdentifier.createMavenCoordinates("g", "direct2", "v", "", "e");
    ComponentIdentifier transitive1 = ComponentIdentifier.createMavenCoordinates("g", "transitive1", "v", "", "e");
    PolicyViolation transitive1PolicyViolation1 =
        tempEntity.newPolicyViolation(policyEvaluation1, policy1, transitive1, "hash21");
    PolicyViolation transitive1PolicyViolation2 =
        tempEntity.newPolicyViolation(policyEvaluation2, policy2, transitive1, "hash21");
    ReportTestUtils.createReportFile(application1.getId(), scanId,
        zipReportDir("/ApiPolicyViolationServiceV2Test/report", tempDir), insightWork);
    ReportHelper.createPolicyThreats(insightWork, application1.getId(), scanId,
        Collections.singletonList(transitive1PolicyViolation1));
    ReportTestUtils.createReportFile(application2.getId(), scanId,
        zipReportDir("/ApiPolicyViolationServiceV2Test/report", tempDir), insightWork);
    ReportHelper.createPolicyThreats(insightWork, application2.getId(), scanId,
        Collections.singletonList(transitive1PolicyViolation2));

    ApiComponentTransitivePolicyViolationsDTO result = apiPolicyViolationService
        .getTransitivePolicyViolationsByOwnerStageComponent(OwnerType.ORGANIZATION, organization.getId(),
            BuildStageType.ID, direct2, null, null);

    assertThat(result).isNotNull();
    assertThat(result.componentIdentifier).usingRecursiveComparison()
        .isEqualTo(ApiComponentIdentifierDTOV2.fromComponentIdentifier(direct2));
    assertThat(result.packageUrl).isEqualTo(PackageUrlIdentifier.fromComponentIdentifier(direct2).getPackageUrl());
    assertThat(result.hash).isEqualTo("hash2");
    assertThat(result.displayName).isEqualTo("g : direct2 : v");
    assertThat(result.isInnerSource).isFalse();
    Component expectedTransitiveComponent1 = new Component();
    expectedTransitiveComponent1.setHash("hash21");
    expectedTransitiveComponent1.setDisplayName("g : transitive1 : v");
    assertThat(result.transitivePolicyViolations).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(
            ApiStagePolicyViolationComponentDTO
                .fromPolicyViolationAndComponent(Pair.of(transitive1PolicyViolation1, expectedTransitiveComponent1)),
            ApiStagePolicyViolationComponentDTO
                .fromPolicyViolationAndComponent(Pair.of(transitive1PolicyViolation2, expectedTransitiveComponent1)));
  }

  @Test
  public void testGetTransitivePolicyViolationsByOwnerStageComponent_MultipleOrganizations() throws Exception {
    Application application1 = tempEntity.newApplicationWithParent();
    Application application2 = tempEntity.newApplicationWithParent();
    String scanId = "scanId";
    PolicyEvaluation policyEvaluation1 =
        tempEntity.newPolicyEvaluation(application1.getId(), BuildStageType.ID, scanId);
    PolicyEvaluation policyEvaluation2 =
        tempEntity.newPolicyEvaluation(application2.getId(), BuildStageType.ID, scanId);
    Policy policy1 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "policy1", 9);
    Policy policy2 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "policy2", 5);
    ComponentIdentifier direct2 = ComponentIdentifier.createMavenCoordinates("g", "direct2", "v", "", "e");
    ComponentIdentifier transitive1 = ComponentIdentifier.createMavenCoordinates("g", "transitive1", "v", "", "e");
    PolicyViolation transitive1PolicyViolation1 =
        tempEntity.newPolicyViolation(policyEvaluation1, policy1, transitive1, "hash21");
    PolicyViolation transitive1PolicyViolation2 =
        tempEntity.newPolicyViolation(policyEvaluation2, policy2, transitive1, "hash21");
    ReportTestUtils.createReportFile(application1.getId(), scanId,
        zipReportDir("/ApiPolicyViolationServiceV2Test/report", tempDir), insightWork);
    ReportHelper.createPolicyThreats(insightWork, application1.getId(), scanId,
        Collections.singletonList(transitive1PolicyViolation1));
    ReportTestUtils.createReportFile(application2.getId(), scanId,
        zipReportDir("/ApiPolicyViolationServiceV2Test/report", tempDir), insightWork);
    ReportHelper.createPolicyThreats(insightWork, application2.getId(), scanId,
        Collections.singletonList(transitive1PolicyViolation2));

    ApiComponentTransitivePolicyViolationsDTO result = apiPolicyViolationService
        .getTransitivePolicyViolationsByOwnerStageComponent(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
            BuildStageType.ID, direct2, null, null);

    assertThat(result).isNotNull();
    assertThat(result.componentIdentifier).usingRecursiveComparison()
        .isEqualTo(ApiComponentIdentifierDTOV2.fromComponentIdentifier(direct2));
    assertThat(result.packageUrl).isEqualTo(PackageUrlIdentifier.fromComponentIdentifier(direct2).getPackageUrl());
    assertThat(result.hash).isEqualTo("hash2");
    assertThat(result.displayName).isEqualTo("g : direct2 : v");
    assertThat(result.isInnerSource).isFalse();
    Component expectedTransitiveComponent1 = new Component();
    expectedTransitiveComponent1.setHash("hash21");
    expectedTransitiveComponent1.setDisplayName("g : transitive1 : v");
    assertThat(result.transitivePolicyViolations).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(
            ApiStagePolicyViolationComponentDTO
                .fromPolicyViolationAndComponent(Pair.of(transitive1PolicyViolation1, expectedTransitiveComponent1)),
            ApiStagePolicyViolationComponentDTO
                .fromPolicyViolationAndComponent(Pair.of(transitive1PolicyViolation2, expectedTransitiveComponent1)));
  }

  @Test
  public void testGetTransitivePolicyViolations_InnerSourceAndNone() throws Exception {
    ComponentIdentifier direct1 = ComponentIdentifier.createMavenCoordinates("g", "direct1", "v", "", "e");
    PolicyEvaluation policyEvaluation = setupTestGetTransitivePolicyViolations_InnerSourceAndNone(direct1);

    ApiComponentTransitivePolicyViolationsDTO result = apiPolicyViolationService.getTransitivePolicyViolations(
        BuildStageType.ID, direct1, null, null, Collections.singletonList(policyEvaluation));

    assertThat(result).isNotNull();
    assertThat(result.componentIdentifier).usingRecursiveComparison()
        .isEqualTo(ApiComponentIdentifierDTOV2.fromComponentIdentifier(direct1));
    assertThat(result.packageUrl).isEqualTo(PackageUrlIdentifier.fromComponentIdentifier(direct1).getPackageUrl());
    assertThat(result.hash).isEqualTo("hash1");
    assertThat(result.displayName).isEqualTo("g : direct1 : v");
    assertThat(result.isInnerSource).isTrue();
    assertThat(result.transitivePolicyViolations).isEmpty();
  }

  @Test
  public void testGetTransitivePolicyViolationsByComponent_InnerSourceAndNone() throws Exception {
    ComponentIdentifier direct1 = ComponentIdentifier.createMavenCoordinates("g", "direct1", "v", "", "e");
    PolicyEvaluation policyEvaluation = setupTestGetTransitivePolicyViolations_InnerSourceAndNone(direct1);

    Pair<Component, List<Pair<PolicyViolation, Component>>> result =
        apiPolicyViolationService.getTransitivePolicyViolationsByComponent(BuildStageType.ID, direct1, null, null,
            Collections.singletonList(policyEvaluation));

    assertThat(result).isNotNull();

    Component component = result.getLeft();
    List<Pair<PolicyViolation, Component>> violations = result.getRight();

    assertThat(component.getComponentIdentifier()).isEqualTo(direct1);
    assertThat(component.getHash()).isEqualTo("hash1");
    assertThat(component.getDisplayName()).isEqualTo("g : direct1 : v");
    assertThat(BooleanUtils.toBoolean(component.getInnerSource())).isTrue();
    assertThat(violations).isEmpty();
  }

  private PolicyEvaluation setupTestGetTransitivePolicyViolations_InnerSourceAndNone(
      ComponentIdentifier componentIdentifier) throws Exception
  {
    Application application = tempEntity.newApplicationWithParent();
    String scanId = "scanId";
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scanId);
    Policy policy = tempEntity.newPolicy();
    PolicyViolation direct1PolicyViolation =
        tempEntity.newPolicyViolation(policyEvaluation, policy, componentIdentifier, "hash1");
    ReportTestUtils.createReportFile(application.getId(), scanId,
        zipReportDir("/ApiPolicyViolationServiceV2Test/report", tempDir), insightWork);
    ReportHelper.createPolicyThreats(insightWork, application.getId(), scanId,
        Collections.singletonList(direct1PolicyViolation));
    return policyEvaluation;
  }

  @Test
  public void testGetTransitivePolicyViolations_Unknown() throws Exception {
    PolicyEvaluation policyEvaluation = setupTestGetTransitivePolicyViolations_Unknown();

    ApiComponentTransitivePolicyViolationsDTO result = apiPolicyViolationService
        .getTransitivePolicyViolations(BuildStageType.ID, null, null, "81399f9f3278d8615a7c",
            Collections.singletonList(policyEvaluation));

    assertThat(result).isNotNull();
    assertThat(result.componentIdentifier).isNull();
    assertThat(result.packageUrl).isNull();
    assertThat(result.hash).isEqualTo("81399f9f3278d8615a7c");
    assertThat(result.displayName).isEqualTo("unknown.zip");
    assertThat(result.isInnerSource).isFalse();
    assertThat(result.transitivePolicyViolations).isEmpty();
  }

  @Test
  public void testGetTransitivePolicyViolationsByComponent_Unknown() throws Exception {
    PolicyEvaluation policyEvaluation = setupTestGetTransitivePolicyViolations_Unknown();

    Pair<Component, List<Pair<PolicyViolation, Component>>> result =
        apiPolicyViolationService.getTransitivePolicyViolationsByComponent(BuildStageType.ID, null, null,
            "81399f9f3278d8615a7c", Collections.singletonList(policyEvaluation));

    assertThat(result).isNotNull();

    Component component = result.getLeft();
    List<Pair<PolicyViolation, Component>> violations = result.getRight();

    assertThat(component.getComponentIdentifier()).isNull();
    assertThat(component.getHash()).isEqualTo("81399f9f3278d8615a7c");
    assertThat(component.getDisplayName()).isEqualTo("unknown.zip");
    assertThat(BooleanUtils.toBoolean(component.getInnerSource())).isFalse();
    assertThat(violations).isEmpty();
  }

  public PolicyEvaluation setupTestGetTransitivePolicyViolations_Unknown() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    String scanId = "scanId";
    ReportTestUtils.createReportFile(application.getId(), scanId,
        zipReportDir("/ApiPolicyViolationServiceV2Test/report", tempDir), insightWork);
    return tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scanId);
  }

  @Test
  public void testSort() {
    ComponentIdentifier c1 = ComponentIdentifier.createMavenCoordinates("a", "a", "a", "a", "a");
    ComponentIdentifier c2 = ComponentIdentifier.createMavenCoordinates("z", "z", "z", "z", "z");
    ApiComponentTransitivePolicyViolationsDTO result;

    result = create(create(0, null), create(0, null));
    apiPolicyViolationService.sort(result.transitivePolicyViolations);
    assertThat(result).usingRecursiveComparison().isEqualTo(create(create(0, null), create(0, null)));

    result = create(create(0, c1), create(0, null));
    apiPolicyViolationService.sort(result.transitivePolicyViolations);
    assertThat(result).usingRecursiveComparison().isEqualTo(create(create(0, c1), create(0, null)));

    result = create(create(0, null), create(0, c1));
    apiPolicyViolationService.sort(result.transitivePolicyViolations);
    assertThat(result).usingRecursiveComparison().isEqualTo(create(create(0, c1), create(0, null)));

    result = create(create(0, c1), create(0, c1));
    apiPolicyViolationService.sort(result.transitivePolicyViolations);
    assertThat(result).usingRecursiveComparison().isEqualTo(create(create(0, c1), create(0, c1)));

    result = create(create(0, c1), create(0, c2));
    apiPolicyViolationService.sort(result.transitivePolicyViolations);
    assertThat(result).usingRecursiveComparison().isEqualTo(create(create(0, c1), create(0, c2)));

    result = create(create(0, c2), create(0, c1));
    apiPolicyViolationService.sort(result.transitivePolicyViolations);
    assertThat(result).usingRecursiveComparison().isEqualTo(create(create(0, c1), create(0, c2)));

    result = create(create(4, null), create(9, null));
    apiPolicyViolationService.sort(result.transitivePolicyViolations);
    assertThat(result).usingRecursiveComparison().isEqualTo(create(create(9, null), create(4, null)));

    result = create(create(4, null), create(9, c1));
    apiPolicyViolationService.sort(result.transitivePolicyViolations);
    assertThat(result).usingRecursiveComparison().isEqualTo(create(create(9, c1), create(4, null)));

    result = create(create(4, c1), create(9, null));
    apiPolicyViolationService.sort(result.transitivePolicyViolations);
    assertThat(result).usingRecursiveComparison().isEqualTo(create(create(9, null), create(4, c1)));

    result = create(create(4, c1), create(9, c1));
    apiPolicyViolationService.sort(result.transitivePolicyViolations);
    assertThat(result).usingRecursiveComparison().isEqualTo(create(create(9, c1), create(4, c1)));

    result = create(create(4, c1), create(9, c2));
    apiPolicyViolationService.sort(result.transitivePolicyViolations);
    assertThat(result).usingRecursiveComparison().isEqualTo(create(create(9, c2), create(4, c1)));

    result = create(create(4, c2), create(9, c1));
    apiPolicyViolationService.sort(result.transitivePolicyViolations);
    assertThat(result).usingRecursiveComparison().isEqualTo(create(create(9, c1), create(4, c2)));
  }

  @Test
  public void testGetTransitiveComponentsByAppScanComponent_ReportNotFound() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> apiPolicyViolationService.getTransitiveComponentsByAppScanComponent("appId", "scanId", null, null, null))
        .withMessageContaining("Component not found.");
  }

  @Test
  public void testGetTransitiveComponentsByAppScanComponent_ComponentNotInReport() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    String scanId = "scanId";
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId);
    ReportTestUtils.createReportFile(app.getId(), scanId,
        zipReportDir("/ApiPolicyViolationServiceV2Test/report", tempDir), insightWork);

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> apiPolicyViolationService.getTransitiveComponentsByAppScanComponent(app.getId(), scanId, null, null,
            "hash3"))
        .withMessageContaining("Component not found.");
  }

  @Test
  public void testGetTransitiveComponentsByAppScanComponent_ComponentWithoutTransitiveComponents() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    String scanId = "scanId";
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId);
    ReportTestUtils.createReportFile(app.getId(), scanId,
        zipReportDir("/ApiPolicyViolationServiceV2Test/report", tempDir), insightWork);

    List<Component> transitiveComponentsByAppScanComponent =
        apiPolicyViolationService.getTransitiveComponentsByAppScanComponent(app.getId(), scanId, null, null, "hash1");

    assertThat(transitiveComponentsByAppScanComponent).isEmpty();
  }

  @Test
  public void testGetTransitiveComponentsByAppScanComponent_ComponentWithTransitiveComponents_ByComponentIdentifier() throws Exception {
    testGetTransitiveComponentsByAppScanComponent_ComponentWithTransitiveComponents(
        ComponentIdentifier.createMavenCoordinates("g", "direct2", "v", "", "e"), null, null);
  }

  @Test
  public void testGetTransitiveComponentsByAppScanComponent_ComponentWithTransitiveComponents_ByPackageUrl() throws Exception {
    testGetTransitiveComponentsByAppScanComponent_ComponentWithTransitiveComponents(
        null, "pkg:maven/g/direct2@v?type=e", null);
  }

  @Test
  public void testGetTransitiveComponentsByAppScanComponent_ComponentWithTransitiveComponents_ByHash() throws Exception {
    testGetTransitiveComponentsByAppScanComponent_ComponentWithTransitiveComponents(
        null, null, "hash2");
  }

  @Test
  public void testGetInnerComponentsByParentComponentIdentifier_NoComponents() {
    List<Component> innerComponentsByParentComponentIdentifier =
        apiPolicyViolationService.getInnerComponentsByParentComponentIdentifier(null, null);
    assertThat(innerComponentsByParentComponentIdentifier).isEmpty();
  }

  @Test
  public void testGetInnerComponentsByParentComponentIdentifier_SingleInnerComponents() {
    Set<InnerSourceData> innerSourceData =
        Set.of(new InnerSourceData(null, null, PACKAGE_URL_MAVEN_V1 + "?type=jar&classifier=javadoc"));
    Component component = new Component();
    component.setInnerSourceData(innerSourceData);

    ComponentIdentifier parentComponentIdentifier =
        ComponentIdentifierAdapter.toComponentIdentifier(PACKAGE_URL_MAVEN_V1 + "?type=jar&classifier=javadoc");
    List<Component> components = List.of(component);

    List<Component> innerComponentsByParentComponentIdentifier =
        apiPolicyViolationService.getInnerComponentsByParentComponentIdentifier(components, parentComponentIdentifier);
    assertThat(innerComponentsByParentComponentIdentifier)
        .hasSize(1)
        .containsOnly(component);
  }

  @Test
  public void testGetInnerComponentsByParentComponentIdentifier_MultipleInnerComponents() {
    Set<InnerSourceData> innerSourceData =
        Set.of(new InnerSourceData(null, null, PACKAGE_URL_MAVEN_V1 + "?type=jar&classifier=javadoc"),
            new InnerSourceData(null, null, PACKAGE_URL_MAVEN_V1 + "?type=jar&classifier="));
    Component component = new Component();
    component.setInnerSourceData(innerSourceData);

    Set<InnerSourceData> innerSourceData1 =
        Set.of(new InnerSourceData(null, null, PACKAGE_URL_MAVEN_V1 + "?type=jar&classifier=javadoc"),
            new InnerSourceData(null, null, PACKAGE_URL_MAVEN_V2 + "?type=jar"),
            new InnerSourceData(null, null, "pkg:maven/org.apache.commons/commons-lang3@3.12.0?type=jar"));
    Component component1 = new Component();
    component1.setInnerSourceData(innerSourceData1);

    Set<InnerSourceData> innerSourceData2 =
        Set.of(new InnerSourceData(null, null, PACKAGE_URL_NUGET + "?type=test"),
            new InnerSourceData(null, null, "pkg:gem/rails@6.1.4?type=test"),
            new InnerSourceData(null, null, "pkg:npm/lodash@4.17.21?type=test"));
    Component component2 = new Component();
    component2.setInnerSourceData(innerSourceData2);

    List<Component> components = List.of(component, component1, component2);
    ComponentIdentifier parentComponentIdentifier =
        ComponentIdentifierAdapter.toComponentIdentifier(PACKAGE_URL_MAVEN_V1 + "?type=jar&classifier=javadoc");

    List<Component> innerComponentsByParentComponentIdentifier =
        apiPolicyViolationService.getInnerComponentsByParentComponentIdentifier(components, parentComponentIdentifier);
    assertThat(innerComponentsByParentComponentIdentifier)
        .hasSize(2)
        .containsExactlyInAnyOrder(component, component1);
  }

  @Test
  public void testGetPolicyViolations_WithDifferentTypes() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Policy orgPolicy = tempEntity.newPolicy(org);
    PolicyEvaluation pe = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1");

    PolicyViolation activeViolation = tempEntity.newPolicyViolation(pe, orgPolicy, "g1", "a1", "v1", "h1", "r1");

    PolicyWaiver policyWaiver = tempEntity.newWaiver(orgPolicy.getId(), org.getId());
    PolicyViolation waivedViolation = tempEntity.newWaivedPolicyViolation(pe, orgPolicy,
        policyWaiver);

    PolicyViolation legacyViolation = tempEntity.newLegacyPolicyViolation(pe, orgPolicy);

    // default
    ApiApplicationViolationListDTOV2 result = apiPolicyViolationService
        .getPolicyViolations(Collections.singleton(orgPolicy.getId()), null, null,
            EnumSet.of(PolicyViolationType.ACTIVE));

    assertThat(result.applicationViolations).hasSize(1);
    assertThat(result.applicationViolations.get(0).policyViolations)
        .hasSize(1)
        .extracting(v -> v.policyViolationId, v -> v.isWaived, v -> v.isLegacy)
        .containsExactly(tuple(activeViolation.getId(), false, false));

    // Only waived
    result = apiPolicyViolationService.getPolicyViolations(
        Collections.singleton(orgPolicy.getId()),
        null,
        null,
        EnumSet.of(PolicyViolationType.WAIVED));

    assertThat(result.applicationViolations).hasSize(1);
    assertThat(result.applicationViolations.get(0).policyViolations)
        .hasSize(1)
        .extracting(v -> v.policyViolationId, v -> v.isWaived, v -> v.isLegacy)
        .containsExactly(tuple(waivedViolation.getId(), true, false));

    // Only legacy
    result = apiPolicyViolationService.getPolicyViolations(
        Collections.singleton(orgPolicy.getId()),
        null,
        null,
        EnumSet.of(PolicyViolationType.LEGACY));

    assertThat(result.applicationViolations).hasSize(1);
    assertThat(result.applicationViolations.get(0).policyViolations)
        .hasSize(1)
        .extracting(v -> v.policyViolationId, v -> v.isWaived, v -> v.isLegacy)
        .containsExactly(tuple(legacyViolation.getId(), false, true));

    // waived and legacy
    PolicyViolation waivedAndLegacyViolation =
        tempEntity.newLegacyAndWaivedPolicyViolation(pe, orgPolicy, policyWaiver);

    result = apiPolicyViolationService.getPolicyViolations(
        Collections.singleton(orgPolicy.getId()),
        null,
        null,
        EnumSet.of(PolicyViolationType.WAIVED, PolicyViolationType.LEGACY));

    assertThat(result.applicationViolations).hasSize(1);
    assertThat(result.applicationViolations.get(0).policyViolations)
        .hasSize(3)
        .extracting(v -> v.policyViolationId, v -> v.isWaived, v -> v.isLegacy)
        .containsExactlyInAnyOrder(
            tuple(waivedViolation.getId(), true, false),
            tuple(legacyViolation.getId(), false, true),
            tuple(waivedAndLegacyViolation.getId(), true, true));

    // All types
    result = apiPolicyViolationService.getPolicyViolations(
        Collections.singleton(orgPolicy.getId()),
        null,
        null,
        EnumSet.of(PolicyViolationType.ACTIVE, PolicyViolationType.WAIVED, PolicyViolationType.LEGACY));

    assertThat(result.applicationViolations).hasSize(1);
    assertThat(result.applicationViolations.get(0).policyViolations)
        .hasSize(4)
        .extracting(v -> v.policyViolationId, v -> v.isWaived, v -> v.isLegacy)
        .containsExactlyInAnyOrder(
            tuple(activeViolation.getId(), false, false),
            tuple(waivedViolation.getId(), true, false),
            tuple(legacyViolation.getId(), false, true),
            tuple(waivedAndLegacyViolation.getId(), true, true));
  }

  @Test
  public void testGetPolicyViolations_WithDateRangeAndMixedViolations() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Policy orgPolicy = tempEntity.newPolicy(org);
    PolicyEvaluation pe = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1");

    PolicyViolation activeInRange = tempEntity.newPolicyViolation(pe, orgPolicy, "g1", "a1", "v1", "h1", "r1");

    PolicyViolation waivedInRange = tempEntity.newWaivedPolicyViolation(pe, orgPolicy,
        tempEntity.newWaiver(orgPolicy.getId(), org.getId()));

    PolicyViolation legacyInRange = tempEntity.newLegacyPolicyViolation(pe, orgPolicy);

    // violations outside date range
    PolicyEvaluation oldPe = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId2",
        DateUtils.addDays(new Date(), -10));

    PolicyViolation activeOutOfRange = tempEntity.newPolicyViolation(oldPe, orgPolicy, "g4", "a4", "v4", "h4", "r4");
    activeOutOfRange.setOpenTime(DateUtils.addDays(new Date(), -10));

    String openTimeAfter = LocalDate.now().minusDays(1).toString();
    String openTimeBefore = LocalDate.now().plusDays(1).toString();

    // All types within date range
    ApiApplicationViolationListDTOV2 result = apiPolicyViolationService
        .getPolicyViolations(Collections.singleton(orgPolicy.getId()),
            openTimeAfter,
            openTimeBefore,
            EnumSet.allOf(PolicyViolationType.class));

    assertThat(result.applicationViolations).hasSize(1);
    assertThat(result.applicationViolations.get(0).policyViolations)
        .hasSize(3)
        .extracting(v -> v.policyViolationId, v -> v.isWaived, v -> v.isLegacy)
        .containsExactlyInAnyOrder(
            tuple(activeInRange.getId(), false, false),
            tuple(waivedInRange.getId(), true, false),
            tuple(legacyInRange.getId(), false, true));

    // Out of range should be empty
    result = apiPolicyViolationService
        .getPolicyViolations(Collections.singleton(orgPolicy.getId()),
            LocalDate.now().plusDays(5).toString(),
            LocalDate.now().plusDays(10).toString(),
            EnumSet.allOf(PolicyViolationType.class));

    assertThat(result.applicationViolations).isEmpty();
  }

  private void testGetTransitiveComponentsByAppScanComponent_ComponentWithTransitiveComponents(
      ComponentIdentifier componentIdentifier,
      String packageUrl,
      String hash) throws Exception
  {
    Application app = tempEntity.newApplicationWithParent();
    String scanId = "scanId";
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId);
    ReportTestUtils.createReportFile(app.getId(), scanId,
        zipReportDir("/ApiPolicyViolationServiceV2Test/report", tempDir), insightWork);

    List<Component> transitiveComponentsByAppScanComponent =
        apiPolicyViolationService.getTransitiveComponentsByAppScanComponent(app.getId(), scanId, componentIdentifier,
            packageUrl, hash);

    assertThat(transitiveComponentsByAppScanComponent).extracting(Component::getHash)
        .containsExactly("hash21", "hash22", "hash221", "hash222");
  }

  private ApiComponentTransitivePolicyViolationsDTO create(
      ApiStagePolicyViolationComponentDTO... policyViolations)
  {
    ApiComponentTransitivePolicyViolationsDTO result =
        new ApiComponentTransitivePolicyViolationsDTO(new Component(), new ArrayList<>());
    result.transitivePolicyViolations.addAll(Arrays.asList(policyViolations));
    return result;
  }

  private ApiStagePolicyViolationComponentDTO create(
      int threatLevel,
      ComponentIdentifier componentIdentifier)
  {
    ApiStagePolicyViolationComponentDTO result = new ApiStagePolicyViolationComponentDTO();
    result.threatLevel = threatLevel;
    result.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);
    return result;
  }

  private void assertPolicyViolation(
      ApiApplicationViolationDTOV2 apiApplicationViolationDTO,
      PolicyData appPolicyData,
      String packagerUrl)
  {
    assertThat(apiApplicationViolationDTO.application).isNotNull();
    assertThat(apiApplicationViolationDTO.application.id).isEqualTo(appPolicyData.application.getId());
    assertThat(apiApplicationViolationDTO.application.name).isEqualTo(appPolicyData.application.getName());
    assertThat(apiApplicationViolationDTO.application.publicId).isEqualTo(appPolicyData.application.getPublicId());
    assertThat(apiApplicationViolationDTO.application.contactUserName)
        .isEqualTo(appPolicyData.application.getContactInternalName());
    assertThat(apiApplicationViolationDTO.application.organizationId)
        .isEqualTo(appPolicyData.application.getOrganizationId());

    assertThat(apiApplicationViolationDTO.policyViolations).hasSize(2);
    ApiEnhancedPolicyViolationDTOV2 apiPolicyViolationDTO1 = apiApplicationViolationDTO.policyViolations.get(0);
    ApiEnhancedPolicyViolationDTOV2 apiPolicyViolationDTO2 = apiApplicationViolationDTO.policyViolations.get(1);

    if (apiPolicyViolationDTO1.policyId.equals(appPolicyData.orgPolicy.getId())) {
      assertPolicyViolation(apiPolicyViolationDTO1, appPolicyData.application, appPolicyData.policyEvaluation1,
          appPolicyData.policyViolation1, appPolicyData, packagerUrl);
      assertPolicyViolation(apiPolicyViolationDTO2, appPolicyData.application, appPolicyData.policyEvaluation2,
          appPolicyData.policyViolation2, appPolicyData, packagerUrl);
    }
    else {
      assertPolicyViolation(apiPolicyViolationDTO1, appPolicyData.application, appPolicyData.policyEvaluation2,
          appPolicyData.policyViolation2, appPolicyData, packagerUrl);
      assertPolicyViolation(apiPolicyViolationDTO2, appPolicyData.application, appPolicyData.policyEvaluation1,
          appPolicyData.policyViolation1, appPolicyData, packagerUrl);
    }
  }

  private void assertPolicyViolation(
      ApiEnhancedPolicyViolationDTOV2 apiPolicyViolationDTO,
      Application application,
      PolicyEvaluation policyEvaluation,
      PolicyViolation policyViolation,
      PolicyData appPolicyData,
      String packageUrl)
  {
    assertThat(apiPolicyViolationDTO.policyId).isEqualTo(policyViolation.getPolicyId());
    assertThat(apiPolicyViolationDTO.policyName).isEqualTo(policyViolation.getPolicyName());
    assertThat(apiPolicyViolationDTO.policyViolationId).isEqualTo(policyViolation.getId());
    assertThat(apiPolicyViolationDTO.threatLevel).isEqualTo(policyViolation.getThreatLevel());
    assertThat(apiPolicyViolationDTO.reportUrl)
        .isEqualTo("ui/links/application/" + application.getPublicId() + "/report/" + policyEvaluation.getScanId());
    assertThat(apiPolicyViolationDTO.stageId).isEqualTo(policyEvaluation.getStageTypeId());
    assertThat(apiPolicyViolationDTO.component.hash).isEqualTo(policyViolation.getHash());
    assertThat(apiPolicyViolationDTO.component.proprietary)
        .isEqualTo(appPolicyData.applicationComponent.isProprietary());
    assertThat(apiPolicyViolationDTO.openTime).isEqualTo(policyViolation.getOpenTime());

    if (policyViolation.getComponentIdentifier() != null) {
      assertThat(apiPolicyViolationDTO.component.componentIdentifier.toComponentIdentifier())
          .isEqualTo(policyViolation.getComponentIdentifier());
      assertThat(apiPolicyViolationDTO.component.packageUrl).isEqualTo(packageUrl);
      assertThat(apiPolicyViolationDTO.component.displayName)
          .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(policyViolation.getComponentIdentifier()).toString());
    }
    else {
      assertThat(apiPolicyViolationDTO.component.componentIdentifier).isNull();
      assertThat(apiPolicyViolationDTO.component.packageUrl).isNull();
      assertThat(apiPolicyViolationDTO.component.displayName).isEqualTo(policyViolation.getFilename());
    }

    assertThat(apiPolicyViolationDTO.constraintViolations).hasSize(1);
    ApiConstraintViolationDTO apiConstraintViolationDTO = apiPolicyViolationDTO.constraintViolations.get(0);
    assertThat(apiConstraintViolationDTO.constraintId)
        .isEqualTo(policyViolation.getConstraintFacts().get(0).getConstraintId());
    assertThat(apiConstraintViolationDTO.constraintName)
        .isEqualTo(policyViolation.getConstraintFacts().get(0).getConstraintName());
    assertThat(apiConstraintViolationDTO.reasons).hasSize(1);
    ApiConstraintViolationReasonDTO apiConstraintViolationReasonDTO = apiConstraintViolationDTO.reasons.get(0);
    assertThat(apiConstraintViolationReasonDTO.reason)
        .isEqualTo(policyViolation.getConstraintFacts().get(0).getConditionFacts().get(0).getReason());
  }

  private PolicyData createPolicyTestData(
      String scanId,
      ComponentIdentifier componentIdentifier,
      String hash,
      String reason,
      String filename)
  {
    PolicyData policyTestData = new PolicyData();
    policyTestData.organization = tempEntity.newOrganization();
    policyTestData.application = tempEntity.newApplication(policyTestData.organization.getId());
    policyTestData.orgPolicy = tempEntity.newPolicy(policyTestData.organization);

    // Create one violation in the past for build stage
    long time = System.currentTimeMillis() - 1000;
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(policyTestData.application.getId(),
        BuildStageType.ID, scanId + "1", new Date(time));
    policyTestData.policyViolation1 = tempEntity.newPolicyViolation(policyEvaluation, policyTestData.orgPolicy,
        componentIdentifier, hash, reason, filename);

    MatchState matchState = componentIdentifier != null ? MatchState.EXACT : MatchState.UNKNOWN;
    // Create a new evaluation for build stage, retaining the previous violation
    policyTestData.policyEvaluation1 = tempEntity.newPolicyEvaluation(policyTestData.application.getId(),
        BuildStageType.ID, scanId, new Date());
    policyTestData.applicationComponent = tempEntity.newApplicationComponent(policyTestData.application.getId(),
        BuildStageType.ID, hash, componentIdentifier, null, matchState, true, new Date(time));

    // Create a current violation for release stage
    policyTestData.policyEvaluation2 = tempEntity.newPolicyEvaluation(policyTestData.application.getId(),
        ReleaseStageType.ID, scanId, new Date());
    policyTestData.policyViolation2 = tempEntity.newPolicyViolation(policyTestData.policyEvaluation2,
        policyTestData.orgPolicy, componentIdentifier, hash, reason, filename);
    policyTestData.applicationComponent = tempEntity.newApplicationComponent(policyTestData.application.getId(),
        ReleaseStageType.ID, hash, componentIdentifier, null, matchState, true, new Date(time));

    return policyTestData;
  }

  private static class PolicyData
  {
    public Organization organization;

    public Application application;

    public Policy orgPolicy;

    public PolicyEvaluation policyEvaluation1;

    public PolicyViolation policyViolation1;

    public PolicyEvaluation policyEvaluation2;

    public PolicyViolation policyViolation2;

    public OwnerComponent applicationComponent;
  }
}
