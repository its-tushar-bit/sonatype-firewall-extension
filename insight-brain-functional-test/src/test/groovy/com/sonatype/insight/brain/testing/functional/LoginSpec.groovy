package com.sonatype.insight.brain.testing.functional

import geb.spock.GebReportingSpec;

class LoginSpec extends GebReportingSpec {
//  @Shared
//  @ClassRule
//  TestRule startServiceRule = new DropwizardServiceRule<InsightConfig>(InsightBrainService.class,
//    Resources.getResource('config-test.yml').getPath())

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
      errorMessage

    and: "user is prompted to log in"
      at(LoginPage)
  }

  def "root web application is protected by authentication"() {
    when: "accessing the root web application"
      via LandingPage
    
    then: "user is prompted to log in"
      waitFor { at(LoginPage) }
  }

  def "report application is protected by authentication"() {
    when: "accessing the report application"
      via ReportPage

    then: "user is prompted to log in"
      waitFor { at(LoginPage) }
  }

  def "management application is protected by authentication"() {
    when: "accessing the management application"
      via ManagementPage

    then: "user is prompted to log in"
      waitFor { at(LoginPage) }
  }

  // TODO session state
  
  // TODO redirect to original location
}
