/**
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import geb.Module
import geb.Page

/**
 * When navigating to this page the public application id and scan must be supplied, in that order.  For example:
 *
 * to GroovyReportPage, app.publicId, scanId
 */
class GroovyReportPage
    extends Page
{
  /**
   * The proper url will be created from the supplied appPublicId and scanId and should look like:
   * rest/report/{appPublicId}/{scanId}/browseReport/index.html
   */
  static url = 'rest/report'

  String convertToPath(Object[] args) {
    args ? '/' + args*.toString().join('/') + '/browseReport/index.html' : ""
  }

  static at = { $('div', class:'container') }

  static content = {
    policyButton(to: PolicyReportPage) { $('#componentcontainerBtn') }
  }

  void toPolicyReportPage() {
    policyButton.click()
  }
}

class PolicyReportPage
    extends Page 
{
  static at = { policyContent.displayed }

  /**
   * Indicates a policy that is in the 'severe threat' group. Used with equality checks on the threatGroup for a
   * PolicyReportRow.
   */
  static severe = 'severeScore'

  /**
   * Indicates a policy that is in the 'no threat' group. Used with equality checks on the threatGroup for a
   * PolicyReportRow.
   */
  static none = 'noScore'

  static content = {
    policyContent(wait: true) { $('div', class: 'slick-viewport') }
    results { moduleList PolicyReportRow, $(class: 'slick-row') }
    resultsWithNoScore { results.findAll { it.threatGroup == none } }
    waiver { module AddPolicyWaiver, $('#add-waiver-modal') }
  }
}

/**
 * Represents a row within the policy report table.
 *
 * The threatGroup can be compared to the constants in {@link PolicyReportPage}.  E.g. results[0].threatGroup == none
 *
 * The CIP for this row can be opened with {@link #showCip()}.
 */
class PolicyReportRow
    extends Module 
{
  static content = {
    cip { module Cip, parents().find('#informationPanel') }
    // can't rely on the text within the cell since it's only shown for the first row with that score
    threatGroup { $(class: iEndsWith('Score')).classes()[0] }
  }

  Cip showCip() {
    // click a cell, not the row, to make the CIP appear
    def activator = $(class:'slick-cell scoreCol')
    activator.click()
    waitFor { cip.displayed }

    return cip
  }

  AddPolicyWaiver addWaiverForFirstViolation() {
    def policyDetail = showCip().policy
    policyDetail.show()

    return policyDetail.violations[0].addWaiver()
  }
}

class Cip
    extends Module 
{
  static content = {
    policy { module PolicyDetail }
    // implement other tabs as needed
  }
}

class PolicyDetail
    extends Module 
{
  static content = {
    violations(wait: true) { moduleList PolicyRow, $('tbody tr') }
  }

  void show() {
    // assume that there will be no anchors with the same name
    def activator = $('a', text: 'Policy')
    activator.click();
    waitFor { $('table', class: 'cip-policy-table').displayed }
  }
}

class PolicyRow
    extends Module 
{
  static content = {
    waiver { module AddPolicyWaiver, parents().find('#add-waiver-modal') }
  }

  AddPolicyWaiver addWaiver() {
    def activator = $('button', text: 'Waive')
    activator.click()
    waitFor { waiver.displayed }

    return waiver
  }
}

class AddPolicyWaiver
    extends Module 
{
  static content = {
    isImplicitScope { $('#add-waiver-scope').displayed == false }
    // input for the scope/limit of the waiver (orgId or appPublicId)
    scope { $('#add-waiver-scope').find('input', name: 'waiverSelectedTarget') }
    // input for the application of the waiver (selectedComponent or allComponents)
    apply { $('#add-waiver-apply').find('input', name: 'waiver-hash') }
    // input value for option to apply to all components
    allComponents { $('#add-waiver-apply').find('label', class: 'radio', text: iContains('all components')).text() }
    // input value for option to apply to selected component
    selectedComponent { $('#add-waiver-apply')
      .find('label', class: 'radio', text: iContains('selected component')).text() }
  }

  void save() {
    $('button', text: 'Waive').click()
  }

  void cancel() {
    $('button', text: 'Cancel').click()
  }
}