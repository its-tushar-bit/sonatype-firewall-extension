/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional


class LoginSpec
    extends BaseSpec 
{
  def "form states are usable"() {
    when: "login modal is shown"
      via ReportViolationsPage
      waitFor { login.isDisplayed() }

    then: "username input has focus"
      js.'document.activeElement'.id == login.usernameInput.firstElement().id

    and: "login action is disabled"
      waitFor { login.loginAction.isDisabled() } // Navigator API version of loginAction.@disabled

    when: "credential inputs are filled in"
      login.usernameInput = "some username"
      login.passwordInput = "some password"
    
    then: "login action is enabled"
      !login.loginAction.isDisabled()
  }
  
  def "can log in with valid credentials"() {
    given: "prompt to log in"
      via ReportViolationsPage

    when: "valid credentials are supplied"
      loginAsAdminVia()
    
    then: "the user is logged in"
      waitFor { !login.isDisplayed() }
  }
  
  def "log in prevented when using invalid credentials" () {
    given: "prompt to log in"
      via ReportViolationsPage

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
      waitFor { login.isDisplayed() }
  }

  def "management application is protected by authentication"() {
    when: "accessing the management application"
      via ManagementPage

    then: "user is prompted to log in"
      waitFor { login.isDisplayed() }
  }

  def "authentication session state is remembered"() {
    when: "accessing management application"
      loginAsAdminVia(ManagementPage)
    
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
      waitFor { login.isDisplayed() }
  }

  def "user can logout from management pages"() {
    given: "user has logged in"
      loginAsAdminVia(ManagementPage)

    when: "logging out"
      userOptions.logoutClick()

    then: "we now see the login module"
      waitFor { login.isDisplayed() }

    when: "attempting to navigate back"
      browser.driver.navigate().back()

    then: "we never lose the login module"
      waitFor { login.isDisplayed() }

    when: "we try to go directly to another page"
      go ManagementPage.url

    then: "we are still prompted to login"
      waitFor { login.isDisplayed() }
  }

  def "user can logout from reporting pages"() {
    given: "user has logged in"
      loginAsAdminVia(ReportViolationsPage)

    when: "logging out"
      userOptions.logoutClick()

    then: "we redirect to the login page"
      waitFor { login.isDisplayed() }

    when: "attempting to navigate back"
      browser.driver.navigate().back()

    then: "the login dialog does not dispose"
      waitFor { login.isDisplayed() }

    when: "we try to go directly to another page"
      go ManagementPage.url

    then: "the login dialog does not dispose"
      waitFor { login.isDisplayed() }
  }
}
