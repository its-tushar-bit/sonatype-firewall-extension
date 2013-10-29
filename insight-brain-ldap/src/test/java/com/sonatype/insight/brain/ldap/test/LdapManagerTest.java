/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ldap.test;

import static com.sonatype.insight.brain.ldap.EmbeddedLdapServer.newEmbeddedLdapServer;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;

import java.net.ServerSocket;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.naming.NamingException;

import org.junit.After;
import org.junit.Test;
import org.sonatype.guice.bean.containers.InjectedTest;

import com.sonatype.insight.brain.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.configuration.ldap.LdapProtocol;
import com.sonatype.insight.brain.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.ldap.EmbeddedLdapServer;
import com.sonatype.insight.brain.ldap.LdapManager;
import com.sonatype.insight.brain.ldap.LdapUser;

/**
 * @since 1.7
 */
public class LdapManagerTest
    extends InjectedTest
{
  private final LdapServerDAO serverDao = new LdapServerDAO();

  private EmbeddedLdapServer ldapServer;

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

  public LdapManagerTest startLdapServer() throws Exception {
    serverDetails = new LdapServer();
    serverDetails.setName("Test Server");
    serverDao.insert(serverDetails);

    ldapServer = newEmbeddedLdapServer();
    ldapServer.start();
    ldapServer.loadData("/ldap_users.ldif");

    return this;
  }

  @After
  public void stopLdapServer() throws Exception {
    if (ldapServer != null) {
      ldapServer.stop();
      ldapServer = null;
    }

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
