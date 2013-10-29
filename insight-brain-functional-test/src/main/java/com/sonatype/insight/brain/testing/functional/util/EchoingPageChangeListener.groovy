/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.util

import geb.Browser
import geb.Page
import geb.PageChangeListener

/**
 * @since 2.6
 */
class EchoingPageChangeListener
    implements PageChangeListener
{
  @Override
  void pageWillChange(final Browser browser, final Page oldPage, final Page newPage) {
    println "EchoingPageChangeListner: browser '$browser' changing page from '$oldPage' to '$newPage'"
  }
}