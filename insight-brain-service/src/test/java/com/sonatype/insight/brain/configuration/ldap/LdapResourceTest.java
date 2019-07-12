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
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapConnectionDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.configuration.ldap.LdapAuthenticationMethod;
import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.model.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.model.configuration.ldap.LdapProtocol;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class LdapResourceTest
    extends AbstractResourceTest
{
  private static final LdapServerDAO serverDao = new LdapServerDAO();

  @Rule
  public TestLdapServer ldapServer = new TestLdapServer();

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(LdapResource.RESOURCE_PATH);
  }

  private HttpRequest testConnectionRequest(LdapConnection conn) {
    return restRequest().path(LdapResource.TEST_CONNECTION_PATH).parameter(conn.getServerId()).body(conn);
  }

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void testServerCRUD() throws Exception {
    // Create
    LdapServer server = new LdapServer("test server");

    HttpResponse response = restRequest().body(server).post();
    assertResponseStatus(200, response);
    server = response.getBody(LdapServer.class);
    assertThat(server).isNotNull();
    assertThat(server.getId()).isNotNull();
    assertThat(server.getName()).isEqualTo("test server");
    String ldapServerId = server.getId();

    LdapServer raw = serverDao.getById(server.getId());

    assertThat(raw).isNotNull();
    assertThat(raw.getId()).isEqualTo(ldapServerId);
    assertThat(raw.getName()).isEqualTo(server.getName());
    assertThat(raw.getNameLowercaseNoWhitespace()).isEqualTo(NameHelper.normalize(server.getName()));

    // Get all
    response = restRequest().get();
    assertResponseStatus(200, response);
    LdapServer[] ldapServers = response.getBody(LdapServer[].class);
    assertThat(ldapServers).hasSize(1);
    LdapServer echo = ldapServers[0];

    assertThat(echo).isNotNull();
    assertThat(echo.getId()).isEqualTo(ldapServerId);
    assertThat(echo.getName()).isEqualTo(server.getName());
    assertThat(echo.getNameLowercaseNoWhitespace()).isEqualTo(NameHelper.normalize(server.getName()));

    String name = "test server updated";

    // Update
    server.setName(name);

    response = restRequest().body(server).put();
    assertResponseStatus(200, response);
    server = response.getBody(LdapServer.class);

    assertThat(server).isNotNull();
    assertThat(server.getId()).isEqualTo(ldapServerId);
    assertThat(server.getName()).isEqualTo(name);

    raw = serverDao.getById(server.getId());
    assertThat(raw).isNotNull();
    assertThat(raw.getId()).isEqualTo(ldapServerId);
    assertThat(raw.getName()).isEqualTo(name);
    assertThat(raw.getNameLowercaseNoWhitespace()).isEqualTo(NameHelper.normalize(name));

    // Delete
    response = restRequest().path(ldapServerId).delete();
    assertResponseStatus(204, response);

    assertThat(serverDao.getById(ldapServerId)).isNull();
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
  public void testUserMappingCRUD() throws Exception {
    final LdapUserMapping orig = newUserMapping("test server");
    HttpRequest request = restRequest().path(LdapResource.USER_MAPPING_PATH);

    // PUT new, a.k.a. "insert"
    HttpResponse response = request.parameter(orig.getServerId()).body(orig).put();
    assertResponseStatus(200, response);
    LdapUserMapping umap = response.getBody(LdapUserMapping.class);
    assertUserMappingEquals(orig, umap);

    // GET
    response = request.get();
    assertResponseStatus(200, response);
    umap = response.getBody(LdapUserMapping.class);
    assertUserMappingEquals(orig, umap);

    // PUT existing, a.k.a "update"
    umap.setUserEmailAttribute(orig.getUserEmailAttribute() + "changed");
    response = request.body(umap).put();
    assertResponseStatus(200, response);
    umap = response.getBody(LdapUserMapping.class);
    assertThat(umap.getUserEmailAttribute()).isEqualTo(orig.getUserEmailAttribute() + "changed");
  }

  @Test
  public void testNewUserMapping() throws Exception {
    LdapServer server = tempEntity.newLdapServer("test server");

    HttpResponse response = restRequest().path(LdapResource.USER_MAPPING_PATH).parameter(server.getId()).get();
    assertResponseStatus(200, response);
    LdapUserMapping umap = response.getBody(LdapUserMapping.class);
    assertThat(umap).isNotNull();
    assertThat(umap.getServerId()).isEqualTo(server.getId());
  }

  @Test
  public void testNewConnection() throws Exception {
    LdapServer server = tempEntity.newLdapServer("test server");

    HttpResponse response = restRequest().path(LdapResource.CONNECTION_PATH).parameter(server.getId()).get();
    assertResponseStatus(200, response);
    LdapConnection conn = response.getBody(LdapConnection.class);
    assertThat(conn).isNotNull();
    assertThat(conn.getServerId()).isEqualTo(server.getId());
  }

  @Test
  public void testConnectionCRUD() throws Exception {
    // Create
    LdapConnection conn = createLdapConnection("test server");
    HttpRequest request = restRequest().path(LdapResource.CONNECTION_PATH).parameter(conn.getServerId());

    HttpResponse response = request.body(conn).put();
    assertResponseStatus(200, response);
    conn = response.getBody(LdapConnection.class);
    assertThat(conn).isNotNull();
    assertThat(conn.getId()).isNotNull();
    String ldapConnId = conn.getId();

    LdapConnectionDAO dao = new LdapConnectionDAO();
    LdapConnection raw = dao.getById(conn.getId());
    String oldEncryptedPassword = raw.getSystemPassword();

    assertThat(raw).isNotNull();
    assertThat(raw.getId()).isEqualTo(ldapConnId);
    assertThat(raw.getProtocol()).isEqualTo(conn.getProtocol());
    assertThat(raw.getHostname()).isEqualTo(conn.getHostname());
    assertThat(raw.getPort()).isEqualTo(conn.getPort());
    assertThat(raw.getSearchBase()).isEqualTo(conn.getSearchBase());
    assertThat(raw.getAuthenticationMethod()).isEqualTo(conn.getAuthenticationMethod());
    assertThat(raw.getSaslRealm()).isEqualTo(conn.getSaslRealm());
    assertThat(raw.getSystemUsername()).isEqualTo(conn.getSystemUsername());
    assertThat(raw.getSystemPassword()).isNotEqualTo(LdapService.FAKE_PASSWORD) //
        .isNotEqualTo(conn.getSystemPassword()); // stored encrypted
    assertThat(raw.getConnectionTimeout()).isEqualTo(conn.getConnectionTimeout());
    assertThat(raw.getRetryDelay()).isEqualTo(conn.getRetryDelay());

    // Get by serverId
    response = request.get();
    assertResponseStatus(200, response);
    LdapConnection echo = response.getBody(LdapConnection.class);

    assertThat(echo).isNotNull();
    assertThat(echo.getId()).isEqualTo(ldapConnId);
    assertThat(echo.getProtocol()).isEqualTo(conn.getProtocol());
    assertThat(echo.getHostname()).isEqualTo(conn.getHostname());
    assertThat(echo.getPort()).isEqualTo(conn.getPort());
    assertThat(echo.getSearchBase()).isEqualTo(conn.getSearchBase());
    assertThat(echo.getAuthenticationMethod()).isEqualTo(conn.getAuthenticationMethod());
    assertThat(echo.getSaslRealm()).isEqualTo(conn.getSaslRealm());
    assertThat(echo.getSystemUsername()).isEqualTo(conn.getSystemUsername());
    assertThat(echo.getSystemPassword()).isEqualTo(LdapService.FAKE_PASSWORD);
    assertThat(echo.getConnectionTimeout()).isEqualTo(conn.getConnectionTimeout());
    assertThat(echo.getRetryDelay()).isEqualTo(conn.getRetryDelay());

    LdapProtocol protocol = LdapProtocol.LDAPS;
    String hostname = "hostname";
    int port = 389;
    String searchBase = "searchBase";
    LdapAuthenticationMethod authenticationMethod = LdapAuthenticationMethod.DIGESTMD5;
    String saslRealm = "saslRealm";
    String systemUsername = "systemUsername";
    String systemPassword = "systemPassword";
    int connectionTimeout = 123;
    int retryDelay = 345;

    // Update
    conn.setProtocol(protocol);
    conn.setHostname(hostname);
    conn.setPort(port);
    conn.setSearchBase(searchBase);
    conn.setAuthenticationMethod(authenticationMethod);
    conn.setSaslRealm(saslRealm);
    conn.setSystemUsername(systemUsername);
    conn.setSystemPassword(systemPassword);
    conn.setConnectionTimeout(connectionTimeout);
    conn.setRetryDelay(retryDelay);

    response = request.body(conn).put();
    assertResponseStatus(200, response);
    conn = response.getBody(LdapConnection.class);

    assertThat(conn).isNotNull();
    assertThat(conn.getId()).isEqualTo(ldapConnId);
    assertThat(conn.getProtocol()).isEqualTo(protocol);
    assertThat(conn.getHostname()).isEqualTo(hostname);
    assertThat(conn.getPort()).isEqualTo(port);
    assertThat(conn.getSearchBase()).isEqualTo(searchBase);
    assertThat(conn.getAuthenticationMethod()).isEqualTo(authenticationMethod);
    assertThat(conn.getSaslRealm()).isEqualTo(saslRealm);
    assertThat(conn.getSystemUsername()).isEqualTo(systemUsername);
    assertThat(conn.getSystemPassword()).isEqualTo(LdapService.FAKE_PASSWORD);
    assertThat(conn.getConnectionTimeout()).isEqualTo(connectionTimeout);
    assertThat(conn.getRetryDelay()).isEqualTo(retryDelay);

    raw = dao.getById(conn.getId());
    assertThat(raw.getSystemPassword()).isNotEqualTo(oldEncryptedPassword).isNotEqualTo(conn.getSystemPassword())
        .isNotEqualTo(LdapService.FAKE_PASSWORD);
  }

  @Test
  public void testInconsistentConnectionServerId() throws Exception {
    LdapConnection conn = createLdapConnection("test server");

    HttpResponse response = restRequest().path(LdapResource.CONNECTION_PATH).parameter("wrong").body(conn).put();
    assertResponseStatus(400, response);
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
  public void testServerGetAll_Unlicensed() throws Exception {
    uninstallLicense();
    HttpResponse response = restRequest().get();
    assertResponseStatus(402, response);
  }

  @Test
  public void testTestAnonymousConnection() throws Exception {
    ldapServer.start();

    LdapConnection conn = createLdapConnection("test");
    conn.setPort(ldapServer.getPort());

    HttpResponse response = testConnectionRequest(conn).put();
    assertResponseStatus(200, response);
    LdapConnectionStatus status = response.getBody(LdapConnectionStatus.class);

    assertThat(status.getStatus()).as(status.getMessage()).isEqualTo(LdapConnectionStatus.Status.OK);
  }

  @Test
  public void testTestSimpleConnection() throws Exception {
    ldapServer.setAuthenticationSimple();
    ldapServer.start();

    LdapConnection conn = createLdapConnection("test");
    conn.setPort(ldapServer.getPort());
    conn.setAuthenticationMethod(LdapAuthenticationMethod.SIMPLE);
    conn.setSystemUsername(ldapServer.getSystemUserDN());
    conn.setSystemPassword(ldapServer.getSystemUserPassword());

    HttpResponse response = testConnectionRequest(conn).put();
    assertResponseStatus(200, response);
    LdapConnectionStatus status = response.getBody(LdapConnectionStatus.class);

    assertThat(status.getStatus()).as(status.getMessage()).isEqualTo(LdapConnectionStatus.Status.OK);
  }

  @Test
  public void testTestDigestConnection() throws Exception {
    ldapServer.setAuthenticationSasl(SupportedSaslMechanisms.DIGEST_MD5);
    ldapServer.start();

    LdapConnection conn = createLdapConnection("test");
    conn.setPort(ldapServer.getPort());
    conn.setAuthenticationMethod(LdapAuthenticationMethod.DIGESTMD5);
    conn.setSystemUsername(ldapServer.getSystemUser());
    conn.setSystemPassword(ldapServer.getSystemUserPassword());

    HttpResponse response = testConnectionRequest(conn).put();
    assertResponseStatus(200, response);
    LdapConnectionStatus status = response.getBody(LdapConnectionStatus.class);

    assertThat(status.getStatus()).as(status.getMessage()).isEqualTo(LdapConnectionStatus.Status.OK);
  }

  @Test
  public void testTestCramConnection() throws Exception {
    ldapServer.setAuthenticationSasl(SupportedSaslMechanisms.CRAM_MD5);
    ldapServer.start();

    LdapConnection conn = createLdapConnection("test");
    conn.setPort(ldapServer.getPort());
    conn.setAuthenticationMethod(LdapAuthenticationMethod.CRAMMD5);
    conn.setSystemUsername(ldapServer.getSystemUser());
    conn.setSystemPassword(ldapServer.getSystemUserPassword());

    HttpResponse response = testConnectionRequest(conn).put();
    assertResponseStatus(200, response);
    LdapConnectionStatus status = response.getBody(LdapConnectionStatus.class);

    assertThat(status.getStatus()).as(status.getMessage()).isEqualTo(LdapConnectionStatus.Status.OK);
  }

  @Test
  public void testTestConnection_InvalidUser() throws Exception {
    ldapServer.setAuthenticationSimple();
    ldapServer.start();

    LdapConnection conn = createLdapConnection("test");
    conn.setPort(ldapServer.getPort());
    conn.setAuthenticationMethod(LdapAuthenticationMethod.SIMPLE);
    String systemUserDN = "litter." + ldapServer.getSystemUserDN() + ".garbage";
    conn.setSystemUsername(systemUserDN);
    conn.setSystemPassword(ldapServer.getSystemUserPassword());

    HttpResponse response = testConnectionRequest(conn).put();
    assertResponseStatus(200, response);
    LdapConnectionStatus status = response.getBody(LdapConnectionStatus.class);

    assertThat(status.getStatus()).isEqualTo(LdapConnectionStatus.Status.FAILURE);
    assertThat(status.getMessage()).contains("Invalid authentication");
  }

  @Test
  public void testTestConnection_InvalidPassword() throws Exception {
    ldapServer.setAuthenticationSimple();
    ldapServer.start();

    LdapConnection conn = createLdapConnection("test");
    conn.setPort(ldapServer.getPort());
    conn.setAuthenticationMethod(LdapAuthenticationMethod.SIMPLE);
    conn.setSystemUsername(ldapServer.getSystemUserDN());
    conn.setSystemPassword("garbage.litter");

    HttpResponse response = testConnectionRequest(conn).put();
    assertResponseStatus(200, response);
    LdapConnectionStatus status = response.getBody(LdapConnectionStatus.class);

    assertThat(status.getStatus()).isEqualTo(LdapConnectionStatus.Status.FAILURE);
    assertThat(status.getMessage()).contains("Cannot authenticate user");
  }

  @Test
  public void testTestConnection_InvalidHostname() throws Exception {
    ldapServer.start();

    LdapConnection conn = createLdapConnection("test");
    conn.setHostname("garbage.localhost.litter");
    conn.setPort(ldapServer.getPort());
    conn.setAuthenticationMethod(LdapAuthenticationMethod.NONE);

    HttpResponse response = testConnectionRequest(conn).put();
    assertResponseStatus(200, response);
    LdapConnectionStatus status = response.getBody(LdapConnectionStatus.class);

    assertThat(status.getStatus()).isEqualTo(LdapConnectionStatus.Status.FAILURE);
    assertThat(status.getMessage()).contains("garbage.localhost.litter")
        .containsPattern("(UnknownHost|Communication)Exception");
  }

  @Test
  public void testTestConnection_invalidSaslRealm() throws Exception {
    ldapServer.setAuthenticationSasl(SupportedSaslMechanisms.DIGEST_MD5);
    ldapServer.start();

    LdapConnection conn = createLdapConnection("test");
    conn.setPort(ldapServer.getPort());
    conn.setAuthenticationMethod(LdapAuthenticationMethod.DIGESTMD5);
    conn.setSystemUsername(ldapServer.getSystemUser());
    conn.setSystemPassword(ldapServer.getSystemUserPassword());
    conn.setSaslRealm("invalidrealm");

    HttpResponse response = testConnectionRequest(conn).put();
    assertResponseStatus(200, response);
    LdapConnectionStatus status = response.getBody(LdapConnectionStatus.class);

    assertThat(status.getStatus()).isEqualTo(LdapConnectionStatus.Status.FAILURE);
    assertThat(status.getMessage()).contains("Nonexistent realm: invalidrealm");
  }

  @Test
  @Ignore("execution-order dependent failures")
  public void testTestConnection_ldaps() throws Exception {
    ldapServer.setAuthenticationSimple();
    ldapServer.enableLdaps(getTestResourceFile("/com/sonatype/insight/test/localhost.jks"), "password");
    ldapServer.start();

    LdapConnection conn = createLdapConnection("test");
    conn.setProtocol(LdapProtocol.LDAPS);
    conn.setHostname("localhost");
    conn.setPort(ldapServer.getPort());
    conn.setAuthenticationMethod(LdapAuthenticationMethod.SIMPLE);
    conn.setSystemUsername(ldapServer.getSystemUserDN());
    conn.setSystemPassword(ldapServer.getSystemUserPassword());

    HttpResponse response = testConnectionRequest(conn).put();
    assertResponseStatus(200, response);
    LdapConnectionStatus status = response.getBody(LdapConnectionStatus.class);

    assertThat(status.getStatus()).as(status.getMessage()).isEqualTo(LdapConnectionStatus.Status.OK);
  }

  @Test
  public void testTestUserMapping() throws Exception {
    ldapServer.start();
    ldapServer.loadData("/LdapResourceTest/ldap_users.ldif");

    LdapServer server = tempEntity.newLdapServer("test");

    LdapUserMapping mapping = tempEntity.newLdapUserMapping(server.getId());
    HttpRequest request = restRequest().path(LdapResource.TEST_USER_MAPPING_PATH).parameter(mapping.getServerId())
        .body(mapping);

    HttpResponse response = request.put();
    assertResponseStatus(400, response);

    tempEntity.newLdapConnection(server.getId(), ldapServer.getPort());

    response = request.put();
    assertResponseStatus(200, response);
    LdapUser[] users = response.getBody(LdapUser[].class);
    Arrays.sort(users);
    assertThat(users).hasSize(3);
    assertThat(users[0].getUsername()).isEqualTo("Beta");
    assertThat(users[0].getRealName()).isEqualTo("Beta User");
    assertThat(users[0].getEmail()).isEqualTo("beta.user@company.com");
  }

  @Test
  public void testTestLogin() throws Exception {
    ldapServer.start();
    ldapServer.loadData("/LdapResourceTest/ldap_users.ldif");

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

    tempEntity.newLdapConnection(server.getId(), ldapServer.getPort());

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

  /**
   * CLM-9430, sanity check the classpath of the server contains a recent version of commons-codec as needed by our
   * LDAP client to support passwords hashed using crypt.
   */
  @Test
  public void testTestLogin_UserPasswordAttributeUsingCrypt() throws Exception {
    ldapServer.start();
    ldapServer.loadData("/LdapResourceTest/ldap_users.ldif");

    LdapServer server = tempEntity.newLdapServer("test");
    tempEntity.newLdapConnection(server.getId(), ldapServer.getPort());
    LdapUserMapping mapping = tempEntity.newLdapUserMapping(server.getId());
    mapping.setUserPasswordAttribute("userPassword");
    new LdapUserMappingDAO().update(mapping);

    LdapTestLoginRequest login = new LdapTestLoginRequest();
    login.setUserMapping(mapping);
    login.setUsername("cryptuser");
    login.setPassword("brianf123");

    HttpResponse response = restRequest().path(LdapResource.TEST_LOGIN_PATH).parameter(mapping.getServerId())
        .body(login).put();
    assertResponseStatus(200, response);
    LdapConnectionStatus status = response.getBody(LdapConnectionStatus.class);
    assertThat(status.getStatus()).isEqualTo(LdapConnectionStatus.Status.OK);
  }

  private File getTestResourceFile(String path) throws IOException {
    URL resource = getClass().getResource(path);
    assertThat(resource).as(path).isNotNull(); // sanity check
    File tempFile = temporaryFolder.newFile();
    FileUtils.copyURLToFile(resource, tempFile);
    return tempFile;
  }

  private LdapConnection createLdapConnection(String name) {
    LdapServer server = tempEntity.newLdapServer(name);

    LdapConnection conn = new LdapConnection();
    conn.setServerId(server.getId());
    conn.setProtocol(LdapProtocol.LDAP);
    conn.setHostname("localhost");
    conn.setPort(389);
    conn.setAuthenticationMethod(LdapAuthenticationMethod.NONE);
    conn.setSystemUsername("system");
    conn.setSystemPassword("password");
    return conn;
  }

  private LdapUserMapping newUserMapping(String name) {
    LdapServer server = tempEntity.newLdapServer(name);

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

  private void assertUserMappingEquals(LdapUserMapping expected, LdapUserMapping actual) {
    assertThat(actual.getServerId()).isEqualTo(expected.getServerId());
    assertThat(actual.getUserBaseDN()).isEqualTo(expected.getUserBaseDN());
    assertThat(actual.isUserSubtree()).isEqualTo(expected.isUserSubtree());
    assertThat(actual.getUserObjectClass()).isEqualTo(expected.getUserObjectClass());
    assertThat(actual.getUserFilter()).isEqualTo(expected.getUserFilter());
    assertThat(actual.getUserIDAttribute()).isEqualTo(expected.getUserIDAttribute());
    assertThat(actual.getUserRealNameAttribute()).isEqualTo(expected.getUserRealNameAttribute());
    assertThat(actual.getUserEmailAttribute()).isEqualTo(expected.getUserEmailAttribute());
    assertThat(actual.getUserPasswordAttribute()).isEqualTo(expected.getUserPasswordAttribute());

    assertThat(actual.getGroupMappingType()).isEqualTo(expected.getGroupMappingType());
    assertThat(actual.getGroupBaseDN()).isEqualTo(expected.getGroupBaseDN());
    assertThat(actual.getGroupObjectClass()).isEqualTo(expected.getGroupObjectClass());
    assertThat(actual.getGroupIDAttribute()).isEqualTo(expected.getGroupIDAttribute());
    assertThat(actual.getGroupMemberAttribute()).isEqualTo(expected.getGroupMemberAttribute());
    assertThat(actual.getGroupMemberFormat()).isEqualTo(expected.getGroupMemberFormat());
    assertThat(actual.getUserMemberOfGroupAttribute()).isEqualTo(expected.getUserMemberOfGroupAttribute());
  }
}
