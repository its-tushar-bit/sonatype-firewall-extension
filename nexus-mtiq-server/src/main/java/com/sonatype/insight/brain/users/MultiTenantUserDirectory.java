/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.users;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.configuration.ldap.LdapService;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.security.CrowdClientFactory;
import com.sonatype.insight.brain.security.SsoUserService;
import com.sonatype.insight.brain.security.UserDirectory;

@Named
@Singleton
public class MultiTenantUserDirectory
    extends UserDirectory
{
  @Inject
  public MultiTenantUserDirectory(
      UserDAO userDao,
      LdapServerDAO ldapServerDAO,
      SsoUserService ssoUserService,
      LdapService ldapService,
      CrowdClientFactory crowdClientFactory)
  {
    super(userDao, ldapServerDAO, ssoUserService, ldapService, crowdClientFactory);
  }

  /**
   * In MTIQ, show the Associate Group text field in access editor UIs whenever the customer is using their own IdP
   */
  @Override
  public boolean isGroupSearchDisabled() {
    boolean idpManagedBySonatype = SystemConfigurationPropertyFeature.SSO_IDP_MANAGED_BY_SONATYPE.isEnabled();

    return !idpManagedBySonatype;
  }
}
