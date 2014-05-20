/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.fasterxml.jackson.databind.ObjectMapper
import com.sonatype.clm.dto.model.policy.Stage
import com.sonatype.insight.brain.dashboard.DashboardFilterDTO
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO
import com.sonatype.insight.brain.model.Application
import com.sonatype.insight.brain.model.Color
import com.sonatype.insight.brain.model.Organization
import com.sonatype.insight.brain.model.policy.Policy
import com.sonatype.insight.brain.model.policy.PolicyEvaluation
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory
import com.sonatype.insight.brain.model.policy.PolicyViolation
import com.sonatype.insight.brain.model.policy.stages.BuildStageType
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType
import com.sonatype.insight.brain.model.tag.Tag
import com.sonatype.insight.brain.service.InsightWork
import com.sonatype.insight.brain.testing.functional.modules.ThreatTableRow
import com.sonatype.insight.brain.testing.functional.report.violation.ReportContainerPage

import org.codehaus.plexus.util.FileUtils;

/**
 * @since 1.11
 */
class DashboardOverviewSpec
  extends BaseSpec
{
  static final String RECENT_AGE = /.*(seconds|minute|minutes) ago/

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
    PolicyEvaluation firstPolicyEvaluation = temporaryEntity.newPolicyEvaluation(firstApp.id, BuildStageType.ID,
        'DashboardSpecFirstEvaluation', now - 7)
    PolicyViolation firstViolation = temporaryEntity.
        newPolicyViolation(firstPolicyEvaluation, policy, 5,
            PolicyThreatCategory.LICENSE, "Group1", "Artifact1", "Version1")
    temporaryEntity.newNewestPolicyViolation(firstViolation.id, firstPolicyEvaluation.applicationId,
        firstPolicyEvaluation.stageTypeId)
    temporaryEntity.newApplicationComponent(firstPolicyEvaluation.getApplicationId(), firstPolicyEvaluation.getStageTypeId(),
        firstViolation.getHash(), firstViolation.getGroupId(), firstViolation.getArtifactId(), firstViolation.getVersion())

    PolicyEvaluation secondPolicyEvaluation = temporaryEntity.newPolicyEvaluation(secondApp.id, ReleaseStageType.ID,
        'DashboardSpecSecondEvaluation', now)
    PolicyViolation secondViolation = temporaryEntity.
        newPolicyViolation(secondPolicyEvaluation, policy, 10,
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
      policyThreatLevelFilters.text() == 'Policy Threat Levels 2 through 7'
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
      waitFor { policyThreatLevelFilters.text() == 'Policy Threat Level 4' }
  }

  def 'Unknown components have popover displaying pathnames'() {
    when: 'switching to the highest risk table and highest risk table is shown'
      tabLinks.policyViolationsTabButton.click()
      waitFor { highestRiskTable.displayed }
      waitFor { highestRiskTable.rows.size() >= 2 }
      WebElement unknownComponentCell = $(highestRiskTable.rows[0].cell(ThreatTableRow.COMPONENT)).firstElement()
      interact {
        moveToElement(unknownComponentCell)
      }
      waitFor { highestRiskTable.unknownComponentPopover.displayed }

    then: 'highest risk popover is properly displayed'
      highestRiskTable.unknownComponentPopoverTitle == 'Component Path'
      highestRiskTable.unknownComponentPopoverText == 'unknown.jar'

    when: 'newest risk table is shown'
      tabLinks.newestRiskTabButton.click()
      waitFor { newestViolationTable.displayed }
      waitFor { newestViolationTable.rows.size() >= 2 }
      unknownComponentCell = $(newestViolationTable.rows[0].cell(ThreatTableRow.COMPONENT)).firstElement()
      interact {
        moveToElement(unknownComponentCell)
      }
      waitFor { newestViolationTable.unknownComponentPopover.displayed }

    then: 'newest risk popover is properly displayed'
      newestViolationTable.unknownComponentPopoverTitle == 'Component Path'
      newestViolationTable.unknownComponentPopoverText == 'unknown.jar'
  }

  def 'Highest Risk Table can be sorted'() {
    when: 'switching to the highest risk table'
      tabLinks.policyViolationsTabButton.click()

    then: 'highest risk table is shown'
      waitFor { highestRiskTable.displayed }
      waitFor { highestRiskTable.rows.size() >= 2 }

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
      waitFor { newestViolationTable.rows.size() >= 2 }

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
    when: 'switching to the highest risk table'
      tabLinks.policyViolationsTabButton.click()

    then: 'highest risk table is shown'
      waitFor { highestRiskTable.displayed }
      waitFor { highestRiskTable.rows.size() == 2 }

    then: 'policy violations are listed by threat level'
      !noDataAvailable.displayed

      highestRiskTable.rows[0].risk == 10
      highestRiskTable.rows[0].policy == 'DashboardSpecPolicy'
      highestRiskTable.rows[0].application == secondApp.name
      highestRiskTable.rows[0].component == 'unknown.jar'

      highestRiskTable.rows[1].risk == 5
      highestRiskTable.rows[1].policy == 'DashboardSpecPolicy'
      highestRiskTable.rows[1].application == firstApp.name
      highestRiskTable.rows[1].component == ["Group1", "Artifact1", "Version1"].join(' : ')
      highestRiskTable.rows[1].age.contains("ago")

    when: 'filtering to an application'
      filterPanelToggle.click()
      waitFor { applicationFiltersDropdown.displayed }
      applicationFiltersDropdown.toggleOption(firstApp.name)
      applyFilter()

    then: 'only violations from that application are shown'
      waitFor { highestRiskTable.rows.size() == 1 }
      !applicationFiltersDropdown.displayed
      highestRiskTable.rows[0].risk == 5
      highestRiskTable.rows[0].policy == 'DashboardSpecPolicy'
      highestRiskTable.rows[0].application == firstApp.name

    when: 'filtering to a stage'
      filterPanelToggle.click()
      // Toggle off previous application filter.
      waitFor { applicationFiltersDropdown.displayed }
      applicationFiltersDropdown.toggleOption(firstApp.name)
      waitFor { stageTypeFiltersDropdown.displayed }
      stageTypeFiltersDropdown.toggleOption('Release')
      applyFilter()

    then: 'only violations from that stage are shown'
      waitFor { highestRiskTable.rows.size() == 1 }
      !stageTypeFiltersDropdown.displayed
      waitFor { highestRiskTable.rows[0].risk == 10 }
      highestRiskTable.rows[0].policy == 'DashboardSpecPolicy'
      highestRiskTable.rows[0].application == secondApp.name
      highestRiskTable.rows[0].component == 'unknown.jar'
      highestRiskTable.rows[0].age.contains("ago")
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
      newestViolationTable.rows[0].age.contains("ago")

      newestViolationTable.rows[1].risk == 5
      newestViolationTable.rows[1].policy == 'DashboardSpecPolicy'
      newestViolationTable.rows[1].application == firstApp.name
      newestViolationTable.rows[1].component == ["Group1", "Artifact1", "Version1"].join(' : ')
      newestViolationTable.rows[1].age.contains("ago")

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
      newestViolationTable.rows[0].age.contains("ago")

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
      newestViolationTable.rows[0].age.contains("ago")
  }

  def 'Filter out all results'() {
    when: 'selecting filters that match no results'
      filterPanelToggle.click()
      waitFor { policyThreatFiltersDropdown.displayed }
      policyThreatFiltersDropdown.toggleOption('Security')
      policyThreatFiltersDropdown.toggleOption('Other')
      applyFilter()

    then: 'the tables are replaced by text indicating there are no results'
      waitFor { noDataAvailable.displayed }
      filterPanel.displayed
  }

  def 'Limits results to 100 records'() {
    setup: 'Add over 100 records'
      List<PolicyEvaluation> evaluations = new ArrayList<>()
      for (i in 0..100) {
        Date now = new Date()
        PolicyEvaluation policyEvaluation = temporaryEntity.newPolicyEvaluation(firstApp.id, BuildStageType.ID,
            'DashboardSpecFistEvaluation', now - 7)
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
      policyThreatLevelFilters.text() == 'Policy Threat Levels 3 through 6'
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
      applicationViolationsTable.rows[1].application.text() == new ReleaseStageType().getName()

    when: 'Expand'
      applicationViolationsTable.rows[2].expand.click()
    then: 'Stage shown'
      waitFor { applicationViolationsTable.rows.size() == 4 }
      applicationViolationsTable.rows[2].collapse.displayed
      applicationViolationsTable.rows[3].application.text() == new BuildStageType().getName()
  }
}
