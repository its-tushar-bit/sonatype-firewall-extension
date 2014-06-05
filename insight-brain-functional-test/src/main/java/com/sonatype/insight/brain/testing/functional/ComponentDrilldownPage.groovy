/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
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
    totalRisk { $('#total-risk')}

    componentApplicationTable { $('.component-application-table') }
    componentApplicationRow { applicationId -> module ComponentApplicationRow, $("#app-row-${applicationId}") }
    componentViolationTable { applicationId -> $("div[id\$='${applicationId}'] .table") }
    componentViolationRow { applicationId, policyViolationName -> module ComponentViolationRow,
      componentViolationTable(applicationId).find('tr').has("td", text: "${policyViolationName}") }
    header(required:false) { int i -> $('thead th', i).text() }
  }
}

class ComponentApplicationRow
    extends Module
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
    cell(required: false) { int i -> $('td', i) }
    cellIcon(required: false) { int i -> $('td', i).find('i') }
    expando { module ExpandoModule, cell(EXPANDO).find('i') }
    applicationImage { $('.image-thumbnail') }
    orgApp { cell(ORG_APP).text().trim() }
    riskPie { cell(RISK_PIE).text().trim() }
    riskCount { cell(RISK_COUNT).text().toInteger() }
    build { cell(BUILD).text() }
  }

  boolean isFail(int cell) {
    return cellIcon(cell).hasClass('fail')
  }

  boolean isWarn(int cell) {
    return cellIcon(cell).hasClass('warn')
  }
}

class ComponentViolationRow
    extends Module
{
  static final int THREAT_LEVEL = 1
  static final int POLICY_NAME = 2
  static final int RISK_PIE = 3
  static final int RISK_COUNT = 4
  static final int BUILD = 5

  static content = {
    cell(required: false) { int i -> $('td', i) }
    cellIcon(required: false) { int i -> $('td', i).find('i') }
    threatLevel { cell(THREAT_LEVEL).text().toInteger() }
    policyName { cell(POLICY_NAME).text().trim() }
    riskPie { cell(RISK_PIE).text().trim() }
    riskCount { cell(RISK_COUNT).text().toInteger() }
    build { cell(BUILD).text() }
  }

  boolean isFail(int cell) {
    return cellIcon(cell).hasClass('fail')
  }

  boolean isWarn(int cell) {
    return cellIcon(cell).hasClass('warn')
  }
}
