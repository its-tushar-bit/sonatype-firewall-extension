/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.cip

import com.sonatype.clm.dto.model.component.ComponentIdentifier
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter

import geb.Module
import geb.Page

/**
 * Parent for pages which embed the CIP in other applications, but which do not share a common url.
 * @since 1.12
 */
abstract class AbstractCIPPage
extends Page {
  static at = { browser.title == 'Component Information Panel' }

  static content = {
    defaultText(required: false) { $('#select-component') }
    cip(required: false) { module CIPModule }
    versionGraph(required: false) { module VersionGraphModule }
    error { $('#error-message') }
  }

  /**
   * Call the same javascript function used by clients to trigger loading component details.
   */
  def setGav(String groupId, String artifactId, String version, String applicationPublicId, boolean wait = true) {
    browser.js.exec(groupId, artifactId, version, applicationPublicId, '''
  window.Insight.setGav({
        groupId: arguments[0],
        artifactId: arguments[1],
        version: arguments[2],
        appId: arguments[3]
  });
  ''')
    if (wait) {
      waitFor('slow') {cip.displayed && cip.getNameField('Group')}
    }
  }

  def setCoordinates(ComponentIdentifier componentIdentifier, String applicationPublicId, boolean wait = true) {
    browser.js.exec(ComponentIdentifierAdapter.toJson(componentIdentifier), applicationPublicId, '''
  var componentIdentifier = JSON.parse(arguments[0]);
  window.Insight.setCoordinates(componentIdentifier.format, componentIdentifier.coordinates, {
        appId: arguments[1]
  });
  ''')
    if (wait) {
      waitFor('slow') {cip.displayed && cip.getNameField('Version')}
    }
  }

  def clearGav() {
    browser.js.exec('window.Insight.clearGav()')
  }
}

class CIPModule
extends Module {
  static base = { $('#infoPanelArtifactTable') }

  static content = {
    effectiveLicense { $('#artifactInfoEffectiveLicenseRow td:last-child').text() }
    declaredLicense { $('#artifactInfoDeclaredLicenseRow td:last-child').text() }
    observedLicense { $('#artifactInfoObservedLicenseRow td:last-child').text() }
    highestPolicyThreat { $('#artifactInfoHighestPolicyThreat td:last-child').text() }
    highestSecurityThreat { $('#artifactInfoSecurityThreatRow td:last-child').text() }
    catalogued { $('#artifactInfoCatalogDateRow td:last-child').text() }
    matchState { $('#artifactInfoSimilarityScoreRow td:last-child').text() }
    identificationSource { $('#artifactInfoIdentificatonSource td:last-child').text() }
    website(required: false) { $('#artifactWebsite a') }
    viewDetails { $('[ng-click="viewDetails()"]') }
    migrate(required: false) { $('[ng-click="markUpgrade()"]') } // only present in Eclipse
  }

  def getNameField(String field) {
    return $("#artifactInfo-${field} td:last-child").text();
  }
}

class VersionGraphModule
extends Module {
  static base = { $('#aiVersionChart') }

  static content = {
    chart { $('#aiVersionChartViz svg') }
    labels { $('#aiVersionChartLabels text')*.text() }
  }
}

