/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import geb.Module

class FilterModule
    extends Module
{
  static content = {
    toggle { $('#dashboard-filter-toggle') }

    applicationSummary(required: false) {
      module TooltipModule, $('#application-filter-item-collapsed')
    }
    applicationTagSummary(required: false) {
      module TooltipModule, $('#application-tag-filter-item-collapsed')
    }
    stageTypeSummary(required: false) {
      module TooltipModule, $('#stage-type-filter-item-collapsed')
    }
    policyTypeSummary(required: false) {
      module TooltipModule, $('#policy-type-filter-item-collapsed')
    }
    policyThreatLevelSummary(required: false) {
      module TooltipModule, $('#policy-threat-level-filter-item-collapsed')
    }

    applicationSummaryCount(required: false) { applicationSummary.find('div') }
    applicationTagSummaryCount(required: false) { applicationTagSummary.find('div') }
    stageTypeSummaryCount(required: false) { stageTypeSummary.find('div') }
    policyTypeSummaryCount(required: false) { policyTypeSummary.find('div') }
    policyThreatLevelSummaryCount(required: false) { policyThreatLevelSummary.find('div') }

    noApplications(required: false) { $('#no-applications') }
    noApplicationTags(required: false) { $('#no-application-tags') }

    applicationMultiselect(required: false) {
      module DropdownMultiSelectModule, $('#application-filter-item span.multi-dropdown'), emptyText: 'All Applications'
    }
    applicationTagMultiselect(required: false) {
      module DropdownMultiSelectModule, $('#application-tag-filter-item span.multi-dropdown'), emptyText: 'All Applications'
    }
    stageTypeMultiselect(required: false) {
      module DropdownMultiSelectModule, $('#stage-type-filter-item span.multi-dropdown'), emptyText: 'All Stages'
    }
    policyTypeMultiselect(required: false) {
      module DropdownMultiSelectModule, $('#policy-type-filter-item span.multi-dropdown'), emptyText: 'All Policy Types'
    }
    policyThreatLevelSlider(required: false) { module SliderModule, $('#policy-threat-level-filter-item') }

    applyButton(required: false) { $('#dashboard-filter-apply') }
    resetButton(required: false) { $('#dashboard-filter-reset') }
    cancelButton(required: false) { $('#dashboard-filter-cancel') }
  }

  def apply() {
    applyButton.click()
    // NOTE: Wait for filter to be persisted
    waitFor { !applyButton.displayed }
  }
}