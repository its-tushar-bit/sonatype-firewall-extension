/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import geb.Module

class GlobalCreateModule
    extends Module 
{
  String browserClassName

  static base = { $('li.dashboard-add') }

  static content = {
    dropdown {
      def navigator = $('a', 'data-toggle': 'dropdown')
      browserClassName == 'ChromeDriver' ? navigator.parent() : navigator
    }
    newOrganization(required: false) { $('#global-create-org') }
    newApplication(required: false) { $('#global-create-app') }
    newPolicy(required: false) { $('#global-create-policy') }
    newLabel(required: false) { $('#global-create-label') }
    newLicenseThreatGroup(required: false) { $('#global-create-ltg') }
    newTag(required: false) { $('#global-create-tag') }
  }
}
