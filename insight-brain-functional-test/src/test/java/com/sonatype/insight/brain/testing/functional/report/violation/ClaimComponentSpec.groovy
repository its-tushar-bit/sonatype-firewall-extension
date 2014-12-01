/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.report.violation

import javax.ws.rs.core.UriBuilder

import com.sonatype.clm.dto.model.component.ComponentIdentifier
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter
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
extends BaseSpec {

  static final String cannedTestReport = '/canned-reports/report-with-unknown.zip'

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

  static final ComponentIdentifier CID = ComponentIdentifier.
  createMavenCoordinates(FORM_FIELDS.groupId, FORM_FIELDS.artifactId, FORM_FIELDS.version, FORM_FIELDS.classifier,
  FORM_FIELDS.extension)

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
    PolicyReportRow firstRow = results[0]
    Cip cip = firstRow.showCip()

    then: 'the claim component tab is shown'
    cip.claimComponent.showTrigger.displayed

    and: 'the filename is shown for the unknown component'
    firstRow.coordinates == 'hello-world.jar'

    when: 'opening the claim component tab'
    cip.claimComponent.showTrigger.click()

    then: 'the form is shown and empty, with a disabled claim button'
    waitFor { cip.claimComponent.claimForm.displayed }
    def form = cip.claimComponent.claimForm
    FORM_FIELDS.keySet().each { name ->
      assert form."$name" == ''
    }
    cip.claimComponent.claim.disabled
    !cip.claimComponent.revoke.present
    !cip.claimComponent.update.present
  }

  def "Should be able to claim an unknown component"() {
    given: 'A GAV not found in our data'
    saasRule.setResponseForURI(createUri(CID), '{"isKnown": false }', 200)
    PolicyReportRow firstRow = results[0]
    Cip cip = firstRow.cip
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

    and: 'the coordinates are updated to match the claim details'
    results[0].coordinates == 'testG : testA : testV'
  }

  def "Should be able to update an already claimed component"() {
    given: 'A GAV not found in our data'
    ComponentIdentifier updatedIdentifier = ComponentIdentifier.
        createMavenCoordinates(CID.coordinates.groupId, CID.coordinates.artifactId, CID.coordinates.version + '-NEW',
        '', CID.coordinates.extension)
    saasRule.setResponseForURI(createUri(updatedIdentifier), '{"isKnown": false }', 200)
    PolicyReportRow firstRow = results[0]
    Cip cip = firstRow.cip
    ClaimComponentModule component = cip.claimComponent

    when: 'Changing the version of the claimed component'
    component.claimForm.version = FORM_FIELDS.version + '-NEW'

    and: 'Removing the classifier'
    component.claimForm.classifier = ''

    then: 'the update button should be enabled'
    waitFor { !component.update.disabled }

    when: 'clicking "Update"'
    component.update.click()
    waitFor { component.update.disabled }

    then: 'data is updated and the update button is no longer enabled'
    component.claimForm.version == FORM_FIELDS.version + '-NEW'
    component.claimForm.classifier == ''

    and: 'the coordinates are updated to match the claim details'
    results[0].coordinates == 'testG : testA : testV-NEW'
  }

  def "Can assign a license to a claimed component"() {
    when: 'opening the Licenses tab'
    Cip cip = results[0].cip
    cip.licenses.showTrigger.click()

    then: 'the form is shown and empty, with a disabled update button'
    waitFor { cip.licenses.form.displayed }
    LicenseModule licenses = cip.licenses
    licenses.validateLicense('', '', '', app.name, 'Open', '', '', false)

    when: 'Selecting to override the license'
    licenses.status = 'Overridden'

    then: 'License choices are shown'
    waitFor { licenses.licenseOptionShown }

    when: 'A new license is selected'
    licenses.license = 'Beerware'

    then: 'The update button should be enabled'
    waitFor { !licenses.update.disabled }

    when: 'We add a comment(which is optional)'
    licenses.comment = 'Because everything goes better with beer!'

    and: 'We click the update button'
    licenses.update.click()

    then: 'The audit log should be extended'
    AuditLogModule auditLog = cip.auditLog
    auditLog.showTrigger.click()
    waitFor { auditLog.audits.size() == 1 }
    auditLog.validateRow(auditLog.audits[0], 'admin', 'Overrode', 'License as Beerware',
        'Because everything goes better with beer!')

    when: 'We go back to the licenses, our new info appears (the UI does not automatically update)'
    cip.licenses.showTrigger.click()

    then:
    waitFor { cip.licenses.form.displayed }
    licenses.validateLicense('', '', 'Beerware', app.name, 'Overridden', 'Beerware', '', false)
  }

  def "Should be able to revoke a claim on a component"() {
    given: 'An already claimed component'
    Cip cip = results[0].cip
    cip.claimComponent.showTrigger.click()
    ClaimComponentModule component = cip.claimComponent

    when: 'Clicking the "Revoke Claim" button'
    component.revoke.click()

    then: 'a confirmation dialog should be launched'
    waitFor { revokeClaimModal.modal.displayed }
    revokeClaimModal.cancel.displayed
    revokeClaimModal.buttons.button('Revoke').displayed

    when: 'clicking "Revoke"'
    revokeClaimModal.buttons.button('Revoke').click()

    then: 'the modal should disappear and the form should be cleared'
    waitFor { component.claim.present }
    FORM_FIELDS.keySet().each { name ->
      component.claimForm."$name" == ''
    }
    !component.revoke.displayed
    !component.update.displayed

    and: 'the filename is shown for the unknown component'
    results[0].coordinates == 'hello-world.jar'
  }

  private String createUri(ComponentIdentifier componentIdentifier) {
    return UriBuilder.fromPath('rest/component/summary').
    queryParam('componentIdentifier', ComponentIdentifierAdapter.toJson(componentIdentifier)).build().toString()
  }
}
