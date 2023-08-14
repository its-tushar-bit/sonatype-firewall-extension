/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.DashboardTabsModule
import com.sonatype.insight.brain.testing.functional.modules.ExpandoModule
import com.sonatype.insight.brain.testing.functional.modules.FilterModule
import com.sonatype.insight.brain.testing.functional.modules.ClmModalModule
import com.sonatype.insight.brain.testing.functional.modules.ThreatTableModule
import geb.Module

/**
 * @since 1.11
 */
class DashboardOverviewPage
    extends DashboardPage
{
  static at = { filters.displayed }

  static content = {
    noDataAvailable(required: false) { $('#dashboard-common-results-no-data') }

    highestRiskDiv(required: false) { $('#highest-risk').module(ThreatTableModule) }

    tabLinks { $('ul.nav.nav-tabs').module(DashboardTabsModule) }

    summaryData(required: false) { $('#summary-data') }
    summaryMatchedApplications(required: false) { $('#summary-matched-applications') }
    summaryMatchedPolicies(required: false) { $('#summary-matched-policies') }
    summaryMatchedComponents(required: false) { $('#summary-matched-components') }

    unknownComponentPopover(required: false) { $('.popover.pathnames-popover') }
    unknownComponentPopoverTitle(required: false) { $('.popover-title').text() }
    unknownComponentPopoverText(required: false) { $('.popover-content.pathnames-popover-content').text() }

    componentMatchSection(required: false) { $('#component-match-results') }
    componentMatchExactCount(required: false) { $('#component-match-results [id$="-count"]')[0] }
    componentMatchSimilarCount(required: false) { $('#component-match-results [id$="-count"]')[1] }
    componentMatchUnknownCount(required: false) { $('#component-match-results [id$="-count"]')[2] }

    policySummary { $('#policySummaryData').module(PolicySummaryModule) }

    applicationHeatMapHelp(required: false) { $('#application-heat-map-help-content') }
    applicationHeatMapHelpClose(required: false) { $('#application-heat-map-help-close') }
    componentHeatMapHelp(required: false) { $('#component-heat-map-help-content') }
    componentHeatMapHelpClose(required: false) { $('#component-heat-map-help-close') }
    modalBackdrop(required: false) { $('div.modal-backdrop') }

    filters { $('#dashboard-filter-container').module(FilterModule) }

    applyFilterModal { module(new ClmModalModule(title: 'Filter Settings Changed')) }

    viewTrendsButton { $('#show-trend-dialog') }
    viewTrendsDialog { $('#policy-trends-dialog') }
    trendsDialogCloseButton { $('#policy-trends-dialog-close') }
  }
}

class NewestRiskDashboardPage
    extends DashboardOverviewPage
{
  static url = DashboardOverviewPage.url + "/violations"

  static content = {
    newestViolationTable(required: false) { $('#highest-risk-table').module(ThreatTableModule) }
  }
}

class ComponentViolationsDashboardPage
    extends DashboardOverviewPage
{
  static url = DashboardOverviewPage.url + "/components"

  static content = {
    componentViolationsTable(required: false) { $('#component-risk').module(ComponentViolationsTable) }
  }
}

class ApplicationViolationsDashboardPage
    extends DashboardOverviewPage
{
  static url = DashboardOverviewPage.url + "/applications"

  static content = {
    applicationViolationsTable(required: false) { $('#application-risk').module(ApplicationViolationsTable) }
  }
}

class PolicySummaryModule
    extends Module
{
  static content = {
    rows(required: false) { $('tr').tail().moduleList(PolicySummaryRow) }
    pendingRow { (PolicySummaryRow) rows[0] }
    waivedRow { (PolicySummaryRow) rows[1] }
    fixedRow { (PolicySummaryRow) rows[2] }
    discoveredRow { (PolicySummaryRow) rows[3] }
  }
}

class PolicySummaryRow
    extends Module
{
  static final int CATEGORY = 0

  static final int COUNT = 1

  static final int AVERAGE_AGE = 2

  static final int NINETY_PERCENTILE_AGE = 3

  static final int BAR_CHART = 7

  static final int SPARKLINE = 8

  static content = {
    cell(required: false) { int i -> $('td', i) }
    category { cell(CATEGORY).text() }
    count { cell(COUNT).text().toInteger() }
    averageAge { cell(AVERAGE_AGE).text() }
    ninetyPercentileAge { cell(NINETY_PERCENTILE_AGE).text() }
    delta { module DeltaModule }
    barChart { cell(BAR_CHART).module(BarChartModule) }
    sparkline { cell(SPARKLINE).module(SparklineModule) }
  }
}

class BarChartModule
    extends Module
{
  static content = {
    bars(required: false) { $('svg rect') }
    points { bars.collect { it.find('title').text().toInteger() } }
  }
}

