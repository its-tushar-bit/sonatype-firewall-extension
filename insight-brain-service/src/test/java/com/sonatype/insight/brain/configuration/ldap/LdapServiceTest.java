/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import jakarta.inject.Inject;
import javax.naming.AuthenticationException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.NotContextException;

import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapConnectionDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.model.configuration.ldap.LdapAuthenticationMethod;
import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.model.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.model.configuration.ldap.LdapProtocol;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.testing.BrainInjectedTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.test.networking.PortAllocator;

import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.offset;

/**
 * @since 1.7
 */
public class LdapServiceTest
    extends BrainInjectedTest
{
  public final TemporaryFolder tempDir = new TemporaryFolder();

  public final TestLdapServer testLdapServer1 = new TestLdapServer();

  public final TestLdapServer testLdapServer2 = new TestLdapServer();

  public final TestLdapServer testLdapServer3 = new TestLdapServer();

  public final TestLdapServer testLdapServer4 = new TestLdapServer();

  @Inject
  private PasswordHandler passwordHandler;

  @Inject
  private LdapConnectionDAO ldapConnectionDAO;

  @Inject
  private LdapServerDAO ldapServerDAO;

  @Inject
  private LdapUserMappingDAO ldapUserMappingDAO;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(tempDir) //
      .around(testLdapServer1)
      .around(testLdapServer2)
      .around(testLdapServer3)
      .around(testLdapServer4);

  private static final String CONNECTION_ERROR_PATTERN =
      "(?i)(connection (closed|refused|reset)|socket closed|read timed out|cancelled)";

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
  public void testTestLdapConnection() throws Exception {
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

  private void assertCanConnect(LdapConnection ldapConnection) {
    LdapConnectionStatus ldapConnectionStatus =
        ldapService.testLdapConnection(ldapConnection.getServerId(), ldapConnection);
    assertThat(ldapConnectionStatus.getStatus()).isEqualTo(LdapConnectionStatus.Status.OK);
  }

  private void assertCannotConnect(LdapConnection ldapConnection) {
    LdapConnectionStatus ldapConnectionStatus =
        ldapService.testLdapConnection(ldapConnection.getServerId(), ldapConnection);
    assertThat(ldapConnectionStatus.getStatus()).isEqualTo(LdapConnectionStatus.Status.FAILURE);
    assertThat(ldapConnectionStatus.getMessage()).containsPattern(CONNECTION_ERROR_PATTERN);
  }

  @Test
  public void testTestLdapConnection_ValidateLdapServerId() {
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    LdapConnection ldapConnection = tempEntity.newLdapConnection(ldapServer.getId());

    assertThatThrownBy(() -> ldapService.testLdapConnection("fake LDAP server id", ldapConnection))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Inconsistent LDAP server ID.");
  }

  @Test
  public void testTestLdapConnection_EscapedUrl() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    // Search base with space will be escaped with %20.
    ldapConnection.setSearchBase("dc=acme brick,dc=com");

    ldapService.testLdapConnection(ldapConnection.getServerId(), ldapConnection);
  }

  @Test
  public void testTestLdapConnection_Timeout() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("test");

    try (ServerSocket socket = new ServerSocket(0)) {
      long begin = 0;
      long end = 0;

      LdapConnection ldapConnection = new LdapConnection();
      ldapConnection.setServerId(ldapServer.getId());
      ldapConnection.setHostname("localhost");
      ldapConnection.setPort(socket.getLocalPort());

      // test very short timeout
      ldapConnection.setConnectionTimeout(1);
      begin = System.currentTimeMillis();
      LdapConnectionStatus ldapConnectionStatus =
          ldapService.testLdapConnection(ldapConnection.getServerId(), ldapConnection);
      end = System.currentTimeMillis();
      assertThat(ldapConnectionStatus.getStatus()).isEqualTo(LdapConnectionStatus.Status.FAILURE);
      assertThat(ldapConnectionStatus.getMessage()).contains("LDAP response read timed out");
      assertThat(Double.valueOf(end - begin)).isCloseTo(1300, offset(500.0));

      // test slightly longer timeout
      ldapConnection.setConnectionTimeout(3);
      begin = System.currentTimeMillis();
      ldapConnectionStatus = ldapService.testLdapConnection(ldapConnection.getServerId(), ldapConnection);
      end = System.currentTimeMillis();
      assertThat(ldapConnectionStatus.getStatus()).isEqualTo(LdapConnectionStatus.Status.FAILURE);
      assertThat(ldapConnectionStatus.getMessage()).contains("LDAP response read timed out");
      assertThat(Double.valueOf(end - begin)).isCloseTo(3300, offset(500.0));
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
      ldapService.upsertLdapConnection(ldapConnection);

      createUserMapping(ldapServer);

      // force three failures by attempting auth against the dangling socket

      for (int failures = 0; failures < 3; failures++) {
        assertThatThrownBy(() -> ldapService.authenticateUser("user", "pass".toCharArray())).isInstanceOf(
            NamingException.class).hasMessageContaining("read timed out");
      }

      long lastFailure = System.currentTimeMillis();

      // the next requests should be ignored while the retry delay is active

      for (int failures = 0; failures < 3; failures++) {
        assertThatThrownBy(() -> ldapService.authenticateUser("user", "pass".toCharArray())).isInstanceOf(
            NamingException.class).hasMessageContaining("Delaying retry");
      }

      while (System.currentTimeMillis() - lastFailure <= 5000) {
        Thread.sleep(200);
      }

      // the next request should NOT be ignored because the delay has expired

      assertThatThrownBy(() -> ldapService.authenticateUser("user", "pass".toCharArray())).isInstanceOf(
          NamingException.class).hasMessageContaining("read timed out");
    }
  }

  @Test
  public void testAuthenticateUser_SingleExceptionThrownAsIs() throws Exception {
    loadLdapServer(testLdapServer1, "Test Server1");

    assertThatThrownBy(() -> ldapService.authenticateUser("test_user2_2", "test".toCharArray())).isInstanceOf(
        NameNotFoundException.class).hasMessage("LDAP user with username 'test_user2_2' does not exist");
  }

  @Test
  public void testAuthenticateUser_MultiServer_BadPassword() throws Exception {
    loadLdapServer(testLdapServer1, "Test Server1");
    loadLdapServer(testLdapServer2, "Test Server2");

    assertThatThrownBy(() -> ldapService.authenticateUser("test_user2_2", "badPWD".toCharArray()))
        .isInstanceOf(AuthenticationException.class)
        .hasMessage("LDAP Server: Test Server2 -> [LDAP: error code 49 - INVALID_CREDENTIALS: Bind failed:"
            + " ERR_229 Cannot authenticate user uid=test_user2_2,ou=users,dc=company,dc=com]")
        .satisfies(e -> assertThat(e.getSuppressed()).extracting(Throwable::getMessage)
            .containsExactly(
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
      assertThatThrownBy(() -> ldapService.authenticateUser("any-user", "anything".toCharArray()))
          .isInstanceOf(NamingException.class)
          .hasMessageMatching("LDAP Server: Test Server1 -> LDAP response read timed out, " +
              "timeout used:\\p{Zs}?1000\\p{Zs}?ms.;\n")
          .satisfies(e -> assertThat(e.getSuppressed()).extracting(Throwable::getMessage)
              .containsAnyOf(
                  "LDAP response read timed out, timeout used:1000ms.",
                  "LDAP response read timed out, timeout used: 1000 ms.")
              .contains(
                  "LDAP user with username 'any-user' does not exist"));
    }
  }

  private LdapConnection createShortTimeoutLdapConnectionWithoutEmbeddedServer(final String ldapServerName) {
    final LdapServer ldapServer1 = tempEntity.newLdapServer(ldapServerName);
    final LdapConnection ldapConnection1 = createLdapConnection(ldapServer1);
    ldapConnection1.setConnectionTimeout(1);
    ldapConnectionDAO.update(ldapConnection1);
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
        assertThatThrownBy(() -> ldapService.authenticateUser("any-user", "anything".toCharArray()))
            .isInstanceOf(NamingException.class)
            .hasMessageMatching("LDAP Server: Test Server1 -> LDAP response read timed out, " +
                "timeout used:\\p{Zs}?1000\\p{Zs}?ms.;\n"
                + "LDAP Server: Test Server2 -> LDAP response read timed out, timeout used:\\p{Zs}?1000\\p{Zs}?ms.;\n")
            .satisfies(e -> assertThat(e.getSuppressed()).extracting(Throwable::getMessage)
                .containsAnyOf(
                    "LDAP response read timed out, timeout used:1000ms.",
                    "LDAP response read timed out, timeout used: 1000 ms.")
                .contains("LDAP user with username 'any-user' does not exist"));
      }
    }
  }

  @Test
  public void testAuthenticateUser_MultiServer_UnknownUser() throws Exception {
    loadLdapServer(testLdapServer1, "Test Server1");
    loadLdapServer(testLdapServer2, "Test Server2");

    testLdapServer1.stop();
    assertThatThrownBy(() -> ldapService.authenticateUser("test_user4", "anything".toCharArray()))
        .isInstanceOf(NamingException.class)
        .hasMessageContainingAll("LDAP Server: Test Server1 -> ",
            "LDAP Server: Test Server2 -> LDAP user with username 'test_user4' does not exist;\n")
        .satisfies(e -> {
          assertThat(e.getSuppressed()).hasSize(2);
          assertThat(e.getSuppressed()[0].getCause()).hasMessageFindingMatch(CONNECTION_ERROR_PATTERN);
          assertThat(e.getSuppressed()[1]).hasMessage("LDAP user with username 'test_user4' does not exist");
        });
  }

  @Test
  public void testAuthenticateUser_MultiServer_UnexpectedError() throws Exception {
    loadLdapServer(testLdapServer1, "Test Server1");
    loadLdapServer(testLdapServer2, "Test Server2");

    assertThatThrownBy(() -> ldapService.authenticateUser("test_user4", "anything".toCharArray()))
        .isInstanceOf(NameNotFoundException.class)
        .hasMessage("LDAP Server: Test Server1 -> LDAP user with username 'test_user4' does not exist;\n"
            + "LDAP Server: Test Server2 -> LDAP user with username 'test_user4' does not exist;\n")
        .satisfies(e -> assertThat(e.getSuppressed()).extracting(Throwable::getMessage)
            .containsExactly(
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
      ldapService.upsertLdapConnection(ldapConnection);

      createUserMapping(ldapServer);

      // force three failures by attempting the operation against the dangling socket

      for (int failures = 0; failures < 3; failures++) {
        assertThatThrownBy(() -> ldapService.getUserByName("user")).isInstanceOf(NamingException.class)
            .hasMessageContaining("read timed out");
      }

      long lastFailure = System.currentTimeMillis();

      // the next requests should be ignored while the retry delay is active

      for (int failures = 0; failures < 3; failures++) {
        assertThatThrownBy(() -> ldapService.getUserByName("user")).isInstanceOf(NamingException.class)
            .hasMessageContaining("Delaying retry");
      }

      while (System.currentTimeMillis() - lastFailure <= 5000) {
        Thread.sleep(200);
      }

      // the next request should NOT be ignored because the delay has expired

      assertThatThrownBy(() -> ldapService.getUserByName("user")).isInstanceOf(NamingException.class)
          .hasMessageContaining("read timed out");
    }
  }

  @Test
  public void testGetUserByName_SingleExceptionThrownAsIs() throws Exception {
    loadLdapServer(testLdapServer1, "Test Server1");

    assertThatThrownBy(() -> ldapService.getUserByName("test_user2_2"))
        .isInstanceOf(NameNotFoundException.class)
        .hasMessage("LDAP user with username 'test_user2_2' does not exist");
  }

  @Test
  public void testGetUserByName_MultiServer_Timeout_Single() throws Exception {
    final LdapConnection ldapConnection1 = createShortTimeoutLdapConnectionWithoutEmbeddedServer("Test Server1");
    loadLdapServer(testLdapServer2, "Test Server2");

    try (final ServerSocket ignored = new ServerSocket(ldapConnection1.getPort())) {
      assertThatThrownBy(() -> ldapService.getUserByName("any-user")).isInstanceOf(NamingException.class)
          .hasMessageMatching("LDAP Server: Test Server1 -> LDAP response read " +
              "timed out, timeout used:\\p{Zs}?1000\\p{Zs}?ms.;\n")
          .satisfies(e -> assertThat(e.getSuppressed()).extracting(Throwable::getMessage)
              .containsAnyOf(
                  "LDAP response read timed out, timeout used:1000ms.",
                  "LDAP response read timed out, timeout used: 1000 ms.")
              .contains(
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
        assertThatThrownBy(() -> ldapService.getUserByName("any-user")).isInstanceOf(NamingException.class)
            .hasMessageMatching("LDAP Server: Test Server1 -> " +
                "LDAP response read timed out, timeout used:\\p{Zs}?1000\\p{Zs}?ms.;\n"
                + "LDAP Server: Test Server2 -> LDAP response read timed out, timeout used:\\p{Zs}?1000\\p{Zs}?ms.;\n")
            .satisfies(e -> assertThat(e.getSuppressed()).extracting(Throwable::getMessage)
                .containsAnyOf(
                    "LDAP response read timed out, timeout used:1000ms.",
                    "LDAP response read timed out, timeout used: 1000 ms.")
                .contains("LDAP user with username 'any-user' does not exist"));
      }
    }
  }

  @Test
  public void testGetUserByName_MultiServer_UnknownUser() throws Exception {
    loadLdapServer(testLdapServer1, "Test Server1");
    loadLdapServer(testLdapServer2, "Test Server2");

    final int ldapServer1Port = testLdapServer1.getPort();
    testLdapServer1.stop();
    assertThatThrownBy(() -> ldapService.getUserByName("test_user4"))
        .isInstanceOf(NamingException.class)
        .hasMessage("LDAP Server: Test Server1 -> localhost:" + ldapServer1Port
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

    assertThatThrownBy(() -> ldapService.getUserByName("test_user4")).isInstanceOf(NameNotFoundException.class)
        .hasMessage("LDAP Server: Test Server1 -> LDAP user with username 'test_user4' does not exist;\n"
            + "LDAP Server: Test Server2 -> LDAP user with username 'test_user4' does not exist;\n")
        .satisfies(e -> assertThat(e.getSuppressed()).extracting(Throwable::getMessage)
            .containsExactly(
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
  public void testTestLdapConnection_BadSearchBase() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    ldapConnection.setSearchBase("!@£$%^&*()");

    LdapConnectionStatus ldapConnectionStatus =
        ldapService.testLdapConnection(ldapConnection.getServerId(), ldapConnection);
    assertThat(ldapConnectionStatus.getStatus()).isEqualTo(LdapConnectionStatus.Status.FAILURE);
    assertThat(ldapConnectionStatus.getMessage()).contains("Invalid name");
  }

  private void setSearchBase(LdapConnection ldapConnection, String searchBase) {
    ldapConnection.setSearchBase(searchBase);
    ldapService.upsertLdapConnection(ldapConnection);
  }

  @Test
  public void testTestLdapConnection_InvalidHostname() throws Exception {
    String badHostname = "garbage.localhost.litter";

    LdapServer ldapServer = tempEntity.newLdapServer("test");
    LdapConnection ldapConnection = newInMemoryLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    ldapConnection.setHostname(badHostname);

    LdapConnectionStatus ldapConnectionStatus =
        ldapService.testLdapConnection(ldapConnection.getServerId(), ldapConnection);

    assertThat(ldapConnectionStatus.getStatus()).isEqualTo(LdapConnectionStatus.Status.FAILURE);
    assertThat(ldapConnectionStatus.getMessage()).contains(badHostname)
        .containsPattern("(UnknownHost|Communication)Exception");
  }

  @Test
  public void testTestLdapConnection_InvalidUser() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    LdapConnection ldapConnection = newInMemoryLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    ldapConnection.setAuthenticationMethod(LdapAuthenticationMethod.SIMPLE);
    String systemUserDN = "litter." + testLdapServer1.getSystemUserDN() + ".garbage";
    ldapConnection.setSystemUsername(systemUserDN);
    ldapConnection.setSystemPassword(testLdapServer1.getSystemUserPassword());

    LdapConnectionStatus ldapConnectionStatus =
        ldapService.testLdapConnection(ldapConnection.getServerId(), ldapConnection);

    assertThat(ldapConnectionStatus.getStatus()).isEqualTo(LdapConnectionStatus.Status.FAILURE);
    assertThat(ldapConnectionStatus.getMessage()).contains("Invalid authentication");
  }

  @Test
  public void testTestLdapConnection_InvalidPassword() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    LdapConnection ldapConnection = newInMemoryLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    ldapConnection.setAuthenticationMethod(LdapAuthenticationMethod.SIMPLE);
    ldapConnection.setSystemUsername(testLdapServer1.getSystemUserDN());
    ldapConnection.setSystemPassword("garbage.litter".toCharArray());

    LdapConnectionStatus ldapConnectionStatus =
        ldapService.testLdapConnection(ldapConnection.getServerId(), ldapConnection);

    assertThat(ldapConnectionStatus.getStatus()).isEqualTo(LdapConnectionStatus.Status.FAILURE);
    assertThat(ldapConnectionStatus.getMessage()).contains("Cannot authenticate user");
  }

  @Test
  public void testTestLdapConnection_InvalidSaslRealm() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    LdapConnection ldapConnection = newInMemoryLdapConnection(ldapServer);
    testLdapServer1.setAuthenticationSasl(SupportedSaslMechanisms.DIGEST_MD5);
    startLdapServer(testLdapServer1, ldapConnection);

    ldapConnection.setAuthenticationMethod(LdapAuthenticationMethod.DIGESTMD5);
    ldapConnection.setSystemUsername(testLdapServer1.getSystemUser());
    ldapConnection.setSystemPassword(testLdapServer1.getSystemUserPassword());
    ldapConnection.setSaslRealm("invalidrealm");

    LdapConnectionStatus ldapConnectionStatus =
        ldapService.testLdapConnection(ldapConnection.getServerId(), ldapConnection);

    assertThat(ldapConnectionStatus.getStatus()).isEqualTo(LdapConnectionStatus.Status.FAILURE);
    assertThat(ldapConnectionStatus.getMessage()).contains("Nonexistent realm: invalidrealm");
  }

  @Test
  public void testTestLdapConnection_AuthenticationMethod_CRAM_MD5() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    LdapConnection ldapConnection = newInMemoryLdapConnection(ldapServer);
    testLdapServer1.setAuthenticationSasl(SupportedSaslMechanisms.CRAM_MD5);
    startLdapServer(testLdapServer1, ldapConnection);

    ldapConnection.setAuthenticationMethod(LdapAuthenticationMethod.CRAMMD5);
    ldapConnection.setSystemUsername(testLdapServer1.getSystemUser());
    ldapConnection.setSystemPassword(testLdapServer1.getSystemUserPassword());

    LdapConnectionStatus ldapConnectionStatus =
        ldapService.testLdapConnection(ldapConnection.getServerId(), ldapConnection);

    assertThat(ldapConnectionStatus.getStatus()).as(ldapConnectionStatus.getMessage())
        .isEqualTo(LdapConnectionStatus.Status.OK);
  }

  @Test
  public void testTestLdapConnection_AuthenticationMethod_DIGESTMD5() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    LdapConnection ldapConnection = newInMemoryLdapConnection(ldapServer);
    testLdapServer1.setAuthenticationSasl(SupportedSaslMechanisms.DIGEST_MD5);
    startLdapServer(testLdapServer1, ldapConnection);

    ldapConnection.setAuthenticationMethod(LdapAuthenticationMethod.DIGESTMD5);
    ldapConnection.setSystemUsername(testLdapServer1.getSystemUser());
    ldapConnection.setSystemPassword(testLdapServer1.getSystemUserPassword());

    LdapConnectionStatus ldapConnectionStatus =
        ldapService.testLdapConnection(ldapConnection.getServerId(), ldapConnection);

    assertThat(ldapConnectionStatus.getStatus()).as(ldapConnectionStatus.getMessage())
        .isEqualTo(LdapConnectionStatus.Status.OK);
  }

  @Test
  public void testTestLdapConnection_AuthenticationMethod_SIMPLE() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    LdapConnection ldapConnection = newInMemoryLdapConnection(ldapServer);
    testLdapServer1.setAuthenticationSimple();
    startLdapServer(testLdapServer1, ldapConnection);

    ldapConnection.setAuthenticationMethod(LdapAuthenticationMethod.SIMPLE);
    ldapConnection.setSystemUsername(testLdapServer1.getSystemUserDN());
    ldapConnection.setSystemPassword(testLdapServer1.getSystemUserPassword());

    LdapConnectionStatus ldapConnectionStatus =
        ldapService.testLdapConnection(ldapConnection.getServerId(), ldapConnection);

    assertThat(ldapConnectionStatus.getStatus()).as(ldapConnectionStatus.getMessage())
        .isEqualTo(LdapConnectionStatus.Status.OK);
  }

  @Test
  public void testTestLdapConnection_AnonymousConnection() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    LdapConnection ldapConnection = newInMemoryLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    LdapConnectionStatus ldapConnectionStatus =
        ldapService.testLdapConnection(ldapConnection.getServerId(), ldapConnection);

    assertThat(ldapConnectionStatus.getStatus()).as(ldapConnectionStatus.getMessage())
        .isEqualTo(LdapConnectionStatus.Status.OK);
  }

  @Test
  public void testTestLdapConnection_FakePasswordSameHostAndSamePort() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    LdapConnection ldapConnection = tempEntity.newLdapConnection(ldapServer.getId(), PortAllocator.nextFreePort(),
        LdapAuthenticationMethod.SIMPLE, passwordHandler.encryptPassword(testLdapServer1.getSystemUserPassword()));
    ldapConnection.setSearchBase(null);
    ldapConnection.setSystemUsername(testLdapServer1.getSystemUserDN());
    ldapConnection.setSystemPassword(LdapService.FAKE_PASSWORD);
    testLdapServer1.setAuthenticationSimple();
    startLdapServer(testLdapServer1, ldapConnection);

    LdapConnectionStatus ldapConnectionStatus = ldapService.testLdapConnection(ldapServer.getId(), ldapConnection);

    assertThat(ldapConnectionStatus.getStatus()).as(ldapConnectionStatus.getMessage())
        .isEqualTo(LdapConnectionStatus.Status.OK);
  }

  @Test
  public void testTestLdapConnection_GivenPasswordDifferentHostAndSamePort() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    LdapConnection ldapConnection = tempEntity.newLdapConnection(ldapServer.getId(), PortAllocator.nextFreePort(),
        LdapAuthenticationMethod.SIMPLE, passwordHandler.encryptPassword(testLdapServer1.getSystemUserPassword()));
    ldapConnection.setSearchBase(null);
    ldapConnection.setSystemUsername(testLdapServer1.getSystemUserDN());
    ldapConnection.setSystemPassword(testLdapServer1.getSystemUserPassword());
    testLdapServer1.setAuthenticationSimple();
    startLdapServer(testLdapServer1, ldapConnection);
    ldapConnection.setHostname(ldapConnection.getHostname() + "different");

    LdapConnectionStatus ldapConnectionStatus = ldapService.testLdapConnection(ldapServer.getId(), ldapConnection);

    // Bogus host so we expect a failure
    assertThat(ldapConnectionStatus.getStatus()).as(ldapConnectionStatus.getMessage())
        .isEqualTo(LdapConnectionStatus.Status.FAILURE);
  }

  @Test
  public void testTestLdapConnection_GivenPasswordSameHostAndDifferentPort() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    LdapConnection ldapConnection = tempEntity.newLdapConnection(ldapServer.getId(), PortAllocator.nextFreePort(),
        LdapAuthenticationMethod.SIMPLE, passwordHandler.encryptPassword(testLdapServer1.getSystemUserPassword()));
    ldapConnection.setSearchBase(null);
    ldapConnection.setSystemUsername(testLdapServer1.getSystemUserDN());
    ldapConnection.setSystemPassword(testLdapServer1.getSystemUserPassword());
    testLdapServer1.setAuthenticationSimple();
    startLdapServer(testLdapServer1, ldapConnection);
    ldapConnection.setPort(ldapConnection.getPort() + 10);

    LdapConnectionStatus ldapConnectionStatus = ldapService.testLdapConnection(ldapServer.getId(), ldapConnection);

    // Bogus port so we expect a failure
    assertThat(ldapConnectionStatus.getStatus()).as(ldapConnectionStatus.getMessage())
        .isEqualTo(LdapConnectionStatus.Status.FAILURE);
  }

  @Test
  public void testTestLdapConnection_FakePasswordDifferentHostAndSamePort() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    LdapConnection ldapConnection = tempEntity.newLdapConnection(ldapServer.getId(), PortAllocator.nextFreePort(),
        LdapAuthenticationMethod.SIMPLE, passwordHandler.encryptPassword(testLdapServer1.getSystemUserPassword()));
    ldapConnection.setSearchBase(null);
    ldapConnection.setSystemUsername(testLdapServer1.getSystemUserDN());
    ldapConnection.setSystemPassword(LdapService.FAKE_PASSWORD);
    testLdapServer1.setAuthenticationSimple();
    startLdapServer(testLdapServer1, ldapConnection);
    ldapConnection.setHostname(ldapConnection.getHostname() + "different");

    assertThatThrownBy(() -> ldapService.testLdapConnection(ldapServer.getId(), ldapConnection))
        .isInstanceOf(BadRequestException.class)
        .hasMessage(
            "The password must be given when updating the hostname or port for a connection that uses authentication.");
  }

  @Test
  public void testTestLdapConnection_FakePasswordSameHostAndDifferentPort() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    LdapConnection ldapConnection = tempEntity.newLdapConnection(ldapServer.getId(), PortAllocator.nextFreePort(),
        LdapAuthenticationMethod.SIMPLE, passwordHandler.encryptPassword(testLdapServer1.getSystemUserPassword()));
    ldapConnection.setSearchBase(null);
    ldapConnection.setSystemUsername(testLdapServer1.getSystemUserDN());
    ldapConnection.setSystemPassword(LdapService.FAKE_PASSWORD);
    testLdapServer1.setAuthenticationSimple();
    startLdapServer(testLdapServer1, ldapConnection);
    ldapConnection.setPort(ldapConnection.getPort() + 10);

    assertThatThrownBy(() -> ldapService.testLdapConnection(ldapServer.getId(), ldapConnection))
        .isInstanceOf(BadRequestException.class)
        .hasMessage(
            "The password must be given when updating the hostname or port for a connection that uses authentication.");
  }

  @Test
  public void testTestLdapUserMapping() throws Exception {
    LdapServer ldapServer1 = tempEntity.newLdapServer("Test Server1");
    LdapConnection ldapConnection1 = createLdapConnection(ldapServer1);
    startLdapServer(testLdapServer1, ldapConnection1);
    LdapUserMapping umap1 = createUserMapping(ldapServer1);

    LdapServer ldapServer2 = tempEntity.newLdapServer("Test Server2");
    LdapConnection ldapConnection2 = createLdapConnection(ldapServer2);
    startLdapServer(testLdapServer2, ldapConnection2);
    LdapUserMapping umap2 = createUserMapping(ldapServer2);

    List<LdapUser> users1 = ldapService.testLdapUserMapping(ldapServer1.getId(), umap1, -1);
    assertUserMapping(users1, "1");
    List<LdapUser> users2 = ldapService.testLdapUserMapping(ldapServer2.getId(), umap2, -1);
    assertUserMapping(users2, "2");
  }

  @Test
  public void testTestLdapUserMapping_ValidateLdapServerId() {
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    LdapUserMapping ldapUserMapping = createUserMapping(ldapServer);

    assertThatThrownBy(() -> ldapService.testLdapUserMapping("fake LDAP server id", ldapUserMapping, -1))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Inconsistent LDAP server ID.");
  }

  @Test
  public void testUpdatePriority() {
    LdapServer ldapServer1 = tempEntity.newLdapServer("server1");
    LdapServer ldapServer2 = tempEntity.newLdapServer("server2");
    ldapService.updatePriority(Arrays.asList(ldapServer2.getId(), ldapServer1.getId()));
    assertThat(ldapServerDAO.getById(ldapServer2.getId()).getPriority()).isEqualTo(1);
    assertThat(ldapServerDAO.getById(ldapServer1.getId()).getPriority()).isEqualTo(2);
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
  public void testTestLdapUserMapping_DynamicGroupMapping() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    LdapUserMapping ldapUserMapping = createUserMapping(ldapServer);
    ldapUserMapping.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    ldapUserMapping.setUserMemberOfGroupAttribute("departmentNumber");

    List<LdapUser> users = ldapService.testLdapUserMapping(ldapServer.getId(), ldapUserMapping, -1);
    assertThat(users).hasSize(3);

    Collections.sort(users); // sorts on username

    assertThat(users.get(0).getMembership()).containsExactlyInAnyOrder("ab", "bc", "bx");
    assertThat(users.get(1).getMembership()).containsExactlyInAnyOrder("ab", "abc", "xb");
    assertThat(users.get(2).getMembership()).containsExactlyInAnyOrder("ab", "bc", "bx");
  }

  @Test
  public void testTestLdapUserMapping_StaticGroupMapping() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    LdapUserMapping ldapUserMapping = createUserMapping(ldapServer);
    ldapUserMapping.setGroupMappingType(LdapGroupMappingType.STATIC);
    ldapUserMapping.setGroupObjectClass("groupOfUniqueNames");
    ldapUserMapping.setGroupMemberAttribute("uniqueMember");
    ldapUserMapping.setGroupMemberFormat("${dn}");

    List<LdapUser> users = ldapService.testLdapUserMapping(ldapServer.getId(), ldapUserMapping, -1);
    assertThat(users).hasSize(3);

    Collections.sort(users); // sorts on username

    assertThat(users.get(0).getMembership()).isEmpty();
    assertThat(users.get(1).getMembership()).containsExactlyInAnyOrder("Alpha");
    assertThat(users.get(2).getMembership()).containsExactlyInAnyOrder("Beta");

    ldapUserMapping.setGroupMappingType(LdapGroupMappingType.STATIC);
    ldapUserMapping.setGroupObjectClass("groupOfNames");
    ldapUserMapping.setGroupMemberAttribute("member");
    ldapUserMapping.setGroupMemberFormat("uid=${username}");

    users = ldapService.testLdapUserMapping(ldapServer.getId(), ldapUserMapping, -1);
    assertThat(users).hasSize(3);

    Collections.sort(users); // sorts on username

    assertThat(users.get(0).getMembership()).isEmpty();
    assertThat(users.get(1).getMembership()).containsExactlyInAnyOrder("Gamma", "Theta", "Omega");
    assertThat(users.get(2).getMembership()).containsExactlyInAnyOrder("Theta");
  }

  @Test
  public void testTestLdapUserMapping_LdapConnectionNotConfigured() throws Exception {
    testLdapServer1.setPort(PortAllocator.nextFreePort());
    testLdapServer1.start();

    LdapServer ldapServer = tempEntity.newLdapServer("test");
    LdapUserMapping ldapUserMapping = createUserMapping(ldapServer);

    assertThatThrownBy(() -> ldapService.testLdapUserMapping(ldapServer.getId(), ldapUserMapping, -1))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("LDAP connection is not configured");
  }

  @Test
  public void testTestUserLogin() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    LdapUserMapping ldapUserMapping = newInMemoryLdapUserMapping(ldapServer);

    ldapUserMapping.setUserPasswordAttribute(null); // AUTH-via-BIND

    assertCannotLogin(ldapUserMapping, "test_user1_1", "badGuess");
    assertCanLogin(ldapUserMapping, "test_user1_1", "far2simple");

    ldapUserMapping.setUserPasswordAttribute("userPassword"); // AUTH-via-ATTRIBUTE

    assertCannotLogin(ldapUserMapping, "test_user1_1", "badGuess");
    assertCanLogin(ldapUserMapping, "test_user1_1", "far2simple");
  }

  @Test
  public void testTestUserLogin_ValidateLdapServerId() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    LdapUserMapping ldapUserMapping = newInMemoryLdapUserMapping(ldapServer);

    assertThatThrownBy(() -> ldapService.testUserLogin("fake LDAP server id", ldapUserMapping, "user",
        "pass".toCharArray())).isInstanceOf(BadRequestException.class)
            .hasMessage("Inconsistent LDAP server ID.");
  }

  @Test
  public void testTestUserLogin_MultiServer() throws Exception {
    LdapServer ldapServer1 = tempEntity.newLdapServer("Test Server1");
    LdapConnection ldapConnection1 = createLdapConnection(ldapServer1);
    startLdapServer(testLdapServer1, ldapConnection1);
    LdapUserMapping umap1 = newInMemoryLdapUserMapping(ldapServer1);

    LdapServer ldapServer2 = tempEntity.newLdapServer("Test Server2");
    LdapConnection ldapConnection2 = createLdapConnection(ldapServer2);
    startLdapServer(testLdapServer2, ldapConnection2);
    LdapUserMapping umap2 = newInMemoryLdapUserMapping(ldapServer2);

    assertCanLogin(umap1, "test_user1_1", "far2simple");
    assertCanLogin(umap2, "test_user1_2", "far2simple");

    testLdapServer1.stop();
    assertCannotLoginWhenServerIsDown(umap1, "test_user1_1", "far2simple");
    assertCanLogin(umap2, "test_user1_2", "far2simple");

    testLdapServer2.stop();
    assertCannotLoginWhenServerIsDown(umap1, "test_user1_1", "far2simple");
    assertCannotLoginWhenServerIsDown(umap2, "test_user1_2", "far2simple");
  }

  /**
   * CLM-9430, sanity check the classpath of the server contains a recent version of commons-codec as needed by our
   * LDAP client to support passwords hashed using crypt.
   */
  @Test
  public void testTestUserLogin_UserPasswordAttributeUsingCrypt() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    testLdapServer1.setLdifResourceName("/" + getClass().getSimpleName() + "/ldap_user_encrypted_password.ldif");
    startLdapServer(testLdapServer1, ldapConnection);

    LdapUserMapping ldapUserMapping = tempEntity.newLdapUserMapping(ldapServer.getId());
    ldapUserMapping.setUserPasswordAttribute("userPassword");
    ldapUserMappingDAO.update(ldapUserMapping);

    LdapConnectionStatus ldapConnectionStatus =
        ldapService.testUserLogin(ldapServer.getId(), ldapUserMapping, "cryptuser", "brianf123".toCharArray());
    assertThat(ldapConnectionStatus.getStatus()).isEqualTo(LdapConnectionStatus.Status.OK);
  }

  private void assertCanLogin(LdapUserMapping ldapUserMapping, String username, String password) {
    LdapConnectionStatus ldapConnectionStatus =
        ldapService.testUserLogin(ldapUserMapping.getServerId(), ldapUserMapping, username, password.toCharArray());
    assertThat(ldapConnectionStatus.getStatus()).isEqualTo(LdapConnectionStatus.Status.OK);
  }

  private void assertCannotLogin(LdapUserMapping ldapUserMapping, String username, String password) {
    LdapConnectionStatus ldapConnectionStatus =
        ldapService.testUserLogin(ldapUserMapping.getServerId(), ldapUserMapping, username, password.toCharArray());
    assertThat(ldapConnectionStatus.getStatus()).isEqualTo(LdapConnectionStatus.Status.FAILURE);
    assertThat(ldapConnectionStatus.getMessage()).contains("javax.naming.AuthenticationException");
  }

  private void assertCannotLoginWhenServerIsDown(LdapUserMapping ldapUserMapping, String username, String password) {
    LdapConnectionStatus ldapConnectionStatus =
        ldapService.testUserLogin(ldapUserMapping.getServerId(), ldapUserMapping, username, password.toCharArray());
    assertThat(ldapConnectionStatus.getStatus()).isEqualTo(LdapConnectionStatus.Status.FAILURE);
    assertThat(ldapConnectionStatus.getMessage()).containsPattern(CONNECTION_ERROR_PATTERN);
  }

  @Test
  public void testAuthenticateUser_RejectEmptyPasswordAsPerRfc4513Section5_1_2() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);
    createUserMapping(ldapServer);

    assertThatThrownBy(() -> ldapService.authenticateUser("test_user", "".toCharArray()))
        .isInstanceOf(AuthenticationException.class);
  }

  @Test
  public void testTestUserLogin_RejectEmptyPasswordAsPerRfc4513Section5_1_2() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);
    LdapUserMapping ldapUserMapping = createUserMapping(ldapServer);

    LdapConnectionStatus ldapConnectionStatus =
        ldapService.testUserLogin(ldapUserMapping.getServerId(), ldapUserMapping, "test_user", "".toCharArray());
    assertThat(ldapConnectionStatus.getStatus()).isEqualTo(LdapConnectionStatus.Status.FAILURE);
    assertThat(ldapConnectionStatus.getMessage()).contains("javax.naming.AuthenticationException");
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
        new String[]{"test_user1_1", "test_user2_1", "test_user1_2"});
    assertThat(users1).extracting(LdapUser::getUsername).containsExactlyInAnyOrder("test_user1_1", "test_user2_1");
    assertThat(users1).extracting(LdapUser::getServerId)
        .containsExactlyInAnyOrder(ldapServer1.getId(),
            ldapServer1.getId());

    users1 = ldapService.getUsersByName(ldapServer1, new String[]{"foo"});
    assertThat(users1).isEmpty();

    List<LdapUser> users2 = ldapService.getUsersByName(ldapServer2,
        new String[]{"test_user1_1", "test_user2_1", "test_user1_2"});
    assertThat(users2).extracting(LdapUser::getUsername).containsExactly("test_user1_2");
    assertThat(users2).extracting(LdapUser::getServerId).containsExactly(ldapServer2.getId());
  }

  @Test
  public void testGetUsersByName_wildcardMatchingNotExpected() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    createUserMapping(ldapServer);

    List<LdapUser> users = ldapService.getUsersByName(ldapServer, new String[]{"test_user*"});
    assertThat(users).isEmpty();
  }

  @Test
  public void testGetGroupsByName_Static() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    LdapUserMapping ldapUserMapping = createUserMapping(ldapServer);
    ldapUserMapping.setGroupMappingType(LdapGroupMappingType.STATIC);
    ldapUserMapping.setGroupObjectClass("groupOfNames");
    ldapUserMapping.setGroupMemberAttribute("member");
    ldapUserMapping.setGroupMemberFormat("uid=${username}");
    ldapUserMappingDAO.update(ldapUserMapping);

    List<LdapGroup> groups = ldapService.getGroupsByName(ldapServer, new String[]{"Gamma", "Theta"});
    assertThat(groups).hasSize(2);

    groups = ldapService.getGroupsByName(ldapServer, new String[]{"foo"});
    assertThat(groups).isEmpty();
  }

  @Test
  public void testGetGroupsByName_Static_wildcardMatchingNotExpected() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    LdapUserMapping ldapUserMapping = createUserMapping(ldapServer);
    ldapUserMapping.setGroupMappingType(LdapGroupMappingType.STATIC);
    ldapUserMapping.setGroupObjectClass("groupOfNames");
    ldapUserMapping.setGroupMemberAttribute("member");
    ldapUserMapping.setGroupMemberFormat("uid=${username}");
    ldapUserMappingDAO.update(ldapUserMapping);

    List<LdapGroup> groups = ldapService.getGroupsByName(ldapServer, new String[]{"*ta"});
    assertThat(groups).isEmpty();
  }

  @Test
  public void testGetGroupsByName_Dynamic() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    LdapUserMapping ldapUserMapping = createUserMapping(ldapServer);
    ldapUserMapping.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    ldapUserMapping.setUserMemberOfGroupAttribute("departmentNumber");
    ldapUserMappingDAO.update(ldapUserMapping);

    List<LdapGroup> groups = ldapService.getGroupsByName(ldapServer, new String[]{"ab", "abc", "bc"});
    assertThat(groups).hasSize(3);

    groups = ldapService.getGroupsByName(ldapServer, new String[]{"foo"});
    assertThat(groups).isEmpty();
  }

  @Test
  public void testGetGroupsByName_Dynamic_wildcardMatchingNotExpected() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    LdapUserMapping ldapUserMapping = createUserMapping(ldapServer);
    ldapUserMapping.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    ldapUserMapping.setUserMemberOfGroupAttribute("departmentNumber");
    ldapUserMappingDAO.update(ldapUserMapping);

    List<LdapGroup> groups = ldapService.getGroupsByName(ldapServer, new String[]{"ab*"});
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
    assertThat(users).extracting(LdapUser::getUsername)
        .containsExactlyInAnyOrder("test_user1_1", "test_user2_1",
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
    assertThat(users1).extracting(LdapUser::getUsername)
        .containsExactlyInAnyOrder("test_user1_1", "test_user2_1",
            "test*user1_1");
    assertThat(users1).extracting(LdapUser::getServerId)
        .containsExactlyInAnyOrder(ldapServer1.getId(),
            ldapServer1.getId(), ldapServer1.getId());

    List<LdapUser> users2 = ldapService.findUsersByName(ldapServer2, "test*" /* name */, 100);
    assertThat(users2).extracting(LdapUser::getUsername)
        .containsExactlyInAnyOrder("test_user1_2", "test_user2_2",
            "test*user1_2");
    assertThat(users2).extracting(LdapUser::getServerId)
        .containsExactlyInAnyOrder(ldapServer2.getId(),
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
    assertThat(groups).extracting(LdapGroup::getGroupname)
        .containsExactlyInAnyOrder("Gamma", "Omega", "Theta",
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

    LdapUserMapping ldapUserMapping = createUserMapping(ldapServer);
    ldapUserMapping.setGroupMappingType(LdapGroupMappingType.STATIC);
    ldapUserMapping.setGroupObjectClass("groupOfNames");
    ldapUserMapping.setGroupMemberAttribute("member");
    ldapUserMapping.setGroupMemberFormat("${dn}");
    ldapUserMappingDAO.update(ldapUserMapping);

    List<LdapUser> users = ldapService.getUsersByGroup(ldapServer, "Epsilon");
    assertThat(users).extracting(LdapUser::getUsername).containsExactlyInAnyOrder("test_user1", "test_user2");
  }

  @Test
  public void testGetUsersByGroup_Static_OmitsLdapMatchingRuleInChain() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer4, ldapConnection);

    LdapUserMapping ldapUserMapping = createUserMapping(ldapServer);
    ldapUserMapping.setGroupMappingType(LdapGroupMappingType.STATIC);
    ldapUserMapping.setGroupObjectClass("groupOfNames");
    ldapUserMapping.setGroupMemberAttribute("member:" + LdapQuery.LDAP_MATCHING_RULE_IN_CHAIN + ":");
    ldapUserMapping.setGroupMemberFormat("${dn}");
    ldapUserMappingDAO.update(ldapUserMapping);

    List<LdapUser> users = ldapService.getUsersByGroup(ldapServer, "Epsilon");
    assertThat(users).extracting(LdapUser::getUsername).containsExactlyInAnyOrder("test_user1", "test_user2");
  }

  @Test
  public void testGetUsersByGroup_Static_NoMembers() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer4, ldapConnection);

    LdapUserMapping ldapUserMapping = createUserMapping(ldapServer);
    ldapUserMapping.setGroupMappingType(LdapGroupMappingType.STATIC);
    ldapUserMapping.setGroupObjectClass("groupOfNames");
    ldapUserMapping.setGroupMemberAttribute("doesNotExist");
    ldapUserMapping.setGroupMemberFormat("${dn}");
    ldapUserMappingDAO.update(ldapUserMapping);

    List<LdapUser> users = ldapService.getUsersByGroup(ldapServer, "Epsilon");
    assertThat(users).extracting(LdapUser::getUsername).isEmpty();
  }

  @Test
  public void testGetUsersByGroup_Static_UsernameExpression() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    LdapUserMapping ldapUserMapping = createUserMapping(ldapServer);
    ldapUserMapping.setGroupMappingType(LdapGroupMappingType.STATIC);
    ldapUserMapping.setGroupObjectClass("groupOfNames");
    ldapUserMapping.setGroupMemberAttribute("member");
    ldapUserMapping.setGroupMemberFormat("uid=qwerty${username}zxcvbn,${dn}yuiop${username}");
    ldapUserMappingDAO.update(ldapUserMapping);

    List<LdapUser> users = ldapService.getUsersByGroup(ldapServer, "Delta");
    assertThat(users).hasSize(2);
  }

  @Test
  public void testGetUsersByGroup_Static_DnExpression() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    LdapUserMapping ldapUserMapping = createUserMapping(ldapServer);
    ldapUserMapping.setGroupMappingType(LdapGroupMappingType.STATIC);
    ldapUserMapping.setGroupObjectClass("groupOfNames");
    ldapUserMapping.setGroupMemberAttribute("member");
    ldapUserMapping.setGroupMemberFormat("dc=company,${dn},dc=com,${dn}");
    ldapUserMappingDAO.update(ldapUserMapping);

    List<LdapUser> users = ldapService.getUsersByGroup(ldapServer, "Lambda");
    assertThat(users).hasSize(2);
  }

  @Test
  public void testGetUsersByGroup_Static_wildcardMatchingNotExpected() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    LdapUserMapping ldapUserMapping = createUserMapping(ldapServer);
    ldapUserMapping.setGroupMappingType(LdapGroupMappingType.STATIC);
    ldapUserMapping.setGroupObjectClass("groupOfNames");
    ldapUserMapping.setGroupMemberAttribute("member");
    ldapUserMapping.setGroupMemberFormat("uid=qwerty${username}zxcvbn,${dn}yuiop${username}");
    ldapUserMappingDAO.update(ldapUserMapping);

    List<LdapUser> users = ldapService.getUsersByGroup(ldapServer, "Delt*");
    assertThat(users).isEmpty();
  }

  @Test
  public void testGetUsersByGroup_Dynamic_wildcardMatchingNotExpected() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    startLdapServer(testLdapServer1, ldapConnection);

    LdapUserMapping ldapUserMapping = createUserMapping(ldapServer);
    ldapUserMapping.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    ldapUserMapping.setUserMemberOfGroupAttribute("departmentNumber");
    ldapUserMappingDAO.update(ldapUserMapping);

    List<LdapUser> users = ldapService.getUsersByGroup(ldapServer, "a*");
    assertThat(users).isEmpty();
  }

  private void createStaticGroupMapping(LdapServer ldapServer) {
    LdapUserMapping ldapUserMapping = createUserMapping(ldapServer);
    ldapUserMapping.setGroupMappingType(LdapGroupMappingType.STATIC);
    ldapUserMapping.setGroupObjectClass("groupOfNames");
    ldapUserMapping.setGroupMemberAttribute("member");
    ldapUserMapping.setGroupMemberFormat("uid=${username}");
    ldapUserMappingDAO.update(ldapUserMapping);
  }

  private void createDynamicGroupMapping(LdapServer ldapServer) {
    LdapUserMapping ldapUserMapping = createUserMapping(ldapServer);
    ldapUserMapping.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    ldapUserMapping.setUserMemberOfGroupAttribute("departmentNumber");
    ldapUserMappingDAO.update(ldapUserMapping);
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
  public void testIsLdapEnabled() {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    assertThat(ldapService.isLdapEnabled(ldapServer)).isFalse();

    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    ldapConnection.setHostname("localhost");
    ldapService.upsertLdapConnection(ldapConnection);
    assertThat(ldapService.isLdapEnabled(ldapServer)).isFalse();

    createUserMapping(ldapServer);

    assertThat(ldapService.isLdapEnabled(ldapServer)).isTrue();
  }

  @Test
  public void testIsGroupSearchEnabled() {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    createLdapConnection(ldapServer);

    assertThat(ldapService.isGroupSearchEnabled(ldapServer)).isFalse();

    LdapUserMapping ldapUserMapping = createUserMapping(ldapServer);
    ldapUserMapping.setGroupMappingType(LdapGroupMappingType.NONE);

    ldapUserMappingDAO.update(ldapUserMapping);

    assertThat(ldapService.isGroupSearchEnabled(ldapServer)).isFalse();

    ldapUserMapping.setGroupMappingType(LdapGroupMappingType.STATIC);
    ldapUserMappingDAO.update(ldapUserMapping);

    assertThat(ldapService.isGroupSearchEnabled(ldapServer)).isTrue();

    ldapUserMapping.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    ldapUserMapping.setDynamicGroupSearchEnabled(true);
    ldapUserMappingDAO.update(ldapUserMapping);

    assertThat(ldapService.isGroupSearchEnabled(ldapServer)).isTrue();

    ldapUserMapping.setDynamicGroupSearchEnabled(false);
    ldapUserMappingDAO.update(ldapUserMapping);

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
    LdapUserMapping ldapUserMapping = createUserMapping(ldapServer);
    ldapUserMapping.setGroupMappingType(groupMappingType);
    ldapUserMapping.setDynamicGroupSearchEnabled(false);

    ldapUserMappingDAO.update(ldapUserMapping);
  }

  private void setupLdapWithDynamicGroupType(String serverName, boolean isDynamicGroupSearchEnabled) {
    LdapServer ldapServer = tempEntity.newLdapServer(serverName);
    createLdapConnection(ldapServer);
    LdapUserMapping ldapUserMapping = createUserMapping(ldapServer);
    ldapUserMapping.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    ldapUserMapping.setDynamicGroupSearchEnabled(isDynamicGroupSearchEnabled);

    ldapUserMappingDAO.update(ldapUserMapping);
  }

  private void startLdapServer(TestLdapServer testLdapServer, LdapConnection ldapConnection) throws Exception {
    testLdapServer.setPort(ldapConnection.getPort());
    testLdapServer.start();
  }

  private LdapConnection createLdapConnection(LdapServer ldapServer) {
    return tempEntity.newLdapConnection(ldapServer.getId(), PortAllocator.nextFreePort());
  }

  private LdapUserMapping newInMemoryLdapUserMapping(LdapServer ldapServer) {
    LdapUserMapping ldapUserMapping = new LdapUserMapping();
    ldapUserMapping.setServerId(ldapServer.getId());
    ldapUserMapping.setUserBaseDN("ou=users");
    ldapUserMapping.setUserObjectClass("person");
    ldapUserMapping.setUserIDAttribute("uid");
    ldapUserMapping.setUserRealNameAttribute("cn");
    ldapUserMapping.setUserEmailAttribute("mail");
    ldapUserMapping.setUserSubtree(true);
    ldapUserMapping.setGroupBaseDN("ou=groups");
    ldapUserMapping.setGroupIDAttribute("cn");
    ldapUserMapping.setGroupSubtree(true);
    return ldapUserMapping;
  }

  private LdapConnection newInMemoryLdapConnection(LdapServer ldapServer) {
    LdapConnection ldapConnection = new LdapConnection();
    ldapConnection.setServerId(ldapServer.getId());
    ldapConnection.setProtocol(LdapProtocol.LDAP);
    ldapConnection.setHostname("localhost");
    ldapConnection.setPort(PortAllocator.nextFreePort());
    ldapConnection.setAuthenticationMethod(LdapAuthenticationMethod.NONE);
    ldapConnection.setSystemUsername("system");
    ldapConnection.setSystemPassword("password".toCharArray());
    return ldapConnection;
  }

  private LdapUserMapping createUserMapping(LdapServer ldapServer) {
    LdapUserMapping ldapUserMapping = newInMemoryLdapUserMapping(ldapServer);
    tempEntity.newLdapUserMapping(ldapUserMapping);
    return ldapUserMapping;
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
    assertThat(users).extracting(LdapUser::getUsername)
        .containsExactlyInAnyOrder("test_user1_1", "test_user2_1",
            "test*user1_1");

    // Group without users
    users = ldapService.getUsersByGroup(ldapServer, "no such group");
    assertThat(users).isEmpty();
  }

  @Test
  public void testAddLdapServer() {
    LdapServer ldapServer = new LdapServer("test");

    ldapServer = ldapService.addLdapServer(ldapServer);
    assertThat(ldapServer.getId()).isNotNull();
    assertThat(ldapServer.getName()).isEqualTo("test");

    LdapServer persistedLdapServer = ldapServerDAO.getById(ldapServer.getId());
    assertThat(persistedLdapServer).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(ldapServer);
  }

  @Test
  public void testUpdateLdapServer() {
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    ldapServer.setName("test updated");
    LdapServer updatedLdapServer = ldapService.updateLdapServer(ldapServer);
    assertThat(updatedLdapServer).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(ldapServer);

    LdapServer persistedLdapServer = ldapServerDAO.getById(ldapServer.getId());
    assertThat(persistedLdapServer).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(ldapServer);
  }

  @Test
  public void testDeleteLdapServer() {
    LdapServer ldapServer = tempEntity.newLdapServer("test");

    ldapService.deleteLdapServer(ldapServer.getId());
    assertThat(ldapServerDAO.getById(ldapServer.getId())).isNull();
  }

  @Test
  public void testGetAllLdapServers() {
    LdapServer ldapServer1 = tempEntity.newLdapServer("test1");
    LdapServer ldapServer2 = tempEntity.newLdapServer("test2");
    List<LdapServer> ldapServers = ldapService.getAllLdapServers();
    assertThat(ldapServers).usingRecursiveFieldByFieldElementComparatorIgnoringFields(JPA.IGNORE_FIELDS)
        .containsExactlyInAnyOrder(ldapServer1, ldapServer2);
  }

  @Test
  public void testGetLdapConnection() {
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    LdapConnection expectedLdapConnection = createLdapConnection(ldapServer);
    expectedLdapConnection.setSystemPassword("password".toCharArray());
    ldapConnectionDAO.update(expectedLdapConnection);

    LdapConnection ldapConnection = ldapService.getLdapConnection(ldapServer.getId());
    expectedLdapConnection.setSystemPassword(LdapService.FAKE_PASSWORD);
    assertThat(ldapConnection).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(expectedLdapConnection);
  }

  @Test
  public void testGetLdapConnection_NoLdapConnection() {
    LdapServer ldapServer = tempEntity.newLdapServer("test");

    LdapConnection ldapConnection = ldapService.getLdapConnection(ldapServer.getId());
    assertThat(ldapConnection).isNotNull();
    assertThat(ldapConnection.getServerId()).isEqualTo(ldapServer.getId());
  }

  @Test
  public void testUpsertLdapConnection_Insert() {
    LdapServer ldapServer = tempEntity.newLdapServer("test");

    LdapConnection expectedLdapConnection = newInMemoryLdapConnection(ldapServer);
    char[] expectedSystemPassword = expectedLdapConnection.getSystemPassword();
    LdapConnection addedLdapConnection = ldapService.upsertLdapConnection(ldapServer.getId(), expectedLdapConnection);
    assertThat(addedLdapConnection.getId()).isNotNull();
    expectedLdapConnection.setId(addedLdapConnection.getId());
    expectedLdapConnection.setSystemPassword(LdapService.FAKE_PASSWORD);
    assertThat(addedLdapConnection).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(expectedLdapConnection);

    LdapConnection persistedLdapConnection = ldapConnectionDAO.getById(expectedLdapConnection.getId());
    assertThat(passwordHandler.decryptPassword(persistedLdapConnection.getSystemPassword()))
        .isEqualTo(expectedSystemPassword);
    expectedLdapConnection.setSystemPassword(persistedLdapConnection.getSystemPassword());
    assertThat(persistedLdapConnection).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(expectedLdapConnection);
  }

  @Test
  public void testUpsertLdapConnection_Update() {
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    LdapConnection expectedLdapConnection = tempEntity.newLdapConnection(ldapServer.getId());
    expectedLdapConnection.setSystemPassword("password".toCharArray());
    expectedLdapConnection.setPort(expectedLdapConnection.getPort() + 1);

    char[] expectedSystemPassword = expectedLdapConnection.getSystemPassword();
    LdapConnection updatedLdapConnection = ldapService.upsertLdapConnection(ldapServer.getId(), expectedLdapConnection);
    expectedLdapConnection.setSystemPassword(LdapService.FAKE_PASSWORD);
    assertThat(updatedLdapConnection).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(expectedLdapConnection);

    LdapConnection persistedLdapConnection = ldapConnectionDAO.getById(expectedLdapConnection.getId());
    assertThat(passwordHandler.decryptPassword(persistedLdapConnection.getSystemPassword()))
        .isEqualTo(expectedSystemPassword);
    expectedLdapConnection.setSystemPassword(persistedLdapConnection.getSystemPassword());
    assertThat(persistedLdapConnection).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(expectedLdapConnection);
  }

  @Test
  public void testUpsertLdapConnection_ValidateLdapServerId() {
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    LdapConnection ldapConnection = tempEntity.newLdapConnection(ldapServer.getId());

    assertThatThrownBy(() -> ldapService.upsertLdapConnection("fake LDAP server id", ldapConnection))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Inconsistent LDAP server ID.");
  }

  @Test
  public void testUpsertLdapConnection_Update_FakePasswordSameHostAndSamePort() {
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    LdapConnection expectedLdapConnection =
        tempEntity.newLdapConnection(ldapServer.getId(), passwordHandler.encryptPassword("password".toCharArray()));
    expectedLdapConnection.setSystemPassword(LdapService.FAKE_PASSWORD);
    // Change something other than hostname and port to make sure the update works
    int expectedRetryDelay = expectedLdapConnection.getRetryDelay() + 10;
    expectedLdapConnection.setRetryDelay(expectedRetryDelay);

    LdapConnection updatedLdapConnection = ldapService.upsertLdapConnection(ldapServer.getId(), expectedLdapConnection);

    assertThat(updatedLdapConnection.getRetryDelay()).isEqualTo(expectedRetryDelay);
  }

  @Test
  public void testUpsertLdapConnection_Update_GivenPasswordDifferentHostAndSamePort() {
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    LdapConnection expectedLdapConnection =
        tempEntity.newLdapConnection(ldapServer.getId(), passwordHandler.encryptPassword("password".toCharArray()));
    expectedLdapConnection.setHostname(expectedLdapConnection.getHostname() + "different");

    LdapConnection updatedLdapConnection = ldapService.upsertLdapConnection(ldapServer.getId(), expectedLdapConnection);

    assertThat(updatedLdapConnection.getHostname()).isEqualTo(expectedLdapConnection.getHostname());
    assertThat(updatedLdapConnection.getPort()).isEqualTo(expectedLdapConnection.getPort());
  }

  @Test
  public void testUpsertLdapConnection_Update_GivenPasswordSameHostAndDifferentPort() {
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    LdapConnection expectedLdapConnection =
        tempEntity.newLdapConnection(ldapServer.getId(), passwordHandler.encryptPassword("password".toCharArray()));
    expectedLdapConnection.setPort(expectedLdapConnection.getPort() + 10);

    LdapConnection updatedLdapConnection = ldapService.upsertLdapConnection(ldapServer.getId(), expectedLdapConnection);

    assertThat(updatedLdapConnection.getHostname()).isEqualTo(expectedLdapConnection.getHostname());
    assertThat(updatedLdapConnection.getPort()).isEqualTo(expectedLdapConnection.getPort());
  }

  @Test
  public void testUpsertLdapConnection_Update_FakePasswordDifferentHostAndSamePort() {
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    LdapConnection expectedLdapConnection =
        tempEntity.newLdapConnection(ldapServer.getId(), passwordHandler.encryptPassword("password".toCharArray()));
    expectedLdapConnection.setSystemPassword(LdapService.FAKE_PASSWORD);
    expectedLdapConnection.setHostname(expectedLdapConnection.getHostname() + "different");

    assertThatThrownBy(() -> ldapService.upsertLdapConnection(ldapServer.getId(), expectedLdapConnection))
        .isInstanceOf(BadRequestException.class)
        .hasMessage(
            "The password must be given when updating the hostname or port for a connection that uses authentication.");
  }

  @Test
  public void testUpsertLdapConnection_Update_FakePasswordSameHostAndDifferentPort() {
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    LdapConnection expectedLdapConnection =
        tempEntity.newLdapConnection(ldapServer.getId(), passwordHandler.encryptPassword("password".toCharArray()));
    expectedLdapConnection.setSystemPassword(LdapService.FAKE_PASSWORD);
    expectedLdapConnection.setPort(expectedLdapConnection.getPort() + 10);

    assertThatThrownBy(() -> ldapService.upsertLdapConnection(ldapServer.getId(), expectedLdapConnection))
        .isInstanceOf(BadRequestException.class)
        .hasMessage(
            "The password must be given when updating the hostname or port for a connection that uses authentication.");
  }

  @Test
  public void testGetLdapUserMapping() {
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    LdapUserMapping expectedLdapUserMapping = createUserMapping(ldapServer);

    LdapUserMapping ldapUserMapping = ldapService.getLdapUserMapping(ldapServer.getId());
    assertThat(ldapUserMapping).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(expectedLdapUserMapping);
  }

  @Test
  public void testGetLdapUserMapping_NoLdapUserMapping() {
    LdapServer ldapServer = tempEntity.newLdapServer("test");

    LdapUserMapping ldapUserMapping = ldapService.getLdapUserMapping(ldapServer.getId());
    assertThat(ldapUserMapping).isNotNull();
    assertThat(ldapUserMapping.getServerId()).isEqualTo(ldapServer.getId());
  }

  @Test
  public void testUpsertLdapUserMapping_Insert() {
    LdapServer ldapServer = tempEntity.newLdapServer("test");

    LdapUserMapping expectedLdapUserMapping = newInMemoryLdapUserMapping(ldapServer);
    LdapUserMapping addedLdapUserMapping =
        ldapService.upsertLdapUserMapping(ldapServer.getId(), expectedLdapUserMapping);
    assertThat(addedLdapUserMapping.getId()).isNotNull();
    expectedLdapUserMapping.setId(addedLdapUserMapping.getId());
    assertThat(addedLdapUserMapping).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(expectedLdapUserMapping);

    LdapUserMapping persistedLdapUserMapping = ldapUserMappingDAO.getById(expectedLdapUserMapping.getId());
    assertThat(persistedLdapUserMapping).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(expectedLdapUserMapping);
  }

  @Test
  public void testUpsertLdapUserMapping_Update() {
    LdapServer ldapServer = tempEntity.newLdapServer("test");

    LdapUserMapping expectedLdapUserMapping = tempEntity.newLdapUserMapping(ldapServer.getId());
    expectedLdapUserMapping.setUserEmailAttribute(expectedLdapUserMapping.getUserEmailAttribute() + "changed");
    LdapUserMapping updatedLdapUserMapping =
        ldapService.upsertLdapUserMapping(ldapServer.getId(), expectedLdapUserMapping);
    assertThat(updatedLdapUserMapping).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(expectedLdapUserMapping);

    LdapUserMapping persistedLdapUserMapping = ldapUserMappingDAO.getById(expectedLdapUserMapping.getId());
    assertThat(persistedLdapUserMapping).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(expectedLdapUserMapping);
  }

  @Test
  public void testUpsertLdapUserMapping_ValidateLdapServerId() {
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    LdapUserMapping ldapUserMapping = tempEntity.newLdapUserMapping(ldapServer.getId());

    assertThatThrownBy(() -> ldapService.upsertLdapUserMapping("fake LDAP server id", ldapUserMapping))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Inconsistent LDAP server ID.");
  }

  @Test
  public void testReferrals() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    LdapConnection ldapConnection = createLdapConnection(ldapServer);
    createUserMapping(ldapServer);
    testLdapServer1.setLdifResourceName("/" + getClass().getSimpleName() + "/ldap_referrals.ldif");
    startLdapServer(testLdapServer1, ldapConnection);

    ldapConnection.setReferralIgnored(false);
    ldapConnectionDAO.update(ldapConnection);
    assertThatExceptionOfType(NotContextException.class)
        .isThrownBy(() -> ldapService.getUsersByName(ldapServer, new String[]{"nobody"}));

    ldapConnection.setReferralIgnored(true);
    ldapConnectionDAO.update(ldapConnection);
    assertThat(ldapService.getUsersByName(ldapServer, new String[]{"nobody"})).isEmpty();
  }
}
