/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ldap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;

import com.sonatype.insight.brain.configuration.ldap.LdapConnection;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.realm.ldap.JndiLdapRealm;
import org.apache.shiro.realm.ldap.LdapUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * Connects Shiro's {@link JndiLdapRealm} to our internal LDAP configuration.
 * 
 * @since 1.7
 */
@Named
@Singleton
public class LdapRealm
    extends JndiLdapRealm
{
  private final LdapConnectionManager ldapConnectionManager;

  @Inject
  public LdapRealm(LdapConnectionManager ldapConnectionManager) {
    this.ldapConnectionManager = ldapConnectionManager;
  }

  @Override
  public boolean supports(AuthenticationToken token) {
    return super.supports(token) && ldapConnectionManager.isLdapConfigured();
  }

  @Override
  public LdapContextFactory getContextFactory() {
    LdapConnection conn = ldapConnectionManager.getDecryptedConnection();
    // TODO: this shortcut for finding user DNs will be replaced with LDAP search once user mapping is integrated
    if (conn != null && StringUtils.isNotBlank(conn.getSearchBase())
        && !conn.getAuthenticationMethod().getMethod().endsWith("MD5")) {
      setUserDnTemplate(conn.getSearchBase());
    }
    return createContextFactory(conn);
  }

  static void testConnection(LdapConnection conn) throws NamingException {
    LdapContext ctx = null;
    try {
      ctx = createContextFactory(conn).getSystemLdapContext();
    }
    finally {
      LdapUtils.closeContext(ctx);
    }
  }

  private static LdapContextFactory createContextFactory(LdapConnection conn) {
    LdapContextFactory contextFactory = new LdapContextFactory();

    contextFactory.setUrl(conn.getUrl());
    contextFactory.setAuthenticationMechanism(conn.getAuthenticationMethod().getMethod());
    contextFactory.setSystemUsername(conn.getSystemUsername());
    contextFactory.setSystemPassword(conn.getSystemPassword());
    contextFactory.setSaslRealm(conn.getSaslRealm());

    return contextFactory;
  }
}
