/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

/**
 * @since 1.7
 */
class TrendingReportPage extends ReportViolationsPage
{
  static url = 'assets/reports.html#/reports/trending'

  static at = { browser.driver.currentUrl.endsWith(url) }

  static content = {
    loadingText(required: false) { $('div.report-content h2').text() }
    refresh(required: false) { $('span', 'refresh-button': 'regenerate()') }
    trendingData { $('div#trending-data') }
    count { index -> trendingData.find('h1.count-header', index) }
    componentCount(required: false) { count(0).text() }
    policyCount(required: false) { count(1).text() }
    applicationCount(require: false) { count(2).text() }
    violationCount(require: false) { count(3).text() }
    tooltip(require: false) { $('div.tooltip .tooltip-inner') }
    reportDate(require: false) { $('#trending-data .pull-right strong') }
    percentageChartControl(require: false) { $('div', "ng-controller": 'PercChartCtrl') }
    exactComponentBar(require: false) { percentageChartControl.find('rect[style="fill: rgb(131, 131, 131);"]') }
    partialComponentBar(require: false) { percentageChartControl.find('rect[style="fill: rgb(170, 170, 170);"]') }
    unknownComponentBar(require: false) { percentageChartControl.find('rect[style="fill: rgb(204, 204, 204);"]') }
    proprietaryComponentBar(require: false) { percentageChartControl.find('rect[style="fill: rgb(232, 232, 232);"]') }
  }
}
