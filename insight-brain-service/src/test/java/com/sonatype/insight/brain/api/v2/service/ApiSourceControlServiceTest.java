/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiSourceControlDTO;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlService.METHOD;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticSourceControlConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.sonatype.plexus.components.cipher.PlexusCipher;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControl.FAKE_SECRET_KEY;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.DISCOVERED_PULL_REQUEST_EVENT;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.EVENT_PRIORITY_HIGHER;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.EVENT_PRIORITY_NORMAL;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.REPOSITORY_URL_UPDATED_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

public class ApiSourceControlServiceTest
    extends AbstractComponentTest
{
  private static final String VALID_URL = "https://example.com/organization/project";

  private static final String TOKEN = "token";

  @Inject
  private ApiSourceControlService sourceControlService;

  @Inject
  private PlexusCipher plexusCipher;

  @Inject
  private TestProductLicense testProductLicense;

  @Inject
  private InsightWork insightWork;

  @Mock
  private TelemetrySender telemetrySenderMock;

  private final SourceControlDAO sourceControlDAO = new SourceControlDAO();

  private Application app;

  private Organization org;

  private SourceControl rootOrgSourcecontrol;

  private final SourceControlEventDAO sourceControlEventDAO = new SourceControlEventDAO();

  @Override
  public void configure(final Binder binder) {
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
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
        .map(ApiSourceControlAdapter::convertToDTO)
        .collect(Collectors.toList());

    // decrypt retrieved tokens for comparison
    final List<ApiSourceControlDTO> retrieved = sourceControlService.getAll();
    for (final ApiSourceControlDTO it : retrieved) {
      synchronized (plexusCipher) {
        it.token = plexusCipher.decrypt(it.token, "CMMDwoV");
      }
    }

    assertThat(retrieved).hasSize(3);
    assertThat(JsonUtils.format(retrieved)).isEqualTo(JsonUtils.format(expected));
  }

  @Test
  public void testAddOrUpdateSourceControl_InvalidPublicId() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(
            () -> sourceControlService.addOrUpdateSourceControlFromAppEvaluation("abcdefg", "https://e.com/org/proj"));
  }

  @Test
  public void testAddOrUpdateSourceControl_InvalidRepoUrl() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> testAddOrUpdateSourceControl_AutomaticSourceControl(true, null,
                "https://not valid", null));
  }

  @Test
  public void testAddOrUpdateSourceControl_AutomaticSourceControlDisabled_Create() {
    testAddOrUpdateSourceControl_AutomaticSourceControl(false, null,
        "https://github.com/org/b", "https://github.com/org/a");
  }

  @Test
  public void testAddOrUpdateSourceControl_AutomaticSourceControlDisabled_Update() {
    testAddOrUpdateSourceControl_AutomaticSourceControl(false, "https://github.com/org/a",
        "https://github.com/org/b", "https://github.com/org/a");
  }

  @Test
  public void testAddOrUpdateSourceControl_AutomaticSourceControlEnabled_Create() {
    testAddOrUpdateSourceControl_AutomaticSourceControl(true, null,
        "https://github.com/org/b", "https://github.com/org/b");
  }

  @Test
  public void testAddOrUpdateSourceControl_AutomaticSourceControlEnabled_Update() {
    testAddOrUpdateSourceControl_AutomaticSourceControl(true, "https://github.com/org/a",
        "https://github.com/org/b", "https://github.com/org/a");
  }

  @Test
  public void testAddOrUpdateSourceControl_ContextPath() {
    testAddOrUpdateSourceControl_AutomaticSourceControl(true, null,
        "https://github.com/context/org/a", "https://github.com/context/org/a");
  }

  @Test
  public void testAddOrUpdateSourceControl_CustomPort()  {
    testAddOrUpdateSourceControl_AutomaticSourceControl(true, null,
        "https://github.com:123/context/org/a", "https://github.com:123/context/org/a");
  }

  @Test
  public void testAddOrUpdateSourceControl_DuplicateAccountNme() {
    testAddOrUpdateSourceControl_AutomaticSourceControl(true, null,
        "https://org@github.com/org/a", "https://org@github.com/org/a");
  }

  @Test
  public void testAddOrUpdateSourceControl_DifferentDuplicateAccountName() {
    testAddOrUpdateSourceControl_AutomaticSourceControl(true, null,
        "ssh://git@github.com/org/a/", "ssh://git@github.com/org/a/");
  }

  @Test
  public void testAddOrUpdateSourceControl_ImplicitSshProtocol() {
    testAddOrUpdateSourceControl_AutomaticSourceControl(true, null,
        "git@github.com:org/a.git", "git@github.com:org/a.git");
  }

  @Test
  public void testAddOrUpdateSourceControl_ExplicitSshProtocol() {
    testAddOrUpdateSourceControl_AutomaticSourceControl(true, null,
        "ssh://git@github.com/org/a.git", "ssh://git@github.com/org/a.git");
  }

  @Test
  public void testAddOrUpdateSourceControl_HttpsWithGitExtension() {
    testAddOrUpdateSourceControl_AutomaticSourceControl(true, null,
        "https://org@github.com/org/a.git", "https://org@github.com/org/a.git");
  }

  private void testAddOrUpdateSourceControl_AutomaticSourceControl(
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
    AutomaticSourceControlConfigurationDAO automaticSourceControlConfigurationDAO =
        new AutomaticSourceControlConfigurationDAO();
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(enabled);

    ApiSourceControlDTO result =
        sourceControlService.addOrUpdateSourceControlFromAppEvaluation(app.getPublicId(), collectedUrl);
    if (!enabled && initialUrl == null) {
      assertThat(result).isNull();
    }
    else {
      assertThat(result.ownerId).isEqualTo(app.getId());
      assertThat(result.repositoryUrl).isEqualTo(expectedUrl);
    }
  }

  @Test
  public void testAddSourceControl_Create_Params() {
    // when new source control with default branch ans SSH URL is added
    String httpUrl = "https://localhost/context/org/a";
    String sshUrl = "git@localhost:org/a.git";
    String branch = "branch";
    ApiSourceControlDTO actual = sourceControlService.addOrUpdateSourceControl(app.getPublicId(),
        httpUrl, sshUrl, branch);

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
  }

  @Test
  public void testAddSourceControlByOwner_TokenEncryption() throws Exception {
    final ApiSourceControlDTO validSourceControl = ApiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(org.getId()).setToken(TOKEN)
            .build());
    final ApiSourceControlDTO sourceControl = sourceControlService.addSourceControlByOwner(
        OwnerType.ORGANIZATION, org.getId(), validSourceControl);
    assertThat(sourceControl.token).isNotEqualTo(TOKEN);
    assertThat(sourceControl.token).isEqualTo(SourceControl.FAKE_SECRET_KEY);

    final SourceControl reloaded = sourceControlDAO.getByIdNotNull(sourceControl.id);

    final String decrypted;
    synchronized (plexusCipher) {
      decrypted = plexusCipher.decrypt(reloaded.getToken(), "CMMDwoV");
    }
    assertThat(decrypted).isEqualTo(TOKEN);
    assertTelemetry(METHOD.ADD, org.getId(), reloaded.getRepositoryUrl(),
        null, reloaded.getRemediationPullRequestsEnabled(), reloaded.getStatusChecksEnabled(),
        reloaded.getBaseBranch());
  }

  @Test
  public void testUpdateSourceControlByOwner_TokenEncryption() throws Exception {
    final ApiSourceControlDTO validSourceControl = ApiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(org.getId()).setToken(TOKEN)
            .build()
    );

    final ApiSourceControlDTO sourceControl =
        sourceControlService.addSourceControlByOwner(OwnerType.ORGANIZATION,
            org.getId(), validSourceControl);
    sourceControl.token = "updatedToken";
    assertTelemetry(METHOD.ADD, org.getId(), sourceControl.repositoryUrl,
        sourceControl.provider, sourceControl.remediationPullRequestsEnabled,
        sourceControl.remediationPullRequestsEnabled,
        sourceControl.baseBranch);

    final ApiSourceControlDTO updatedScm =
        sourceControlService.updateSourceControlByOwner(OwnerType.ORGANIZATION,
            org.getId(), sourceControl);
    assertThat(updatedScm.token).isEqualTo(SourceControl.FAKE_SECRET_KEY);
    assertTelemetry(METHOD.UPDATE, org.getId(), sourceControl.repositoryUrl,
        sourceControl.provider, sourceControl.remediationPullRequestsEnabled,
        sourceControl.remediationPullRequestsEnabled,
        sourceControl.baseBranch);

    final SourceControl reloaded = sourceControlDAO.getByIdNotNull(sourceControl.id);

    final String decrypted;
    synchronized (plexusCipher) {
      decrypted = plexusCipher.decrypt(reloaded.getToken(), "CMMDwoV");
    }
    assertThat(decrypted).isEqualTo("updatedToken");
  }

  @Test
  public void testGetSourceControlByOwnerDecrypted_ForOrganization() {
    final ApiSourceControlDTO validSourceControl = ApiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(org.getId()).setToken(TOKEN)
            .build());
    sourceControlService.addSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(),
            validSourceControl);
    assertThat(sourceControlService.getSourceControlByOwnerDecrypted(org.getId()).getToken()).isEqualTo(TOKEN);
  }

  @Test
  public void testGetSourceControlByOwnerDecrypted_ForApplication() {
    final ApiSourceControlDTO validSourceControl = ApiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(app.getId()).setRepositoryUrl(VALID_URL).setToken(TOKEN)
            .build());
    sourceControlService.addSourceControlByOwner(OwnerType.ORGANIZATION, app.getId(), validSourceControl);
    assertThat(sourceControlService.getSourceControlByOwnerDecrypted(app.getId()).getToken()).isEqualTo(TOKEN);
  }

  @Test
  public void testDeleteSourceControlByOwner_ForOrganization_licensedByNotifications() {
    setLicensedForSourceControlByNotifications();

    final ApiSourceControlDTO validSourceControl = ApiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(org.getId()).setToken(TOKEN)
            .build());

    final ApiSourceControlDTO sourceControl = sourceControlService
        .addSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(), validSourceControl);
    assertThat(sourceControlService.getAll()).hasSize(2);
    assertTelemetry(METHOD.ADD, org.getId(), sourceControl.repositoryUrl, sourceControl.provider,
        sourceControl.remediationPullRequestsEnabled, sourceControl.statusChecksEnabled,
        sourceControl.baseBranch);

    sourceControlService.deleteSourceControlByOwner(OwnerType.ORGANIZATION, org.getId());
    assertThat(sourceControlService.getAll()).hasSize(1);
    assertTelemetry(METHOD.DELETE, org.getId(), sourceControl.repositoryUrl, sourceControl.provider,
        sourceControl.remediationPullRequestsEnabled, sourceControl.statusChecksEnabled,
        sourceControl.baseBranch);
  }

  @Test
  public void testDeleteSourceControlByOwner_ForApplication_licensedByAutomation() throws Exception {
    setLicensedForSourceControlByAutomation();
    final ApiSourceControlDTO validSourceControl = ApiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(app.getId()).setRepositoryUrl(VALID_URL).setToken(TOKEN)
            .build());

    final ApiSourceControlDTO sourceControl = sourceControlService
        .addSourceControlByOwner(OwnerType.APPLICATION, app.getId(), validSourceControl);
    assertThat(sourceControlService.getAll()).hasSize(2);
    assertTelemetry(METHOD.ADD, app.getId(), sourceControl.repositoryUrl,
        sourceControl.provider, sourceControl.remediationPullRequestsEnabled, sourceControl.statusChecksEnabled,
        sourceControl.baseBranch);

    File sourceControlDir = insightWork.getSourceControlDir(app.getId());
    sourceControlDir.mkdirs();
    assertThat(sourceControlDir).isDirectory();

    sourceControlService.deleteSourceControlByOwner(OwnerType.APPLICATION, app.getId());
    assertThat(sourceControlService.getAll()).hasSize(1);
    assertTelemetry(METHOD.DELETE, app.getId(), sourceControl.repositoryUrl,
        sourceControl.provider, sourceControl.remediationPullRequestsEnabled, sourceControl.statusChecksEnabled,
        sourceControl.baseBranch);

    assertThat(sourceControlDir).doesNotExist();
  }

  @Test
  public void testUpdateSourceControlByOwner_WithFakeToken() {
    final ApiSourceControlDTO sourceControl = sourceControlService
        .addSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(),
            ApiSourceControlAdapter.convertToDTO(
                new SourceControl.Builder().setOwnerId(org.getId()).setToken(TOKEN)
                    .build()));
    sourceControl.token = SourceControl.FAKE_SECRET_KEY;
    sourceControlService.updateSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(), sourceControl);
    assertThat(sourceControlService.getSourceControlByOwnerDecrypted(org.getId()).getToken()).isEqualTo(TOKEN);
  }

  @Test
  public void testUpdateSourceControlByOwner_WithEmptyToken() {
    final ApiSourceControlDTO sourceControl = sourceControlService
        .addSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(),
            ApiSourceControlAdapter.convertToDTO(
                new SourceControl.Builder().setOwnerId(org.getId()).setToken(TOKEN)
                    .build()));
    sourceControl.token = null;
    sourceControlService.updateSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(),
        sourceControl);
    assertThat(sourceControlService.getSourceControlByOwnerDecrypted(org.getId()).getToken()).isNull();
  }

  @Test
  public void testUpdateSourceControlByOwner_WrongOwnerId() {
    final ApiSourceControlDTO sourceControl = ApiSourceControlAdapter.convertToDTO(
        tempEntity.newSourceControl(org.getId(), null, "token", null));
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() ->
        sourceControlService.updateSourceControlByOwner(OwnerType.ORGANIZATION,
            "foo", sourceControl)
    ).withMessage(String.format(
        "Cannot find SourceControl for organization with id: foo"));
  }

  @Test
  public void testAddSourceControlByOwner_unlicensed() {
    setUnlicensedForSourceControl();
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> sourceControlService.addSourceControlByOwner(
            OwnerType.ORGANIZATION, "foo", ApiSourceControlAdapter.convertToDTO(
                new SourceControl.Builder().setOwnerId(testName.getMethodName()).setRepositoryUrl("bar").setToken("baz")
                    .build())));
  }

  @Test
  public void testAddSourceControlByOwner_licensedByAutomation() {
    setLicensedForSourceControlByAutomation();
    ApiSourceControlDTO sourceControlDTO = sourceControlService.addSourceControlByOwner(
        OwnerType.APPLICATION,
        app.getId(),
        createSourceControlDtoForTesting()
    );
    assertThat(sourceControlDTO).isNotNull();
  }

  @Test
  public void testAddSourceControlByOwner_licensedByNotifications() {
    setLicensedForSourceControlByNotifications();
    ApiSourceControlDTO sourceControlDTO = sourceControlService.addSourceControlByOwner(
        OwnerType.APPLICATION,
        app.getId(),
        createSourceControlDtoForTesting()
    );
    assertThat(sourceControlDTO).isNotNull();
  }

  @Test
  public void testUpdateSourceControlByOwner_unlicensed() {
    setUnlicensedForSourceControl();
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() ->
            sourceControlService.updateSourceControlByOwner(OwnerType.ORGANIZATION,
            "foo", ApiSourceControlAdapter.convertToDTO(
                    new SourceControl.Builder().setOwnerId(testName.getMethodName()).setRepositoryUrl("bar")
                        .setToken("baz").build())));
  }

  @Test
  public void testUpdateSourceControlByOwner_licensedByAutomation() {
    setLicensedForSourceControlByAutomation();
    testUpdateSourceControlByOwner();
  }

  @Test
  public void testUpdateSourceControlByOwner_licensedByNotifications() {
    setLicensedForSourceControlByNotifications();
    testUpdateSourceControlByOwner();
  }

  private void testUpdateSourceControlByOwner() {
    ApiSourceControlDTO persistedSourceControlDTO = sourceControlService.addSourceControlByOwner(
        OwnerType.APPLICATION,
        app.getId(),
        createSourceControlDtoForTesting()
    );
    assertThat(persistedSourceControlDTO).isNotNull();

    SourceControl sourceControl = sourceControlDAO.getByOwnerId(app.getId());
    final Date pollTime = new Date(System.currentTimeMillis() - 5_000);
    sourceControl.setPullRequestPollTime(pollTime);
    final int errorCount = 2;
    sourceControl.setPullRequestErrorCount(errorCount);
    sourceControlDAO.update(sourceControl);

    persistedSourceControlDTO.token = "newToken";

    ApiSourceControlDTO updatedControlDTO = sourceControlService.updateSourceControlByOwner(
        OwnerType.APPLICATION,
        app.getId(),
        persistedSourceControlDTO
    );

    SourceControl sourceControlAfterUpdate = sourceControlDAO.getByOwnerId(app.getId());

    assertThat(updatedControlDTO).isNotNull();
    assertThat(sourceControlAfterUpdate.getPullRequestPollTime()).isEqualTo(pollTime);
    assertThat(sourceControlAfterUpdate.getPullRequestErrorCount()).isEqualTo(errorCount);
  }

  @Test
  public void testAddSourceControlByOwner_Duplicate() {
    // given an existing source control for an organization
    final ApiSourceControlDTO sourceControl = ApiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(org.getId()).setToken(TOKEN)
            .build());
    sourceControlService.addSourceControlByOwner(
        OwnerType.ORGANIZATION, org.getId(), sourceControl);

    // expect adding another for the same organization gives error
    final ApiSourceControlDTO sourceControlAgain = ApiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(org.getId()).setToken(TOKEN)
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
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() ->
        sourceControlService.getSourceControlByOwner(OwnerType.ORGANIZATION,
            org.getId()));
  }

  @Test
  public void testGetSourceControlByOwnerDecrypted_DoesNotExist() {
    final SourceControl sourceControl = sourceControlService.getSourceControlByOwnerDecrypted("FAKE_ID");
    assertThat(sourceControl).isNull();
  }

  @Test
  public void testAddSourceControlByOwner_PRTelemetry() {
    final Boolean[] booleanOptions = new Boolean[]{true, false, null};
    final String[] branchOptions = new String[]{"branchA", "", null};

    for (final Boolean remediationPullRequestsEnabled : booleanOptions) {
      for (final Boolean statusChecksEnabled : booleanOptions) {
        for (final String baseBranch : branchOptions) {
          final ApiSourceControlDTO validSourceControl = ApiSourceControlAdapter.convertToDTO(
              new SourceControl.Builder().setOwnerId(org.getId()).setToken(TOKEN)
                  .setStatusChecksEnabled(statusChecksEnabled)
                  .setRemediationPullRequestsEnabled(remediationPullRequestsEnabled)
                  .setBaseBranch(baseBranch)
                  .build());
          final Organization tmpOrg = tempEntity.newOrganization();

          final ApiSourceControlDTO sourceControl = sourceControlService.addSourceControlByOwner(
              OwnerType.ORGANIZATION, tmpOrg.getId(), validSourceControl);

          final SourceControl reloaded = sourceControlDAO.getByIdNotNull(sourceControl.id);

          final Map<String, Object> expectedAttributes = new HashMap<>();
          expectedAttributes.put("method", METHOD.ADD);
          expectedAttributes.put("owner_id", HdsClientAnalytics.obfuscate(tmpOrg.getId()));
          expectedAttributes.put("repository_url", HdsClientAnalytics.obfuscate(reloaded.getRepositoryUrl()));
          expectedAttributes.put("provider", null);
          expectedAttributes.put("remediation_pull_requests_enabled", remediationPullRequestsEnabled);
          expectedAttributes.put("status_checks_enabled", statusChecksEnabled);
          expectedAttributes.put("base_branch", baseBranch);

          final ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor =
              ArgumentCaptor.forClass(TelemetryData.class);
          verify(telemetrySenderMock).send(telemetryDataArgumentCaptor.capture());
          final TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();
          assertThat(telemetryData).isNotNull();
          assertThat(telemetryData.getAttributes()).isEqualTo(expectedAttributes);
          reset(telemetrySenderMock);
        }
      }
    }
  }

  @Test
  public void testGetSourceControlByOwnerDecrypted() {
    final SourceControl sourceControl = new SourceControl.Builder().setOwnerId(app.getId()).setRepositoryUrl(VALID_URL)
        .setToken("token").build();
    sourceControlService.encryptToken(sourceControl);
    tempEntity.newSourceControl(sourceControl.getOwnerId(), sourceControl.getRepositoryUrl(), sourceControl.getToken(),
        sourceControl.getProvider());
    final SourceControl sourceControlByApplicationId =
        sourceControlService.getSourceControlByOwnerDecrypted(app.getId());
    assertThat(sourceControlByApplicationId.getOwnerId()).isEqualTo(app.getId());
    assertThat(sourceControlByApplicationId.getRepositoryUrl()).isEqualTo(VALID_URL);
    assertThat(sourceControlByApplicationId.getUsername()).isNull();
    assertThat(sourceControlByApplicationId.getToken()).isEqualTo(TOKEN);
    assertThat(sourceControlByApplicationId.getProvider()).isNull();
  }

  @Test
  public void testGetSourceControlByOwnerDecrypted_NotFound() {
    final SourceControl sourceControlByApplicationId =
        sourceControlService.getSourceControlByOwnerDecrypted("INVALID_ID");
    assertThat(sourceControlByApplicationId).isNull();
  }

  private void assertTelemetry(final METHOD method,
                               final String ownerId,
                               final String repositoryUrl,
                               final String provider,
                               final Boolean remediationPullRequestsEnabled,
                               final Boolean statusChecksEnabled,
                               final String baseBranch)
  {
    final ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock).send(telemetryDataArgumentCaptor.capture());
    final TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();
    final Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("method", method);
    expectedAttributes.put("owner_id", HdsClientAnalytics.obfuscate(ownerId));
    expectedAttributes.put("repository_url", HdsClientAnalytics.obfuscate(repositoryUrl));
    expectedAttributes.put("provider", provider);
    expectedAttributes.put("remediation_pull_requests_enabled", remediationPullRequestsEnabled);
    expectedAttributes.put("status_checks_enabled", statusChecksEnabled);
    expectedAttributes.put("base_branch", baseBranch);
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
            ApiSourceControlAdapter
                .convertToDTO(new SourceControl.Builder().setOwnerId(org.getId()).setToken(TOKEN).build()));
    sourceControl.token = FAKE_SECRET_KEY;
    sourceControlService.updateSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(),
        sourceControl);
    assertThat(sourceControlService.getSourceControlByOwnerDecrypted(org.getId()).getToken())
        .isEqualTo(TOKEN);
  }

  @Test
  public void testGetSourceControlByOwner_RespondsWithEmptyTokenIfNotSet() {
    ApiSourceControlDTO sourceControl = sourceControlService
        .addSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(),
            ApiSourceControlAdapter.convertToDTO(new SourceControl.Builder().setOwnerId(org.getId()).build()));
    sourceControl.token = FAKE_SECRET_KEY;
    sourceControl = sourceControlService.getSourceControlByOwner(OwnerType.ORGANIZATION, org.getId());
    assertThat(sourceControl.token).isEqualTo(null);
  }

  @Test
  public void testAddSourceControlByOwner_RespondsWithEmptyTokenIfNotSet() {
    ApiSourceControlDTO sourceControl =
        ApiSourceControlAdapter.convertToDTO(new SourceControl.Builder().setOwnerId(org.getId()).build());
    sourceControl = sourceControlService.addSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(), sourceControl);
    assertThat(sourceControl.token).isEqualTo(null);
  }

  @Test
  public void testUpdateSourceControlByOwner_RespondsWithEmptyTokenIfNotSet() {
    ApiSourceControlDTO sourceControl = sourceControlService
        .addSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(),
            ApiSourceControlAdapter
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

    // explicit ssh URLs are accepted - user provided
    assertThatNoException().isThrownBy(() -> sourceControlService.validateUrl("ssh://git@server/owner/repo.git"));

    // explicit ssh URLs are accepted - no user provided
    assertThatNoException().isThrownBy(() -> sourceControlService.validateUrl("ssh://server/owner/repo.git"));

    // implicit ssh URLs are accepted
    assertThatNoException().isThrownBy(() -> sourceControlService.validateUrl("git@server:owner/repo.git"));
  }

  @Test
  public void testValidateUrl_UnsupportedUrlFormat() {
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
    //given : Create sourcecontrol, with associated event
    ApiSourceControlDTO persistedSourceControlDTO = sourceControlService.addSourceControlByOwner(
        OwnerType.APPLICATION,
        app.getId(),
        createSourceControlDtoForTesting()
    );
    assertThat(persistedSourceControlDTO).isNotNull();
    tempEntity.newSourceControlEvent(app, new PolicyEvaluation());

    //when : repo url is updated
    persistedSourceControlDTO.repositoryUrl = "http://www.github.com/myOrg/myApp2";
    sourceControlService.updateSourceControlByOwner(
        OwnerType.APPLICATION,
        app.getId(),
        persistedSourceControlDTO
    );

    //then : events are cleared and new event added
    List<SourceControlEvent> events = sourceControlEventDAO.getAllByApplicationId(app.getId());
    assertThat(events).hasSize(1);
    assertThat(events.get(0).getEventType()).isEqualTo(REPOSITORY_URL_UPDATED_EVENT);
    assertThat(events.get(0).getEventPriority()).isEqualTo(EVENT_PRIORITY_HIGHER);
  }

  @Test
  public void testUpdateSourceControlByOwner_doesNotCreateEventWhenUrlNotChanged() {
    //given : Create sourcecontrol, with associated eval and comment
    ApiSourceControlDTO persistedSourceControlDTO = sourceControlService.addSourceControlByOwner(
        OwnerType.APPLICATION,
        app.getId(),
        createSourceControlDtoForTesting()
    );
    assertThat(persistedSourceControlDTO).isNotNull();
    tempEntity.newSourceControlEvent(app, new PolicyEvaluation());

    //when : update with constant repo url
    persistedSourceControlDTO.remediationPullRequestsEnabled = true;
    sourceControlService.updateSourceControlByOwner(
        OwnerType.APPLICATION,
        app.getId(),
        persistedSourceControlDTO
    );

    //then : events are not cleared and new event not added
    List<SourceControlEvent> events = sourceControlEventDAO.getAllByApplicationId(app.getId());
    assertThat(events).hasSize(1);
    assertThat(events.get(0).getEventType()).isEqualTo(DISCOVERED_PULL_REQUEST_EVENT);
    assertThat(events.get(0).getEventPriority()).isEqualTo(EVENT_PRIORITY_NORMAL);
  }

  private ApiSourceControlDTO createSourceControlDtoForTesting() {
    return ApiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder()
            .setOwnerId(testName.getMethodName())
            .setRepositoryUrl("http://www.github.com/myOrg/myApp")
            .setToken("baz")
            .build()
    );
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
}
