/**
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import geb.Module

/**
 * @since 1.9
 */
class HelpModule
    extends Module 
{
  static base = { $('#help') }

  static content = {
    dropdown { $('a', 'data-toggle': 'dropdown') }
    documentation(required: false) { $('#documentation-link') }
    support(required: false) { $('#support-link') }
  }
}
