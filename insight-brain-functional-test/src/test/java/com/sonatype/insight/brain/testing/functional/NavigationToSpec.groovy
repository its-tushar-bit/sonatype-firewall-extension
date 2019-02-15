/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import spock.lang.Stepwise
import spock.lang.Unroll

/**
 * @since 1.7
 */
@Stepwise
class NavigationToSpec
    extends BaseSpec
{
  @Override
  def setupSpec() {
    loginAsAdminVia()
  }

  @Unroll("Navigating to #pageUnderTest.simpleName should take us to #pageUnderTest.url")
  def "Should be able to navigate directly using URLs once logged in"() {
    when: "Navigating to #pageUnderTest"
      to pageUnderTest

    then: "Should be at #pageUnderTest.url"
      at pageUnderTest

    where:
      pageUnderTest << [ReportViolationsPage, UserManagementPage, AdministratorsPage]
  }
}
