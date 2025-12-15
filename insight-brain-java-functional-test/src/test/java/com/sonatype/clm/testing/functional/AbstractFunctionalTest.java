/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional;

import com.codeborne.selenide.Condition;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import com.sonatype.clm.dto.model.remediation.VersionScoringDTO;
import com.sonatype.clm.testing.functional.elements.LoginModal;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.NxSubmitMask;
import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.elements.UnsavedModal;
import com.sonatype.clm.testing.functional.elements.UserMenu;
import com.sonatype.clm.testing.functional.utils.SeleniumTestContainer;
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
import com.sonatype.insight.brain.scheduler.QuartzJobStoreTX;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.scheduler.TestQuartzJobStoreTx;
import com.sonatype.insight.brain.scheduler.TestTaskScheduler;
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
import com.sonatype.insight.test.reverseproxy.ReverseProxyServer;
import org.sonatype.licensing.product.ProductLicenseManager;
import org.sonatype.licensing.product.util.LicenseFingerprinter;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.FileDownloadMode;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.ex.UIAssertionError;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.matcher.Matchers;
import io.dropwizard.core.server.DefaultServerFactory;
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
import org.junit.rules.TestName;
import org.mockito.Mockito;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.sonatype.clm.testing.functional.utils.BaseUrl.resolveBaseUrl;
import static com.sonatype.clm.testing.functional.utils.SeleniumChromeOptions.chromeOptions;
import static com.sonatype.insight.brain.db.rule.DatabaseRule.DatabaseType.POSTGRES_DB;
import static com.sonatype.insight.brain.hds.VersionScoringService.HDS_BULK_SCORE_VERSIONING_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(SlowTest.class)
public abstract class AbstractFunctionalTest
{
  private static final Logger log = LoggerFactory.getLogger(AbstractFunctionalTest.class);

  private static final int VIEWPORT_WIDTH = 1366;

  private static final int VIEWPORT_HEIGHT = 1064;

  protected static final TestProductLicenseManager productLicenseManager;

  protected static final TestLicenseFingerprinter licenseFingerprinter;

  protected static final TestProductLicense testProductLicense;

  protected static final JiraService jiraService;

  protected static final TestCLMServer testCLMServer;

  protected static final ReverseProxyServer reverseProxyServer;

  protected static DatabaseContainer databaseContainer;

  private static TestProductLicenseRule testProductLicenseRule;

  // The base URL of the IQ server usable from the test code, not from the containerized browser.
  // in contrast, Configuration.baseUrl is the URL of the IQ server as seen from the containerized browser and should
  // not be used to reference the server from the test code.
  protected static String baseUrlFromTest;

  /**
   * A map from class to mock instance.
   * <br /><br />
   * Individual test methods should add mocks to this map if needed.
   * <br /><br />
   * Mock instances are cleared at the end of each test.
   */
  protected static Map<Class<?>, Object> mocks = new HashMap<>();

  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Rule
  public EyesWatcher eyesWatcher = new EyesWatcher(); // enables visual testing

  @Rule
  public TestName testName = new TestName();

  private PersistedUserSessionDAO persistedUserSessionDAO;

  private ShiroSessionDAO shiroSessionDAO;

  protected static DeveloperEnablementService mockDeveloperEnablementService = mock(DeveloperEnablementService.class);

