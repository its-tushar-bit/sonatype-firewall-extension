/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.google.common.io.Resources
import com.sonatype.insight.brain.service.InsightBrainService
import com.sonatype.insight.brain.service.InsightConfig
import com.yammer.dropwizard.testing.junit.DropwizardServiceRule
import geb.spock.GebReportingSpec
import org.junit.ClassRule
import org.junit.rules.TestRule
import spock.lang.Shared
import spock.lang.Stepwise
import spock.lang.Unroll

/**
 * @since 1.7
 */
@Stepwise
class NavigationToSpec
    extends GebReportingSpec
{
  @Shared
  @ClassRule
  TestRule startServiceRule = new DropwizardServiceRule<InsightConfig>(InsightBrainService.class,
      Resources.getResource('config-test.yml').getPath())

  def setupSpec() {
    to LoginPage
    loginAsAdmin()
    waitFor { at ReportPage }
  }

  @Unroll("Navigating to #pageUnderTest should take us to #pageUnderTest.url")
  def "Should be able to navigate directly using URLs once logged in"() {
    when: "Navigating to "
    to pageUnderTest

    then:
    at pageUnderTest

    where:
    pageUnderTest << [LoginPage, ManagementPage, ReportPage, UserManagementPage, GlobalRolesPage,
        ApplicationManagementPage, OrganizationManagementPage]
    //deliberately not including TrendingReportPage here as that automatically triggers generation of the report data
  }
}
