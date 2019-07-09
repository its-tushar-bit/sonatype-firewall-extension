/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.TestLicenseFingerprinter;
import com.sonatype.insight.brain.TestLicenseManager;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.features.LicensedFeature;
import com.sonatype.insight.brain.jira.JiraClient;
import com.sonatype.insight.brain.jira.JiraClientFactory;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.ProductLicenseResource;
import com.sonatype.insight.brain.product.notifications.HdsProductNotificationService;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.client.utils.Authentication;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.mock.hds.HdsMockServer.HdsConfigurator;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryHeader;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.sonatype.licensing.product.ProductLicenseManager;
import org.sonatype.licensing.product.util.LicenseFingerprinter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractBrainServiceTest
{
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.METHOD })
  public @interface ManualServerInit
  {
  }

  private static final ObjectMapper objectMapper = new ObjectMapper();

  private final Logger log = LoggerFactory.getLogger(getClass());

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

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

  protected static JiraClient mockJiraClient;

  public void setUpTestLicenseThreatGroups() {
    LicenseThreatGroupDataHelper.createTestLicenseThreatGroups(tempEntity);
  }

  @Before
  public void initTest() throws Exception {
    log.info("Before: {}", testName.getMethodName());
    if (!isTestUsingManualServerInit()) {
      initServer(null);
    }

    setUpTestLicenseThreatGroups();
  }

  protected boolean isTestUsingManualServerInit() throws Exception {
    String testMethod = testName.getMethodName();
    int paramStart = testMethod.indexOf('[');
    if (paramStart >= 0) {
      testMethod = testMethod.substring(0, paramStart);
    }
    return getClass().getMethod(testMethod).isAnnotationPresent(ManualServerInit.class);
  }

  protected void initServer(Configurator configurator, HdsConfigurator hdsConfigurator) throws Exception {
    if (testCLMServer != null
        && !testCLMServer.isReusable(isProxyRequiredToReachHds(), configurator, hdsConfigurator)) {
      testCLMServer.stop();
      testCLMServer = null;
    }

    if (testCLMServer == null) {
      testCLMServer = new TestCLMServer(isProxyRequiredToReachHds(), getBrainModules(), configurator, hdsConfigurator);
      testCLMServer.start();
    }
  }

  protected void initServer(Configurator configurator) throws Exception {
    initServer(configurator, null);
  }

  @After
  public void cleanupTest() throws Exception {
    log.info("After: {}", testName.getMethodName());
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
      testCLMServer.getHdsServer().reset();
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
        bind(CLMLicenseManager.class).to(TestLicenseManager.class);
        bind(ProductLicenseManager.class).to(TestProductLicenseManager.class);
        bind(TestProductLicenseManager.class).toInstance(licenseManager);
        bind(LicenseFingerprinter.class).toInstance(licenseFingerprinter);
        mockHdsProductNotificationService = mock(HdsProductNotificationService.class);
        bind(HdsProductNotificationService.class).toInstance(mockHdsProductNotificationService);

        mockJiraClient = mock(JiraClient.class);
        JiraClientFactory jiraClientFactory = mock(JiraClientFactory.class);
        when(jiraClientFactory.create()).thenReturn(mockJiraClient);
        bind(JiraClientFactory.class).toInstance(jiraClientFactory);
      }
    });
    return modules;
  }

  private boolean isProxyRequiredToReachHds() {
    return getClass().getName().endsWith("ProxyTest");
  }

  protected HttpRequest restRequest() {
    HttpRequest request = HttpRequest.to(getRestBaseUrl());
    Authentication serverAuth = getCLMServer().getClientConfiguration().getServerAuth();
    if (serverAuth != null) {
      return request.auth(serverAuth.getUsername(), new String(serverAuth.getPassword()));
    }
    return request;
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
    getHdsServer().setResponseForURI(uri, body, status);
  }

  protected void setHdsResponseForURI(String uri, int status, String bodyResource) {
    setHdsResponseForURI(uri, getClass().getResource(bodyResource), status);
  }

  protected void mockScanReceipt(ScanReceipt scanReceipt) {
    setHdsResponseForURI("rest/application/analysis", scanReceipt, 200);
  }

  protected String mockReport(String resourceName) {
    String scanId = tempEntity.uuid();
    mockReport(scanId, resourceName);
    return scanId;
  }

  protected void mockReport(String scanId, String resourceName) {
    URL resourceUrl;
    if (!resourceName.endsWith(".zip")) {
      File reportZipFile = zipResourceDir(resourceName);
      try {
        resourceUrl = reportZipFile.toURI().toURL();
      }
      catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    }
    else {
      resourceUrl = getClass().getResource(resourceName);
    }
    setHdsResponseForURI("rest/application/analysis/" + scanId, resourceUrl, 200);
  }

  protected File zipResourceDir(String resourceName) {
    try {
      URL resourceUrl = getClass().getResource(resourceName);
      File resourceDir = new File(resourceUrl.toURI());
      if (!resourceDir.isDirectory()) {
        throw new RuntimeException("'" + resourceDir.getAbsolutePath() + "' is not a directory.");
      }
      File reportZipFile = new File(tempDir.getRoot(), getClass().getSimpleName() + "-" + UUID.randomUUID() + ".zip");
      Zipper.zip(resourceDir, reportZipFile);
      return reportZipFile;
    }
    catch (IOException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  protected void mockComponentSummary(ComponentIdentifier componentIdentifier, ComponentSummary componentSummary)
      throws Exception
  {
    String uri = UriBuilder.fromPath("rest/component/summary")
        .queryParam("componentIdentifier", URLEncoder.encode(toJson(componentIdentifier), "UTF-8")).build().toString();
    setHdsResponseForURI(uri, componentSummary, 200);
  }

  protected static void assertResponseStatus(final int expectedStatus, final HttpResponse response) {
    assertThat(response.getStatusCode()).as("URI:" + response.getUrl() + ", StatusText:" + response.getStatusText()
        + ", ResponseBody:" + response.getBodyText()).isEqualTo(expectedStatus);
  }

  private String toJson(Object object) {
    try {
      return objectMapper.writeValueAsString(object);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  protected TestInsightBrainServiceRule getCLMServer() {
    return testCLMServer.getCLMServer();
  }

  protected HdsMockServerRule getHdsServer() {
    return testCLMServer.getHdsServer();
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

    assertThat(licenseManager.isValid()).isTrue();
  }

  protected HttpRequest licenseRequest() {
    HttpRequest request = HttpRequest.to(getRestBaseUrl()).path(ProductLicenseResource.RESOURCE_PATH)
        .csrfToken("nonce", null, "nonce");
    request.part("file", "sonatype.lic", new byte[0]);
    return request;
  }

  protected HttpResponse uploadLicense(HttpRequest licenseRequest) throws Exception {
    HttpResponse response = licenseRequest.post();
    productlicenseWasUninstalled = false;
    return response;
  }

  protected void uninstallLicense() throws Exception {
    HttpRequest.to(getRestBaseUrl()).path(ProductLicenseResource.RESOURCE_PATH).delete();
    productlicenseWasUninstalled = true;

    assertThat(licenseManager.isValid()).isFalse();
  }

  protected void setFeatures(LicensedFeature... features) throws Exception {
    licenseManager.setFeatures(features);
    installLicense();
  }

  protected void setMissingFeature(LicensedFeature feature) throws Exception {
    licenseManager.setFeatures(EnumSet.complementOf(EnumSet.of(feature)).toArray(new LicensedFeature[0]));
    installLicense();
  }

  protected void setApplicationLimit(Integer applicationLimit) throws Exception {
    licenseManager.setApplicationLimit(applicationLimit);
    installLicense();
  }

  protected void setLicenseProducts(String... products) throws Exception {
    licenseManager.setProducts(products);
    installLicense();
  }

  protected String getUsername() {
    return getCLMServer().getClientConfiguration().getServerAuth().getUsername();
  }

  protected void createScanFile(String applicationId, String scanId) {
    File scanFile = getCLMServer().getInjector().getInstance(InsightWork.class).getScanFile(applicationId, scanId);
    try {
      Files.createDirectories(scanFile.getParentFile().toPath());
      Files.write(scanFile.toPath(), new byte[0]);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  protected List<TelemetryItem> getTelemetryItems(final Map<ByteArrayDataSource, Integer> responses)
      throws MessagingException, IOException
  {
    List<TelemetryItem> telemetryItems = new ArrayList<>();
    for (Map.Entry<ByteArrayDataSource, Integer> response : responses.entrySet()) {
      Integer status = response.getValue();
      MimeMultipart multipart = new MimeMultipart(response.getKey());
      BodyPart bodyPart = multipart.getBodyPart(0);
      String filename = bodyPart.getFileName();
      assertThat(TelemetrySender.ZIP_FILENAME).isEqualTo(filename);
      assertThat(status).isEqualTo(204);
      try (ZipInputStream zipInputStream = new ZipInputStream(bodyPart.getInputStream())) {
        telemetryItems.add(new TelemetryItem(zipInputStream).invoke());
      }
    }
    return telemetryItems;
  }

  protected class TelemetryItem
  {
    private final ZipInputStream zipInputStream;

    private TelemetryHeader telemetryHeaderReceived;

    private TelemetryData telemetryDataReceived;

    public TelemetryItem(final ZipInputStream zipInputStream) {
      this.zipInputStream = zipInputStream;
    }

    public TelemetryHeader getTelemetryHeader() {
      return telemetryHeaderReceived;
    }

    public TelemetryData getTelemetryData() {
      return telemetryDataReceived;
    }

    public TelemetryPurpose getTelemetryPurpose() {
      return telemetryDataReceived.getPurpose();
    }

    public TelemetryItem invoke() throws IOException {
      byte[] buffer = new byte[1024];

      ZipEntry zipEntryHeader = zipInputStream.getNextEntry();
      assertThat(zipEntryHeader.getName()).isEqualTo(TelemetrySender.HEADER_ENTRY_NAME);
      zipInputStream.read(buffer);
      telemetryHeaderReceived = JsonUtils.parse(buffer, TelemetryHeader.class);

      ZipEntry zipEntryData = zipInputStream.getNextEntry();
      assertThat(zipEntryData.getName()).isEqualTo(TelemetrySender.DATA_ENTRY_NAME);
      zipInputStream.read(buffer);
      telemetryDataReceived = JsonUtils.parse(buffer, TelemetryData.class);
      return this;
    }
  }
}
