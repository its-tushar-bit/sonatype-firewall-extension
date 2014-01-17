/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.testing.functional


class OrganizationManagementPage
    extends BasePage
{
  static url = "assets/index.html#/management/organization"

  static at = { driver.currentUrl.endsWith(url) }

  static content = {
    newOrganizationButton(wait: true, to: OrganizationPage) { $('a', text: contains('New Organization')) }
    organizationList(required: false) { $('li', 'ng-repeat': startsWith('organization in organizations')).find('a') }
    organization { name -> organizationList.find {it.text() == name} }
  }

  public void createOrg(name = 'test organization') {
    newOrganizationButton.click()
    browser.with {
      OrganizationPage organizationPage = at(OrganizationPage)
      organizationPage.editOrg(name)
    }
  }
}
