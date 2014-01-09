/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

class DashboardHeaderSpec extends BaseSpec {
  def "displays logged in users display name"() {
    when: "user logs in"
      to ReportViolationsPage
      login.loginAsAdmin()

    then: "users display name is shown"
      userOptions.displayName.text() == "Admin BuiltIn"
  }
}
