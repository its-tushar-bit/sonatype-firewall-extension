/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import spock.lang.Stepwise


/**
 * @since 1.7
 */
@Stepwise
class NavigationLinksSpec
    extends BaseSpec
{
  def setupSpec() {
    loginAsAdminVia()
  }

  def "Can use navigation to view the Management application"(){
    when: 'clicking the dropdown and then the Management link'
      dashboardModule.toManagement()

    then: 'we should end up at the Management page with the Applications nav link shown as active'
      at ManagementPage
  }


  def "Can use navigation to view the Report application"(){
    when: 'clicking the dropdown and then the Reports link'
      dashboardModule.toReports()

    then: 'we should end up back at the Report page with the Violations nav link shown as active'
      at ReportViolationsPage
  }
}
