/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.OAuth2ConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.OidcConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.SsoConfigurationDTO;
import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OAuth2ConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OidcConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.oauth2.OAuth2Configuration;
import com.sonatype.insight.brain.model.configuration.oauth2.OidcConfiguration;
import com.sonatype.insight.brain.api.v2.service.ApiOidcConfigurationService;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.security.SsoUserService;
import com.sonatype.insight.brain.security.TestMultiTenantEncryptionKeyStore;
import com.sonatype.insight.brain.security.oauth2.OidcLoginFilter;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.sonatype.insight.brain.api.admin.SsoConfigurationTestHelper.ISSUER;
import static com.sonatype.insight.brain.api.admin.SsoConfigurationTestHelper.createOAuth2Configuration;
import static com.sonatype.insight.brain.api.admin.SsoConfigurationTestHelper.createOidcConfiguration;
import static com.sonatype.insight.brain.api.admin.SsoConfigurationTestHelper.createSsoConfigurationDTO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TenantSsoConfigurationServiceTest
    extends AbstractMultiTenantTest
{
  @Mock
  private OAuth2ConfigurationDAO mockOAuth2ConfigurationDAO;

  @Mock
  private OidcConfigurationDAO mockOidcConfigurationDAO;

  @Mock
  private TenantUtil mockTenantUtil;

  @Mock
  private TenantValidator mockTenantValidator;

  @Mock
  private SsoUserService mockSsoUserService;

  @Mock
  private OidcLoginFilter mockOidcLoginFilter;

  @Mock
  private TransactionContext transactionContext;

  @Inject
  private PasswordHandler passwordHandler;

  @Captor
  private ArgumentCaptor<OAuth2Configuration> oAuth2ConfigurationCaptor;

  @Captor
  private ArgumentCaptor<OidcConfiguration> oidcConfigurationCaptor;

  private TenantSsoConfigurationService underTest;

  @BeforeEach
  public void setup() {
    passwordHandler = new PasswordHandler(new TestMultiTenantEncryptionKeyStore());

    lenient().when(mockTenantValidator.validateTenantExists(anyString())).thenReturn(true);
    lenient().when(mockOidcConfigurationDAO.createTransactionContext()).thenReturn(transactionContext);
    underTest = new TenantSsoConfigurationService(passwordHandler, mockTenantUtil, mockTenantValidator,
        mockOAuth2ConfigurationDAO, mockOidcConfigurationDAO, mockOidcLoginFilter, mockSsoUserService);
  }

  @Test
  public void shouldSyncSsoProviderDataSources() {
    testAsNewTenant(tenant -> {
      when(mockTenantUtil.isGlobalTenant()).thenReturn(false);
      when(mockTenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);

      underTest.syncSsoProviderDataSources(tenant.tenantSlug);

      verify(mockSsoUserService).syncSsoProviderDataSources();
      verify(mockSsoUserService).loadSsoConfiguration();
    });
  }

  @Test
  public void shouldThrowRuntimeException_whenCallSyncSsoProviderDataSourcesAndTenantDoesntExist() {
    testAsNewTenant(tenant -> {
      when(mockTenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(false);

      assertThatThrownBy(
          () -> underTest.syncSsoProviderDataSources(tenant.tenantSlug))
              .withFailMessage("Tenant doesn't exist")
              .isInstanceOf(NotFoundException.class);
    });
  }

  @Test
  public void shouldThrowRuntimeException_whenCallSyncSsoProviderDataSourcesAndUsingGlobalTenant() {
    testAsGlobalTenant(tenant -> {
      when(mockTenantUtil.isGlobalTenant()).thenReturn(true);

      assertThatThrownBy(
          () -> underTest.syncSsoProviderDataSources(tenant.tenantSlug))
              .withFailMessage("Invalid tenant")
              .isInstanceOf(BadRequestException.class);
    });
  }

  @Test
  public void shouldInsertSsoConfiguration() {
    SsoConfigurationDTO ssoConfigurationDTO = createSsoConfigurationDTO();

    testAsNewTenant(tenant -> {
      testUpdateSsoConfiguration(tenant, ssoConfigurationDTO, true, true);
    });
  }

  @Test
  public void shouldUpdateSsoConfiguration() {
    SsoConfigurationDTO ssoConfigurationDTO = createSsoConfigurationDTO();
    OAuth2Configuration oAuth2Configuration = createOAuth2Configuration();
    OidcConfiguration oidcConfiguration = createOidcConfiguration();

    testAsNewTenant(tenant -> {
      when(mockOAuth2ConfigurationDAO.getAll(transactionContext)).thenReturn(List.of(oAuth2Configuration));
      when(mockOidcConfigurationDAO.get(transactionContext)).thenReturn(oidcConfiguration);

      testUpdateSsoConfiguration(tenant, ssoConfigurationDTO, false, false);

      verify(mockOidcLoginFilter).clearCachedOidcClientSecret();
    });
  }

  @Test
  public void shouldUpdateOauth2ConfigurationAndInsertOidcConfiguration() {
    SsoConfigurationDTO ssoConfigurationDTO = createSsoConfigurationDTO();
    OAuth2Configuration oAuth2Configuration = createOAuth2Configuration();

    testAsNewTenant(tenant -> {
      when(mockOAuth2ConfigurationDAO.getAll(transactionContext)).thenReturn(List.of(oAuth2Configuration));

      testUpdateSsoConfiguration(tenant, ssoConfigurationDTO, false, true);
    });
  }

  @Test
  public void shouldInsertOauth2ConfigurationAndUpdateOidcConfiguration() {
    SsoConfigurationDTO ssoConfigurationDTO = createSsoConfigurationDTO();
    OidcConfiguration oidcConfiguration = createOidcConfiguration();

    testAsNewTenant(tenant -> {
      when(mockOidcConfigurationDAO.get(transactionContext)).thenReturn(oidcConfiguration);

      testUpdateSsoConfiguration(tenant, ssoConfigurationDTO, true, false);
    });
  }

  @Test
  public void shouldReplaceConfigurationsAndPreserveSecret_whenIdpIssuerChanges() {
    String newIssuer = "http://new-idp/";
    SsoConfigurationDTO ssoConfigurationDTO = createSsoConfigurationDTO();
    ssoConfigurationDTO.getOAuth2Configuration().setIdpIssuer(newIssuer);
    ssoConfigurationDTO.getOidcConfiguration().setIdpIssuer(newIssuer);
    ssoConfigurationDTO.getOidcConfiguration().setClientSecret(ApiOidcConfigurationService.CLIENT_SECRET_MASK);

    OAuth2Configuration existingOAuth2 = createOAuth2Configuration();
    OidcConfiguration existingOidc = createOidcConfiguration();

    testAsNewTenant(tenant -> {
      when(mockTenantUtil.isGlobalTenant()).thenReturn(false);
      when(mockTenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);
      when(mockOAuth2ConfigurationDAO.getAll(transactionContext)).thenReturn(List.of(existingOAuth2));
      when(mockOidcConfigurationDAO.get(transactionContext)).thenReturn(existingOidc);

      underTest.updateSsoConfiguration(ssoConfigurationDTO, tenant.tenantSlug);

      verify(mockOAuth2ConfigurationDAO).delete(transactionContext, existingOAuth2);
      verify(mockOAuth2ConfigurationDAO).insert(eq(transactionContext), oAuth2ConfigurationCaptor.capture());
      verify(mockOAuth2ConfigurationDAO, never()).update(any(), any());

      verify(mockOidcConfigurationDAO).delete(transactionContext, existingOidc);
      verify(mockOidcConfigurationDAO).insert(eq(transactionContext), oidcConfigurationCaptor.capture());
      verify(mockOidcConfigurationDAO, never()).update(any(), any());

      assertThat(oAuth2ConfigurationCaptor.getValue().getId()).isEqualTo(newIssuer);
      OidcConfiguration insertedOidc = oidcConfigurationCaptor.getValue();
      assertThat(insertedOidc.getId()).isEqualTo(newIssuer);
      assertThat(insertedOidc.getClientSecret()).isEqualTo(existingOidc.getClientSecret());

      verify(mockSsoUserService).loadSsoConfiguration();
      verify(mockOidcLoginFilter).clearCachedOidcClientSecret();
    });
  }

  @Test
  public void shouldRemoveStaleRow_whenDuplicateOauth2ConfigurationExists() {
    SsoConfigurationDTO ssoConfigurationDTO = createSsoConfigurationDTO();

    OAuth2Configuration staleOAuth2 = createOAuth2Configuration();
    staleOAuth2.setId("http://stale-idp/");
    OAuth2Configuration currentOAuth2 = createOAuth2Configuration();

    testAsNewTenant(tenant -> {
      when(mockTenantUtil.isGlobalTenant()).thenReturn(false);
      when(mockTenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);
      when(mockOAuth2ConfigurationDAO.getAll(transactionContext)).thenReturn(List.of(staleOAuth2, currentOAuth2));

      underTest.updateSsoConfiguration(ssoConfigurationDTO, tenant.tenantSlug);

      verify(mockOAuth2ConfigurationDAO).delete(transactionContext, staleOAuth2);
      verify(mockOAuth2ConfigurationDAO, never()).delete(transactionContext, currentOAuth2);
      verify(mockOAuth2ConfigurationDAO).update(eq(transactionContext), oAuth2ConfigurationCaptor.capture());
      assertThat(oAuth2ConfigurationCaptor.getValue().getId()).isEqualTo(ISSUER);
    });
  }

  @Test
  public void shouldThrowBadRequest_whenIdpIssuersDoNotMatch() {
    SsoConfigurationDTO ssoConfigurationDTO = createSsoConfigurationDTO();
    ssoConfigurationDTO.getOidcConfiguration().setIdpIssuer("http://different-idp/");

    testAsNewTenant(tenant -> {
      when(mockTenantUtil.isGlobalTenant()).thenReturn(false);
      when(mockTenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);

      assertThatThrownBy(() -> underTest.updateSsoConfiguration(ssoConfigurationDTO, tenant.tenantSlug))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("OIDC IdP issuer must match OAuth2 IdP issuer");

      verify(mockOidcConfigurationDAO, never()).createTransactionContext();
    });
  }

  @Test
  public void shouldThrowBadRequest_whenOidcConfigurationIsNull() {
    SsoConfigurationDTO ssoConfigurationDTO = createSsoConfigurationDTO();
    ssoConfigurationDTO.setOidcConfiguration(null);

    testAsNewTenant(tenant -> {
      when(mockTenantUtil.isGlobalTenant()).thenReturn(false);
      when(mockTenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);

      assertThatThrownBy(() -> underTest.updateSsoConfiguration(ssoConfigurationDTO, tenant.tenantSlug))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("OAuth2 and OIDC configurations must be provided");

      verify(mockOidcConfigurationDAO, never()).createTransactionContext();
    });
  }

  @Test
  public void shouldThrowBadRequest_whenDaoRejectsConfiguration() {
    SsoConfigurationDTO ssoConfigurationDTO = createSsoConfigurationDTO();

    testAsNewTenant(tenant -> {
      when(mockTenantUtil.isGlobalTenant()).thenReturn(false);
      when(mockTenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);
      when(mockOAuth2ConfigurationDAO.getAll(transactionContext)).thenReturn(List.of());
      doThrow(new IllegalArgumentException("IDP_JWS_ALGORITHM_REQUIRED"))
          .when(mockOAuth2ConfigurationDAO)
          .insert(eq(transactionContext), any());

      assertThatThrownBy(() -> underTest.updateSsoConfiguration(ssoConfigurationDTO, tenant.tenantSlug))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Invalid OIDC configuration")
          .hasMessageContaining("IDP_JWS_ALGORITHM_REQUIRED");

      verify(mockSsoUserService, never()).loadSsoConfiguration();
    });
  }

  @Test
  public void shouldThrowRuntimeException_whenCallingUpdateSsoConfigurationAndTenantDoesntExist() {
    SsoConfigurationDTO ssoConfigurationDTO = createSsoConfigurationDTO();

    testAsNewTenant(tenant -> {
      when(mockTenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(false);

      assertThatThrownBy(
          () -> underTest.updateSsoConfiguration(ssoConfigurationDTO, tenant.tenantSlug))
              .withFailMessage("Tenant doesn't exist")
              .isInstanceOf(NotFoundException.class);
    });
  }

  @Test
  public void shouldThrowRuntimeException_whenCallingUpdateSsoConfigurationAndUsingGlobalTenant() {
    SsoConfigurationDTO ssoConfigurationDTO = createSsoConfigurationDTO();

    testAsGlobalTenant(tenant -> {
      when(mockTenantUtil.isGlobalTenant()).thenReturn(true);

      assertThatThrownBy(
          () -> underTest.updateSsoConfiguration(ssoConfigurationDTO, tenant.tenantSlug))
              .withFailMessage("Invalid tenant")
              .isInstanceOf(BadRequestException.class);
    });
  }

  @Test
  public void getSsoConfiguration_returnsDtoWithRedactedClientSecret() {
    OidcConfiguration oidcConfiguration = createOidcConfiguration();
    OAuth2Configuration oAuth2Configuration = createOAuth2Configuration();

    testAsNewTenant(tenant -> {
      when(mockTenantUtil.isGlobalTenant()).thenReturn(false);
      when(mockTenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);
      when(mockOidcConfigurationDAO.get()).thenReturn(oidcConfiguration);
      when(mockOAuth2ConfigurationDAO.getById(ISSUER)).thenReturn(oAuth2Configuration);

      SsoConfigurationDTO result = underTest.getSsoConfiguration(tenant.tenantSlug);

      assertThat(result).isNotNull();
      assertThat(result.getOidcConfiguration().getIdpIssuer()).isEqualTo(ISSUER);
      assertThat(result.getOidcConfiguration().getClientId()).isEqualTo(oidcConfiguration.getClientId());
      assertThat(result.getOidcConfiguration().getClientSecret())
          .isEqualTo(ApiOidcConfigurationService.CLIENT_SECRET_MASK);
      assertThat(result.getOAuth2Configuration().getIdpIssuer()).isEqualTo(ISSUER);
      assertThat(result.getOAuth2Configuration().getIdpJwsAlgorithm())
          .isEqualTo(oAuth2Configuration.getIdpJwsAlgorithm());
    });
  }

  @Test
  public void getSsoConfiguration_throwsNotFound_whenOidcMissing() {
    testAsNewTenant(tenant -> {
      when(mockTenantUtil.isGlobalTenant()).thenReturn(false);
      when(mockTenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);
      when(mockOidcConfigurationDAO.get()).thenReturn(null);

      assertThatThrownBy(() -> underTest.getSsoConfiguration(tenant.tenantSlug))
          .isInstanceOf(NotFoundException.class)
          .hasMessage("SSO configuration not set: OIDC configuration not found");
    });
  }

  @Test
  public void getSsoConfiguration_throwsNotFound_whenOAuth2Missing() {
    OidcConfiguration oidcConfiguration = createOidcConfiguration();

    testAsNewTenant(tenant -> {
      when(mockTenantUtil.isGlobalTenant()).thenReturn(false);
      when(mockTenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);
      when(mockOidcConfigurationDAO.get()).thenReturn(oidcConfiguration);
      when(mockOAuth2ConfigurationDAO.getById(ISSUER)).thenReturn(null);

      assertThatThrownBy(() -> underTest.getSsoConfiguration(tenant.tenantSlug))
          .isInstanceOf(NotFoundException.class)
          .hasMessage("SSO configuration not set: OAuth2 configuration not found");
    });
  }

  @Test
  public void getSsoConfiguration_throwsNotFound_whenTenantDoesNotExist() {
    testAsNewTenant(tenant -> {
      when(mockTenantUtil.isGlobalTenant()).thenReturn(false);
      when(mockTenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(false);

      assertThatThrownBy(() -> underTest.getSsoConfiguration(tenant.tenantSlug))
          .isInstanceOf(NotFoundException.class)
          .hasMessage("Tenant " + tenant.tenantSlug + " doesn't exist");
    });
  }

  @Test
  public void getSsoConfiguration_throwsBadRequest_whenGlobalTenant() {
    testAsGlobalTenant(tenant -> {
      when(mockTenantUtil.isGlobalTenant()).thenReturn(true);

      assertThatThrownBy(() -> underTest.getSsoConfiguration(tenant.tenantSlug))
          .isInstanceOf(BadRequestException.class)
          .hasMessage("Operation not supported for global tenant");
    });
  }

  private void testUpdateSsoConfiguration(
      Tenant tenant,
      SsoConfigurationDTO ssoConfigurationDTO,
      boolean insertOAuth2Configuration,
      boolean insertOidcConfiguration)
  {
    when(mockTenantUtil.isGlobalTenant()).thenReturn(false);
    when(mockTenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);

    underTest.updateSsoConfiguration(ssoConfigurationDTO, tenant.tenantSlug);

    if (insertOAuth2Configuration) {
      verify(mockOAuth2ConfigurationDAO).insert(eq(transactionContext), oAuth2ConfigurationCaptor.capture());
    }
    else {
      verify(mockOAuth2ConfigurationDAO).update(eq(transactionContext), oAuth2ConfigurationCaptor.capture());
    }

    if (insertOidcConfiguration) {
      verify(mockOidcConfigurationDAO).insert(eq(transactionContext), oidcConfigurationCaptor.capture());
    }
    else {
      verify(mockOidcConfigurationDAO).update(eq(transactionContext), oidcConfigurationCaptor.capture());
    }

    verify(mockSsoUserService).loadSsoConfiguration();
    verify(mockOidcLoginFilter).clearCachedOidcClientSecret();

    assertOauth2ConfigurationIsTheExpected(ssoConfigurationDTO.getOAuth2Configuration(),
        oAuth2ConfigurationCaptor.getValue());
    assertOidcConfigurationIsTheExpected(ssoConfigurationDTO.getOidcConfiguration(),
        oidcConfigurationCaptor.getValue());
  }

  private void assertOauth2ConfigurationIsTheExpected(
      final OAuth2ConfigurationDTO oAuth2ConfigurationDTO,
      final OAuth2Configuration oAuth2Configuration)
  {
    assertEquals(oAuth2ConfigurationDTO.getIdpIssuer(), oAuth2Configuration.getId());
    assertEquals(oAuth2ConfigurationDTO.getIdpJwsAlgorithm(), oAuth2Configuration.getIdpJwsAlgorithm());
    assertEquals(oAuth2ConfigurationDTO.getIdpJwksUrl(), oAuth2Configuration.getIdpJwksUrl());
    assertEquals(oAuth2ConfigurationDTO.getIdpJwks(), oAuth2Configuration.getIdpJwks());
  }

  private void assertOidcConfigurationIsTheExpected(
      final OidcConfigurationDTO oidcConfigurationDTO,
      final OidcConfiguration oidcConfiguration)
  {
    assertEquals(oidcConfigurationDTO.getIdpIssuer(), oidcConfiguration.getId());
    assertEquals(oidcConfigurationDTO.getClientId(), oidcConfiguration.getClientId());
    String clientSecret = passwordHandler.decryptPassword(oidcConfiguration.getClientSecret());
    assertEquals(oidcConfigurationDTO.getClientSecret(), clientSecret);
    assertEquals(oidcConfigurationDTO.getIdpAuthorizationUrl(), oidcConfiguration.getIdpAuthorizationUrl());
    assertEquals(oidcConfigurationDTO.getIdpTokenUrl(), oidcConfiguration.getIdpTokenUrl());
  }
}
