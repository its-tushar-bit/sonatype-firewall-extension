/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage
import com.sonatype.insight.brain.dashboard.DashboardFilterDTO
import com.sonatype.insight.brain.dataaccess.ApplicationDAO
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO
import com.sonatype.insight.brain.model.Application
import com.sonatype.insight.brain.model.Color
import com.sonatype.insight.brain.model.Organization
import com.sonatype.insight.brain.model.component.MatchState
import com.sonatype.insight.brain.model.policy.Policy
import com.sonatype.insight.brain.model.policy.PolicyEvaluation
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory
import com.sonatype.insight.brain.model.policy.PolicyViolation
import com.sonatype.insight.brain.model.policy.actions.FailActionType
import com.sonatype.insight.brain.model.policy.actions.WarnActionType
import com.sonatype.insight.brain.model.policy.stages.BuildStageType
import com.sonatype.insight.brain.model.policy.stages.OperateStageType
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType
import com.sonatype.insight.brain.model.policy.stages.StageReleaseStageType
import com.sonatype.insight.brain.model.tag.Tag
import com.sonatype.insight.brain.service.InsightWork
import com.sonatype.insight.brain.testing.functional.modules.ThreatTableRow
import com.sonatype.insight.brain.testing.functional.report.violation.ReportContainerPage

import com.fasterxml.jackson.databind.ObjectMapper
import org.codehaus.plexus.util.FileUtils
import static spock.util.matcher.HamcrestMatchers.closeTo
import static spock.util.matcher.HamcrestSupport.that

/**
 * @since 1.11
 */
