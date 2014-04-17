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

    def firstPolicyEvaluation = temporaryEntity.newPolicyEvaluation(firstApp.id, BuildStageType.ID,
        'DashboardSpecFistEvaluation')
    def firstViolation = temporaryEntity.newPolicyViolation(firstPolicyEvaluation.id, policy, 5,
        PolicyThreatCategory.LICENSE, "Group1", "Artifact1", "Version1")
    temporaryEntity.newNewestPolicyViolation(firstViolation.id, firstPolicyEvaluation.applicationId,
        firstPolicyEvaluation.stageTypeId)

    def secondPolicyEvaluation = temporaryEntity.newPolicyEvaluation(secondApp.id, BuildStageType.ID,
        'DashboardSpecSecondEvaluation')
    temporaryEntity.newPolicyViolation(secondPolicyEvaluation.id, policy, 10,
        PolicyThreatCategory.QUALITY, null, null, null)
  }

  def setup() {
    loginAsAdminVia(DashboardPage)
  }

  def 'Dashboard Filters'() {
    when: 'clicking the filter toggle button'
      filterPanelToggle.click()

    then: 'the dashboard filters are shown'
      waitFor { applicationFiltersDropdown.displayed }
      policyThreatFiltersDropdown.displayed

    and: 'application filters are loaded'
      applicationFiltersDropdown.showDropdown()
      applicationFiltersDropdown.dropdownCheck(firstApp.name).displayed
      applicationFiltersDropdown.dropdownCheck(secondApp.name).displayed
      applicationFiltersDropdown.hideDropdown()

    and: 'policy threat category filters are shown'
      policyThreatFiltersDropdown.showDropdown()
      policyThreatFiltersDropdown.dropdownCheck('Security').displayed
      policyThreatFiltersDropdown.dropdownCheck('License').displayed
      policyThreatFiltersDropdown.dropdownCheck('Quality').displayed
      policyThreatFiltersDropdown.dropdownCheck('Other').displayed
      policyThreatFiltersDropdown.hideDropdown()
      
    and: 'stage type filters are shown'
      stageTypeFiltersDropdown.showDropdown()
      stageTypeFiltersDropdown.dropdownCheck('Develop').displayed
      stageTypeFiltersDropdown.dropdownCheck('Build').displayed
      stageTypeFiltersDropdown.dropdownCheck('Stage Release').displayed
      stageTypeFiltersDropdown.dropdownCheck('Release').displayed
      stageTypeFiltersDropdown.dropdownCheck('Operate').displayed
      stageTypeFiltersDropdown.hideDropdown()

    when: 'dashboard filters are applied'
      applicationFiltersDropdown.toggleOption(firstApp.name)
      applicationFiltersDropdown.toggleOption(secondApp.name)
      filterButtons.button('Apply').click()

    then: 'filters show up in readonly mode'
      waitFor { filterPanel.displayed }
      applicationFilters.collect{ it.text() }.join('') == firstApp.name + ',' + secondApp.name
  }

  def 'Risk Tables'() {
    when: 'highest risk table is shown'
      waitFor { highestRiskTable.displayed }

    then: 'risks are sorted by descending threat level'
      policyViolationRisk(highestRiskTable, 0).text() == '10'
      policyViolationRisk(highestRiskTable, 1).text() == '5'

    when: 'table is sorted by ascending threat level'
      threatLevelHeader.click()

    then: 'risks are sorted by ascending threat level'
      policyViolationRisk(highestRiskTable, 0).text() == '5'
      policyViolationRisk(highestRiskTable, 1).text() == '10'
  }

  def 'Highest Risk Table'() {
    when: 'highest risk table is shown'
      waitFor { highestRiskTable.displayed }

    then: 'policy violations are listed by threat level'
      !noDataAvailableHighest.displayed
      highestRiskTable.size() == 2
      policyViolationRisk(highestRiskTable, 0).text() == '10'
      policyViolationPolicy(highestRiskTable, 0).text() == 'DashboardSpecPolicy'
      policyViolationApplication(highestRiskTable, 0).text() == secondApp.name
      policyViolationComponent(highestRiskTable, 0).text() == 'Unknown'

      policyViolationRisk(highestRiskTable, 1).text() == '5'
      policyViolationPolicy(highestRiskTable, 1).text() == 'DashboardSpecPolicy'
      policyViolationApplication(highestRiskTable, 1).text() == firstApp.name
      policyViolationComponent(highestRiskTable, 1).text() == ["Group1", "Artifact1", "Version1"].join(' : ')

    when: 'filtering to an application'
      filterPanelToggle.click()
      waitFor { applicationFiltersDropdown.displayed }
      applicationFiltersDropdown.toggleOption(firstApp.name)
      filterButtons.button('Apply').click()
      waitFor { !applicationFiltersDropdown.displayed }

    then: 'only violations from that application are shown'
      waitFor { highestRiskTable.size() == 1 }
      policyViolationRisk(highestRiskTable, 0).text() == '5'
      policyViolationPolicy(highestRiskTable, 0).text() == 'DashboardSpecPolicy'
      policyViolationApplication(highestRiskTable, 0).text() == firstApp.name
  }

  def 'Newest Risk Table'() {
    when: 'newest risk table is shown'
      waitFor { newestViolationTable.displayed }

    then: 'policy violations are listed by threat level'
      !noDataAvailableNewest.displayed
      newestViolationTable.size() == 1
      policyViolationRisk(newestViolationTable, 0).text() == '5'
      policyViolationPolicy(newestViolationTable, 0).text() == 'DashboardSpecPolicy'
      policyViolationApplication(newestViolationTable, 0).text() == firstApp.name
      policyViolationComponent(newestViolationTable, 0).text() == ["Group1", "Artifact1", "Version1"].join(' : ')

    when: 'filtering to an application'
      filterPanelToggle.click()
      waitFor { applicationFiltersDropdown.displayed }
      applicationFiltersDropdown.toggleOption(secondApp.name)
      filterButtons.button('Apply').click()
      waitFor { !applicationFiltersDropdown.displayed }

    then: 'no violations are shown'
      waitFor { !newestViolationTable }
      noDataAvailableNewest.displayed
  }

  def 'Filter out all results'() {
    when: 'selecting filters that match no results'
      filterPanelToggle.click()
      waitFor { policyThreatFiltersDropdown.displayed }
      policyThreatFiltersDropdown.toggleOption('Security')
      policyThreatFiltersDropdown.toggleOption('Other')
      filterButtons.button('Apply').click()

    then: 'the tables are replaced by text indicating there are no results'
      waitFor { noDataAvailableHighest.displayed }
      noDataAvailableNewest.displayed
      !filterPanel.displayed
  }
}
