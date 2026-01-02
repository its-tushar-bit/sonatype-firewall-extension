/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import javax.inject.Inject;

import com.sonatype.insight.brain.SslSettings;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.model.configuration.ldap.LdapAuthenticationMethod;
import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.model.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.model.configuration.ldap.LdapProtocol;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.model.security.Group;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.testing.BrainInjectedTest;
import com.sonatype.insight.test.networking.SslProperties;

import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.PrincipalCollection;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.sonatype.insight.brain.common.test.SlowTest;

/**
 * @since 1.7
 */
@Category(SlowTest.class)
public class LdapRealmTest
    extends BrainInjectedTest
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private LdapAuthenticationMethod authentication;

  private boolean authenticateWithBind;

  private LdapProtocol protocol;

  @Rule
  public TestLdapServer testLdapServer = new TestLdapServer();

  @Rule
  public SslSettings sslSettings = new SslSettings();

  @Inject
  private LdapService ldapService;

  @Inject
  private LdapRealm realm;

  @Inject
  private LdapUserMappingDAO ldapUserMappingDAO;

  private LdapServer ldapServer;

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

  private void runAuthTests() {
    if (authentication.getMethod().endsWith("MD5")) {
      assertEmptyPassword("anonymous", null);
      assertEmptyPassword("anonymous", "");
      assertBadCredentials("anonymous", "guest");
      assertBadCredentials("anonymous", "s3cr3t");

      assertEmptyPassword("test_sasl_user1_1", null);
      assertEmptyPassword("test_sasl_user1_1", "");
      assertBadCredentials("test_sasl_user1_1", "guest");
      assertGoodCredentials("test_sasl_user1_1", "test_sasl_user1_1", "Test", "s3cr3t",
          Group.AUTHENTICATED_USERS_GROUP_ID);
      // Verify that the input username is case-insensitive.
      assertGoodCredentials("tesT_sasl_User1_1", "test_sasl_user1_1", "Test", "s3cr3t",
          Group.AUTHENTICATED_USERS_GROUP_ID);
    }
    else {
      assertEmptyPassword("anonymous", null);
      assertEmptyPassword("anonymous", "");
      assertBadCredentials("anonymous", "guest");
      assertBadCredentials("anonymous", "far2simple");

      assertEmptyPassword("test_user1_1", null);
      assertEmptyPassword("test_user1_1", "");
      assertBadCredentials("test_user1_1", "guest");
      assertGoodCredentials("test_user1_1", "test_user1_1", "Test", "far2simple", "Gamma", "Theta", "Omega",
          Group.AUTHENTICATED_USERS_GROUP_ID);
      // Verify that the input username is case-insensitive.
      assertGoodCredentials("tesT_User1_1", "test_user1_1", "Test", "far2simple", "Gamma", "Theta", "Omega",
          Group.AUTHENTICATED_USERS_GROUP_ID);
    }
  }

  private void assertGoodCredentials(
      String inputUsername,
      String expectedUsername,
      String displayName,
      String password,
      String... groups)
  {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(inputUsername, password);
    AuthenticationInfo authenticationInfo = realm.getAuthenticationInfo(usernamePasswordToken);
    PrincipalCollection principalCollection = authenticationInfo.getPrincipals();
    assertThat((Iterable<?>) principalCollection).hasSize(1);
    Object principal = principalCollection.iterator().next();
    assertThat(principal).isEqualTo(new UserPrincipal(expectedUsername, displayName, ldapServer.getId()));
    assertThat(((UserPrincipal) principal).getMembership()).containsExactlyInAnyOrder(groups);
    assertThat(principalCollection.getRealmNames()).containsExactlyInAnyOrder(realm.getName());
  }

  private void assertBadCredentials(String username, String password) {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(username, password);
    assertThatThrownBy(() -> realm.getAuthenticationInfo(usernamePasswordToken))
        .isInstanceOf(AuthenticationException.class);
  }

  private void assertEmptyPassword(String username, String password) {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(username, password);
    assertThatThrownBy(() -> realm.getAuthenticationInfo(usernamePasswordToken))
        .isInstanceOf(AuthenticationException.class)
        .satisfies(e -> assertThat(e.getCause()).hasMessage("Password must not be empty"));
  }

  private LdapRealmTest withSimpleAuth() {
    authentication = LdapAuthenticationMethod.SIMPLE;
    return this;
  }

  private LdapRealmTest withDigestAuth() {
    authentication = LdapAuthenticationMethod.DIGESTMD5;
    return this;
  }

  private LdapRealmTest withCramAuth() {
    authentication = LdapAuthenticationMethod.CRAMMD5;
    return this;
  }

  private LdapRealmTest withSsl() {
    sslSettings.use();
    protocol = LdapProtocol.LDAPS;
    return this;
  }

  private LdapRealmTest withBind() {
    authenticateWithBind = true;
    return this;
  }

  private LdapRealmTest startLdapServer() throws Exception {
    ldapServer = tempEntity.newLdapServer("Test Server");

    LdapUserMapping ldapUserMapping = new LdapUserMapping();
    ldapUserMapping.setServerId(ldapServer.getId());
    ldapUserMapping.setUserBaseDN("ou=users");
    ldapUserMapping.setUserObjectClass("person");
    ldapUserMapping.setUserIDAttribute("uid");
    ldapUserMapping.setUserRealNameAttribute("givenName");
    ldapUserMapping.setUserEmailAttribute("mail");
    ldapUserMapping.setUserSubtree(true);

    ldapUserMapping.setGroupBaseDN("ou=groups");
    ldapUserMapping.setGroupIDAttribute("cn");
    ldapUserMapping.setGroupSubtree(true);
    ldapUserMapping.setGroupMappingType(LdapGroupMappingType.STATIC);
    ldapUserMapping.setGroupObjectClass("groupOfNames");
    ldapUserMapping.setGroupMemberAttribute("member");
    ldapUserMapping.setGroupMemberFormat("uid=${username}");

    if (!authenticateWithBind) {
      ldapUserMapping.setUserPasswordAttribute("userPassword");
    }

    LdapConnection ldapConnection = new LdapConnection();
    ldapConnection.setServerId(ldapServer.getId());
    ldapConnection.setProtocol(protocol);
    ldapConnection.setHostname(testLdapServer.getHostname());
    ldapConnection.setSearchBase("dc=company,dc=com");
    ldapConnection.setSystemUsername(testLdapServer.getSystemUserDN());
    ldapConnection.setSystemPassword(testLdapServer.getSystemUserPassword());
    ldapConnection.setAuthenticationMethod(authentication);

    if (authentication == LdapAuthenticationMethod.SIMPLE) {
      testLdapServer.setAuthenticationSimple();
    }
    else if (authentication == LdapAuthenticationMethod.DIGESTMD5) {
      testLdapServer.setAuthenticationSasl(SupportedSaslMechanisms.DIGEST_MD5);
      ldapConnection.setSearchBase("ou=system"); // match embedded server base settings when using strong auth
      ldapConnection.setSystemUsername(testLdapServer.getSystemUser()); // SASL-based auth expects username not DN
      ldapConnection.setSaslRealm(testLdapServer.getSaslRealm());
      ldapUserMapping.setUserBaseDN("");
    }
    else if (authentication == LdapAuthenticationMethod.CRAMMD5) {
      testLdapServer.setAuthenticationSasl(SupportedSaslMechanisms.CRAM_MD5);
      ldapConnection.setSearchBase("ou=system"); // match embedded server base settings when using strong auth
      ldapConnection.setSystemUsername(testLdapServer.getSystemUser()); // SASL-based auth expects username not DN
      ldapConnection.setSaslRealm(testLdapServer.getSaslRealm());
      ldapUserMapping.setUserBaseDN("");
    }

    if (protocol == LdapProtocol.LDAPS) {
      testLdapServer.enableLdaps(SslProperties.SERVER_STORE_FILE, SslProperties.KEY_STORE_PASSWORD);
    }

    ldapUserMappingDAO.insert(ldapUserMapping);

    testLdapServer.start();
    testLdapServer.loadData("/" + getClass().getSimpleName() + "/ldap_users1.ldif");

    ldapConnection.setPort(testLdapServer.getPort());
    ldapService.upsertLdapConnection(ldapConnection);

    return this;
  }
}