class DeltaModule
    extends Module
{
  static content = {
    valueDiv { $('.delta-column').last() }
    chevron { $('.delta-column i') }
    isUp { chevron.classes().contains('up') }
    isDown { chevron.classes().contains('down') }
    value { valueDiv.text().toInteger() }
    row { $() }
    isInverse { $().classes().contains('inverse') }
    isNatural { $().classes().contains('natural') }
    isNeutral { $().classes().contains('neutral') }
  }
}

class SparklineModule
    extends Module
{
  static content = {
    previousPath { $('.line.base') }
    presentPath { $('.line:not(.base)') }
    svgContainer { $('svg') }
    guideText { $('.guide-text') }
  }

  List<Number> getValues() {
    final double height = 25.0
    final int padding = 2

    def path = previousPath.attr('d');
    def points = path.split('L').collect {
      it.split(',')[1].toDouble()
    }
    path = presentPath.attr('d')
    def presentPoint = path.split('L').collect {
      it.split(',')[1].toDouble()
    }.drop(1)
    points = points.plus(presentPoint)
    return points.collect {
      1.0 - (it - padding) / (height - 2 * padding)
    }
  }

  boolean isTrailingGreen() {
    def parent = parent('tr')
    return presentPath.hasClass('up') && parent.hasClass('natural') || presentPath.hasClass('down') && parent.hasClass('inverse')
  }

  boolean isTrailingRed() {
    def parent = parent('tr')
    return presentPath.hasClass('up') && parent.hasClass('inverse') || presentPath.hasClass('down') && parent.hasClass('natural')
  }

  boolean isTrailingBlue() {
    return parent('tr').hasClass('neutral')
  }
}

class ThreatHeaderModule
    extends Module
{
  static final String CRITICAL = 'critical'

  static final String SEVERE = 'severe'

  static final String MODERATE = 'moderate'

  static final String LOW = 'low'

  int columnOffset

  static content = {
    critical(required: false) { $('#threat-header-critical') }
    severe(required: false) { $('#threat-header-severe') }
    moderate(required: false) { $('#threat-header-moderate') }
    low(required: false) { $('#threat-header-low') }
  }

  Map<String, Integer> getColumnPositions() {
    Map<String, Integer> positions = new HashMap<>()
    int position = columnOffset + 1
    positions.put(CRITICAL, critical.displayed ? position++ : 0)
    positions.put(SEVERE, severe.displayed ? position++ : 0)
    positions.put(MODERATE, moderate.displayed ? position++ : 0)
    positions.put(LOW, low.displayed ? position++ : 0)
    return positions
  }
}

class ComponentViolationsTable
    extends Module
{
  static content = {
    threatHeaders { module(new ThreatHeaderModule(columnOffset: 3)) }
    rows { $('tbody tr').moduleList { new ComponentViolationsTableRow(threatColumnPositions: threatHeaders.columnPositions) } }
  }
}

class ComponentViolationsTableRow
    extends Module
{
  Map<String, Integer> threatColumnPositions

  static content = {
    component { $('td:first-child') }
    componentLink { $('td:first-child > a') }
    affectedApplications { $('td:nth-child(2)') }
    affectedApplicationsLink { $('td:nth-child(2) > a') }
    netRisk { $('td:nth-child(3)') }
    criticalRisk(required: false) { $("td:nth-child(${threatColumnPositions[ThreatHeaderModule.CRITICAL]})") }
    severeRisk(required: false) { $("td:nth-child(${threatColumnPositions[ThreatHeaderModule.SEVERE]})") }
    moderateRisk(required: false) { $("td:nth-child(${threatColumnPositions[ThreatHeaderModule.MODERATE]})") }
    lowRisk(required: false) { $("td:nth-child(${threatColumnPositions[ThreatHeaderModule.LOW]})") }
  }
}

class ApplicationViolationsTable
    extends Module
{
  static content = {
    threatHeaders { module(new ThreatHeaderModule(columnOffset: 2)) }
    rows {
      $('tbody tr').moduleList { new ApplicationViolationsTableRow(threatColumnPositions: threatHeaders.columnPositions) }
    }
  }
}

class ApplicationViolationsTableRow
    extends Module
{
  Map<String, Integer> threatColumnPositions

  static content = {
    expand(required: false) { $('td:first-child span.expand').module(ExpandoModule) }
    collapse(required: false) { $('td:first-child span.collapse').module(ExpandoModule) }
    application { $('td:first-child') }
    netRisk { $('td:nth-child(2)') }
    criticalRisk(required: false) { $("td:nth-child(${threatColumnPositions[ThreatHeaderModule.CRITICAL]})") }
    severeRisk(required: false) { $("td:nth-child(${threatColumnPositions[ThreatHeaderModule.SEVERE]})") }
    moderateRisk(required: false) { $("td:nth-child(${threatColumnPositions[ThreatHeaderModule.MODERATE]})") }
    lowRisk(required: false) { $("td:nth-child(${threatColumnPositions[ThreatHeaderModule.LOW]})") }
    reportLink(required: false) { $('td:first-child > a') }
  }
}
