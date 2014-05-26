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

    emptyMessage { $('div h5#clm-welcome-message') }

    filter(require: false) { $('input') }
    reportViolationRows(require: false) { moduleList ReportViolationsRowModule, $('table.clm-table tbody tr') }
    appNameHeader(require: false) { $('table.clm-table thead th:first-child') }
    orgNameHeader(require: false) { $('table.clm-table thead th:last-child') }
  }
}
