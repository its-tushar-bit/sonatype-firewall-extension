/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.MainModule
import com.sonatype.insight.brain.testing.functional.modules.NavListModule
import com.sonatype.insight.brain.testing.functional.modules.ReportViolationsRowModule


class ReportViolationsPage
    extends BasePage
{
  static url = "assets/index.html#/reports/violations"

  static at = { title == 'CLM Management' }

  static content = {
    mainModule { module MainModule }
    nav { module NavListModule }

    emptyMessage { $('#clm-welcome-message') }

    filter(required: false) { $('input') }
    reportViolationRows(required: false) { moduleList ReportViolationsRowModule, $('table.clm-table tbody tr') }
    tableHeaders(required: false) { $('#report-list-header > th') }
    appNameHeader(required: false) { $('table.clm-table thead th:first-child') }
    orgNameHeader(required: false) { $('table.clm-table thead th:last-child') }
  }
}
