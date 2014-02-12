/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.db.DataSourceFactory;

import com.google.inject.Module;
import com.ning.http.client.Cookie;
import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.rules.TestName;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public abstract class AbstractBrainServiceTest
{
  static {
    System.setProperty("javax.net.ssl.trustStore", "src/test/resources/ssl/server-store");
  }

  private static File saasWork = new File("target/mock-saas-work/");

  private int insightMockServerPort = PortAllocator.findFreePort(8090);

  @Rule
  public TestName testName = new TestName();

  @Rule
  public InsightMockServerRule insightMockServer = new InsightMockServerRule(insightMockServerPort, saasWork,
      isProxyRequiredToReachSaas());

  @Rule
  public TestInsightBrainServiceRule brain = new TestInsightBrainServiceRule(PortAllocator.findFreePort(8070),
      PortAllocator.findFreePort(8071), getBrainBaseUrl(), "http://localhost:" + insightMockServerPort,
      isProxyRequiredToReachSaas(), addBrainModules());

  @AfterClass
  public static void afterClass() {
    DataSourceFactory.clear_ForTestsOnly();
  }

  /**
   * Returns modules to be added to the test brain server's injector.
   * This method is called before the test brain is initialized. Calling this method after the test brain server is
   * initialized (i.e. from a test method) has no effect.
   * 
   * @since 1.9.1
   */
  protected List<Module> addBrainModules() {
    return null;
  }

  protected String getBrainBaseUrl() {
    return null;
  }

  private boolean isProxyRequiredToReachSaas() {
    return getClass().getName().endsWith("ProxyTest");
  }

  private static File getJsonResponseDirectory() {
    return new File(saasWork, "json");
  }

  private static File getZipResponseDirectory() {
    return new File(saasWork, "zip");
  }

  protected static File getScanResponseFile(final String licenseFingerprint) {
    return new File(getJsonResponseDirectory(), licenseFingerprint + ".json");
  }

  protected static File getReportResponseFile(final String licenseFingerprint, final String scanId) {
    return new File(getZipResponseDirectory(), licenseFingerprint + '-' + scanId + ".zip");
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
    insightMockServer.setResponseForURI(uri, body, status);
  }

  protected void setSaasResponseForURI(String uri, String body, int status) {
    insightMockServer.setResponseForURI(uri, body, status);
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
