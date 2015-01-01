/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

/**
 * @since 1.11
 */
class IndexPage
    extends BasePage
{
  static url = "assets/index.html"

  static at = { DashboardPage.at || ReportViolationsPage.at }
}
