/**
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import geb.Module

class ReportViolationsRow
    extends Module 
{
  static content = {
    name { $('td:first-child a') }
    contactName { $('td:nth-child(5) span') }
    orgName { $('td:last-child a') }
  }
}
