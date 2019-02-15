/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.report.violation

import geb.Page

/**
 * When navigating to this page the public application id and scan must be supplied, in that order.  For example:
 *
 * to ReportPage, appPublicId, scanId
 */
class ReportPage
    extends Page
{
  /**
   * The proper url will be created from the supplied appPublicId and scanId and should look like:
   * rest/report/{appPublicId}/{scanId}/browseReport/index.html
   */
  static url = 'rest/report'

  @Override
  String convertToPath(Object[] args) {
    args ? '/' + args*.toString().join('/') + '/browseReport/index.html' : ""
  }

  static at = { contentContainer.displayed }

  static content = {
    navigation { module ReportSubNavigation }
    contentContainer(wait: true) { $('div.container') }
  }
}
