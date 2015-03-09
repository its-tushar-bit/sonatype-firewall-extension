/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.Application
import com.sonatype.insight.brain.model.ApplicationComponent
import com.sonatype.insight.brain.model.Organization
import com.sonatype.insight.brain.model.policy.Policy
import com.sonatype.insight.brain.model.policy.PolicyEvaluation
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory
import com.sonatype.insight.brain.model.policy.PolicyViolation
import com.sonatype.insight.brain.model.policy.actions.FailActionType
import com.sonatype.insight.brain.model.policy.stages.BuildStageType
import com.sonatype.insight.brain.testing.functional.report.violation.ReportContainerPage

import spock.lang.Stepwise

/**
 * @since 1.11
 */
@Stepwise
class ComponentDrilldownSpec
extends BaseSpec {
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
        'DashboardSpecFirstEvaluation', daysAgo(now, 7))
    policyViolation = temporaryEntity.newPolicyViolation(policyEvaluation, policy, 5,
        PolicyThreatCategory.LICENSE, "Group1", "Artifact1", "Version1", "hash", FailActionType.ID)
    temporaryEntity.newFirstOccurrencePolicyViolation(policyViolation.id, policyEvaluation.applicationId,
        policyEvaluation.stageTypeId)
    applicationComponent = temporaryEntity.newApplicationComponent(app.id, policyEvaluation.stageTypeId,
        policyViolation.hash, ComponentIdentifier.createMavenCoordinates("Group1", "Artifact1", "Version1"))

    def newestRiskPage = loginAsAdminVia(NewestRiskDashboardPage)
    waitFor { newestRiskPage.newestViolationTable.rows.size() == 1 }
    newestRiskPage.newestViolationTable.rows[0].componentLink.click()
    at ComponentDrilldownPage
  }

  def 'Component Drilldown Breadcrumb'() {
    when: 'The dashboard overview is loaded'
    waitFor { breadcrumbs.size() == 2 }

    then: 'The dashboard breadcrumb is shown'
    crumb('dashboard.overview.newest-risk').displayed
    crumb('dashboard.overview.newest-risk').text().trim() == 'Dashboard'

    and: 'The component details link is shown as the last crumb'
    lastCrumb.displayed
    lastCrumb.text().trim() == 'Component Details'

    and: 'the desired component name is shown'
    waitFor { componentName.text() == 'Group1 : Artifact1 : Version1' }
  }

  def 'Component Drilldown Application Row'() {
    when: 'The component data is loaded'
    waitFor { componentApplicationRow(app.id).displayed }

    then: 'Stages are shown in the appropriate order'
    header(ComponentApplicationRow.BUILD) == 'BUILD'
    header(ComponentApplicationRow.STAGE) == 'STAGE'
    header(ComponentApplicationRow.RELEASE) == 'RELEASE'
    header(ComponentApplicationRow.OPERATE) == 'OPERATE'

    and: 'Application row displays component application data'
    ComponentApplicationRow applicationRow = componentApplicationRow(app.id)
    applicationRow.orgApp == org.name + " : " + app.name
    applicationRow.riskPie == '100%'
    applicationRow.riskCount == 5
    applicationRow.build == '7d'
    applicationRow.isFail(ComponentApplicationRow.BUILD)
  }

  def 'Component Drilldown Violation Row'() {
    when: 'The component application is expanded'
    waitFor { componentApplicationRow(app.id).displayed }
    componentApplicationRow(app.id).expando.click()

    then: 'Violation row show component violation data'
    waitFor { componentViolationTable(app.id).displayed }
    ComponentViolationRow violationRow = componentViolationRow(app.id, policy.name)
    violationRow.threatLevel == policyViolation.threatLevel
    violationRow.policyName == policy.name
    violationRow.riskPie == "100%"
    violationRow.riskCount == 5
    violationRow.build == '7d'
    violationRow.isFail(ComponentViolationRow.BUILD)
    violationRow.isLatestRisk(ComponentViolationRow.BUILD)
  }

  def 'Links to reports open in a new window'() {
    when: 'A user wants more information about the component in an application'
    ComponentApplicationRow row = componentApplicationRow(app.id)

    then: 'clicking the stage label takes us to the most recent report for that stage'
    withNewWindow(page: ReportContainerPage, { row.click(row.cell(ComponentApplicationRow.BUILD)) }) {
      verifyAt()
      reportTitle.text()
    } ==~ app.name + ' .* Build Report'

    and: 'the corresponding policy violation row label links to the same report'
    ComponentViolationRow violationRow = componentViolationRow(app.id, policy.name)
    withNewWindow(page: ReportContainerPage, { violationRow.click(violationRow.cell(ComponentViolationRow.BUILD)) }) {
      verifyAt()
      reportTitle.text()
    } ==~ app.name + ' .* Build Report'
  }
}
