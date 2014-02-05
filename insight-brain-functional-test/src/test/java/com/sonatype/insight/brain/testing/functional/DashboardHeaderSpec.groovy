/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

class DashboardHeaderSpec
    extends BaseSpec 
{
  def setup() {
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
      waitFor { dashboardModule.version.displayed }
      waitFor { dashboardModule.version.text() == props["version"] }
  }
}
