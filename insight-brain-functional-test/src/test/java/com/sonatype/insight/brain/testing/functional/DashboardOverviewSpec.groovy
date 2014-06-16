/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.fasterxml.jackson.databind.ObjectMapper
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
import org.codehaus.plexus.util.FileUtils

import static com.sonatype.insight.brain.testing.functional.modules.ThreatTableRow.BUILD_AGE
import static com.sonatype.insight.brain.testing.functional.modules.ThreatTableRow.RELEASE_AGE
import static com.sonatype.insight.brain.testing.functional.modules.ThreatTableRow.STAGE_RELEASE_AGE
import static com.sonatype.insight.brain.testing.functional.modules.ThreatTableRow.OPERATE_AGE
import static spock.util.matcher.HamcrestMatchers.closeTo
import static spock.util.matcher.HamcrestSupport.that

/**
 * @since 1.11
 */
class DashboardOverviewSpec
    extends BaseSpec
{
  static final String RECENT_AGE = /[1-9]min/

  static final String alphaMatcher = /background-color: rgba\([0-9][0-9][0-9]?, [0-9][0-9][0-9]?, [0-9][0-9][0-9]?, (.*)\);/

  // accept differential for precision of alpha results
  static final BigDecimal TOLERANCE = 0.05

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
        firstViolation.hash, firstViolation.groupId, firstViolation.artifactId, firstViolation.version, )
    temporaryEntity.newApplicationComponent(firstPolicyEvaluation.applicationId, firstPolicyEvaluation.stageTypeId, '987654321', MatchState.SIMILAR, false);
    temporaryEntity.newApplicationComponent(firstPolicyEvaluation.applicationId, firstPolicyEvaluation.stageTypeId, '987654322', MatchState.UNKNOWN, false);

    //same policy as first evaluation, but a different stage and earlier
    PolicyEvaluation firstPolicyEvaluationSecondStage = temporaryEntity.newPolicyEvaluation(firstApp.id,
        StageReleaseStageType.ID, 'DashboardSpecFirstEvaluationSecondStage', now - 14)
    PolicyViolation firstViolationSecondStage = temporaryEntity.newPolicyViolation(firstPolicyEvaluationSecondStage,
        policy, 5, PolicyThreatCategory.LICENSE, "Group1", "Artifact1", "Version1", "hash", WarnActionType.ID)
    temporaryEntity.newFirstOccurrencePolicyViolation(firstViolationSecondStage.id,
        firstPolicyEvaluationSecondStage.applicationId, firstPolicyEvaluationSecondStage.stageTypeId)
    temporaryEntity.newApplicationComponent(firstPolicyEvaluationSecondStage.applicationId,
        firstPolicyEvaluationSecondStage.stageTypeId, firstViolationSecondStage.hash,
        firstViolationSecondStage.groupId, firstViolationSecondStage.artifactId,
        firstViolationSecondStage.version)

    // evaluation in yet another stage
    PolicyEvaluation thirdPolicyEvaluation = temporaryEntity.newPolicyEvaluation(firstApp.id, ReleaseStageType.ID,
        'DashboardSpecThirdEvaluation', now - 8)
    PolicyViolation thirdViolation = temporaryEntity.newPolicyViolation(thirdPolicyEvaluation, policy, 2, PolicyThreatCategory.QUALITY, 
        "Group1", "Artifact1", "Version1")
    temporaryEntity.newFirstOccurrencePolicyViolation(thirdViolation.id, thirdPolicyEvaluation.applicationId,
        thirdPolicyEvaluation.stageTypeId)

    // and one more stage to cover them all
    PolicyEvaluation forthPolicyEvaluation = temporaryEntity.newPolicyEvaluation(firstApp.id, OperateStageType.ID,
        'DashboardSpecForthEvaluation', now - 9)
    PolicyViolation forthViolation = temporaryEntity.newPolicyViolation(forthPolicyEvaluation, policy, 1, PolicyThreatCategory.OTHER, 
        "Group1", "Artifact1", "Version1")
    temporaryEntity.newFirstOccurrencePolicyViolation(forthViolation.id, forthPolicyEvaluation.applicationId,
        forthPolicyEvaluation.stageTypeId)

    //most recent evaluation
    PolicyEvaluation secondPolicyEvaluation = temporaryEntity.newPolicyEvaluation(secondApp.id, ReleaseStageType.ID,
        'DashboardSpecSecondEvaluation', now)
    PolicyViolation secondViolation = temporaryEntity.newPolicyViolation(secondPolicyEvaluation, policy, 10,
            PolicyThreatCategory.QUALITY, null, null, null)
    temporaryEntity.newFirstOccurrencePolicyViolation(secondViolation.id, secondPolicyEvaluation.applicationId,
        secondPolicyEvaluation.stageTypeId)

    InsightWork work = new InsightWork(serviceRule.configuration)
    File reportZip = work.getReportFile(firstPolicyEvaluation.getApplicationId(), firstPolicyEvaluation.getScanId())
    FileUtils.copyURLToFile(getClass().getResource('/canned-reports/small-report.zip'), reportZip)

    loginAsAdminVia(DashboardOverviewPage)
  }

  def setup() {
    clearFilter()
  }

  /**
   * Do not clear cookies so we don't have to log back in after every feature test.
   * Delete the stored filter directly from the database and refresh the page to clear locally cached filter.
   */
  private void clearFilter() {
    browser.config.autoClearCookies = false
    DashboardFilterDAO dao = new DashboardFilterDAO();
    dao.delete(dao.getByUsername("admin"));
    driver.navigate().refresh()
    waitFor { at DashboardOverviewPage }
  }

  def 'Dashboard Overview Breadcrumb'() {
    when: 'The dashboard overview is loaded'
      waitFor { breadcrumbs.size() == 1 }
    then: 'Only the dashboard breadcrumb is shown'
      crumb('dashboard.overview').displayed
      crumb('dashboard.overview').text() == ' Dashboard'
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

    and: 'stage type filters are shown in proper chronological order'
      stageTypeFiltersDropdown.showDropdown()
      stageTypeFiltersDropdown.dropdownName(0).displayed
      stageTypeFiltersDropdown.dropdownName(0).text() == 'Build'
      stageTypeFiltersDropdown.dropdownName(1).displayed
      stageTypeFiltersDropdown.dropdownName(1).text() == 'Stage Release'
      stageTypeFiltersDropdown.dropdownName(2).displayed
      stageTypeFiltersDropdown.dropdownName(2).text() == 'Release'
      stageTypeFiltersDropdown.dropdownName(3).displayed
      stageTypeFiltersDropdown.dropdownName(3).text() == 'Operate'
      !stageTypeFiltersDropdown.dropdownName(4).present
      stageTypeFiltersDropdown.hideDropdown()

    and: 'application tag filters are shown'
      applicationTagFiltersDropdown.showDropdown()
      applicationTagFiltersDropdown.dropdownCheck(firstAppTag.name).displayed
      applicationTagFiltersDropdown.dropdownOwner(firstAppTag.name).text() == 'in ' + org.name
      applicationTagFiltersDropdown.areOptionsColored([(firstAppTag.name): "blue"])
      applicationTagFiltersDropdown.hideDropdown()

    and: 'policy threat level filter is shown'
      policyThreatLevelSlider.slider.displayed
      policyThreatLevelSlider.minLabel.text() == "0"
      policyThreatLevelSlider.maxLabel.text() == "10"

    when: 'dashboard filters are applied'
      applicationFiltersDropdown.toggleOption(firstApp.name)
      applicationFiltersDropdown.toggleOption(secondApp.name)
      applicationTagFiltersDropdown.toggleOption(firstAppTag.name)
      stageTypeFiltersDropdown.toggleOption('Release')
      policyThreatFiltersDropdown.toggleOption('Security')
      policyThreatLevelSlider.setValues(2,7)
      applyFilter()

    then: 'filters show up in readonly mode'
      waitFor { filterPanel.displayed }
      applicationFilters.collect{it.text()}.join('') == firstApp.name + ',' + secondApp.name
      applicationTagFilters.text() == firstAppTag.name
      stageTypeFilters.text() == 'Release'
      policyThreatTypeFilters.text() == 'Security'
      policyThreatLevelFilters.text() == '2 through 7'
  }

  def 'Filter reset'() {
    when: 'clicking the filter toggle button'
      filterPanelToggle.click()

    and: 'Set some filters'
      waitFor { applicationFiltersDropdown.displayed }
      applicationFiltersDropdown.toggleOption(firstApp.name)
      applicationTagFiltersDropdown.toggleOption(firstAppTag.name)
      stageTypeFiltersDropdown.toggleOption('Release')
      policyThreatFiltersDropdown.toggleOption('Security')
      policyThreatLevelSlider.setValues(2,7)

    and: 'reset the filter'
      resetButton.click()

    then: 'filters are empty'
      applicationFiltersDropdown.isEmpty()
      applicationTagFiltersDropdown.isEmpty()
      stageTypeFiltersDropdown.isEmpty()
      policyThreatFiltersDropdown.isEmpty()
      policyThreatLevelSlider.minValue.text() == '2'
      policyThreatLevelSlider.maxValue.text() == '10'
  }

  def 'Single value threat level slider filter'() {
    when: 'clicking the filter toggle button'
      filterPanelToggle.click()

    then: 'the policy threat level slider is shown'
      waitFor { policyThreatLevelSlider.slider.displayed }

    when: 'threat level filter is applied'
      policyThreatLevelSlider.setValues(4,4)
      applyFilter()

    then: 'filter text shows one value'
      waitFor { policyThreatLevelFilters.text() == '4' }
  }

  def 'Unknown components have popover displaying pathnames'() {
    when: 'newest risk table is shown'
      tabLinks.newestRiskTabButton.click()
      waitFor { newestViolationTable.displayed }
      waitFor { newestViolationTable.rows.size() >= 2 }
      def unknownComponentCell = $(newestViolationTable.rows[0].cell(ThreatTableRow.COMPONENT)).firstElement()
      interact {
        moveToElement(unknownComponentCell)
      }
      waitFor { unknownComponentPopover.displayed }

    then: 'newest risk popover is properly displayed'
      unknownComponentPopoverTitle == 'Component Path'
      unknownComponentPopoverText == 'unknown.jar'
  }

  def 'Newest Risk table can be sorted by age'() {
    when: 'the newest risk table is shown'
      waitFor{ newestViolationTable.displayed }
      waitFor { newestViolationTable.rows.size() >= 2 }

    then: 'risks are sorted by descending threat level, with the most recent results shown first'
      ThreatTableRow rowOne = newestViolationTable.rows[0]
      ThreatTableRow rowTwo = newestViolationTable.rows[1]
      rowOne.risk == 10

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
      rowTwo.risk == 5
      rowTwo.buildAge == '7d'
      rowTwo.buildAge == rowTwo.age
      rowTwo.stageReleaseAge == '14d'

    and: 'the build stage is marked as fail'
      rowTwo.isMarkedAsFail(BuildStageType.ID)

    and: 'the build stage is marked as most recent'
      rowTwo.isLatestRisk(BuildStageType.ID)

    and: 'the stage release stage is marked as warn'
      rowTwo.isMarkedAsWarn(StageReleaseStageType.ID)

    and: 'the '

    when: 'clicking the AGE header'
      newestViolationTable.ageHeader.click()

    then: 'we should now show the oldest result first'
      newestViolationTable.rows[0].age == '8d'
      newestViolationTable.rows[1].age == '7d'
      newestViolationTable.rows[2].age ==~ RECENT_AGE
  }


  def 'Newest Risk table can be sorted by stage time'() {
    when: 'the newest risk table is shown'
      waitFor{ newestViolationTable.displayed }
      waitFor { newestViolationTable.rows.size() == 3 }

    then: 'the first row has no build stage results'
      newestViolationTable.rows[0].age ==~ RECENT_AGE
      !newestViolationTable.rows[0].buildAge

    when: 'clicking on the first stage header(BUILD in this case)'
      newestViolationTable.clickStageHeader(newestViolationTable.buildHeader)

    then: 'we should now show the oldest stage result first, followed by the empty results'
      waitFor { newestViolationTable.rows[0].buildAge == '7d' }
      !newestViolationTable.rows[1].buildAge
      !newestViolationTable.rows[2].buildAge

    when: 'clicking on the second stage header(STAGE)'
      newestViolationTable.clickStageHeader(newestViolationTable.stageHeader)

    then: 'we should sort the only result in this column to the top'
      waitFor { newestViolationTable.rows[0].stageReleaseAge == '14d' }
      !newestViolationTable.rows[1].stageReleaseAge
      !newestViolationTable.rows[2].stageReleaseAge

    when: 'clicking on the third stage header(RELEASE)'
      newestViolationTable.clickStageHeader(newestViolationTable.releaseHeader)

    then: 'we should sort the two results in this column to the top, ordered with most recent first'
      waitFor { newestViolationTable.rows[0].releaseAge ==~ RECENT_AGE }
      newestViolationTable.rows[1].releaseAge == '8d'
      !newestViolationTable.rows[2].releaseAge

    when: 'clicking the third stage header again'
      newestViolationTable.clickStageHeader(newestViolationTable.releaseHeader)

    then: 'the results should be sorted in reverse, with empty values at the end'
      waitFor { newestViolationTable.rows[0].releaseAge == '8d' }
      newestViolationTable.rows[1].releaseAge ==~ RECENT_AGE
      !newestViolationTable.rows[2].releaseAge
  }

  def 'Newest Risk Table can be filtered'() {
    when: 'newest risk table is shown'
      waitFor { newestViolationTable.displayed }
      waitFor { newestViolationTable.rows.size() == 3 }

    then: 'policy violations are listed by age and then threat level'
      !noDataAvailable.displayed

      newestViolationTable.rows[0].risk == 10
      newestViolationTable.rows[0].policy == 'DashboardSpecPolicy'
      newestViolationTable.rows[0].application == secondApp.name
      newestViolationTable.rows[0].component == 'unknown.jar'
      newestViolationTable.rows[0].age ==~ RECENT_AGE

      newestViolationTable.rows[1].risk == 5
      newestViolationTable.rows[1].policy == 'DashboardSpecPolicy'
      newestViolationTable.rows[1].application == firstApp.name
      newestViolationTable.rows[1].component == ["Group1", "Artifact1", "Version1"].join(' : ')
      newestViolationTable.rows[1].age == '7d'

    when: 'filtering to an application'
      filterPanelToggle.click()
      waitFor { applicationFiltersDropdown.displayed }
      applicationFiltersDropdown.toggleOption(secondApp.name)
      applyFilter()

    then: 'only violations from that application are shown'
      waitFor { newestViolationTable.rows.size() == 1 }
      !applicationFiltersDropdown.displayed
      newestViolationTable.rows[0].risk == 10
      newestViolationTable.rows[0].policy == 'DashboardSpecPolicy'
      newestViolationTable.rows[0].application == secondApp.name
      newestViolationTable.rows[0].component == 'unknown.jar'
      newestViolationTable.rows[0].age ==~ RECENT_AGE

    when: 'filtering to a stage'
      filterPanelToggle.click()
      waitFor { stageTypeFiltersDropdown.displayed }
      stageTypeFiltersDropdown.toggleOption('Release')
      applyFilter()

    then: 'only violations from that stage are shown'
      waitFor { newestViolationTable.rows.size() == 1 }
      !stageTypeFiltersDropdown.displayed
      newestViolationTable.rows[0].risk == 10
      newestViolationTable.rows[0].policy == 'DashboardSpecPolicy'
      newestViolationTable.rows[0].application == secondApp.name
      newestViolationTable.rows[0].component == 'unknown.jar'
      newestViolationTable.rows[0].age ==~ RECENT_AGE

    and: 'only release stage is visible'
      !newestViolationTable.buildHeader.displayed
      !newestViolationTable.stageHeader.displayed
      newestViolationTable.releaseHeader.displayed
      !newestViolationTable.operateHeader.displayed
  }

  def 'Newest Risk Table shows stages in chronological order'() {
    when: 'newest risk table is shown'
      waitFor { newestViolationTable.displayed }

    then: 'the table header lists the stages in proper order'
      waitFor { newestViolationTable.headers[5..8]*.@id == [ 'stage-header-build', 'stage-header-stage-release', 'stage-header-release', 'stage-header-operate' ] }
  }

  def 'Filter out all results'() {
    when: 'selecting filters that match no results on the newest risk tab'
      filterPanelToggle.click()
      waitFor { policyThreatFiltersDropdown.displayed }
      policyThreatFiltersDropdown.toggleOption('Security')
      applyFilter()

    then: 'the table is replaced by no result text'
      waitFor { noDataAvailable.displayed }

    and: 'newest risk no data text is shown'
      noDataAvailable.text() == "No data available in the last 30 days given the applied filters and available permissions.";

    when: 'user clicks the by component tab'
      tabLinks.componentsTabButton.click()

    then: 'the table is replaced by no result text'
      waitFor { noDataAvailable.displayed }

    when: 'user clicks the by application tab'
      tabLinks.applicationsTabButton.click()

    then: 'the table is replaced by no result text'
      waitFor { noDataAvailable.displayed }
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

    then: 'Components tab heat map is shown'
      waitFor { componentViolationsTable.rows.size() == 4 }
      ComponentViolationsTableRow firstComponentRow = componentViolationsTable.rows[0]
      ComponentViolationsTableRow secondComponentRow = componentViolationsTable.rows[1]
      ComponentViolationsTableRow thirdComponentRow = componentViolationsTable.rows[2]
      ComponentViolationsTableRow fourthComponentRow = componentViolationsTable.rows[3]

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

      that parseAlpha(firstComponentRow.lowRisk.attr('style')), closeTo(alphaCalc(0, 0), TOLERANCE)
      that parseAlpha(secondComponentRow.lowRisk.attr('style')), closeTo(alphaCalc(0, 0), TOLERANCE)
      that parseAlpha(thirdComponentRow.lowRisk.attr('style')), closeTo(alphaCalc(0, 0), TOLERANCE)
      that parseAlpha(fourthComponentRow.lowRisk.attr('style')), closeTo(alphaCalc(0, 0), TOLERANCE)

    when: 'Switching to Applications Tab'
      tabLinks.applicationsTabButton.click()

    then: 'Applications tab heat map is shown'
      waitFor { applicationViolationsTable.rows.size() == 5 }

      ApplicationViolationsTableRow firstApplicationRow = applicationViolationsTable.rows[0]
      ApplicationViolationsTableRow secondApplicationRow = applicationViolationsTable.rows[1]
      ApplicationViolationsTableRow thirdApplicationRow = applicationViolationsTable.rows[2]
      ApplicationViolationsTableRow fourthApplicationRow = applicationViolationsTable.rows[3]
      ApplicationViolationsTableRow fifthApplicationRow = applicationViolationsTable.rows[4]

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

      that parseAlpha(firstApplicationRow.lowRisk.attr('style')), closeTo(alphaCalc(0, 0), TOLERANCE)
      that parseAlpha(secondApplicationRow.lowRisk.attr('style')), closeTo(alphaCalc(0, 0), TOLERANCE)
      that parseAlpha(thirdApplicationRow.lowRisk.attr('style')), closeTo(alphaCalc(0, 0), TOLERANCE)
      that parseAlpha(fourthApplicationRow.lowRisk.attr('style')), closeTo(alphaCalc(0, 0), TOLERANCE)
      that parseAlpha(fifthApplicationRow.lowRisk.attr('style')), closeTo(alphaCalc(0, 0), TOLERANCE)

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
      waitFor { newestViolationTable.rows[0].displayed }
      int tableBottom = newestViolationTable.y + newestViolationTable.height

      // It is a reasonable expectation that the first 5 rows will render within 500 px in every browser
      for (i in 0..5)
        assert newestViolationTable.rows[i].y < tableBottom
      // And that rows 44-49 will not render within the scroll view
      for (i in 44..49)
        assert newestViolationTable.rows[i].y > tableBottom

    and: 'A message is displayed to show that only the top results are shown'
      newestViolationTable.maxResults.text() == 'Showing the newest 100 results'

    cleanup:
      PolicyEvaluationDAO dao = new PolicyEvaluationDAO().delete(policyEvaluation)
  }

  def 'Filters stored as expected'() {
    when: 'dashboard filters are applied'
      filterPanelToggle.click()
      waitFor { applicationFiltersDropdown.displayed }
      policyThreatFiltersDropdown.displayed
      applicationFiltersDropdown.toggleOption(firstApp.name)
      applicationFiltersDropdown.toggleOption(secondApp.name)
      applicationTagFiltersDropdown.toggleOption(firstAppTag.name)
      policyThreatFiltersDropdown.toggleOption('Security')
      policyThreatFiltersDropdown.toggleOption('Other')
      stageTypeFiltersDropdown.toggleOption('Release')
      applyFilter()

    then: 'filters are stored to disk'
      DashboardFilterDTO dto = new ObjectMapper().readValue(new DashboardFilterDAO().getByUsername("admin").filter, DashboardFilterDTO.class);
      dto.applicationFilters.contains(firstApp.publicId)
      dto.applicationFilters.contains(secondApp.publicId)
      dto.tagFilters.contains(firstAppTag.id)
      dto.policyThreatCategoryFilters.contains(PolicyThreatCategory.SECURITY)
      dto.policyThreatCategoryFilters.contains(PolicyThreatCategory.OTHER)
      dto.stageTypeFilters.contains('release')
  }

  def 'Stored filters loaded on view of dashboard'() {
    setup: 'Add filter for admin user'
      DashboardFilterDTO dto = new DashboardFilterDTO()
      dto.applicationFilters = [firstApp.publicId, secondApp.publicId]
      dto.maxPolicyThreatLevel = 6
      dto.minPolicyThreatLevel = 3
      dto.policyThreatCategoryFilters = [PolicyThreatCategory.SECURITY, PolicyThreatCategory.OTHER]
      dto.stageTypeFilters = [Stage.ID_RELEASE]
      dto.tagFilters = [firstAppTag.id]

      temporaryEntity.updateDashboardFilter('admin', new ObjectMapper().writeValueAsString(dto));

    when: 'Refresh the page to reload the filters'
      driver.navigate().refresh()

    then: 'See proper values set in the filters'
      waitFor { applicationFilters.displayed }
      applicationFilters.collect { it.text() }.join('') == firstApp.name + ',' + secondApp.name
      applicationTagFilters.text() == firstAppTag.name
      stageTypeFilters.text() == 'Release'
      policyThreatTypeFilters.collect { it.text() }.join('') == 'Security,Other'
      policyThreatLevelFilters.text() == '3 through 6'
  }

  def 'Components Table'() {
    when: 'Switch to Components Tab'
      tabLinks.componentsTabButton.click()

    then: 'Component Table Displayed'
      waitFor { componentViolationsTable.rows.size() == 1 }
      componentViolationsTable.rows[0].component.text() == "Group1 : Artifact1 : Version1"
      componentViolationsTable.rows[0].affectedApplications.text() == "2"
      componentViolationsTable.rows[0].affectedApplicationsLink.displayed
      componentViolationsTable.rows[0].netRisk.text() == "15"
      componentViolationsTable.rows[0].criticalRisk.text() == "10"
      componentViolationsTable.rows[0].severeRisk.text() == "5"
      componentViolationsTable.rows[0].moderateRisk.text() == "0"
      componentViolationsTable.rows[0].lowRisk.text() == "0"
      componentViolationsTable.rows[0].componentLink.displayed

    when: 'clicking the component link'
      componentViolationsTable.rows[0].componentLink.click()

    then: 'the component drilldown page is shown'
      at ComponentDrilldownPage

    cleanup: 'return to the overview page; hack to ensure we are refreshing the correct page in setup() for next feature'
      to DashboardOverviewPage
  }

  def 'Applications Table'() {
    when: 'Switch to Applications Tab'
      tabLinks.applicationsTabButton.click()
    then:
      waitFor { applicationViolationsTable.rows.size() == 2 }
      applicationViolationsTable.rows[0].application.text() == secondApp.getName()
      applicationViolationsTable.rows[0].netRisk.text() == "10"
      applicationViolationsTable.rows[0].criticalRisk.text() == "10"
      applicationViolationsTable.rows[0].severeRisk.text() == "0"
      applicationViolationsTable.rows[0].moderateRisk.text() == "0"
      applicationViolationsTable.rows[0].lowRisk.text() == "0"
      applicationViolationsTable.rows[0].expand.displayed
      applicationViolationsTable.rows[1].application.text() == firstApp.getName()
      applicationViolationsTable.rows[1].netRisk.text() == "5"
      applicationViolationsTable.rows[1].criticalRisk.text() == "0"
      applicationViolationsTable.rows[1].severeRisk.text() == "5"
      applicationViolationsTable.rows[1].moderateRisk.text() == "0"
      applicationViolationsTable.rows[1].lowRisk.text() == "0"
      applicationViolationsTable.rows[1].expand.displayed

    when: 'Expand'
      applicationViolationsTable.rows[0].expand.click()
    then: 'Stage shown'
      waitFor { applicationViolationsTable.rows.size() == 3 }
      applicationViolationsTable.rows[0].collapse.displayed
      applicationViolationsTable.rows[1].application.text() == new ReleaseStageType().getName().toUpperCase()
      applicationViolationsTable.rows[1].reportLink.displayed

    when: 'Expand'
      applicationViolationsTable.rows[2].expand.click()

    then: 'stages shown in chronological order'
      waitFor { applicationViolationsTable.rows.size() == 6 }
      applicationViolationsTable.rows[2].collapse.displayed
      applicationViolationsTable.rows[3].application.text() == new BuildStageType().getName().toUpperCase()
      applicationViolationsTable.rows[3].reportLink.displayed
      applicationViolationsTable.rows[4].application.text() == new StageReleaseStageType().getName().toUpperCase()
      applicationViolationsTable.rows[4].reportLink.displayed
      applicationViolationsTable.rows[5].application.text() == new ReleaseStageType().getName().toUpperCase()
      applicationViolationsTable.rows[5].reportLink.displayed

    and: 'the stage label links to the underlying report'
      withNewWindow(page: ReportContainerPage, { applicationViolationsTable.rows[3].reportLink.click() } ) {
        verifyAt()
        reportTitle.text()
      }  ==~ firstApp.getName() + ' .* Build Report'
  }

  def 'Dashboard Filter Summary'() {
    when: 'the filter summary data is loaded'
      waitFor { summaryData.displayed }

    then: 'the count of total applications is shown'
      summaryTotalApplications.displayed
      summaryTotalApplications.text() == '2'

    and: 'the count of matched applications is shown'
      summaryMatchedApplications.displayed
      summaryMatchedApplications.text() == '2'

    and: 'the percentage of matched applications is shown'
      summaryPercentApplications.displayed
      summaryPercentApplications.text() == '100%'

    and: 'the count of total policies is shown'
      summaryTotalPolicies.displayed
      summaryTotalPolicies.text() == '1'

    and: 'the count of matched policies is shown'
      summaryMatchedPolicies.displayed
      summaryMatchedPolicies.text() == '1'

    and: 'the percentage of matched policies is shown'
      summaryPercentPolicies.displayed
      summaryPercentPolicies.text() == '100%'

    and: 'the count of total components is shown'
      summaryTotalComponents.displayed
      summaryTotalComponents.text() == '3'

    and: 'the count of matched components is shown'
      summaryMatchedComponents.displayed
      summaryMatchedComponents.text() == '3'

    and: 'the percentage of matched components is shown'
      summaryPercentComponents.displayed
      summaryPercentComponents.text() == '100%'
  }

  def 'Dashboard component match summary'() {
    when: 'the component match summary is shown'
      waitFor { componentMatchSection.displayed }

    then: 'the count of exact match components is shown'
      componentMatchExactCount.displayed
      componentMatchExactCount.text() == '1 (33%)'

    and: 'the count of similar match components is shown'
      componentMatchSimilarCount.displayed
      componentMatchSimilarCount.text() == '1 (33%)'

    and: 'the count of unknown components is shown'
      componentMatchUnknownCount.displayed
      componentMatchUnknownCount.text() == '1 (33%)'
  }

  def 'Heat Map Help Modal' () {
    when: 'component heat map help icon is clicked'
      clickComponentHeatMapHelp()
    
    then: 'component heat map help is displayed'
      waitFor { componentHeatMapHelp.displayed }
    
    when: 'the modal backdrop is clicked'
      waitFor { componentHeatMapHelp.displayed }
      modalBackdrop.click()
      
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
      waitFor { applicationHeatMapHelp.displayed }
      modalBackdrop.click()

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
