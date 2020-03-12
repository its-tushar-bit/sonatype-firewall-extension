/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.model.Application
import com.sonatype.insight.brain.model.Organization
import com.sonatype.insight.brain.model.policy.PolicyEvaluation
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory
import com.sonatype.insight.brain.model.policy.stages.BuildStageType
import com.sonatype.insight.brain.model.security.Permission
import com.sonatype.insight.brain.model.security.Role
import com.sonatype.insight.brain.model.security.User

/**
 * @since 1.11
 */
class DashboardAuthzSpec
extends BaseSpec {
  static Organization org

  static Application firstApp

  static Application secondApp

  static User userWithPermission

  static User userWithoutPermission

  @Override
  def setupSpec() {
    org = temporaryEntity.newOrganization('DashboardAuthzSpec')
    firstApp = temporaryEntity.newApplication('DashboardAuthzSpecAppOne', 'DashboardAuthzSpecAppOne', org.id)
    secondApp = temporaryEntity.newApplication('DashboardAuthzSpecAppTwo', 'DashboardAuthzSpecAppTwo', org.id)

    def policy = temporaryEntity.newPolicy(org.id, 'DashboardAuthzSpecPolicy')

    PolicyEvaluation firstPolicyEvaluation = temporaryEntity.
        newPolicyEvaluation(firstApp.id, BuildStageType.ID, 'DashboardAuthzSpecFirstEvaluation')
    temporaryEntity.newPolicyViolation(firstPolicyEvaluation, policy, 5,
        PolicyThreatCategory.LICENSE, "Group1", "Artifact1", "Version1")
    PolicyEvaluation secondPolicyEvaluation = temporaryEntity.
        newPolicyEvaluation(secondApp.id, BuildStageType.ID, 'DashboardAuthzSpecSecondEvaluation')
    temporaryEntity.newPolicyViolation(secondPolicyEvaluation, policy, 10, PolicyThreatCategory.QUALITY)

    userWithPermission = temporaryEntity.newUser();
    Role role = temporaryEntity.newRole(false, Permission.READ);
    temporaryEntity.newMembershipMapping(firstApp.id, role.id, userWithPermission.username);

    userWithoutPermission = temporaryEntity.newUser()
  }

  def 'Should only see one of two available applications based on READ permission to one'() {
    setup: 'Logging in as a user permission to 1 of 2 applications'
    loginAsUserVia(userWithPermission.username, userWithPermission.password, NewestRiskDashboardPage)

    when: 'looking at available applications to filter on'
    waitFor { filters.applicationFilter.displayed }
    filters.applicationFilter.nxTwisty.click()

    then: 'only the permissioned application and the all application option are shown'
    !filters.applicationFilter.multiSelectList.displayed
    filters.applicationFilter.nxCounter.text() == '1'
  }

  def 'Should have no applications to choose from without permissions'() {
    setup: 'Logging in as a user without permission to any applications'
    loginAsUserVia(userWithoutPermission.username, userWithoutPermission.password, NewestRiskDashboardPage)

    when: 'looking at available applications to filter on'
    waitFor { filters.applicationFilter.displayed }

    then: 'application filter is disabled with a tooltip on hover'
    filters.applicationFilter.hasClass("nx-tree-view--disabled")
  }

  def 'Should have only the "uncategorized applications" application category to choose from'() {
    setup: 'Logging in as a user without permission to any applications'
    loginAsUserVia(userWithoutPermission.username, userWithoutPermission.password, NewestRiskDashboardPage)

    when: 'looking at available application category filters'
    waitFor { filters.applicationCategoryFilter.displayed }
    filters.applicationCategoryFilter.nxTwisty.click()

    then: '"uncategorized applications" option and the "All" option should be the only options'
    filters.applicationCategoryFilter.nxMultiSelectList.size() == 2
    filters.applicationCategoryFilter.nxCounter.text() == '1'
    filters.applicationCategoryFilter.nxMultiSelectList.getAt(0).text() == ' all/none'
    filters.applicationCategoryFilter.nxMultiSelectList.getAt(1).text() == ' uncategorized applications'
  }
}
