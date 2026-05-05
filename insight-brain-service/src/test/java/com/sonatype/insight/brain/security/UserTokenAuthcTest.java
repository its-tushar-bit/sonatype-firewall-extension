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
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@RunWith(Parameterized.class)
@Category(SlowTest.class)
public class UserTokenAuthcTest
    extends AbstractBrainServiceIntegrationTest
{
  @Rule
  public TestLdapServer testLdapServer1 = new TestLdapServer();

  @Rule
  public TestLdapServer testLdapServer2 = new TestLdapServer();

  // Must match the username defined in the LDAP config for these tests.
  private static final String USERNAME = "testuser";

  private final boolean setupLdap;

  private final boolean isLdapUser;

  private final boolean isInternalUser;

  private final boolean isSamlUser;

  private final boolean isOAuth2User;

  private String realmId = "testRealmId";

  private final String userTokenPassword = "TestPassword";

  private UserToken userToken;

  private UserTokenDAO userTokenDAO;

  public UserTokenAuthcTest(
      boolean setupLdap,
      boolean isLdapUser,
      boolean isInternalUser,
      boolean isSamlUser,
      boolean isOAuth2User)
  {
    this.setupLdap = setupLdap;
    this.isLdapUser = isLdapUser;
    this.isInternalUser = isInternalUser;
    this.isSamlUser = isSamlUser;
    this.isOAuth2User = isOAuth2User;
  }

  @Parameterized.Parameters(name = "setupLdap={0}, isLdapUser={1}, isInternalUser={2}, isSamlUser={3}")
  public static Collection<Object[]> data() {
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

  @Before
  public void init() throws Exception {
    userTokenDAO = lookup(UserTokenDAO.class);

    if (setupLdap) {
      testLdapServer1.start();
      LdapServer ldapServer1 = tempEntity.newLdapServer("LDAP1");
      tempEntity.newLdapConnection(ldapServer1.getId(), this.testLdapServer1.getPort());
      tempEntity.newLdapUserMapping(ldapServer1.getId());

      testLdapServer2.start();
      LdapServer ldapServer2 = tempEntity.newLdapServer("LDAP2");
      tempEntity.newLdapConnection(ldapServer2.getId(), this.testLdapServer2.getPort());
      tempEntity.newLdapUserMapping(ldapServer2.getId());

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
      tempEntity.newSamlUser(USERNAME, "John", "Doe", "test.user@company.com",
          new LinkedHashSet<>(Arrays.asList("group1", "group2")));
      realmId = SamlRealm.ID;
    }
    if (isOAuth2User) {
      tempEntity.newSamlUser(USERNAME, "John", "Doe", "test.user@company.com",
          new LinkedHashSet<>(Arrays.asList("group1", "group2")));
      realmId = OAuth2Realm.ID;
    }
    if (isInternalUser) {
      // Be sure to keep the detail in-sync with the ldap defined user details for the testuser
      tempEntity.newUser(USERNAME, "John", "Doe", "test.user@company.com");
      realmId = InternalRealm.ID;
    }

    String hashedUserTokenPassword =
        getCLMServer().getInstance(PasswordService.class).encryptPassword(userTokenPassword);
    userToken = tempEntity.newUserToken(USERNAME, "TestUserCode", hashedUserTokenPassword, realmId);
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

  @Test
  public void testAuthenticate() throws Exception {
    HttpRequest request = restRequest();
    Date date = new Date();
    HttpResponse response =
        request.subpath(UserSessionResource.RESOURCE_PATH).auth(userToken.getUserCode(), userTokenPassword).get();

    if (isInternalUser || isLdapUser || isSamlUser) {
      assertResponseStatus(200, response);
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
      assertResponseStatus(200, response);
      assertThat(response.getSessionCookie()).isNull();
    }
    else {
      assertResponseStatus(401, response);
      assertThat(response.getBodyText()).isEqualTo("Invalid credentials. Please try again.");
      assertThat(response.getSessionCookie()).isNull();
    }
  }

  @Test
  public void testAuthenticate_LDAPUserDoesNotExistAnymore() throws Exception {
    // Run the test only if LDAP is configured and this is an LDAP user.
    Assume.assumeTrue(setupLdap && isLdapUser && !isInternalUser);

    // Change the username in the user token to simulate a token for a user that doesn't exist in LDAP anymore.
    userTokenDAO.delete(userToken);
    userToken = tempEntity.newUserToken("UserDoesNotExist", userToken.getUserCode(), userToken.getPassCode(),
        userToken.getRealmId());

    HttpRequest request = restRequest();
    HttpResponse response =
        request.path(UserSessionResource.RESOURCE_PATH).auth(userToken.getUserCode(), userTokenPassword).get();
    assertResponseStatus(401, response);
    assertThat(response.getSessionCookie()).isNull();
    assertThat(userTokenDAO.getById(userToken.getId())).isNull();
  }

  @Test
  public void testAuthenticate_WrongPassCode() throws Exception {
    HttpRequest request = restRequest();
    HttpResponse response =
        request.path(UserSessionResource.RESOURCE_PATH).auth(userToken.getUserCode(), "WrongPassword").get();
    assertResponseStatus(401, response);
    assertThat(response.getSessionCookie()).isNull();
  }
}
