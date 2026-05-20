/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import com.sonatype.insight.brain.common.test.SlowTest;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.sourcecontrol.ApiSourceControlRepositoryUserDTO;
import com.sonatype.insight.brain.api.experimental.dto.ApiOwnerUserRateLimitsDTO;
import com.sonatype.insight.brain.api.experimental.dto.ApiRateLimitDTO;
import com.sonatype.insight.brain.api.experimental.dto.ApiUserRateLimitsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiOwnerDTO;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiPullRequestResults;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiSourceControlDTO;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlService.METHOD;
import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticSourceControlConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppInstallationStateDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.git.EnhancedPullRequestResult;
import com.sonatype.insight.brain.git.GitApiFactory;
import com.sonatype.insight.brain.git.GitClientFactory;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.nexus.git.utils.api.GitApi;
import com.sonatype.nexus.git.utils.api.GitException;
import com.sonatype.nexus.iq.manager.PullRequestResult;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GeneralSCMApiClient;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.github.dto.GithubRateLimitResponse;
import com.sonatype.nexus.scm.github.dto.GithubRateLimitsResponse;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControl.FAKE_SECRET_KEY;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.DISCOVERED_PULL_REQUEST_EVENT;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.EVENT_PRIORITY_HIGHER;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.EVENT_PRIORITY_NORMAL;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.REPOSITORY_URL_UPDATED_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Category(SlowTest.class)
public class ApiSourceControlServiceTest
    extends AbstractComponentTest
{
  private static final String VALID_URL = "https://example.com/organization/project";

  private static final String TOKEN = "token";

  private static final Integer TEST_GITHUB_APP_ID = 12345;

  private static final String TEST_GITHUB_APP_SLUG = "test-app";

  private static final String TEST_CLIENT_SECRET = "test-client-secret";

  private static final Long TEST_GITHUB_INSTALLATION_ID = 67890L;

  @Inject
  private ApiSourceControlService sourceControlService;

  @Inject
  private PasswordHandler passwordHandler;

  @Inject
  private TestProductLicense testProductLicense;

  @Inject
  private InsightWork insightWork;

  @Mock
  private TelemetrySender telemetrySenderMock;

  @Mock
  private GitClientFactory mockGitClientFactory;

  @Inject
  private AutomaticSourceControlConfigurationDAO automaticSourceControlConfigurationDAO;

  @Inject
  private OrganizationDAO organizationDAO;

  @Inject
  private SourceControlDAO sourceControlDAO;

  @Inject
  private SourceControlConfigurationDAO sourceControlConfigurationDAO;

  @Inject
  private SourceControlEventDAO sourceControlEventDAO;

  @Inject
  private ApiSourceControlAdapter apiSourceControlAdapter;

  @Inject
  private GitHubAppDAO gitHubAppDAO;

  @Inject
  private GitHubAppInstallationStateDAO installationStateDAO;

  @Inject
  private InsightProxy insightProxy;

  @Rule
  public WireMockRule githubMockServer = new WireMockRule(wireMockConfig().dynamicPort());

  private Application app;

  private Organization org;

  private SourceControl rootOrgSourcecontrol;

  @Mock
  private GitApiFactory gitApiFactory;

  @Override
  public void configure(final Binder binder) {
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
    binder.bind(GitApiFactory.class).toInstance(gitApiFactory);
    binder.bind(GitClientFactory.class).toInstance(mockGitClientFactory);
    super.configure(binder);
  }

  @Before
  public void setup() {
    org = tempEntity.newOrganization();
    rootOrgSourcecontrol = tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    app = tempEntity.newApplication(org.getId());
  }

  @Test
  public void testGetAll_unlicensed() {
    setUnlicensedForSourceControl();
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> sourceControlService.getAll());
  }

  @Test
  public void testGetAll_licensedByAutomation() throws Exception {
    setLicensedForSourceControlByAutomation();
    testGetAll();
  }

  @Test
  public void testGetAll_licensedByNotifications() throws Exception {
    setLicensedForSourceControlByNotifications();
    testGetAll();
  }

  private void testGetAll() throws Exception {
    final List<ApiSourceControlDTO> expected = Stream.of(
        rootOrgSourcecontrol,
        tempEntity.newSourceControl(app.getId(), VALID_URL, TOKEN, null),
        tempEntity.newSourceControl(org.getId(), null, TOKEN, null))
        .map(sourceControl -> apiSourceControlAdapter.convertToDTO(sourceControl))
        .collect(Collectors.toList());

    final List<ApiSourceControlDTO> retrieved = sourceControlService.getAll();

    for (final ApiSourceControlDTO it : expected) {
      it.token = it.token != null ? FAKE_SECRET_KEY : null;
    }

    assertThat(retrieved).hasSize(3);
    assertThat(JsonUtils.format(retrieved)).isEqualTo(JsonUtils.format(expected));
  }

  @Test
  public void testAddOrUpdateSourceControl_InvalidPublicId() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(
            () -> sourceControlService
                .addOrUpdateSourceControl("abcdefg", "https://e.com/org/proj", null));
  }

  @Test
  public void testAddOrUpdateSourceControl_MissingRepoUrl_QueryParam() {
    assertThatThrownBy(() -> sourceControlService
        .addOrUpdateSourceControl("hello", "", null))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Query parameter 'repositoryUrl' is required");
  }

  @Test
  public void testAddOrUpdateSourceControl_MissingPublicId_BodyProperty() {
    ApiSourceControlRepositoryUserDTO apiSourceControlRepoUserDTO = new ApiSourceControlRepositoryUserDTO();
    String publicId = "";

    assertThatThrownBy(() -> sourceControlService
        .addOrUpdateSourceControl(
            publicId,
            null,
            apiSourceControlRepoUserDTO))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Query parameter 'repositoryUrl' is required");
  }

  @Test
  public void testAddOrUpdateSourceControl_MissingRepoUrl_BodyProperty() {
    ApiSourceControlRepositoryUserDTO apiSourceControlRepoUserDTO = new ApiSourceControlRepositoryUserDTO();
    String publicId = "hello";

    assertThatThrownBy(() -> sourceControlService
        .addOrUpdateSourceControl(
            publicId,
            null,
            apiSourceControlRepoUserDTO))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Query parameter 'repositoryUrl' is required");
  }

  @Test
  public void testAddOrUpdateSourceControlFromAppEvaluation_InvalidRepoUrl() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> testAddOrUpdateSourceControlFromAppEvaluation_AutomaticSourceControl(true, null,
                "https://not valid", null));
  }

  @Test
  public void testAddOrUpdateSourceControlFromAppEvaluation_AutomaticSourceControlDisabled_Create() {
    testAddOrUpdateSourceControlFromAppEvaluation_AutomaticSourceControl(false, null,
        "https://github.com/org/b", "https://github.com/org/a");
  }

  private final GitApi mockGitApiInstance = mock(GitApi.class);

  @Test
  public void testAddOrUpdateSourceControlFromAppEvaluation_AutomaticSourceControlDisabled_Create_Ssh() throws Exception {
    String httpUrl = "https://github.com/a/b.git";
    String sshUrl = "git@github.com:a/b.git";

    HashMap<String, String> headCommitsForAllBranches = new HashMap<>();
    headCommitsForAllBranches.put("main", "data");

    when(mockGitApiInstance.getHeadCommitsForAllBranches(httpUrl)).thenReturn(headCommitsForAllBranches);
    when(gitApiFactory.createGitApi(any())).thenReturn(mockGitApiInstance);

    testAddOrUpdateSourceControlFromAppEvaluation_AutomaticSourceControl(true, null, sshUrl, httpUrl);

    // Verify ssh url also inserted in source control entity
    SourceControl sourceControl =
        sourceControlDAO.getAll().stream().filter(sc -> httpUrl.equals(sc.getRepositoryUrl())).findAny().get();
    assertThat(sourceControl.getRepositorySshUrl()).isEqualTo(sshUrl);
  }

  @Test
  public void testAddOrUpdateSourceControlFromAppEvaluation_AutomaticSourceControlDisabled_Create_Ssh_ExistsNot() throws Exception {
    String httpUrl = "https://github.com/a/b.git";
    String sshUrl = "git@github.com:a/b.git";

    when(gitApiFactory.createGitApi(any())).thenReturn(mockGitApiInstance);
    when(mockGitApiInstance.getHeadCommitsForAllBranches(httpUrl)).thenThrow(new GitException(""));

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> testAddOrUpdateSourceControlFromAppEvaluation_AutomaticSourceControl(true, null, sshUrl, httpUrl));
  }

  @Test
  public void testAddOrUpdateSourceControlFromAppEvaluation_AutomaticSourceControlDisabled_Update() {
    testAddOrUpdateSourceControlFromAppEvaluation_AutomaticSourceControl(false, "https://github.com/org/a",
        "https://github.com/org/b", "https://github.com/org/a");
  }

  @Test
  public void testAddOrUpdateSourceControlFromAppEvaluation_AutomaticSourceControlEnabled_Create() {
    testAddOrUpdateSourceControlFromAppEvaluation_AutomaticSourceControl(true, null,
        "https://github.com/org/b", "https://github.com/org/b");
  }

  @Test
  public void testAddOrUpdateSourceControlFromAppEvaluation_AutomaticSourceControlEnabled_Update() {
    testAddOrUpdateSourceControlFromAppEvaluation_AutomaticSourceControl(true, "https://github.com/org/a",
        "https://github.com/org/b", "https://github.com/org/a");
  }

  @Test
  public void testAddOrUpdateSourceControlFromAppEvaluation_ContextPath() {
    testAddOrUpdateSourceControlFromAppEvaluation_AutomaticSourceControl(true, null,
        "https://github.com/context/org/a", "https://github.com/context/org/a");
  }

  @Test
  public void testAddOrUpdateSourceControlFromAppEvaluation_CustomPort() {
    testAddOrUpdateSourceControlFromAppEvaluation_AutomaticSourceControl(true, null,
        "https://github.com:123/context/org/a", "https://github.com:123/context/org/a");
  }

  @Test
  public void testAddOrUpdateSourceControlFromAppEvaluation_DuplicateAccountNme() {
    testAddOrUpdateSourceControlFromAppEvaluation_AutomaticSourceControl(true, null,
        "https://org@github.com/org/a", "https://org@github.com/org/a");
  }

  @Test
  public void testAddOrUpdateSourceControl_HttpsWithGitExtension() {
    testAddOrUpdateSourceControlFromAppEvaluation_AutomaticSourceControl(true, null,
        "https://org@github.com/org/a.git", "https://org@github.com/org/a.git");
  }

  private void testAddOrUpdateSourceControlFromAppEvaluation_AutomaticSourceControl(
      boolean enabled,
      String initialUrl,
      String collectedUrl,
      String expectedUrl)
  {
    // add application record, if needed
    if (initialUrl != null) {
      tempEntity.newSourceControl(app.getId(), initialUrl, null, null);
    }

    // try automatic scm
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(enabled);

    ApiSourceControlDTO result =
        sourceControlService.addOrUpdateSourceControl(app.getPublicId(), collectedUrl, null);
    if (!enabled && initialUrl == null) {
      assertThat(result).isNull();
      verify(telemetrySenderMock, never()).send(any(TelemetryData.class));
    }
    else {
      assertThat(result.ownerId).isEqualTo(app.getId());
      assertThat(result.repositoryUrl).isEqualTo(expectedUrl);

      if (enabled && initialUrl == null) {
        assertTelemetry(METHOD.ADD_OR_UPDATE, app.getId(), expectedUrl, rootOrgSourcecontrol.getProvider().toString(),
            rootOrgSourcecontrol.getRemediationPullRequestsEnabled(), rootOrgSourcecontrol.getStatusChecksEnabled(),
            rootOrgSourcecontrol.getBaseBranch(), null, rootOrgSourcecontrol.getRepositoryUrl());
      }
      else {
        verify(telemetrySenderMock, never()).send(any(TelemetryData.class));
      }
    }
  }

  @Test
  public void testAddOrUpdateSourceControl_Add() {
    // when new source control with default branch and SSH URL is added
    String httpUrl = "https://localhost/context/org/a";
    String sshUrl = "git@localhost:org/a.git";
    String branch = "branch";
    ApiSourceControlDTO actual =
        sourceControlService.addOrUpdateSourceControl(app.getPublicId(), httpUrl, sshUrl, branch);

    // then params are returned
    assertThat(actual.baseBranch).isEqualTo(branch);
    assertThat(actual.repositoryUrl).isEqualTo(httpUrl);
    assertThat(actual.baseBranch).isEqualTo(branch);
    // sshUrl is not exposed as a property

    // and params are stored in database
    SourceControl persisted = sourceControlDAO.getByIdNotNull(actual.id);
    assertThat(persisted.getBaseBranch()).isEqualTo("branch");
    assertThat(persisted.getRepositoryUrl()).isEqualTo(httpUrl);
    assertThat(persisted.getRepositorySshUrl()).isEqualTo(sshUrl);

    assertTelemetry(METHOD.ADD_OR_UPDATE, app.getId(), httpUrl, rootOrgSourcecontrol.getProvider().toString(),
        rootOrgSourcecontrol.getRemediationPullRequestsEnabled(), rootOrgSourcecontrol.getStatusChecksEnabled(),
        branch, null, null);
  }

  @Test
  public void testAddSourceControlByOwner_TokenEncryption() throws Exception {
    final ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(org.getId())
            .setToken(TOKEN)
            .build());
    final ApiSourceControlDTO sourceControl = sourceControlService.addSourceControlByOwner(
        OwnerType.ORGANIZATION, org.getId(), validSourceControl);
    assertThat(sourceControl.token).isNotEqualTo(TOKEN);
    assertThat(sourceControl.token).isEqualTo(SourceControl.FAKE_SECRET_KEY);

    final SourceControl reloaded = sourceControlDAO.getByIdNotNull(sourceControl.id);
    final String decrypted = passwordHandler.decryptPassword(reloaded.getToken());

    assertThat(decrypted).isEqualTo(TOKEN);
    assertTelemetry(METHOD.ADD, org.getId(), reloaded.getRepositoryUrl(),
        null, reloaded.getRemediationPullRequestsEnabled(), reloaded.getStatusChecksEnabled(),
        reloaded.getBaseBranch(), null, null);
  }

  @Test
  public void testUpdateSourceControlByOwner_TokenEncryption() throws Exception {
    final ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(org.getId())
            .setToken(TOKEN)
            .build());

    final ApiSourceControlDTO sourceControl =
        sourceControlService.addSourceControlByOwner(OwnerType.ORGANIZATION,
            org.getId(), validSourceControl);
    sourceControl.token = "updatedToken";
    assertTelemetry(METHOD.ADD, org.getId(), sourceControl.repositoryUrl,
        sourceControl.provider, sourceControl.remediationPullRequestsEnabled,
        sourceControl.remediationPullRequestsEnabled,
        sourceControl.baseBranch, null, null);

    final ApiSourceControlDTO updatedScm =
        sourceControlService.updateSourceControlByOwner(OwnerType.ORGANIZATION,
            org.getId(), sourceControl);
    assertThat(updatedScm.token).isEqualTo(SourceControl.FAKE_SECRET_KEY);
    assertTelemetry(METHOD.UPDATE, org.getId(), sourceControl.repositoryUrl,
        sourceControl.provider, sourceControl.remediationPullRequestsEnabled,
        sourceControl.remediationPullRequestsEnabled,
        sourceControl.baseBranch, null, null);

    final SourceControl reloaded = sourceControlDAO.getByIdNotNull(sourceControl.id);
    final String decrypted = passwordHandler.decryptPassword(reloaded.getToken());
    assertThat(decrypted).isEqualTo("updatedToken");
  }

  @Test
  public void testGetSourceControlByOwnerDecrypted_ForOrganization() {
    final ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(org.getId())
            .setToken(TOKEN)
            .build());
    sourceControlService.addSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(),
        validSourceControl);
    assertThat(sourceControlService.getCompositeSourceControlByOwnerDecrypted(org.getId()).getToken()).isEqualTo(TOKEN);
  }

  @Test
  public void testAddSourceControlByOwner_CatersForBitbucketProvider() {
    SourceControl sourceControl = new SourceControl.Builder().setOwnerId(org.getId())
        .setToken(TOKEN)
        .setProvider(SourceControlProvider.BITBUCKET)
        .build();

    ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(sourceControl);
    sourceControlService.addSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(), validSourceControl);
    assertThat(sourceControlService.getCompositeSourceControlByOwnerDecrypted(org.getId()).getToken()).isEqualTo(TOKEN);
  }

  @Test
  public void testAddSourceControlByOwner_CatersForAzureProvider() {
    SourceControl sourceControl = new SourceControl.Builder().setOwnerId(org.getId())
        .setToken(TOKEN)
        .setProvider(SourceControlProvider.AZURE)
        .build();

    ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(sourceControl);
    sourceControlService.addSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(), validSourceControl);
    assertThat(sourceControlService.getCompositeSourceControlByOwnerDecrypted(org.getId()).getToken()).isEqualTo(TOKEN);
  }

  @Test
  public void testAddSourceControlByOwner_CatersForGitlabProvider() {
    SourceControl sourceControl = new SourceControl.Builder().setOwnerId(org.getId())
        .setToken(TOKEN)
        .setProvider(SourceControlProvider.BITBUCKET)
        .build();

    ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(sourceControl);
    sourceControlService.addSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(), validSourceControl);
    assertThat(sourceControlService.getCompositeSourceControlByOwnerDecrypted(org.getId()).getToken()).isEqualTo(TOKEN);
  }

  @Test
  public void testGetSourceControlByOwnerDecrypted_ForApplication() {
    final ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(app.getId())
            .setRepositoryUrl(VALID_URL)
            .setToken(TOKEN)
            .build());
    sourceControlService.addSourceControlByOwner(OwnerType.ORGANIZATION, app.getId(), validSourceControl);
    assertThat(sourceControlService.getCompositeSourceControlByOwnerDecrypted(app.getId()).getToken()).isEqualTo(TOKEN);
  }

  @Test
  public void testDeleteSourceControlByOwner_ForOrganization_licensedByNotifications() {
    setLicensedForSourceControlByNotifications();

    final ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(org.getId())
            .setToken(TOKEN)
            .build());

    final ApiSourceControlDTO sourceControl = sourceControlService
        .addSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(), validSourceControl);
    assertThat(sourceControlService.getAll()).hasSize(2);
    assertTelemetry(METHOD.ADD, org.getId(), sourceControl.repositoryUrl, sourceControl.provider,
        sourceControl.remediationPullRequestsEnabled, sourceControl.statusChecksEnabled,
        sourceControl.baseBranch, null, null);

    sourceControlService.deleteSourceControlByOwner(OwnerType.ORGANIZATION, org.getId());
    assertThat(sourceControlService.getAll()).hasSize(1);
    assertTelemetry(METHOD.DELETE, org.getId(), sourceControl.repositoryUrl, sourceControl.provider,
        sourceControl.remediationPullRequestsEnabled, sourceControl.statusChecksEnabled,
        sourceControl.baseBranch, null, null);
  }

  @Test
  public void testDeleteSourceControlByOwner_ForApplication_licensedByAutomation() {
    setLicensedForSourceControlByAutomation();
    final ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(app.getId())
            .setRepositoryUrl(VALID_URL)
            .setToken(TOKEN)
            .build());
    when(mockGitClientFactory.createApiClient(any())).thenReturn(mock(GitApiClient.class));

    final ApiSourceControlDTO sourceControl = sourceControlService
        .addSourceControlByOwner(OwnerType.APPLICATION, app.getId(), validSourceControl);
    assertThat(sourceControlService.getAll()).hasSize(2);
    assertTelemetry(METHOD.ADD, app.getId(), sourceControl.repositoryUrl, rootOrgSourcecontrol.getProvider().toString(),
        rootOrgSourcecontrol.getRemediationPullRequestsEnabled(), rootOrgSourcecontrol.getStatusChecksEnabled(),
        rootOrgSourcecontrol.getBaseBranch(), "public", sourceControl.repositoryUrl);

    File sourceControlDir = insightWork.getSourceControlDir(app.getId());
    sourceControlDir.mkdirs();
    assertThat(sourceControlDir).isDirectory();

    sourceControlService.deleteSourceControlByOwner(OwnerType.APPLICATION, app.getId());
    assertThat(sourceControlService.getAll()).hasSize(1);
    assertTelemetry(METHOD.DELETE, app.getId(), sourceControl.repositoryUrl,
        rootOrgSourcecontrol.getProvider().toString(), rootOrgSourcecontrol.getRemediationPullRequestsEnabled(),
        rootOrgSourcecontrol.getStatusChecksEnabled(), rootOrgSourcecontrol.getBaseBranch(),
        "public", sourceControl.repositoryUrl);

    assertThat(sourceControlDir).doesNotExist();
  }

  @Test
  public void testUpdateSourceControlByOwner_WithFakeToken() {
    final ApiSourceControlDTO sourceControl = sourceControlService
        .addSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(),
            apiSourceControlAdapter.convertToDTO(
                new SourceControl.Builder().setOwnerId(org.getId())
                    .setToken(TOKEN)
                    .build()));
    sourceControl.token = SourceControl.FAKE_SECRET_KEY;
    sourceControlService.updateSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(), sourceControl);
    assertThat(sourceControlService.getCompositeSourceControlByOwnerDecrypted(org.getId()).getToken()).isEqualTo(TOKEN);
  }

  @Test
  public void testUpdateSourceControlByOwner_WithEmptyToken() {
    final ApiSourceControlDTO sourceControl = sourceControlService
        .addSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(),
            apiSourceControlAdapter.convertToDTO(
                new SourceControl.Builder().setOwnerId(org.getId())
                    .setToken(TOKEN)
                    .build()));
    sourceControl.token = null;
    sourceControlService.updateSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(),
        sourceControl);
    assertThat(sourceControlService.getCompositeSourceControlByOwnerDecrypted(org.getId()).getToken()).isNull();
  }

  @Test
  public void testUpdateSourceControlByOwner_WrongOwnerId() {
    final ApiSourceControlDTO sourceControl = apiSourceControlAdapter.convertToDTO(
        tempEntity.newSourceControl(org.getId(), null, "token", null));
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> sourceControlService.updateSourceControlByOwner(OwnerType.ORGANIZATION,
            "foo", sourceControl))
        .withMessage("Cannot find SourceControl for organization with id: foo");
  }

  @Test
  public void testAddSourceControlByOwner_unlicensed() {
    setUnlicensedForSourceControl();
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> sourceControlService.addSourceControlByOwner(
            OwnerType.ORGANIZATION, "foo", apiSourceControlAdapter.convertToDTO(
                new SourceControl.Builder()
                    .setOwnerId(testName.getMethodName())
                    .setRepositoryUrl(VALID_URL)
                    .setToken("baz")
                    .build())));
  }

  @Test
  public void testAddSourceControlByOwner_licensedByAutomation() {
    when(mockGitClientFactory.createApiClient(any())).thenReturn(mock(GitApiClient.class));

    setLicensedForSourceControlByAutomation();
    ApiSourceControlDTO sourceControlDTO = sourceControlService.addSourceControlByOwner(
        OwnerType.APPLICATION,
        app.getId(),
        createSourceControlDtoForTesting());
    assertThat(sourceControlDTO).isNotNull();
    assertTelemetry(METHOD.ADD, app.getId(), sourceControlDTO.repositoryUrl,
        rootOrgSourcecontrol.getProvider().toString(), rootOrgSourcecontrol.getRemediationPullRequestsEnabled(),
        rootOrgSourcecontrol.getStatusChecksEnabled(), rootOrgSourcecontrol.getBaseBranch(),
        "public", sourceControlDTO.repositoryUrl);
  }

  @Test
  public void testAddSourceControlByOwner_licensedByNotifications() {
    // Given
    setLicensedForSourceControlByNotifications();
    when(mockGitClientFactory.createApiClient(any())).thenReturn(mock(GitApiClient.class));

    // When
    ApiSourceControlDTO sourceControlDTO = sourceControlService.addSourceControlByOwner(
        OwnerType.APPLICATION,
        app.getId(),
        createSourceControlDtoForTesting());

    // Then
    assertThat(sourceControlDTO).isNotNull();
    assertTelemetry(METHOD.ADD, app.getId(), sourceControlDTO.repositoryUrl,
        rootOrgSourcecontrol.getProvider().toString(), rootOrgSourcecontrol.getRemediationPullRequestsEnabled(),
        rootOrgSourcecontrol.getStatusChecksEnabled(), rootOrgSourcecontrol.getBaseBranch(),
        "public", sourceControlDTO.repositoryUrl);
  }

  @Test
  public void testUpdateSourceControlByOwner_unlicensed() {
    setUnlicensedForSourceControl();
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> sourceControlService.updateSourceControlByOwner(OwnerType.ORGANIZATION,
            "foo", apiSourceControlAdapter.convertToDTO(
                new SourceControl.Builder().setOwnerId(testName.getMethodName())
                    .setRepositoryUrl(VALID_URL)
                    .setToken("baz")
                    .build())));
  }

  @Test
  public void testUpdateSourceControlByOwner_licensedByAutomation() throws Exception {
    when(mockGitClientFactory.createApiClient(any())).thenReturn(mock(GitApiClient.class));

    setLicensedForSourceControlByAutomation();
    testUpdateSourceControlByOwner();
  }

  @Test
  public void testUpdateSourceControlByOwner_licensedByNotifications() throws Exception {
    when(mockGitClientFactory.createApiClient(any())).thenReturn(mock(GitApiClient.class));

    setLicensedForSourceControlByNotifications();
    testUpdateSourceControlByOwner();
  }

  private void testUpdateSourceControlByOwner() throws Exception {
    SourceControl sourceControl = tempEntity.newSourceControl(app.getId(), "http://example.com/test/test");
    ApiSourceControlDTO sourceControlDTO = apiSourceControlAdapter.convertToDTO(sourceControl);

    final Date pollTime = new Date(System.currentTimeMillis() - 5_000);
    sourceControl.setPullRequestPollTime(pollTime);
    final int errorCount = 2;
    sourceControl.setPullRequestErrorCount(errorCount);
    sourceControlDAO.update(sourceControl);

    sourceControlDTO.token = "newToken";

    ApiSourceControlDTO updatedControlDTO = sourceControlService.updateSourceControlByOwner(
        OwnerType.APPLICATION,
        app.getId(),
        sourceControlDTO);

    SourceControl sourceControlAfterUpdate = sourceControlDAO.getByOwnerId(app.getId());

    assertThat(updatedControlDTO).isNotNull();
    String decryptedToken = passwordHandler.decryptPassword(sourceControlAfterUpdate.getToken());
    assertThat(decryptedToken).isEqualTo(sourceControlDTO.token);
    assertThat(sourceControlAfterUpdate.getPullRequestPollTime()).isEqualTo(pollTime);
    assertThat(sourceControlAfterUpdate.getPullRequestErrorCount()).isEqualTo(errorCount);
    assertTelemetry(METHOD.UPDATE, app.getId(), sourceControl.getRepositoryUrl(),
        rootOrgSourcecontrol.getProvider().toString(), rootOrgSourcecontrol.getRemediationPullRequestsEnabled(),
        rootOrgSourcecontrol.getStatusChecksEnabled(), rootOrgSourcecontrol.getBaseBranch(),
        "public", sourceControlAfterUpdate.getRepositoryUrl());
  }

  @Test
  public void testAddSourceControlByOwner_Duplicate() {
    // given an existing source control for an organization
    final ApiSourceControlDTO sourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(org.getId())
            .setToken(TOKEN)
            .build());
    sourceControlService.addSourceControlByOwner(
        OwnerType.ORGANIZATION, org.getId(), sourceControl);

    // expect adding another for the same organization gives error
    final ApiSourceControlDTO sourceControlAgain = apiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(org.getId())
            .setToken(TOKEN)
            .build());
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> sourceControlService.addSourceControlByOwner(
            OwnerType.ORGANIZATION, org.getId(), sourceControlAgain))
        .withMessageContaining(
            "SourceControl already exists for organization with id: " + org.getId());
  }

  @Test
  public void testDeleteSourceControlByOwner_unlicensed() {
    setUnlicensedForSourceControl();
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> sourceControlService.deleteSourceControlByOwner(OwnerType.ORGANIZATION, "foo"));
  }

  @Test
  public void testGetSourceControlByOwnerId_ForOrganization() {
    tempEntity.newSourceControl(org.getId(), null, "token", null);
    final ApiSourceControlDTO sourceControlByApplicationId = sourceControlService
        .getSourceControlByOwner(OwnerType.ORGANIZATION, org.getId());
    assertThat(sourceControlByApplicationId.id).isEqualTo(
        sourceControlByApplicationId.id);
    assertThat(sourceControlByApplicationId.repositoryUrl)
        .isEqualTo(sourceControlByApplicationId.repositoryUrl);
    assertThat(sourceControlByApplicationId.token).isEqualTo(SourceControl.FAKE_SECRET_KEY);
  }

  @Test
  public void testGetSourceControlByOwnerId_ForApplication() {
    tempEntity.newSourceControl(app.getId(), VALID_URL, "token", null);
    final ApiSourceControlDTO sourceControlByApplicationId = sourceControlService
        .getSourceControlByOwner(OwnerType.ORGANIZATION, app.getId());
    assertThat(sourceControlByApplicationId.id).isEqualTo(
        sourceControlByApplicationId.id);
    assertThat(sourceControlByApplicationId.repositoryUrl)
        .isEqualTo(sourceControlByApplicationId.repositoryUrl);
    assertThat(sourceControlByApplicationId.token).isEqualTo(SourceControl.FAKE_SECRET_KEY);
  }

  @Test
  public void testGetSourceControlByOwner_DoesNotExist() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> sourceControlService.getSourceControlByOwner(OwnerType.ORGANIZATION,
            org.getId()));
  }

  @Test
  public void testGetSourceControlByOwnerDecrypted_DoesNotExist() {
    final SourceControl sourceControl = sourceControlService.getCompositeSourceControlByOwnerDecrypted("FAKE_ID");
    assertThat(sourceControl).isNull();
  }

  @Test
  public void testAddSourceControlByOwner_PRTelemetry() {
    final Boolean[] booleanOptions = new Boolean[]{true, false, null};
    final String[] branchOptions = new String[]{"branchA", "", null};

    for (final Boolean remediationPullRequestsEnabled : booleanOptions) {
      for (final Boolean statusChecksEnabled : booleanOptions) {
        for (final String baseBranch : branchOptions) {
          final ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
              new SourceControl.Builder().setOwnerId(org.getId())
                  .setToken(TOKEN)
                  .setStatusChecksEnabled(statusChecksEnabled)
                  .setRemediationPullRequestsEnabled(remediationPullRequestsEnabled)
                  .setBaseBranch(baseBranch)
                  .build());
          final Organization tmpOrg = tempEntity.newOrganization();

          final ApiSourceControlDTO sourceControl = sourceControlService.addSourceControlByOwner(
              OwnerType.ORGANIZATION, tmpOrg.getId(), validSourceControl);

          final SourceControl reloaded = sourceControlDAO.getByIdNotNull(sourceControl.id);

          assertTelemetry(METHOD.ADD, tmpOrg.getId(), reloaded.getRepositoryUrl(), null /* provider */,
              remediationPullRequestsEnabled, statusChecksEnabled, baseBranch, null, null);
        }
      }
    }
  }

  @Test
  public void testAddSourceControlByOwner_ForApplication_private_repo_to_telemetry() throws IOException {
    GitApiClient mockGitApiClient = mock(GitApiClient.class);
    when(mockGitClientFactory.createApiClient(any())).thenReturn(mockGitApiClient);
    when(mockGitApiClient.isRepositoryPrivate()).thenReturn(true);
    final ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(app.getId())
            .setRepositoryUrl(VALID_URL)
            .setToken(TOKEN)
            .build());

    final ApiSourceControlDTO sourceControl = sourceControlService
        .addSourceControlByOwner(OwnerType.APPLICATION, app.getId(), validSourceControl);
    assertThat(sourceControlService.getAll()).hasSize(2);
    assertTelemetry(METHOD.ADD, app.getId(), sourceControl.repositoryUrl, rootOrgSourcecontrol.getProvider().toString(),
        rootOrgSourcecontrol.getRemediationPullRequestsEnabled(), rootOrgSourcecontrol.getStatusChecksEnabled(),
        rootOrgSourcecontrol.getBaseBranch(), "private", null);
  }

  @Test
  public void testUpdateSourceControlByOwner_ForApplication_private_repo_to_telemetry() throws IOException {
    GitApiClient mockGitApiClient = mock(GitApiClient.class);
    when(mockGitClientFactory.createApiClient(any())).thenReturn(mockGitApiClient);
    when(mockGitApiClient.isRepositoryPrivate()).thenReturn(true);
    final ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(app.getId())
            .setRepositoryUrl(VALID_URL)
            .setToken(TOKEN)
            .build());

    final ApiSourceControlDTO sourceControl = sourceControlService
        .addSourceControlByOwner(OwnerType.APPLICATION, app.getId(), validSourceControl);
    assertThat(sourceControlService.getAll()).hasSize(2);
    assertTelemetry(METHOD.ADD, app.getId(), sourceControl.repositoryUrl, rootOrgSourcecontrol.getProvider().toString(),
        rootOrgSourcecontrol.getRemediationPullRequestsEnabled(), rootOrgSourcecontrol.getStatusChecksEnabled(),
        rootOrgSourcecontrol.getBaseBranch(), "private", null);

    ApiSourceControlDTO updatedControlDTO = sourceControlService.updateSourceControlByOwner(
        OwnerType.APPLICATION, app.getId(), sourceControl);
    assertThat(updatedControlDTO).isNotNull();
    assertTelemetry(METHOD.UPDATE, app.getId(), sourceControl.repositoryUrl,
        rootOrgSourcecontrol.getProvider().toString(), rootOrgSourcecontrol.getRemediationPullRequestsEnabled(),
        rootOrgSourcecontrol.getStatusChecksEnabled(), rootOrgSourcecontrol.getBaseBranch(),
        "private", null);
  }

  @Test
  public void testDeleteSourceControlByOwner_ForApplication_private_repo_to_telemetry() throws IOException {
    GitApiClient mockGitApiClient = mock(GitApiClient.class);
    when(mockGitClientFactory.createApiClient(any())).thenReturn(mockGitApiClient);
    when(mockGitApiClient.isRepositoryPrivate()).thenReturn(true);
    final ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(app.getId())
            .setRepositoryUrl(VALID_URL)
            .setToken(TOKEN)
            .build());

    final ApiSourceControlDTO sourceControl = sourceControlService
        .addSourceControlByOwner(OwnerType.APPLICATION, app.getId(), validSourceControl);
    assertThat(sourceControlService.getAll()).hasSize(2);
    assertTelemetry(METHOD.ADD, app.getId(), sourceControl.repositoryUrl, rootOrgSourcecontrol.getProvider().toString(),
        rootOrgSourcecontrol.getRemediationPullRequestsEnabled(), rootOrgSourcecontrol.getStatusChecksEnabled(),
        rootOrgSourcecontrol.getBaseBranch(), "private", null);

    sourceControlService.deleteSourceControlByOwner(OwnerType.APPLICATION, app.getId());
    assertThat(sourceControlService.getAll()).hasSize(1);
    assertTelemetry(METHOD.DELETE, app.getId(), sourceControl.repositoryUrl,
        rootOrgSourcecontrol.getProvider().toString(), rootOrgSourcecontrol.getRemediationPullRequestsEnabled(),
        rootOrgSourcecontrol.getStatusChecksEnabled(), rootOrgSourcecontrol.getBaseBranch(),
        "private", null);
  }

  @Test
  public void testAddSourceControlByOwner_ForApplication_unable_determine_repo_visibility() throws IOException {
    GitApiClient mockGitApiClient = mock(GitApiClient.class);
    when(mockGitClientFactory.createApiClient(any())).thenReturn(mockGitApiClient);
    when(mockGitApiClient.isRepositoryPrivate()).thenThrow(IOException.class);
    final ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(app.getId())
            .setRepositoryUrl(VALID_URL)
            .setToken(TOKEN)
            .build());

    final ApiSourceControlDTO sourceControl = sourceControlService
        .addSourceControlByOwner(OwnerType.APPLICATION, app.getId(), validSourceControl);

    assertThat(sourceControlService.getAll()).hasSize(2);
    assertTelemetry(METHOD.ADD, app.getId(), sourceControl.repositoryUrl, rootOrgSourcecontrol.getProvider().toString(),
        rootOrgSourcecontrol.getRemediationPullRequestsEnabled(), rootOrgSourcecontrol.getStatusChecksEnabled(),
        rootOrgSourcecontrol.getBaseBranch(), null, null);
  }

  @Test
  public void testGetSourceControlByOwnerDecrypted() {
    final SourceControl sourceControl = new SourceControl.Builder().setOwnerId(app.getId())
        .setRepositoryUrl(VALID_URL)
        .setToken("token")
        .build();
    sourceControlService.encryptToken(sourceControl);
    tempEntity.newSourceControl(sourceControl.getOwnerId(), sourceControl.getRepositoryUrl(), sourceControl.getToken(),
        sourceControl.getProvider());
    final SourceControl sourceControlByApplicationId =
        sourceControlService.getCompositeSourceControlByOwnerDecrypted(app.getId());
    assertThat(sourceControlByApplicationId.getOwnerId()).isEqualTo(app.getId());
    assertThat(sourceControlByApplicationId.getRepositoryUrl()).isEqualTo(VALID_URL);
    assertThat(sourceControlByApplicationId.getUsername()).isNull();
    assertThat(sourceControlByApplicationId.getToken()).isEqualTo(TOKEN);
    assertThat(sourceControlByApplicationId.getProvider()).isEqualTo(SourceControlProvider.GITHUB);
  }

  @Test
  public void testGetSourceControlByOwnerDecrypted_nLevelOwnerHierarchy() {
    // given an org1 under root org, an org2 under org1, and an app under it
    Organization org1 = tempEntity.newOrganization();
    Organization org2 = tempEntity.newOrganization(org1);
    Application app2 = tempEntity.newApplication(org2.getId());

    // and a series of SourceControl records - for app2
    SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(app2.getId())
        .setRepositoryUrl(VALID_URL)
        .build();
    tempEntity.newSourceControl(sourceControl);
    // for org2 - sets token
    sourceControl = new SourceControl.Builder()
        .setOwnerId(org2.getId())
        .setToken("token2")
        .build();
    sourceControlService.encryptToken(sourceControl);
    tempEntity.newSourceControl(sourceControl);
    // for org1 - disable RemediationPullRequests
    sourceControl = new SourceControl.Builder()
        .setOwnerId(org1.getId())
        .setRemediationPullRequestsEnabled(false)
        .build();
    tempEntity.newSourceControl(sourceControl);

    // when:
    final SourceControl sourceControlByApplicationId =
        sourceControlService.getCompositeSourceControlByOwnerDecrypted(app2.getId());

    // then:
    assertThat(sourceControlByApplicationId.getOwnerId()).isEqualTo(app2.getId());
    assertThat(sourceControlByApplicationId.getRepositoryUrl()).isEqualTo(VALID_URL);
    assertThat(sourceControlByApplicationId.getUsername()).isNull();
    assertThat(sourceControlByApplicationId.getToken()).isEqualTo("token2"); // from org2
    assertThat(sourceControlByApplicationId.getRemediationPullRequestsEnabled()).isFalse(); // from org1
    assertThat(sourceControlByApplicationId.getProvider()).isEqualTo(SourceControlProvider.GITHUB); // from root org
  }

  @Test
  public void testGetSourceControlByOwnerDecrypted_NotFound() {
    final SourceControl sourceControlByApplicationId =
        sourceControlService.getCompositeSourceControlByOwnerDecrypted("INVALID_ID");
    assertThat(sourceControlByApplicationId).isNull();
  }

  private void assertTelemetry(
      final METHOD method,
      final String ownerId,
      final String repositoryUrl,
      final String provider,
      final Boolean remediationPullRequestsEnabled,
      final Boolean statusChecksEnabled,
      final String baseBranch,
      final String repoVisibility,
      final String publicRepositoryUrl)
  {
    final ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock).send(telemetryDataArgumentCaptor.capture());
    final TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();
    final Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("method", method);
    expectedAttributes.put("owner_id", HdsClientAnalytics.obfuscate(ownerId));
    expectedAttributes.put("real_owner_id", ownerId);

    expectedAttributes.put("repository_url", HdsClientAnalytics.obfuscate(repositoryUrl));
    expectedAttributes.put("provider", provider);
    expectedAttributes.put("enable_pull_requests", remediationPullRequestsEnabled);
    expectedAttributes.put("enable_status_checks", statusChecksEnabled);
    expectedAttributes.put("base_branch", baseBranch);
    if (repoVisibility != null) {
      expectedAttributes.put("repo_visibility", repoVisibility);
      expectedAttributes.put("public_repository_url", publicRepositoryUrl);
    }
    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SOURCE_CONTROL);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    assertThat(telemetryData.getAttributes()).isEqualTo(expectedAttributes);
    reset(telemetrySenderMock);
  }

  @Test
  public void testUpdateSourceControlByOwner_WithFakeSecretKey() {
    final ApiSourceControlDTO sourceControl = sourceControlService
        .addSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(),
            apiSourceControlAdapter
                .convertToDTO(new SourceControl.Builder().setOwnerId(org.getId()).setToken(TOKEN).build()));
    sourceControl.token = FAKE_SECRET_KEY;
    sourceControlService.updateSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(),
        sourceControl);
    assertThat(sourceControlService.getCompositeSourceControlByOwnerDecrypted(org.getId()).getToken())
        .isEqualTo(TOKEN);
  }

  @Test
  public void testGetSourceControlByOwner_RespondsWithEmptyTokenIfNotSet() {
    ApiSourceControlDTO sourceControl = sourceControlService
        .addSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(),
            apiSourceControlAdapter.convertToDTO(new SourceControl.Builder().setOwnerId(org.getId()).build()));
    sourceControl.token = FAKE_SECRET_KEY;
    sourceControl = sourceControlService.getSourceControlByOwner(OwnerType.ORGANIZATION, org.getId());
    assertThat(sourceControl.token).isEqualTo(null);
  }

  @Test
  public void testAddSourceControlByOwner_RespondsWithEmptyTokenIfNotSet() {
    ApiSourceControlDTO sourceControl =
        apiSourceControlAdapter.convertToDTO(new SourceControl.Builder().setOwnerId(org.getId()).build());
    sourceControl = sourceControlService.addSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(), sourceControl);
    assertThat(sourceControl.token).isEqualTo(null);
  }

  @Test
  public void testUpdateSourceControlByOwner_RespondsWithEmptyTokenIfNotSet() {
    ApiSourceControlDTO sourceControl = sourceControlService
        .addSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(),
            apiSourceControlAdapter
                .convertToDTO(new SourceControl.Builder().setOwnerId(org.getId()).setToken(TOKEN).build()));
    sourceControl.token = null;
    sourceControl = sourceControlService
        .updateSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(), sourceControl);
    assertThat(sourceControl.token).isEqualTo(null);
  }

  @Test
  public void testValidateUrl_SupportedUrlFormat() {
    // http URLs are accepted
    assertThatNoException().isThrownBy(() -> sourceControlService.validateUrl("http://server/owner/repo"));

    // https URLs are accepted
    assertThatNoException().isThrownBy(() -> sourceControlService.validateUrl("https://server/owner/repo"));
  }

  @Test
  public void testValidateUrl_UnsupportedUrlFormat() {
    // explicit ssh URLs
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> sourceControlService.validateUrl("ssh://git@server/owner/repo.git"));

    // explicit ssh URLs
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> sourceControlService.validateUrl("ssh://server/owner/repo.git"));

    // implicit ssh URLs
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> sourceControlService.validateUrl("git@server:owner/repo.git"));

    // local protocol
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> sourceControlService.validateUrl("/server/owner/repo.git"));

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> sourceControlService.validateUrl("file:///server/owner/repo.git"));

    // git protocol
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> sourceControlService.validateUrl("git://server/owner/repo.git"));

    // broken URLs
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> sourceControlService.validateUrl("ht://host/org2/broken-url-2.git"));
  }

  @Test
  public void testUpdateSourceControlByOwner_createsEventOnUrlChange() {
    when(mockGitClientFactory.createApiClient(any())).thenReturn(mock(GitApiClient.class));

    // given : Create sourcecontrol, with associated event
    ApiSourceControlDTO persistedSourceControlDTO = sourceControlService.addSourceControlByOwner(
        OwnerType.APPLICATION,
        app.getId(),
        createSourceControlDtoForTesting());
    assertThat(persistedSourceControlDTO).isNotNull();
    tempEntity.newSourceControlEvent(app, new PolicyEvaluation());

    // when : repo url is updated
    persistedSourceControlDTO.repositoryUrl = "http://www.github.com/myOrg/myApp2";
    sourceControlService.updateSourceControlByOwner(
        OwnerType.APPLICATION,
        app.getId(),
        persistedSourceControlDTO);

    // then : events are cleared and new event added
    List<SourceControlEvent> events = sourceControlEventDAO.getAllByApplicationId(app.getId());
    assertThat(events).hasSize(1);
    assertThat(events.get(0).getEventType()).isEqualTo(REPOSITORY_URL_UPDATED_EVENT);
    assertThat(events.get(0).getEventPriority()).isEqualTo(EVENT_PRIORITY_HIGHER);
  }

  @Test
  public void testUpdateSourceControlByOwner_doesNotCreateEventWhenUrlNotChanged() {
    when(mockGitClientFactory.createApiClient(any())).thenReturn(mock(GitApiClient.class));

    // given : Create sourcecontrol, with associated eval and comment
    ApiSourceControlDTO persistedSourceControlDTO = sourceControlService.addSourceControlByOwner(
        OwnerType.APPLICATION,
        app.getId(),
        createSourceControlDtoForTesting());
    assertThat(persistedSourceControlDTO).isNotNull();
    tempEntity.newSourceControlEvent(app, new PolicyEvaluation());

    // when : update with constant repo url
    persistedSourceControlDTO.remediationPullRequestsEnabled = true;
    sourceControlService.updateSourceControlByOwner(
        OwnerType.APPLICATION,
        app.getId(),
        persistedSourceControlDTO);

    // then : events are not cleared and new event not added
    List<SourceControlEvent> events = sourceControlEventDAO.getAllByApplicationId(app.getId());
    assertThat(events).hasSize(1);
    assertThat(events.get(0).getEventType()).isEqualTo(DISCOVERED_PULL_REQUEST_EVENT);
    assertThat(events.get(0).getEventPriority()).isEqualTo(EVENT_PRIORITY_NORMAL);
  }

  @Test
  public void testGetRateLimits_UnlicensedForSourceControl() {
    setUnlicensedForSourceControl();

    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(
        () -> sourceControlService.getRateLimits(OwnerType.ORGANIZATION, ROOT_ORGANIZATION_ID));
  }

  @Test
  public void testGetRateLimits_OnlyLicensedForSourceControlByAutomation() {
    setLicensedForSourceControlByAutomation();

    assertThat(sourceControlService.getRateLimits(OwnerType.ORGANIZATION, ROOT_ORGANIZATION_ID)).isNotNull();
  }

  @Test
  public void testGetRateLimits_OnlyLicensedForSourceControlByNotifications() {
    setLicensedForSourceControlByNotifications();

    assertThat(sourceControlService.getRateLimits(OwnerType.ORGANIZATION, ROOT_ORGANIZATION_ID)).isNotNull();
  }

  @Test
  public void testGetRateLimits_NoSourceControlConfigured() {
    Organization rootOrganization = organizationDAO.getById(ROOT_ORGANIZATION_ID);
    tempEntity.newApplicationWithParent();

    ApiOwnerUserRateLimitsDTO dto = sourceControlService.getRateLimits(OwnerType.ORGANIZATION, ROOT_ORGANIZATION_ID);

    assertThat(dto).isNotNull();
    assertThat(dto.ownerType).isEqualTo(OwnerType.ORGANIZATION.toString());
    assertThat(dto.ownerId).isEqualTo(ROOT_ORGANIZATION_ID);
    assertThat(dto.ownerPublicId).isEqualTo(ROOT_ORGANIZATION_ID);
    assertThat(dto.ownerName).isEqualTo(rootOrganization.getName());
    assertThat(dto.userRateLimits).isEmpty();
  }

  @Test
  public void testGetRateLimits_OrganizationSourceControlConfigured() {
    Organization organization = tempEntity.newOrganization();
    tempEntity.newSourceControl(organization.getId(), null);

    ApiOwnerUserRateLimitsDTO dto = sourceControlService.getRateLimits(organization.getType(), organization.getId());

    assertThat(dto).isNotNull();
    assertThat(dto.ownerType).isEqualTo(organization.getType().toString());
    assertThat(dto.ownerId).isEqualTo(organization.getId());
    assertThat(dto.ownerPublicId).isEqualTo(organization.getPublicId());
    assertThat(dto.ownerName).isEqualTo(organization.getName());
    assertThat(dto.userRateLimits).isEmpty();
  }

  @Test
  public void testGetRateLimits_ApplicationSourceControlConfigured_NoToken() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newSourceControl(application.getId(), "https://github.com/orgName/repoName");

    ApiOwnerUserRateLimitsDTO dto = sourceControlService.getRateLimits(application.getType(), application.getId());

    assertThat(dto).isNotNull();
    assertThat(dto.ownerType).isEqualTo(application.getType().toString());
    assertThat(dto.ownerId).isEqualTo(application.getId());
    assertThat(dto.ownerPublicId).isEqualTo(application.getPublicId());
    assertThat(dto.ownerName).isEqualTo(application.getName());
    assertThat(dto.userRateLimits).isEmpty();
  }

  @Test
  public void testGetRateLimits_ApplicationSourceControlConfigured() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    SourceControl sourceControl =
        tempEntity.newSourceControl(application.getId(), "https://github.com/orgName/repoName",
            passwordHandler.encryptPassword(TOKEN), SourceControlProvider.GITHUB);
    GeneralSCMApiClient mockGeneralSCMApiClient = createMockGeneralSCMApiClient();
    when(mockGitClientFactory.createGeneralApiClient(sourceControl.getProvider(), "https://github.com/orgName/repoName",
        sourceControl.getUsername(), "token")).thenReturn(mockGeneralSCMApiClient);
    GitApiClient mockGitApiClient = createMockGitApiClient("userId2");
    when(mockGitClientFactory.createApiClient(any())).thenReturn(mockGitApiClient);

    ApiOwnerUserRateLimitsDTO dto = sourceControlService.getRateLimits(application.getType(), application.getId());

    assertThat(dto).isNotNull();
    assertThat(dto.ownerType).isEqualTo(application.getType().toString());
    assertThat(dto.ownerId).isEqualTo(application.getId());
    assertThat(dto.ownerPublicId).isEqualTo(application.getPublicId());
    assertThat(dto.ownerName).isEqualTo(application.getName());
    assertThat(dto.userRateLimits).hasSize(1);
    assertThat(dto.userRateLimits.get(0).user).isEqualTo("userId2");
    assertThat(dto.userRateLimits.get(0).provider).isEqualTo(SourceControlProvider.GITHUB);
    assertThat(dto.userRateLimits.get(0).definingOwners).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(ApiOwnerDTO.fromOwner(application));
    assertThat(dto.userRateLimits.get(0).associatedApplications).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(ApiOwnerDTO.fromOwner(application));
    assertRateLimitResponses(dto.userRateLimits.get(0));
  }

  @Test
  public void testGetRateLimits_OrganizationAndApplicationSourceControlConfigured() throws Exception {
    Organization org = tempEntity.newOrganization();
    SourceControl orgSourceControl = tempEntity.newSourceControl(org.getId(), null,
        passwordHandler.encryptPassword("token1"), SourceControlProvider.GITHUB);
    Application app1 = tempEntity.newApplication(org.getId());
    tempEntity.newSourceControl(app1.getId(), "https://github.com/orgName/repoName1", null, null);
    Application app2 = tempEntity.newApplication(org.getId());
    tempEntity.newSourceControl(app2.getId(), "https://github.com/orgName/repoName2",
        passwordHandler.encryptPassword("token2"), null);
    GeneralSCMApiClient mockGeneralSCMApiClient = createMockGeneralSCMApiClient();
    when(mockGitClientFactory.createGeneralApiClient(eq(orgSourceControl.getProvider()), any(),
        eq(orgSourceControl.getUsername()), any())).thenReturn(mockGeneralSCMApiClient);
    lenient().when(mockGitClientFactory.createApiClient(any())).thenAnswer(invocationOnMock -> {
      GitRepositoryInfo gitRepositoryInfo = invocationOnMock.getArgument(0);
      if ("https://github.com/orgName/repoName1".equals(gitRepositoryInfo.getRepositoryUrl())) {
        return createMockGitApiClient("userId2");
      }
      if ("https://github.com/orgName/repoName2".equals(gitRepositoryInfo.getRepositoryUrl())) {
        return createMockGitApiClient("userId1");
      }
      return null;
    });

    ApiOwnerUserRateLimitsDTO dto = sourceControlService.getRateLimits(org.getType(), org.getId());

    assertThat(dto).isNotNull();
    assertThat(dto.ownerType).isEqualTo(org.getType().toString());
    assertThat(dto.ownerId).isEqualTo(org.getId());
    assertThat(dto.ownerPublicId).isEqualTo(org.getPublicId());
    assertThat(dto.ownerName).isEqualTo(org.getName());
    assertThat(dto.userRateLimits).hasSize(2);
    assertThat(dto.userRateLimits.get(0).user).isEqualTo("userId1");
    assertThat(dto.userRateLimits.get(0).provider).isEqualTo(SourceControlProvider.GITHUB);
    assertThat(dto.userRateLimits.get(0).definingOwners).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(ApiOwnerDTO.fromOwner(app2));
    assertThat(dto.userRateLimits.get(0).associatedApplications).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(ApiOwnerDTO.fromOwner(app2));
    assertRateLimitResponses(dto.userRateLimits.get(0));
    assertThat(dto.userRateLimits.get(1).user).isEqualTo("userId2");
    assertThat(dto.userRateLimits.get(1).provider).isEqualTo(SourceControlProvider.GITHUB);
    assertThat(dto.userRateLimits.get(1).definingOwners).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(ApiOwnerDTO.fromOwner(org));
    assertThat(dto.userRateLimits.get(1).associatedApplications).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(ApiOwnerDTO.fromOwner(app1));
    assertRateLimitResponses(dto.userRateLimits.get(1));
  }

  @Test
  public void testGetRateLimits_DoesNotDuplicateToken() throws Exception {
    Organization org = tempEntity.newOrganization();
    SourceControl orgSourceControl = tempEntity.newSourceControl(org.getId(), null,
        passwordHandler.encryptPassword(TOKEN), SourceControlProvider.GITHUB);
    Application app1 = tempEntity.newApplication(org.getId());
    tempEntity.newSourceControl(app1.getId(), "https://github.com/orgName/repoName1", null, null);
    Application app2 = tempEntity.newApplication(org.getId());
    tempEntity.newSourceControl(app2.getId(), "https://github.com/orgName/repoName2",
        passwordHandler.encryptPassword(TOKEN), null);
    GeneralSCMApiClient mockGeneralSCMApiClient = createMockGeneralSCMApiClient();
    when(mockGitClientFactory.createGeneralApiClient(eq(orgSourceControl.getProvider()), any(),
        eq(orgSourceControl.getUsername()), any())).thenReturn(mockGeneralSCMApiClient);
    GitApiClient mockGitApiClient = createMockGitApiClient("userId2");
    when(mockGitClientFactory.createApiClient(any())).thenReturn(mockGitApiClient);

    ApiOwnerUserRateLimitsDTO dto = sourceControlService.getRateLimits(org.getType(), org.getId());

    assertThat(dto).isNotNull();
    assertThat(dto.ownerType).isEqualTo(org.getType().toString());
    assertThat(dto.ownerId).isEqualTo(org.getId());
    assertThat(dto.ownerPublicId).isEqualTo(org.getPublicId());
    assertThat(dto.ownerName).isEqualTo(org.getName());
    assertThat(dto.userRateLimits).hasSize(1);
    assertThat(dto.userRateLimits.get(0).user).isEqualTo("userId2");
    assertThat(dto.userRateLimits.get(0).provider).isEqualTo(SourceControlProvider.GITHUB);
    assertThat(dto.userRateLimits.get(0).definingOwners).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(ApiOwnerDTO.fromOwner(org), ApiOwnerDTO.fromOwner(app2));
    assertThat(dto.userRateLimits.get(0).associatedApplications).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(ApiOwnerDTO.fromOwner(app1), ApiOwnerDTO.fromOwner(app2));
    assertRateLimitResponses(dto.userRateLimits.get(0));
  }

  @Test
  public void testGetRateLimits_Error() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newSourceControl(application.getId(), "https://github.com/orgName/repoName",
        passwordHandler.encryptPassword(TOKEN), SourceControlProvider.GITHUB);
    GitApiClient mockGitApiClient = createMockGitApiClient("userId2");
    when(mockGitApiClient.getSynchronizationKey()).thenThrow(new RuntimeException("Some Error"));
    when(mockGitClientFactory.createApiClient(any())).thenReturn(mockGitApiClient);

    ApiOwnerUserRateLimitsDTO dto = sourceControlService.getRateLimits(application.getType(), application.getId());

    assertThat(dto).isNotNull();
    assertThat(dto.ownerType).isEqualTo(application.getType().toString());
    assertThat(dto.ownerId).isEqualTo(application.getId());
    assertThat(dto.ownerPublicId).isEqualTo(application.getPublicId());
    assertThat(dto.ownerName).isEqualTo(application.getName());
    assertThat(dto.userRateLimits).isEmpty();
  }

  @Test
  public void testGetRateLimits_NoToken() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    SourceControl sourceControl =
        tempEntity.newSourceControl(application.getId(), "https://github.com/orgName/repoName",
            null, SourceControlProvider.GITHUB);
    GeneralSCMApiClient mockGeneralSCMApiClient = createMockGeneralSCMApiClient();
    lenient().when(
        mockGitClientFactory.createGeneralApiClient(sourceControl.getProvider(), "https://github.com/orgName/repoName",
            sourceControl.getUsername(), "token"))
        .thenReturn(mockGeneralSCMApiClient);
    GitApiClient mockGitApiClient = createMockGitApiClient("userId2");
    lenient().when(mockGitClientFactory.createApiClient(any())).thenReturn(mockGitApiClient);

    ApiOwnerUserRateLimitsDTO dto = sourceControlService.getRateLimits(application.getType(), application.getId());

    assertThat(dto).isNotNull();
    assertThat(dto.ownerType).isEqualTo(application.getType().toString());
    assertThat(dto.ownerId).isEqualTo(application.getId());
    assertThat(dto.ownerPublicId).isEqualTo(application.getPublicId());
    assertThat(dto.ownerName).isEqualTo(application.getName());
    assertThat(dto.userRateLimits).isEmpty();
  }

  @Test
  public void testGetRateLimits_NoGitRepositoryInfo() throws Exception {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    SourceControl sourceControl =
        tempEntity.newSourceControl(organization.getId(), null,
            passwordHandler.encryptPassword(TOKEN), SourceControlProvider.GITHUB);
    GeneralSCMApiClient mockGeneralSCMApiClient = createMockGeneralSCMApiClient();
    lenient().when(mockGitClientFactory.createGeneralApiClient(eq(sourceControl.getProvider()), any(),
        eq(sourceControl.getUsername()), eq("token"))).thenReturn(mockGeneralSCMApiClient);
    GitApiClient mockGitApiClient = createMockGitApiClient("userId2");
    lenient().when(mockGitClientFactory.createApiClient(any())).thenReturn(mockGitApiClient);

    ApiOwnerUserRateLimitsDTO dto = sourceControlService.getRateLimits(application.getType(), application.getId());

    assertThat(dto).isNotNull();
    assertThat(dto.ownerType).isEqualTo(application.getType().toString());
    assertThat(dto.ownerId).isEqualTo(application.getId());
    assertThat(dto.ownerPublicId).isEqualTo(application.getPublicId());
    assertThat(dto.ownerName).isEqualTo(application.getName());
    assertThat(dto.userRateLimits).isEmpty();
  }

  @Test
  public void testGetCompositeSourceControlByApplicationId_h2() {
    final SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(app.getId())
        .setRepositoryUrl(VALID_URL)
        .setRepositorySshUrl(VALID_URL)
        .setToken("fake token")
        .build();

    tempEntity.newSourceControl(
        sourceControl.getOwnerId(),
        sourceControl.getRepositoryUrl(),
        sourceControl.getToken(),
        sourceControl.getProvider());

    final SourceControl compositeSourceControl =
        sourceControlService.getCompositeSourceControlByApplicationId(app.getId());

    assertThat(compositeSourceControl.getOwnerId()).isEqualTo(app.getId());
    assertThat(compositeSourceControl.getRepositoryUrl()).isEqualTo(VALID_URL);
    assertThat(compositeSourceControl.getUsername()).isNull();
    assertThat(compositeSourceControl.getProvider()).isEqualTo(SourceControlProvider.GITHUB);
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetCompositeSourceControlByApplicationId_postgres() {
    final SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(app.getId())
        .setRepositoryUrl(VALID_URL)
        .setRepositorySshUrl(VALID_URL)
        .setToken("fake token")
        .build();

    tempEntity.newSourceControl(
        sourceControl.getOwnerId(),
        sourceControl.getRepositoryUrl(),
        sourceControl.getToken(),
        sourceControl.getProvider());

    final SourceControl compositeSourceControl =
        sourceControlService.getCompositeSourceControlByApplicationId(app.getId());

    assertThat(compositeSourceControl.getOwnerId()).isEqualTo(app.getId());
    assertThat(compositeSourceControl.getRepositoryUrl()).isEqualTo(VALID_URL);
    assertThat(compositeSourceControl.getUsername()).isNull();
    assertThat(compositeSourceControl.getProvider()).isEqualTo(SourceControlProvider.GITHUB);
  }

  @Test
  public void testGetCompositeSourceControlByApplicationId_DoesNotExist_h2() {
    final SourceControl sourceControl = sourceControlService.getCompositeSourceControlByApplicationId("Fake ID");
    assertThat(sourceControl).isNull();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetCompositeSourceControlByApplicationId_DoesNotExist_postgres() {
    final SourceControl sourceControl = sourceControlService.getCompositeSourceControlByApplicationId("Fake ID");
    assertThat(sourceControl).isNull();
  }

  @Test
  public void testGetCompositeSourceControlByApplicationId_nLevelOwnerHierarchy_h2() {
    Organization org1 = tempEntity.newOrganization();
    Organization org2 = tempEntity.newOrganization(org1);
    Application app2 = tempEntity.newApplication(org2.getId());

    // Create a series of SourceControl records - for app2
    SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(app2.getId())
        .setRepositoryUrl(VALID_URL)
        .build();
    tempEntity.newSourceControl(sourceControl);
    // for org2 - sets token
    sourceControl = new SourceControl.Builder()
        .setOwnerId(org2.getId())
        .setToken("token2")
        .build();
    sourceControlService.encryptToken(sourceControl);
    tempEntity.newSourceControl(sourceControl);
    // for org1 - disable RemediationPullRequests
    sourceControl = new SourceControl.Builder()
        .setOwnerId(org1.getId())
        .setRemediationPullRequestsEnabled(false)
        .build();
    tempEntity.newSourceControl(sourceControl);

    // when:
    final SourceControl sourceControlByApplicationId =
        sourceControlService.getCompositeSourceControlByApplicationId(app2.getId());

    // then:
    assertThat(sourceControlByApplicationId.getOwnerId()).isEqualTo(app2.getId());
    assertThat(sourceControlByApplicationId.getRepositoryUrl()).isEqualTo(VALID_URL);
    assertThat(sourceControlByApplicationId.getUsername()).isNull();
    assertThat(sourceControlByApplicationId.getRemediationPullRequestsEnabled()).isFalse(); // from org1
    assertThat(sourceControlByApplicationId.getProvider()).isEqualTo(SourceControlProvider.GITHUB); // from root org
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetCompositeSourceControlByApplicationId_nLevelOwnerHierarchy_postgres() {
    Organization org1 = tempEntity.newOrganization();
    Organization org2 = tempEntity.newOrganization(org1);
    Application app2 = tempEntity.newApplication(org2.getId());

    // Create a series of SourceControl records - for app2
    SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(app2.getId())
        .setRepositoryUrl(VALID_URL)
        .build();
    tempEntity.newSourceControl(sourceControl);
    // for org2 - sets token
    sourceControl = new SourceControl.Builder()
        .setOwnerId(org2.getId())
        .setToken("token2")
        .build();
    sourceControlService.encryptToken(sourceControl);
    tempEntity.newSourceControl(sourceControl);
    // for org1 - disable RemediationPullRequests
    sourceControl = new SourceControl.Builder()
        .setOwnerId(org1.getId())
        .setRemediationPullRequestsEnabled(false)
        .build();
    tempEntity.newSourceControl(sourceControl);

    // when:
    final SourceControl sourceControlByApplicationId =
        sourceControlService.getCompositeSourceControlByApplicationId(app2.getId());

    // then:
    assertThat(sourceControlByApplicationId.getOwnerId()).isEqualTo(app2.getId());
    assertThat(sourceControlByApplicationId.getRepositoryUrl()).isEqualTo(VALID_URL);
    assertThat(sourceControlByApplicationId.getUsername()).isNull();
    assertThat(sourceControlByApplicationId.getRemediationPullRequestsEnabled()).isFalse(); // from org1
    assertThat(sourceControlByApplicationId.getProvider()).isEqualTo(SourceControlProvider.GITHUB); // from root org
  }

  @Test
  public void testGetSourceControlMetricsForApplication_filtersOutManualPRs() {
    setLicensedForSourceControlByAutomation();
    PullRequestResult automatedPR = new PullRequestResult();
    automatedPR.setCheckoutTime(1L);
    automatedPR.setRemediationTime(1L);
    automatedPR.setPushTime(1L);
    automatedPR.setPullRequestCreationTime(1L);
    automatedPR.setSuccessful(true);
    EnhancedPullRequestResult enhancedAutomatedPR =
        new EnhancedPullRequestResult(automatedPR, new Date(System.currentTimeMillis() - 1000),
            ComponentIdentifier.createMavenCoordinates("foo", "bar", "1.0"),
            "Auto Bump bar to 1.1", false, false);

    PullRequestResult manualPR = new PullRequestResult();
    manualPR.setCheckoutTime(1L);
    manualPR.setRemediationTime(1L);
    manualPR.setPushTime(1L);
    manualPR.setPullRequestCreationTime(1L);
    manualPR.setSuccessful(true);
    EnhancedPullRequestResult enhancedManualPR =
        new EnhancedPullRequestResult(manualPR, new Date(System.currentTimeMillis() - 2000),
            ComponentIdentifier.createMavenCoordinates("foo", "bar", "1.0"),
            "Manual Bump bar to 1.1", false, true);

    tempEntity.newSourceControlPullRequestResult(app.getId(), JsonUtils.writeUnformatted(enhancedAutomatedPR));
    tempEntity.newSourceControlPullRequestResult(app.getId(), JsonUtils.writeUnformatted(enhancedManualPR));

    ApiPullRequestResults results = sourceControlService.getSourceControlMetricsForApplication(
        OwnerType.APPLICATION, app.getId());

    assertThat(results.results).hasSize(1);
    assertThat(results.results.get(0).title).isEqualTo("Auto Bump bar to 1.1");
  }

  @Test
  public void testUpdateSourceControlByOwner_AuthTypeChange_GitHubAppToPat_DeactivatesGitHubApp() {
    GitHubApp gitHubApp = createAndInsertGitHubApp(app.getId());
    gitHubApp.setActive(true);
    gitHubAppDAO.update(gitHubApp);

    SourceControl sourceControl = createSourceControl(
        app.getId(),
        SourceControl.AuthenticationType.GITHUB_APP,
        "http://example.com/test/test");
    tempEntity.newSourceControl(sourceControl);

    ApiSourceControlDTO updateDTO = apiSourceControlAdapter.convertToDTO(sourceControl);
    updateDTO.authenticationType = SourceControl.AuthenticationType.PAT.name();
    updateDTO.token = "new-pat-token";

    sourceControlService.updateSourceControlByOwner(OwnerType.APPLICATION, app.getId(), updateDTO);

    GitHubApp deactivatedApp = gitHubAppDAO.getById(gitHubApp.getId());
    assertThat(deactivatedApp).isNotNull();
    assertThat(deactivatedApp.isActive()).isFalse();

    SourceControl updated = sourceControlDAO.getByOwnerId(app.getId());
    assertThat(updated.getAuthenticationType()).isEqualTo(SourceControl.AuthenticationType.PAT);
    assertThat(updated.getToken()).isNotNull();
  }

  @Test
  public void testUpdateSourceControlByOwner_AuthTypeChange_PatToGitHubApp_ClearsToken() {
    SourceControl sourceControl = createSourceControl(
        app.getId(),
        SourceControl.AuthenticationType.PAT,
        "http://example.com/test/test");
    sourceControl.setToken(passwordHandler.encryptPassword("pat-token"));
    tempEntity.newSourceControl(sourceControl);

    ApiSourceControlDTO updateDTO = apiSourceControlAdapter.convertToDTO(sourceControl);
    updateDTO.authenticationType = SourceControl.AuthenticationType.GITHUB_APP.name();
    updateDTO.token = null;

    sourceControlService.updateSourceControlByOwner(OwnerType.APPLICATION, app.getId(), updateDTO);
    SourceControl updated = sourceControlDAO.getByOwnerId(app.getId());
    assertThat(updated.getAuthenticationType()).isEqualTo(SourceControl.AuthenticationType.GITHUB_APP);
    assertThat(updated.getToken()).isNull();
  }

  @Test
  public void testUpdateSourceControlByOwner_AuthTypeChange_NoGitHubApp_DoesNotFail() {
    when(mockGitClientFactory.createApiClient(any())).thenReturn(mock(GitApiClient.class));

    SourceControl sourceControl = createSourceControl(
        app.getId(),
        SourceControl.AuthenticationType.GITHUB_APP,
        "http://example.com/test/test");
    tempEntity.newSourceControl(sourceControl);

    ApiSourceControlDTO updateDTO = apiSourceControlAdapter.convertToDTO(sourceControl);
    updateDTO.authenticationType = "PAT";
    updateDTO.token = "new-pat-token";

    sourceControlService.updateSourceControlByOwner(OwnerType.APPLICATION, app.getId(), updateDTO);
    SourceControl updated = sourceControlDAO.getByOwnerId(app.getId());
    assertThat(updated.getAuthenticationType()).isEqualTo(SourceControl.AuthenticationType.PAT);
    assertThat(updated.getToken()).isNotNull();
  }

  @Test
  public void testUpdateSourceControlByOwner_AuthTypeChange_NonGitHubProvider_DeactivatesGitHubApp() {
    when(mockGitClientFactory.createApiClient(any())).thenReturn(mock(GitApiClient.class));
    GitHubApp gitHubApp = createAndInsertGitHubApp(app.getId());
    gitHubApp.setActive(true);
    gitHubAppDAO.update(gitHubApp);

    SourceControl sourceControl = createSourceControl(
        app.getId(),
        SourceControl.AuthenticationType.GITHUB_APP,
        "https://bitbucket.example.com/scm/PROJECT/repo");
    sourceControl.setProvider(SourceControlProvider.BITBUCKET);
    tempEntity.newSourceControl(sourceControl);

    ApiSourceControlDTO updateDTO = apiSourceControlAdapter.convertToDTO(sourceControl);
    updateDTO.authenticationType = SourceControl.AuthenticationType.PAT.name();
    updateDTO.token = "new-pat-token";

    sourceControlService.updateSourceControlByOwner(OwnerType.APPLICATION, app.getId(), updateDTO);

    GitHubApp deactivated = gitHubAppDAO.getById(gitHubApp.getId());
    assertThat(deactivated).isNotNull();
    assertThat(deactivated.isActive()).isFalse();

    SourceControl updated = sourceControlDAO.getByOwnerId(app.getId());
    assertThat(updated.getAuthenticationType()).isEqualTo(SourceControl.AuthenticationType.PAT);
  }

  @Test
  public void testUpdateSourceControlByOwner_AuthTypeChange_WithInstallationStates_DeactivatesGitHubApp() {
    when(mockGitClientFactory.createApiClient(any())).thenReturn(mock(GitApiClient.class));
    GitHubApp gitHubApp = createAndInsertGitHubApp(app.getId());
    gitHubApp.setActive(true);
    gitHubAppDAO.update(gitHubApp);

    SourceControl sourceControl = createSourceControl(
        app.getId(),
        SourceControl.AuthenticationType.GITHUB_APP,
        "http://example.com/test/test");
    tempEntity.newSourceControl(sourceControl);

    ApiSourceControlDTO updateDTO = apiSourceControlAdapter.convertToDTO(sourceControl);
    updateDTO.authenticationType = SourceControl.AuthenticationType.PAT.name();
    updateDTO.token = "new-pat-token";

    sourceControlService.updateSourceControlByOwner(OwnerType.APPLICATION, app.getId(), updateDTO);

    SourceControl updated = sourceControlDAO.getByOwnerId(app.getId());
    assertThat(updated.getAuthenticationType()).isEqualTo(SourceControl.AuthenticationType.PAT);

    GitHubApp deactivated = gitHubAppDAO.getById(gitHubApp.getId());
    assertThat(deactivated).isNotNull();
    assertThat(deactivated.isActive()).isFalse();
  }

  @Test
  public void testDeleteSourceControlByOwner_WithGitHubApp_DeactivatesGitHubApp() {
    GitHubApp gitHubApp = createAndInsertGitHubApp(app.getId());
    gitHubApp.setActive(true);
    gitHubAppDAO.update(gitHubApp);

    SourceControl sourceControl = createSourceControl(
        app.getId(),
        SourceControl.AuthenticationType.GITHUB_APP,
        "http://example.com/test/test");
    tempEntity.newSourceControl(sourceControl);

    assertThat(gitHubAppDAO.getById(gitHubApp.getId())).isNotNull();
    assertThat(gitHubAppDAO.getById(gitHubApp.getId()).isActive()).isTrue();

    sourceControlService.deleteSourceControlByOwner(OwnerType.APPLICATION, app.getId());

    GitHubApp deactivated = gitHubAppDAO.getById(gitHubApp.getId());
    assertThat(deactivated).isNotNull();
    assertThat(deactivated.isActive()).isFalse();
    assertThat(sourceControlDAO.getByOwnerId(app.getId())).isNull();
  }

  @Test
  public void testDeleteSourceControlByOwner_WithGitHubAppAndPendingInstallationStates_DeactivatesGitHubApp() {
    GitHubApp gitHubApp = createAndInsertGitHubApp(app.getId());
    gitHubApp.setActive(true);
    gitHubAppDAO.update(gitHubApp);

    Date expiresAt = new Date(System.currentTimeMillis() + 900000);
    tempEntity.newGitHubAppInstallationState("pending-state-1", gitHubApp.getId(), "code-verifier-1", expiresAt);
    tempEntity.newGitHubAppInstallationState("pending-state-2", gitHubApp.getId(), "code-verifier-2", expiresAt);

    SourceControl sourceControl = createSourceControl(
        app.getId(),
        SourceControl.AuthenticationType.GITHUB_APP,
        "http://example.com/test/test");
    tempEntity.newSourceControl(sourceControl);

    try (TransactionContext tx = installationStateDAO.createTransactionContext()) {
      tx.begin();
      assertThat(installationStateDAO.findByStateToken(tx, "pending-state-1")).isNotNull();
      assertThat(installationStateDAO.findByStateToken(tx, "pending-state-2")).isNotNull();
      tx.commit();
    }

    sourceControlService.deleteSourceControlByOwner(OwnerType.APPLICATION, app.getId());

    GitHubApp deactivated = gitHubAppDAO.getById(gitHubApp.getId());
    assertThat(deactivated).isNotNull();
    assertThat(deactivated.isActive()).isFalse();
    assertThat(sourceControlDAO.getByOwnerId(app.getId())).isNull();
  }

  @Test
  public void testDeleteSourceControlByOwner_NonGitHubProvider_DeactivatesGitHubApp() {
    GitHubApp gitHubApp = createAndInsertGitHubApp(app.getId());
    gitHubApp.setActive(true);
    gitHubAppDAO.update(gitHubApp);

    SourceControl sourceControl = createSourceControl(
        app.getId(),
        SourceControl.AuthenticationType.GITHUB_APP,
        "https://bitbucket.example.com/scm/PROJECT/repo");
    sourceControl.setProvider(SourceControlProvider.BITBUCKET);
    tempEntity.newSourceControl(sourceControl);
    assertThat(gitHubAppDAO.getById(gitHubApp.getId())).isNotNull();

    sourceControlService.deleteSourceControlByOwner(OwnerType.APPLICATION, app.getId());

    assertThat(sourceControlDAO.getByOwnerId(app.getId())).isNull();

    GitHubApp deactivated = gitHubAppDAO.getById(gitHubApp.getId());
    assertThat(deactivated).isNotNull();
    assertThat(deactivated.isActive()).isFalse();
  }

  @Test
  public void testDeleteSourceControlByOwner_PatAuthentication_NoGitHubAppCleanup() {
    when(mockGitClientFactory.createApiClient(any())).thenReturn(mock(GitApiClient.class));
    SourceControl sourceControl = createSourceControl(
        app.getId(),
        SourceControl.AuthenticationType.PAT,
        "http://example.com/test/test");
    sourceControl.setToken(passwordHandler.encryptPassword("pat-token"));
    tempEntity.newSourceControl(sourceControl);
    assertThatNoException()
        .isThrownBy(() -> sourceControlService.deleteSourceControlByOwner(OwnerType.APPLICATION, app.getId()));
    assertThat(sourceControlDAO.getByOwnerId(app.getId())).isNull();
  }

  @Test
  public void testAddSourceControlByOwner_NoneToGitHubApp_CreatesGitHubAppEntry() {
    assertThat(sourceControlDAO.getByOwnerId(app.getId())).isNull();
    assertThat(gitHubAppDAO.getByOwnerId(app.getId())).isEmpty();
    ApiSourceControlDTO newSourceControl = new ApiSourceControlDTO();
    newSourceControl.ownerId = app.getId();
    newSourceControl.repositoryUrl = "https://github.com/test/repo";
    newSourceControl.authenticationType = SourceControl.AuthenticationType.GITHUB_APP.name();
    newSourceControl.provider = SourceControlProvider.GITHUB.name().toLowerCase();
    ApiSourceControlDTO result = sourceControlService.addSourceControlByOwner(
        OwnerType.APPLICATION, app.getId(), newSourceControl);
    assertThat(result).isNotNull();
    SourceControl created = sourceControlDAO.getByOwnerId(app.getId());
    assertThat(created).isNotNull();
    assertThat(created.getAuthenticationType()).isEqualTo(SourceControl.AuthenticationType.GITHUB_APP);
    assertThat(created.getProvider()).isEqualTo(SourceControlProvider.GITHUB);
    assertThat(created.getToken()).isNull();
  }

  @Test
  public void testUpdateSourceControlByOwner_GitHubAppToGitLab_DeactivatesGitHubApp() {
    when(mockGitClientFactory.createApiClient(any())).thenReturn(mock(GitApiClient.class));
    GitHubApp gitHubApp = createAndInsertGitHubApp(app.getId());
    gitHubApp.setActive(true);
    gitHubAppDAO.update(gitHubApp);

    SourceControl sourceControl = createSourceControl(
        app.getId(),
        SourceControl.AuthenticationType.GITHUB_APP,
        "https://github.com/test/repo");
    tempEntity.newSourceControl(sourceControl);

    ApiSourceControlDTO updateDTO = apiSourceControlAdapter.convertToDTO(sourceControl);
    updateDTO.repositoryUrl = "https://gitlab.com/test/repo";
    updateDTO.provider = "gitlab";
    updateDTO.authenticationType = "PAT";
    updateDTO.token = "gitlab-token";

    sourceControlService.updateSourceControlByOwner(OwnerType.APPLICATION, app.getId(), updateDTO);

    GitHubApp deactivated = gitHubAppDAO.getById(gitHubApp.getId());
    assertThat(deactivated).isNotNull();
    assertThat(deactivated.isActive()).isFalse();

    SourceControl updated = sourceControlDAO.getByOwnerId(app.getId());
    assertThat(updated.getProvider()).isEqualTo(SourceControlProvider.GITLAB);
    assertThat(updated.getAuthenticationType()).isEqualTo(SourceControl.AuthenticationType.PAT);
    assertThat(updated.getToken()).isNotNull();
  }

  private GeneralSCMApiClient createMockGeneralSCMApiClient() throws Exception {
    GeneralSCMApiClient mockGeneralSCMApiClient = mock(GeneralSCMApiClient.class);
    GithubRateLimitsResponse rateLimitsResponse = new GithubRateLimitsResponse();
    Map<String, GithubRateLimitResponse> rateLimitResponseMap = new HashMap<>();
    rateLimitResponseMap.put("category2", createGithubRateLimitResponse());
    rateLimitResponseMap.put("category3", createGithubRateLimitResponse());
    rateLimitResponseMap.put("category1", createGithubRateLimitResponse());
    rateLimitsResponse.setResources(rateLimitResponseMap);
    lenient().when(mockGeneralSCMApiClient.listAllRateLimits()).thenReturn(rateLimitsResponse);
    return mockGeneralSCMApiClient;
  }

  private GithubRateLimitResponse createGithubRateLimitResponse() {
    GithubRateLimitResponse githubRateLimitResponse = new GithubRateLimitResponse();
    githubRateLimitResponse.setLimit(10);
    githubRateLimitResponse.setRemaining(4);
    githubRateLimitResponse.setUsed(6);
    githubRateLimitResponse.setReset(4444);
    return githubRateLimitResponse;
  }

  private GitApiClient createMockGitApiClient(String userId) {
    GitApiClient mockGitApiClient = mock(GitApiClient.class);
    lenient().when(mockGitApiClient.getSynchronizationKey()).thenReturn(userId);
    return mockGitApiClient;
  }

  private void assertRateLimitResponses(ApiUserRateLimitsDTO dto) {
    assertThat(dto.rateLimits.get(0).category).isEqualTo("category1");
    assertThat(dto.rateLimits.get(0)).usingRecursiveComparison()
        .ignoringFields("category")
        .isEqualTo(ApiRateLimitDTO.convert(createGithubRateLimitResponse()));
    assertThat(dto.rateLimits.get(1).category).isEqualTo("category2");
    assertThat(dto.rateLimits.get(1)).usingRecursiveComparison()
        .ignoringFields("category")
        .isEqualTo(ApiRateLimitDTO.convert(createGithubRateLimitResponse()));
    assertThat(dto.rateLimits.get(2).category).isEqualTo("category3");
    assertThat(dto.rateLimits.get(2)).usingRecursiveComparison()
        .ignoringFields("category")
        .isEqualTo(ApiRateLimitDTO.convert(createGithubRateLimitResponse()));
  }

  private ApiSourceControlDTO createSourceControlDtoForTesting() {
    return apiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder()
            .setOwnerId(testName.getMethodName())
            .setRepositoryUrl("http://www.github.com/myOrg/myApp")
            .setToken("baz")
            .build());
  }

  private void setUnlicensedForSourceControl() {
    testProductLicense.setMissingFeatures(LicensedFeature.NOTIFICATIONS, LicensedFeature.AUTOMATION);
  }

  private void setLicensedForSourceControlByAutomation() {
    // remove notification feature, leaving automation
    testProductLicense.setMissingFeatures(LicensedFeature.NOTIFICATIONS);
  }

  private void setLicensedForSourceControlByNotifications() {
    // remove automation feature, leaving notifications
    testProductLicense.setMissingFeatures(LicensedFeature.AUTOMATION);
  }

  private String generateTestRsaPrivateKey() {
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      KeyPair keyPair = keyPairGenerator.generateKeyPair();
      byte[] pkcs8EncodedKey = keyPair.getPrivate().getEncoded();
      return Base64.getEncoder().encodeToString(pkcs8EncodedKey);
    }
    catch (Exception e) {
      throw new RuntimeException("Failed to generate test RSA key", e);
    }
  }

  @Test
  public void testAddSourceControlByOwner_CreatesDefaultSourceControlConfiguration() {
    assertThat(sourceControlConfigurationDAO.get()).isNull();

    Application app = tempEntity.newApplicationWithParent();
    ApiSourceControlDTO dto = new ApiSourceControlDTO();
    dto.repositoryUrl = "https://github.com/sonatype/test-repo";
    dto.token = "test_token";
    dto.baseBranch = "main";

    ApiSourceControlDTO result = sourceControlService.addSourceControlByOwner(
        OwnerType.APPLICATION,
        app.getId(),
        dto);

    assertThat(result).isNotNull();
    assertThat(result.ownerId).isEqualTo(app.getId());

    SourceControlConfiguration config = sourceControlConfigurationDAO.get();
    assertThat(config).isNotNull();
    assertThat(config.getId()).isEqualTo(SourceControlConfigurationDAO.SINGLETON_ENTITY_ID);
    assertThat(config.getCloneDirectory()).isEqualTo(SourceControlConfiguration.DEFAULT_SOURCE_CONTROL_CLONE_DIR);
    assertThat(config.getDefaultBranchMonitoringIntervalHours())
        .isEqualTo(SourceControlConfiguration.DEFAULT_BRANCH_MONITORING_INTERVAL_HOURS);
    assertThat(config.getPullRequestMonitoringIntervalSeconds())
        .isEqualTo(SourceControlConfiguration.DEFAULT_PULL_REQUEST_MONITORING_INTERVAL_SECONDS);
  }

  @Test
  public void testAddSourceControlByOwner_DoesNotOverwriteExistingSourceControlConfiguration() {
    SourceControlConfiguration existingConfig = new SourceControlConfiguration();
    existingConfig.setCloneDirectory("custom-clone-dir");
    existingConfig.setDefaultBranchMonitoringIntervalHours(12);
    existingConfig.setPullRequestMonitoringIntervalSeconds(120);
    sourceControlConfigurationDAO.set(existingConfig);

    Application app = tempEntity.newApplicationWithParent();
    ApiSourceControlDTO dto = new ApiSourceControlDTO();
    dto.repositoryUrl = "https://github.com/sonatype/test-repo";
    dto.token = "test_token";
    dto.baseBranch = "main";

    sourceControlService.addSourceControlByOwner(
        OwnerType.APPLICATION,
        app.getId(),
        dto);

    SourceControlConfiguration config = sourceControlConfigurationDAO.get();
    assertThat(config).isNotNull();
    assertThat(config.getCloneDirectory()).isEqualTo("custom-clone-dir");
    assertThat(config.getDefaultBranchMonitoringIntervalHours()).isEqualTo(12);
    assertThat(config.getPullRequestMonitoringIntervalSeconds()).isEqualTo(120);
  }

  @Test
  public void testAddOrUpdateSourceControl_CreatesDefaultSourceControlConfiguration() {
    assertThat(sourceControlConfigurationDAO.get()).isNull();

    Application app = tempEntity.newApplicationWithParent();
    String repositoryUrl = "https://github.com/sonatype/test-repo";
    String baseBranch = "main";

    ApiSourceControlDTO result = sourceControlService.addOrUpdateSourceControl(
        app.getPublicId(), repositoryUrl, null, baseBranch);

    assertThat(result).isNotNull();
    assertThat(result.ownerId).isEqualTo(app.getId());

    SourceControlConfiguration config = sourceControlConfigurationDAO.get();
    assertThat(config).isNotNull();
    assertThat(config.getId()).isEqualTo(SourceControlConfigurationDAO.SINGLETON_ENTITY_ID);
    assertThat(config.getCloneDirectory()).isEqualTo(SourceControlConfiguration.DEFAULT_SOURCE_CONTROL_CLONE_DIR);
    assertThat(config.getDefaultBranchMonitoringIntervalHours())
        .isEqualTo(SourceControlConfiguration.DEFAULT_BRANCH_MONITORING_INTERVAL_HOURS);
    assertThat(config.getPullRequestMonitoringIntervalSeconds())
        .isEqualTo(SourceControlConfiguration.DEFAULT_PULL_REQUEST_MONITORING_INTERVAL_SECONDS);
  }

  @Test
  public void testAddOrUpdateSourceControl_UpdateDoesNotCreateSingleton() {
    Application app = tempEntity.newApplicationWithParent();
    String repositoryUrl = "https://github.com/sonatype/test-repo";
    String baseBranch = "main";

    sourceControlService.addOrUpdateSourceControl(
        app.getPublicId(), repositoryUrl, null, baseBranch);

    SourceControlConfiguration configAfterInsert = sourceControlConfigurationDAO.get();
    assertThat(configAfterInsert).isNotNull();
    String customCloneDir = "custom-clone-directory";
    configAfterInsert.setCloneDirectory(customCloneDir);
    sourceControlConfigurationDAO.update(configAfterInsert);

    String updatedBranch = "develop";
    sourceControlService.addOrUpdateSourceControl(
        app.getPublicId(), repositoryUrl, null, updatedBranch);

    SourceControlConfiguration configAfterUpdate = sourceControlConfigurationDAO.get();
    assertThat(configAfterUpdate).isNotNull();
    assertThat(configAfterUpdate.getId()).isEqualTo(SourceControlConfigurationDAO.SINGLETON_ENTITY_ID);
    assertThat(configAfterUpdate.getCloneDirectory()).isEqualTo(customCloneDir);
  }

  @Test
  public void testUpdateSourceControlByOwner_CreatesDefaultSourceControlConfiguration() {
    Application app = tempEntity.newApplicationWithParent();
    SourceControl sourceControl = tempEntity.newSourceControl(app.getId(), "https://github.com/sonatype/test-repo");

    SourceControlConfiguration config = sourceControlConfigurationDAO.get();
    if (config != null) {
      sourceControlConfigurationDAO.delete(config);
    }
    assertThat(sourceControlConfigurationDAO.get()).isNull();

    ApiSourceControlDTO dto = apiSourceControlAdapter.convertToDTO(sourceControl);
    dto.baseBranch = "develop";
    sourceControlService.updateSourceControlByOwner(OwnerType.APPLICATION, app.getId(), dto);

    SourceControlConfiguration createdConfig = sourceControlConfigurationDAO.get();
    assertThat(createdConfig).isNotNull();
    assertThat(createdConfig.getId()).isEqualTo(SourceControlConfigurationDAO.SINGLETON_ENTITY_ID);
    assertThat(createdConfig.getCloneDirectory())
        .isEqualTo(SourceControlConfiguration.DEFAULT_SOURCE_CONTROL_CLONE_DIR);
    assertThat(createdConfig.getDefaultBranchMonitoringIntervalHours())
        .isEqualTo(SourceControlConfiguration.DEFAULT_BRANCH_MONITORING_INTERVAL_HOURS);
    assertThat(createdConfig.getPullRequestMonitoringIntervalSeconds())
        .isEqualTo(SourceControlConfiguration.DEFAULT_PULL_REQUEST_MONITORING_INTERVAL_SECONDS);
  }

  @Test
  public void testUpdateSourceControlByOwner_DoesNotOverwriteExistingConfiguration() {
    Application app = tempEntity.newApplicationWithParent();
    SourceControl sourceControl = tempEntity.newSourceControl(app.getId(), "https://github.com/sonatype/test-repo");

    SourceControlConfiguration existingConfig = new SourceControlConfiguration();
    existingConfig.setCloneDirectory("custom-clone-dir");
    existingConfig.setDefaultBranchMonitoringIntervalHours(12);
    existingConfig.setPullRequestMonitoringIntervalSeconds(120);
    sourceControlConfigurationDAO.set(existingConfig);

    ApiSourceControlDTO dto = apiSourceControlAdapter.convertToDTO(sourceControl);
    dto.baseBranch = "develop";
    sourceControlService.updateSourceControlByOwner(OwnerType.APPLICATION, app.getId(), dto);

    SourceControlConfiguration config = sourceControlConfigurationDAO.get();
    assertThat(config).isNotNull();
    assertThat(config.getCloneDirectory()).isEqualTo("custom-clone-dir");
    assertThat(config.getDefaultBranchMonitoringIntervalHours()).isEqualTo(12);
    assertThat(config.getPullRequestMonitoringIntervalSeconds()).isEqualTo(120);
  }

  @Test
  public void testAddOrUpdateSourceControl_UpdateCreatesConfigurationIfMissing() {
    Application app = tempEntity.newApplicationWithParent();
    String repositoryUrl = "https://github.com/sonatype/test-repo";
    sourceControlService.addOrUpdateSourceControl(app.getPublicId(), repositoryUrl, null, "main");

    SourceControlConfiguration config = sourceControlConfigurationDAO.get();
    assertThat(config).isNotNull();
    sourceControlConfigurationDAO.delete(config);
    assertThat(sourceControlConfigurationDAO.get()).isNull();

    sourceControlService.addOrUpdateSourceControl(app.getPublicId(), "https://github.com/sonatype/updated-repo", null,
        "develop");

    SourceControlConfiguration recreatedConfig = sourceControlConfigurationDAO.get();
    assertThat(recreatedConfig).isNotNull();
    assertThat(recreatedConfig.getId()).isEqualTo(SourceControlConfigurationDAO.SINGLETON_ENTITY_ID);
  }

  @Test
  public void testEnsureDefaultSourceControlConfigurationExists_HandlesConcurrentCreation() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    SourceControlConfiguration config = sourceControlConfigurationDAO.get();
    if (config != null) {
      sourceControlConfigurationDAO.delete(config);
    }
    assertThat(sourceControlConfigurationDAO.get()).isNull();

    SourceControlConfigurationDAO mockDao = mock(SourceControlConfigurationDAO.class);
    when(mockDao.get()).thenReturn(null);
    doThrow(new org.jooq.exception.DataAccessException("Unique constraint violation")).when(mockDao).insert(any());

    Field daoField = ApiSourceControlService.class.getDeclaredField("sourceControlConfigurationDAO");
    daoField.setAccessible(true);
    SourceControlConfigurationDAO originalDao = (SourceControlConfigurationDAO) daoField.get(sourceControlService);
    try {
      daoField.set(sourceControlService, mockDao);
      sourceControlService.ensureDefaultSourceControlConfigurationExists();
      verify(mockDao, times(1)).get();
      verify(mockDao, times(1)).insert(any());
    }
    finally {
      daoField.set(sourceControlService, originalDao);
    }
  }

  private GitHubApp createAndInsertGitHubApp(String ownerId) {
    GitHubApp gitHubApp = new GitHubApp();
    gitHubApp.setId(UUID.randomUUID().toString());
    gitHubApp.setOwnerId(ownerId);
    gitHubApp.setAppId(TEST_GITHUB_APP_ID);
    gitHubApp.setSlug(TEST_GITHUB_APP_SLUG);
    gitHubApp.setGithubOrganizationName("myOrg");
    gitHubApp.setLastUpdatedAt(new Date());
    gitHubApp.setClientId("client-id-" + UUID.randomUUID());
    gitHubApp.setClientSecret(passwordHandler.encryptPassword(TEST_CLIENT_SECRET));
    gitHubApp.setPrivateKey(passwordHandler.encryptPassword(generateTestRsaPrivateKey()));
    gitHubApp.setInstallationId(TEST_GITHUB_INSTALLATION_ID);
    return tempEntity.newGitHubApp(gitHubApp);
  }

  private SourceControl createSourceControl(
      String ownerId,
      SourceControl.AuthenticationType authType,
      String repositoryUrl)
  {
    SourceControl sourceControl = new SourceControl();
    sourceControl.setId(UUID.randomUUID().toString());
    sourceControl.setOwnerId(ownerId);
    sourceControl.setAuthenticationType(authType);
    sourceControl.setProvider(SourceControlProvider.GITHUB);
    sourceControl.setRepositoryUrl(repositoryUrl);
    return sourceControl;
  }

  @Test
  public void testEnsureDefaultSourceControlConfigurationExists_ConcurrentCreation_HandlesDuplicateKeyException() throws Exception {
    assertThat(sourceControlConfigurationDAO.get()).isNull();

    SourceControlConfigurationDAO daoSpy = spy(sourceControlConfigurationDAO);

    doAnswer(invocation -> {
      SourceControlConfiguration config = new SourceControlConfiguration();
      sourceControlConfigurationDAO.insert(config);
      throw new jakarta.persistence.EntityExistsException("Duplicate key");
    }).when(daoSpy).insert(any(SourceControlConfiguration.class));

    // Using reflection to inject spy for concurrent insert simulation
    // This is necessary to test the race condition handling without major refactoring
    Field daoField = ApiSourceControlService.class.getDeclaredField("sourceControlConfigurationDAO");
    daoField.setAccessible(true);
    SourceControlConfigurationDAO originalDao = (SourceControlConfigurationDAO) daoField.get(sourceControlService);
    try {
      daoField.set(sourceControlService, daoSpy);

      sourceControlService.ensureDefaultSourceControlConfigurationExists();

      SourceControlConfiguration persistedConfig = sourceControlConfigurationDAO.get();
      assertThat(persistedConfig).isNotNull();
      assertThat(persistedConfig.getId()).isEqualTo(SourceControlConfigurationDAO.SINGLETON_ENTITY_ID);
    }
    finally {
      daoField.set(sourceControlService, originalDao);
    }
  }

  @Test
  public void testEndToEndUpgradePath() {
    // Simulate upgrade scenario where config doesn't exist
    assertThat(sourceControlConfigurationDAO.get()).isNull();

    Application app = tempEntity.newApplicationWithParent();
    String repositoryUrl = "https://github.com/sonatype/test-repo";
    String baseBranch = "main";

    // User creates source control (triggers ensureDefaultSourceControlConfigurationExists)
    sourceControlService.addOrUpdateSourceControl(app.getPublicId(), repositoryUrl, null, baseBranch);

    // Verify source control was created
    SourceControl sourceControl = sourceControlDAO.getByOwnerId(app.getId());
    assertThat(sourceControl).isNotNull();
    assertThat(sourceControl.getRepositoryUrl()).isEqualTo(repositoryUrl);

    // Verify config was automatically created
    SourceControlConfiguration config = sourceControlConfigurationDAO.get();
    assertThat(config).isNotNull();
    assertThat(config.getId()).isEqualTo(SourceControlConfigurationDAO.SINGLETON_ENTITY_ID);
    assertThat(config.getCloneDirectory()).isEqualTo(SourceControlConfiguration.DEFAULT_SOURCE_CONTROL_CLONE_DIR);
  }
}
