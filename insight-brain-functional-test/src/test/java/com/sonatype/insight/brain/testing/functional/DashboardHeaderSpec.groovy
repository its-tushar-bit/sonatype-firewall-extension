/**
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

class DashboardHeaderSpec extends BaseSpec {
  def "displays logged in users display name"() {
    when: "user logs in"
      via ReportViolationsPage
      login.loginAsAdmin()
      verifyAt()

    then: "users display name is shown"
      waitFor { userOptions.displayName.text() == "Admin BuiltIn" }
  }
}
