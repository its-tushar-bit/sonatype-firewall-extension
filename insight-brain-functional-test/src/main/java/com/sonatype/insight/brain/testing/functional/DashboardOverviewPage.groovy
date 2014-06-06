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
      filterPanel.find('span[ng-repeat="applicationId in filter.applicationPublicIds"]')
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

    applicationFiltersDropdown(required: false) { module DropdownMultiSelectModule, $('span[items="applications"]') }
    policyThreatFiltersDropdown(required: false) { module DropdownMultiSelectModule, $('span[items="policyThreatTypes"]') }
    stageTypeFiltersDropdown(required: false) { module DropdownMultiSelectModule, $('span[items="stageTypes"]') }
    applicationTagFiltersDropdown(required: false) { module DropdownMultiSelectModule, $('span[items="applicationTags"]') }
    policyThreatLevelSlider(required: false) { module SliderModule, $('#policy-threat-levels') }

    highestRiskTable(required: false) { module ThreatTableModule, $('div[ng-switch-when="policy-violations"]') }
    newestViolationTable(required: false) { module ThreatTableModule, $('div[ng-switch-when="newest-risk"]') }
    componentViolationsTable(required: false) { module ComponentViolationsTable, $('#component-risk') }
    applicationViolationsTable(required: false) { module ApplicationViolationsTable, $('#application-risk') }

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
  }

  void applyFilter() {
    applyButton.click()
    // NOTE: Wait for filter to be persisted before the next test tries to reset it
    waitFor { !applyButton.displayed }
  }
}

class PolicySummaryModule
    extends Module
{
  static content = {
    rows(required: false) { moduleList PolicySummaryRow, $('tr').tail() }
  }
}

class PolicySummaryRow
    extends Module
{
  static final int CATEGORY = 0
  static final int COUNTS = 1
  static final int DELTA = 2
  static final int BAR_CHART = 3
  static final int SPARKLINE = 4

  static content = {
    cell(required: false) { int i -> $('td', i) }
    category { cell(CATEGORY).text() }
    counts { cell(COUNTS).text() }
    delta { cell(DELTA).text() }
    barChart { module BarChartModule, cell(BAR_CHART)}
    sparkline { module SparklineModule, cell(SPARKLINE) }
  }
}

class BarChartModule
    extends Module
{

}

class SparklineModule
    extends Module
{
  static content = {
    previousPath { $('.line.base') }
    presentPath { $('.line:not(.base)') }
    guideText { $('.guide-text') }
  }

  List<Number> getValues() {
    def path = previousPath.attr('d');
    def points = path.split('L').collect {
      it.split(',')[1].toDouble()
    }
    path = presentPath.attr('d')
    def presentPoint = path.split('L').collect {
      it.split(',')[1].toDouble()
    }.drop(1)
    points = points.plus(presentPoint)
    def maxValue = points.max()
    return points.collect {
      1.0 - it / maxValue
    }
  }

  boolean isTrailingGreen() {
    return presentPath.hasClass('green')
  }
}

class ComponentViolationsTable
    extends Module 
{
  static content = {
    rows { moduleList ComponentViolationsTableRow, $('tbody tr') }
  }
}

class ComponentViolationsTableRow
    extends Module
{
  static content = {
    component { $('td:first-child') }
    componentLink { $('td:first-child > a') }
    affectedApplications { $('td:nth-child(2)') }
    netRisk { $('td:nth-child(3)') }
    criticalRisk { $('td:nth-child(4)') }
    severeRisk { $('td:nth-child(5)') }
    moderateRisk { $('td:nth-child(6)') }
    lowRisk { $('td:nth-child(7)') }
  }
}

class ApplicationViolationsTable
    extends Module
{
  static content = {
    rows { moduleList ApplicationViolationsTableRow, $('tbody tr') }
  }
}

class ApplicationViolationsTableRow
    extends Module
{
  static content = {
    expand(required: false) { module ExpandoModule, $('td:first-child i.expand') }
    collapse(required: false) { module ExpandoModule, $('td:first-child i.collapse') }
    application { $('td:first-child') }
    netRisk { $('td:nth-child(2)') }
    criticalRisk { $('td:nth-child(3)') }
    severeRisk { $('td:nth-child(4)') }
    moderateRisk { $('td:nth-child(5)') }
    lowRisk { $('td:nth-child(6)') }
    reportLink(required: false) { $('td:first-child > a') }
  }
}
