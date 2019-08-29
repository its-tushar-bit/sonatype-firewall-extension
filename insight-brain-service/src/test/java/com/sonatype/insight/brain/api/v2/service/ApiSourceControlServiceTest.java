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
    app = tempEntity.newApplicationWithParent();
    org = tempEntity.newOrganization();
  }

  @Test
  public void testAddSourceControl_TokenEncryption() throws Exception {
    ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl(app.getId(), VALID_URL, TOKEN, SourceControlProvider.GITHUB));
    ApiSourceControlDTO sourceControl = sourceControlService.addSourceControl(
        app.getId(), validSourceControl);
    assertThat(sourceControl.token).isNotEqualTo(TOKEN);
    assertThat(sourceControl.token).isEqualTo(SourceControl.FAKE_SECRET_KEY);

    SourceControl reloaded = sourceControlDAO.getByIdNotNull(sourceControl.id);

    String decrypted;
    synchronized (plexusCipher) {
      decrypted = plexusCipher.decrypt(reloaded.getToken(), "CMMDwoV");
    }
    assertThat(decrypted).isEqualTo(TOKEN);
    assertTelemetry(METHOD.ADD, app.getId(), reloaded.getRepositoryUrl(),
        reloaded.getProvider().toString());
  }

  @Test
  public void testUpdateSourceControl_TokenEncryption() throws Exception {
    ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl(app.getId(), VALID_URL, TOKEN, SourceControlProvider.GITHUB)
    );

    ApiSourceControlDTO sourceControl = sourceControlService.addSourceControl(
        app.getId(), validSourceControl);
    sourceControl.token = "updatedToken";
    assertTelemetry(METHOD.ADD, app.getId(), sourceControl.repositoryUrl,
        sourceControl.provider);

    ApiSourceControlDTO updatedScm = sourceControlService.updateSourceControl(
        app.getId(), sourceControl);
    assertThat(updatedScm.token).isEqualTo(SourceControl.FAKE_SECRET_KEY);
    assertTelemetry(METHOD.UPDATE, app.getId(), sourceControl.repositoryUrl,
        sourceControl.provider);

    SourceControl reloaded = sourceControlDAO.getByIdNotNull(sourceControl.id);

    String decrypted;
    synchronized (plexusCipher) {
      decrypted = plexusCipher.decrypt(reloaded.getToken(), "CMMDwoV");
    }
    assertThat(decrypted).isEqualTo("updatedToken");
  }

  @Test
  public void testGetSourceControlDecrypted() {
    ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl(app.getId(), VALID_URL, TOKEN, SourceControlProvider.GITHUB));
    ApiSourceControlDTO sourceControl = sourceControlService.addSourceControl(
        app.getId(), validSourceControl);
    assertThat(sourceControlService.getSourceControlDecrypted(
        app.getId(), sourceControl.id).token).isEqualTo(TOKEN);
  }

  @Test
  public void testGetSourceControlByApplicationIdDecrypted() {
    ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl(app.getId(), VALID_URL, TOKEN, SourceControlProvider.GITHUB));
    sourceControlService.addSourceControl(app.getId(), validSourceControl);
    assertThat(sourceControlService.getSourceControlByApplicationIdDecrypted(
        app.getId()).token).isEqualTo(TOKEN);
  }

  @Test
  public void testGetSourceControlByApplicationIdDecrypted_NotFound() {
    assertThat(sourceControlService.getSourceControlByApplicationIdDecrypted(
        app.getId())).isNull();
  }

  @Test
  public void testDeleteSourceControl() {
    ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl(app.getId(), VALID_URL, TOKEN,
            SourceControlProvider.GITHUB));

    ApiSourceControlDTO sourceControl = sourceControlService.addSourceControl(
        app.getId(), validSourceControl);
    assertThat(sourceControlService.getAll()).hasSize(1);
    assertTelemetry(METHOD.ADD, app.getId(), sourceControl.repositoryUrl,
        sourceControl.provider.toString());

    sourceControlService.deleteSourceControl(app.getId(), sourceControl.id);
    assertThat(sourceControlService.getAll().isEmpty()).isTrue();
    assertTelemetry(METHOD.DELETE, app.getId(), sourceControl.repositoryUrl,
        sourceControl.provider.toString());
  }

  @Test
  public void testUpdateSourceControl_WithFakeToken() {
    ApiSourceControlDTO sourceControl = sourceControlService.addSourceControl(
        app.getId(), apiSourceControlAdapter.convertToDTO(
            new SourceControl(app.getId(), VALID_URL, TOKEN,
                SourceControlProvider.GITHUB)));
    sourceControl.token = SourceControl.FAKE_SECRET_KEY;
    sourceControlService.updateSourceControl(app.getId(), sourceControl);
    assertThat(sourceControlService.getSourceControlDecrypted(app.getId(),
        sourceControl.id).token).isEqualTo(TOKEN);
  }

  @Test
  public void testUpdateSourceControl_WithEmptyToken() {
    ApiSourceControlDTO sourceControl = sourceControlService.addSourceControl(
        app.getId(), apiSourceControlAdapter.convertToDTO(
            new SourceControl(app.getId(), VALID_URL, TOKEN,
                SourceControlProvider.GITHUB)));
    sourceControl.token = null;
    sourceControlService.updateSourceControl(app.getId(), sourceControl);
    assertThat(sourceControlService.getSourceControlDecrypted(app.getId(),
        sourceControl.id).token).isEqualTo(TOKEN);
  }

  @Test
  public void testUpdateSourceControl_WrongAppId() {
    ApiSourceControlDTO sourceControl = apiSourceControlAdapter.convertToDTO(
        tempEntity.newSourceControl(app.getId(), VALID_URL, "token",
            SourceControlProvider.GITHUB));
    sourceControl.ownerId = "foo";
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() ->
        sourceControlService.updateSourceControl(app.getId(), sourceControl)
    ).withMessage("Cannot find SourceControl with id: "
        + sourceControl.id + " for application with id: " + app.getId());
  }

  @Test
  public void testAddSourceControl_unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.NOTIFICATIONS);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> sourceControlService.addSourceControl(
            "foo", apiSourceControlAdapter.convertToDTO(
                new SourceControl(testName.getMethodName(), "bar", "baz",
                    SourceControlProvider.GITHUB))));
  }

  @Test
  public void testUpdateSourceControl_unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.NOTIFICATIONS);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() ->
            sourceControlService.updateSourceControl("foo",
                apiSourceControlAdapter.convertToDTO(new SourceControl(
                    testName.getMethodName(), "bar", "baz",
                    SourceControlProvider.GITHUB))));
  }

  @Test
  public void testDeleteSourceControl_unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.NOTIFICATIONS);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> sourceControlService.deleteSourceControl("foo", "bar"));
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
  public void testGetSourceControlByApplicationId() {
    tempEntity.newSourceControl(app.getId(), VALID_URL, "token", SourceControlProvider.GITHUB);
    ApiSourceControlDTO sourceControlByApplicationId = sourceControlService
        .getSourceControlByApplicationId(app.getId());
    assertThat(sourceControlByApplicationId.id).isEqualTo(
        sourceControlByApplicationId.id);
    assertThat(sourceControlByApplicationId.repositoryUrl)
        .isEqualTo(sourceControlByApplicationId.repositoryUrl);
    assertThat(sourceControlByApplicationId.token).isEqualTo(SourceControl.FAKE_SECRET_KEY);
  }

  @Test
  public void testGetSourceControlByApplicationId_AppDoesNotExist() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() ->
        sourceControlService.getSourceControlByApplicationId(app.getId()));
  }

  @Test
  public void testGetSourceControlByApplicationIdDecrypted_AppDoesNotExist() {
    ApiSourceControlDTO sourceControl = sourceControlService
        .getSourceControlByApplicationIdDecrypted(app.getId());
    assertThat(sourceControl).isNull();
  }

  @Test
  public void testAddSourceControl_NoSourceControlProviderProvided() {
    ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl(app.getId(), VALID_URL, TOKEN, null));
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> sourceControlService.addSourceControl(app.getId(), validSourceControl));

  }

  @Test
  public void testUpdateSourceControl_NoSourceControlProviderProvided() {
    ApiSourceControlDTO validSourceControl = sourceControlService.addSourceControl(
        app.getId(), apiSourceControlAdapter.convertToDTO(new SourceControl(
            app.getId(), VALID_URL, TOKEN, SourceControlProvider.GITLAB)));
    validSourceControl.provider = null;
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> sourceControlService.addSourceControl(app.getId(), validSourceControl));
  }

  @Test
  public void testAddOrUpdateSourceControl_AddNew() {
    // add org record
    sourceControlService.addSourceControl(
        app.getOrganizationId(), apiSourceControlAdapter.convertToDTO(new SourceControl(
            app.getOrganizationId(), null, TOKEN, SourceControlProvider.GITHUB)));
    String repoUrl = "https://example.com/org/proj";
    sourceControlService.addOrUpdateSourceControl(app.getPublicId(), repoUrl);
    assertThat(sourceControlService.getSourceControlByApplicationId(app.getId()).repositoryUrl)
        .isEqualTo(repoUrl);
  }

  @Test
  public void testAddOrUpdateSourceControl_Update() {
    ApiSourceControlDTO sourceControl = sourceControlService.addSourceControl(
        app.getId(), apiSourceControlAdapter.convertToDTO(new SourceControl(
            app.getId(), VALID_URL, TOKEN, SourceControlProvider.GITHUB)));
    String repoUrl = "https://example.com/org/proj";
    sourceControlService.addOrUpdateSourceControl(app.getPublicId(), repoUrl);
    assertThat(sourceControlService.getSourceControlDecrypted(app.getId(), sourceControl.id).repositoryUrl)
        .isEqualTo(repoUrl);
  }

  @Test
  public void testAddOrUpdateSourceControl_InvalidPublicId() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> sourceControlService.addOrUpdateSourceControl("abcdefg", "https://e.com/org/proj"));
  }

  @Test
  public void testAddOrUpdateSourceControl_InvalidRepoUrl() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> sourceControlService.addOrUpdateSourceControl(app.getPublicId(), "https://not valid"));
  }

  @Test
  public void testAddSourceControlByOwner_TokenEncryption() throws Exception {
    ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl(org.getId(), null, TOKEN, SourceControlProvider.GITHUB));
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
        reloaded.getProvider().toString());
  }

  @Test
  public void testUpdateSourceControlByOwner_TokenEncryption() throws Exception {
    ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl(org.getId(), null, TOKEN,
            SourceControlProvider.GITHUB)
    );

    ApiSourceControlDTO sourceControl =
        sourceControlService.addSourceControlByOwner(OwnerType.ORGANIZATION,
            org.getId(), validSourceControl);
    sourceControl.token = "updatedToken";
    assertTelemetry(METHOD.ADD, org.getId(), sourceControl.repositoryUrl,
        sourceControl.provider);

    ApiSourceControlDTO updatedScm =
        sourceControlService.updateSourceControlByOwner(OwnerType.ORGANIZATION,
            org.getId(), sourceControl);
    assertThat(updatedScm.token).isEqualTo(SourceControl.FAKE_SECRET_KEY);
    assertTelemetry(METHOD.UPDATE, org.getId(), sourceControl.repositoryUrl,
        sourceControl.provider);

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
        new SourceControl(org.getId(), null, TOKEN,
            SourceControlProvider.GITHUB));
    ApiSourceControlDTO sourceControl =
        sourceControlService.addSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(),
            validSourceControl);
    assertThat(sourceControlService.getSourceControlByOwnerDecrypted(
        OwnerType.ORGANIZATION, org.getId(), sourceControl.id).token).isEqualTo(TOKEN);
  }

  @Test
  public void testGetSourceControlByOwnerDecrypted_ForApplication() {
    ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl(app.getId(), VALID_URL, TOKEN,
            SourceControlProvider.GITHUB));
    ApiSourceControlDTO sourceControl =
        sourceControlService.addSourceControlByOwner(OwnerType.ORGANIZATION, app.getId(),
            validSourceControl);
    assertThat(sourceControlService.getSourceControlByOwnerDecrypted(
        OwnerType.ORGANIZATION, app.getId(), sourceControl.id).token).isEqualTo(TOKEN);
  }

  @Test
  public void testDeleteSourceControlByOwner_ForOrganization() {
    ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl(org.getId(), null, TOKEN,
            SourceControlProvider.GITHUB));

    ApiSourceControlDTO sourceControl = sourceControlService
        .addSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(), validSourceControl);
    assertThat(sourceControlService.getAll()).hasSize(1);
    assertTelemetry(METHOD.ADD, org.getId(), sourceControl.repositoryUrl,
        sourceControl.provider);

    sourceControlService.deleteSourceControlByOwner(OwnerType.ORGANIZATION,
        org.getId(), sourceControl.id);
    assertThat(sourceControlService.getAll().isEmpty()).isTrue();
    assertTelemetry(METHOD.DELETE, org.getId(), sourceControl.repositoryUrl,
        sourceControl.provider);
  }

  @Test
  public void testDeleteSourceControlByOwner_ForApplication() {
    ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl(app.getId(), VALID_URL, TOKEN,
            SourceControlProvider.GITHUB));

    ApiSourceControlDTO sourceControl = sourceControlService
        .addSourceControlByOwner(OwnerType.ORGANIZATION, app.getId(), validSourceControl);
    assertThat(sourceControlService.getAll()).hasSize(1);
    assertTelemetry(METHOD.ADD, app.getId(), sourceControl.repositoryUrl,
        sourceControl.provider);

    sourceControlService.deleteSourceControlByOwner(OwnerType.ORGANIZATION,
        app.getId(), sourceControl.id);
    assertThat(sourceControlService.getAll().isEmpty()).isTrue();
    assertTelemetry(METHOD.DELETE, app.getId(), sourceControl.repositoryUrl,
        sourceControl.provider);
  }

  @Test
  public void testUpdateSourceControlByOwner_WithFakeToken() {
    ApiSourceControlDTO sourceControl = sourceControlService
        .addSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(),
            apiSourceControlAdapter.convertToDTO(new SourceControl(
                org.getId(), null, TOKEN, SourceControlProvider.GITHUB)));
    sourceControl.token = SourceControl.FAKE_SECRET_KEY;
    sourceControlService.updateSourceControl(org.getId(), sourceControl);
    assertThat(sourceControlService.getSourceControlByOwnerDecrypted(
        OwnerType.ORGANIZATION, org.getId(), sourceControl.id).token).isEqualTo(TOKEN);
  }

  @Test
  public void testUpdateSourceControlByOwner_WithEmptyToken() {
    ApiSourceControlDTO sourceControl = sourceControlService
        .addSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(),
            apiSourceControlAdapter.convertToDTO(new SourceControl(
                org.getId(), null, TOKEN,
                SourceControlProvider.GITHUB)));
    sourceControl.token = null;
    sourceControlService.updateSourceControlByOwner(OwnerType.ORGANIZATION, org.getId(),
        sourceControl);
    assertThat(sourceControlService.getSourceControlByOwnerDecrypted(
        OwnerType.ORGANIZATION, org.getId(), sourceControl.id).token).isEqualTo(TOKEN);
  }

  @Test
  public void testUpdateSourceControlByOwner_WrongOwnerId() {
    ApiSourceControlDTO sourceControl = apiSourceControlAdapter.convertToDTO(
        tempEntity.newSourceControl(org.getId(), null, "token",
            SourceControlProvider.GITHUB));
    sourceControl.ownerId = "foo";
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() ->
        sourceControlService.updateSourceControlByOwner(OwnerType.ORGANIZATION,
            org.getId(), sourceControl)
    ).withMessage(String.format(
        "Cannot find SourceControl with id: %s for organization with id: %s",
        sourceControl.id, org.getId()));
  }

  @Test
  public void testAddSourceControlByOwner_unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.NOTIFICATIONS);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> sourceControlService.addSourceControlByOwner(
            OwnerType.ORGANIZATION, "foo", apiSourceControlAdapter.convertToDTO(
                new SourceControl(testName.getMethodName(), "bar", "baz",
                    SourceControlProvider.GITHUB))));
  }

  @Test
  public void testUpdateSourceControlByOwner_unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.NOTIFICATIONS);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() ->
            sourceControlService.updateSourceControlByOwner(OwnerType.ORGANIZATION,
                "foo", apiSourceControlAdapter.convertToDTO(new SourceControl(
                    testName.getMethodName(), "bar", "baz",
                    SourceControlProvider.GITHUB))));
  }

  @Test
  public void testAddSourceControlByOwner_Duplicate() {
    // given an existing source control for an organization
    ApiSourceControlDTO sourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl(org.getId(), null, TOKEN, SourceControlProvider.GITHUB));
    sourceControlService.addSourceControlByOwner(
        OwnerType.ORGANIZATION, org.getId(), sourceControl);

    // expect adding another for the same organization gives error
    ApiSourceControlDTO sourceControlAgain = apiSourceControlAdapter.convertToDTO(
        new SourceControl(org.getId(), null, TOKEN, SourceControlProvider.GITHUB));
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
        .isThrownBy(() -> sourceControlService.deleteSourceControlByOwner(
            OwnerType.ORGANIZATION, "foo", "bar"));
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
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() ->
        sourceControlService.getSourceControlByOwnerDecrypted(OwnerType.ORGANIZATION,
            org.getId(), "not there"));
  }

  @Test
  public void testAddSourceControlByOwner_NoSourceControlProviderProvided() {
    ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl(org.getId(), null, TOKEN, null));
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> sourceControlService.addSourceControlByOwner(
            OwnerType.ORGANIZATION, org.getId(), validSourceControl));
  }

  @Test
  public void testUpdateSourceControlByOwner_NoSourceControlProviderProvided() {
    ApiSourceControlDTO validSourceControl =
        sourceControlService.addSourceControlByOwner(OwnerType.ORGANIZATION,
            org.getId(), apiSourceControlAdapter.convertToDTO(new SourceControl(
                org.getId(), null, TOKEN, SourceControlProvider.GITLAB)));
    validSourceControl.provider = null;

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> sourceControlService.updateSourceControlByOwner(
            OwnerType.ORGANIZATION, org.getId(), validSourceControl));
  }

  @Test
  public void testPopulateProviderAndTokenFromOrganizationIfNeeded_ProviderAndTokenAlreadyPopulated() {
    ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl(app.getId(), VALID_URL, TOKEN, SourceControlProvider.GITHUB));

    ApiSourceControlDTO value =
        sourceControlService.populateProviderAndTokenFromOrganizationIfNeeded(validSourceControl);

    assertThat(value.token).isEqualTo(TOKEN);
    assertThat(value.provider).isEqualTo(SourceControlProvider.GITHUB.toString());
  }

  @Test
  public void testPopulateProviderAndTokenFromOrganizationIfNeeded_NoProviderAndTokenApplicationNotFound() {
    ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl("INVALID_ID", VALID_URL, null, null));

    ApiSourceControlDTO value =
        sourceControlService.populateProviderAndTokenFromOrganizationIfNeeded(validSourceControl);

    assertThat(value.token).isEqualTo(null);
    assertThat(value.provider).isEqualTo(null);
  }

  @Test
  public void testPopulateProviderAndTokenFromOrganizationIfNeeded_NoProviderAndTokenOrgSourceControlNotFound() {
    ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl(app.getId(), VALID_URL, null, null));

    ApiSourceControlDTO value =
        sourceControlService.populateProviderAndTokenFromOrganizationIfNeeded(validSourceControl);

    assertThat(value.token).isEqualTo(null);
    assertThat(value.provider).isEqualTo(null);
  }

  @Test
  public void testPopulateProviderAndTokenFromOrganizationIfNeeded_PopulateFromOrgSourceControl() {
    ApiSourceControlDTO validSourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl(app.getId(), VALID_URL, null, null));
    ApiSourceControlDTO orgSourceControlDto = apiSourceControlAdapter.convertToDTO(
        new SourceControl(org.getId(), null, TOKEN, SourceControlProvider.GITHUB));
    sourceControlService.addSourceControlByOwner(OwnerType.ORGANIZATION, app.getOrganizationId(), orgSourceControlDto);

    ApiSourceControlDTO value =
        sourceControlService.populateProviderAndTokenFromOrganizationIfNeeded(validSourceControl);

    assertThat(value.token).isEqualTo(TOKEN);
    assertThat(value.provider).isEqualTo(SourceControlProvider.GITHUB.toString());
  }

  private void assertTelemetry(final METHOD method,
                               final String ownerId,
                               final String repositoryUrl,
                               final String provider)
  {
    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock).send(telemetryDataArgumentCaptor.capture());
    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();
    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("method", method);
    expectedAttributes.put("owner_id", HdsClientAnalytics.obfuscate(ownerId));
    expectedAttributes.put("repository_url", HdsClientAnalytics.obfuscate(repositoryUrl));
    expectedAttributes.put("provider", provider);
    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SOURCE_CONTROL);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    assertThat(telemetryData.getAttributes()).isEqualTo(expectedAttributes);
    reset(telemetrySenderMock);
  }
}
