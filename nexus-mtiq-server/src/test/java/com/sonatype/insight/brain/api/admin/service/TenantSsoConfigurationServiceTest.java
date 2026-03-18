/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.OAuth2ConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.OidcConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.SsoConfigurationDTO;
import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OAuth2ConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OidcConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.oauth2.OAuth2Configuration;
import com.sonatype.insight.brain.model.configuration.oauth2.OidcConfiguration;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.security.SsoUserService;
import com.sonatype.insight.brain.security.TestMultiTenantEncryptionKeyStore;
import com.sonatype.insight.brain.security.oauth2.OidcLoginFilter;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.api.admin.SsoConfigurationTestHelper.ISSUER;
import static com.sonatype.insight.brain.api.admin.SsoConfigurationTestHelper.createOAuth2Configuration;
import static com.sonatype.insight.brain.api.admin.SsoConfigurationTestHelper.createOidcConfiguration;
import static com.sonatype.insight.brain.api.admin.SsoConfigurationTestHelper.createSsoConfigurationDTO;
import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
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

  @Inject
  private PasswordHandler passwordHandler;

  @Captor
  private ArgumentCaptor<OAuth2Configuration> oAuth2ConfigurationCaptor;

  @Captor
  private ArgumentCaptor<OidcConfiguration> oidcConfigurationCaptor;

  private TenantSsoConfigurationService underTest;

  @Before
  public void setup() {
    passwordHandler = new PasswordHandler(new TestMultiTenantEncryptionKeyStore());

    when(mockTenantValidator.validateTenantExists(anyString())).thenReturn(true);
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
      when(mockOAuth2ConfigurationDAO.getById(ISSUER)).thenReturn(oAuth2Configuration);
      when(mockOidcConfigurationDAO.get()).thenReturn(oidcConfiguration);

      testUpdateSsoConfiguration(tenant, ssoConfigurationDTO, false, false);

      verify(mockOidcLoginFilter).clearCachedOidcClientSecret();
    });
  }

  @Test
  public void shouldUpdateOauth2ConfigurationAndInsertOidcConfiguration() {
    SsoConfigurationDTO ssoConfigurationDTO = createSsoConfigurationDTO();
    OAuth2Configuration oAuth2Configuration = createOAuth2Configuration();

    testAsNewTenant(tenant -> {
      when(mockOAuth2ConfigurationDAO.getById(ISSUER)).thenReturn(oAuth2Configuration);

      testUpdateSsoConfiguration(tenant, ssoConfigurationDTO, false, true);
    });
  }

  @Test
  public void shouldInsertOauth2ConfigurationAndUpdateOidcConfiguration() {
    SsoConfigurationDTO ssoConfigurationDTO = createSsoConfigurationDTO();
    OidcConfiguration oidcConfiguration = createOidcConfiguration();

    testAsNewTenant(tenant -> {
      when(mockOidcConfigurationDAO.get()).thenReturn(oidcConfiguration);

      testUpdateSsoConfiguration(tenant, ssoConfigurationDTO, true, false);
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
      verify(mockOAuth2ConfigurationDAO).insert(oAuth2ConfigurationCaptor.capture());
    }
    else {
      verify(mockOAuth2ConfigurationDAO).update(oAuth2ConfigurationCaptor.capture());
    }

    if (insertOidcConfiguration) {
      verify(mockOidcConfigurationDAO).insert(oidcConfigurationCaptor.capture());
    }
    else {
      verify(mockOidcConfigurationDAO).update(oidcConfigurationCaptor.capture());
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
