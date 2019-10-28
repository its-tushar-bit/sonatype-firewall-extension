/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.model.configuration.ldap.LdapAuthenticationMethod;
import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.model.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.model.configuration.ldap.LdapProtocol;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.model.security.Group;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.test.SslProperties;

import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.PrincipalCollection;
import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @since 1.7
 */
public class LdapRealmTest
    extends InjectedTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private LdapAuthenticationMethod authentication;

  private boolean authenticateWithBind;

  private LdapProtocol protocol;

  @Rule
  public TestLdapServer testLdapServer = new TestLdapServer();

  @Inject
  private LdapService ldapService;

  @Inject
  private LdapRealm realm;

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
      assertGoodCredentials("test_sasl_user1_1", "Test", "s3cr3t", Group.AUTHENTICATED_USERS_GROUP_ID);
    }
    else {
      assertEmptyPassword("anonymous", null);
      assertEmptyPassword("anonymous", "");
      assertBadCredentials("anonymous", "guest");
      assertBadCredentials("anonymous", "far2simple");

      assertEmptyPassword("test_user1_1", null);
      assertEmptyPassword("test_user1_1", "");
      assertBadCredentials("test_user1_1", "guest");
      assertGoodCredentials("test_user1_1", "Test", "far2simple", "Gamma", "Theta", "Omega",
          Group.AUTHENTICATED_USERS_GROUP_ID);
    }
  }

  private void assertGoodCredentials(String username, String displayName, String password, String... groups) {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(username, password);
    AuthenticationInfo authenticationInfo = realm.getAuthenticationInfo(usernamePasswordToken);
    PrincipalCollection principalCollection = authenticationInfo.getPrincipals();
    assertThat((Iterable<?>) principalCollection).hasSize(1);
    Object principal = principalCollection.iterator().next();
    assertThat(principal).isEqualTo(new UserPrincipal(username, displayName, ldapServer.getId()));
    assertThat(((UserPrincipal) principal).getMembership()).containsExactlyInAnyOrder(groups);
    assertThat(principalCollection.getRealmNames()).containsExactlyInAnyOrder(realm.getName());
  }

  private void assertBadCredentials(String username, String password) {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(username, password);
    assertThatThrownBy(() -> {
      realm.getAuthenticationInfo(usernamePasswordToken);
    }).isInstanceOf(AuthenticationException.class);
  }

  private void assertEmptyPassword(String username, String password) {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(username, password);
    assertThatThrownBy(() -> {
      realm.getAuthenticationInfo(usernamePasswordToken);
    }).isInstanceOf(AuthenticationException.class)
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
    protocol = LdapProtocol.LDAPS;
    return this;
  }

  private LdapRealmTest withBind() {
    authenticateWithBind = true;
    return this;
  }

  private LdapRealmTest startLdapServer() throws Exception {
    ldapServer = tempEntity.newLdapServer("Test Server");

    LdapUserMapping userMappingDetails = new LdapUserMapping();
    userMappingDetails.setServerId(ldapServer.getId());
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

    LdapConnection connectionDetails = new LdapConnection();
    connectionDetails.setServerId(ldapServer.getId());
    connectionDetails.setProtocol(protocol);
    connectionDetails.setHostname(testLdapServer.getHostname());
    connectionDetails.setSearchBase("dc=company,dc=com");
    connectionDetails.setSystemUsername(testLdapServer.getSystemUserDN());
    connectionDetails.setSystemPassword(testLdapServer.getSystemUserPassword());
    connectionDetails.setAuthenticationMethod(authentication);

    if (authentication == LdapAuthenticationMethod.SIMPLE) {
      testLdapServer.setAuthenticationSimple();
    }
    else if (authentication == LdapAuthenticationMethod.DIGESTMD5) {
      testLdapServer.setAuthenticationSasl(SupportedSaslMechanisms.DIGEST_MD5);
      connectionDetails.setSearchBase("ou=system"); // match embedded server base settings when using strong auth
      connectionDetails.setSystemUsername(testLdapServer.getSystemUser()); // SASL-based auth expects username not DN
      connectionDetails.setSaslRealm(testLdapServer.getSaslRealm());
      userMappingDetails.setUserBaseDN("");
    }
    else if (authentication == LdapAuthenticationMethod.CRAMMD5) {
      testLdapServer.setAuthenticationSasl(SupportedSaslMechanisms.CRAM_MD5);
      connectionDetails.setSearchBase("ou=system"); // match embedded server base settings when using strong auth
      connectionDetails.setSystemUsername(testLdapServer.getSystemUser()); // SASL-based auth expects username not DN
      connectionDetails.setSaslRealm(testLdapServer.getSaslRealm());
      userMappingDetails.setUserBaseDN("");
    }

    if (protocol == LdapProtocol.LDAPS) {
      testLdapServer.enableLdaps(SslProperties.SERVER_STORE_FILE, SslProperties.KEY_STORE_PASSWORD);
    }

    new LdapUserMappingDAO().insert(userMappingDetails);

    testLdapServer.start();
    testLdapServer.loadData("/" + getClass().getSimpleName() + "/ldap_users1.ldif");

    connectionDetails.setPort(testLdapServer.getPort());
    ldapService.saveConnection(connectionDetails);

    return this;
  }
}
