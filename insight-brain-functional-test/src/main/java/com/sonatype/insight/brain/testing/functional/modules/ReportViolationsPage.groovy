/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.MainModule
import com.sonatype.insight.brain.testing.functional.modules.NavListModule


class ReportViolationsPage
    extends BasePage
{
  static url = "assets/index.html#/reports/violations"

  static at = { title == 'IQ Server - Reports' }

  static content = {
    mainModule { module MainModule }
    nav { module NavListModule }
  }
}