/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.DashboardNavigation
import com.sonatype.insight.brain.testing.functional.modules.NavListModule


class ManagementPage
extends BasePage {
  static url = "assets/index.html#/management/application"

  static at = { title == 'CLM Management' }

  static content = {
    dashboardNavigation { module DashboardNavigation }
    nav { module NavListModule }
  }
}
