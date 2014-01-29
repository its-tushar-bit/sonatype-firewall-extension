/**
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.model.Organization

import spock.lang.Stepwise

@Stepwise
class ReportViolationsSpec
extends BaseSpec {
  def setupSpec() {
    Organization org = createOrganization("org1");
    createApplication("ReportViolationsSpec1", "ReportViolationsSpec1", org.getId());
    org = createOrganization("org2");
    createApplication("ReportViolationsSpec2", "ReportViolationsSpec2", org.getId());

    loginAsAdmin();
  }

  def cleanupSpec() {
    cleanAppsAndOrgs()
  }

  def "Organization Sorting"() {
    given:
      ReportViolationsPage page = at ReportViolationsPage;
      waitFor { page.reportViolationRows.size() == 2; };
    when: "org header is clicked"
      page.orgNameHeader.click();
    then: "rows sorted by organization"
      page.reportViolationRows[0].orgName.text()== "org1";
      page.reportViolationRows[1].orgName.text() == "org2";

    when: "org header is clicked again"
      page.orgNameHeader.click();
    then: "rows reverse sorted by organization"
      page.reportViolationRows[0].orgName.text() == "org2";
      page.reportViolationRows[1].orgName.text() == "org1";
  }

  def "Filter organization" () {
    given:
      ReportViolationsPage page = at ReportViolationsPage;
    when: "I type a filter"
      page.filter << "org2";
    then:
      reportViolationRows.size() == 1;
      reportViolationRows[0].orgName.text() == "org2";
  }
}
