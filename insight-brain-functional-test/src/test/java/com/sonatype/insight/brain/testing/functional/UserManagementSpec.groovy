/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.dataaccess.security.UserDAO
import com.sonatype.insight.brain.service.InsightBrainService
import com.sonatype.insight.brain.service.InsightConfig

import com.google.common.io.Resources
import com.yammer.dropwizard.testing.junit.DropwizardServiceRule
import geb.spock.GebReportingSpec
import org.junit.ClassRule
import org.junit.rules.TestRule
import org.openqa.selenium.Keys
import spock.lang.Shared

class UserManagementSpec extends GebReportingSpec {
  @Shared
  @ClassRule
  TestRule startServiceRule = new DropwizardServiceRule<InsightConfig>(InsightBrainService.class,
    Resources.getResource('config-test.yml').getPath())

  // assumes a license has already been installed
  // get to the user page
  def setup() {
    to LandingPage
    waitFor { at(LoginPage) }
    loginAsAdmin()
    at(ReportPage)
    to UserManagementPage
  }
  
  //remove any created users
  def cleanup() {
    UserDAO userDAO = new UserDAO();
    userDAO.getAll().each {
      if (it.username == 'addusertest') {
        userDAO.delete(it);
      }
    }
  }
  
  def "new user fields provide client-side validation"() {
    given: "user arrives at user page"
      waitFor { newUserButton.present }
      
    when: "click add new user"
      newUserButton.click()
      
    then: "verify add form visible"
      waitFor { firstNameInput.present }
      firstNameValidations.errorFree
      save.disabled

    when: "removing the first name value"
      firstNameInput << "a"
      firstNameInput << Keys.BACK_SPACE
      
    then: "make sure validation error shown"
      report 'missing required first name'
      firstNameValidations.required.displayed
            
    when: "adding a first name"
      firstNameInput << "a"
      
    then: "make sure validation error not shown"
      firstNameValidations.errorFree

    when: "adding a first name that contains non-alphanumeric characters"
      firstNameInput << "##"
      
    then: "make sure validation error shown"
      report 'first name contains illegal characters'
      firstNameValidations.alphaNumeric.displayed
      
    when: "removing the alphanumeric characters"
      firstNameInput << Keys.BACK_SPACE
      firstNameInput << Keys.BACK_SPACE

    then: "make sure alphanumeric validation error not shown"
      firstNameValidations.errorFree
      
    when: "adding extra spaces"
      firstNameInput << '  a'
      
    then: "make sure spaces validation error is shown"
      firstNameValidations.noSpaces.displayed
      
    when: "removing extra characters"
      firstNameInput << Keys.BACK_SPACE
      firstNameInput << Keys.BACK_SPACE
      firstNameInput << Keys.BACK_SPACE
      
    then: "make sure spaces validation error is removed"
      firstNameValidations.errorFree
      
    when: "removing the last name"
      lastNameInput << "a"
      lastNameInput << Keys.BACK_SPACE
      
    then: "make sure required validation error shown"
      report 'missing required last name'
      lastNameValidations.required.displayed

    when: "adding a last name"
      lastNameInput << "a"
      
    then: "make sure required validation error not shown"
      lastNameValidations.errorFree
      
    when: "adding a last name that contains non-alphanumeric characters"
      lastNameInput << "##"
      
    then: "make sure validation error shown"
    report 'last name contains illegal characters'
      lastNameValidations.alphaNumeric.displayed
      
    when: "removing the alphanumeric characters"
      lastNameInput << Keys.BACK_SPACE
      lastNameInput << Keys.BACK_SPACE
      
    then: "make sure alphanumeric validation error not shown"
      lastNameValidations.errorFree
      
    when: "adding extra spaces"
      lastNameInput << '  a'
      
    then: "make sure spaces validation error is shown"
      lastNameValidations.noSpaces.displayed

    when: "removing extra characters"
      lastNameInput << Keys.BACK_SPACE
      lastNameInput << Keys.BACK_SPACE
      lastNameInput << Keys.BACK_SPACE
      
    then: "make sure spaces validation error is removed"
      lastNameValidations.errorFree

    when: "removing the email value"
      emailInput << "a"
      emailInput << Keys.BACK_SPACE
      
    then: "make sure required validation error shown"
      report 'missing required email'
      emailValidations.required.displayed

    when: "adding an invalid email value"
      emailInput << "a"
      
    then: "make sure required validation error not shown and format error is shown"
      report 'email incorrect format'
      emailValidations.invalidEmail.displayed
      
    when: "adding a valid email"
      emailInput << '@test.com'
      
    then: "make sure validation error not shown"
      emailValidations.errorFree
      
    when: "removing the username value"
      usernameInput << "a"
      usernameInput << Keys.BACK_SPACE
      
    then: "make sure validation error shown"
      report 'missing required username'
      usernameValidations.required.displayed

    when: "adding a username value"
      usernameInput << "a"

    then: "make sure validation error not shown"
      usernameValidations.errorFree
      
    when: "adding a username with non-alphanumeric characters"
      usernameInput << "##"
      
    then: "make sure validation error shown"
      report 'username contains illegal characters'
      usernameValidations.alphaNumeric.displayed

    when: "removing the username value"
      usernameInput << Keys.BACK_SPACE
      usernameInput << Keys.BACK_SPACE

    then: "make sure validation error not shown"
      usernameValidations.errorFree
      
    when: "adding extra spaces"
      usernameInput << '  a'
      
    then: "make sure spaces validation error is shown"
      usernameValidations.pattern.displayed

    when: "removing extra characters"
      usernameInput << Keys.BACK_SPACE
      usernameInput << Keys.BACK_SPACE
      usernameInput << Keys.BACK_SPACE
      
    then: "make sure spaces validation error is removed"
      usernameValidations.errorFree
      
    when: "check password required validation"
      passwordInput << "a"
      passwordInput << Keys.BACK_SPACE
      
    then: "make sure validation error shown"
      report 'missing required password'
      passwordValidations.required.displayed
            
    when: "check password required validation gone"
      passwordInput << "a"
      
    then: "make sure validation error not shown"
      passwordValidations.errorFree
      
    when: "check password match required validation"
      passwordInput << Keys.BACK_SPACE
      passwordValidateInput << "a"
      passwordValidateInput << Keys.BACK_SPACE
      
    then: "make sure validation error shown"
      report 'required password validation is missing'
      passwordValidateValidations.required.displayed
            
    when: "check password match required validation gone"
      passwordValidateInput << "a"
      
    then: "make sure validation error not shown"
      !passwordValidateValidations.required.displayed
      passwordValidations.required.displayed
      
    when: "check password match validation"
      passwordValidateInput << Keys.BACK_SPACE
      passwordInput << "abc"
      passwordValidateInput << "a"
      
    then: "make sure validation error shown"
      report 'password validation failure'
      passwordValidateValidations.passwordMatches.displayed
      
    when: "check password match validation gone"
      passwordValidateInput << "bc"
      
    then: "make sure validation error not shown"
      passwordValidateValidations.errorFree
      !save.disabled
      
    when: "cancel new user"
      cancel.click()
      
    then: "user form no longer displayed"
      !userForm.present
      newUserButton.present
  }
  
  def "new user save"() {
    given: "user arrives at user page"
      waitFor { newUserButton.present }
      int userCount = header.size()
      
    when: "click add new user"
      newUserButton.click()
      
    then: "verify add form visible"
      waitFor { firstNameInput.present }
      !firstNameRequiredError.displayed
      save.disabled
      
    when: "user populates all the fields"
      firstNameInput << "add"
      lastNameInput << "user"
      emailInput << "addusertest@email.com"
      usernameInput << "addusertest"
      passwordInput << "123abc"
      passwordValidateInput << "123abc"
      
    then: "save button becomes enabled"
      !save.disabled
      
    when: "user clicks the save button"
      save.click()
      
    then: "add form no longer visible"
      waitFor { newUserButton.present }
      
    then: "add form no longer visible and user is added to the list"
      waitFor { newUserButton.present }
      waitFor { userCount < header.size() }
      
    when: "user views user summary"
      header.first().click()
    
    then: "user sees the read only fields from the object"
      waitFor { summarySection.first().displayed }
      summarySection.first().find('td', text: 'add').displayed
      summarySection.first().find('td', text: 'user').displayed
      summarySection.first().find('td', text: 'addusertest@email.com').displayed
  }
}