/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.TestLicenseFingerprinter
import com.sonatype.insight.brain.TestProductLicenseManager
import com.sonatype.insight.brain.dataaccess.ApplicationDAO
import com.sonatype.insight.brain.dataaccess.OrganizationDAO
import com.sonatype.insight.brain.dataaccess.TemporaryEntity
import com.sonatype.insight.brain.model.Organization
import com.sonatype.insight.brain.model.security.Permission
import com.sonatype.insight.brain.model.security.Role
import com.sonatype.insight.brain.product.license.CLMLicenseManager
import com.sonatype.insight.brain.product.license.ProductLicense
import com.sonatype.insight.brain.service.HdsMockServerRule
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.brain.service.TestInsightBrainServiceRule
import com.sonatype.insight.brain.testing.functional.utils.BrowserInfo
import com.sonatype.insight.test.PortAllocator
import com.sonatype.insight.test.SslProperties;

import org.sonatype.licensing.product.ProductLicenseManager
import org.sonatype.licensing.product.util.LicenseFingerprinter

import com.google.inject.AbstractModule
import geb.spock.GebReportingSpec
import groovy.util.logging.Slf4j
import org.junit.ClassRule
import org.junit.Rule
import org.junit.rules.TestName
import org.openqa.selenium.logging.LogEntry
import org.openqa.selenium.logging.LogType
import org.openqa.selenium.StaleElementReferenceException
import spock.lang.Shared

