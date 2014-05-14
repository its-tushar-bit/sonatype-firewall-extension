/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import geb.Module

/**
 * @since 1.11
 */
class ThreatTableModule
    extends Module
{
  static content = {
    headerLinks { int i -> $('th a', i) }
    riskHeader { headerLinks(0) }
    ageHeader(required: false) { headerLinks(1) }
    rows(required: false) { moduleList ThreatTableRow, $('tr').tail() }
    unknownComponentPopover(required: false) { $('div.popover.fade.top.in') }
    unknownComponentPopoverTitle(required: false) { unknownComponentPopover.children('.popover-title').text() }
    unknownComponentPopoverText(required: false) { unknownComponentPopover.children('.popover-content').text() }
    maxResults(required: false) { $('span[id$="-max-results-shown"]') }
  }
}

class ThreatTableRow
    extends Module
{
  static final int RISK_COLOR = 0

  static final int RISK = 1

  static final int POLICY = 2

  static final int APPLICATION = 3

  static final int COMPONENT = 4

  static final int AGE = 5

  static content = {
    cell(required: false) { int i -> $('td', i) }
    risk { cell(RISK).text().toInteger() }
    policy { cell(POLICY).text() }
    application { cell(APPLICATION).text() }
    component { cell(COMPONENT).text() }
    componentLink { cell(COMPONENT).find('a') }
    age(required: false) { cell(AGE).text() }
  }
}
