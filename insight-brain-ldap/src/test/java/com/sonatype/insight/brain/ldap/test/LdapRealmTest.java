/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ldap.test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.inject.Inject;

import com.sonatype.insight.brain.configuration.ldap.LdapAuthenticationMethod;
import com.sonatype.insight.brain.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.configuration.ldap.LdapProtocol;
import com.sonatype.insight.brain.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.ldap.EmbeddedLdapServer;
import com.sonatype.insight.brain.ldap.LdapManager;
import com.sonatype.insight.brain.ldap.LdapRealm;
import com.sonatype.insight.brain.model.security.UserPrincipal;

import org.sonatype.guice.bean.containers.InjectedTest;

import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.PrincipalCollection;
import org.codehaus.plexus.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.sonatype.insight.brain.ldap.EmbeddedLdapServer.newEmbeddedLdapServer;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @since 1.7
 */
public class LdapRealmTest
    extends InjectedTest
{
  private static final String SYSPROP_SSLTRUSTSTORE = "javax.net.ssl.trustStore";

  private static final LdapServerDAO serverDao = new LdapServerDAO();
  private static final LdapUserMappingDAO userDao = new LdapUserMappingDAO();

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private LdapAuthenticationMethod authentication;

  private boolean authenticateWithBind;

  private LdapProtocol protocol;

  private EmbeddedLdapServer ldapServer;

  private LdapServer serverDetails;

  private LdapConnection connectionDetails;

  private LdapUserMapping userMappingDetails;

  private String oldTrustStore;

  @Inject
  private LdapManager manager;

  @Inject
  private LdapRealm realm;

  @Before
  public void initialize() {
    authentication = LdapAuthenticationMethod.NONE;
    authenticateWithBind = false;
    protocol = LdapProtocol.LDAP;
  }

  @Test
  public void testAnonymousAuth() throws Exception {
    startLdapServer().runAuthTests();
  }

  @Test
  public void testAnonymousAuthWithBind() throws Exception {
    withBind().startLdapServer().runAuthTests();
  }

  @Test
  public void testAnonymousAuthWithSsl() throws Exception {
    withSsl().startLdapServer().runAuthTests();
  }

  @Test
  public void testAnonymousAuthWithBindWithSsl() throws Exception {
    withBind().withSsl().startLdapServer().runAuthTests();
  }

  @Test
  public void testSimpleAuth() throws Exception {
    withSimpleAuth().startLdapServer().runAuthTests();
  }

  @Test
  public void testSimpleAuthWithBind() throws Exception {
    withSimpleAuth().withBind().startLdapServer().runAuthTests();
  }

  @Test
  public void testSimpleAuthWithSsl() throws Exception {
    withSimpleAuth().withSsl().startLdapServer().runAuthTests();
  }

  @Test
  public void testSimpleAuthWithBindWithSsl() throws Exception {
    withSimpleAuth().withBind().withSsl().startLdapServer().runAuthTests();
  }

  @Test
  public void testDigestAuth() throws Exception {
    withDigestAuth().startLdapServer().runAuthTests();
  }

  @Test
  public void testDigestAuthWithBind() throws Exception {
    withDigestAuth().withBind().startLdapServer().runAuthTests();
  }

  @Test
  public void testDigestAuthWithSsl() throws Exception {
    withDigestAuth().withSsl().startLdapServer().runAuthTests();
  }

  @Test
  public void testDigestAuthWithBindWithSsl() throws Exception {
    withDigestAuth().withBind().withSsl().startLdapServer().runAuthTests();
  }

  @Test
  public void testCramAuth() throws Exception {
    withCramAuth().startLdapServer().runAuthTests();
  }

  @Test
  public void testCramAuthWithBind() throws Exception {
    withCramAuth().withBind().startLdapServer().runAuthTests();
  }

  @Test
  public void testCramAuthWithSsl() throws Exception {
    withCramAuth().withSsl().startLdapServer().runAuthTests();
  }

  @Test
  public void testCramAuthWithBindWithSsl() throws Exception {
    withCramAuth().withBind().withSsl().startLdapServer().runAuthTests();
  }

  public void runAuthTests() {
    if (authentication.getMethod().endsWith("MD5")) {
      assertBadCredentials("anonymous", null);
      assertBadCredentials("anonymous", "");
      assertBadCredentials("anonymous", "guest");
      assertBadCredentials("anonymous", "s3cr3t");

      assertBadCredentials("test_sasl_user", null);
      assertBadCredentials("test_sasl_user", "");
      assertBadCredentials("test_sasl_user", "guest");
      assertGoodCredentials("test_sasl_user", "s3cr3t");
    }
    else {
      assertBadCredentials("anonymous", null);
      assertBadCredentials("anonymous", "");
      assertBadCredentials("anonymous", "guest");
      assertBadCredentials("anonymous", "far2simple");

      assertBadCredentials("test_user", null);
      assertBadCredentials("test_user", "");
      assertBadCredentials("test_user", "guest");
      assertGoodCredentials("test_user", "far2simple", "Gamma", "Theta", "Omega");
    }
  }

  public void assertGoodCredentials(String username, String password, String... groups) {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(username, password);
    AuthenticationInfo authenticationInfo = realm.getAuthenticationInfo(usernamePasswordToken);
    PrincipalCollection principalCollection = authenticationInfo.getPrincipals();
    assertNotNull(principalCollection);
    assertFalse(principalCollection.isEmpty());
    Iterator<?> principalIterator = principalCollection.iterator();
    Object principal = principalIterator.next();
    assertEquals(new UserPrincipal(username, false), principal);
    assertThat(((UserPrincipal) principal).membership, containsInAnyOrder(groups));
    assertFalse(principalIterator.hasNext());
    assertThat(principalCollection.getRealmNames(), hasSize(1));
    assertEquals(realm.getName(), principalCollection.getRealmNames().iterator().next());
  }

  public void assertBadCredentials(String username, String password) {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(username, password);
    try {
      realm.getAuthenticationInfo(usernamePasswordToken);
      fail("Expected IncorrectCredentialsException");
    }
    catch (AuthenticationException expected) {
    }
  }

  public LdapRealmTest withSimpleAuth() {
    authentication = LdapAuthenticationMethod.SIMPLE;
    return this;
  }

  public LdapRealmTest withDigestAuth() {
    authentication = LdapAuthenticationMethod.DIGESTMD5;
    return this;
  }

  public LdapRealmTest withCramAuth() {
    authentication = LdapAuthenticationMethod.CRAMMD5;
    return this;
  }

  public LdapRealmTest withSsl() {
    protocol = LdapProtocol.LDAPS;
    return this;
  }

  public LdapRealmTest withBind() {
    authenticateWithBind = true;
    return this;
  }

  public LdapRealmTest startLdapServer() throws Exception {

    serverDetails = new LdapServer();
    serverDetails.setName("Test Server");
    serverDao.insert(serverDetails);

    userMappingDetails = new LdapUserMapping();
    userMappingDetails.setServerId(serverDetails.getId());
    userMappingDetails.setUserBaseDN("ou=users");
    userMappingDetails.setUserObjectClass("person");
    userMappingDetails.setUserIDAttribute("uid");
    userMappingDetails.setUserRealNameAttribute("givenName");
    userMappingDetails.setUserEmailAttribute("mail");
    userMappingDetails.setUserSubtree(true);

    userMappingDetails.setGroupBaseDN("ou=groups");
    userMappingDetails.setGroupIDAttribute("cn");
    userMappingDetails.setGroupSubtree(true);
    userMappingDetails.setGroupMappingType(LdapGroupMappingType.STATIC);
    userMappingDetails.setGroupObjectClass("groupOfNames");
    userMappingDetails.setGroupMemberAttribute("member");
    userMappingDetails.setGroupMemberFormat("uid=${username}");

    if (!authenticateWithBind) {
      userMappingDetails.setUserPasswordAttribute("userPassword");
    }

    ldapServer = newEmbeddedLdapServer();

    connectionDetails = new LdapConnection();
    connectionDetails.setServerId(serverDetails.getId());
    connectionDetails.setProtocol(protocol);
    connectionDetails.setHostname(ldapServer.getHostname());
    connectionDetails.setSearchBase("dc=company,dc=com");
    connectionDetails.setSystemUsername(ldapServer.getSystemUserDN());
    connectionDetails.setSystemPassword(ldapServer.getSystemUserPassword());
    connectionDetails.setAuthenticationMethod(authentication);

    if (authentication == LdapAuthenticationMethod.SIMPLE) {
      ldapServer.setAuthenticationSimple();
    }
    else if (authentication == LdapAuthenticationMethod.DIGESTMD5) {
      ldapServer.setAuthenticationSasl(SupportedSaslMechanisms.DIGEST_MD5);
      connectionDetails.setSearchBase("ou=system"); // match embedded server base settings when using strong auth
      connectionDetails.setSystemUsername(ldapServer.getSystemUser()); // SASL-based auth expects username not DN
      connectionDetails.setSaslRealm(ldapServer.getSaslRealm());
      userMappingDetails.setUserBaseDN("");
    }
    else if (authentication == LdapAuthenticationMethod.CRAMMD5) {
      ldapServer.setAuthenticationSasl(SupportedSaslMechanisms.CRAM_MD5);
      connectionDetails.setSearchBase("ou=system"); // match embedded server base settings when using strong auth
      connectionDetails.setSystemUsername(ldapServer.getSystemUser()); // SASL-based auth expects username not DN
      connectionDetails.setSaslRealm(ldapServer.getSaslRealm());
      userMappingDetails.setUserBaseDN("");
    }

    if (protocol == LdapProtocol.LDAPS) {
      oldTrustStore = System.getProperty(SYSPROP_SSLTRUSTSTORE);
      System.setProperty(SYSPROP_SSLTRUSTSTORE, getTestResourceFile("/keystore/insight-testclient.ks")
          .getAbsolutePath());
      ldapServer.enableLdaps(getTestResourceFile("/keystore/insight-test.ks"), "secret");
    }

    userDao.insert(userMappingDetails);

    ldapServer.start();
    ldapServer.loadData("/ldap_users.ldif");

    connectionDetails.setPort(ldapServer.getPort());
    manager.saveConnection(connectionDetails);

    return this;
  }

  public File getTestResourceFile(String path) throws IOException {
    URL resource = getClass().getResource(path);
    assertNotNull(resource); // sanity check
    File tempFile = temporaryFolder.newFile();
    FileUtils.copyURLToFile(resource, tempFile);
    return tempFile;
  }

  @After
  public void stopLdapServer() throws Exception {
    if (protocol == LdapProtocol.LDAPS) {
      if (oldTrustStore != null) {
        System.setProperty(SYSPROP_SSLTRUSTSTORE, oldTrustStore);
      }
      else {
        System.clearProperty(SYSPROP_SSLTRUSTSTORE);
      }
    }

    if (ldapServer != null) {
      ldapServer.stop();
      ldapServer = null;
    }

    if (serverDetails != null) {
      serverDao.delete(serverDetails);
      serverDetails = null;
      connectionDetails = null;
      userMappingDetails = null;
    }

    assertThat(serverDao.getAll(), is(empty()));
  }
}
