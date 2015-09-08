/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.TestLicenseFingerprinter;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.notifications.HdsProductNotificationService;
import com.sonatype.insight.brain.product.license.ProductLicenseResource;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.license.model.CLMEnforcementPoint;

import org.sonatype.licensing.product.ProductLicenseManager;
import org.sonatype.licensing.product.util.LicenseFingerprinter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public abstract class AbstractBrainServiceTest
{
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.METHOD })
  public @interface ManualServerInit
  {
  }

  static {
    System.setProperty("javax.net.ssl.trustStore", "src/test/resources/ssl/server-store");
  }

  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Rule
  public TestName testName = new TestName();

  // by default license is always valid, to override, simply uninstall the license
  private static final TestProductLicenseManager licenseManager = new TestProductLicenseManager();

  private static boolean productlicenseWasUninstalled;

  private static final TestLicenseFingerprinter licenseFingerprinter = new TestLicenseFingerprinter();

  private static String savedLicenseFingerprint;

  private static TestCLMServer testCLMServer;

  // The mock service that would normally talk to HDS for product notifications
  protected static HdsProductNotificationService mockHdsProductNotificationService;

  private LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();

  @Before
  public void initTest() throws Exception {
    if (!isTestUsingManualServerInit()) {
      initServer(null);
    }
    // Make sure the default LTGs are created
    licenseThreatGroupDAO.createDefaultLicenseThreatGroups();
  }

  private boolean isTestUsingManualServerInit() throws Exception {
    String testMethod = testName.getMethodName();
    int paramStart = testMethod.indexOf('[');
    if (paramStart >= 0) {
      testMethod = testMethod.substring(0, paramStart);
    }
    return getClass().getMethod(testMethod).isAnnotationPresent(ManualServerInit.class);
  }

  protected void initServer(Configurator configurator) throws Exception {
    if (testCLMServer != null && !testCLMServer.isReusable(isProxyRequiredToReachHds(), configurator)) {
      testCLMServer.stop();
      testCLMServer = null;
    }

    if (testCLMServer == null) {
      testCLMServer = new TestCLMServer(isProxyRequiredToReachHds(), getBrainModules(), configurator);
      testCLMServer.start();
    }
  }

  @After
  public void cleanupTest() throws Exception {
    // Delete the default LTGs when stopping
    licenseThreatGroupDAO.deleteDefaultLicenseThreatGroups();

    boolean installLicense = false;
    if (savedLicenseFingerprint != null) {
      licenseFingerprinter.setDummyLicenseFingerprint(savedLicenseFingerprint);
      savedLicenseFingerprint = null;
      installLicense = true;
    }
    if (licenseManager.wasChanged()) {
      licenseManager.reset();
      installLicense = true;
    }
    if (productlicenseWasUninstalled) {
      installLicense = true;
    }

    if (testCLMServer != null) {
      testCLMServer.getInsightServer().reset();
      if (installLicense) {
        installLicense();
      }
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
        mockHdsProductNotificationService = mock(HdsProductNotificationService.class);
        bind(HdsProductNotificationService.class).toInstance(mockHdsProductNotificationService);
      }
    });
    return modules;
  }

  private boolean isProxyRequiredToReachHds() {
    return getClass().getName().endsWith("ProxyTest");
  }

  protected HttpRequest restRequest() {
    return HttpRequest.to(getRestBaseUrl());
  }

  protected String getRestBaseUrl() {
    String restBaseUrl = getCLMServer().getClientConfiguration().getServerUrl();
    if (!restBaseUrl.endsWith("/")) {
      restBaseUrl = restBaseUrl + "/";
    }
    return restBaseUrl;
  }

  protected HttpRequest adminRequest() {
    return HttpRequest.to(getCLMServer().getClientConfiguration().getServerAdminUrl());
  }

  protected void setHdsResponseForURI(String uri, Object body, int status) {
    getInsightServer().setResponseForURI(uri, body, status);
  }

  protected void setHdsResponseForURI(String uri, int status, String bodyResource) {
    setHdsResponseForURI(uri, getClass().getResource(bodyResource), status);
  }

  protected void mockScanReceipt(ScanReceipt scanReceipt) {
    setHdsResponseForURI("rest/application/analysis", toJson(scanReceipt), 200);
  }

  protected void mockReport(String scanId, String resourceName) {
    setHdsResponseForURI("rest/ci/report?scanId=" + scanId, 200, resourceName);
  }

  protected void mockComponentSummary(ComponentIdentifier componentIdentifier, ComponentSummary componentSummary)
      throws Exception
  {
    String uri = UriBuilder.fromPath("rest/component/summary")
        .queryParam("componentIdentifier", toJson(componentIdentifier)).build().toString();
    setHdsResponseForURI(uri, toJson(componentSummary), 200);
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

  protected static void assertResponseStatus(final int expectedStatus, final HttpResponse response) {
    final int actualStatus = response.getStatusCode();
    assertEquals(
        "URI:" + response.getUrl() + ", StatusText:" + response.getStatusText() + ", ResponseBody:"
            + response.getBodyText(), expectedStatus, actualStatus);
  }

  protected String toJson(Object object) {
    try {
      return objectMapper.writeValueAsString(object);
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

  protected void installLicense() throws Exception {
    HttpResponse response = uploadLicense(licenseRequest());
    assertResponseStatus(200, response);

    Assert.assertTrue(licenseManager.isValid());
  }

  protected HttpRequest licenseRequest() {
    HttpRequest request = HttpRequest.to(getRestBaseUrl()).path(ProductLicenseResource.SERVICE_PATH)
        .csrfToken("nonce", null, "nonce");
    request.part("file", "sonatype.lic", getClass().getResource("/productlicense/license.lic"));
    return request;
  }

  protected HttpResponse uploadLicense(HttpRequest licenseRequest) throws Exception {
    HttpResponse response = licenseRequest.post();
    productlicenseWasUninstalled = false;
    return response;
  }

  protected void uninstallLicense() throws Exception {
    HttpRequest.to(getRestBaseUrl()).path(ProductLicenseResource.SERVICE_PATH).delete();
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

  protected void setLicenseProducts(String... products) throws Exception {
    licenseManager.setProducts(products);
    installLicense();
  }
}
