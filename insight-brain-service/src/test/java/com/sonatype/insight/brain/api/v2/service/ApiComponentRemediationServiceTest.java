/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.remediation.VersionScoringDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.hds.ComponentDetailsDTO;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.InvalidPackageURLException;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.inject.Binder;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static com.sonatype.insight.brain.hds.VersionScoringService.HDS_BULK_SCORE_VERSIONING_PATH;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ApiComponentRemediationServiceTest
    extends AbstractComponentTest
{
  private static final String PACKAGE_URL_MAVEN_V1 = "pkg:maven/g1/a1@v1?type=jar";

  private static final String PACKAGE_URL_MAVEN_V2 = "pkg:maven/g1/a1@v2?type=jar";

  public static final String MISSING_COORDINATES = "The following coordinates are missing for given format: ";

  private static final ComponentIdentifier MAVEN_COORDINATES_V1 = ComponentIdentifier.createMavenCoordinates("g1", "a1",
      "v1", "", "jar");

  private static final ComponentIdentifier MAVEN_COORDINATES_V2 = ComponentIdentifier.createMavenCoordinates("g1", "a1",
      "v2", "", "jar");

  private static final ComponentIdentifier MAVEN_COORDINATES_V3 = ComponentIdentifier.createMavenCoordinates("g1", "a1",
      "v3", "", "jar");

  private static final ComponentIdentifier MAVEN_COORDINATES_NO_EXT =
      ComponentIdentifier.createMavenCoordinates("g1", "a1", "v4");

  private static final ComponentIdentifier PYPI_COORDINATES =
      ComponentIdentifier.createPypiCoordinates("n", "v", "q", "e");

  private static final ComponentIdentifier PYPI_COORDINATES_NO_EXT =
      ComponentIdentifier.createPypiCoordinates("n", "v", null, null);

  private static final ComponentIdentifier RPM_COORDINATES = ComponentIdentifier.createRpmCoordinates("n", "v", "a");

  private static final ComponentIdentifier RPM_COORDINATES_NO_ARCH =
      ComponentIdentifier.createRpmCoordinates("n", "v", null);

  final PolicyAlert failAlert = new PolicyAlert(new PolicyFact("policyId", "Policy Name", 10),
      Collections.singletonList(new Action(Action.ID_FAIL)));

  final PolicyAlert warnAlert = new PolicyAlert(new PolicyFact("policyId", "Policy Name", 10),
      Collections.singletonList(new Action(Action.ID_WARN)));

  private Application app;

  @Inject
  private ApiComponentRemediationService service;

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

  @Override
  public void configure(Binder binder) {
    binder.bind(ComponentInfoService.class).toInstance(componentInfoServiceMock);
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
    binder.bind(HdsClient.class).toInstance(hdsClientMock);
    binder.bind(ProductLicense.class).toInstance(productLicense);
    binder.bind(ThirdPartyComponentDAO.class).toInstance(thirdPartyComponentDAO);
    lenient().doReturn(ComponentSummary.create(true))
        .when(hdsClientMock)
        .get(eq(ComponentSummary.class),
            eq("rest/component/summary"), anyMap());
    super.configure(binder);
  }

  @Before
  public void setupApplication() {
    app = tempEntity.newApplicationWithParent();
    mockHdsGetVersionScoringData();
  }

  @Test
  public void testGetSuggestedRemediationForComponent_InvalidVersion() {
    ApiComponentDTOV2 componentDto = new ApiComponentDTOV2();
    componentDto.componentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g1", "t1", "", "", "jar"));

    lenient().doReturn(ComponentSummary.create(false))
        .when(hdsClientMock)
        .get(eq(ComponentSummary.class),
            eq("rest/component/summary"), anyMap());
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getSuggestedRemediationForComponent(componentDto, OwnerType.APPLICATION, app.getId(),
            DevelopStageType.ID, null /* identificationSource */, null /* scanId */, null))
        .withMessage("Invalid Component Identifier or packageUrl");
  }

  @Test
  public void testGetSuggestedRemediationForComponent_InvalidVersion_Purl() {
    ApiComponentDTOV2 componentDto = new ApiComponentDTOV2();
    componentDto.packageUrl = "pkg:maven/g1/a1@abcfeg?type=jar";

    lenient().doReturn(ComponentSummary.create(false))
        .when(hdsClientMock)
        .get(eq(ComponentSummary.class),
            eq("rest/component/summary"), anyMap());
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getSuggestedRemediationForComponent(componentDto, OwnerType.APPLICATION, app.getId(),
            DevelopStageType.ID, null /* identificationSource */, null /* scanId */, null))
        .withMessage("Invalid Component Identifier or packageUrl");
  }

  @Test
  public void testGetSuggestedRemediationForComponent_emptyRequest() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.getSuggestedRemediationForComponent(new ApiComponentDTOV2(), OwnerType.APPLICATION, app.getId(),
            DevelopStageType.ID, null /* identificationSource */, null /* scanId */, null))
        .withMessage("One of either componentIdentifier or packageUrl must be supplied.");
  }

  @Test
  public void testGetSuggestedRemediationForComponent_nullRequest() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getSuggestedRemediationForComponent(null, OwnerType.APPLICATION, app.getId(),
            DevelopStageType.ID, null /* identificationSource */, null /* scanId */, null))
        .withMessage("One of either componentIdentifier or packageUrl must be supplied.");
  }

  @Test
  public void testGetSuggestedRemediationForComponent_invalidComponentIdentifier_NoCoordinates() throws Exception {
    String jsonRequest =
        "{\"hash\":\"h1\",\"componentIdentifier\":{\"format\":\"maven\"},\"proprietary\":false}";
    ApiComponentDTOV2 request = JsonUtils.parse(jsonRequest, ApiComponentDTOV2.class);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> service
        .getSuggestedRemediationForComponent(request, OwnerType.APPLICATION, app.getId(), DevelopStageType.ID,
            null /* identificationSource */, null /* scanId */, null))
        .withMessage("A component identifier must have at least one coordinate.");
  }

  @Test
  public void testGetSuggestedRemediationForComponent_InvalidPackageUrl() {
    ApiComponentDTOV2 component = new ApiComponentDTOV2();
    component.packageUrl = "invalid-package-url";

    assertThatExceptionOfType(InvalidPackageURLException.class).isThrownBy(() -> service
        .getSuggestedRemediationForComponent(component, OwnerType.APPLICATION, app.getId(), DevelopStageType.ID,
            null /* identificationSource */, null /* scanId */, null))
        .withMessage("Invalid package url");
  }

  @Test
  public void testGetSuggestedRemediationForComponent_InvalidPackageUrl_NoExtension() {
    ApiComponentDTOV2 component = new ApiComponentDTOV2();
    component.packageUrl = "pkg:maven/g1/a1@v1";

    assertThatExceptionOfType(InvalidPackageURLException.class).isThrownBy(() -> service
        .getSuggestedRemediationForComponent(component, OwnerType.APPLICATION, app.getId(), DevelopStageType.ID,
            null /* identificationSource */, null /* scanId */, null))
        .withMessage(MISSING_COORDINATES + "[type]");
  }

  @Test
  public void testGetSuggestedRemediationForComponent_InvalidComponentIdentifier_NoExtension() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ApiComponentDTOV2 component = createComponent(componentIdentifier);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> service
        .getSuggestedRemediationForComponent(component, OwnerType.APPLICATION, app.getId(), DevelopStageType.ID,
            null /* identificationSource */, null /* scanId */, null))
        .withMessage(MISSING_COORDINATES + "[extension]");
  }

  @Test
  public void testGetSuggestedRemediationForComponent_BadOwnerId() {
    testGetSuggestedRemediationForComponent_BadOwnerId(OwnerType.APPLICATION, "Application with ID ");
    testGetSuggestedRemediationForComponent_BadOwnerId(OwnerType.ORGANIZATION, "Organization with ID ");
  }

  private void testGetSuggestedRemediationForComponent_BadOwnerId(
      final OwnerType ownerType,
      final String expectedErrMsgPrefix)
  {
    ApiComponentDTOV2 dto = new ApiComponentDTOV2();
    dto.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(MAVEN_COORDINATES_V1);
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> service.getSuggestedRemediationForComponent(dto, ownerType, "bogusOwnerId", DevelopStageType.ID,
            null /* identificationSource */, null /* scanId */, null))
        .withMessageContaining(expectedErrMsgPrefix + "bogusOwnerId");
  }

  @Test
  public void testGetSuggestedRemediationForComponent_NoClassifier() {
    ApiComponentDTOV2 dto = new ApiComponentDTOV2();
    // pass in a component identifier with no classifier
    dto.componentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar"));

    assertNoClassifier(dto);
  }

  @Test
  public void testGetSuggestedRemediationForComponent_NoClassifier_Purl() {
    ApiComponentDTOV2 dto = new ApiComponentDTOV2();
    // pass in a purl with no classifier
    dto.packageUrl = PACKAGE_URL_MAVEN_V1;

    assertNoClassifier(dto);
  }

  private void assertNoClassifier(final ApiComponentDTOV2 dto) {
    ComponentDetailsDTO componentDetailsDTO = new ComponentDetailsDTO();
    componentDetailsDTO.componentIdentifier = MAVEN_COORDINATES_V1;
    componentDetailsDTO.violatedPolicyCount = 0;

    List<ComponentDetailsDTO> list = Stream.of(componentDetailsDTO).collect(Collectors.toList());
    mockHdsGetComponentDetailsList(list, componentDetailsDTO.componentIdentifier);
    mockLicenseFeature(false);

    ApiComponentRemediationDTO retVal = service.getSuggestedRemediationForComponent(dto, OwnerType.APPLICATION,
        app.getId(), DevelopStageType.ID, null /* identificationSource */, null /* scanId */, null);
    assertNoViolations(retVal.remediation, componentDetailsDTO.componentIdentifier,
        PackageUrlIdentifier.toPackageUrl(componentDetailsDTO.componentIdentifier));
    assertTelemetry("application", app.getId(), componentDetailsDTO.componentIdentifier, "option_next_no_violations",
        "option_next_non_failing");
  }

  @Test
  public void testGetSuggestedRemediationForComponent_WithAdvancedRecommendations() {
    ApiComponentDTOV2 dto = new ApiComponentDTOV2();
    dto.packageUrl = PACKAGE_URL_MAVEN_V1;

    ComponentDetailsDTO componentDetailsDTO = new ComponentDetailsDTO();
    componentDetailsDTO.componentIdentifier = MAVEN_COORDINATES_V1;
    componentDetailsDTO.violatedPolicyCount = 0;

    List<ComponentDetailsDTO> list = Stream.of(componentDetailsDTO).collect(Collectors.toList());
    mockHdsGetComponentDetailsList(list, componentDetailsDTO.componentIdentifier);

    // mock dependencies for advanced recommendation strategies
    PackageUrlIdentifier mvnPurlId = PackageUrlIdentifier.fromComponentIdentifier(MAVEN_COORDINATES_V1);
    PackageUrlIdentifier depPurlId = PackageUrlIdentifier.fromComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("g1", "a2", "v1", "", "jar"));
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();
    dependenciesMap.put(mvnPurlId, Collections.singletonList(depPurlId));
    detailsMap.put(depPurlId, new ComponentDetails());
    ComponentDependenciesDTO dependenciesDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    mockHdsGetComponentDependencies(dependenciesDto);
    mockLicenseFeature(true);

    ApiComponentRemediationDTO retVal = service.getSuggestedRemediationForComponent(dto, OwnerType.APPLICATION,
        app.getId(), DevelopStageType.ID, null /* identificationSource */, null /* scanId */, null);
    assertNoViolationsWithAdvancedRecommendations(retVal.remediation,
        PackageUrlIdentifier.toPackageUrl(componentDetailsDTO.componentIdentifier));
    assertTelemetry("application", app.getId(), componentDetailsDTO.componentIdentifier,
        "option_next_no_violations", "option_next_non_failing",
        "option_next_no_violations_with_dependencies", "option_next_non_failing_with_dependencies");
  }

  @Test
  public void testGetSuggestedRemediationForComponent_AllVersionsWithViolations() {
    ApiComponentDTOV2 dto = new ApiComponentDTOV2();
    dto.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(MAVEN_COORDINATES_V1);
    assertAllVersionsWithViolations(dto);
  }

  @Test
  public void testGetSuggestedRemediationForComponent_AllVersionsWithViolations_Purl() {
    ApiComponentDTOV2 dto = new ApiComponentDTOV2();
    dto.packageUrl = PACKAGE_URL_MAVEN_V1;
    assertAllVersionsWithViolations(dto);
  }

  @Test
  public void testGetSuggestedRemediationForComponent_Pypi() {
    assertSuggestedRemediationForComponent_ThirdParty(PYPI_COORDINATES);
  }

  @Test
  public void testGetSuggestedRemediationForComponent_Pypi_NoExt() {
    assertSuggestedRemediationForComponent_ThirdParty(PYPI_COORDINATES_NO_EXT);
  }

  @Test
  public void testGetSuggestedRemediationForComponent_Rpm() {
    assertSuggestedRemediationForComponent_ThirdParty(RPM_COORDINATES);
  }

  @Test
  public void testGetSuggestedRemediationForComponent_Rpm_NoExt() {
    ApiComponentDTOV2 component = createComponent(RPM_COORDINATES_NO_ARCH);
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getSuggestedRemediationForComponent(component, OwnerType.APPLICATION, app.getId(),
            DevelopStageType.ID, null /* identificationSource */, null /* scanId */, null))
        .withMessage("The following coordinates are missing for given format: [architecture]");
  }

  @Test
  public void testGetSuggestedRemediationForComponent_Maven_ThirdParty() {
    assertSuggestedRemediationForComponent_ThirdParty(MAVEN_COORDINATES_V1);
  }

  @Test
  public void testGetSuggestedRemediationForComponent_Pypi_ThirdParty() {
    assertSuggestedRemediationForComponent_ThirdParty(PYPI_COORDINATES);
  }

  @Test
  public void testGetSuggestedRemediationForComponent_Rpm_ThirdParty() {
    assertSuggestedRemediationForComponent_ThirdParty(RPM_COORDINATES);
  }

  @Test
  public void testGetSuggestedRemediationForComponent_Maven_ThirdParty_NoExtension() {
    assertSuggestedRemediationForComponent_ThirdParty(MAVEN_COORDINATES_NO_EXT);
  }

  @Test
  public void testGetSuggestedRemediationForComponent_Pypi_ThirdParty_NoExtension() {
    assertSuggestedRemediationForComponent_ThirdParty(PYPI_COORDINATES_NO_EXT);
  }

  @Test
  public void testGetSuggestedRemediationForComponent_Rpm_ThirdParty_NoArchitecture() {
    assertSuggestedRemediationForComponent_ThirdParty(RPM_COORDINATES_NO_ARCH);
  }

  private void assertSuggestedRemediationForComponent_ThirdParty(ComponentIdentifier componentIdentifier) {
    ApiComponentDTOV2 component = createComponent(componentIdentifier);
    final String identificationSource = "Clair";
    final String scanId = "scanId";
    ComponentDetailsDTO componentDetailsDTO = new ComponentDetailsDTO();
    componentDetailsDTO.componentIdentifier = componentIdentifier;
    componentDetailsDTO.violatedPolicyCount = 1;
    componentDetailsDTO.policyAlerts = Collections.singletonList(failAlert);
    doReturn(ComponentSummary.create(true)).when(thirdPartyComponentDAO)
        .getComponentSummary(componentIdentifier, app.getId(), scanId);
    doReturn(new ApiComponentRemediationValueDTO()).when(thirdPartyComponentDAO)
        .getSuggestedRemmediation(app.getId(),
            componentIdentifier, scanId);
    doReturn(Pair.of(Collections.singletonList(componentDetailsDTO), null)).when(componentInfoServiceMock)
        .getComponentDetailsForAllVersionsNoAuth(any(), eq(componentIdentifier), eq(DevelopStageType.ID),
            eq(identificationSource), eq(scanId), isNull(), any(), anyBoolean());
    ApiComponentRemediationDTO retVal = service
        .getSuggestedRemediationForComponent(component, OwnerType.APPLICATION, app.getId(), DevelopStageType.ID,
            identificationSource, scanId, null);
    assertRemediationZeroCounts(retVal.remediation);
  }

  private void assertAllVersionsWithViolations(final ApiComponentDTOV2 dto) {
    ComponentDetailsDTO dto1 = new ComponentDetailsDTO();
    dto1.componentIdentifier = MAVEN_COORDINATES_V1;
    dto1.violatedPolicyCount = 1;
    dto1.policyAlerts = Collections.singletonList(failAlert);
    ComponentDetailsDTO dto2 = new ComponentDetailsDTO();
    dto2.componentIdentifier = MAVEN_COORDINATES_V2;
    dto2.violatedPolicyCount = 1;
    dto2.policyAlerts = Collections.singletonList(failAlert);
    ComponentDetailsDTO dto3 = new ComponentDetailsDTO();
    dto3.componentIdentifier = MAVEN_COORDINATES_V3;
    dto3.violatedPolicyCount = 1;
    dto3.policyAlerts = Collections.singletonList(failAlert);

    List<ComponentDetailsDTO> list = Stream.of(dto1, dto2, dto3).collect(Collectors.toList());
    mockHdsGetComponentDetailsList(list, dto1.componentIdentifier);

    ApiComponentRemediationDTO retVal = service.getSuggestedRemediationForComponent(dto, OwnerType.APPLICATION,
        app.getId(), DevelopStageType.ID, null /* identificationSource */, null /* scanId */, null);
    assertRemediationZeroCounts(retVal.remediation);
    assertTelemetry("application", app.getId(), dto1.componentIdentifier);
  }

  @Test
  public void testGetSuggestedRemediationForComponent_NoViolations_PreviousNonVulnerableVersion() {
    ApiComponentDTOV2 dto = new ApiComponentDTOV2();
    dto.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(MAVEN_COORDINATES_V2);

    assertNoViolations_PreviousNonVulnerableVersion(dto);
  }

  @Test
  public void testGetSuggestedRemediationForComponent_NoViolations_PreviousNonVulnerableVersion_Purl() {
    ApiComponentDTOV2 dto = new ApiComponentDTOV2();
    dto.packageUrl = PACKAGE_URL_MAVEN_V2;

    assertNoViolations_PreviousNonVulnerableVersion(dto);
  }

  private void assertNoViolations_PreviousNonVulnerableVersion(final ApiComponentDTOV2 dto) {
    ComponentDetailsDTO v1 = new ComponentDetailsDTO();
    v1.componentIdentifier = MAVEN_COORDINATES_V1;
    v1.violatedPolicyCount = 0;
    ComponentDetailsDTO v2 = new ComponentDetailsDTO();
    v2.componentIdentifier = MAVEN_COORDINATES_V2;
    v2.violatedPolicyCount = 1;
    v2.policyAlerts = Collections.singletonList(failAlert);
    ComponentDetailsDTO v3 = new ComponentDetailsDTO();
    v3.componentIdentifier = MAVEN_COORDINATES_V3;
    v3.violatedPolicyCount = 1;
    v3.policyAlerts = Collections.singletonList(failAlert);

    List<ComponentDetailsDTO> list = Stream.of(v1, v2, v3).collect(Collectors.toList());
    mockHdsGetComponentDetailsList(list, v2.componentIdentifier);

    ApiComponentRemediationDTO retVal = service.getSuggestedRemediationForComponent(dto, OwnerType.APPLICATION,
        app.getId(), DevelopStageType.ID, null /* identificationSource */, null /* scanId */, null);
    // we only look forward so we shouldn't downgrade
    assertRemediationZeroCounts(retVal.remediation);
  }

  @Test
  public void testGetSuggestedRemediationForComponent_NoViolations_LastVersion() {
    ComponentDetailsDTO v1 = new ComponentDetailsDTO();
    v1.componentIdentifier = MAVEN_COORDINATES_V1;
    v1.violatedPolicyCount = 1;
    v1.policyAlerts = Collections.singletonList(failAlert);
    ComponentDetailsDTO v2 = new ComponentDetailsDTO();
    v2.componentIdentifier = MAVEN_COORDINATES_V2;
    v2.violatedPolicyCount = 1;
    v2.policyAlerts = Collections.singletonList(failAlert);
    ComponentDetailsDTO v3 = new ComponentDetailsDTO();
    v3.componentIdentifier = MAVEN_COORDINATES_V3;
    v3.violatedPolicyCount = 1;
    v3.policyAlerts = Collections.singletonList(failAlert);

    List<ComponentDetailsDTO> list = Stream.of(v1, v2, v3).collect(Collectors.toList());
    mockHdsGetComponentDetailsList(list, v3.componentIdentifier);

    ApiComponentDTOV2 dto = new ApiComponentDTOV2();
    dto.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(MAVEN_COORDINATES_V3);

    ApiComponentRemediationDTO retVal = service.getSuggestedRemediationForComponent(dto, OwnerType.APPLICATION,
        app.getId(), DevelopStageType.ID, null /* identificationSource */, null /* scanId */, null);
    assertRemediationZeroCounts(retVal.remediation);
  }

  @Test
  public void testGetSuggestedRemediationForComponent_NoViolations_Next() {
    ComponentDetailsDTO v1 = new ComponentDetailsDTO();
    v1.componentIdentifier = MAVEN_COORDINATES_V1;
    v1.violatedPolicyCount = 1;
    v1.policyAlerts = Collections.singletonList(failAlert);
    ComponentDetailsDTO v2 = new ComponentDetailsDTO();
    v2.componentIdentifier = MAVEN_COORDINATES_V2;
    v2.violatedPolicyCount = 0;
    ComponentDetailsDTO v3 = new ComponentDetailsDTO();
    v3.componentIdentifier = MAVEN_COORDINATES_V3;
    v3.violatedPolicyCount = 0;

    List<ComponentDetailsDTO> list = Stream.of(v1, v2, v3).collect(Collectors.toList());
    mockHdsGetComponentDetailsList(list, v1.componentIdentifier);

    ApiComponentDTOV2 dto = new ApiComponentDTOV2();
    dto.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(MAVEN_COORDINATES_V1);

    ApiComponentRemediationDTO retVal = service.getSuggestedRemediationForComponent(dto, OwnerType.APPLICATION,
        app.getId(), DevelopStageType.ID, null /* identificationSource */, null /* scanId */, null);
    assertNoViolations(retVal.remediation, v2.componentIdentifier,
        PackageUrlIdentifier.toPackageUrl(v2.componentIdentifier));
    assertTelemetry("application", app.getId(), v1.componentIdentifier, "option_next_no_violations",
        "option_next_non_failing");
  }

  @Test
  public void testGetSuggestedRemediationForComponent_NoViolations_SameVersion() {
    ComponentDetailsDTO v1 = new ComponentDetailsDTO();
    v1.componentIdentifier = MAVEN_COORDINATES_V1;
    v1.violatedPolicyCount = 0;
    ComponentDetailsDTO v2 = new ComponentDetailsDTO();
    v2.componentIdentifier = MAVEN_COORDINATES_V2;
    v2.violatedPolicyCount = 0;
    ComponentDetailsDTO v3 = new ComponentDetailsDTO();
    v3.componentIdentifier = MAVEN_COORDINATES_V3;
    v3.violatedPolicyCount = 0;

    List<ComponentDetailsDTO> list = Stream.of(v1, v2, v3).collect(Collectors.toList());
    mockHdsGetComponentDetailsList(list, v1.componentIdentifier);

    ApiComponentDTOV2 dto = new ApiComponentDTOV2();
    dto.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(MAVEN_COORDINATES_V1);

    ApiComponentRemediationDTO retVal = service.getSuggestedRemediationForComponent(dto, OwnerType.APPLICATION,
        app.getId(), DevelopStageType.ID, null /* identificationSource */, null /* scanId */, null);
    assertNoViolations(retVal.remediation, v1.componentIdentifier,
        PackageUrlIdentifier.toPackageUrl(v1.componentIdentifier));
    assertTelemetry("application", app.getId(), v1.componentIdentifier, "option_next_no_violations",
        "option_next_non_failing");
  }

  @Test
  public void testGetSuggestedRemediationForComponent_NonFailing() {
    ComponentDetailsDTO v1 = new ComponentDetailsDTO();
    v1.componentIdentifier = MAVEN_COORDINATES_V1;
    v1.violatedPolicyCount = 1;
    v1.policyAlerts = asList(warnAlert, failAlert);
    ComponentDetailsDTO v2 = new ComponentDetailsDTO();
    v2.componentIdentifier = MAVEN_COORDINATES_V2;
    v2.violatedPolicyCount = 1;
    v2.policyAlerts = Collections.singletonList(warnAlert);
    ComponentDetailsDTO v3 = new ComponentDetailsDTO();
    v3.componentIdentifier = MAVEN_COORDINATES_V3;
    v3.violatedPolicyCount = 1;
    v3.policyAlerts = Collections.singletonList(warnAlert);

    List<ComponentDetailsDTO> list = Stream.of(v1, v2, v3).collect(Collectors.toList());
    mockHdsGetComponentDetailsList(list, v1.componentIdentifier);

    ApiComponentDTOV2 dto = new ApiComponentDTOV2();
    dto.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(MAVEN_COORDINATES_V1);

    ApiComponentRemediationDTO retVal = service.getSuggestedRemediationForComponent(dto, OwnerType.APPLICATION,
        app.getId(), DevelopStageType.ID, null /* identificationSource */, null /* scanId */, null);
    assertNonFailing(retVal.remediation, v2.componentIdentifier,
        PackageUrlIdentifier.toPackageUrl(v2.componentIdentifier));
    assertTelemetry("application", app.getId(), v1.componentIdentifier, "option_next_non_failing");
  }

  @Test
  public void testGetSuggestedRemediationForComponent_NoStage() {
    ComponentDetailsDTO v1 = new ComponentDetailsDTO();
    v1.componentIdentifier = MAVEN_COORDINATES_V1;
    v1.violatedPolicyCount = 1;
    v1.policyAlerts = Collections.singletonList(failAlert);
    ComponentDetailsDTO v2 = new ComponentDetailsDTO();
    v2.componentIdentifier = MAVEN_COORDINATES_V2;
    v2.violatedPolicyCount = 1;
    v2.policyAlerts = Collections.singletonList(failAlert);
    ComponentDetailsDTO v3 = new ComponentDetailsDTO();
    v3.componentIdentifier = MAVEN_COORDINATES_V3;
    v3.violatedPolicyCount = 0;

    List<ComponentDetailsDTO> list = Stream.of(v1, v2, v3).collect(Collectors.toList());
    mockHdsGetComponentDetailsList(list, v1.componentIdentifier);

    ApiComponentDTOV2 dto = new ApiComponentDTOV2();
    dto.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(MAVEN_COORDINATES_V1);

    ApiComponentRemediationDTO retVal = service.getSuggestedRemediationForComponent(dto, OwnerType.APPLICATION,
        app.getId(), null /* stageId */, null /* identificationSource */, null /* scanId */, null);

    String expectedPackageUrl = PackageUrlIdentifier.toPackageUrl(v3.componentIdentifier);

    assertThat(retVal.remediation.componentOverrides).hasSize(0);
    assertThat(retVal.remediation.policyWaivers).hasSize(0);

    assertThat(retVal.remediation).isNotNull();

    // no stage implies we do not process non-failing
    assertThat(retVal.remediation.versionChanges).hasSize(1);
    ApiVersionChangeOptionDTO noViolationsOption = retVal.remediation.versionChanges.get(0);
    assertThat(noViolationsOption.getType()).isEqualTo(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS);
    ApiComponentDTOV2 noViolationsDto = noViolationsOption.getData().getComponent();

    assertThat(noViolationsDto.componentIdentifier).isNotNull();
    assertThat(noViolationsDto.componentIdentifier.toComponentIdentifier()).isEqualTo(v3.componentIdentifier);
    assertThat(noViolationsDto.hash).isNull();
    assertThat(noViolationsDto.packageUrl).isEqualTo(expectedPackageUrl);
    assertThat(noViolationsDto.displayName)
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(v3.componentIdentifier).toString());
    assertThat(noViolationsDto.proprietary).isNull();
    assertTelemetry("application", app.getId(), v1.componentIdentifier, "option_next_no_violations");
  }

  @Test
  public void testGetSuggestedRemediationForComponent_BadStageId() {
    ComponentDetailsDTO v1 = new ComponentDetailsDTO();

    ApiComponentDTOV2 dto = new ApiComponentDTOV2();
    dto.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(v1.componentIdentifier);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.getSuggestedRemediationForComponent(dto, OwnerType.APPLICATION, app.getId(), "bogusStageId",
            null /* identificationSource */, null /* scanId */, null))
        .withMessage("Invalid stage ID: bogusStageId.");
  }

  @Test
  public void testGetSuggestedRemediationForComponent_Repository() {
    Repository repo = tempEntity.newRepository();
    ApiComponentDTOV2 apiComponentDTOV2 = new ApiComponentDTOV2();
    apiComponentDTOV2.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(MAVEN_COORDINATES_V1);
    ComponentDetailsDTO dto1 = new ComponentDetailsDTO();
    dto1.componentIdentifier = MAVEN_COORDINATES_V1;
    dto1.violatedPolicyCount = 1;
    dto1.policyAlerts = Collections.singletonList(failAlert);
    ComponentDetailsDTO dto2 = new ComponentDetailsDTO();
    dto2.componentIdentifier = MAVEN_COORDINATES_V2;
    ComponentDetailsDTO dto3 = new ComponentDetailsDTO();
    dto3.componentIdentifier = MAVEN_COORDINATES_V3;

    List<ComponentDetailsDTO> componentDetailsDTOList = Arrays.asList(dto1, dto2, dto3);
    mockHdsGetComponentDetailsList(componentDetailsDTOList, dto1.componentIdentifier);

    ApiComponentRemediationDTO apiComponentRemediationDTO = service
        .getSuggestedRemediationForComponent(apiComponentDTOV2, OwnerType.REPOSITORY, repo.getId(), ProxyStageType.ID,
            null /* identificationSource */, null /* scanId */, null);

    assertNoViolations(apiComponentRemediationDTO.remediation, dto2.componentIdentifier,
        PackageUrlIdentifier.toPackageUrl(dto2.componentIdentifier));
    assertTelemetry("repository", repo.getId(), dto1.componentIdentifier, "option_next_no_violations",
        "option_next_non_failing");
  }

  @Test
  public void testGetSuggestedRemediationForComponent_Repository_StageIdNull() {
    Repository repo = tempEntity.newRepository();
    ApiComponentDTOV2 apiComponentDTOV2 = new ApiComponentDTOV2();
    apiComponentDTOV2.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(MAVEN_COORDINATES_V1);
    ComponentDetailsDTO dto1 = new ComponentDetailsDTO();
    dto1.componentIdentifier = MAVEN_COORDINATES_V1;
    dto1.violatedPolicyCount = 1;
    dto1.policyAlerts = Collections.singletonList(failAlert);
    ComponentDetailsDTO dto2 = new ComponentDetailsDTO();
    dto2.componentIdentifier = MAVEN_COORDINATES_V2;
    ComponentDetailsDTO dto3 = new ComponentDetailsDTO();
    dto3.componentIdentifier = MAVEN_COORDINATES_V3;

    List<ComponentDetailsDTO> componentDetailsDTOList = Arrays.asList(dto1, dto2, dto3);
    mockHdsGetComponentDetailsList(componentDetailsDTOList, dto1.componentIdentifier);

    ApiComponentRemediationDTO apiComponentRemediationDTO = service
        .getSuggestedRemediationForComponent(apiComponentDTOV2, OwnerType.REPOSITORY, repo.getId(), null /* stageId */,
            null /* identificationSource */, null /* scanId */, null);

    assertNoViolations(apiComponentRemediationDTO.remediation, dto2.componentIdentifier,
        PackageUrlIdentifier.toPackageUrl(dto2.componentIdentifier));
    assertTelemetry("repository", repo.getId(), dto1.componentIdentifier, "option_next_no_violations",
        "option_next_non_failing");
  }

  @Test
  public void testGetSuggestedRemediationForComponent_Repository_NotProxyStage() {
    Repository repo = tempEntity.newRepository();

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getSuggestedRemediationForComponent(new ApiComponentDTOV2(), OwnerType.REPOSITORY,
            repo.getId(), BuildStageType.ID, null /* identificationSource */, null /* scanId */, null))
        .withMessage("Invalid stage ID for repositories: build.");
  }

  @Test
  public void testGetSuggestedRemediationForComponent_Repository_ScanIdNotNull() {
    Repository repo = tempEntity.newRepository();

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getSuggestedRemediationForComponent(new ApiComponentDTOV2(), OwnerType.REPOSITORY,
            repo.getId(), null /* stageId */, null /* identificationSource */, "testScanId", null))
        .withMessage("The scan ID is not allowed for repositories.");
  }

  private void assertTelemetry(
      final String ownerType,
      final String ownerId,
      final ComponentIdentifier componentIdentifier,
      final String... expectedTrueAttributes)
  {
    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock).send(telemetryDataArgumentCaptor.capture());
    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();
    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("owner_type", ownerType);
    expectedAttributes.put("owner_id", HdsClientAnalytics.obfuscate(ownerId));
    expectedAttributes.put("real_owner_id", ownerId);
    expectedAttributes.put("component", HdsClientAnalytics.obfuscate(JsonUtils.writeUnformatted(componentIdentifier)));
    expectedAttributes.put("option_next_no_violations", "false");
    expectedAttributes.put("option_next_non_failing", "false");
    expectedAttributes.put("option_next_no_violations_with_dependencies", "false");
    expectedAttributes.put("option_next_non_failing_with_dependencies", "false");
    for (String attribute : expectedTrueAttributes) {
      expectedAttributes.put(attribute, "true");
    }
    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.COMPONENT_REMEDIATION);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    assertThat(telemetryData.getAttributes()).isEqualTo(expectedAttributes);
  }

  private void mockHdsGetComponentDetailsList(List<ComponentDetailsDTO> list, ComponentIdentifier componentIdentifier) {
    lenient().doReturn(Pair.of(list, null))
        .when(componentInfoServiceMock)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), eq(componentIdentifier), any(), any(), any(), isNull(), any(), anyBoolean());
  }

  private void mockHdsGetComponentDependencies(ComponentDependenciesDTO dependenciesDto) {
    when(hdsClientMock.post(eq(ComponentDependenciesDTO.class), eq("rest/component/dependencies"), anyCollection()))
        .thenReturn(dependenciesDto);
  }

  private void mockHdsGetVersionScoringData() {
    lenient().when(hdsClientMock.post(eq(VersionScoringDTO[].class), eq(HDS_BULK_SCORE_VERSIONING_PATH), anyList(),
        eq(Map.of("stableVersionsOnly", "true"))))
        .thenReturn(new VersionScoringDTO[]{});
  }

  private void mockLicenseFeature(boolean includeAdvancedStrategies) {
    when(productLicense.hasFeature(eq(LicensedFeature.ADVANCED_RECOMMENDATION_STRATEGIES)))
        .thenReturn(includeAdvancedStrategies);
  }

  private void assertRemediationZeroCounts(ApiComponentRemediationValueDTO apiComponentRemediationValueDTO) {
    assertThat(apiComponentRemediationValueDTO.componentOverrides).hasSize(0);
    assertThat(apiComponentRemediationValueDTO.policyWaivers).hasSize(0);
    assertThat(apiComponentRemediationValueDTO.versionChanges).hasSize(0);
  }

  private void assertNoViolations(
      ApiComponentRemediationValueDTO apiComponentRemediationValueDTO,
      ComponentIdentifier expectedComponentIdentifier,
      String expectedPackageUrl)
  {
    assertThat(apiComponentRemediationValueDTO.componentOverrides).hasSize(0);
    assertThat(apiComponentRemediationValueDTO.policyWaivers).hasSize(0);

    assertThat(apiComponentRemediationValueDTO).isNotNull();
    assertThat(apiComponentRemediationValueDTO.versionChanges).hasSize(1);
    ApiVersionChangeOptionDTO noViolationsOption = apiComponentRemediationValueDTO.versionChanges.get(0);
    assertThat(noViolationsOption.getType()).isEqualTo(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS);
    ApiComponentDTOV2 noViolationsDto = noViolationsOption.getData().getComponent();

    assertThat(noViolationsDto.componentIdentifier).isNotNull();
    assertThat(noViolationsDto.componentIdentifier.toComponentIdentifier()).isEqualTo(expectedComponentIdentifier);
    assertThat(noViolationsDto.hash).isNull();
    assertThat(noViolationsDto.packageUrl).isEqualTo(expectedPackageUrl);
    assertThat(noViolationsDto.displayName)
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(expectedComponentIdentifier).toString());
    assertThat(noViolationsDto.proprietary).isNull();
  }

  private void assertNonFailing(
      ApiComponentRemediationValueDTO apiComponentRemediationValueDTO,
      ComponentIdentifier expectedComponentIdentifier,
      String expectedPackageUrl)
  {
    assertThat(apiComponentRemediationValueDTO.componentOverrides).hasSize(0);
    assertThat(apiComponentRemediationValueDTO.policyWaivers).hasSize(0);

    assertThat(apiComponentRemediationValueDTO).isNotNull();
    assertThat(apiComponentRemediationValueDTO.versionChanges).hasSize(1);

    ApiVersionChangeOptionDTO nonFailingOption = apiComponentRemediationValueDTO.versionChanges.get(0);
    assertThat(nonFailingOption.getType()).isEqualTo(ApiVersionChangeOptionType.NEXT_NON_FAILING);
    ApiComponentDTOV2 nonFailingDto = nonFailingOption.getData().getComponent();

    assertThat(nonFailingDto.componentIdentifier).isNotNull();
    assertThat(nonFailingDto.componentIdentifier.toComponentIdentifier()).isEqualTo(expectedComponentIdentifier);
    assertThat(nonFailingDto.hash).isNull();
    assertThat(nonFailingDto.packageUrl).isEqualTo(expectedPackageUrl);
    assertThat(nonFailingDto.displayName)
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(expectedComponentIdentifier).toString());
    assertThat(nonFailingDto.proprietary).isNull();
  }

  private void assertNoViolationsWithAdvancedRecommendations(
      ApiComponentRemediationValueDTO apiComponentRemediationValueDTO,
      String expectedPackageUrl)
  {
    assertThat(apiComponentRemediationValueDTO.componentOverrides).hasSize(0);
    assertThat(apiComponentRemediationValueDTO.policyWaivers).hasSize(0);

    assertThat(apiComponentRemediationValueDTO).isNotNull();
    assertThat(apiComponentRemediationValueDTO.versionChanges).hasSize(1);

    ApiVersionChangeOptionDTO noViolationsWithDepOption = apiComponentRemediationValueDTO.versionChanges.get(0);
    assertThat(noViolationsWithDepOption.getType())
        .isEqualTo(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES);
    ApiComponentDTOV2 noViolationsWithDepDto = noViolationsWithDepOption.getData().getComponent();
    assertThat(noViolationsWithDepDto.packageUrl).isEqualTo(expectedPackageUrl);
    assertThat(noViolationsWithDepDto.displayName).isEqualTo(ComponentDisplayNameUtil
        .fromIdentifier(noViolationsWithDepOption.getData().getComponent().componentIdentifier.toComponentIdentifier())
        .toString());
  }

  private ApiComponentDTOV2 createComponent(final ComponentIdentifier componentIdentifier) {
    ApiComponentDTOV2 component = new ApiComponentDTOV2();
    component.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);
    return component;
  }
}
