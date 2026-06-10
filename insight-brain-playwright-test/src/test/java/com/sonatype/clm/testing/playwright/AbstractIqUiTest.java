/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import com.microsoft.playwright.assertions.LocatorAssertions;
import com.sonatype.clm.dto.model.remediation.VersionScoringDTO;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.clm.testing.functional.utils.proxy.ReverseProxyServer;
import com.sonatype.clm.testing.playwright.pages.HeaderComponent;
import com.sonatype.clm.testing.playwright.pages.LoginPage;
import com.sonatype.clm.testing.playwright.pages.LoginPageAssertions;
import com.sonatype.clm.testing.playwright.pages.UnsavedChangesModalComponent;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.clm.testing.playwright.utils.TestCredentials;
import com.sonatype.insight.brain.StaticInjectionTestHelper;
import com.sonatype.insight.brain.TestLicenseFingerprinter;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.DAOFactory;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.TestDAOFactory;
import com.sonatype.insight.brain.dataaccess.security.PersistedUserSessionDAO;
import com.sonatype.insight.brain.dataaccess.security.ShiroSessionDAO;
import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.brain.db.DatabaseProvisioner;
import com.sonatype.insight.brain.db.DefaultDatabaseContainer;
import com.sonatype.insight.brain.db.datasource.DataSourceProvider;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DataStoreProvider;
import com.sonatype.insight.brain.db.datastore.DefaultAggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DefaultDataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DefaultOperationalDataStore;
import com.sonatype.insight.brain.db.datastore.DefaultThirdPartyScansDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.SimpleDataStoreProvider;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.db.fixture.DatabaseFixture;
import com.sonatype.insight.brain.db.fixture.h2.H2DiskDatabaseFixture;
import com.sonatype.insight.brain.db.fixture.h2.H2InMemoryDatabaseFixture;
import com.sonatype.insight.brain.db.fixture.postgres.PostgresDatabaseFixture;
import com.sonatype.insight.brain.db.migrations.DatabaseMigrations;
import com.sonatype.insight.brain.db.rule.DatabaseRule.DatabaseType;
import com.sonatype.insight.brain.developer.integrationdashboard.DeveloperEnablementService;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.jira.JiraService;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.PersistedUserSession;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.product.TestProductLicenseRule;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.scheduler.QuartzConcurrencyListener;
import com.sonatype.insight.brain.scheduler.QuartzJobSchedulingService;
import com.sonatype.insight.brain.scheduler.QuartzJobStoreTX;
import com.sonatype.insight.brain.scheduler.QuartzTriggerListener;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.scheduler.TestQuartzJobStoreTx;
import com.sonatype.insight.brain.scheduler.TestTaskScheduler;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.security.EncryptionKeyStore;
import com.sonatype.insight.brain.security.FIPSModeDetector;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.security.TestEncryptionKeyStore;
import com.sonatype.insight.brain.security.TestFipsEncryptionKeyStore;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.TestCLMServer;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.brain.testing.DefaultInsightBrainServiceFactory;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.license.model.LicensedFeature;

import org.sonatype.licensing.product.ProductLicenseManager;
import org.sonatype.licensing.product.util.LicenseFingerprinter;

import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.quartz.spi.JobFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static com.sonatype.insight.brain.db.rule.DatabaseRule.DatabaseType.POSTGRES_DB;
import static com.sonatype.insight.brain.hds.VersionScoringService.HDS_BULK_SCORE_VERSIONING_PATH;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Base class for Playwright-driven UI tests that need an embedded IQ Server.
 *
 * <p>
 * Sits between {@link AbstractPlaywrightTest} (browser lifecycle only) and the concrete
 * {@code *PlaywrightTest} classes. It owns:
 * <ul>
 * <li>The embedded {@link TestCLMServer} (plus {@link ReverseProxyServer}, HDS mock, Shiro
 * security context, Guice modules, {@link TestProductLicenseManager}).</li>
 * <li>The test database lifecycle via {@link DatabaseContainer} (PostgreSQL by default;
 * H2 when {@code -DfunctionalTestDatabase=H2_IN_MEMORY_DB} or {@code H2_DISK_DB} is set).</li>
 * <li>Per-test cleanup via {@link TemporaryEntity}.</li>
 * <li>IQ-aware login/logout helpers that drive the IQ login modal.</li>
 * </ul>
 *
 * <p>
 * <b>Future MTIQ-UI reuse:</b> when MTIQ Playwright tests are added, they should live in a
 * sibling {@code AbstractMtiqUiTest} class that also extends {@link AbstractPlaywrightTest}
 * (with its own MTIQ-flavoured server bootstrap and credentials). Single inheritance means we
 * cannot share *infrastructure* through the hierarchy beyond what {@link AbstractPlaywrightTest}
 * provides; if the IQ↔MTIQ overlap inside this class grows large enough to be painful, extract
 * the shared parts into a composable rule or helper rather than introducing another inheritance
 * layer.
 */
