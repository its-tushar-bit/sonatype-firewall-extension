/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiSourceControlDTO;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlService.METHOD;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticSourceControlConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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

  @Mock
  private TelemetrySender telemetrySenderMock;

  private SourceControlDAO sourceControlDAO = new SourceControlDAO();

  private ApiSourceControlAdapter apiSourceControlAdapter =
      new ApiSourceControlAdapter();

  private Application app;

  private Organization org;

  @Override
  public void configure(Binder binder) {
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
    super.configure(binder);
  }

  @Before
  public void setup() {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());
  }

  @Test
  public void testGetAll_unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.NOTIFICATIONS);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> sourceControlService.getAll());
  }

  @Test
  public void testGetAll() throws Exception {
    List<ApiSourceControlDTO> expected = Arrays.asList(
        tempEntity.newSourceControl(app.getId(), VALID_URL, TOKEN,
            SourceControlProvider.GITHUB),
        tempEntity.newSourceControl(org.getId(), null, TOKEN,
            SourceControlProvider.GITHUB)).stream()
        .map(apiSourceControlAdapter::convertToDTO)
        .collect(Collectors.toList());

    // decrypt retrieved tokens for comparison
    List<ApiSourceControlDTO> retrieved = sourceControlService.getAll();
    for (ApiSourceControlDTO it: retrieved) {
      synchronized (plexusCipher) {
        it.token = plexusCipher.decrypt(it.token, "CMMDwoV");
      }
    }

    assertThat(retrieved).hasSize(2);
    assertThat(JsonUtils.format(retrieved)).isEqualTo(JsonUtils.format(expected));
  }

  @Test
  public void testAddOrUpdateSourceControl_InvalidPublicId() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> sourceControlService.addOrUpdateSourceControl("abcdefg", "https://e.com/org/proj"));
  }

  @Test
  public void testAddOrUpdateSourceControl_InvalidRepoUrl() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> testAddOrUpdateSourceControl_AutomaticSourceControl(true, "https://github.com/org/a",
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
        "https://github.com/org/b", "https://github.com/org/b");
  }

  private void testAddOrUpdateSourceControl_AutomaticSourceControl(
      boolean enabled,
      String initialUrl,
      String collectedUrl,
      String expectedUrl)
  {
    // add ROOT ORGANIZATION record
    tempEntity.newSourceControl(Organization.ROOT_ORGANIZATION_ID, null, TOKEN,
        SourceControlProvider.GITHUB);

    // add application record, if needed
    if (initialUrl != null) {
      tempEntity.newSourceControl(app.getId(), initialUrl, null, null);
    }

    // try automatic scm
    AutomaticSourceControlConfigurationDAO automaticSourceControlConfigurationDAO =
        new AutomaticSourceControlConfigurationDAO();
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(enabled);

    ApiSourceControlDTO result =
        sourceControlService.addOrUpdateSourceControl(app.getPublicId(), collectedUrl);
    if (!enabled && initialUrl == null) {
      assertThat(result).isNull();
    }
    else {
      assertThat(result.ownerId).isEqualTo(app.getId());
      assertThat(result.repositoryUrl).isEqualTo(expectedUrl);
    }
  }

  @Test
  public void testAddSourceControlByOwner_TokenEncryption() throws Exception {
    ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(org.getId()).setToken(TOKEN)
            .setProvider(SourceControlProvider.GITHUB).build());
    ApiSourceControlDTO sourceControl = sourceControlService.addSourceControlByOwner(
        OwnerType.ORGANIZATION, org.getId(), validSourceControl);
    assertThat(sourceControl.token).isNotEqualTo(TOKEN);
    assertThat(sourceControl.token).isEqualTo(SourceControl.FAKE_SECRET_KEY);

    SourceControl reloaded = sourceControlDAO.getByIdNotNull(sourceControl.id);

    String decrypted;
    synchronized (plexusCipher) {
      decrypted = plexusCipher.decrypt(reloaded.getToken(), "CMMDwoV");
    }
    assertThat(decrypted).isEqualTo(TOKEN);
    assertTelemetry(METHOD.ADD, org.getId(), reloaded.getRepositoryUrl(),
        reloaded.getProvider().toString(), reloaded.getEnablePullRequests(), reloaded.getEnableStatusChecks(),
        reloaded.getBaseBranch());
  }

  @Test
  public void testUpdateSourceControlByOwner_TokenEncryption() throws Exception {
    ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(org.getId()).setToken(TOKEN)
            .setProvider(SourceControlProvider.GITHUB).build()
    );

    ApiSourceControlDTO sourceControl =
        sourceControlService.addSourceControlByOwner(OwnerType.ORGANIZATION,
            org.getId(), validSourceControl);
    sourceControl.token = "updatedToken";
    assertTelemetry(METHOD.ADD, org.getId(), sourceControl.repositoryUrl,
        sourceControl.provider, sourceControl.enablePullRequests, sourceControl.enablePullRequests,
        sourceControl.baseBranch);

    ApiSourceControlDTO updatedScm =
        sourceControlService.updateSourceControlByOwner(OwnerType.ORGANIZATION,
            org.getId(), sourceControl);
    assertThat(updatedScm.token).isEqualTo(SourceControl.FAKE_SECRET_KEY);
    assertTelemetry(METHOD.UPDATE, org.getId(), sourceControl.repositoryUrl,
        sourceControl.provider, sourceControl.enablePullRequests, sourceControl.enablePullRequests,
        sourceControl.baseBranch);

    SourceControl reloaded = sourceControlDAO.getByIdNotNull(sourceControl.id);

    String decrypted;
    synchronized (plexusCipher) {
      decrypted = plexusCipher.decrypt(reloaded.getToken(), "CMMDwoV");
    }
    assertThat(decrypted).isEqualTo("updatedToken");
  }

  @Test
  public void testGetSourceControlByOwnerDecrypted_ForOrganization() {
    ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(org.getId()).setToken(TOKEN)
            .setProvider(SourceControlProvider.GITHUB).build());
    sourceControlService.addSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(),
            validSourceControl);
    assertThat(sourceControlService.getSourceControlByOwnerDecrypted(
        OwnerType.ORGANIZATION, org.getId()).getToken()).isEqualTo(TOKEN);
  }

  @Test
  public void testGetSourceControlByOwnerDecrypted_ForApplication() {
    ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(app.getId()).setRepositoryUrl(VALID_URL).setToken(TOKEN)
            .setProvider(SourceControlProvider.GITHUB).build());
    sourceControlService.addSourceControlByOwner(OwnerType.ORGANIZATION, app.getId(), validSourceControl);
    assertThat(sourceControlService.getSourceControlByOwnerDecrypted(OwnerType.ORGANIZATION, app.getId()).getToken())
        .isEqualTo(TOKEN);
  }

  @Test
  public void testDeleteSourceControlByOwner_ForOrganization() {
    ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(org.getId()).setToken(TOKEN)
            .setProvider(SourceControlProvider.GITHUB).build());

    ApiSourceControlDTO sourceControl = sourceControlService
        .addSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(), validSourceControl);
    assertThat(sourceControlService.getAll()).hasSize(1);
    assertTelemetry(METHOD.ADD, org.getId(), sourceControl.repositoryUrl, sourceControl.provider,
        sourceControl.enablePullRequests, sourceControl.enableStatusChecks,
        sourceControl.baseBranch);

    sourceControlService.deleteSourceControlByOwner(OwnerType.ORGANIZATION, org.getId());
    assertThat(sourceControlService.getAll().isEmpty()).isTrue();
    assertTelemetry(METHOD.DELETE, org.getId(), sourceControl.repositoryUrl, sourceControl.provider,
        sourceControl.enablePullRequests, sourceControl.enableStatusChecks,
        sourceControl.baseBranch);
  }

  @Test
  public void testDeleteSourceControlByOwner_ForApplication() {
    ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(app.getId()).setRepositoryUrl(VALID_URL).setToken(TOKEN)
            .setProvider(SourceControlProvider.GITHUB).build());

    ApiSourceControlDTO sourceControl = sourceControlService
        .addSourceControlByOwner(OwnerType.ORGANIZATION, app.getId(), validSourceControl);
    assertThat(sourceControlService.getAll()).hasSize(1);
    assertTelemetry(METHOD.ADD, app.getId(), sourceControl.repositoryUrl,
        sourceControl.provider, sourceControl.enablePullRequests, sourceControl.enableStatusChecks,
        sourceControl.baseBranch);

    sourceControlService.deleteSourceControlByOwner(OwnerType.ORGANIZATION, app.getId());
    assertThat(sourceControlService.getAll().isEmpty()).isTrue();
    assertTelemetry(METHOD.DELETE, app.getId(), sourceControl.repositoryUrl,
        sourceControl.provider, sourceControl.enablePullRequests, sourceControl.enableStatusChecks,
        sourceControl.baseBranch);
  }

  @Test
  public void testUpdateSourceControlByOwner_WithFakeToken() {
    ApiSourceControlDTO sourceControl = sourceControlService
        .addSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(),
            apiSourceControlAdapter.convertToDTO(
                new SourceControl.Builder().setOwnerId(org.getId()).setToken(TOKEN)
                    .setProvider(SourceControlProvider.GITHUB).build()));
    sourceControl.token = SourceControl.FAKE_SECRET_KEY;
    sourceControlService.updateSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(), sourceControl);
    assertThat(sourceControlService.getSourceControlByOwnerDecrypted(OwnerType.ORGANIZATION, org.getId()).getToken())
        .isEqualTo(TOKEN);
  }

  @Test
  public void testUpdateSourceControlByOwner_WithEmptyToken() {
    ApiSourceControlDTO sourceControl = sourceControlService
        .addSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(),
            apiSourceControlAdapter.convertToDTO(
                new SourceControl.Builder().setOwnerId(org.getId()).setToken(TOKEN)
                    .setProvider(SourceControlProvider.GITHUB).build()));
    sourceControl.token = null;
    sourceControlService.updateSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(),
        sourceControl);
    assertThat(sourceControlService.getSourceControlByOwnerDecrypted(OwnerType.ORGANIZATION, org.getId()).getToken())
        .isEqualTo(TOKEN);
  }

  @Test
  public void testUpdateSourceControlByOwner_WrongOwnerId() {
    ApiSourceControlDTO sourceControl = apiSourceControlAdapter.convertToDTO(
        tempEntity.newSourceControl(org.getId(), null, "token",
            SourceControlProvider.GITHUB));
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() ->
        sourceControlService.updateSourceControlByOwner(OwnerType.ORGANIZATION,
            "foo", sourceControl)
    ).withMessage(String.format(
        "Cannot find SourceControl for organization with id: foo"));
  }

  @Test
  public void testAddSourceControlByOwner_unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.NOTIFICATIONS);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> sourceControlService.addSourceControlByOwner(
            OwnerType.ORGANIZATION, "foo", apiSourceControlAdapter.convertToDTO(
                new SourceControl.Builder().setOwnerId(testName.getMethodName()).setRepositoryUrl("bar").setToken("baz")
                    .setProvider(SourceControlProvider.GITHUB).build())));
  }

  @Test
  public void testUpdateSourceControlByOwner_unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.NOTIFICATIONS);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() ->
            sourceControlService.updateSourceControlByOwner(OwnerType.ORGANIZATION,
                "foo", apiSourceControlAdapter.convertToDTO(
                    new SourceControl.Builder().setOwnerId(testName.getMethodName()).setRepositoryUrl("bar")
                        .setToken("baz").setProvider(SourceControlProvider.GITHUB).build())));
  }

  @Test
  public void testAddSourceControlByOwner_Duplicate() {
    // given an existing source control for an organization
    ApiSourceControlDTO sourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(org.getId()).setToken(TOKEN)
            .setProvider(SourceControlProvider.GITHUB).build());
    sourceControlService.addSourceControlByOwner(
        OwnerType.ORGANIZATION, org.getId(), sourceControl);

    // expect adding another for the same organization gives error
    ApiSourceControlDTO sourceControlAgain = apiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(org.getId()).setToken(TOKEN)
            .setProvider(SourceControlProvider.GITHUB).build());
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> sourceControlService.addSourceControlByOwner(
            OwnerType.ORGANIZATION, org.getId(), sourceControlAgain))
        .withMessageContaining(
            "SourceControl already exists for organization with id: " + org.getId());
  }

  @Test
  public void testDeleteSourceControlByOwner_unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.NOTIFICATIONS);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> sourceControlService.deleteSourceControlByOwner(OwnerType.ORGANIZATION, "foo"));
  }

  @Test
  public void testGetSourceControlByOwnerId_ForOrganization() {
    tempEntity.newSourceControl(org.getId(), null, "token",
        SourceControlProvider.GITHUB);
    ApiSourceControlDTO sourceControlByApplicationId = sourceControlService
        .getSourceControlByOwner(OwnerType.ORGANIZATION, org.getId());
    assertThat(sourceControlByApplicationId.id).isEqualTo(
        sourceControlByApplicationId.id);
    assertThat(sourceControlByApplicationId.repositoryUrl)
        .isEqualTo(sourceControlByApplicationId.repositoryUrl);
    assertThat(sourceControlByApplicationId.token).isEqualTo(SourceControl.FAKE_SECRET_KEY);
  }

  @Test
  public void testGetSourceControlByOwnerId_ForApplication() {
    tempEntity.newSourceControl(app.getId(), VALID_URL, "token",
        SourceControlProvider.GITHUB);
    ApiSourceControlDTO sourceControlByApplicationId = sourceControlService
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
    SourceControl sourceControl =
        sourceControlService.getSourceControlByOwnerDecrypted(OwnerType.ORGANIZATION, "FAKE_ID");
    assertThat(sourceControl).isNull();
  }

  @Test
  public void testAddSourceControlByOwner_NoSourceControlProviderProvided() {
    ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(org.getId()).setRepositoryUrl(null).setToken(TOKEN).build());
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> sourceControlService.addSourceControlByOwner(
            OwnerType.ORGANIZATION, org.getId(), validSourceControl));
  }

  @Test
  public void testUpdateSourceControlByOwner_NoSourceControlProviderProvided() {
    ApiSourceControlDTO validSourceControl =
        sourceControlService.addSourceControlByOwner(OwnerType.ORGANIZATION,
            org.getId(), apiSourceControlAdapter.convertToDTO(
                new SourceControl.Builder().setOwnerId(org.getId()).setToken(TOKEN)
                    .setProvider(SourceControlProvider.GITLAB).build()));
    validSourceControl.provider = null;

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> sourceControlService.updateSourceControlByOwner(
            OwnerType.ORGANIZATION, org.getId(), validSourceControl));
  }

  @Test
  public void testAddSourceControlByOwner_PRTelemetry() throws Exception {
    Boolean[] booleanOptions = new Boolean[]{true, false, null};
    String[] branchOptions = new String[]{"branchA", "", null};

    for (Boolean enablePullRequest : booleanOptions) {
      for (Boolean enableStatusChecks : booleanOptions) {
        for (String baseBranch : branchOptions) {
          ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
              new SourceControl.Builder().setOwnerId(org.getId()).setToken(TOKEN)
                  .setProvider(SourceControlProvider.GITHUB)
                  .setEnableStatusChecks(enableStatusChecks)
                  .setEnablePullRequests(enablePullRequest)
                  .setBaseBranch(baseBranch)
                  .build());
          Organization tmpOrg = tempEntity.newOrganization();

          ApiSourceControlDTO sourceControl = sourceControlService.addSourceControlByOwner(
              OwnerType.ORGANIZATION, tmpOrg.getId(), validSourceControl);

          SourceControl reloaded = sourceControlDAO.getByIdNotNull(sourceControl.id);

          Map<String, Object> expectedAttributes = new HashMap<>();
          expectedAttributes.put("method", METHOD.ADD);
          expectedAttributes.put("owner_id", HdsClientAnalytics.obfuscate(tmpOrg.getId()));
          expectedAttributes.put("repository_url", HdsClientAnalytics.obfuscate(reloaded.getRepositoryUrl()));
          expectedAttributes.put("provider", SourceControlProvider.GITHUB.toString());
          expectedAttributes.put("enable_pull_requests", enablePullRequest);
          expectedAttributes.put("enable_status_checks", enableStatusChecks);
          expectedAttributes.put("base_branch", baseBranch);

          ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
          verify(telemetrySenderMock).send(telemetryDataArgumentCaptor.capture());
          TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();
          assertThat(telemetryData).isNotNull();
          assertThat(telemetryData.getAttributes()).isEqualTo(expectedAttributes);
          reset(telemetrySenderMock);
        }
      }
    }
  }

  @Test
  public void testGetSourceControlByOwnerDecrypted() {
    SourceControl sourceControl =
        new SourceControl.Builder().setOwnerId(app.getId()).setRepositoryUrl(VALID_URL).setToken("token")
            .setProvider(SourceControlProvider.GITHUB).build();
    sourceControlService.encryptToken(sourceControl);
    tempEntity.newSourceControl(sourceControl.getOwnerId(), sourceControl.getRepositoryUrl(), sourceControl.getToken(),
        sourceControl.getProvider());
    SourceControl sourceControlByApplicationId = sourceControlService
        .getSourceControlByOwnerDecrypted(OwnerType.APPLICATION, app.getId());
    assertThat(sourceControlByApplicationId.getOwnerId()).isEqualTo(app.getId());
    assertThat(sourceControlByApplicationId.getRepositoryUrl()).isEqualTo(VALID_URL);
    assertThat(sourceControlByApplicationId.getToken()).isEqualTo(TOKEN);
    assertThat(sourceControlByApplicationId.getProvider()).isEqualTo(SourceControlProvider.GITHUB);
  }

  @Test
  public void testGetSourceControlByOwnerDecrypted_NotFound() {
    SourceControl sourceControlByApplicationId = sourceControlService
        .getSourceControlByOwnerDecrypted(OwnerType.APPLICATION, "INVALID_ID");
    assertThat(sourceControlByApplicationId).isNull();
  }

  private void assertTelemetry(final METHOD method,
                               final String ownerId,
                               final String repositoryUrl,
                               final String provider,
                               final Boolean enablePullRequests,
                               final Boolean enableStatusChecks,
                               final String baseBranch)
  {
    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock).send(telemetryDataArgumentCaptor.capture());
    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();
    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("method", method);
    expectedAttributes.put("owner_id", HdsClientAnalytics.obfuscate(ownerId));
    expectedAttributes.put("repository_url", HdsClientAnalytics.obfuscate(repositoryUrl));
    expectedAttributes.put("provider", provider);
    expectedAttributes.put("enable_pull_requests", enablePullRequests);
    expectedAttributes.put("enable_status_checks", enableStatusChecks);
    expectedAttributes.put("base_branch", baseBranch);
    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SOURCE_CONTROL);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    assertThat(telemetryData.getAttributes()).isEqualTo(expectedAttributes);
    reset(telemetrySenderMock);
  }
}