  static {
    // Creating a Database Container and initializing the DB that will be used for the entire functional test suite.
    // This MUST happen before the server start or before the TemporaryEntity before method is called
    databaseContainer = createDatabaseContainer();
    initDatabase();

    productLicenseManager = new TestProductLicenseManager();
    licenseFingerprinter = new TestLicenseFingerprinter();
    testProductLicense = new TestProductLicense(productLicenseManager, mockDeveloperEnablementService);
    when(mockDeveloperEnablementService.shouldEnableDeveloperProduct()).thenReturn(true);
    testProductLicenseRule = new TestProductLicenseRule(databaseContainer);
    jiraService = Mockito.mock(JiraService.class);
    initMocks();

    // Reuse the configurator to allow reuse of MTIQ server
    String contextPath = System.getProperty("iq.contextPath", "/iq-test");
    Configurator configurator = config -> {
      ((DefaultServerFactory) config.getServerFactory()).setApplicationContextPath(contextPath);
    };

    testCLMServer = new TestCLMServer(new DefaultInsightBrainServiceFactory(),
        false /* isProxyRequiredToReachHds */, getBrainModules(), configurator, databaseContainer);
    reverseProxyServer = new ReverseProxyServer(testCLMServer.getCLMServer().getPort());

    try {
      // Insert license so it can be populated on server start-up - can't use a @Rule because this functional test is
      // setup with a static database
      testProductLicenseRule.insertLicenseIfNeeded();

      testCLMServer.start();
      reverseProxyServer.start();

      baseUrlFromTest = resolveBaseUrl(getBaseUrl(contextPath));

      if ("docker".equals(System.getProperty("run-functional-tests"))) {
        Configuration.baseUrl = SeleniumTestContainer.start(baseUrlFromTest);
      }
      else {
        Configuration.baseUrl = baseUrlFromTest;
      }

      Configuration.reportsFolder = "target/selenide-reports";
      Configuration.downloadsFolder = "target/selenide-downloads";

      // Use the actual browser file-download-to-folder mechanism rather than the default mode of Selenide grabbing
      // and fetching the href URL of a link. The FOLDER mode works in more situations, such as downloads triggered
      // by a <button> and javascript
      Configuration.fileDownload = FileDownloadMode.FOLDER;
      setBaseUrl(Configuration.baseUrl);
    }
    catch (Throwable e) {
      e.printStackTrace();
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
    // hook for subclasses to perform further cleanup action after TemporaryEntity has reset the database
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
    setupWebDriver();
    Subject subject = mock(Subject.class);
    lenient().when(subject.getPrincipal()).thenReturn(new UserPrincipal("admin", "Admin", InternalRealm.ID));
    SecurityManager securityManager = mock(SecurityManager.class);
    lenient().when(securityManager.createSubject(any(SubjectContext.class))).thenReturn(subject);
    ThreadContext.bind(securityManager);
    ThreadContext.bind(subject);

    // Set the default product license - can't use a @Rule because this functional test is setup with a static database
    testProductLicenseRule.insertLicenseIfNeeded();
    testCLMServer.getCLMServer().setHdsUrl();
  }

  protected static void setupWebDriver() {
    if (Configuration.browser.equalsIgnoreCase("chrome")) {

      Configuration.browserCapabilities = new DesiredCapabilities();
      Configuration.browserCapabilities
          .setCapability(ChromeOptions.CAPABILITY,
              chromeOptions(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, Configuration.headless));
    }

    WebDriver driver = WebDriverRunner.getAndCheckWebDriver();

    // Enforcing specific view port size for stable applitools validations.
    setViewportSize(driver);
  }

  @AfterClass
  public static void tearDownClass() {
    ThreadContext.unbindSecurityManager();
    ThreadContext.unbindSubject();
    hardreset();
  }

  public static void hardreset() {
    WebDriverRunner.getWebDriver().manage().deleteAllCookies();
  }

  @Before
  public final void beforeTest() {
    log.info("Before: {}", testName.getMethodName());
    setEnableDefaultPasswordWarning(false);
    setBaseUrl(Configuration.baseUrl);

    persistedUserSessionDAO = lookup(PersistedUserSessionDAO.class);
    shiroSessionDAO = lookup(ShiroSessionDAO.class);

    // Re-inject classes that have static dependencies
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
    tryOpenSidebarNav();
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
    // so we aren't on app between page loads
    navigate(() -> {
      Selenide.open("about");
      clearAlerts();
      closeOtherWindows();
      return true;
    });
  }

  private void tryOpenSidebarNav() {
    try {
      // restore sidebar to open state if available
      if (SidebarNavigation.container().is(visible)) {
        SidebarNavigation.openNavigationSidebar();
      }
    }
    catch (Exception | UIAssertionError unexpectedException) {
      // there might be an element interfering with the click but since we are not sure of it's nature we'll ignore
      log.debug("Attempted to return the header to open but failed", unexpectedException);
    }
  }

  @SuppressWarnings("unchecked")
  protected static void setViewportSize(WebDriver driver) {
    JavascriptExecutor executor = (JavascriptExecutor) WebDriverRunner.getWebDriver();
    // get the windows size for the specified view port
    @SuppressWarnings("rawtypes")
    List<Long> sizes = (List) executor.executeScript(
        "return [window.outerWidth - window.innerWidth + arguments[0], " +
            "window.outerHeight - window.innerHeight + arguments[1]];",
        VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
    driver.manage().window().setSize(new Dimension(sizes.get(0).intValue(), sizes.get(1).intValue()));
  }

  private static List<Module> getBrainModules() {
    return Collections.singletonList(new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(ProductLicense.class).toInstance(testProductLicense);
        bind(ProductLicenseManager.class).to(TestProductLicenseManager.class);
        bind(TestProductLicenseManager.class).toInstance(productLicenseManager);
        bind(LicenseFingerprinter.class).toInstance(licenseFingerprinter);
        bind(TestProductLicenseManager.class).toInstance(productLicenseManager);
        bind(JiraService.class).toInstance(jiraService);
        bind(QuartzJobStoreTX.class).to(TestQuartzJobStoreTx.class);
        bind(TaskScheduler.class).to(TestTaskScheduler.class);

        // Bind EncryptionKeyStore to use consistent keys for FIPS and non-FIPS tests
        bind(EncryptionKeyStore.class).toInstance(() -> {
          if (FIPSModeDetector.isEnabled()) {
            return new TestFipsEncryptionKeyStore().getKey();
          }
          else {
            return new TestEncryptionKeyStore().getKey();
          }
        });

        // Bind an interceptor to intercept method calls to classes that can normally be mocked / spied
        // i.e. not final and containing a non-private constructor or no constructor.
        // 
        // When a method is intercepted, get its declaring class and check if there is a mock object for that class.
        // If there is, invoke the method on the mock object instead.
        //
        // Using an interceptor allows us to change mocks/spies without having to restart the server.
        bindInterceptor(AbstractFunctionalTest::isInterceptable, Matchers.any(),
            invocation -> {
              Object object = mocks.get(invocation.getMethod().getDeclaringClass());
              if (object == null || invocation.getThis() == object) {
                return invocation.proceed();
              }
              return invocation.getMethod().invoke(object, invocation.getArguments());
            });
      }
    });
  }

