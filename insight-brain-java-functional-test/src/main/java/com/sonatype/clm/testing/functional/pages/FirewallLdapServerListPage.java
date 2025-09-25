/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.utils.BaseUrl;

public class FirewallLdapServerListPage
    extends LdapServerListPage
{
  @Override
  public String url() {
    return BaseUrl.resolvePageUrl("/firewall/ldap-servers");
  }
}
