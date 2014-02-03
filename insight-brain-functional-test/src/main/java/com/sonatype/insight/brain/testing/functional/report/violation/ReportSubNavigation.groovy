/**
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.report.violation

import geb.Module

class ReportSubNavigation
    extends Module
{
  static content = {
    summaryTrigger(to: SummaryReportPage) { $('#summaryBtn') }
    policyTrigger(to: PolicyReportPage) { $('#componentcontainerBtn') }
  }

  void toSummaryReportPage() {
    summaryTrigger.click()
  }

  void toPolicyReportPage() {
    policyTrigger.click()
  }
}
