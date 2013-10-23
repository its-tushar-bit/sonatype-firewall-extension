/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import geb.Page

class ApplicationPage extends Page {
  static at = {  applicationImage.displayed }

  static content = {
    applicationImage(required: false) { $('div.editor-image') }
    applicationName(required: false) { $('#applicationName') }
    applicationNameField(required: false) { $('input', 'placeholder':'Enter Application Name') }
    applicationId(required: false) { $('#applicationPublicId') }
    applicationIdField(required: false) { $('input', 'placeholder':'Enter ID') }
    applicationOrgField(required: false) { $('a', text:contains('Select Organization')) }
    applicationSaveButton(required: false) { $('button', text:'Save') }
    securityTabButton(required: false) { $('div', 'on': 'selectedApplication.id').find('a', text: 'SECURITY') }
    securityTab(required: false) { $('#security') }
    deleteButton(required: false) { $('a', 'title': 'Remove Application') }
    deleteButtonAccept(required: false) { $('button', 'ng-click':'deleteApplication();') }
    developerRole(required: false) { $('p', text:'Developer' ) }
    ownerRole(required: false) { $('p', text:'Owner' ) }
  }
}
