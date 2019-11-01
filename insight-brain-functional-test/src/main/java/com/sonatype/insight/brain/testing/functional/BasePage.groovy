/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.HelpModule
import com.sonatype.insight.brain.testing.functional.modules.LoginModule
import com.sonatype.insight.brain.testing.functional.modules.NotificationModule
import com.sonatype.insight.brain.testing.functional.modules.SystemConfigModule
import com.sonatype.insight.brain.testing.functional.modules.UserOptionsModule

import geb.Page

/**
 * Common infrastructure for all pages in the application
 */
abstract class BasePage
    extends Page
{
  static content = {
    login { module LoginModule }
    notificationMenu { module NotificationModule }
    userOptions { module UserOptionsModule }
    helpLinks { module HelpModule }
    systemConfig(required: false) { module SystemConfigModule }
    functionalTestingSupport { $('#functionalTestingSupport') }
    unsavedModal { $('#unsaved-modal') }
  }
}
