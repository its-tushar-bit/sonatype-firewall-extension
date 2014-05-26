/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import org.openqa.selenium.Keys
import spock.lang.Stepwise
import spock.lang.Unroll

@Stepwise
class UserManagementSpec
    extends BaseSpec 
{
  // assumes a license has already been installed
  // get to the user page
  def setupSpec() {
    loginAsAdminVia(UserManagementPage)
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
      [
        firstNameInput,
        lastNameInput,
        emailInput,
        usernameInput,
        passwordInput,
        passwordValidateInput
      ].each { input ->
        assert input.displayed
        assert input.value() == ''
      }
  }

  @Unroll
  def "If multiple space characters are present during validation the #input field should show a noSpaces error"() {
    when: 'inserting text containing leading spaces and losing focus on the field'
      input << 'a  a'
      input << Keys.TAB

    then: 'the noSpaces validation error is shown'
      inputValidations.noSpaces.displayed
      !inputValidations.errorFree
      !errorFree
      report 'after invalidation'

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
      int userCount = headers.size()

    when: 'we fix the password validation error'
      passwordValidateInput.value('123abc')

    then: 'we can now save'
      !save.disabled

    when: 'we click save'
      save.click()

    then: "add form no longer visible and newly added user is displayed"
      waitFor { !userForm.present }
      headers.size() > userCount
  }

  def "The newly added user should now appear in the list of users"() {
    when: "user views the newly added user summary"
      header(0).click()

    then: "user sees the read only fields from the object"
      def summary = summarySection(0)
      waitFor { summary.displayed }
      summary.find('td', text: 'add').displayed
      summary.find('td', text: 'user').displayed
      summary.find('td', text: 'addusertest@email.com').displayed
      currentUsers.size() == 1
      currentUsers.text() == 'admin (Admin BuiltIn)'
  }

  def "A user's password can be reset"() {
    when: 'hovering over the header of the user in the list'
      interact {
        moveToElement(header(0))
      }

    then: 'we can now see the reset symbol'
      def resetUser = resetUserButton(0)
      resetUser.displayed

    when: 'clicking on reset'
      resetUser.click()

    then: 'we are presented with a confirmation dialog'
      waitFor { resetModal.modal.displayed }

    when: 'we confirm reset'
      resetModal.confirm.click()

    then: 'we are presented with the new password'
      waitFor { newPasswordField.displayed }
      def newPassword = newPasswordField.value()
      resetModal.ok.click()
      waitFor { !newPasswordField.displayed }

    when: 'user logs in with new password'
      userOptions.logoutClick()
      login.login('addusertest', newPassword)

    then: 'login succeeds'
      !login.isDisplayed()
      userOptions.logoutClick()
  }

  def "The newly added user can be deleted"() {
    setup: 'log back in as an admin'
      loginAsAdminVia(UserManagementPage)

    when: 'hovering over the header of the user in the list'
      interact {
        moveToElement(header(0))
      }

    then: 'we can now see the delete symbol'
      def deleteUser = deleteUserButton(0)
      deleteUser.displayed

    when: 'clicking on delete'
      deleteUser.click()

    then: 'we are presented with a confirmation dialog'
      waitFor { deleteModal.modal.displayed }

    when: 'we confirm deletion'
      deleteModal.confirm.click()

    then: 'the user is deleted'
      waitFor { headers.size() == 1 }
  }
}
