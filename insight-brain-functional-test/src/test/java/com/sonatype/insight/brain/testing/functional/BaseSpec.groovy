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
import com.sonatype.insight.brain.product.license.ProductLicenseDetailsCache
import com.sonatype.insight.brain.product.license.TestProductLicenseDetailsCache
import com.sonatype.insight.brain.service.HdsMockServerRule
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.brain.service.TestInsightBrainServiceRule
import com.sonatype.insight.brain.testing.functional.utils.BrowserInfo
import com.sonatype.insight.test.networking.PortAllocator
import com.sonatype.insight.test.networking.SslProperties;

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
import org.openqa.selenium.remote.RemoteWebDriver
import spock.lang.Shared

@Slf4j
abstract class BaseSpec
extends GebReportingSpec {
  static {
    SslProperties.use();
  }

  @Shared
  private int hdsPort = PortAllocator.nextFreePort()

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

  def getBrainModules() {
    return Arrays.asList(new AbstractModule() {
      @Override
      protected void configure() {
        bind(ProductLicenseManager.class).to(TestProductLicenseManager.class)
        bind(LicenseFingerprinter.class).to(TestLicenseFingerprinter.class)
        bind(ProductLicenseDetailsCache.class).to(TestProductLicenseDetailsCache.class)
      }
    });
  }

  def createServiceRule() {
    new TestInsightBrainServiceRule(PortAllocator.nextFreePort(), PortAllocator.nextFreePort(),
        "http://localhost:" + hdsPort, false, getBrainModules())
  }

  def setupSpec() {
    // Use port as reported by service under test since it's not known until runtime.
    def baseUrl = resolveBaseUrl(driver, "http://localhost:${serviceRule.getPort()}/")

    System.setProperty("geb.build.baseUrl", baseUrl)
    // HTTP CSP headers that prohibit eval break webdriver control of phantomjs
    serviceRule.setCspEnabled(false)
    BrowserInfo.init(driver)
  }

  def resolveBaseUrl(def driver, String baseUrl) {
    if (driver.getWrappedDriver().getClass() == RemoteWebDriver) {
      // On some docker hosts the containers cannot use the loopback address of the host, so we need to lookup an
      // address that they can use
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

      while (interfaces.hasMoreElements()) {
        NetworkInterface iface = interfaces.nextElement();
        try {
          if (iface.isUp() && !iface.isLoopback()) {
            Enumeration<InetAddress> addresses = iface.getInetAddresses();
            while (addresses.hasMoreElements()) {
              InetAddress address = addresses.nextElement();

              // only try 32-bit (IPv4 addresses)
              if (address.getAddress().length == 4) {
                String addressedUrl = baseUrl.replace("localhost", address.getHostAddress());
                try {
                  if (!address.isLoopbackAddress() && address.isReachable(2000)) {
                    return addressedUrl;
                  }
                }
                catch (Exception ignored) {
                  // try the next address
                }
              }
            }
          }
        }
        catch (Exception ignored) {
          // try the next interface
        }
      }
    }

    return baseUrl
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
