/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.ButtonsModule
import com.sonatype.insight.brain.testing.functional.modules.DashboardTabsModule
import com.sonatype.insight.brain.testing.functional.modules.DropdownMultiSelectModule
import com.sonatype.insight.brain.testing.functional.modules.ExpandoModule
import com.sonatype.insight.brain.testing.functional.modules.SliderModule
import com.sonatype.insight.brain.testing.functional.modules.ThreatTableModule

import geb.Module

/**
 * @since 1.11
 */
class DashboardOverviewPage
    extends DashboardPage
{
  static at = { filterPanelToggle.displayed }

  static content = {
    filterPanelToggle { $('a[ng-click="toggleCollapse()"]') }
    filterPanel(required: false) { $('.filter-readonly') }
    filterButtons(required: false) { module ButtonsModule, $('.filter-edit-buttons') }
    applyButton(required: false) { filterButtons.button('Apply') }
    resetButton(required: false) { filterButtons.button('Reset') }

    applicationFilters(required: false) {
      filterPanel.find('span[ng-repeat="applicationId in filter.applicationIds"]')
    }
    applicationTagFilters(required: false) {
      filterPanel.find('span[ng-repeat="applicationTagId in filter.applicationTagIds"]')
    }
    stageTypeFilters(required: false) {
      filterPanel.find('span[ng-repeat="stageTypeId in filter.stageTypeIds"]')
    }
    policyThreatTypeFilters(required: false) {
      filterPanel.find('span[ng-repeat="policyThreatTypeId in filter.policyThreatTypes"]')
    }
    policyThreatLevelFilters(required: false) {
      $('#filter-summary-threat-level span')
    }

    noAvailableApplications(required: false) { $('#no-permissions') }
    noAvailableApplicationTags(required: false) { $('#no-application-tags') }
    noDataAvailable(required: false) { $('#no-data') }

    applicationFiltersDropdown(required: false) {
      module DropdownMultiSelectModule, $('span[items="applications"]'), emptyText: 'All Applications'
    }
    policyThreatFiltersDropdown(required: false) {
      module DropdownMultiSelectModule, $('span[items="policyThreatTypes"]'), emptyText: 'All Policy Types'
    }
    stageTypeFiltersDropdown(required: false) {
      module DropdownMultiSelectModule, $('span[items="stageTypes"]'), emptyText: 'All Stages'
    }
    applicationTagFiltersDropdown(required: false) {
      module DropdownMultiSelectModule, $('span[items="applicationTags"]'), emptyText: 'All Applications'
    }
    policyThreatLevelSlider(required: false) { module SliderModule, $('#policy-threat-levels') }

    highestRiskDiv(required: false) { module ThreatTableModule, $('#highest-risk') }
    maxResults(required: false) { module ThreatTableModule, $('#max-results-shown') }

    tabLinks { module DashboardTabsModule, $('ul.nav.nav-tabs') }

    summaryData(required: false) { $('#summary-data') }
    summaryTotalApplications(required: false) { $('#summary-total-applications') }
    summaryMatchedApplications(required: false) { $('#summary-matched-applications') }
    summaryPercentApplications(required: false) { $('#summary-percent-applications') }
    summaryTotalPolicies(required: false) { $('#summary-total-policies') }
    summaryMatchedPolicies(required: false) { $('#summary-matched-policies') }
    summaryPercentPolicies(required: false) { $('#summary-percent-policies') }
    summaryTotalComponents(required: false) { $('#summary-total-components') }
    summaryMatchedComponents(required: false) { $('#summary-matched-components') }
    summaryPercentComponents(required: false) { $('#summary-percent-components') }

    unknownComponentPopover(required: false) { $('.popover.pathnames-popover') }
    unknownComponentPopoverTitle(required: false) { $('.popover-title').text() }
    unknownComponentPopoverText(required: false) { $('.popover-content.pathnames-popover-content').text() }

    componentMatchSection(required: false) { $('#component-match-results') }
    componentMatchExactCount(required: false) { $('#component-match-results .percentage-graph-legend-count')[0] }
    componentMatchSimilarCount(required: false) { $('#component-match-results .percentage-graph-legend-count')[1] }
    componentMatchUnknownCount(required: false) { $('#component-match-results .percentage-graph-legend-count')[2] }

    policySummary { module PolicySummaryModule, $('#policySummaryData') }

    applicationHeatMapHelp(required: false) { $('#application-heat-map-help-content') }
    applicationHeatMapHelpClose(required: false) { $('#application-heat-map-help-close') }
    componentHeatMapHelp(required: false) { $('#component-heat-map-help-content') }
    componentHeatMapHelpClose(required: false) { $('#component-heat-map-help-close') }
    modalBackdrop(required: false) { $('div.modal-backdrop') }
  }

  void applyFilter() {
    applyButton.click()
    // NOTE: Wait for filter to be persisted before the next test tries to reset it
    waitFor { !applyButton.displayed }
  }
}

