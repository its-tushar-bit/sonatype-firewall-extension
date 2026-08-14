/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.security.OAuth2UserDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;
import com.sonatype.insight.brain.users.MtiqUserDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MultiTenantSsoUserServiceTest
    extends AbstractMultiTenantTest
{
  @Mock
  SamlSsoUserProvider samlSsoUserProvider;

  @Mock
  OAuth2SsoUserProvider oAuth2SsoUserProvider;

  @Mock
  SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @Mock
  SamlUserDAO samlUserDAO;

  @Mock
  OAuth2UserDAO oAuth2UserDAO;

  private MultiTenantSsoUserService underTest;

  @BeforeEach
  public void setup() {
    underTest = new MultiTenantSsoUserService(samlSsoUserProvider, oAuth2SsoUserProvider, samlUserDAO, oAuth2UserDAO);
  }

  @Test
  public void testUpsertByUsername_Saml() {
    when(samlSsoUserProvider.isSsoConfigured()).thenReturn(true);
    when(oAuth2SsoUserProvider.isSsoConfigured()).thenReturn(false);

    MtiqUserDTO mtiqUserDTO = new MtiqUserDTO();
    mtiqUserDTO.setUsername("username");

    testAsNewTenant(t1 -> {
      underTest.loadSsoConfiguration();

      underTest.upsertByUsername(mtiqUserDTO);

      verify(samlSsoUserProvider).upsertByUsername(any(SsoUser.class));
    });
  }

  @Test
  public void testUpsertByUsername_OAuth2() {
    when(samlSsoUserProvider.isSsoConfigured()).thenReturn(false);
    when(oAuth2SsoUserProvider.isSsoConfigured()).thenReturn(true);
    when(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.OAUTH2_ENABLED)).thenReturn(
        new SystemConfigurationProperty(SystemConfigurationProperty.OAUTH2_ENABLED, "true"));
    when(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.SAML_ENABLED)).thenReturn(
        new SystemConfigurationProperty(SystemConfigurationProperty.SAML_ENABLED, "true"));

    MtiqUserDTO mtiqUserDTO = new MtiqUserDTO();
    mtiqUserDTO.setUsername("username");

    testAsNewTenant(t1 -> {
      SystemConfigurationPropertyFeature.injectDependencies(systemConfigurationPropertyDAO);
      underTest.loadSsoConfiguration();

      underTest.upsertByUsername(mtiqUserDTO);

      verify(oAuth2SsoUserProvider).upsertByUsername(any(SsoUser.class));
    });
  }
}
