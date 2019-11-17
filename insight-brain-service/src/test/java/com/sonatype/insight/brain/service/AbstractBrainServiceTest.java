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
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.jira.JiraClient;
import com.sonatype.insight.brain.jira.JiraClientFactory;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.product.license.ProductLicenseResource;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.product.notifications.HdsProductNotificationService;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.client.utils.Authentication;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.mock.hds.HdsMockResponse;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryHeader;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.insight.test.networking.PortAllocator;

import org.sonatype.licensing.product.ProductLicenseManager;
import org.sonatype.licensing.product.util.LicenseFingerprinter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.codehaus.plexus.util.FileUtils;
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

  private static final TestProductLicense testProductLicense = new TestProductLicense(licenseManager);

  private static boolean productlicenseWasUninstalled;

  private static final TestLicenseFingerprinter licenseFingerprinter = new TestLicenseFingerprinter();

  private static String savedLicenseFingerprint;

  private static HdsMockServerRule hdsMockServer;

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
    initHds();
    if (!isTestUsingManualServerInit()) {
      initServer();
    }
    setUpTestLicenseThreatGroups();
  }

  private void initHds() throws Exception {
    if (hdsMockServer != null && !hdsMockServer.isReusable(isProxyRequiredToReachHds())) {
      hdsMockServer.stop();
      hdsMockServer = null;
    }
    if (hdsMockServer == null) {
      hdsMockServer = new HdsMockServerRule(PortAllocator.nextFreePort(), isProxyRequiredToReachHds());
      hdsMockServer.start();
    }
    else {
      hdsMockServer.reset();
    }
  }

  protected boolean isTestUsingManualServerInit() throws Exception {
    String testMethod = testName.getMethodName();
    int paramStart = testMethod.indexOf('[');
    if (paramStart >= 0) {
      testMethod = testMethod.substring(0, paramStart);
    }
    return getClass().getMethod(testMethod).isAnnotationPresent(ManualServerInit.class);
  }

  protected void initServer() throws Exception {
    initServer(null);
  }

  protected void initServer(Configurator configurator) throws Exception {
    if (testCLMServer != null && !testCLMServer.isReusable(isProxyRequiredToReachHds(), configurator)) {
      testCLMServer.stop();
      testCLMServer = null;
    }

    if (testCLMServer == null) {
      testCLMServer = new TestCLMServer(isProxyRequiredToReachHds(), getBrainModules(), configurator, hdsMockServer);
      testCLMServer.start();
    }
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
    testProductLicense.reset();

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
        bind(ProductLicense.class).to(TestProductLicense.class);
        bind(TestProductLicense.class).toInstance(testProductLicense);
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

  protected HdsMockResponse hdsRespondWith(Object body) {
    return getHdsServer().respondWith(body);
  }

  protected HdsMockResponse hdsRespondWithResource(String bodyResource) {
    return hdsRespondWith(getClass().getResource(bodyResource));
  }

  protected void mockScanReceipt(ScanReceipt scanReceipt) {
    hdsRespondWith(scanReceipt).atUri("rest/application/analysis");
  }

  protected String mockReport(String resourceName) {
    String scanId = tempEntity.uuid();
    mockReport(scanId, resourceName);
    return scanId;
  }

  protected void mockReport(String scanId, String resourceName) {
    URL resourceUrl = ReportHelper.zipReport(resourceName, tempDir);
    hdsRespondWith(resourceUrl).atUri("rest/application/analysis/" + scanId);
  }

  protected void mockComponentSummary(ComponentIdentifier componentIdentifier, ComponentSummary componentSummary)
      throws Exception
  {
    hdsRespondWith(componentSummary).atUri(UriBuilder.fromPath("rest/component/summary")
        .queryParam("componentIdentifier", URLEncoder.encode(toJson(componentIdentifier), "UTF-8")).build());
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
    return hdsMockServer;
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
    return licenseRequest(new byte[1]);
  }

  protected HttpRequest licenseRequest(Object licenseFile) {
    HttpRequest request = HttpRequest.to(getRestBaseUrl()).path(ProductLicenseResource.RESOURCE_PATH)
        .csrfToken("nonce", null, "nonce");
    request.part("file", "sonatype.lic", licenseFile);
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
    testProductLicense.setMaxApplications(applicationLimit);
  }

  protected void setLicenseProducts(String... products) throws Exception {
    licenseManager.setProducts(products);
    installLicense();
  }

  protected String getUsername() {
    return getCLMServer().getClientConfiguration().getServerAuth().getUsername();
  }

  protected void createScanFile(String applicationId, String scanId) {
    File scanFile = getCLMServer().getInstance(InsightWork.class).getScanFile(applicationId, scanId);
    try {
      Files.createDirectories(scanFile.getParentFile().toPath());
      Files.write(scanFile.toPath(), new byte[0]);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  protected File createReportFile(String applicationId, String scanId, String sourceReportDir) throws IOException {
    File reportFile = getCLMServer().getInstance(InsightWork.class).getReportFile(applicationId, scanId);
    FileUtils.copyURLToFile(ReportHelper.zipReport(sourceReportDir, tempDir), reportFile);
    return reportFile;
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

    private List<TelemetryData> telemetryDataReceived;

    public TelemetryItem(final ZipInputStream zipInputStream) {
      this.zipInputStream = zipInputStream;
    }

    public TelemetryHeader getTelemetryHeader() {
      return telemetryHeaderReceived;
    }

    public List<TelemetryData> getTelemetryData() {
      return telemetryDataReceived;
    }

    public List<TelemetryPurpose> getTelemetryPurposes() {
      return telemetryDataReceived.stream().map(TelemetryData::getPurpose).collect(Collectors.toList());
    }

    public TelemetryItem invoke() throws IOException {
      ObjectMapper objectMapper = new ObjectMapper().disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);

      ZipEntry zipEntryHeader = zipInputStream.getNextEntry();
      assertThat(zipEntryHeader.getName()).isEqualTo(TelemetrySender.HEADER_ENTRY_NAME);
      telemetryHeaderReceived = objectMapper.readValue(zipInputStream, TelemetryHeader.class);

      ZipEntry zipEntryData = zipInputStream.getNextEntry();
      assertThat(zipEntryData.getName()).isEqualTo(TelemetrySender.DATA_ENTRY_NAME);
      telemetryDataReceived = objectMapper.readValue(zipInputStream, new TypeReference<List<TelemetryData>>() { });
      return this;
    }
  }
}