class DashboardOverviewSpec
extends BaseSpec {
  static final String RECENT_AGE = /[1-9]min/

  static
  final String alphaMatcher = /background-color: rgba\([0-9][0-9][0-9]?, [0-9][0-9][0-9]?, [0-9][0-9][0-9]?, (.*)\);/

  // accept differential for precision of alpha results
  static final BigDecimal TOLERANCE = 0.05

  static final ComponentIdentifier DEFAULT_COMPONENT_IDENTIFIER = ComponentIdentifier.createMavenCoordinates("Group1", "Artifact1", "Version1");

  static final String DEFAULT_COMPONENT = [
    "Group1",
    "Artifact1",
    "Version1"
  ].join(' : ')

  static Organization org

  static Application firstApp

  static Tag firstAppTag

  static Application secondApp

  static Policy policy

  def setupSpec() {
    org = temporaryEntity.newOrganization('DashboardSpec')
    firstApp = temporaryEntity.newApplication('DashboardSpecAppOne', 'DashboardSpecAppOne', org.id)
    firstAppTag = temporaryEntity.newTag(org.id, 'DashboardSpecAppOneTag', Color.blue)
    temporaryEntity.newApplicationTag(firstApp.id, firstAppTag.id)

    secondApp = temporaryEntity.newApplication('DashboardSpecAppTwo', 'DashboardSpecAppTwo', org.id)

    policy = temporaryEntity.newPolicy(org.id, 'DashboardSpecPolicy')

    Date now = new Date()

    //first evaluation dated a week ago
    PolicyEvaluation firstPolicyEvaluation = temporaryEntity.newPolicyEvaluation(firstApp.id, BuildStageType.ID,
        'DashboardSpecFirstEvaluation', now - 7)
    PolicyViolation firstViolation = temporaryEntity.
        newPolicyViolation(firstPolicyEvaluation, policy, 5,
        PolicyThreatCategory.LICENSE, "Group1", "Artifact1", "Version1", "hash", FailActionType.ID)
    temporaryEntity.newFirstOccurrencePolicyViolation(firstViolation.id, firstPolicyEvaluation.applicationId,
        firstPolicyEvaluation.stageTypeId)
    temporaryEntity.newApplicationComponent(firstPolicyEvaluation.applicationId, firstPolicyEvaluation.stageTypeId,
        firstViolation.hash, DEFAULT_COMPONENT_IDENTIFIER)
    temporaryEntity.
        newApplicationComponent(firstPolicyEvaluation.applicationId, firstPolicyEvaluation.stageTypeId, '987654321',
        MatchState.SIMILAR, false);
    temporaryEntity.
        newApplicationComponent(firstPolicyEvaluation.applicationId, firstPolicyEvaluation.stageTypeId, '987654322',
        MatchState.UNKNOWN, false);

    //same policy as first evaluation, but a different stage and earlier
    PolicyEvaluation firstPolicyEvaluationSecondStage = temporaryEntity.newPolicyEvaluation(firstApp.id,
        StageReleaseStageType.ID, 'DashboardSpecFirstEvaluationSecondStage', now - 14)
    PolicyViolation firstViolationSecondStage = temporaryEntity.newPolicyViolation(firstPolicyEvaluationSecondStage,
        policy, 5, PolicyThreatCategory.LICENSE, "Group1", "Artifact1", "Version1", "hash", WarnActionType.ID)
    temporaryEntity.newFirstOccurrencePolicyViolation(firstViolationSecondStage.id,
        firstPolicyEvaluationSecondStage.applicationId, firstPolicyEvaluationSecondStage.stageTypeId)
    temporaryEntity.newApplicationComponent(firstPolicyEvaluationSecondStage.applicationId,
        firstPolicyEvaluationSecondStage.stageTypeId, firstViolationSecondStage.hash,
        DEFAULT_COMPONENT_IDENTIFIER)

    // evaluation in yet another stage
    PolicyEvaluation thirdPolicyEvaluation = temporaryEntity.newPolicyEvaluation(firstApp.id, ReleaseStageType.ID,
        'DashboardSpecThirdEvaluation', now - 8)
    PolicyViolation thirdViolation = temporaryEntity.
        newPolicyViolation(thirdPolicyEvaluation, policy, 2, PolicyThreatCategory.QUALITY,
        "Group1", "Artifact1", "Version1")
    temporaryEntity.newFirstOccurrencePolicyViolation(thirdViolation.id, thirdPolicyEvaluation.applicationId,
        thirdPolicyEvaluation.stageTypeId)

    // and one more stage to cover them all
    PolicyEvaluation forthPolicyEvaluation = temporaryEntity.newPolicyEvaluation(firstApp.id, OperateStageType.ID,
        'DashboardSpecForthEvaluation', now - 9)
    PolicyViolation forthViolation = temporaryEntity.
        newPolicyViolation(forthPolicyEvaluation, policy, 1, PolicyThreatCategory.OTHER,
        "Group1", "Artifact1", "Version1")
    temporaryEntity.newFirstOccurrencePolicyViolation(forthViolation.id, forthPolicyEvaluation.applicationId,
        forthPolicyEvaluation.stageTypeId)

    //most recent evaluation
    PolicyEvaluation secondPolicyEvaluation = temporaryEntity.newPolicyEvaluation(secondApp.id, ReleaseStageType.ID,
        'DashboardSpecSecondEvaluation', now)
    PolicyViolation secondViolation = temporaryEntity.newPolicyViolation(secondPolicyEvaluation, policy, 10,
        PolicyThreatCategory.QUALITY)
    temporaryEntity.newFirstOccurrencePolicyViolation(secondViolation.id, secondPolicyEvaluation.applicationId,
        secondPolicyEvaluation.stageTypeId)

    InsightWork work = new InsightWork(serviceRule.configuration)
    File reportZip = work.getReportFile(firstPolicyEvaluation.getApplicationId(), firstPolicyEvaluation.getScanId())
    FileUtils.copyURLToFile(getClass().getResource('/canned-reports/small-report.zip'), reportZip)

    loginAsAdminVia(DashboardOverviewPage)
  }

  def setup() {
    // Do not clear cookies so we don't have to log back in after every feature test.
    browser.config.autoClearCookies = false
    to NewestRiskDashboardPage
    // Refresh the page to clear locally cached filter.
    driver.navigate().refresh()
    waitFor { at DashboardOverviewPage }
  }

  def cleanup() {
    clearFilter()
  }

  private void clearFilter() {
    DashboardFilterDAO dao = new DashboardFilterDAO();
    dao.delete(dao.getByUsername("admin"));
  }

  def 'Dashboard Overview Breadcrumb'() {
    when: 'The dashboard overview is loaded'
    waitFor { breadcrumbs.size() == 2 }
    then: 'The dashboard link is shown'
    crumb('dashboard.overview.newest-risk').text().trim() == "Dashboard"
    crumb('dashboard.overview.newest-risk').@href.contains("/dashboard/newest-risk")
    and: 'The newest risk link is shown as the last crumb'
    lastCrumb.text().trim() == "Newest Risk"

  }

  def 'Dashboard Filters'() {
    when: 'clicking the filter toggle button'
    def newestRiskPage = at NewestRiskDashboardPage
    newestRiskPage.filters.toggle.click()

    then: 'the dashboard filters are shown'
    waitFor { newestRiskPage.filters.applicationMultiselect.displayed }
    newestRiskPage.filters.policyTypeMultiselect.displayed

    and: 'application filters are loaded'
    newestRiskPage.filters.applicationMultiselect.showDropdown()
    newestRiskPage.filters.applicationMultiselect.dropdownCheck(firstApp.name).displayed
    newestRiskPage.filters.applicationMultiselect.dropdownCheck(secondApp.name).displayed
    newestRiskPage.filters.applicationMultiselect.hideDropdown()

    and: 'policy threat category filters are shown'
    newestRiskPage.filters.policyTypeMultiselect.showDropdown()
    newestRiskPage.filters.policyTypeMultiselect.dropdownCheck('Security').displayed
    newestRiskPage.filters.policyTypeMultiselect.dropdownCheck('License').displayed
    newestRiskPage.filters.policyTypeMultiselect.dropdownCheck('Quality').displayed
    newestRiskPage.filters.policyTypeMultiselect.dropdownCheck('Other').displayed
    newestRiskPage.filters.policyTypeMultiselect.hideDropdown()

    and: 'stage type filters are shown in proper chronological order'
    newestRiskPage.filters.stageTypeMultiselect.showDropdown()
    newestRiskPage.filters.stageTypeMultiselect.dropdownName(0).displayed
    newestRiskPage.filters.stageTypeMultiselect.dropdownName(0).text() == 'Build'
    newestRiskPage.filters.stageTypeMultiselect.dropdownName(1).displayed
    newestRiskPage.filters.stageTypeMultiselect.dropdownName(1).text() == 'Stage Release'
    newestRiskPage.filters.stageTypeMultiselect.dropdownName(2).displayed
    newestRiskPage.filters.stageTypeMultiselect.dropdownName(2).text() == 'Release'
    newestRiskPage.filters.stageTypeMultiselect.dropdownName(3).displayed
    newestRiskPage.filters.stageTypeMultiselect.dropdownName(3).text() == 'Operate'
    !newestRiskPage.filters.stageTypeMultiselect.dropdownName(4).present
    newestRiskPage.filters.stageTypeMultiselect.hideDropdown()

    and: 'application tag filters are shown'
    newestRiskPage.filters.applicationTagMultiselect.showDropdown()
    newestRiskPage.filters.applicationTagMultiselect.dropdownCheck(firstAppTag.name).displayed
    newestRiskPage.filters.applicationTagMultiselect.dropdownOwner(firstAppTag.name).text() == 'in ' + org.name
    newestRiskPage.filters.applicationTagMultiselect.areOptionsColored([(firstAppTag.name): "blue"])
    newestRiskPage.filters.applicationTagMultiselect.hideDropdown()

    and: 'policy threat level filter is shown'
    newestRiskPage.filters.policyThreatLevelSlider.slider.displayed

    when: 'dashboard filters are applied'
    newestRiskPage.filters.applicationMultiselect.toggleOption(firstApp.name)
    newestRiskPage.filters.applicationMultiselect.toggleOption(secondApp.name)
    newestRiskPage.filters.applicationTagMultiselect.toggleOption(firstAppTag.name)
    newestRiskPage.filters.stageTypeMultiselect.toggleOption('Release')
    newestRiskPage.filters.policyTypeMultiselect.toggleOption('Security')
    newestRiskPage.filters.policyThreatLevelSlider.setValues(2, 7)
    newestRiskPage.filters.apply()

    then: 'filters show up in readonly mode'
    waitFor { newestRiskPage.filters.applicationSummary.displayed }
    newestRiskPage.filters.applicationSummary.getTooltipContent() == firstApp.name + '\n' + secondApp.name
    newestRiskPage.filters.applicationTagSummary.getTooltipContent() == firstAppTag.name
    newestRiskPage.filters.stageTypeSummary.getTooltipContent() == 'Release'
    newestRiskPage.filters.policyTypeSummary.getTooltipContent() == 'Security'
    newestRiskPage.filters.policyThreatLevelSummary.getTooltipContent() == 'Policy threat levels 2 through 7'
  }

  def 'Filter reset'() {
    when: 'clicking the filter toggle button'
    filters.toggle.click()

    and: 'Set some filters'
    waitFor { filters.applicationMultiselect.displayed }
    filters.applicationMultiselect.toggleOption(firstApp.name)
    filters.applicationTagMultiselect.toggleOption(firstAppTag.name)
    filters.stageTypeMultiselect.toggleOption('Release')
    filters.policyTypeMultiselect.toggleOption('Security')
    filters.policyThreatLevelSlider.setValues(2, 7)

    and: 'reset the filter'
    filters.resetButton.click()

    then: 'filters are empty'
    filters.applicationMultiselect.isEmpty()
    filters.applicationTagMultiselect.isEmpty()
    filters.stageTypeMultiselect.isEmpty()
    filters.policyTypeMultiselect.isEmpty()
    filters.policyThreatLevelSlider.minValue.text() == '2'
    filters.policyThreatLevelSlider.maxValue.text() == '10'
  }

  def 'Single value threat level slider filter'() {
    when: 'clicking the filter toggle button'
    filters.toggle.click()

    then: 'the policy threat level slider is shown'
    waitFor { filters.policyThreatLevelSlider.slider.displayed }

    when: 'threat level filter is applied'
    filters.policyThreatLevelSlider.setValues(4, 4)
    filters.apply()

    then: 'filter text shows one value'
    waitFor { filters.policyThreatLevelSummary.getTooltipContent() == 'Policy threat level 4' }
  }

  def 'Collapse filters presents apply dialog when necessary'() {
    when: 'clicking the filter toggle button'
    filters.toggle.click()

    and: 'Change some data'
    waitFor { filters.applicationMultiselect.displayed }
    filters.applicationMultiselect.toggleOption(firstApp.name)

    and: 'Collapse the panel'
    filters.toggle.click()

    then: 'The apply filter dialog is shown'
    waitFor { applyFilterModal.displayed }

    when: 'User clicks cancel'
    applyFilterModal.cancel.click()

    then: 'The panel is closed'
    waitFor { filters.applicationSummary.displayed }

    and: 'no new applications are added'
    filters.applicationSummary.getTooltipContent() == 'All applications'

    when: 'clicking the filter toggle button'
    filters.toggle.click()

    and: 'Change some data'
    waitFor { filters.applicationMultiselect.displayed }
    filters.applicationMultiselect.toggleOption(firstApp.name)

    and: 'Collapse the panel'
    filters.toggle.click()

    then: 'The apply filter dialog is shown'
    waitFor { applyFilterModal.displayed }

    when: 'User clicks apply'
    applyFilterModal.applyButton.click()

    then: 'The panel is closed'
    waitFor { filters.applicationSummary.displayed }

    and: 'the new application is added'
    filters.applicationSummary.getTooltipContent() == firstApp.name
  }

  def 'Unknown components have popover displaying pathnames'() {
    when: 'newest risk table is shown'
    tabLinks.newestRiskTabButton.click()
    def newestRiskPage = at NewestRiskDashboardPage
    waitFor { newestRiskPage.newestViolationTable.displayed }
    waitFor { newestRiskPage.newestViolationTable.rows.size() >= 2 }
    def unknownComponentCell =
        $(newestRiskPage.newestViolationTable.rows[0].cell(ThreatTableRow.COMPONENT)).firstElement()
    interact { moveToElement(unknownComponentCell) }
    waitFor { newestRiskPage.unknownComponentPopover.displayed }

    then: 'newest risk popover is properly displayed'
    newestRiskPage.unknownComponentPopoverTitle == 'Component Path'
    newestRiskPage.unknownComponentPopoverText == 'unknown.jar'
  }

  def 'Newest Risk table can be sorted by age'() {
    when: 'the newest risk table is shown'
    def newestRiskPage = at NewestRiskDashboardPage
    waitFor { newestRiskPage.newestViolationTable.displayed }
    waitFor { newestRiskPage.newestViolationTable.rows.size() >= 2 }

    then: 'risks are sorted by ascending age(most recent first), and then by threat level'
    ThreatTableRow rowOne = newestRiskPage.newestViolationTable.rows[0]
    ThreatTableRow rowTwo = newestRiskPage.newestViolationTable.rows[1]
    rowOne.threat == 10
    newestViolationTable.rows[0].age ==~ RECENT_AGE
    newestViolationTable.rows[1].age == '7d'
    newestViolationTable.rows[2].age == '8d'

    and: 'the sort is by default indicated as ascending for age'
    newestViolationTable.isUp(newestViolationTable.ageHeader)
    !newestViolationTable.isDown(newestViolationTable.ageHeader)

    and: 'only one stage is populated for the first result'
    !rowOne.buildAge
    !rowOne.operateAge
    rowOne.releaseAge ==~ RECENT_AGE
    rowOne.releaseAge == rowOne.age
    !rowOne.stageReleaseAge
    rowOne.isLatestRisk(ReleaseStageType.ID)

    and: 'none of the stages are marked warn/fail for the first result'
    !rowOne.isMarkedAsWarn(BuildStageType.ID)
    !rowOne.isMarkedAsWarn(StageReleaseStageType.ID)
    !rowOne.isMarkedAsWarn(ReleaseStageType.ID)
    !rowOne.isMarkedAsWarn(OperateStageType.ID)

    and: 'two stages are populated for the second result'
    rowTwo.threat == 5
    rowTwo.buildAge == '7d'
    rowTwo.buildAge == rowTwo.age
    rowTwo.stageReleaseAge == '14d'

    and: 'the build stage is marked as fail'
    rowTwo.isMarkedAsFail(BuildStageType.ID)

    and: 'the build stage is marked as most recent'
    rowTwo.isLatestRisk(BuildStageType.ID)

    and: 'the stage release stage is marked as warn'
    rowTwo.isMarkedAsWarn(StageReleaseStageType.ID)

    when: 'clicking the AGE header'
    newestRiskPage.newestViolationTable.ageHeader.click()

    then: 'we should now show the oldest result first'
    waitFor { newestViolationTable.rows[0].age == '8d' }
    newestRiskPage.newestViolationTable.rows[1].age == '7d'
    newestRiskPage.newestViolationTable.rows[2].age ==~ RECENT_AGE

    and: 'the sort is indicated as descending for age'
    newestViolationTable.isDown(newestViolationTable.ageHeader)
    !newestViolationTable.isUp(newestViolationTable.ageHeader)
  }

  def 'Newest Risk table can be sorted by stage time'() {
    when: 'the newest risk table is shown'
    def newestRiskPage = at NewestRiskDashboardPage
    waitFor { newestRiskPage.newestViolationTable.displayed }
    waitFor { newestRiskPage.newestViolationTable.rows.size() == 3 }

    then: 'the first row has no build stage results'
    newestRiskPage.newestViolationTable.rows[0].age ==~ RECENT_AGE
    !newestRiskPage.newestViolationTable.rows[0].buildAge

    when: 'clicking on the first stage header(BUILD in this case)'
    newestRiskPage.newestViolationTable.clickStageHeader(newestViolationTable.buildHeader)

    then: 'we should now show the most recent stage result first, followed by the empty results'
    waitFor { newestRiskPage.newestViolationTable.rows[0].buildAge == '7d' }
    !newestRiskPage.newestViolationTable.rows[1].buildAge
    !newestRiskPage.newestViolationTable.rows[2].buildAge

    when: 'clicking on the second stage header(STAGE)'
    newestRiskPage.newestViolationTable.clickStageHeader(newestRiskPage.newestViolationTable.stageHeader)

    then: 'we should sort the only result in this column to the top'
    waitFor { newestRiskPage.newestViolationTable.rows[0].stageReleaseAge == '14d' }
    !newestRiskPage.newestViolationTable.rows[1].stageReleaseAge
    !newestRiskPage.newestViolationTable.rows[2].stageReleaseAge

    when: 'clicking on the third stage header(RELEASE)'
    newestRiskPage.newestViolationTable.clickStageHeader(newestViolationTable.releaseHeader)

    then: 'we should sort the two results in this column to the top, ordered with most recent first'
    waitFor { newestRiskPage.newestViolationTable.rows[0].releaseAge ==~ RECENT_AGE }
    newestRiskPage.newestViolationTable.rows[1].releaseAge == '8d'
    !newestRiskPage.newestViolationTable.rows[2].releaseAge

    and: 'the sort is indicated as ascending for the RELEASE age'
    newestViolationTable.isUp(newestViolationTable.releaseHeader)
    !newestViolationTable.isDown(newestViolationTable.releaseHeader)

    when: 'clicking the third stage header again'
    newestRiskPage.newestViolationTable.clickStageHeader(newestViolationTable.releaseHeader)

    then: 'the results should be sorted in reverse, with empty values at the end'
    waitFor { newestRiskPage.newestViolationTable.rows[0].releaseAge == '8d' }
    newestRiskPage.newestViolationTable.rows[1].releaseAge ==~ RECENT_AGE
    !newestRiskPage.newestViolationTable.rows[2].releaseAge

    and: 'the sort is indicated as descending for the RELEASE age'
    !newestRiskPage.newestViolationTable.isUp(newestRiskPage.newestViolationTable.releaseHeader)
    newestRiskPage.newestViolationTable.isDown(newestRiskPage.newestViolationTable.releaseHeader)
  }

  def 'Newest risk table can be sorted by threat'() {
    when: 'the newest risk table is shown'
    def newestRiskPage = at NewestRiskDashboardPage
    waitFor { newestViolationTable.displayed }
    waitFor { newestViolationTable.rows.size() == 3 }

    and: 'we click on the THREAT column header to sort'
    newestViolationTable.threatHeader.click()

    then: 'the highest threat should sort to the top'
    waitFor { newestViolationTable.rows[0].threat == 10 }

    and: 'the sort is indicated as descending for threat'
    !newestViolationTable.isUp(newestViolationTable.threatHeader)
    newestViolationTable.isDown(newestViolationTable.threatHeader)

    when: 'clicking the header again'
    newestViolationTable.threatHeader.click()

    then: 'the lowest threat should sort to the top'
    waitFor { newestViolationTable.rows[0].threat == 2 }

    and: 'the sort is indicated as ascending for threat'
    newestViolationTable.isUp(newestViolationTable.threatHeader)
    !newestViolationTable.isDown(newestViolationTable.threatHeader)
  }

  def 'Newest risk table can be sorted by application name'() {
    when: 'the newest risk table is shown'
    def newestRiskPage = at NewestRiskDashboardPage
    waitFor { newestViolationTable.displayed }
    waitFor { newestViolationTable.rows.size() == 3 }

    and: 'we click on the APPLICATION column header to sort'
    newestViolationTable.applicationHeader.click()

    then: 'the table should sort by application alphabetically'
    waitFor { newestViolationTable.rows[0].application == 'DashboardSpecAppOne' }
    newestViolationTable.rows[1].application == 'DashboardSpecAppOne'
    newestViolationTable.rows[2].application == 'DashboardSpecAppTwo'

    and: 'the sort is indicated as ascending for application'
    newestViolationTable.isUp(newestViolationTable.applicationHeader)
    !newestViolationTable.isDown(newestViolationTable.applicationHeader)

    when: 'clicking the header again'
    newestViolationTable.applicationHeader.click()

    then: 'the table should reverse the sort'
    waitFor { newestViolationTable.rows[0].application == 'DashboardSpecAppTwo' }
    newestViolationTable.rows[1].application == 'DashboardSpecAppOne'
    newestViolationTable.rows[2].application == 'DashboardSpecAppOne'

    and: 'the sort is indicated as descending for application'
    !newestViolationTable.isUp(newestViolationTable.applicationHeader)
    newestViolationTable.isDown(newestViolationTable.applicationHeader)
  }

  def 'Newest risk table can be sorted by component name'() {
    when: 'the newest risk table is shown'
    def newestRiskPage = at NewestRiskDashboardPage
    waitFor { newestViolationTable.displayed }
    waitFor { newestViolationTable.rows.size() == 3 }

    and: 'we click on the COMPONENT column header to sort'
    newestViolationTable.componentHeader.click()

    then: 'the table should sort by component alphabetically'
    waitFor { newestViolationTable.rows[0].component == DEFAULT_COMPONENT }
    newestViolationTable.rows[1].component == DEFAULT_COMPONENT
    newestViolationTable.rows[2].component == 'unknown.jar'

    and: 'the sort is indicated as ascending for component'
    newestViolationTable.isUp(newestViolationTable.componentHeader)
    !newestViolationTable.isDown(newestViolationTable.componentHeader)

    when: 'clicking the header again'
    newestViolationTable.componentHeader.click()

    then: 'the table should reverse the sort'
    waitFor { newestViolationTable.rows[2].component == DEFAULT_COMPONENT }
    newestViolationTable.rows[1].component == DEFAULT_COMPONENT
    newestViolationTable.rows[0].component == 'unknown.jar'

    and: 'the sort is indicated as descending for component'
    !newestViolationTable.isUp(newestViolationTable.componentHeader)
    newestViolationTable.isDown(newestViolationTable.componentHeader)
  }

  def 'Newest Risk Table can be filtered'() {
    when: 'newest risk table is shown'
    def newestRiskPage = at NewestRiskDashboardPage
    waitFor { newestRiskPage.newestViolationTable.displayed }
    waitFor { newestRiskPage.newestViolationTable.rows.size() == 3 }

    then: 'policy violations are listed by age and then threat level'
    !newestRiskPage.noDataAvailable.displayed

    newestRiskPage.newestViolationTable.rows[0].threat == 10
    newestRiskPage.newestViolationTable.rows[0].policy == 'DashboardSpecPolicy'
    newestRiskPage.newestViolationTable.rows[0].application == secondApp.name
    newestRiskPage.newestViolationTable.rows[0].component == 'unknown.jar'
    newestRiskPage.newestViolationTable.rows[0].age ==~ RECENT_AGE

    newestRiskPage.newestViolationTable.rows[1].threat == 5
    newestRiskPage.newestViolationTable.rows[1].policy == 'DashboardSpecPolicy'
    newestRiskPage.newestViolationTable.rows[1].application == firstApp.name
    newestRiskPage.newestViolationTable.rows[1].component == DEFAULT_COMPONENT
    newestRiskPage.newestViolationTable.rows[1].age == '7d'

    when: 'filtering to an application'
    newestRiskPage.filters.toggle.click()
    waitFor { newestRiskPage.filters.applicationMultiselect.displayed }
    newestRiskPage.filters.applicationMultiselect.toggleOption(secondApp.name)
    newestRiskPage.filters.apply()

    then: 'only violations from that application are shown'
    waitFor { newestRiskPage.newestViolationTable.rows.size() == 1 }
    !newestRiskPage.filters.applicationMultiselect.displayed
    newestRiskPage.newestViolationTable.rows[0].threat == 10
    newestRiskPage.newestViolationTable.rows[0].policy == 'DashboardSpecPolicy'
    newestRiskPage.newestViolationTable.rows[0].application == secondApp.name
    newestRiskPage.newestViolationTable.rows[0].component == 'unknown.jar'
    newestRiskPage.newestViolationTable.rows[0].age ==~ RECENT_AGE

    when: 'filtering to a stage'
    newestRiskPage.filters.toggle.click()
    waitFor { newestRiskPage.filters.stageTypeMultiselect.displayed }
    newestRiskPage.filters.stageTypeMultiselect.toggleOption('Release')
    newestRiskPage.filters.apply()

    then: 'only violations from that stage are shown'
    waitFor { newestRiskPage.newestViolationTable.rows.size() == 1 }
    !newestRiskPage.filters.stageTypeMultiselect.displayed
    newestRiskPage.newestViolationTable.rows[0].threat == 10
    newestRiskPage.newestViolationTable.rows[0].policy == 'DashboardSpecPolicy'
    newestRiskPage.newestViolationTable.rows[0].application == secondApp.name
    newestRiskPage.newestViolationTable.rows[0].component == 'unknown.jar'
    newestRiskPage.newestViolationTable.rows[0].age ==~ RECENT_AGE

    and: 'only release stage is visible'
    !newestRiskPage.newestViolationTable.buildHeader.displayed
    !newestRiskPage.newestViolationTable.stageHeader.displayed
    newestRiskPage.newestViolationTable.releaseHeader.displayed
    !newestRiskPage.newestViolationTable.operateHeader.displayed
  }

  def 'Newest Risk Table shows stages in chronological order'() {
    when: 'newest risk table is shown'
    def newestRiskPage = at NewestRiskDashboardPage
    waitFor { newestRiskPage.newestViolationTable.displayed }

    then: 'the table header lists the stages in proper order'
    waitFor {
      newestRiskPage.newestViolationTable.headers[5..8]*.@id ==
          [
            'stage-header-build',
            'stage-header-stage-release',
            'stage-header-release',
            'stage-header-operate'
          ]
    }
  }

  def 'Filter out all results'() {
    when: 'selecting filters that match no results on the newest risk tab'
    def newestRiskPage = at NewestRiskDashboardPage
    newestRiskPage.filters.toggle.click()
    waitFor { newestRiskPage.filters.policyTypeMultiselect.displayed }
    newestRiskPage.filters.policyTypeMultiselect.toggleOption('Security')
    newestRiskPage.filters.apply()

    then: 'the table is replaced by no result text'
    waitFor { newestRiskPage.noDataAvailable.displayed }

    and: 'newest risk no data text is shown'
    newestRiskPage.noDataAvailable.text() ==
        "No data available in the last 30 days given the applied filters and available permissions.";

    when: 'user clicks the by component tab'
    newestRiskPage.tabLinks.componentsTabButton.click()
    def compViolationsPage = at ComponentViolationsDashboardPage

    then: 'the table is replaced by no result text'
    waitFor { compViolationsPage.noDataAvailable.displayed }

    when: 'user clicks the by application tab'
    compViolationsPage.tabLinks.applicationsTabButton.click()
    def appViolationsPage = at ApplicationViolationsDashboardPage

    then: 'the table is replaced by no result text'
    waitFor { appViolationsPage.noDataAvailable.displayed }
  }

  def 'Threat level cells heat map'() {
    // same calculation performed at the UI level, which sets a minimum value of 0.1
    Closure alphaCalc = { double value, double max ->
      max ? ((9 * (value / max)) + 1) / 10 : 0.1
    }

    setup: 'Add a few records'
    List<Application> applications = new ArrayList<Application>();
    for (i in 0..2) {
      Date now = new Date()
      def application = temporaryEntity.newApplication("DashboardSpecApp${i}", "DashboardSpecApp${i}", org.id)
      applications.add(application)
      def policyEvaluation = temporaryEntity.newPolicyEvaluation(application.id, BuildStageType.ID,
          "DashboardSpecFirstEvaluation${i}", now - 7)
      def violation = temporaryEntity.newPolicyViolation(policyEvaluation, policy, 2 + (3 * i),
          PolicyThreatCategory.SECURITY, "Group${i}", "Artifact${i}", "Version${i}", "hash${i}")
      temporaryEntity.newFirstOccurrencePolicyViolation(violation.id, policyEvaluation.applicationId,
          policyEvaluation.stageTypeId)
    }

    when: 'Switching to Components Tab'
    tabLinks.componentsTabButton.click()
    def compViolationsPage = at ComponentViolationsDashboardPage

    then: 'Components tab heat map is shown'
    waitFor { compViolationsPage.componentViolationsTable.rows.size() == 4 }
    ComponentViolationsTableRow firstComponentRow = compViolationsPage.componentViolationsTable.rows[0]
    ComponentViolationsTableRow secondComponentRow = compViolationsPage.componentViolationsTable.rows[1]
    ComponentViolationsTableRow thirdComponentRow = compViolationsPage.componentViolationsTable.rows[2]
    ComponentViolationsTableRow fourthComponentRow = compViolationsPage.componentViolationsTable.rows[3]

    that parseAlpha(secondComponentRow.netRisk.attr('style')), closeTo(alphaCalc(8, 15), TOLERANCE)
    that parseAlpha(thirdComponentRow.netRisk.attr('style')), closeTo(alphaCalc(5, 15), TOLERANCE)
    that parseAlpha(fourthComponentRow.netRisk.attr('style')), closeTo(alphaCalc(2, 15), TOLERANCE)

    that parseAlpha(secondComponentRow.criticalRisk.attr('style')), closeTo(alphaCalc(8, 10), TOLERANCE)
    that parseAlpha(thirdComponentRow.criticalRisk.attr('style')), closeTo(alphaCalc(0, 10), TOLERANCE)
    that parseAlpha(fourthComponentRow.criticalRisk.attr('style')), closeTo(alphaCalc(0, 10), TOLERANCE)

    that parseAlpha(secondComponentRow.severeRisk.attr('style')), closeTo(alphaCalc(0, 5), TOLERANCE)
    that parseAlpha(fourthComponentRow.severeRisk.attr('style')), closeTo(alphaCalc(0, 5), TOLERANCE)

    that parseAlpha(firstComponentRow.moderateRisk.attr('style')), closeTo(alphaCalc(0, 2), TOLERANCE)
    that parseAlpha(secondComponentRow.moderateRisk.attr('style')), closeTo(alphaCalc(0, 2), TOLERANCE)
    that parseAlpha(thirdComponentRow.moderateRisk.attr('style')), closeTo(alphaCalc(0, 2), TOLERANCE)

    when: 'Switching to Applications Tab'
    tabLinks.applicationsTabButton.click()
    def appViolationsPage = at ApplicationViolationsDashboardPage

    then: 'Applications tab heat map is shown'
    waitFor { appViolationsPage.applicationViolationsTable.rows.size() == 5 }

    ApplicationViolationsTableRow firstApplicationRow = appViolationsPage.applicationViolationsTable.rows[0]
    ApplicationViolationsTableRow secondApplicationRow = appViolationsPage.applicationViolationsTable.rows[1]
    ApplicationViolationsTableRow thirdApplicationRow = appViolationsPage.applicationViolationsTable.rows[2]
    ApplicationViolationsTableRow fourthApplicationRow = appViolationsPage.applicationViolationsTable.rows[3]
    ApplicationViolationsTableRow fifthApplicationRow = appViolationsPage.applicationViolationsTable.rows[4]

    that parseAlpha(secondApplicationRow.netRisk.attr('style')), closeTo(alphaCalc(8, 10), TOLERANCE)
    that parseAlpha(thirdApplicationRow.netRisk.attr('style')), closeTo(alphaCalc(5, 10), TOLERANCE)
    that parseAlpha(fourthApplicationRow.netRisk.attr('style')), closeTo(alphaCalc(5, 10), TOLERANCE)
    that parseAlpha(fifthApplicationRow.netRisk.attr('style')), closeTo(alphaCalc(2, 10), TOLERANCE)

    that parseAlpha(secondApplicationRow.criticalRisk.attr('style')), closeTo(alphaCalc(8, 10), TOLERANCE)
    that parseAlpha(thirdApplicationRow.criticalRisk.attr('style')), closeTo(alphaCalc(0, 10), TOLERANCE)
    that parseAlpha(fourthApplicationRow.criticalRisk.attr('style')), closeTo(alphaCalc(0, 10), TOLERANCE)
    that parseAlpha(fifthApplicationRow.criticalRisk.attr('style')), closeTo(alphaCalc(0, 10), TOLERANCE)

    that parseAlpha(firstApplicationRow.severeRisk.attr('style')), closeTo(alphaCalc(0, 5), TOLERANCE)
    that parseAlpha(secondApplicationRow.severeRisk.attr('style')), closeTo(alphaCalc(0, 5), TOLERANCE)
    that parseAlpha(fifthApplicationRow.severeRisk.attr('style')), closeTo(alphaCalc(0, 5), TOLERANCE)

    that parseAlpha(firstApplicationRow.moderateRisk.attr('style')), closeTo(alphaCalc(0, 2), TOLERANCE)
    that parseAlpha(secondApplicationRow.moderateRisk.attr('style')), closeTo(alphaCalc(0, 2), TOLERANCE)
    that parseAlpha(thirdApplicationRow.moderateRisk.attr('style')), closeTo(alphaCalc(0, 2), TOLERANCE)
    that parseAlpha(fourthApplicationRow.moderateRisk.attr('style')), closeTo(alphaCalc(0, 2), TOLERANCE)

    cleanup:
    ApplicationDAO dao = new ApplicationDAO()
    for (Application application : applications) {
      dao.delete(application)
    }
  }

  def 'Limits results to 100 records'() {
    setup: 'Add over 100 records'
    Date now = new Date()
    PolicyEvaluation policyEvaluation = temporaryEntity.newPolicyEvaluation(firstApp.id, BuildStageType.ID,
        'DashboardSpecFirstEvaluation', now - 7)
    for (i in 0..100) {
      PolicyViolation violation = temporaryEntity.
          newPolicyViolation(policyEvaluation, policy, 5, PolicyThreatCategory.SECURITY,
          "Group${i}", "Artifact${i}", "Version${i}", "Hash${i}")
      temporaryEntity.newFirstOccurrencePolicyViolation(violation.id, policyEvaluation.applicationId,
          policyEvaluation.stageTypeId)
    }

    when: 'Refreshing to page'
    driver.navigate().refresh()

    then: 'Only the first 500 pixels of results are shown'
    def newestRiskPage = at NewestRiskDashboardPage
    waitFor { newestRiskPage.newestViolationTable.rows[0].displayed }
    int tableBottom = newestRiskPage.highestRiskDiv.y + newestRiskPage.highestRiskDiv.height

    // It is a reasonable expectation that the first 5 rows will render within 500 px in every browser
    for (i in 0..5) {
      assert newestRiskPage.newestViolationTable.rows[i].y < tableBottom
    }
    // And that rows 44-49 will not render within the scroll view
    for (i in 44..49) {
      assert newestRiskPage.newestViolationTable.rows[i].y > tableBottom
    }

    and: 'A message is displayed to show that only the top results are shown'
    newestRiskPage.maxResults.text() == 'Showing the newest 100 results'

    cleanup:
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO().delete(policyEvaluation)
  }

  def 'Threat level columns are hidden when not matching filter'() {
    given: 'dashboard filter includes only severe threat level'
    def newestRiskPage = at NewestRiskDashboardPage
    newestRiskPage.filters.toggle.click()
    waitFor { newestRiskPage.filters.policyThreatLevelSlider.displayed }
    newestRiskPage.filters.policyThreatLevelSlider.setValues(4, 7)
    newestRiskPage.filters.apply()

    when: 'switching to the component risk view'
    newestRiskPage.tabLinks.componentsTabButton.click()
    def compViolationsPage = at ComponentViolationsDashboardPage

    then: 'only the severe threat level column is shown'
    waitFor { compViolationsPage.componentViolationsTable.threatHeaders.severe.displayed }
    !compViolationsPage.componentViolationsTable.threatHeaders.critical.displayed
    !compViolationsPage.componentViolationsTable.threatHeaders.moderate.displayed
    !compViolationsPage.componentViolationsTable.threatHeaders.low.displayed
    waitFor { !compViolationsPage.componentViolationsTable.rows.empty }
    compViolationsPage.componentViolationsTable.rows[0].severeRisk.displayed
    compViolationsPage.componentViolationsTable.rows[0].severeRisk.text() == '5'
    !compViolationsPage.componentViolationsTable.rows[0].criticalRisk.displayed
    !compViolationsPage.componentViolationsTable.rows[0].moderateRisk.displayed
    !compViolationsPage.componentViolationsTable.rows[0].lowRisk.displayed

    when: 'switching to the application risk view'
    compViolationsPage.tabLinks.applicationsTabButton.click()
    def appViolationsPage = at ApplicationViolationsDashboardPage

    then: 'only the severe threat level column is shown'
    waitFor { appViolationsPage.applicationViolationsTable.threatHeaders.severe.displayed }
    !appViolationsPage.applicationViolationsTable.threatHeaders.critical.displayed
    !appViolationsPage.applicationViolationsTable.threatHeaders.moderate.displayed
    !appViolationsPage.applicationViolationsTable.threatHeaders.low.displayed
    waitFor { !appViolationsPage.applicationViolationsTable.rows.empty }
    appViolationsPage.applicationViolationsTable.rows[0].severeRisk.displayed
    appViolationsPage.applicationViolationsTable.rows[0].severeRisk.text() == '5'
    !appViolationsPage.applicationViolationsTable.rows[0].criticalRisk.displayed
    !appViolationsPage.applicationViolationsTable.rows[0].moderateRisk.displayed
    !appViolationsPage.applicationViolationsTable.rows[0].lowRisk.displayed

    when: 'expanding the application row'
    appViolationsPage.applicationViolationsTable.rows[0].expand.click()

    then: 'only the severe threat level column is shown for the stage details'
    waitFor { appViolationsPage.applicationViolationsTable.rows.size() > 1 }
    appViolationsPage.applicationViolationsTable.rows[1].severeRisk.displayed
    appViolationsPage.applicationViolationsTable.rows[1].severeRisk.text() == '5'
    !appViolationsPage.applicationViolationsTable.rows[1].criticalRisk.displayed
    !appViolationsPage.applicationViolationsTable.rows[1].moderateRisk.displayed
    !appViolationsPage.applicationViolationsTable.rows[1].lowRisk.displayed
  }

  def 'Filters stored as expected'() {
    when: 'dashboard filters are applied'
    def newestRiskPage = at NewestRiskDashboardPage
    newestRiskPage.filters.toggle.click()
    waitFor { newestRiskPage.filters.applicationMultiselect.displayed }
    newestRiskPage.filters.policyTypeMultiselect.displayed
    newestRiskPage.filters.applicationMultiselect.toggleOption(firstApp.name)
    newestRiskPage.filters.applicationMultiselect.toggleOption(secondApp.name)
    newestRiskPage.filters.applicationTagMultiselect.toggleOption(firstAppTag.name)
    newestRiskPage.filters.policyTypeMultiselect.toggleOption('Security')
    newestRiskPage.filters.policyTypeMultiselect.toggleOption('Other')
    newestRiskPage.filters.stageTypeMultiselect.toggleOption('Release')
    newestRiskPage.filters.apply()

    then: 'filters are stored to disk'
    DashboardFilterDTO dto = new ObjectMapper().
        readValue(new DashboardFilterDAO().getByUsername("admin").filter, DashboardFilterDTO.class);
    dto.applicationFilters.contains(firstApp.id)
    dto.applicationFilters.contains(secondApp.id)
    dto.tagFilters.contains(firstAppTag.id)
    dto.policyThreatCategoryFilters.contains(PolicyThreatCategory.SECURITY)
    dto.policyThreatCategoryFilters.contains(PolicyThreatCategory.OTHER)
    dto.stageTypeFilters.contains('release')
  }

  def 'Stored filters loaded on view of dashboard'() {
    setup: 'Add filter for admin user'
    DashboardFilterDTO dto = new DashboardFilterDTO()
    dto.applicationFilters = [firstApp.id, secondApp.id]
    dto.maxPolicyThreatLevel = 6
    dto.minPolicyThreatLevel = 3
    dto.policyThreatCategoryFilters = [
      PolicyThreatCategory.SECURITY,
      PolicyThreatCategory.OTHER
    ]
    dto.stageTypeFilters = [Stage.ID_RELEASE]
    dto.tagFilters = [firstAppTag.id]

    temporaryEntity.updateDashboardFilter('admin', new ObjectMapper().writeValueAsString(dto));

    when: 'Refresh the page to reload the filters'
    driver.navigate().refresh()

    and: 'the filters have been reloaded'
    def newestRiskPage = at NewestRiskDashboardPage
    waitFor { newestRiskPage.filters.applicationSummary.displayed && componentMatchSection.displayed }

    then: 'See proper values set in the filters'
    newestRiskPage.filters.applicationSummary.getTooltipContent() == firstApp.name + '\n' + secondApp.name
    newestRiskPage.filters.applicationTagSummary.getTooltipContent() == firstAppTag.name
    newestRiskPage.filters.stageTypeSummary.getTooltipContent() == 'Release'
    newestRiskPage.filters.policyTypeSummary.getTooltipContent() == 'Security\nOther'
    newestRiskPage.filters.policyThreatLevelSummary.getTooltipContent() == 'Policy threat levels 3 through 6'

    and: 'See proper counts set in the filters'
    newestRiskPage.filters.applicationSummaryCount.text() == '2'
    newestRiskPage.filters.applicationTagSummary.text() == '1'
    newestRiskPage.filters.stageTypeSummary.text() == '1'
    newestRiskPage.filters.policyTypeSummary.text() == '2'
    newestRiskPage.filters.policyThreatLevelSummary.text() == '4'
  }

  def 'Components Table'() {
    when: 'Switch to Components Tab'
    tabLinks.componentsTabButton.click()
    def compViolationsPage = at ComponentViolationsDashboardPage

    then: 'Component Table Displayed'
    waitFor { compViolationsPage.componentViolationsTable.rows.size() == 1 }
    compViolationsPage.componentViolationsTable.rows[0].component.text() == "Group1 : Artifact1 : Version1"
    compViolationsPage.componentViolationsTable.rows[0].affectedApplications.text() == "2"
    compViolationsPage.componentViolationsTable.rows[0].affectedApplicationsLink.displayed
    compViolationsPage.componentViolationsTable.rows[0].netRisk.text() == "15"
    compViolationsPage.componentViolationsTable.rows[0].criticalRisk.text() == "10"
    compViolationsPage.componentViolationsTable.rows[0].severeRisk.text() == "5"
    compViolationsPage.componentViolationsTable.rows[0].moderateRisk.text() == "0"
    !compViolationsPage.componentViolationsTable.rows[0].lowRisk.displayed
    compViolationsPage.componentViolationsTable.rows[0].componentLink.displayed
    report('Components Table')

    when: 'clicking the component link'
    compViolationsPage.componentViolationsTable.rows[0].componentLink.click()

    then: 'the component drilldown page is shown'
    at ComponentDrilldownPage
  }

  def 'Applications Table'() {
    when: 'Switch to Applications Tab'
    tabLinks.applicationsTabButton.click()
    def appViolationsPage = at ApplicationViolationsDashboardPage
    then:
    waitFor { appViolationsPage.applicationViolationsTable.rows.size() == 2 }
    appViolationsPage.applicationViolationsTable.rows[0].application.text() == secondApp.getName()
    appViolationsPage.applicationViolationsTable.rows[0].netRisk.text() == "10"
    appViolationsPage.applicationViolationsTable.rows[0].criticalRisk.text() == "10"
    appViolationsPage.applicationViolationsTable.rows[0].severeRisk.text() == "0"
    appViolationsPage.applicationViolationsTable.rows[0].moderateRisk.text() == "0"
    !appViolationsPage.applicationViolationsTable.rows[0].lowRisk.displayed
    appViolationsPage.applicationViolationsTable.rows[0].expand.displayed
    appViolationsPage.applicationViolationsTable.rows[1].application.text() == firstApp.getName()
    appViolationsPage.applicationViolationsTable.rows[1].netRisk.text() == "5"
    appViolationsPage.applicationViolationsTable.rows[1].criticalRisk.text() == "0"
    appViolationsPage.applicationViolationsTable.rows[1].severeRisk.text() == "5"
    appViolationsPage.applicationViolationsTable.rows[1].moderateRisk.text() == "0"
    !appViolationsPage.applicationViolationsTable.rows[1].lowRisk.displayed
    appViolationsPage.applicationViolationsTable.rows[1].expand.displayed

    when: 'Expand'
    appViolationsPage.applicationViolationsTable.rows[0].expand.click()
    then: 'Stage shown'
    waitFor { appViolationsPage.applicationViolationsTable.rows.size() == 3 }
    appViolationsPage.applicationViolationsTable.rows[0].collapse.displayed
    appViolationsPage.applicationViolationsTable.rows[1].application.text() ==
        new ReleaseStageType().getName().toUpperCase()
    appViolationsPage.applicationViolationsTable.rows[1].reportLink.displayed

    when: 'Expand'
    appViolationsPage.applicationViolationsTable.rows[2].expand.click()

    then: 'stages shown in chronological order'
    waitFor { appViolationsPage.applicationViolationsTable.rows.size() == 6 }
    appViolationsPage.applicationViolationsTable.rows[2].collapse.displayed
    appViolationsPage.applicationViolationsTable.rows[3].application.text() ==
        new BuildStageType().getName().toUpperCase()
    appViolationsPage.applicationViolationsTable.rows[3].reportLink.displayed
    appViolationsPage.applicationViolationsTable.rows[4].application.text() ==
        new StageReleaseStageType().getName().toUpperCase()
    appViolationsPage.applicationViolationsTable.rows[4].reportLink.displayed
    appViolationsPage.applicationViolationsTable.rows[5].application.text() ==
        new ReleaseStageType().getName().toUpperCase()
    appViolationsPage.applicationViolationsTable.rows[5].reportLink.displayed
    report('Applications Table')

    and: 'the stage label links to the underlying report'
    withNewWindow(page: ReportContainerPage,
    { appViolationsPage.applicationViolationsTable.rows[3].reportLink.click() }) {
      verifyAt()
      reportTitle.text()
    } ==~ firstApp.getName() + ' .* Build Report'
  }

  def 'Dashboard Filter Summary'() {
    when: 'the filter summary data is loaded'
    def newestRiskPage = at NewestRiskDashboardPage
    waitFor { newestRiskPage.summaryData.displayed }

    then: 'the count of total applications is shown'
    newestRiskPage.summaryTotalApplications.displayed
    newestRiskPage.summaryTotalApplications.text() == '2'

    and: 'the count of matched applications is shown'
    newestRiskPage.summaryMatchedApplications.displayed
    newestRiskPage.summaryMatchedApplications.text() == '2'

    and: 'the percentage of matched applications is shown'
    newestRiskPage.summaryPercentApplications.displayed
    newestRiskPage.summaryPercentApplications.text() == '100%'

    and: 'the count of total policies is shown'
    newestRiskPage.summaryTotalPolicies.displayed
    newestRiskPage.summaryTotalPolicies.text() == '1'

    and: 'the count of matched policies is shown'
    newestRiskPage.summaryMatchedPolicies.displayed
    newestRiskPage.summaryMatchedPolicies.text() == '1'

    and: 'the percentage of matched policies is shown'
    newestRiskPage.summaryPercentPolicies.displayed
    newestRiskPage.summaryPercentPolicies.text() == '100%'

    and: 'the count of total components is shown'
    newestRiskPage.summaryTotalComponents.displayed
    newestRiskPage.summaryTotalComponents.text() == '3'

    and: 'the count of matched components is shown'
    newestRiskPage.summaryMatchedComponents.displayed
    newestRiskPage.summaryMatchedComponents.text() == '3'

    and: 'the percentage of matched components is shown'
    newestRiskPage.summaryPercentComponents.displayed
    newestRiskPage.summaryPercentComponents.text() == '100%'
  }

  def 'Dashboard component match summary'() {
    when: 'the component match summary is shown'
    def newestRiskPage = at NewestRiskDashboardPage
    waitFor { newestRiskPage.componentMatchSection.displayed }

    then: 'the count of exact match components is shown'
    newestRiskPage.componentMatchExactCount.displayed
    newestRiskPage.componentMatchExactCount.text() == '1 (33%)'

    and: 'the count of similar match components is shown'
    newestRiskPage.componentMatchSimilarCount.displayed
    newestRiskPage.componentMatchSimilarCount.text() == '1 (33%)'

    and: 'the count of unknown components is shown'
    newestRiskPage.componentMatchUnknownCount.displayed
    newestRiskPage.componentMatchUnknownCount.text() == '1 (33%)'
  }

  def 'Heat Map Help Modal'() {
    when: 'component heat map help icon is clicked'
    clickComponentHeatMapHelp()

    then: 'component heat map help is displayed'
    waitFor { componentHeatMapHelp.displayed }

    when: 'the modal backdrop is clicked'
    interact {
      // don't click at the center of the backdrop which can still be within the modal
      moveToElement(modalBackdrop, 10, 10)
      click()
    }

    then: 'the component heat map help closes'
    waitFor { !componentHeatMapHelp.displayed }

    when: 'the component heat map help close button is clicked'
    clickComponentHeatMapHelp()
    waitFor { componentHeatMapHelpClose.displayed }
    componentHeatMapHelpClose.click()

    then: 'the help modal closes'
    waitFor { !componentHeatMapHelp.displayed }

    when: 'application heat map help icon is clicked'
    clickApplicationHeatMapHelp()

    then: 'application heat map help is displayed'
    waitFor { applicationHeatMapHelp.displayed }

    when: 'the modal backdrop is clicked'
    interact {
      // don't click at the center of the backdrop which can still be within the modal
      moveToElement(modalBackdrop, 10, 10)
      click()
    }

    then: 'the application heat map help closes'
    waitFor { !applicationHeatMapHelp.displayed }

    when: 'the application heat map help close button is clicked '
    clickApplicationHeatMapHelp()
    waitFor { applicationHeatMapHelpClose.displayed }
    applicationHeatMapHelpClose.click()

    then: 'the help modal closes'
    waitFor { !applicationHeatMapHelp.displayed }
  }

  def clickComponentHeatMapHelp() {
    tabLinks.componentsTabButton.click()
    waitFor { tabLinks.componentHeatMapHelpIcon.displayed }
    tabLinks.componentHeatMapHelpIcon.click()
  }

  def clickApplicationHeatMapHelp() {
    tabLinks.applicationsTabButton.click()
    waitFor { tabLinks.applicationHeatMapHelpIcon.displayed }
    tabLinks.applicationHeatMapHelpIcon.click()
  }

  /**
   * helper method to parse alpha value from an rgb style string
   */
  def parseAlpha(String style) {
    (style =~ alphaMatcher)[0][1].toBigDecimal()
  }
}
