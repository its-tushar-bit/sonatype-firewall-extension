/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional


class LoginSpec extends BaseSpec {
  def "form states are usable"() {
    when: "login modal is shown"
    to ReportViolationsPage

    then: "username input has focus"
      js.'document.activeElement'.id == login.usernameInput.firstElement().id

    and: "login action is disabled"
      login.loginAction.isDisabled() // Navigator API version of loginAction.@disabled

    when: "credential inputs are filled in"
      login.usernameInput = "some username"
      login.passwordInput = "some password"
    
    then: "login action is enabled"
      ! login.loginAction.isDisabled()
  }
  
  def "can log in with valid credentials"() {
    given: "prompt to log in"
    to ReportViolationsPage

    when: "valid credentials are supplied"
      login.loginAsAdmin()
    
    then: "the user is logged in"
      waitFor { !login.isDisplayed() }
  }
  
  def "log in prevented when using invalid credentials" () {
    given: "prompt to log in"
    to ReportViolationsPage

    when: "invalid credentials are supplied"
      login.login("unknown", "user", true)
      
    then: "an error indicating bad credentials is shown"
      waitFor { login.errorMessage.text().contains("Invalid credentials") }

    and: "user is prompted to log in"
      login.isDisplayed()
  }

  def "report application is protected by authentication"() {
    when: "accessing the report application"
      via ReportViolationsPage

    then: "user is prompted to log in"
      login.isDisplayed()
  }

  def "management application is protected by authentication"() {
    when: "accessing the management application"
      via ManagementPage

    then: "user is prompted to log in"
      login.isDisplayed()
  }

  def "authentication session state is remembered"() {
    when: "accessing management application"
      to ManagementPage
      login.loginAsAdmin()
    
    then: "user is not prompted to log in"
      report 'management page'
      at ManagementPage
    
    when: "accessing report application"
      to ReportViolationsPage

    then: "user is not prompted to log in"
      report 'report page'
      at ReportViolationsPage
    
    when: "cookies are removed"
      clearCookies()

    and: "accessing something that requires authentication"
    via ReportViolationsPage

    then: "user is prompted to log in"
      login.isDisplayed()
  }

  def "user can logout from management pages"() {
    given: "user has logged in"
    to ManagementPage
    login.loginAsAdmin()

    when: "logging out"
    logout.link.click()

    then: "we now see the login module"
    login.isDisplayed()

    when: "attempting to navigate back"
    browser.driver.navigate().back()

    then: "we never lose the login module"
    login.isDisplayed()

    when: "we try to go directly to another page"
    go ManagementPage.url

    then: "we are still prompted to login"
    at ManagementPage
    login.isDisplayed()
  }

  def "user can logout from reporting pages"() {
    given: "user has logged in"
    to ReportViolationsPage
    login.loginAsAdmin()

    when: "logging out"
    logout.link.click()
    to ReportViolationsPage

    then: "we redirect to the login page"
    login.isDisplayed()

    when: "attempting to navigate back"
    browser.driver.navigate().back()

    then: "we never leave the login page"
    login.isDisplayed()

    when: "we try to go directly to another page"
    go ReportViolationsPage.url

    then: "we never leave the login page"
    at ReportViolationsPage
    login.isDisplayed()
  }
}
