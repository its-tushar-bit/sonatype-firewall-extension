/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.ImportPolicyModule

class OrganizationManagementPage
    extends BasePage
{
  static url = "assets/index.html#/management/organization"

  static at = { newOrganizationButton.displayed }

  static content = {
    newOrganizationButton(wait: true, to: OrganizationPage) { $('#nav-create-org') }
    organizationList(required: false) { $('#nav-org-list > li[ng-repeat] > a') }
    organization(to: OrganizationPage) { name -> organizationList.find {it.text() == name} }
  }

  public void createOrg(name = 'test organization') {
    newOrganizationButton.click()
    browser.with {
      OrganizationPage organizationPage = at(OrganizationPage)
      organizationPage.editOrg(name)
    }
  }

  def createOrgWithDefaultPolicy(name = 'test organization', File file = ImportPolicyModule.samplePolicyFile) {
    newOrganizationButton.click()
    browser.with {
      OrganizationPage organizationPage = at(OrganizationPage)
      organizationPage.editOrg(name)
      organizationPage.policyImport.importPolicy(file)
    }
  }
}
