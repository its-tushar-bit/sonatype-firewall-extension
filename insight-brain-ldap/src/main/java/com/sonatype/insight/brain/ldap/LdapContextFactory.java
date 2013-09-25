/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ldap;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;

import org.apache.shiro.realm.ldap.JndiLdapContextFactory;
import org.codehaus.plexus.util.StringUtils;

import static java.lang.Boolean.TRUE;

/**
 * Extends Shiro's {@link JndiLdapContextFactory} to properly track system contexts.
 * 
 * @since 1.7
 */
class LdapContextFactory
    extends JndiLdapContextFactory
{
  private static final ThreadLocal<Boolean> systemContext = new ThreadLocal<Boolean>();

  private String saslRealm;

  @Override
  public LdapContext getSystemLdapContext() throws NamingException {
    systemContext.set(TRUE);
    try {
      return super.getSystemLdapContext();
    }
    finally {
      systemContext.remove();
    }
  }

  public void setSaslRealm(String saslRealm) {
    this.saslRealm = saslRealm;
  }

  public String getSaslRealm() {
    return saslRealm;
  }

  @Override
  protected boolean isPoolingConnections(Object principal) {
    // replace original system name check with safer thread-context check;
    // was isPoolingEnabled() && principal != null && principal.equals(getSystemUsername());
    return isPoolingEnabled() && principal != null && isSystemContext();
  }

  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
  protected LdapContext createLdapContext(Hashtable env) throws NamingException {
    // force switch to 'simple' if auth is 'none' and this is not the system context
    if ("none".equals(env.get(Context.SECURITY_AUTHENTICATION)) && !isSystemContext()) {
      env.put(Context.SECURITY_AUTHENTICATION, SIMPLE_AUTHENTICATION_MECHANISM_NAME);
    }
    if (StringUtils.isNotBlank(saslRealm)) {
      env.put("java.naming.security.sasl.realm", saslRealm);
    }
    return super.createLdapContext(env);
  }

  private static boolean isSystemContext() {
    return TRUE == systemContext.get();
  }
}
