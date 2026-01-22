/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiPullRequestResults;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiSourceControlDTO;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticSourceControlConfigurationDAO;
import com.sonatype.insight.brain.git.GitClientFactory;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.security.TestEncryptionKeyStore;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;

import com.google.inject.Binder;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * @since 1.66
 */
public class ApiSourceControlServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private static final String VALID_URL = "https://example.com/organization/project";

  private static final String TOKEN = new String(
      new PasswordHandler(new TestEncryptionKeyStore())
          .encryptPassword("token".toCharArray())
  );

  @Before
  public void before() {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    lenient().when(mockGitClientFactory.createApiClient(any())).thenReturn(mock(GitApiClient.class));
  }

  @Inject
  public ApiSourceControlService sourceControlService;

  @Inject
  private AutomaticSourceControlConfigurationDAO automaticSourceControlConfigurationDAO;

  @Inject
  private ApiSourceControlAdapter apiSourceControlAdapter;

  @Mock
  private GitClientFactory mockGitClientFactory;

  @Override
  public void configure(final Binder binder) {
    binder.bind(GitClientFactory.class).toInstance(mockGitClientFactory);
    super.configure(binder);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetAll_Unauthenticated() {
    sourceControlService.getAll();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetAll_Unauthorized() {
    login();
    sourceControlService.getAll();
  }

  @Test
  public void testGetAll_Authorized() {
    grantGlobalPermission(Permission.READ);
    assertThat(sourceControlService.getAll()).hasSize(1);
  }

  @Test
  public void testGetSourceControlByOwner_Authorized() {
    grantReadPermission(app.getId());
    SourceControl sourceControl = tempEntity.newSourceControl(
        app.getId(), VALID_URL, "token", null);
    ApiSourceControlDTO sourceControlByApplicationId =
        sourceControlService.getSourceControlByOwner(
            OwnerType.APPLICATION, app.getId());
    assertThat(sourceControlByApplicationId.id).isEqualTo(sourceControl.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetSourceControlByOwner_Unauthenticated() {
    sourceControlService.getSourceControlByOwner(OwnerType.APPLICATION, app.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetSourceControlByOwner_Unauthorized() {
    login();
    sourceControlService.getSourceControlByOwner(OwnerType.APPLICATION, app.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddSourceControlByOwner_Unauthenticated() {
    sourceControlService.addSourceControlByOwner(
        OwnerType.APPLICATION, app.getId(), new ApiSourceControlDTO());
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddSourceControlByOwner_Unauthorized() {
    login();
    sourceControlService.addSourceControlByOwner(
        OwnerType.APPLICATION, app.getId(), new ApiSourceControlDTO());
  }

  @Test
  public void testAddSourceControlByOwner_Authorized() {
    grantWritePermission(app.getId());
    sourceControlService.addSourceControlByOwner(
        OwnerType.APPLICATION, app.getId(),
        apiSourceControlAdapter.convertToDTO(
            new SourceControl.Builder().setOwnerId(app.getId()).setRepositoryUrl(VALID_URL).setToken("token")
                .build()));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdateSourceControlByOwner_Unauthenticated() {
    sourceControlService.updateSourceControlByOwner(
        OwnerType.APPLICATION, app.getId(), new ApiSourceControlDTO());
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdateSourceControlByOwner_Unauthorized() {
    login();
    sourceControlService.updateSourceControlByOwner(
        OwnerType.APPLICATION, app.getId(), new ApiSourceControlDTO());
  }

  @Test
  public void testUpdateSourceControlByOwner_Authorized() {
    grantWritePermission(app.getId());
    ApiSourceControlDTO sourceControl = sourceControlService.addSourceControlByOwner(OwnerType.APPLICATION,
        app.getId(), apiSourceControlAdapter.convertToDTO(
            new SourceControl.Builder().setOwnerId(app.getId()).setRepositoryUrl(VALID_URL).setToken("token")
              .build()));
    sourceControl.token = "newToken";
    sourceControlService.updateSourceControlByOwner(
        OwnerType.APPLICATION, app.getId(), sourceControl);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteSourceControlByOwner_Unauthenticated() {
    sourceControlService.deleteSourceControlByOwner(
        OwnerType.APPLICATION, app.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteSourceControlByOwner_Unauthorized() {
    login();
    sourceControlService.deleteSourceControlByOwner(
        OwnerType.APPLICATION, app.getId());
  }

  @Test
  public void testDeleteSourceControlByOwner_Authorized() {
    grantWritePermission(app.getId());
    tempEntity.newSourceControl(app.getId(), VALID_URL, TOKEN, null);
    sourceControlService.deleteSourceControlByOwner(
        OwnerType.APPLICATION, app.getId());
  }

  @Test
  public void testAddOrUpdateSourceControl_AutomaticScmEnabled_Authorized() {
    // ensure org record exists
    tempEntity.newSourceControl(app.getOrganizationId(), null, TOKEN, null);
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(true);
    grantEvaluateApplicationPermission(org.getId());
    sourceControlService.addOrUpdateSourceControl(app.getPublicId(), VALID_URL, null);
  }

  @Test
  public void testAddOrUpdateSourceControl_AutomaticScmDisabled_Authorized() {
    // ensure org record exists
    tempEntity.newSourceControl(app.getOrganizationId(), null, "token", null);
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(false);
    grantEvaluateApplicationPermission(org.getId());
    sourceControlService.addOrUpdateSourceControl(app.getPublicId(), VALID_URL, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddOrUpdateSourceControl_AutomaticScmEnabled_Unauthorized() {
    login();
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(true);
    sourceControlService.addOrUpdateSourceControl(app.getPublicId(), VALID_URL, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddOrUpdateSourceControl_AutomaticScmDisabled_Unauthorized() {
    login();
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(false);
    sourceControlService.addOrUpdateSourceControl(app.getPublicId(), VALID_URL, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddOrUpdateSourceControl_AutomaticScmEnabled_Unauthenticated() {
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(true);
    sourceControlService.addOrUpdateSourceControl(app.getPublicId(), VALID_URL, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddOrUpdateSourceControl_AutomaticScmDisabled_Unauthenticated() {
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(false);
    sourceControlService.addOrUpdateSourceControl(app.getPublicId(), VALID_URL, null);
  }

  @Test
  public void testGetSourceControlMetricsForApplication() {
    grantReadPermission(app.getId());
    ApiPullRequestResults results =
        sourceControlService.getSourceControlMetricsForApplication(OwnerType.APPLICATION, app.getId());
    assertThat(results.results).hasSize(0);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetSourceControlMetricsForApplication_Unauthorized() {
    login();
    sourceControlService.getSourceControlMetricsForApplication(OwnerType.APPLICATION, app.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetSourceControlMetricsForApplication_Unauthenticated() {
    sourceControlService.getSourceControlMetricsForApplication(OwnerType.APPLICATION, "any");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetRateLimits_Unauthenticated() {
    sourceControlService.getRateLimits(OwnerType.APPLICATION, app.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetRateLimits_Unauthorized() {
    login();
    sourceControlService.getRateLimits(OwnerType.APPLICATION, app.getId());
  }

  @Test
  public void testGetRateLimits() {
    grantReadPermission(app.getId());

    assertThat(sourceControlService.getRateLimits(OwnerType.APPLICATION, app.getId())).isNotNull();
  }
}
