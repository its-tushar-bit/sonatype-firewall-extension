/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.LogoutModule
import geb.Page

class ManagementPage
    extends Page
{
  static url = "assets/index.html#/management/application"

  static at = { title == 'CLM Management' }

  static content = {
    user { module LogoutModule }
  }
}
