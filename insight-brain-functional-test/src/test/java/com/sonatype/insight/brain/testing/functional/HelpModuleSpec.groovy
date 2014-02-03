/**
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import spock.lang.Stepwise
/**
 * @since 1.9
 */
@Stepwise
class HelpModuleSpec extends BaseSpec {

  def setupSpec() {
    loginAsAdminVia()
  }

  def "Should be able to open our online documentation"() {
    when: 'Clicking on the documentation link'
      helpLinks.dropdown.click()
      waitFor { helpLinks.documentation.displayed }
      helpLinks.documentation.click()

    then: 'A new window should open with our online documentation'
      withWindow(close: true, availableWindows[1]) {
        waitFor { $('h1', text: contains('Sonatype CLM Documentation Index')).displayed }
      }
  }

  def "Should be able to easily open a new support request"(){
    when: 'Clicking on the support link'
      helpLinks.dropdown.click()
      waitFor { helpLinks.support.displayed }
      helpLinks.support.click()

    then: 'A new window should open on a form ready to create a support request'
      withWindow(close: true, availableWindows[1]) {
        waitFor { $('h2', text: 'Submit a request').displayed }
      }
  }
}