  private static boolean isInterceptable(final Class<?> clazz) {
    return !Modifier.isFinal(clazz.getModifiers()) && !Arrays.stream(clazz.getConstructors())
        .allMatch(constructor -> Modifier.isPrivate(constructor.getModifiers()));
  }

  protected static void loginAsAdmin() {
    login("admin", "admin123");
  }

  protected void login() {
    login(getUsername(), TemporaryEntity.USER_PASSWORD_CLEAR);
  }

  protected static void login(String username, String password) {
    LoginModal loginModal = new LoginModal();
    loginModal.shouldBe(visible);
    loginModal.username().setValue(username);
    loginModal.password().setValue(password);
    loginModal.loginButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    loginModal.shouldBe(hidden);
  }

  protected static void logoutAndDontIgnoreUnsavedChangesModal() {
    logout(false);
  }

  protected static void logout() {
    logout(true);
  }

  private static void logout(boolean shouldIgnoreUnsavedChangesModal) {
    UserMenu userMenu = MainHeader.userMenu();
    userMenu.dropdownToggle().shouldBe(visible).click();
    userMenu.logout().should(appear).click();
    WebDriver webDriver = WebDriverRunner.getWebDriver();
    if (shouldIgnoreUnsavedChangesModal) {
      try {
        webDriver.findElement(By.id("unsaved-modal"));
        UnsavedModal unsavedModal = new UnsavedModal();
        unsavedModal.shouldBe(visible);
        unsavedModal.continueButton().click();
      }
      catch (NoSuchElementException e) {
        // do nothing
      }
      // CLM-34380. After refactoring the logout process from angular code to react code, process is async
      // and we need to wait for the login alert to be present.
      waitUntilLoginDialogAppears();
      userMenu.shouldNotBe(visible);
    }
  }

  /**
   * Helper method to get the text out of an expected input validation popover.
   */
  protected String popoverText(SelenideElement element) {
    return popoverViolations(element).shouldBe(visible).text();
  }

  /**
   * Find all popover violation messages in a given element. Intended to confirm the presence/absence of violations in a
   * form.
   *
   * @throws NoSuchElementException if the element was not found
   */
  protected SelenideElement popoverViolations(SelenideElement element) {
    return $('#' + element.attr("name") + "-popover.in");
  }

  /**
   * Find all popover violation messages in a given element as a list. Intended to confirm the presence/absence of
   * violations in a form. If not found this will return an empty collection.
   */
  protected ElementsCollection popoverViolationsList(SelenideElement element) {
    return $$('#' + element.attr("name") + "-popover.in");
  }

  protected static void refresh() {
    navigate(() -> {
      log.info("Refreshing page {}", WebDriverRunner.getWebDriver().getCurrentUrl());
      WebDriverRunner.getWebDriver().navigate().refresh();
      clearAlerts();
      return true;
    });
  }

  protected static void refreshOrOpen(String url) {
    navigate(() -> {
      String currentUrl = WebDriverRunner.getWebDriver().getCurrentUrl();
      if (currentUrl != null && currentUrl.endsWith(url)) {
        log.info("Refreshing page {}", currentUrl);
        WebDriverRunner.getWebDriver().navigate().refresh();
        return true;
      }
      else {
        log.info("Opening page {}", url);
        Selenide.open(url);
        return !digestUrl(currentUrl).equals(digestUrl(url));
      }
    });
  }

  private static String digestUrl(final String url) {
    try {
      return new URI(url).getSchemeSpecificPart();
    }
    catch (Exception e) {
      return "";
    }
  }

