/**
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.DashboardModule
import com.sonatype.insight.brain.testing.functional.modules.NavListModule
import com.sonatype.insight.brain.testing.functional.modules.ReportViolationsRow


class ReportViolationsPage
    extends BasePage
{
  static url = "assets/reports.html#/reports/violations"

  static at = { title == 'CLM Reports' }

  static content = {
    dashboardModule { module DashboardModule }
    nav { module NavListModule }

    emptyMessage { $('div h5', text: startsWith('Welcome to Sonatype CLM. Get started by')) }

    filter(require: false) { $('input') }
    reportViolationRows(require: false) { moduleList ReportViolationsRow, $('table.clm-table tbody tr') }
    appNameHeader(require: false) { $('table.clm-table thead th:first-child') }
    orgNameHeader(require: false) { $('table.clm-table thead th:last-child') }
  }
}
