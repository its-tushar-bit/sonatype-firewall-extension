/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.configuration.LdapConfigurationPage
import com.sonatype.insight.brain.testing.functional.configuration.LdapConnectionConfigurationPage
import com.sonatype.insight.brain.testing.functional.configuration.LdapUserAndGroupMappingConfigurationPage


/**
 * @since 1.7
 */
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
      inlineEditor.value('TestLDAP')
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

    when: "filling out the required fields in the form"
      requiredFields.each{
        it << 'foo'
      }

    then: "save is enabled"
      report 'connection details'
      requiredFields.each{
        assert !it.hasClass('ng-invalid-required')
      }
      !save.disabled

    when: "cancelling the form"
      reset.click()
      waitFor{ discard?.present }
      report 'confirmation of discarding changes'
      discard.click()

    and: "navigating to the user and group settings"
      userAndGroupSettingsTab.click()

    then: "user and group mapping form appears"
      report 'user and group mappings'
      at LdapUserAndGroupMappingConfigurationPage
      requiredFields.each{
        assert it.hasClass('ng-invalid-required')
      }

    and: "controls are disabled"
      checkUserMapping.disabled
      checkUserLogin.disabled
      save.disabled

    when: "filling out required fields"
      requiredFields.each{
        it << 'foo'
      }

    then: "buttons are enabled"
      report 'form is ready to save'
      waitFor { !checkUserMapping.disabled }
      !checkUserLogin.disabled
      !save.disabled

    when: "resetting form to discard changes"
      reset.click()
      waitFor{ discard?.present }

    then: "confirmation is requested"
      report 'confirmation of discarding changes'
      discard.click()

    and: "changes are discarded"
      requiredFields.each{
        assert !it.value()
      }
  }

  /**
   * assumes logged in at the end of each test, removes any configuration information stored
   * @return
   */
  def cleanup() {
    to LdapConfigurationPage
    if (delete?.present) {
      delete.click()
      waitFor{ deleteConfirm?.present }
      deleteConfirm.click()
      waitFor { !delete.present } // wait for the request to complete, otherwise an error dialog results and upsets the following tests
    }
  }
}
