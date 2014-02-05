/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import spock.lang.Stepwise
import spock.lang.Unroll

/**
 * @since 1.7
 */
@Stepwise
class NavigationToSpec
    extends BaseSpec
{
  def setupSpec() {
    loginAsAdminVia()
  }

  @Unroll("Navigating to #pageUnderTest should take us to #pageUnderTest.url")
  def "Should be able to navigate directly using URLs once logged in"() {
    when: "Navigating to "
      to pageUnderTest
    
    then:
      at pageUnderTest

    where:
      pageUnderTest << [ManagementPage, ReportViolationsPage, UserManagementPage, GlobalRolesPage,
          ApplicationManagementPage, OrganizationManagementPage, ReportViolationsPage]
    //deliberately not including TrendingReportPage here as that automatically triggers generation of the report data
  }
}
