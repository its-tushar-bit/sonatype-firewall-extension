/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import geb.Module

class ContextTabsModule
    extends Module
{
  static content = {
    tabLinks { $('ul.tri-pane a') }
    policiesTabButton { tabLinks.find { it.text() == 'POLICIES' } }
    policiesTab { $('#policy') }
    labelsTabButton { tabLinks.find { it.text() == 'LABELS' } }
    labelsTab { $('#labels') }
    ltgTabButton { tabLinks.find { it.text() == 'LICENSES' } }
    ltgTab { $('#ltg') }
    securityTabButton { tabLinks.find { it.text() == 'SECURITY' } }
    securityTab { module RoleMappingModule }
    tagTabButton { tabLinks.find { it.text() == 'TAGS' } }
    tagTab { $('#tags') }
  }
}