  /**
   * Performs the specified browser navigation and waits for the current page to get dismissed if the navigation causes
   * a full page reload, thereby ensuring future interactions do not mistake the old page for the new page.
   */
  private static void navigate(BooleanSupplier navigation) {
    waitUntilUrlStable();
    WebElement body = getWebElement("body");
    boolean fullPageReload = navigation.getAsBoolean();
    if (!fullPageReload || body == null) {
      return;
    }
    try {
      Selenide.Wait().withMessage("Page body did not update").until(webDriver -> {
        try {
          body.isDisplayed();
          return false;
        }
        catch (StaleElementReferenceException e) {
          return true;
        }
        catch (UnhandledAlertException e) {
          clearAlerts();
          return false;
        }
      });
    }
    catch (TimeoutException e) {
      throw (UIAssertionError) UIAssertionError.wrap(WebDriverRunner.driver(), e, Configuration.timeout);
    }
  }

  private static WebElement getWebElement(final String selector) {
    try {
      return $(selector).toWebElement();
    }
    catch (NoSuchElementException e) {
      return null;
    }
  }

  /**
   * Some URLs denote interim page states that route to another page state. Until the final page state is reached, we
   * cannot reliably navigate the browser.
   */
  private static void waitUntilUrlStable() {
    waitUntil(webDriver -> {
      String url = webDriver.getCurrentUrl();
      try {
        assertThat(url).doesNotEndWith("/assets/index.html").doesNotEndWith("/assets/index.html#/management/view");
      }
      catch (AssertionError e) {
        // interim URL, unless ...
        // ... the login modal is shown
        try {
          assertThat(webDriver.findElement(By.id("iq-login-modal")).isDisplayed()).isTrue();
          // ... and not currently performing a login
          try {
            assertThat(webDriver.findElement(By.cssSelector(".nx-submit-mask")).isDisplayed()).isTrue();
          }
          catch (AssertionError | NoSuchElementException | StaleElementReferenceException ignored) {
            return;
          }
        }
        catch (AssertionError | NoSuchElementException | StaleElementReferenceException suppressed) {
          e.addSuppressed(suppressed);
        }
        // ... or the management view
        if (url.endsWith("#/management/view")) {
          // ... has finished loading
          try {
            assertThat(webDriver.findElement(By.id("owner-tree-view-owner-rows")).isDisplayed()).isTrue();
            // ... and nothing to redirect to
            try {
              assertThat(webDriver.findElement(By.cssSelector(".owner-tree-view__row--organization")).isDisplayed())
                  .isTrue();
            }
            catch (AssertionError | NoSuchElementException | StaleElementReferenceException ignored) {
              return;
            }
          }
          catch (AssertionError | NoSuchElementException | StaleElementReferenceException suppressed) {
            e.addSuppressed(suppressed);
          }
        }
        throw e;
      }
    });
  }

  protected static void waitUntilUrl(final String url) {
    waitUntil(webDriver -> assertThat(webDriver.getCurrentUrl()).isEqualTo(url));
  }

  protected static void waitUntilNotUrl(final String url) {
    waitUntil(webDriver -> assertThat(webDriver.getCurrentUrl()).isNotEqualTo(url));
  }

  protected static void waitUntilLoginDialogAppears() {
    waitUntil(webDriver -> assertThat(new LoginModal()).actual().shouldBe(Condition.visible));
  }

  private static void waitUntil(Consumer<WebDriver> assertion) {
    try {
      Selenide.Wait().ignoring(AssertionError.class).until(webDriver -> {
        assertion.accept(webDriver);
        return true;
      });
    }
    catch (TimeoutException e) {
      throw (UIAssertionError) UIAssertionError.wrap(WebDriverRunner.driver(), e, Configuration.timeout);
    }
  }

  protected static void clearAlerts() {
    WebDriver driver = WebDriverRunner.getWebDriver();
    try {
      Alert alert = driver.switchTo().alert();
      log.debug("Clearing alert: {}", alert.getText());
      alert.accept();
    }
    catch (NoAlertPresentException e) {
      // do nothing
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

  protected static void executeJavaScript(String script) {
    WebDriver driver = WebDriverRunner.getWebDriver();
    JavascriptExecutor js = (JavascriptExecutor) driver;
    js.executeScript(script);
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

  // Close all tabs/windows except the currently active one.
  // Thanks to https://stackoverflow.com/a/18504970 for the code
  private void closeOtherWindows() {
    WebDriver driver = WebDriverRunner.getWebDriver();
    String currentHandle = driver.getWindowHandle();

    for (String handle : driver.getWindowHandles()) {
      if (!handle.equals(currentHandle)) {
        driver.switchTo().window(handle);
        driver.close();
      }
    }

    driver.switchTo().window(currentHandle);
  }

  protected void cleanupAllPersistedUserSessions() {
    persistedUserSessionDAO.getAll().stream().map(PersistedUserSession::getId)
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
}
