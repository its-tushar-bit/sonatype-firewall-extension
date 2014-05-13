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
    policiesTabButton { $('#tab-button-policies') }
    policiesTab { $('#policy') }
    labelsTabButton { $('#tab-button-labels') }
    labelsTab { $('#labels') }
    ltgTabButton { $('#tab-button-licenses') }
    ltgTab { $('#ltg') }
    tagTabButton { $('#tab-button-tags') }
    tagTab { $('#tags') }
    securityTabButton { $('#tab-button-security') }
    securityTab { module RoleMappingModule }
  }
}