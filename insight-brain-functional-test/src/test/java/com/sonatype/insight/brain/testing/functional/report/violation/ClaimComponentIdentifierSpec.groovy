/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.report.violation

import com.sonatype.clm.dto.model.component.ComponentIdentifier

import spock.lang.Ignore
import spock.lang.Stepwise

/**
 * @since 1.13
 */
@Stepwise
@Ignore //https://sonatype.atlassian.net/browse/CLM-30530
class ClaimComponentIdentifierSpec
extends AbstractClaimComponentSpec {
  @Override
  String getReportId() {
    // The reportId must match the reportId value recorded inside the test report.zip used for this test
    return '9fcc64dfc37d4569813970f741966303'
  }


  @Override
  String getReportPath() {
    return '/canned-reports/report-with-unknown-component-identifier.zip'
  }

  @Override
  String getExpectedDisplayNameString() {
    return 'testG : testA : testE : testC : testV'
  }

  @Override
  String getExpectedUpdatedDisplayNameString() {
    return 'testG : testA : testE : testV-NEW'
  }

  @Override
  String getExpectedHash() {
    return '035f9a97aa10bb7a13ad'
  }

  def 'Should not show jar extensions in the display name when claimed without a classifier'() {
    given: 'A GAV not found in our data'
    ComponentIdentifier identifier = ComponentIdentifier.
        createMavenCoordinates(CID.coordinates.groupId, CID.coordinates.artifactId, CID.coordinates.version, '', 'jar')
    hdsRule.respondWith('{"isKnown": false }').atUri(createUri(identifier))
    PolicyReportRow firstRow = results[0]
    Cip cip = firstRow.cip as Cip
    ClaimComponentModule component = cip.claimComponent as ClaimComponentModule

    when: 'Claiming a component with a jar extension and no classifier'
    def claimForm = component.claimForm
    claimForm.groupId = FORM_FIELDS['groupId']
    claimForm.artifactId = FORM_FIELDS['artifactId']
    claimForm.version = FORM_FIELDS['version']
    claimForm.extension = 'jar'
    claimForm.createTimeText = FORM_FIELDS['createTimeText']
    claimForm.comment = FORM_FIELDS['comment']
    component.claim.click()

    then: 'data is updated and the update button is no longer enabled'
    waitFor { component.update.displayed }
    component.claimForm.extension == 'jar'

    and: 'the coordinates do not show the jar extension'
    results[0].coordinates == 'testG : testA : testV'
  }

  def 'Should show jar extensions in the display name when claimed with a classifier'() {
    given: 'A GAV not found in our data'
    ComponentIdentifier updatedIdentifier = ComponentIdentifier.
        createMavenCoordinates(CID.coordinates.groupId, CID.coordinates.artifactId, CID.coordinates.version,
        CID.coordinates.classifier, 'jar')
    hdsRule.respondWith('{"isKnown": false }').atUri(createUri(updatedIdentifier))
    PolicyReportRow firstRow = results[0]
    Cip cip = firstRow.cip as Cip
    ClaimComponentModule component = cip.claimComponent as ClaimComponentModule

    when: 'Claiming a component with a jar extension and no classifier'
    component.claimForm.classifier = CID.coordinates.classifier
    component.update.click()

    then: 'data is updated and the update button is no longer enabled'
    waitFor { component.update.disabled }
    component.claimForm.classifier == CID.coordinates.classifier

    and: 'the coordinates show the jar extension along with the classifier'
    results[0].coordinates == 'testG : testA : jar : testC : testV'
  }
}
