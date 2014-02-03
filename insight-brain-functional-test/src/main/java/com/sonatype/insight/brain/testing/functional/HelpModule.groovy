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
class HelpModule extends Module {
  static base = { $('#help') }

  static content = {
    dropdown { $('a', 'data-toggle': 'dropdown') }
    links(required: false) { $('li a') }
    documentation(required: false) { links.find { it.text().contains('Online Help') } }
    support(required: false) { links.find { it.text().contains('Request Support') } }
  }
}
