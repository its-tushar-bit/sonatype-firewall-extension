/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

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
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.InvalidPackageURLException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.google.common.collect.Sets;
import com.google.inject.Binder;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.mockito.Mock;

import static com.sonatype.insight.brain.report.ReportTestUtils.zipReportDir;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

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

  @Mock
  private StageTypeService mockStageTypeService;

  @Override
  public void configure(Binder binder) {
    lenient().when(mockStageTypeService.getLicensedStageTypes()).thenReturn(StageTypes.getAll());
    binder.bind(StageTypeService.class).toInstance(mockStageTypeService);
    super.configure(binder);
  }

  @Test
  public void testGetPolicyViolations_noPolicyIds() {
    createPolicyTestData("scanId1App1", ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), "h1", "r1");

    ApiApplicationViolationListDTOV2 apiApplicationViolationListDTO = apiPolicyViolationService
        .getPolicyViolations(Collections.emptySet());
    assertThat(apiApplicationViolationListDTO).isNotNull();
    assertThat(apiApplicationViolationListDTO.applicationViolations).isEmpty();
  }

  @Test
  public void testGetPolicyViolations_filteredByPolicyId() {
    PolicyData appPolicyData1 = createPolicyTestData("scanId1App1",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), "h1", "r1");
    PolicyData appPolicyData2 = createPolicyTestData("scanId1App2",
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"), "h2", "r2");
    createPolicyTestData("scanId1App3", ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"), "h3", "r3");

    // Get the policy violations for two (out of three) policies
    Set<String> policyIds = Sets.newHashSet(appPolicyData1.orgPolicy.getId(), appPolicyData2.orgPolicy.getId());
    ApiApplicationViolationListDTOV2 apiApplicationViolationListDTO = apiPolicyViolationService
        .getPolicyViolations(policyIds);

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
  }

  @Test
  public void testGetPolicyViolations_nuGetFilteredByPolicyId() {
    PolicyData appPolicyData1 = createPolicyTestData("scanId1App1",
        ComponentIdentifier.createNugetCoordinates("nuget1", "v1"), "h3", "r4");
    createPolicyTestData("scanId1App2", ComponentIdentifier.createNugetCoordinates("nuget2", "v1"), "h4", "r5");

    Set<String> policyIds = Sets.newHashSet(appPolicyData1.orgPolicy.getId());
    ApiApplicationViolationListDTOV2 apiApplicationViolationListDTO = apiPolicyViolationService
        .getPolicyViolations(policyIds);

    assertThat(apiApplicationViolationListDTO).isNotNull();
    assertThat(apiApplicationViolationListDTO.applicationViolations).hasSize(1);
    ApiApplicationViolationDTOV2 apiApplicationViolationDTO1 = apiApplicationViolationListDTO.applicationViolations
        .get(0);
    assertPolicyViolation(apiApplicationViolationDTO1, appPolicyData1, PACKAGE_URL_NUGET);
  }

  @Test
  public void testGetPolicyViolations_unknownComponent() {
    PolicyData appPolicyData = createPolicyTestData("scanId", null /* componentIdentifier */, "testhash", "testreason");

    Set<String> policyIds = Collections.singleton(appPolicyData.orgPolicy.getId());
    ApiApplicationViolationListDTOV2 apiApplicationViolationListDTO = apiPolicyViolationService
        .getPolicyViolations(policyIds);

    assertThat(apiApplicationViolationListDTO.applicationViolations).hasSize(1);
    ApiApplicationViolationDTOV2 apiApplicationViolationDTO = apiApplicationViolationListDTO.applicationViolations
        .get(0);
    assertPolicyViolation(apiApplicationViolationDTO, appPolicyData, null);
  }

  @Test
  public void testGetPolicyViolations_ExcludeWaivedViolations() {
    PolicyData policyData = createPolicyTestData("scanId", null /* componentIdentifier */, "testhash", "testreason");
    tempEntity.newWaivedPolicyViolation(policyData.policyEvaluation1, policyData.orgPolicy,
        tempEntity.newWaiver(policyData.orgPolicy.getId(), policyData.organization.getId()));

    Set<String> policyIds = Collections.singleton(policyData.orgPolicy.getId());
    ApiApplicationViolationListDTOV2 apiApplicationViolationListDTO = apiPolicyViolationService
        .getPolicyViolations(policyIds);

    assertThat(apiApplicationViolationListDTO.applicationViolations).hasSize(1);
    ApiApplicationViolationDTOV2 apiApplicationViolationDTO = apiApplicationViolationListDTO.applicationViolations
        .get(0);
    assertPolicyViolation(apiApplicationViolationDTO, policyData, null);
  }

  @Test
  public void testGetTransitivePolicyViolations_InvalidStageId() {
    String invalidStageId = "InVaLiD";

    assertThatExceptionOfType(InvalidStageException.class)
        .isThrownBy(() -> apiPolicyViolationService.getTransitivePolicyViolations(OwnerType.ORGANIZATION,
            Organization.ROOT_ORGANIZATION_ID, invalidStageId, null, null, null))
        .withMessageContaining("Invalid stage id=" + invalidStageId);
  }

  @Test
  public void testGetTransitivePolicyViolations_StageTypeNotLicensed() {
    when(mockStageTypeService.getLicensedStageTypes()).thenReturn(Collections.emptyList());

    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> apiPolicyViolationService.getTransitivePolicyViolations(OwnerType.ORGANIZATION,
            Organization.ROOT_ORGANIZATION_ID, BuildStageType.ID, null, null, null))
        .withMessageContaining("Stage '" + BuildStageType.ID + "' is not supported by your license.");
  }

  @Test
  public void testGetTransitivePolicyViolations_NoComponentIdentifierAndNoPackageUrlAndNoHash() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiPolicyViolationService.getTransitivePolicyViolations(OwnerType.ORGANIZATION,
            Organization.ROOT_ORGANIZATION_ID, BuildStageType.ID, null, null, null))
        .withMessageContaining("componentIdentifier or packageUrl or hash must be specified.");
  }

  @Test
  public void testGetTransitivePolicyViolations_UnknownHash() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> apiPolicyViolationService.getTransitivePolicyViolations(OwnerType.ORGANIZATION,
            Organization.ROOT_ORGANIZATION_ID, BuildStageType.ID, null, null, "unknown"))
        .withMessageContaining("Component not found.");
  }

  @Test
  public void testGetTransitivePolicyViolations_InvalidPackageUrl() {
    assertThatExceptionOfType(InvalidPackageURLException.class)
        .isThrownBy(() -> apiPolicyViolationService.getTransitivePolicyViolations(OwnerType.ORGANIZATION,
            Organization.ROOT_ORGANIZATION_ID, BuildStageType.ID, null, "invalidPackageUrl", null))
        .withMessageContaining("Invalid package url");
  }

  @Test
  public void testGetTransitivePolicyViolations_IncompletePackageUrl() {
    assertThatExceptionOfType(InvalidPackageURLException.class)
        .isThrownBy(() -> apiPolicyViolationService.getTransitivePolicyViolations(OwnerType.ORGANIZATION,
            Organization.ROOT_ORGANIZATION_ID, BuildStageType.ID, null, "pkg:maven/g/a@v", null))
        .withMessageContaining("The following coordinates are missing for given format: [type]");
  }

  @Test
  public void testGetTransitivePolicyViolations_IncompleteComponentIdentifier() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiPolicyViolationService.getTransitivePolicyViolations(OwnerType.ORGANIZATION,
            Organization.ROOT_ORGANIZATION_ID, BuildStageType.ID,
            ComponentIdentifier.createMavenCoordinates("g", "a", "v", null, null), null, null))
        .withMessageContaining("The following coordinates are missing for given format: [extension]");
  }

  @Test
  public void testGetTransitivePolicyViolations_MissingReport() {
    Application application = tempEntity.newApplicationWithParent();
    String scanId = "scanId";
    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scanId);
    ComponentIdentifier direct = ComponentIdentifier.createMavenCoordinates("g", "direct2", "v", "", "e");

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> apiPolicyViolationService
        .getTransitivePolicyViolations(OwnerType.APPLICATION, application.getId(), BuildStageType.ID, direct, null,
            null))
        .withMessageContaining("Component not found.");
  }

  @Test
  public void testGetTransitivePolicyViolations_MissingReports() {
    Organization organization = tempEntity.newOrganization();
    Application application1 = tempEntity.newApplication(organization.getId());
    Application application2 = tempEntity.newApplication(organization.getId());
    String scanId = "scanId";
    tempEntity.newPolicyEvaluation(application1.getId(), BuildStageType.ID, scanId);
    tempEntity.newPolicyEvaluation(application2.getId(), BuildStageType.ID, scanId);
    ComponentIdentifier direct = ComponentIdentifier.createMavenCoordinates("g", "direct2", "v", "", "e");

    List<String> ids = Arrays.asList(application1.getPublicId(), application2.getPublicId());
    ids.sort(Comparator.naturalOrder());
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> apiPolicyViolationService
        .getTransitivePolicyViolations(OwnerType.ORGANIZATION, organization.getId(), BuildStageType.ID, direct, null,
            null))
        .withMessageContaining("Component not found.");
  }

  @Test
  public void testGetTransitivePolicyViolations_ComponentNotFound_Application() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    String scanId = "scanId";
    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scanId);
    ReportTestUtils.createReportFile(application.getId(), scanId,
        zipReportDir("/ApiPolicyViolationServiceV2Test/report", tempDir), insightWork);

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> apiPolicyViolationService
        .getTransitivePolicyViolations(OwnerType.APPLICATION, application.getId(), BuildStageType.ID, null,
            "pkg:maven/g/other@v?type=e", null))
        .withMessageContaining("Component not found.");
  }

  @Test
  public void testGetTransitivePolicyViolations_ComponentNotFound_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    String scanId = "scanId";
    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scanId);
    ReportTestUtils.createReportFile(application.getId(), scanId,
        zipReportDir("/ApiPolicyViolationServiceV2Test/report", tempDir), insightWork);

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> apiPolicyViolationService
        .getTransitivePolicyViolations(OwnerType.ORGANIZATION, organization.getPublicId(), BuildStageType.ID, null,
            "pkg:maven/g/other@v?type=e", null))
        .withMessageContaining("Component not found.");
  }

  @Test
  public void testGetTransitivePolicyViolations_ByComponentIdentifier() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    testGetTransitivePolicyViolations(application,
        ComponentIdentifier.createMavenCoordinates("g", "direct2", "v", "", "e"), null, null);
  }

  @Test
  public void testGetTransitivePolicyViolations_ByPackageUrl() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    testGetTransitivePolicyViolations(application, null, "pkg:maven/g/direct2@v?type=e", null);
  }

  @Test
  public void testGetTransitivePolicyViolations_ByHash() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    testGetTransitivePolicyViolations(application, null, null, "hash2");
  }

  @Test
  public void testGetTransitivePolicyViolations_ByComponentIdentifierAndPackageUrl() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    testGetTransitivePolicyViolations(application,
        ComponentIdentifier.createMavenCoordinates("g", "direct2", "v", "", "e"), "invalidPackageUrl", null);
  }

  @Test
  public void testGetTransitivePolicyViolations_ByComponentIdentifierAndHash() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    testGetTransitivePolicyViolations(application,
        ComponentIdentifier.createMavenCoordinates("g", "direct2", "v", "", "e"), null, "unknown");
  }

  @Test
  public void testGetTransitivePolicyViolations_ByPackageUrlAndHash() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    testGetTransitivePolicyViolations(application, null, "pkg:maven/g/direct2@v?type=e", "unknown");
  }

  @Test
  public void testGetTransitivePolicyViolations_ByComponentIdentifierAndPackageUrlAndHash() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    testGetTransitivePolicyViolations(application,
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
    tempEntity.newPolicyViolation(policyEvaluation, policy, direct2, "hash2");
    PolicyViolation transitive2PolicyViolation =
        tempEntity.newPolicyViolation(policyEvaluation, policy, transitive2, "hash22");
    PolicyViolation transitive22PolicyViolation =
        tempEntity.newPolicyViolation(policyEvaluation, policy, transitive22, "hash222");
    ReportTestUtils.createReportFile(application.getId(), scanId,
        zipReportDir("/ApiPolicyViolationServiceV2Test/report", tempDir), insightWork);

    ApiComponentTransitivePolicyViolationsDTO result = apiPolicyViolationService.getTransitivePolicyViolations(
        OwnerType.APPLICATION, application.getId(), BuildStageType.ID, componentIdentifier, packageUrl, hash);

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
    assertThat(result.transitivePolicyViolations).usingRecursiveFieldByFieldElementComparator().containsExactly(
        ApiStagePolicyViolationComponentDTO
            .fromPolicyViolationAndComponent(Pair.of(transitive2PolicyViolation, expectedTransitiveComponent2)),
        ApiStagePolicyViolationComponentDTO
            .fromPolicyViolationAndComponent(Pair.of(transitive22PolicyViolation, expectedTransitiveComponent22)));
  }

  @Test
  public void testGetTransitivePolicyViolations_MultipleApplications() throws Exception {
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
    ReportTestUtils.createReportFile(application2.getId(), scanId,
        zipReportDir("/ApiPolicyViolationServiceV2Test/report", tempDir), insightWork);

    ApiComponentTransitivePolicyViolationsDTO result = apiPolicyViolationService
        .getTransitivePolicyViolations(OwnerType.ORGANIZATION, organization.getId(), BuildStageType.ID, direct2, null,
            null);

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
    assertThat(result.transitivePolicyViolations).usingRecursiveFieldByFieldElementComparator().containsExactly(
        ApiStagePolicyViolationComponentDTO
            .fromPolicyViolationAndComponent(Pair.of(transitive1PolicyViolation1, expectedTransitiveComponent1)),
        ApiStagePolicyViolationComponentDTO
            .fromPolicyViolationAndComponent(Pair.of(transitive1PolicyViolation2, expectedTransitiveComponent1))
    );
  }

  @Test
  public void testGetTransitivePolicyViolations_MultipleOrganizations() throws Exception {
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
    ReportTestUtils.createReportFile(application2.getId(), scanId,
        zipReportDir("/ApiPolicyViolationServiceV2Test/report", tempDir), insightWork);

    ApiComponentTransitivePolicyViolationsDTO result = apiPolicyViolationService.getTransitivePolicyViolations(
        OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, BuildStageType.ID, direct2, null, null);

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
    assertThat(result.transitivePolicyViolations).usingRecursiveFieldByFieldElementComparator().containsExactly(
        ApiStagePolicyViolationComponentDTO
            .fromPolicyViolationAndComponent(Pair.of(transitive1PolicyViolation1, expectedTransitiveComponent1)),
        ApiStagePolicyViolationComponentDTO
            .fromPolicyViolationAndComponent(Pair.of(transitive1PolicyViolation2, expectedTransitiveComponent1))
    );
  }

  @Test
  public void testGetTransitivePolicyViolations_InnerSourceAndNone() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    String scanId = "scanId";
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scanId);
    Policy policy = tempEntity.newPolicy();
    ComponentIdentifier direct1 = ComponentIdentifier.createMavenCoordinates("g", "direct1", "v", "", "e");
    tempEntity.newPolicyViolation(policyEvaluation, policy, direct1, "hash1");
    ReportTestUtils.createReportFile(application.getId(), scanId,
        zipReportDir("/ApiPolicyViolationServiceV2Test/report", tempDir), insightWork);

    ApiComponentTransitivePolicyViolationsDTO result = apiPolicyViolationService.getTransitivePolicyViolations(
        OwnerType.APPLICATION, application.getId(), BuildStageType.ID, direct1, null, null);

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
  public void testGetTransitivePolicyViolations_Unknown() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    String scanId = "scanId";
    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scanId);
    ReportTestUtils.createReportFile(application.getId(), scanId,
        zipReportDir("/ApiPolicyViolationServiceV2Test/report", tempDir), insightWork);
    
    ApiComponentTransitivePolicyViolationsDTO result = apiPolicyViolationService.getTransitivePolicyViolations(
        application.getType(), application.getId(), BuildStageType.ID, null, null, "81399f9f3278d8615a7c");
    
    assertThat(result).isNotNull();
    assertThat(result.componentIdentifier).isNull();
    assertThat(result.packageUrl).isNull();
    assertThat(result.hash).isEqualTo("81399f9f3278d8615a7c");
    assertThat(result.displayName).isEqualTo("unknown.zip");
    assertThat(result.isInnerSource).isFalse();
    assertThat(result.transitivePolicyViolations).isEmpty();
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
      PolicyData appPolicyData, String packagerUrl)
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
      assertThat(apiPolicyViolationDTO.component.displayName).isNull();
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
      String reason)
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
        componentIdentifier, hash, reason);

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
        policyTestData.orgPolicy, componentIdentifier, hash, reason);
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

    public ApplicationComponent applicationComponent;
  }
}
