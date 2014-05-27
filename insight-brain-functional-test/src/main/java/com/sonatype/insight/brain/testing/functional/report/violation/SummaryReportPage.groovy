/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.report.violation

import geb.Page

class SummaryReportPage
    extends Page
{
  static at = { summaryContent.displayed }

  static content = {
    navigation { module ReportSubNavigation }
    summaryContent(wait: true) { $('#summary') }
  }
}