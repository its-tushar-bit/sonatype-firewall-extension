package com.sonatype.insight.brain.ldap.test;

import java.util.Hashtable;

import javax.naming.AuthenticationException;
import javax.naming.AuthenticationNotSupportedException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;

import com.sonatype.insight.brain.ldap.EmbeddedLdapServer;

import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import static com.sonatype.insight.brain.ldap.EmbeddedLdapServer.newEmbeddedLdapServer;

/**
 * This may sound silly, but this test actually tests test LDAP server.
 */
public class EmbeddedLdapServerTest
{
  private static final String AUTH_CRAMMD5 = "CRAM-MD5";
  private static final String AUTH_DIGESTMD5 = "DIGEST-MD5";
  private static final String AUTH_SIMPLE = "simple";
  private static final String AUTH_NONE = "none";

  private EmbeddedLdapServer server;

  @After
  public void stopServer() throws Exception {
    if (server != null) {
      server.stop();
      server = null;
    }
  }

  @Test
  public void testAnonymous() throws Exception {
    server = newEmbeddedLdapServer();
    server.start();

    assertLogin(AUTH_NONE);
    assertLoginFailure(AUTH_SIMPLE, AUTH_DIGESTMD5, AUTH_CRAMMD5);
  }

  @Test
  public void testSimple() throws Exception {
    server = newEmbeddedLdapServer();
    server.setAuthenticationSimple();
    server.start();

    assertLogin(AUTH_SIMPLE);
    assertLoginFailure(AUTH_NONE, AUTH_DIGESTMD5, AUTH_CRAMMD5);
  }

  @Test
  public void testDigest() throws Exception {
    server = newEmbeddedLdapServer();
    server.setAuthenticationSasl(SupportedSaslMechanisms.DIGEST_MD5);
    server.start();

    assertLogin(AUTH_DIGESTMD5);
    assertLoginFailure(AUTH_NONE, AUTH_SIMPLE, AUTH_CRAMMD5);
  }

  @Test
  public void testCram() throws Exception {
    server = newEmbeddedLdapServer();
    server.setAuthenticationSasl(SupportedSaslMechanisms.CRAM_MD5);
    server.start();

    assertLogin(AUTH_CRAMMD5);
    assertLoginFailure(AUTH_NONE, AUTH_SIMPLE, AUTH_DIGESTMD5);
  }

  @Test
  public void testSaslRealm() throws Exception {
    server = newEmbeddedLdapServer();
    server.setAuthenticationSasl(SupportedSaslMechanisms.CRAM_MD5);
    server.start();

    Hashtable<String, Object> env = getEnv(AUTH_CRAMMD5);
    env.put("java.naming.security.sasl.realm", "wrongrealm");
    try {
      new InitialDirContext(env).close();
      Assert.fail();
    }
    catch (NamingException expected) {
    }

  }

  private void assertLogin(String... mechanisms) throws NamingException {
    for (String mechanism : mechanisms) {
      login(mechanism);
    }
  }

  private void assertLoginFailure(String... mechanisms) throws NamingException {
    for (String mechanism : mechanisms) {
      try {
        login(mechanism);
        Assert.fail();
      }
      catch (AuthenticationException expected) {
        // oddly, apacheds throws auth exception for unsupported simple auth
      }
      catch (AuthenticationNotSupportedException expected) {
      }
    }
  }

  private void login(String mechanism) throws NamingException {
    new InitialDirContext(getEnv(mechanism)).close();
  }

  private Hashtable<String, Object> getEnv(String mechanism) {
    Hashtable<String, Object> env = new Hashtable<String, Object>();
    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    env.put(Context.PROVIDER_URL, server.getUrl());
    env.put(Context.SECURITY_AUTHENTICATION, mechanism);
    if (AUTH_SIMPLE.equals(mechanism)) {
      env.put(Context.SECURITY_PRINCIPAL, server.getSystemUserDN());
      env.put(Context.SECURITY_CREDENTIALS, server.getSystemUserPassword());
    }
    else if (AUTH_CRAMMD5.equals(mechanism) || AUTH_DIGESTMD5.equals(mechanism)) {
      env.put(Context.SECURITY_PRINCIPAL, server.getSystemUser());
      env.put(Context.SECURITY_CREDENTIALS, server.getSystemUserPassword());
      env.put("java.naming.security.sasl.realm", server.getSaslRealm());
    }
    return env;
  }
}
