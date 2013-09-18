/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapConfigurationDAO;
import com.sonatype.insight.brain.ldap.EmbeddedLdapServer;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.codehaus.plexus.util.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import static com.sonatype.insight.brain.ldap.EmbeddedLdapServer.newEmbeddedLdapServer;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class LdapConfigurationResourceTest
    extends AbstractResourceTest
{

  private EmbeddedLdapServer ldapServer;

  private Set<File> tempFiles = new HashSet<File>();

  @After
  public void stopEmbeddedLdapServer() throws Exception {
    if (ldapServer != null) {
      ldapServer.stop();
      ldapServer = null;
    }
  }

  @After
  public void deleteTemporaryFiles() throws Exception {
    for (File tempFile : tempFiles) {
      if (tempFile.isDirectory()) {
        FileUtils.deleteDirectory(tempFile);
      }
      else {
        Assert.assertTrue(tempFile.delete());
      }
    }
  }

  @Test
  public void testCRUD() throws Exception {
    // Create
    LdapConfiguration config = createLdapConfiguration("LdapConfigurationResourceTest");

    Response response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(config));
    assertResponseStatus(200, response);
    config = JsonHelpers.fromJson(response.getResponseBody(), LdapConfiguration.class);
    assertNotNull(config);
    assertNotNull(config.getId());
    assertEquals("LdapConfigurationResourceTest", config.getName());
    String ldapConfigurationId = config.getId();

    LdapConfigurationDAO dao = new LdapConfigurationDAO();
    LdapConfiguration raw = dao.getById(config.getId());
    String oldEncryptedPassword = raw.getSystemPassword();

    assertNotNull(raw);
    assertEquals(ldapConfigurationId, raw.getId());
    assertEquals(config.getName(), raw.getName());
    assertEquals(NameHelper.normalize(config.getName()), raw.getNameLowercaseNoWhitespace());
    assertEquals(config.getProtocol(), raw.getProtocol());
    assertEquals(config.getHostname(), raw.getHostname());
    assertEquals(config.getPort(), raw.getPort());
    assertEquals(config.getSearchBase(), raw.getSearchBase());
    assertEquals(config.getAuthenticationMethod(), raw.getAuthenticationMethod());
    assertEquals(config.getSaslRealm(), raw.getSaslRealm());
    assertEquals(config.getSystemUsername(), raw.getSystemUsername());
    assertNotEquals(config.getSystemPassword(), raw.getSystemPassword()); // stored encrypted
    assertNotEquals(LdapConfigurationResource.FAKE_PASSWORD, raw.getSystemPassword());
    assertEquals(config.getConnectionTimeout(), raw.getConnectionTimeout());
    assertEquals(config.getRetryDelay(), raw.getRetryDelay());

    // Get by name
    response = AuthedRestAccess.get(getServiceURL() + "/" + config.getName());
    assertResponseStatus(200, response);
    LdapConfiguration echo = JsonHelpers.fromJson(response.getResponseBody(), LdapConfiguration.class);

    assertNotNull(echo);
    assertEquals(ldapConfigurationId, echo.getId());
    assertEquals(config.getName(), echo.getName());
    assertEquals(NameHelper.normalize(config.getName()), echo.getNameLowercaseNoWhitespace());
    assertEquals(config.getProtocol(), echo.getProtocol());
    assertEquals(config.getHostname(), echo.getHostname());
    assertEquals(config.getPort(), echo.getPort());
    assertEquals(config.getSearchBase(), echo.getSearchBase());
    assertEquals(config.getAuthenticationMethod(), echo.getAuthenticationMethod());
    assertEquals(config.getSaslRealm(), echo.getSaslRealm());
    assertEquals(config.getSystemUsername(), echo.getSystemUsername());
    assertEquals(LdapConfigurationResource.FAKE_PASSWORD, echo.getSystemPassword());
    assertEquals(config.getConnectionTimeout(), echo.getConnectionTimeout());
    assertEquals(config.getRetryDelay(), echo.getRetryDelay());

    // Get all
    response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(200, response);
    LdapConfiguration[] LdapConfigurations = JsonHelpers
        .fromJson(response.getResponseBody(), LdapConfiguration[].class);
    assertNotNull(LdapConfigurations);
    assertEquals(1, LdapConfigurations.length);
    echo = LdapConfigurations[0];

    assertNotNull(echo);
    assertEquals(ldapConfigurationId, echo.getId());
    assertEquals(config.getName(), echo.getName());
    assertEquals(NameHelper.normalize(config.getName()), echo.getNameLowercaseNoWhitespace());
    assertEquals(config.getProtocol(), echo.getProtocol());
    assertEquals(config.getHostname(), echo.getHostname());
    assertEquals(config.getPort(), echo.getPort());
    assertEquals(config.getSearchBase(), echo.getSearchBase());
    assertEquals(config.getAuthenticationMethod(), echo.getAuthenticationMethod());
    assertEquals(config.getSaslRealm(), echo.getSaslRealm());
    assertEquals(config.getSystemUsername(), echo.getSystemUsername());
    assertEquals(LdapConfigurationResource.FAKE_PASSWORD, echo.getSystemPassword());
    assertEquals(config.getConnectionTimeout(), echo.getConnectionTimeout());
    assertEquals(config.getRetryDelay(), echo.getRetryDelay());

    String name = "LdapConfigurationResourceTest updated";
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
    config.setName(name);
    config.setProtocol(protocol);
    config.setHostname(hostname);
    config.setPort(port);
    config.setSearchBase(searchBase);
    config.setAuthenticationMethod(authenticationMethod);
    config.setSaslRealm(saslRealm);
    config.setSystemUsername(systemUsername);
    config.setSystemPassword(systemPassword);
    config.setConnectionTimeout(connectionTimeout);
    config.setRetryDelay(retryDelay);

    response = AuthedRestAccess.put(getServiceURL(), JsonHelpers.asJson(config));
    assertResponseStatus(200, response);
    config = JsonHelpers.fromJson(response.getResponseBody(), LdapConfiguration.class);

    assertNotNull(config);
    assertEquals(ldapConfigurationId, ldapConfigurationId);
    assertEquals(name, config.getName());
    assertEquals(NameHelper.normalize(name), config.getNameLowercaseNoWhitespace());
    assertEquals(protocol, config.getProtocol());
    assertEquals(hostname, config.getHostname());
    assertEquals(port, config.getPort());
    assertEquals(searchBase, config.getSearchBase());
    assertEquals(authenticationMethod, config.getAuthenticationMethod());
    assertEquals(saslRealm, config.getSaslRealm());
    assertEquals(systemUsername, config.getSystemUsername());
    assertEquals(LdapConfigurationResource.FAKE_PASSWORD, config.getSystemPassword());
    assertEquals(connectionTimeout, config.getConnectionTimeout());
    assertEquals(retryDelay, config.getRetryDelay());

    raw = dao.getById(config.getId());
    assertNotEquals(oldEncryptedPassword, raw.getSystemPassword());
    assertNotEquals(config.getSystemPassword(), raw.getSystemPassword());
    assertNotEquals(LdapConfigurationResource.FAKE_PASSWORD, raw.getSystemPassword());

    // Delete
    response = AuthedRestAccess.delete(getServiceURL() + "/" + ldapConfigurationId);
    assertResponseStatus(204, response);

    assertNull(new LdapConfigurationDAO().getById(ldapConfigurationId));
  }

  @Test
  public void testAddLdapConfiguration_Unlicensed() throws Exception {
    uninstallLicense();
    LdapConfiguration config = createLdapConfiguration("LdapConfigurationResourceTest");

    Response response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(config));
    assertResponseStatus(402, response);
  }

  @Test
  public void testUpdateLdapConfiguration_Unlicensed() throws Exception {
    LdapConfiguration config = createLdapConfiguration("LdapConfigurationResourceTest");
    Response response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(config));
    assertResponseStatus(200, response);
    uninstallLicense();
    response = AuthedRestAccess.put(getServiceURL(), JsonHelpers.asJson(config));
    assertResponseStatus(402, response);

    new LdapConfigurationDAO().delete(config);
  }

  @Test
  public void testGetAll_Unlicensed() throws Exception {
    uninstallLicense();
    Response response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(402, response);
  }

  @Test
  public void testTestAnonymousConnection() throws Exception {
    ldapServer = newEmbeddedLdapServer();
    ldapServer.start();

    LdapConfiguration config = new LdapConfiguration();
    config.setProtocol(LdapProtocol.LDAP);
    config.setHostname("localhost");
    config.setPort(ldapServer.getPort());
    config.setAuthenticationMethod(LdapAuthenticationMethod.NONE);

    Response response = AuthedRestAccess.put(getServiceURL() + "/test", JsonHelpers.asJson(config));
    assertResponseStatus(200, response);
    LdapConnectionStatus status = JsonHelpers.fromJson(response.getResponseBody(), LdapConnectionStatus.class);

    Assert.assertEquals(LdapConnectionStatus.Status.OK, status.getStatus());
  }

  @Test
  public void testTestSimpleConnection() throws Exception {
    ldapServer = newEmbeddedLdapServer();
    ldapServer.setAuthenticationSimple();
    ldapServer.start();

    LdapConfiguration config = new LdapConfiguration();
    config.setProtocol(LdapProtocol.LDAP);
    config.setHostname("localhost");
    config.setPort(ldapServer.getPort());
    config.setAuthenticationMethod(LdapAuthenticationMethod.SIMPLE);
    config.setSystemUsername(ldapServer.getSystemUserDN());
    config.setSystemPassword(ldapServer.getSystemUserPassword());

    Response response = AuthedRestAccess.put(getServiceURL() + "/test", JsonHelpers.asJson(config));
    assertResponseStatus(200, response);
    LdapConnectionStatus status = JsonHelpers.fromJson(response.getResponseBody(), LdapConnectionStatus.class);

    Assert.assertEquals(LdapConnectionStatus.Status.OK, status.getStatus());
  }

  @Test
  public void testTestDigestConnection() throws Exception {
    ldapServer = newEmbeddedLdapServer();
    ldapServer.setAuthenticationSasl(SupportedSaslMechanisms.DIGEST_MD5);
    ldapServer.start();

    LdapConfiguration config = new LdapConfiguration();
    config.setProtocol(LdapProtocol.LDAP);
    config.setHostname("localhost");
    config.setPort(ldapServer.getPort());
    config.setAuthenticationMethod(LdapAuthenticationMethod.DIGESTMD5);
    config.setSystemUsername(ldapServer.getSystemUser());
    config.setSystemPassword(ldapServer.getSystemUserPassword());

    Response response = AuthedRestAccess.put(getServiceURL() + "/test", JsonHelpers.asJson(config));
    assertResponseStatus(200, response);
    LdapConnectionStatus status = JsonHelpers.fromJson(response.getResponseBody(), LdapConnectionStatus.class);

    Assert.assertEquals(LdapConnectionStatus.Status.OK, status.getStatus());
  }

  @Test
  public void testTestCramConnection() throws Exception {
    ldapServer = newEmbeddedLdapServer();
    ldapServer.setAuthenticationSasl(SupportedSaslMechanisms.CRAM_MD5);
    ldapServer.start();

    LdapConfiguration config = new LdapConfiguration();
    config.setProtocol(LdapProtocol.LDAP);
    config.setHostname("localhost");
    config.setPort(ldapServer.getPort());
    config.setAuthenticationMethod(LdapAuthenticationMethod.CRAMMD5);
    config.setSystemUsername(ldapServer.getSystemUser());
    config.setSystemPassword(ldapServer.getSystemUserPassword());

    Response response = AuthedRestAccess.put(getServiceURL() + "/test", JsonHelpers.asJson(config));
    assertResponseStatus(200, response);
    LdapConnectionStatus status = JsonHelpers.fromJson(response.getResponseBody(), LdapConnectionStatus.class);

    Assert.assertEquals(LdapConnectionStatus.Status.OK, status.getStatus());
  }

  @Test
  public void testTestConnection_InvalidUser() throws Exception {
    ldapServer = newEmbeddedLdapServer();
    ldapServer.setAuthenticationSimple();
    ldapServer.start();

    LdapConfiguration config = new LdapConfiguration();
    config.setProtocol(LdapProtocol.LDAP);
    config.setHostname("localhost");
    config.setPort(ldapServer.getPort());
    config.setAuthenticationMethod(LdapAuthenticationMethod.SIMPLE);
    String systemUserDN = "litter." + ldapServer.getSystemUserDN() + ".garbage";
    config.setSystemUsername(systemUserDN);
    config.setSystemPassword(ldapServer.getSystemUserPassword());

    Response response = AuthedRestAccess.put(getServiceURL() + "/test", JsonHelpers.asJson(config));
    assertResponseStatus(200, response);
    LdapConnectionStatus status = JsonHelpers.fromJson(response.getResponseBody(), LdapConnectionStatus.class);

    Assert.assertEquals(LdapConnectionStatus.Status.FAILURE, status.getStatus());
    assertThat(status.getMessage(), allOf(containsString("Incorrect DN"), containsString(systemUserDN)));
  }

  @Test
  public void testTestConnection_InvalidPassword() throws Exception {
    ldapServer = newEmbeddedLdapServer();
    ldapServer.setAuthenticationSimple();
    ldapServer.start();

    LdapConfiguration config = new LdapConfiguration();
    config.setProtocol(LdapProtocol.LDAP);
    config.setHostname("localhost");
    config.setPort(ldapServer.getPort());
    config.setAuthenticationMethod(LdapAuthenticationMethod.SIMPLE);
    config.setSystemUsername(ldapServer.getSystemUserDN());
    config.setSystemPassword("garbage.litter");

    Response response = AuthedRestAccess.put(getServiceURL() + "/test", JsonHelpers.asJson(config));
    assertResponseStatus(200, response);
    LdapConnectionStatus status = JsonHelpers.fromJson(response.getResponseBody(), LdapConnectionStatus.class);

    Assert.assertEquals(LdapConnectionStatus.Status.FAILURE, status.getStatus());
    assertThat(status.getMessage(), containsString("Cannot authenticate user"));
  }

  @Test
  public void testTestConnection_InvalidHostname() throws Exception {
    ldapServer = newEmbeddedLdapServer();
    ldapServer.start();

    LdapConfiguration config = new LdapConfiguration();
    config.setProtocol(LdapProtocol.LDAP);
    config.setHostname("garbage.localhost.litter");
    config.setPort(ldapServer.getPort());
    config.setAuthenticationMethod(LdapAuthenticationMethod.NONE);

    Response response = AuthedRestAccess.put(getServiceURL() + "/test", JsonHelpers.asJson(config));
    assertResponseStatus(200, response);
    LdapConnectionStatus status = JsonHelpers.fromJson(response.getResponseBody(), LdapConnectionStatus.class);

    Assert.assertEquals(LdapConnectionStatus.Status.FAILURE, status.getStatus());
    assertThat(status.getMessage(),
        allOf(containsString("UnknownHostException"), containsString("garbage.localhost.litter")));
  }

  @Test
  public void testTestConnection_invalidSaslRealm() throws Exception {
    ldapServer = newEmbeddedLdapServer();
    ldapServer.setAuthenticationSasl(SupportedSaslMechanisms.DIGEST_MD5);
    ldapServer.start();

    LdapConfiguration config = new LdapConfiguration();
    config.setProtocol(LdapProtocol.LDAP);
    config.setHostname("localhost");
    config.setPort(ldapServer.getPort());
    config.setAuthenticationMethod(LdapAuthenticationMethod.DIGESTMD5);
    config.setSystemUsername(ldapServer.getSystemUser());
    config.setSystemPassword(ldapServer.getSystemUserPassword());
    config.setSaslRealm("invalidrealm");

    Response response = AuthedRestAccess.put(getServiceURL() + "/test", JsonHelpers.asJson(config));
    assertResponseStatus(200, response);
    LdapConnectionStatus status = JsonHelpers.fromJson(response.getResponseBody(), LdapConnectionStatus.class);

    Assert.assertEquals(LdapConnectionStatus.Status.FAILURE, status.getStatus());
    assertThat(status.getMessage(), containsString("Nonexistent realm: invalidrealm"));
  }

  @Test
  public void testTestConnection_ldaps() throws Exception {
    ldapServer = newEmbeddedLdapServer();
    ldapServer.setAuthenticationSimple();
    ldapServer.enableLdaps(getTestResourceFile("/keystore/insight-test.ks"), "secret");
    ldapServer.start();

    System.setProperty("javax.net.ssl.trustStore", getTestResourceFile("/keystore/insight-testclient.ks")
        .getCanonicalPath());

    try {
      LdapConfiguration config = new LdapConfiguration();
      config.setProtocol(LdapProtocol.LDAPS);
      config.setHostname("localhost");
      config.setPort(ldapServer.getPort());
      config.setAuthenticationMethod(LdapAuthenticationMethod.SIMPLE);
      config.setSystemUsername(ldapServer.getSystemUserDN());
      config.setSystemPassword(ldapServer.getSystemUserPassword());

      Response response = AuthedRestAccess.put(getServiceURL() + "/test", JsonHelpers.asJson(config));
      assertResponseStatus(200, response);
      LdapConnectionStatus status = JsonHelpers.fromJson(response.getResponseBody(), LdapConnectionStatus.class);

      Assert.assertEquals(LdapConnectionStatus.Status.OK, status.getStatus());
    }
    finally {
      System.getProperties().remove("javax.net.ssl.trustStore");
    }
  }

  private File getTestResourceFile(String path) throws IOException {
    URL resource = getClass().getResource(path);
    Assert.assertNotNull(resource); // sanity check
    File tempFile = File.createTempFile("testresource", ".tmp");
    tempFiles.add(tempFile);
    FileUtils.copyURLToFile(resource, tempFile);
    return tempFile;
  }

  private String getServiceURL() {
    return getRestBaseUrl() + LdapConfigurationResource.SERVICE_PATH;
  }

  private static LdapConfiguration createLdapConfiguration(String name) {
    LdapConfiguration config = new LdapConfiguration();
    config.setName(name);
    config.setProtocol(LdapProtocol.LDAP);
    config.setHostname("localhost");
    config.setPort(389);
    config.setAuthenticationMethod(LdapAuthenticationMethod.NONE);
    config.setSystemUsername("system");
    config.setSystemPassword("password");
    return config;
  }
}
