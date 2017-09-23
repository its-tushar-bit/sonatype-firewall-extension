/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import javax.naming.ldap.LdapContext;

import org.apache.shiro.realm.ldap.LdapUtils;

/**
 * Helper to enable try-with-resources with LDAP contexts.
 */
class LdapContextHolder
    implements AutoCloseable
{
  final LdapContext ctx;

  public LdapContextHolder(LdapContext ctx) {
    this.ctx = ctx;
  }

  @Override
  public void close() {
    LdapUtils.closeContext(ctx);
  }
}
