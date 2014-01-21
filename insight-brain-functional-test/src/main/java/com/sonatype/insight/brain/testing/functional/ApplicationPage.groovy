/**
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.ButtonsModule
import com.sonatype.insight.brain.testing.functional.modules.EditorToolsModule
import com.sonatype.insight.brain.testing.functional.modules.ContextTabsModule
import com.sonatype.insight.brain.testing.functional.modules.PolicyMonitoringModule


class ApplicationPage extends ApplicationManagementPage {
  static at = {  applicationImageWrapper.displayed }

  static content = {
    applicationImageWrapper(wait: true) { $('div.editor-image') }
    applicationImage(wait: true) { $('#userIcon') }
    applicationImageFileDialog(wait: true) { $('#file') }
    applicationName(required: false) { $('#aoName .editable') }
    applicationNameField(required: false) { $('input', 'placeholder':'Enter Application Name') }
    applicationId(required: false) { $('#applicationPublicId .editable') }
    applicationIdField(required: false) { $('input', 'placeholder':'Enter ID') }
    applicationIdSaved(required:false){ $('div.setappid')}
    applicationOrgField(required: false) { $('div', 'on': 'selectedApplication.id && selectedApplication.organizationId').find('a') }
    applicationOrgName(required: false) { orgName -> $('a', text: orgName) }

    applicationContactField(required: false) { $('#contact-field') }
    applicationContactDialog(wait: true) { $('#contact-modal-dialog') }
    applicationContactDialogSearchField(required: false) { $('input', 'placeholder':'Find User') }
    applicationContactDialogResultList(required : false) { $('.large-select-list-item') }

    securityTabButton(required: false) { $('div', 'on': 'selectedApplication.id').find('a', text: 'SECURITY') }
    securityTab(required: false) { $('#security') }
    developerRole(required: false) { $('p', text:'Developer' ) }
    ownerRole(required: false) { $('p', text:'Owner' ) }
    buttons { module ButtonsModule, $('#aoEditor') }
    applicationSaveButton(required: false) { buttons.save }
    applicationCancelButton(required: false) { buttons.cancel }
    deleteButton(required: false) { $('a', 'title': 'Remove Application') }
    deleteButtonAccept(required: false) { $('button', text: 'Delete') }
    developerRole(required: false) { $('p', text: 'Developer') }
    ownerRole(required: false) { $('p', text: 'Owner') }

    policyMonitoring { module PolicyMonitoringModule }

    tabs { module ContextTabsModule }
    tools { module EditorToolsModule }
  }

  void createApp(String name = 'test application', String id = 'test application', String orgName = 'test organization'){
    newApplicationButton.click()
    applicationId.click()
    waitFor { applicationIdField.displayed }
    applicationIdField = id
    applicationOrgField.click()
    waitFor { applicationOrgName(orgName).displayed }
    applicationOrgName(orgName).click()
    editApp(name)
  }

  void editApp(name = 'test application') {
    applicationName.click()
    waitFor { applicationNameField.displayed }
    applicationNameField = name
    applicationSaveButton.click()
  }
}
