/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.users;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.configuration.ldap.LdapService;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.security.CrowdClientFactory;
import com.sonatype.insight.brain.security.SamlUserGroupHelper;
import com.sonatype.insight.brain.security.UserDirectory;

@Named
@Singleton
public class MultiTenantUserDirectory
    extends UserDirectory
{
  @Inject
  public MultiTenantUserDirectory(
      UserDAO userDao,
      SamlUserGroupHelper samlUserGroupHelper,
      LdapService ldapService,
      CrowdClientFactory crowdClientFactory)
  {
    super(userDao, samlUserGroupHelper, ldapService, crowdClientFactory);
  }

  /**
   * In MTIQ, show the Associate Group text field in access editor UIs whenever the customer is using their own IdP
   */
  @Override
  public boolean isGroupSearchDisabled() {
    boolean idpManagedBySonatype =
        ApiConfigFeaturesService.SystemConfigurationPropertyFeature.SSO_IDP_MANAGED_BY_SONATYPE.isEnabled();

    return !idpManagedBySonatype;
  }
}
