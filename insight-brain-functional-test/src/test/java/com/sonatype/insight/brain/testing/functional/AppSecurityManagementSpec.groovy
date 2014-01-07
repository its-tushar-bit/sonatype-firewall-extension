/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional


class AppSecurityManagementSpec extends BaseSpec {
  // assumes a license has already been installed
  // get to the organizations page
  def setup() {
    to ReportViolationsPage
    login.loginAsAdmin()
  }

  def cleanup() {
    applicationDAO.getAll().each {
      applicationDAO.delete(it);
    }
    organizationDAO.getAll().each {
      organizationDAO.delete(it);
    }
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
    to OrganizationManagementPage
    newOrganizationButton.click()
    organizationName.click()
    waitFor { organizationNameField.displayed }
    organizationNameField << "test organization"
    organizationSaveButton.click()
    waitFor { securityTabButton.displayed }
  }

  void createApplication() {
    to ApplicationManagementPage
    newApplicationButton.click()
    applicationName.click()
    waitFor { applicationNameField.displayed }
    applicationNameField << "test application"
    applicationId.click()
    waitFor { applicationIdField.displayed }
    applicationIdField << "testapp"
    applicationOrgField.click()
    waitFor { applicationOrgName('test organization').displayed }
    applicationOrgName('test organization').click()
    applicationSaveButton.click()
  }
}