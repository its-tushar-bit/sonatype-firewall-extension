/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
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
  static at = { browser.title == 'Component Detail' }

  static content = {
    sectionHeaders { $('h5')*.text().findAll { it.trim() } }
    noPolicyViolations { $('#no-policy-violations') }
    policyViolationTable(required: false) { module PolicyViolationTableModule, $('h5.policy-header ~ table') }

    noLicenseForUnknown { $('#license-unknown') }
    noLicenseForClaimed { $('#license-claimed') }
    licenseAnalysisTable(required: false) { module LicenseViolationTableModule, $('h5.license-header ~ table') }

    noSecurityForUnknown { $('#security-unknown') }
    noSecurityForClaimed { $('#security-claimed') }
    noSecurity { $('#security-none') }
    securityViolationTable(required: false) { module SecurityViolationTableModule, $('h5.security-header ~ table') }

    error { $('#error-message') }
  }
}

class PolicyViolationTableModule
    extends Module
{
  static content = {
    headers { $('th')*.text() }
    rows { moduleList PolicyViolationTableRow, $('tbody tr') }
  }
}

class PolicyViolationTableRow
    extends Module
{
  static final int THREAT_LEVEL = 0

  static final int POLICY_NAME = 1

  static final int CONSTRAINT_NAME = 2

  static final int SUMMARY = 3

  static content = {
    cell { int i -> $('td', i) }
    policyName { cell(POLICY_NAME).text() }
    constraintName { cell(CONSTRAINT_NAME).text() }
    summary { cell(SUMMARY).text() }
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
    rows { moduleList SecurityViolationTableRow, $('tbody tr') }

  }
}

class SecurityViolationTableRow
    extends Module
{
  static final int THREAT_LEVEL = 1

  static final int PROBLEM_CODE = 2

  static final int STATUS = 3

  static final int SUMMARY = 4

  static content = {
    cell { int i -> $('td', i) }
    threatLevel { cell(THREAT_LEVEL).text().toInteger() }
    problemCode { cell(PROBLEM_CODE).find('a') }
    status { cell(STATUS).text() }
    summary { cell(SUMMARY).text() }
  }
}
