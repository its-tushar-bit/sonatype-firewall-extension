/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.google.common.io.Resources
import com.sonatype.insight.brain.service.InsightBrainService
import com.sonatype.insight.brain.service.InsightConfig
import com.sonatype.insight.brain.testing.functional.configuration.LDAPConfigurationPage
import com.sonatype.insight.brain.testing.functional.configuration.LDAPConnectionConfigurationPage
import com.sonatype.insight.brain.testing.functional.configuration.LDAPUserAndGroupMappingConfigurationPage
import com.sonatype.insight.brain.testing.functional.util.EchoingPageChangeListener
import com.yammer.dropwizard.testing.junit.DropwizardServiceRule
import geb.spock.GebReportingSpec
import org.junit.ClassRule
import org.junit.rules.TestRule
import spock.lang.Shared

/**
 * @since 1.7
 */
class LDAPConfigurationSpec extends GebReportingSpec
{
  @Shared
  @ClassRule
  TestRule startServiceRule = new DropwizardServiceRule<InsightConfig>(InsightBrainService.class,
      Resources.getResource('config-test.yml').getPath())

  // assumes a license has already been installed

  def setupSpec() {
    browser.config.baseUrl = "http://localhost:8070/"
    browser.registerPageChangeListener(new EchoingPageChangeListener())
  }

  def "create a new LDAP and navigate the connection and user/group mappings forms"(){
    setup: "login"
    to LoginPage
    loginAsAdmin()
    waitFor { title != "CLM Login" }

    when: "going to the LDAP page"
    waitFor { to LDAPConfigurationPage }

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

    when: "saving a value"
    inlineEditor.value('TestLDAP')
    save.click()

    then: "the connection form appears, the inline editing components are removed"
    report 'connection form'
    waitFor{ at LDAPConnectionConfigurationPage}
    !inlineEditor.displayed

    when: "filling out the required fields in the form"
    requiredFields.each{
      it << 'foo'
    }

    then: "save is enabled"
    report 'connection details'
    requiredFields.each{
      !it.hasClass('ng-invalid-required')
    }
    !save.disabled

    when: "cancelling the form and navigating to the user and group settings"
    reset.click()
    waitFor{ discard?.present }
    report 'confirmation of discarding changes'
    discard.click()
    userAndGroupSettingsTab.click()

    then: "user and group mapping form appears, with controls disabled"
    report 'user and group mappings'
    at LDAPUserAndGroupMappingConfigurationPage
    requiredFields.each{
      it.hasClass('ng-invalid-required')
    }
    checkUserMapping.disabled
    checkUserLogin.disabled

    when: "filling out required fields"
    requiredFields.each{
      it << 'foo'
    }

    then: "buttons are enabled"
    report 'form is ready to save'
    !checkUserMapping.disabled
    !checkUserLogin.disabled

    when:
    reset.click()
    waitFor{ discard?.present }
    report 'confirmation of discarding changes'
    discard.click()

    then:
    requiredFields.each{
      !it.value()
    }
  }

  /**
   * assumes logged in at the end of each test, removes any configuration information stored
   * @return
   */
  def cleanup() {
    to LDAPConfigurationPage
    if(delete?.present){
      delete.click()
      waitFor{ deleteConfirm?.present }
      deleteConfirm.click()
    }
  }
}
