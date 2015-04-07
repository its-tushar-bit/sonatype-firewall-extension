/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.BaseSpec
import spock.lang.Stepwise

@Stepwise
class NotificationSpec
    extends BaseSpec
{

  def setupSpec() {
    Date now = new Date()
    long tenMinutesAgo = now.getTime() - (1000 * 60 * 10)
    long tenHoursAgo = now.getTime() - (1000 * 60 * 60 * 10)
    saasRule.setResponseForURI('rest/productNotifications', '{"productNotifications":[{' +
        '"id" : "1",' +
        '"type" : "DEFAULT",' +
        '"summaryText" : "summary1",' +
        '"detailHtml" : "detail1",' +
        '"dateCreated" : ' + tenMinutesAgo +
        '},{' +
        '"id" : "2",' +
        '"type" : "DEFAULT",' +
        '"summaryText" : "summary2",' +
        '"detailHtml" : "detail2",' +
        '"dateCreated" : ' + tenHoursAgo +
        '}]}', 200)
    loginAsAdminVia()
  }

  def "Notification count is shown"() {
    expect: 'We are presented with the proper notification count'
      waitFor { notificationMenu.notificationCount.displayed }
      notificationMenu.notificationCount.text() == '2'
  }

  def "Proper notifications shown in dropdown"() {
    when: 'We click the dropdown'
     notificationMenu.dropdown.click()

    then: 'We are presented with the list of notifications'
      waitFor { notificationMenu.notificationList[0].displayed }
      notificationMenu.notificationList[0].age.text() == '10'
      notificationMenu.notificationList[0].ageLabel.text() == 'mins ago'
      notificationMenu.notificationList[0].summary.text() == 'summary1'
      notificationMenu.notificationList[1].age.text() == '10'
      notificationMenu.notificationList[1].ageLabel.text() == 'hours ago'
      notificationMenu.notificationList[1].summary.text() == 'summary2'
  }

  def "Notification detail panel shown on click"() {
    when: 'We click the first notification item'
      notificationMenu.notificationList[0].click()

    then: 'We are presented with the detail view'
      waitFor { notificationMenu.notificationList[0].detailHeader.text() == 'summary1' }
      notificationMenu.notificationList[0].detailBody.text() == 'detail1'

    and: 'The notification count has gone down'
      waitFor { notificationMenu.notificationCount.text() == '1' }

    when: 'We click the other notification'
      notificationMenu.notificationList[1].click()

    then: 'We are presented with the other detail view'
      waitFor { notificationMenu.notificationList[1].detailHeader.text() == 'summary2' }
      notificationMenu.notificationList[1].detailBody.text() == 'detail2'

    and: 'The notificiation count is gone'
      waitFor { !notificationMenu.notificationCount.displayed }

    when: 'We click the same notification'
      notificationMenu.notificationList[1].click()

    then: 'The detail panel is removed'
      waitFor { !notificationMenu.notificationList[1].detailHeader.displayed }
  }
}
