/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.model.Application
import com.sonatype.insight.brain.model.Organization
import com.sonatype.insight.brain.model.policy.Policy
import com.sonatype.insight.brain.model.policy.PolicyEvaluation
import com.sonatype.insight.brain.model.policy.stages.BuildStageType
import spock.lang.Stepwise

import static spock.util.matcher.HamcrestMatchers.closeTo
import static spock.util.matcher.HamcrestSupport.that

/**
 * Dashboard tests specific to the Policy Summary section, which requires more data to fully test than is necessary
 * for the rest of the page.
 * @since 1.11
 */
@Stepwise
class DashboardPolicySummarySpec
    extends BaseSpec
{

  static Organization org

  static Application app

  static Policy policy

  // Tolerance for svg calculations
  static final BigDecimal TOLERANCE = 0.05

  static final List<Map> COMPONENTS = (1..10).inject([]) { List<Map> components, int index ->
    components << [
        groupId   : "group-$index",
        artifactId: "artifact-$index",
        version   : index.toString(),
        hash      : index.toString(),
        pathnames : "pathname-$index"
    ]
    return components
  }.asImmutable()

  static final List<Integer> NEW_ROW_DELTAS = [2, 0, 0, 2, 0, 0, 0, 1, 0, 1, 0, 1]
  static final List<Integer> FIXED_ROW_DELTAS  = [0, 0, 0, 1, 0, 0, 0, 1, 2, 1, 0, 3]
  static final List<Integer> UNRESOLVED_ROW_DELTAS = [2, 0, 0, 1, 0, 0, 0, 0, -2, 0, 0, -2]

  def setupSpec() {
    org = temporaryEntity.newOrganization('DashboardPolicySummarySpec')
    app = temporaryEntity.newApplication('DashboardPolicySummarySpecApp', 'DashboardPolicySummarySpecApp', org.id)
    policy = temporaryEntity.newPolicy(org.id, 'DashboardPolicySummarySpec')

    Date now = new Date()
    (12..0).each { int weeksAgo ->
      Date time = now.minus(7 * weeksAgo)
      switch (weeksAgo) {
        case 12: // introduce 3 violations outside the bounds of the 12 week delta to start with
          PolicyEvaluation seedEval = temporaryEntity.newPolicyEvaluation(app.id, BuildStageType.ID, 'SeedEval', time)
          createViolations(seedEval, COMPONENTS[0..2])
          break
        case 11:
          // introduce 2 new violations
          PolicyEvaluation twelthWeekEval = temporaryEntity.
              newPolicyEvaluation(app.id, BuildStageType.ID, 'twelthWeekEval', time)
          createViolations(twelthWeekEval, COMPONENTS[0..4])
          break
        case 10..9: // nothing happens these weeks
          break
        case 8: // fix an issue and introduce 2 new ones
          PolicyEvaluation ninthWeekEval = temporaryEntity.
              newPolicyEvaluation(app.id, BuildStageType.ID, 'ninthWeekEval', time)
          createViolations(ninthWeekEval, COMPONENTS[1..6])
          break
        case 7..5: // nothing happens these weeks
          break
        case 4: // find one, fix one
          PolicyEvaluation fifthWeekEval = temporaryEntity.
              newPolicyEvaluation(app.id, BuildStageType.ID, 'fifthWeekEval', time)
          createViolations(fifthWeekEval, COMPONENTS[2..7])
          break
        case 3: // fix two
          PolicyEvaluation fourthWeekEval = temporaryEntity.
              newPolicyEvaluation(app.id, BuildStageType.ID, 'fourthWeekEval', time)
          createViolations(fourthWeekEval, COMPONENTS[4..7])
          break
        case 2: // find one, fix one
          PolicyEvaluation thirdWeekEval = temporaryEntity.
              newPolicyEvaluation(app.id, BuildStageType.ID, 'thirdWeekEval', time)
          createViolations(thirdWeekEval, COMPONENTS[5..8])
          break
        case 1: // nothing happens this week
          break
        case 0: // finish strong by fixing all but one existing problem and adding a new one
          PolicyEvaluation thisWeekEval = temporaryEntity.
              newPolicyEvaluation(app.id, BuildStageType.ID, 'thisWeekEval', time)
          createViolations(thisWeekEval, COMPONENTS[8..9])
          break
      }
    }
    loginAsAdminVia(DashboardOverviewPage)
  }

  def 'The policy summary view should show 12 weeks of data'() {
    when: 'The data is loaded'
      waitFor { policySummary.rows.size() == 3 }
      PolicySummaryModule policySummary = policySummary;

    then: 'The expected categories are shown'
      policySummary.discoveredRow.category== 'Discovered'
      policySummary.fixedRow.category == 'Fixed'
      policySummary.pendingRow.category== 'Pending'

    and: 'The counts for each category are shown'
      policySummary.discoveredRow.count == 10
      policySummary.fixedRow.count == 8
      policySummary.pendingRow.count == 2

    and: 'The deltas for each category are shown'
      policySummary.discoveredRow.delta.value == 7
      policySummary.fixedRow.delta.value == 8
      policySummary.pendingRow.delta.value == -1

    and: 'The deltas for each category are styled properly'
      policySummary.discoveredRow.delta.isUp
      policySummary.discoveredRow.delta.isNegative
      policySummary.fixedRow.delta.isUp
      policySummary.fixedRow.delta.isPositive
      policySummary.pendingRow.delta.isDown
      policySummary.pendingRow.delta.isPositive

    and: 'The bar charts show the expected values'
      policySummary.discoveredRow.barChart.points == NEW_ROW_DELTAS
      policySummary.fixedRow.barChart.points == FIXED_ROW_DELTAS
      policySummary.pendingRow.barChart.points == UNRESOLVED_ROW_DELTAS

    and: 'The sparkline charts show the expected values'
      isDeltaArrayCloseToSum(3, NEW_ROW_DELTAS, policySummary.discoveredRow.sparkline.getValues())
      isDeltaArrayCloseToSum(0, FIXED_ROW_DELTAS, policySummary.fixedRow.sparkline.getValues())
      isDeltaArrayCloseToSum(3, UNRESOLVED_ROW_DELTAS, policySummary.pendingRow.sparkline.getValues())
  }

  def 'Filtering out all data should show an empty policy summary'() {
    when: 'clicking the filter toggle button'
      PolicySummaryModule policySummary = policySummary;
      filterPanelToggle.click()

    then: 'the dashboard filters are shown'
      waitFor { applicationFiltersDropdown.displayed }
      policyThreatFiltersDropdown.displayed

    when: 'we select a policy threat type that we have no violations for'
      policyThreatFiltersDropdown.toggleOption('License')
      applyFilter()
    
    then: 'the policy summary should be empty'
      waitFor { filterPanel.displayed }
      policySummary.discoveredRow.category== 'Discovered'
      policySummary.fixedRow.category == 'Fixed'
      policySummary.pendingRow.category == 'Pending'

    and: 'The counts for each category should all be zero'
      policySummary.discoveredRow.count == 0
      policySummary.fixedRow.count == 0
      policySummary.pendingRow.count == 0

    and: 'The deltas for each category should all be zero'
      policySummary.discoveredRow.delta.value == 0
      policySummary.fixedRow.delta.value == 0
      policySummary.pendingRow.delta.value == 0

    and: 'The deltas for each category should have no styling'
      [policySummary.discoveredRow.delta, policySummary.fixedRow.delta, policySummary.pendingRow.delta].each{ DeltaModule delta ->
        assert !delta.isUp
        assert !delta.isDown
        assert !delta.isPositive
        assert !delta.isNegative
      }

    and: 'The bar chart points should all be zero'
      def emptyPoints = (0..11).collect { 0 }
      policySummary.discoveredRow.barChart.points == emptyPoints
      policySummary.fixedRow.barChart.points == emptyPoints
      policySummary.pendingRow.barChart.points == emptyPoints

    and: 'The sparkline points should all be zero'
      isDeltaArrayCloseToSum(0, emptyPoints, policySummary.discoveredRow.sparkline.getValues())
      isDeltaArrayCloseToSum(0, emptyPoints, policySummary.fixedRow.sparkline.getValues())
      isDeltaArrayCloseToSum(0, emptyPoints, policySummary.pendingRow.sparkline.getValues())
  }


  private void createViolations(PolicyEvaluation evaluation, List<Map> components) {
    components.each { Map component ->
      temporaryEntity.
          newPolicyViolation(evaluation, policy, component.groupId, component.artifactId, component.version,
              component.hash, '')
    }
  }

  private static void isDeltaArrayCloseToSum(startingValue, expectedDeltaArray, actualSumArray) {
    def sum = [startingValue]
    expectedDeltaArray.eachWithIndex { int delta, int i -> sum << (startingValue + expectedDeltaArray[0..i].sum()) }
    def max = sum.max()
    sum = sum.collect { max == 0 ? 0 : it / max }
    sum.eachWithIndex { BigDecimal expected, int i ->
      assert that(actualSumArray[i], closeTo(expected, TOLERANCE))
    }
  }
}
