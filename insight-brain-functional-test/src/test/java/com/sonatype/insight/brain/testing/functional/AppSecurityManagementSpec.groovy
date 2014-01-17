/**
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional


class AppSecurityManagementSpec extends BaseSpec {
  // assumes a license has already been installed
  // get to the organizations page
  def setup() {
    loginAsAdmin()
  }

  def cleanup() {
    cleanAppsAndOrgs()
  }

  def "validate organization roles"() {
    when: "Open Security Tab"
    createOrganization();
    tabs.securityTabButton.click()

    then: "security tab content is shown"
    waitFor { tabs.securityTab.displayed }
    waitFor { tabs.securityTab.role("Developer").displayed }
    waitFor { tabs.securityTab.role("Owner").displayed }
  }

  def "validate application roles"() {

    when: "create a new application"
    createOrganization();
    createApplication();

    then: "see the security tab shown"
    waitFor { tabs.securityTabButton.displayed }

    when: "user clicks on security tab"
    tabs.securityTabButton.click()

    then: "security tab is shown"
    waitFor { tabs.securityTab.displayed }
    waitFor { tabs.securityTab.role("Developer").displayed }
    waitFor { tabs.securityTab.role("Owner").displayed }
  }

  void createOrganization() {
    OrganizationManagementPage organizationManagementPage = to(OrganizationManagementPage)
    organizationManagementPage.createOrg()
  }

  void createApplication() {
    ApplicationManagementPage applicationManagementPage = to(ApplicationManagementPage)
    applicationManagementPage.createApp()
  }
}