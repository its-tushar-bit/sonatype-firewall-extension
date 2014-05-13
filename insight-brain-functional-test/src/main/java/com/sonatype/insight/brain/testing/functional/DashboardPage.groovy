/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

/**
 @since 1.11
 */
class DashboardPage
extends BasePage {
  static url = "assets/index.html#/dashboard"

  static at = { breadcrumb.displayed }

  static content = {
    breadcrumb { $('p.nav-crumb') }
    breadcrumbs { breadcrumb.find('a') }
    crumb { state -> breadcrumb.find("[ui-sref='${state}']") }
  }
}
