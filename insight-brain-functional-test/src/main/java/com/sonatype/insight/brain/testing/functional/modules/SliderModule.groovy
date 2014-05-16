/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import geb.Module

/**
 * Since 1.11
 */
class SliderModule
    extends Module
{
  static content = {
    slider { $('.slider') }
    minLabel { slider.previous() }
    maxLabel { slider.next() }
  }

  void setValues(int min, int max) {
    //so rather than putz around trying to drag some handles to precise locations
    //I simply use the javascript api to set the values
    browser.js.exec(slider.firstElement(), '$( arguments[0] ).find("div[slider]").trigger({type:"slide",value: [' + min + ',' + max + ']});')
  }
}
