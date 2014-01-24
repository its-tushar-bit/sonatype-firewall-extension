/**
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.DashboardModule
import com.sonatype.insight.brain.testing.functional.modules.NavListModule


class ReportViolationsPage
    extends BasePage
{
  static url = "assets/reports.html#/reports/violations"

  static at = { title == 'CLM Reports' }

  static content = {
    dashboardModule { module DashboardModule }
    nav { module NavListModule }

    emptyMessage { $('div h5', text: startsWith('Welcome to Sonatype CLM. Get started by')) }
  }
}