@Category(SlowTest.class)
public abstract class AbstractIqUiTest
    extends AbstractPlaywrightTest
{
  private static final Logger log = LoggerFactory.getLogger(AbstractIqUiTest.class);

  protected static final TestProductLicenseManager productLicenseManager;

  protected static final TestLicenseFingerprinter licenseFingerprinter;

  protected static final TestProductLicense testProductLicense;

  protected static final JiraService jiraService;

  protected static final TestCLMServer testCLMServer;

  protected static final ReverseProxyServer reverseProxyServer;

  protected static DatabaseContainer databaseContainer;

  private static TestProductLicenseRule testProductLicenseRule;

  /**
   * Map from class to mock instance — individual test methods add mocks here and the map is
   * cleared at the end of each test in {@code cleanUpInsightBrainConfigChanges()}.
   *
   * <p>
   * <b>Threading model:</b> JUnit 4 runs methods within a class sequentially by default, but
   * Surefire/Failsafe can launch multiple test classes in parallel forks. Each fork gets its
   * own classloader and therefore its own copy of this static field, so a plain {@link
   * HashMap} is safe in the standard configuration. If this module ever opts into
   * intra-class parallelism, swap this for a {@link java.util.concurrent.ConcurrentHashMap}.
   * The reference is {@code final} so tests cannot accidentally reassign it.
   */
  protected static final Map<Class<?>, Object> mocks = new HashMap<>();

  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  private PersistedUserSessionDAO persistedUserSessionDAO;

  private ShiroSessionDAO shiroSessionDAO;

  protected static DeveloperEnablementService mockDeveloperEnablementService = mock(DeveloperEnablementService.class);

  static {
    databaseContainer = createDatabaseContainer();
    initDatabase();

    productLicenseManager = new TestProductLicenseManager();
    licenseFingerprinter = new TestLicenseFingerprinter();
    testProductLicense = new TestProductLicense(productLicenseManager, mockDeveloperEnablementService);
    when(mockDeveloperEnablementService.shouldEnableDeveloperProduct()).thenReturn(true);
    testProductLicenseRule = new TestProductLicenseRule(databaseContainer);
    jiraService = Mockito.mock(JiraService.class);
    initMocks();

    String contextPath = TestCLMServer.WEBPACK_DEV_MODE ? "" : System.getProperty("iq.contextPath", "/iq-test");
    if (!contextPath.isEmpty()) {
      System.setProperty("iq.contextPath", contextPath);
    }
    Configurator configurator = config -> {
    };

    testCLMServer = new TestCLMServer(new DefaultInsightBrainServiceFactory(),
        false /* isProxyRequiredToReachHds */, List.of(PlaywrightTestConfiguration.class), configurator,
        databaseContainer);
    int reverseProxyTarget = TestCLMServer.WEBPACK_DEV_MODE ? 8070 : testCLMServer.getCLMServer().getPort();
    reverseProxyServer = new ReverseProxyServer(reverseProxyTarget);

    try {
      testProductLicenseRule.insertLicenseIfNeeded();

      testCLMServer.start();
      reverseProxyServer.start();

      baseUrlFromTest = BaseUrl.resolveBaseUrl(getBaseUrl(contextPath));
      setBaseUrl(baseUrlFromTest);
    }
    catch (Throwable e) {
      // Static initializer failure — we cannot proceed (no test class would run with a half-
      // initialised TestCLMServer / reverse proxy / license fingerprinter). Log via SLF4J so
      // CI captures the cause in the same stream as the rest of the test logs, then abort.
      log.error("Failed to start embedded IQ infrastructure in static initializer", e);
      System.exit(1);
    }
  }

  private static DatabaseContainer createDatabaseContainer() {
    String functionalTestDatabase =
        System.getProperty("functionalTestDatabase", System.getenv("FUNCTIONAL_TESTS_DATABASE"));

    DatabaseType databaseType = POSTGRES_DB;
    if (functionalTestDatabase != null) {
      databaseType = DatabaseType.valueOf(functionalTestDatabase.toUpperCase());
    }

    DatabaseFixture fixture = switch (databaseType) {
      case H2_IN_MEMORY_DB -> new H2InMemoryDatabaseFixture(false, false, null);
      case H2_DISK_DB -> new H2DiskDatabaseFixture(50, null, null);
      case POSTGRES_DB -> new PostgresDatabaseFixture("testPostgresFixture", false, 50);
    };

    DataSourceProvider dataSourceProvider = fixture.getDataSourceProvider();

    OperationalDataStore operationalDataStore =
        new DefaultOperationalDataStore(dataSourceProvider, fixture.getDatabaseConfig(DatabaseName.ods.name()));
    AggregationDataStore aggregationDataStore =
        new DefaultAggregationDataStore(dataSourceProvider, fixture.getDatabaseConfig(DatabaseName.aggregation.name()));
    DataMartDataStore dataMartDataStore =
        new DefaultDataMartDataStore(dataSourceProvider, fixture.getDatabaseConfig(DatabaseName.dm.name()));
    ThirdPartyScansDataStore thirdPartyScansDataStore = new DefaultThirdPartyScansDataStore(dataSourceProvider,
        fixture.getDatabaseConfig(DatabaseName.third_party_scans.name()));

    DataStoreProvider dataStoreProvider =
        new SimpleDataStoreProvider(operationalDataStore, aggregationDataStore, dataMartDataStore,
            thirdPartyScansDataStore);

    DatabaseMigrations databaseMigrations = new DatabaseMigrations(dataStoreProvider);
    DatabaseProvisioner databaseProvisioner = new DatabaseProvisioner(dataStoreProvider, databaseMigrations);
    return new DefaultDatabaseContainer(dataSourceProvider, dataStoreProvider, databaseProvisioner);
  }

  private static void initDatabase() {
    DatabaseProvisioner databaseProvisioner = databaseContainer.getDatabaseProvisioner();
    databaseProvisioner.initializeDatabaseWithMigration();
  }

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity(databaseContainer)
  {
    @Override
    public void after() {
      super.after();
      afterDatabaseReset();
    }

    @Override
    public void initializePersistedUserSessions() {
      // noop
    }

    @Override
    public void cleanupPersistedUserSessions() {
      // noop
    }
  };

  protected void afterDatabaseReset() {
    // hook for subclasses
  }

  private static String getBaseUrl(String contextPath) {
    String url = reverseProxyServer.getUrl();
    if (url.endsWith("/")) {
      url = url.substring(0, url.length() - 1);
    }
    url += contextPath;
    if (!url.endsWith("/")) {
      url += '/';
    }
    return url;
  }

  public static void setBaseUrl(String baseUrl) {
    baseUrlFromTest = baseUrl;
    ApiConfigurationService service = testCLMServer.getCLMServer().getInstance(ApiConfigurationService.class);
    service.setConfigurationNoAuthz(SystemConfigurationProperty.BASE_URL, baseUrl);
  }

  public static void setEnableDefaultPasswordWarning(boolean enableDefaultPasswordWarning) {
    ApiConfigurationService service = testCLMServer.getCLMServer().getInstance(ApiConfigurationService.class);
    service.setConfigurationNoAuthz(SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING,
        enableDefaultPasswordWarning);
  }

  private static void initMocks() {
    try {
      Mockito.reset(jiraService);
      Mockito.when(jiraService.isEnabled()).thenReturn(false);
      Mockito.doThrow(new IllegalStateException()).when(jiraService).getProjectsWithAcceptableIssueTypes();
    }
    catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @BeforeClass
  public static void disableWaitToCloseOldClients() {
    HdsClient.waitToCloseOldClients = false;
  }

  @BeforeClass
  public static void setUpClass() {
    Subject subject = mock(Subject.class);
    lenient().when(subject.getPrincipal()).thenReturn(new UserPrincipal("admin", "Admin", InternalRealm.ID));
    lenient().when(subject.associateWith(any(Runnable.class))).thenAnswer(invocation -> invocation.getArgument(0));
    lenient().when(subject.associateWith(any(Callable.class))).thenAnswer(invocation -> invocation.getArgument(0));
    SecurityManager securityManager = mock(SecurityManager.class);
    lenient().when(securityManager.createSubject(any(SubjectContext.class))).thenReturn(subject);
    ThreadContext.bind(securityManager);
    ThreadContext.bind(subject);

    testProductLicenseRule.insertLicenseIfNeeded();
    testCLMServer.getCLMServer().setHdsUrl();
  }

  @AfterClass
  public static void tearDownClass() {
    ThreadContext.unbindSecurityManager();
    ThreadContext.unbindSubject();
  }

  @Before
  public final void beforeTest() {
    log.info("Before: {}", testName.getMethodName());
    setEnableDefaultPasswordWarning(false);
    setBaseUrl(baseUrlFromTest);

    persistedUserSessionDAO = lookup(PersistedUserSessionDAO.class);
    shiroSessionDAO = lookup(ShiroSessionDAO.class);

    DAOFactory daoFactory = new TestDAOFactory(databaseContainer);
    StaticInjectionTestHelper.inject(daoFactory);

    mockHdsVersionScoringResponse();
  }

  @After
  public void afterTest() throws Exception {
    log.info("After: {}", testName.getMethodName());
    mocks.clear();
    InsightConfig insightConfig = testCLMServer.getCLMServer().getConfiguration();
    if (insightConfig != null) {
      insightConfig.setFeatures(Collections.emptyMap());
    }
    TaskScheduler taskScheduler = testCLMServer.getCLMServer().getInstance(TaskScheduler.class);
    if (taskScheduler != null) {
      taskScheduler.standby();
      taskScheduler.clear();
    }
    initMocks();
    if (!testCLMServer.isRunning()) {
      testCLMServer.start();
    }
    testCLMServer.getHdsServer().reset();
    if (productLicenseManager.wasChanged()) {
      productLicenseManager.reset();
      installLicense();
    }
  }

  @Configuration
  static class PlaywrightTestConfiguration
  {
    @Bean
    @Primary
    public ProductLicense productLicense() {
      return testProductLicense;
    }

    @Bean
    public ProductLicenseManager productLicenseManager() {
      return productLicenseManager;
    }

    @Bean
    public LicenseFingerprinter licenseFingerprinter() {
      return licenseFingerprinter;
    }

    @Bean
    @Primary
    public JiraService jiraService() {
      return jiraService;
    }

    @Bean
    @Primary
    public EncryptionKeyStore encryptionKeyStore() {
      return () -> FIPSModeDetector.isEnabled()
          ? new TestFipsEncryptionKeyStore().getKey()
          : new TestEncryptionKeyStore().getKey();
    }

    @Bean
    @Primary
    public QuartzJobStoreTX quartzJobStoreTX(
        ProductLicense productLicense,
        InsightConfig insightConfig,
        OperationalDataStore operationalDataStore) throws Exception
    {
      return new TestQuartzJobStoreTx(productLicense, insightConfig, operationalDataStore);
    }

    @Bean
    @Primary
    public TaskScheduler taskScheduler(
        QuartzJobStoreTX quartzJobStoreTX,
        JobFactory jobFactory,
        QuartzTriggerListener quartzTriggerListener,
        QuartzConcurrencyListener quartzConcurrencyListener,
        OperationalDataStore operationalDataStore,
        ShutdownHandler shutdownHandler,
        QuartzJobSchedulingService quartzJobSchedulingService)
    {
      return new TestTaskScheduler(
          quartzJobStoreTX,
          jobFactory,
          quartzTriggerListener,
          quartzConcurrencyListener,
          operationalDataStore,
          shutdownHandler,
          quartzJobSchedulingService);
    }
  }

  public String getUsername() {
    return getClass().getSimpleName();
  }

  public User createUser() {
    return tempEntity.newUser(getUsername());
  }

  public void grantPermissions(String username, String contextId, Permission... perms) {
    Role role = tempEntity.newRole(false /* global */, perms);
    tempEntity.newMembershipMapping(contextId, role.getId(), username);
  }

  protected void setFeatures(LicensedFeature... features) {
    productLicenseManager.setFeatures(features);
    installLicense();
  }

  protected void setMissingFeature(LicensedFeature feature) {
    setFeatures(EnumSet.complementOf(EnumSet.of(feature)).toArray(new LicensedFeature[0]));
  }

  protected void setMissingFeatures(LicensedFeature... features) {
    setFeatures(EnumSet.complementOf(EnumSet.copyOf(Arrays.asList(features))).toArray(new LicensedFeature[0]));
  }

  protected void setLicensedProducts(String... products) {
    productLicenseManager.setProducts(products);
    installLicense();
  }

  protected void setExpirationDate(Date date) {
    productLicenseManager.setExpirationDate(date);
    installLicense();
  }

  protected void installLicense() {
    testProductLicenseRule.insertLicenseIfNeeded();
    try {
      lookup(CLMLicenseManager.class)
          .installLicense(new ByteArrayInputStream(new byte[1]));
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected void uninstallLicense() {
    lookup(CLMLicenseManager.class).uninstallLicense();
  }

  protected void cleanupAllPersistedUserSessions() {
    persistedUserSessionDAO.getAll()
        .stream()
        .map(PersistedUserSession::getId)
        .forEach(shiroSessionDAO::deleteById);
  }

  protected <T> T lookup(Class<T> type) {
    return testCLMServer.getCLMServer().getInstance(type);
  }

  private void mockHdsVersionScoringResponse() {
    testCLMServer.getHdsServer()
        .respondWith(new VersionScoringDTO[]{})
        .atUri(HDS_BULK_SCORE_VERSIONING_PATH);
  }

  protected void mockHdsResponseForDownloadingReport(String scanId) {
    URL zippedReport = ReportHelper.zipReport("/canned-reports/large-report", tempDir);
    testCLMServer.getHdsServer()
        .respondWith(zippedReport)
        .atUri("rest/application/analysis/" + scanId);
  }

  /**
   * Login as admin using default credentials.
   */
  protected void playwrightLogin() {
    playwrightLogin(TestCredentials.ADMIN_USERNAME, TestCredentials.ADMIN_PASSWORD);
  }

  /**
   * Login with the given credentials via the IQ login modal.
   * Does NOT navigate — the caller must have already navigated to a page
   * where the login modal will appear (matching the Selenide {@code login()} behavior).
   *
   * <p>
   * Waits for the authenticated header before returning — see
   * {@link #waitForAuthenticatedHeader} for the race condition this guards against.
   */
  protected void playwrightLogin(String username, String password) {
    log.debug("Logging in as '{}' on current page (url='{}')", username, page.url());
    new LoginPage().loginAs(username, password);
    waitForAuthenticatedHeader();
    log.debug("Login successful for user: {}", username);
  }

  /**
   * Wait for the authenticated header (specifically the user menu) to be visible.
   *
   * <p>
   * After {@link LoginPage#loginAs} returns, the modal is gone and the submit mask has been
   * dismissed — but the Redux auth slice may not yet be hydrated and the header may not have
   * re-rendered into its authenticated layout. Tests that immediately call
   * {@link AbstractPlaywrightTest#playwrightRefreshOrOpen} would race the SPA: a same-document
   * hash change (e.g. {@code #/management/view/...} → {@code #/management/edit/...}) does not
   * trigger a document load, so the navigate returns instantly while route guards run async.
   * If a guard fires before {@code isLoggedIn} is true, it issues a 401 and redirects back to
   * a safe landing page — surfacing later as "{@code #policy-editor-summary} not visible".
   *
   * <p>
   * Waiting for the {@code #user-menu} element ({@code MenuBar.jsx} only mounts {@code
   * <UserMenu />} when {@code isLoggedIn === true}) gives a single, cheap signal that the
   * authenticated header has actually rendered.
   *
   * <p>
   * Call this from every login helper that returns control to a test, so subsequent
   * navigations are not racing async auth hydration.
   */
  private void waitForAuthenticatedHeader() {
    assertThat(new HeaderComponent().userMenu())
        .isVisible(new LocatorAssertions.IsVisibleOptions()
            .setTimeout(PlaywrightTiming.MODAL_OR_LOGIN_TIMEOUT_MS));
  }

  /**
   * Logout the current user via the user menu.
   * Handles the unsaved changes modal if it appears.
   */
  protected void playwrightLogout() {
    playwrightLogout(true);
  }

  private void playwrightLogout(boolean dismissUnsavedModal) {
    new HeaderComponent().logout();
    if (dismissUnsavedModal) {
      new UnsavedChangesModalComponent().dismissIfAppearsWithin(PlaywrightTiming.SHORT_UI_CUE_MS);
    }
    new LoginPageAssertions(new LoginPage()).shouldBeVisibleWithin(PlaywrightTiming.MODAL_OR_LOGIN_TIMEOUT_MS);
  }

  /**
   * Login with a non-admin user at the specified page.
   *
   * <p>
   * Waits for the authenticated header before returning — see
   * {@link #waitForAuthenticatedHeader} for the race condition this guards against.
   */
  protected void playwrightLoginAt(String path, String username, String password) {
    playwrightRefreshOrOpen(path);
    new LoginPage().loginAs(username, password);
    waitForAuthenticatedHeader();
  }

  /**
   * Login as admin at a specific page URL. Delegates to {@link #playwrightLoginAt} so the
   * authenticated-header guard applies transitively.
   */
  protected void playwrightLoginAdminAt(String path) {
    playwrightLoginAt(path, TestCredentials.ADMIN_USERNAME, TestCredentials.ADMIN_PASSWORD);
  }

}
