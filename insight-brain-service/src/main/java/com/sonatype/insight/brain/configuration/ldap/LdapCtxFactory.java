/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.realm.ldap.JndiLdapContextFactory;

import static java.lang.Boolean.TRUE;

/**
 * Shiro LDAP context factory that uses a thread-local to properly track system contexts.
 *
 * @since 1.7
 */
class LdapCtxFactory
    extends JndiLdapContextFactory
{
  static {
    // ensure LDAP pooling is enabled for all protocols and methods (CRAM-MD5 cannot be pooled)
    System.setProperty("com.sun.jndi.ldap.connect.pool.authentication", "none simple DIGEST-MD5");
    System.setProperty("com.sun.jndi.ldap.connect.pool.protocol", "plain ssl");
  }

  private static final ThreadLocal<Boolean> systemContext = new ThreadLocal<>();

  private String saslRealm;

  private int connectionTimeout;

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

  /**
   * @param connectionTimeout timeout in seconds
   */
  public void setConnectionTimeout(int connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

  /**
   * @return timeout in seconds
   */
  public int getConnectionTimeout() {
    return connectionTimeout;
  }

  @Override
  protected boolean isPoolingConnections(Object principal) {
    // replace original system name check with safer thread-context check;
    // was isPoolingEnabled() && principal != null && principal.equals(getSystemUsername());
    return isPoolingEnabled() && principal != null && isSystemContext();
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  protected LdapContext createLdapContext(Hashtable env) throws NamingException {
    // force switch to 'simple' if auth is 'none' and this is not the system context
    if ("none".equals(env.get(Context.SECURITY_AUTHENTICATION)) && !isSystemContext()) {
      env.put(Context.SECURITY_AUTHENTICATION, SIMPLE_AUTHENTICATION_MECHANISM_NAME);
    }
    if (StringUtils.isNotBlank(saslRealm)) {
      env.put("java.naming.security.sasl.realm", saslRealm);
    }
    if (connectionTimeout > 0) {
      // According to JDK docs these should be Strings representing the timeout in milliseconds
      env.put("com.sun.jndi.ldap.connect.timeout", Integer.toString(connectionTimeout * 1000));
      env.put("com.sun.jndi.ldap.read.timeout", Integer.toString(connectionTimeout * 1000));
    }
    return super.createLdapContext(env);
  }

  private static boolean isSystemContext() {
    return TRUE == systemContext.get();
  }

  void setSystemPassword(char[] password) {
    setSystemPassword(password == null ? null : String.valueOf(password));
  }
}
