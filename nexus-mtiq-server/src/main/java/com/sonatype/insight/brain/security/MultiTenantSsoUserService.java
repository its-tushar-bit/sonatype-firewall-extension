/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.users.MtiqUserDTO;

@Named
@Singleton
public class MultiTenantSsoUserService
    extends SsoUserService
{
  @Inject
  public MultiTenantSsoUserService(SamlSsoUserProvider samlUserGroupHelper) {
    super(samlUserGroupHelper);
  }

  public void upsertByUsername(final MtiqUserDTO ssoUser) {
    samlSsoUserProvider.upsertByUsername(ssoUserFromMtiqUser(ssoUser));
  }

  static SsoUser ssoUserFromMtiqUser(final MtiqUserDTO user) {
    SsoUser ssoUser = new SsoUser(user.getUsername(), user.getFirstName(), user.getLastName(), user.getEmail(),
        SamlUser.SAML_REALM_ID, null);
    return ssoUser;
  }
}
