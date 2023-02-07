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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
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
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiJiraConfigurationDTO;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ApiJiraConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ApiProxyServerConfigurationService;
import com.sonatype.insight.brain.dataaccess.PerpetualLockDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyServerConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.db.AggregationDataStoreProvider;
import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.db.DatamartProvider;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.db.ThirdPartyScansProvider;
import com.sonatype.insight.brain.git.SourceControlInstanceManager;
import com.sonatype.insight.brain.hds.DefaultHdsClient;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.jira.JiraClient;
import com.sonatype.insight.brain.jira.JiraClientFactory;
import com.sonatype.insight.brain.model.PerpetualLock;
import com.sonatype.insight.brain.model.configuration.ProxyServerConfiguration;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.scheduler.QuartzJobStoreTX;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.scheduler.TestQuartzJobStoreTx;
import com.sonatype.insight.brain.scheduler.TestTaskScheduler;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.utils.DatabaseProvisionUtils;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.brain.utils.ScanHelper;
import com.sonatype.insight.client.utils.Authentication;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.json.store.JsonUtils;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public abstract class AbstractBrainServiceTest
{
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD})
  public @interface ManualServerInit
  {
  }

  private static final ObjectMapper objectMapper = new ObjectMapper();

  private static final PerpetualLockDAO perpetualLockDAO = new PerpetualLockDAO();

  private final Logger log = LoggerFactory.getLogger(getClass());

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity()
  {
    @Override
    public void after() {
      super.after();
      afterDatabaseReset();
    }
  };

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

  protected static JiraClient mockJiraClient;

  protected DatabaseContainer databaseContainer;

  public void setUpTestLicenseThreatGroups() {
    LicenseThreatGroupDataHelper.createTestLicenseThreatGroups(tempEntity);
  }

  @Before
  public void initTest() throws Exception {
    DefaultHdsClient.waitToCloseOldClients = false;

    log.info("Before: {}", testName.getMethodName());
    initHds();
    initDatabaseContainer();
    if (!isTestUsingManualServerInit()) {
      initServer();
    }
    setUpTestLicenseThreatGroups();
    if (testCLMServer != null) {
      ProxyServerConfiguration proxyServerConfiguration = getCLMServer().getProxyServerConfiguration();
      if (proxyServerConfiguration != null) {
        new ProxyServerConfigurationDAO().set(proxyServerConfiguration);
        tempEntity.setSavedProxyServerConfiguration(proxyServerConfiguration);
        getCLMServer().getInstance(ApiProxyServerConfigurationService.class).applyProxyServerConfigurationToClients();
      }
    }
  }

  private void initDatabaseContainer() {
    if (databaseContainer == null) {
      DatabaseProvisionUtils databaseProvisionUtils =
          spy(new DatabaseProvisionUtils(OperationalDataStoreProvider.getInstance(),
              AggregationDataStoreProvider.getInstance(), DatamartProvider.getInstance(),
              ThirdPartyScansProvider.getInstance()));
      databaseContainer = new DatabaseContainer(new DataSourceFactory(), databaseProvisionUtils);
    }
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
      testCLMServer = new TestCLMServer(isProxyRequiredToReachHds(), getBrainModules(), configurator, hdsMockServer,
          databaseContainer);
      testCLMServer.start();
    }
    setBaseUrl("http://localhost");
    testCLMServer.getCLMServer().setHdsUrl();
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
      TaskScheduler taskScheduler = testCLMServer.getCLMServer().getInstance(TaskScheduler.class);
      if (taskScheduler != null) {
        taskScheduler.standby();
        taskScheduler.clear();
      }
      getCLMServer().resetDisableForTesting();
      InsightConfig insightConfig = getCLMServer().getConfiguration();
      if (insightConfig != null) {
        getCLMServer().getConfiguration().setFeatures(Collections.emptyMap());
        getCLMServer().getConfiguration().setSystemAllowlist(Collections.emptyList());
      }
      resetProperties(SystemConfigurationProperty.BASE_URL,
          SystemConfigurationProperty.FORCE_BASE_URL,
          SystemConfigurationProperty.CSRF_PROTECTION,
          SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH,
          SystemConfigurationProperty.PURGE_SCAN_FILES,
          SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH,
          SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH,
          SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS,
          SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS,
          SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS,
          SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER,
          SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING,
          SystemConfigurationProperty.POLICY_MONITORING_HOUR,
          SystemConfigurationProperty.DB_BACKUP_DIR,
          SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE,
          SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED,
          SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING,
          SystemConfigurationProperty.ACCESS_ALLOWLIST,
          SystemConfigurationProperty.SCHEMA_MIGRATION_ENABLED);
    }
    releaseScmPerpetualLock();
    new ProxyServerConfigurationDAO().delete();
    tempEntity.setSavedProxyServerConfiguration(null);
    if (testCLMServer != null) {
      ApiProxyServerConfigurationService proxyServerConfigurationService =
          getCLMServer().getInstance(ApiProxyServerConfigurationService.class);
      if (proxyServerConfigurationService != null) {
        proxyServerConfigurationService.applyProxyServerConfigurationToClients();
      }
    }
  }

  private void releaseScmPerpetualLock() {
    String perpetualLockId = SourceControlInstanceManager.SOURCE_CONTROL_ACCESS_LOCK;
    PerpetualLock perpetualLock = perpetualLockDAO.getPerpetualLockById(perpetualLockId);
    if (perpetualLock != null) {
      perpetualLockDAO.releasePerpetualLockForOwner(perpetualLockId, perpetualLock.getOwner());
    }
  }

  protected void afterDatabaseReset() {
    // hook for subclasses to perform further cleanup action after TemporaryEntity has reset the database
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
        bind(QuartzJobStoreTX.class).to(TestQuartzJobStoreTx.class);
        bind(TaskScheduler.class).to(TestTaskScheduler.class);

        mockJiraClient = mock(JiraClient.class);
        JiraClientFactory jiraClientFactory = mock(JiraClientFactory.class);
        when(jiraClientFactory.create(any())).thenReturn(mockJiraClient);
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

  protected HdsMockResponse mockScanReceipt(ScanReceipt scanReceipt) {
    return hdsRespondWith(scanReceipt).atUri(ScanUploader.HDS_PATH);
  }

  protected String mockReport(String resourceName) {
    String scanId = tempEntity.uuid();
    mockReport(scanId, resourceName);
    return scanId;
  }

  protected void mockReport(PolicyEvaluation evaluation, String classSimpleName) {
    try {
      Path reportDir = getCLMServer().getInstance(InsightWork.class)
          .getReportDir(evaluation.getApplicationId(), evaluation.getScanId()).toPath();
      Files.createDirectories(reportDir);
      Files.write(reportDir.resolve("report.zip"), Collections.singletonList("report.zip"));
      File reportFile = reportDir.resolve("report.zip").toFile();
      try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(reportFile.toPath()))) {
        zos.putNextEntry(new ZipEntry("index.html"));
      }
      String[] filenames = {
          Report.BOM_JSON_FILENAME, Report.SECURITY_JSON_FILENAME, Report.LICENSES_JSON_FILENAME,
          Report.DATA_JSON_FILENAME, Report.DEPENDENCIES_JSON_FILENAME
      };
      for (String filename : filenames) {
        File file = Report.getCacheFile(reportFile, filename);
        FileUtils.copyURLToFile(getClass().getResource("/" + classSimpleName + "/report/" + filename), file);
      }
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
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

  protected void mockGetDependencies(ComponentDependenciesDTO componentDependenciesDTO) {
    hdsRespondWith(componentDependenciesDTO).atUri("rest/component/dependencies");
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

  protected TestCLMServer getTestCLMServer() {
    return testCLMServer;
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
    HttpRequest request = HttpRequest.to(getRestBaseUrl()).path(PublicApiPaths.PRODUCT_LICENSE_RESOURCE_PATH);
    request.part("file", "sonatype.lic", licenseFile);
    return request;
  }

  protected HttpResponse uploadLicense(HttpRequest licenseRequest) throws Exception {
    HttpResponse response = licenseRequest.post();
    productlicenseWasUninstalled = false;
    return response;
  }

  protected void uninstallLicense() throws Exception {
    HttpResponse response =
        HttpRequest.to(getRestBaseUrl()).path(PublicApiPaths.PRODUCT_LICENSE_RESOURCE_PATH).delete();
    assertResponseStatus(204, response);
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

  protected void setApplicationLimit(Integer applicationLimit) {
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
    ScanHelper.createDummyScanFile(getCLMServer().getInstance(InsightWork.class), applicationId, scanId);
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

  public void setBaseUrl(String baseUrl) {
    setBaseUrl(baseUrl, false);
  }

  public void setBaseUrl(String baseUrl, boolean forceBaseUrl) {
    Map<String, Object> properties = new HashMap<>();
    properties.put(SystemConfigurationProperty.BASE_URL, baseUrl);
    properties.put(SystemConfigurationProperty.FORCE_BASE_URL, forceBaseUrl);

    setProperties(properties);
  }

  public void setAccessAllowlist(List<Map<String, String>> allowList) {
    Map<String, Object> properties = new HashMap<>();
    properties.put(SystemConfigurationProperty.ACCESS_ALLOWLIST, allowList);

    setProperties(properties);
  }

  private void setProperties(Map<String, Object> properties) {
    ApiConfigurationService service = getCLMServer().getInstance(ApiConfigurationService.class);
    service.setConfigurationNoAuthz(properties);
    service.applyConfigurationToClients(properties.keySet());
  }

  public void resetProperties(String... propertyNames) {
    ApiConfigurationService service = getCLMServer().getInstance(ApiConfigurationService.class);
    if (service != null) {
      service.deleteConfigurationNoAuthz(propertyNames);
      service.applyConfigurationToClients(propertyNames);
    }
  }

  public void createJiraConfiguration() {
    ApiJiraConfigurationDTO dto = new ApiJiraConfigurationDTO();
    dto.url = "http://url";
    ApiJiraConfigurationService jiraConfigurationService =
        getCLMServer().getInstance(ApiJiraConfigurationService.class);
    jiraConfigurationService.setConfigurationNoAuthz(JsonUtils.asTree(dto));
    jiraConfigurationService.applyJiraConfigurationToClients();
  }
}
