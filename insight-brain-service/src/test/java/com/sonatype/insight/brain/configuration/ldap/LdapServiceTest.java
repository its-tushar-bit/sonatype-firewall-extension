/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.naming.AuthenticationException;
import javax.naming.CommunicationException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapConnectionDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.model.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;

import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.offset;

/**
 * @since 1.7
 */
public class LdapServiceTest
    extends InjectedTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  public TemporaryFolder tempDir = new TemporaryFolder();

  public TestLdapServer testLdapServer1 = new TestLdapServer();

  public TestLdapServer testLdapServer2 = new TestLdapServer();

  public TestLdapServer testLdapServer3 = new TestLdapServer();

  public TestLdapServer testLdapServer4 = new TestLdapServer();

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(tempDir) //
      .around(testLdapServer1).around(testLdapServer2).around(testLdapServer3).around(testLdapServer4);

  @Before
  public void before() {
    String testClassName = getClass().getSimpleName();
    testLdapServer1.setWorkingDirectory(tempDir).setLdifResourceName("/" + testClassName + "/ldap_users1.ldif");
    testLdapServer2.setWorkingDirectory(tempDir).setLdifResourceName("/" + testClassName + "/ldap_users2.ldif");
    testLdapServer3.setWorkingDirectory(tempDir).setLdifResourceName("/" + testClassName + "/ldap_users2.ldif");
    testLdapServer4.setWorkingDirectory(tempDir).setLdifResourceName("/" + testClassName + "/ldap_users3.ldif");
  }

  @Inject
  private LdapService ldapService;

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
    ldapService.testConnection(ldapConnection);
  }

  private void assertCannotConnect(LdapConnection ldapConnection) {
    assertThatThrownBy(() -> {
      ldapService.testConnection(ldapConnection);
    }).isInstanceOf(CommunicationException.class)
        .satisfies(e -> assertThat(e.getCause()).hasMessageStartingWith("Connection refused"));
  }

  @Test
  public void testTestConnection_EscapedUrl() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    // Search base with space will be escaped with %20.
    ldapConnection.setSearchBase("dc=acme brick,dc=com");

    ldapService.testConnection(ldapConnection);
  }

  @Test
  public void testTestConnection_Timeout() throws Exception {
    try (ServerSocket socket = new ServerSocket(0)) {
      long begin = 0;
      long end = 0;

      LdapConnection ldapConnection = new LdapConnection();
      ldapConnection.setHostname("localhost");
      ldapConnection.setPort(socket.getLocalPort());

      // test very short timeout

      ldapConnection.setConnectionTimeout(1);
      begin = System.currentTimeMillis();
      assertThatThrownBy(() -> {
        ldapService.testConnection(ldapConnection);
      }).isInstanceOf(NamingException.class);
      end = System.currentTimeMillis();

      assertThat(Double.valueOf(end - begin)).isCloseTo(1300, offset(500.0));

      // test slightly longer timeout

      ldapConnection.setConnectionTimeout(5);
      begin = System.currentTimeMillis();
      assertThatThrownBy(() -> {
        ldapService.testConnection(ldapConnection);
      }).isInstanceOf(NamingException.class);
      end = System.currentTimeMillis();

      assertThat(Double.valueOf(end - begin)).isCloseTo(5300, offset(500.0));
    }
  }

  @Test
  public void testAuthenticateUser_RetryDelay() throws Exception {
    try (ServerSocket socket = new ServerSocket(0)) {
      LdapServer ldapServer = tempEntity.newLdapServer("Test Server");

      LdapConnection ldapConnection = createLdapConnection(ldapServer);
      ldapConnection.setHostname("localhost");
      ldapConnection.setPort(socket.getLocalPort());
      ldapConnection.setConnectionTimeout(1);
      ldapConnection.setRetryDelay(5);
      ldapService.saveConnection(ldapConnection);

      createUserMapping(ldapServer);

      // force three failures by attempting auth against the dangling socket

      for (int failures = 0; failures < 3; failures++) {
        assertThatThrownBy(() -> {
          ldapService.authenticateUser("user", "pass".toCharArray());
        }).isInstanceOf(NamingException.class).hasMessageContaining("read timed out");
      }

      long lastFailure = System.currentTimeMillis();

      // the next requests should be ignored while the retry delay is active

      for (int failures = 0; failures < 3; failures++) {
        assertThatThrownBy(() -> {
          ldapService.authenticateUser("user", "pass".toCharArray());
        }).isInstanceOf(NamingException.class).hasMessageContaining("Delaying retry");
      }

      while (System.currentTimeMillis() - lastFailure <= 5000) {
        Thread.sleep(200);
      }

      // the next request should NOT be ignored because the delay has expired

      assertThatThrownBy(() -> {
        ldapService.authenticateUser("user", "pass".toCharArray());
      }).isInstanceOf(NamingException.class).hasMessageContaining("read timed out");
    }
  }

  @Test
  public void testAuthenticateUser_SingleExceptionThrownAsIs() throws Exception {
    loadLdapServer(testLdapServer1, "Test Server1");

    assertThatThrownBy(() -> {
      ldapService.authenticateUser("test_user2_2", "test".toCharArray());
    }).isInstanceOf(NameNotFoundException.class).hasMessage("LDAP user with username 'test_user2_2' does not exist");
  }

  @Test
  public void testAuthenticateUser_MultiServer_BadPassword() throws Exception {
    loadLdapServer(testLdapServer1, "Test Server1");
    loadLdapServer(testLdapServer2, "Test Server2");

    assertThatThrownBy(() -> {
      ldapService.authenticateUser("test_user2_2", "badPWD".toCharArray());
    }).isInstanceOf(AuthenticationException.class)
        .hasMessage("LDAP Server: Test Server2 -> [LDAP: error code 49 - INVALID_CREDENTIALS: Bind failed:"
            + " ERR_229 Cannot authenticate user uid=test_user2_2,ou=users,dc=company,dc=com]")
        .satisfies(e -> assertThat(e.getSuppressed()).extracting(Throwable::getMessage).containsExactly(
            "LDAP user with username 'test_user2_2' does not exist",
            "[LDAP: error code 49 - INVALID_CREDENTIALS: Bind failed: "
                + "ERR_229 Cannot authenticate user uid=test_user2_2,ou=users,dc=company,dc=com]"));
  }

  private LdapServer loadLdapServer(final TestLdapServer testLdapServer, final String serverName) throws Exception {
    final LdapServer ldapServer = tempEntity.newLdapServer(serverName);
    final LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer, ldapConnection);

    createUserMapping(ldapServer);
    return ldapServer;
  }

  @Test
  public void testAuthenticateUser_MultiServer_Timeout_Single() throws Exception {
    final LdapConnection ldapConnection1 = createShortTimeoutLdapConnectionWithoutEmbeddedServer("Test Server1");
    loadLdapServer(testLdapServer2, "Test Server2");

    try (final ServerSocket ignored = new ServerSocket(ldapConnection1.getPort())) {
      assertThatThrownBy(() -> {
        ldapService.authenticateUser("any-user", "anything".toCharArray());
      }).isInstanceOf(NamingException.class)
          .hasMessage("LDAP Server: Test Server1 -> LDAP response read timed out, timeout used:1000ms.;\n")
          .satisfies(e -> assertThat(e.getSuppressed()).extracting(Throwable::getMessage).containsExactly(
              "LDAP response read timed out, timeout used:1000ms.",
              "LDAP user with username 'any-user' does not exist"));
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

    loadLdapServer(testLdapServer3, "Test Server3");
    try (final ServerSocket ignored = new ServerSocket(ldapConnection1.getPort())) {
      try (final ServerSocket ignored2 = new ServerSocket(ldapConnection2.getPort())) {
        assertThatThrownBy(() -> {
          ldapService.authenticateUser("any-user", "anything".toCharArray());
        }).isInstanceOf(NamingException.class)
            .hasMessage("LDAP Server: Test Server1 -> LDAP response read timed out, timeout used:1000ms.;\n"
                + "LDAP Server: Test Server2 -> LDAP response read timed out, timeout used:1000ms.;\n")
            .satisfies(e -> assertThat(e.getSuppressed()).extracting(Throwable::getMessage).containsExactly(
                "LDAP response read timed out, timeout used:1000ms.",
                "LDAP response read timed out, timeout used:1000ms.",
                "LDAP user with username 'any-user' does not exist"));
      }
    }
  }

  @Test
  public void testAuthenticateUser_MultiServer_UnknownUser() throws Exception {
    loadLdapServer(testLdapServer1, "Test Server1");
    loadLdapServer(testLdapServer2, "Test Server2");

    final int ldapServer1Port = testLdapServer1.getPort();
    testLdapServer1.stop();
    assertThatThrownBy(() -> {
      ldapService.authenticateUser("test_user4", "anything".toCharArray());
    }).isInstanceOf(NamingException.class).hasMessage("LDAP Server: Test Server1 -> localhost:" + ldapServer1Port
        + ";\n" + "LDAP Server: Test Server2 -> LDAP user with username 'test_user4' does not exist;\n")
        .satisfies(e -> {
          assertThat(e.getSuppressed()).hasSize(2);
          // Use startsWith because the error message depends on the OS.
          assertThat(e.getSuppressed()[0].getCause()).hasMessageStartingWith("Connection refused");
          assertThat(e.getSuppressed()[1]).hasMessage("LDAP user with username 'test_user4' does not exist");
        });
  }

  @Test
  public void testAuthenticateUser_MultiServer_UnexpectedError() throws Exception {
    loadLdapServer(testLdapServer1, "Test Server1");
    loadLdapServer(testLdapServer2, "Test Server2");

    assertThatThrownBy(() -> {
      ldapService.authenticateUser("test_user4", "anything".toCharArray());
    }).isInstanceOf(NameNotFoundException.class)
        .hasMessage("LDAP Server: Test Server1 -> LDAP user with username 'test_user4' does not exist;\n"
            + "LDAP Server: Test Server2 -> LDAP user with username 'test_user4' does not exist;\n")
        .satisfies(e -> assertThat(e.getSuppressed()).extracting(Throwable::getMessage).containsExactly(
            "LDAP user with username 'test_user4' does not exist",
            "LDAP user with username 'test_user4' does not exist"));
  }

  @Test
  public void testAuthenticateUser_MultiServer_ValidLoginFirstServer() throws Exception {
    LdapServer ldapServer1 = loadLdapServer(testLdapServer1, "Test Server1");
    loadLdapServer(testLdapServer2, "Test Server2");

    final LdapUser ldapUser = ldapService.authenticateUser("test_user2_1", "test".toCharArray());
    assertThat(ldapUser.getRealName()).isEqualTo("Test User 2 1");
    assertThat(ldapUser.getServerId()).isEqualTo(ldapServer1.getId());
  }

  @Test
  public void testAuthenticateUser_MultiServer_ValidLoginSecondServer() throws Exception {
    loadLdapServer(testLdapServer1, "Test Server1");
    LdapServer ldapServer2 = loadLdapServer(testLdapServer2, "Test Server2");

    final LdapUser ldapUser = ldapService.authenticateUser("test_user2_2", "test".toCharArray());
    assertThat(ldapUser.getRealName()).isEqualTo("Test User 2 2");
    assertThat(ldapUser.getServerId()).isEqualTo(ldapServer2.getId());
  }

  @Test
  public void testGetUserByName_RetryDelay() throws Exception {
    try (ServerSocket socket = new ServerSocket(0)) {
      LdapServer ldapServer = tempEntity.newLdapServer("Test Server");

      LdapConnection ldapConnection = createLdapConnection(ldapServer);
      ldapConnection.setHostname("localhost");
      ldapConnection.setPort(socket.getLocalPort());
      ldapConnection.setConnectionTimeout(1);
      ldapConnection.setRetryDelay(5);
      ldapService.saveConnection(ldapConnection);

      createUserMapping(ldapServer);

      // force three failures by attempting the operation against the dangling socket

      for (int failures = 0; failures < 3; failures++) {
        assertThatThrownBy(() -> {
          ldapService.getUserByName("user");
        }).isInstanceOf(NamingException.class).hasMessageContaining("read timed out");
      }

      long lastFailure = System.currentTimeMillis();

      // the next requests should be ignored while the retry delay is active

      for (int failures = 0; failures < 3; failures++) {
        assertThatThrownBy(() -> {
          ldapService.getUserByName("user");
        }).isInstanceOf(NamingException.class).hasMessageContaining("Delaying retry");
      }

      while (System.currentTimeMillis() - lastFailure <= 5000) {
        Thread.sleep(200);
      }

      // the next request should NOT be ignored because the delay has expired

      assertThatThrownBy(() -> {
        ldapService.getUserByName("user");
      }).isInstanceOf(NamingException.class).hasMessageContaining("read timed out");
    }
  }

  @Test
  public void testGetUserByName_SingleExceptionThrownAsIs() throws Exception {
    loadLdapServer(testLdapServer1, "Test Server1");

    assertThatThrownBy(() -> {
      ldapService.getUserByName("test_user2_2");
    }).isInstanceOf(NameNotFoundException.class).hasMessage("LDAP user with username 'test_user2_2' does not exist");
  }

  @Test
  public void testGetUserByName_MultiServer_Timeout_Single() throws Exception {
    final LdapConnection ldapConnection1 = createShortTimeoutLdapConnectionWithoutEmbeddedServer("Test Server1");
    loadLdapServer(testLdapServer2, "Test Server2");

    try (final ServerSocket ignored = new ServerSocket(ldapConnection1.getPort())) {
      assertThatThrownBy(() -> {
        ldapService.getUserByName("any-user");
      }).isInstanceOf(NamingException.class)
          .hasMessage("LDAP Server: Test Server1 -> LDAP response read timed out, timeout used:1000ms.;\n")
          .satisfies(e -> assertThat(e.getSuppressed()).extracting(Throwable::getMessage).containsExactly(
              "LDAP response read timed out, timeout used:1000ms.",
              "LDAP user with username 'any-user' does not exist"));
    }
  }

  @Test
  public void testGetUserByName_MultiServer_Timeout_MultipleAggregated() throws Exception {
    final LdapConnection ldapConnection1 = createShortTimeoutLdapConnectionWithoutEmbeddedServer("Test Server1");
    final LdapConnection ldapConnection2 = createShortTimeoutLdapConnectionWithoutEmbeddedServer("Test Server2");

    loadLdapServer(testLdapServer3, "Test Server3");
    try (final ServerSocket ignored = new ServerSocket(ldapConnection1.getPort())) {
      try (final ServerSocket ignored2 = new ServerSocket(ldapConnection2.getPort())) {
        assertThatThrownBy(() -> {
          ldapService.getUserByName("any-user");
        }).isInstanceOf(NamingException.class)
            .hasMessage("LDAP Server: Test Server1 -> LDAP response read timed out, timeout used:1000ms.;\n"
                + "LDAP Server: Test Server2 -> LDAP response read timed out, timeout used:1000ms.;\n")
            .satisfies(e -> assertThat(e.getSuppressed()).extracting(Throwable::getMessage).containsExactly(
                "LDAP response read timed out, timeout used:1000ms.",
                "LDAP response read timed out, timeout used:1000ms.",
                "LDAP user with username 'any-user' does not exist"));
      }
    }
  }

  @Test
  public void testGetUserByName_MultiServer_UnknownUser() throws Exception {
    loadLdapServer(testLdapServer1, "Test Server1");
    loadLdapServer(testLdapServer2, "Test Server2");

    final int ldapServer1Port = testLdapServer1.getPort();
    testLdapServer1.stop();
    assertThatThrownBy(() -> {
      ldapService.getUserByName("test_user4");
    }).isInstanceOf(NamingException.class).hasMessage("LDAP Server: Test Server1 -> localhost:" + ldapServer1Port
        + ";\n" + "LDAP Server: Test Server2 -> LDAP user with username 'test_user4' does not exist;\n")
        .satisfies(e -> {
          assertThat(e.getSuppressed()).hasSize(2);
          // Use startsWith because the error message depends on the OS.
          assertThat(e.getSuppressed()[0].getCause()).hasMessageStartingWith("Connection refused");
          assertThat(e.getSuppressed()[1]).hasMessage("LDAP user with username 'test_user4' does not exist");
        });
  }

  @Test
  public void testGetUserByName_MultiServer_UnexpectedError() throws Exception {
    loadLdapServer(testLdapServer1, "Test Server1");
    loadLdapServer(testLdapServer2, "Test Server2");

    assertThatThrownBy(() -> {
      ldapService.getUserByName("test_user4");
    }).isInstanceOf(NameNotFoundException.class)
        .hasMessage("LDAP Server: Test Server1 -> LDAP user with username 'test_user4' does not exist;\n"
            + "LDAP Server: Test Server2 -> LDAP user with username 'test_user4' does not exist;\n")
        .satisfies(e -> assertThat(e.getSuppressed()).extracting(Throwable::getMessage).containsExactly(
            "LDAP user with username 'test_user4' does not exist",
            "LDAP user with username 'test_user4' does not exist"));
  }

  @Test
  public void testGetUserByName_MultiServer_ValidLoginFirstServer() throws Exception {
    LdapServer ldapServer1 = loadLdapServer(testLdapServer1, "Test Server1");
    loadLdapServer(testLdapServer2, "Test Server2");

    final LdapUser ldapUser = ldapService.getUserByName("test_user2_1");
    assertThat(ldapUser.getRealName()).isEqualTo("Test User 2 1");
    assertThat(ldapUser.getServerId()).isEqualTo(ldapServer1.getId());
  }

  @Test
  public void testGetUserByName_MultiServer_ValidLoginSecondServer() throws Exception {
    loadLdapServer(testLdapServer1, "Test Server1");
    LdapServer ldapServer2 = loadLdapServer(testLdapServer2, "Test Server2");

    final LdapUser ldapUser = ldapService.getUserByName("test_user2_2");
    assertThat(ldapUser.getRealName()).isEqualTo("Test User 2 2");
    assertThat(ldapUser.getServerId()).isEqualTo(ldapServer2.getId());
  }

  @Test
  public void testGetUserByName_FirstServerDisabled() throws Exception {
    tempEntity.newLdapServer("Test Server1");
    loadLdapServer(testLdapServer2, "Test Server2");

    assertThat(ldapService.getUserByName("test_user1_2")).isNotNull();
  }

  @Test
  public void testTestConnection_BadSearchBase() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    ldapConnection.setSearchBase("!@£$%^&*()");

    assertThatThrownBy(() -> {
      ldapService.testConnection(ldapConnection);
    }).isInstanceOf(NamingException.class);
  }

  private void setSearchBase(LdapConnection ldapConnection, String searchBase) {
    ldapConnection.setSearchBase(searchBase);
    ldapService.saveConnection(ldapConnection);
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

    List<LdapUser> users1 = ldapService.testUserMapping(umap1, -1);
    assertUserMapping(users1, "1");
    List<LdapUser> users2 = ldapService.testUserMapping(umap2, -1);
    assertUserMapping(users2, "2");
  }

  private void assertUserMapping(List<LdapUser> users, String suffix) {
    assertThat(users).hasSize(3);

    Collections.sort(users); // sorts on username

    LdapUser user = users.get(0);
    assertThat(user.getUsername()).isEqualTo("test*user1_" + suffix);
    assertThat(user.getDn()).isEqualTo("uid=test*user1_" + suffix + ",ou=users,dc=company,dc=com");
    assertThat(user.getRealName()).isEqualTo("Test*User 1 " + suffix);
    assertThat(user.getEmail()).isEqualTo("test.user3_" + suffix + "@company.com");
    assertThat(user.getPassword()).isNull(); // make sure password is not passed back
    assertThat(user.getMembership()).isNull();

    user = users.get(1);
    assertThat(user.getUsername()).isEqualTo("test_user1_" + suffix);
    assertThat(user.getDn()).isEqualTo("uid=test_user1_" + suffix + ",ou=users,dc=company,dc=com");
    assertThat(user.getRealName()).isEqualTo("Test User 1 " + suffix);
    assertThat(user.getEmail()).isEqualTo("test.user1_" + suffix + "@company.com");
    assertThat(user.getPassword()).isNull(); // make sure password is not passed back
    assertThat(user.getMembership()).isNull();

    user = users.get(2);
    assertThat(user.getUsername()).isEqualTo("test_user2_" + suffix);
    assertThat(user.getDn()).isEqualTo("uid=test_user2_" + suffix + ",ou=users,dc=company,dc=com");
    assertThat(user.getRealName()).isEqualTo("Test User 2 " + suffix);
    assertThat(user.getEmail()).isEqualTo("test.user2_" + suffix + "@company.com");
    assertThat(user.getPassword()).isNull(); // make sure password is not passed back
    assertThat(user.getMembership()).isNull();
  }

  @Test
  public void testTestUserMapping_DynamicGroupMapping() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    LdapUserMapping umap = createUserMapping(ldapServer);
    umap.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    umap.setUserMemberOfGroupAttribute("departmentNumber");

    List<LdapUser> users = ldapService.testUserMapping(umap, -1);
    assertThat(users).hasSize(3);

    Collections.sort(users); // sorts on username

    assertThat(users.get(0).getMembership()).containsExactlyInAnyOrder("ab", "bc", "bx");
    assertThat(users.get(1).getMembership()).containsExactlyInAnyOrder("ab", "abc", "xb");
    assertThat(users.get(2).getMembership()).containsExactlyInAnyOrder("ab", "bc", "bx");
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

    List<LdapUser> users = ldapService.testUserMapping(umap, -1);
    assertThat(users).hasSize(3);

    Collections.sort(users); // sorts on username

    assertThat(users.get(0).getMembership()).isEmpty();
    assertThat(users.get(1).getMembership()).containsExactlyInAnyOrder("Alpha");
    assertThat(users.get(2).getMembership()).containsExactlyInAnyOrder("Beta");

    umap.setGroupMappingType(LdapGroupMappingType.STATIC);
    umap.setGroupObjectClass("groupOfNames");
    umap.setGroupMemberAttribute("member");
    umap.setGroupMemberFormat("uid=${username}");

    users = ldapService.testUserMapping(umap, -1);
    assertThat(users).hasSize(3);

    Collections.sort(users); // sorts on username

    assertThat(users.get(0).getMembership()).isEmpty();
    assertThat(users.get(1).getMembership()).containsExactlyInAnyOrder("Gamma", "Theta", "Omega");
    assertThat(users.get(2).getMembership()).containsExactlyInAnyOrder("Theta");
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
    ldapService.testUserLogin(umap, username, password.toCharArray());
  }

  private void assertCannotLogin(LdapUserMapping umap, String username, String password) {
    assertThatThrownBy(() -> {
      ldapService.testUserLogin(umap, username, password.toCharArray());
    }).isInstanceOf(AuthenticationException.class);
  }

  private void assertCannotLoginWhenServerIsDown(LdapUserMapping umap, String username, String password) {
    assertThatThrownBy(() -> {
      ldapService.testUserLogin(umap, username, password.toCharArray());
    }).isInstanceOf(CommunicationException.class)
        .satisfies(e -> assertThat(e.getCause()).hasMessageStartingWith("Connection refused"));
  }

  @Test
  public void testAuthenticateUser_RejectEmptyPasswordAsPerRfc4513Section5_1_2() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);
    createUserMapping(ldapServer);

    assertThatThrownBy(() -> {
      ldapService.authenticateUser("test_user", "".toCharArray());
    }).isInstanceOf(AuthenticationException.class);
  }

  @Test
  public void testTestUserLogin_RejectEmptyPasswordAsPerRfc4513Section5_1_2() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);
    LdapUserMapping umap = createUserMapping(ldapServer);

    assertThatThrownBy(() -> {
      ldapService.testUserLogin(umap, "test_user", "".toCharArray());
    }).isInstanceOf(AuthenticationException.class);
  }

  @Test
  public void testAuthenticateUser_usernameLeakViaInjection() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);
    createUserMapping(ldapServer);

    assertThatThrownBy(() -> {
      // prior to the sanitization of query parameters, an AuthenticationException
      // would've been thrown here, and it leaked the first user name in the system
      ldapService.authenticateUser("*)(uid=*))(|(uid=*", "invalid".toCharArray());
    }).isInstanceOf(NameNotFoundException.class).satisfies(e -> assertThat(e.getMessage()).doesNotContain("test_user"));
  }

  @Test(expected = NameNotFoundException.class)
  public void testAuthenticateUser_wildcardMatchingNotExpected() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);
    createUserMapping(ldapServer);

    // previous to escaping characters in the ldap query, this auth check would have succeeded
    // matching against the first test user in the system
    ldapService.authenticateUser("test*", "test".toCharArray());
  }

  @Test
  public void testAuthenticateUser_wildcardMatchingEscapedValue() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);
    createUserMapping(ldapServer);

    ldapService.authenticateUser("test*user1_1", "te*st".toCharArray());
  }

  @Test
  public void testGetUserByName_usernameLeakViaInjection() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);
    createUserMapping(ldapServer);

    assertThatThrownBy(() -> {
      // prior to the sanitization of query parameters, an AuthenticationException
      // would've been thrown here, and it leaked the first user name in the system
      ldapService.getUserByName("*)(uid=*))(|(uid=*");
    }).isInstanceOf(NameNotFoundException.class).satisfies(e -> assertThat(e.getMessage()).doesNotContain("test_user"));
  }

  @Test(expected = NameNotFoundException.class)
  public void testGetUserByName_wildcardMatchingNotExpected() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);
    createUserMapping(ldapServer);

    // previous to escaping characters in the ldap query, this would have succeeded
    // matching against the first test user in the system
    ldapService.getUserByName("test*");
  }

  @Test
  public void testGetUserByName_wildcardMatchingEscapedValue() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);
    createUserMapping(ldapServer);

    ldapService.getUserByName("test*user1_1");
  }

  @Test
  public void testGetUsersByName() throws Exception {
    LdapServer ldapServer1 = tempEntity.newLdapServer("Test Server1");
    LdapServer ldapServer2 = tempEntity.newLdapServer("Test Server2");

    LdapConnection ldapConnection1 = createLdapConnection(ldapServer1);
    startLdapServer(testLdapServer1, ldapConnection1);
    LdapConnection ldapConnection2 = createLdapConnection(ldapServer2);
    startLdapServer(testLdapServer2, ldapConnection2);

    createUserMapping(ldapServer1);
    createUserMapping(ldapServer2);

    List<LdapUser> users1 = ldapService.getUsersByName(ldapServer1,
        new String[] { "test_user1_1", "test_user2_1", "test_user1_2" });
    assertThat(users1).extracting(LdapUser::getUsername).containsExactlyInAnyOrder("test_user1_1", "test_user2_1");
    assertThat(users1).extracting(LdapUser::getServerId).containsExactlyInAnyOrder(ldapServer1.getId(),
        ldapServer1.getId());

    users1 = ldapService.getUsersByName(ldapServer1, new String[] { "foo" });
    assertThat(users1).isEmpty();

    List<LdapUser> users2 = ldapService.getUsersByName(ldapServer2,
        new String[] { "test_user1_1", "test_user2_1", "test_user1_2" });
    assertThat(users2).extracting(LdapUser::getUsername).containsExactly("test_user1_2");
    assertThat(users2).extracting(LdapUser::getServerId).containsExactly(ldapServer2.getId());
  }

  @Test
  public void testGetUsersByName_wildcardMatchingNotExpected() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createUserMapping(ldapServer);

    List<LdapUser> users = ldapService.getUsersByName(ldapServer, new String[] { "test_user*" });
    assertThat(users).isEmpty();
  }

  @Test
  public void testGetGroupsByName_Static() throws Exception {
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

    List<LdapGroup> groups = ldapService.getGroupsByName(ldapServer, new String[] { "Gamma", "Theta" });
    assertThat(groups).hasSize(2);

    groups = ldapService.getGroupsByName(ldapServer, new String[] { "foo" });
    assertThat(groups).isEmpty();
  }

  @Test
  public void testGetGroupsByName_Static_wildcardMatchingNotExpected() throws Exception {
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

    List<LdapGroup> groups = ldapService.getGroupsByName(ldapServer, new String[] { "*ta" });
    assertThat(groups).isEmpty();
  }

  @Test
  public void testGetGroupsByName_Dynamic() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    LdapUserMapping umap = createUserMapping(ldapServer);
    umap.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    umap.setUserMemberOfGroupAttribute("departmentNumber");
    userMappingDAO.update(umap);

    List<LdapGroup> groups = ldapService.getGroupsByName(ldapServer, new String[] { "ab", "abc", "bc" });
    assertThat(groups).hasSize(3);

    groups = ldapService.getGroupsByName(ldapServer, new String[] { "foo" });
    assertThat(groups).isEmpty();
  }

  @Test
  public void testGetGroupsByName_Dynamic_wildcardMatchingNotExpected() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    LdapUserMapping umap = createUserMapping(ldapServer);
    umap.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    umap.setUserMemberOfGroupAttribute("departmentNumber");
    userMappingDAO.update(umap);

    List<LdapGroup> groups = ldapService.getGroupsByName(ldapServer, new String[] { "ab*" });
    assertThat(groups).isEmpty();
  }

  @Test
  public void testFindUsersByName_Exact() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createUserMapping(ldapServer);

    List<LdapUser> users = ldapService.findUsersByName(ldapServer, "Test User 2 1", 100);
    assertThat(users).extracting(LdapUser::getRealName).containsExactly("Test User 2 1");
  }

  @Test
  public void testFindUsersByName_CaseInsensitive() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createUserMapping(ldapServer);

    List<LdapUser> users = ldapService.findUsersByName(ldapServer, "tEST user 2 1", 100);
    assertThat(users).extracting(LdapUser::getRealName).containsExactly("Test User 2 1");
  }

  @Test
  public void testFindUsersByName_Null() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createUserMapping(ldapServer);

    List<LdapUser> users = ldapService.findUsersByName(ldapServer, null /* name */, 100);
    assertThat(users).isEmpty();
  }

  @Test
  public void testFindUsersByName_Wildcard() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createUserMapping(ldapServer);

    List<LdapUser> users = ldapService.findUsersByName(ldapServer, "*" /* name */, 100);
    assertThat(users).extracting(LdapUser::getUsername).containsExactlyInAnyOrder("test_user1_1", "test_user2_1",
        "test*user1_1");
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

    List<LdapUser> users1 = ldapService.findUsersByName(ldapServer1, "test*" /* name */, 100);
    assertThat(users1).extracting(LdapUser::getUsername).containsExactlyInAnyOrder("test_user1_1", "test_user2_1",
        "test*user1_1");
    assertThat(users1).extracting(LdapUser::getServerId).containsExactlyInAnyOrder(ldapServer1.getId(),
        ldapServer1.getId(), ldapServer1.getId());

    List<LdapUser> users2 = ldapService.findUsersByName(ldapServer2, "test*" /* name */, 100);
    assertThat(users2).extracting(LdapUser::getUsername).containsExactlyInAnyOrder("test_user1_2", "test_user2_2",
        "test*user1_2");
    assertThat(users2).extracting(LdapUser::getServerId).containsExactlyInAnyOrder(ldapServer2.getId(),
        ldapServer2.getId(), ldapServer2.getId());
  }

  @Test
  public void testFindUsersByName_MaxResults() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createUserMapping(ldapServer);

    List<LdapUser> users = ldapService.findUsersByName(ldapServer, "*" /* name */, 100);
    assertThat(users).hasSize(3);

    users = ldapService.findUsersByName(ldapServer, "*" /* name */, 1);
    assertThat(users).hasSize(1);
  }

  @Test
  public void testFindGroupsByName_Static_WrongObjectClass() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createStaticGroupMapping(ldapServer);

    // Wrong Objectclass, groupOfUniqueNames not groupOfNames
    List<LdapGroup> groups = ldapService.findGroupsByName(ldapServer, "Alpha", 100);
    assertThat(groups).isEmpty();
  }

  @Test
  public void testFindGroupsByName_Static_Exact() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createStaticGroupMapping(ldapServer);

    List<LdapGroup> groups = ldapService.findGroupsByName(ldapServer, "Omega", 100);
    assertThat(groups).extracting(LdapGroup::getGroupname).containsExactly("Omega");

    groups = ldapService.findGroupsByName(ldapServer, "meg", 100);
    assertThat(groups).isEmpty();
  }

  @Test
  public void testFindGroupsByName_Static_CaseInsensitive() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createStaticGroupMapping(ldapServer);

    List<LdapGroup> groups = ldapService.findGroupsByName(ldapServer, "oMEGA", 100);
    assertThat(groups).extracting(LdapGroup::getGroupname).containsExactly("Omega");
  }

  @Test
  public void testFindGroupsByName_Static_MaxResults() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createStaticGroupMapping(ldapServer);

    List<LdapGroup> groups = ldapService.findGroupsByName(ldapServer, "*a*", 100);
    assertThat(groups).hasSize(5);

    groups = ldapService.findGroupsByName(ldapServer, "*a*", 2);
    assertThat(groups).hasSize(2);
  }

  @Test
  public void testFindGroupsByName_Static_Wildcard() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createStaticGroupMapping(ldapServer);

    List<LdapGroup> groups = ldapService.findGroupsByName(ldapServer, "*a", 100);
    assertThat(groups).extracting(LdapGroup::getGroupname).containsExactlyInAnyOrder("Gamma", "Omega", "Theta",
        "Lambda", "Delta");
  }

  @Test
  public void testFindGroupsByName_Static_NotFound() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createStaticGroupMapping(ldapServer);

    List<LdapGroup> groups = ldapService.findGroupsByName(ldapServer, "Foo", 100);
    assertThat(groups).isEmpty();
  }

  @Test
  public void testGetUsersByGroup_Static_DnExpressionCaseInsensitive() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer4, ldapConnection);

    LdapUserMapping umap = createUserMapping(ldapServer);
    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    umap.setGroupMappingType(LdapGroupMappingType.STATIC);
    umap.setGroupObjectClass("groupOfNames");
    umap.setGroupMemberAttribute("member");
    umap.setGroupMemberFormat("${dn}");
    userMappingDAO.update(umap);

    List<LdapUser> users = ldapService.getUsersByGroup(ldapServer, "Epsilon");
    assertThat(users).extracting(LdapUser::getUsername).containsExactlyInAnyOrder("test_user1", "test_user2");
  }

  @Test
  public void testGetUsersByGroup_Static_UsernameExpression() throws Exception {
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

    List<LdapUser> users = ldapService.getUsersByGroup(ldapServer, "Delta");
    assertThat(users).hasSize(2);
  }

  @Test
  public void testGetUsersByGroup_Static_DnExpression() throws Exception {
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

    List<LdapUser> users = ldapService.getUsersByGroup(ldapServer, "Lambda");
    assertThat(users).hasSize(2);
  }

  @Test
  public void testGetUsersByGroup_Static_wildcardMatchingNotExpected() throws Exception {
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

    List<LdapUser> users = ldapService.getUsersByGroup(ldapServer, "Delt*");
    assertThat(users).isEmpty();
  }

  @Test
  public void testGetUsersByGroup_Dynamic_wildcardMatchingNotExpected() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    LdapUserMapping umap = createUserMapping(ldapServer);
    umap.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    umap.setUserMemberOfGroupAttribute("departmentNumber");
    new LdapUserMappingDAO().update(umap);

    List<LdapUser> users = ldapService.getUsersByGroup(ldapServer, "a*");
    assertThat(users).isEmpty();
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

    List<LdapGroup> groups = ldapService.findGroupsByName(ldapServer, "*b*", 100);
    assertThat(groups).hasSize(5);

    groups = ldapService.findGroupsByName(ldapServer, "*b*", 2);
    assertThat(groups).hasSize(2);
  }

  @Test
  public void testFindGroupsByName_Dynamic_WildcardPrefix() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createDynamicGroupMapping(ldapServer);

    List<LdapGroup> groups = ldapService.findGroupsByName(ldapServer, "*b", 100);
    assertThat(groups).extracting(LdapGroup::getGroupname).containsExactlyInAnyOrder("ab", "xb");
  }

  @Test
  public void testFindGroupsByName_Dynamic_WildcardSuffix() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createDynamicGroupMapping(ldapServer);

    List<LdapGroup> groups = ldapService.findGroupsByName(ldapServer, "b*", 100);
    assertThat(groups).extracting(LdapGroup::getGroupname).containsExactlyInAnyOrder("bc", "bx");
  }

  @Test
  public void testFindGroupsByName_Dynamic_WildcardPrefixAndSuffix() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createDynamicGroupMapping(ldapServer);

    List<LdapGroup> groups = ldapService.findGroupsByName(ldapServer, "*b*", 100);
    assertThat(groups).extracting(LdapGroup::getGroupname).containsExactlyInAnyOrder("ab", "abc", "bc", "xb", "bx");
  }

  @Test
  public void testFindGroupsByName_Dynamic_Exact() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createDynamicGroupMapping(ldapServer);

    List<LdapGroup> groups = ldapService.findGroupsByName(ldapServer, "ab", 100);
    assertThat(groups).extracting(LdapGroup::getGroupname).containsExactly("ab");

    groups = ldapService.findGroupsByName(ldapServer, "b", 100);
    assertThat(groups).isEmpty();
  }

  @Test
  public void testFindGroupsByName_Dynamic_CaseInsensitive() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createDynamicGroupMapping(ldapServer);

    List<LdapGroup> groups = ldapService.findGroupsByName(ldapServer, "ABC", 100);
    assertThat(groups).extracting(LdapGroup::getGroupname).containsExactly("abc");
  }

  @Test
  public void testFindGroupsByName_Dynamic_NotFound() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createDynamicGroupMapping(ldapServer);

    List<LdapGroup> groups = ldapService.findGroupsByName(ldapServer, "Foo", 100);
    assertThat(groups).isEmpty();
  }

  @Test
  public void testIsLdapEnabled() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    assertThat(ldapService.isLdapEnabled(ldapServer)).isFalse();

    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    ldapConnection.setHostname("localhost");
    ldapService.saveConnection(ldapConnection);
    assertThat(ldapService.isLdapEnabled(ldapServer)).isFalse();

    createUserMapping(ldapServer);

    assertThat(ldapService.isLdapEnabled(ldapServer)).isTrue();
  }
  
  @Test
  public void testIsGroupSearchEnabled() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    createLdapConnection(ldapServer);

    assertThat(ldapService.isGroupSearchEnabled(ldapServer)).isFalse();

    LdapUserMapping umap = createUserMapping(ldapServer);
    umap.setGroupMappingType(LdapGroupMappingType.NONE);

    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    userMappingDAO.update(umap);

    assertThat(ldapService.isGroupSearchEnabled(ldapServer)).isFalse();

    umap.setGroupMappingType(LdapGroupMappingType.STATIC);
    userMappingDAO.update(umap);

    assertThat(ldapService.isGroupSearchEnabled(ldapServer)).isTrue();

    umap.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    umap.setDynamicGroupSearchEnabled(true);
    userMappingDAO.update(umap);

    assertThat(ldapService.isGroupSearchEnabled(ldapServer)).isTrue();

    umap.setDynamicGroupSearchEnabled(false);
    userMappingDAO.update(umap);

    assertThat(ldapService.isGroupSearchEnabled(ldapServer)).isFalse();
  }

  @Test
  public void testIsDynamicGroupSearchDisabled_MultipleDynamicGroupMappingTypes() {
    setupLdapWithDynamicGroupType("test server 1", false);
    setupLdapWithDynamicGroupType("test server 2", true);
    
    assertThat(ldapService.isDynamicGroupSearchDisabled()).isTrue();
  }

  @Test
  public void testIsDynamicGroupSearchDisabled_MixedTypesWithDynamicGroupSearchDisabled() {
    setupLdapWithNonDynamicGroupType("test server 1", LdapGroupMappingType.STATIC);
    setupLdapWithDynamicGroupType("test server 2", false);
    
    assertThat(ldapService.isDynamicGroupSearchDisabled()).isTrue();
  }

  @Test
  public void testIsDynamicGroupSearchDisabled_MixedTypesWithDynamicGroupSearchEnabled() {
    setupLdapWithDynamicGroupType("test server 1", true);
    setupLdapWithNonDynamicGroupType("test server 2", LdapGroupMappingType.STATIC);
    setupLdapWithNonDynamicGroupType("test server 3", LdapGroupMappingType.NONE);
    
    assertThat(ldapService.isDynamicGroupSearchDisabled()).isFalse();
  }

  private void setupLdapWithNonDynamicGroupType(String serverName, LdapGroupMappingType groupMappingType) {
    LdapServer ldapServer = tempEntity.newLdapServer(serverName);
    createLdapConnection(ldapServer);
    LdapUserMapping umap = createUserMapping(ldapServer);
    umap.setGroupMappingType(groupMappingType);
    umap.setDynamicGroupSearchEnabled(false);

    new LdapUserMappingDAO().update(umap);
  }

  private void setupLdapWithDynamicGroupType(String serverName, boolean isDynamicGroupSearchEnabled) {
    LdapServer ldapServer = tempEntity.newLdapServer(serverName);
    createLdapConnection(ldapServer);
    LdapUserMapping umap = createUserMapping(ldapServer);
    umap.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    umap.setDynamicGroupSearchEnabled(isDynamicGroupSearchEnabled);

    new LdapUserMappingDAO().update(umap);
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
  public void testGetUsersByGroup_Dynamic() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createDynamicGroupMapping(ldapServer);

    // Group with one user
    List<LdapUser> users = ldapService.getUsersByGroup(ldapServer, "xb");
    assertThat(users).hasSize(1);
    LdapUser user = users.get(0);
    assertThat(user.getUsername()).isEqualTo("test_user1_1");
    assertThat(user.getRealName()).isEqualTo("Test User 1 1");
    assertThat(user.getEmail()).isEqualTo("test.user1_1@company.com");

    // Group with two users
    users = ldapService.getUsersByGroup(ldapServer, "ab");
    assertThat(users).extracting(LdapUser::getUsername).containsExactlyInAnyOrder("test_user1_1", "test_user2_1",
        "test*user1_1");

    // Group without users
    users = ldapService.getUsersByGroup(ldapServer, "no such group");
    assertThat(users).isEmpty();
  }

  private static int getRandomPort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
