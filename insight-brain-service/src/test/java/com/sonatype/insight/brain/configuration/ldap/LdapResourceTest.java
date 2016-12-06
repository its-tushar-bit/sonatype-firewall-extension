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
import com.sonatype.insight.brain.ldap.LdapManager;
import com.sonatype.insight.brain.ldap.LdapUser;
import com.sonatype.insight.brain.ldap.TestLdapServer;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class LdapResourceTest
    extends AbstractResourceTest
{

  private static final String SYSPROP_SSLTRUSTSTORE = "javax.net.ssl.trustStore";

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
    assertNotNull(server);
    assertNotNull(server.getId());
    assertEquals("test server", server.getName());
    String ldapServerId = server.getId();

    LdapServer raw = serverDao.getById(server.getId());

    assertNotNull(raw);
    assertEquals(ldapServerId, raw.getId());
    assertEquals(server.getName(), raw.getName());
    assertEquals(NameHelper.normalize(server.getName()), raw.getNameLowercaseNoWhitespace());

    // Get all
    response = restRequest().get();
    assertResponseStatus(200, response);
    LdapServer[] ldapConfigurations = response.getBody(LdapServer[].class);
    assertNotNull(ldapConfigurations);
    assertEquals(1, ldapConfigurations.length);
    LdapServer echo = ldapConfigurations[0];

    assertNotNull(echo);
    assertEquals(ldapServerId, echo.getId());
    assertEquals(server.getName(), echo.getName());
    assertEquals(NameHelper.normalize(server.getName()), echo.getNameLowercaseNoWhitespace());

    String name = "LdapConfigurationResourceTest updated";

    // Update
    server.setName(name);

    response = restRequest().body(server).put();
    assertResponseStatus(200, response);
    server = response.getBody(LdapServer.class);

    assertNotNull(server);
    assertEquals(ldapServerId, server.getId());
    assertEquals(name, server.getName());

    raw = serverDao.getById(server.getId());
    assertNotNull(raw);
    assertEquals(ldapServerId, raw.getId());
    assertEquals(name, raw.getName());
    assertEquals(NameHelper.normalize(name), raw.getNameLowercaseNoWhitespace());

    // Delete
    response = restRequest().path(ldapServerId).delete();
    assertResponseStatus(204, response);

    assertNull(serverDao.getById(ldapServerId));
  }

  @Test
  public void testUpdatePriority() throws Exception {
    LdapServer server1 = tempEntity.newLdapServer("server1");
    LdapServer server2 = tempEntity.newLdapServer("server2");
    HttpResponse response = restRequest().path(LdapResource.PRIORITY_PATH)
        .body(Arrays.asList(server2.getId(), server1.getId())).put();
    assertThat(serverDao.getById(server2.getId()).getPriority(), is(1));
    assertThat(serverDao.getById(server1.getId()).getPriority(), is(2));
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
    Assert.assertEquals(orig.getUserEmailAttribute() + "changed", umap.getUserEmailAttribute());
  }

  @Test
  public void testNewUserMapping() throws Exception {
    LdapServer server = tempEntity.newLdapServer("test server");

    HttpResponse response = restRequest().path(LdapResource.USER_MAPPING_PATH).parameter(server.getId()).get();
    assertResponseStatus(200, response);
    LdapUserMapping umap = response.getBody(LdapUserMapping.class);
    Assert.assertNotNull(umap);
    Assert.assertEquals(server.getId(), umap.getServerId());
  }

  @Test
  public void testNewConnection() throws Exception {
    LdapServer server = tempEntity.newLdapServer("test server");

    HttpResponse response = restRequest().path(LdapResource.CONNECTION_PATH).parameter(server.getId()).get();
    assertResponseStatus(200, response);
    LdapConnection conn = response.getBody(LdapConnection.class);
    Assert.assertNotNull(conn);
    Assert.assertEquals(server.getId(), conn.getServerId());
  }

  @Test
  public void testConnectionCRUD() throws Exception {
    // Create
    LdapConnection conn = createLdapConnection("test server");
    HttpRequest request = restRequest().path(LdapResource.CONNECTION_PATH).parameter(conn.getServerId());

    HttpResponse response = request.body(conn).put();
    assertResponseStatus(200, response);
    conn = response.getBody(LdapConnection.class);
    assertNotNull(conn);
    assertNotNull(conn.getId());
    String ldapConnId = conn.getId();

    LdapConnectionDAO dao = new LdapConnectionDAO();
    LdapConnection raw = dao.getById(conn.getId());
    String oldEncryptedPassword = raw.getSystemPassword();

    assertNotNull(raw);
    assertEquals(ldapConnId, raw.getId());
    assertEquals(conn.getProtocol(), raw.getProtocol());
    assertEquals(conn.getHostname(), raw.getHostname());
    assertEquals(conn.getPort(), raw.getPort());
    assertEquals(conn.getSearchBase(), raw.getSearchBase());
    assertEquals(conn.getAuthenticationMethod(), raw.getAuthenticationMethod());
    assertEquals(conn.getSaslRealm(), raw.getSaslRealm());
    assertEquals(conn.getSystemUsername(), raw.getSystemUsername());
    assertNotEquals(conn.getSystemPassword(), raw.getSystemPassword()); // stored encrypted
    assertNotEquals(LdapManager.FAKE_PASSWORD, raw.getSystemPassword());
    assertEquals(conn.getConnectionTimeout(), raw.getConnectionTimeout());
    assertEquals(conn.getRetryDelay(), raw.getRetryDelay());

    // Get by serverId
    response = request.get();
    assertResponseStatus(200, response);
    LdapConnection echo = response.getBody(LdapConnection.class);

    assertNotNull(echo);
    assertEquals(ldapConnId, echo.getId());
    assertEquals(conn.getProtocol(), echo.getProtocol());
    assertEquals(conn.getHostname(), echo.getHostname());
    assertEquals(conn.getPort(), echo.getPort());
    assertEquals(conn.getSearchBase(), echo.getSearchBase());
    assertEquals(conn.getAuthenticationMethod(), echo.getAuthenticationMethod());
    assertEquals(conn.getSaslRealm(), echo.getSaslRealm());
    assertEquals(conn.getSystemUsername(), echo.getSystemUsername());
    assertEquals(LdapManager.FAKE_PASSWORD, echo.getSystemPassword());
    assertEquals(conn.getConnectionTimeout(), echo.getConnectionTimeout());
    assertEquals(conn.getRetryDelay(), echo.getRetryDelay());

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

    assertNotNull(conn);
    assertEquals(ldapConnId, ldapConnId);
    assertEquals(protocol, conn.getProtocol());
    assertEquals(hostname, conn.getHostname());
    assertEquals(port, conn.getPort());
    assertEquals(searchBase, conn.getSearchBase());
    assertEquals(authenticationMethod, conn.getAuthenticationMethod());
    assertEquals(saslRealm, conn.getSaslRealm());
    assertEquals(systemUsername, conn.getSystemUsername());
    assertEquals(LdapManager.FAKE_PASSWORD, conn.getSystemPassword());
    assertEquals(connectionTimeout, conn.getConnectionTimeout());
    assertEquals(retryDelay, conn.getRetryDelay());

    raw = dao.getById(conn.getId());
    assertNotEquals(oldEncryptedPassword, raw.getSystemPassword());
    assertNotEquals(conn.getSystemPassword(), raw.getSystemPassword());
    assertNotEquals(LdapManager.FAKE_PASSWORD, raw.getSystemPassword());
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

    Assert.assertEquals(LdapConnectionStatus.Status.OK, status.getStatus());
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

    Assert.assertEquals(LdapConnectionStatus.Status.OK, status.getStatus());
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

    Assert.assertEquals(LdapConnectionStatus.Status.OK, status.getStatus());
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

    Assert.assertEquals(LdapConnectionStatus.Status.OK, status.getStatus());
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

    Assert.assertEquals(LdapConnectionStatus.Status.FAILURE, status.getStatus());
    assertThat(status.getMessage(), allOf(containsString("Incorrect DN"), containsString(systemUserDN)));
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

    Assert.assertEquals(LdapConnectionStatus.Status.FAILURE, status.getStatus());
    assertThat(status.getMessage(), containsString("Cannot authenticate user"));
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

    Assert.assertEquals(LdapConnectionStatus.Status.FAILURE, status.getStatus());
    assertThat(status.getMessage(),
        allOf(anyOf(containsString("UnknownHostException"), containsString("CommunicationException")),
            containsString("garbage.localhost.litter")));
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

    Assert.assertEquals(LdapConnectionStatus.Status.FAILURE, status.getStatus());
    assertThat(status.getMessage(), containsString("Nonexistent realm: invalidrealm"));
  }

  @Test
  public void testTestConnection_ldaps() throws Exception {
    ldapServer.setAuthenticationSimple();
    ldapServer.enableLdaps(getTestResourceFile("/keystore/insight-test.ks"), "secret");
    ldapServer.start();

    String origTruststore = System.getProperty(SYSPROP_SSLTRUSTSTORE);
    try {
      System.setProperty(SYSPROP_SSLTRUSTSTORE,
          getTestResourceFile("/keystore/insight-testclient.ks").getCanonicalPath());

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

      Assert.assertEquals(LdapConnectionStatus.Status.OK, status.getStatus());
    }
    finally {
      if (origTruststore != null) {
        System.setProperty(SYSPROP_SSLTRUSTSTORE, origTruststore);
      }
      else {
        System.getProperties().remove(SYSPROP_SSLTRUSTSTORE);
      }
    }
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
    assertThat(users.length, is(2));
    assertThat(users[0].getUsername(), is("Beta"));
    assertThat(users[0].getRealName(), is("Beta User"));
    assertThat(users[0].getEmail(), is("beta.user@company.com"));
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
    assertThat(status.getStatus(), is(LdapConnectionStatus.Status.FAILURE));

    tempEntity.newLdapConnection(server.getId(), ldapServer.getPort());

    response = request.body(login).put();
    assertResponseStatus(200, response);
    status = response.getBody(LdapConnectionStatus.class);
    assertThat(status.getStatus(), is(LdapConnectionStatus.Status.FAILURE));

    login.setPassword("far2simple");
    response = request.body(login).put();
    assertResponseStatus(200, response);
    status = response.getBody(LdapConnectionStatus.class);
    assertThat(status.getStatus(), is(LdapConnectionStatus.Status.OK));
  }

  private File getTestResourceFile(String path) throws IOException {
    URL resource = getClass().getResource(path);
    Assert.assertNotNull(resource); // sanity check
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
    Assert.assertEquals(expected.getServerId(), actual.getServerId());
    Assert.assertEquals(expected.getUserBaseDN(), actual.getUserBaseDN());
    Assert.assertEquals(expected.isUserSubtree(), actual.isUserSubtree());
    Assert.assertEquals(expected.getUserObjectClass(), actual.getUserObjectClass());
    Assert.assertEquals(expected.getUserFilter(), actual.getUserFilter());
    Assert.assertEquals(expected.getUserIDAttribute(), actual.getUserIDAttribute());
    Assert.assertEquals(expected.getUserRealNameAttribute(), actual.getUserRealNameAttribute());
    Assert.assertEquals(expected.getUserEmailAttribute(), actual.getUserEmailAttribute());
    Assert.assertEquals(expected.getUserPasswordAttribute(), actual.getUserPasswordAttribute());

    Assert.assertEquals(expected.getGroupMappingType(), actual.getGroupMappingType());
    Assert.assertEquals(expected.getGroupBaseDN(), actual.getGroupBaseDN());
    Assert.assertEquals(expected.getGroupObjectClass(), actual.getGroupObjectClass());
    Assert.assertEquals(expected.getGroupIDAttribute(), actual.getGroupIDAttribute());
    Assert.assertEquals(expected.getGroupMemberAttribute(), actual.getGroupMemberAttribute());
    Assert.assertEquals(expected.getGroupMemberFormat(), actual.getGroupMemberFormat());
    Assert.assertEquals(expected.getUserMemberOfGroupAttribute(), actual.getUserMemberOfGroupAttribute());

  }
}
