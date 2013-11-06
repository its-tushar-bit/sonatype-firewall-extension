/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import com.sonatype.insight.brain.testing.functional.ManagementPage
import com.sonatype.insight.brain.testing.functional.ReportPage
import geb.Module

/**
 * @since 1.7
 */
class DropdownNav
    extends Module
{
  public static final String REPORTS = 'Reports'

  public static final String MANAGEMENT = 'Management'

  static content = {
    trigger { $('a.dropdown-toggle')}
    links { $('ul.dropdown-menu').find('a') }
    management(to: ManagementPage) { links.find { it.text().trim() == MANAGEMENT } }
    reports(to: ReportPage) { links.find { it.text().trim() == REPORTS } }
  }

  void toManagement() {
    trigger.click()
    management.click()
  }

  void toReports() {
    trigger.click()
    reports.click()
  }
}
