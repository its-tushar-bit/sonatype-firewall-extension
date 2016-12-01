/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ldap.test;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.naming.AuthenticationException;
import javax.naming.CommunicationException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import com.sonatype.insight.brain.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapConnectionDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.ldap.LdapGroup;
import com.sonatype.insight.brain.ldap.LdapManager;
import com.sonatype.insight.brain.ldap.LdapUser;
import com.sonatype.insight.brain.ldap.TestLdapServer;

import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.fail;

/**
 * @since 1.7
 */
public class LdapManagerTest
    extends InjectedTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Rule
  public TestLdapServer testLdapServer1;

  @Rule
  public TestLdapServer testLdapServer2;

  @Before
  public void before() {
    testLdapServer1 = new TestLdapServer(new File(tempDir.getRoot(), "server1"), "/ldap_users1.ldif");
    testLdapServer2 = new TestLdapServer(new File(tempDir.getRoot(), "server2"), "/ldap_users2.ldif");
  }

  @Inject
  private LdapManager manager;

  @Test
  public void testTestConnection() throws Exception {
    LdapServer ldapServer1 = tempEntity.newLdapServer("Test Server1");
    LdapConnection ldapConnection1 = createLdapConnection(ldapServer1);
    setSearchBase(ldapConnection1, null);
    startLdapServer(testLdapServer1, ldapConnection1);
    LdapServer ldapServer2 = tempEntity.newLdapServer("Test Server2");
    LdapConnection ldapConnection2 = createLdapConnection(ldapServer2);
    setSearchBase(ldapConnection2, null);
    startLdapServer(testLdapServer2, ldapConnection2);
    assertCanConnect(ldapConnection1);
    assertCanConnect(ldapConnection2);

    testLdapServer1.stop();
    assertCannotConnect(ldapConnection1);
    assertCanConnect(ldapConnection2);

    testLdapServer2.stop();
    assertCannotConnect(ldapConnection1);
    assertCannotConnect(ldapConnection2);
  }

  private void assertCanConnect(LdapConnection ldapConnection) throws NamingException {
    manager.testConnection(ldapConnection);
  }

  private void assertCannotConnect(LdapConnection ldapConnection) throws NamingException {
    try {
      manager.testConnection(ldapConnection);
      fail("Expected exception");
    }
    catch (CommunicationException expected) {
      assertThat(expected.getCause(), is(notNullValue()));
      assertThat(expected.getCause().getMessage(), startsWith("Connection refused"));
    }
  }

  @Test
  public void testTestConnection_EscapedUrl() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    // Search base with space will be escaped with %20.
    ldapConnection.setSearchBase("dc=acme brick,dc=com");

    manager.testConnection(ldapConnection);
  }

  @Test
  public void testTestConnection_Timeout() throws Exception {
    ServerSocket socket = new ServerSocket(0);
    try {
      long begin = 0, end = 0;

      LdapConnection ldapConnection = new LdapConnection();
      ldapConnection.setHostname("localhost");
      ldapConnection.setPort(socket.getLocalPort());

      // test very short timeout

      try {
        ldapConnection.setConnectionTimeout(1);
        begin = System.currentTimeMillis();
        manager.testConnection(ldapConnection);

        fail("Expected NamingException");
      }
      catch (NamingException expected) {
        end = System.currentTimeMillis();
      }

      assertThat(Double.valueOf(end - begin), is(closeTo(1300, 500)));

      // test slightly longer timeout

      try {
        ldapConnection.setConnectionTimeout(5);
        begin = System.currentTimeMillis();
        manager.testConnection(ldapConnection);

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
  public void testAuthenticateUser_RetryDelay() throws Exception {
    ServerSocket socket = new ServerSocket(0);
    try {
      LdapServer ldapServer = tempEntity.newLdapServer("Test Server");

      LdapConnection ldapConnection = createLdapConnection(ldapServer);
      ldapConnection.setHostname("localhost");
      ldapConnection.setPort(socket.getLocalPort());
      ldapConnection.setConnectionTimeout(1);
      ldapConnection.setRetryDelay(5);
      manager.saveConnection(ldapConnection);

      createUserMapping(ldapServer);

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
  public void testAuthenticateUser_SingleExceptionThrownAsIs() throws Exception {
    loadLdapServer(testLdapServer1, "Test Server1");

    try {
      manager.authenticateUser("test_user2_2", "test".toCharArray());
      fail("wrong password for valid user in 'Test Server2' should fail");
    }
    catch (NameNotFoundException e) {
      assertThat(e.getMessage(), is("LDAP user with username 'test_user2_2' does not exist"));
    }
  }

  @Test
  public void testAuthenticateUser_MultiServer_BadPassword() throws Exception {
    loadLdapServer(testLdapServer1, "Test Server1");
    loadLdapServer(testLdapServer2, "Test Server2");

    try {
      manager.authenticateUser("test_user2_2", "badPWD".toCharArray());
      fail("wrong password for valid user in 'Test Server2' should fail");
    }
    catch (AuthenticationException e) {
      assertThat(e.getMessage(), is(
          "LDAP Server: Test Server2 -> [LDAP: error code 49 - INVALID_CREDENTIALS: Bind failed: ERR_229 Cannot authenticate user uid=test_user2_2,ou=users,dc=company,dc=com]"));

      assertThat(e.getSuppressed()[0].getMessage(), is("LDAP user with username 'test_user2_2' does not exist"));
      assertThat(e.getSuppressed()[1].getMessage(), is(
          "[LDAP: error code 49 - INVALID_CREDENTIALS: Bind failed: ERR_229 Cannot authenticate user uid=test_user2_2,ou=users,dc=company,dc=com]"));
      assertThat(e.getSuppressed().length, is(2));
    }
  }

  private void loadLdapServer(final TestLdapServer testLdapServer, final String serverName) throws Exception {
    final LdapServer ldapServer = tempEntity.newLdapServer(serverName);
    final LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer, ldapConnection);

    createUserMapping(ldapServer);
  }

  @Test
  public void testAuthenticateUser_MultiServer_Timeout_Single() throws Exception {
    final LdapConnection ldapConnection1 = createShortTimeoutLdapConnectionWithoutEmbeddedServer("Test Server1");
    loadLdapServer(testLdapServer2, "Test Server2");

    try (final ServerSocket ignored = new ServerSocket(ldapConnection1.getPort())) {
      try {
        manager.authenticateUser("any-user", "anything".toCharArray());
        fail("magic string 'timeout' in any error message should fail");
      }
      catch (NamingException e) {
        assertThat(e.getMessage(),
            is("LDAP Server: Test Server1 -> LDAP response read timed out, timeout used:1000ms.;\n"));

        assertThat(e.getSuppressed()[0].getMessage(), is("LDAP response read timed out, timeout used:1000ms."));
        assertThat(e.getSuppressed()[1].getMessage(), is("LDAP user with username 'any-user' does not exist"));
        assertThat(e.getSuppressed().length, is(2));
      }
    }
  }

  private LdapConnection createShortTimeoutLdapConnectionWithoutEmbeddedServer(final String ldapServerName) {
    final LdapServer ldapServer1 = tempEntity.newLdapServer(ldapServerName);
    final LdapConnection ldapConnection1 = createLdapConnection(ldapServer1);
    ldapConnection1.setConnectionTimeout(1);
    new LdapConnectionDAO().update(ldapConnection1);
    createUserMapping(ldapServer1);
    return ldapConnection1;
  }

  @Test
  public void testAuthenticateUser_MultiServer_Timeout_MultipleAggregated() throws Exception {
    final LdapConnection ldapConnection1 = createShortTimeoutLdapConnectionWithoutEmbeddedServer("Test Server1");
    final LdapConnection ldapConnection2 = createShortTimeoutLdapConnectionWithoutEmbeddedServer("Test Server2");

    final TestLdapServer testLdapServer3 = new TestLdapServer(new File(tempDir.getRoot(), "server3"),
        "/ldap_users2.ldif");
    loadLdapServer(testLdapServer3, "Test Server3");
    try {
      try (final ServerSocket ignored = new ServerSocket(ldapConnection1.getPort())) {
        try (final ServerSocket ignored2 = new ServerSocket(ldapConnection2.getPort())) {

          try {
            manager.authenticateUser("any-user", "anything".toCharArray());
            fail("magic string 'timeout' in any error message should fail");
          }
          catch (NamingException e) {
            assertThat(e.getMessage(),
                is("LDAP Server: Test Server1 -> LDAP response read timed out, timeout used:1000ms.;\n"
                    + "LDAP Server: Test Server2 -> LDAP response read timed out, timeout used:1000ms.;\n"));

            assertThat(e.getSuppressed()[0].getMessage(), is("LDAP response read timed out, timeout used:1000ms."));
            assertThat(e.getSuppressed()[1].getMessage(), is("LDAP response read timed out, timeout used:1000ms."));
            assertThat(e.getSuppressed()[2].getMessage(), is("LDAP user with username 'any-user' does not exist"));
            assertThat(e.getSuppressed().length, is(3));
          }
        }
      }
    }
    finally {
      testLdapServer3.stop();
    }
  }

  @Test
  public void testAuthenticateUser_MultiServer_UnknownUser() throws Exception {
    loadLdapServer(testLdapServer1, "Test Server1");
    loadLdapServer(testLdapServer2, "Test Server2");

    final int ldapServer1Port = testLdapServer1.getPort();
    testLdapServer1.stop();
    try {
      manager.authenticateUser("test_user4", "anything".toCharArray());
      fail("Unknown user in any server should fail");
    }
    catch (NamingException e) {
      assertThat(e.getMessage(), is("LDAP Server: Test Server1 -> localhost:" + ldapServer1Port + ";\n"
          + "LDAP Server: Test Server2 -> LDAP user with username 'test_user4' does not exist;\n"));

      // Use startsWith because the error message depends on the OS.
      assertThat(e.getSuppressed()[0].getCause().getMessage(), startsWith("Connection refused"));
      assertThat(e.getSuppressed()[1].getMessage(), is("LDAP user with username 'test_user4' does not exist"));
      assertThat(e.getSuppressed().length, is(2));
    }
  }

  @Test
  public void testAuthenticateUser_MultiServer_UnexpectedError() throws Exception {
    loadLdapServer(testLdapServer1, "Test Server1");
    loadLdapServer(testLdapServer2, "Test Server2");

    try {
      manager.authenticateUser("test_user4", "anything".toCharArray());
      fail("Unknown user in any server should fail");
    }
    catch (NameNotFoundException e) {
      assertThat(e.getMessage(),
          is("LDAP Server: Test Server1 -> LDAP user with username 'test_user4' does not exist;\n"
              + "LDAP Server: Test Server2 -> LDAP user with username 'test_user4' does not exist;\n"));

      assertThat(e.getSuppressed()[0].getMessage(), is("LDAP user with username 'test_user4' does not exist"));
      assertThat(e.getSuppressed()[1].getMessage(), is("LDAP user with username 'test_user4' does not exist"));
      assertThat(e.getSuppressed().length, is(2));
    }
  }

  @Test
  public void testAuthenticateUser_MultiServer_ValidLoginFirstServer() throws Exception {
    loadLdapServer(testLdapServer1, "Test Server1");
    loadLdapServer(testLdapServer2, "Test Server2");

    final LdapUser ldapUser = manager.authenticateUser("test_user2_1", "test".toCharArray());
    assertThat(ldapUser.getRealName(), is("Test User 2 1"));
  }

  @Test
  public void testAuthenticateUser_MultiServer_ValidLoginSecondServer() throws Exception {
    loadLdapServer(testLdapServer1, "Test Server1");
    loadLdapServer(testLdapServer2, "Test Server2");

    final LdapUser ldapUser = manager.authenticateUser("test_user2_2", "test".toCharArray());
    assertThat(ldapUser.getRealName(), is("Test User 2 2"));
  }

  @Test
  public void testAuthenticateUserForReverseProxy_RetryDelay() throws Exception {
    ServerSocket socket = new ServerSocket(0);
    try {
      LdapServer ldapServer = tempEntity.newLdapServer("Test Server");

      LdapConnection ldapConnection = createLdapConnection(ldapServer);
      ldapConnection.setHostname("localhost");
      ldapConnection.setPort(socket.getLocalPort());
      ldapConnection.setConnectionTimeout(1);
      ldapConnection.setRetryDelay(5);
      manager.saveConnection(ldapConnection);

      createUserMapping(ldapServer);

      // force three failures by attempting auth against the dangling socket

      for (int failures = 0; failures < 3; failures++) {
        try {
          manager.authenticateUserForReverseProxy("user");
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
          manager.authenticateUserForReverseProxy("user");
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
        manager.authenticateUserForReverseProxy("user");
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
  public void testAuthenticateUserForReverseProxy_SingleExceptionThrownAsIs() throws Exception {
    loadLdapServer(testLdapServer1, "Test Server1");

    try {
      manager.authenticateUserForReverseProxy("test_user2_2");
      fail("wrong password for valid user in 'Test Server2' should fail");
    }
    catch (NameNotFoundException e) {
      assertThat(e.getMessage(), is("LDAP user with username 'test_user2_2' does not exist"));
    }
  }

  @Test
  public void testAuthenticateUserForReverseProxy_MultiServer_Timeout_Single() throws Exception {
    final LdapConnection ldapConnection1 = createShortTimeoutLdapConnectionWithoutEmbeddedServer("Test Server1");
    loadLdapServer(testLdapServer2, "Test Server2");

    try (final ServerSocket ignored = new ServerSocket(ldapConnection1.getPort())) {
      try {
        manager.authenticateUserForReverseProxy("any-user");
        fail("magic string 'timeout' in any error message should fail");
      }
      catch (NamingException e) {
        assertThat(e.getMessage(),
            is("LDAP Server: Test Server1 -> LDAP response read timed out, timeout used:1000ms.;\n"));

        assertThat(e.getSuppressed()[0].getMessage(), is("LDAP response read timed out, timeout used:1000ms."));
        assertThat(e.getSuppressed()[1].getMessage(), is("LDAP user with username 'any-user' does not exist"));
        assertThat(e.getSuppressed().length, is(2));
      }
    }
  }

  @Test
  public void testAuthenticateUserForReverseProxy_MultiServer_Timeout_MultipleAggregated() throws Exception {
    final LdapConnection ldapConnection1 = createShortTimeoutLdapConnectionWithoutEmbeddedServer("Test Server1");
    final LdapConnection ldapConnection2 = createShortTimeoutLdapConnectionWithoutEmbeddedServer("Test Server2");

    final TestLdapServer testLdapServer3 = new TestLdapServer(new File(tempDir.getRoot(), "server3"),
        "/ldap_users2.ldif");
    loadLdapServer(testLdapServer3, "Test Server3");
    try {
      try (final ServerSocket ignored = new ServerSocket(ldapConnection1.getPort())) {
        try (final ServerSocket ignored2 = new ServerSocket(ldapConnection2.getPort())) {

          try {
            manager.authenticateUserForReverseProxy("any-user");
            fail("magic string 'timeout' in any error message should fail");
          }
          catch (NamingException e) {
            assertThat(e.getMessage(),
                is("LDAP Server: Test Server1 -> LDAP response read timed out, timeout used:1000ms.;\n"
                    + "LDAP Server: Test Server2 -> LDAP response read timed out, timeout used:1000ms.;\n"));

            assertThat(e.getSuppressed()[0].getMessage(), is("LDAP response read timed out, timeout used:1000ms."));
            assertThat(e.getSuppressed()[1].getMessage(), is("LDAP response read timed out, timeout used:1000ms."));
            assertThat(e.getSuppressed()[2].getMessage(), is("LDAP user with username 'any-user' does not exist"));
            assertThat(e.getSuppressed().length, is(3));
          }
        }
      }
    }
    finally {
      testLdapServer3.stop();
    }
  }

  @Test
  public void testAuthenticateUserForReverseProxy_MultiServer_UnknownUser() throws Exception {
    loadLdapServer(testLdapServer1, "Test Server1");
    loadLdapServer(testLdapServer2, "Test Server2");

    final int ldapServer1Port = testLdapServer1.getPort();
    testLdapServer1.stop();
    try {
      manager.authenticateUserForReverseProxy("test_user4");
      fail("Unknown user in any server should fail");
    }
    catch (NamingException e) {
      assertThat(e.getMessage(), is("LDAP Server: Test Server1 -> localhost:" + ldapServer1Port + ";\n"
          + "LDAP Server: Test Server2 -> LDAP user with username 'test_user4' does not exist;\n"));

      // Use startsWith because the error message depends on the OS.
      assertThat(e.getSuppressed()[0].getCause().getMessage(), startsWith("Connection refused"));
      assertThat(e.getSuppressed()[1].getMessage(), is("LDAP user with username 'test_user4' does not exist"));
      assertThat(e.getSuppressed().length, is(2));
    }
  }

  @Test
  public void testAuthenticateUserForReverseProxy_MultiServer_UnexpectedError() throws Exception {
    loadLdapServer(testLdapServer1, "Test Server1");
    loadLdapServer(testLdapServer2, "Test Server2");

    try {
      manager.authenticateUserForReverseProxy("test_user4");
      fail("Unknown user in any server should fail");
    }
    catch (NameNotFoundException e) {
      assertThat(e.getMessage(),
          is("LDAP Server: Test Server1 -> LDAP user with username 'test_user4' does not exist;\n"
              + "LDAP Server: Test Server2 -> LDAP user with username 'test_user4' does not exist;\n"));

      assertThat(e.getSuppressed()[0].getMessage(), is("LDAP user with username 'test_user4' does not exist"));
      assertThat(e.getSuppressed()[1].getMessage(), is("LDAP user with username 'test_user4' does not exist"));
      assertThat(e.getSuppressed().length, is(2));
    }
  }

  @Test
  public void testAuthenticateUserForReverseProxy_MultiServer_ValidLoginFirstServer() throws Exception {
    loadLdapServer(testLdapServer1, "Test Server1");
    loadLdapServer(testLdapServer2, "Test Server2");

    final LdapUser ldapUser = manager.authenticateUserForReverseProxy("test_user2_1");
    assertThat(ldapUser.getRealName(), is("Test User 2 1"));
  }

  @Test
  public void testAuthenticateUserForReverseProxy_MultiServer_ValidLoginSecondServer() throws Exception {
    loadLdapServer(testLdapServer1, "Test Server1");
    loadLdapServer(testLdapServer2, "Test Server2");

    final LdapUser ldapUser = manager.authenticateUserForReverseProxy("test_user2_2");
    assertThat(ldapUser.getRealName(), is("Test User 2 2"));
  }

  @Test
  public void testTestConnection_BadSearchBase() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    ldapConnection.setSearchBase("!@£$%^&*()");

    try {
      manager.testConnection(ldapConnection);
      fail("Expected NamingException");
    }
    catch (NamingException expected) {
    }
  }

  private void setSearchBase(LdapConnection ldapConnection, String searchBase) {
    ldapConnection.setSearchBase(searchBase);
    manager.saveConnection(ldapConnection);
  }

  @Test
  public void testTestUserMapping() throws Exception {
    LdapServer ldapServer1 = tempEntity.newLdapServer("Test Server1");
    LdapConnection ldapConnection1 = createLdapConnection(ldapServer1);
    startLdapServer(testLdapServer1, ldapConnection1);
    LdapUserMapping umap1 = createUserMapping(ldapServer1);

    LdapServer ldapServer2 = tempEntity.newLdapServer("Test Server2");
    LdapConnection ldapConnection2 = createLdapConnection(ldapServer2);
    startLdapServer(testLdapServer2, ldapConnection2);
    LdapUserMapping umap2 = createUserMapping(ldapServer2);

    List<LdapUser> users1 = manager.testUserMapping(umap1, -1);
    assertUserMapping(users1, "1");
    List<LdapUser> users2 = manager.testUserMapping(umap2, -1);
    assertUserMapping(users2, "2");
  }

  private void assertUserMapping(List<LdapUser> users, String suffix) {
    assertThat(users, hasSize(3));

    Collections.sort(users); // sorts on username

    LdapUser user = users.get(0);
    assertThat(user.getUsername(), is("test*user1_" + suffix));
    assertThat(user.getDn(), is("uid=test*user1_" + suffix + ",ou=users,dc=company,dc=com"));
    assertThat(user.getRealName(), is("Test*User 1 " + suffix));
    assertThat(user.getEmail(), is("test.user3@company.com"));
    assertThat(user.getPassword(), nullValue()); // make sure password is not passed back
    assertThat(user.getMembership(), nullValue());

    user = users.get(1);
    assertThat(user.getUsername(), is("test_user1_" + suffix));
    assertThat(user.getDn(), is("uid=test_user1_" + suffix + ",ou=users,dc=company,dc=com"));
    assertThat(user.getRealName(), is("Test User 1 " + suffix));
    assertThat(user.getEmail(), is("test.user@company.com"));
    assertThat(user.getPassword(), nullValue()); // make sure password is not passed back
    assertThat(user.getMembership(), nullValue());

    user = users.get(2);
    assertThat(user.getUsername(), is("test_user2_" + suffix));
    assertThat(user.getDn(), is("uid=test_user2_" + suffix + ",ou=users,dc=company,dc=com"));
    assertThat(user.getRealName(), is("Test User 2 " + suffix));
    assertThat(user.getEmail(), is("test.user2@company.com"));
    assertThat(user.getPassword(), nullValue()); // make sure password is not passed back
    assertThat(user.getMembership(), nullValue());
  }

  @Test
  public void testTestUserMapping_DynamicGroupMapping() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    LdapUserMapping umap = createUserMapping(ldapServer);
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
  public void testTestUserMapping_StaticGroupMapping() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    LdapUserMapping umap = createUserMapping(ldapServer);
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
  public void testTestUserLogin() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    LdapUserMapping umap = newInMemoryUserMapping(ldapServer);

    umap.setUserPasswordAttribute(null); // AUTH-via-BIND

    assertCannotLogin(umap, "test_user1_1", "badGuess");
    assertCanLogin(umap, "test_user1_1", "far2simple");

    umap.setUserPasswordAttribute("userPassword"); // AUTH-via-ATTRIBUTE

    assertCannotLogin(umap, "test_user1_1", "badGuess");
    assertCanLogin(umap, "test_user1_1", "far2simple");
  }

  @Test
  public void testTestUserLogin_MultiServer() throws Exception {
    LdapServer ldapServer1 = tempEntity.newLdapServer("Test Server1");
    LdapConnection ldapConnection1 = createLdapConnection(ldapServer1);
    startLdapServer(testLdapServer1, ldapConnection1);
    LdapUserMapping umap1 = newInMemoryUserMapping(ldapServer1);

    LdapServer ldapServer2 = tempEntity.newLdapServer("Test Server2");
    LdapConnection ldapConnection2 = createLdapConnection(ldapServer2);
    startLdapServer(testLdapServer2, ldapConnection2);
    LdapUserMapping umap2 = newInMemoryUserMapping(ldapServer2);

    assertCanLogin(umap1, "test_user1_1", "far2simple");
    assertCanLogin(umap2, "test_user1_2", "far2simple");

    testLdapServer1.stop();
    assertCannotLoginWhenServerIsDown(umap1, "test_user1_1", "far2simple");
    assertCanLogin(umap2, "test_user1_2", "far2simple");

    testLdapServer2.stop();
    assertCannotLoginWhenServerIsDown(umap1, "test_user1_1", "far2simple");
    assertCannotLoginWhenServerIsDown(umap2, "test_user1_2", "far2simple");
  }

  private void assertCanLogin(LdapUserMapping umap, String username, String password) throws NamingException {
    manager.testUserLogin(umap, username, password.toCharArray());
  }

  private void assertCannotLogin(LdapUserMapping umap, String username, String password) throws NamingException {
    try {
      manager.testUserLogin(umap, username, password.toCharArray());
      fail("Expected AuthenticationException");
    }
    catch (AuthenticationException expected) {
    }
  }

  private void assertCannotLoginWhenServerIsDown(LdapUserMapping umap, String username, String password)
      throws NamingException
  {
    try {
      manager.testUserLogin(umap, username, password.toCharArray());
      fail("Expected AuthenticationException");
    }
    catch (CommunicationException expected) {
      assertThat(expected.getCause(), is(notNullValue()));
      assertThat(expected.getCause().getMessage(), startsWith("Connection refused"));
    }
  }

  @Test
  public void testRejectEmptyPasswordAsPerRfc4513Section5_1_2() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);
    LdapUserMapping umap = createUserMapping(ldapServer);

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
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);
    createUserMapping(ldapServer);

    try {
      // prior to the sanitization of query parameters, an AuthenticationException
      // would've been thrown here, and it leaked the first user name in the system
      manager.authenticateUser("*)(uid=*))(|(uid=*", "invalid".toCharArray());
      fail("authentication should have failed");
    }
    catch (NameNotFoundException e) {
      assertThat(e.getMessage(), not(containsString("test_user")));
    }
  }

  @Test(expected = NameNotFoundException.class)
  public void testAuthenticateUser_wildcardMatchingNotExpected() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);
    createUserMapping(ldapServer);

    // previous to escaping characters in the ldap query, this auth check would have succeeded
    // matching against the first test user in the system
    manager.authenticateUser("test*", "test".toCharArray());
  }

  @Test
  public void testAuthenticateUser_wildcardMatchingEscapedValue() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);
    createUserMapping(ldapServer);

    manager.authenticateUser("test*user1_1", "te*st".toCharArray());
  }

  @Test
  public void testAuthenticateUserForReverseProxy_usernameLeakViaInjection() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);
    createUserMapping(ldapServer);

    try {
      // prior to the sanitization of query parameters, an AuthenticationException
      // would've been thrown here, and it leaked the first user name in the system
      manager.authenticateUserForReverseProxy("*)(uid=*))(|(uid=*");
      fail("authentication should have failed");
    }
    catch (NameNotFoundException e) {
      assertThat(e.getMessage(), not(containsString("test_user")));
    }
  }

  @Test(expected = NameNotFoundException.class)
  public void testAuthenticateUserForReverseProxy_wildcardMatchingNotExpected() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);
    createUserMapping(ldapServer);

    // previous to escaping characters in the ldap query, this auth check would have succeeded
    // matching against the first test user in the system
    manager.authenticateUserForReverseProxy("test*");
  }

  @Test
  public void testAuthenticateUserForReverseProxy_wildcardMatchingEscapedValue() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);
    createUserMapping(ldapServer);

    manager.authenticateUserForReverseProxy("test*user1_1");
  }

  @Test
  public void testGetUsers() throws Exception {
    LdapServer ldapServer1 = tempEntity.newLdapServer("Test Server1");
    LdapServer ldapServer2 = tempEntity.newLdapServer("Test Server2");

    LdapConnection ldapConnection1 = createLdapConnection(ldapServer1);
    startLdapServer(testLdapServer1, ldapConnection1);
    LdapConnection ldapConnection2 = createLdapConnection(ldapServer2);
    startLdapServer(testLdapServer2, ldapConnection2);

    createUserMapping(ldapServer1);
    createUserMapping(ldapServer2);

    List<LdapUser> users1 = manager.getUsers(ldapServer1, new String[] { "test_user1_1", "test_user2_1", "test_user1_2" }, 100);
    assertThat(users1.size(), is(2));
    Collections.sort(users1);
    assertThat(users1.get(0).getUsername(), is("test_user1_1"));
    assertThat(users1.get(1).getUsername(), is("test_user2_1"));

    users1 = manager.getUsers(ldapServer1, new String[] { "foo" }, 100);
    assertThat(users1.size(), is(0));

    List<LdapUser> users2 = manager.getUsers(ldapServer2, new String[] { "test_user1_1", "test_user2_1", "test_user1_2" }, 100);
    assertThat(users2.size(), is(1));
    assertThat(users2.get(0).getUsername(), is("test_user1_2"));
  }

  @Test
  public void testGetUsers_wildcardMatchingNotExpected() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createUserMapping(ldapServer);

    List<LdapUser> users = manager.getUsers(ldapServer, new String[] { "test_user*" }, 100);
    assertThat(users.size(), is(0));
  }

  @Test
  public void testGetGroups_Static() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    LdapUserMapping umap = createUserMapping(ldapServer);
    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    umap.setGroupMappingType(LdapGroupMappingType.STATIC);
    umap.setGroupObjectClass("groupOfNames");
    umap.setGroupMemberAttribute("member");
    umap.setGroupMemberFormat("uid=${username}");
    userMappingDAO.update(umap);

    List<LdapGroup> groups = manager.getGroups(new String[] { "Gamma", "Theta" }, 100);
    assertThat(groups.size(), is(2));

    // Test max results
    groups = manager.getGroups(new String[] { "Gamma", "Theta" }, 1);
    assertThat(groups.size(), is(1));

    groups = manager.getGroups(new String[] { "foo" }, 100);
    assertThat(groups.size(), is(0));
  }

  @Test
  public void testGetGroups_Static_wildcardMatchingNotExpected() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    LdapUserMapping umap = createUserMapping(ldapServer);
    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    umap.setGroupMappingType(LdapGroupMappingType.STATIC);
    umap.setGroupObjectClass("groupOfNames");
    umap.setGroupMemberAttribute("member");
    umap.setGroupMemberFormat("uid=${username}");
    userMappingDAO.update(umap);

    List<LdapGroup> groups = manager.getGroups(new String[] { "*ta" }, 100);
    assertThat(groups.size(), is(0));
  }

  @Test
  public void testGetGroups_Dynamic() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    LdapUserMapping umap = createUserMapping(ldapServer);
    umap.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    umap.setUserMemberOfGroupAttribute("departmentNumber");
    userMappingDAO.update(umap);

    List<LdapGroup> groups = manager.getGroups(new String[] { "ab", "abc", "bc" }, 100);
    assertThat(groups.size(), is(3));

    groups = manager.getGroups(new String[] { "ab", "abc", "bc" }, 1);
    assertThat(groups.size(), is(1));

    groups = manager.getGroups(new String[] { "foo" }, 100);
    assertThat(groups.size(), is(0));
  }

  @Test
  public void testGetGroups_Dynamic_wildcardMatchingNotExpected() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    LdapUserMapping umap = createUserMapping(ldapServer);
    umap.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    umap.setUserMemberOfGroupAttribute("departmentNumber");
    userMappingDAO.update(umap);

    List<LdapGroup> groups = manager.getGroups(new String[] { "ab*" }, 100);
    assertThat(groups.size(), is(0));
  }

  @Test
  public void testFindUsersByName_Exact() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createUserMapping(ldapServer);

    List<LdapUser> users = manager.findUsersByName(ldapServer, "Test User 2 1", 100);
    assertThat(users, hasSize(1));
    assertThat(users.get(0).getRealName(), is("Test User 2 1"));
  }

  @Test
  public void testFindUsersByName_CaseInsensitive() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createUserMapping(ldapServer);

    List<LdapUser> users = manager.findUsersByName(ldapServer, "tEST user 2 1", 100);
    assertThat(users, hasSize(1));
    assertThat(users.get(0).getRealName(), is("Test User 2 1"));
  }

  @Test
  public void testFindUsersByName_Null() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createUserMapping(ldapServer);

    List<LdapUser> users = manager.findUsersByName(ldapServer, null /* name */, 100);
    assertThat(users, hasSize(0));
  }

  @Test
  public void testFindUsersByName_Wildcard() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createUserMapping(ldapServer);

    List<LdapUser> users = manager.findUsersByName(ldapServer, "*" /* name */, 100);
    assertThat(users, hasSize(3));
    List<String> foundNames = new ArrayList<>();
    for (LdapUser user : users) {
      foundNames.add(user.getUsername());
    }
    assertThat(foundNames, containsInAnyOrder("test_user1_1", "test_user2_1", "test*user1_1"));
  }

  @Test
  public void testFindUsersByName_TwoLdapServers() throws Exception {
    LdapServer ldapServer1 = tempEntity.newLdapServer("Test Server1");
    LdapConnection ldapConnection1 = createLdapConnection(ldapServer1);
    createUserMapping(ldapServer1);
    startLdapServer(testLdapServer1, ldapConnection1);
    LdapServer ldapServer2 = tempEntity.newLdapServer("Test Server2");
    LdapConnection ldapConnection2 = createLdapConnection(ldapServer2);
    createUserMapping(ldapServer2);
    startLdapServer(testLdapServer2, ldapConnection2);

    List<LdapUser> users1 = manager.findUsersByName(ldapServer1, "test*" /* name */, 100);
    assertThat(users1, hasSize(3));
    List<String> foundNames1 = new ArrayList<>();
    for (LdapUser user : users1) {
      foundNames1.add(user.getUsername());
    }
    assertThat(foundNames1, containsInAnyOrder("test_user1_1", "test_user2_1", "test*user1_1"));

    List<LdapUser> users2 = manager.findUsersByName(ldapServer2, "test*" /* name */, 100);
    assertThat(users2, hasSize(3));
    List<String> foundNames2 = new ArrayList<>();
    for (LdapUser user : users2) {
      foundNames2.add(user.getUsername());
    }
    assertThat(foundNames2, containsInAnyOrder("test_user1_2", "test_user2_2", "test*user1_2"));
  }

  @Test
  public void testFindUsersByName_MaxResults() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createUserMapping(ldapServer);

    List<LdapUser> users = manager.findUsersByName(ldapServer, "*" /* name */, 100);
    assertThat(users, hasSize(3));

    users = manager.findUsersByName(ldapServer, "*" /* name */, 1);
    assertThat(users, hasSize(1));
  }

  @Test
  public void testFindGroupsByName_Static_WrongObjectClass() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createStaticGroupMapping(ldapServer);

    // Wrong Objectclass, groupOfUniqueNames not groupOfNames
    List<LdapGroup> groups = manager.findGroupsByName(ldapServer, "Alpha", 100);
    assertThat(groups, hasSize(0));
  }

  @Test
  public void testFindGroupsByName_Static_Exact() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createStaticGroupMapping(ldapServer);

    List<LdapGroup> groups = manager.findGroupsByName(ldapServer, "Omega", 100);
    assertThat(groups, hasSize(1));
    assertThat(groups.get(0).getGroupname(), is("Omega"));

    groups = manager.findGroupsByName(ldapServer, "meg", 100);
    assertThat(groups, hasSize(0));
  }

  @Test
  public void testFindGroupsByName_Static_CaseInsensitive() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createStaticGroupMapping(ldapServer);

    List<LdapGroup> groups = manager.findGroupsByName(ldapServer, "oMEGA", 100);
    assertThat(groups, hasSize(1));
    assertThat(groups.get(0).getGroupname(), is("Omega"));
  }

  @Test
  public void testFindGroupsByName_Static_MaxResults() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createStaticGroupMapping(ldapServer);

    List<LdapGroup> groups = manager.findGroupsByName(ldapServer, "*a*", 100);
    assertThat(groups, hasSize(5));

    groups = manager.findGroupsByName(ldapServer, "*a*", 2);
    assertThat(groups, hasSize(2));
  }

  @Test
  public void testFindGroupsByName_Static_Wildcard() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createStaticGroupMapping(ldapServer);

    List<LdapGroup> groups = manager.findGroupsByName(ldapServer, "*a", 100);
    assertThat(groups, hasSize(5));
    List<String> foundNames = new ArrayList<>();
    for (LdapGroup group : groups) {
      foundNames.add(group.getGroupname());
    }
    assertThat(foundNames, containsInAnyOrder("Gamma", "Omega", "Theta", "Lambda", "Delta"));
  }

  @Test
  public void testFindGroupsByName_Static_NotFound() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createStaticGroupMapping(ldapServer);

    List<LdapGroup> groups = manager.findGroupsByName(ldapServer, "Foo", 100);
    assertThat(groups, hasSize(0));
  }

  @Test
  public void testFindUsersByGroup_Static_OnlyDnExpression() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    LdapUserMapping umap = createUserMapping(ldapServer);
    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    umap.setGroupMappingType(LdapGroupMappingType.STATIC);
    umap.setGroupObjectClass("groupOfNames");
    umap.setGroupMemberAttribute("member");
    umap.setGroupMemberFormat("${dn}");
    userMappingDAO.update(umap);

    List<LdapUser> users = manager.findUsersByGroup("Epsilon", 100);
    assertThat(users, hasSize(2));
  }

  @Test
  public void testFindUsersByGroup_Static_UsernameExpression() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    LdapUserMapping umap = createUserMapping(ldapServer);
    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    umap.setGroupMappingType(LdapGroupMappingType.STATIC);
    umap.setGroupObjectClass("groupOfNames");
    umap.setGroupMemberAttribute("member");
    umap.setGroupMemberFormat("uid=qwerty${username}zxcvbn,${dn}yuiop${username}");
    userMappingDAO.update(umap);

    List<LdapUser> users = manager.findUsersByGroup("Delta", 100);
    assertThat(users, hasSize(2));
  }

  @Test
  public void testFindUsersByGroup_Static_DnExpression() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    LdapUserMapping umap = createUserMapping(ldapServer);
    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    umap.setGroupMappingType(LdapGroupMappingType.STATIC);
    umap.setGroupObjectClass("groupOfNames");
    umap.setGroupMemberAttribute("member");
    umap.setGroupMemberFormat("dc=company,${dn},dc=com,${dn}");
    userMappingDAO.update(umap);

    List<LdapUser> users = manager.findUsersByGroup("Lambda", 100);
    assertThat(users, hasSize(2));
  }

  @Test
  public void testFindUsersByGroup_Static_Dn_MaxResults() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    LdapUserMapping umap = createUserMapping(ldapServer);
    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    umap.setGroupMappingType(LdapGroupMappingType.STATIC);
    umap.setGroupObjectClass("groupOfNames");
    umap.setGroupMemberAttribute("member");
    umap.setGroupMemberFormat("${dn}");
    userMappingDAO.update(umap);

    List<LdapUser> users = manager.findUsersByGroup("Epsilon", 1);
    assertThat(users, hasSize(1));
    users = manager.findUsersByGroup("Epsilon", 0);
    assertThat(users, hasSize(2));
  }

  @Test
  public void testFindUsersByGroup_Static_Username_MaxResults() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createStaticGroupMapping(ldapServer);

    List<LdapUser> users = manager.findUsersByGroup("Theta", 1);
    assertThat(users, hasSize(1));
    users = manager.findUsersByGroup("Theta", 0);
    assertThat(users, hasSize(2));
  }

  @Test
  public void testFindUsersByGroup_Static_wildcardMatchingNotExpected() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    LdapUserMapping umap = createUserMapping(ldapServer);
    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    umap.setGroupMappingType(LdapGroupMappingType.STATIC);
    umap.setGroupObjectClass("groupOfNames");
    umap.setGroupMemberAttribute("member");
    umap.setGroupMemberFormat("uid=qwerty${username}zxcvbn,${dn}yuiop${username}");
    userMappingDAO.update(umap);

    List<LdapUser> users = manager.findUsersByGroup("Delt*", 100);
    assertThat(users, hasSize(0));
  }

  @Test
  public void testFindUsersByGroup_Dynamic_wildcardMatchingNotExpected() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    LdapUserMapping umap = createUserMapping(ldapServer);
    umap.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    umap.setUserMemberOfGroupAttribute("departmentNumber");
    new LdapUserMappingDAO().update(umap);

    List<LdapUser> users = manager.findUsersByGroup("a*", 100);
    assertThat(users, hasSize(0));
  }

  private void createStaticGroupMapping(LdapServer ldapServer) {
    LdapUserMapping umap = createUserMapping(ldapServer);
    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    umap.setGroupMappingType(LdapGroupMappingType.STATIC);
    umap.setGroupObjectClass("groupOfNames");
    umap.setGroupMemberAttribute("member");
    umap.setGroupMemberFormat("uid=${username}");
    userMappingDAO.update(umap);
  }

  private void createDynamicGroupMapping(LdapServer ldapServer) {
    LdapUserMapping umap = createUserMapping(ldapServer);
    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    umap.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    umap.setUserMemberOfGroupAttribute("departmentNumber");
    userMappingDAO.update(umap);
  }

  @Test
  public void testFindGroupsByName_Dynamic_MaxResults() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createDynamicGroupMapping(ldapServer);

    List<LdapGroup> groups = manager.findGroupsByName(ldapServer, "*b*", 100);
    assertThat(groups, hasSize(5));

    groups = manager.findGroupsByName(ldapServer, "*b*", 2);
    assertThat(groups, hasSize(2));
  }

  @Test
  public void testFindGroupsByName_Dynamic_WildcardPrefix() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createDynamicGroupMapping(ldapServer);

    List<LdapGroup> groups = manager.findGroupsByName(ldapServer, "*b", 100);
    assertThat(groups, hasSize(2));
    List<String> foundNames = new ArrayList<>();
    for (LdapGroup group : groups) {
      foundNames.add(group.getGroupname());
    }
    assertThat(foundNames, containsInAnyOrder("ab", "xb"));
  }

  @Test
  public void testFindGroupsByName_Dynamic_WildcardSuffix() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createDynamicGroupMapping(ldapServer);

    List<LdapGroup> groups = manager.findGroupsByName(ldapServer, "b*", 100);
    assertThat(groups, hasSize(2));
    List<String> foundNames = new ArrayList<>();
    for (LdapGroup group : groups) {
      foundNames.add(group.getGroupname());
    }
    assertThat(foundNames, containsInAnyOrder("bc", "bx"));
  }

  @Test
  public void testFindGroupsByName_Dynamic_WildcardPrefixAndSuffix() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createDynamicGroupMapping(ldapServer);

    List<LdapGroup> groups = manager.findGroupsByName(ldapServer, "*b*", 100);
    assertThat(groups.toString(), groups, hasSize(5));
    List<String> foundNames = new ArrayList<>();
    for (LdapGroup group : groups) {
      foundNames.add(group.getGroupname());
    }
    assertThat(foundNames, containsInAnyOrder("ab", "abc", "bc", "xb", "bx"));
  }

  @Test
  public void testFindGroupsByName_Dynamic_Exact() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createDynamicGroupMapping(ldapServer);

    List<LdapGroup> groups = manager.findGroupsByName(ldapServer, "ab", 100);
    assertThat(groups, hasSize(1));
    assertThat(groups.get(0).getGroupname(), is("ab"));

    groups = manager.findGroupsByName(ldapServer, "b", 100);
    assertThat(groups, hasSize(0));
  }

  @Test
  public void testFindGroupsByName_Dynamic_CaseInsensitive() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createDynamicGroupMapping(ldapServer);

    List<LdapGroup> groups = manager.findGroupsByName(ldapServer, "ABC", 100);
    assertThat(groups, hasSize(1));
    assertThat(groups.get(0).getGroupname(), is("abc"));
  }

  @Test
  public void testFindGroupsByName_Dynamic_NotFound() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createDynamicGroupMapping(ldapServer);

    List<LdapGroup> groups = manager.findGroupsByName(ldapServer, "Foo", 100);
    assertThat(groups, hasSize(0));
  }

  @Test
  public void testIsLdapEnabled() throws Exception {
    assertThat(manager.isLdapEnabled(), is(false));

    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    assertThat(manager.isLdapEnabled(), is(false));

    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    ldapConnection.setHostname("localhost");
    manager.saveConnection(ldapConnection);
    assertThat(manager.isLdapEnabled(), is(false));

    createUserMapping(ldapServer);

    assertThat(manager.isLdapEnabled(), is(true));
  }

  @Test
  public void testIsLdapGroupEnabled() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    assertThat(manager.isLdapGroupEnabled(), is(false));

    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    ldapConnection.setHostname("localhost");
    ldapConnection.setSearchBase("dc=company,dc=com");
    manager.saveConnection(ldapConnection);

    assertThat(manager.isLdapGroupEnabled(), is(false));

    LdapUserMapping umap = createUserMapping(ldapServer);
    umap.setGroupMappingType(LdapGroupMappingType.NONE);

    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    userMappingDAO.update(umap);

    assertThat(manager.isLdapGroupEnabled(), is(false));

    umap.setGroupMappingType(LdapGroupMappingType.STATIC);
    userMappingDAO.update(umap);

    assertThat(manager.isLdapGroupEnabled(), is(true));
  }

  @Test
  public void testIsGroupSearchEnabled() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    assertThat(manager.isGroupSearchEnabled(), is(false));

    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    ldapConnection.setHostname("localhost");
    ldapConnection.setSearchBase("dc=company,dc=com");
    manager.saveConnection(ldapConnection);

    assertThat(manager.isGroupSearchEnabled(), is(false));

    LdapUserMapping umap = createUserMapping(ldapServer);
    umap.setGroupMappingType(LdapGroupMappingType.NONE);

    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    userMappingDAO.update(umap);

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
  public void testIsGroupSearchEnabled_WithGivenLdapServer() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    ldapConnection.setHostname("localhost");
    ldapConnection.setSearchBase("dc=company,dc=com");
    manager.saveConnection(ldapConnection);

    assertThat(manager.isGroupSearchEnabled(ldapServer), is(false));

    LdapUserMapping umap = createUserMapping(ldapServer);
    umap.setGroupMappingType(LdapGroupMappingType.NONE);

    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    userMappingDAO.update(umap);

    assertThat(manager.isGroupSearchEnabled(ldapServer), is(false));

    umap.setGroupMappingType(LdapGroupMappingType.STATIC);
    userMappingDAO.update(umap);

    assertThat(manager.isGroupSearchEnabled(ldapServer), is(true));

    umap.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    umap.setDynamicGroupSearchEnabled(true);
    userMappingDAO.update(umap);

    assertThat(manager.isGroupSearchEnabled(ldapServer), is(true));

    umap.setDynamicGroupSearchEnabled(false);
    userMappingDAO.update(umap);

    assertThat(manager.isGroupSearchEnabled(ldapServer), is(false));
  }
  
  @Test
  public void testIsGroupSearchEnabled_LdapConnectionNotSetup() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    try {
      manager.isGroupSearchEnabled(ldapServer);
      fail("Expected exception.");
    }
    catch (IllegalStateException expected) {
      assertThat(expected.getMessage(),
          containsString("LDAP connection is not configured for LDAP server " + ldapServer.getName() + "."));
    }
  }

  @Test
  public void testGetLdapServerName() throws Exception {
    try {
      manager.getLdapServerName();
      fail("Expected IllegalStateException");
    }
    catch (IllegalStateException expected) {
      assertThat(expected.getMessage(), is("LDAP server is not configured"));
    }

    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);
    String name = manager.getLdapServerName();
    assertThat(name, is("Test Server"));
  }

  private void startLdapServer(TestLdapServer testLdapServer, LdapConnection ldapConnection) throws Exception {
    testLdapServer.setPort(ldapConnection.getPort());
    testLdapServer.start();
  }

  private LdapConnection createLdapConnection(LdapServer ldapServer) {
    return tempEntity.newLdapConnection(ldapServer.getId(), getRandomPort());
  }

  private LdapUserMapping newInMemoryUserMapping(LdapServer ldapServer) {
    LdapUserMapping umap = new LdapUserMapping();
    umap.setServerId(ldapServer.getId());
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

  private LdapUserMapping createUserMapping(LdapServer ldapServer) {
    LdapUserMapping umap = newInMemoryUserMapping(ldapServer);
    tempEntity.newLdapUserMapping(umap);
    return umap;
  }

  @Test
  public void testFindUsersByGroup_Dynamic() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createDynamicGroupMapping(ldapServer);

    // Group with one user
    List<LdapUser> users = manager.findUsersByGroup("xb", 0 /* maxResults */);
    assertThat(users, hasSize(1));
    LdapUser user = users.get(0);
    assertThat(user.getUsername(), is("test_user1_1"));
    assertThat(user.getRealName(), is("Test User 1 1"));
    assertThat(user.getEmail(), is("test.user@company.com"));

    // Group with two users
    users = manager.findUsersByGroup("ab", 0 /* maxResults */);
    Set<String> usernames = new HashSet<>();
    for (LdapUser user1 : users) {
      usernames.add(user1.getUsername());
    }
    assertThat(usernames, containsInAnyOrder("test_user1_1", "test_user2_1", "test*user1_1"));

    // Group without users
    users = manager.findUsersByGroup("no such group", 0 /* maxResults */);
    assertThat(users, hasSize(0));
  }

  @Test
  public void testFindUsersByGroup_Dynamic_MaxResults() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createDynamicGroupMapping(ldapServer);

    // Group with two users
    List<LdapUser> users = manager.findUsersByGroup("ab", 0 /* maxResults */);
    assertThat(users, hasSize(3));
    users = manager.findUsersByGroup("ab", 1 /* maxResults */);
    assertThat(users, hasSize(1));
  }

  private static int getRandomPort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
