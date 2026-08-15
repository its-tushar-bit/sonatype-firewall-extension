/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.configuration.ldap.TestLdapServer;
import com.sonatype.insight.brain.dataaccess.security.UserTokenDAO;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.security.Group;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.brain.security.UserSessionResource.AuthenticationStatus;
import com.sonatype.insight.brain.security.oauth2.OAuth2Realm;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kept in the original package/simple name because {@link #init} resolves the {@code /UserTokenAuthcTest/...}
 * LDAP fixtures via {@code getClass().getSimpleName()}. Each legacy {@code @RunWith(Parameterized.class)}
 * constructor scenario (setupLdap/isLdapUser/isInternalUser/isSamlUser/isOAuth2User) is ported to a
 * {@code @ParameterizedTest} per test method, with the legacy {@code @Before} body inlined as
 * {@link #init(boolean, boolean, boolean, boolean, boolean)} since JUnit 5 has no per-scenario
 * {@code @BeforeEach} parameter injection.
 */
@IqH2Test
class UserTokenAuthcTest
{
  private IqTestContext ctx;

  // Must match the username defined in the LDAP config for these tests.
  private static final String USERNAME = "testuser";

  private final TestLdapServer testLdapServer1 = new TestLdapServer();

  private final TestLdapServer testLdapServer2 = new TestLdapServer();

  private final String userTokenPassword = "TestPassword";

  private String realmId = "testRealmId";

  private boolean isLdapUser;

  private boolean isInternalUser;

  private boolean isSamlUser;

  private boolean isOAuth2User;

  private UserToken userToken;

  private UserTokenDAO userTokenDAO;

  @AfterEach
  void tearDown() throws Exception {
    testLdapServer1.stop();
    testLdapServer2.stop();
  }

  static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
      {false, false, false, false, false}, // totally unknown user, no LDAP configured
      {true, false, false, false, false}, // totally unknown user, LDAP configured
      {false, false, true, false, false}, // user only present in local db, no LDAP configured
      {true, false, true, false, false}, // user only present in local db, LDAP configured
      {true, true, false, false, false}, // user only present in LDAP
      {true, true, true, false, false}, // user present in local db and LDAP
      {false, false, false, true, false}, // user only present in SAML
      {false, false, true, true, false}, // user present in local db and SAML
      {false, false, true, false, true}, // user only present in OAuth2
      {false, false, true, false, true} // user present in local db and OAuth2
    });
  }

  static Stream<Arguments> userScenarios() {
    return data().stream().map(row -> Arguments.of(row[0], row[1], row[2], row[3], row[4]));
  }

  private void init(
      boolean setupLdap,
      boolean isLdapUser,
      boolean isInternalUser,
      boolean isSamlUser,
      boolean isOAuth2User) throws Exception
  {
    this.isLdapUser = isLdapUser;
    this.isInternalUser = isInternalUser;
    this.isSamlUser = isSamlUser;
    this.isOAuth2User = isOAuth2User;

    userTokenDAO = ctx.lookup(UserTokenDAO.class);

    if (setupLdap) {
      testLdapServer1.start();
      LdapServer ldapServer1 = ctx.tempEntity().newLdapServer("LDAP1");
      ctx.tempEntity().newLdapConnection(ldapServer1.getId(), this.testLdapServer1.getPort());
      ctx.tempEntity().newLdapUserMapping(ldapServer1.getId());

      testLdapServer2.start();
      LdapServer ldapServer2 = ctx.tempEntity().newLdapServer("LDAP2");
      ctx.tempEntity().newLdapConnection(ldapServer2.getId(), this.testLdapServer2.getPort());
      ctx.tempEntity().newLdapUserMapping(ldapServer2.getId());

      if (isLdapUser) {
        this.testLdapServer1.loadData("/" + getClass().getSimpleName() + "/ldap_users1.ldif");
        this.testLdapServer2.loadData("/" + getClass().getSimpleName() + "/ldap_users2.ldif");

        // The user exists in both LDAP servers, but it has different group memberships.
        // We create the UserToken for the second LDAP server,
        // so the user should have the memberships from the second LDAP server.
        realmId = ldapServer2.getId();
      }
    }
    if (isSamlUser) {
      ctx.tempEntity()
          .newSamlUser(USERNAME, "John", "Doe", "test.user@company.com",
              new LinkedHashSet<>(Arrays.asList("group1", "group2")));
      realmId = SamlRealm.ID;
    }
    if (isOAuth2User) {
      ctx.tempEntity()
          .newSamlUser(USERNAME, "John", "Doe", "test.user@company.com",
              new LinkedHashSet<>(Arrays.asList("group1", "group2")));
      realmId = OAuth2Realm.ID;
    }
    if (isInternalUser) {
      // Be sure to keep the detail in-sync with the ldap defined user details for the testuser
      ctx.tempEntity().newUser(USERNAME, "John", "Doe", "test.user@company.com");
      realmId = InternalRealm.ID;
    }

    String hashedUserTokenPassword = ctx.lookup(PasswordService.class).encryptPassword(userTokenPassword);
    userToken = ctx.tempEntity().newUserToken(USERNAME, "TestUserCode", hashedUserTokenPassword, realmId);
    assertThat(userTokenDAO.getById(userToken.getId()).getLastAccessTime()).isNull();
  }

  private Set<String> getExpectedGroupNames() {
    Set<String> groupNames = new HashSet<>();
    groupNames.add(Group.AUTHENTICATED_USERS_GROUP_ID);
    if (isLdapUser && !isInternalUser) {
      groupNames.add("Alpha2");
    }
    if ((isSamlUser || isOAuth2User) && !isInternalUser) {
      groupNames.add("group1");
      groupNames.add("group2");
    }
    return groupNames;
  }

  @ParameterizedTest(name = "setupLdap={0}, isLdapUser={1}, isInternalUser={2}, isSamlUser={3}")
  @MethodSource("userScenarios")
  void testAuthenticate(
      boolean setupLdap,
      boolean isLdapUser,
      boolean isInternalUser,
      boolean isSamlUser,
      boolean isOAuth2User) throws Exception
  {
    init(setupLdap, isLdapUser, isInternalUser, isSamlUser, isOAuth2User);

    HttpRequest request = ctx.restRequest();
    Date date = new Date();
    HttpResponse response =
        request.subpath(UserSessionResource.RESOURCE_PATH).auth(userToken.getUserCode(), userTokenPassword).get();

    if (isInternalUser || isLdapUser || isSamlUser) {
      ctx.assertResponseStatus(200, response);
      assertThat(response.getSessionCookie()).isNotNull();
      AuthenticationStatus authStatus = response.getBody(AuthenticationStatus.class);
      assertThat(authStatus.isAuthenticated()).isTrue();
      assertThat(authStatus.isInternalUser()).isFalse();
      assertThat(authStatus.getUsername()).isEqualTo(USERNAME);
      assertThat(authStatus.getDisplayName()).isEqualTo("John Doe");
      assertThat(authStatus.getGroups()).isEqualTo(getExpectedGroupNames());
      Date lastAccessTime = userTokenDAO.getById(userToken.getId()).getLastAccessTime();
      assertThat(lastAccessTime).isAfterOrEqualTo(date);

      response = request.subpath(PublicApiPaths.ORG_RESOURCE_PATH).get();
      ctx.assertResponseStatus(200, response);
      assertThat(response.getSessionCookie()).isNull();
    }
    else {
      ctx.assertResponseStatus(401, response);
      assertThat(response.getBodyText()).isEqualTo("Invalid credentials. Please try again.");
      assertThat(response.getSessionCookie()).isNull();
    }
  }

  @ParameterizedTest(name = "setupLdap={0}, isLdapUser={1}, isInternalUser={2}, isSamlUser={3}")
  @MethodSource("userScenarios")
  void testAuthenticate_LDAPUserDoesNotExistAnymore(
      boolean setupLdap,
      boolean isLdapUser,
      boolean isInternalUser,
      boolean isSamlUser,
      boolean isOAuth2User) throws Exception
  {
    init(setupLdap, isLdapUser, isInternalUser, isSamlUser, isOAuth2User);

    // Run the test only if LDAP is configured and this is an LDAP user.
    Assumptions.assumeTrue(setupLdap && isLdapUser && !isInternalUser);

    // Change the username in the user token to simulate a token for a user that doesn't exist in LDAP anymore.
    userTokenDAO.delete(userToken);
    userToken = ctx.tempEntity()
        .newUserToken("UserDoesNotExist", userToken.getUserCode(), userToken.getPassCode(),
            userToken.getRealmId());

    HttpRequest request = ctx.restRequest();
    HttpResponse response =
        request.path(UserSessionResource.RESOURCE_PATH).auth(userToken.getUserCode(), userTokenPassword).get();
    ctx.assertResponseStatus(401, response);
    assertThat(response.getSessionCookie()).isNull();
    assertThat(userTokenDAO.getById(userToken.getId())).isNull();
  }

  @ParameterizedTest(name = "setupLdap={0}, isLdapUser={1}, isInternalUser={2}, isSamlUser={3}")
  @MethodSource("userScenarios")
  void testAuthenticate_WrongPassCode(
      boolean setupLdap,
      boolean isLdapUser,
      boolean isInternalUser,
      boolean isSamlUser,
      boolean isOAuth2User) throws Exception
  {
    init(setupLdap, isLdapUser, isInternalUser, isSamlUser, isOAuth2User);

    HttpRequest request = ctx.restRequest();
    HttpResponse response =
        request.path(UserSessionResource.RESOURCE_PATH).auth(userToken.getUserCode(), "WrongPassword").get();
    ctx.assertResponseStatus(401, response);
    assertThat(response.getSessionCookie()).isNull();
  }
}
