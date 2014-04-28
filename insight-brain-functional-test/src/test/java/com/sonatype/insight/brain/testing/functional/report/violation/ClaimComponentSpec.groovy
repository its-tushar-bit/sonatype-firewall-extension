/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.report.violation

import com.sonatype.insight.brain.model.Application
import com.sonatype.insight.brain.service.InsightWork
import com.sonatype.insight.brain.testing.functional.BaseSpec
import com.sonatype.insight.brain.testing.functional.utils.TestReportEvaluator
import spock.lang.Shared
import spock.lang.Stepwise

/**
 * @since 1.11
 */
@Stepwise
class ClaimComponentSpec
    extends BaseSpec
{

  static final String cannedTestReport = '/canned-reports/report-with-unknown.zip'

  static final ArrayList<String> HDS_PARAMS = ['groupId', 'artifactId', 'version', 'extension', 'classifier']

  @Shared
  Application app

  @Shared
  InsightWork work

  @Shared
  def evaluator

  @Shared
  String scanId


  static final Map<String, String> FORM_FIELDS = ['groupId'       : 'testG', 'artifactId': 'testA', 'version': 'testV',
                                                  'extension'     : 'testE', 'classifier': 'testC',
                                                  'createTimeText': '01/01/2014',
                                                  'comment'       : 'Something witty'].asImmutable()

  def setupSpec() {
    work = new InsightWork(serviceRule.configuration)
    app = temporaryEntity.newApplication(temporaryEntity.newOrganization().id)
    evaluator = new TestReportEvaluator(app, getClass().getResource(cannedTestReport), browser.baseUrl, work)
    scanId = evaluator.evaluatePolicy()
    loginAsAdminVia()
    to ReportPage, app.publicId, scanId
  }

  def "Should see claim tab for an unknown component"() {
    when: 'First navigating to a report with an unknown component'
      navigation.toPolicyReportPage()
      Cip cip = results[0].showCip()

    then: 'the claim component tab is shown'
      cip.claimComponent.showTrigger.displayed

    when: 'opening the claim component tab'
      cip.claimComponent.showTrigger.click()

    then: 'the form is shown and empty, with a disabled claim button'
      def form = cip.claimComponent.claimForm
      FORM_FIELDS.keySet().each { name ->
        form."$name" == ''
      }
      cip.claimComponent.claim.disabled
      !cip.claimComponent.revoke.present
      !cip.claimComponent.update.present
  }

  def "Should be able to claim an unknown component"() {
    given: 'A GAV not found in our data'
      saasRule.setResponseForURI(createUri(FORM_FIELDS), '{"isKnown": false }', 200)
      Cip cip = results[0].cip
      def form = cip.claimComponent.claimForm

    when: 'Filling out the form'
      FORM_FIELDS.each { String name, String value ->
        form."$name" = value
      }

    then: 'the "Claim" button is enabled'
      waitFor { !cip.claimComponent.claim.disabled }

    when: 'clicking "Claim"'
      cip.claimComponent.claim.click()
      waitFor { !cip.claimComponent.claim.displayed }
      form = cip.claimComponent.claimForm

    then: 'the form is saved and "Update" and "Revoke" buttons appear'
      cip.claimComponent.update.disabled
      !cip.claimComponent.revoke.disabled
      FORM_FIELDS.each { String name, String value ->
        form."$name" == value
      }
  }

  def "Should be able to update an already claimed component"() {
    given: 'A GAV not found in our data'
      Map updatedVersion = new LinkedHashMap<>(FORM_FIELDS)
      updatedVersion.version = FORM_FIELDS.version + '-NEW'
      saasRule.setResponseForURI(createUri(updatedVersion), '{"isKnown": false }', 200)
      def component = results[0].cip.claimComponent

    when: 'Changing the version of the claimed component'
      component.claimForm.version = FORM_FIELDS.version + '-NEW'

    then: 'the update button should be enabled'
      waitFor { !component.update.disabled }

    when: 'clicking "Update"'
      component.update.click()
      waitFor { component.update.disabled }

    then: 'data is updated and the update button is no longer enabled'
      component.claimForm.version == FORM_FIELDS.version + '-NEW'
  }

  def "Should be able to revoke a claim on a component"() {
    given: 'An already claimed component'
      def component = results[0].cip.claimComponent

    when: 'Clicking the "Revoke Claim" button'
      component.revoke.click()

    then: 'a confirmation dialog should be launched'
      waitFor { revokeClaimModal.modal.displayed }
      revokeClaimModal.cancel.displayed
      revokeClaimModal.buttons.button('Revoke').displayed

    when: 'clicking "Revoke"'
      revokeClaimModal.buttons.button('Revoke').click()

    then: 'the modal should disappear and the form should be cleared'
      FORM_FIELDS.keySet().each { name ->
        component.claimForm."$name" == ''
      }
      !component.revoke.displayed
      !component.update.displayed
  }

  private static String createUri(Map fields) {
    return "rest/ide/component?${createParams(fields)}"
  }

  private static String createParams(Map fields) {
    fields.
        findAll { k, v -> k in HDS_PARAMS }.
        collect { k, v -> "$k=$v" }.join('&')
  }

}
