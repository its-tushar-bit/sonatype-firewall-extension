/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.google.common.io.Resources
import com.sonatype.insight.brain.dataaccess.security.UserDAO
import com.sonatype.insight.brain.model.security.User
import com.sonatype.insight.brain.security.CLMRealm
import com.sonatype.insight.brain.service.InsightBrainService
import com.sonatype.insight.brain.service.InsightConfig
import com.yammer.dropwizard.testing.junit.DropwizardServiceRule
import geb.spock.GebReportingSpec
import org.junit.ClassRule
import org.junit.rules.TestRule
import spock.lang.Shared
import spock.lang.Stepwise

/**
 * @since 1.7
 */
@Stepwise
class ReportSpec
    extends GebReportingSpec
{
  @Shared
  @ClassRule
  TestRule startServiceRule = new DropwizardServiceRule<InsightConfig>(InsightBrainService.class,
      Resources.getResource('config-test.yml').getPath())

  @Shared User nonAdminUser

  def setupSpec() {
    UserDAO userDAO = new UserDAO()
    nonAdminUser = new User(username: "test", password: new CLMRealm().encryptPassword("secret"), firstName: "John",
        lastName: "Doe", email: "john@doe.net")
    userDAO.insert(nonAdminUser);

    to LoginPage
    loginAsAdmin()
    waitFor { at ReportPage }
  }

  def cleanupSpec() {
    new UserDAO().delete(nonAdminUser)
  }

  def "When we first login we're invited to create a new Org"() {
    expect:
    emptyMessage.displayed
  }

  def "We can navigate to the trending report"() {
    when: 'we click the navigation link to Trending'
    nav.link('Trending').click()

    then: 'we see the large loading progress meter'
    at TrendingReportPage
    loadingText.startsWith('CLM Server is generating the trending report')
  }

  def "We can load the (empty) report"(){
    when: 'the report is generated and we refresh the page'
    browser.driver.navigate().refresh()

    then: 'we see that no violations have occurred, since we have not scanned anything'
    at TrendingReportPage
    refresh.displayed
    componentCount == '0 Components across all Applications'
    policyCount == '0 Policies'
    applicationCount ==  '0 Applications'
    violationCount == '0 Violations'
  }

  def "We can regenerate a report"() {
    def dateMatcher = reportDate.text() =~ /Generated on: (.*)/
    dateMatcher.size() == 1
    dateMatcher[0].size() == 2
    def originalDate = Date.parse("MMM dd - hh:mm a, yyyy", dateMatcher[0][1]);

    when: "we click the refresh button"
      js.exec '$( ".content" ).scrollLeft( 300 );'
      refresh.click()
      interact {
        moveToElement(refresh)
      }

    then: "refresh tooltip shows up"
      tooltip.displayed
      tooltip.text() == "Report generation running 0 seconds, total number of applications 0, applications processed so far 0"

    when: "report generation is finished"
      waitFor { !tooltip.displayed }
      dateMatcher = reportDate.text() =~ /Generated on: (.*)/

    then: "report is updated"
      dateMatcher.size() == 1
      dateMatcher[0].size() == 2
      def refreshDate = Date.parse("MMM dd - hh:mm a, yyyy", dateMatcher[0][1]);
      refreshDate.getTime() > originalDate.getTime()
  }

  def "A non-admin user cannot regenerate the report"(){
    when: 'we log in as a non-admin user'
    user.logout.link.click()
    at LoginPage
    login('test', 'secret')
    at ReportPage
    to TrendingReportPage

    then: 'no refresh button is displayed, but the report is visible'
    !refresh.displayed
    componentCount == '0 Components across all Applications'
    policyCount == '0 Policies'
    applicationCount ==  '0 Applications'
    violationCount == '0 Violations'
  }

}
