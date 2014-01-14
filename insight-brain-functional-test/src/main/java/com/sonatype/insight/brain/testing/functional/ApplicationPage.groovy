/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

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
    applicationOrgField(required: false) { $('div', 'on': 'selectedApplication.id && selectedApplication.organizationId').find('a') }
    applicationOrgName(required: false) { orgName -> $('a', text: orgName) }
    applicationSaveButton(required: false) { $('button', text:'Save') }
    applicationCancelButton(required: false) { $('button', text:'Cancel') }
    securityTabButton(required: false) { $('div', 'on': 'selectedApplication.id').find('a', text: 'SECURITY') }
    securityTab(required: false) { $('#security') }
    developerRole(required: false) { $('p', text:'Developer' ) }
    ownerRole(required: false) { $('p', text:'Owner' ) }

    policyMonitoring { module PolicyMonitoringModule }

    tabs { module ContextTabsModule }
    tools { module EditorToolsModule }
  }
}
