/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.report.violation

import com.sonatype.insight.brain.testing.functional.BasePage

/**
 * When navigating to this page the public application id and scan must be supplied, in that order.  For example:
 * 
 * to ReportContainerPage, app.publicId, scanId
 * 
 * Example using the IFRAME page
 * http://www.gebish.org/manual/current/pages.html#dealing_with_frames
 *
 * to GroovyReportContainerPage, 'appPublicId', 'scanId'
 * withFrame(reportFrame, GroovyReportPage) {
 *   //...
 * }
 */
class ReportContainerPage
    extends BasePage 
{
  /**
   * The proper url will be created from the supplied appPublicId and scanId and should look like:
   * assets/index.html#/reports/{appPublicId/{scanId}
   */
  static url = 'assets/index.html#/reports'

  static at = { $('#evaluationReportContainer') }

  static content = {
    reportTitle { $('#report-title') }
    reportFrame { $('iframe') }
  }
}
