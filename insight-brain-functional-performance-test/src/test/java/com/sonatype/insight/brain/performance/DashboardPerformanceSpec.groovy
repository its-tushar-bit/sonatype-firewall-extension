/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.performance

import com.sonatype.insight.brain.testing.functional.ApplicationViolationsDashboardPage
import com.sonatype.insight.brain.testing.functional.ComponentViolationsDashboardPage
import com.sonatype.insight.brain.testing.functional.NewestRiskDashboardPage

import spock.lang.Unroll

/**
 * @since 1.12
 */
class DashboardPerformanceSpec
    extends BasePerformanceSpec
{

  @Unroll
  def "Loading #pageUnderTest.url"(pageUnderTest, Closure waitClosure) {
    def harName = pageUnderTest.simpleName
    setup:
      proxyServer.newHar(harName)

    when:
      to pageUnderTest

    then:
      waitFor(waitClosure)

    cleanup:
      reportHAR(harName)

    where:
      pageUnderTest                      | waitClosure
      NewestRiskDashboardPage            | { policySummary.displayed && newestViolationTable.rows }
      ComponentViolationsDashboardPage   | { policySummary.displayed && componentViolationsTable.rows }
      ApplicationViolationsDashboardPage | { policySummary.displayed && applicationViolationsTable.rows }
  }
}
