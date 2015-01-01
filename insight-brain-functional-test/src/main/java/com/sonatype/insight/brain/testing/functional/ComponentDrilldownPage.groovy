/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.ExpandoModule

import geb.Module

/**
 * Represents the dashboard's Component Details view
 * @since 1.11
 */
class ComponentDrilldownPage
    extends DashboardPage
{
  static at = { totalRisk.displayed }

  static content = {
    componentName { $('#component-name') }
    totalRisk { $('#total-risk') }

    componentApplicationTable { $('.component-application-table') }
    componentApplicationRow { applicationId -> module ComponentApplicationRow, $("#app-row-${applicationId}") }
    componentViolationTable { applicationId -> $("div[id\$='${applicationId}'] .table") }
    componentViolationRow { applicationId, policyViolationName ->
      module ComponentViolationRow,
          componentViolationTable(applicationId).find('tr').has("td", text: "${policyViolationName}")
    }
    header(required: false) { int i -> $('thead th', i).text() }
  }
}

class ComponentRow
    extends Module
{
  static abstract int RISK_PIE

  static abstract int RISK_COUNT

  static abstract int BUILD

  static abstract int STAGE

  static abstract int RELEASE

  static abstract int OPERATE

  static content = {
    cell(required: false) { int i -> $('td', i) }
    cellIcon(required: false) { int i -> $('td', i).find('i') }
    riskPie { cell(RISK_PIE).text().trim() }
    riskCount { cell(RISK_COUNT).text().toInteger() }
    build { cell(BUILD).text() }
    stage { cell(STAGE).text() }
    release { cell(RELEASE).text() }
    operate { cell(OPERATE).text() }
  }

  boolean isFail(int cell) {
    return cellIcon(cell).hasClass('fail')
  }

  boolean isWarn(int cell) {
    return cellIcon(cell).hasClass('warn')
  }

  boolean click(cell) {
    cell.find('a').click()
  }
}

class ComponentApplicationRow
    extends ComponentRow
{
  static final int EXPANDO = 0

  static final int ORG_APP = 1

  static final int RISK_PIE = 2

  static final int RISK_COUNT = 3

  static final int BUILD = 4

  static final int STAGE = 5

  static final int RELEASE = 6

  static final int OPERATE = 7

  static content = {
    expando { module ExpandoModule, cell(EXPANDO).find('i') }
    applicationImage { $('.image-thumbnail') }
    orgApp { cell(ORG_APP).text().trim() }
  }
}

class ComponentViolationRow
    extends ComponentRow
{
  static final int THREAT_LEVEL = 1

  static final int POLICY_NAME = 2

  static final int RISK_PIE = 3

  static final int RISK_COUNT = 4

  static final int BUILD = 5

  static final int STAGE = 6

  static final int RELEASE = 7

  static final int OPERATE = 8

  static content = {
    threatLevel { cell(THREAT_LEVEL).text().toInteger() }
    policyName { cell(POLICY_NAME).text().trim() }
  }

  boolean isLatestRisk(int cellIndex) {
    return cell(cellIndex).classes().contains('latest-risk')
  }
}
