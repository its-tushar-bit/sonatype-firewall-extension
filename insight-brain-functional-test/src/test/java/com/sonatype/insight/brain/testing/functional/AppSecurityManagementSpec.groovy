/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.service.InsightBrainService
import com.sonatype.insight.brain.service.InsightConfig

import com.google.common.io.Resources
import com.yammer.dropwizard.testing.junit.DropwizardServiceRule
import geb.spock.GebReportingSpec
import org.junit.ClassRule
import org.junit.rules.TestRule
import org.openqa.selenium.Keys
import spock.lang.Shared

class AppSecurityManagementSpec extends GebReportingSpec {
  @Shared
  @ClassRule
  TestRule startServiceRule = new DropwizardServiceRule<InsightConfig>(InsightBrainService.class,
    Resources.getResource('config-test.yml').getPath())
  

  // assumes a license has already been installed
  // get to the organizations page
  def setup() {
    browser.config.baseUrl = "http://localhost:8070/"
    to LandingPage
    waitFor { at(LoginPage) }
    loginAsAdmin()
    waitFor { at(ReportPage) }
    waitFor { browser.getDriver().manage().getCookieNamed('JSESSIONID') != null }
  }
  
  def "validate listed roles"() {
    when: "create a new organization"
      to OrganizationManagementPage
      waitFor { at(OrganizationManagementPage) }
      newOrganizationButton.click()
      waitFor { at(OrganizationPage) }
      organizationName.click()
      waitFor { organizationNameField.displayed }
      organizationNameField << "test organization"
      organizationSaveButton.click()
      
    then: "see the security tab shown"
      waitFor { securityTabButton.displayed }
      
    when: "user clicks on security tab"
      securityTabButton.click()
    
    then: "security tab content is shown"
      waitFor { securityTab.displayed }
    
    //TODO: when server actually sends list of roles back, add test to validate they are shown
    when: "create a new application"
      to ApplicationManagementPage
      waitFor { at(ApplicationManagementPage) }
      newApplicationButton.click()
      waitFor { at(ApplicationPage) }
      applicationName.click()
      waitFor { applicationNameField.displayed }
      applicationNameField << "test application"
      applicationId.click()
      waitFor { applicationIdField.displayed }
      applicationIdField << "testapp"
      applicationOrgField.click()
      waitFor { $('a', text:'test organization').displayed }
      $('a', text:'test organization').click()
      applicationSaveButton.click()
      
    then: "see the security tab shown"
      waitFor { securityTabButton.displayed }
      
    when: "user clicks on security tab"
      securityTabButton.click()
    
    then: "security tab is shown"
      waitFor { securityTab.displayed }
 
    //TODO: when server actually sends list of roles back, add test to validate they are shown  
  }
}