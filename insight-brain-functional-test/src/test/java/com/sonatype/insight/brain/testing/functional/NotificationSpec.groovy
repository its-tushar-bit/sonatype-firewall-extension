/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.NotificationItemModule
import com.sonatype.insight.brain.testing.functional.modules.NotificationModule

import org.openqa.selenium.interactions.Actions
import spock.lang.Stepwise

@Stepwise
class NotificationSpec
    extends BaseSpec
{
  static NotificationModule notificationMenu

  @Override
  def setupSpec() {
    Date now = new Date()
    long tenMinutesAgo = now.getTime() - (1000 * 60 * 10)
    long tenHoursAgo = now.getTime() - (1000 * 60 * 60 * 10)
    hdsRule.setResponseForURI('rest/productNotifications', '{"productNotifications":[{' +
        '"id" : "1",' +
        '"type" : "DEFAULT",' +
        '"summaryText" : "summary1",' +
        '"detailHtml" : "detail1",' +
        '"dateCreated" : ' + tenMinutesAgo +
        '},{' +
        '"id" : "2",' +
        '"type" : "DEFAULT",' +
        '"summaryText" : "summary2",' +
        '"detailHtml" : "<a href=\'http://www.google.com/ncr\' target=\'_blank\'>detail2</a>",' +
        '"dateCreated" : ' + tenHoursAgo +
        '}]}', 200)
    ReportViolationsPage reportViolationsPage = loginAsAdminVia(ReportViolationsPage)
    notificationMenu = reportViolationsPage.notificationMenu as NotificationModule
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
      notificationMenu.notificationList[0].ageLabel.text() == 'minutes ago'
      notificationMenu.notificationList[0].summary.text() == 'summary1'
      notificationMenu.notificationList[1].age.text() == '10'
      notificationMenu.notificationList[1].ageLabel.text() == 'hours ago'
      notificationMenu.notificationList[1].summary.text() == 'summary2'
  }

  def "Notification detail panel shown on click"() {
    when: 'We click the first notification item'
      notificationMenu.notificationList[0].click()

    then: 'We are presented with the detail view'
      waitFor { notificationMenu.detailHeader.text() == 'summary1' }
      notificationMenu.detailBody.text() == 'detail1'

    and: 'The notification count has gone down'
      waitFor { notificationMenu.notificationCount.text() == '1' }

    when: 'We click the other notification'
      notificationMenu.notificationList[1].click()

    then: 'We are presented with the other detail view'
      waitFor { notificationMenu.detailHeader.text() == 'summary2' }
      notificationMenu.detailBody.text() == 'detail2'

    and: 'The notificiation count is gone'
      waitFor { !notificationMenu.notificationCount.displayed }

    when: 'We click the same notification'
      notificationMenu.notificationList[1].click()

    then: 'The detail panel is removed'
      waitFor { !notificationMenu.detailHeader.displayed }
  }

  def 'Notification detail panel remains when clicking on it'() {
    when: 'We click the first notification item'
      NotificationItemModule firstNotificationItem = notificationMenu.notificationList[0]
      firstNotificationItem.click()

    then: 'We are presented with the detail view'
      waitFor { notificationMenu.detailHeader.text() == 'summary1' }
      notificationMenu.detailBody.text() == 'detail1'

    when: 'We click on the detail body'
      def actions = new Actions(driver)
      actions.moveToElement(notificationMenu.detailBody.firstElement(), 10, 10).click().build().perform()

    then: 'The detail panel remains'
      notificationMenu.detailBody.displayed

    when: 'We click on the detail header'
      actions.moveToElement(notificationMenu.detailHeader.firstElement(), 10, 10).click().build().perform()

    then: 'The detail panel remains'
      notificationMenu.detailHeader.displayed

    when: 'We click the same notification'
      actions.moveToElement(firstNotificationItem.firstElement(), 10, 10).click().build().perform()

    then: 'The detail panel is removed'
      waitFor { !notificationMenu.detailHeader.displayed }
  }

  def 'Clicking on link in detail panel opens in a new window'() {
    when: 'We click on the second notification item'
      NotificationItemModule secondNotificationItem = notificationMenu.notificationList[1]
      secondNotificationItem.click()

    then: 'the detail panel opens'
      waitFor { notificationMenu.detailBody.displayed }

    when: 'We click on the second notification detail link'
      notificationMenu.detailedBodyLinks.first().click()

    then: 'A link opens in a new tab'
      waitFor { getAvailableWindows().size() == 2 }
      withWindow(close: true, availableWindows[1]) {
        waitFor { driver.currentUrl.contains('google.com') }
      }
  }
}
