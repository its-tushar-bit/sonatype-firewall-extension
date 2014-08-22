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
import com.sonatype.insight.brain.model.policy.PolicyWaiver
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
extends BaseSpec {

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

  static final List<Integer> FIXED_ROW_DELTAS = [0, 0, 0, 1, 0, 0, 1, 1, 3, 1, 0, 1]

  static final List<Integer> UNRESOLVED_ROW_DELTAS = [2, 0, 0, 1, -3, 0, 0, 0, -2, 0, 0, 0]

  static final List<Integer> WAIVED_ROW_DELTAS = [0, 0, 0, 0, 3, 0, -1, 0, -1, 0, 0, 0]

  static final List<PolicyWaiver> existingWaivers = new ArrayList<>();

  def setupSpec() {
    org = temporaryEntity.newOrganization('DashboardPolicySummarySpec')
    app = temporaryEntity.newApplication('DashboardPolicySummarySpecApp', 'DashboardPolicySummarySpecApp', org.id)
    policy = temporaryEntity.newPolicy(org.id, 'DashboardPolicySummarySpec')

    Date now = new Date()
    (12..0).each { int weeksAgo ->
      Date time = now.minus(7 * weeksAgo + 2)
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
        case 7: // Waive 3 violations
          PolicyEvaluation eigthWeekEval = temporaryEntity.
          newPolicyEvaluation(app.id, BuildStageType.ID, 'eightWeekEval', time)
          createViolations(eigthWeekEval, COMPONENTS[4..6])
          createWaivedViolations(eigthWeekEval, COMPONENTS[1..3])
          break
        case 6: // nothing happens this weeks
          break
        case 5: // Fix one waived violation
          PolicyEvaluation fifthWeekEval = temporaryEntity.
          newPolicyEvaluation(app.id, BuildStageType.ID, 'fifthWeekEval', time)
          createViolations(fifthWeekEval, COMPONENTS[4..6])
          createWaivedViolations(fifthWeekEval, COMPONENTS[2..3])
          break;
        case 4: // find one, fix one
          PolicyEvaluation fourthWeekEval = temporaryEntity.
          newPolicyEvaluation(app.id, BuildStageType.ID, 'fourthWeekEval', time)
          createViolations(fourthWeekEval, COMPONENTS[5..7])
          createWaivedViolations(fourthWeekEval, COMPONENTS[2..3])
          break
        case 3: // fix two, fix one waived violation
          PolicyEvaluation thirdWeekEval = temporaryEntity.
          newPolicyEvaluation(app.id, BuildStageType.ID, 'thirdWeekEval', time)
          createViolations(thirdWeekEval, [COMPONENTS[7]])
          createWaivedViolations(thirdWeekEval, [COMPONENTS[3]])
          break
        case 2: // find one, fix one
          PolicyEvaluation secondWeekEval = temporaryEntity.
          newPolicyEvaluation(app.id, BuildStageType.ID, 'secondWeekEval', time)
          createViolations(secondWeekEval, [COMPONENTS[8]])
          createWaivedViolations(secondWeekEval, [COMPONENTS[3]])
          break
        case 1: // nothing happens this week
          break
        case 0: // find one, fix one
          PolicyEvaluation thisWeekEval = temporaryEntity.
          newPolicyEvaluation(app.id, BuildStageType.ID, 'thisWeekEval', time)
          createViolations(thisWeekEval, [COMPONENTS[9]])
          createWaivedViolations(thisWeekEval, [COMPONENTS[3]])
          break
      }
    }
    loginAsAdminVia(DashboardOverviewPage)
  }

  def 'The policy summary view should show 12 weeks of data'() {
    when: 'The data is loaded'
    waitFor { policySummary.rows.size() == 4 }
    PolicySummaryModule policySummary = policySummary;

    then: 'The expected categories are shown'
    policySummary.discoveredRow.category == 'Discovered'
    policySummary.fixedRow.category == 'Fixed'
    policySummary.pendingRow.category == 'Pending'
    policySummary.waivedRow.category == 'Waived'

    and: 'The counts for each category are shown'
    policySummary.discoveredRow.count == 10
    policySummary.fixedRow.count == 8
    policySummary.pendingRow.count == 1
    policySummary.waivedRow.count == 1

    and: 'The deltas for each category are shown'
    policySummary.discoveredRow.delta.value == 7
    policySummary.fixedRow.delta.value == 8
    policySummary.pendingRow.delta.value == -2
    policySummary.waivedRow.delta.value == 1

    and: 'The deltas for each category are styled properly'
    policySummary.discoveredRow.delta.isUp
    policySummary.discoveredRow.delta.isNeutral
    policySummary.fixedRow.delta.isUp
    policySummary.fixedRow.delta.isNatural
    policySummary.pendingRow.delta.isDown
    policySummary.pendingRow.delta.isInverse
    policySummary.waivedRow.delta.isUp
    policySummary.waivedRow.delta.isInverse

    and: 'The bar charts show the expected values'
    policySummary.discoveredRow.barChart.points == NEW_ROW_DELTAS
    policySummary.fixedRow.barChart.points == FIXED_ROW_DELTAS
    policySummary.pendingRow.barChart.points == UNRESOLVED_ROW_DELTAS
    policySummary.waivedRow.barChart.points == WAIVED_ROW_DELTAS

    and: 'The sparkline charts show the expected values'
    isDeltaArrayCloseToSum(3, NEW_ROW_DELTAS, policySummary.discoveredRow.sparkline.getValues())
    isDeltaArrayCloseToSum(0, FIXED_ROW_DELTAS, policySummary.fixedRow.sparkline.getValues())
    isDeltaArrayCloseToSum(3, UNRESOLVED_ROW_DELTAS, policySummary.pendingRow.sparkline.getValues())
    isDeltaArrayCloseToSum(0, WAIVED_ROW_DELTAS, policySummary.waivedRow.sparkline.getValues())

    and: 'The sparkline charts trail with the correct colors'
    policySummary.discoveredRow.sparkline.isTrailingBlue()
    policySummary.fixedRow.sparkline.isTrailingGreen()
    policySummary.pendingRow.sparkline.isTrailingGreen()
    policySummary.waivedRow.sparkline.isTrailingRed()

    when: 'hovering over sparkline'
    interact {
      moveToElement(policySummary.discoveredRow.sparkline.previousPath)
    }
    waitFor { policySummary.discoveredRow.sparkline.guideText.displayed }

    then: 'value is displayed'
    // moveToElement moves to the middle of the sparkline
    policySummary.discoveredRow.sparkline.guideText.text() == '7'
  }

  def 'Filtering out all data should show an empty policy summary'() {
    when: 'clicking the filter toggle button'
    PolicySummaryModule policySummary = policySummary;
    filters.toggle.click()

    then: 'the dashboard filters are shown'
    waitFor { filters.applicationMultiselect.displayed }
    filters.policyTypeMultiselect.displayed

    when: 'we select a policy threat type that we have no violations for'
    filters.policyTypeMultiselect.toggleOption('License')
    filters.apply()

    then: 'The counts for each category should all be zero'
    policySummary.discoveredRow.count == 0
    policySummary.fixedRow.count == 0
    policySummary.pendingRow.count == 0
    policySummary.waivedRow.count == 0

    and: 'The deltas for each category should all be zero'
    policySummary.discoveredRow.delta.value == 0
    policySummary.fixedRow.delta.value == 0
    policySummary.pendingRow.delta.value == 0
    policySummary.waivedRow.delta.value == 0

    and: 'The deltas for each category should have no styling'
    [policySummary.discoveredRow.delta, policySummary.fixedRow.delta, policySummary.pendingRow.delta, policySummary.waivedRow.delta].each { DeltaModule delta ->
      assert !delta.isUp
      assert !delta.isDown
    }

    and: 'The bar chart points should all be zero'
    def emptyPoints = (0..11).collect { 0 }
    policySummary.discoveredRow.barChart.points == emptyPoints
    policySummary.fixedRow.barChart.points == emptyPoints
    policySummary.pendingRow.barChart.points == emptyPoints
    policySummary.waivedRow.barChart.points == emptyPoints

    and: 'The sparkline points should all be zero'
    isDeltaArrayCloseToSum(0, emptyPoints, policySummary.discoveredRow.sparkline.getValues())
    isDeltaArrayCloseToSum(0, emptyPoints, policySummary.fixedRow.sparkline.getValues())
    isDeltaArrayCloseToSum(0, emptyPoints, policySummary.pendingRow.sparkline.getValues())
    isDeltaArrayCloseToSum(0, emptyPoints, policySummary.waivedRow.sparkline.getValues())
  }


  private void createViolations(PolicyEvaluation evaluation, List<Map> components) {
    components.each { Map component ->
      temporaryEntity.
          newPolicyViolation(evaluation, policy, component.groupId, component.artifactId, component.version,
          component.hash, '')
    }
  }

  private void createWaivedViolations(PolicyEvaluation evaluation, List<Map> components) {
    components.each { Map component ->
      def waiver = existingWaivers.find { w -> w.hash.equals(component.hash) }
      if (waiver == null) {
        waiver = temporaryEntity.newWaiver(component.hash, policy.id, app.id)
        existingWaivers.add(waiver)
      }
      temporaryEntity.newWaivedPolicyViolation(evaluation, policy, component.groupId, component.artifactId,
          component.version, component.hash, waiver)
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
