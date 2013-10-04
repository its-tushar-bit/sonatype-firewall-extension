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
import spock.lang.Shared

class LoginSpec extends GebReportingSpec {
  @Shared
  @ClassRule
  TestRule startServiceRule = new DropwizardServiceRule<InsightConfig>(InsightBrainService.class,
    Resources.getResource('config-test.yml').getPath())

  // assumes a license has already been installed

  def setup() {
    browser.config.baseUrl = "http://localhost:8070/"
  }

  def "can navigate to login page"() {
    when: "navigate to the login page"
      to LoginPage
    
    then: "login page is shown"
      at(LoginPage)
  }
  
  def "form states are usable"() {
    when: "login page is shown"
      to LoginPage
    
    then: "username input has focus"
      js.'document.activeElement'.id == usernameInput.firstElement().id

    and: "login action is disabled"
      loginAction.isDisabled() // Navigator API version of loginAction.@disabled

    when: "credential inputs are filled in"
      usernameInput = "some username"
      passwordInput = "some password"
    
    then: "login action is enabled"
      ! loginAction.isDisabled()
  }
  
  def "can log in with valid credentials"() {
    given: "prompt to log in"
      to LoginPage
    
    when: "valid credentials are supplied"
      loginAsAdmin()
    
    then: "the user is logged in"
      waitFor { title != "CLM Login" }
  }
  
  def "log in prevented when using invalid credentials" () {
    given: "prompt to log in"
      to LoginPage
  
    when: "invalid credentials are supplied"
      login("unknown", "user")
      
    then: "an error indicating bad credentials is shown"
      waitFor { errorMessage.text().contains("Invalid credentials") }

    and: "user is prompted to log in"
      at(LoginPage)
  }

  def "root web application is protected by authentication"() {
    when: "accessing the root web application"
      via LandingPage
    
    then: "user is prompted to log in"
      // see CLM-976
      waitFor { at(LoginPage) }
  }

  def "report application is protected by authentication"() {
    when: "accessing the report application"
      via ReportPage

    then: "user is prompted to log in"
      // see CLM-976
      waitFor { at(LoginPage) }
  }

  def "management application is protected by authentication"() {
    when: "accessing the management application"
      via ManagementPage

    then: "user is prompted to log in"
      // see CLM-976
      waitFor { at(LoginPage) }
  }

  def "authentication session state is remembered"() {
    given: "user has logged in"
      autoLogin()
      
    when: "accessing management application"
      to ManagementPage
    
    then: "user is not prompted to log in"
      report 'management page'
      at ManagementPage
    
    when: "accessing report application"
      to ReportPage

    then: "user is not prompted to log in"
      report 'report page'
      at ReportPage
    
    when: "cookies are removed"
      clearCookies()

    and: "accessing something that requires authentication"
      via LandingPage
    
    then: "user is prompted to log in"
      waitFor { at LoginPage }
  }
  
  // TODO redirect to original location

  void autoLogin() {
    to LoginPage
    loginAsAdmin()
    waitFor { title != "CLM Login" }
  }
}
