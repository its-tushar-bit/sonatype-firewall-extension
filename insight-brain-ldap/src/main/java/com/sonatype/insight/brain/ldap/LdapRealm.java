/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ldap;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;

/**
 * @since 1.7
 */
public class LdapRealm
{
  /**
   * @since 1.7
   */
  public static void testConnection(String url, String authenticationMechanism, String securityPrincipal,
      String securityPassword, String saslRealm) throws NamingException
  {
    Hashtable<String, Object> env = new Hashtable<String, Object>();
    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    env.put(Context.PROVIDER_URL, url);
    env.put(Context.SECURITY_AUTHENTICATION, authenticationMechanism);
    if (securityPrincipal != null) {
      env.put(Context.SECURITY_PRINCIPAL, securityPrincipal);
    }
    if (securityPassword != null) {
      env.put(Context.SECURITY_CREDENTIALS, securityPassword);
    }
    if (saslRealm != null) {
      env.put("java.naming.security.sasl.realm", saslRealm);
    }
    new InitialDirContext(env).close();
  }
}
