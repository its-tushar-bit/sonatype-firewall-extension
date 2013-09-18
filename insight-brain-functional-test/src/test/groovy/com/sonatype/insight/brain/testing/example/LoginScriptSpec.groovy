/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.example

import com.sonatype.insight.brain.service.InsightBrainService
import com.sonatype.insight.brain.service.InsightConfig
import com.sonatype.insight.brain.testing.functional.LoginSpec;

import com.google.common.io.Resources
import com.yammer.dropwizard.testing.junit.DropwizardServiceRule
import geb.spock.GebReportingSpec
import org.junit.ClassRule
import org.junit.rules.TestRule
import spock.lang.Shared

/**
 * An example of scripting page interactions using Geb.  
 * 
 * Compare and contrast to the page object approach seen with {@link LoginSpec} and supporting objects.
 * 
 * This will not be kept up-to-date with UI changes.  It is known to work with UI from commit 798321a .
 */
class LoginScriptSpec extends GebReportingSpec {
  @Shared
  @ClassRule
  TestRule startServiceRule = new DropwizardServiceRule<InsightConfig>(InsightBrainService.class,
    Resources.getResource('config-test.yml').getPath())

  def setup() {
    browser.config.baseUrl = "http://localhost:8070/"
  }
  
  // assumes a license has already been installed
  
  def "root web application is protected by authentication"() {
    when: "accessing the root web application"
      go() // to the base url
    
    then: "user is prompted to log in"
      waitFor { title == "CLM Login" }
  }
  
  def "report application is protected by authentication"() {
    when: "accessing the report application"
      go "assets/reports.html#/reports"
      // reports js app is actually served up, but then quickly redirects to login
      // is this expected?  I would have expected the auth filter to redirect to the login page without serving up
      // the report page

      // sometimes the redirect is fast enough for it to not be captured by the test and then pass
      // instead of using
      //   assert title == "CLM Login"
      // use a wait for the clm login page

    then: "user is prompted to log in"
      waitFor { title == "CLM Login" }
  }

  def "management application is protected by authentication"() {
    when: "accessing the management application"
      go "assets/index.html#/management/application"
      // management js app is actually served up, but then quickly redirects to login
      // is this expected?  I would have expected the auth filter to redirect to the login page without serving up
      // the report page
      
      // sometimes the redirect is fast enough for it to not be captured by the test and then pass
      // instead of using
      //   assert title == "CLM Login"
      // use a wait for the clm login page 

    then: "user is prompted to log in"
      waitFor { title == "CLM Login" }
  }
  
  def "can log in with valid credentials"() {
    given: "prompt to log in"
      go "login-assets/login.html"
    
    when: "valid credentials are supplied"
      $("input", id: "user").value("admin")
      $("input", id: "password").value("admin123")
      
      report()
      
      $("button", text: "Sign in").click()
    
    then: "the user is logged in"
      waitFor { title != "CLM Login" }
  }
  
  def "log in prevented when using invalid credentials" () {
    given: "prompt to log in"
      go "login-assets/login.html"
  
    when: "invalid credentials are supplied"
      $("input", id: "user").value("unknown")
      $("input", id: "password").value("user")
      $("button", text: "Sign in").click()
      
    then: "an error indicating bad credentials is shown"
      waitFor { $(text: contains("Invalid credentials")) }

    and: "user is prompted to log in"
      assert title == "CLM Login"
  }
}
