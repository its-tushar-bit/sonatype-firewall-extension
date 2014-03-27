/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapConnectionDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.ldap.LdapManager;
import com.sonatype.insight.brain.ldap.LdapUser;
import com.sonatype.insight.brain.ldap.TestLdapServer;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.codehaus.plexus.util.FileUtils;
import org.junit.After;
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

  private LdapServer server;

  @After
  public void deleteLdapServer() {
    if (server != null) {
      serverDao.delete(server);
      server = null;
    }

    Assert.assertEquals(0, serverDao.getAll().size());
  }

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void testServerCRUD() throws Exception {
    // Create
    LdapServer server = createLdapServer("LdapConfigurationResourceTest");

    Response response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(server));
    assertResponseStatus(200, response);
    server = JsonHelpers.fromJson(response.getResponseBody(), LdapServer.class);
    assertNotNull(server);
    assertNotNull(server.getId());
    assertEquals("LdapConfigurationResourceTest", server.getName());
    String ldapServerId = server.getId();

    LdapServer raw = serverDao.getById(server.getId());

    assertNotNull(raw);
    assertEquals(ldapServerId, raw.getId());
    assertEquals(server.getName(), raw.getName());
    assertEquals(NameHelper.normalize(server.getName()), raw.getNameLowercaseNoWhitespace());

    // Get all
    response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(200, response);
    LdapServer[] ldapConfigurations = JsonHelpers.fromJson(response.getResponseBody(), LdapServer[].class);
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

    response = AuthedRestAccess.put(getServiceURL(), JsonHelpers.asJson(server));
    assertResponseStatus(200, response);
    server = JsonHelpers.fromJson(response.getResponseBody(), LdapServer.class);

    assertNotNull(server);
    assertEquals(ldapServerId, ldapServerId);
    assertEquals(name, server.getName());

    raw = serverDao.getById(server.getId());

    // Delete
    response = AuthedRestAccess.delete(getServiceURL() + "/" + ldapServerId);
    assertResponseStatus(204, response);

    assertNull(serverDao.getById(ldapServerId));
  }

  @Test
  public void testUserMappingCRUD() throws Exception {
    final LdapUserMapping orig = newUserMapping("test server");

    // PUT new, a.k.a. "insert"
    Response response = AuthedRestAccess.put(getUsermappingServiceURL(orig.getServerId()), JsonHelpers.asJson(orig));
    assertResponseStatus(200, response);
    LdapUserMapping umap = JsonHelpers.fromJson(response.getResponseBody(), LdapUserMapping.class);
    assertUserMappingEquals(orig, umap);

    // GET
    response = AuthedRestAccess.get(getUsermappingServiceURL(orig.getServerId()));
    assertResponseStatus(200, response);
    umap = JsonHelpers.fromJson(response.getResponseBody(), LdapUserMapping.class);
    assertUserMappingEquals(orig, umap);

    // PUT existing, a.k.a "update"
    umap.setUserEmailAttribute(orig.getUserEmailAttribute() + "changed");
    response = AuthedRestAccess.put(getUsermappingServiceURL(umap.getServerId()), JsonHelpers.asJson(umap));
    assertResponseStatus(200, response);
    umap = JsonHelpers.fromJson(response.getResponseBody(), LdapUserMapping.class);
    Assert.assertEquals(orig.getUserEmailAttribute() + "changed", umap.getUserEmailAttribute());
  }

  @Test
  public void testNewUserMapping() throws Exception {
    server = createLdapServer("test server");
    serverDao.insert(server);

    Response response = AuthedRestAccess.get(getUsermappingServiceURL(server.getId()));
    assertResponseStatus(200, response);
    LdapUserMapping umap = JsonHelpers.fromJson(response.getResponseBody(), LdapUserMapping.class);
    Assert.assertNotNull(umap);
    Assert.assertEquals(server.getId(), umap.getServerId());
  }

  @Test
  public void testNewConnection() throws Exception {
    server = createLdapServer("test server");
    serverDao.insert(server);

    Response response = AuthedRestAccess.get(getConnectionServiceURL(server.getId()));
    assertResponseStatus(200, response);
    LdapConnection conn = JsonHelpers.fromJson(response.getResponseBody(), LdapConnection.class);
    Assert.assertNotNull(conn);
    Assert.assertEquals(server.getId(), conn.getServerId());
  }

  @Test
  public void testConnectionCRUD() throws Exception {
    // Create
    LdapConnection conn = createLdapConnection("LdapConfigurationResourceTest");

    Response response = AuthedRestAccess.put(getConnectionServiceURL(conn.getServerId()), JsonHelpers.asJson(conn));
    assertResponseStatus(200, response);
    conn = JsonHelpers.fromJson(response.getResponseBody(), LdapConnection.class);
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
    response = AuthedRestAccess.get(getConnectionServiceURL(conn.getServerId()));
    assertResponseStatus(200, response);
    LdapConnection echo = JsonHelpers.fromJson(response.getResponseBody(), LdapConnection.class);

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

    response = AuthedRestAccess.put(getConnectionServiceURL(conn.getServerId()), JsonHelpers.asJson(conn));
    assertResponseStatus(200, response);
    conn = JsonHelpers.fromJson(response.getResponseBody(), LdapConnection.class);

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
    LdapConnection conn = createLdapConnection("LdapConfigurationResourceTest");

    Response response = AuthedRestAccess.put(getConnectionServiceURL(conn.getServerId() + "wrong"),
        JsonHelpers.asJson(conn));
    assertResponseStatus(400, response);
  }

  @Test
  public void testAddLdapServer_Unlicensed() throws Exception {
    uninstallLicense();
    LdapServer server = createLdapServer("LdapServerResourceTest");

    Response response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(server));
    assertResponseStatus(402, response);
  }

  @Test
  public void testUpdateLdapServer_Unlicensed() throws Exception {
    LdapServer server = createLdapServer("LdapServerResourceTest");
    Response response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(server));
    assertResponseStatus(200, response);
    server = JsonHelpers.fromJson(response.getResponseBody(), LdapServer.class);

    uninstallLicense();
    response = AuthedRestAccess.put(getServiceURL(), JsonHelpers.asJson(server));
    assertResponseStatus(402, response);

    serverDao.delete(serverDao.getById(server.getId()));
  }

  @Test
  public void testServerGetAll_Unlicensed() throws Exception {
    uninstallLicense();
    Response response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(402, response);
  }

  @Test
  public void testTestAnonymousConnection() throws Exception {
    ldapServer.start();

    LdapConnection conn = createLdapConnection("test");
    conn.setPort(ldapServer.getPort());

    Response response = AuthedRestAccess.put(getTestConnectionServiceUrl(conn.getServerId()), JsonHelpers.asJson(conn));
    assertResponseStatus(200, response);
    LdapConnectionStatus status = JsonHelpers.fromJson(response.getResponseBody(), LdapConnectionStatus.class);

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

    Response response = AuthedRestAccess.put(getTestConnectionServiceUrl(conn.getServerId()), JsonHelpers.asJson(conn));
    assertResponseStatus(200, response);
    LdapConnectionStatus status = JsonHelpers.fromJson(response.getResponseBody(), LdapConnectionStatus.class);

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

    Response response = AuthedRestAccess.put(getTestConnectionServiceUrl(conn.getServerId()), JsonHelpers.asJson(conn));
    assertResponseStatus(200, response);
    LdapConnectionStatus status = JsonHelpers.fromJson(response.getResponseBody(), LdapConnectionStatus.class);

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

    Response response = AuthedRestAccess.put(getTestConnectionServiceUrl(conn.getServerId()), JsonHelpers.asJson(conn));
    assertResponseStatus(200, response);
    LdapConnectionStatus status = JsonHelpers.fromJson(response.getResponseBody(), LdapConnectionStatus.class);

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

    Response response = AuthedRestAccess.put(getTestConnectionServiceUrl(conn.getServerId()), JsonHelpers.asJson(conn));
    assertResponseStatus(200, response);
    LdapConnectionStatus status = JsonHelpers.fromJson(response.getResponseBody(), LdapConnectionStatus.class);

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

    Response response = AuthedRestAccess.put(getTestConnectionServiceUrl(conn.getServerId()), JsonHelpers.asJson(conn));
    assertResponseStatus(200, response);
    LdapConnectionStatus status = JsonHelpers.fromJson(response.getResponseBody(), LdapConnectionStatus.class);

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

    Response response = AuthedRestAccess.put(getTestConnectionServiceUrl(conn.getServerId()), JsonHelpers.asJson(conn));
    assertResponseStatus(200, response);
    LdapConnectionStatus status = JsonHelpers.fromJson(response.getResponseBody(), LdapConnectionStatus.class);

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

    Response response = AuthedRestAccess.put(getTestConnectionServiceUrl(conn.getServerId()), JsonHelpers.asJson(conn));
    assertResponseStatus(200, response);
    LdapConnectionStatus status = JsonHelpers.fromJson(response.getResponseBody(), LdapConnectionStatus.class);

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
      System.setProperty(SYSPROP_SSLTRUSTSTORE, getTestResourceFile("/keystore/insight-testclient.ks")
          .getCanonicalPath());

      LdapConnection conn = createLdapConnection("test");
      conn.setProtocol(LdapProtocol.LDAPS);
      conn.setHostname("localhost");
      conn.setPort(ldapServer.getPort());
      conn.setAuthenticationMethod(LdapAuthenticationMethod.SIMPLE);
      conn.setSystemUsername(ldapServer.getSystemUserDN());
      conn.setSystemPassword(ldapServer.getSystemUserPassword());

      Response response = AuthedRestAccess.put(getTestConnectionServiceUrl(conn.getServerId()),
          JsonHelpers.asJson(conn));
      assertResponseStatus(200, response);
      LdapConnectionStatus status = JsonHelpers.fromJson(response.getResponseBody(), LdapConnectionStatus.class);

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

    server = createLdapServer("test");
    serverDao.insert(server);

    LdapUserMapping mapping = tempEntity.newLdapUserMapping(server.getId());
    String url = getRestUrl(LdapResource.SERVICE_PATH + "/{ldapServerId}/testUserMapping", mapping.getServerId());

    Response response = AuthedRestAccess.put(url, toJson(mapping));
    assertResponseStatus(400, response);

    tempEntity.newLdapConnection(server.getId(), ldapServer.getPort());

    response = AuthedRestAccess.put(url, toJson(mapping));
    assertResponseStatus(200, response);
    LdapUser[] users = fromJson(response, LdapUser[].class);
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

    server = createLdapServer("test");
    serverDao.insert(server);

    LdapUserMapping mapping = tempEntity.newLdapUserMapping(server.getId());
    LdapTestLoginRequest request = new LdapTestLoginRequest();
    request.setUserMapping(mapping);
    request.setUsername("testuser");
    request.setPassword("bad");
    String url = getRestUrl(LdapResource.SERVICE_PATH + "/{ldapServerId}/testLogin", mapping.getServerId());

    Response response = AuthedRestAccess.put(url, toJson(request));
    assertResponseStatus(200, response);
    LdapConnectionStatus status = fromJson(response, LdapConnectionStatus.class);
    assertThat(status.getStatus(), is(LdapConnectionStatus.Status.FAILURE));

    tempEntity.newLdapConnection(server.getId(), ldapServer.getPort());

    response = AuthedRestAccess.put(url, toJson(request));
    assertResponseStatus(200, response);
    status = fromJson(response, LdapConnectionStatus.class);
    assertThat(status.getStatus(), is(LdapConnectionStatus.Status.FAILURE));

    request.setPassword("far2simple");
    response = AuthedRestAccess.put(url, toJson(request));
    assertResponseStatus(200, response);
    status = fromJson(response, LdapConnectionStatus.class);
    assertThat(status.getStatus(), is(LdapConnectionStatus.Status.OK));
  }

  private File getTestResourceFile(String path) throws IOException {
    URL resource = getClass().getResource(path);
    Assert.assertNotNull(resource); // sanity check
    File tempFile = temporaryFolder.newFile();
    FileUtils.copyURLToFile(resource, tempFile);
    return tempFile;
  }

  private String getServiceURL() {
    return getRestBaseUrl() + LdapResource.SERVICE_PATH;
  }

  private String getConnectionServiceURL(String serverId) {
    return getRestBaseUrl() + LdapResource.SERVICE_PATH + "/" + serverId + "/connection";
  }

  private String getUsermappingServiceURL(String serverId) {
    return getRestBaseUrl() + LdapResource.SERVICE_PATH + "/" + serverId + "/userMapping";
  }

  private String getTestConnectionServiceUrl(String serverId) {
    return getRestBaseUrl() + LdapResource.SERVICE_PATH + "/" + serverId + "/testConnection";
  }

  private static LdapServer createLdapServer(String name) {
    LdapServer config = new LdapServer();
    config.setName(name);
    return config;
  }

  private LdapConnection createLdapConnection(String name) {
    if (server == null) {
      server = createLdapServer(name);
      serverDao.insert(server);
    }

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
    if (server == null) {
      server = createLdapServer(name);
      serverDao.insert(server);
    }

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
