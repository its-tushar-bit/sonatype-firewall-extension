/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import java.util.Hashtable;
import javax.naming.AuthenticationException;
import javax.naming.AuthenticationNotSupportedException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;

import com.sonatype.insight.brain.SslSettings;
import com.sonatype.insight.test.networking.SslProperties;

import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * This may sound silly, but this test actually tests test LDAP server.
 */
public class EmbeddedLdapServerTest
{
  private static final String AUTH_CRAMMD5 = "CRAM-MD5";

  private static final String AUTH_DIGESTMD5 = "DIGEST-MD5";

  private static final String AUTH_SIMPLE = "simple";

  private static final String AUTH_NONE = "none";

  private EmbeddedLdapServer testLdapServer = new EmbeddedLdapServer();

  @Rule
  public SslSettings sslSettings = new SslSettings();

  @After
  public void stopServer() throws Exception {
    if (testLdapServer != null) {
      testLdapServer.stop();
      testLdapServer = null;
    }
  }

  @Test
  public void testAnonymous() throws Exception {
    testLdapServer.start();

    assertLogin(AUTH_NONE, AUTH_SIMPLE);
    assertLoginFailure(AUTH_DIGESTMD5, AUTH_CRAMMD5);
  }

  @Test
  public void testSimple() throws Exception {
    testLdapServer.setAuthenticationSimple();
    testLdapServer.start();

    assertLogin(AUTH_SIMPLE);
    assertLoginFailure(AUTH_NONE, AUTH_DIGESTMD5, AUTH_CRAMMD5);
  }

  @Test
  public void testDigest() throws Exception {
    testLdapServer.setAuthenticationSasl(SupportedSaslMechanisms.DIGEST_MD5);
    testLdapServer.start();

    assertLogin(AUTH_DIGESTMD5);
    assertLoginFailure(AUTH_NONE, AUTH_SIMPLE, AUTH_CRAMMD5);
  }

  @Test
  public void testCram() throws Exception {
    testLdapServer.setAuthenticationSasl(SupportedSaslMechanisms.CRAM_MD5);
    testLdapServer.start();

    assertLogin(AUTH_CRAMMD5);
    assertLoginFailure(AUTH_NONE, AUTH_SIMPLE, AUTH_DIGESTMD5);
  }

  @Test
  public void testInvalidSaslRealm() throws Exception {
    testLdapServer.setAuthenticationSasl(SupportedSaslMechanisms.DIGEST_MD5);
    testLdapServer.start();

    Hashtable<String, Object> env = getEnv(AUTH_DIGESTMD5);
    env.put("java.naming.security.sasl.realm", "wrongrealm");
    assertThatThrownBy(() -> new InitialDirContext(env).close()).isInstanceOf(NamingException.class)
        .hasMessageContaining("Nonexistent realm: wrongrealm");
  }

  @Test
  public void testNoSaslRealm() throws Exception {
    testLdapServer.setAuthenticationSasl(SupportedSaslMechanisms.DIGEST_MD5);
    testLdapServer.start();

    Hashtable<String, Object> env = getEnv(AUTH_DIGESTMD5);
    env.remove("java.naming.security.sasl.realm");

    // this is apparently client-only affair, so this is expected to work

    new InitialDirContext(env).close();
  }

  @Test
  public void testLdaps() throws Exception {
    testLdapServer.enableLdaps(SslProperties.SERVER_STORE_FILE, SslProperties.KEY_STORE_PASSWORD);
    testLdapServer.start();

    sslSettings.use();
    assertLogin(AUTH_NONE);
  }

  private void assertLogin(String... mechanisms) throws NamingException {
    for (String mechanism : mechanisms) {
      login(mechanism);
    }
  }

  private void assertLoginFailure(String... mechanisms) {
    for (String mechanism : mechanisms) {
      assertThatThrownBy(() -> {
        login(mechanism);
        // oddly, apacheds throws auth exception for unsupported simple auth
      }).isInstanceOfAny(AuthenticationException.class, AuthenticationNotSupportedException.class);
    }
  }

  private void login(String mechanism) throws NamingException {
    new InitialDirContext(getEnv(mechanism)).close();
  }

  private Hashtable<String, Object> getEnv(String mechanism) {
    Hashtable<String, Object> env = new Hashtable<>();
    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    env.put(Context.PROVIDER_URL, testLdapServer.getUrl());
    env.put(Context.SECURITY_AUTHENTICATION, mechanism);
    if (AUTH_SIMPLE.equals(mechanism)) {
      env.put(Context.SECURITY_PRINCIPAL, testLdapServer.getSystemUserDN());
      env.put(Context.SECURITY_CREDENTIALS, testLdapServer.getSystemUserPassword());
    }
    else if (AUTH_CRAMMD5.equals(mechanism) || AUTH_DIGESTMD5.equals(mechanism)) {
      env.put(Context.SECURITY_PRINCIPAL, testLdapServer.getSystemUser());
      env.put(Context.SECURITY_CREDENTIALS, testLdapServer.getSystemUserPassword());
      env.put("java.naming.security.sasl.realm", testLdapServer.getSaslRealm());
    }
    return env;
  }
}
