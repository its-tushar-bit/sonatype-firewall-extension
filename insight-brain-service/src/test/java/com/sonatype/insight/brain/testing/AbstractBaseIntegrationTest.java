/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.sonatype.insight.brain.MockCleaner;
import com.sonatype.insight.brain.StaticInjectionTestHelper;
import com.sonatype.insight.brain.TestLicenseFingerprinter;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiJiraConfigurationDTO;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ApiJiraConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ApiProxyServerConfigurationService;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.DAOFactory;
import com.sonatype.insight.brain.dataaccess.PerpetualLockDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.TestDAOFactory;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyServerConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.security.RolePermissionDAO;
import com.sonatype.insight.brain.db.rule.DatabaseContainerRule;
import com.sonatype.insight.brain.db.rule.DatabaseRule;
import com.sonatype.insight.brain.developer.integrationdashboard.DeveloperEnablementService;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.jira.JiraClient;
import com.sonatype.insight.brain.jira.JiraClientFactory;
import com.sonatype.insight.brain.model.PerpetualLock;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.product.TestProductLicenseRule;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.scheduler.QuartzJobStoreTX;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.scheduler.TestQuartzJobStoreTx;
import com.sonatype.insight.brain.scheduler.TestTaskScheduler;
import com.sonatype.insight.brain.service.HdsMockServerRule;
import com.sonatype.insight.brain.service.InsightBrainService;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.service.TestCLMServer;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.brain.service.TestInsightBrainServiceRule;
import com.sonatype.insight.brain.sourcecontrol.SourceControlLoadBalancer;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
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
import com.google.inject.Binder;
import com.google.inject.Module;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * <p>
 * Abstract base class for IQ integration tests. These are tests that run the 'CLM stack' which includes an IQ Server
 * (i.e. {@link InsightBrainService}) with various database setups, a mock HDS server, a mock Jira client, etc...
 * <p>
 *
 * <p>
 * Important notes:
 * <ul>
 *   <li>Use @{@link AbstractBrainServiceIntegrationTest} for a regular single-tenant version.</li>
 *   <li>Use AbstractMultiTenantBrainServiceIntegrationTest (in the nexus-mtiq-server module) for the multi-tenant
 *   version</li>
 *   <li>See {@link DatabaseRule} to see how to set up a DB depending on your needs. Note this class uses
 *   {@link DatabaseContainerRule} to provide access to the full database container.</li>
 *   <li>Assign any test dependencies used for setup (e.g. DAOs) by overridding {@link #assignTestDependencies()}</li>
 *   <li>The test IQ server will be started automatically with a default configuration. Use the
 *   {@link ManualIqServerInit} annotation to suppress automatic start and then call
 *   {@link #startIqTestServer(Configurator)} with a custom configuration.</li>
 * </ul>
 * </p>
 */
@Category(SlowTest.class)
public abstract class AbstractBaseIntegrationTest
{
  /**
   * Tests annotated with this will not automatically start the test IQ server. You must invoke
   * {@link #startIqTestServer(Configurator)} to start the server, optionally with a custom configuration.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD})
  public @interface ManualIqServerInit
  { }

  private static final Configurator DEFAULT_CONFIGURATOR = new Configurator()
  {
    @Override
    public void configure(final InsightConfig config) {
      // no-op
    }

    @Override
    public boolean isReusable() {
      return true;
    }
  };

  // by default license is always valid, to override, simply uninstall the license
  protected static final TestProductLicenseManager licenseManager = new TestProductLicenseManager();

  protected static DeveloperEnablementService mockDeveloperEnablementService = mock(DeveloperEnablementService.class);

  public static final TestProductLicense testProductLicense =
      new TestProductLicense(licenseManager, false, mockDeveloperEnablementService);

  private static final ObjectMapper objectMapper = new ObjectMapper();

  private final Logger log = LoggerFactory.getLogger(getClass());

  /** You should only use this `daoFactory` when you use the {@link ManualIqServerInit} annotation, and you need a DAO
   * before calling the `startIqTestServer` method on the test, or when you need a DAO and no server has started yet.
   * For other scenarios, use the `lookup` method to get an injected instance of the DAO from the server */
  protected DAOFactory daoFactory;

  @Rule(order = 1)
  public DatabaseContainerRule databaseContainerRule = getDatabaseContainerRule();

  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Rule
  public TestName testName = new TestName();

  @Rule
  public MockCleaner mockCleaner = new MockCleaner();

  @Rule(order = 2) // after DatabaseContainerRule
  public TestProductLicenseRule testProductLicenseRule = new TestProductLicenseRule(databaseContainerRule);

  protected static boolean productlicenseWasUninstalled;

  protected static final TestLicenseFingerprinter licenseFingerprinter = new TestLicenseFingerprinter();

  private static String savedLicenseFingerprint;

  // note: this is a rule but is initialized in the test reset process
  protected static HdsMockServerRule hdsMockServer;

  // note: this is a rule but is initialized in the test reset process
  protected static TestCLMServer testCLMServer;

  private static Class<? extends AbstractBaseIntegrationTest> binderConfigurationClass =
      AbstractBaseIntegrationTest.class;

  protected static JiraClient mockJiraClient = mock(JiraClient.class);

  private static JiraClientFactory mockJiraClientFactory = mock(JiraClientFactory.class);

  public abstract void setUpTestLicenseThreatGroups();

  @BeforeClass
  public static void disableWaitToCloseOldClients() {
    HdsClient.waitToCloseOldClients = false;
  }

  protected DatabaseContainerRule getDatabaseContainerRule() {
    return DatabaseContainerRule.getInstance(AbstractBaseIntegrationTest.class);
  }

  @Before
  public void initTest() throws Exception {
    log.info("@Before (AbstractBaseIntegrationTest.initTest): {}", testName.getMethodName());
    initHds();

    testProductLicense.reset();

    if (!isTestUsingManualServerInit()) {
      startIqTestServer();
    }

    setUpTestLicenseThreatGroups();

    // This must get reset for every test because MockCleaner undoes it after every test
    lenient().when(mockJiraClientFactory.create(any())).thenReturn(mockJiraClient);

    // Re-inject classes that have static dependencies
    daoFactory = new TestDAOFactory(databaseContainerRule);
    StaticInjectionTestHelper.inject(daoFactory);
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
    return getClass().getMethod(testMethod).isAnnotationPresent(ManualIqServerInit.class);
  }

  private Class<? extends AbstractBaseIntegrationTest> getConfigureBinderClass() throws Exception {
    return (Class<? extends AbstractBaseIntegrationTest>) getClass().getMethod("configure", Binder.class)
        .getDeclaringClass();
  }

  protected void startIqTestServer() throws Exception {
    startIqTestServer(null);
  }

  protected void startIqTestServer(Configurator configurator) throws Exception {
    if (configurator == null) {
      configurator = DEFAULT_CONFIGURATOR;
    }

    // check if the test IQ server needs to be stopped
    maybeStopTestIqServer(configurator);

    // start the test IQ server
    if (testCLMServer == null) {
      testCLMServer = new TestCLMServer(getInsightBrainServiceFactory(), isProxyRequiredToReachHds(), getBrainModules(),
          configurator, hdsMockServer, databaseContainerRule.getDatabaseContainer());
      testCLMServer.start();
    }

    setBaseUrl("http://localhost");
    testCLMServer.getCLMServer().setProxyConfiguration();
    testCLMServer.getCLMServer().setHdsUrl();
  }

  protected InsightBrainServiceFactory getInsightBrainServiceFactory() {
    return new DefaultInsightBrainServiceFactory();
  }

  private void maybeStopTestIqServer(final Configurator configuratorToApply) throws Exception {
    // default is the TestCLMServer is re-used and should not be restarted
    boolean stopIqServer = false;

    Class<? extends AbstractBaseIntegrationTest> binderConfigurationClass = getConfigureBinderClass();

    if (testCLMServer != null) {
      if (!(testCLMServer.getCLMServer().getIsHdsProxyRequired() == isProxyRequiredToReachHds())) {
        log.info("Test IQ server is not reusable due to HDS proxy requirement change. Restarting test IQ server.");
        stopIqServer = true;
      }
      if (testCLMServer.getCLMServer().getConfigurator() != configuratorToApply) {
        log.info("Test IQ server is not reusable due to configurator change. Will restart test IQ server.");
        stopIqServer = true;
      }

      if (!configuratorToApply.isReusable()) {
        log.info("Test IQ server is not reusable due to custom configurator. Will restart test IQ server.");
        stopIqServer = true;
      }

      // if the database test fixture says its not-reusable
      if (!databaseContainerRule.isDatabaseFixtureReusable()) {
        log.info("Test IQ server is not reusable due to custom database settings. Will restart test IQ server.");
        stopIqServer = true;
      }

      // If a test wants to change the binding configuration via overriding configure (e.g. to use mocks/spies)
      // then we need to restart the server to account for those
      // additionally when going back to the original configuration in this class the server will also need restarting
      if (AbstractBaseIntegrationTest.binderConfigurationClass != binderConfigurationClass) {
        log.info("Test IQ server is not reusable due to different binder configuration. Will restart test IQ server.");
        stopIqServer = true;
      }
    }

    AbstractBaseIntegrationTest.binderConfigurationClass = binderConfigurationClass;

    if (stopIqServer) {
      stopClmServer();
    }
  }

  protected static void stopClmServer() {
    if (testCLMServer != null) {
      testCLMServer.stop();
      testCLMServer = null;
    }
  }

  @After
  public void cleanupTest() throws Exception {
    log.info("@After (AbstractBaseIntegrationTest.cleanupTest): {}", testName.getMethodName());

    databaseContainerRule.resetMocks();

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
        installLicenseOnCleanup();
      }

      if (isTestIqServerRunning()) {
        cleanTaskScheduler();
        getCLMServer().resetDisableForTesting();
        InsightConfig insightConfig = getCLMServer().getConfiguration();
        if (insightConfig != null) {
          getCLMServer().getConfiguration().setFeatures(Collections.emptyMap());
          getCLMServer().getConfiguration().setSystemAllowlist(Collections.emptyList());
        }
        // TODO this reset is probably not needed anymore since the temporary entity rules in the child classes
        // take care of this reset in a more general way
        resetProperties(SystemConfigurationProperty.BASE_URL, SystemConfigurationProperty.FORCE_BASE_URL,
            SystemConfigurationProperty.QUARANTINED_COMPONENT_REPORT_EXPIRATION_TIME_IN_HOURS,
            SystemConfigurationProperty.CSRF_PROTECTION, SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH,
            SystemConfigurationProperty.PURGE_SCAN_FILES, SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH,
            SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH, SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS,
            SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS,
            SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS,
            SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER,
            SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING,
            SystemConfigurationProperty.POLICY_MONITORING_HOUR, SystemConfigurationProperty.DB_BACKUP_DIR,
            SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE,
            SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED,
            SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING,
            SystemConfigurationProperty.ACCESS_ALLOWLIST, SystemConfigurationProperty.SCHEMA_MIGRATION_ENABLED,
            SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_INSPECTION_HOUR,
            SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_MONITORING_ENABLED,
            SystemConfigurationProperty.API_ACCESS_ALLOW_LIST
        );
      }
    }
    releaseScmPerpetualLock();
    new ProxyServerConfigurationDAO(databaseContainerRule.getOperationalDataStore()).delete();
    if (isTestIqServerRunning()) {
      ApiProxyServerConfigurationService proxyServerConfigurationService =
          lookup(ApiProxyServerConfigurationService.class);
      if (proxyServerConfigurationService != null) {
        proxyServerConfigurationService.applyProxyServerConfigurationToClients();
      }
    }

    RolePermissionDAO.resetClearRolePermissionCacheForAllOtherNodes();
  }

  protected void cleanTaskScheduler() throws Exception {
    TaskScheduler taskScheduler = testCLMServer.getCLMServer().getInstance(TaskScheduler.class);
    if (taskScheduler != null) {
      taskScheduler.standby();
      taskScheduler.clear();
    }
  }

  private void releaseScmPerpetualLock() {
    final PerpetualLockDAO perpetualLockDAO = new PerpetualLockDAO(databaseContainerRule.getOperationalDataStore());
    String perpetualLockId = SourceControlLoadBalancer.SOURCE_CONTROL_EVENT_MAINTENANCE_LOCK;
    PerpetualLock perpetualLock = perpetualLockDAO.getPerpetualLockById(perpetualLockId);
    if (perpetualLock != null) {
      perpetualLockDAO.releasePerpetualLockForOwner(perpetualLockId, perpetualLock.getOwner());
    }
  }

  protected void afterDatabaseReset() {
    // hook for subclasses to perform further cleanup action after TemporaryEntity has reset the database
  }

  protected List<Module> getBrainModules() {
    List<Module> modules = new ArrayList<>();
    modules.add(new AbstractModule()
    {
      @Override
      protected void configure() {
        AbstractBaseIntegrationTest.this.configure(binder());
      }
    });

    return modules;
  }

  /**
   * Note that overriding this method will cause the server to restart if it's already running.
   * Additionally, the server will restart again once the overridden method is no longer being used.
   */
  public void configure(final Binder binder) {
    binder.bind(ProductLicense.class).to(TestProductLicense.class);
    binder.bind(TestProductLicense.class).toInstance(testProductLicense);
    binder.bind(ProductLicenseManager.class).to(TestProductLicenseManager.class);
    binder.bind(TestProductLicenseManager.class).toInstance(licenseManager);
    binder.bind(LicenseFingerprinter.class).toInstance(licenseFingerprinter);
    binder.bind(QuartzJobStoreTX.class).to(TestQuartzJobStoreTx.class);
    binder.bind(TaskScheduler.class).to(TestTaskScheduler.class);

    // using a provider so the MockCleaner doesn't break the mocked JiraClientFactory between tests
    binder.bind(JiraClientFactory.class).toInstance(mockJiraClientFactory);
  }

  protected boolean isProxyRequiredToReachHds() {
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
    String scanId = TemporaryEntity.uuid();
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
    assertThat(response.getStatusCode()).as(
        "URI:" + response.getUrl() + ", StatusText:" + response.getStatusText() + ", ResponseBody:" +
            response.getBodyText()).isEqualTo(expectedStatus);
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

  protected void installLicenseOnCleanup() throws Exception {
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
    includeSbomManagerStagesIfNeeded(features);
    installLicense();
  }

  private static void includeSbomManagerStagesIfNeeded(final LicensedFeature[] features) {
    if (ArrayUtils.contains(features, LicensedFeature.SBOM_MANAGER)) {
      Set<StageType> stageTypes = licenseManager.getStageTypes();
      if (stageTypes == null) {
        licenseManager.setStageTypes(StageTypes.COMPLIANCE);
      }
      else {
        stageTypes = new LinkedHashSet<>(stageTypes);
        stageTypes.add(StageTypes.COMPLIANCE);
        licenseManager.setStageTypes(stageTypes.toArray(new StageType[0]));
      }
    }
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

  protected <T> T lookup(Class<T> type) {
    if (isTestIqServerRunning()) {
      return getTestCLMServer().getCLMServer().getInstance(type);
    }
    else {
      log.warn("Trying to `lookup` bean {} when the server hasn't started", type.getSimpleName());
      return null;
    }
  }

  private boolean isTestIqServerRunning() {
    return getTestCLMServer() != null && getTestCLMServer().isRunning();
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

  public void setSupportReadLimitBytes(long supportReadLimitBytes) {
    Map<String, Object> properties = new HashMap<>();
    properties.put(SystemConfigurationProperty.SUPPORT_READ_LIMIT_BYTES, supportReadLimitBytes);

    setProperties(properties);
  }

  protected void setProperties(Map<String, Object> properties) {
    ApiConfigurationService service = getCLMServer().getInstance(ApiConfigurationService.class);
    service.setConfigurationInDatabaseNoAuthz(properties);
    service.applyConfigurationToClients(properties.keySet());
  }

  public void resetProperties(String... propertyNames) {
    ApiConfigurationService service = getCLMServer().getInstance(ApiConfigurationService.class);
    if (service != null) {
      service.deleteConfigurationInDatabaseNoAuthz(propertyNames);
      service.applyConfigurationToClients(propertyNames);
    }
  }

  public void createJiraConfiguration() {
    ApiJiraConfigurationDTO dto = new ApiJiraConfigurationDTO();
    dto.url = "http://url";
    ApiJiraConfigurationService jiraConfigurationService =
        getCLMServer().getInstance(ApiJiraConfigurationService.class);
    jiraConfigurationService.setConfigurationInDatabaseNoAuthz(JsonUtils.asTree(dto));
    jiraConfigurationService.applyJiraConfigurationToClients();
  }
}
