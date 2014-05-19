/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.ButtonsModule
import com.sonatype.insight.brain.testing.functional.modules.DashboardTabsModule
import com.sonatype.insight.brain.testing.functional.modules.DropdownMultiSelectModule
import com.sonatype.insight.brain.testing.functional.modules.SliderModule
import com.sonatype.insight.brain.testing.functional.modules.ThreatTableModule
import geb.Module

/**
  @since 1.11
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
      filterPanel.find('div[ng-if="filter.policyThreatLevel[0] > 0 || filter.policyThreatLevel[1] < 10"]').find('span')
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
  }

  void applyFilter() {
    applyButton.click()
    // NOTE: Wait for filter to be persisted before the next test tries to reset it
    waitFor { !applyButton.displayed }
  }
}

class ComponentViolationsTable extends Module {
  static content = {
    rows { moduleList ComponentViolationsTableRow, $('tbody tr') }
  }
}

class ComponentViolationsTableRow extends Module {
  static content = {
    component { $('td:first-child') }
    componentLink { $('td:first-child > a') }
    netRisk { $('td:nth-child(2)') }
    criticalRisk { $('td:nth-child(3)') }
    severeRisk { $('td:nth-child(4)') }
    moderateRisk { $('td:nth-child(5)') }
    lowRisk { $('td:nth-child(6)') }
  }
}

class ApplicationViolationsTable extends Module {
  static content = {
    rows { moduleList ApplicationViolationsTableRow, $('tbody tr') }
  }
}

class ApplicationViolationsTableRow extends Module {
  static content = {
    expand(required: false) { $('td:first-child i.icon-plus-sign') }
    collapse(required: false) { $('td:first-child i.icon-minus-sign') }
    application { $('td:first-child') }
    netRisk { $('td:nth-child(2)') }
    criticalRisk { $('td:nth-child(3)') }
    severeRisk { $('td:nth-child(4)') }
    moderateRisk { $('td:nth-child(5)') }
    lowRisk { $('td:nth-child(6)') }
    reportLink(required: false) { $('td:first-child > a') }
  }
}
