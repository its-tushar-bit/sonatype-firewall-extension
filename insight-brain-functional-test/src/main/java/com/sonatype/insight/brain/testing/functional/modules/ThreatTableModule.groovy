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
    unknownComponentPopover(required: false) { $('div.popover.pathnames-popover.fade.top.in') }
    unknownComponentPopoverTitle(required: false) { $('div.popover.pathnames-popover.fade.top.in .popover-title').text() }
    unknownComponentPopoverText(required: false) { $('div.popover.pathnames-popover.fade.top.in .popover-content.pathnames-popover-content').text() }
    maxResults(required: false) { $('#max-results-shown') }
  }
}

class ThreatTableRow
    extends Module
{
  static final int RISK_COLOR = 0

  static final int RISK = 1

  static final int AGE = 2

  static final int POLICY = 3

  static final int APPLICATION = 4

  static final int COMPONENT = 5

  static final int BUILD_AGE = 6

  static final int STAGE_RELEASE_AGE = 7

  static final int RELEASE_AGE = 8

  static final int OPERATE_AGE = 9


  static content = {
    cell(required: false) { int i -> $('td', i) }
    risk { cell(RISK).text().toInteger() }
    age { cell(AGE).text() }
    policy { cell(POLICY).text() }
    application { cell(APPLICATION).text() }
    component { cell(COMPONENT).text() }
    componentLink { cell(COMPONENT).find('a') }
    buildAge { cell(BUILD_AGE).text() }
    operateAge { cell(OPERATE_AGE).text() }
    releaseAge { cell(RELEASE_AGE).text() }
    stageReleaseAge { cell(STAGE_RELEASE_AGE).text() }
  }
}
