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
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LdapResourceTest
    extends AbstractResourceTest
{
  private static final LdapServerDAO serverDao = new LdapServerDAO();

  @Rule
  public TestLdapServer testLdapServer = new TestLdapServer();

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(LdapResource.RESOURCE_PATH);
  }

  private HttpRequest testConnectionRequest(LdapConnection conn) {
    return restRequest().path(LdapResource.TEST_CONNECTION_PATH).parameter(conn.getServerId()).body(conn);
  }

  @Test
  public void testLdapServerCRUD() throws Exception {
    // Create
    LdapServer ldapServer = new LdapServer("test server");
    HttpResponse response = restRequest().body(ldapServer).post();
    assertResponseStatus(200, response);
    LdapServer addedLdapServer = ldapServer = response.getBody(LdapServer.class);
    assertThat(addedLdapServer.getId()).isNotNull();
    tempEntity.register(ldapServer);
    ldapServer.setId(addedLdapServer.getId());
    assertThat(addedLdapServer).isEqualToIgnoringGivenFields(ldapServer, JPA.IGNORE_FIELDS);
    LdapServer persistedLdapServer = serverDao.getById(ldapServer.getId());
    assertThat(persistedLdapServer).isEqualToIgnoringGivenFields(ldapServer, JPA.IGNORE_FIELDS);

    // Get all
    response = restRequest().get();
    assertResponseStatus(200, response);
    LdapServer[] ldapServers = response.getBody(LdapServer[].class);
    assertThat(ldapServers).usingElementComparatorIgnoringFields(JPA.IGNORE_FIELDS)
        .containsExactly(ldapServer);

    // Update
    ldapServer.setName("test server updated");
    response = restRequest().body(ldapServer).put();
    assertResponseStatus(200, response);
    LdapServer updatedLdapServer = response.getBody(LdapServer.class);
    assertThat(updatedLdapServer).isEqualToIgnoringGivenFields(ldapServer, JPA.IGNORE_FIELDS);
    persistedLdapServer = serverDao.getById(ldapServer.getId());
    assertThat(persistedLdapServer).isEqualToIgnoringGivenFields(ldapServer, JPA.IGNORE_FIELDS);

    // Delete
    response = restRequest().path(ldapServer.getId()).delete();
    assertResponseStatus(204, response);
    assertThat(serverDao.getById(ldapServer.getId())).isNull();
  }

  @Test
  public void testUpdatePriority() throws Exception {
    LdapServer server1 = tempEntity.newLdapServer("server1");
    LdapServer server2 = tempEntity.newLdapServer("server2");
    HttpResponse response = restRequest().path(LdapResource.PRIORITY_PATH)
        .body(Arrays.asList(server2.getId(), server1.getId())).put();
    assertThat(serverDao.getById(server2.getId()).getPriority()).isEqualTo(1);
    assertThat(serverDao.getById(server1.getId()).getPriority()).isEqualTo(2);
    assertResponseStatus(204, response);
  }

  @Test
  public void testLdapUserMappingCRUD() throws Exception {
    LdapUserMapping expectedLdapUserMapping = createLdapUserMapping();
    HttpRequest request = restRequest().path(LdapResource.USER_MAPPING_PATH);

    // Create
    HttpResponse response =
        request.parameter(expectedLdapUserMapping.getServerId()).body(expectedLdapUserMapping).put();
    assertResponseStatus(200, response);
    LdapUserMapping addedLdapUserMapping = response.getBody(LdapUserMapping.class);
    assertThat(addedLdapUserMapping.getId()).isNotNull();
    expectedLdapUserMapping.setId(addedLdapUserMapping.getId());
    assertThat(addedLdapUserMapping).isEqualToIgnoringGivenFields(expectedLdapUserMapping, JPA.IGNORE_FIELDS);

    LdapUserMapping persistedLdapUserMapping = new LdapUserMappingDAO().getById(expectedLdapUserMapping.getId());
    assertThat(persistedLdapUserMapping).isEqualToIgnoringGivenFields(expectedLdapUserMapping, JPA.IGNORE_FIELDS);

    // Get
    response = request.get();
    assertResponseStatus(200, response);
    LdapUserMapping ldapUserMapping = response.getBody(LdapUserMapping.class);
    assertThat(ldapUserMapping).isEqualToIgnoringGivenFields(expectedLdapUserMapping, JPA.IGNORE_FIELDS);

    // Update
    expectedLdapUserMapping.setUserEmailAttribute(expectedLdapUserMapping.getUserEmailAttribute() + "changed");
    response = request.body(expectedLdapUserMapping).put();
    assertResponseStatus(200, response);
    ldapUserMapping = response.getBody(LdapUserMapping.class);
    assertThat(ldapUserMapping).isEqualToIgnoringGivenFields(expectedLdapUserMapping, JPA.IGNORE_FIELDS);
  }

  @Test
  public void testLdapConnectionCRUD() throws Exception {
    PasswordHandler passwordHandler = getCLMServer().getInstance(PasswordHandler.class);

    // Create
    LdapConnection expectedLdapConnection = createLdapConnection();
    char[] expectedSystemPassword = expectedLdapConnection.getSystemPassword();
    HttpRequest request =
        restRequest().path(LdapResource.CONNECTION_PATH).parameter(expectedLdapConnection.getServerId());

    HttpResponse response = request.body(expectedLdapConnection).put();
    assertResponseStatus(200, response);
    LdapConnection addedLdapConnection = response.getBody(LdapConnection.class);
    assertThat(addedLdapConnection.getId()).isNotNull();
    expectedLdapConnection.setId(addedLdapConnection.getId());
    expectedLdapConnection.setSystemPassword(LdapService.FAKE_PASSWORD);
    assertThat(addedLdapConnection).isEqualToIgnoringGivenFields(expectedLdapConnection, JPA.IGNORE_FIELDS);

    LdapConnection persistedLdapConnection = new LdapConnectionDAO().getById(expectedLdapConnection.getId());
    assertThat(passwordHandler.decryptPassword(persistedLdapConnection.getSystemPassword()))
        .isEqualTo(expectedSystemPassword);
    expectedLdapConnection.setSystemPassword(persistedLdapConnection.getSystemPassword());
    assertThat(persistedLdapConnection).isEqualToIgnoringGivenFields(expectedLdapConnection, JPA.IGNORE_FIELDS);

    // Get by serverId
    response = request.get();
    assertResponseStatus(200, response);
    LdapConnection ldapConnection = response.getBody(LdapConnection.class);
    expectedLdapConnection.setSystemPassword(LdapService.FAKE_PASSWORD);
    assertThat(ldapConnection).isEqualToIgnoringGivenFields(expectedLdapConnection, JPA.IGNORE_FIELDS);

    // Update
    expectedLdapConnection.setPort(expectedLdapConnection.getPort() + 1);

    response = request.body(expectedLdapConnection).put();
    assertResponseStatus(200, response);
    ldapConnection = response.getBody(LdapConnection.class);
    assertThat(ldapConnection).isEqualToIgnoringGivenFields(expectedLdapConnection, JPA.IGNORE_FIELDS);
    persistedLdapConnection = new LdapConnectionDAO().getById(expectedLdapConnection.getId());
    assertThat(passwordHandler.decryptPassword(persistedLdapConnection.getSystemPassword()))
        .isEqualTo(expectedSystemPassword);
    expectedLdapConnection.setSystemPassword(persistedLdapConnection.getSystemPassword());
    assertThat(persistedLdapConnection).isEqualToIgnoringGivenFields(expectedLdapConnection, JPA.IGNORE_FIELDS);
  }

  @Test
  public void testAddLdapServer_Unlicensed() throws Exception {
    uninstallLicense();
    LdapServer server = new LdapServer("test server");

    HttpResponse response = restRequest().body(server).post();
    assertResponseStatus(402, response);
  }

  @Test
  public void testUpdateLdapServer_Unlicensed() throws Exception {
    LdapServer server = tempEntity.newLdapServer("test server");

    uninstallLicense();
    HttpResponse response = restRequest().body(server).put();
    assertResponseStatus(402, response);
  }

  @Test
  public void testGetAllLdapServers_Unlicensed() throws Exception {
    uninstallLicense();
    HttpResponse response = restRequest().get();
    assertResponseStatus(402, response);
  }

  @Test
  public void testTestLdapConnection() throws Exception {
    testLdapServer.setAuthenticationSimple();
    testLdapServer.start();

    LdapConnection conn = createLdapConnection();
    conn.setPort(testLdapServer.getPort());
    conn.setAuthenticationMethod(LdapAuthenticationMethod.SIMPLE);
    conn.setSystemUsername(testLdapServer.getSystemUserDN());
    conn.setSystemPassword(testLdapServer.getSystemUserPassword());

    HttpResponse response = testConnectionRequest(conn).put();
    assertResponseStatus(200, response);
    LdapConnectionStatus status = response.getBody(LdapConnectionStatus.class);

    assertThat(status.getStatus()).as(status.getMessage()).isEqualTo(LdapConnectionStatus.Status.OK);
  }

  @Test
  @Ignore("execution-order dependent failures")
  public void testTestLdapConnection_ldaps() throws Exception {
    testLdapServer.setAuthenticationSimple();
    testLdapServer.enableLdaps(getTestResourceFile("/com/sonatype/insight/test/networking/localhost.jks"), "password");
    testLdapServer.start();

    LdapConnection conn = createLdapConnection();
    conn.setProtocol(LdapProtocol.LDAPS);
    conn.setHostname("localhost");
    conn.setPort(testLdapServer.getPort());
    conn.setAuthenticationMethod(LdapAuthenticationMethod.SIMPLE);
    conn.setSystemUsername(testLdapServer.getSystemUserDN());
    conn.setSystemPassword(testLdapServer.getSystemUserPassword());

    HttpResponse response = testConnectionRequest(conn).put();
    assertResponseStatus(200, response);
    LdapConnectionStatus status = response.getBody(LdapConnectionStatus.class);

    assertThat(status.getStatus()).as(status.getMessage()).isEqualTo(LdapConnectionStatus.Status.OK);
  }

  @Test
  public void testTestLdapUserMapping() throws Exception {
    testLdapServer.start();
    testLdapServer.loadData("/" + getClass().getSimpleName() + "/ldap_users.ldif");

    LdapServer server = tempEntity.newLdapServer("test");

    LdapUserMapping mapping = tempEntity.newLdapUserMapping(server.getId());
    tempEntity.newLdapConnection(server.getId(), testLdapServer.getPort());
    HttpRequest request = restRequest().path(LdapResource.TEST_USER_MAPPING_PATH).parameter(mapping.getServerId())
        .body(mapping);

    HttpResponse response = request.put();
    assertResponseStatus(200, response);
    LdapUser[] users = response.getBody(LdapUser[].class);
    Arrays.sort(users);
    assertThat(users).hasSize(3);
    assertThat(users[0].getUsername()).isEqualTo("Beta");
    assertThat(users[0].getRealName()).isEqualTo("Beta User");
    assertThat(users[0].getEmail()).isEqualTo("beta.user@company.com");
  }

  @Test
  public void testTestUserLogin() throws Exception {
    testLdapServer.start();
    testLdapServer.loadData("/" + getClass().getSimpleName() + "/ldap_users.ldif");

    LdapServer server = tempEntity.newLdapServer("test");

    LdapUserMapping mapping = tempEntity.newLdapUserMapping(server.getId());
    LdapTestLoginRequest login = new LdapTestLoginRequest();
    login.setUserMapping(mapping);
    login.setUsername("testuser");
    login.setPassword("bad");
    HttpRequest request = restRequest().path(LdapResource.TEST_LOGIN_PATH).parameter(mapping.getServerId());

    HttpResponse response = request.body(login).put();
    assertResponseStatus(200, response);
    LdapConnectionStatus status = response.getBody(LdapConnectionStatus.class);
    assertThat(status.getStatus()).isEqualTo(LdapConnectionStatus.Status.FAILURE);

    tempEntity.newLdapConnection(server.getId(), testLdapServer.getPort());

    response = request.body(login).put();
    assertResponseStatus(200, response);
    status = response.getBody(LdapConnectionStatus.class);
    assertThat(status.getStatus()).isEqualTo(LdapConnectionStatus.Status.FAILURE);

    login.setPassword("far2simple");
    response = request.body(login).put();
    assertResponseStatus(200, response);
    status = response.getBody(LdapConnectionStatus.class);
    assertThat(status.getStatus()).as(status.getMessage()).isEqualTo(LdapConnectionStatus.Status.OK);
  }

  private File getTestResourceFile(String path) throws IOException {
    URL resource = getClass().getResource(path);
    assertThat(resource).as(path).isNotNull(); // sanity check
    File tempFile = tempDir.newFile();
    FileUtils.copyURLToFile(resource, tempFile);
    return tempFile;
  }

  private LdapConnection createLdapConnection() {
    LdapServer server = tempEntity.newLdapServer("test");

    LdapConnection conn = new LdapConnection();
    conn.setServerId(server.getId());
    conn.setProtocol(LdapProtocol.LDAP);
    conn.setHostname("localhost");
    conn.setAuthenticationMethod(LdapAuthenticationMethod.NONE);
    conn.setSystemUsername("system");
    conn.setSystemPassword("password".toCharArray());
    return conn;
  }

  private LdapUserMapping createLdapUserMapping() {
    LdapServer server = tempEntity.newLdapServer("test");

    LdapUserMapping umap = new LdapUserMapping();

    umap.setServerId(server.getId());
    umap.setUserBaseDN("userBaseDN");
    umap.setUserSubtree(true);
    umap.setUserObjectClass("userObjectClass");
    umap.setUserFilter("userFilter");
    umap.setUserIDAttribute("userIDAttribute");
    umap.setUserRealNameAttribute("realNameAttribute");
    umap.setUserEmailAttribute("emailAttribute");
    umap.setUserPasswordAttribute("passwordAttribute");

    umap.setGroupMappingType(LdapGroupMappingType.STATIC);
    umap.setGroupBaseDN("groupBaseDN");
    umap.setGroupSubtree(true);
    umap.setGroupObjectClass("groupObjectClass");
    umap.setGroupIDAttribute("groupIDAttribute");
    umap.setGroupMemberAttribute("groupMemberAttribute");
    umap.setGroupMemberFormat("groupMemberFormat");
    umap.setUserMemberOfGroupAttribute("userMemberOfGroupAttribute");

    return umap;
  }
}
