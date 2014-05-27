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
import com.sonatype.insight.brain.model.policy.Policy
import com.sonatype.insight.brain.model.policy.PolicyEvaluation
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory
import com.sonatype.insight.brain.model.policy.PolicyViolation
import com.sonatype.insight.brain.model.policy.actions.FailActionType
import com.sonatype.insight.brain.model.policy.actions.WarnActionType
import com.sonatype.insight.brain.model.policy.stages.BuildStageType
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType
import com.sonatype.insight.brain.model.policy.stages.StageReleaseStageType
import com.sonatype.insight.brain.model.tag.Tag
import com.sonatype.insight.brain.service.InsightWork
import com.sonatype.insight.brain.testing.functional.modules.ThreatTableRow
import com.sonatype.insight.brain.testing.functional.report.violation.ReportContainerPage
import org.codehaus.plexus.util.FileUtils

/**
 * @since 1.11
 */
class DashboardOverviewSpec
  extends BaseSpec
{
  static final String RECENT_AGE = /[1-9]{1}min/

  static final String alphaMatcher = /background-color: rgba\([0-9][0-9][0-9]?, [0-9][0-9][0-9]?, [0-9][0-9][0-9]?, (.*)\);/
  boolean assertAlpha(style, percentage) {
    // Different browsers render opacity differently. Epsilon 0.05 provides an accurate comparison of alpha.
    Math.abs((style =~ alphaMatcher)[0][1].toBigDecimal() - percentage) < 0.05
  }

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
    temporaryEntity.newNewestPolicyViolation(firstViolation.id, firstPolicyEvaluation.applicationId,
        firstPolicyEvaluation.stageTypeId)
    temporaryEntity.newApplicationComponent(firstPolicyEvaluation.applicationId, firstPolicyEvaluation.stageTypeId,
        firstViolation.hash, firstViolation.groupId, firstViolation.artifactId, firstViolation.version)

    //same policy as first evaluation, but a different stage and earlier
    PolicyEvaluation firstPolicyEvaluationSecondStage = temporaryEntity.newPolicyEvaluation(firstApp.id,
        StageReleaseStageType.ID, 'DashboardSpecFirstEvaluationSecondStage', now - 14)
    PolicyViolation firstViolationSecondStage = temporaryEntity.newPolicyViolation(firstPolicyEvaluationSecondStage,
        policy, 5, PolicyThreatCategory.LICENSE, "Group1", "Artifact1", "Version1", "hash", WarnActionType.ID)
    temporaryEntity.newNewestPolicyViolation(firstViolationSecondStage.id,
        firstPolicyEvaluationSecondStage.applicationId, firstPolicyEvaluationSecondStage.stageTypeId)
    temporaryEntity.newApplicationComponent(firstPolicyEvaluationSecondStage.applicationId,
        firstPolicyEvaluationSecondStage.stageTypeId, firstViolationSecondStage.hash,
        firstViolationSecondStage.groupId, firstViolationSecondStage.artifactId,
        firstViolationSecondStage.version)

    //most recent evaluation
    PolicyEvaluation secondPolicyEvaluation = temporaryEntity.newPolicyEvaluation(secondApp.id, ReleaseStageType.ID,
        'DashboardSpecSecondEvaluation', now)
    PolicyViolation secondViolation = temporaryEntity.newPolicyViolation(secondPolicyEvaluation, policy, 10,
            PolicyThreatCategory.QUALITY, null, null, null)
    temporaryEntity.newNewestPolicyViolation(secondViolation.id, secondPolicyEvaluation.applicationId,
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

    and: 'stage type filters are shown'
      stageTypeFiltersDropdown.showDropdown()
      stageTypeFiltersDropdown.dropdownCheck('Build').displayed
      !stageTypeFiltersDropdown.dropdownItem('Develop').present
      stageTypeFiltersDropdown.dropdownCheck('Release').displayed
      stageTypeFiltersDropdown.dropdownCheck('Stage Release').displayed
      stageTypeFiltersDropdown.dropdownCheck('Operate').displayed
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
      waitFor { newestViolationTable.unknownComponentPopover.displayed }

    then: 'newest risk popover is properly displayed'
      newestViolationTable.unknownComponentPopoverTitle == 'Component Path'
      newestViolationTable.unknownComponentPopoverText == 'unknown.jar'
  }

  def 'Newest Risk table can be sorted by age'() {
    when: 'the newest risk table is shown'
      waitFor{ newestViolationTable.displayed }
      waitFor { newestViolationTable.rows.size() >= 2 }

    then: 'risks are sorted by descending threat level, with the most recent results shown first'
      newestViolationTable.rows[0].risk == 10
      !newestViolationTable.rows[0].buildAge
      !newestViolationTable.rows[0].operateAge
      newestViolationTable.rows[0].releaseAge ==~ RECENT_AGE
      newestViolationTable.rows[0].releaseAge == newestViolationTable.rows[0].age
      !newestViolationTable.rows[0].stageReleaseAge
      newestViolationTable.rows[1].risk == 5
      newestViolationTable.rows[1].buildAge == '7d'
      newestViolationTable.rows[1].buildAge == newestViolationTable.rows[1].age
      newestViolationTable.rows[1].stageReleaseAge == '14d'

    when: 'clicking the AGE header'
      newestViolationTable.ageHeader.click()

    then: 'we should now show the oldest result first'
      newestViolationTable.rows[0].age == '7d'
      newestViolationTable.rows[1].age ==~ RECENT_AGE
  }

  def 'Newest Risk Table can be filtered'() {
    when: 'newest risk table is shown'
      waitFor { newestViolationTable.displayed }
      waitFor { newestViolationTable.rows.size() == 2 }

    then: 'policy violations are listed by threat level'
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
  }

  def 'Filter out all results'() {
    when: 'selecting filters that match no results on the newest risk tab'
      filterPanelToggle.click()
      waitFor { policyThreatFiltersDropdown.displayed }
      policyThreatFiltersDropdown.toggleOption('Security')
      policyThreatFiltersDropdown.toggleOption('Other')
      applyFilter()

    then: 'the table is replaced by no result text'
      waitFor { noDataAvailable.displayed }

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
    setup: 'Add a few records'
      List<Application> applications = new ArrayList<Application>();
      for (i in 0..2) {
        Date now = new Date()
        def application = temporaryEntity.newApplication("DashboardSpecApp${i}", "DashboardSpecApp${i}", org.id)
        applications.add(application)
        def policyEvaluation = temporaryEntity.newPolicyEvaluation(application.id, BuildStageType.ID,
          "DashboardSpecFirstEvaluation${i}", now - 7)
        def violation = temporaryEntity.newPolicyViolation(policyEvaluation, policy, 4 * i,
          PolicyThreatCategory.SECURITY, "Group${i}", "Artifact${i}", "Version${i}", "hash${i}")
        temporaryEntity.newNewestPolicyViolation(violation.id, policyEvaluation.applicationId,
          policyEvaluation.stageTypeId)
      }

    when: 'Switching to Components Tab'
      tabLinks.componentsTabButton.click()

    then: 'Components tab heat map is shown'
      waitFor { componentViolationsTable.rows.size() == 4 }
      assertAlpha(componentViolationsTable.rows[1].netRisk.attr('style'), 8d/15)
      assertAlpha(componentViolationsTable.rows[2].netRisk.attr('style'), 4d/15)
      assertAlpha(componentViolationsTable.rows[3].netRisk.attr('style'), 0)
      assertAlpha(componentViolationsTable.rows[1].criticalRisk.attr('style'), 8d/10)
      assertAlpha(componentViolationsTable.rows[2].criticalRisk.attr('style'), 0)
      assertAlpha(componentViolationsTable.rows[3].criticalRisk.attr('style'), 0)
      assertAlpha(componentViolationsTable.rows[1].severeRisk.attr('style'), 0)
      assertAlpha(componentViolationsTable.rows[2].severeRisk.attr('style'), 4d/5)
      assertAlpha(componentViolationsTable.rows[3].severeRisk.attr('style'), 0)
      assertAlpha(componentViolationsTable.rows[1].moderateRisk.attr('style'), 0)
      assertAlpha(componentViolationsTable.rows[2].moderateRisk.attr('style'), 0)
      assertAlpha(componentViolationsTable.rows[3].moderateRisk.attr('style'), 0)
      assertAlpha(componentViolationsTable.rows[1].lowRisk.attr('style'), 0)
      assertAlpha(componentViolationsTable.rows[2].lowRisk.attr('style'), 0)
      assertAlpha(componentViolationsTable.rows[3].lowRisk.attr('style'), 0)

    when: 'Switching to Applications Tab'
      tabLinks.applicationsTabButton.click()

    then: 'Applications tab heat map is shown'
      waitFor { applicationViolationsTable.rows.size() == 4 }
      assertAlpha(applicationViolationsTable.rows[1].netRisk.attr('style'), 8d/10)
      assertAlpha(applicationViolationsTable.rows[2].netRisk.attr('style'), 5d/10)
      assertAlpha(applicationViolationsTable.rows[3].netRisk.attr('style'), 4d/10)
      assertAlpha(applicationViolationsTable.rows[1].criticalRisk.attr('style'), 8d/10)
      assertAlpha(applicationViolationsTable.rows[2].criticalRisk.attr('style'), 0)
      assertAlpha(applicationViolationsTable.rows[3].criticalRisk.attr('style'), 0)
      assertAlpha(applicationViolationsTable.rows[0].severeRisk.attr('style'), 0)
      assertAlpha(applicationViolationsTable.rows[1].severeRisk.attr('style'), 0)
      assertAlpha(applicationViolationsTable.rows[3].severeRisk.attr('style'), 4d/5)
      assertAlpha(applicationViolationsTable.rows[1].moderateRisk.attr('style'), 0)
      assertAlpha(applicationViolationsTable.rows[2].moderateRisk.attr('style'), 0)
      assertAlpha(applicationViolationsTable.rows[3].moderateRisk.attr('style'), 0)
      assertAlpha(applicationViolationsTable.rows[1].lowRisk.attr('style'), 0)
      assertAlpha(applicationViolationsTable.rows[2].lowRisk.attr('style'), 0)
      assertAlpha(applicationViolationsTable.rows[3].lowRisk.attr('style'), 0)

    cleanup:
      ApplicationDAO dao = new ApplicationDAO()
      for (Application application : applications) {
        dao.delete(application)
      }
  }

  def 'Limits results to 100 records'() {
    setup: 'Add over 100 records'
      List<PolicyEvaluation> evaluations = new ArrayList<>()
      for (i in 0..100) {
        Date now = new Date()
        PolicyEvaluation policyEvaluation = temporaryEntity.newPolicyEvaluation(firstApp.id, BuildStageType.ID,
            'DashboardSpecFirstEvaluation', now - 7)
        evaluations.add(policyEvaluation)
        PolicyViolation violation = temporaryEntity.
            newPolicyViolation(policyEvaluation, policy, 5, PolicyThreatCategory.SECURITY,
                "Group${i}", "Artifact${i}", "Version${i}")
        temporaryEntity.newNewestPolicyViolation(violation.id, policyEvaluation.applicationId,
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
      newestViolationTable.maxResults.text() == 'Showing the top 100 results'

    cleanup:
      PolicyEvaluationDAO dao = new PolicyEvaluationDAO()
      for (PolicyEvaluation evaluation : evaluations) {
        dao.delete(evaluation)
      }
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

      temporaryEntity.newDashboardFilter('admin', new ObjectMapper().writeValueAsString(dto));

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
    then: 'Stage shown'
      waitFor { applicationViolationsTable.rows.size() == 5 }
      applicationViolationsTable.rows[2].collapse.displayed
      applicationViolationsTable.rows[3].application.text() == new BuildStageType().getName().toUpperCase()
      applicationViolationsTable.rows[3].reportLink.displayed

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
      summaryTotalComponents.text() == '1'

    and: 'the count of matched components is shown'
      summaryMatchedComponents.displayed
      summaryMatchedComponents.text() == '1'

    and: 'the percentage of matched components is shown'
      summaryPercentComponents.displayed
      summaryPercentComponents.text() == '100%'
  }
}
