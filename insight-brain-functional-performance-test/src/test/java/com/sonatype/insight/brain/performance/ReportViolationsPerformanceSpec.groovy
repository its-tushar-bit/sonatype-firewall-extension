/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.performance

import com.sonatype.insight.brain.testing.functional.ReportViolationsPage

/**
 * @since 1.12
 */
class ReportViolationsPerformanceSpec
    extends BasePerformanceSpec
{
  def "Loading the Report Violations page"() {
    def harName = ReportViolationsPage.simpleName
    setup:
      proxyServer.newHar(harName)

    when:
      to ReportViolationsPage

    then:
      waitFor { reportViolationRows.displayed }

    cleanup:
      reportHAR(harName)
  }
}
