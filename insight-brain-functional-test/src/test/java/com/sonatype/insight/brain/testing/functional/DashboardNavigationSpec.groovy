/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

/**
 * @since 1.11
 */
class DashboardNavigationSpec
    extends BaseSpec
{

  def "Can navigate directly to page with url"() {
    when:
      loginAsAdminVia(DashboardOverviewPage, tableName)

    then:
      waitFor { noDataAvailable.displayed }

    where:
      tableName << ['newest-risk', 'components', 'applications']
  }

  def "Back button will always take us back to the previous dashboard page"() {
    setup: 'logging in as admin and click through the pages'
      loginAsAdminVia(NewestRiskDashboardPage)
      to ComponentViolationsDashboardPage
      to ApplicationViolationsDashboardPage

    when: 'back button press'
      driver.navigate().back()

    then: 'puts us at the component violation page'
      waitFor { at(ComponentViolationsDashboardPage) }

    when: 'back button again'
      driver.navigate().back()

    then: 'will take us back to newest risk page'
      waitFor { at(NewestRiskDashboardPage) }
  }
}