/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.ImportPolicyModule
import com.sonatype.insight.brain.testing.functional.modules.LabelModule
import com.sonatype.insight.brain.testing.functional.modules.ModalModule
import com.sonatype.insight.brain.testing.functional.modules.ContextTabsModule
import com.sonatype.insight.brain.testing.functional.modules.EditorToolsModule
import com.sonatype.insight.brain.testing.functional.modules.PolicyModule
import com.sonatype.insight.brain.testing.functional.modules.PolicyMonitoringModule
import com.sonatype.insight.brain.testing.functional.modules.TagModule


class OrganizationPage
    extends OrganizationManagementPage
{
  static at = { $('#organizationEditor').displayed }

  static content = {
    organizationImage(wait: true) { $('div.editor-image') }
    organizationName(required: false) { $('#aoName .editable') }
    organizationNameField(required: false) { $('input', 'placeholder':'Enter Organization Name') }
    organizationSaveButton(required: false) { $('button', text:'Save') }
    deleteButton(required: false) { $('a', 'title': 'Remove Organization') }
    deleteButtonAccept(required: false) { $('button', text:'Delete') }
    developerRole(required: false) { $('p', text:'Developer') }
    ownerRole(required: false) { $('p', text:'Owner') }

    tabs { module ContextTabsModule }
    tools(required: false) { module EditorToolsModule }
    policies { module PolicyModule, tabs.policiesTab }
    policyMonitoring { module PolicyMonitoringModule, tabs.policiesTab }
    labels { module LabelModule, tabs.labelsTab }
    tags { module TagModule, tabs.tagTab }
    deleteModal { module ModalModule, title: startsWith('Delete ') }
    policyImport { module ImportPolicyModule }
    isEditingModal { module ModalModule, title: 'Unsaved Changes' }
  }

  def editOrg(name) {
    organizationName.click()
    waitFor { organizationNameField.displayed }
    organizationNameField = name
    organizationSaveButton.click()
    waitFor { !organizationSaveButton.displayed }
  }
}
