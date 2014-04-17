/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.ButtonsModule
import com.sonatype.insight.brain.testing.functional.modules.DropdownMultiSelectModule

/**
 * Since 1.11
 */
class DashboardPage
  extends BasePage
{
  static url = "assets/index.html#/dashboard"

  static at = { filterPanelToggle.displayed }

  static content = {
    filterPanelToggle { $('a', 'ng-click': 'toggleCollapse()') }
    filterPanel(required: false) { $('div', 'ng-if': 'filters.applicationPublicIds.applied.length > 0') }
    filterButtons(required: false) { module ButtonsModule, $('.filter-edit-buttons') }

    applicationFilters(required: false) {
      filterPanel.find('span', 'ng-repeat': 'applicationId in filters.applicationPublicIds.applied')
    }

    noAvailableApplications(required: false) { $('#no-permissions') }
    noDataAvailableHighest(required: false) { $('#no-data-highest-risk') }
    noDataAvailableNewest(required: false) { $('#no-data-newest-risk') }

    applicationFiltersDropdown(required: false) { module DropdownMultiSelectModule, $('span', items: 'applications') }
    policyThreatFiltersDropdown(required: false) { module DropdownMultiSelectModule, $('span', items: 'policyThreatCategories') }
    stageTypeFiltersDropdown(required: false) { module DropdownMultiSelectModule, $('span', items: 'stageTypes') }

    highestRiskTable(required: false) { $('#highest-risk tr', 'ng-repeat': startsWith('risk in risks')) }
    newestViolationTable(required: false) { $('#newest-risk tr', 'ng-repeat': startsWith('risk in risks')) }
    threatLevelHeader(required: false) { $('#highest-risk-threat-header a') }
    policyViolation { table, i -> table[i] }
    policyViolationRisk { table, i -> table[i].find('td')[1] }
    policyViolationPolicy { table, i -> table[i].find('td')[2] }
    policyViolationApplication { table, i -> table[i].find('td')[3] }
    policyViolationComponent { table, i -> table[i].find('td')[4] }
    policyViolationTime(required: false) { table, i -> table[i].find('td')[5] }
  }
}