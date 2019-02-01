/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
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

  static at = { title == 'IQ Server - Report Violations' || title == 'IQ Server -' }

  static content = {
    mainModule { module MainModule }
    nav { module NavListModule }

    emptyMessage { $('#no-reports-message') }

    filter(required: false) { $('input') }
    reportViolationRows(required: false) {
      $('#report-list-table tbody.iq-report-list-results tr').moduleList(ReportViolationsRowModule)
    }
    tableHeaders(required: false) { $('#report-list-headers > th') }
    appNameHeader(required: false) { $('#report-list-header-app') }
    orgNameHeader(required: false) { $('#report-list-header-org') }
  }

  void clickHeader(header) {
    header.find('a').click()
  }
}
