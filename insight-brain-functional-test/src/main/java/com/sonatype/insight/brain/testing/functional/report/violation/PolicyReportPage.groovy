/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
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
    policyContent(wait: true) { $('#componentTable .grid-canvas', 0) }
    results { moduleList PolicyReportRow, policyContent.children('.slick-row') }
    resultsWithNoScore { results.findAll { it.threatGroup == none } }
    waiver(required: false) { module AddPolicyWaiver, $('#add-waiver-modal') }
    policyDetailWaivers(required: false) { module PolicyDetailWaivers, $('#componentExistingWaiverModal') }
    removeWaiverModal(required: false) { module RemoveWaiverModal, $('#confirm-delete-waiver-modal') }
    summaryViolations { $('#policy-violation-filter li a', text: 'Summary') }
    allViolations { $('#policy-violation-filter li a', text: 'All') }
    waivedViolations { $('#policy-violation-filter li a', text: 'Waived') }
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
    waived(required: false) { $("img[src='flag_white.png']") }

    // private, use page methods for interaction
    // click a cell, not the row, to make the CIP appear
    showCipTrigger { $('.slick-cell.scoreCol') }
  }

  Cip showCip() {
    showCipTrigger.click()
    waitFor { cip.displayed }

    return cip
  }

  def closeCip() {
    showCipTrigger.click()
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
    licenses(required: false) { module LicenseModule }
    auditLog(required: false) { module AuditLogModule }
    componentInfo(required: false) { module ComponentInfoModule }
    // implement other tabs as needed
  }
}

class ComponentInfoModule
    extends Module
{
  static content = {
    detailContainer { $('#version-graph') }
    effectiveLicense(required: false) { $('#artifactInfoEffectiveLicenseRow td:nth-child(2) span') }
    showTrigger { $('a', text: 'Component Info') }
  }

  void show() {
    showTrigger.click()
    waitFor { detailContainer.displayed }
  }
}

class PolicyDetail
    extends Module
{
  static content = {
    viewWaiversButton { $('#view-existing-waivers') }
    
    violations(wait: true) { moduleList PolicyRow, $('tbody tr') }

    // private
    detailContainer { $('table.cip-policy-table') }

    // private, use page methods for interaction
    // assume that there will be no anchors with the same name
    showTrigger { $('a', text: 'Policy') }
  }

  void showWaivers() {
    viewWaiversButton.click()
  }
  
  void show() {
    showTrigger.click()
    waitFor { detailContainer.displayed }
  }
}

class PolicyDetailWaivers
    extends Module
{
  static content = {
    rows { moduleList WaiverRow, $('div.modal-body table.table.table-condensed tr').tail() }
    noWaivers { $('#no-waivers-assigned') }
    closeButton(required: false) { $('#close-component-existing-waivers') }
  }
  
  void close() {
    closeButton.click()
  }
}

class WaiverRow
    extends Module
{
  static final int POLICY = 0

  static final int CREATED = 1

  static final int OWNER = 2

  static final int COMMENT = 3
  
  static final int REMOVE_BUTTON = 4

  static content = {
    cell(required: false) { int i -> $('td', i) }
    policy { cell(POLICY) }
    created { cell(CREATED) }
    owner { cell(OWNER) }
    comment { cell(COMMENT) }
    removeWaiverButton { cell(REMOVE_BUTTON).children('#remove-waiver') }
  }
  
  void showRemoveWaiverModal() {
    removeWaiverButton.click()
  }
}

class RemoveWaiverModal
    extends Module
{
  static content = {
    cancelButton { $('#cancel-remove-waiver') }
    removeButton { $('#confirm-remove-waiver') }
  }
  
  void cancel() {
    cancelButton.click()
  }
  
  void remove() {
    removeButton.click()
  }
}

class PolicyRow
    extends Module
{
  static content = {
    waiver { module AddPolicyWaiver, parents().find('#add-waiver-modal') }

    // private, use page methods for interaction
    addWaiverTrigger { $('button.btn-primary') }
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
    // private
    commentTextArea { $('#add-waiver-modal textarea') }

    isImplicitScope { scopeContainer.displayed == false }
    // input for the scope/limit of the waiver (orgId or appPublicId)
    scope { $('#add-waiver-scope input[name="waiverSelectedTarget"]') }
    // input for the application of the waiver (selectedComponent or allComponents)
    apply { $('#add-waiver-apply input[name="waiver-hash"]') }
    // input value for option to apply to all components
    allComponents { $('#add-waiver-apply label.radio', text: iContains('all components')).text() }
    // input value for option to apply to selected component
    selectedComponent { $('#add-waiver-apply label.radio', text: iContains('selected component')).text() }

    // private, use page methods for interaction
    saveTrigger { $('button.btn-primary') }
    cancelTrigger { $('button:nth-child(2)') }
  }

  void setComment(String comment) {
    commentTextArea = comment
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

/**
 * Models the License tab of the Cip
 */
class LicenseModule
    extends Module
{
  static content = {
    form { $('form[name=licenseEditorForm]') }
    buttons { module ButtonsModule, form }
    update(required: false) { buttons.button('Update') }
    declaredLicenses { form.find('#declaredLicenseBlock').text() }
    observedLicenses { form.find('#observedLicenseBlock').text() }
    effectiveLicense { form.find('#effectiveLicenseBlock').text() }
    showTrigger { $('a', text: 'Licenses') }
    licenseOptionShown(required: false) { form.find('select[name=license]').displayed }
    selectedScope { selectedOptionText(form.scope()) }
    selectedStatus { selectedOptionText(form.status()) }
    selectedLicense(required: false) { licenseOptionShown ? selectedOptionText(form.license()) : '' }
    selectedOptionText { field -> field.find('option', value: field.value()).text() }
  }

  boolean validateLicense(declared, observed, effective, scope, status, selected, comment, updateEnabled) {
    assert declaredLicenses == declared
    assert observedLicenses == observed
    assert effectiveLicense == effective
    assert selectedScope == scope
    assert selectedStatus == status
    assert selectedLicense == selected
    assert form.comment == comment
    assert update.enabled == updateEnabled
    return true // assertions used for better output, but still need a truthy return value to use in Geb expectations
  }
}

/**
 * Models the Audit Log tab of the Cip
 */
class AuditLogModule
    extends Module
{
  static content = {
    noChangesMessage(required: false) { $('.tab-content').text() }
    auditTable(required: false) { $('#auditTable') }
    audits { moduleList AuditLogRow, auditTable.find('.slick-row') }
    showTrigger { $('a', text: 'Audit Log') }
  }

  def validateRow(AuditLogRow auditLogRow, String user, String action, String detail, String comment) {
    assert auditLogRow.user == user
    assert auditLogRow.action == action
    assert auditLogRow.detail == detail
    assert auditLogRow.comment == comment
    return true // assertions used for better output, but still need a truthy return value to use in Geb expectations
  }
}

class AuditLogRow
    extends Module
{
  static final int DATE = 0

  static final int USER = 1

  static final int ACTION = 2

  static final int DETAIL = 3

  static final int COMMENT = 4

  static content = {
    cell(required: false) { int i -> $('.slick-cell', i) }

    date { cell(DATE).text().trim() }
    user { cell(USER).text() }
    userTooltip { cell(USER).@tooltip }
    action { cell(ACTION).text() }
    detail { cell(DETAIL).text() }
    comment { cell(COMMENT).text() }
  }
}
