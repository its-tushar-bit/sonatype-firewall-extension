/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import com.sonatype.clm.testing.functional.elements.LoginDialog;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.UserMenu;
import com.sonatype.clm.testing.functional.utils.PageTweakingWebDriver;
import com.sonatype.insight.brain.TestLicenseFingerprinter;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.jira.JiraService;
import com.sonatype.insight.brain.migration.RootOrganizationConfigMigrationUtils;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.TestCLMServer;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.test.reverseproxy.ReverseProxyServer;

import org.sonatype.licensing.product.ProductLicenseManager;
import org.sonatype.licensing.product.util.LicenseFingerprinter;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.ex.UIAssertionError;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.openqa.selenium.Alert;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.sonatype.clm.testing.functional.utils.BaseUrl.resolveBaseUrl;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public abstract class AbstractFunctionalTest
{
  private static Logger log = LoggerFactory.getLogger(AbstractFunctionalTest.class);

  public static TestProductLicenseManager productLicenseManager;

  public static TestLicenseFingerprinter licenseFingerprinter;

  public static CLMLicenseManager clmLicenseManager;

  protected static RootOrganizationConfigMigrationUtils rootOrganizationConfigMigrationUtils;

  protected static JiraService jiraService;

  protected static TestCLMServer testCLMServer;

  protected static ReverseProxyServer reverseProxyServer;

  private static Matcher<String> urlEquals(final String url) {
    return is(either(equalTo(url)).or(equalTo(Configuration.baseUrl + url)));
  }

  static {
    productLicenseManager = new TestProductLicenseManager();
    licenseFingerprinter = new TestLicenseFingerprinter();
    clmLicenseManager = new CLMLicenseManager(productLicenseManager, licenseFingerprinter);

    testCLMServer = new TestCLMServer(false /* isProxyRequiredToReachHds */, getBrainModules(), new Configurator()
    {
      @Override
      public void configure(InsightConfig config) {
        config.setBaseUrl(reverseProxyServer.getUrl());
      }
    });
    reverseProxyServer = new ReverseProxyServer(testCLMServer.getCLMServer().getPort());

    try {
      testCLMServer.start();
      reverseProxyServer.start();

      Configuration.baseUrl = resolveBaseUrl(reverseProxyServer.getUrl());
      Configuration.reportsFolder = "target/selenide-reports";
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

  @BeforeClass
  public static void setup() {
    WebDriver driver = WebDriverRunner.getAndCheckWebDriver();
    
    if (!(driver instanceof PageTweakingWebDriver)) {
      WebDriverRunner.setWebDriver(new PageTweakingWebDriver(driver));
    }

    Mockito.when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(true);
    Mockito.when(rootOrganizationConfigMigrationUtils.isMigrationScheduled()).thenReturn(false);
    Mockito.when(jiraService.isEnabled()).thenReturn(false);
  }

  @AfterClass
  public static void hardreset() {
    WebDriverRunner.getWebDriver().manage().deleteAllCookies();
  }

  @After
  public void reset() {
    testCLMServer.getInsightServer().reset();
    refreshOrOpen("about"); // so we aren't on app between page loads
    clearAlerts();
  }

  private static List<Module> getBrainModules() {
    return Arrays.<Module> asList(new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(ProductLicenseManager.class).toInstance(productLicenseManager);
        bind(LicenseFingerprinter.class).toInstance(licenseFingerprinter);
        bind(CLMLicenseManager.class).toInstance(clmLicenseManager);

        rootOrganizationConfigMigrationUtils = Mockito.mock(RootOrganizationConfigMigrationUtils.class);
        bind(RootOrganizationConfigMigrationUtils.class).toInstance(rootOrganizationConfigMigrationUtils);

        jiraService = Mockito.mock(JiraService.class);
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
    LoginDialog.root().shouldBe(visible);
    LoginDialog.username().setValue(username);
    LoginDialog.password().setValue(password);
    LoginDialog.loginButton().click();
    LoginDialog.root().shouldNotBe(visible);
  }

  protected static void logout() {
    MainHeader.userMenuToggle().shouldBe(visible).click();
    UserMenu.root().should(appear);
    UserMenu.logout().click();
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
   */
  protected SelenideElement popoverViolations(SelenideElement element) {
    return $('#' + element.attr("name") + "-popover.in");
  }

  protected static void refresh() {
    WebDriverRunner.getWebDriver().navigate().refresh();
    clearAlerts();
  }

  protected static void refreshOrOpen(String url) {
    String currentUrl = WebDriverRunner.getWebDriver().getCurrentUrl();
    if (currentUrl != null && currentUrl.endsWith(url)) {
      WebDriverRunner.getWebDriver().navigate().refresh();
    }
    else {
      Selenide.open(url);
    }
  }

  protected static void switchToWindow(final int index) {
    Selenide.switchTo().window(index);
  }

  protected static void waitUntilUrl(final String url) {
    waitUntil(webDriver -> assertThat(webDriver.getCurrentUrl(), urlEquals(url)));
  }

  protected static void waitUntilNotUrl(final String url) {
    waitUntil(webDriver -> assertThat(webDriver.getCurrentUrl(), not(urlEquals(url))));
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
      log.debug("Clearing alert: " + alert.getText());
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
}
