/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ldap.test;

import java.net.ServerSocket;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.naming.AuthenticationException;
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

  @Test
  public void testUserMapping() throws Exception {
    startLdapServer();

    LdapConnection conn = createLdapConnection();
    conn.setSearchBase("dc=company,dc=com");
    manager.saveConnection(conn);

    LdapUserMapping umap = createUserMapping();

    List<LdapUser> users = manager.testUserMapping(umap, -1);
    assertThat(users, hasSize(2));

    Collections.sort(users); // sorts on username

    LdapUser user = users.get(0);
    assertThat(user.getUsername(), is("test_user"));
    assertThat(user.getDn(), is("uid=test_user,ou=users,dc=company,dc=com"));
    assertThat(user.getRealName(), is("Test User"));
    assertThat(user.getEmail(), is("test.user@company.com"));
    assertThat(user.getPassword(), nullValue()); // make sure password is not passed back
    assertThat(user.getMembership(), nullValue());

    user = users.get(1);
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

    LdapConnection conn = createLdapConnection();
    conn.setSearchBase("dc=company,dc=com");
    manager.saveConnection(conn);

    LdapUserMapping umap = createUserMapping();
    umap.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    umap.setUserMemberOfGroupAttribute("departmentNumber");

    List<LdapUser> users = manager.testUserMapping(umap, -1);
    assertThat(users, hasSize(2));

    Collections.sort(users); // sorts on username

    assertThat(users.get(0).getMembership(), containsInAnyOrder("testUsers", "primaryUsers"));
    assertThat(users.get(1).getMembership(), containsInAnyOrder("testUsers", "secondaryUsers"));
  }

  @Test
  public void testStaticGroupMapping() throws Exception {
    startLdapServer();

    LdapConnection conn = createLdapConnection();
    conn.setSearchBase("dc=company,dc=com");
    manager.saveConnection(conn);

    LdapUserMapping umap = createUserMapping();
    umap.setGroupMappingType(LdapGroupMappingType.STATIC);
    umap.setGroupObjectClass("groupOfUniqueNames");
    umap.setGroupMemberAttribute("uniqueMember");
    umap.setGroupMemberFormat("${dn}");

    List<LdapUser> users = manager.testUserMapping(umap, -1);
    assertThat(users, hasSize(2));

    Collections.sort(users); // sorts on username

    assertThat(users.get(0).getMembership(), containsInAnyOrder("Alpha"));
    assertThat(users.get(1).getMembership(), containsInAnyOrder("Beta"));

    umap.setGroupMappingType(LdapGroupMappingType.STATIC);
    umap.setGroupObjectClass("groupOfNames");
    umap.setGroupMemberAttribute("member");
    umap.setGroupMemberFormat("uid=${username}");

    users = manager.testUserMapping(umap, -1);
    assertThat(users, hasSize(2));

    Collections.sort(users); // sorts on username

    assertThat(users.get(0).getMembership(), containsInAnyOrder("Gamma", "Theta", "Omega"));
    assertThat(users.get(1).getMembership(), containsInAnyOrder("Theta"));
  }

  @Test
  public void testUserLogin() throws Exception {
    startLdapServer();

    LdapConnection conn = createLdapConnection();
    conn.setSearchBase("dc=company,dc=com");
    manager.saveConnection(conn);

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

    LdapConnection conn = createLdapConnection();
    conn.setSearchBase("dc=company,dc=com");
    manager.saveConnection(conn);
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
  public void testGetUsers() throws Exception {
    startLdapServer();

    LdapConnection conn = createLdapConnection();
    conn.setSearchBase("dc=company,dc=com");
    manager.saveConnection(conn);

    LdapUserMapping umap = createUserMapping();
    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    userMappingDAO.insert(umap);

    List<LdapUser> users = manager.getUsers(new String[]{"test_user", "test_user2"}, 100);
    assertThat(users.size(), is(2));

    users = manager.getUsers(new String[]{"foo"}, 100);
    assertThat(users.size(), is(0));
  }

  @Test
  public void testGetStaticGroups() throws Exception {
    startLdapServer();

    LdapConnection conn = createLdapConnection();
    conn.setSearchBase("dc=company,dc=com");
    manager.saveConnection(conn);

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
  public void testGetDynamicGroups() throws Exception {
    startLdapServer();

    LdapConnection conn = createLdapConnection();
    conn.setSearchBase("dc=company,dc=com");
    manager.saveConnection(conn);

    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    LdapUserMapping umap = createUserMapping();
    umap.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    umap.setUserMemberOfGroupAttribute("departmentNumber");
    userMappingDAO.insert(umap);

    List<LdapGroup> groups = manager.getGroups(new String[]{"testUsers", "primaryUsers", "secondaryUsers"}, 100);
    assertThat(groups.size(), is(3));

    groups = manager.getGroups(new String[]{"testUsers", "primaryUsers", "secondaryUsers"}, 1);
    assertThat(groups.size(), is(1));

    groups = manager.getGroups(new String[]{"foo"}, 100);
    assertThat(groups.size(), is(0));
  }

  @Test
  public void testFindUserByName() throws Exception {
    startLdapServer();

    LdapConnection conn = createLdapConnection();
    conn.setSearchBase("dc=company,dc=com");
    manager.saveConnection(conn);

    LdapUserMapping umap = createUserMapping();

    //note this also checks case insensitive check, as the name is 'Test User 2'
    List<LdapUser> users = manager.testFindUsersByName(umap, "user 2", 100);
    assertThat(users.size(), is(1));

  }

  @Test
  public void testFindStaticGroupsByName() throws Exception {
    startLdapServer();

    LdapConnection conn = createLdapConnection();
    conn.setSearchBase("dc=company,dc=com");
    manager.saveConnection(conn);

    LdapUserMapping umap = createUserMapping();
    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    umap.setGroupMappingType(LdapGroupMappingType.STATIC);
    umap.setGroupObjectClass("groupOfNames");
    umap.setGroupMemberAttribute("member");
    umap.setGroupMemberFormat("uid=${username}");
    userMappingDAO.insert(umap);

    // Wrong Objectclass, groupOfUniqueNames not groupOfNames
    List<LdapGroup> groups = manager.findGroupsByName("Alpha", 100);
    assertThat(groups.size(), is(0));

    groups = manager.findGroupsByName("a", 100);
    assertThat(groups.size(), is(3));

    // Test max results
    groups = manager.findGroupsByName("a", 2);
    assertThat(groups.size(), is(2));

    groups = manager.findGroupsByName("Foo", 100);
    assertThat(groups.size(), is(0));
  }

  @Test
  public void testFindDynamicGroupsByName() throws Exception {
    startLdapServer();

    LdapConnection conn = createLdapConnection();
    conn.setSearchBase("dc=company,dc=com");
    manager.saveConnection(conn);

    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    LdapUserMapping umap = createUserMapping();
    umap.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    umap.setUserMemberOfGroupAttribute("departmentNumber");
    userMappingDAO.insert(umap);

    List<LdapGroup> groups = manager.findGroupsByName("Users", 100);
    assertThat(groups.size(), is(3));

    groups = manager.findGroupsByName("Users", 1);
    assertThat(groups.size(), is(1));

    groups = manager.findGroupsByName("Foo", 100);
    assertThat(groups.size(), is(0));
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
}
