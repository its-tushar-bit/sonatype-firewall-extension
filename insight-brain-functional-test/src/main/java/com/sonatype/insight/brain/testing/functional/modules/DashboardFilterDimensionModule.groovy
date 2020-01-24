/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import geb.Module;

public class DashboardFilterDimensionModule
    extends Module
{
  static content = {
    nxTwisty { $('.nx-tree-view__twisty') }
    nxCounter { $('.nx-counter') }
    twisty { $('.iq-tree-view__twisty') }
    counter { $('.iq-counter') }
    trigger { $('.iq-tree-view__trigger')}
    multiSelectList(required: false) { $('.iq-tree-view__child .iq-checkbox__label').moduleList(FilterCheckboxRow) }
    tooltip { module TooltipModule }
  }

  boolean isCounterInactive() {
    return counter.hasClass('inactive')
  }
}

class FilterCheckboxRow
    extends Module
{
  static content = {
    checkbox(required: true) { $('input', type: 'checkbox') }
    name(required: true) { $('span') }
  }
}
