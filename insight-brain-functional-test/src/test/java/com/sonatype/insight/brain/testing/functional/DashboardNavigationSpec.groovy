/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.model.policy.PolicyEvaluation
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory
import com.sonatype.insight.brain.model.policy.actions.FailActionType
import com.sonatype.insight.brain.model.policy.stages.BuildStageType

import spock.lang.Unroll

/**
 * @since 1.11
 */
class DashboardNavigationSpec
    extends BaseSpec
{

  def setupSpec() {
    setup: 'logging in as admin to newest risk table'
      def org = temporaryEntity.newOrganization('DashboardNavigationSpec')
      def app = temporaryEntity.newApplication('DashboardNavigationSpecAppOne', 'DashboardNavigationSpecAppOne', org.id)

      def policy = temporaryEntity.newPolicy(org.id, 'DashboardNavigationSpecPolicy')

      Date now = new Date()
      PolicyEvaluation policyEvaluation = temporaryEntity.newPolicyEvaluation(app.id, BuildStageType.ID,
          'DashboardSpecFirstEvaluation', now - 7)
      def policyViolation = temporaryEntity.newPolicyViolation(policyEvaluation, policy, 5,
          PolicyThreatCategory.LICENSE, "Group1", "Artifact1", "Version1", "hash", FailActionType.ID)
      temporaryEntity.newFirstOccurrencePolicyViolation(policyViolation.id, policyEvaluation.applicationId,
          policyEvaluation.stageTypeId)
      temporaryEntity.newApplicationComponent(app.id, policyEvaluation.stageTypeId,
          policyViolation.hash, policyViolation.groupId, policyViolation.artifactId, policyViolation.version)
  }

  @Unroll
  def "Can navigate directly to #table with url"() {
    when:
      loginAsAdminVia(table)

    then:
      at table

    where:
      table << [NewestRiskDashboardPage, ComponentViolationsDashboardPage, ApplicationViolationsDashboardPage]
  }

  def "Back button will always take us back to the previous dashboard page"() {
    setup: 'logging in as admin and click through the pages'
      loginAsAdminVia(NewestRiskDashboardPage)
      to ComponentViolationsDashboardPage
      to ApplicationViolationsDashboardPage

    when: 'back button press'
      driver.navigate().back()

    then: 'puts us at the component violation page'
      waitFor { at(ComponentViolationsDashboardPage) }

    when: 'back button again'
      driver.navigate().back()

    then: 'will take us back to newest risk page'
      waitFor { at(NewestRiskDashboardPage) }
  }

  @Unroll
  def 'Dashboard link will link to previously viewed dashboard table (#test.state)'() {
    setup: 'logging in as admin'
      loginAsAdminVia(NewestRiskDashboardPage)

    when: 'navigating to components page from a dashboard table'
      to test.table
      test.componentRow().click()
      at ComponentDrilldownPage

    then: 'dashboard crumb links to previous dashboard table'
      crumb(test.state).text().trim() == "Dashboard"

    where:
      test << [[
                   state: 'dashboard.overview.newest-risk',
                   table: NewestRiskDashboardPage,
                   componentRow: { newestViolationTable.rows[0].componentLink }
               ], [
                   state: 'dashboard.overview.components',
                   table: ComponentViolationsDashboardPage,
                   componentRow: { componentViolationsTable.rows[0].componentLink }
               ]]
  }
}
