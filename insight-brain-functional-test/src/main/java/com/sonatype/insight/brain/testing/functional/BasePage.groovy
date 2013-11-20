/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.ChangePasswordModule
import com.sonatype.insight.brain.testing.functional.modules.LoginModule
import com.sonatype.insight.brain.testing.functional.modules.LogoutModule

import geb.Page

abstract class BasePage extends Page {
  static content = {
    changePassword { module ChangePasswordModule }
    login { module LoginModule }
    logout { module LogoutModule }
  }
}
