/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import com.sonatype.insight.brain.testing.functional.ManagementPage
import com.sonatype.insight.brain.testing.functional.ReportViolationsPage

import geb.Module

/**
 * @since 1.7
 */
class DashboardModule
    extends Module
{
  static content = {
    management(to: ManagementPage) { $('.organizational-design') }
    reports(to: ReportViolationsPage) { $('.reporting') }
    version { $('.navbar-version') }
  }

  void toManagement() {
    management.click()
  }

  void toReports() {
    reports.click()
  }
}
