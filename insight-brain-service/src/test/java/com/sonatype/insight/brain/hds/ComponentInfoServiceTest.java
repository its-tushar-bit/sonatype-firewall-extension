/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import static com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType.NEXT_NON_FAILING_WITH_DEPENDENCIES;
import static com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES;
import static com.sonatype.insight.brain.hds.VersionScoringService.HDS_BULK_SCORE_VERSIONING_PATH;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.ALP_OBSERVED_LICENSE_DETECTION_ENABLED;
import static com.sonatype.insight.brain.model.license.License.NOT_SUPPORTED_ID;
import static com.sonatype.insight.brain.model.license.License.UNSPECIFIED_ID;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentCategory;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.clm.dto.model.ide.LicenseStatus;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.remediation.VersionScoringDTO;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.api.v2.service.ApiComponentDetailsServiceV2;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestDAO;
import com.sonatype.insight.brain.git.ManualPullRequestImpossibilityReason;
import com.sonatype.insight.brain.hds.AutomatedRemediationStatusDTO.ManualPullRequestNotPossibleDTO;
import com.sonatype.insight.brain.hds.AutomatedRemediationStatusDTO.PullRequestCreationFailedDTO;
import com.sonatype.insight.brain.hds.AutomatedRemediationStatusDTO.PullRequestCreationPendingDTO;
import com.sonatype.insight.brain.hds.AutomatedRemediationStatusDTO.PullRequestDTO;
import com.sonatype.insight.brain.hds.ComponentInfoService.ComponentLicenses;
import com.sonatype.insight.brain.hds.ComponentInfoService.ComponentMultiLicenses;
import com.sonatype.insight.brain.hds.ComponentInfoService.ComponentSecurityVulnerabilities;
import com.sonatype.insight.brain.hds.ComponentInfoService.LicenseWithThreatLevel;
import com.sonatype.insight.brain.hds.ComponentInfoService.MultiLicenseWithThreatLevel;
import com.sonatype.insight.brain.hds.HdsClient.RelayResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.ComponentDataSource;
import com.sonatype.insight.brain.model.component.DependencyType;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.actions.WarnActionType;
import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.brain.model.policy.conditions.DataSourceConditionType;
import com.sonatype.insight.brain.model.policy.conditions.DependencyTypeConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ProprietaryConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ProprietaryNameConflictConditionType;
import com.sonatype.insight.brain.model.policy.conditions.RelativePopularityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.SourceStageType;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestState;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.policy.evaluator.ComponentPolicyEvaluator;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.report.ReportDataReader;
import com.sonatype.insight.brain.repository.ProprietaryComponentNameDetector;
import com.sonatype.insight.brain.repository.RepositoryAllVersionsResponse;
import com.sonatype.insight.brain.repository.ProxyRepositoryComponentResult;
import com.sonatype.insight.brain.repository.RepositoryQueryService;
import com.sonatype.insight.brain.repository.RepositorySourceResponseDTO;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.util.MetadataRecorderUtils;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import com.google.common.cache.LoadingCache;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.groups.Tuple;
import org.jooq.exception.DataAccessException;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class ComponentInfoServiceTest
    extends AbstractComponentTest
{
  private static final ComponentIdentifier MAVEN_A1_COORDINATES = ComponentIdentifier.createMavenCoordinates("g1", "a1",
      "v1", "", "jar");

  private static final ComponentIdentifier MAVEN_A2_COORDINATES = ComponentIdentifier.createMavenCoordinates("g1", "a2",
      "v1", "", "jar");

  private static final ComponentIdentifier NUGET_COORDINATES = ComponentIdentifier.createNugetCoordinates("a", "v");

  private static final ComponentIdentifier NPM_COORDINATES = ComponentIdentifier.createNpmCoordinates("p1", "v1");

  private static final ComponentIdentifier GENERIC_COORDINATES =
      ComponentIdentifier.createGenericCoordinates("g1", "a1", null);

  // This is the tool name (ci, ide, rm) used in REST paths for HDS resources. Since we use it when we mock the HDS
  // client, it doesn't really matter what value we use here, because we don't really access HDS REST paths.
  private static final String TOOL_NAME = "ci";

  @Inject
  private MultiLicenseDAO multiLicenseDAO;

  @Inject
  private SourceControlEventDAO sourceControlEventDAO;

  @Inject
  private ComponentInfoService componentInfoService;

  @Inject
  private ComponentDetailsLoaderFactory componentDetailsLoaderFactory;

  private Application application;

  private Repository repository;

  @Mock
  private ProductLicense productLicenseMock;

  @Mock
  private HdsClient hdsClientMock;

  @Mock
  private ApiComponentDetailsServiceV2 apiComponentDetailsServiceV2Mock;

  @Mock
  private HttpServletRequest httpRequestMock;

  @Mock
  private ThirdPartyComponentDAO thirdPartyComponentDAO;

  /**
   * Named with a "Spy" suffix (rather than {@code @Mock}/{@code @Spy}-annotated) so the test harness wraps the
   * real, database-backed {@link SourceControlPullRequestDAO} bean in a Mockito spy instead of replacing it
   * with a bare mock. This keeps tests that rely on real pull request rows (created via {@code tempEntity})
   * working, while still letting the one test below that exercises DB-failure resilience stub a specific call.
   */
  private SourceControlPullRequestDAO sourceControlPullRequestDAOSpy;

  @Mock
  private RepositoryQueryService repositoryQueryService;

  @Mock
  private ReportDataReader reportDataReader;

  @Inject
  private ApiConfigurationService configurationService;

  @Inject
  private Configuration configuration;

  @Inject
  private IdUtils idUtils;

  @Inject
  private OrganizationDAO organizationDAO;

  @Before
  public void before() {
    resetProprietaryComponentNameDetector();
    resetComponentPolicyEvaluatorCache();

    componentInfoService.setToolName(TOOL_NAME);

    application = tempEntity.newApplicationWithParent();
    repository = tempEntity.newRepository();

    mockHdsGetVersionScoringData();
  }

  private NamedComponentDetails newNamedComponentDetails(ComponentIdentifier componentIdentifier) {
    NamedComponentDetails namedComponentDetails = new NamedComponentDetails();
    namedComponentDetails.setComponentIdentifier(componentIdentifier);
    return namedComponentDetails;
  }

  private Map<String, String> newCoordinatesQueryParam(NamedComponentDetails componentDetails) {
    Map<String, String> queryParams = new HashMap<>();
    if (componentDetails.getHash() != null) {
      queryParams.put("hash", componentDetails.getHash());
    }
    queryParams
        .put("componentIdentifier", ComponentIdentifierAdapter.toJson(componentDetails.getComponentIdentifier()));
    return queryParams;
  }

  private ProprietaryComponentNameDetector getProprietaryComponentNameDetector() {
    try {
      Field field = ComponentDetailsLoaderFactory.class.getDeclaredField("proprietaryComponentNameDetector");
      field.setAccessible(true);
      return (ProprietaryComponentNameDetector) field.get(componentDetailsLoaderFactory);
    }
    catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to access ProprietaryComponentNameDetector from test", e);
    }
  }

  private void resetProprietaryComponentNameDetector() {
    try {
      Method method = ProprietaryComponentNameDetector.class.getDeclaredMethod("invalidateMatchers");
      method.setAccessible(true);
      method.invoke(getProprietaryComponentNameDetector());
    }
    catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to reset ProprietaryComponentNameDetector cache", e);
    }
  }

  private void resetComponentPolicyEvaluatorCache() {
    try {
      Field field = ComponentPolicyEvaluator.class.getDeclaredField("droolsCodeKiaBase");
      field.setAccessible(true);
      ((LoadingCache<?, ?>) field.get(null)).invalidateAll();
    }
    catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to reset ComponentPolicyEvaluator cache", e);
    }
  }

  private void mockLicenseFeature(boolean includeAdvancedStrategies) {
    when(productLicenseMock.hasFeature(eq(LicensedFeature.ADVANCED_RECOMMENDATION_STRATEGIES)))
        .thenReturn(includeAdvancedStrategies);
  }

  private void mockHdsGetComponentDependencies(ComponentDependenciesDTO dependenciesDto) {
    when(hdsClientMock.post(eq(ComponentDependenciesDTO.class), eq("rest/component/dependencies"), anyCollection()))
        .thenReturn(dependenciesDto);
  }

  private void mockHdsGetComponentDetails(NamedComponentDetails hdsComponentDetails) throws IOException {
    when(
        hdsClientMock.relay(httpRequestMock, NamedComponentDetails.class, "rest/" + TOOL_NAME + "/componentDetails",
            newCoordinatesQueryParam(hdsComponentDetails))).thenReturn(new RelayResponse<>(hdsComponentDetails));
  }

  private void mockHdsGetComponentDetailsException(NamedComponentDetails hdsComponentDetails) throws IOException {
    when(hdsClientMock.relay(httpRequestMock, NamedComponentDetails.class, "rest/" + TOOL_NAME + "/componentDetails",
        newCoordinatesQueryParam(hdsComponentDetails))).thenThrow(NotFoundException.class);
  }

  private void mockHdsGetComponentDetailsListBulk(
      List<ComponentIdentifier> componentIdentifiers,
      String responsePath,
      String responseVersion)
  {
    Map<String, List<String>> stringListMap =
        Collections.singletonMap(responsePath, Collections.singletonList(responseVersion));

    when(hdsClientMock.post(Map.class, "rest/component/versions/list", componentIdentifiers, Map.of(
        "stableVersionsOnly", "false")))
            .thenReturn(stringListMap);
  }

  private void mockGetComponentDetailsListFromHds(
      List<ComponentEvaluationDataList.ComponentEvaluationData> componentEvaluationDataList)
  {
    when(apiComponentDetailsServiceV2Mock.getComponentDetailsListFromHds(anyList(), any(String.class)))
        .thenReturn(componentEvaluationDataList);
  }

  private void mockHdsGetComponentDetailsList(
      ComponentDetailsList hdsComponentDetailsList,
      ComponentIdentifier identifier)
  {
    mockHdsGetComponentDetailsList(hdsComponentDetailsList, identifier, true);
  }

  private void mockHdsGetComponentDetailsList(
      ComponentDetailsList hdsComponentDetailsList,
      ComponentIdentifier identifier,
      boolean stableVersionsOnly)
  {
    when(hdsClientMock.get(ComponentDetailsList.class, "rest/" + TOOL_NAME +
        "/componentDetails/list",
        Map.of("componentIdentifier", ComponentIdentifierAdapter.toJson(identifier), "stableVersionsOnly",
            String.valueOf(stableVersionsOnly))))
                .thenReturn(hdsComponentDetailsList);
  }

  @Test
  public void testGetSelectableLicenses() throws Exception {
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);

    // Verify that UNSPECIFIED is removed from the result
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("EPL-1.0", "UNSPECIFIED"));
    mockHdsGetComponentDetails(hdsComponentDetails);
    List<License> licenses = componentInfoService.getLicenses(OwnerType.APPLICATION, application.getPublicId(),
        MAVEN_A1_COORDINATES, httpRequestMock, null, null).selectableLicenses;
    assertThat(licenses).extracting(License::getLicenseId).containsExactlyInAnyOrder("EPL-1.0");

    // Verify that a versionless license is resolved to versioned licenses
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("Apache-UNSPECIFIED"));
    mockHdsGetComponentDetails(hdsComponentDetails);
    licenses = componentInfoService.getLicenses(OwnerType.APPLICATION, application.getPublicId(), MAVEN_A1_COORDINATES,
        httpRequestMock, null, null).selectableLicenses;
    assertThat(licenses).extracting(License::getLicenseId)
        .containsExactlyInAnyOrder("Apache-UNSPECIFIED", "Apache-1.0",
            "Apache-1.1", "Apache-2.0", "Apache-2.0-with-Astramind-MA", "Apache-XML-Security-License",
            "Apache-2.0-with-LLVM-exception", "Apache-2.0-with-Commons-Clause-1.0", "Apache-2.0-with-Swift-exception",
            "Apache-2.0-with-Commercial-Use-Enforcer", "Apache-2.0-with-Commons-Clause-UNSPECIFIED");

    // Verify that declared and observed licenses are merged
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("Apache-2.0", "EPL-1.0"));
    hdsComponentDetails.setObservedLicenses(toLicenseSet("EPL-1.0", "GPL-2.0"));
    mockHdsGetComponentDetails(hdsComponentDetails);
    licenses = componentInfoService.getLicenses(OwnerType.APPLICATION, application.getPublicId(), MAVEN_A1_COORDINATES,
        httpRequestMock, null, null).selectableLicenses;
    assertThat(licenses).extracting(License::getLicenseId)
        .containsExactlyInAnyOrder("Apache-2.0", "EPL-1.0",
            "GPL-2.0");
  }

  @Test
  public void testGetLicenses_NoComponentIdentifier() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> componentInfoService.getLicenses(null, null, null /* componentIdentifier */, httpRequestMock, null,
                null))
        .withMessage("componentIdentifier is required");
  }

  @Test
  public void testgetMultiLicensesNoAuth_NoComponentIdentifier() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> componentInfoService.getMultiLicensesNoAuth(null, null, null /* componentIdentifier */,
            httpRequestMock, null, null))
        .withMessage("componentIdentifier is required");
  }

  @Test
  public void testGetLicenses_BadOwnerId() {
    testGetLicenses_BadOwnerId(OwnerType.APPLICATION, "Application with ID ");
    testGetLicenses_BadOwnerId(OwnerType.REPOSITORY, "Repository with ID ");
  }

  @Test
  public void testgetMultiLicensesNoAuth_BadOwnerId() {
    testgetMultiLicensesNoAuth_BadOwnerId(OwnerType.APPLICATION, "Application with ID ");
    testgetMultiLicensesNoAuth_BadOwnerId(OwnerType.REPOSITORY, "Repository with ID ");
  }

  private void testGetLicenses_BadOwnerId(final OwnerType ownerType, final String expectedErrMsgPrefix) {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> componentInfoService.getLicenses(ownerType, "bogusOwnerId", MAVEN_A1_COORDINATES, httpRequestMock, null,
            null))
        .withMessageContaining(expectedErrMsgPrefix + "bogusOwnerId");
  }

  private void testgetMultiLicensesNoAuth_BadOwnerId(OwnerType ownerType, String expectedErrMsgPrefix) {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> componentInfoService.getMultiLicensesNoAuth(ownerType, "bogusOwnerId", MAVEN_A1_COORDINATES,
            httpRequestMock, null,
            null))
        .withMessageContaining(expectedErrMsgPrefix + "bogusOwnerId");
  }

  @Test
  public void testGetLicensesApplication() throws Exception {
    testGetLicenses(OwnerType.APPLICATION, application.getPublicId());
  }

  @Test
  public void testgetMultiLicensesNoAuthApplication() throws Exception {
    testgetMultiLicensesNoAuth(OwnerType.APPLICATION, application.getPublicId());
  }

  @Test
  public void testGetLicensesRepository() throws Exception {
    testGetLicenses(OwnerType.REPOSITORY, repository.getId());
  }

  @Test
  public void testgetMultiLicensesNoAuthRepository() throws Exception {
    testgetMultiLicensesNoAuth(OwnerType.REPOSITORY, repository.getId());
  }

  @Test
  public void testGetLicenses_ThirdParty() throws Exception {
    String scanId = "scanId";
    final String identificationSource = IdentificationSource.CLAIR.getId();
    NamedComponentDetails tpsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    tpsComponentDetails.setMatchState(MatchState.EXACT.getId());
    tpsComponentDetails.setIdentificationSource(identificationSource);

    when(thirdPartyComponentDAO.getComponentDetailsByIdentifier(MAVEN_A1_COORDINATES, application.getId(), scanId))
        .thenReturn(tpsComponentDetails);

    ComponentLicenses licenses =
        componentInfoService.getLicenses(OwnerType.APPLICATION, application.getPublicId(), MAVEN_A1_COORDINATES,
            httpRequestMock, identificationSource, scanId);

    assertLicenses(licenses.declaredlicenses, tuple(UNSPECIFIED_ID, "Not Provided", 5));
    assertLicenses(licenses.observedlicenses, tuple(UNSPECIFIED_ID, "Not Provided", 5));
    assertLicenses(licenses.effectiveLicenses, tuple(UNSPECIFIED_ID, "Not Provided", 5));
  }

  @Test
  public void testgetMultiLicensesNoAuth_ThirdParty() throws Exception {
    String scanId = "scanId";
    String identificationSource = IdentificationSource.CLAIR.getId();
    NamedComponentDetails tpsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    tpsComponentDetails.setMatchState(MatchState.EXACT.getId());
    tpsComponentDetails.setIdentificationSource(identificationSource);

    when(thirdPartyComponentDAO.getComponentDetailsByIdentifier(MAVEN_A1_COORDINATES, application.getId(), scanId))
        .thenReturn(tpsComponentDetails);

    ComponentMultiLicenses licenses =
        componentInfoService.getMultiLicensesNoAuth(OwnerType.APPLICATION, application.getPublicId(),
            MAVEN_A1_COORDINATES, httpRequestMock, identificationSource, scanId);

    assertMultiLicenses(licenses.declaredLicenses, tuple(UNSPECIFIED_ID, "Not Provided", 5));
    assertMultiLicenses(licenses.observedLicenses, tuple(UNSPECIFIED_ID, "Not Provided", 5));
    assertMultiLicenses(licenses.effectiveLicenses, tuple(UNSPECIFIED_ID, "Not Provided", 5));
    assertThat(licenses.hiddenObservedLicenses).isFalse();
    assertThat(licenses.supportAlpObservedLicenses).isFalse();
  }

  @Test
  public void testGetLicenses_PackageManifest() throws Exception {
    String scanId = "scanId";
    String identificationSource = IdentificationSource.PACKAGE_MANIFEST.getId();
    NamedComponentDetails componentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    componentDetails.setMatchState(MatchState.EXACT.getId());
    componentDetails.setIdentificationSource(identificationSource);

    mockHdsGetComponentDetailsException(componentDetails);

    ComponentLicenses licenses = componentInfoService.getLicenses(OwnerType.APPLICATION, application.getPublicId(),
        MAVEN_A1_COORDINATES, httpRequestMock, identificationSource, scanId);

    assertLicenses(licenses.declaredlicenses, tuple(UNSPECIFIED_ID, "Not Provided", 5));
    assertLicenses(licenses.observedlicenses, tuple(UNSPECIFIED_ID, "Not Provided", 5));
    assertLicenses(licenses.effectiveLicenses, tuple(UNSPECIFIED_ID, "Not Provided", 5));
    assertThat(licenses.selectableLicenses).isEmpty();
  }

  @Test
  public void testgetMultiLicensesNoAuth_PackageManifest() throws Exception {
    String scanId = "scanId";
    String identificationSource = IdentificationSource.PACKAGE_MANIFEST.getId();
    NamedComponentDetails componentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    componentDetails.setMatchState(MatchState.EXACT.getId());
    componentDetails.setIdentificationSource(identificationSource);

    mockHdsGetComponentDetailsException(componentDetails);

    ComponentMultiLicenses licenses =
        componentInfoService.getMultiLicensesNoAuth(OwnerType.APPLICATION, application.getPublicId(),
            MAVEN_A1_COORDINATES, httpRequestMock, identificationSource, scanId);

    assertMultiLicenses(licenses.declaredLicenses, tuple(UNSPECIFIED_ID, "Not Provided", 5));
    assertMultiLicenses(licenses.observedLicenses, tuple(UNSPECIFIED_ID, "Not Provided", 5));
    assertMultiLicenses(licenses.effectiveLicenses, tuple(UNSPECIFIED_ID, "Not Provided", 5));
    assertThat(licenses.selectableLicenses).isEmpty();
    assertThat(licenses.hiddenObservedLicenses).isFalse();
    assertThat(licenses.supportAlpObservedLicenses).isFalse();
  }

  private void testGetLicenses(final OwnerType ownerType, final String ownerId) throws Exception {
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);

    // Verify component without licenses
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentLicenses licenses = componentInfoService.getLicenses(ownerType, ownerId, MAVEN_A1_COORDINATES,
        httpRequestMock, null, null);
    assertLicenses(licenses.declaredlicenses, tuple(UNSPECIFIED_ID, "Not Provided", 5));
    assertLicenses(licenses.observedlicenses, tuple(UNSPECIFIED_ID, "Not Provided", 5));
    assertLicenses(licenses.effectiveLicenses, tuple(UNSPECIFIED_ID, "Not Provided", 5));
    assertThat(licenses.selectableLicenses).isEmpty();

    final String privateOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);

    // Verify component with licenses
    tempEntity.newLicenseThreatGroup(
        // Note: For now, only an Org or App (not a Repository) can contain a LTG
        OwnerType.APPLICATION.equals(ownerType) ? privateOwnerId : Organization.ROOT_ORGANIZATION_ID,
        "ComponentInfoServiceTest", 5, "LGPL-2.0", "BSD-3-Clause");

    hdsComponentDetails.getEffectiveLicenses().clear();
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("Apache-2.0", "LGPL-2.0-MPL-1.1"));
    hdsComponentDetails.setObservedLicenses(toLicenseSet("GPL-2.0", "AFL-2.1-BSD-3-Clause"));
    mockHdsGetComponentDetails(hdsComponentDetails);
    licenses = componentInfoService.getLicenses(ownerType, ownerId, MAVEN_A1_COORDINATES, httpRequestMock, null, null);
    assertLicenses(licenses.declaredlicenses, tuple("Apache-2.0", "Apache-2.0", 0), tuple("LGPL-2.0", "LGPL-2.0", 5),
        tuple("MPL-1.1", "MPL-1.1", 2));
    assertLicenses(licenses.observedlicenses, tuple("GPL-2.0", "GPL-2.0", 9), tuple("AFL-2.1", "AFL-2.1", 2),
        tuple("BSD-3-Clause", "BSD-3-Clause", 5));
    assertLicenses(licenses.effectiveLicenses, tuple("Apache-2.0", "Apache-2.0", 0), tuple("LGPL-2.0", "LGPL-2.0", 5),
        tuple("MPL-1.1", "MPL-1.1", 2), tuple("GPL-2.0", "GPL-2.0", 9), tuple("AFL-2.1", "AFL-2.1", 2),
        tuple("BSD-3-Clause", "BSD-3-Clause", 5));
    assertThat(licenses.selectableLicenses).extracting(License::getLicenseId)
        .containsExactlyInAnyOrder("Apache-2.0",
            "LGPL-2.0", "MPL-1.1", "GPL-2.0", "BSD-3-Clause", "AFL-2.1");
  }

  private void testgetMultiLicensesNoAuth(final OwnerType ownerType, final String ownerId) throws Exception {
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);

    // Verify component without licenses
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentMultiLicenses licenses =
        componentInfoService.getMultiLicensesNoAuth(ownerType, ownerId, MAVEN_A1_COORDINATES, httpRequestMock, null,
            null);
    assertMultiLicenses(licenses.declaredLicenses, tuple(UNSPECIFIED_ID, "Not Provided", 5));
    assertMultiLicenses(licenses.observedLicenses, tuple(UNSPECIFIED_ID, "Not Provided", 5));
    assertMultiLicenses(licenses.effectiveLicenses, tuple(UNSPECIFIED_ID, "Not Provided", 5));
    assertThat(licenses.selectableLicenses).isEmpty();
    assertThat(licenses.hiddenObservedLicenses).isFalse();
    assertThat(licenses.supportAlpObservedLicenses).isFalse();

    String privateOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);

    // Verify component with licenses
    tempEntity.newLicenseThreatGroup(
        // Note: For now, only an Org or App (not a Repository) can contain a LTG
        OwnerType.APPLICATION.equals(ownerType) ? privateOwnerId : Organization.ROOT_ORGANIZATION_ID,
        "ComponentInfoServiceTest", 5, "LGPL-2.0", "BSD-3-Clause");

    hdsComponentDetails.getEffectiveLicenses().clear();
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("Apache-2.0", "LGPL-2.0-MPL-1.1"));
    hdsComponentDetails.setObservedLicenses(toLicenseSet("GPL-2.0", "AFL-2.1-BSD-3-Clause"));
    mockHdsGetComponentDetails(hdsComponentDetails);
    licenses =
        componentInfoService.getMultiLicensesNoAuth(ownerType, ownerId, MAVEN_A1_COORDINATES, httpRequestMock, null,
            null);
    assertMultiLicenses(licenses.declaredLicenses, tuple("Apache-2.0", "Apache-2.0", 0),
        tuple("LGPL-2.0", "LGPL-2.0", 5),
        tuple("MPL-1.1", "MPL-1.1", 2));
    assertMultiLicenses(licenses.observedLicenses, tuple("GPL-2.0", "GPL-2.0", 9), tuple("AFL-2.1", "AFL-2.1", 2),
        tuple("BSD-3-Clause", "BSD-3-Clause", 5));
    assertMultiLicenses(licenses.effectiveLicenses, tuple("Apache-2.0", "Apache-2.0", 0),
        tuple("LGPL-2.0", "LGPL-2.0", 5),
        tuple("MPL-1.1", "MPL-1.1", 2), tuple("GPL-2.0", "GPL-2.0", 9), tuple("AFL-2.1", "AFL-2.1", 2),
        tuple("BSD-3-Clause", "BSD-3-Clause", 5));
    assertThat(licenses.selectableLicenses).extracting(License::getLicenseId)
        .containsExactlyInAnyOrder("Apache-2.0",
            "LGPL-2.0", "MPL-1.1", "GPL-2.0", "BSD-3-Clause", "AFL-2.1");
    assertThat(licenses.hiddenObservedLicenses).isFalse();
    assertThat(licenses.supportAlpObservedLicenses).isFalse();
  }

  @Test
  public void testGetLicensesApplication_withOverride() throws Exception {
    testGetLicenses_withOverride(OwnerType.APPLICATION, application.getPublicId());
  }

  @Test
  public void testgetMultiLicensesNoAuthApplication_withOverride() throws Exception {
    testgetMultiLicensesNoAuth_withOverride(OwnerType.APPLICATION, application.getPublicId());
  }

  @Test
  public void testGetLicensesRepository_withOverride() throws Exception {
    testGetLicenses_withOverride(OwnerType.REPOSITORY, repository.getId());
  }

  @Test
  public void testgetMultiLicensesNoAuthRepository_withOverride() throws Exception {
    testgetMultiLicensesNoAuth_withOverride(OwnerType.REPOSITORY, repository.getId());
  }

  private void testGetLicenses_withOverride(final OwnerType ownerType, final String ownerId) throws Exception {
    final String privateOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);

    // Verify component with licenses
    // Note: For now, only an Org or App (not a Repository) can contain a LTG
    final String tempEntityOwnerId = OwnerType.APPLICATION.equals(ownerType)
        ? privateOwnerId
        : Organization.ROOT_ORGANIZATION_ID;
    tempEntity.newLicenseThreatGroup(tempEntityOwnerId, "ComponentInfoServiceTest", 5, "LGPL-2.0", "BSD-3-Clause");
    tempEntity.newLicenseOverride(tempEntityOwnerId, MAVEN_A1_COORDINATES, LicenseOverrideStatus.SELECTED,
        "BSD-3-Clause");

    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("Apache-2.0", "LGPL-2.0-MPL-1.1"));
    hdsComponentDetails.setObservedLicenses(toLicenseSet("GPL-2.0", "AFL-2.1-BSD-3-Clause"));
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentLicenses licenses = componentInfoService.getLicenses(ownerType, ownerId, MAVEN_A1_COORDINATES,
        httpRequestMock, null, null);
    assertLicenses(licenses.declaredlicenses, tuple("Apache-2.0", "Apache-2.0", 0), tuple("LGPL-2.0", "LGPL-2.0", 5),
        tuple("MPL-1.1", "MPL-1.1", 2));
    assertLicenses(licenses.observedlicenses, tuple("GPL-2.0", "GPL-2.0", 9), tuple("AFL-2.1", "AFL-2.1", 2),
        tuple("BSD-3-Clause", "BSD-3-Clause", 5));
    assertLicenses(licenses.effectiveLicenses, tuple("BSD-3-Clause", "BSD-3-Clause", 5));
  }

  private void testgetMultiLicensesNoAuth_withOverride(OwnerType ownerType, String ownerId) throws Exception {
    String privateOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);

    // Verify component with licenses
    // Note: For now, only an Org or App (not a Repository) can contain a LTG
    String tempEntityOwnerId =
        OwnerType.APPLICATION.equals(ownerType) ? privateOwnerId : Organization.ROOT_ORGANIZATION_ID;
    tempEntity.newLicenseThreatGroup(tempEntityOwnerId, "ComponentInfoServiceTest", 5, "LGPL-2.0", "BSD-3-Clause");
    tempEntity.newLicenseOverride(tempEntityOwnerId, MAVEN_A1_COORDINATES, LicenseOverrideStatus.SELECTED,
        "BSD-3-Clause");

    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("Apache-2.0", "LGPL-2.0-MPL-1.1"));
    hdsComponentDetails.setObservedLicenses(toLicenseSet("GPL-2.0", "AFL-2.1-BSD-3-Clause"));
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentMultiLicenses licenses =
        componentInfoService.getMultiLicensesNoAuth(ownerType, ownerId, MAVEN_A1_COORDINATES, httpRequestMock, null,
            null);
    assertMultiLicenses(licenses.declaredLicenses, tuple("Apache-2.0", "Apache-2.0", 0),
        tuple("LGPL-2.0", "LGPL-2.0", 5), tuple("MPL-1.1", "MPL-1.1", 2));
    assertMultiLicenses(licenses.observedLicenses, tuple("GPL-2.0", "GPL-2.0", 9), tuple("AFL-2.1", "AFL-2.1", 2),
        tuple("BSD-3-Clause", "BSD-3-Clause", 5));
    assertMultiLicenses(licenses.effectiveLicenses, tuple("BSD-3-Clause", "BSD-3-Clause", 5));
    assertThat(licenses.hiddenObservedLicenses).isFalse();
    assertThat(licenses.supportAlpObservedLicenses).isFalse();
  }

  @Test
  public void testGetLicenses_withNotDeclaredForDeclaredLicenses() throws Exception {
    testGetLicenses_withNotDeclaredForDeclaredLicenses(OwnerType.APPLICATION, application.getPublicId());
    testGetLicenses_withNotDeclaredForDeclaredLicenses(OwnerType.REPOSITORY, repository.getId());
  }

  @Test
  public void testgetMultiLicensesNoAuth_withNotDeclaredForDeclaredLicenses() throws Exception {
    testgetMultiLicensesNoAuth_withNotDeclaredForDeclaredLicenses(OwnerType.APPLICATION, application.getPublicId());
    testgetMultiLicensesNoAuth_withNotDeclaredForDeclaredLicenses(OwnerType.REPOSITORY, repository.getId());
  }

  private void testGetLicenses_withNotDeclaredForDeclaredLicenses(
      final OwnerType ownerType,
      final String ownerId) throws Exception
  {
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("Not-Declared"));
    hdsComponentDetails.setObservedLicenses(toLicenseSet("GPL-2.0"));
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentLicenses licenses = componentInfoService.getLicenses(ownerType, ownerId, MAVEN_A1_COORDINATES,
        httpRequestMock, null, null);
    assertLicenses(licenses.declaredlicenses, tuple("Not-Declared", "Not Declared", 5));
    assertLicenses(licenses.observedlicenses, tuple("GPL-2.0", "GPL-2.0", 9));
    assertLicenses(licenses.effectiveLicenses, tuple("GPL-2.0", "GPL-2.0", 9));
  }

  private void testgetMultiLicensesNoAuth_withNotDeclaredForDeclaredLicenses(
      OwnerType ownerType,
      String ownerId) throws Exception
  {
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("Not-Declared"));
    hdsComponentDetails.setObservedLicenses(toLicenseSet("GPL-2.0"));
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentMultiLicenses licenses =
        componentInfoService.getMultiLicensesNoAuth(ownerType, ownerId, MAVEN_A1_COORDINATES, httpRequestMock, null,
            null);
    assertMultiLicenses(licenses.declaredLicenses, tuple("Not-Declared", "Not Declared", 5));
    assertMultiLicenses(licenses.observedLicenses, tuple("GPL-2.0", "GPL-2.0", 9));
    assertMultiLicenses(licenses.effectiveLicenses, tuple("GPL-2.0", "GPL-2.0", 9));
    assertThat(licenses.hiddenObservedLicenses).isFalse();
    assertThat(licenses.supportAlpObservedLicenses).isFalse();
  }

  @Test
  public void testGetLicenses_withNoSourcesForObservedLicenses() throws Exception {
    testGetLicenses_withNoSourcesForObservedLicenses(OwnerType.APPLICATION, application.getPublicId());
    testGetLicenses_withNoSourcesForObservedLicenses(OwnerType.REPOSITORY, repository.getId());
  }

  @Test
  public void testgetMultiLicensesNoAuth_withNoSourcesForObservedLicenses() throws Exception {
    testgetMultiLicensesNoAuth_withNoSourcesForObservedLicenses(OwnerType.APPLICATION, application.getPublicId());
    testgetMultiLicensesNoAuth_withNoSourcesForObservedLicenses(OwnerType.REPOSITORY, repository.getId());
  }

  private void testGetLicenses_withNoSourcesForObservedLicenses(
      final OwnerType ownerType,
      final String ownerId) throws Exception
  {
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("GPL-2.0"));
    hdsComponentDetails.setObservedLicenses(toLicenseSet("No-Sources"));
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentLicenses licenses = componentInfoService.getLicenses(ownerType, ownerId, MAVEN_A1_COORDINATES,
        httpRequestMock, null, null);
    assertLicenses(licenses.declaredlicenses, tuple("GPL-2.0", "GPL-2.0", 9));
    assertLicenses(licenses.observedlicenses, tuple("No-Sources", "No Sources", 5));
    assertLicenses(licenses.effectiveLicenses, tuple("GPL-2.0", "GPL-2.0", 9));
  }

  private void testgetMultiLicensesNoAuth_withNoSourcesForObservedLicenses(
      OwnerType ownerType,
      String ownerId) throws Exception
  {
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("GPL-2.0"));
    hdsComponentDetails.setObservedLicenses(toLicenseSet("No-Sources"));
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentMultiLicenses licenses =
        componentInfoService.getMultiLicensesNoAuth(ownerType, ownerId, MAVEN_A1_COORDINATES, httpRequestMock, null,
            null);
    assertMultiLicenses(licenses.declaredLicenses, tuple("GPL-2.0", "GPL-2.0", 9));
    assertMultiLicenses(licenses.observedLicenses, tuple("No-Sources", "No Sources", 5));
    assertMultiLicenses(licenses.effectiveLicenses, tuple("GPL-2.0", "GPL-2.0", 9));
    assertThat(licenses.hiddenObservedLicenses).isFalse();
    assertThat(licenses.supportAlpObservedLicenses).isFalse();
  }

  @Test
  public void testGetLicenses_withNoSourceLicenseForObservedLicenses() throws Exception {
    testGetLicenses_withNoSourceLicenseForObservedLicenses(OwnerType.APPLICATION, application.getPublicId());
    testGetLicenses_withNoSourceLicenseForObservedLicenses(OwnerType.REPOSITORY, repository.getId());
  }

  @Test
  public void testgetMultiLicensesNoAuth_withNoSourceLicenseForObservedLicenses() throws Exception {
    testgetMultiLicensesNoAuth_withNoSourceLicenseForObservedLicenses(OwnerType.APPLICATION, application.getPublicId());
    testgetMultiLicensesNoAuth_withNoSourceLicenseForObservedLicenses(OwnerType.REPOSITORY, repository.getId());
  }

  private void testGetLicenses_withNoSourceLicenseForObservedLicenses(
      final OwnerType ownerType,
      final String ownerId) throws Exception
  {
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("GPL-2.0"));
    hdsComponentDetails.setObservedLicenses(toLicenseSet("No-Source-License"));
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentLicenses licenses = componentInfoService.getLicenses(ownerType, ownerId, MAVEN_A1_COORDINATES,
        httpRequestMock, null, null);
    assertLicenses(licenses.declaredlicenses, tuple("GPL-2.0", "GPL-2.0", 9));
    assertLicenses(licenses.observedlicenses, tuple("No-Source-License", "No Source License", 5));
    assertLicenses(licenses.effectiveLicenses, tuple("GPL-2.0", "GPL-2.0", 9));
  }

  private void testgetMultiLicensesNoAuth_withNoSourceLicenseForObservedLicenses(
      OwnerType ownerType,
      String ownerId) throws Exception
  {
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("GPL-2.0"));
    hdsComponentDetails.setObservedLicenses(toLicenseSet("No-Source-License"));
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentMultiLicenses licenses =
        componentInfoService.getMultiLicensesNoAuth(ownerType, ownerId, MAVEN_A1_COORDINATES, httpRequestMock, null,
            null);
    assertMultiLicenses(licenses.declaredLicenses, tuple("GPL-2.0", "GPL-2.0", 9));
    assertMultiLicenses(licenses.observedLicenses, tuple("No-Source-License", "No Source License", 5));
    assertMultiLicenses(licenses.effectiveLicenses, tuple("GPL-2.0", "GPL-2.0", 9));
    assertThat(licenses.hiddenObservedLicenses).isFalse();
    assertThat(licenses.supportAlpObservedLicenses).isFalse();
  }

  @Test
  public void testGetLicenses_withNotDeclaredForDeclaredLicensesAndNoSourcesForObservedLicenses() throws Exception {
    testGetLicenses_withNotDeclaredForDeclaredLicensesAndNoSourcesForObservedLicenses(OwnerType.APPLICATION,
        application.getPublicId());
    testGetLicenses_withNotDeclaredForDeclaredLicensesAndNoSourcesForObservedLicenses(OwnerType.REPOSITORY,
        repository.getId());
  }

  @Test
  public void testgetMultiLicensesNoAuth_withNotDeclaredLicensesAndNoSourcesForObservedLicenses() throws Exception {
    testgetMultiLicensesNoAuth_withNotDeclaredForDeclaredLicensesAndNoSourcesForObservedLicenses(OwnerType.APPLICATION,
        application.getPublicId());
    testgetMultiLicensesNoAuth_withNotDeclaredForDeclaredLicensesAndNoSourcesForObservedLicenses(OwnerType.REPOSITORY,
        repository.getId());
  }

  private void testGetLicenses_withNotDeclaredForDeclaredLicensesAndNoSourcesForObservedLicenses(
      final OwnerType ownerType,
      final String ownerId) throws Exception
  {
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("Not-Declared"));
    hdsComponentDetails.setObservedLicenses(toLicenseSet("No-Source-License"));
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentLicenses licenses = componentInfoService.getLicenses(ownerType, ownerId, MAVEN_A1_COORDINATES,
        httpRequestMock, null, null);
    assertLicenses(licenses.declaredlicenses, tuple("Not-Declared", "Not Declared", 5));
    assertLicenses(licenses.observedlicenses, tuple("No-Source-License", "No Source License", 5));
    assertLicenses(licenses.effectiveLicenses, tuple("Not-Declared", "Not Declared", 5),
        tuple("No-Source-License", "No Source License", 5));
  }

  private void testgetMultiLicensesNoAuth_withNotDeclaredForDeclaredLicensesAndNoSourcesForObservedLicenses(
      OwnerType ownerType,
      String ownerId) throws Exception
  {
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("Not-Declared"));
    hdsComponentDetails.setObservedLicenses(toLicenseSet("No-Source-License"));
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentMultiLicenses licenses =
        componentInfoService.getMultiLicensesNoAuth(ownerType, ownerId, MAVEN_A1_COORDINATES, httpRequestMock, null,
            null);
    assertMultiLicenses(licenses.declaredLicenses, tuple("Not-Declared", "Not Declared", 5));
    assertMultiLicenses(licenses.observedLicenses, tuple("No-Source-License", "No Source License", 5));
    assertMultiLicenses(licenses.effectiveLicenses, tuple("Not-Declared", "Not Declared", 5),
        tuple("No-Source-License", "No Source License", 5));
    assertThat(licenses.hiddenObservedLicenses).isFalse();
    assertThat(licenses.supportAlpObservedLicenses).isFalse();
  }

  @Test
  public void testGetLicenses_withNotSupportedLicense() throws Exception {
    testGetLicenses_withNotSupportedLicense(OwnerType.APPLICATION,
        application.getPublicId());
    testGetLicenses_withNotSupportedLicense(OwnerType.REPOSITORY,
        repository.getId());
  }

  @Test
  public void testgetMultiLicensesNoAuth_withNotSupportedLicense() throws Exception {
    testgetMultiLicensesNoAuth_withNotSupportedLicense(OwnerType.APPLICATION, application.getPublicId());
    testgetMultiLicensesNoAuth_withNotSupportedLicense(OwnerType.REPOSITORY, repository.getId());
  }

  private void testGetLicenses_withNotSupportedLicense(
      final OwnerType ownerType,
      final String ownerId) throws Exception
  {
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(NUGET_COORDINATES);
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("MIT"));
    hdsComponentDetails.setObservedLicenses(toLicenseSet("Not-Supported"));
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentLicenses licenses = componentInfoService.getLicenses(ownerType, ownerId, NUGET_COORDINATES,
        httpRequestMock, null, null);
    assertLicenses(licenses.declaredlicenses, tuple("MIT", "MIT", 0));
    assertLicenses(licenses.observedlicenses, tuple("Not-Supported", "Not Supported", null));
    assertLicenses(licenses.effectiveLicenses, tuple("MIT", "MIT", 0));
    assertThat(licenses.selectableLicenses).isNotEmpty()
        .extracting(License::getLicenseId)
        .doesNotContain("Not-Supported");
  }

  private void testgetMultiLicensesNoAuth_withNotSupportedLicense(
      OwnerType ownerType,
      String ownerId) throws Exception
  {
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(NUGET_COORDINATES);
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("MIT"));
    hdsComponentDetails.setObservedLicenses(toLicenseSet("Not-Supported"));
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentMultiLicenses licenses =
        componentInfoService.getMultiLicensesNoAuth(ownerType, ownerId, NUGET_COORDINATES, httpRequestMock, null, null);
    assertMultiLicenses(licenses.declaredLicenses, tuple("MIT", "MIT", 0));
    assertMultiLicenses(licenses.observedLicenses, tuple("Not-Supported", "Not Supported", null));
    assertMultiLicenses(licenses.effectiveLicenses, tuple("MIT", "MIT", 0));
    assertThat(licenses.selectableLicenses).isNotEmpty()
        .extracting(License::getLicenseId)
        .doesNotContain("Not-Supported");
    assertThat(licenses.hiddenObservedLicenses).isFalse();
    assertThat(licenses.supportAlpObservedLicenses).isTrue();
  }

  @Test
  public void testGetLicensesApplication_claimedComponent() throws Exception {
    testGetLicenses_claimedComponent(OwnerType.APPLICATION, application.getPublicId());
  }

  @Test
  public void testgetMultiLicensesNoAuthApplication_claimedComponent() throws Exception {
    testgetMultiLicensesNoAuth_claimedComponent(OwnerType.APPLICATION, application.getPublicId());
  }

  @Test
  public void testGetLicensesRepository_claimedComponent() throws Exception {
    testGetLicenses_claimedComponent(OwnerType.REPOSITORY, repository.getId());
  }

  @Test
  public void testgetMultiLicensesNoAuthRepository_claimedComponent() throws Exception {
    testgetMultiLicensesNoAuth_claimedComponent(OwnerType.REPOSITORY, repository.getId());
  }

  private void testGetLicenses_claimedComponent(final OwnerType ownerType, final String ownerId) throws Exception {
    // Verify exception is not thrown if component is not known to HDS
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("componentIdentifier", ComponentIdentifierAdapter.toJson(MAVEN_A1_COORDINATES));

    when(
        hdsClientMock.relay(httpRequestMock, NamedComponentDetails.class, "rest/" + TOOL_NAME + "/componentDetails",
            queryParams)).thenThrow(new NotFoundException("test"));
    ComponentLicenses licenses = componentInfoService.getLicenses(ownerType, ownerId, MAVEN_A1_COORDINATES,
        httpRequestMock, null, null);
    // if we got here, we are good, but let's do some sanity check
    assertLicenses(licenses.declaredlicenses, tuple(UNSPECIFIED_ID, "Not Provided", 5));
    assertLicenses(licenses.observedlicenses, tuple(UNSPECIFIED_ID, "Not Provided", 5));
    assertLicenses(licenses.effectiveLicenses, tuple(UNSPECIFIED_ID, "Not Provided", 5));
  }

  private void testgetMultiLicensesNoAuth_claimedComponent(OwnerType ownerType, String ownerId) throws Exception {
    // Verify exception is not thrown if component is not known to HDS
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("componentIdentifier", ComponentIdentifierAdapter.toJson(MAVEN_A1_COORDINATES));

    when(hdsClientMock.relay(httpRequestMock, NamedComponentDetails.class, "rest/" + TOOL_NAME + "/componentDetails",
        queryParams)).thenThrow(new NotFoundException("test"));
    ComponentMultiLicenses licenses =
        componentInfoService.getMultiLicensesNoAuth(ownerType, ownerId, MAVEN_A1_COORDINATES, httpRequestMock, null,
            null);
    // if we got here, we are good, but let's do some sanity check
    assertMultiLicenses(licenses.declaredLicenses, tuple(UNSPECIFIED_ID, "Not Provided", 5));
    assertMultiLicenses(licenses.observedLicenses, tuple(UNSPECIFIED_ID, "Not Provided", 5));
    assertMultiLicenses(licenses.effectiveLicenses, tuple(UNSPECIFIED_ID, "Not Provided", 5));
    assertThat(licenses.hiddenObservedLicenses).isFalse();
    assertThat(licenses.supportAlpObservedLicenses).isFalse();
  }

  @Test
  public void testgetMultiLicensesNoAuth_HiddenObservedLicenses() throws Exception {
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(NPM_COORDINATES);
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("MIT"));
    hdsComponentDetails.setObservedLicenses(toLicenseSet("Apache-2.0"));
    mockHdsGetComponentDetails(hdsComponentDetails);

    configurationService.setConfigurationInDatabaseNoAuthz(ALP_OBSERVED_LICENSE_DETECTION_ENABLED, false);
    configuration.configurationChanged(Collections.singleton(ALP_OBSERVED_LICENSE_DETECTION_ENABLED));

    ComponentMultiLicenses licenses = componentInfoService.getMultiLicensesNoAuth(OwnerType.APPLICATION,
        application.getPublicId(), NPM_COORDINATES, httpRequestMock, null, null);

    assertMultiLicenses(licenses.declaredLicenses, tuple("MIT", "MIT", 0));
    assertMultiLicenses(licenses.observedLicenses, tuple(NOT_SUPPORTED_ID, "Not Supported", null));
    assertMultiLicenses(licenses.effectiveLicenses, tuple("MIT", "MIT", 0));
    assertThat(licenses.hiddenObservedLicenses).isTrue();
    assertThat(licenses.supportAlpObservedLicenses).isTrue();
  }

  @Test
  public void testGetLicenses_DeduplicationOfLicenses() throws Exception {
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("LGPL-2.1", "LGPL-2.1+", "Apache-2.0-LGPL-2.1"));
    hdsComponentDetails.setObservedLicenses(toLicenseSet("LGPL-2.1", "LGPL-2.1+", "Apache-2.0-LGPL-2.1"));
    mockHdsGetComponentDetails(hdsComponentDetails);

    ComponentLicenses licenses = componentInfoService.getLicenses(OwnerType.APPLICATION, application.getPublicId(),
        MAVEN_A1_COORDINATES, httpRequestMock, null, null);

    assertLicenses(licenses.declaredlicenses, tuple("LGPL-2.1", "LGPL-2.1", 2), tuple("LGPL-3.0", "LGPL-3.0", 2),
        tuple("Apache-2.0", "Apache-2.0", 0));
    assertLicenses(licenses.observedlicenses, tuple("LGPL-2.1", "LGPL-2.1", 2), tuple("LGPL-3.0", "LGPL-3.0", 2),
        tuple("Apache-2.0", "Apache-2.0", 0));
    assertLicenses(licenses.effectiveLicenses, tuple("LGPL-2.1", "LGPL-2.1", 2), tuple("LGPL-3.0", "LGPL-3.0", 2),
        tuple("Apache-2.0", "Apache-2.0", 0));
  }

  private void assertLicenses(Iterable<LicenseWithThreatLevel> actual, Tuple... tuples) {
    assertThat(actual).extracting(lwtl -> lwtl.license.getLicenseId(), lwtl -> lwtl.license.getLicenseName(),
        lwtl -> lwtl.threatLevel).containsExactlyInAnyOrder(tuples);
  }

  private void assertMultiLicenses(Iterable<MultiLicenseWithThreatLevel> actual, Tuple... tuples) {
    assertThat(actual)
        .flatExtracting(mlwtl -> mlwtl.licenses)
        .extracting(
            lwtl -> lwtl.license.getLicenseId(), lwtl -> lwtl.license.getLicenseName(), lwtl -> lwtl.threatLevel)
        .containsExactlyInAnyOrder(tuples);
  }

  @Test
  public void testGetComponentDetailsList() {
    // Create an application without LTGs
    Organization organization = tempEntity.newOrganization("testGetComponentDetailsList");
    String applicationPublicId = "testGetComponentDetailsList";
    Application application = tempEntity.newApplication(applicationPublicId, applicationPublicId, organization.getId());
    String appId = application.getId();
    // Create license threat groups
    tempEntity.newLicenseThreatGroup(appId, "Group1", 9, "Apache-2.0");
    // Various LTG groups to test case insensitive ordering
    tempEntity.newLicenseThreatGroup(appId, "groupA", 10, "GPL-2.0");
    tempEntity.newLicenseThreatGroup(appId, "Groupb", 10, "GPL-2.0");
    tempEntity.newLicenseThreatGroup(appId, "GroupC", 10, "GPL-2.0");

    // Create the mocked hds response
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "1.0.0");
    ComponentDetails hdsComponentDetails1 = newNamedComponentDetails(componentIdentifier1);
    Set<License> licenses1 = new LinkedHashSet<>();
    licenses1.add(new License("Apache-2.0", "Apache-2.0"));
    hdsComponentDetails1.setDeclaredLicenses(licenses1);
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "2.0.0");
    ComponentDetails hdsComponentDetails2 = newNamedComponentDetails(componentIdentifier2);
    Set<License> licenses2 = new LinkedHashSet<>();
    licenses2.add(new License("GPL-2.0", "GPL-2.0"));
    hdsComponentDetails2.setDeclaredLicenses(licenses2);
    // This should match the default LTG Copyleft from the root organization
    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "3.0.0");
    ComponentDetails hdsComponentDetails3 = newNamedComponentDetails(componentIdentifier3);
    Set<License> licenses3 = new LinkedHashSet<>();
    licenses3.add(new License("OSL-1.0", "OSL-1.0"));
    hdsComponentDetails3.setDeclaredLicenses(licenses3);
    // generic component identifier
    ComponentIdentifier componentIdentifierGeneric = ComponentIdentifier.createGenericCoordinates("a1", "1.0", null);
    ComponentDetails hdsComponentDetailsGeneric = newNamedComponentDetails(componentIdentifierGeneric);
    Set<License> licenses4 = new LinkedHashSet<>();
    licenses4.add(new License(UNSPECIFIED_ID, "Not Provided"));
    hdsComponentDetailsGeneric.setEffectiveLicenses(licenses4);
    // mock hdsComponentDetailsList
    ComponentDetailsList hdsComponentDetailsList = new ComponentDetailsList();
    hdsComponentDetailsList.setList(asList(hdsComponentDetails1, hdsComponentDetails2, hdsComponentDetails3,
        hdsComponentDetailsGeneric));
    mockHdsGetComponentDetailsList(hdsComponentDetailsList, componentIdentifier1);

    ComponentDetailsList componentDetailsList =
        componentInfoService.getComponentDetailsList(componentIdentifier1, null, null, null, null,
            true).getLeft();
    componentDetailsLoaderFactory.newInstance(application)
        .augmentComponentDetails(componentDetailsList.getList(),
            MatchState.EXACT.getId(), null);

    assertThat(componentDetailsList).isNotNull();
    assertThat(componentDetailsList.getList()).hasSize(4);
    ComponentDetails componentDetails = componentDetailsList.getList().get(0);
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(componentIdentifier1);
    assertThat(componentDetails.getLicenseThreatLevel()).isEqualTo(9);
    assertThat(componentDetails.getLicenseThreatGroupNames()).hasSize(1);
    assertThat(componentDetails.getLicenseThreatGroupNames().get(0)).isEqualTo("Group1");
    assertThat(componentDetails.getDeclaredLicenses()).hasSize(1);
    assertThat(componentDetails.getDeclaredLicenses().iterator().next().getLicenseName()).isEqualTo("Apache-2.0");
    assertThat(componentDetails.getDeclaredLicenses().iterator().next().getLicenseId()).isEqualTo("Apache-2.0");
    assertThat(componentDetails.getObservedLicenses()).isEmpty();
    assertThat(componentDetails.getEffectiveLicenses()).hasSize(1);
    assertThat(componentDetails.getEffectiveLicenses().iterator().next().getLicenseName()).isEqualTo("Apache-2.0");
    assertThat(componentDetails.getEffectiveLicenses().iterator().next().getLicenseId()).isEqualTo("Apache-2.0");
    assertThat(componentDetails.getEffectiveLicenseStatus()).isNull();
    componentDetails = componentDetailsList.getList().get(1);
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(componentIdentifier2);
    assertThat(componentDetails.getLicenseThreatLevel()).isEqualTo(10);
    assertThat(componentDetails.getLicenseThreatGroupNames()).containsExactly("groupA", "Groupb", "GroupC");
    assertThat(componentDetails.getDeclaredLicenses()).hasSize(1);
    assertThat(componentDetails.getDeclaredLicenses().iterator().next().getLicenseName()).isEqualTo("GPL-2.0");
    assertThat(componentDetails.getDeclaredLicenses().iterator().next().getLicenseId()).isEqualTo("GPL-2.0");
    assertThat(componentDetails.getObservedLicenses()).isEmpty();
    assertThat(componentDetails.getEffectiveLicenses()).hasSize(1);
    assertThat(componentDetails.getEffectiveLicenses().iterator().next().getLicenseName()).isEqualTo("GPL-2.0");
    assertThat(componentDetails.getEffectiveLicenses().iterator().next().getLicenseId()).isEqualTo("GPL-2.0");
    assertThat(componentDetails.getEffectiveLicenseStatus()).isNull();
    // Test match against default LGT Copyleft from the root organization
    componentDetails = componentDetailsList.getList().get(2);
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(componentIdentifier3);
    assertThat(componentDetails.getLicenseThreatLevel()).isEqualTo(9);
    assertThat(componentDetails.getLicenseThreatGroupNames()).containsExactly("Copyleft");
    assertThat(componentDetails.getDeclaredLicenses()).hasSize(1);
    assertThat(componentDetails.getDeclaredLicenses().iterator().next().getLicenseName()).isEqualTo("OSL-1.0");
    assertThat(componentDetails.getDeclaredLicenses().iterator().next().getLicenseId()).isEqualTo("OSL-1.0");
    assertThat(componentDetails.getObservedLicenses()).isEmpty();
    assertThat(componentDetails.getEffectiveLicenses()).hasSize(1);
    assertThat(componentDetails.getEffectiveLicenses().iterator().next().getLicenseName()).isEqualTo("OSL-1.0");
    assertThat(componentDetails.getEffectiveLicenses().iterator().next().getLicenseId()).isEqualTo("OSL-1.0");
    assertThat(componentDetails.getEffectiveLicenseStatus()).isNull();
    // allow generic component identifier which are components that do not
    // currently have broad support in the lifecycle ecosystem
    componentDetails = componentDetailsList.getList().get(3);
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(componentIdentifierGeneric);
    assertThat(componentDetails.getLicenseThreatLevel()).isNull();
    assertThat(componentDetails.getLicenseThreatGroupNames()).isNull();
    assertThat(componentDetails.getDeclaredLicenses()).isEmpty();
    assertThat(componentDetails.getObservedLicenses()).isEmpty();
    assertThat(componentDetails.getEffectiveLicenses()).hasSize(1);
    assertThat(componentDetails.getEffectiveLicenses().iterator().next().getLicenseName()).isEqualTo("Not Provided");
    assertThat(componentDetails.getEffectiveLicenses().iterator().next().getLicenseId()).isEqualTo(UNSPECIFIED_ID);
    assertThat(componentDetails.getEffectiveLicenseStatus()).isNull();
  }

  @Test
  public void testGetComponentDetailsListBulk_noComponents() {
    Map<ComponentIdentifier, List<ComponentDetails>> componentDetailsMap =
        componentInfoService.getComponentDetailsListBulk(Collections.emptyList(), null, null, false);

    assertThat(componentDetailsMap).isEmpty();
  }

  @Test
  public void testGetComponentDetailsListBulk_dontFetchEarlierVersions() {
    // Create the mocked hds response
    ComponentEvaluationDataList.ComponentEvaluationData componentEvaluationData1 =
        new ComponentEvaluationDataList.ComponentEvaluationData();
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "1.0.0");
    componentEvaluationData1.componentIdentifier = componentIdentifier1;
    componentEvaluationData1.declaredLicenses = Sets.newHashSet(new License("Apache-2.0", "Apache-2.0"));

    List<ComponentIdentifier> componentIdentifiers = asList(componentIdentifier1);

    mockHdsGetComponentDetailsListBulk(
        Collections.singletonList(componentIdentifier1),
        "pkg:maven/g1/a1",
        "0.0.1");

    // Versions that come BEFORE the requested component identifiers should not be returned
    Map<ComponentIdentifier, List<ComponentDetails>> componentDetailsMap =
        componentInfoService.getComponentDetailsListBulk(componentIdentifiers, application, null, false);
    assertThat(componentDetailsMap).isEmpty();
  }

  @Test
  public void testGetComponentDetailsListBulk_doFetchEquivalentVersions() {
    // Create the mocked hds response
    ComponentEvaluationDataList.ComponentEvaluationData componentEvaluationData1 =
        new ComponentEvaluationDataList.ComponentEvaluationData();
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "1.0.0");
    componentEvaluationData1.componentIdentifier = componentIdentifier1;
    componentEvaluationData1.declaredLicenses = Sets.newHashSet(new License("Apache-2.0", "Apache-2.0"));

    List<ComponentIdentifier> componentIdentifiers = asList(componentIdentifier1);

    mockHdsGetComponentDetailsListBulk(
        Collections.singletonList(componentIdentifier1),
        "pkg:maven/g1/a1",
        "1.0.0");
    mockGetComponentDetailsListFromHds(Collections.singletonList(componentEvaluationData1));
    // Versions that equal the requested component identifiers should be returned
    Map<ComponentIdentifier, List<ComponentDetails>> componentDetailsMap =
        componentInfoService.getComponentDetailsListBulk(componentIdentifiers, application, null, false);
    assertThat(componentDetailsMap).hasSize(1);
  }

  @Test
  public void testGetComponentDetailsListBulk_doFetchLaterVersions() {
    // Create the mocked hds response
    ComponentEvaluationDataList.ComponentEvaluationData componentEvaluationData1 =
        new ComponentEvaluationDataList.ComponentEvaluationData();
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "1.0.0");
    componentEvaluationData1.componentIdentifier = componentIdentifier1;
    componentEvaluationData1.declaredLicenses = Sets.newHashSet(new License("Apache-2.0", "Apache-2.0"));

    List<ComponentIdentifier> componentIdentifiers = asList(componentIdentifier1);

    mockHdsGetComponentDetailsListBulk(
        Collections.singletonList(componentIdentifier1),
        "pkg:maven/g1/a1",
        "2.0.0");
    mockGetComponentDetailsListFromHds(Collections.singletonList(componentEvaluationData1));

    // Versions that come AFTER the requested component identifiers should be returned
    Map<ComponentIdentifier, List<ComponentDetails>> componentDetailsMap =
        componentInfoService.getComponentDetailsListBulk(componentIdentifiers, application, null, false);
    assertThat(componentDetailsMap).hasSize(1);
  }

  @Test
  public void testGetComponentDetailsListBulk_nonTerraformComponents() {
    Organization organization = tempEntity.newOrganization("testGetComponentDetailsList");
    String applicationPublicId = "testGetComponentDetailsList";
    Application application = tempEntity.newApplication(applicationPublicId, applicationPublicId, organization.getId());
    // Create the mocked hds response
    ComponentEvaluationDataList.ComponentEvaluationData componentEvaluationData1 =
        new ComponentEvaluationDataList.ComponentEvaluationData();
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "1.0.0");
    componentEvaluationData1.componentIdentifier = componentIdentifier1;
    componentEvaluationData1.declaredLicenses = Sets.newHashSet(new License("Apache-2.0", "Apache-2.0"));

    ComponentEvaluationDataList.ComponentEvaluationData componentEvaluationData2 =
        new ComponentEvaluationDataList.ComponentEvaluationData();
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "2.0.0");
    componentEvaluationData2.componentIdentifier = componentIdentifier2;
    componentEvaluationData2.declaredLicenses = Sets.newHashSet(new License("GPL-2.0", "GPL-2.0"));

    ComponentEvaluationDataList.ComponentEvaluationData componentEvaluationData3 =
        new ComponentEvaluationDataList.ComponentEvaluationData();
    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "3.0.0");
    componentEvaluationData3.componentIdentifier = componentIdentifier3;
    componentEvaluationData3.declaredLicenses = Sets.newHashSet(new License("OSL-1.0", "OSL-1.0"));

    // mock hdsComponentDetailsList
    List<ComponentIdentifier> componentIdentifiers =
        asList(componentIdentifier1, componentIdentifier2, componentIdentifier3);

    mockHdsGetComponentDetailsListBulk(
        asList(componentIdentifier1, componentIdentifier2, componentIdentifier3),
        "pkg:maven/g1/a1",
        "3.0.0");
    mockGetComponentDetailsListFromHds(
        asList(componentEvaluationData1, componentEvaluationData2, componentEvaluationData3));

    Map<ComponentIdentifier, List<ComponentDetails>> componentDetailsMap =
        componentInfoService.getComponentDetailsListBulk(componentIdentifiers, application, null, false);

    assertThat(componentDetailsMap).hasSize(3);
    ComponentDetails componentDetails = componentDetailsMap.get(componentIdentifier1).get(0);
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(componentIdentifier1);

    assertThat(componentDetails.getDeclaredLicenses()).hasSize(1);
    assertThat(componentDetails.getDeclaredLicenses().iterator().next().getLicenseName()).isEqualTo("Apache-2.0");
    assertThat(componentDetails.getDeclaredLicenses().iterator().next().getLicenseId()).isEqualTo("Apache-2.0");
    assertThat(componentDetails.getObservedLicenses()).isNull();
    assertThat(componentDetails.getEffectiveLicenseStatus()).isNull();

    componentDetails = componentDetailsMap.get(componentIdentifier2).get(0);
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(componentIdentifier2);
    assertThat(componentDetails.getDeclaredLicenses()).hasSize(1);
    assertThat(componentDetails.getDeclaredLicenses().iterator().next().getLicenseName()).isEqualTo("GPL-2.0");
    assertThat(componentDetails.getDeclaredLicenses().iterator().next().getLicenseId()).isEqualTo("GPL-2.0");
    assertThat(componentDetails.getObservedLicenses()).isNull();
    assertThat(componentDetails.getEffectiveLicenseStatus()).isNull();

    // Test match against default LGT Copyleft from the root organization
    componentDetails = componentDetailsMap.get(componentIdentifier3).get(0);
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(componentIdentifier3);
    assertThat(componentDetails.getDeclaredLicenses()).hasSize(1);
    assertThat(componentDetails.getDeclaredLicenses().iterator().next().getLicenseName()).isEqualTo("OSL-1.0");
    assertThat(componentDetails.getDeclaredLicenses().iterator().next().getLicenseId()).isEqualTo("OSL-1.0");
    assertThat(componentDetails.getObservedLicenses()).isNull();
    assertThat(componentDetails.getEffectiveLicenseStatus()).isNull();
  }

  @Test
  public void testGetComponentDetailsListBulk_genericComponents() {
    // Create an application without LTGs
    Organization organization = tempEntity.newOrganization("testGetComponentDetailsList");
    String applicationPublicId = "testGetComponentDetailsList";
    Application application = tempEntity.newApplication(applicationPublicId, applicationPublicId, organization.getId());

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createGenericCoordinates("a1", "1.0", null);
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createGenericCoordinates("a1", "2.0", null);
    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createGenericCoordinates("a1", "3.0", null);
    ComponentIdentifier componentIdentifier4 = new ComponentIdentifier("apt", new TreeMap<String, String>()
    {
      {
        this.put("plan", "a1");
        this.put("name", "g1");
        this.put("version", "1.0.0");
      }
    });

    List<ComponentIdentifier> componentIdentifiers =
        asList(componentIdentifier1, componentIdentifier2, componentIdentifier3, componentIdentifier4);

    Map<ComponentIdentifier, List<ComponentDetails>> componentDetailsMap =
        componentInfoService.getComponentDetailsListBulk(componentIdentifiers, application, null, false);

    assertThat(componentDetailsMap).hasSize(3);
    assertGenericComponentDetails(componentDetailsMap.get(componentIdentifier1).get(0), componentIdentifier1);
    assertGenericComponentDetails(componentDetailsMap.get(componentIdentifier2).get(0), componentIdentifier2);
    assertGenericComponentDetails(componentDetailsMap.get(componentIdentifier3).get(0), componentIdentifier3);
  }

  @Test
  public void testGetComponentDetailsListBulk_genericComponents_identificationSourceAsThirdParty() {
    // Create an application without LTGs
    Organization organization = tempEntity.newOrganization("testGetComponentDetailsList");
    String applicationPublicId = "testGetComponentDetailsList";
    Application application = tempEntity.newApplication(applicationPublicId, applicationPublicId, organization.getId());

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createGenericCoordinates("a1", "1.0", null);
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createGenericCoordinates("a1", "2.0", null);
    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createGenericCoordinates("a1", "3.0", null);

    List<ComponentIdentifier> componentIdentifiers =
        asList(componentIdentifier1, componentIdentifier2, componentIdentifier3);

    mockComponentResolution(componentIdentifier1, application.getId());
    mockComponentResolution(componentIdentifier2, application.getId());
    mockComponentResolution(componentIdentifier3, application.getId());

    Map<ComponentIdentifier, List<ComponentDetails>> componentDetailsMap =
        componentInfoService.getComponentDetailsListBulk(componentIdentifiers, application, "myScanId", false);

    assertThat(componentDetailsMap).hasSize(3);

    ComponentDetails componentDetails = componentDetailsMap.get(componentIdentifier1).get(0);
    assertThat(componentDetails.getIdentificationSource()).isEqualTo("randomIdentificationSource");
    assertThat(componentDetails.getComponentIdentifier())
        .isEqualTo(componentIdentifier1);

    componentDetails = componentDetailsMap.get(componentIdentifier2).get(0);
    assertThat(componentDetails.getIdentificationSource()).isEqualTo("randomIdentificationSource");
    assertThat(componentDetails.getComponentIdentifier())
        .isEqualTo(componentIdentifier2);

    componentDetails = componentDetailsMap.get(componentIdentifier3).get(0);
    assertThat(componentDetails.getIdentificationSource()).isEqualTo("randomIdentificationSource");
    assertThat(componentDetails.getComponentIdentifier())
        .isEqualTo(componentIdentifier3);
  }

  private void mockComponentResolution(ComponentIdentifier componentIdentifier, String publicId) {
    ComponentDetails componentDetails = new ComponentDetails(componentIdentifier);
    componentDetails.setIdentificationSource("randomIdentificationSource");
    when(thirdPartyComponentDAO.resolveComponentDetails(eq(publicId), eq(componentIdentifier), eq("myScanId")))
        .thenReturn(componentDetails);

    ComponentDetailsList componentDetailsList = new ComponentDetailsList();
    componentDetailsList.setList(Collections.singletonList(componentDetails));
    when(thirdPartyComponentDAO.getAllVersions(eq(publicId), eq(componentIdentifier), eq("myScanId")))
        .thenReturn(componentDetailsList);
  }

  @Test
  public void testGetComponentDetailsListBulk_allComponentTypes() {
    // Create an application without LTGs
    Organization organization = tempEntity.newOrganization("testGetComponentDetailsList");
    String applicationPublicId = "testGetComponentDetailsList";
    Application application = tempEntity.newApplication(applicationPublicId, applicationPublicId, organization.getId());

    // nonTerraform
    ComponentEvaluationDataList.ComponentEvaluationData componentEvaluationData1 =
        new ComponentEvaluationDataList.ComponentEvaluationData();
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "1.0.0");
    componentEvaluationData1.componentIdentifier = componentIdentifier1;
    componentEvaluationData1.declaredLicenses = Sets.newHashSet(new License("Apache-2.0", "Apache-2.0"));

    mockHdsGetComponentDetailsListBulk(
        Collections.singletonList(componentIdentifier1),
        "pkg:maven/g1/a1",
        "3.0.0");
    mockGetComponentDetailsListFromHds(Collections.singletonList(componentEvaluationData1));

    // generic
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createGenericCoordinates("a1", "1.0", null);
    when(thirdPartyComponentDAO.resolveComponentDetails(
        eq(application.getId()), eq(componentIdentifier2), eq("myScanId")))
            .thenReturn(null);

    // generic third party component
    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createGenericCoordinates("b1", "2.0", null);

    mockComponentResolution(componentIdentifier3, application.getId());

    // unidentified component (discarded)
    ComponentIdentifier componentIdentifier4 = new ComponentIdentifier("apt", new TreeMap<String, String>()
    {
      {
        this.put("plan", "a1");
        this.put("name", "g1");
        this.put("version", "1.0.0");
      }
    });

    List<ComponentIdentifier> componentIdentifiers =
        asList(componentIdentifier1,
            componentIdentifier2,
            componentIdentifier3,
            componentIdentifier4);

    Map<ComponentIdentifier, List<ComponentDetails>> componentDetailsMap =
        componentInfoService.getComponentDetailsListBulk(componentIdentifiers, application, "myScanId", false);

    assertThat(componentDetailsMap).hasSize(3);

    // nonTerraform assertion
    ComponentDetails componentDetails = componentDetailsMap.get(componentIdentifier1).get(0);
    assertThat(componentDetails.getDeclaredLicenses()).hasSize(1);
    assertThat(componentDetails.getDeclaredLicenses().iterator().next().getLicenseName()).isEqualTo("Apache-2.0");
    assertThat(componentDetails.getDeclaredLicenses().iterator().next().getLicenseId()).isEqualTo("Apache-2.0");
    assertThat(componentDetails.getObservedLicenses()).isNull();
    assertThat(componentDetails.getEffectiveLicenseStatus()).isNull();

    // generic assertion
    assertGenericComponentDetails(componentDetailsMap.get(componentIdentifier2).get(0), componentIdentifier2);

    // generic third party assertion
    componentDetails = componentDetailsMap.get(componentIdentifier3).get(0);
    assertThat(componentDetails.getIdentificationSource()).isEqualTo("randomIdentificationSource");
    assertThat(componentDetails.getComponentIdentifier())
        .isEqualTo(componentIdentifier3);
  }

  @Test
  public void testGetComponentDetailsListBulk_shouldFilterOutDeprecatedDebianFormat() {
    Organization organization = tempEntity.newOrganization("testGetComponentDetailsList");
    String applicationPublicId = "testGetComponentDetailsList";
    Application application = tempEntity.newApplication(applicationPublicId, applicationPublicId, organization.getId());

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createGenericCoordinates("a1", "1.0", null);
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createGenericCoordinates("a1", "2.0", null);
    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createGenericCoordinates("a1", "3.0", null);
    ComponentIdentifier componentIdentifier4 = new ComponentIdentifier("apt", new TreeMap<String, String>()
    {
      {
        this.put("plan", "a1");
        this.put("name", "g1");
        this.put("version", "1.0.0");
      }
    });

    List<ComponentIdentifier> componentIdentifiers =
        asList(componentIdentifier1, componentIdentifier2, componentIdentifier3, componentIdentifier4);

    Map<ComponentIdentifier, List<ComponentDetails>> componentDetailsMap =
        componentInfoService.getComponentDetailsListBulk(componentIdentifiers, application, null, false);

    assertThat(componentDetailsMap).hasSize(3);
    assertGenericComponentDetails(componentDetailsMap.get(componentIdentifier1).get(0), componentIdentifier1);
    assertGenericComponentDetails(componentDetailsMap.get(componentIdentifier2).get(0), componentIdentifier2);
    assertGenericComponentDetails(componentDetailsMap.get(componentIdentifier3).get(0), componentIdentifier3);
  }

  @Test
  public void testGetComponentDetailsListBulk_debianFormatShouldBeFilteredOut() {
    Organization organization = tempEntity.newOrganization("testGetComponentDetailsList");
    String applicationPublicId = "testGetComponentDetailsList";
    Application application = tempEntity.newApplication(applicationPublicId, applicationPublicId, organization.getId());

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createGenericCoordinates("a1", "1.0", null);
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createGenericCoordinates("a1", "2.0", null);
    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createGenericCoordinates("a1", "3.0", null);
    ComponentIdentifier componentIdentifier4deb = new ComponentIdentifier("deb", new TreeMap<String, String>()
    {
      {
        this.put("plan", "a1");
        this.put("name", "g1");
        this.put("version", "1.0.0");
      }
    });

    List<ComponentIdentifier> componentIdentifiers =
        asList(componentIdentifier1, componentIdentifier2, componentIdentifier3, componentIdentifier4deb);

    Map<ComponentIdentifier, List<ComponentDetails>> componentDetailsMap =
        componentInfoService.getComponentDetailsListBulk(componentIdentifiers, application, null, false);

    assertThat(componentDetailsMap).hasSize(3);
    assertGenericComponentDetails(componentDetailsMap.get(componentIdentifier1).get(0), componentIdentifier1);
    assertGenericComponentDetails(componentDetailsMap.get(componentIdentifier2).get(0), componentIdentifier2);
    assertGenericComponentDetails(componentDetailsMap.get(componentIdentifier3).get(0), componentIdentifier3);
  }

  @Test
  public void testGetComponentDetailsListBulk_allDebianFormatShouldBeFilteredOut() {
    Organization organization = tempEntity.newOrganization("testGetComponentDetailsList");
    String applicationPublicId = "testGetComponentDetailsList";
    Application application = tempEntity.newApplication(applicationPublicId, applicationPublicId, organization.getId());

    ComponentIdentifier componentIdentifier1deb = new ComponentIdentifier("deb", new TreeMap<String, String>()
    {
      {
        this.put("plan", "a1");
        this.put("name", "g1");
        this.put("version", "1.0.0");
      }
    });
    ComponentIdentifier componentIdentifier2deb = new ComponentIdentifier("deb", new TreeMap<String, String>()
    {
      {
        this.put("plan", "a1");
        this.put("name", "g1");
        this.put("version", "1.0.0");
      }
    });

    List<ComponentIdentifier> componentIdentifiers =
        asList(componentIdentifier1deb, componentIdentifier2deb);

    Map<ComponentIdentifier, List<ComponentDetails>> componentDetailsMap =
        componentInfoService.getComponentDetailsListBulk(componentIdentifiers, application, null, false);

    assertThat(componentDetailsMap).isEmpty();

    verify(hdsClientMock, times(0))
        .post(Map.class, "rest/component/versions/list", componentIdentifiers);
  }

  @Test
  public void testGetComponentDetails_PolicyAlerts() throws Exception {
    String hash = "01234567890123456789";

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    Condition condition1 = new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0");
    constraint1.addCondition(condition1);
    Policy policy1 = new Policy("PolicyId1", "Policy1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setAction(BuildStageType.ID, FailActionType.ID);
    policy1.setOwnerId(application.getId());
    tempEntity.newPolicy(policy1);

    Constraint constraint2 = new Constraint("C2", "Constraint 2", LogicalOperator.AND);
    constraint2.addCondition(new Condition(MatchStateConditionType.ID, "is not", "similar"));
    Policy policy2 = new Policy("PolicyId2", "Policy2");
    policy2.setThreatLevel(8);
    policy2.addConstraint(constraint2);
    policy2.setAction(BuildStageType.ID, FailActionType.ID);
    policy2.setOwnerId(application.getId());
    tempEntity.newPolicy(policy2);

    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    hdsComponentDetails.setHash(hash);
    hdsComponentDetails.addSecurityVulnerability(new SecurityVulnerability("Test Ref Id", "Test Source", 7.5F));
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentDetails componentDetails = componentInfoService.getComponentDetails(application, MAVEN_A1_COORDINATES,
        MatchState.SIMILAR.getId(), hash, false /* proprietary */, httpRequestMock);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(MAVEN_A1_COORDINATES);
    assertCategories(componentDetails);
    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    assertThat(policyAlerts).hasSize(1);
    assertThat(policyAlerts.get(0).getTrigger().getPolicyName()).isEqualTo("Policy1");

    Map<String, Integer> policyMaxThreatLevel = componentDetails.getPolicyMaxThreatLevelsByCategory();
    assertThat(policyMaxThreatLevel).isNotNull();
    assertThat(policyMaxThreatLevel).hasSize(1);
    assertThat(policyMaxThreatLevel.get("security")).isEqualTo(8);
  }

  @Test
  public void testGetComponentDetails_PolicyAlerts_Metadata() throws Exception {
    String hash = "01234567890123456789";

    Constraint constraint1 = new Constraint("C1", "C1", LogicalOperator.AND);
    Condition condition1 =
        new Condition(DataSourceConditionType.ID, "has support for", ComponentDataSource.IDENTITY.getId());
    constraint1.addCondition(condition1);
    Policy policy1 = new Policy("PolicyId1", "Policy1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setAction(BuildStageType.ID, FailActionType.ID);
    policy1.setOwnerId(application.getId());
    tempEntity.newPolicy(policy1);

    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    hdsComponentDetails.setHash(hash);
    hdsComponentDetails.setAnalyzerFeatures(
        MetadataRecorderUtils.createAnalyzerFeatures(MAVEN_A1_COORDINATES.getFormat()));
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentDetails componentDetails = componentInfoService.getComponentDetails(application, MAVEN_A1_COORDINATES,
        MatchState.EXACT.getId(), hash, false /* proprietary */, httpRequestMock);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(MAVEN_A1_COORDINATES);

    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    assertThat(policyAlerts).hasSize(1);
    assertThat(policyAlerts.get(0).getTrigger().getPolicyName()).isEqualTo("Policy1");
  }

  @Test
  public void testGetComponentDetails_PolicyAlerts_NoMetadata() throws Exception {
    String hash = "01234567890123456789";

    Constraint constraint1 = new Constraint("C1", "C1", LogicalOperator.AND);
    Condition condition1 =
        new Condition(DataSourceConditionType.ID, "has support for", ComponentDataSource.IDENTITY.getId());
    constraint1.addCondition(condition1);
    Policy policy1 = new Policy("PolicyId1", "Policy1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setAction(BuildStageType.ID, FailActionType.ID);
    policy1.setOwnerId(application.getId());
    tempEntity.newPolicy(policy1);

    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    hdsComponentDetails.setHash(hash);
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentDetails componentDetails = componentInfoService.getComponentDetails(application, MAVEN_A1_COORDINATES,
        MatchState.EXACT.getId(), hash, false /* proprietary */, httpRequestMock);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(MAVEN_A1_COORDINATES);

    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    assertThat(policyAlerts).isEmpty();
  }

  @Test
  public void testGetComponentDetails_policyMaxThreatLevel() throws Exception {
    String hash = "01234567890123456789";

    Label label1 = tempEntity.newLabel(application.getId(), "red");
    Label label2 = tempEntity.newLabel(application.getId(), "blue");
    tempEntity.newComponentLabel(application.getId(), label1.getId(), hash);
    tempEntity.newComponentLabel(application.getId(), label2.getId(), hash);
    tempEntity.newLicenseOverride(application.getId(), MAVEN_A1_COORDINATES, LicenseOverrideStatus.OVERRIDDEN,
        "GPL-2.0", null /* comment */);

    Constraint securityConstrain1 = new Constraint("SC1", "SecurityConstraint 1", LogicalOperator.AND);
    Condition securityCondition1 = new Condition(SecurityVulnerabilitySeverityConditionType.ID, "<=", "3");
    securityConstrain1.addCondition(securityCondition1);
    Policy securityPolicy1 = new Policy("SecurityPolicyId1", "SecurityPolicy1");
    securityPolicy1.setThreatLevel(2);
    securityPolicy1.addConstraint(securityConstrain1);
    securityPolicy1.setAction(BuildStageType.ID, FailActionType.ID);
    securityPolicy1.setOwnerId(application.getId());
    tempEntity.newPolicy(securityPolicy1);

    Constraint securityConstrain2 = new Constraint("SC1", "SecurityConstraint 1", LogicalOperator.AND);
    Condition securityCondition2 = new Condition(SecurityVulnerabilitySeverityConditionType.ID, "<=", "7");
    securityConstrain2.addCondition(securityCondition2);
    Policy securityPolicy2 = new Policy("SecurityPolicyId2", "SecurityPolicy2");
    securityPolicy2.setThreatLevel(8);
    securityPolicy2.addConstraint(securityConstrain2);
    securityPolicy2.setOwnerId(application.getId());
    tempEntity.newPolicy(securityPolicy2);

    Constraint qualityConstrain1 = new Constraint("QC1", "QualityConstraint 1", LogicalOperator.AND);
    Condition qualityCondition1 = new Condition(RelativePopularityConditionType.ID, "<=", "10");
    qualityConstrain1.addCondition(qualityCondition1);
    Policy qualityPolicy1 = new Policy("QualityPolicyId1", "QualityPolicy1");
    qualityPolicy1.setThreatLevel(2);
    qualityPolicy1.addConstraint(qualityConstrain1);
    qualityPolicy1.setAction(BuildStageType.ID, FailActionType.ID);
    qualityPolicy1.setOwnerId(application.getId());
    tempEntity.newPolicy(qualityPolicy1);

    Constraint qualityConstrain2 = new Constraint("QC2", "QualityConstraint 2", LogicalOperator.AND);
    Condition qualityCondition2 = new Condition(RelativePopularityConditionType.ID, "<=", "5");
    qualityConstrain2.addCondition(qualityCondition2);
    Policy qualityPolicy2 = new Policy("QualityPolicyId2", "QualityPolicy2");
    qualityPolicy2.setThreatLevel(6);
    qualityPolicy2.addConstraint(qualityConstrain2);
    qualityPolicy2.setAction(BuildStageType.ID, FailActionType.ID);
    qualityPolicy2.setOwnerId(application.getId());
    tempEntity.newPolicy(qualityPolicy2);

    Constraint licenseContraint1 = new Constraint("LC1", "LicenseConstraint1", LogicalOperator.AND);
    Condition licenseCondition1 = new Condition(LicenseConditionType.ID, "is", "GPL-2.0");
    licenseContraint1.addCondition(licenseCondition1);
    Policy licensePolicy1 = new Policy("LicensePolicyId1", "LicensePolicy1");
    licensePolicy1.setThreatLevel(3);
    licensePolicy1.addConstraint(licenseContraint1);
    licensePolicy1.setAction(BuildStageType.ID, FailActionType.ID);
    licensePolicy1.setOwnerId(application.getId());
    tempEntity.newPolicy(licensePolicy1);

    Constraint otherConstraint1 = new Constraint("OC1", "Other Constraint 1", LogicalOperator.AND);
    otherConstraint1.addCondition(new Condition(LabelConditionType.ID, "is", label1.getId()));
    Policy otherPolicy1 = new Policy("OtherPolicyId1", "Other Policy Name 1");
    otherPolicy1.setThreatLevel(5);
    otherPolicy1.addConstraint(otherConstraint1);
    otherPolicy1.setAction(BuildStageType.ID, FailActionType.ID);
    otherPolicy1.setOwnerId(application.getId());
    tempEntity.newPolicy(otherPolicy1);

    Constraint otherConstraint2 = new Constraint("OC2", "Other Constraint 2", LogicalOperator.AND);
    otherConstraint2.addCondition(new Condition(LabelConditionType.ID, "is", label2.getId()));
    Policy otherPolicy2 = new Policy("OtherPolicyId2", "Other Policy Name 2");
    otherPolicy2.setThreatLevel(2);
    otherPolicy2.addConstraint(otherConstraint2);
    otherPolicy2.setAction(BuildStageType.ID, FailActionType.ID);
    otherPolicy2.setOwnerId(application.getId());
    tempEntity.newPolicy(otherPolicy2);

    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    hdsComponentDetails.setHash(hash);
    hdsComponentDetails.addSecurityVulnerability(new SecurityVulnerability("Test Ref Id", "Test Source", 2.5F));
    hdsComponentDetails.addSecurityVulnerability(new SecurityVulnerability("Test Ref Id 2", "Test Source 2", 7.5F));
    hdsComponentDetails.setRelativePopularity(2);
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentDetails componentDetails = componentInfoService.getComponentDetails(application, MAVEN_A1_COORDINATES,
        MatchState.SIMILAR.getId(), hash, false /* proprietary */, httpRequestMock);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(MAVEN_A1_COORDINATES);
    assertCategories(componentDetails);
    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    assertThat(policyAlerts).isNotNull();
    assertThat(policyAlerts).hasSize(7);
    assertThat(policyAlerts.get(0).getTrigger().getPolicyName()).isEqualTo("LicensePolicy1");
    assertThat(policyAlerts.get(1).getTrigger().getPolicyName()).isEqualTo("Other Policy Name 1");
    assertThat(policyAlerts.get(2).getTrigger().getPolicyName()).isEqualTo("Other Policy Name 2");
    assertThat(policyAlerts.get(3).getTrigger().getPolicyName()).isEqualTo("QualityPolicy1");
    assertThat(policyAlerts.get(4).getTrigger().getPolicyName()).isEqualTo("QualityPolicy2");
    assertThat(policyAlerts.get(5).getTrigger().getPolicyName()).isEqualTo("SecurityPolicy1");
    assertThat(policyAlerts.get(6).getTrigger().getPolicyName()).isEqualTo("SecurityPolicy2");

    Map<String, Integer> policyMaxThreatLevel = componentDetails.getPolicyMaxThreatLevelsByCategory();
    assertThat(policyMaxThreatLevel).isNotNull();
    assertThat(policyMaxThreatLevel).hasSize(4);
    assertThat(policyMaxThreatLevel.get("security")).isEqualTo(8);
    assertThat(policyMaxThreatLevel.get("other")).isEqualTo(5);
    assertThat(policyMaxThreatLevel.get("license")).isEqualTo(3);
    assertThat(policyMaxThreatLevel.get("quality")).isEqualTo(6);
  }

  @Test
  public void testGetComponentDetails_policyMaxThreatLevel_oneElement() throws Exception {
    String hash = "01234567890123456789";

    Label label1 = tempEntity.newLabel(application.getId(), "red");
    Label label2 = tempEntity.newLabel(application.getId(), "blue");
    tempEntity.newComponentLabel(application.getId(), label1.getId(), hash);
    tempEntity.newComponentLabel(application.getId(), label2.getId(), hash);

    Constraint otherConstraint1 = new Constraint("OC1", "Other Constraint 1", LogicalOperator.AND);
    otherConstraint1.addCondition(new Condition(LabelConditionType.ID, "is", label1.getId()));
    Policy otherPolicy1 = new Policy("OtherPolicyId1", "Other Policy Name 1");
    otherPolicy1.setThreatLevel(5);
    otherPolicy1.addConstraint(otherConstraint1);
    otherPolicy1.setAction(BuildStageType.ID, FailActionType.ID);
    otherPolicy1.setOwnerId(application.getId());
    tempEntity.newPolicy(otherPolicy1);

    Constraint otherConstraint2 = new Constraint("OC2", "Other Constraint 2", LogicalOperator.AND);
    otherConstraint2.addCondition(new Condition(LabelConditionType.ID, "is", label2.getId()));
    Policy otherPolicy2 = new Policy("OtherPolicyId2", "Other Policy Name 2");
    otherPolicy2.setThreatLevel(2);
    otherPolicy2.addConstraint(otherConstraint2);
    otherPolicy2.setAction(BuildStageType.ID, FailActionType.ID);
    otherPolicy2.setOwnerId(application.getId());
    tempEntity.newPolicy(otherPolicy2);

    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    hdsComponentDetails.setHash(hash);
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentDetails componentDetails = componentInfoService.getComponentDetails(application, MAVEN_A1_COORDINATES,
        MatchState.SIMILAR.getId(), hash, false /* proprietary */, httpRequestMock);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(MAVEN_A1_COORDINATES);
    assertCategories(componentDetails);
    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    assertThat(policyAlerts).isNotNull();
    assertThat(policyAlerts).hasSize(2);
    assertThat(policyAlerts.get(0).getTrigger().getPolicyName()).isEqualTo("Other Policy Name 1");
    assertThat(policyAlerts.get(1).getTrigger().getPolicyName()).isEqualTo("Other Policy Name 2");

    Map<String, Integer> policyMaxThreatLevel = componentDetails.getPolicyMaxThreatLevelsByCategory();
    assertThat(policyMaxThreatLevel).isNotNull();
    assertThat(policyMaxThreatLevel).hasSize(1);
    assertThat(policyMaxThreatLevel.get("other")).isEqualTo(5);
  }

  @Test
  public void testGetComponentDetails_PolicyAlerts_DependencyType() throws Exception {
    String hash = "01234567890123456789";

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    constraint1.addCondition(new Condition(DependencyTypeConditionType.ID, "is", "direct"));
    Policy policy1 = new Policy("PolicyId1", "Policy1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setAction(BuildStageType.ID, FailActionType.ID);
    policy1.setOwnerId(application.getId());
    tempEntity.newPolicy(policy1);

    Constraint constraint2 = new Constraint("C2", "Constraint 2", LogicalOperator.AND);
    constraint2.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    constraint2.addCondition(new Condition(DependencyTypeConditionType.ID, "is", "transitive"));
    Policy policy2 = new Policy("PolicyId2", "Policy2");
    policy2.setThreatLevel(7);
    policy2.addConstraint(constraint2);
    policy2.setAction(BuildStageType.ID, FailActionType.ID);
    policy2.setOwnerId(application.getId());
    tempEntity.newPolicy(policy2);

    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    hdsComponentDetails.setHash(hash);
    hdsComponentDetails.addSecurityVulnerability(new SecurityVulnerability("Test Ref Id", "Test Source", 7.5F));
    mockHdsGetComponentDetails(hdsComponentDetails);

    ComponentDetails componentDetails = componentInfoService.getComponentDetails(application, MAVEN_A1_COORDINATES,
        MatchState.SIMILAR.getId(), hash, false /* proprietary */, httpRequestMock, null, null,
        DependencyType.DIRECT);

    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(MAVEN_A1_COORDINATES);
    assertCategories(componentDetails);
    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    assertThat(policyAlerts).extracting(alert -> alert.getTrigger().getPolicyName()).containsExactly("Policy1");

    Map<String, Integer> policyMaxThreatLevel = componentDetails.getPolicyMaxThreatLevelsByCategory();
    assertThat(policyMaxThreatLevel).isNotNull();
    assertThat(policyMaxThreatLevel).hasSize(1);
    assertThat(policyMaxThreatLevel.get("security")).isEqualTo(8);
  }

  @Test
  public void testGetComponentDetails_PolicyAlerts_ProprietaryComponentNameConflict() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "testPublicId", RepositoryType.hosted,
        ComponentIdentifier.FORMAT_MAVEN);

    String hash = "01234567890123456789";

    Constraint constraint = new Constraint(null, "Constraint", LogicalOperator.OR);
    constraint.addCondition(
        new Condition(ProprietaryNameConflictConditionType.ID, ProprietaryNameConflictConditionType.OP_IS_PRESENT));
    Policy policy = new Policy(null, "Dependency Confusion");
    policy.setOwnerId(Organization.ROOT_ORGANIZATION_ID);
    policy.setThreatLevel(8);
    policy.addConstraint(constraint);
    policy.setAction(BuildStageType.ID, FailActionType.ID);
    tempEntity.newPolicy(policy);

    getProprietaryComponentNameDetector().addPatterns(
        MAVEN_A1_COORDINATES.getFormat(),
        Collections.singletonList(
            new ProprietaryComponentNamePattern(repository.getId(), MAVEN_A1_COORDINATES.getFormat())
                .withNamespacePattern(MAVEN_A1_COORDINATES.get(ComponentIdentifier.MAVEN_GROUP_ID))));

    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    hdsComponentDetails.setHash(hash);
    mockHdsGetComponentDetails(hdsComponentDetails);

    ComponentDetails componentDetails = componentInfoService.getComponentDetails(repository, MAVEN_A1_COORDINATES,
        MatchState.EXACT.getId(), hash, false /* proprietary */, httpRequestMock, null, null, DependencyType.DIRECT);

    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(MAVEN_A1_COORDINATES);
    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    assertThat(policyAlerts).extracting(alert -> alert.getTrigger().getPolicyName()).containsExactly(policy.getName());

    Map<String, Integer> policyMaxThreatLevel = componentDetails.getPolicyMaxThreatLevelsByCategory();
    assertThat(policyMaxThreatLevel).isNotNull();
    assertThat(policyMaxThreatLevel).hasSize(1);
    assertThat(policyMaxThreatLevel.get("security")).isEqualTo(8);
  }

  @Test
  public void testGetComponentDetails_OverriddenLicense() throws Exception {
    tempEntity.newLicenseOverride(application.getId(), MAVEN_A1_COORDINATES, LicenseOverrideStatus.OVERRIDDEN,
        "GPL-2.0", null /* comment */);

    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    mockHdsGetComponentDetails(hdsComponentDetails);

    ComponentDetails componentDetails = componentInfoService.getComponentDetails(application, MAVEN_A1_COORDINATES,
        MatchState.EXACT.getId(), null /* hash */, false /* proprietary */, httpRequestMock);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(MAVEN_A1_COORDINATES);
    assertThat(componentDetails.getOverriddenLicenses()).hasSize(1);
    License overriddenLicense = componentDetails.getOverriddenLicenses().iterator().next();
    assertThat(overriddenLicense).isNotNull();
    assertThat(overriddenLicense.getLicenseId()).isEqualTo("GPL-2.0");
    assertThat(overriddenLicense.getLicenseName()).isEqualTo("GPL-2.0");
    assertThat(componentDetails.getLicenseThreatLevel()).isEqualTo(9);
    assertThat(componentDetails.getLicenseThreatGroupNames()).containsExactlyInAnyOrder("Copyleft");
    assertThat(componentDetails.getEffectiveLicenses()).hasSize(1);
    License effectiveLicense = componentDetails.getEffectiveLicenses().iterator().next();
    assertThat(effectiveLicense.getLicenseId()).isEqualTo("GPL-2.0");
    assertThat(effectiveLicense.getLicenseName()).isEqualTo("GPL-2.0");
    assertThat(componentDetails.getEffectiveLicenseStatus()).isEqualTo(LicenseStatus.Overridden);
    assertCategories(componentDetails);
  }

  @Test
  public void testGetComponentDetails_SelectedLicense() throws Exception {
    tempEntity.newLicenseOverride(application.getId(), MAVEN_A1_COORDINATES, LicenseOverrideStatus.SELECTED, "GPL-2.0",
        null /* comment */);

    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    mockHdsGetComponentDetails(hdsComponentDetails);

    ComponentDetails componentDetails = componentInfoService.getComponentDetails(application, MAVEN_A1_COORDINATES,
        MatchState.EXACT.getId(), null /* hash */, false /* proprietary */, httpRequestMock);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(MAVEN_A1_COORDINATES);
    assertThat(componentDetails.getOverriddenLicenses()).hasSize(1);
    License overriddenLicense = componentDetails.getOverriddenLicenses().iterator().next();
    assertThat(overriddenLicense).isNotNull();
    assertThat(overriddenLicense.getLicenseId()).isEqualTo("GPL-2.0");
    assertThat(overriddenLicense.getLicenseName()).isEqualTo("GPL-2.0");
    assertThat(componentDetails.getLicenseThreatLevel()).isEqualTo(9);
    assertThat(componentDetails.getLicenseThreatGroupNames()).containsExactlyInAnyOrder("Copyleft");
    assertThat(componentDetails.getEffectiveLicenses()).hasSize(1);
    License effectiveLicense = componentDetails.getEffectiveLicenses().iterator().next();
    assertThat(effectiveLicense.getLicenseId()).isEqualTo("GPL-2.0");
    assertThat(effectiveLicense.getLicenseName()).isEqualTo("GPL-2.0");
    assertThat(componentDetails.getEffectiveLicenseStatus()).isEqualTo(LicenseStatus.Selected);
    assertCategories(componentDetails);
  }

  @Test
  public void testGetComponentDetails_UnknownComponent() throws Exception {
    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(MatchStateConditionType.ID, "is", "unknown"));
    Policy policy1 = new Policy("PolicyId1", "Policy1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setAction(BuildStageType.ID, FailActionType.ID);
    policy1.setOwnerId(application.getId());
    tempEntity.newPolicy(policy1);

    String hash = "01234567890123456789";
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    hdsComponentDetails.setHash(hash);
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentDetails componentDetails = componentInfoService.getComponentDetails(application, MAVEN_A1_COORDINATES,
        MatchState.UNKNOWN.getId(), hash, false /* proprietary */, httpRequestMock);

    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(MAVEN_A1_COORDINATES);
    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    assertThat(policyAlerts).hasSize(1);
    assertThat(policyAlerts.get(0).getTrigger().getPolicyName()).isEqualTo("Policy1");

    ComponentIdentifier emptyComponentIdentifier = ComponentIdentifier.createMavenCoordinates("", "", "");
    hdsComponentDetails = newNamedComponentDetails(emptyComponentIdentifier);
    hdsComponentDetails.setHash(hash);
    when(
        hdsClientMock.relay(httpRequestMock, NamedComponentDetails.class, "rest/" + TOOL_NAME + "/componentDetails",
            newCoordinatesQueryParam(hdsComponentDetails))).thenThrow(new NotFoundException("unknown GAV"));
    componentDetails = componentInfoService.getComponentDetails(application, emptyComponentIdentifier,
        MatchState.UNKNOWN.getId(), "01234567890123456789", false /* proprietary */, httpRequestMock);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(emptyComponentIdentifier);
    policyAlerts = componentDetails.getPolicyAlerts();
    assertThat(policyAlerts).hasSize(1);
    assertThat(policyAlerts.get(0).getTrigger().getPolicyName()).isEqualTo("Policy1");
  }

  // CLM-4195
  @Test
  public void testGetComponentDetails_UnknownComponentNullIdentifier() throws Exception {
    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(ProprietaryConditionType.ID, "is true"));
    Policy policy1 = new Policy("PolicyId1", "Policy1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setAction(BuildStageType.ID, FailActionType.ID);
    policy1.setOwnerId(application.getId());
    tempEntity.newPolicy(policy1);

    String hash = "01234567890123456789";
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    hdsComponentDetails.setHash(hash);
    ComponentDetails componentDetails = componentInfoService.getComponentDetails(application,
        null /* componentIdentifier */, MatchState.UNKNOWN.getId(), hash, true /* proprietary */, httpRequestMock);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getHash()).isEqualTo(hash);
    assertThat(componentDetails.getComponentIdentifier()).isNull();

    assertThat(componentDetails.getMatchState()).isEqualTo(MatchState.UNKNOWN.getId());

    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    assertThat(policyAlerts).hasSize(1);
  }

  @Test
  public void testGetComponentDetails_AppPublicIdWithUnsafeCharacters() throws Exception {
    String applicationPublicId = "bom 1&2%20?";
    tempEntity.newApplicationWithInvalidPublicId(applicationPublicId);

    String hash = "01234567890123456789";
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    hdsComponentDetails.setHash(hash);
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentDetails componentDetails = componentInfoService.getComponentDetails(application, MAVEN_A1_COORDINATES,
        MatchState.UNKNOWN.getId(), hash, false /* proprietary */, httpRequestMock);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(MAVEN_A1_COORDINATES);
  }

  @Test
  public void testGetComponentDetails_ProprietaryComponent() throws Exception {
    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(ProprietaryConditionType.ID, "is true"));
    Policy policy1 = new Policy("PolicyId1", "Policy1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setAction(BuildStageType.ID, FailActionType.ID);
    policy1.setOwnerId(application.getId());
    tempEntity.newPolicy(policy1);

    String hash = "01234567890123456789";
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    hdsComponentDetails.setHash(hash);
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentDetails componentDetails = componentInfoService.getComponentDetails(application, MAVEN_A1_COORDINATES,
        MatchState.SIMILAR.getId(), hash, true /* proprietary */, httpRequestMock);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(MAVEN_A1_COORDINATES);
    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    assertThat(policyAlerts).hasSize(1);
    assertThat(policyAlerts.get(0).getTrigger().getPolicyName()).isEqualTo("Policy1");

    componentDetails = componentInfoService.getComponentDetails(application, MAVEN_A1_COORDINATES,
        MatchState.SIMILAR.getId(), hash, false /* proprietary */, httpRequestMock);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(MAVEN_A1_COORDINATES);
    policyAlerts = componentDetails.getPolicyAlerts();
    assertThat(policyAlerts).isEmpty();
  }

  @Test
  public void testGetComponentDetails_ManuallyIdentifiedComponent() throws Exception {
    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(MatchStateConditionType.ID, "is", "exact"));
    constraint1.addCondition(new Condition(AgeInDaysConditionType.ID, "younger than", "30"));
    Policy policy1 = new Policy("PolicyId1", "Policy1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setAction(BuildStageType.ID, FailActionType.ID);
    policy1.setOwnerId(application.getId());
    tempEntity.newPolicy(policy1);

    String hash = "01234567890123456789";
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    hdsComponentDetails.setHash(hash);
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentDetails componentDetails = componentInfoService.getComponentDetails(application, MAVEN_A1_COORDINATES,
        MatchState.SIMILAR.getId(), hash, false /* proprietary */, httpRequestMock);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getHash()).isEqualTo(hash);
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(MAVEN_A1_COORDINATES);
    assertThat(componentDetails.getMatchState()).isEqualTo(MatchState.SIMILAR.getId());
    assertThat(componentDetails.getIdentificationSource()).isEqualTo(IdentificationSource.SONATYPE.getId());
    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    assertThat(policyAlerts).isEmpty();

    ComponentIdentifier claimedComponentIdentifier = ComponentIdentifier.createMavenCoordinates("Claimed g",
        "Claimed a", "Claimed v");
    HashComponentIdentifier claimedComponent = tempEntity.newClaimedComponent(hash, claimedComponentIdentifier);
    componentDetails = componentInfoService.getComponentDetails(application, MAVEN_A1_COORDINATES,
        MatchState.SIMILAR.getId(), hash, false /* proprietary */, httpRequestMock);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getHash()).isEqualTo(hash);
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(claimedComponentIdentifier);
    assertThat(componentDetails.getMatchState()).isEqualTo(MatchState.EXACT.getId());
    assertThat(componentDetails.getIdentificationSource()).isEqualTo(IdentificationSource.MANUAL.getId());
    assertThat(componentDetails.getIdentificationSourceComment()).isEqualTo(claimedComponent.getComment());
    policyAlerts = componentDetails.getPolicyAlerts();
    assertThat(policyAlerts).hasSize(1);
    assertThat(policyAlerts.get(0).getTrigger().getPolicyName()).isEqualTo("Policy1");
  }

  @Test
  public void testGetComponentDetails_Label_DefinedAtAppLevel() throws Exception {
    testGetComponentDetails_Label(false, false);
  }

  @Test
  public void testGetComponentDetails_Label_DefinedAtOrgLevel_AppliedAtOrgLevel() throws Exception {
    testGetComponentDetails_Label(true, true);
  }

  @Test
  public void testGetComponentDetails_Label_DefinedAtOrgLevel_AppliedAtAppLevel() throws Exception {
    testGetComponentDetails_Label(true, false);
  }

  private void testGetComponentDetails_Label(boolean orgLabel, boolean orgComponentLabel) throws Exception {
    String hash = "01234567890123456789";
    Label label = tempEntity.newLabel(orgLabel ? application.getOrganizationId() : application.getId(), "red");
    tempEntity.newComponentLabel(orgComponentLabel ? application.getOrganizationId() : application.getId(),
        label.getId(), hash);

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(LabelConditionType.ID, "is", label.getId()));
    Policy policy1 = new Policy("PolicyId1", "Policy Name 1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setAction(BuildStageType.ID, FailActionType.ID);
    policy1.setOwnerId(application.getId());
    tempEntity.newPolicy(policy1);

    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    hdsComponentDetails.setHash(hash);
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentDetails componentDetails = componentInfoService.getComponentDetails(application, MAVEN_A1_COORDINATES,
        MatchState.SIMILAR.getId(), hash, false /* proprietary */, httpRequestMock);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getHash()).isEqualTo(hash);
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(MAVEN_A1_COORDINATES);
    assertThat(componentDetails.getMatchState()).isEqualTo(MatchState.SIMILAR.getId());
    assertThat(componentDetails.getIdentificationSource()).isEqualTo(IdentificationSource.SONATYPE.getId());
    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    assertThat(policyAlerts).hasSize(1);

    Map<String, Integer> policyMaxThreatLevel = componentDetails.getPolicyMaxThreatLevelsByCategory();
    assertThat(policyMaxThreatLevel).isNotNull();
    assertThat(policyMaxThreatLevel).hasSize(1);
    assertThat(policyMaxThreatLevel.get("other")).isEqualTo(8);
  }

  @Test
  public void testGetComponentDetails_GetPoliciesById_Invoked_Once() throws Exception {
    tempEntity.newLicenseOverride(application.getId(), MAVEN_A1_COORDINATES, LicenseOverrideStatus.OVERRIDDEN,
        "GPL-2.0", null /* comment */);

    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentInfoService componentInfoServiceMock = spy(componentInfoService);
    componentInfoServiceMock.getComponentDetails(application, MAVEN_A1_COORDINATES,
        MatchState.EXACT.getId(), null /* hash */, false /* proprietary */, httpRequestMock);

    verify(componentInfoServiceMock, times(1)).getPoliciesById(application);
  }

  private Set<License> toLicenseSet(String... licenseIds) {
    Set<License> result = new LinkedHashSet<>();
    for (String licenseId : licenseIds) {
      MultiLicense multiLicense = multiLicenseDAO.getByIdNotNull(licenseId);
      result.add(new License(multiLicense.getId(), multiLicense.getShortDisplayName()));
    }
    return result;
  }

  private void testGetComponentDetails_ReadPermission(final Owner owner, final String ownerId) throws Exception {
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentDetails componentDetails = componentInfoService
        .getComponentDetails_ReadPermission(owner.getType(), ownerId, MAVEN_A1_COORDINATES, MatchState.EXACT.getId(),
            null /* hash */, false /* proprietary */, httpRequestMock, null, null, null);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(MAVEN_A1_COORDINATES);
    assertThat(componentDetails.getMatchState()).isEqualTo(MatchState.EXACT.getId());
    assertThat(componentDetails.getIdentificationSource()).isEqualTo(IdentificationSource.SONATYPE.getId());
  }

  @Test
  public void testGetComponentDetails_ReadPermission_Application() throws Exception {
    testGetComponentDetails_ReadPermission(application, application.getPublicId());
  }

  @Test
  public void testGetComponentDetails_ReadPermission_Repository() throws Exception {
    testGetComponentDetails_ReadPermission(repository, repository.getId());
  }

  @Test
  public void testGetComponentDetails_ReadPermission_ThirdParty() throws Exception {
    String scanId = "scanId";
    final String identificationSource = IdentificationSource.CLAIR.getId();
    NamedComponentDetails tpsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    tpsComponentDetails.setMatchState(MatchState.EXACT.getId());
    tpsComponentDetails.setIdentificationSource(identificationSource);

    when(thirdPartyComponentDAO.getComponentDetailsByIdentifier(MAVEN_A1_COORDINATES, application.getId(), scanId))
        .thenReturn(tpsComponentDetails);

    ComponentDetails componentDetails = componentInfoService
        .getComponentDetails_ReadPermission(application.getType(), application.getPublicId(), MAVEN_A1_COORDINATES,
            MatchState.EXACT.getId(), null /* hash */, false /* proprietary */, httpRequestMock, identificationSource,
            scanId, null);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(MAVEN_A1_COORDINATES);
    assertThat(componentDetails.getMatchState()).isEqualTo(MatchState.EXACT.getId());
    assertThat(componentDetails.getIdentificationSource()).isEqualTo(identificationSource);
    assertCategories(componentDetails);
  }

  @Test
  public void testGetComponentDetails_ReadPermission_PackageManifest() throws Exception {
    String scanId = "scanId";
    String identificationSource = IdentificationSource.PACKAGE_MANIFEST.getId();
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    hdsComponentDetails.setMatchState(MatchState.EXACT.getId());
    hdsComponentDetails.setIdentificationSource(identificationSource);

    mockHdsGetComponentDetailsException(hdsComponentDetails);

    ComponentDetails componentDetails = componentInfoService.getComponentDetails_ReadPermission(application.getType(),
        application.getPublicId(), MAVEN_A1_COORDINATES, MatchState.EXACT.getId(), null /* hash */,
        false /* proprietary */, httpRequestMock, identificationSource, scanId, null);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(MAVEN_A1_COORDINATES);
    assertThat(componentDetails.getMatchState()).isEqualTo(MatchState.EXACT.getId());
    assertThat(componentDetails.getIdentificationSource()).isEqualTo(identificationSource);
    assertCategories(componentDetails);
  }

  @Test
  public void testGetComponentDetails_ReadPermission_ExternalRepo() throws Exception {
    String scanId = "scanId";
    String identificationSource = IdentificationSource.EXTERNAL_REPO.getId();
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    hdsComponentDetails.setMatchState(MatchState.UNKNOWN.getId());

    mockHdsGetComponentDetailsException(hdsComponentDetails);

    ComponentDetails componentDetails = componentInfoService.getComponentDetails_ReadPermission(application.getType(),
        application.getPublicId(), MAVEN_A1_COORDINATES, MatchState.EXACT.getId(), null /* hash */,
        false /* proprietary */, httpRequestMock, identificationSource, scanId, null);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(MAVEN_A1_COORDINATES);
    assertThat(componentDetails.getMatchState()).isEqualTo(MatchState.EXACT.getId());
    assertThat(componentDetails.getIdentificationSource()).isEqualTo(identificationSource);
    assertCategories(componentDetails);
  }

  @Deprecated
  private void testGetComponentDetailsList_ReadPermission(final Owner owner, final String ownerId) {
    ComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    ComponentDetailsList hdsComponentDetailsList = new ComponentDetailsList();
    hdsComponentDetailsList.setList(Collections.singletonList(hdsComponentDetails));
    mockHdsGetComponentDetailsList(hdsComponentDetailsList, MAVEN_A1_COORDINATES, false);
    ComponentDetailsList componentDetailsList = componentInfoService.getComponentDetailsList_ReadPermission(
        owner.getType(), ownerId, MAVEN_A1_COORDINATES, MatchState.EXACT.getId());
    assertThat(componentDetailsList.getList()).hasSize(1);
    ComponentDetails componentDetails = componentDetailsList.getList().get(0);
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(MAVEN_A1_COORDINATES);
    assertThat(componentDetails.getMatchState()).isEqualTo(MatchState.EXACT.getId());
  }

  @Deprecated
  @Test
  public void testGetComponentDetailsList_ReadPermission_Application() {
    testGetComponentDetailsList_ReadPermission(application, application.getPublicId());
  }

  @Deprecated
  @Test
  public void testGetComponentDetailsList_ReadPermission_Repository() {
    testGetComponentDetailsList_ReadPermission(repository, repository.getId());
  }

  private ComponentVersionInfoDTO testGetComponentVersionInfo(
      final Owner owner,
      final String ownerId,
      final String stageId)
  {
    return testGetComponentVersionInfo(owner, ownerId, stageId, DependencyType.DIRECT);
  }

  private ComponentVersionInfoDTO testGetComponentVersionInfo(
      final Owner owner,
      final String ownerId,
      final String stageId,
      final DependencyType dependencyType)
  {
    ComponentDetails hdsComponentDetails1 = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    long timestamp = DateTime.now().getMillis();
    hdsComponentDetails1.setCatalogDate(timestamp);
    hdsComponentDetails1.setSecurityVulnerabilities(asList(
        new SecurityVulnerability("cve-8", "cve", 8.1f),
        new SecurityVulnerability("cve-4", "cve", 4f)));
    ComponentDetails hdsComponentDetails2 = newNamedComponentDetails(MAVEN_A2_COORDINATES);
    hdsComponentDetails2.setCatalogDate(timestamp);
    hdsComponentDetails2.setSecurityVulnerabilities(Collections.singletonList(
        new SecurityVulnerability("cve-7", "cve", 0.1f))); // too low for our security policy
    ComponentDetailsList hdsComponentDetailsList = new ComponentDetailsList();
    hdsComponentDetailsList.setList(asList(hdsComponentDetails1, hdsComponentDetails2));
    mockHdsGetComponentDetailsList(hdsComponentDetailsList, MAVEN_A1_COORDINATES);

    ComponentVersionInfoDTO dto = componentInfoService.getComponentVersionInfo(owner.getType(), ownerId,
        MAVEN_A1_COORDINATES, stageId, null, null, dependencyType);

    List<ComponentDetailsDTO> componentDetailsList = dto.allVersions;

    assertThat(componentDetailsList).hasSize(2);

    ComponentDetailsDTO componentDetails1 = componentDetailsList.get(0);
    assertThat(componentDetails1.displayName)
        .hasToString(ComponentDisplayNameUtil.fromIdentifier(MAVEN_A1_COORDINATES).toString());
    assertThat(componentDetails1.matchState).isEqualTo(MatchState.EXACT.getId());
    assertThat(componentDetails1.componentIdentifier).isEqualTo(hdsComponentDetails1.getComponentIdentifier());
    assertThat(componentDetails1.highestSecurityVulnerabilitySeverity).isEqualTo(8.1f);
    assertThat(componentDetails1.catalogDate).isEqualTo(timestamp);

    ComponentDetailsDTO componentDetails2 = componentDetailsList.get(1);
    assertThat(componentDetails2.displayName)
        .hasToString(ComponentDisplayNameUtil.fromIdentifier(MAVEN_A2_COORDINATES).toString());
    assertThat(componentDetails2.matchState).isEqualTo(MatchState.EXACT.getId());
    assertThat(componentDetails2.componentIdentifier).isEqualTo(hdsComponentDetails2.getComponentIdentifier());
    assertThat(componentDetails2.highestSecurityVulnerabilitySeverity).isEqualTo(0.1f);
    assertThat(componentDetails2.securityVulnerabilityCount).isEqualTo(1);
    assertThat(componentDetails2.catalogDate).isEqualTo(timestamp);

    return dto;
  }

  @Test
  public void testGetComponentVersionInfo_Application_NoStageId() {
    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "8"));
    Policy policy1 = new Policy("security-high", "Security-High");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setAction(ReleaseStageType.ID, FailActionType.ID);
    policy1.setOwnerId(application.getId());
    tempEntity.newPolicy(policy1);

    Constraint constraint2 = new Constraint("C2", "Constraint 2", LogicalOperator.AND);
    constraint2.addCondition(new Condition(LicenseConditionType.ID, "is not", "GPL-2.0")); // will hit both components
    Policy policy2 = new Policy("NonGpl2", "Non-GPL-2");
    policy2.setThreatLevel(6);
    policy2.addConstraint(constraint2);
    policy2.setAction(ReleaseStageType.ID, WarnActionType.ID);
    policy2.setOwnerId(application.getId());
    tempEntity.newPolicy(policy2);

    mockLicenseFeature(false);
    ComponentVersionInfoDTO dto = testGetComponentVersionInfo(
        application, application.getPublicId(), null);

    List<ComponentDetailsDTO> componentDetailsList = dto.allVersions;

    ComponentDetailsDTO componentDetails1 = componentDetailsList.get(0);
    assertThat(componentDetails1.policyMaxThreatLevelsByCategory)
        .isEqualTo(ImmutableMap.of(PolicyThreatCategory.SECURITY, 8, PolicyThreatCategory.LICENSE, 6));
    assertThat(componentDetails1.violatedPolicyCount).isEqualTo(2);
    assertThat(componentDetails1.policyAlerts).hasSize(2);
    // should use BuildStageType by default, so expected no alerts
    assertThat(componentDetails1.policyAlerts.get(0).getActions()).hasSize(0);
    assertThat(componentDetails1.policyAlerts.get(1).getActions()).hasSize(0);

    ComponentDetailsDTO componentDetails2 = componentDetailsList.get(1);
    assertThat(componentDetails2.policyMaxThreatLevelsByCategory)
        .isEqualTo(ImmutableMap.of(PolicyThreatCategory.LICENSE, 6));
    assertThat(componentDetails2.violatedPolicyCount).isEqualTo(1);
    assertThat(componentDetails2.policyAlerts).hasSize(1);
    // should use BuildStageType by default, so expected no alerts
    assertThat(componentDetails2.policyAlerts.get(0).getActions()).hasSize(0);

    assertThat(dto.remediation.versionChanges).isNotNull();
    assertThat(dto.remediation.versionChanges).hasSize(0);
  }

  @Test
  public void testGetComponentVersionInfo_Application_WithStageId() {
    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "8"));
    Policy policy1 = new Policy("security-high", "Security-High");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setAction(ReleaseStageType.ID, FailActionType.ID);
    policy1.setOwnerId(application.getId());
    tempEntity.newPolicy(policy1);

    Constraint constraint2 = new Constraint("C2", "Constraint 2", LogicalOperator.AND);
    constraint2.addCondition(new Condition(LicenseConditionType.ID, "is not", "GPL-2.0")); // will hit both components
    Policy policy2 = new Policy("NonGpl2", "Non-GPL-2");
    policy2.setThreatLevel(6);
    policy2.addConstraint(constraint2);
    policy2.setAction(ReleaseStageType.ID, WarnActionType.ID);
    policy2.setOwnerId(application.getId());
    tempEntity.newPolicy(policy2);

    mockLicenseFeature(false);
    ComponentVersionInfoDTO dto = testGetComponentVersionInfo(
        application, application.getPublicId(), ReleaseStageType.ID);

    List<ComponentDetailsDTO> componentDetailsList = dto.allVersions;

    ComponentDetailsDTO componentDetails1 = componentDetailsList.get(0);
    assertThat(componentDetails1.policyMaxThreatLevelsByCategory)
        .isEqualTo(ImmutableMap.of(PolicyThreatCategory.SECURITY, 8, PolicyThreatCategory.LICENSE, 6));
    assertThat(componentDetails1.violatedPolicyCount).isEqualTo(2);
    assertThat(componentDetails1.policyAlerts).hasSize(2);
    assertThat(componentDetails1.policyAlerts.get(0).getActions()).extracting(Action::getActionTypeId).contains("warn");
    assertThat(componentDetails1.policyAlerts.get(1).getActions()).extracting(Action::getActionTypeId).contains("fail");

    ComponentDetailsDTO componentDetails2 = componentDetailsList.get(1);
    assertThat(componentDetails2.policyMaxThreatLevelsByCategory)
        .isEqualTo(ImmutableMap.of(PolicyThreatCategory.LICENSE, 6));
    assertThat(componentDetails2.violatedPolicyCount).isEqualTo(1);
    assertThat(componentDetails2.policyAlerts).hasSize(1);
    assertThat(componentDetails2.policyAlerts.get(0).getActions()).extracting(Action::getActionTypeId).contains("warn");

    assertThat(dto.remediation.versionChanges).isNotNull();
    assertThat(dto.remediation.versionChanges).hasSize(1);
    assertThat(dto.remediation.versionChanges.get(0).getType()).isEqualTo(ApiVersionChangeOptionType.NEXT_NON_FAILING);
    assertThat(dto.remediation.versionChanges.get(0).getData().getComponent().packageUrl)
        .isEqualTo("pkg:maven/g1/a2@v1?type=jar");
    assertThat(dto.remediation.versionChanges.get(0).getData().getComponent().displayName).isEqualTo(
        ComponentDisplayNameUtil.fromIdentifier(
            dto.remediation.versionChanges.get(0).getData().getComponent().componentIdentifier
                .toComponentIdentifier())
            .toString());
  }

  @Test
  public void testGetComponentVersionInfo_Repository() {
    ComponentVersionInfoDTO dto = testGetComponentVersionInfo(repository, repository.getId(), null);
    assertThat(dto.remediation).isNotNull();
  }

  @Test
  public void testGetComponentVersionInfo_ExtraParams() {
    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "8"));
    constraint1.addCondition(new Condition(DependencyTypeConditionType.ID, "is", "transitive"));
    Policy policy1 = new Policy("security-high", "Security-High");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setAction(BuildStageType.ID, FailActionType.ID);
    policy1.setOwnerId(application.getId());
    tempEntity.newPolicy(policy1);

    final String identificationSource = "Clair";
    final String scanId = "scanId";
    final DependencyType dependencyType = DependencyType.TRANSITIVE;

    Map<String, String> coordinates = new HashMap<>();
    coordinates.put("name", "test");
    coordinates.put("version", "2.0.0");
    ComponentIdentifier componentIdentifier1 = new ComponentIdentifier("debian:9", coordinates);

    ComponentDetails tpComponentDetails = newNamedComponentDetails(componentIdentifier1);
    tpComponentDetails.setIdentificationSource(identificationSource);
    tpComponentDetails.setSecurityVulnerabilities(asList(
        new SecurityVulnerability("cve-8", "cve", 8.1f),
        new SecurityVulnerability("cve-4", "cve", 4f)));
    tpComponentDetails.setDeclaredLicenses(Collections.singleton(new License("Apache-2.0", "Apache License 2.0")));
    ComponentDetailsList thirdPartyComponentDetailsList = new ComponentDetailsList();
    thirdPartyComponentDetailsList.setList(Collections.singletonList(tpComponentDetails));

    when(thirdPartyComponentDAO.getAllVersions(application.getId(), componentIdentifier1, scanId))
        .thenReturn(thirdPartyComponentDetailsList);
    when(thirdPartyComponentDAO.getSuggestedRemmediation(application.getId(), componentIdentifier1, scanId))
        .thenReturn(new ApiComponentRemediationValueDTO());

    ComponentVersionInfoDTO dto = componentInfoService
        .getComponentVersionInfo(application.getType(), application.getPublicId(),
            componentIdentifier1, null, identificationSource, scanId, dependencyType);

    List<ComponentDetailsDTO> componentDetailsList = dto.allVersions;

    assertThat(componentDetailsList).hasSize(1);
    assertThat(dto.remediation).isNotNull();
    assertThat(dto.remediation.versionChanges).hasSize(0);

    ComponentDetailsDTO componentDetails = componentDetailsList.get(0);
    assertThat(componentDetails.displayName)
        .hasToString(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier1).toString());
    assertThat(componentDetails.matchState).isEqualTo(MatchState.EXACT.getId());
    assertThat(componentDetails.componentIdentifier).isEqualTo(tpComponentDetails.getComponentIdentifier());
    assertThat(componentDetails.highestSecurityVulnerabilitySeverity).isEqualTo(8.1f);
    assertThat(componentDetails.identificationSource).isEqualTo(identificationSource);

    assertThat(componentDetails.policyMaxThreatLevelsByCategory)
        .isEqualTo(ImmutableMap.of(PolicyThreatCategory.SECURITY, 8));
    assertThat(componentDetails.violatedPolicyCount).isEqualTo(1);

    assertThat(componentDetails.declaredLicenses).hasSize(1);
  }

  @Test
  public void testGetComponentVersionInfo_PackageManifest() {
    String identificationSource = IdentificationSource.PACKAGE_MANIFEST.getId();
    String scanId = "scanId";

    ComponentDetails componentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    componentDetails.setIdentificationSource(identificationSource);
    ComponentDetailsList componentDetailsList = new ComponentDetailsList();
    componentDetailsList.setList(Collections.singletonList(componentDetails));

    mockHdsGetComponentDetailsList(new ComponentDetailsList(), componentDetails.getComponentIdentifier());

    ComponentVersionInfoDTO dto = componentInfoService.getComponentVersionInfo(application.getType(),
        application.getPublicId(), MAVEN_A1_COORDINATES, null, identificationSource, scanId, null);

    List<ComponentDetailsDTO> resultComponentDetailsList = dto.allVersions;

    assertThat(resultComponentDetailsList).hasSize(1);
    assertThat(dto.remediation).isNotNull();
    assertThat(dto.remediation.versionChanges).isNotEmpty();
    assertThat(
        dto.remediation.versionChanges.get(0).getData().getComponent().componentIdentifier.toComponentIdentifier())
            .isEqualTo(MAVEN_A1_COORDINATES);

    ComponentDetailsDTO resultComponentDetails = resultComponentDetailsList.get(0);
    assertThat(resultComponentDetails.displayName)
        .hasToString(ComponentDisplayNameUtil.fromIdentifier(MAVEN_A1_COORDINATES).toString());
    assertThat(resultComponentDetails.matchState).isEqualTo(MatchState.EXACT.getId());
    assertThat(resultComponentDetails.componentIdentifier).isEqualTo(componentDetails.getComponentIdentifier());
    assertThat(resultComponentDetails.highestSecurityVulnerabilitySeverity).isEqualTo(0f);
    assertThat(resultComponentDetails.identificationSource).isEqualTo(identificationSource);
    assertThat(resultComponentDetails.policyMaxThreatLevelsByCategory).isEmpty();
    assertThat(resultComponentDetails.violatedPolicyCount).isEqualTo(0);
    assertThat(resultComponentDetails.declaredLicenses).extracting(License::getLicenseId, License::getLicenseName)
        .contains(tuple(UNSPECIFIED_ID, "Not Provided"));
    assertThat(resultComponentDetails.observedLicenses).extracting(License::getLicenseId, License::getLicenseName)
        .contains(tuple(UNSPECIFIED_ID, "Not Provided"));
    assertThat(resultComponentDetails.effectiveLicenses).extracting(License::getLicenseId, License::getLicenseName)
        .contains(tuple(UNSPECIFIED_ID, "Not Provided"));
  }

  @Test
  public void testGetComponentVersionInfo_InnerSourceRepository_withRequestedVersion_Beginning() {
    String identificationSource = IdentificationSource.PACKAGE_MANIFEST.getId();
    String scanId = "scanId";

    mockRepositoryQueryServiceAllVersionResponse(MAVEN_A1_COORDINATES, "v1", "v2", "v3");
    mockHdsGetComponentDetailsList(new ComponentDetailsList(), MAVEN_A1_COORDINATES);

    ComponentVersionInfoDTO dto = componentInfoService.getComponentVersionInfo(application.getType(),
        application.getPublicId(), MAVEN_A1_COORDINATES, null, identificationSource, scanId,
        DependencyType.INNER_SOURCE);

    List<ComponentDetailsDTO> result = dto.allVersions;

    assertThat(result).hasSize(3);
    assertGetComponentVersionsRepositoryResult(result.get(0), MAVEN_A1_COORDINATES.createAlternativeVersion("v1"));
    assertGetComponentVersionsRepositoryResult(result.get(1), MAVEN_A1_COORDINATES.createAlternativeVersion("v2"));
    assertGetComponentVersionsRepositoryResult(result.get(2), MAVEN_A1_COORDINATES.createAlternativeVersion("v3"));
    assertThat(dto.sourceResponse.source).isEqualTo("https://repo.sonatype.com/");
  }

  @Test
  public void testGetComponentVersionInfo_InnerSourceRepository_withRequestedVersion_ThirdParty() {
    String identificationSource = "third-party";
    String scanId = "scanId";

    mockRepositoryQueryServiceAllVersionResponse(MAVEN_A1_COORDINATES, "v1", "v2", "v3");

    ComponentDetails tpComponent = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    ComponentDetailsList hdsComponentDetailsList = new ComponentDetailsList();
    hdsComponentDetailsList.setList(Stream.of(tpComponent).collect(Collectors.toList()));
    mockHdsGetComponentDetailsList(hdsComponentDetailsList, MAVEN_A1_COORDINATES);

    tpComponent.setIdentificationSource(identificationSource);
    when(thirdPartyComponentDAO.resolveComponentDetails(application.getId(), MAVEN_A1_COORDINATES, scanId))
        .thenReturn(tpComponent);

    ComponentVersionInfoDTO dto = componentInfoService.getComponentVersionInfo(application.getType(),
        application.getPublicId(), MAVEN_A1_COORDINATES, null, identificationSource, scanId,
        DependencyType.INNER_SOURCE);

    List<ComponentDetailsDTO> result = dto.allVersions;

    assertThat(result).hasSize(3);
    assertGetComponentVersionsRepositoryResult(result.get(0), MAVEN_A1_COORDINATES.createAlternativeVersion("v1"));
    assertGetComponentVersionsRepositoryResult(result.get(1), MAVEN_A1_COORDINATES.createAlternativeVersion("v2"));
    assertGetComponentVersionsRepositoryResult(result.get(2), MAVEN_A1_COORDINATES.createAlternativeVersion("v3"));
    assertThat(dto.sourceResponse.source).isEqualTo("https://repo.sonatype.com/");
  }

  @Test
  public void testGetComponentVersionInfo_InnerSourceRepository_withRequestedVersion_inBetween() {
    String identificationSource = IdentificationSource.PACKAGE_MANIFEST.getId();
    String scanId = "scanId";

    mockRepositoryQueryServiceAllVersionResponse(MAVEN_A1_COORDINATES, "v0", "v1", "v2");
    mockHdsGetComponentDetailsList(new ComponentDetailsList(), MAVEN_A1_COORDINATES);

    ComponentVersionInfoDTO dto = componentInfoService.getComponentVersionInfo(application.getType(),
        application.getPublicId(), MAVEN_A1_COORDINATES, null, identificationSource, scanId,
        DependencyType.INNER_SOURCE);

    List<ComponentDetailsDTO> result = dto.allVersions;

    assertThat(result).hasSize(3);
    assertGetComponentVersionsRepositoryResult(result.get(0), MAVEN_A1_COORDINATES.createAlternativeVersion("v0"));
    assertGetComponentVersionsRepositoryResult(result.get(1), MAVEN_A1_COORDINATES.createAlternativeVersion("v1"));
    assertGetComponentVersionsRepositoryResult(result.get(2), MAVEN_A1_COORDINATES.createAlternativeVersion("v2"));
    assertThat(dto.sourceResponse.source).isEqualTo("https://repo.sonatype.com/");
  }

  @Test
  public void testGetComponentVersionInfo_InnerSourceRepository_withRequestedVersion_End() {
    String identificationSource = IdentificationSource.PACKAGE_MANIFEST.getId();
    String scanId = "scanId";

    mockRepositoryQueryServiceAllVersionResponse(MAVEN_A1_COORDINATES, "v0.4", "v0.8", "v1");
    mockHdsGetComponentDetailsList(new ComponentDetailsList(), MAVEN_A1_COORDINATES);

    ComponentVersionInfoDTO dto = componentInfoService.getComponentVersionInfo(application.getType(),
        application.getPublicId(), MAVEN_A1_COORDINATES, null, identificationSource, scanId,
        DependencyType.INNER_SOURCE);

    List<ComponentDetailsDTO> result = dto.allVersions;

    assertThat(result).hasSize(3);
    assertGetComponentVersionsRepositoryResult(result.get(0), MAVEN_A1_COORDINATES.createAlternativeVersion("v0.4"));
    assertGetComponentVersionsRepositoryResult(result.get(1), MAVEN_A1_COORDINATES.createAlternativeVersion("v0.8"));
    assertGetComponentVersionsRepositoryResult(result.get(2), MAVEN_A1_COORDINATES.createAlternativeVersion("v1"));
    assertThat(dto.sourceResponse.source).isEqualTo("https://repo.sonatype.com/");
  }

  @Test
  public void testGetComponentVersionInfo_InnerSourceRepository_missingRequestedVersion() {
    String identificationSource = IdentificationSource.PACKAGE_MANIFEST.getId();
    String scanId = "scanId";
    mockRepositoryQueryServiceAllVersionResponse(MAVEN_A1_COORDINATES, "v0", "v3");
    mockHdsGetComponentDetailsList(new ComponentDetailsList(), MAVEN_A1_COORDINATES);

    ComponentVersionInfoDTO dto = componentInfoService.getComponentVersionInfo(application.getType(),
        application.getPublicId(), MAVEN_A1_COORDINATES, null, identificationSource, scanId,
        DependencyType.INNER_SOURCE);

    List<ComponentDetailsDTO> result = dto.allVersions;

    assertThat(result).hasSize(3);
    assertGetComponentVersionsRepositoryResult(result.get(0), MAVEN_A1_COORDINATES.createAlternativeVersion("v0"));
    assertGetComponentVersionsRepositoryResult(result.get(1), MAVEN_A1_COORDINATES);
    assertGetComponentVersionsRepositoryResult(result.get(2), MAVEN_A1_COORDINATES.createAlternativeVersion("v3"));
    assertThat(dto.sourceResponse.source).isEqualTo("https://repo.sonatype.com/");
  }

  @Test
  public void testGetComponentVersionInfo_InnerSourceRepository_noResult() {
    String identificationSource = IdentificationSource.PACKAGE_MANIFEST.getId();
    String scanId = "scanId";
    mockRepositoryQueryServiceAllVersionResponse(MAVEN_A1_COORDINATES);
    mockHdsGetComponentDetailsList(new ComponentDetailsList(), MAVEN_A1_COORDINATES);

    ComponentVersionInfoDTO dto = componentInfoService.getComponentVersionInfo(application.getType(),
        application.getPublicId(), MAVEN_A1_COORDINATES, null, identificationSource, scanId,
        DependencyType.INNER_SOURCE);

    List<ComponentDetailsDTO> result = dto.allVersions;

    assertThat(result).hasSize(1);
    assertGetComponentVersionsRepositoryResult(result.get(0), MAVEN_A1_COORDINATES);
    assertThat(dto.sourceResponse.source).isNull();
  }

  @Test
  public void testGetComponentVersionInfo_InnerSourceRepository_nullSource() {
    String identificationSource = IdentificationSource.PACKAGE_MANIFEST.getId();
    String scanId = "scanId";
    when(repositoryQueryService.getAllVersions(eq(MAVEN_A1_COORDINATES), any(Owner.class))).thenReturn(
        Pair.of(new RepositoryAllVersionsResponse(Collections.emptyList()), null));
    mockHdsGetComponentDetailsList(new ComponentDetailsList(), MAVEN_A1_COORDINATES);

    ComponentVersionInfoDTO dto = componentInfoService.getComponentVersionInfo(application.getType(),
        application.getPublicId(), MAVEN_A1_COORDINATES, null, identificationSource, scanId,
        DependencyType.INNER_SOURCE);

    List<ComponentDetailsDTO> result = dto.allVersions;
    assertThat(result).hasSize(1);
    assertGetComponentVersionsRepositoryResult(result.get(0), MAVEN_A1_COORDINATES);
    assertThat(dto.sourceResponse).isNull();
  }

  @Test
  public void testGetComponentVersionInfo_InnerSourceRepository_ThirdParty_noResult() {
    String identificationSource = "third-party";
    String scanId = "scanId";
    mockRepositoryQueryServiceAllVersionResponse(MAVEN_A1_COORDINATES);
    ComponentDetailsList componentDetailsList = new ComponentDetailsList();
    componentDetailsList.setList(new ArrayList<>());
    mockHdsGetComponentDetailsList(componentDetailsList, MAVEN_A1_COORDINATES);
    ComponentDetails componentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    componentDetails.setIdentificationSource(identificationSource);
    when(thirdPartyComponentDAO.resolveComponentDetails(application.getId(), MAVEN_A1_COORDINATES, scanId))
        .thenReturn(componentDetails);

    ComponentVersionInfoDTO dto = componentInfoService.getComponentVersionInfo(application.getType(),
        application.getPublicId(), MAVEN_A1_COORDINATES, null, identificationSource, scanId,
        DependencyType.INNER_SOURCE);

    List<ComponentDetailsDTO> result = dto.allVersions;

    assertThat(result).hasSize(1);
    assertThat(result.get(0).componentIdentifier).isEqualTo(MAVEN_A1_COORDINATES);
    assertThat(result.get(0).matchState).isEqualTo(MatchState.EXACT.getId());
    assertThat(result.get(0).identificationSource).isEqualTo(identificationSource);
    assertThat(dto.sourceResponse.source).isNull();
  }

  @Test
  public void testGetComponentVersionInfo_NoInnerSourceRepository_HdsResults_Matched() {
    String identificationSource = IdentificationSource.SONATYPE.getId();
    String scanId = "scanId";

    ComponentDetails hdsComponentDetails1 = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    ComponentDetails hdsComponentDetails2 = newNamedComponentDetails(MAVEN_A2_COORDINATES);
    ComponentDetailsList hdsComponentDetailsList = new ComponentDetailsList();
    hdsComponentDetailsList.setList(asList(hdsComponentDetails1, hdsComponentDetails2));
    mockHdsGetComponentDetailsList(hdsComponentDetailsList, MAVEN_A1_COORDINATES);

    ComponentVersionInfoDTO dto = componentInfoService.getComponentVersionInfo(application.getType(),
        application.getPublicId(), MAVEN_A1_COORDINATES, null, identificationSource, scanId,
        DependencyType.INNER_SOURCE);

    List<ComponentDetailsDTO> result = dto.allVersions;

    assertThat(result).hasSize(2)
        .allSatisfy(detail -> assertThat(detail.identificationSource).isEqualTo(identificationSource));
    assertThat(dto.sourceResponse).isNull();
    verify(repositoryQueryService, never()).getAllVersions(eq(MAVEN_A1_COORDINATES), any(Owner.class));
  }

  @Test
  public void testGetComponentVersionInfo_NoInnerSourceRepository_HdsResults_PackageManifest() {
    String identificationSource = IdentificationSource.PACKAGE_MANIFEST.getId();
    String scanId = "scanId";

    ComponentDetails hdsComponentDetails1 = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    hdsComponentDetails1.setIdentificationSource(identificationSource);
    ComponentDetails hdsComponentDetails2 = newNamedComponentDetails(MAVEN_A2_COORDINATES);
    ComponentDetailsList hdsComponentDetailsList = new ComponentDetailsList();
    hdsComponentDetailsList.setList(asList(hdsComponentDetails1, hdsComponentDetails2));
    mockHdsGetComponentDetailsList(hdsComponentDetailsList, MAVEN_A1_COORDINATES);

    ComponentVersionInfoDTO dto = componentInfoService.getComponentVersionInfo(application.getType(),
        application.getPublicId(), MAVEN_A1_COORDINATES, null, identificationSource, scanId,
        DependencyType.INNER_SOURCE);

    List<ComponentDetailsDTO> result = dto.allVersions;

    assertThat(result).hasSize(2)
        .extracting(d -> d.identificationSource)
        .containsExactlyInAnyOrder(identificationSource, IdentificationSource.SONATYPE.getId());
    assertThat(dto.sourceResponse).isNull();
    verify(repositoryQueryService, never()).getAllVersions(eq(MAVEN_A1_COORDINATES), any(Owner.class));
  }

  @Test
  public void testGetComponentVersionInfo_NoInnerSourceRepository_HdsResults_ThirdParty() {
    String identificationSource = "third-party";
    String scanId = "scanId";

    ComponentDetails hdsComponentDetails1 = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    ComponentDetails hdsComponentDetails2 = newNamedComponentDetails(MAVEN_A2_COORDINATES);
    ComponentDetailsList hdsComponentDetailsList = new ComponentDetailsList();
    hdsComponentDetailsList.setList(asList(hdsComponentDetails1, hdsComponentDetails2));
    mockHdsGetComponentDetailsList(hdsComponentDetailsList, MAVEN_A1_COORDINATES);

    hdsComponentDetails1.setIdentificationSource(identificationSource);
    when(thirdPartyComponentDAO.resolveComponentDetails(application.getId(), MAVEN_A1_COORDINATES, scanId))
        .thenReturn(hdsComponentDetails1);

    ComponentVersionInfoDTO dto = componentInfoService.getComponentVersionInfo(application.getType(),
        application.getPublicId(), MAVEN_A1_COORDINATES, null, identificationSource, scanId,
        DependencyType.INNER_SOURCE);

    List<ComponentDetailsDTO> result = dto.allVersions;

    assertThat(result).hasSize(2)
        .extracting(d -> d.identificationSource)
        .containsExactlyInAnyOrder(identificationSource, IdentificationSource.SONATYPE.getId());
    assertThat(dto.sourceResponse).isNull();
    verify(repositoryQueryService, never()).getAllVersions(eq(MAVEN_A1_COORDINATES), any(Owner.class));
  }

  @Test
  public void testGetComponentVersionInfo_InnerSourceRepository_npm() {
    String identificationSource = IdentificationSource.PACKAGE_MANIFEST.getId();
    String scanId = "scanId";
    mockHdsGetComponentDetailsList(new ComponentDetailsList(), NPM_COORDINATES);
    mockRepositoryQueryServiceAllVersionResponse(NPM_COORDINATES, "v0", "v1", "v2");

    ComponentVersionInfoDTO dto = componentInfoService.getComponentVersionInfo(application.getType(),
        application.getPublicId(), NPM_COORDINATES, null, identificationSource, scanId,
        DependencyType.INNER_SOURCE);

    List<ComponentDetailsDTO> result = dto.allVersions;

    assertThat(result).hasSize(3);
    assertGetComponentVersionsRepositoryResult(result.get(0), NPM_COORDINATES.createAlternativeVersion("v0"));
    assertGetComponentVersionsRepositoryResult(result.get(1), NPM_COORDINATES);
    assertGetComponentVersionsRepositoryResult(result.get(2), NPM_COORDINATES.createAlternativeVersion("v2"));
    assertThat(dto.sourceResponse.source).isEqualTo("https://repo.sonatype.com/");
  }

  private void assertGetComponentVersionsRepositoryResult(
      final ComponentDetailsDTO cp,
      final ComponentIdentifier expectedComponentIdentifier)
  {
    assertThat(cp.componentIdentifier).isEqualTo(expectedComponentIdentifier);
    assertThat(cp.matchState).isEqualTo(MatchState.EXACT.getId());
    assertThat(cp.identificationSource).isEqualTo(IdentificationSource.PACKAGE_MANIFEST.getId());
    assertThat(cp.declaredLicenses).hasSize(1).extracting("licenseId").containsExactly("UNSPECIFIED");
    assertThat(cp.observedLicenses).hasSize(1).extracting("licenseId").containsExactly("UNSPECIFIED");
    assertThat(cp.effectiveLicenses).hasSize(1).extracting("licenseId").containsExactly("UNSPECIFIED");
  }

  private void mockRepositoryQueryServiceAllVersionResponse(
      ComponentIdentifier componentIdentifier,
      String... mockVersions)
  {
    List<ProxyRepositoryComponentResult> resultComponents = Stream.of(mockVersions)
        .map(v -> new ProxyRepositoryComponentResult(componentIdentifier.createAlternativeVersion(v), "sha" + v))
        .collect(Collectors.toList());
    RepositoryAllVersionsResponse response = new RepositoryAllVersionsResponse(resultComponents);
    RepositorySourceResponseDTO mockSource = new RepositorySourceResponseDTO();
    mockSource.source = "https://repo.sonatype.com/";
    when(repositoryQueryService.getAllVersions(eq(componentIdentifier), any(Owner.class))).thenReturn(
        Pair.of(response, mockSource));
  }

  @Test
  public void testGetComponentDetailsList_UnknownHdsComponent() {
    String scanId = "test";
    String identificationSource = "cyclone";

    // Create the mocked hds response
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createCargoCoordinates("test", "2.0.0", null);
    ComponentDetails hdsComponentDetails1 = newNamedComponentDetails(componentIdentifier1);

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createCargoCoordinates("test", "1.0.0", null);
    ComponentDetails hdsComponentDetails2 = newNamedComponentDetails(componentIdentifier2);
    ComponentDetailsList hdsComponentDetailsList = new ComponentDetailsList();

    hdsComponentDetailsList.setList(Arrays.asList(hdsComponentDetails1, hdsComponentDetails2));
    mockHdsGetComponentDetailsList(hdsComponentDetailsList, componentIdentifier1);

    ComponentDetails tpComponentDetails = newNamedComponentDetails(componentIdentifier1);
    tpComponentDetails.setIdentificationSource(identificationSource);
    tpComponentDetails.setSecurityVulnerabilities(asList(
        new SecurityVulnerability("cve-8", "cve", 8.1f),
        new SecurityVulnerability("cve-4", "cve", 4f)));
    tpComponentDetails.setDeclaredLicenses(Collections.singleton(new License("Apache-2.0", "Apache License 2.0")));

    when(thirdPartyComponentDAO.resolveComponentDetails(application.getId(), componentIdentifier1, scanId))
        .thenReturn(tpComponentDetails);

    ComponentDetailsList componentDetailsList = componentInfoService.getComponentDetailsList(
        componentIdentifier1, application, identificationSource, scanId, null, true).getLeft();

    assertThat(componentDetailsList).isNotNull();
    assertThat(componentDetailsList.getList()).hasSize(2);

    ComponentDetails componentDetails = componentDetailsList.getList().get(0);
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(componentIdentifier1);
    assertThat(componentDetails.getSecurityVulnerabilities()).hasSize(2);
    assertThat(componentDetails.getDeclaredLicenses()).extracting(License::getLicenseId, License::getLicenseName)
        .contains(tuple("Apache-2.0", "Apache License 2.0"));
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(componentIdentifier1);

    componentDetails = componentDetailsList.getList().get(1);
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(componentIdentifier2);
  }

  @Test
  public void testGetComponentDetailsList_ErrorHdsComponent_ThirdParty() {
    String scanId = "test";
    String identificationSource = "cyclone";

    Map<String, String> coordinates = new HashMap<>();
    coordinates.put("name", "test");
    coordinates.put("version", "2.0.0");
    ComponentIdentifier componentIdentifier1 = new ComponentIdentifier(ComponentIdentifier.FORMAT_PYPI, coordinates);

    when(hdsClientMock.get(ComponentDetailsList.class, "rest/" + TOOL_NAME +
        "/componentDetails/list",
        Map.of("componentIdentifier", ComponentIdentifierAdapter.toJson(componentIdentifier1),
            "stableVersionsOnly", "true")))
                .thenThrow(BadRequestException.class);

    ComponentDetails tpComponentDetails = newNamedComponentDetails(componentIdentifier1);
    tpComponentDetails.setIdentificationSource(identificationSource);
    tpComponentDetails.setSecurityVulnerabilities(asList(
        new SecurityVulnerability("cve-8", "cve", 8.1f),
        new SecurityVulnerability("cve-4", "cve", 4f)));
    tpComponentDetails.setDeclaredLicenses(Collections.singleton(new License("Apache-2.0", "Apache License 2.0")));

    ComponentDetailsList thirdPartyComponentDetailsList = new ComponentDetailsList();
    thirdPartyComponentDetailsList.setList(Collections.singletonList(tpComponentDetails));

    when(thirdPartyComponentDAO.getAllVersions(application.getId(), componentIdentifier1, scanId))
        .thenReturn(thirdPartyComponentDetailsList);

    ComponentDetailsList componentDetailsList = componentInfoService.getComponentDetailsList(
        componentIdentifier1, application, identificationSource, scanId, null, true).getLeft();

    assertThat(componentDetailsList).isNotNull();
    assertThat(componentDetailsList.getList()).hasSize(1);

    ComponentDetails componentDetails = componentDetailsList.getList().get(0);
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(componentIdentifier1);
    assertThat(componentDetails.getSecurityVulnerabilities()).hasSize(2);
    assertThat(componentDetails.getDeclaredLicenses()).extracting(License::getLicenseId, License::getLicenseName)
        .contains(tuple("Apache-2.0", "Apache License 2.0"));
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(componentIdentifier1);
  }

  @Test
  public void testGetComponentDetailsList_ErrorHdsComponent() {
    String scanId = "test";
    DependencyType dependencyType = DependencyType.DIRECT;
    String identificationSource = IdentificationSource.SONATYPE.toString();

    Map<String, String> coordinates = new HashMap<>();
    coordinates.put("name", "test");
    coordinates.put("version", "2.0.0");
    ComponentIdentifier componentIdentifier1 = new ComponentIdentifier(ComponentIdentifier.FORMAT_PYPI, coordinates);

    when(hdsClientMock.get(ComponentDetailsList.class, "rest/" + TOOL_NAME +
        "/componentDetails/list",
        Map.of("componentIdentifier", ComponentIdentifierAdapter.toJson(componentIdentifier1),
            "stableVersionsOnly", "true")))
                .thenThrow(BadRequestException.class);

    assertThatThrownBy(
        () -> componentInfoService
            .getComponentDetailsList(componentIdentifier1, application, identificationSource, scanId, dependencyType,
                true))
                    .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void testGetComponentDetailsList_Terraform() {
    String scanId = "test";
    String identificationSource = "IaC";

    Map<String, String> coordinates = new HashMap<>();
    coordinates.put("plan", "plan.tfplan");
    coordinates.put("name", "test");
    coordinates.put("version", "current");
    ComponentIdentifier componentIdentifier1 =
        new ComponentIdentifier(ComponentIdentifier.FORMAT_TERRAFORM, coordinates);

    ComponentDetails tpComponentDetails = newNamedComponentDetails(componentIdentifier1);
    tpComponentDetails.setIdentificationSource(identificationSource);
    tpComponentDetails.setSecurityVulnerabilities(asList(
        new SecurityVulnerability("cve-8", "cve", 8.1f),
        new SecurityVulnerability("cve-4", "cve", 4f)));

    ComponentDetailsList tpComponentDetailsList = new ComponentDetailsList();
    tpComponentDetailsList.setList(Collections.singletonList(tpComponentDetails));

    when(thirdPartyComponentDAO.getAllVersions(application.getId(), componentIdentifier1, scanId))
        .thenReturn(tpComponentDetailsList);

    ComponentDetailsList componentDetailsList =
        componentInfoService.getComponentDetailsList(componentIdentifier1, application, identificationSource, scanId,
            null, true).getLeft();

    assertThat(componentDetailsList).isNotNull();
    ComponentDetails componentDetails = componentDetailsList.getList().get(0);
    assertThat(componentDetails.getIdentificationSource()).isEqualTo("IaC");
    ComponentIdentifier componentIdentifier = componentDetails.getComponentIdentifier();
    assertThat(componentIdentifier.getFormat()).isEqualTo(ComponentIdentifier.FORMAT_TERRAFORM);
    assertThat(componentIdentifier.get("plan")).isEqualTo("plan.tfplan");
    assertThat(componentIdentifier.get("name")).isEqualTo("test");
    assertThat(componentIdentifier.get("version")).isEqualTo("current");
    assertThat(componentDetails.getSecurityVulnerabilities()).hasSize(2);
    assertThat(componentDetails.getSecurityVulnerabilities().get(0).getRefId()).isEqualTo("cve-8");
    assertThat(componentDetails.getSecurityVulnerabilities().get(1).getRefId()).isEqualTo("cve-4");
  }

  @Test
  public void testGetComponentDetailsList_InvalidComponent() {
    Map<String, String> coordinates = new HashMap<>();
    coordinates.put("name", "test");
    coordinates.put("version", "2.0.0");
    ComponentIdentifier componentIdentifier1 = new ComponentIdentifier("unknown", coordinates);

    assertThatThrownBy(
        () -> componentInfoService.getComponentDetailsList(componentIdentifier1, application, null, null, null,
            true))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid format: unknown");
  }

  @Test
  public void testGetComponentVersionInfo_WithAdvancedRecommendation() {
    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "5"));
    Policy policy1 = new Policy("security-low", "Security-Low");
    policy1.setThreatLevel(5);
    policy1.addConstraint(constraint1);
    policy1.setAction(ReleaseStageType.ID, WarnActionType.ID);
    policy1.setOwnerId(application.getId());
    tempEntity.newPolicy(policy1);

    // mock dependencies for advanced recommendation strategies
    PackageUrlIdentifier mvnPurlId = PackageUrlIdentifier.fromComponentIdentifier(MAVEN_A1_COORDINATES);
    PackageUrlIdentifier depPurlId = PackageUrlIdentifier.fromComponentIdentifier(MAVEN_A2_COORDINATES);
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();
    dependenciesMap.put(mvnPurlId, Collections.singletonList(depPurlId));
    detailsMap.put(depPurlId, new ComponentDetails());
    ComponentDependenciesDTO dependenciesDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    mockHdsGetComponentDependencies(dependenciesDto);
    mockLicenseFeature(true);

    ComponentVersionInfoDTO dto =
        testGetComponentVersionInfo(application, application.getPublicId(), ReleaseStageType.ID);

    assertThat(dto.remediation.versionChanges).isNotNull();
    assertThat(dto.remediation.versionChanges).hasSize(2);
    assertThat(dto.remediation.versionChanges).extracting(vc -> vc.getType().name())
        .containsExactlyInAnyOrder(
            NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES.name(),
            NEXT_NON_FAILING_WITH_DEPENDENCIES.name());
    assertThat(dto.remediation.versionChanges).extracting(vc -> vc.getData().getComponent().packageUrl)
        .containsExactlyInAnyOrder(
            depPurlId.getPackageUrl(),
            mvnPurlId.getPackageUrl());
  }

  @Test
  public void testGetComponentVersionInfo_WithAdvancedRecommendationAndPullRequestStatus() {
    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "5"));
    Policy policy1 = new Policy("security-low", "Security-Low");
    policy1.setThreatLevel(5);
    policy1.addConstraint(constraint1);
    policy1.setAction(ReleaseStageType.ID, WarnActionType.ID);
    policy1.setOwnerId(application.getId());
    tempEntity.newPolicy(policy1);

    // mock dependencies for advanced recommendation strategies
    PackageUrlIdentifier mvnPurlId = PackageUrlIdentifier.fromComponentIdentifier(MAVEN_A1_COORDINATES);
    PackageUrlIdentifier depPurlId = PackageUrlIdentifier.fromComponentIdentifier(MAVEN_A2_COORDINATES);
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();
    dependenciesMap.put(mvnPurlId, Collections.singletonList(depPurlId));
    detailsMap.put(depPurlId, new ComponentDetails());
    ComponentDependenciesDTO dependenciesDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    mockHdsGetComponentDependencies(dependenciesDto);
    mockLicenseFeature(true);

    // pull request creation complete
    SourceControlEvent sourceControlEvent =
        insertSourceControlEvent(SourceControlEvent.EVENT_STATUS_COMPLETE);
    ComponentVersionInfoDTO dto =
        testGetComponentVersionInfo(application, application.getPublicId(), SourceStageType.ID);

    PullRequestDTO pullRequestDTO = (PullRequestDTO) dto.automatedRemediationStatus;
    assertThat(pullRequestDTO).isNotNull();
    assertThat(pullRequestDTO.status).isEqualTo(AutomatedRemediationStatus.PULL_REQUEST);
    assertThat(pullRequestDTO.url).isEqualTo("https://git.com/pull/1");

    // pull request creation failed
    sourceControlEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_ERROR);
    sourceControlEventDAO.update(sourceControlEvent);
    dto = testGetComponentVersionInfo(application, application.getPublicId(), SourceStageType.ID);

    PullRequestCreationFailedDTO pullRequestCreationFailedDTO =
        (PullRequestCreationFailedDTO) dto.automatedRemediationStatus;
    assertThat(pullRequestCreationFailedDTO).isNotNull();
    assertThat(pullRequestCreationFailedDTO.status).isEqualTo(AutomatedRemediationStatus.PULL_REQUEST_CREATION_FAILED);

    // pull request creation in progress
    sourceControlEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_IN_PROGRESS);
    sourceControlEventDAO.update(sourceControlEvent);
    dto = testGetComponentVersionInfo(application, application.getPublicId(), SourceStageType.ID);

    PullRequestCreationPendingDTO pullRequestCreationPendingDTO =
        (PullRequestCreationPendingDTO) dto.automatedRemediationStatus;
    assertThat(pullRequestCreationPendingDTO).isNotNull();
    assertThat(pullRequestCreationPendingDTO.status).isEqualTo(
        AutomatedRemediationStatus.PULL_REQUEST_CREATION_PENDING);

    // pull request creation new
    sourceControlEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_NEW);
    sourceControlEventDAO.update(sourceControlEvent);
    dto = testGetComponentVersionInfo(application, application.getPublicId(), SourceStageType.ID);

    PullRequestCreationPendingDTO pullRequestCreationPendingDTONew =
        (PullRequestCreationPendingDTO) dto.automatedRemediationStatus;
    assertThat(pullRequestCreationPendingDTONew).isNotNull();
    assertThat(pullRequestCreationPendingDTONew.status).isEqualTo(
        AutomatedRemediationStatus.PULL_REQUEST_CREATION_PENDING);
  }

  @Test
  public void testGetComponentVersionInfo_PullRequestStatusLookupFailsGracefullyOnDbError() {
    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "5"));
    Policy policy1 = new Policy("security-low", "Security-Low");
    policy1.setThreatLevel(5);
    policy1.addConstraint(constraint1);
    policy1.setAction(ReleaseStageType.ID, WarnActionType.ID);
    policy1.setOwnerId(application.getId());
    tempEntity.newPolicy(policy1);

    // mock dependencies for advanced recommendation strategies
    PackageUrlIdentifier mvnPurlId = PackageUrlIdentifier.fromComponentIdentifier(MAVEN_A1_COORDINATES);
    PackageUrlIdentifier depPurlId = PackageUrlIdentifier.fromComponentIdentifier(MAVEN_A2_COORDINATES);
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();
    dependenciesMap.put(mvnPurlId, Collections.singletonList(depPurlId));
    detailsMap.put(depPurlId, new ComponentDetails());
    ComponentDependenciesDTO dependenciesDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    mockHdsGetComponentDependencies(dependenciesDto);
    mockLicenseFeature(true);

    // pull request creation complete, but looking up its current state hits a DB failure (e.g. a transient
    // connectivity issue) instead of returning normally
    insertSourceControlEvent(SourceControlEvent.EVENT_STATUS_COMPLETE);
    doThrow(new DataAccessException("simulated DB read failure"))
        .when(sourceControlPullRequestDAOSpy)
        .getByApplicationIdAndPullRequestId(anyString(), anyInt());

    ComponentVersionInfoDTO dto =
        testGetComponentVersionInfo(application, application.getPublicId(), SourceStageType.ID);

    // the whole component-details response must still succeed, reporting the existing pull request rather than
    // failing the request or silently dropping its status
    PullRequestDTO pullRequestDTO = (PullRequestDTO) dto.automatedRemediationStatus;
    assertThat(pullRequestDTO).isNotNull();
    assertThat(pullRequestDTO.status).isEqualTo(AutomatedRemediationStatus.PULL_REQUEST);
    assertThat(pullRequestDTO.url).isEqualTo("https://git.com/pull/1");

    // guard against the stub silently not firing (e.g. if the production call's arguments ever changed) and the
    // assertions above passing for the wrong reason
    verify(sourceControlPullRequestDAOSpy).getByApplicationIdAndPullRequestId(application.getId(), 1);
  }

  @Test
  public void testGetComponentVersionInfo_WithAdvancedRecommendationAndPullRequestStatus_Priority() {
    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "5"));
    Policy policy1 = new Policy("security-low", "Security-Low");
    policy1.setThreatLevel(5);
    policy1.addConstraint(constraint1);
    policy1.setAction(ReleaseStageType.ID, WarnActionType.ID);
    policy1.setOwnerId(application.getId());
    tempEntity.newPolicy(policy1);

    // mock dependencies for advanced recommendation strategies
    PackageUrlIdentifier mvnPurlId = PackageUrlIdentifier.fromComponentIdentifier(MAVEN_A1_COORDINATES);
    PackageUrlIdentifier depPurlId = PackageUrlIdentifier.fromComponentIdentifier(MAVEN_A2_COORDINATES);
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();
    dependenciesMap.put(mvnPurlId, Collections.singletonList(depPurlId));
    detailsMap.put(depPurlId, new ComponentDetails());
    ComponentDependenciesDTO dependenciesDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    mockHdsGetComponentDependencies(dependenciesDto);
    mockLicenseFeature(true);

    // insert multiple source control events
    insertSourceControlEvent("InvalidStatus");
    SourceControlEvent errorEvent = insertSourceControlEvent(SourceControlEvent.EVENT_STATUS_ERROR);
    SourceControlEvent completeEvent = insertSourceControlEvent(SourceControlEvent.EVENT_STATUS_COMPLETE);
    SourceControlEvent inProgressEvent = insertSourceControlEvent(SourceControlEvent.EVENT_STATUS_IN_PROGRESS);
    SourceControlEvent newEvent = insertSourceControlEvent(SourceControlEvent.EVENT_STATUS_NEW);

    // complete must be the priority
    ComponentVersionInfoDTO dto =
        testGetComponentVersionInfo(application, application.getPublicId(), SourceStageType.ID);

    PullRequestDTO pullRequestDTO = (PullRequestDTO) dto.automatedRemediationStatus;
    assertThat(pullRequestDTO).isNotNull();
    assertThat(pullRequestDTO.status).isEqualTo(AutomatedRemediationStatus.PULL_REQUEST);
    assertThat(pullRequestDTO.url).isEqualTo("https://git.com/pull/1");

    // in progress must be the priority
    sourceControlEventDAO.delete(completeEvent);
    dto = testGetComponentVersionInfo(application, application.getPublicId(), SourceStageType.ID);

    PullRequestCreationPendingDTO pendingDTO = (PullRequestCreationPendingDTO) dto.automatedRemediationStatus;
    assertThat(pendingDTO).isNotNull();
    assertThat(pendingDTO.status).isEqualTo(AutomatedRemediationStatus.PULL_REQUEST_CREATION_PENDING);
    assertThat(pendingDTO.id).isEqualTo(inProgressEvent.getId());

    // new must be the priority
    sourceControlEventDAO.delete(inProgressEvent);
    dto = testGetComponentVersionInfo(application, application.getPublicId(), SourceStageType.ID);

    PullRequestCreationPendingDTO newDTO = (PullRequestCreationPendingDTO) dto.automatedRemediationStatus;
    assertThat(newDTO).isNotNull();
    assertThat(newDTO.status).isEqualTo(AutomatedRemediationStatus.PULL_REQUEST_CREATION_PENDING);
    assertThat(newDTO.id).isEqualTo(newEvent.getId());

    // error must be the priority
    sourceControlEventDAO.delete(newEvent);
    dto = testGetComponentVersionInfo(application, application.getPublicId(), SourceStageType.ID);

    PullRequestCreationFailedDTO errorDTO = (PullRequestCreationFailedDTO) dto.automatedRemediationStatus;
    assertThat(errorDTO).isNotNull();
    assertThat(errorDTO.status).isEqualTo(AutomatedRemediationStatus.PULL_REQUEST_CREATION_FAILED);
    assertThat(errorDTO.reason).isEqualTo("error reason");

    // invalid must return creation failed
    sourceControlEventDAO.delete(errorEvent);
    dto = testGetComponentVersionInfo(application, application.getPublicId(), SourceStageType.ID);

    PullRequestCreationFailedDTO invalidDTO = (PullRequestCreationFailedDTO) dto.automatedRemediationStatus;
    assertThat(invalidDTO).isNotNull();
    assertThat(invalidDTO.status).isEqualTo(AutomatedRemediationStatus.PULL_REQUEST_CREATION_FAILED);
  }

  @Test
  public void testGetComponentVersionInfo_WithAdvancedRecommendationAndPullRequestStatus_Empty() {
    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "5"));
    Policy policy1 = new Policy("security-low", "Security-Low");
    policy1.setThreatLevel(5);
    policy1.addConstraint(constraint1);
    policy1.setAction(ReleaseStageType.ID, WarnActionType.ID);
    policy1.setOwnerId(application.getId());
    tempEntity.newPolicy(policy1);

    // mock dependencies for advanced recommendation strategies
    PackageUrlIdentifier mvnPurlId = PackageUrlIdentifier.fromComponentIdentifier(MAVEN_A1_COORDINATES);
    PackageUrlIdentifier depPurlId = PackageUrlIdentifier.fromComponentIdentifier(MAVEN_A2_COORDINATES);
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();
    dependenciesMap.put(mvnPurlId, Collections.singletonList(depPurlId));
    detailsMap.put(depPurlId, new ComponentDetails());
    ComponentDependenciesDTO dependenciesDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    mockHdsGetComponentDependencies(dependenciesDto);
    mockLicenseFeature(true);

    // case: there is no remediation event -> proceeds to check if manual pull request is possible
    ComponentVersionInfoDTO dto =
        testGetComponentVersionInfo(application, application.getPublicId(), SourceStageType.ID);
    ManualPullRequestNotPossibleDTO prNotPossible = (ManualPullRequestNotPossibleDTO) dto.automatedRemediationStatus;
    assertThat(prNotPossible.status).isEqualTo(AutomatedRemediationStatus.MANUAL_PULL_REQUEST_NOT_POSSIBLE);
    assertThat(prNotPossible.reason).isEqualTo(ManualPullRequestImpossibilityReason.INSUFFICIENT_PERMISSIONS);

    // case: branch name cannot be generated -> proceeds to check if manual pull request is possible
    insertSourceControlEvent(SourceControlEvent.EVENT_STATUS_COMPLETE);
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1",
        null, "", "jar");

    ComponentDetails hdsComponentDetails = newNamedComponentDetails(componentIdentifier);
    hdsComponentDetails.setCatalogDate(DateTime.now().getMillis());
    hdsComponentDetails.setSecurityVulnerabilities(asList(
        new SecurityVulnerability("cve-8", "cve", 8.1f),
        new SecurityVulnerability("cve-4", "cve", 4f)));
    ComponentDetailsList hdsComponentDetailsList = new ComponentDetailsList();
    hdsComponentDetailsList.setList(Collections.singletonList(hdsComponentDetails));
    mockHdsGetComponentDetailsList(hdsComponentDetailsList, MAVEN_A1_COORDINATES);

    dto = componentInfoService.getComponentVersionInfo(application.getType(), application.getId(),
        MAVEN_A1_COORDINATES, SourceStageType.ID, null, null, null);
    prNotPossible = (ManualPullRequestNotPossibleDTO) dto.automatedRemediationStatus;
    assertThat(prNotPossible.status).isEqualTo(AutomatedRemediationStatus.MANUAL_PULL_REQUEST_NOT_POSSIBLE);
    assertThat(prNotPossible.reason).isEqualTo(ManualPullRequestImpossibilityReason.INSUFFICIENT_PERMISSIONS);
  }

  @Test
  public void testGetComponentVersionInfo_WithAdvancedRecommendationAndPullRequestStatus_DependencyType() {
    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "5"));
    Policy policy1 = new Policy("security-low", "Security-Low");
    policy1.setThreatLevel(5);
    policy1.addConstraint(constraint1);
    policy1.setAction(ReleaseStageType.ID, WarnActionType.ID);
    policy1.setOwnerId(application.getId());
    tempEntity.newPolicy(policy1);

    // mock dependencies for advanced recommendation strategies
    PackageUrlIdentifier mvnPurlId = PackageUrlIdentifier.fromComponentIdentifier(MAVEN_A1_COORDINATES);
    PackageUrlIdentifier depPurlId = PackageUrlIdentifier.fromComponentIdentifier(MAVEN_A2_COORDINATES);
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();
    dependenciesMap.put(mvnPurlId, Collections.singletonList(depPurlId));
    detailsMap.put(depPurlId, new ComponentDetails());
    ComponentDependenciesDTO dependenciesDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    mockHdsGetComponentDependencies(dependenciesDto);
    mockLicenseFeature(true);

    // case: remediation event exists but dependency is null
    // -> proceeds to check if manual pull request is possible
    insertSourceControlEvent(SourceControlEvent.EVENT_STATUS_ERROR);
    ComponentVersionInfoDTO dto =
        testGetComponentVersionInfo(application, application.getPublicId(), SourceStageType.ID, null);
    ManualPullRequestNotPossibleDTO prNotPossible = (ManualPullRequestNotPossibleDTO) dto.automatedRemediationStatus;
    assertThat(prNotPossible.status).isEqualTo(AutomatedRemediationStatus.MANUAL_PULL_REQUEST_NOT_POSSIBLE);
    assertThat(prNotPossible.reason).isEqualTo(ManualPullRequestImpossibilityReason.INSUFFICIENT_PERMISSIONS);

    // case: remediation event exists but dependency is transitive
    // -> proceeds to check if manual pull request is possible
    insertSourceControlEvent(SourceControlEvent.EVENT_STATUS_ERROR);
    dto = testGetComponentVersionInfo(application, application.getPublicId(), SourceStageType.ID,
        DependencyType.TRANSITIVE);
    prNotPossible = (ManualPullRequestNotPossibleDTO) dto.automatedRemediationStatus;
    assertThat(prNotPossible.status).isEqualTo(AutomatedRemediationStatus.MANUAL_PULL_REQUEST_NOT_POSSIBLE);
    assertThat(prNotPossible.reason).isEqualTo(ManualPullRequestImpossibilityReason.INSUFFICIENT_PERMISSIONS);

    // case: remediation event exists and dependency is direct
    // -> returns the remediation event status
    insertSourceControlEvent(SourceControlEvent.EVENT_STATUS_ERROR);
    dto =
        testGetComponentVersionInfo(application, application.getPublicId(), SourceStageType.ID, DependencyType.DIRECT);
    PullRequestCreationFailedDTO invalidDTO = (PullRequestCreationFailedDTO) dto.automatedRemediationStatus;
    assertThat(invalidDTO).isNotNull();
    assertThat(invalidDTO.status).isEqualTo(AutomatedRemediationStatus.PULL_REQUEST_CREATION_FAILED);
  }

  @Test
  public void testGetComponentVersionInfo_WithAdvancedRecommendationAndPullRequestStatus_MergedPrShowsCreatePr() {
    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "5"));
    Policy policy1 = new Policy("security-low", "Security-Low");
    policy1.setThreatLevel(5);
    policy1.addConstraint(constraint1);
    policy1.setAction(ReleaseStageType.ID, WarnActionType.ID);
    policy1.setOwnerId(application.getId());
    tempEntity.newPolicy(policy1);

    PackageUrlIdentifier mvnPurlId = PackageUrlIdentifier.fromComponentIdentifier(MAVEN_A1_COORDINATES);
    PackageUrlIdentifier depPurlId = PackageUrlIdentifier.fromComponentIdentifier(MAVEN_A2_COORDINATES);
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();
    dependenciesMap.put(mvnPurlId, Collections.singletonList(depPurlId));
    detailsMap.put(depPurlId, new ComponentDetails());
    ComponentDependenciesDTO dependenciesDto = new ComponentDependenciesDTO(dependenciesMap, detailsMap);
    mockHdsGetComponentDependencies(dependenciesDto);
    mockLicenseFeature(true);

    insertSourceControlEvent(SourceControlEvent.EVENT_STATUS_COMPLETE);

    String repoUrl = "https://github.com/org/repo";
    tempEntity.newSourceControl(application.getId(), repoUrl, null, null, null, SourceControlProvider.GITHUB,
        null, null, "main", null, null, null, null, null, null, null, null);

    Date now = new Date();
    String normalizedUrl = SourceControl.normalizeRepositoryUrl(repoUrl);
    tempEntity.newSourceControlPullRequest(normalizedUrl, 1, "head", "base",
        "branch", "main", now, now, now, PullRequestState.MERGED);

    ComponentVersionInfoDTO dto =
        testGetComponentVersionInfo(application, application.getPublicId(), SourceStageType.ID);

    ManualPullRequestNotPossibleDTO prNotPossible = (ManualPullRequestNotPossibleDTO) dto.automatedRemediationStatus;
    assertThat(prNotPossible).isNotNull();
    assertThat(prNotPossible.status).isEqualTo(AutomatedRemediationStatus.MANUAL_PULL_REQUEST_NOT_POSSIBLE);
  }

  private SourceControlEvent insertSourceControlEvent(final String eventStatus) {
    SourceControlEvent sourceControlEvent =
        new SourceControlEvent().forRemediationPullRequest()
            .setBranchName(application.getId().substring(0, 6) + "/g1/a1/v1-to-v1");
    sourceControlEvent.setApplicationId(application.getId());
    sourceControlEvent.setEventStatus(eventStatus);
    if (SourceControlEvent.EVENT_STATUS_COMPLETE.equals(eventStatus)) {
      sourceControlEvent.setEventStatusDetails("https://git.com/pull/1");
      sourceControlEvent.setPullRequestNumber(1);
    }
    else if (SourceControlEvent.EVENT_STATUS_ERROR.equals(eventStatus)) {
      sourceControlEvent.setEventStatusDetails("error reason");
    }
    sourceControlEventDAO.insert(sourceControlEvent);
    return sourceControlEvent;
  }

  @Test
  public void testGetComponentDetails_TruncatesFullSha1WhenLoadingHashBasedData() throws Exception {
    String hash = "01234567890123456789";
    String fullHash = hash + hash;

    Label label = tempEntity.newLabel(application.getId(), "red");
    tempEntity.newComponentLabel(application.getId(), label.getId(), hash);

    // policy that triggers if the component label was loaded properly by hash
    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(LabelConditionType.ID, "is", label.getId()));
    Policy policy1 = new Policy("PolicyId1", "Policy Name 1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setAction(BuildStageType.ID, FailActionType.ID);
    policy1.setOwnerId(application.getId());
    tempEntity.newPolicy(policy1);

    // policy that doesn't trigger if the corresponding waiver was loaded properly by hash
    Constraint constraint2 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint2.addCondition(new Condition(LabelConditionType.ID, "is", label.getId()));
    Policy policy2 = new Policy("PolicyId2", "Policy Name 2");
    policy2.setThreatLevel(8);
    policy2.addConstraint(constraint1);
    policy2.setAction(BuildStageType.ID, FailActionType.ID);
    policy2.setOwnerId(application.getId());
    tempEntity.newPolicy(policy2);
    tempEntity.newWaiver(hash, policy2.getId(), application.getId());

    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    hdsComponentDetails.setHash(hash);
    mockHdsGetComponentDetails(hdsComponentDetails);

    NamedComponentDetails componentDetails = componentInfoService.getComponentDetails(application, MAVEN_A1_COORDINATES,
        MatchState.EXACT.getId(), fullHash, false /* proprietary */, httpRequestMock);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getHash()).isEqualTo(hash);
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(MAVEN_A1_COORDINATES);
    assertThat(componentDetails.getMatchState()).isEqualTo(MatchState.EXACT.getId());
    assertThat(componentDetails.getIdentificationSource()).isEqualTo(IdentificationSource.SONATYPE.getId());
    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    assertThat(policyAlerts).hasSize(1);
    assertThat(policyAlerts.get(0).getTrigger().getPolicyName()).isEqualTo(policy1.getName());
  }

  @Test
  public void testGetSecurityVulnerabilities_Repository() throws Exception {
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    String hash = "01234567890123456789";
    SecurityVulnerability vulnerability = new SecurityVulnerability("refId", "source", 5.0f, "summary");
    hdsComponentDetails.setSecurityVulnerabilities(Collections.singletonList(vulnerability));
    hdsComponentDetails.setHash(hash);
    mockHdsGetComponentDetails(hdsComponentDetails);

    tempEntity.newSecurityVulnerabilityOverride(repository.getId(), hash, "source", "refId",
        SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED, "abcd");

    ComponentSecurityVulnerabilities retrievedVulnerabilities = componentInfoService.getSecurityVulnerabilities(
        OwnerType.REPOSITORY, repository.getId(), hash, MAVEN_A1_COORDINATES, httpRequestMock, null, null);
    assertGetSecurityVulnerabilityResults(vulnerability, retrievedVulnerabilities);
  }

  @Test
  public void testGetSecurityVulnerabilities_Application() throws Exception {
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    String hash = "01234567890123456789";
    SecurityVulnerability vulnerability = new SecurityVulnerability("refId", "source", 5.0f, "summary");
    hdsComponentDetails.setSecurityVulnerabilities(Collections.singletonList(vulnerability));
    hdsComponentDetails.setHash(hash);
    mockHdsGetComponentDetails(hdsComponentDetails);

    tempEntity.newSecurityVulnerabilityOverride(application.getId(), hash, "source", "refId",
        SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED, "abcd");

    ComponentSecurityVulnerabilities retrievedVulnerabilities = componentInfoService.getSecurityVulnerabilities(
        OwnerType.APPLICATION, application.getPublicId(), hash, MAVEN_A1_COORDINATES, httpRequestMock, null, null);

    assertGetSecurityVulnerabilityResults(vulnerability, retrievedVulnerabilities);
  }

  @Test
  public void testGetSecurityVulnerabilities_Application_ThirdParty() throws Exception {
    String scanId = "scanId";
    String hash = "01234567890123456789";
    final String identificationSource = IdentificationSource.CLAIR.getId();
    NamedComponentDetails tpsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    tpsComponentDetails.setMatchState(MatchState.EXACT.getId());
    tpsComponentDetails.setIdentificationSource(identificationSource);
    SecurityVulnerability vulnerability = new SecurityVulnerability("refId", "source", 5.0f, "summary");
    tpsComponentDetails.setHash(hash);
    tpsComponentDetails.setSecurityVulnerabilities(Collections.singletonList(vulnerability));

    when(thirdPartyComponentDAO.getComponentDetailsByIdentifier(MAVEN_A1_COORDINATES, application.getId(), scanId))
        .thenReturn(tpsComponentDetails);

    tempEntity.newSecurityVulnerabilityOverride(application.getId(), hash, "source", "refId",
        SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED, "abcd");

    ComponentSecurityVulnerabilities retrievedVulnerabilities = componentInfoService.getSecurityVulnerabilities(
        OwnerType.APPLICATION, application.getPublicId(), hash, MAVEN_A1_COORDINATES, httpRequestMock,
        IdentificationSource.CLAIR.getId(), scanId);

    assertGetSecurityVulnerabilityResults(vulnerability, retrievedVulnerabilities);
  }

  @Test
  public void testGetSecurityVulnerabilities_Application_ThirdParty_UnsupportedFormat() throws Exception {
    String scanId = "scanId";
    String hash = "01234567890123456789";
    final String identificationSource = IdentificationSource.SBOM.getId();
    NamedComponentDetails tpsComponentDetails = newNamedComponentDetails(GENERIC_COORDINATES);
    tpsComponentDetails.setMatchState(MatchState.EXACT.getId());
    tpsComponentDetails.setIdentificationSource(identificationSource);
    SecurityVulnerability vulnerability = new SecurityVulnerability("refId", "source", 5.0f, "summary");
    tpsComponentDetails.setHash(hash);
    tpsComponentDetails.setSecurityVulnerabilities(Collections.singletonList(vulnerability));

    when(reportDataReader.getComponentDetailsByIdentifier(GENERIC_COORDINATES, application.getId(), scanId))
        .thenReturn(tpsComponentDetails);

    tempEntity.newSecurityVulnerabilityOverride(application.getId(), hash, "source", "refId",
        SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED, "abcd");

    ComponentSecurityVulnerabilities retrievedVulnerabilities = componentInfoService.getSecurityVulnerabilities(
        OwnerType.APPLICATION, application.getPublicId(), hash, GENERIC_COORDINATES, httpRequestMock,
        IdentificationSource.SBOM.getId(), scanId);

    assertGetSecurityVulnerabilityResults(vulnerability, retrievedVulnerabilities);
  }

  @Test
  public void testGetLicenses_Application_ThirdParty() throws Exception {
    String scanId = "scanId";
    String hash = "01234567890123456789";
    final String identificationSource = IdentificationSource.CLAIR.getId();
    NamedComponentDetails tpsComponentDetails = newNamedComponentDetails(MAVEN_A1_COORDINATES);
    tpsComponentDetails.setMatchState(MatchState.EXACT.getId());
    tpsComponentDetails.setIdentificationSource(identificationSource);
    License license = new License("Apache-2.0", "Apache License 2.0");
    tpsComponentDetails.setHash(hash);
    tpsComponentDetails.setDeclaredLicenses(Collections.singleton(license));

    when(thirdPartyComponentDAO.getComponentDetailsByIdentifier(MAVEN_A1_COORDINATES, application.getId(), scanId))
        .thenReturn(tpsComponentDetails);

    tempEntity.newSecurityVulnerabilityOverride(application.getId(), hash, "source", "refId",
        SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED, "abcd");

    ComponentLicenses retrievedLicenses = componentInfoService.getLicenses(OwnerType.APPLICATION,
        application.getPublicId(), MAVEN_A1_COORDINATES, httpRequestMock, IdentificationSource.CLAIR.getId(), scanId);

    assertGetLicensesResults(license, retrievedLicenses);
  }

  @Test
  public void testGetComponentDetailsList_KnownFormat_ThirdParty_NoHdsResults() {
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    String scanId = "scanId";
    ComponentDetailsList componentDetailsList = new ComponentDetailsList();
    componentDetailsList.setList(new ArrayList<>());
    mockHdsGetComponentDetailsList(componentDetailsList, componentIdentifier);
    ComponentDetails componentDetails = new ComponentDetails();
    when(thirdPartyComponentDAO.resolveComponentDetails(app.getId(), componentIdentifier, scanId))
        .thenReturn(componentDetails);

    ComponentDetailsList result =
        componentInfoService.getComponentDetailsList(componentIdentifier, app, "third-party", scanId,
            DependencyType.DIRECT, true).getLeft();

    assertThat(result.getList()).containsExactly(componentDetails);
  }

  @Test
  public void testGetComponentDetailsList_ExternalRepo() {
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    String scanId = "scanId";

    mockHdsGetComponentDetailsList(new ComponentDetailsList(), componentIdentifier);

    NamedComponentDetails details = new NamedComponentDetails();
    details.setComponentIdentifier(componentIdentifier);
    details.setMatchState(MatchState.EXACT.getId());
    details.setIdentificationSource(IdentificationSource.EXTERNAL_REPO.getId());
    details.setHash("b6341755d9028ef36c51");
    Set<License> license = Collections.singleton(new License("UNSPECIFIED", "Not Provided"));
    details.setDeclaredLicenses(license);
    details.setObservedLicenses(license);
    details.setSecurityVulnerabilities(Collections.emptyList());
    details.setPackageUrl("pkg:maven/g/a@v?classifier=c&type=e");

    ComponentDetailsList result = componentInfoService.getComponentDetailsList(componentIdentifier, app,
        IdentificationSource.EXTERNAL_REPO.getId(), scanId, DependencyType.DIRECT, true).getLeft();

    assertThat(result.getList().get(0)).usingRecursiveComparison().isEqualTo(details);
  }

  @Test
  public void testGetComponentDetailsList_CpeComponent() {
    Application app = tempEntity.newApplicationWithParent();
    String scanId = "scanId";
    String identificationSource = IdentificationSource.SBOM.getId();

    NamedComponentDetails componentDetails = newNamedComponentDetails(GENERIC_COORDINATES);
    componentDetails.setMatchState(MatchState.EXACT.getId());
    componentDetails.setIdentificationSource(identificationSource);

    when(reportDataReader.getComponentDetailsByIdentifier(GENERIC_COORDINATES, app.getId(), scanId))
        .thenReturn(componentDetails);

    ComponentDetailsList result = componentInfoService.getComponentDetailsList(GENERIC_COORDINATES, app,
        IdentificationSource.SBOM.getId(), scanId, DependencyType.DIRECT, true).getLeft();

    assertThat(result.getList().get(0)).usingRecursiveComparison().isEqualTo(componentDetails);
  }

  @Test
  public void testGetComponentDetailsList_ContainerFromFirewallForDocker() {
    Repository repository =
        tempEntity.newRepository(tempEntity.newRepositoryManager(), "docker-repo", RepositoryType.proxy, "docker");

    Organization organization = tempEntity.newOrganization();
    organization.setRelatedRepositoryId(repository.getId());
    organizationDAO.update(organization);

    Application application = tempEntity.newApplicationWithParent(organization);

    String scanId = "scanId";
    String identificationSource = IdentificationSource.SONATYPE_CONTAINER.getId();

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createContainerCoordinates("ns", "name", "v");

    NamedComponentDetails componentDetails = newNamedComponentDetails(componentIdentifier);
    componentDetails.setMatchState(MatchState.EXACT.getId());
    componentDetails.setIdentificationSource(identificationSource);

    when(reportDataReader.getComponentDetailsByIdentifier(componentIdentifier, application.getId(), scanId))
        .thenReturn(componentDetails);

    ComponentDetailsList result = componentInfoService.getComponentDetailsList(componentIdentifier, application,
        IdentificationSource.SONATYPE_CONTAINER.getId(), scanId, DependencyType.DIRECT, true).getLeft();

    assertThat(result.getList().get(0)).usingRecursiveComparison().isEqualTo(componentDetails);
  }

  @Test
  public void testGetComponentDetailsList_ContainerFromPackageManifest() {
    Application app = tempEntity.newApplicationWithParent();
    String scanId = "scanId";
    String identificationSource = IdentificationSource.PACKAGE_MANIFEST.getId();

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createContainerCoordinates("alpine:3.24.1",
        "apk-tools", "3.0.6-r0");

    NamedComponentDetails componentDetails = newNamedComponentDetails(componentIdentifier);
    componentDetails.setMatchState(MatchState.EXACT.getId());
    componentDetails.setIdentificationSource(identificationSource);

    when(reportDataReader.getComponentDetailsByIdentifier(componentIdentifier, app.getId(), scanId))
        .thenReturn(componentDetails);

    ComponentDetailsList result = componentInfoService.getComponentDetailsList(componentIdentifier, app,
        identificationSource, scanId, DependencyType.DIRECT, true).getLeft();

    assertThat(result.getList().get(0)).usingRecursiveComparison().isEqualTo(componentDetails);
  }

  @Test
  public void testGetComponentDetails_genericComponent_sbomSource() throws IOException {
    String scanId = "scanId";
    String identificationSource = IdentificationSource.SBOM.getId();
    NamedComponentDetails componentDetails = newNamedComponentDetails(GENERIC_COORDINATES);
    componentDetails.setMatchState(MatchState.EXACT.getId());
    componentDetails.setIdentificationSource(identificationSource);

    when(reportDataReader.getComponentDetailsByIdentifier(GENERIC_COORDINATES, application.getId(), scanId))
        .thenReturn(componentDetails);

    ComponentDetails result = componentInfoService.getComponentDetails(
        application, componentDetails.getComponentIdentifier(), componentDetails.getMatchState(), null, false,
        httpRequestMock, IdentificationSource.SBOM.getId(), scanId, null);

    assertThat(result).isNotNull();
    assertGenericComponentDetails(componentDetails, GENERIC_COORDINATES);
  }

  @Test
  public void testGetComponentDetails_firewallForContainers() throws IOException {
    String scanId = "scanId";
    String identificationSource = IdentificationSource.SONATYPE_CONTAINER.getId();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createContainerCoordinates("ns", "n", "v");
    NamedComponentDetails componentDetails = newNamedComponentDetails(componentIdentifier);
    componentDetails.setMatchState(MatchState.EXACT.getId());
    componentDetails.setIdentificationSource(identificationSource);

    Repository repository =
        tempEntity.newRepository(tempEntity.newRepositoryManager(), "docker-repo", RepositoryType.proxy, "docker");

    Organization organization = tempEntity.newOrganization();
    organization.setRelatedRepositoryId(repository.getId());
    organizationDAO.update(organization);

    Application application = tempEntity.newApplicationWithParent(organization);

    when(reportDataReader.getComponentDetailsByIdentifier(componentIdentifier, application.getId(), scanId))
        .thenReturn(componentDetails);

    ComponentDetails result = componentInfoService.getComponentDetails(
        application, componentDetails.getComponentIdentifier(), componentDetails.getMatchState(), null, false,
        httpRequestMock, IdentificationSource.SONATYPE_CONTAINER.getId(), scanId, null);

    assertThat(result).isNotNull();
    assertGenericComponentDetails(componentDetails, componentIdentifier);
  }

  @Test
  public void testgetMultiLicensesNoAuth_genericComponent_sbomSource() throws Exception {
    String scanId = "scanId";
    String identificationSource = IdentificationSource.SBOM.getId();
    NamedComponentDetails componentDetails = newNamedComponentDetails(GENERIC_COORDINATES);
    componentDetails.setMatchState(MatchState.EXACT.getId());
    componentDetails.setIdentificationSource(identificationSource);

    when(reportDataReader.getComponentDetailsByIdentifier(GENERIC_COORDINATES, application.getId(), scanId))
        .thenReturn(componentDetails);

    ComponentMultiLicenses licenses =
        componentInfoService.getMultiLicensesNoAuth(OwnerType.APPLICATION, application.getPublicId(),
            GENERIC_COORDINATES, httpRequestMock, identificationSource, scanId);

    assertMultiLicenses(licenses.declaredLicenses, tuple(UNSPECIFIED_ID, "Not Provided", 5));
    assertMultiLicenses(licenses.observedLicenses, tuple(UNSPECIFIED_ID, "Not Provided", 5));
    assertMultiLicenses(licenses.effectiveLicenses, tuple(UNSPECIFIED_ID, "Not Provided", 5));
    assertThat(licenses.hiddenObservedLicenses).isFalse();
  }

  private void assertGetLicensesResults(final License license, final ComponentLicenses retrievedLicenses) {
    assertThat(retrievedLicenses.declaredlicenses).hasSize(1);
    assertThat(retrievedLicenses.effectiveLicenses).hasSize(1);
    License retrievedLicense = retrievedLicenses.declaredlicenses.get(0).license;
    assertThat(retrievedLicense.getLicenseId()).isEqualTo(license.getLicenseId());
    assertThat(retrievedLicense.getLicenseName()).isEqualTo(license.getLicenseId());
  }

  private void assertGetSecurityVulnerabilityResults(
      final SecurityVulnerability vulnerability,
      final ComponentSecurityVulnerabilities retrievedVulnerabilities)
  {
    assertThat(retrievedVulnerabilities.securityVulnerabilities).hasSize(1);
    SecurityVulnerability retrievedVulnerability = retrievedVulnerabilities.securityVulnerabilities.get(0);
    assertThat(retrievedVulnerability.getRefId()).isEqualTo(vulnerability.getRefId());
    assertThat(retrievedVulnerability.getSource()).isEqualTo(vulnerability.getSource());
    assertThat(retrievedVulnerability.getSeverity()).isEqualTo(vulnerability.getSeverity());
    assertThat(retrievedVulnerability.getSummary()).isEqualTo(vulnerability.getSummary());
    assertThat(retrievedVulnerability.getStatus())
        .isEqualTo(SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED.getName());
  }

  private void assertCategories(ComponentDetails componentDetails) {
    assertThat(componentDetails.getComponentCategories())
        .extracting(ComponentCategory::getComponentCategoryId, ComponentCategory::getPath)
        .containsExactly(Tuple.tuple(113, "Other"));
  }

  private void assertGenericComponentDetails(
      ComponentDetails componentDetails,
      ComponentIdentifier componentIdentifier1)
  {
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(componentIdentifier1);

    assertThat(componentDetails.getDeclaredLicenses()).isNotEmpty();
    assertThat(componentDetails.getDeclaredLicenses()).hasSize(1);
    assertThat(componentDetails.getDeclaredLicenses().iterator().next().getLicenseName()).isEqualTo("Not Provided");
    assertThat(componentDetails.getDeclaredLicenses().iterator().next().getLicenseId()).isEqualTo(UNSPECIFIED_ID);
    assertThat(componentDetails.getObservedLicenses()).hasSize(1);
    assertThat(componentDetails.getObservedLicenses().iterator().next().getLicenseName()).isEqualTo("Not Provided");
    assertThat(componentDetails.getObservedLicenses().iterator().next().getLicenseId()).isEqualTo(UNSPECIFIED_ID);
    assertThat(componentDetails.getEffectiveLicenseStatus()).isNull();
  }

  @Test
  public void testGetComponentDetailsFromHDS_NoDetailsReturned() throws Exception {
    when(hdsClientMock.relay(eq(httpRequestMock), eq(NamedComponentDetails.class),
        eq("rest/" + TOOL_NAME + "/componentDetails"),
        any(Map.class))).thenReturn(new RelayResponse<>(null));

    final ComponentIdentifier terraformCoords =
        ComponentIdentifier.createTerraformCoordinates("plan", "name", "version");

    final NamedComponentDetails componentDetails =
        componentInfoService.getComponentDetailsFromHDS(null, "hash", terraformCoords, httpRequestMock, "");

    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getMatchState()).isEqualToIgnoringCase(MatchState.UNKNOWN.getName());
    assertThat(componentDetails.getDeclaredLicenses()).isEmpty();
    assertThat(componentDetails.getObservedLicenses()).isEmpty();
    assertThat(componentDetails.getEffectiveLicenses()).isEmpty();
  }

  @Test
  public void testGetComponentDetailsList_PackageUrl_WithIdentifier() {
    // Given: Component with identifier (format-agnostic, using Maven as example)
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("org.springframework",
        "spring-core", "4.3.0.RELEASE");
    ComponentDetails hdsComponentDetails = newNamedComponentDetails(identifier);

    ComponentDetailsList hdsComponentDetailsList = new ComponentDetailsList();
    hdsComponentDetailsList.setList(Collections.singletonList(hdsComponentDetails));
    mockHdsGetComponentDetailsList(hdsComponentDetailsList, identifier);

    // When: Getting component details list
    ComponentDetailsList result = componentInfoService.getComponentDetailsList(
        identifier, null, null, null, null, true).getLeft();

    // Then: Package URL should be set (format doesn't matter for this service)
    assertThat(result.getList()).hasSize(1);
    assertThat(result.getList().get(0).getPackageUrl()).isNotNull();
    assertThat(result.getList().get(0).getPackageUrl()).startsWith("pkg:maven/");
  }

  @Test
  public void testGetComponentDetailsList_PackageUrl_NullIdentifier() {
    // Given: Component with null identifier (hash-only component)
    ComponentDetails hdsComponentDetails = new ComponentDetails();
    hdsComponentDetails.setHash("test-hash");
    hdsComponentDetails.setComponentIdentifier(null);

    ComponentDetailsList hdsComponentDetailsList = new ComponentDetailsList();
    hdsComponentDetailsList.setList(Collections.singletonList(hdsComponentDetails));

    ComponentIdentifier queryIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "1.0");
    mockHdsGetComponentDetailsList(hdsComponentDetailsList, queryIdentifier);

    // When: Getting component details list where a component has null identifier
    ComponentDetailsList result = componentInfoService.getComponentDetailsList(
        queryIdentifier, null, null, null, null, true).getLeft();

    // Then: Package URL should not be set for component with null identifier
    assertThat(result.getList()).hasSize(1);
    assertThat(result.getList().get(0).getPackageUrl()).isNull();
  }

  private void mockHdsGetVersionScoringData() {
    lenient().when(hdsClientMock.post(eq(VersionScoringDTO[].class), eq(HDS_BULK_SCORE_VERSIONING_PATH), anyList(),
        eq(Map.of("stableVersionsOnly", "true"))))
        .thenReturn(new VersionScoringDTO[]{});
  }
}
