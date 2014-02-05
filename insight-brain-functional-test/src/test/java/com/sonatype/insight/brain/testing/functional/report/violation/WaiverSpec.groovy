/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.report.violation

import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO
import com.sonatype.insight.brain.model.Application
import com.sonatype.insight.brain.model.policy.Condition
import com.sonatype.insight.brain.model.policy.Constraint
import com.sonatype.insight.brain.model.policy.Policy
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType
import com.sonatype.insight.brain.service.InsightWork
import com.sonatype.insight.brain.testing.functional.BaseSpec
import com.sonatype.insight.brain.testing.functional.utils.TestReportEvaluator

import spock.lang.Stepwise

/**
 * For the organization based policy tests it is assumed that the unit tests have proven correct functionality regarding
 * scope and applied components when there are multiple child applications.
 */
@Stepwise // Share the login and browser instance to reduce execution time
class WaiverSpec
    extends BaseSpec 
{

  static cannedTestReport = '/canned-reports/small-report.zip'

  // The number of components that are in the canned test report
  static numberOfComponents = 4

  def app = newApplication()

  def work = new InsightWork(serviceRule.configuration)

  def evaluator = new TestReportEvaluator(app, getClass().getResource(cannedTestReport), browser.baseUrl, work)

  def scanId

  def setupSpec() {
    // Can't do anything without a logged in user
    loginAsAdminVia()
  }

  /**
   * WaiverUxSpec that confirms the steps to open and dismiss the waiver dialog as well as the contents
   */
  def "Waiver dialog can be interacted with"() {
    given: 'an org with a policy and a child app'
      createGavViolatingPolicy(app.organizationId)

    and: 'a policy evaluation'
      scanId = evaluator.evaluatePolicy()

    when: 'apply waiver dialog is active'
      to ReportPage, app.publicId, scanId
      navigation.toPolicyReportPage()

      results[0].addWaiverForFirstViolation()

    then: 'there is no implicit scope...because the violation was against an orgs policy'
      waiver.isImplicitScope == false

    and: 'limit to scope option is present with two options'
      waiver.scope.displayed == true
      waiver.scope.size() == 2

    and: 'default is to limit to application'
      waiver.scope.value() == app.publicId

    when: 'selecting option to limit scope to organization'
      waiver.scope = app.organizationId

    then: 'sets the scope option value to the organization'
      waiver.scope == app.organizationId

    when: 'selecting option to limit scope to application'
      waiver.scope = app.publicId

    then: 'sets the scope option value to the application'
      waiver.scope == app.publicId

    when: 'apply waiver dialog is active'
      // it already should be...this is like a StepWise test but all in one method

    then: 'apply to option is present with two options'
      waiver.scope.displayed == true
      waiver.apply.size() == 2

    and: 'the label text for the apply inputs can be retrieved...for use in setting the input value using Geb magic'
      waiver.allComponents.size() > 0
      waiver.selectedComponent.size() > 0

    and: 'default is to apply to selected component'
      // Need to test indirectly since the visual value (descriptive text with GAV) is different than the submitted
      // value (hash)
      waiver.apply.value() != waiver.allComponents
      // the value should be set to the hash of the component, not the text of the label
      waiver.apply.value() != waiver.selectedComponent

    when: 'selecting option to apply to all components'
      waiver.apply = waiver.allComponents

    then: 'sets the apply option value to null'
      waiver.apply.value() == null

    when: 'selecting option to apply to current component'
      waiver.apply = waiver.selectedComponent

    then: 'sets the apply option to the hash (it is not null)'
      waiver.apply.value().size() > 0
  }

  def "Application policy does not prompt for scope of waiver (limits to application only)"() {
    given: 'an app with only app policy'
      createGavViolatingPolicy(app.id)

    and: 'a policy evaluation'
      scanId = evaluator.evaluatePolicy()

    when: 'waiving a component'
      waiveComponent()

    then: 'cannot select a scope to limit waiver'
      waiver.isImplicitScope == true
  }

  def "Organization policy prompts for scope of waiver"() {
    given: 'an org with a policy'
      createGavViolatingPolicy(app.organizationId)

    and: 'a child app with no policy'
      // nothing to do

    and: 'a policy evaluation'
      scanId = evaluator.evaluatePolicy()

    when: 'waiving a component'
      waiveComponent()

    then: 'prompted to select a scope to limit waiver'
      waiver.isImplicitScope == false
  }

  def "Waiver can be applied to selected component for children of organization"() {
    given: 'an org with a policy'
      createGavViolatingPolicy(app.organizationId)

    and: 'a child app with no policy'
      // nothing to do

    and: 'a policy evaluation'
      scanId = evaluator.evaluatePolicy()

    when: 'waiving component'
      waiveComponent()

    and: 'that is scoped to organization'
      waiver.scope = app.organizationId

    and: 'that is applied to component'
      waiver.apply = waiver.selectedComponent

    and: 'that is saved'
      waiver.save()

    and: 'policy is revaluated'
      evaluator.reevaluatePolicy()

      to ReportPage, app.publicId, scanId
      navigation.toPolicyReportPage()

    then: 'waived policy is not violated'
      results.size() == numberOfComponents
      resultsWithNoScore.size() == 1
  }

  def "Waiver can be applied to all components for children of organization"() {
    given: 'an org with a policy'
      createGavViolatingPolicy(app.organizationId)

    and: 'a child app with no policy'
      // nothing to do

    and: 'a policy evaluation'
      scanId = evaluator.evaluatePolicy()

    when: 'waiving component'
      waiveComponent()

    and: 'that is scoped to organization'
      waiver.scope = app.organizationId

    and: 'that is applied to all components'
      waiver.apply = waiver.allComponents

    and: 'that is saved'
      waiver.save()

    and: 'policy is revaluated'
      evaluator.reevaluatePolicy()

      to ReportPage, app.publicId, scanId
      navigation.toPolicyReportPage()

    then: 'all components do not have violations'
      results.size() == numberOfComponents
      resultsWithNoScore.size() == numberOfComponents
  }

  def "Waiver can be applied to selected component for application"() {
    given: 'an app with only app policy'
      createGavViolatingPolicy(app.id)

    and: 'a policy evaluation'
      scanId = evaluator.evaluatePolicy()

    when: 'waiving component'
      waiveComponent()

    and: 'that is scoped to applicaiton'
      // implicit when there are no org policies

    and: 'that is applied to component'
      waiver.apply = waiver.selectedComponent

    and: 'that is saved'
      waiver.save()

    and: 'policy is revaluated'
      evaluator.reevaluatePolicy()

      to ReportPage, app.publicId, scanId
      navigation.toPolicyReportPage()

    then: 'waived policy is not violated'
      results.size() == numberOfComponents
      resultsWithNoScore.size() == 1
  }

  def "Waiver can be applied to all components for application"() {
    given: 'an app with only app policy'
      createGavViolatingPolicy(app.id)

    and: 'a policy evaluation'
      scanId = evaluator.evaluatePolicy()

    when: 'waiving component'
      waiveComponent()

    and: 'that is scoped to applicaiton'
      // implicit when there are no org policies

    and: 'that is applied to all components'
      waiver.apply = waiver.allComponents

    and: 'that is saved'
      waiver.save()

    and: 'policy is re-evaluated'
      evaluator.reevaluatePolicy()

      to ReportPage, app.publicId, scanId
      navigation.toPolicyReportPage()

    then: 'all components do not have violations'
      results.size() == numberOfComponents
      resultsWithNoScore.size() == numberOfComponents
  }

  private Application newApplication() {
    def org = temporaryEntity.newOrganization()
    def app = temporaryEntity.newApplication(org.id)

    return app
  }

  private createGavViolatingPolicy(String ownerId) {
    // create policy
    def condition = new Condition(CoordinatesConditionType.ID, 'match', '*')
    def constraint = new Constraint()
    constraint.name = 'All coordinates'
    constraint.addCondition(condition)
    def policy = new Policy()
    policy.name = 'All components'
    policy.addConstraint(constraint)
    policy.setOwnerId(ownerId)

    // add policy
    new PolicyDAO().insert(policy)
  }

  private waiveComponent() {
    to ReportPage, app.publicId, scanId
    navigation.toPolicyReportPage()
    results[0].addWaiverForFirstViolation()
  }
}