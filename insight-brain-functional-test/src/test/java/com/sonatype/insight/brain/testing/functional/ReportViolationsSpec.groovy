/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import org.openqa.selenium.Keys
import spock.lang.Stepwise

@Stepwise
class ReportViolationsSpec
    extends BaseSpec
{
  @Override
  def setupSpec() {
    temporaryEntity.
        newApplication('ReportViolationsSpec1', 'ReportViolationsSpec1', temporaryEntity.newOrganization('org1').id)
    temporaryEntity.
        newApplication('ReportViolationsSpec2', 'ReportViolationsSpec2', temporaryEntity.newOrganization('org2').id)
    loginAsAdminVia()
  }

  def "Organization Sorting"() {
    given:
      ReportViolationsPage page = at ReportViolationsPage
      // angular-vs-repeat adds a psuedo row before and after content
      waitFor { page.reportViolationRows.size() == 4 }

    when: "org header is clicked"
      clickHeader(orgNameHeader)
      waitFor { page.reportViolationRows[1].orgName }

    then: "rows sorted by organization"
      page.reportViolationRows[1].orgName.text() == "org1"
      page.reportViolationRows[2].orgName.text() == "org2"

    when: "org header is clicked again"
      clickHeader(orgNameHeader)
      waitFor { page.reportViolationRows[1].orgName }

    then: "rows reverse sorted by organization"
      page.reportViolationRows[1].orgName.text() == "org2"
      page.reportViolationRows[2].orgName.text() == "org1"
  }

  def "Filter organization"() {
    given:
      ReportViolationsPage page = at ReportViolationsPage

    when: "I type a filter"
      page.filter << "org2" + Keys.ENTER
      waitFor { reportViolationRows.size() == 3 }

    then:
      reportViolationRows[1].orgName.text() == "org2"
  }

  def "Stages are listed in chronological order"() {
    when: "viewing the report violations page"
      at ReportViolationsPage

    then: "the table header lists the stages in proper order"
      tableHeaders[3..6]*.@id ==
          ['report-list-header-source', 'report-list-header-build', 'report-list-header-stage-release',
          'report-list-header-release']
  }
}
