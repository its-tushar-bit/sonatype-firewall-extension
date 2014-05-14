/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
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
    extends BaseSpec
{
  static Organization org

  static Application firstApp

  static Application secondApp

  static User userWithPermission

  static User userWithoutPermission

  def setupSpec() {
    org = temporaryEntity.newOrganization('DashboardAuthzSpec')
    firstApp = temporaryEntity.newApplication('DashboardAuthzSpecAppOne', 'DashboardAuthzSpecAppOne', org.id)
    secondApp = temporaryEntity.newApplication('DashboardAuthzSpecAppTwo', 'DashboardAuthzSpecAppTwo', org.id)

    def policy = temporaryEntity.newPolicy(org.id, 'DashboardAuthzSpecPolicy')

    PolicyEvaluation firstPolicyEvaluation = temporaryEntity.
        newPolicyEvaluation(firstApp.id, BuildStageType.ID, 'DashboardAuthzSpecFistEvaluation')
    temporaryEntity.newPolicyViolation(firstPolicyEvaluation.id, firstPolicyEvaluation.time, policy, 5,
        PolicyThreatCategory.LICENSE, "Group1", "Artifact1", "Version1")
    PolicyEvaluation secondPolicyEvaluation = temporaryEntity.
        newPolicyEvaluation(secondApp.id, BuildStageType.ID, 'DashboardAuthzSpecSecondEvaluation')
    temporaryEntity.newPolicyViolation(secondPolicyEvaluation.id, secondPolicyEvaluation.time, policy, 10,
        PolicyThreatCategory.QUALITY, null, null, null)

    userWithPermission = temporaryEntity.newUser();
    Role role = temporaryEntity.newRole(false, Permission.READ);
    temporaryEntity.newMembershipMapping(firstApp.id, role.id, userWithPermission.username);

    userWithoutPermission = temporaryEntity.newUser()
  }

  def 'Should only see one of two available applications based on READ permission to one'() {
    setup: 'Logging in as a user permission to 1 of 2 applications'
      loginAsUserVia(userWithPermission.username, userWithPermission.password, DashboardOverviewPage)

    when: 'looking at available applications to filter on'
      filterPanelToggle.click()
      waitFor { applicationFiltersDropdown.displayed }
      applicationFiltersDropdown.showDropdown()

    then: 'only the permissioned application is shown'
      applicationFiltersDropdown.dropdownList.size() == 1
      !noAvailableApplications.displayed
  }

  def 'Should be advised that there are no applications to choose from without permissions'() {
    setup: 'Logging in as a user without permission to any applications'
      loginAsUserVia(userWithoutPermission.username, userWithoutPermission.password, DashboardOverviewPage)

    when: 'looking at available applications to filter on'
      filterPanelToggle.click()

    then: 'the select is not shown, and instead a message is presented'
      waitFor { noAvailableApplications.displayed }
      !applicationFiltersDropdown.displayed
  }

  def 'Should only see application tag dropdown when application tags exist'() {
    setup: 'Logging in as a user without permission to any applications'
      loginAsUserVia(userWithoutPermission.username, userWithoutPermission.password, DashboardOverviewPage)

    when: 'looking at available application tag filters'
      filterPanelToggle.click()

    then: 'the select is not shown, and instead a message is presented'
      waitFor { noAvailableApplicationTags.displayed }
      !applicationTagFiltersDropdown.displayed
  }
}
