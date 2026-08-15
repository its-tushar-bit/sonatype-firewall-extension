/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
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
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kept in the {@code LdapResource}/{@code LdapService} package because it accesses their package-private
 * {@code RESOURCE_PATH}/path constants and {@code FAKE_PASSWORD}; kept the exact original simple name because
 * {@link #testTestLdapUserMapping} and {@link #testTestUserLogin} resolve the LDAP fixture via
 * {@code getClass().getSimpleName()}.
 */
@IqH2Test
class LdapResourceTest
{
  private IqTestContext ctx;

  private final TestLdapServer testLdapServer = new TestLdapServer();

  private LdapUserMappingDAO ldapUserMappingDAO;

  private LdapConnectionDAO ldapConnectionDAO;

  private LdapServerDAO ldapServerDAO;

  @BeforeEach
  void setUp() {
    ldapUserMappingDAO = ctx.lookup(LdapUserMappingDAO.class);
    ldapConnectionDAO = ctx.lookup(LdapConnectionDAO.class);
    ldapServerDAO = ctx.lookup(LdapServerDAO.class);
  }

  @AfterEach
  void tearDown() throws Exception {
    testLdapServer.stop();
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(LdapResource.RESOURCE_PATH);
  }

  private HttpRequest testConnectionRequest(LdapConnection ldapConnection) {
    return restRequest().path(LdapResource.TEST_CONNECTION_PATH)
        .parameter(ldapConnection.getServerId())
        .body(ldapConnection);
  }

  @Test
  void testLdapServerCRUD() throws Exception {
    // Create
    LdapServer ldapServer = new LdapServer("test server");
    HttpResponse response = restRequest().body(ldapServer).post();
    ctx.assertResponseStatus(200, response);
    LdapServer addedLdapServer = ldapServer = response.getBody(LdapServer.class);
    assertThat(addedLdapServer.getId()).isNotNull();
    ldapServer.setId(addedLdapServer.getId());
    assertThat(addedLdapServer).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(ldapServer);
    LdapServer persistedLdapServer = ldapServerDAO.getById(ldapServer.getId());
    assertThat(persistedLdapServer).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(ldapServer);

    // Get all
    response = restRequest().get();
    ctx.assertResponseStatus(200, response);
    LdapServer[] ldapServers = response.getBody(LdapServer[].class);
    assertThat(ldapServers).usingRecursiveFieldByFieldElementComparatorIgnoringFields(JPA.IGNORE_FIELDS)
        .containsExactly(ldapServer);

    // Update
    ldapServer.setName("test server updated");
    response = restRequest().body(ldapServer).put();
    ctx.assertResponseStatus(200, response);
    LdapServer updatedLdapServer = response.getBody(LdapServer.class);
    assertThat(updatedLdapServer).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(ldapServer);
    persistedLdapServer = ldapServerDAO.getById(ldapServer.getId());
    assertThat(persistedLdapServer).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(ldapServer);

    // Delete
    response = restRequest().path(ldapServer.getId()).delete();
    ctx.assertResponseStatus(204, response);
    assertThat(ldapServerDAO.getById(ldapServer.getId())).isNull();
  }

  @Test
  void testUpdatePriority() throws Exception {
    LdapServer ldapServer1 = ctx.tempEntity().newLdapServer("server1");
    LdapServer ldapServer2 = ctx.tempEntity().newLdapServer("server2");
    HttpResponse response = restRequest().path(LdapResource.PRIORITY_PATH)
        .body(Arrays.asList(ldapServer2.getId(), ldapServer1.getId()))
        .put();
    assertThat(ldapServerDAO.getById(ldapServer2.getId()).getPriority()).isEqualTo(1);
    assertThat(ldapServerDAO.getById(ldapServer1.getId()).getPriority()).isEqualTo(2);
    ctx.assertResponseStatus(204, response);
  }

  @Test
  void testLdapUserMappingCRUD() throws Exception {
    LdapUserMapping expectedLdapUserMapping = createLdapUserMapping();
    HttpRequest request = restRequest().path(LdapResource.USER_MAPPING_PATH);

    // Create
    HttpResponse response =
        request.parameter(expectedLdapUserMapping.getServerId()).body(expectedLdapUserMapping).put();
    ctx.assertResponseStatus(200, response);
    LdapUserMapping addedLdapUserMapping = response.getBody(LdapUserMapping.class);
    assertThat(addedLdapUserMapping.getId()).isNotNull();
    expectedLdapUserMapping.setId(addedLdapUserMapping.getId());
    assertThat(addedLdapUserMapping).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(expectedLdapUserMapping);

    LdapUserMapping persistedLdapUserMapping = ldapUserMappingDAO.getById(expectedLdapUserMapping.getId());
    assertThat(persistedLdapUserMapping).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(expectedLdapUserMapping);

    // Get
    response = request.get();
    ctx.assertResponseStatus(200, response);
    LdapUserMapping ldapUserMapping = response.getBody(LdapUserMapping.class);
    assertThat(ldapUserMapping).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(expectedLdapUserMapping);

    // Update
    expectedLdapUserMapping.setUserEmailAttribute(expectedLdapUserMapping.getUserEmailAttribute() + "changed");
    response = request.body(expectedLdapUserMapping).put();
    ctx.assertResponseStatus(200, response);
    ldapUserMapping = response.getBody(LdapUserMapping.class);
    assertThat(ldapUserMapping).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(expectedLdapUserMapping);
  }

  @Test
  void testLdapConnectionCRUD() throws Exception {
    PasswordHandler passwordHandler = ctx.lookup(PasswordHandler.class);

    // Create
    LdapConnection expectedLdapConnection = createLdapConnection();
    char[] expectedSystemPassword = expectedLdapConnection.getSystemPassword();
    HttpRequest request =
        restRequest().path(LdapResource.CONNECTION_PATH).parameter(expectedLdapConnection.getServerId());

    HttpResponse response = request.body(expectedLdapConnection).put();
    ctx.assertResponseStatus(200, response);
    LdapConnection addedLdapConnection = response.getBody(LdapConnection.class);
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

    // Get by serverId
    response = request.get();
    ctx.assertResponseStatus(200, response);
    LdapConnection ldapConnection = response.getBody(LdapConnection.class);
    expectedLdapConnection.setSystemPassword(LdapService.FAKE_PASSWORD);
    assertThat(ldapConnection).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(expectedLdapConnection);

    // Update
    expectedLdapConnection.setPort(expectedLdapConnection.getPort() + 1);

    response = request.body(expectedLdapConnection).put();
    ctx.assertResponseStatus(200, response);
    ldapConnection = response.getBody(LdapConnection.class);
    assertThat(ldapConnection).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(expectedLdapConnection);
    persistedLdapConnection = ldapConnectionDAO.getById(expectedLdapConnection.getId());
    assertThat(passwordHandler.decryptPassword(persistedLdapConnection.getSystemPassword()))
        .isEqualTo(expectedSystemPassword);
    expectedLdapConnection.setSystemPassword(persistedLdapConnection.getSystemPassword());
    assertThat(persistedLdapConnection).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(expectedLdapConnection);
  }

  @Test
  void testAddLdapServer_Unlicensed() throws Exception {
    ctx.uninstallLicense();
    LdapServer ldapServer = new LdapServer("test server");

    HttpResponse response = restRequest().body(ldapServer).post();
    ctx.assertResponseStatus(402, response);
  }

  @Test
  void testUpdateLdapServer_Unlicensed() throws Exception {
    LdapServer ldapServer = ctx.tempEntity().newLdapServer("test server");

    ctx.uninstallLicense();
    HttpResponse response = restRequest().body(ldapServer).put();
    ctx.assertResponseStatus(402, response);
  }

  @Test
  void testGetAllLdapServers_Unlicensed() throws Exception {
    ctx.uninstallLicense();
    HttpResponse response = restRequest().get();
    ctx.assertResponseStatus(402, response);
  }

  @Test
  void testTestLdapConnection() throws Exception {
    testLdapServer.setAuthenticationSimple();
    testLdapServer.start();

    LdapConnection ldapConnection = createLdapConnection();
    ldapConnection.setPort(testLdapServer.getPort());
    ldapConnection.setAuthenticationMethod(LdapAuthenticationMethod.SIMPLE);
    ldapConnection.setSystemUsername(testLdapServer.getSystemUserDN());
    ldapConnection.setSystemPassword(testLdapServer.getSystemUserPassword());

    HttpResponse response = testConnectionRequest(ldapConnection).put();
    ctx.assertResponseStatus(200, response);
    LdapConnectionStatus status = response.getBody(LdapConnectionStatus.class);

    assertThat(status.getStatus()).as(status.getMessage()).isEqualTo(LdapConnectionStatus.Status.OK);
  }

  @Test
  void testTestLdapUserMapping() throws Exception {
    testLdapServer.start();
    testLdapServer.loadData("/" + getClass().getSimpleName() + "/ldap_users.ldif");

    LdapServer ldapServer = ctx.tempEntity().newLdapServer("test");

    LdapUserMapping mapping = ctx.tempEntity().newLdapUserMapping(ldapServer.getId());
    ctx.tempEntity().newLdapConnection(ldapServer.getId(), testLdapServer.getPort());
    HttpRequest request = restRequest().path(LdapResource.TEST_USER_MAPPING_PATH)
        .parameter(mapping.getServerId())
        .body(mapping);

    HttpResponse response = request.put();
    ctx.assertResponseStatus(200, response);
    LdapUser[] users = response.getBody(LdapUser[].class);
    Arrays.sort(users);
    assertThat(users).hasSize(3);
    assertThat(users[0].getUsername()).isEqualTo("Beta");
    assertThat(users[0].getRealName()).isEqualTo("Beta User");
    assertThat(users[0].getEmail()).isEqualTo("beta.user@company.com");
  }

  @Test
  void testTestUserLogin() throws Exception {
    testLdapServer.start();
    testLdapServer.loadData("/" + getClass().getSimpleName() + "/ldap_users.ldif");

    LdapServer ldapServer = ctx.tempEntity().newLdapServer("test");

    LdapUserMapping mapping = ctx.tempEntity().newLdapUserMapping(ldapServer.getId());
    LdapTestLoginRequest login = new LdapTestLoginRequest();
    login.setUserMapping(mapping);
    login.setUsername("testuser");
    login.setPassword("bad");
    HttpRequest request = restRequest().path(LdapResource.TEST_LOGIN_PATH).parameter(mapping.getServerId());

    HttpResponse response = request.body(login).put();
    ctx.assertResponseStatus(200, response);
    LdapConnectionStatus status = response.getBody(LdapConnectionStatus.class);
    assertThat(status.getStatus()).isEqualTo(LdapConnectionStatus.Status.FAILURE);

    ctx.tempEntity().newLdapConnection(ldapServer.getId(), testLdapServer.getPort());

    response = request.body(login).put();
    ctx.assertResponseStatus(200, response);
    status = response.getBody(LdapConnectionStatus.class);
    assertThat(status.getStatus()).isEqualTo(LdapConnectionStatus.Status.FAILURE);

    login.setPassword("far2simple");
    response = request.body(login).put();
    ctx.assertResponseStatus(200, response);
    status = response.getBody(LdapConnectionStatus.class);
    assertThat(status.getStatus()).as(status.getMessage()).isEqualTo(LdapConnectionStatus.Status.OK);
  }

  private File getTestResourceFile(String path) throws IOException {
    URL resource = getClass().getResource(path);
    assertThat(resource).as(path).isNotNull(); // sanity check
    File tempFile = ctx.tempFolder().newFile();
    FileUtils.copyURLToFile(resource, tempFile);
    return tempFile;
  }

  private LdapConnection createLdapConnection() {
    LdapServer ldapServer = ctx.tempEntity().newLdapServer("test");

    LdapConnection ldapConnection = new LdapConnection();
    ldapConnection.setServerId(ldapServer.getId());
    ldapConnection.setProtocol(LdapProtocol.LDAP);
    ldapConnection.setHostname("localhost");
    ldapConnection.setAuthenticationMethod(LdapAuthenticationMethod.NONE);
    ldapConnection.setSystemUsername("system");
    ldapConnection.setSystemPassword("password".toCharArray());
    return ldapConnection;
  }

  private LdapUserMapping createLdapUserMapping() {
    LdapServer ldapServer = ctx.tempEntity().newLdapServer("test");

    LdapUserMapping ldapUserMapping = new LdapUserMapping();

    ldapUserMapping.setServerId(ldapServer.getId());
    ldapUserMapping.setUserBaseDN("userBaseDN");
    ldapUserMapping.setUserSubtree(true);
    ldapUserMapping.setUserObjectClass("userObjectClass");
    ldapUserMapping.setUserFilter("userFilter");
    ldapUserMapping.setUserIDAttribute("userIDAttribute");
    ldapUserMapping.setUserRealNameAttribute("realNameAttribute");
    ldapUserMapping.setUserEmailAttribute("emailAttribute");
    ldapUserMapping.setUserPasswordAttribute("passwordAttribute");

    ldapUserMapping.setGroupMappingType(LdapGroupMappingType.STATIC);
    ldapUserMapping.setGroupBaseDN("groupBaseDN");
    ldapUserMapping.setGroupSubtree(true);
    ldapUserMapping.setGroupObjectClass("groupObjectClass");
    ldapUserMapping.setGroupIDAttribute("groupIDAttribute");
    ldapUserMapping.setGroupMemberAttribute("groupMemberAttribute");
    ldapUserMapping.setGroupMemberFormat("groupMemberFormat");
    ldapUserMapping.setUserMemberOfGroupAttribute("userMemberOfGroupAttribute");

    return ldapUserMapping;
  }
}
