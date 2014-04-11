/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.ButtonsModule
import com.sonatype.insight.brain.testing.functional.modules.DropdownMultiSelect

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
    filterPanel(required: false) { $('div', 'ng-if': 'appliedApplicationPublicIds.length > 0') }
    filterButtons(required: false) { module ButtonsModule, $('.filter-edit-buttons') }

    applicationFilters(required: false) {
      filterPanel.find('span', 'ng-repeat': 'applicationId in appliedApplicationPublicIds')
    }
    applicationFiltersDropdown(required: false) { module DropdownMultiSelect, $('span', items: 'applications') }

    highestRiskTable(required: false) { $('tr', 'ng-repeat': startsWith('risk in highestRisks')) }
    newestViolationTable(required: false) { $('tr', 'ng-repeat': startsWith('risk in newestRisks')) }
    policyViolation { table, i -> table[i] }
    policyViolationRisk { table, i -> table[i].find('td')[1] }
    policyViolationPolicy { table, i -> table[i].find('td')[2] }
    policyViolationApplication { table, i -> table[i].find('td')[3] }
    policyViolationComponent { table, i -> table[i].find('td')[4] }
  }
}