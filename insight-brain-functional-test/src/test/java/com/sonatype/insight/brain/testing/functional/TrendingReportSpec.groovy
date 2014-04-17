/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.dataaccess.security.UserDAO
import com.sonatype.insight.brain.model.security.User
import com.sonatype.insight.brain.security.CLMRealm
import org.codehaus.plexus.util.FileUtils
import org.codehaus.plexus.util.IOUtil
import spock.lang.Shared
import spock.lang.Stepwise

/**
 * @since 1.7
 */
@Stepwise
class TrendingReportSpec
    extends BaseSpec 
{

  private static final File TEST_FILE = new File('target/test-brain-work/report/trending-report.json')

  @Shared User nonAdminUser

  def setupSpec() {
    //pre-emptively delete, as generation of this file will be triggered by any visit to the TrendingReportPage prior to this test
    TEST_FILE.delete()
    UserDAO userDAO = new UserDAO()
    nonAdminUser = new User(username: "test", password: new CLMRealm().encryptPassword("secret"), firstName: "John",
    lastName: "Doe", email: "john@doe.net")
    userDAO.insert(nonAdminUser);

    loginAsAdminVia()
  }

  def cleanupSpec() {
    new UserDAO().delete(nonAdminUser)
    assert TEST_FILE.delete() || !TEST_FILE.exists()
  }

  def "When we first login we're invited to create a new Org"() {
    expect:
      waitFor { emptyMessage.displayed }
  }

  def "We can navigate to the trending report"() {
    when: 'we click the navigation link to Trending'
      nav.link('Trending').click()

    then: 'we see the large loading progress meter'
      at TrendingReportPage
      waitFor(10, 0.05) { loadingText.startsWith('CLM Server is generating the trending report') }
  }

  def "We can load the (empty) report"() {
    when: 'the report is generated and we refresh the page'
      browser.driver.navigate().refresh()

    then: 'we see that no violations have occurred, since we have not scanned anything'
      at TrendingReportPage
      waitFor { trendingData.displayed }
      refresh.displayed
      componentCount == '0 Components across all Applications'
      policyCount == '0 Policies'
      applicationCount ==  '0 Applications'
      violationCount == '0 Violations'
  }

  def "A non-admin user cannot regenerate the report"() {
    when: 'we log in as a non-admin user'
      userOptions.logoutClick()
      via TrendingReportPage
      login.login('test', 'secret')
      verifyAt()

    then: 'no refresh button is displayed, but the report is visible'
      waitFor { trendingData.displayed }
      !refresh.displayed
      componentCount == '0 Components across all Applications'
      policyCount == '0 Policies'
      applicationCount ==  '0 Applications'
      violationCount == '0 Violations'
  }

  def "We display an accurate component chart"() {
    when:
      def json = IOUtil.toString(getClass().getResourceAsStream("/ReportTest/trending-report.json"), "UTF-8")
      json = json.replace("@generatedOn@", Long.toString(System.currentTimeMillis()))
      FileUtils.fileWrite(new File(serviceRule.configuration.sonatypeWork, 'report/trending-report.json'), "UTF-8", json)
      browser.driver.navigate().refresh()
      at TrendingReportPage

    then:
      waitFor { percentageChartControl.displayed }
      def chartWidth = percentageChartControl.getWidth()

      componentBars.size() == 3
      exactComponentBar.displayed
      partialComponentBar.displayed
      unknownComponentBar.displayed

      exactComponentBar.getWidth() / chartWidth == 0.5
      partialComponentBar.getWidth() / chartWidth == 0.3
      unknownComponentBar.getWidth() / chartWidth == 0.2
  }
}
