/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.license.model.ProductLicenseDetails

class MainHeaderSpec
    extends BaseSpec
{
  def setupSpec() {
    // ensure that an actual page is loaded.  Without this the call to `refresh()` in the setup method will
    // fail
    via ReportViolationsPage
  }

  def setup() {
    productLicenseManager.reset()
    clmLicenseManager.installLicense(null)

    // The session gets reset between each test. We therefore need to refresh before we can cleanly log in again
    // (otherwise we'd get the login modal with the username preset and uneditable).
    driver.navigate().refresh()
    loginAsAdminVia()
  }

  def "displays logged in users display name"() {
    given: "user has logged in"

    expect: "users display name is shown"
      waitFor { userOptions.displayName.text() == "Admin BuiltIn" }
  }

  def "displays version in the header"() {
    given: "user has logged in"
      def props = new Properties()
      props.load(getClass().getResourceAsStream("/version.properties"));

    expect: "version is shown"
      waitFor { mainModule.version.displayed }
      waitFor { mainModule.version.text().contains(props["version"]) }
  }

  def "dashboard icon shown when licensed"() {
    given: "user has logged in"

    expect: "dashboard icon to be visible"
      waitFor { mainModule.dashboard.displayed }
  }

  def "dashboard icon not shown when not licensed"() {
    given: "license is modified and page is refreshed"
      setLicensedProducts(ProductLicenseDetails.PRODUCT_NEXUS)
      driver.navigate().refresh()

    expect: "dashboard icon is not shown"
      waitFor { !mainModule.dashboard.present && mainModule.reports.present && mainModule.management.present }
  }

  def "dashboard default page when licensed"() {
    when: "user navigates to index page"
      via IndexPage

    then: "user arrives at dashboard"
      waitFor { at DashboardOverviewPage }
  }

  def "dashboard not default page when unlicensed"() {
    given: "a license that doesn't support the dashboard"
      setLicensedProducts(ProductLicenseDetails.PRODUCT_NEXUS)

    when: "user navigates to index page"
      via IndexPage

    then: "user arrives at reports page"
      waitFor { at ReportViolationsPage }
  }
}
