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
    organizationFilter(required: false) {
      module DashboardFilterDimensionModule, $('#org-app-filters iq-tree-view-multi-select:nth-child(1)')
    }

    applicationFilter(required: false) {
      module DashboardFilterDimensionModule, $('#org-app-filters iq-tree-view-multi-select:nth-child(2)')
    }

    applicationCategoryFilter(required: false) {
      module DashboardFilterDimensionModule, $('#category-filter')
    }
    stagesFilter(required: false) {
      module DashboardFilterDimensionModule, $('#stage-filter')
    }

    policyTypesFilter(required: false) {
      module DashboardFilterDimensionModule, $('#policy-type-filter')
    }
    policyThreatLevelFilter(required: false) {
      module DashboardFilterDimensionModule, $('#threat-level-filter')
    }

    policyThreatLevelSlider(required: false) { module SliderModule, $('.policy-threat-level-slider') }

    applyButton(required: false) { $('#dashboard-filter-apply') }
    revertButton(required: false) { $('#dashboard-filter-revert') }
    clearButton(required: false) { $('#dashboard-filter-clear') }
  }

  def toggleTwisties(){
    organizationFilter.twisty.click()
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
