/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.viewdetails

import geb.Module
import geb.Page

/**
 * @since 1.12
 */
abstract class AbstractViewDetailsPage
    extends Page
{
  static at = { sectionHeaders.size > 2 }

  static content = {
    sectionHeaders(wait: true) { $('h5')*.text().findAll{ it.trim() } }
    noPolicyViolations { $('#no-policy-violations') }
    policyViolationTable(required: false) { module PolicyViolationTableModule, $('h5.policy-header ~ table') }

    noLicenseForUnknown { $('#license-unknown') }
    noLicenseForClaimed { $('#license-claimed') }
    licenseAnalysisTable(required: false) { module LicenseViolationTableModule, $('h5.license-header ~ table') }

    noSecurityForUnknown { $('#security-unknown') }
    noSecurityForClaimed { $('#security-claimed') }
    noSecurity { $('#security-none') }
    securityViolationTable(required: false) { module SecurityViolationTableModule, $('h5.security-header ~ table') }
  }
}

class PolicyViolationTableModule
    extends Module
{
  static content = {
    headers { $('th')*.text() }
  }
}

class LicenseViolationTableModule
    extends Module
{
  static content = {
    headers { $('th')*.text() }
    rows { moduleList LicenseViolationTableRow, $('tbody tr') }
  }
}

class LicenseViolationTableRow
    extends Module
{
  static final int THREAT_LEVEL = 0

  static final int POLICY_NAME = 1

  static final int OVERRIDDEN = 2

  static final int DECLARED = 3

  static final int OBSERVED = 4

  static content = {
    cell { int i -> $('td', i) }
    policyName { cell(POLICY_NAME).text() }
    declaredLicense { cell(DECLARED).text() }
    observedLicense { cell(OBSERVED).text() }
  }
}

class SecurityViolationTableModule
    extends Module
{
  static content = {
    headers { $('th')*.text() }
  }
}

