/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional


class ApplicationManagementPage
    extends BasePage
{
  static url = "assets/index.html#/management/application"

  static at = { newApplicationButton.displayed }

  static content = {
    newApplicationButton(wait: true, to: ApplicationPage) { $('#nav-create-app') }
    applicationList(required: false) { $('#nav-app-list > li[ng-repeat] > a') }
    application { name -> applicationList.find {it.text() == name} }
  }

  void createApp(name = 'test application', id = 'test application', orgName = 'test organization') {
    newApplicationButton.click()
    browser.with {
      ApplicationPage applicationPage = at(ApplicationPage)
      applicationPage.editNewApp(name, id, orgName)
    }
  }
}
