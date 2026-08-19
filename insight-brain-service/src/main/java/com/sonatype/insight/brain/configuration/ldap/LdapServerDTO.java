/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;

public class LdapServerDTO
{
  public String ldapServerId;

  public String ldapServerName;

  public LdapServerDTO() {
    // for jackson
  }

  public LdapServerDTO(final LdapServer ldapServer) {
    this.ldapServerId = ldapServer.getId();
    this.ldapServerName = ldapServer.getName();
  }
}
