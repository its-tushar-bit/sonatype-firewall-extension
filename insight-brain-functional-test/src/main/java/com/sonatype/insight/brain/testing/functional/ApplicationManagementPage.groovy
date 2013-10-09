/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import geb.Page

class ApplicationManagementPage extends Page {
  static url = "assets/index.html#/management/application"

  static at = {  newApplicationButton.displayed }

  static content = {
    newApplicationButton(required: false) { $('div.new-entity-button').find('a', 'href':contains('#/management/application/')) }
  }
}
