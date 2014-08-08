/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.performance

import com.sonatype.insight.brain.testing.functional.ApplicationPage

import spock.lang.Unroll

/**
 * @since 1.12
 */
class ApplicationPerformanceTest
    extends BasePerformanceSpec
{
  // app with the largest number of violations in the performance data set
  static final String APP_PUBLIC_ID = 'drools-wb-webapp'

  @Unroll
  def "Loading Application #appPublicId with tab: #tab"(String appPublicId, String tab, Closure waitClosure) {
    def harName = "${ApplicationPage.simpleName}-$appPublicId-$tab"
    setup:
      proxyServer.newHar(harName)

    when:
      to ApplicationPage, appPublicId, tab

    then:
      waitFor(waitClosure)

    cleanup:
      reportHAR(harName)

    where:
      appPublicId   | tab        | waitClosure
      APP_PUBLIC_ID | 'policies' | { tabs.policiesTab.displayed }
      APP_PUBLIC_ID | 'labels'   | { tabs.labelsTab.displayed }
      APP_PUBLIC_ID | 'licenses' | { tabs.ltgTab.displayed }
      APP_PUBLIC_ID | 'tags'     | { tabs.tagTab.displayed }
      APP_PUBLIC_ID | 'security' | { tabs.securityTab.displayed }
  }

  /**
   * Simulate the interaction of loading the App page and clicking between the tabs
   */
  def "Loading Application and navigate between tabs"() {
    def harName = "${ApplicationPage.simpleName}-$APP_PUBLIC_ID-navigation"
    setup:
      proxyServer.newHar(harName)

    when:
      to ApplicationPage, APP_PUBLIC_ID, 'policies'

    then:
      waitFor { tabs.policiesTab.displayed }

    when:
      tabs.labelsTabButton.click()

    then:
      waitFor { tabs.labelsTab.displayed }

    when:
      tabs.ltgTabButton.click()

    then:
      waitFor { tabs.ltgTab.displayed }

    when:
      tabs.tagTabButton.click()

    then:
      waitFor { tabs.tagTab.displayed }

    when:
      tabs.securityTabButton.click()

    then:
      waitFor { tabs.securityTab.displayed }

    cleanup:
      reportHAR(harName)
  }
}
