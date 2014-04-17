/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import org.openqa.selenium.logging.LogEntry
import spock.lang.IgnoreIf

/**
 * Tests of utility methods available from BaseSpec
 * @since 1.9
 */
class BaseUtilSpec
    extends BaseSpec
{

  def setup() {
    loginAsAdminVia(ApplicationManagementPage)
  }

  def cleanup() {
    addHtmlToPage(functionalTestingSupport, '')
  }

  def "We can inject text into the page"() {
    when:
      addHtmlToPage(functionalTestingSupport, "<em>$testName.methodName</em>")

    then:
      functionalTestingSupport.text() == testName.methodName
      report 'text on page'
  }

  def "We can highlight an element on the page"() {
    when:
      addHtmlToPage(functionalTestingSupport, testName.methodName)
      highlightElement(newApplicationButton)

    then:
      newApplicationButton.firstElement().getCssValue('border').matches('2px solid (red|rgb.*)')
      report 'text on page2'
  }

  /**
   * We can still inspect FF console messages, but it doesn't appear that our added entry appears in the output?
   * Works as expected in Chrome and Phantom.
   */
  @IgnoreIf({ System.getProperty('geb.env', 'unset') == 'unset' })
  def "We can inspect the console output(for some browsers)"() {
    when:
      addHtmlToPage(functionalTestingSupport, testName.methodName)
      browser.js.exec('console.error("BaseUtilSpec");')

    then:
      List<LogEntry> output = getConsoleOutput()
      !output.isEmpty()
      output.find { LogEntry entry -> entry.message.contains('BaseUtilSpec') }
      report 'text on page3'
  }
}
