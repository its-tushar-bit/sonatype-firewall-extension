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
      $('#org-app-filters > div:nth-child(1)').module(DashboardFilterDimensionModule)
    }

    applicationFilter(required: false) {
      $('#org-app-filters > div:nth-child(2)').module(DashboardFilterDimensionModule)
    }

    applicationCategoryFilter(required: false) {
      $('#category-filter').module(DashboardFilterDimensionModule)
    }
    stagesFilter(required: false) {
      $('#stage-filter').module(DashboardFilterDimensionModule)
    }

    policyTypesFilter(required: false) {
      $('#policy-type-filter').module(DashboardFilterDimensionModule)
    }
    policyThreatLevelFilter(required: false) {
      $('#threat-level-filter').module(DashboardFilterDimensionModule)
    }

    policyThreatLevelSlider(required: false) { $('.policy-threat-level-slider').module(SliderModule) }

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
