/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;

/**
 * @since 1.27
 */
class LdapConfig
{
  private final LdapServer ldapServer;

  private final LdapConnection ldapConnection;

  private final LdapUserMapping ldapUserMapping;

  LdapConfig(final LdapServer ldapServer, final LdapConnection ldapConnection, final LdapUserMapping ldapUserMapping) {
    this.ldapServer = ldapServer;
    this.ldapConnection = ldapConnection;
    this.ldapUserMapping = ldapUserMapping;
  }

  public LdapServer getLdapServer() {
    return ldapServer;
  }

  public LdapConnection getLdapConnection() {
    return ldapConnection;
  }

  public LdapUserMapping getLdapUserMapping() {
    return ldapUserMapping;
  }
}
