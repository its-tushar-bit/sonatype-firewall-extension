/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.model.Application
import com.sonatype.insight.brain.model.ApplicationComponent
import com.sonatype.insight.brain.model.Organization
import com.sonatype.insight.brain.model.policy.Policy
import com.sonatype.insight.brain.model.policy.PolicyEvaluation
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory
import com.sonatype.insight.brain.model.policy.PolicyViolation
import com.sonatype.insight.brain.model.policy.stages.BuildStageType
import spock.lang.Stepwise

/**
 * @since 1.11
 */
@Stepwise
class ComponentDrilldownSpec
  extends BaseSpec
{
  static Organization org
  static Application app
  static Policy policy
  static PolicyViolation policyViolation
  static ApplicationComponent applicationComponent

  def setupSpec() {
    org = temporaryEntity.newOrganization('DashboardSpec')
    app = temporaryEntity.newApplication('DashboardSpecAppOne', 'DashboardSpecAppOne', org.id)

    policy = temporaryEntity.newPolicy(org.id, 'DashboardSpecPolicy')

    Date now = new Date()
    PolicyEvaluation policyEvaluation = temporaryEntity.newPolicyEvaluation(app.id, BuildStageType.ID,
        'DashboardSpecFistEvaluation', now - 7)
    policyViolation = temporaryEntity.newPolicyViolation(policyEvaluation.id, policy, 5,
        PolicyThreatCategory.LICENSE, "Group1", "Artifact1", "Version1")
    temporaryEntity.newNewestPolicyViolation(policyViolation.id, policyEvaluation.applicationId,
        policyEvaluation.stageTypeId)
    applicationComponent = temporaryEntity.newApplicationComponent(app.id, policyEvaluation.stageTypeId,
        policyViolation.hash, policyViolation.groupId, policyViolation.artifactId, policyViolation.version)

    loginAsAdminVia(DashboardOverviewPage)
    waitFor { highestRiskTable.rows.size() == 1 }
    highestRiskTable.rows[0].componentLink.click()
    at ComponentDrilldownPage
  }

  def 'Component Drilldown Breadcrumb'() {
    when: 'The dashboard overview is loaded'
      waitFor { breadcrumbs.size() == 2 }
    then: 'Only the dashboard breadcrumb is shown'
      crumb('dashboard.overview').displayed
      crumb('dashboard.overview').text().trim() == 'Dashboard'
      crumb('dashboard.component').displayed
      crumb('dashboard.component').text().trim() == 'Component Details'
  }

  def 'Component Drilldown Application Row'() {
    when: 'The component data is loaded'
      waitFor { componentApplicationRow(app.id).displayed }

    then: 'Application row displays component application data'
      ComponentApplicationRow applicationRow = componentApplicationRow(app.id)
      applicationRow.orgApp == org.name + " : " + app.name
      applicationRow.riskPie == '100%'
      applicationRow.riskCount == 5
  }

  def 'Component Drilldown Violation Row'() {
    when: 'The component application is expanded'
      waitFor { componentApplicationRow(app.id).displayed }
      componentApplicationRow(app.id).click()

    then: 'Violation row show component violation data'
      waitFor { componentViolationTable(app.id).displayed }
      ComponentViolationRow violationRow = componentViolationRow(app.id, policy.name)
      violationRow.threatLevel == policyViolation.threatLevel
      violationRow.policyName == policy.name
      violationRow.riskPie == "100%"
      violationRow.riskCount == 5
  }
}
