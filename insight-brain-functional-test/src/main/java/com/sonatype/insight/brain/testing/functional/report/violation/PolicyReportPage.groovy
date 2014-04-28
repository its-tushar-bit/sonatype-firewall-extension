/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.report.violation

import com.sonatype.insight.brain.testing.functional.modules.ButtonsModule
import com.sonatype.insight.brain.testing.functional.modules.ModalModule
import geb.Module
import geb.Page

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
    navigation { module ReportSubNavigation }
    policyContent(wait: true) { $('div', class: 'slick-viewport') }
    results { moduleList PolicyReportRow, $(class: 'slick-row') }
    resultsWithNoScore { results.findAll { it.threatGroup == none } }
    waiver { module AddPolicyWaiver, $('#add-waiver-modal') }
    summaryViolations { $('#policy-violation-filter li a', text : 'Summary') }
    allViolations { $('#policy-violation-filter li a', text : 'All') }
    waivedViolations { $('#policy-violation-filter li a', text : 'Waived') }
    selectedViolationFilter { $('#policy-violation-filter li.active a').text() }
    revokeClaimModal(required: false) { module ModalModule, title: 'Revoke Claim' }
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
    cip { module Cip, parent().find('#informationPanel') }
    // can't rely on the text within the cell since it's only shown for the first row with that score
    threatGroup { $(class: iEndsWith('Score')).classes()[0] }
    coordinates { $('.l1').text() }
    waived(required : false) { $('.waiver-icon-container') }

    // private, use page methods for interaction
    // click a cell, not the row, to make the CIP appear
    showCipTrigger { $(class:'slick-cell scoreCol') }
  }

  Cip showCip() {
    showCipTrigger.click()
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
    claimComponent(required: false) { module ClaimComponentModule }
    // implement other tabs as needed
  }
}

class PolicyDetail
    extends Module 
{
  static content = {
    violations(wait: true) { moduleList PolicyRow, $('tbody tr') }

    // private
    detailContainer { $('table', class: 'cip-policy-table') }

    // private, use page methods for interaction
    // assume that there will be no anchors with the same name
    showTrigger { $('a', text: 'Policy') }
  }

  void show() {
    showTrigger.click();
    waitFor { detailContainer.displayed }
  }
}

class PolicyRow
    extends Module 
{
  static content = {
    waiver { module AddPolicyWaiver, parents().find('#add-waiver-modal') }

    // private, use page methods for interaction
    addWaiverTrigger { $('button', text: 'Waive') }
  }

  AddPolicyWaiver addWaiver() {
    addWaiverTrigger.click()
    waitFor { waiver.apply.displayed }

    return waiver
  }
}

class AddPolicyWaiver
    extends Module 
{
  static content = {
    // private
    scopeContainer { $('#add-waiver-scope') }
    // private
    applyContainer { $('#add-waiver-apply') }

    isImplicitScope { scopeContainer.displayed == false }
    // input for the scope/limit of the waiver (orgId or appPublicId)
    scope { scopeContainer.find('input', name: 'waiverSelectedTarget') }
    // input for the application of the waiver (selectedComponent or allComponents)
    apply { applyContainer.find('input', name: 'waiver-hash') }
    // input value for option to apply to all components
    allComponents { applyContainer.find('label', class: 'radio', text: iContains('all components')).text() }
    // input value for option to apply to selected component
    selectedComponent { applyContainer.find('label', class: 'radio', text: iContains('selected component')).text() }

    // private, use page methods for interaction
    saveTrigger { $('button', text: 'Waive') }
    cancelTrigger { $('button', text: 'Cancel') }
  }

  void save() {
    saveTrigger.click()
  }

  void cancel() {
    cancelTrigger.click()
  }
}

class ClaimComponentModule
    extends Module
{
  static content = {
    claimForm { $('form[name=claimForm]') }
    buttons { module ButtonsModule, claimForm }
    claim(required: false) { buttons.button('Claim') }
    revoke(required: false) { buttons.button('Revoke Claim') }
    update(required: false) { buttons.button('Update') }
    showTrigger { $('a', text: 'Claim Component') }
  }
}