/**
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.google.common.io.Resources
import com.sonatype.insight.brain.TemporaryEntity
import com.sonatype.insight.brain.dataaccess.ApplicationDAO
import com.sonatype.insight.brain.dataaccess.OrganizationDAO
import com.sonatype.insight.brain.service.InsightConfig
import com.sonatype.insight.brain.service.TestInsightBrainService
import com.sonatype.insight.brain.testing.functional.utils.InsightMockServerRule
import com.yammer.dropwizard.testing.junit.DropwizardServiceRule
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
abstract class BaseSpec extends GebReportingSpec {
  static {
    System.setProperty("javax.net.ssl.trustStore", "src/test/resources/ssl/server-store");
  }
  @Shared
  @ClassRule
  TestRule serviceRule = new DropwizardServiceRule<InsightConfig>(TestInsightBrainService.class,Resources.getResource('config-test.yml').getPath())

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
    System.setProperty("geb.build.baseUrl", "http://localhost:" + serviceRule.getLocalPort() + "/")
  }

  def cleanupSpec(){
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
  def highlightElement(element){
    setElementCss(element, ['border': '2px solid red'])
  }

  /**
   * Remove the border of an element
   */
  def unHighlightElement(element){
    setElementCss(element, ['border': '0px'])
  }

  /**
   * Set arbitrary css styles on a given element.
   * @param element The element to set styles on
   * @param styles Map of css properties to values, i.e. ['border': '2px solid red']
   */
  def setElementCss(element, Map<String, String> styles = [:]) {
    if(!element.empty){
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

  void createOrganization() {
    OrganizationManagementPage organizationManagementPage = to(OrganizationManagementPage)
    int size = !organizationList?.empty ? organizationList.size() : 0
    organizationManagementPage.createOrg()
    waitFor{ organizationList.size() > size }
  }

  void createApplication() {
    ApplicationManagementPage applicationManagementPage = to(ApplicationManagementPage)
    int size = !applicationList?.empty ? applicationList.size() : 0
    applicationManagementPage.createApp()
    waitFor{ applicationList.size() > size }
  }
}