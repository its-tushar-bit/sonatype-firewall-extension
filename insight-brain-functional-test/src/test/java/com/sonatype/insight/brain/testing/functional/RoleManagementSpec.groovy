/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import spock.lang.Stepwise

@Stepwise
class RoleManagementSpec
extends BaseSpec {
  def setupSpec() {
    loginAsAdminVia(RoleManagementPage)
  }

  def "Arriving at role management page we should see the list of roles."() {
    when: 'first viewing the page'
    at RoleManagementPage

    then: 'the list of roles is present'
    roleItems.size() > 0

    and: 'the list of roles is sorted properly'
    roleName(0) == 'Administrator'
    roleName(1) == 'CLM Administrator'
    roleName(2) == 'Owner'
    roleName(3) == 'Developer'
    roleName(4) == 'Application Evaluator'
    roleName(5) == 'Component Evaluator'
  }
}