@Slf4j
abstract class BaseSpec
extends GebReportingSpec {
  static {
    SslProperties.use();
  }

  @Shared
  private int hdsPort = PortAllocator.findFreePort(8090)

  @Shared
  @ClassRule
  HdsMockServerRule hdsRule = new HdsMockServerRule(hdsPort, false)

  @Shared
  @ClassRule
  TestInsightBrainServiceRule serviceRule = createServiceRule()

  @Shared
  @ClassRule
  TemporaryEntity temporaryEntity

  @Rule
  TestName testName = new TestName()

  static OrganizationDAO organizationDAO = new OrganizationDAO()

  static ApplicationDAO applicationDAO = new ApplicationDAO()

  public static TestProductLicenseManager productLicenseManager = new TestProductLicenseManager()

  public static TestLicenseFingerprinter licenseFingerprinter = new TestLicenseFingerprinter()

  public static ProductLicense productLicense = new ProductLicense()

  public static CLMLicenseManager clmLicenseManager = new CLMLicenseManager(productLicense, productLicenseManager,
      licenseFingerprinter, null)

  def getBrainModules() {
    return Arrays.asList(new AbstractModule() {
      @Override
      protected void configure() {
        bind(ProductLicense.class).toInstance(productLicense)
        bind(ProductLicenseManager.class).toInstance(productLicenseManager)
        bind(LicenseFingerprinter.class).toInstance(licenseFingerprinter)
        bind(CLMLicenseManager.class).toInstance(clmLicenseManager)
      }
    });
  }

  def createServiceRule() {
    def rule = new TestInsightBrainServiceRule(PortAllocator.findFreePort(8070), PortAllocator.findFreePort(8071),
        "http://localhost:" + hdsPort, false, getBrainModules())

    rule.setConfigurator(new Configurator() {
      @Override
      void configure(InsightConfig config) {
        // HTTP CSP headers that prohibit eval break webdriver control of phantomjs
        config.setCspEnabled(false)
      }
    })
  }

  def setupSpec() {
    // Use port as reported by service under test since it's not known until runtime.
    System.setProperty("geb.build.baseUrl", "http://localhost:" + serviceRule.getPort() + "/")
    productLicenseManager.reset()
    clmLicenseManager.installLicense(null)
    BrowserInfo.init(driver)
  }

  def cleanupSpec() {
    cleanAppsAndOrgs()
  }

  /**
   * Write html inside a specific element to enhance the present UI.
   * @param element The element to assign html to
   * @param html
   */
  def addHtmlToPage(element, String html) {
    if (!element.empty) {
      browser.js.exec(element.firstElement(), html, 'jQuery(arguments[0]).html(arguments[1]);')
      log.info("Set html: '$html' to Element: $element")
    }
    else {
      log.error("Element was empty: $element")
    }
  }

  /**
   * Set a red border around an element
   */
  def highlightElement(element) {
    setElementCss(element, ['border': '2px solid red'])
  }

  /**
   * Remove the border of an element
   */
  def unHighlightElement(element) {
    setElementCss(element, ['border': '0px'])
  }

  /**
   * Set arbitrary css styles on a given element.
   * @param element The element to set styles on
   * @param styles Map of css properties to values, i.e. ['border': '2px solid red']
   */
  def setElementCss(element, Map<String, String> styles = [:]) {
    if (!element.empty) {
      styles.each { key, value ->
        browser.js.exec(element.firstElement(), key, value, 'jQuery(arguments[0]).css(arguments[1], arguments[2]);')
      }
    }
  }

  /**
   * Grab the browser console messages
   */
  public List<LogEntry> getConsoleOutput() {
    return browser.driver.manage().logs().get(LogType.BROWSER).getAll()
  }

  def cleanAppsAndOrgs() {
    applicationDAO.getAll().each {
      applicationDAO.delete(it);
    }
    organizationDAO.getAll().each {
      if (!Organization.ROOT_ORGANIZATION_ID.equals(it.getId())) {
        organizationDAO.delete(it);
      }
    }
  }

  /**
   * Log into the application as the administrator via a specific Page.
   * Once logged in we will verify that the specified Page is loaded.
   *
   * @param initialPage the Page to navigate to while logging in
   * @param args additional path segments for the Page
   * @return a reference to the newly loaded Page
   */
  public <T> T loginAsAdminVia(Class<T> initialPage = ReportViolationsPage, Object[] args) {
    return loginAsUserVia("admin", "admin123", initialPage, args)
  }

  /**
   * Log in as an arbitrary user via a specific Page.
   * Once logged in we will verify that the specified Page is loaded.
   *
   * @param username
   * @param password
   * @param initialPage the Page to navigate to while logging in
   * @param args additional path segments for the Page
   * @return a reference to the newly loaded Page
   */
  public <T> T loginAsUserVia(String username, String password, Class<T> initialPage = ReportViolationsPage,
      Object[] args) {
    via initialPage, args
    /* 
     * Sadly, a module can't reliably wait for itself to appear. Once a function of the module is called, its base
     * element gets frozen and if that becomes stale, e.g. due to a page change as done above, all module contents
     * suffer the same fate, no matter how long the invoked module function waits and a module can't reload its base.
     */
    waitFor { login.displayed }
    try {
      login.login(username, password)
    }
    catch (Exception e) {
      /*
       * Especially when the login prompt was already shown before the page change, the test can succeed in observing
       * the modal as displayed (and continue execution) just before the page gets updated and the original modal
       * becomes stale. It is hard to proactively wait for the DOM to stabilize so we try to recover afterwards.
       */
      if (!(e instanceof StaleElementReferenceException || e.cause instanceof StaleElementReferenceException)) {
        throw e
      }
      login.login(username, password)
    }
    if (ManagementPage.class.equals(initialPage)) {
      at(RootOrgManagementPage)
    } else {
      verifyAt()
    }
    return page
  }

  public <T> T loginAsUserVia(Class<T> initialPage = ReportViolationsPage, Object[] args) {
    return loginAsUserVia(getUsername(), TemporaryEntity.USER_PASSWORD_CLEAR, initialPage, args)
  }

  void setLicensedProducts(String... products) {
    productLicenseManager.setProducts(products)
    clmLicenseManager.installLicense(null)
  }

  /**
   * Helper method to get the text out of an expected input validation popover.
   */
  String popoverText(element) {
    def popover = popoverViolations(element)
    waitFor { popover.displayed }
    return popover.text()
  }

  /**
   * Find all popover violation messages in a given element. Intended to confirm the presence/absence of violations in a form.
   */
  def popoverViolations(element) {
    def name = element.attr('name');
    $("#${name}-popover.in")
  }

  def getUsername() {
    return getClass().getSimpleName()
  }

  def createUser(String username = getUsername()) {
    return temporaryEntity.newUser(username)
  }

  def grantPermissions(String username, String contextId, Permission... perms) {
    Role role = temporaryEntity.newRole(false /* global */, perms)
    temporaryEntity.newMembershipMapping(contextId, role.getId(), username)
  }

  Date daysAgo(Date date, int days) {
    // ensure "n days ago" is at least "n * 24 hours ago", even with DST
    return new Date((date - days).time - 2 * 60 * 60 * 1000);
  }
}
