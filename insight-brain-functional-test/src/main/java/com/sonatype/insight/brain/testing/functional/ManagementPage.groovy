/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.ChangePasswordModule
import com.sonatype.insight.brain.testing.functional.modules.DropdownNav
import com.sonatype.insight.brain.testing.functional.modules.NavListModule
import com.sonatype.insight.brain.testing.functional.modules.UserControlsModule

import geb.Page

class ManagementPage
extends Page {
  static url = "assets/index.html#/management/application"

  static at = { title == 'CLM Management' }

  static content = {
    dropdownNav { module DropdownNav }
    nav { module NavListModule }
    changePassword { module ChangePasswordModule }
    user { module UserControlsModule }
  }
}
