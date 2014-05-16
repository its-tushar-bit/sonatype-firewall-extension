/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules;

import geb.Module

class DashboardTabsModule
  extends Module {

  static content = {
    applicationsTabButton { $('li:nth-child(2) a') }
    componentsTabButton { $('li:nth-child(3) a') }
    policyViolationsTabButton { $('li:nth-child(4) a') }
    newestRiskTabButton { $('li:nth-child(5) a') }
  }
}
