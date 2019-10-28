/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.LoginModal;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.UserMenu;
import com.sonatype.clm.testing.functional.utils.PageTweakingWebDriver;
import com.sonatype.insight.brain.TestLicenseFingerprinter;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.jira.JiraService;
import com.sonatype.insight.brain.migration.RootOrganizationConfigMigrationUtils;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.TestCLMServer;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.test.reverseproxy.ReverseProxyServer;

import org.sonatype.licensing.product.ProductLicenseManager;
import org.sonatype.licensing.product.util.LicenseFingerprinter;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.ex.UIAssertionError;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import io.dropwizard.server.DefaultServerFactory;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
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
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.sonatype.clm.testing.functional.utils.BaseUrl.resolveBaseUrl;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

public abstract class AbstractFunctionalTest
{
  private static Logger log = LoggerFactory.getLogger(AbstractFunctionalTest.class);

  protected static final TestProductLicenseManager productLicenseManager;

  protected static final TestLicenseFingerprinter licenseFingerprinter;

  protected static final TestProductLicense testProductLicense;

  protected static final RootOrganizationConfigMigrationUtils rootOrganizationConfigMigrationUtils;

  protected static final JiraService jiraService;

  protected static TestCLMServer testCLMServer;

  protected static ReverseProxyServer reverseProxyServer;
  
  private static final int VIEWPORT_WIDTH = 1366;
  
  private static final int VIEWPORT_HEIGHT = 1024;

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

  static {
    productLicenseManager = new TestProductLicenseManager();
    licenseFingerprinter = new TestLicenseFingerprinter();
    testProductLicense = new TestProductLicense(productLicenseManager);
    rootOrganizationConfigMigrationUtils = Mockito.mock(RootOrganizationConfigMigrationUtils.class);
    jiraService = Mockito.mock(JiraService.class);
    initMocks();

    String contextPath = System.getProperty("iq.contextPath", "/iq-test");
    testCLMServer = new TestCLMServer(false /* isProxyRequiredToReachHds */, getBrainModules(), new Configurator()
    {
      @Override
      public void configure(InsightConfig config) {
        config.setBaseUrl(Configuration.baseUrl);
        ((DefaultServerFactory) config.getServerFactory()).setApplicationContextPath(contextPath);
      }
    });
    reverseProxyServer = new ReverseProxyServer(testCLMServer.getCLMServer().getPort());

    try {
      testCLMServer.start();
      reverseProxyServer.start();

      Configuration.baseUrl = resolveBaseUrl(getBaseUrl(contextPath));
      Configuration.reportsFolder = "target/selenide-reports";
      testCLMServer.getCLMServer().getConfiguration().setBaseUrl(Configuration.baseUrl);
    }
    catch (Throwable e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  @ClassRule
  public static TemporaryEntity staticTempEntity = new TemporaryEntity();

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Rule
  public EyesWatcher eyesWatcher = new EyesWatcher(); // enables visual testing

  @Rule
  public TestName testName = new TestName();

  private static void initMocks() {
    try {
      Mockito.reset(rootOrganizationConfigMigrationUtils, jiraService);
      Mockito.when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(true);
      Mockito.when(rootOrganizationConfigMigrationUtils.isMigrationScheduled()).thenReturn(false);
      Mockito.when(jiraService.isEnabled()).thenReturn(false);
      Mockito.doThrow(new IllegalStateException()).when(jiraService).getProjectsWithAcceptableIssueTypes();
    }
    catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @BeforeClass
  public static void setUpClass() {
    setupWebDriver();
    LicenseThreatGroupDataHelper.createTestLicenseThreatGroups(staticTempEntity);
    Subject subject = mock(Subject.class);
    lenient().when(subject.getPrincipal()).thenReturn(new UserPrincipal("admin", "Admin", InternalRealm.ID));
    SecurityManager securityManager = mock(SecurityManager.class);
    lenient().when(securityManager.createSubject(any(SubjectContext.class))).thenReturn(subject);
    ThreadContext.bind(securityManager);
    ThreadContext.bind(subject);
  }

  protected static void setupWebDriver() {
    WebDriver driver = WebDriverRunner.getAndCheckWebDriver();

    // Enforcing specific view port size for stable applitools validations.
    setViewPortSize(driver);

    if (!(driver instanceof PageTweakingWebDriver)) {
      WebDriverRunner.setWebDriver(new PageTweakingWebDriver(driver));
    }
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
  }

  @After
  public final void afterTest() throws Exception {
    log.info("After: {}", testName.getMethodName());
    initMocks();
    testCLMServer.getHdsServer().reset();
    if (productLicenseManager.wasChanged()) {
      productLicenseManager.reset();
      testCLMServer.getCLMServer().getInstance(CLMLicenseManager.class)
          .installLicense(new ByteArrayInputStream(new byte[1]));
    }
    // so we aren't on app between page loads
    navigate(() -> {
      Selenide.open("about");
      clearAlerts();
      return true;
    });
  }

  protected void setLicensedProducts(String... products) {
    productLicenseManager.setProducts(products);
    try {
      testCLMServer.getCLMServer().getInstance(CLMLicenseManager.class)
          .installLicense(new ByteArrayInputStream(new byte[1]));
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private static void setViewPortSize(WebDriver driver) {
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
    return Arrays.asList(new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(ProductLicense.class).toInstance(testProductLicense);
        bind(ProductLicenseManager.class).to(TestProductLicenseManager.class);
        bind(TestProductLicenseManager.class).toInstance(productLicenseManager);
        bind(LicenseFingerprinter.class).toInstance(licenseFingerprinter);
        bind(RootOrganizationConfigMigrationUtils.class).toInstance(rootOrganizationConfigMigrationUtils);
        bind(JiraService.class).toInstance(jiraService);
      }
    });
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
    FormMask.seeAndWaitForDismissal();
    loginModal.shouldBe(hidden);
  }

  protected static void logout() {
    UserMenu userMenu = MainHeader.userMenu();
    userMenu.dropdownToggle().shouldBe(visible).click();
    userMenu.logout().should(appear).click();
    userMenu.shouldNotBe(visible);
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
      });
    }
    catch (TimeoutException e) {
      throw UIAssertionError.wrapThrowable(e, Configuration.timeout);
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
          assertThat(webDriver.findElement(By.id("login-modal")).isDisplayed()).isTrue();
          // ... and not currently performing a login
          try {
            assertThat(webDriver.findElement(By.cssSelector(".form-mask")).isDisplayed()).isTrue();
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

  private static void waitUntil(Consumer<WebDriver> assertion) {
    try {
      Selenide.Wait().ignoring(AssertionError.class).until(webDriver -> {
        assertion.accept(webDriver);
        return true;
      });
    }
    catch (TimeoutException e) {
      throw UIAssertionError.wrapThrowable(e, Configuration.timeout);
    }
  }

  protected static void clearAlerts() {
    if (WebDriverRunner.isHeadless()) {
      return;
    }
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
    JavascriptExecutor js = (JavascriptExecutor)driver;
    js.executeScript(script);
  }

  protected void uninstallLicense() {
    testCLMServer.getCLMServer().getInstance(CLMLicenseManager.class).uninstallLicense();
  }
}
