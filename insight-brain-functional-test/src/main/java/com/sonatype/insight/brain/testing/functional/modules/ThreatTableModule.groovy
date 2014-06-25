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
    headers { $('th') }
    headerLinks { int i -> $('th a', i) }
    threatHeader { headerLinks(0) }
    ageHeader { headerLinks(1) }
    policyHeader { headerLinks(2) }
    applicationHeader { headerLinks(3) }
    componentHeader { headerLinks(4) }
    rows(required: false) { moduleList ThreatTableRow, $('tr').tail(), stageColumns: getStageColumns() }

    buildHeader(required: false) { $('#stage-header-build') }
    stageHeader(required: false) { $('#stage-header-stage-release') }
    releaseHeader(required: false) { $('#stage-header-release') }
    operateHeader(required: false) { $('#stage-header-operate') }
  }

  List<String> getStageColumns() {
    List<String> headers = []
    if (buildHeader.displayed) {
      headers.push('build')
    }
    if (stageHeader.displayed) {
      headers.push('stage-release')
    }
    if (releaseHeader.displayed) {
      headers.push('release')
    }
    if (operateHeader.displayed) {
      headers.push('operate')
    }
    return headers;
  }

  def clickStageHeader(header) {
    header.find('a').click()
  }

  def isUp(header) {
    return header.find('i').hasClass('up')
  }

  def isDown(header) {
    return header.find('i').hasClass('down')
  }
}

class ThreatTableRow
    extends Module
{
  static final int RISK_COLOR = 0

  static final int THREAT = 1

  static final int AGE = 2

  static final int POLICY = 3

  static final int APPLICATION = 4

  static final int COMPONENT = 5

  List<String> stageColumns

  static content = {
    cell(required: false) { int i -> $('td', i) }
    threat { cell(THREAT).text().toInteger() }
    age { cell(AGE).text() }
    policy { cell(POLICY).text() }
    application { cell(APPLICATION).text() }
    component { cell(COMPONENT).text() }
    componentLink { cell(COMPONENT).find('a') }

    buildAge(required: false) { cell(getStageColumnIndex('build')).text() }
    operateAge(required: false) { cell(getStageColumnIndex('operate')).text() }
    releaseAge(required: false) { cell(getStageColumnIndex('release')).text() }
    stageReleaseAge(required: false) { cell(getStageColumnIndex('stage-release')).text() }

    isLatestRisk { String stageId -> cell(getStageColumnIndex(stageId)).classes().contains('latest-risk') }
    isMarkedAsWarn { String stageId -> cell(getStageColumnIndex(stageId)).find('i').classes().contains('warn') }
    isMarkedAsFail { String stageId -> cell(getStageColumnIndex(stageId)).find('i').classes().contains('fail') }
  }

  int getStageColumnIndex(String stageId) {
    int index = stageColumns.indexOf(stageId)
    if (index > -1) {
      return COMPONENT + 1 + index;
    }
    return -1;
  }
}
