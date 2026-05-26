/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseThreatDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LicenseLegalApplicationComponentsFilterDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LicenseObligationReviewStatus;
import com.sonatype.insight.brain.api.v2.service.ApiLicenseDataAdapter;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.legal.ObligationStatus;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.report.InnerSourceUtils;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.license.dto.model.LicenseMetadataDTO;
import com.sonatype.insight.license.dto.model.LicenseObligationDTO;
import com.sonatype.insight.license.model.LicensedFeature;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

@Category(SlowTest.class)
public class LegalApplicationDashboardServiceTest
    extends AbstractComponentTest
{
  @Inject
  private LegalApplicationDashboardService legalApplicationDashboardService;

  @Inject
  private TestProductLicense testProductLicense;

  @Inject
  private LicenseThreatGroupDAO licenseThreatGroupDAO;

  @Inject
  private MultiLicenseDAO multiLicenseDAO;

  @Mock
  private ApiLicenseLegalHdsService mockApiLicenseLegalHdsService;

  @Test
  public void testGetLicenseLegalApplicationDashboard_Unlicensed() {
    setUnlicensedForAdvancedLegalPack();
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> legalApplicationDashboardService.getLicenseLegalApplicationDashboard(null, null));
  }

  @Test
  public void testGetLicenseLegalApplicationDashboard_ApplicationNotFound() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> legalApplicationDashboardService.getLicenseLegalApplicationDashboard("fake-app-id", null));
  }

  @Test
  public void testGetLicenseLegalApplicationDashboard_WithoutComponents() {
    Application app = tempEntity.newApplicationWithParent();
    assertThat(legalApplicationDashboardService.getLicenseLegalApplicationDashboard(app.getPublicId(), null)).isEmpty();
  }

  @Test
  public void testGetLicenseLegalApplicationDashboard_WithUnknownComponent() {
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, "someHash", null);
    assertThat(legalApplicationDashboardService.getLicenseLegalApplicationDashboard(app.getPublicId(), null)).isEmpty();
  }

  @Test
  public void testGetLicenseLegalApplicationDashboard() {
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    List<String> licenseIds = Arrays.asList("MIT", "Apache-2.0");
    List<ApiLicenseDTOV2> licenses = getApiLicenses(licenseIds);

    Triple<Application, Tag, PolicyEvaluation> appTagEval =
        setupApplicationWithLicenses(componentIdentifier1, licenseIds.toArray(new String[0]));

    // Create another app with components evaluated to see if only the details from the one above are returned
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2");
    setupApplicationWithLicenses(componentIdentifier2, "Apache-1.0");

    List<ApiLicenseLegalApplicationComponentDTO> result =
        legalApplicationDashboardService.getLicenseLegalApplicationDashboard(appTagEval.getLeft().getPublicId(), null);
    assertThat(result).hasSize(1);

    assertApiLicenseLegalApplicationComponentDTO(result.get(0), componentIdentifier1, licenses, 0, 0,
        LicenseObligationReviewStatus.COMPLETED);
  }

  @Test
  public void testGetLicenseLegalApplicationDashboard_FlaggedObligation() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    List<String> licenseIds = Collections.singletonList("MIT");
    List<ApiLicenseDTOV2> licenses = getApiLicenses(licenseIds);

    Application app = setupApplicationWithLicenses(componentIdentifier, licenseIds.get(0)).getLeft();
    setupLicenseObligations(app, componentIdentifier, licenseIds, ObligationStatus.FULFILLED, ObligationStatus.FLAGGED);

    List<ApiLicenseLegalApplicationComponentDTO> result =
        legalApplicationDashboardService.getLicenseLegalApplicationDashboard(app.getPublicId(), null);
    assertThat(result).hasSize(1);

    assertApiLicenseLegalApplicationComponentDTO(result.get(0), componentIdentifier, licenses, 1, 2,
        LicenseObligationReviewStatus.FLAGGED);
  }

  @Test
  public void testGetLicenseLegalApplicationDashboard_CompletedObligation() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    List<String> licenseIds = Collections.singletonList("MIT");
    List<ApiLicenseDTOV2> licenses = getApiLicenses(licenseIds);

    Application app = setupApplicationWithLicenses(componentIdentifier, licenseIds.get(0)).getLeft();
    setupLicenseObligations(app, componentIdentifier, licenseIds, ObligationStatus.FULFILLED, ObligationStatus.IGNORED);

    List<ApiLicenseLegalApplicationComponentDTO> result =
        legalApplicationDashboardService.getLicenseLegalApplicationDashboard(app.getPublicId(), null);
    assertThat(result).hasSize(1);

    assertApiLicenseLegalApplicationComponentDTO(result.get(0), componentIdentifier, licenses, 2, 2,
        LicenseObligationReviewStatus.COMPLETED);
  }

  @Test
  public void testGetLicenseLegalApplicationDashboard_CompletedObligation_LicensesWithoutObligations() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    List<String> licenseIds = Collections.singletonList("MIT");
    List<ApiLicenseDTOV2> licenses = getApiLicenses(licenseIds);

    Application app = setupApplicationWithLicenses(componentIdentifier, licenseIds.get(0)).getLeft();

    List<ApiLicenseLegalApplicationComponentDTO> result =
        legalApplicationDashboardService.getLicenseLegalApplicationDashboard(app.getPublicId(), null);
    assertThat(result).hasSize(1);

    assertApiLicenseLegalApplicationComponentDTO(result.get(0), componentIdentifier, licenses, 0, 0,
        LicenseObligationReviewStatus.COMPLETED);
  }

  @Test
  public void testGetLicenseLegalApplicationDashboard_UnreviewedObligation() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    List<String> licenseIds = Collections.singletonList("MIT");
    List<ApiLicenseDTOV2> licenses = getApiLicenses(licenseIds);

    Application app = setupApplicationWithLicenses(componentIdentifier, licenseIds.get(0)).getLeft();
    setupLicenseObligations(app, componentIdentifier, licenseIds, ObligationStatus.OPEN, ObligationStatus.OPEN);

    List<ApiLicenseLegalApplicationComponentDTO> result =
        legalApplicationDashboardService.getLicenseLegalApplicationDashboard(app.getPublicId(), null);
    assertThat(result).hasSize(1);

    assertApiLicenseLegalApplicationComponentDTO(result.get(0), componentIdentifier, licenses, 0, 2,
        LicenseObligationReviewStatus.UNREVIEWED);
  }

  @Test
  public void testGetLicenseLegalApplicationDashboard_UnreviewedObligation_ComponentWithoutLicenses() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    List<ApiLicenseDTOV2> licenses = Collections.emptyList();

    Application app = setupApplicationWithLicenses(componentIdentifier).getLeft();

    List<ApiLicenseLegalApplicationComponentDTO> result =
        legalApplicationDashboardService.getLicenseLegalApplicationDashboard(app.getPublicId(), null);
    assertThat(result).hasSize(1);

    assertApiLicenseLegalApplicationComponentDTO(result.get(0), componentIdentifier, licenses, 0, 0,
        LicenseObligationReviewStatus.UNREVIEWED);
  }

  @Test
  public void testGetLicenseLegalApplicationDashboard_UnreviewedObligation_ComponentWithUnspecifiedLicenses() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    List<String> licenseIds = Arrays.asList(License.NO_SOURCE_LICENSE_ID, License.NO_SOURCES_ID,
        License.NOT_DECLARED_ID, License.NOT_SUPPORTED_ID, License.UNSPECIFIED_ID);
    List<ApiLicenseDTOV2> licenses = getApiLicenses(licenseIds);

    Application app =
        setupApplicationWithLicenses(componentIdentifier, licenseIds.toArray(new String[0])).getLeft();

    List<ApiLicenseLegalApplicationComponentDTO> result =
        legalApplicationDashboardService.getLicenseLegalApplicationDashboard(app.getPublicId(), null);
    assertThat(result).hasSize(1);

    assertApiLicenseLegalApplicationComponentDTO(result.get(0), componentIdentifier, licenses, 0, 0,
        LicenseObligationReviewStatus.UNREVIEWED);
  }

  @Test
  public void testGetLicenseLegalApplicationDashboard_InProgressObligation() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    List<String> licenseIds = Collections.singletonList("MIT");
    List<ApiLicenseDTOV2> licenses = getApiLicenses(licenseIds);

    Application app = setupApplicationWithLicenses(componentIdentifier, licenseIds.get(0)).getLeft();
    setupLicenseObligations(app, componentIdentifier, licenseIds, ObligationStatus.OPEN, ObligationStatus.IGNORED);

    List<ApiLicenseLegalApplicationComponentDTO> result =
        legalApplicationDashboardService.getLicenseLegalApplicationDashboard(app.getPublicId(), null);
    assertThat(result).hasSize(1);

    assertApiLicenseLegalApplicationComponentDTO(result.get(0), componentIdentifier, licenses, 1, 2,
        LicenseObligationReviewStatus.IN_PROGRESS);
  }

  @Test
  public void testGetLicenseLegalApplicationDashboard_ByStageTypeId() {
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    List<String> licenseIds = Arrays.asList("MIT", "Apache-2.0");
    List<ApiLicenseDTOV2> licenses = getApiLicenses(licenseIds);

    Triple<Application, Tag, PolicyEvaluation> triple =
        setupApplicationWithLicenses(componentIdentifier1, licenseIds.toArray(new String[0]));
    Application app = triple.getLeft();
    PolicyEvaluation policyEvaluation = triple.getRight();

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2");
    tempEntity.newApplicationComponent(app.getId(), ReleaseStageType.ID, "hash", componentIdentifier2);

    LicenseLegalApplicationComponentsFilterDTO filter = new LicenseLegalApplicationComponentsFilterDTO();
    filter.stageTypeIds = Sets.newHashSet(policyEvaluation.getStageTypeId());

    List<ApiLicenseLegalApplicationComponentDTO> result = legalApplicationDashboardService
        .getLicenseLegalApplicationDashboard(app.getPublicId(), filter);
    assertThat(result).hasSize(1);

    assertApiLicenseLegalApplicationComponentDTO(result.get(0), componentIdentifier1, licenses, 0, 0,
        LicenseObligationReviewStatus.COMPLETED);
  }

  @Test
  public void testGetLicenseLegalApplicationDashboard_SortedLicenses() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    List<String> licenseIds = Arrays.asList("MIT", "Apache-2.0", "LGPL-2.1", "Apache-1.0");

    Triple<Application, Tag, PolicyEvaluation> triple =
        setupApplicationWithLicenses(componentIdentifier, licenseIds.toArray(new String[0]));
    Application app = triple.getLeft();

    LicenseLegalApplicationComponentsFilterDTO filter = new LicenseLegalApplicationComponentsFilterDTO();

    List<ApiLicenseLegalApplicationComponentDTO> result =
        legalApplicationDashboardService.getLicenseLegalApplicationDashboard(app.getPublicId(), filter);

    assertThat(result.get(0).licenses.stream().map(license -> license.licenseName)).containsExactly("Apache-1.0",
        "Apache-2.0", "LGPL-2.1", "MIT");
  }

  @Test
  public void testGetLicenseLegalApplicationDashboard_ByReviewStatus_Unreviewed() {
    testGetLicenseLegalApplicationDashboard_ByReviewStatus(LicenseObligationReviewStatus.UNREVIEWED,
        ObligationStatus.OPEN, ObligationStatus.FLAGGED);
  }

  @Test
  public void testGetLicenseLegalApplicationDashboard_ByReviewStatus_Flagged() {
    testGetLicenseLegalApplicationDashboard_ByReviewStatus(LicenseObligationReviewStatus.FLAGGED,
        ObligationStatus.FLAGGED, ObligationStatus.FULFILLED);
  }

  @Test
  public void testGetLicenseLegalApplicationDashboard_ByReviewStatus_InProgress() {
    testGetLicenseLegalApplicationDashboard_ByReviewStatus(LicenseObligationReviewStatus.IN_PROGRESS,
        ObligationStatus.OPEN, ObligationStatus.FLAGGED, ObligationStatus.FULFILLED, 1, 2);
  }

  @Test
  public void testGetLicenseLegalApplicationDashboard_ByReviewStatus_Completed() {
    testGetLicenseLegalApplicationDashboard_ByReviewStatus(LicenseObligationReviewStatus.COMPLETED,
        ObligationStatus.FULFILLED, ObligationStatus.FLAGGED, null, 1, 1);
  }

  private void testGetLicenseLegalApplicationDashboard_ByReviewStatus(
      LicenseObligationReviewStatus reviewStatus,
      ObligationStatus obligationStatus1,
      ObligationStatus obligationStatus2)
  {
    testGetLicenseLegalApplicationDashboard_ByReviewStatus(reviewStatus, obligationStatus1, obligationStatus2, null, 0,
        1);
  }

  private void testGetLicenseLegalApplicationDashboard_ByReviewStatus(
      LicenseObligationReviewStatus reviewStatus,
      ObligationStatus compontent1ObligationStatus,
      ObligationStatus compontent2ObligationStatus,
      ObligationStatus compontent1ObligationStatusExtra,
      int reviewCompletedCount,
      int reviewTotalCount)
  {
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2");
    List<String> licenseIds = Arrays.asList("MIT", "Apache-1.0");
    List<ApiLicenseDTOV2> licenses = getApiLicenses(Collections.singletonList("MIT"));

    Application app = setupApplicationWithLicenses(componentIdentifier1, licenseIds.get(0)).getLeft();
    setupLicenseObligations(app, componentIdentifier1, licenseIds, compontent1ObligationStatus,
        compontent1ObligationStatusExtra);

    ApplicationComponent applicationComponent =
        tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, "hash2", componentIdentifier2);
    tempEntity.newApplicationComponentLicense(applicationComponent.getId(), licenseIds.get(1));
    tempEntity.newComponentObligation(componentIdentifier2, app.getId(), "obligation0", "comment",
        compontent2ObligationStatus, "hash2");

    LicenseLegalApplicationComponentsFilterDTO filter = new LicenseLegalApplicationComponentsFilterDTO();
    filter.reviewStatuses = Sets.newHashSet(reviewStatus);

    List<ApiLicenseLegalApplicationComponentDTO> result =
        legalApplicationDashboardService.getLicenseLegalApplicationDashboard(app.getPublicId(), filter);

    assertThat(result).hasSize(1);
    assertApiLicenseLegalApplicationComponentDTO(result.get(0), componentIdentifier1, licenses, reviewCompletedCount,
        reviewTotalCount, reviewStatus);
  }

  @Test
  public void testGetLicenseLegalApplicationDashboard_ByLicenseThreatGroupNames() {
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    List<String> licenseIds = Collections.singletonList("MIT");

    Application app = setupApplicationWithLicenses(componentIdentifier1, licenseIds.get(0)).getLeft();

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2");
    ApplicationComponent applicationComponent =
        tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, "hash2", componentIdentifier2);
    tempEntity.newApplicationComponentLicense(applicationComponent.getId(), "Beerware-Pizzaware");

    tempEntity.newLicenseThreatGroup(app.getId(), "Group 1", 0, licenseIds.get(0));
    tempEntity.newLicenseThreatGroup(app.getOrganizationId(), "Group 2", 5, licenseIds.get(0));
    tempEntity.newLicenseThreatGroup(Organization.ROOT_ORGANIZATION_ID, "Group 3", 9, licenseIds.get(0));
    tempEntity.newLicenseThreatGroup(Organization.ROOT_ORGANIZATION_ID, "Group 4", 3, "Apache-1.0");
    tempEntity.newLicenseThreatGroup(Organization.ROOT_ORGANIZATION_ID, "Group 5", 7, "Pizzaware");

    ApiLicenseThreatDTOV2 groupTest = new ApiLicenseThreatDTOV2();
    groupTest.licenseThreatGroupName = "Group 1";
    List<ApiLicenseDTOV2> licenses =
        Collections.singletonList(new ApiLicenseDTOV2(licenseIds.get(0), licenseIds.get(0),
            Collections.singletonList(groupTest)));

    LicenseLegalApplicationComponentsFilterDTO filter = new LicenseLegalApplicationComponentsFilterDTO();
    filter.licenseThreatGroupNames = Sets.newHashSet(groupTest.licenseThreatGroupName);

    List<ApiLicenseLegalApplicationComponentDTO> result = legalApplicationDashboardService
        .getLicenseLegalApplicationDashboard(app.getPublicId(), filter);
    assertThat(result).hasSize(1);
    assertApiLicenseLegalApplicationComponentDTO(result.get(0), componentIdentifier1, licenses, 0, 0,
        LicenseObligationReviewStatus.COMPLETED);

    groupTest = new ApiLicenseThreatDTOV2();
    groupTest.licenseThreatGroupName = "Group 2";
    licenses =
        Collections.singletonList(new ApiLicenseDTOV2(licenseIds.get(0), licenseIds.get(0),
            Collections.singletonList(groupTest)));

    filter = new LicenseLegalApplicationComponentsFilterDTO();
    filter.licenseThreatGroupNames = Sets.newHashSet(groupTest.licenseThreatGroupName);

    result = legalApplicationDashboardService.getLicenseLegalApplicationDashboard(app.getPublicId(), filter);
    assertThat(result).hasSize(1);
    assertApiLicenseLegalApplicationComponentDTO(result.get(0), componentIdentifier1, licenses, 0, 0,
        LicenseObligationReviewStatus.COMPLETED);

    groupTest = new ApiLicenseThreatDTOV2();
    groupTest.licenseThreatGroupName = "Group 3";
    licenses = Collections.singletonList(new ApiLicenseDTOV2(licenseIds.get(0), licenseIds.get(0),
        Collections.singletonList(groupTest)));

    filter = new LicenseLegalApplicationComponentsFilterDTO();
    filter.licenseThreatGroupNames = Sets.newHashSet(groupTest.licenseThreatGroupName);

    result = legalApplicationDashboardService.getLicenseLegalApplicationDashboard(app.getPublicId(), filter);
    assertThat(result).hasSize(1);
    assertApiLicenseLegalApplicationComponentDTO(result.get(0), componentIdentifier1, licenses, 0, 0,
        LicenseObligationReviewStatus.COMPLETED);

    // Filter by LTG which is within a multi license
    groupTest = new ApiLicenseThreatDTOV2();
    groupTest.licenseThreatGroupName = "Group 5";
    licenses =
        Collections.singletonList(
            new ApiLicenseDTOV2("Beerware-Pizzaware", "Beerware or Pizzaware", Collections.singletonList(groupTest)));

    filter = new LicenseLegalApplicationComponentsFilterDTO();
    filter.licenseThreatGroupNames = Sets.newHashSet(groupTest.licenseThreatGroupName);

    result = legalApplicationDashboardService.getLicenseLegalApplicationDashboard(app.getPublicId(), filter);
    assertThat(result).hasSize(1);
    assertApiLicenseLegalApplicationComponentDTO(result.get(0), "hash2", componentIdentifier2, licenses, 0, 0,
        LicenseObligationReviewStatus.COMPLETED);
  }

  @Test
  public void testGetLicenseLegalApplicationDashboard_HasObligationsButNoneSaved() {
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ApplicationComponent applicationComponent =
        tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, "hash", componentIdentifier);
    String effectiveLicenseId = "MIT";
    tempEntity.newApplicationComponentLicense(applicationComponent.getId(), effectiveLicenseId);
    LicenseObligationDTO licenseObligationDTO = new LicenseObligationDTO("obligation", Collections.emptySet());
    LicenseMetadataDTO licenseMetadataDTO = new LicenseMetadataDTO();
    licenseMetadataDTO.setLicenseId(effectiveLicenseId);
    licenseMetadataDTO.setLicenseObligations(Collections.singleton(licenseObligationDTO));
    doReturn(Collections.singletonList(licenseMetadataDTO)).when(mockApiLicenseLegalHdsService)
        .getLicenseMetadata(any());

    List<ApiLicenseLegalApplicationComponentDTO> result =
        legalApplicationDashboardService.getLicenseLegalApplicationDashboard(app.getPublicId(), null);

    assertThat(result).hasSize(1);
    ApiLicenseThreatDTOV2 expectedLtg = new ApiLicenseThreatDTOV2();
    expectedLtg.licenseThreatGroupName = "Liberal";
    assertApiLicenseLegalApplicationComponentDTO(result.get(0), componentIdentifier,
        Collections.singletonList(new ApiLicenseDTOV2(effectiveLicenseId, null, Lists.newArrayList(expectedLtg))), 0, 1,
        LicenseObligationReviewStatus.UNREVIEWED);
  }

  @Test
  public void testGetLicenseLegalApplicationDashboard_IgnoresInnerSourceComponents() {
    Application app = tempEntity.newApplicationWithParent();
    Application otherApp = tempEntity.newApplicationWithParent();

    ComponentIdentifier notInnerSource = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentIdentifier appInnerSource = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2");
    ComponentIdentifier otherAppInnerSource = ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3");

    tempEntity.newInnerSourceApplication(InnerSourceUtils.getVersionlessPackageUrl(appInnerSource).getPackageUrl(),
        app);
    tempEntity.newInnerSourceApplication(InnerSourceUtils.getVersionlessPackageUrl(otherAppInnerSource).getPackageUrl(),
        otherApp);

    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, "hash", notInnerSource);
    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, "hash2", appInnerSource);
    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, "hash3", otherAppInnerSource);

    List<ApiLicenseLegalApplicationComponentDTO> result =
        legalApplicationDashboardService.getLicenseLegalApplicationDashboard(app.getPublicId(), null);

    assertThat(result).hasSize(1);
    assertApiLicenseLegalApplicationComponentDTO(result.get(0), notInnerSource, Collections.emptyList(), 0, 0,
        LicenseObligationReviewStatus.UNREVIEWED);
  }

  @Test
  public void testGetLicenseLegalApplicationDashboard_Conan() {
    ComponentIdentifier componentIdentifier1 = new ComponentIdentifier(ComponentIdentifier.FORMAT_CONAN, Map.of(
        ComponentIdentifier.CONAN_CHANNEL, "",
        ComponentIdentifier.CONAN_OWNER, "",
        ComponentIdentifier.CONAN_NAME, "bzip2",
        ComponentIdentifier.VERSION, "1.0.8"));
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createConanCoordinates("bzip2", "1.0.8", null, null);
    List<String> licenseIds = Collections.singletonList("MIT");
    Application app = setupApplicationWithLicenses(componentIdentifier1, licenseIds.get(0)).getLeft();
    app.setId(Organization.ROOT_ORGANIZATION_ID);
    setupLicenseObligations(app, componentIdentifier2, licenseIds, ObligationStatus.FULFILLED,
        ObligationStatus.FULFILLED);

    List<ApiLicenseLegalApplicationComponentDTO> result =
        legalApplicationDashboardService.getLicenseLegalApplicationDashboard(app.getPublicId(), null);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).reviewCompletedCount).isEqualTo(2);
    assertThat(result.get(0).reviewTotalCount).isEqualTo(2);
  }

  private void setupLicenseObligations(
      Application app,
      ComponentIdentifier componentIdentifier,
      List<String> licenses,
      ObligationStatus... obligationStatuses)
  {
    Set<LicenseObligationDTO> obligationDtos = new LinkedHashSet<>();
    for (int i = 0; i < obligationStatuses.length; i++) {
      if (obligationStatuses[i] != null) {
        tempEntity.newComponentObligation(componentIdentifier, app.getId(), "obligation" + i, "comment",
            obligationStatuses[i], "hash" + i);
        obligationDtos.add(new LicenseObligationDTO("obligation" + i, Collections.emptySet()));
      }
    }

    List<LicenseMetadataDTO> licenseMetadataDTOs = createLicenseMetadataDTOs(licenses);
    for (LicenseMetadataDTO licenseMetadataDTO : licenseMetadataDTOs) {
      licenseMetadataDTO.setLicenseObligations(obligationDtos);
    }

    doReturn(licenseMetadataDTOs).when(mockApiLicenseLegalHdsService)
        .getLicenseMetadata(argThat(list -> list.containsAll(licenses)));
  }

  private void assertApiLicenseLegalApplicationComponentDTO(
      ApiLicenseLegalApplicationComponentDTO dto,
      ComponentIdentifier componentIdentifier,
      List<ApiLicenseDTOV2> licenses,
      int reviewCompletedCount,
      int reviewTotalCount,
      LicenseObligationReviewStatus reviewStatus)
  {
    assertApiLicenseLegalApplicationComponentDTO(dto, "hash", componentIdentifier, licenses, reviewCompletedCount,
        reviewTotalCount, reviewStatus);
  }

  private void assertApiLicenseLegalApplicationComponentDTO(
      ApiLicenseLegalApplicationComponentDTO dto,
      String hash,
      ComponentIdentifier componentIdentifier,
      List<ApiLicenseDTOV2> licenses,
      int reviewCompletedCount,
      int reviewTotalCount,
      LicenseObligationReviewStatus reviewStatus)
  {
    assertThat(dto.hash).isEqualTo(hash);
    assertThat(dto.displayName).isEqualTo(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString());
    assertThat(dto.licenses.stream().map(license -> license.licenseId).collect(Collectors.toSet()))
        .containsExactlyInAnyOrderElementsOf(
            licenses.stream().map(license -> license.licenseId).collect(Collectors.toSet()));
    assertThat(dto.licenses.stream()
        .flatMap(license -> license.licenseThreatGroups.stream())
        .map(group -> group.licenseThreatGroupName)
        .collect(Collectors.toList())).containsExactlyInAnyOrderElementsOf(
            licenses.stream()
                .flatMap(license -> license.licenseThreatGroups.stream())
                .map(group -> group.licenseThreatGroupName)
                .collect(Collectors.toList()));
    assertThat(dto.reviewCompletedCount).isEqualTo(reviewCompletedCount);
    assertThat(dto.reviewTotalCount).isEqualTo(reviewTotalCount);
    assertThat(dto.reviewStatus).isEqualTo(reviewStatus);
  }

  private Triple<Application, Tag, PolicyEvaluation> setupApplicationWithLicenses(
      ComponentIdentifier componentIdentifier,
      String... effectiveLicenseIds)
  {
    Application application = tempEntity.newApplicationWithParent();
    Tag tag = tempEntity.newTag(application.getOrganizationId(), TemporaryEntity.uuid());
    tempEntity.newApplicationTag(application.getId(), tag.getId());
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, TemporaryEntity.uuid(), new Date());
    ApplicationComponent applicationComponent =
        tempEntity.newApplicationComponent(application.getId(), BuildStageType.ID, "hash", componentIdentifier);
    for (String effectiveLicenseId : effectiveLicenseIds) {
      tempEntity.newApplicationComponentLicense(applicationComponent.getId(), effectiveLicenseId);
    }
    return Triple.of(application, tag, policyEvaluation);
  }

  private List<LicenseMetadataDTO> createLicenseMetadataDTOs(Collection<String> licenseIds) {
    return licenseIds.stream().map(this::createLicenseMetadataDTO).collect(Collectors.toList());
  }

  private LicenseMetadataDTO createLicenseMetadataDTO(String licenseId) {
    LicenseMetadataDTO licenseMetadataDTO = new LicenseMetadataDTO();
    licenseMetadataDTO.setLicenseId(licenseId);
    licenseMetadataDTO.setLicenseText("licenseText");
    licenseMetadataDTO.setLicenseObligations(
        new LinkedHashSet<>(Arrays.asList(createLicenseObligationDTO(), createLicenseObligationDTO())));
    return licenseMetadataDTO;
  }

  private LicenseObligationDTO createLicenseObligationDTO() {
    LicenseObligationDTO licenseObligationDTO = new LicenseObligationDTO();
    licenseObligationDTO.setName("name");
    licenseObligationDTO.setObligationTexts(new LinkedHashSet<>(Arrays.asList("obligationText1", "obligationText2")));
    return licenseObligationDTO;
  }

  private void setUnlicensedForAdvancedLegalPack() {
    testProductLicense.setMissingFeatures(LicensedFeature.ADVANCED_LEGAL_PACK);
  }

  private List<ApiLicenseDTOV2> getApiLicenses(List<String> licenseIds) {
    List<ApiLicenseDTOV2> licenses = new ArrayList<>(licenseIds.size());
    ApiLicenseDataAdapter licenseDataAdapter = new ApiLicenseDataAdapter(multiLicenseDAO);

    for (String licenseId : licenseIds) {
      List<ApiLicenseThreatDTOV2> groups =
          licenseThreatGroupDAO.getByOwnerIdAndLicenseId(Organization.ROOT_ORGANIZATION_ID, licenseId)
              .stream()
              .map(licenseDataAdapter::convert)
              .collect(Collectors.toList());

      licenses.add(new ApiLicenseDTOV2(licenseId, licenseId, groups));
    }

    return licenses;
  }
}
