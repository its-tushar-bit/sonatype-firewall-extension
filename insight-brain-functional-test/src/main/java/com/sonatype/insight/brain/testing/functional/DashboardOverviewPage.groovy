/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.ButtonsModule
import com.sonatype.insight.brain.testing.functional.modules.DropdownMultiSelectModule
import com.sonatype.insight.brain.testing.functional.modules.SliderModule
import com.sonatype.insight.brain.testing.functional.modules.ThreatTableModule

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
      filterPanel.find('div[ng-if="filter.policyThreatLevel[0] > 0 || filter.policyThreatLevel[1] < 10"]').find('strong')
    }

    noAvailableApplications(required: false) { $('#no-permissions') }
    noAvailableApplicationTags(required: false) { $('#no-application-tags') }
    noDataAvailableHighest(required: false) { $('#no-data-highest-risk') }
    noDataAvailableNewest(required: false) { $('#no-data-newest-risk') }

    applicationFiltersDropdown(required: false) { module DropdownMultiSelectModule, $('span[items="applications"]') }
    policyThreatFiltersDropdown(required: false) { module DropdownMultiSelectModule, $('span[items="policyThreatTypes"]') }
    stageTypeFiltersDropdown(required: false) { module DropdownMultiSelectModule, $('span[items="stageTypes"]') }
    applicationTagFiltersDropdown(required: false) { module DropdownMultiSelectModule, $('span[items="applicationTags"]') }
    policyThreatLevelSlider(required: false) { module SliderModule, $('#policy-threat-levels') }

    highestRiskTable(required: false) { module ThreatTableModule, $('#highest-risk') }
    newestViolationTable(required: false) { module ThreatTableModule, $('#newest-risk') }
  }

  void applyFilter() {
    applyButton.click()
    // NOTE: Wait for filter to be persisted before the next test tries to reset it
    waitFor { !applyButton.displayed }
  }
}
