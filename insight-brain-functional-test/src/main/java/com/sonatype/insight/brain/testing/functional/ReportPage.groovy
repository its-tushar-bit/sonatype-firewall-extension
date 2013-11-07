/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.DropdownNav
import com.sonatype.insight.brain.testing.functional.modules.NavListModule
import com.sonatype.insight.brain.testing.functional.modules.UserControlsModule

import geb.Page

class ReportPage extends Page {
  static url = "assets/reports.html#/reports/violations"

  static at = { title == 'CLM Reports' }

  static content = {
    dropdownNav { module DropdownNav }
    nav { module NavListModule }

    emptyMessage { $('div h5', text: startsWith('Welcome to Sonatype CLM. Get started by')) }
    user { module UserControlsModule }
  }
}
