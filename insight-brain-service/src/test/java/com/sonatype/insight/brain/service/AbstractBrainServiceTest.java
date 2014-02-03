/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

import javax.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.mock.InsightMockServer;

import com.ning.http.client.Cookie;
import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public abstract class AbstractBrainServiceTest
{
  static {
    System.setProperty("javax.net.ssl.trustStore", "src/test/resources/ssl/server-store");
  }

  private static final Logger log = LoggerFactory.getLogger(AbstractBrainServiceTest.class);

  private static int saasPort = findFreePort(8090);

  private static int brainPort = findFreePort(8070);

  private static int brainAdminPort = findFreePort(8071);

  private static File saasWork = new File("target/mock-saas-work/");

  protected InsightMockServer saas;

  protected TestInsightBrainService brain;

  @Rule
  public TestName testName = new TestName();

  @AfterClass
  public static void afterClass() {
    DataSourceFactory.clear_ForTestsOnly();
  }

  @Before
  public void startService() throws Exception {
    long start = System.currentTimeMillis();

    if (saas == null) {
      log.debug("Starting InsightMockServer on port {}", saasPort);
      saas = new InsightMockServer();
      saas.setHttpPort(saasPort);
      saas.setJsonResponseDirectory(getJsonResponseDirectory());
      saas.setZipResponseDirectory(getZipResponseDirectory());
      if (isProxyRequiredToReachSaas()) {
        saas.setKeyStore(System.getProperty("javax.net.ssl.trustStore"), "server-pwd");
        saas.setProxyAuthentication("proxyuser", "proxypass");
      }
      configureSaas(saas);
      saas.start();
    }
    log.debug("Started InsightMockServer in {}", System.currentTimeMillis() - start);

    start = System.currentTimeMillis();
    if (brain == null) {
      log.debug("Starting TestInsightBrainService on port {}, admin port {}", brainPort, brainAdminPort);
      brain = new TestInsightBrainService();
      brain.setHttpPort(brainPort);
      brain.setHttpAdminPort(brainAdminPort);
      brain.setSaasAddress(saas.getHttpUrl());
      if (isProxyRequiredToReachSaas()) {
        brain.setProxyConfig("127.0.0.1", saasPort, "proxyuser", "proxypass");
      }
      configureBrain(brain);
      brain.start();
    }

    log.debug("Started TestInsightBrainService in {}", System.currentTimeMillis() - start);
  }

  protected void configureBrain(final TestInsightBrainService brain) {
  }

  protected void configureSaas(final InsightMockServer saas) {
    // hook for sub classes
  }

  protected boolean isProxyRequiredToReachSaas() {
    return getClass().getName().endsWith("ProxyTest");
  }

  @After
  public void stopService() throws Exception {
    long start = System.currentTimeMillis();

    if (brain != null) {
      brain.stop();
      brain = null;
    }
    if (saas != null) {
      saas.stop();
      saas = null;
    }

    log.debug("Stopped test servers in {}", System.currentTimeMillis() - start);
  }

  protected static File getJsonResponseDirectory() {
    return new File(saasWork, "json");
  }

  protected static File getZipResponseDirectory() {
    return new File(saasWork, "zip");
  }

  protected static File getScanResponseFile(final String licenseFingerprint) {
    return new File(getJsonResponseDirectory(), licenseFingerprint + ".json");
  }

  protected static File getReportResponseFile(final String licenseFingerprint, final String scanId) {
    return new File(getZipResponseDirectory(), licenseFingerprint + '-' + scanId + ".zip");
  }

  protected static int findFreePort(final int defaultPort) {
    int port = defaultPort;
    ServerSocket socket = null;
    try {
      socket = new ServerSocket(0);
      port = socket.getLocalPort();
    }
    catch (final IOException e) {
      e.printStackTrace();
    }
    finally {
      if (socket != null) {
        try {
          socket.close();
        }
        catch (final IOException e) {
          e.printStackTrace();
        }
      }
    }
    return port;
  }

  protected String getRestBaseUrl() {
    String restBaseUrl = brain.getClientConfiguration().getServerUrl();
    if (!restBaseUrl.endsWith("/")) {
      restBaseUrl = restBaseUrl + "/";
    }
    return restBaseUrl;
  }

  private String expandRestUrl(String templateUrl, Object... paramValues) {
    return UriBuilder.fromPath(templateUrl).build(paramValues).toString();
  }

  protected String getRestUrl(String templateUrl, Object... paramValues) {
    return getRestBaseUrl() + expandRestUrl(templateUrl, paramValues);
  }

  protected void setSaasResponseForURI(String uri, int status, Object body) {
    saas.setResponseForURI(uri, body, status);
  }

  protected void setSaasResponseForURI(String uri, String body, int status) {
    saas.setResponseForURI(uri, body, status);
  }

  protected void setSaasResponseForURI(String uri, int status, String bodyResource) {
    setSaasResponseForURI(uri, toString(bodyResource), status);
  }

  protected void setSecurityAuditLog(String appId, String jsonResource) {
    setAuditLog(appId, "security.json", jsonResource);
  }

  private void setAuditLog(String appId, String jsonFile, String jsonResource) {
    File logFile = new File(brain.getAuditDir(appId), jsonFile);
    logFile.getAbsoluteFile().getParentFile().mkdirs();
    try {
      FileUtils.fileWrite(logFile, "UTF-8", toString(jsonResource));
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private String toString(String resource) {
    try {
      return IOUtil.toString(getClass().getResourceAsStream(resource), "UTF-8");
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  protected static void assertResponseStatus(final int expectedStatus, final Response response) throws IOException {
    final int actualStatus = response.getStatusCode();
    assertEquals(
        "URI:" + response.getUri() + ", StatusText:" + response.getStatusText() + ", ResponseBody:"
            + response.getResponseBody(), expectedStatus, actualStatus);
  }

  protected Cookie extractSessionCookie(final Response response) {
    for (final Cookie cookie : response.getCookies()) {
      if ("JSESSIONID".equals(cookie.getName())) {
        return cookie;
      }
    }

    fail("Missing session cookie");
    return null;
  }

  protected String toJson(Object object) {
    try {
      return JsonHelpers.asJson(object);
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  protected <T> T fromJson(Response response, Class<T> type) {
    try {
      return JsonHelpers.fromJson(response.getResponseBody(), type);
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
