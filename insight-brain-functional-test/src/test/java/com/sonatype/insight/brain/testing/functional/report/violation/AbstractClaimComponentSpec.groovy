/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.report.violation

import javax.ws.rs.core.UriBuilder

import com.sonatype.clm.dto.model.component.ComponentIdentifier
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO
import com.sonatype.insight.brain.model.Application
import com.sonatype.insight.brain.model.component.HashComponentIdentifier
import com.sonatype.insight.brain.model.component.IdentificationSource
import com.sonatype.insight.brain.service.InsightWork
import com.sonatype.insight.brain.testing.functional.BaseSpec
import com.sonatype.insight.brain.testing.functional.utils.TestReportEvaluator
import com.sonatype.insight.json.store.JsonUtils

import org.eclipse.jetty.util.UrlEncoded
import spock.lang.Shared
import spock.lang.Stepwise

/**
 * @since 1.11
 */
@Stepwise
abstract class AbstractClaimComponentSpec
extends BaseSpec {

  @Shared
  Application app

  @Shared
  InsightWork work

  @Shared
  def evaluator

  static final Map<String, String> FORM_FIELDS = ['groupId'       : 'testG', 'artifactId': 'testA', 'version': 'testV',
    'extension'     : 'testE', 'classifier': 'testC',
    'createTimeText': '01/01/2014',
    'comment'       : 'Something witty'].asImmutable()

  static final ComponentIdentifier CID = ComponentIdentifier.
  createMavenCoordinates(FORM_FIELDS.groupId, FORM_FIELDS.artifactId, FORM_FIELDS.version, FORM_FIELDS.classifier,
  FORM_FIELDS.extension)

  static final ComponentIdentifier UCID = ComponentIdentifier.
  createMavenCoordinates(FORM_FIELDS.groupId, FORM_FIELDS.artifactId, FORM_FIELDS.version + '-NEW',
  '', FORM_FIELDS.extension)

  @Override
  def setupSpec() {
    work = new InsightWork(serviceRule.configuration)
    app = temporaryEntity.newApplication(temporaryEntity.newOrganization().id)
    String reportId = getReportId()
    evaluator = new TestReportEvaluator(app, reportId, getClass().getResource(getReportPath()), browser.baseUrl, work)
    evaluator.evaluatePolicy()
    loginAsAdminVia()
    to ReportPage, app.publicId, reportId
  }

  @Override
  def cleanupSpec() {
    HashComponentIdentifierDAO dao = new HashComponentIdentifierDAO()
    HashComponentIdentifier hci = dao.getByComponentIdentifier(CID)
    if (hci) {
      dao.delete(hci)
    }
  }

  def "Should see claim tab for an unknown component"() {
    when: 'First navigating to a report with an unknown component'
    navigation.toPolicyReportPage()
    PolicyReportRow firstRow = results[0]
    Cip cip = firstRow.showCip() as Cip

    then: 'the claim component tab is shown'
    cip.claimComponent.showTrigger.displayed

    and: 'the filename is shown for the unknown component'
    firstRow.coordinates == 'hello-world.jar'

    when: 'opening the claim component tab'
    cip.claimComponent.showTrigger.click()

    then: 'the form is shown and empty, with a disabled claim button'
    waitFor { cip.claimComponent.claimForm.displayed }
    def claimForm = cip.claimComponent.claimForm
    FORM_FIELDS.keySet().each { name ->
      assert claimForm."$name" == ''
    }
    cip.claimComponent.claim.disabled
    !cip.claimComponent.revoke.present
    !cip.claimComponent.update.present
  }

  def "Should be able to claim an unknown component"() {
    given: 'A GAV not found in our data'
    hdsRule.setResponseForURI(createUri(CID), '{"isKnown": false }', 200)
    PolicyReportRow firstRow = results[0]
    Cip cip = firstRow.cip as Cip
    def claimForm = cip.claimComponent.claimForm

    when: 'Filling out the form'
    FORM_FIELDS.each { String name, String value ->
      claimForm."$name" = value
    }

    then: 'the "Claim" button is enabled'
    waitFor { !cip.claimComponent.claim.disabled }

    when: 'clicking "Claim"'
    cip.claimComponent.claim.click()
    waitFor { !cip.claimComponent.claim.displayed }
    claimForm = cip.claimComponent.claimForm

    then: 'the form is saved and "Update" and "Revoke" buttons appear'
    cip.claimComponent.update.disabled
    !cip.claimComponent.revoke.disabled
    FORM_FIELDS.each { String name, String value ->
      claimForm."$name" == value
    }

    and: 'the coordinates are updated to match the claim details'
    results[0].coordinates == getExpectedDisplayNameString()

    when: 'We go to the component info page'
    mockHdsComponentDetailsListResponse(CID);
    cip.componentInfo.show()

    then: 'The claimed component coordinates are shown'
    waitFor { cip.componentInfo.group.text() == 'testG' }
    cip.componentInfo.artifact.text() == 'testA'
    cip.componentInfo.extension.text() == 'testE'
    cip.componentInfo.classifier.text() == 'testC'
    cip.componentInfo.version.text() == 'testV'
    cip.componentInfo.identificationSource.text() == IdentificationSource.MANUAL.getName()
    cip.componentInfo.claimComment.text() == 'Something witty'
  }

  def "Should be able to update an already claimed component"() {
    given: 'A GAV not found in our data'
    hdsRule.setResponseForURI(createUri(UCID), '{"isKnown": false }', 200)
    PolicyReportRow firstRow = results[0]
    Cip cip = firstRow.cip as Cip
    ClaimComponentModule component = cip.claimComponent as ClaimComponentModule
    cip.claimComponent.showTrigger.click()
    waitFor { component.claimForm.displayed && component.claimForm.version }

    when: 'Changing the version of the claimed component'
    component.claimForm.version = FORM_FIELDS.version + '-NEW'

    and: 'Removing the classifier'
    component.claimForm.classifier = ''

    then: 'the update button should be enabled'
    waitFor { !component.update.disabled }

    when: 'clicking "Update"'
    component.update.click()

    then: 'data is updated and the update button is no longer enabled'
    waitFor { component.update.disabled }
    component.claimForm.version == FORM_FIELDS.version + '-NEW'
    component.claimForm.classifier == ''

    and: 'the coordinates are updated to match the claim details'
    results[0].coordinates == getExpectedUpdatedDisplayNameString()

    when: 'We go to the component info page'
    mockHdsComponentDetailsListResponse(UCID);
    cip.componentInfo.show()

    then: 'The claimed component coordinates are shown'
    waitFor { cip.componentInfo.group.text() == 'testG' }
    cip.componentInfo.artifact.text() == 'testA'
    cip.componentInfo.extension.text() == 'testE'
    !cip.componentInfo.classifier.displayed
    cip.componentInfo.version.text() == 'testV-NEW'
    cip.componentInfo.identificationSource.text() == IdentificationSource.MANUAL.getName()
    cip.componentInfo.claimComment.text() == 'Something witty'
  }

  def "Can assign a license to a claimed component"() {
    when: 'opening the Licenses tab'
    Cip cip = results[0].cip as Cip
    cip.licenses.showTrigger.click()

    then: 'the form is shown and empty, with a disabled update button'
    waitFor { cip.licenses.form.displayed }
    LicenseModule licenses = cip.licenses as LicenseModule
    licenses.validateLicense('', '', '', app.name, 'Open', '', '', false)

    when: 'Selecting to override the license'
    licenses.status = 'Overridden'

    then: 'License choices are shown'
    waitFor { licenses.selectedLicenses.dropdownButton.text() == 'None selected' }

    when: 'A new license is selected'
    licenses.selectedLicenses.toggleOption('Beerware')

    then: 'The update button should be enabled'
    waitFor { !licenses.update.disabled }

    when: 'We add a comment(which is optional)'
    licenses.comment = 'Because everything goes better with beer!'

    and: 'We click the update button'
    licenses.update.click()

    then: 'The audit log should be extended'
    AuditLogModule auditLog = cip.auditLog as AuditLogModule
    auditLog.showTrigger.click()
    waitFor { auditLog.audits.size() == 1 }
    auditLog.validateRow(auditLog.audits[0], 'admin', 'Overrode', 'License as Beerware',
        'Because everything goes better with beer!')

    when: 'We go back to the licenses, our new info appears(the UI does not automatically update)'
    cip.licenses.showTrigger.click()

    then:
    waitFor { cip.licenses.form.displayed }
    licenses.validateLicense('', '', 'Beerware', app.name, 'Overridden', 'Beerware', '', false)

    when: 'We go to the component info page'
    mockHdsComponentDetailsListResponse(UCID);
    cip.componentInfo.show()

    then: 'The effective license is shown'
    waitFor { cip.componentInfo.effectiveLicense*.text() == ['Beerware'] }
  }

  def "Should be able to revoke a claim on a component"() {
    given: 'An already claimed component'
    Cip cip = results[0].cip as Cip
    cip.claimComponent.showTrigger.click()
    ClaimComponentModule component = cip.claimComponent as ClaimComponentModule
    waitFor { component.revoke.displayed }

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

  String createUri(ComponentIdentifier componentIdentifier) {
    return UriBuilder.fromPath('rest/component/summary').queryParam('componentIdentifier',
        URLEncoder.encode(ComponentIdentifierAdapter.toJson(componentIdentifier), "UTF-8")).build().toString()
  }

  void mockHdsComponentDetailsListResponse(ComponentIdentifier identifier) {
    hdsRule.setResponseForURI("rest/ci/componentDetails/list?componentIdentifier=" +
        UrlEncoded.encodeString(JsonUtils.writeUnformatted(identifier)) +
        "&hash=" + getExpectedHash() + "&matchState=exact", '{"list":[]}', 200)
  }

  abstract String getReportPath()
  abstract String getReportId()
  abstract String getExpectedDisplayNameString()
  abstract String getExpectedUpdatedDisplayNameString()
  abstract String getExpectedHash()
}
