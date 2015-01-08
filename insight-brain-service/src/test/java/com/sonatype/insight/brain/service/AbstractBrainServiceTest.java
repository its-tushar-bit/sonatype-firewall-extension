/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.TestLicenseFingerprinter;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.product.license.ProductLicenseResource;
import com.sonatype.insight.brain.security.CLMShiroModule;
import com.sonatype.insight.license.model.CLMEnforcementPoint;

import org.sonatype.licensing.product.ProductLicenseManager;
import org.sonatype.licensing.product.util.LicenseFingerprinter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Cookie;
import com.ning.http.client.Response;
import com.ning.http.multipart.ByteArrayPartSource;
import com.ning.http.multipart.FilePart;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public abstract class AbstractBrainServiceTest
{
  static {
    System.setProperty("javax.net.ssl.trustStore", "src/test/resources/ssl/server-store");
  }

  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  private static File saasWork = new File("target/mock-saas-work/");

  @Rule
  public TestName testName = new TestName();

  // by default license is always valid, to override, simply uninstall the license
  private static final TestProductLicenseManager licenseManager = new TestProductLicenseManager();

  private static boolean productlicenseWasUninstalled;

  private static final TestLicenseFingerprinter licenseFingerprinter = new TestLicenseFingerprinter();

  private static String savedLicenseFingerprint;

  private static TestCLMServer testCLMServer;

  @Before
  public void initTest() throws Throwable {
    if (testCLMServer != null && isProxyRequiredToReachSaas() != testCLMServer.isProxyRequiredToReachSaas()) {
      testCLMServer.stop();
      testCLMServer = null;
    }

    if (testCLMServer == null) {
      testCLMServer = new TestCLMServer(saasWork, isProxyRequiredToReachSaas(), getBrainModules());
      testCLMServer.start();
    }
  }

  @After
  public void cleanupTest() throws Exception {
    testCLMServer.getInsightServer().reset();

    if (savedLicenseFingerprint != null) {
      licenseFingerprinter.setDummyLicenseFingerprint(savedLicenseFingerprint);
      savedLicenseFingerprint = null;
      installLicense();
    }

    if (licenseManager.wasChanged()) {
      licenseManager.reset();
      installLicense();
    }
    if (productlicenseWasUninstalled) {
      installLicense();
    }
  }

  private List<Module> getBrainModules() {
    List<Module> modules = new ArrayList<>();
    modules.add(new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(ProductLicenseManager.class).toInstance(licenseManager);
        bind(LicenseFingerprinter.class).toInstance(licenseFingerprinter);
      }
    });
    return modules;
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
    String restBaseUrl = getCLMServer().getClientConfiguration().getServerUrl();
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

  protected void setSaasResponseForURI(String uri, Object body, int status) {
    getInsightServer().setResponseForURI(uri, body, status);
  }

  protected void setSaasResponseForURI(String uri, int status, String bodyResource) {
    setSaasResponseForURI(uri, toString(bodyResource), status);
  }

  protected void mockComponentSummary(ComponentIdentifier componentIdentifier, ComponentSummary componentSummary)
      throws Exception
  {
    String uri = UriBuilder.fromPath("rest/component/summary")
        .queryParam("componentIdentifier", toJson(componentIdentifier)).build().toString();
    setSaasResponseForURI(uri, toJson(componentSummary), 200);
  }

  protected void setSecurityAuditLog(String appId, String jsonResource) {
    setAuditLog(appId, "security.json", jsonResource);
  }

  private void setAuditLog(String appId, String jsonFile, String jsonResource) {
    File logFile = new File(getCLMServer().getAuditDir(appId), jsonFile);
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
      if (CLMShiroModule.SESSION_COOKIE_NAME.equals(cookie.getName())) {
        return cookie;
      }
    }

    fail("Missing session cookie");
    return null;
  }

  protected String toJson(Object object) {
    try {
      return objectMapper.writeValueAsString(object);
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  protected <T> T fromJson(Response response, Class<T> type) {
    try {
      return objectMapper.readValue(response.getResponseBody(), type);
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  protected TestInsightBrainServiceRule getCLMServer() {
    return testCLMServer.getCLMServer();
  }

  protected InsightMockServerRule getInsightServer() {
    return testCLMServer.getInsightServer();
  }

  protected TestProductLicenseManager getTestProductLicenseManager() {
    return licenseManager;
  }

  protected String getLicenseFingerprint() {
    return licenseFingerprinter.calculate();
  }

  protected void setLicenseFingerprint(String licenseFingerprint) throws Exception {
    if (savedLicenseFingerprint == null) {
      savedLicenseFingerprint = licenseFingerprinter.calculate();
    }
    licenseFingerprinter.setDummyLicenseFingerprint(licenseFingerprint);
    installLicense();
  }

  protected String installLicense() throws Exception {
    Response response = uploadLicense(null);
    assertResponseStatus(200, response);

    Assert.assertTrue(licenseManager.isValid());

    return response.getResponseBody();
  }

  protected Response installLicense(boolean forceSuccess) throws Exception {
    return uploadLicense(Collections.singletonMap("forceSuccess", Boolean.toString(forceSuccess)));
  }

  protected Response uploadLicense(Map<String, String> queryParams, String username, String password) throws Exception {
    InputStream license = this.getClass().getResourceAsStream("/productlicense/license.lic");
    try {
      AsyncHttpClient.BoundRequestBuilder builder = AuthedRestAccess.getClient().preparePost(
          getProductLicenseServiceURL());
      builder.addBodyPart(new FilePart("file", new ByteArrayPartSource(null, IOUtil.toByteArray(license))));
      if (queryParams != null) {
        for (String key : queryParams.keySet()) {
          builder.addQueryParameter(key, queryParams.get(key));
        }
      }

      Response response;
      if (username == null) {
        response = AuthedRestAccess.execute(builder);
      }
      else {
        response = AuthedRestAccess.execute(builder, username, password);
      }
      productlicenseWasUninstalled = false;
      return response;
    }
    finally {
      IOUtil.close(license);
    }
  }

  private Response uploadLicense(Map<String, String> queryParams) throws Exception {
    return uploadLicense(queryParams, null /* username */, null /* password */);
  }

  private String getProductLicenseServiceURL() {
    return getRestBaseUrl() + ProductLicenseResource.SERVICE_PATH;
  }

  protected void uninstallLicense() throws Exception {
    AuthedRestAccess.delete(getProductLicenseServiceURL());
    productlicenseWasUninstalled = true;

    Assert.assertFalse(licenseManager.isValid());
  }

  protected void setEnforcementPoints(CLMEnforcementPoint... enforcementPoints) throws Exception {
    licenseManager.setEnforcementPoints(enforcementPoints);
    installLicense();
  }

  protected void setApplicationLimit(int applicationLimit) throws Exception {
    licenseManager.setApplicationLimit(applicationLimit);
    installLicense();
  }

  protected void setLicenseProducts(String[] products) throws Exception {
    licenseManager.setProducts(products);
    installLicense();
  }
}
