/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import com.sonatype.insight.brain.testing.functional.utils.BrowserInfo

import geb.Module
import geb.navigator.Navigator

/**
 * Exists only to simplify application of workaround for issues like https://github.com/ariya/phantomjs/issues/10592
 * across the board.
 */
class ExpandoModule
    extends Module
{
  static content = {
    expando(required: false) { $() }
  }

  private boolean isPhantomJs() {
    return BrowserInfo.phantom
  }

  @Override
  boolean isDisplayed() {
    if (isPhantomJs()) {
      expando.present
    }
    else {
      expando.displayed
    }
  }

  @Override
  Navigator click() {
    if (isPhantomJs()) {
      browser.js.exec(expando.firstElement(), 'jQuery(arguments[0]).click()')
    }
    else {
      expando.click()
    }
    return this
  }
}
