/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import geb.Module

class ContextTabsModule extends Module {
  static content = {
    policiesTabButton(required: false) { $('ul.tri-pane').find('a', text: 'POLICIES') }
    policiesTab(required: false) { $('#policy') }
    labelsTabButton(required: false) { $('ul.tri-pane').find('a', text: 'LABELS') }
    labelsTab(required: false) { $('#labels') }
    ltgTabButton(required: false) { $('ul.tri-pane').find('a', text: 'LICENSES') }
    ltgTab(required: false) { $('#ltg') }
    securityTabButton(required: false) { $('ul.tri-pane').find('a', text: 'SECURITY') }
    securityTab(required: false) { module RoleMappingModule }
  }
}