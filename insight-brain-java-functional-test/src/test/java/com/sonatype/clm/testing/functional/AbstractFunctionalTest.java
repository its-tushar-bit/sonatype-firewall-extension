/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional;

import java.util.Arrays;
import java.util.List;

import com.sonatype.clm.testing.functional.elements.LoginDialog;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.UserMenu;
import com.sonatype.clm.testing.functional.utils.PageTweakingWebDriver;
import com.sonatype.insight.brain.TestLicenseFingerprinter;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.service.TestCLMServer;

import org.sonatype.licensing.product.ProductLicenseManager;
import org.sonatype.licensing.product.util.LicenseFingerprinter;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.relocated.common.base.Predicate;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.openqa.selenium.Alert;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

public abstract class AbstractFunctionalTest
{
  private static Logger log = LoggerFactory.getLogger(AbstractFunctionalTest.class);

  public static TestProductLicenseManager productLicenseManager;

  public static TestLicenseFingerprinter licenseFingerprinter;

  public static CLMLicenseManager clmLicenseManager;

  protected static TestCLMServer testCLMServer;

  static {
    productLicenseManager = new TestProductLicenseManager();
    licenseFingerprinter = new TestLicenseFingerprinter();
    clmLicenseManager = new CLMLicenseManager(productLicenseManager, licenseFingerprinter);
    testCLMServer = new TestCLMServer(false /* isProxyRequiredToReachHds */, getBrainModules());
    try {
      testCLMServer.start();
      Configuration.baseUrl = "http://localhost:" + testCLMServer.getCLMServer().getPort() + "/";
      Configuration.reportsFolder = "target/selenide/reports";
    }
    catch (Throwable e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @BeforeClass
  public static void setup() {
    WebDriver driver = WebDriverRunner.getAndCheckWebDriver();
    if (!(driver instanceof PageTweakingWebDriver)) {
      WebDriverRunner.setWebDriver(new PageTweakingWebDriver(driver));
    }
  }

  @AfterClass
  public static void hardreset() {
    WebDriverRunner.getWebDriver().manage().deleteAllCookies();
  }

  @After
  public void reset() {
    open("about"); // so we aren't on app between page loads
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
      }
    });
  }

  protected static void loginAsAdmin() {
    login("admin", "admin123");
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

  protected static Predicate<WebDriver> notUrlPredicate(final String url) {
    return new Predicate<WebDriver>()
    {

      @Override
      public boolean apply(WebDriver input) {
        return !url.equals(WebDriverRunner.url());
      }
    };
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
}
