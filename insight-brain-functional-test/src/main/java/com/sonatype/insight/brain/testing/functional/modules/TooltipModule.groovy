/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import geb.Module

class TooltipModule
    extends Module
{
  static content = {
    tooltipTarget(required: false) { $() }
    body(required: false) {tooltipTarget.parents('body')}
    tooltip(required: false) { body.find('.tooltip .tooltip-inner') }
  }

  String getTooltipContent() {
    //move to this element first to clear existing tooltip
    page.interact {
      moveToElement(body);
    }

    waitFor { !tooltip.displayed }

    page.interact {
      moveToElement(tooltipTarget)
    }

    waitFor { tooltip.displayed }

    return tooltip.text()
  }
}