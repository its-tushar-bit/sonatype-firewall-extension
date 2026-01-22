/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.security.OAuth2UserDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.users.MtiqUserDTO;

import ru.vyarus.dropwizard.guice.module.installer.scanner.InvisibleForScanner;

@Named
@Singleton
@InvisibleForScanner
public class MultiTenantSsoUserService
    extends SsoUserService
{
  @Inject
  public MultiTenantSsoUserService(
      SamlSsoUserProvider samlSsoUserProvider,
      OAuth2SsoUserProvider oAuth2SsoUserProvider,
      SamlUserDAO samlUserDAO,
      OAuth2UserDAO oAuth2UserDAO)
  {
    super(samlSsoUserProvider, oAuth2SsoUserProvider, samlUserDAO, oAuth2UserDAO);
  }

  public void upsertByUsername(final MtiqUserDTO ssoUser) {
    getEnabledSsoUserProvider().upsertByUsername(ssoUserFromMtiqUser(ssoUser));
  }

  private static SsoUser ssoUserFromMtiqUser(final MtiqUserDTO user) {
    return new SsoUser(user.getUsername().toLowerCase(), user.getFirstName(), user.getLastName(), user.getEmail());
  }
}
