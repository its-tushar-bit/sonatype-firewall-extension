/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.model.Application
import com.sonatype.insight.brain.model.Color
import com.sonatype.insight.brain.model.Organization
import com.sonatype.insight.brain.model.policy.Policy
import com.sonatype.insight.brain.model.policy.PolicyEvaluation
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory
import com.sonatype.insight.brain.model.policy.PolicyViolation
import com.sonatype.insight.brain.model.policy.stages.BuildStageType
import com.sonatype.insight.brain.model.tag.Tag

/**
 * Since 1.11
 */
class DashboardSpec
  extends BaseSpec
{
  static final String RECENT_AGE = /.*(seconds|minute|minutes) ago/

  static Organization org
  static Application firstApp
  static Tag firstAppTag
  static Application secondApp

  def setupSpec() {
    org = temporaryEntity.newOrganization('DashboardSpec')
    firstApp = temporaryEntity.newApplication('DashboardSpecAppOne', 'DashboardSpecAppOne', org.id)
    firstAppTag = temporaryEntity.newTag(org.id, 'DashboardSpecAppOneTag', Color.blue)
    temporaryEntity.newApplicationTag(firstApp.id, firstAppTag.id)

    secondApp = temporaryEntity.newApplication('DashboardSpecAppTwo', 'DashboardSpecAppTwo', org.id)

    Policy policy = temporaryEntity.newPolicy(org.id, 'DashboardSpecPolicy')

    Date now = new Date()
    PolicyEvaluation firstPolicyEvaluation = temporaryEntity.newPolicyEvaluation(firstApp.id, BuildStageType.ID,
        'DashboardSpecFistEvaluation', now - 7)
    PolicyViolation firstViolation = temporaryEntity.newPolicyViolation(firstPolicyEvaluation.id, policy, 5,
        PolicyThreatCategory.LICENSE, "Group1", "Artifact1", "Version1")
    temporaryEntity.newNewestPolicyViolation(firstViolation.id, firstPolicyEvaluation.applicationId,
        firstPolicyEvaluation.stageTypeId)

    PolicyEvaluation secondPolicyEvaluation = temporaryEntity.newPolicyEvaluation(secondApp.id, BuildStageType.ID,
        'DashboardSpecSecondEvaluation', now)
    PolicyViolation secondViolation = temporaryEntity.newPolicyViolation(secondPolicyEvaluation.id, policy, 10,
        PolicyThreatCategory.QUALITY, null, null, null)
    temporaryEntity.newNewestPolicyViolation(secondViolation.id, secondPolicyEvaluation.applicationId,
        secondPolicyEvaluation.stageTypeId)
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
      //note only checking for items available from every product type
      stageTypeFiltersDropdown.dropdownCheck('Release').displayed
      stageTypeFiltersDropdown.hideDropdown()

    and: 'application tag filters are shown'
      applicationTagFiltersDropdown.showDropdown()
      applicationTagFiltersDropdown.dropdownCheck(firstAppTag.name).displayed
      applicationTagFiltersDropdown.dropdownOwner(firstAppTag.name).text() == 'in ' + org.name
      applicationTagFiltersDropdown.areOptionsColored([(firstAppTag.name): "blue"])
      applicationTagFiltersDropdown.hideDropdown()

    when: 'dashboard filters are applied'
      applicationFiltersDropdown.toggleOption(firstApp.name)
      applicationFiltersDropdown.toggleOption(secondApp.name)
      applicationTagFiltersDropdown.toggleOption(firstAppTag.name)
      filterButtons.button('Apply').click()

    then: 'filters show up in readonly mode'
      waitFor { filterPanel.displayed }
      applicationFilters.collect{it.text()}.join('') == firstApp.name + ',' + secondApp.name
      applicationTagFilters.text() == firstAppTag.name
  }

  def 'Highest Risk Table can be sorted'() {
    when: 'highest risk table is shown'
      waitFor { highestRiskTable.displayed }

    then: 'risks are sorted by descending threat level'
      highestRiskTable.rows[0].risk == 10
      highestRiskTable.rows[1].risk == 5

    when: 'table is sorted by ascending threat level'
      highestRiskTable.riskHeader.click()

    then: 'risks are sorted by ascending threat level'
      highestRiskTable.rows[0].risk == 5
      highestRiskTable.rows[1].risk == 10
  }

  def 'Newest Risk table can be sorted by age'() {
    when: 'the newest risk table is shown'
      waitFor{ newestViolationTable.displayed }

    then: 'risks are sorted by descending threat level, with the most recent results shown first'
      newestViolationTable.rows[0].risk == 10
      newestViolationTable.rows[0].age ==~ RECENT_AGE
      newestViolationTable.rows[1].risk == 5
      newestViolationTable.rows[1].age.endsWith('days ago')

    when: 'clicking the AGE header'
      newestViolationTable.ageHeader.click()

    then: 'we should now show the oldest result first'
      newestViolationTable.rows[0].age.endsWith('days ago')
      newestViolationTable.rows[1].age ==~ RECENT_AGE
  }

  def 'Highest Risk Table can be filtered'() {
    when: 'highest risk table is shown'
      waitFor { highestRiskTable.displayed }

    then: 'policy violations are listed by threat level'
      !noDataAvailableHighest.displayed
      highestRiskTable.rows.size() == 2

      highestRiskTable.rows[0].risk == 10
      highestRiskTable.rows[0].policy == 'DashboardSpecPolicy'
      highestRiskTable.rows[0].application == secondApp.name
      highestRiskTable.rows[0].component == 'Unknown'

      highestRiskTable.rows[1].risk == 5
      highestRiskTable.rows[1].policy == 'DashboardSpecPolicy'
      highestRiskTable.rows[1].application == firstApp.name
      highestRiskTable.rows[1].component == ["Group1", "Artifact1", "Version1"].join(' : ')
      highestRiskTable.rows[1].age == null

    when: 'filtering to an application'
      filterPanelToggle.click()
      waitFor { applicationFiltersDropdown.displayed }
      applicationFiltersDropdown.toggleOption(firstApp.name)
      filterButtons.button('Apply').click()

    then: 'only violations from that application are shown'
      waitFor { highestRiskTable.rows.size() == 1 }
      !applicationFiltersDropdown.displayed
      highestRiskTable.rows[0].risk == 5
      highestRiskTable.rows[0].policy == 'DashboardSpecPolicy'
      highestRiskTable.rows[0].application == firstApp.name
  }

  def 'Newest Risk Table can be filtered'() {
    when: 'newest risk table is shown'
      waitFor { newestViolationTable.displayed }

    then: 'policy violations are listed by threat level'
      !noDataAvailableNewest.displayed
      newestViolationTable.rows.size() == 2

      newestViolationTable.rows[0].risk == 10
      newestViolationTable.rows[0].policy == 'DashboardSpecPolicy'
      newestViolationTable.rows[0].application == secondApp.name
      newestViolationTable.rows[0].component == 'Unknown'

      newestViolationTable.rows[1].risk == 5
      newestViolationTable.rows[1].policy == 'DashboardSpecPolicy'
      newestViolationTable.rows[1].application == firstApp.name
      newestViolationTable.rows[1].component == ["Group1", "Artifact1", "Version1"].join(' : ')
      newestViolationTable.rows[1].age.contains("ago")
      
    when: 'filtering to an application'
      filterPanelToggle.click()
      waitFor { applicationFiltersDropdown.displayed }
      applicationFiltersDropdown.toggleOption(secondApp.name)
      filterButtons.button('Apply').click()

    then: 'no violations are shown'
      waitFor { newestViolationTable.rows.size() == 1 }
      !applicationFiltersDropdown.displayed
      newestViolationTable.rows[0].risk == 10
      newestViolationTable.rows[0].policy == 'DashboardSpecPolicy'
      newestViolationTable.rows[0].application == secondApp.name
      newestViolationTable.rows[0].component == 'Unknown'
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
      filterPanel.displayed
  }
}
