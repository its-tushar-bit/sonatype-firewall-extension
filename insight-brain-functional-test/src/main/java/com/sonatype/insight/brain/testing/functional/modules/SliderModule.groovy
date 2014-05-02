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
}
