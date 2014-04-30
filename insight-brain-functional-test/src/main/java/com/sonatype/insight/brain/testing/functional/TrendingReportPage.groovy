/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

/**
 * @since 1.7
 */
class TrendingReportPage
    extends ReportViolationsPage
{
  static url = 'assets/index.html#/reports/trending'

  static at = { trendingData.present }

  static content = {
    loadingText(required: false) { $('#generatingPlaceholderText').text() }
    refresh(required: false) { $('span', 'refresh-button': 'regenerate()') }
    trendingData { $('div#trending-data') }
    count { index -> trendingData.find('h1.count-header', index) }
    componentCount(required: false) { count(0).text() }
    policyCount(required: false) { count(1).text() }
    applicationCount(require: false) { count(2).text() }
    violationCount(require: false) { count(3).text() }
    tooltip(require: false) { $('div.tooltip .tooltip-inner') }
    reportDate(require: false) { $('#trending-data .pull-right strong') }

    percentageChartControl(require: false) { $('div#percChart') }
    componentBars(require: false) { percentageChartControl.find('rect') }
    exactComponentBar(require: false) { componentBars[0] }
    partialComponentBar(require: false) { componentBars[1] }
    unknownComponentBar(require: false) { componentBars[2] }

    barChartControl { $('div#diffChart') }
    barCharts { barChartControl.find('svg.chart') }
    barChartText { barCharts.collect{ it.text()replaceAll('[\\n\\s\\r]', '') } }
  }
}
