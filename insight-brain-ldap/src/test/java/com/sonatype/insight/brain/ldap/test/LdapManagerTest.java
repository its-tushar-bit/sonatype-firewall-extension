/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ldap.test;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.naming.AuthenticationException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import com.sonatype.insight.brain.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.configuration.ldap.LdapProtocol;
import com.sonatype.insight.brain.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapConnectionDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.ldap.LdapGroup;
import com.sonatype.insight.brain.ldap.LdapManager;
import com.sonatype.insight.brain.ldap.LdapUser;
import com.sonatype.insight.brain.ldap.TestLdapServer;

import org.eclipse.sisu.launch.InjectedTest;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;

/**
 * @since 1.7
 */
public class LdapManagerTest
    extends InjectedTest
{
  private final LdapServerDAO serverDao = new LdapServerDAO();

  @Rule
  public TestLdapServer ldapServer = new TestLdapServer();

  private LdapServer serverDetails;

  @Inject
  private LdapManager manager;

  @Test
  public void testConnection() throws Exception {
    startLdapServer();

    manager.testConnection(createLdapConnection());
  }

  @Test
  public void testConnection_EscapedUrl() throws Exception {
    startLdapServer();

    LdapConnection connection = createLdapConnection();
    // Search base with space will be escaped with %20.
    connection.setSearchBase("dc=acme brick,dc=com");

    manager.testConnection(connection);
  }

  @Test
  public void testConnectionTimeout() throws Exception {
    ServerSocket socket = new ServerSocket(0);
    try {
      long begin = 0, end = 0;

      LdapConnection conn = new LdapConnection();
      conn.setHostname("localhost");
      conn.setPort(socket.getLocalPort());

      // test very short timeout

      try {
        conn.setConnectionTimeout(1);
        begin = System.currentTimeMillis();
        manager.testConnection(conn);

        fail("Expected NamingException");
      }
      catch (NamingException expected) {
        end = System.currentTimeMillis();
      }

      assertThat(Double.valueOf(end - begin), is(closeTo(1300, 500)));

      // test slightly longer timeout

      try {
        conn.setConnectionTimeout(5);
        begin = System.currentTimeMillis();
        manager.testConnection(conn);

        fail("Expected NamingException");
      }
      catch (NamingException expected) {
        end = System.currentTimeMillis();
      }

      assertThat(Double.valueOf(end - begin), is(closeTo(5300, 500)));
    }
    finally {
      socket.close();
    }
  }

  @Test
  public void testRetryDelay() throws Exception {
    ServerSocket socket = new ServerSocket(0);
    try {

      serverDetails = new LdapServer();
      serverDetails.setName("Test Server");
      serverDao.insert(serverDetails);

      LdapConnection conn = createLdapConnection();
      conn.setHostname("localhost");
      conn.setPort(socket.getLocalPort());
      conn.setConnectionTimeout(1);
      conn.setRetryDelay(5);
      manager.saveConnection(conn);

      new LdapUserMappingDAO().insert(createUserMapping());

      // force three failures by attempting auth against the dangling socket

      for (int failures = 0; failures < 3; failures++) {
        try {
          manager.authenticateUser("user", "pass".toCharArray());
          fail("Expected NamingException");
        }
        catch (NamingException expected) {
          assertThat(expected.getMessage(), containsString("read timed out"));
        }
      }

      long lastFailure = System.currentTimeMillis();

      // the next requests should be ignored while the retry delay is active

      for (int failures = 0; failures < 3; failures++) {
        try {
          manager.authenticateUser("user", "pass".toCharArray());
          fail("Expected NamingException");
        }
        catch (NamingException expected) {
          assertThat(expected.getMessage(), containsString("Delaying retry"));
        }
      }

      while (System.currentTimeMillis() - lastFailure <= 5000) {
        Thread.sleep(200);
      }

      // the next request should NOT be ignored because the delay has expired

      try {
        manager.authenticateUser("user", "pass".toCharArray());
        fail("Expected NamingException");
      }
      catch (NamingException expected) {
        assertThat(expected.getMessage(), containsString("read timed out"));
      }
    }
    finally {
      socket.close();
    }
  }

  @Test
  public void testBadSearchBase() throws Exception {
    startLdapServer();

    LdapConnection conn = createLdapConnection();
    conn.setSearchBase("!@£$%^&*()");

    try {
      manager.testConnection(conn);
      fail("Expected NamingException");
    }
    catch (NamingException expected) {
    }
  }

  private void setSearchBase() {
    LdapConnection conn = createLdapConnection();
    conn.setSearchBase("dc=company,dc=com");
    manager.saveConnection(conn);
  }

  @Test
  public void testUserMapping() throws Exception {
    startLdapServer();
    setSearchBase();

    LdapUserMapping umap = createUserMapping();

    List<LdapUser> users = manager.testUserMapping(umap, -1);
    assertThat(users, hasSize(3));

    Collections.sort(users); // sorts on username

    LdapUser user = users.get(0);
    assertThat(user.getUsername(), is("test*user"));
    assertThat(user.getDn(), is("uid=test*user,ou=users,dc=company,dc=com"));
    assertThat(user.getRealName(), is("Test*User"));
    assertThat(user.getEmail(), is("test.user3@company.com"));
    assertThat(user.getPassword(), nullValue()); // make sure password is not passed back
    assertThat(user.getMembership(), nullValue());

    user = users.get(1);
    assertThat(user.getUsername(), is("test_user"));
    assertThat(user.getDn(), is("uid=test_user,ou=users,dc=company,dc=com"));
    assertThat(user.getRealName(), is("Test User"));
    assertThat(user.getEmail(), is("test.user@company.com"));
    assertThat(user.getPassword(), nullValue()); // make sure password is not passed back
    assertThat(user.getMembership(), nullValue());

    user = users.get(2);
    assertThat(user.getUsername(), is("test_user2"));
    assertThat(user.getDn(), is("uid=test_user2,ou=users,dc=company,dc=com"));
    assertThat(user.getRealName(), is("Test User 2"));
    assertThat(user.getEmail(), is("test.user2@company.com"));
    assertThat(user.getPassword(), nullValue()); // make sure password is not passed back
    assertThat(user.getMembership(), nullValue());
  }

  @Test
  public void testDynamicGroupMapping() throws Exception {
    startLdapServer();
    setSearchBase();

    LdapUserMapping umap = createUserMapping();
    umap.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    umap.setUserMemberOfGroupAttribute("departmentNumber");

    List<LdapUser> users = manager.testUserMapping(umap, -1);
    assertThat(users, hasSize(3));

    Collections.sort(users); // sorts on username

    assertThat(users.get(0).getMembership(), containsInAnyOrder("ab", "bc", "bx"));
    assertThat(users.get(1).getMembership(), containsInAnyOrder("ab", "abc", "xb"));
    assertThat(users.get(2).getMembership(), containsInAnyOrder("ab", "bc", "bx"));
  }

  @Test
  public void testStaticGroupMapping() throws Exception {
    startLdapServer();
    setSearchBase();

    LdapUserMapping umap = createUserMapping();
    umap.setGroupMappingType(LdapGroupMappingType.STATIC);
    umap.setGroupObjectClass("groupOfUniqueNames");
    umap.setGroupMemberAttribute("uniqueMember");
    umap.setGroupMemberFormat("${dn}");

    List<LdapUser> users = manager.testUserMapping(umap, -1);
    assertThat(users, hasSize(3));

    Collections.sort(users); // sorts on username

    assertThat(users.get(0).getMembership(), hasSize(0));
    assertThat(users.get(1).getMembership(), containsInAnyOrder("Alpha"));
    assertThat(users.get(2).getMembership(), containsInAnyOrder("Beta"));

    umap.setGroupMappingType(LdapGroupMappingType.STATIC);
    umap.setGroupObjectClass("groupOfNames");
    umap.setGroupMemberAttribute("member");
    umap.setGroupMemberFormat("uid=${username}");

    users = manager.testUserMapping(umap, -1);
    assertThat(users, hasSize(3));

    Collections.sort(users); // sorts on username

    assertThat(users.get(0).getMembership(), hasSize(0));
    assertThat(users.get(1).getMembership(), containsInAnyOrder("Gamma", "Theta", "Omega"));
    assertThat(users.get(2).getMembership(), containsInAnyOrder("Theta"));
  }

  @Test
  public void testUserLogin() throws Exception {
    startLdapServer();
    setSearchBase();

    LdapUserMapping umap = createUserMapping();

    umap.setUserPasswordAttribute(null); // AUTH-via-BIND

    try {
      manager.testUserLogin(umap, "test_user", "badGuess".toCharArray());
      fail("Expected NamingException");
    }
    catch (NamingException expected) {
      manager.testUserLogin(umap, "test_user", "far2simple".toCharArray());
    }

    umap.setUserPasswordAttribute("userPassword"); // AUTH-via-ATTRIBUTE

    try {
      manager.testUserLogin(umap, "test_user", "badGuess".toCharArray());
      fail("Expected NamingException");
    }
    catch (NamingException expected) {
      manager.testUserLogin(umap, "test_user", "far2simple".toCharArray());
    }
  }

  @Test
  public void testRejectEmptyPasswordAsPerRfc4513Section5_1_2() throws Exception {
    startLdapServer();
    setSearchBase();
    LdapUserMapping umap = createUserMapping();
    new LdapUserMappingDAO().insert(umap);

    try {
      manager.authenticateUser("test_user", "".toCharArray());
      fail("Expected exception");
    }
    catch (AuthenticationException expected) {
      // expected
    }

    try {
      manager.testUserLogin(umap, "test_user", "".toCharArray());
      fail("Expected exception");
    }
    catch (AuthenticationException expected) {
      // expected
    }
  }


  @Test
  public void testAuthenticateUser_usernameLeakViaInjection() throws Exception {
    startLdapServer();
    setSearchBase();
    LdapUserMapping umap = createUserMapping();
    new LdapUserMappingDAO().insert(umap);

    try {
      //prior to the sanitization of query parameters, an AuthenticationException
      //would've been thrown here, and it leaked the first user name in the system
      manager.authenticateUser("*)(uid=*))(|(uid=*", "invalid".toCharArray());
      fail("authentication should have failed");
    }
    catch (NameNotFoundException e) {
      assertThat(e.getMessage(), not(containsString("test_user")));
    }
  }

  @Test(expected = NameNotFoundException.class)
  public void testAuthenticateUser_wildcardMatchingNotExpected() throws Exception {
    startLdapServer();
    setSearchBase();
    LdapUserMapping umap = createUserMapping();
    new LdapUserMappingDAO().insert(umap);

    //previous to escaping characters in the ldap query, this auth check would have succeeded
    //matching against the first test user in the system
    manager.authenticateUser("test*", "test".toCharArray());
  }

  @Test
  public void testAuthenticateUser_wildcardMatchingEscapedValue() throws Exception {
    startLdapServer();
    setSearchBase();
    LdapUserMapping umap = createUserMapping();
    new LdapUserMappingDAO().insert(umap);

    manager.authenticateUser("test*user", "te*st".toCharArray());
  }

  @Test
  public void testGetUser() throws Exception {
    startLdapServer();
    setSearchBase();
    LdapUserMapping umap = createUserMapping();
    umap.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    umap.setUserMemberOfGroupAttribute("departmentNumber");
    new LdapUserMappingDAO().insert(umap);

    LdapUser user = manager.getUser("test_user");
    assertThat(user, is(notNullValue()));
    assertThat(user.getUsername(), is("test_user"));
    assertThat(user.getRealName(), is("Test User"));
    assertThat(user.getMembership(), containsInAnyOrder("ab", "xb", "abc"));
  }

  @Test(expected = NameNotFoundException.class)
  public void testGetUser_NoWildcardMatching() throws Exception {
    startLdapServer();
    setSearchBase();
    LdapUserMapping umap = createUserMapping();
    new LdapUserMappingDAO().insert(umap);

    manager.getUser("test_*");
  }

  @Test
  public void testGetUser_WildcardEscaped() throws Exception {
    startLdapServer();
    setSearchBase();
    LdapUserMapping umap = createUserMapping();
    umap.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    umap.setUserMemberOfGroupAttribute("departmentNumber");
    new LdapUserMappingDAO().insert(umap);

    LdapUser user = manager.getUser("test*user");
    assertThat(user, is(notNullValue()));
    assertThat(user.getUsername(), is("test*user"));
    assertThat(user.getRealName(), is("Test*User"));
    assertThat(user.getMembership(), containsInAnyOrder("ab", "bc", "bx"));
  }

  @Test
  public void testGetUsers() throws Exception {
    startLdapServer();
    setSearchBase();

    LdapUserMapping umap = createUserMapping();
    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    userMappingDAO.insert(umap);

    List<LdapUser> users = manager.getUsers(new String[]{"test_user", "test_user2"}, 100);
    assertThat(users.size(), is(2));

    users = manager.getUsers(new String[]{"foo"}, 100);
    assertThat(users.size(), is(0));
  }

  @Test
  public void testGetUsers_wildcardMatchingNotExpected() throws Exception {
    startLdapServer();
    setSearchBase();

    LdapUserMapping umap = createUserMapping();
    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    userMappingDAO.insert(umap);

    List<LdapUser> users = manager.getUsers(new String[]{"test_user*"}, 100);
    assertThat(users.size(), is(0));
  }

  @Test
  public void testGetGroups_Static() throws Exception {
    startLdapServer();
    setSearchBase();

    LdapUserMapping umap = createUserMapping();
    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    umap.setGroupMappingType(LdapGroupMappingType.STATIC);
    umap.setGroupObjectClass("groupOfNames");
    umap.setGroupMemberAttribute("member");
    umap.setGroupMemberFormat("uid=${username}");
    userMappingDAO.insert(umap);

    List<LdapGroup> groups = manager.getGroups(new String[]{"Gamma", "Theta"}, 100);
    assertThat(groups.size(), is(2));

    // Test max results
    groups = manager.getGroups(new String[]{"Gamma", "Theta"}, 1);
    assertThat(groups.size(), is(1));

    groups = manager.getGroups(new String[]{"foo"}, 100);
    assertThat(groups.size(), is(0));
  }

  @Test
  public void testGetGroups_Static_wildcardMatchingNotExpected() throws Exception {
    startLdapServer();
    setSearchBase();

    LdapUserMapping umap = createUserMapping();
    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    umap.setGroupMappingType(LdapGroupMappingType.STATIC);
    umap.setGroupObjectClass("groupOfNames");
    umap.setGroupMemberAttribute("member");
    umap.setGroupMemberFormat("uid=${username}");
    userMappingDAO.insert(umap);

    List<LdapGroup> groups = manager.getGroups(new String[]{"*ta"}, 100);
    assertThat(groups.size(), is(0));
  }

  @Test
  public void testGetGroups_Dynamic() throws Exception {
    startLdapServer();
    setSearchBase();

    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    LdapUserMapping umap = createUserMapping();
    umap.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    umap.setUserMemberOfGroupAttribute("departmentNumber");
    userMappingDAO.insert(umap);

    List<LdapGroup> groups = manager.getGroups(new String[]{"ab", "abc", "bc"}, 100);
    assertThat(groups.size(), is(3));

    groups = manager.getGroups(new String[] { "ab", "abc", "bc" }, 1);
    assertThat(groups.size(), is(1));

    groups = manager.getGroups(new String[]{"foo"}, 100);
    assertThat(groups.size(), is(0));
  }

  @Test
  public void testGetGroups_Dynamic_wildcardMatchingNotExpected() throws Exception {
    startLdapServer();
    setSearchBase();

    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    LdapUserMapping umap = createUserMapping();
    umap.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    umap.setUserMemberOfGroupAttribute("departmentNumber");
    userMappingDAO.insert(umap);

    List<LdapGroup> groups = manager.getGroups(new String[]{"ab*"}, 100);
    assertThat(groups.size(), is(0));
  }

  @Test
  public void testFindUserByName_Exact() throws Exception {
    startLdapServer();
    setSearchBase();

    LdapUserMapping umap = createUserMapping();

    List<LdapUser> users = manager.testFindUsersByName(umap, "Test User 2", 100);
    assertThat(users, hasSize(1));
    assertThat(users.get(0).getRealName(), is("Test User 2"));
  }

  @Test
  public void testFindUserByName_CaseInsensitive() throws Exception {
    startLdapServer();
    setSearchBase();

    LdapUserMapping umap = createUserMapping();

    List<LdapUser> users = manager.testFindUsersByName(umap, "tEST user 2", 100);
    assertThat(users, hasSize(1));
    assertThat(users.get(0).getRealName(), is("Test User 2"));
  }

  @Test
  public void testFindUserByName_Null() throws Exception {
    startLdapServer();
    setSearchBase();

    LdapUserMapping umap = createUserMapping();

    List<LdapUser> users = manager.testFindUsersByName(umap, null /* name */, 100);
    assertThat(users, hasSize(0));
  }

  @Test
  public void testFindUserByName_Wildcard() throws Exception {
    startLdapServer();
    setSearchBase();

    LdapUserMapping umap = createUserMapping();

    List<LdapUser> users = manager.testFindUsersByName(umap, "*" /* name */, 100);
    assertThat(users, hasSize(3));
    List<String> foundNames = new ArrayList<>();
    for (LdapUser user : users) {
      foundNames.add(user.getUsername());
    }
    assertThat(foundNames, containsInAnyOrder("test_user", "test_user2", "test*user"));
  }

  @Test
  public void testFindUserByName_MaxResults() throws Exception {
    startLdapServer();
    setSearchBase();

    LdapUserMapping umap = createUserMapping();

    List<LdapUser> users = manager.testFindUsersByName(umap, "*" /* name */, 100);
    assertThat(users, hasSize(3));

    users = manager.testFindUsersByName(umap, "*" /* name */, 1);
    assertThat(users, hasSize(1));
  }

  @Test
  public void testFindGroupsByName_Static_WrongObjectClass() throws Exception {
    startLdapServer();
    setSearchBase();

    createStaticGroupMapping();

    // Wrong Objectclass, groupOfUniqueNames not groupOfNames
    List<LdapGroup> groups = manager.findGroupsByName("Alpha", 100);
    assertThat(groups, hasSize(0));
  }

  @Test
  public void testFindGroupsByName_Static_Exact() throws Exception {
    startLdapServer();
    setSearchBase();

    createStaticGroupMapping();

    List<LdapGroup> groups = manager.findGroupsByName("Omega", 100);
    assertThat(groups, hasSize(1));
    assertThat(groups.get(0).getGroupname(), is("Omega"));

    groups = manager.findGroupsByName("meg", 100);
    assertThat(groups, hasSize(0));
  }

  @Test
  public void testFindGroupsByName_Static_CaseInsensitive() throws Exception {
    startLdapServer();
    setSearchBase();

    createStaticGroupMapping();

    List<LdapGroup> groups = manager.findGroupsByName("oMEGA", 100);
    assertThat(groups, hasSize(1));
    assertThat(groups.get(0).getGroupname(), is("Omega"));
  }

  @Test
  public void testFindGroupsByName_Static_MaxResults() throws Exception {
    startLdapServer();
    setSearchBase();

    createStaticGroupMapping();

    List<LdapGroup> groups = manager.findGroupsByName("*a*", 100);
    assertThat(groups, hasSize(5));

    groups = manager.findGroupsByName("*a*", 2);
    assertThat(groups, hasSize(2));
  }

  @Test
  public void testFindGroupsByName_Static_Wildcard() throws Exception {
    startLdapServer();
    setSearchBase();

    createStaticGroupMapping();

    List<LdapGroup> groups = manager.findGroupsByName("*a", 100);
    assertThat(groups, hasSize(5));
    List<String> foundNames = new ArrayList<>();
    for (LdapGroup group : groups) {
      foundNames.add(group.getGroupname());
    }
    assertThat(foundNames, containsInAnyOrder("Gamma", "Omega", "Theta", "Lambda", "Delta"));
  }

  @Test
  public void testFindGroupsByName_Static_NotFound() throws Exception {
    startLdapServer();
    setSearchBase();

    createStaticGroupMapping();

    List<LdapGroup> groups = manager.findGroupsByName("Foo", 100);
    assertThat(groups, hasSize(0));
  }

  @Test
  public void testFindUsersByGroup_Static_OnlyDnExpression() throws Exception {
    startLdapServer();
    setSearchBase();

    LdapUserMapping umap = createUserMapping();
    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    umap.setGroupMappingType(LdapGroupMappingType.STATIC);
    umap.setGroupObjectClass("groupOfNames");
    umap.setGroupMemberAttribute("member");
    umap.setGroupMemberFormat("${dn}");
    userMappingDAO.insert(umap);

    List<LdapUser> users = manager.findUsersByGroup("Epsilon", 100);
    assertThat(users, hasSize(2));
  }

  @Test
  public void testFindUsersByGroup_Static_UsernameExpression() throws Exception {
    startLdapServer();
    setSearchBase();

    LdapUserMapping umap = createUserMapping();
    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    umap.setGroupMappingType(LdapGroupMappingType.STATIC);
    umap.setGroupObjectClass("groupOfNames");
    umap.setGroupMemberAttribute("member");
    umap.setGroupMemberFormat("uid=qwerty${username}zxcvbn,${dn}yuiop${username}");
    userMappingDAO.insert(umap);

    List<LdapUser> users = manager.findUsersByGroup("Delta", 100);
    assertThat(users, hasSize(2));
  }

  @Test
  public void testFindUsersByGroup_Static_DnExpression() throws Exception {
    startLdapServer();
    setSearchBase();

    LdapUserMapping umap = createUserMapping();
    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    umap.setGroupMappingType(LdapGroupMappingType.STATIC);
    umap.setGroupObjectClass("groupOfNames");
    umap.setGroupMemberAttribute("member");
    umap.setGroupMemberFormat("dc=company,${dn},dc=com,${dn}");
    userMappingDAO.insert(umap);

    List<LdapUser> users = manager.findUsersByGroup("Lambda", 100);
    assertThat(users, hasSize(2));
  }

  @Test
  public void testFindUsersByGroup_Static_Dn_MaxResults() throws Exception {
    startLdapServer();
    setSearchBase();

    LdapUserMapping umap = createUserMapping();
    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    umap.setGroupMappingType(LdapGroupMappingType.STATIC);
    umap.setGroupObjectClass("groupOfNames");
    umap.setGroupMemberAttribute("member");
    umap.setGroupMemberFormat("${dn}");
    userMappingDAO.insert(umap);

    List<LdapUser> users = manager.findUsersByGroup("Epsilon", 1);
    assertThat(users, hasSize(1));
    users = manager.findUsersByGroup("Epsilon", 0);
    assertThat(users, hasSize(2));
  }

  @Test
  public void testFindUsersByGroup_Static_Username_MaxResults() throws Exception {
    startLdapServer();
    setSearchBase();

    createStaticGroupMapping();

    List<LdapUser> users = manager.findUsersByGroup("Theta", 1);
    assertThat(users, hasSize(1));
    users = manager.findUsersByGroup("Theta", 0);
    assertThat(users, hasSize(2));
  }


  @Test
  public void testFindUsersByGroup_Static_wildcardMatchingNotExpected() throws Exception {
    startLdapServer();
    setSearchBase();

    LdapUserMapping umap = createUserMapping();
    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    umap.setGroupMappingType(LdapGroupMappingType.STATIC);
    umap.setGroupObjectClass("groupOfNames");
    umap.setGroupMemberAttribute("member");
    umap.setGroupMemberFormat("uid=qwerty${username}zxcvbn,${dn}yuiop${username}");
    userMappingDAO.insert(umap);

    List<LdapUser> users = manager.findUsersByGroup("Delt*", 100);
    assertThat(users, hasSize(0));
  }

  @Test
  public void testFindUsersByGroup_Dynamic_wildcardMatchingNotExpected() throws Exception {
    startLdapServer();
    setSearchBase();

    LdapUserMapping umap = createUserMapping();
    umap.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    umap.setUserMemberOfGroupAttribute("departmentNumber");
    new LdapUserMappingDAO().insert(umap);

    List<LdapUser> users = manager.findUsersByGroup("a*", 100);
    assertThat(users, hasSize(0));
  }

  private void createStaticGroupMapping() {
    LdapUserMapping umap = createUserMapping();
    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    umap.setGroupMappingType(LdapGroupMappingType.STATIC);
    umap.setGroupObjectClass("groupOfNames");
    umap.setGroupMemberAttribute("member");
    umap.setGroupMemberFormat("uid=${username}");
    userMappingDAO.insert(umap);
  }

  private void createDynamicGroupMapping() {
    LdapUserMapping umap = createUserMapping();
    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    umap.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    umap.setUserMemberOfGroupAttribute("departmentNumber");
    userMappingDAO.insert(umap);
  }

  @Test
  public void testFindGroupsByName_Dynamic_MaxResults() throws Exception {
    startLdapServer();
    setSearchBase();

    createDynamicGroupMapping();

    List<LdapGroup> groups = manager.findGroupsByName("*b*", 100);
    assertThat(groups, hasSize(5));

    groups = manager.findGroupsByName("*b*", 2);
    assertThat(groups, hasSize(2));
  }

  @Test
  public void testFindGroupsByName_Dynamic_WildcardPrefix() throws Exception {
    startLdapServer();
    setSearchBase();

    createDynamicGroupMapping();

    List<LdapGroup> groups = manager.findGroupsByName("*b", 100);
    assertThat(groups, hasSize(2));
    List<String> foundNames = new ArrayList<>();
    for (LdapGroup group : groups) {
      foundNames.add(group.getGroupname());
    }
    assertThat(foundNames, containsInAnyOrder("ab", "xb"));
  }

  @Test
  public void testFindGroupsByName_Dynamic_WildcardSuffix() throws Exception {
    startLdapServer();
    setSearchBase();

    createDynamicGroupMapping();

    List<LdapGroup> groups = manager.findGroupsByName("b*", 100);
    assertThat(groups, hasSize(2));
    List<String> foundNames = new ArrayList<>();
    for (LdapGroup group : groups) {
      foundNames.add(group.getGroupname());
    }
    assertThat(foundNames, containsInAnyOrder("bc", "bx"));
  }

  @Test
  public void testFindGroupsByName_Dynamic_WildcardPrefixAndSuffix() throws Exception {
    startLdapServer();
    setSearchBase();

    createDynamicGroupMapping();

    List<LdapGroup> groups = manager.findGroupsByName("*b*", 100);
    assertThat(groups.toString(), groups, hasSize(5));
    List<String> foundNames = new ArrayList<>();
    for (LdapGroup group : groups) {
      foundNames.add(group.getGroupname());
    }
    assertThat(foundNames, containsInAnyOrder("ab", "abc", "bc", "xb", "bx"));
  }

  @Test
  public void testFindGroupsByName_Dynamic_Exact() throws Exception {
    startLdapServer();
    setSearchBase();

    createDynamicGroupMapping();

    List<LdapGroup> groups = manager.findGroupsByName("ab", 100);
    assertThat(groups, hasSize(1));
    assertThat(groups.get(0).getGroupname(), is("ab"));

    groups = manager.findGroupsByName("b", 100);
    assertThat(groups, hasSize(0));
  }

  @Test
  public void testFindGroupsByName_Dynamic_CaseInsensitive() throws Exception {
    startLdapServer();
    setSearchBase();

    createDynamicGroupMapping();

    List<LdapGroup> groups = manager.findGroupsByName("ABC", 100);
    assertThat(groups, hasSize(1));
    assertThat(groups.get(0).getGroupname(), is("abc"));
  }

  @Test
  public void testFindGroupsByName_Dynamic_NotFound() throws Exception {
    startLdapServer();
    setSearchBase();

    createDynamicGroupMapping();

    List<LdapGroup> groups = manager.findGroupsByName("Foo", 100);
    assertThat(groups, hasSize(0));
  }

  @Test
  public void testIsLdapEnabled() throws Exception {
    assertThat(manager.isLdapEnabled(), is(false));

    serverDetails = new LdapServer();
    serverDetails.setName("Test Server");
    serverDao.insert(serverDetails);
    assertThat(manager.isLdapEnabled(), is(false));

    LdapConnection conn = createLdapConnection();
    conn.setHostname("localhost");
    new LdapConnectionDAO().insert(conn);
    assertThat(manager.isLdapEnabled(), is(false));

    LdapUserMapping umap = createUserMapping();
    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    userMappingDAO.insert(umap);

    assertThat(manager.isLdapEnabled(), is(true));
  }

  @Test
  public void testIsLdapGroupEnabled() throws Exception {
    serverDetails = new LdapServer();
    serverDetails.setName("Test Server");
    serverDao.insert(serverDetails);
    assertThat(manager.isLdapGroupEnabled(), is(false));

    LdapConnection conn = createLdapConnection();
    conn.setHostname("localhost");
    conn.setSearchBase("dc=company,dc=com");
    new LdapConnectionDAO().insert(conn);

    assertThat(manager.isLdapGroupEnabled(), is(false));

    LdapUserMapping umap = createUserMapping();
    umap.setGroupMappingType(LdapGroupMappingType.NONE);

    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    userMappingDAO.insert(umap);

    assertThat(manager.isLdapGroupEnabled(), is(false));

    umap.setGroupMappingType(LdapGroupMappingType.STATIC);
    userMappingDAO.update(umap);

    assertThat(manager.isLdapGroupEnabled(), is(true));
  }

  @Test
  public void testIsGroupSearchEnabled() throws Exception {
    serverDetails = new LdapServer();
    serverDetails.setName("Test Server");
    serverDao.insert(serverDetails);
    assertThat(manager.isGroupSearchEnabled(), is(false));

    LdapConnection conn = createLdapConnection();
    conn.setHostname("localhost");
    conn.setSearchBase("dc=company,dc=com");
    new LdapConnectionDAO().insert(conn);

    assertThat(manager.isGroupSearchEnabled(), is(false));

    LdapUserMapping umap = createUserMapping();
    umap.setGroupMappingType(LdapGroupMappingType.NONE);

    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    userMappingDAO.insert(umap);

    assertThat(manager.isGroupSearchEnabled(), is(false));

    umap.setGroupMappingType(LdapGroupMappingType.STATIC);
    userMappingDAO.update(umap);

    assertThat(manager.isGroupSearchEnabled(), is(true));

    umap.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    umap.setDynamicGroupSearchEnabled(true);
    userMappingDAO.update(umap);

    assertThat(manager.isGroupSearchEnabled(), is(true));

    umap.setDynamicGroupSearchEnabled(false);
    userMappingDAO.update(umap);

    assertThat(manager.isGroupSearchEnabled(), is(false));
  }

  @Test
  public void testGetLdapServerName() throws Exception {
    try {
      manager.getLdapServerName();
      fail("Expected IllegalStateException");
    } catch(IllegalStateException expected) {
      assertThat(expected.getMessage(), is("LDAP server is not configured"));
    }

    startLdapServer();
    String name = manager.getLdapServerName();
    assertThat(name, is("Test Server"));
  }

  public LdapManagerTest startLdapServer() throws Exception {
    serverDetails = new LdapServer();
    serverDetails.setName("Test Server");
    serverDao.insert(serverDetails);

    ldapServer.start();
    ldapServer.loadData("/ldap_users.ldif");

    return this;
  }

  @After
  public void cleanup() throws Exception {
    for (LdapServer s : serverDao.getAll()) {
      serverDao.delete(s);
    }
    assertThat(serverDao.getAll(), is(empty()));
  }

  protected LdapConnection createLdapConnection() {
    LdapConnection conn = manager.loadConnection(serverDetails.getId());
    conn.setServerId(serverDetails.getId());
    conn.setProtocol(LdapProtocol.LDAP);
    if (ldapServer != null) {
      conn.setHostname(ldapServer.getHostname());
      conn.setPort(ldapServer.getPort());
    }
    return conn;
  }

  protected LdapUserMapping createUserMapping() {
    LdapUserMapping umap = new LdapUserMapping();
    umap.setServerId(serverDetails.getId());
    umap.setUserBaseDN("ou=users");
    umap.setUserObjectClass("person");
    umap.setUserIDAttribute("uid");
    umap.setUserRealNameAttribute("cn");
    umap.setUserEmailAttribute("mail");
    umap.setUserSubtree(true);
    umap.setGroupBaseDN("ou=groups");
    umap.setGroupIDAttribute("cn");
    umap.setGroupSubtree(true);
    return umap;
  }

  @Test
  public void testFindUsersByGroup_Dynamic() throws Exception {
    startLdapServer();
    setSearchBase();

    createDynamicGroupMapping();

    // Group with one user
    List<LdapUser> users = manager.findUsersByGroup("xb", 0 /* maxResults */);
    assertThat(users, hasSize(1));
    LdapUser user = users.get(0);
    assertThat(user.getUsername(), is("test_user"));
    assertThat(user.getRealName(), is("Test User"));
    assertThat(user.getEmail(), is("test.user@company.com"));

    // Group with two users
    users = manager.findUsersByGroup("ab", 0 /* maxResults */);
    Set<String> usernames = new HashSet<>();
    for (LdapUser user1 : users) {
      usernames.add(user1.getUsername());
    }
    assertThat(usernames, containsInAnyOrder("test_user", "test_user2", "test*user"));

    // Group without users
    users = manager.findUsersByGroup("no such group", 0 /* maxResults */);
    assertThat(users, hasSize(0));
  }

  @Test
  public void testFindUsersByGroup_Dynamic_MaxResults() throws Exception {
    startLdapServer();
    setSearchBase();

    createDynamicGroupMapping();

    // Group with two users
    List<LdapUser> users = manager.findUsersByGroup("ab", 0 /* maxResults */);
    assertThat(users, hasSize(3));
    users = manager.findUsersByGroup("ab", 1 /* maxResults */);
    assertThat(users, hasSize(1));
  }
}
