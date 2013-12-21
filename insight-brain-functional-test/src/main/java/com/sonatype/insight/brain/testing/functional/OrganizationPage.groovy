/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.ContextTabsModule
import com.sonatype.insight.brain.testing.functional.modules.PolicyMonitoringModule


class OrganizationPage extends OrganizationManagementPage {
  static at = {  organizationImage.displayed }

  static content = {
    organizationImage(wait: true) { $('div.editor-image') }
    organizationName(required: false) { $('#aoName .editable') }
    organizationNameField(required: false) { $('input', 'placeholder':'Enter Organization Name') }
    organizationSaveButton(required: false) { $('button', text:'Save') }
    securityTabButton(required: false) { $('.tri-pane').find('a', text: 'SECURITY') }
    securityTab(required: false) { $('#security') }
    deleteButton(required: false) { $('a', 'title': 'Remove Organization') }
    deleteButtonAccept(required: false) { $('button', 'ng-click':'deleteOrganization();') }
    developerRole(required: false) { $('p', text:'Developer') }
    ownerRole(required: false) { $('p', text:'Owner') }

    policyMonitoring { module PolicyMonitoringModule }

    tabs(required: false) { module ContextTabsModule }
  }
}
