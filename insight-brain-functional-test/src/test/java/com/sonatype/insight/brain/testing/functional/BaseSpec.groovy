/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.dataaccess.ApplicationDAO
import com.sonatype.insight.brain.dataaccess.OrganizationDAO
import com.sonatype.insight.brain.dataaccess.TemporaryEntity
import com.sonatype.insight.brain.service.PortAllocator
import com.sonatype.insight.brain.service.TestInsightBrainServiceRule
import com.sonatype.insight.brain.testing.functional.utils.InsightMockServerRule

import geb.Page
import geb.spock.GebReportingSpec
import groovy.util.logging.Slf4j
import org.junit.ClassRule
import org.junit.Rule
import org.junit.rules.TestName
import org.junit.rules.TestRule
import org.openqa.selenium.logging.LogEntry
import org.openqa.selenium.logging.LogType
import spock.lang.Shared

@Slf4j
abstract class BaseSpec
    extends GebReportingSpec 
{
  static {
    System.setProperty("javax.net.ssl.trustStore", "src/test/resources/ssl/server-store");
  }

  @Shared
  @ClassRule
  TestInsightBrainServiceRule serviceRule = new TestInsightBrainServiceRule(PortAllocator.findFreePort(8070),
    PortAllocator.findFreePort(8071), null, null, false, null)

  @Shared
  @ClassRule
  TestRule saasRule = new InsightMockServerRule();

  @Shared
  @ClassRule
  TemporaryEntity temporaryEntity

  @Rule
  TestName testName = new TestName()
  
  static OrganizationDAO organizationDAO = new OrganizationDAO()
  static ApplicationDAO  applicationDAO = new ApplicationDAO()

  def setupSpec() {
    // Use port as reported by service under test since it's not known until runtime.
    System.setProperty("geb.build.baseUrl", "http://localhost:" + serviceRule.getPort() + "/")
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
      styles.each{ key, value ->
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
      organizationDAO.delete(it);
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
  Page loginAsAdminVia(initialPage = ReportViolationsPage, Object[]args) {
    via initialPage, args
    login.loginAsAdmin()
    verifyAt()
    return page
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
  Page loginAsUserVia(String username, String password, initialPage = ReportViolationsPage, Object[] args) {
    via initialPage, args
    login.login(username, password)
    verifyAt()
    return page
  }

  void createOrganization(name = 'test organization') {
    OrganizationManagementPage organizationManagementPage = to(OrganizationManagementPage)
    int size = !organizationList?.empty ? organizationList.size() : 0
    organizationManagementPage.createOrg(name)
    waitFor{ organizationList.size() > size }
  }

  void createApplication(name = 'test application', id = 'test application', orgName = 'test organization') {
    ApplicationManagementPage applicationManagementPage = to(ApplicationManagementPage)
    int size = !applicationList?.empty ? applicationList.size() : 0
    applicationManagementPage.createApp(name, id, orgName)
    waitFor{ applicationList.size() > size }
  }
}