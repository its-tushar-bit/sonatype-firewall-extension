/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiPullRequestResults;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiSourceControlDTO;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticSourceControlConfigurationDAO;
import com.sonatype.insight.brain.git.GitClientFactory;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.security.TestEncryptionKeyStore;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.insight.brain.relay.RelayRegistrationService;
import com.sonatype.insight.brain.relay.dto.RelayRegisterAdminRequest;
import com.sonatype.nexus.scm.api.GitApiClient;
import jakarta.inject.Inject;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

/**
 * @since 1.66
 */
@ComponentH2Test
public class ApiSourceControlServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  private static final String VALID_URL = "https://example.com/organization/project";

  private static final String TOKEN = new String(
      new PasswordHandler(new TestEncryptionKeyStore())
          .encryptPassword("token".toCharArray()));

  @BeforeEach
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

  @Test
  public void testGetAll_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      sourceControlService.getAll();
    });
  }

  @Test
  public void testGetAll_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> {
      sourceControlService.getAll();
    });
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

  @Test
  public void testGetSourceControlByOwner_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      sourceControlService.getSourceControlByOwner(OwnerType.APPLICATION, app.getId());
    });
  }

  @Test
  public void testGetSourceControlByOwner_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> {
      sourceControlService.getSourceControlByOwner(OwnerType.APPLICATION, app.getId());
    });
  }

  @Test
  public void testAddSourceControlByOwner_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      sourceControlService.addSourceControlByOwner(
          OwnerType.APPLICATION, app.getId(), new ApiSourceControlDTO());
    });
  }

  @Test
  public void testAddSourceControlByOwner_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> {
      sourceControlService.addSourceControlByOwner(
          OwnerType.APPLICATION, app.getId(), new ApiSourceControlDTO());
    });
  }

  @Test
  public void testAddSourceControlByOwner_Authorized() {
    grantWritePermission(app.getId());
    sourceControlService.addSourceControlByOwner(
        OwnerType.APPLICATION, app.getId(),
        apiSourceControlAdapter.convertToDTO(
            new SourceControl.Builder().setOwnerId(app.getId())
                .setRepositoryUrl(VALID_URL)
                .setToken("token")
                .build()));
  }

  @Test
  public void testUpdateSourceControlByOwner_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      sourceControlService.updateSourceControlByOwner(
          OwnerType.APPLICATION, app.getId(), new ApiSourceControlDTO());
    });
  }

  @Test
  public void testUpdateSourceControlByOwner_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> {
      sourceControlService.updateSourceControlByOwner(
          OwnerType.APPLICATION, app.getId(), new ApiSourceControlDTO());
    });
  }

  @Test
  public void testUpdateSourceControlByOwner_Authorized() {
    grantWritePermission(app.getId());
    ApiSourceControlDTO sourceControl = sourceControlService.addSourceControlByOwner(OwnerType.APPLICATION,
        app.getId(), apiSourceControlAdapter.convertToDTO(
            new SourceControl.Builder().setOwnerId(app.getId())
                .setRepositoryUrl(VALID_URL)
                .setToken("token")
                .build()));
    sourceControl.token = "newToken";
    sourceControlService.updateSourceControlByOwner(
        OwnerType.APPLICATION, app.getId(), sourceControl);
  }

  @Test
  public void testDeleteSourceControlByOwner_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      sourceControlService.deleteSourceControlByOwner(
          OwnerType.APPLICATION, app.getId());
    });
  }

  @Test
  public void testDeleteSourceControlByOwner_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> {
      sourceControlService.deleteSourceControlByOwner(
          OwnerType.APPLICATION, app.getId());
    });
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

  @Test
  public void testAddOrUpdateSourceControl_AutomaticScmEnabled_Unauthorized() {
    login();
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(true);
    assertThrows(UnauthorizedException.class, () -> {
      sourceControlService.addOrUpdateSourceControl(app.getPublicId(), VALID_URL, null);
    });
  }

  @Test
  public void testAddOrUpdateSourceControl_AutomaticScmDisabled_Unauthorized() {
    login();
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(false);
    assertThrows(UnauthorizedException.class, () -> {
      sourceControlService.addOrUpdateSourceControl(app.getPublicId(), VALID_URL, null);
    });
  }

  @Test
  public void testAddOrUpdateSourceControl_AutomaticScmEnabled_Unauthenticated() {
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(true);
    assertThrows(UnauthenticatedException.class, () -> {
      sourceControlService.addOrUpdateSourceControl(app.getPublicId(), VALID_URL, null);
    });
  }

  @Test
  public void testAddOrUpdateSourceControl_AutomaticScmDisabled_Unauthenticated() {
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(false);
    assertThrows(UnauthenticatedException.class, () -> {
      sourceControlService.addOrUpdateSourceControl(app.getPublicId(), VALID_URL, null);
    });
  }

  @Test
  public void testGetSourceControlMetricsForApplication() {
    grantReadPermission(app.getId());
    ApiPullRequestResults results =
        sourceControlService.getSourceControlMetricsForApplication(OwnerType.APPLICATION, app.getId());
    assertThat(results.results).hasSize(0);
  }

  @Test
  public void testGetSourceControlMetricsForApplication_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> {
      sourceControlService.getSourceControlMetricsForApplication(OwnerType.APPLICATION, app.getId());
    });
  }

  @Test
  public void testGetSourceControlMetricsForApplication_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      sourceControlService.getSourceControlMetricsForApplication(OwnerType.APPLICATION, "any");
    });
  }

  @Test
  public void testGetRateLimits_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      sourceControlService.getRateLimits(OwnerType.APPLICATION, app.getId());
    });
  }

  @Test
  public void testGetRateLimits_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> {
      sourceControlService.getRateLimits(OwnerType.APPLICATION, app.getId());
    });
  }

  @Test
  public void testGetRateLimits() {
    grantReadPermission(app.getId());

    assertThat(sourceControlService.getRateLimits(OwnerType.APPLICATION, app.getId())).isNotNull();
  }

  @Test
  public void testRegisterWithRelay_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      sourceControlService.registerWithRelay();
    });
  }

  @Test
  public void testRegisterWithRelay_Unauthorized() {
    login();
    // Manage SCM config is NOT enough; CONFIGURE_SYSTEM is required for the admin re-register hook.
    grantGlobalPermission(Permission.MANAGE_AUTOMATIC_SCM_CONFIGURATION);
    assertThrows(UnauthorizedException.class, () -> {
      sourceControlService.registerWithRelay();
    });
  }

  @Test
  public void testRegisterWithRelay_Authorized_throwsFeatureDisabledByDefault() {
    grantGlobalPermission(Permission.CONFIGURE_SYSTEM);
    assertThatExceptionOfType(RelayRegistrationService.RelayFeatureDisabledException.class)
        .isThrownBy(() -> sourceControlService.registerWithRelay());
  }

  @Test
  public void testRegisterWithRelay_NullBody_callsRegisterOnDemand() {
    grantGlobalPermission(Permission.CONFIGURE_SYSTEM);
    // Null body routes to the PAT path; gate is closed in tests so we expect feature-disabled.
    assertThatExceptionOfType(RelayRegistrationService.RelayFeatureDisabledException.class)
        .isThrownBy(() -> sourceControlService.registerWithRelay((RelayRegisterAdminRequest) null));
  }

  @Test
  public void testRegisterWithRelay_EmptyBody_callsRegisterOnDemand() {
    grantGlobalPermission(Permission.CONFIGURE_SYSTEM);
    assertThatExceptionOfType(RelayRegistrationService.RelayFeatureDisabledException.class)
        .isThrownBy(() -> sourceControlService.registerWithRelay(new RelayRegisterAdminRequest()));
  }

  @Test
  public void testRegisterWithRelay_WithInstallationId_callsRegisterGitHubAppOnDemand() {
    grantGlobalPermission(Permission.CONFIGURE_SYSTEM);
    RelayRegisterAdminRequest body = new RelayRegisterAdminRequest();
    body.setInstallationId("42");
    body.setWebhookSecret("hmac");
    // Routes to the GitHub App path; gate is closed in tests so we expect feature-disabled.
    assertThatExceptionOfType(RelayRegistrationService.RelayFeatureDisabledException.class)
        .isThrownBy(() -> sourceControlService.registerWithRelay(body));
  }

  @Test
  public void testRegisterWithRelay_BodyVariant_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      sourceControlService.registerWithRelay(new RelayRegisterAdminRequest());
    });
  }

  @Test
  public void testRegisterWithRelay_BodyVariant_Unauthorized() {
    login();
    grantGlobalPermission(Permission.MANAGE_AUTOMATIC_SCM_CONFIGURATION);
    RelayRegisterAdminRequest body = new RelayRegisterAdminRequest();
    body.setInstallationId("42");
    assertThrows(UnauthorizedException.class, () -> {
      sourceControlService.registerWithRelay(body);
    });
  }

  @Test
  public void testGetRelayWebhookUrl_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      sourceControlService.getRelayWebhookUrl();
    });
  }

  @Test
  public void testGetRelayWebhookUrl_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> {
      sourceControlService.getRelayWebhookUrl();
    });
  }

  @Test
  public void testGetRelayWebhookUrl_Authorized_throwsFeatureDisabledByDefault() {
    grantGlobalPermission(Permission.MANAGE_AUTOMATIC_SCM_CONFIGURATION);
    assertThatExceptionOfType(RelayRegistrationService.RelayFeatureDisabledException.class)
        .isThrownBy(() -> sourceControlService.getRelayWebhookUrl());
  }

  @Test
  public void testGetRelayWebhookSecret_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      sourceControlService.getRelayWebhookSecret();
    });
  }

  @Test
  public void testGetRelayWebhookSecret_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> {
      sourceControlService.getRelayWebhookSecret();
    });
  }

  @Test
  public void testGetRelayWebhookSecret_Authorized_throwsFeatureDisabledByDefault() {
    grantGlobalPermission(Permission.MANAGE_AUTOMATIC_SCM_CONFIGURATION);
    assertThatExceptionOfType(RelayRegistrationService.RelayFeatureDisabledException.class)
        .isThrownBy(() -> sourceControlService.getRelayWebhookSecret());
  }

  @Test
  public void testGetGitHubAppWebhookUrl_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      sourceControlService.getGitHubAppWebhookUrl();
    });
  }

  @Test
  public void testGetGitHubAppWebhookUrl_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> {
      sourceControlService.getGitHubAppWebhookUrl();
    });
  }

  @Test
  public void testGetGitHubAppWebhookUrl_Authorized_throwsFeatureDisabledByDefault() {
    grantGlobalPermission(Permission.MANAGE_AUTOMATIC_SCM_CONFIGURATION);
    assertThatExceptionOfType(RelayRegistrationService.RelayFeatureDisabledException.class)
        .isThrownBy(() -> sourceControlService.getGitHubAppWebhookUrl());
  }

  @Test
  public void testRotateRelayApiKey_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      sourceControlService.rotateRelayApiKey();
    });
  }

  @Test
  public void testRotateRelayApiKey_Unauthorized() {
    login();
    // Manage SCM config is NOT enough; CONFIGURE_SYSTEM is required for the admin rotate hook.
    grantGlobalPermission(Permission.MANAGE_AUTOMATIC_SCM_CONFIGURATION);
    assertThrows(UnauthorizedException.class, () -> {
      sourceControlService.rotateRelayApiKey();
    });
  }

  @Test
  public void testRotateRelayApiKey_Authorized_throwsFeatureDisabledByDefault() {
    grantGlobalPermission(Permission.CONFIGURE_SYSTEM);
    assertThatExceptionOfType(RelayRegistrationService.RelayFeatureDisabledException.class)
        .isThrownBy(() -> sourceControlService.rotateRelayApiKey());
  }

  @Test
  public void testRotateRelayWebhookSecret_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      sourceControlService.rotateRelayWebhookSecret();
    });
  }

  @Test
  public void testRotateRelayWebhookSecret_Unauthorized() {
    login();
    grantGlobalPermission(Permission.MANAGE_AUTOMATIC_SCM_CONFIGURATION);
    assertThrows(UnauthorizedException.class, () -> {
      sourceControlService.rotateRelayWebhookSecret();
    });
  }

  @Test
  public void testRotateRelayWebhookSecret_Authorized_throwsFeatureDisabledByDefault() {
    grantGlobalPermission(Permission.CONFIGURE_SYSTEM);
    assertThatExceptionOfType(RelayRegistrationService.RelayFeatureDisabledException.class)
        .isThrownBy(() -> sourceControlService.rotateRelayWebhookSecret());
  }
}
