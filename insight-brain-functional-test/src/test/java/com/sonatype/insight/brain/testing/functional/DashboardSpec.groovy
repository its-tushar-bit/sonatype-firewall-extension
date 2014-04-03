/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.model.Application
import com.sonatype.insight.brain.model.Organization
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory
import com.sonatype.insight.brain.model.policy.stages.BuildStageType

/**
 * Since 1.11
 */
class DashboardSpec
  extends BaseSpec
{
  static Organization org
  static Application firstApp
  static Application secondApp

  def setupSpec() {
    org = temporaryEntity.newOrganization('DashboardSpec')
    firstApp = temporaryEntity.newApplication('DashboardSpecAppOne', 'DashboardSpecAppOne', org.id)
    secondApp = temporaryEntity.newApplication('DashboardSpecAppTwo', 'DashboardSpecAppTwo', org.id)

    def policy = temporaryEntity.newPolicy(org.id, 'DashboardSpecPolicy')

    def firstPolicyEvaluation = temporaryEntity.newPolicyEvaluation(firstApp.id, BuildStageType.ID, 'DashboardSpecFistEvaluation')
    temporaryEntity.newPolicyViolation(firstPolicyEvaluation.id, policy, 5, PolicyThreatCategory.LICENSE, "Group1",
        "Artifact1", "Version1")
    def secondPolicyEvaluation = temporaryEntity.newPolicyEvaluation(secondApp.id, BuildStageType.ID, 'DashboardSpecSecondEvaluation')
    temporaryEntity.newPolicyViolation(secondPolicyEvaluation.id, policy, 10, PolicyThreatCategory.QUALITY, "Group2",
        "Artifact2", "Version2")
  }

  def setup() {
    loginAsAdminVia(DashboardPage)
  }

  def 'Dashboard Filters'() {
    when: 'application filters are shown'
      applicationFiltersDropdown.displayed

    then: 'application filters are loaded'
      applicationFiltersDropdown.showDropdown()
      applicationFiltersDropdown.dropdownCheck(firstApp.name).displayed
      applicationFiltersDropdown.dropdownCheck(secondApp.name).displayed
      applicationFiltersDropdown.hideDropdown()
  }

  def 'Highest Risk Table'() {
    when: 'highest risk table is shown'
      highestRiskTable.displayed

    then: 'policy violations are listed by threat level'
      highestRiskTable.size() == 2
      policyViolationRisk(0).text() == '10'
      policyViolationPolicy(0).text() == 'DashboardSpecPolicy'
      policyViolationApplication(0).text() == secondApp.name
      policyViolationRisk(1).text() == '5'
      policyViolationPolicy(1).text() == 'DashboardSpecPolicy'
      policyViolationApplication(1).text() == firstApp.name

    when: 'filtering to an application'
      applicationFiltersDropdown.toggleOption(firstApp.name)

    then: 'only violations from that application are shown'
      highestRiskTable.size() == 1
      policyViolationRisk(0).text() == '5'
      policyViolationPolicy(0).text() == 'DashboardSpecPolicy'
      policyViolationApplication(0).text() == firstApp.name
  }
}
