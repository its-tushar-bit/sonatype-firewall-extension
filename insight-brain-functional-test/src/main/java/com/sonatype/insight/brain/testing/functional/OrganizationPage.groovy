/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import geb.Page

class OrganizationPage extends Page {
  static at = {  organizationImage.displayed }

  static content = {
    organizationImage(required: false) { $('div.editor-image') }
    organizationName(required: false) { $('#organizationName') }
    organizationNameField(required: false) { $('input', 'placeholder':'Enter Organization Name') }
    organizationSaveButton(required: false) { $('button', text:'Save') }
    securityTabButton(required: false) { $('div', 'on': 'selectedOrganization.id').find('a', text: 'SECURITY') }
    securityTab(required: false) { $('#security') }
    deleteButton(required: false) { $('a', 'title': 'Remove Organization') }
    deleteButtonAccept(required: false) { $('button', 'ng-click':'deleteOrganization();') }
    developerRole(required: false) { $('p', text:'Developer') }
    ownerRole(required: false) { $('p', text:'Owner') }
  }
}
