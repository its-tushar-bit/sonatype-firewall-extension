/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.configuration.LdapConfigurationPage
import com.sonatype.insight.brain.testing.functional.configuration.LdapConnectionConfigurationPage
import com.sonatype.insight.brain.testing.functional.configuration.LdapUserAndGroupMappingConfigurationPage

import org.openqa.selenium.Keys
import spock.lang.Stepwise


/**
 * @since 1.7
 */
@Stepwise
class LdapConfigurationSpec
    extends BaseSpec
{

  def "create a new LDAP and navigate the connection and user/group mappings forms"() {
    setup: "login"
      loginAsAdminVia()

    when: "going to the LDAP page"
      to LdapConfigurationPage

    then:
      report 'initial state with no LDAP configured'
      inlineEditorSpan.present
      !inlineEditor.displayed
      save.displayed
      save.disabled
      cancel.displayed

    when: "clicking into the inline editor"
      inlineEditorSpan.click()

    then: "the text field and buttons are now displayed"
      report 'editor displayed'
      inlineEditor.displayed

    when: "Saving a LDAP server"
      inlineEditor.value('CLM Ldap Server')
      save.click()

    then: "the connection form appears"
      report 'connection form'
      at LdapConnectionConfigurationPage

    and: "the inline editing components are removed"
      !inlineEditor.displayed

    and: "the defaults are loaded"
      waitFor { port.value() == '389' }

    and: "the save button is disabled"
      save.disabled
  }

  def "Fill out the connection form"() {
    when: "filling out the required fields in the form"
      hostname = 'ldap.clm'
      searchBase = 'dc=win,dc=blackforest,dc=local'

    then: "save is enabled"
      requiredFields.each {
        assert !it.hasClass('ng-invalid-required')
      }
      !save.disabled
  }

  def "Cancelling on the connection form"() {
    when: "cancelling the form"
      cancel.click()
      waitFor { discard.displayed }
      discard.click()

    then: 'save is disabled'
      waitFor { !discard.displayed }
      save.disabled
  }

  def "Required inputs for connection show validation error popovers"() {
    when: 'Leaving a required field blank'
      input << 'a'
      input << Keys.BACK_SPACE

    then: 'A popover should be displayed indicating that the field is required'
      popoverText(input) == 'Please enter a value'

    and: 'save should be disabled'
      save.disabled

    where:
      input << requiredFields
  }

  def "Invalid numeric inputs for connection show validation error popovers"() {
    when: 'Setting a value too low for the port'
      port = 0

    then: 'A validation popover is shown'
      popoverText(port) == 'Minimum allowed value is 1'
      report 'minimum port value error'

    when: 'Setting a value too high for the port'
      port = 999999

    then: 'A validation popover is shown'
      popoverText(port) == 'Maximum allowed value is 65535'
      report 'minimum port value error'

    cleanup: 'Remove changes so we can navigate away'
      cancel.click()
      waitFor { discard.displayed }
      discard.click()
      waitFor { !discard.displayed }
  }

  def "Fill out the user/group mapping form"() {
    when: "navigating to the user and group settings"
      userAndGroupSettingsTab.click()

    then: "user and group mapping form appears"
      waitFor { at LdapUserAndGroupMappingConfigurationPage }
      requiredFields.each { assert it.hasClass('ng-invalid-required') }

    and: "controls are disabled"
      checkUserMapping.disabled
      checkUserLogin.disabled
      save.disabled

    when: "filling out required fields"
      userObjectClass = 'user'
      userIDAttribute = 'sAMAccountName'
      userRealNameAttribute = 'displayName'
      userEmailAttribute = 'mail'

    then: "buttons are enabled"
      report 'form is ready to save'
      waitFor { !checkUserMapping.disabled }
      !checkUserLogin.disabled
      !save.disabled

    when: "resetting form to discard changes"
      cancel.click()
      waitFor { discard?.present }

    then: "confirmation is requested"
      report 'confirmation of discarding changes'
      discard.click()

    and: "changes are discarded"
      requiredFields.each {
        assert !it.value()
      }
  }

  def "Required inputs for user/group mappings show validation error popovers"() {
    when: 'Leaving a required field blank'
      input << 'a'
      input << Keys.BACK_SPACE

    then: 'A popover should be displayed indicating that the field is required'
      popoverText(input) == 'Please enter a value'

    and: 'save should be disabled'
      save.disabled

    where:
      input << requiredFields
  }

  def "Cancelling on the user/group mappings form"() {
    when: "cancelling the form"
      cancel.click()
      waitFor { discard.displayed }
      discard.click()

    then: 'save is disabled'
      waitFor { !discard.displayed }
      save.disabled
  }

  def "We can delete the LDAP server"() {
    when: 'We go to the LDAP page with a server configured'
    to LdapConfigurationPage
    waitFor { delete.displayed }

    and: 'We confirm deletion of the LDAP server'
    delete.click()
    waitFor { deleteConfirm.displayed }
    deleteConfirm.click()

    then: 'The LDAP Server is deleted and we are forwarded to the Org management page'
    waitFor { !delete.present }
    at OrganizationManagementPage
  }
}
