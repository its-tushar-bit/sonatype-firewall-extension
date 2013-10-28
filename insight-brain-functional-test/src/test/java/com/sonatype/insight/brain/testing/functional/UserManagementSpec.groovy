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
import spock.lang.Stepwise
import spock.lang.Unroll

@Stepwise
class UserManagementSpec
    extends GebReportingSpec
{
  @Shared
  @ClassRule
  TestRule startServiceRule = new DropwizardServiceRule<InsightConfig>(InsightBrainService.class,
      Resources.getResource('config-test.yml').getPath())

  // assumes a license has already been installed
  // get to the user page
  def setupSpec() {
    to LoginPage
    loginAsAdmin()
    at ReportPage
    to UserManagementPage
  }

  def "Arriving at user management page we should see the 'new user button' and no form."() {
    when: 'first viewing the page'
    at UserManagementPage

    then: 'no form is present, but the new user button is'
    newUserButton.displayed
    !userForm.present
  }

  def "Clicking the 'new user button' should open a new empty form"() {
    when: 'click add new user'
    newUserButton.click()

    then: 'verify add form visible'
    waitFor { userForm.present }
    errorFree
    save.disabled
    [firstNameInput, lastNameInput, emailInput, usernameInput, passwordInput, passwordValidateInput].each { input ->
      input.displayed
      input.value() == ''
    }
  }

  @Unroll
  def "If multiple space characters are present during validation the #input field should show a noSpaces error"() {
    when: 'inserting text containing leading spaces and losing focus on the field'
    input << '  a'
    input << Keys.TAB

    then: 'the noSpaces validation error is shown'
    inputValidations.noSpaces.displayed
    !inputValidations.errorFree
    !errorFree
    report 'before cleanup'

    cleanup:
    input.value('')
    input << Keys.TAB
    assert !inputValidations.noSpaces.displayed
    assert errorFree

    where:
    input          | inputValidations
    firstNameInput | firstNameValidations
    lastNameInput  | lastNameValidations
  }

  @Unroll
  def "If non alphaNumeric content is present during validation the #input field should show an alphaNumeric error"() {
    when: 'we use non alphaNumeric content'
    input << '#'

    then: 'the alphaNumeric validation error is shown'
    inputValidations.alphaNumeric.displayed
    !inputValidations.errorFree
    !errorFree
    report 'before cleanup'

    cleanup:
    input.value('')

    where:
    input          | inputValidations
    firstNameInput | firstNameValidations
    lastNameInput  | lastNameValidations
    usernameInput  | usernameValidations
  }

  @Unroll
  def "If no content is present during validation the #input field should show a required error"() {
    when: 'we add and remove content'
    input.value('a')
    input << Keys.BACK_SPACE

    then: 'the required validation error is shown'
    inputValidations.required.displayed
    !inputValidations.errorFree
    !errorFree

    where:
    input                 | inputValidations
    firstNameInput        | firstNameValidations
    lastNameInput         | lastNameValidations
    emailInput            | emailValidations
    usernameInput         | usernameValidations
    passwordInput         | passwordValidations
    passwordValidateInput | passwordValidateValidations
  }

  def "We fill out all fields correctly"() {
    when: 'providing valid values for fields'
    firstNameInput << "add"
    lastNameInput << "user"
    emailInput << "addusertest@email.com"
    usernameInput << "addusertest"
    passwordInput << "123abc"
    passwordValidateInput << "123abc"

    then: 'no errors are shown'
    errorFree
    !save.disabled
  }

  def "We fail to properly validate the password"() {
    when: 'providing non matching passwords'
    passwordValidateInput << "23abc"

    then: 'an error is displayed stating that the passwords do not match'
    passwordValidateValidations.passwordMatches.displayed
    !errorFree
    save.disabled
  }

  def "If the form is correct, we can save it"() {
    given: 'a count of present users in the system'
    int userCount = header.size()

    when: 'we fix the password validation error'
    passwordValidateInput.value('123abc')

    then: 'we can now save'
    !save.disabled

    when: 'we click save'
    save.click()

    then: "add form no longer visible"
    waitFor { !userForm.present }
    header.size() > userCount
  }

  def "The newly added user should now appear in the list of users"() {
    when: "user views the newly added user summary"
    header.first().click()

    then: "user sees the read only fields from the object"
    def summary = summarySection(0)
    waitFor { summary.displayed }
    summary.find('td', text: 'add').displayed
    summary.find('td', text: 'user').displayed
    summary.find('td', text: 'addusertest@email.com').displayed
  }
}