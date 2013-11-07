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


/**
 * @since 1.7
 */
@Stepwise
class NavigationLinksSpec extends GebReportingSpec
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

  def "Can use navigation to view the Management application"(){
    when: 'clicking the dropdown and then the Management link'
    dropdownNav.toManagement()

    then: 'we should end up at the Management page with the Applications nav link shown as active'
    at ManagementPage
  }


  def "Can use navigation to view the Report application"(){
    when: 'clicking the dropdown and then the Reports link'
    dropdownNav.toReports()

    then: 'we should end up back at the Report page with the Violations nav link shown as active'
    at ReportPage
  }

}
