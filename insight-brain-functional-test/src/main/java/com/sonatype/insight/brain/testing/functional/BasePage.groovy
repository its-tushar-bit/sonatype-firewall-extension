/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.HelpModule
import com.sonatype.insight.brain.testing.functional.modules.LoginModule

import geb.Page

/**
 * Common infrastructure for all pages in the application
 */
abstract class BasePage
    extends Page
{
  static content = {
    login { module LoginModule }
    helpLinks { module HelpModule }
    unsavedModal { $('#unsaved-modal') }
  }
}
