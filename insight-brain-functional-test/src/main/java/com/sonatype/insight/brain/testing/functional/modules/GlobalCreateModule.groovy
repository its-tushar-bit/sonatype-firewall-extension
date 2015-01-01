/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import com.sonatype.insight.brain.testing.functional.utils.BrowserInfo

import geb.Module

class GlobalCreateModule
    extends Module
{
  static base = { $('#global-create-menu') }

  static content = {
    dropdown {
      def navigator = $('#global-create-dropdown-toggle')
      BrowserInfo.chrome ? navigator.parent() : navigator
    }
    newOrganization(required: false) { $('#global-create-org') }
    newApplication(required: false) { $('#global-create-app') }
    newPolicy(required: false) { $('#global-create-policy') }
    newLabel(required: false) { $('#global-create-label') }
    newLicenseThreatGroup(required: false) { $('#global-create-ltg') }
    newTag(required: false) { $('#global-create-tag') }
  }
}