class NewestRiskDashboardPage
    extends DashboardOverviewPage
{
  static url = DashboardOverviewPage.url + "/newest-risk"

  static content = {
    newestViolationTable(required: false) { module ThreatTableModule, $('#highest-risk-table') }
  }
}

class ComponentViolationsDashboardPage
    extends DashboardOverviewPage
{
  static url = DashboardOverviewPage.url + "/components"

  static content = {
    componentViolationsTable(required: false) { module ComponentViolationsTable, $('#component-risk') }
  }
}

class ApplicationViolationsDashboardPage
    extends DashboardOverviewPage
{
  static url = DashboardOverviewPage.url + "/applications"

  static content = {
    applicationViolationsTable(required: false) { module ApplicationViolationsTable, $('#application-risk') }
  }
}

class PolicySummaryModule
    extends Module
{
  static content = {
    rows(required: false) { moduleList PolicySummaryRow, $('tr').tail() }
    pendingRow { (PolicySummaryRow) rows[0] }
    fixedRow { (PolicySummaryRow) rows[1] }
    discoveredRow { (PolicySummaryRow) rows[2] }
  }
}

class PolicySummaryRow
    extends Module
{
  static final int CATEGORY = 0

  static final int COUNT = 1

  static final int BAR_CHART = 5

  static final int SPARKLINE = 6

  static content = {
    cell(required: false) { int i -> $('td', i) }
    category { cell(CATEGORY).text() }
    count { cell(COUNT).text().toInteger() }
    delta { module DeltaModule }
    barChart { module BarChartModule, cell(BAR_CHART) }
    sparkline { module SparklineModule, cell(SPARKLINE) }
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
    outerDiv { $('.delta-column div') }
    valueDiv { $('.delta-column').last() }
    chevronDiv { $('i') }
    isUp { chevronDiv.classes().contains('up') }
    isDown { chevronDiv.classes().contains('down') }
    value { valueDiv.text().toInteger() }
    isPositive { outerDiv.classes().contains('delta-positive') }
    isNegative { outerDiv.classes().contains('delta-negative') }
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
    def path = previousPath.attr('d');
    def padding = 2
    def points = path.split('L').collect {
      it.split(',')[1].toDouble()
    }
    path = presentPath.attr('d')
    def presentPoint = path.split('L').collect {
      it.split(',')[1].toDouble()
    }.drop(1)
    points = points.plus(presentPoint)
    def maxValue = svgContainer.attr('height').toDouble()
    return points.collect {
      1.0 - (it - padding) / (maxValue - 2 * padding)
    }
  }

  boolean isTrailingGreen() {
    return presentPath.hasClass('green')
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
    threatHeaders { module ThreatHeaderModule, columnOffset: 3 }
    rows { moduleList ComponentViolationsTableRow, $('tbody tr'), threatColumnPositions: threatHeaders.columnPositions }
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
    threatHeaders { module ThreatHeaderModule, columnOffset: 2 }
    rows {
      moduleList ApplicationViolationsTableRow, $('tbody tr'), threatColumnPositions: threatHeaders.columnPositions
    }
  }
}

class ApplicationViolationsTableRow
    extends Module
{
  Map<String, Integer> threatColumnPositions

  static content = {
    expand(required: false) { module ExpandoModule, $('td:first-child i.expand') }
    collapse(required: false) { module ExpandoModule, $('td:first-child i.collapse') }
    application { $('td:first-child') }
    netRisk { $('td:nth-child(2)') }
    criticalRisk(required: false) { $("td:nth-child(${threatColumnPositions[ThreatHeaderModule.CRITICAL]})") }
    severeRisk(required: false) { $("td:nth-child(${threatColumnPositions[ThreatHeaderModule.SEVERE]})") }
    moderateRisk(required: false) { $("td:nth-child(${threatColumnPositions[ThreatHeaderModule.MODERATE]})") }
    lowRisk(required: false) { $("td:nth-child(${threatColumnPositions[ThreatHeaderModule.LOW]})") }
    reportLink(required: false) { $('td:first-child > a') }
  }
}
