/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.DropdownMultiSelect

/**
 * Since 1.11
 */
class DashboardPage
  extends BasePage
{
  static url = "assets/index.html#/dashboard"

  static at = { applicationFiltersDropdown.displayed }

  static content = {
    applicationFiltersDropdown { module DropdownMultiSelect, $('span', items: 'applications') }
    highestRiskTable { $('tr', 'ng-repeat': 'risk in highestRisks') }
    policyViolation { i -> highestRiskTable[i] }
    policyViolationRisk { i -> policyViolation(i).find('td')[1] }
    policyViolationPolicy { i -> policyViolation(i).find('td')[2] }
    policyViolationApplication { i -> policyViolation(i).find('td')[3] }
  }
}