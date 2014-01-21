/**
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.DashboardModule
import com.sonatype.insight.brain.testing.functional.modules.NavListModule

/**
 * When navigating to this page the public application id and scan must be supplied, in that order.  For example:
 * 
 * to GroovyReportContainerPage, app.publicId, scanId
 */
class GroovyReportContainerPage extends BasePage {
  /**
   * The proper url will be created from the supplied appPublicId and scanId and should look like:
   * assets/reports.html#/reports/{appPublicId/{scanId}
   */
  static url = 'assets/reports.html#/reports'

  static at = { $('#evaluationReportContainer') }

  static content = {
    reportFrame { $('iframe') }
  }
}
