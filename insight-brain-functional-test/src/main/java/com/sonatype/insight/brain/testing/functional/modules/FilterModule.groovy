/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import geb.Module

class FilterModule
    extends Module
{
  static content = {
    applicationFilter(required: false) {
      module DashboardFilterDimensionModule, $('dashboard-filter-dimension:nth-child(1)')
    }

    applicationCategoryFilter(required: false) {
      module DashboardFilterDimensionModule, $('dashboard-filter-dimension:nth-child(2)')
    }
    stagesFilter(required: false) {
      module DashboardFilterDimensionModule, $('dashboard-filter-dimension:nth-child(3)')
    }

    policyTypesFilter(required: false) {
      module DashboardFilterDimensionModule, $('dashboard-filter-dimension:nth-child(4)')
    }
    policyThreatLevelFilter(required: false) {
      module DashboardFilterDimensionModule, $('.tree-view-group:nth-child(5)')
    }

    policyThreatLevelSlider(required: false) { module SliderModule, $('.policy-threat-level-slider') }

    applyButton(required: false) { $('#dashboard-filter-apply') }
    revertButton(required: false) { $('#dashboard-filter-revert') }
    clearButton(required: false) { $('#dashboard-filter-clear') }
  }

  def toggleTwisties(){
    applicationFilter.twisty.click()
    applicationCategoryFilter.twisty.click()
    stagesFilter.twisty.click()
    policyTypesFilter.twisty.click()
    policyThreatLevelFilter.twisty.click()
  }

  def apply() {
    applyButton.click()
    // NOTE: Wait for filter to be persisted
    waitFor { applyButton.classes().contains('disabled') }
  }
}
