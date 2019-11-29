/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.Date;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.NotificationMenu;
import com.sonatype.clm.testing.functional.pages.ReportListPage;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class NotificationTest
    extends AbstractFunctionalTest
{
  @Before
  public void before() throws Exception {
    Date now = new Date();
    long tenMinutesAgo = now.getTime() - (1000 * 60 * 10);
    long tenHoursAgo = now.getTime() - (1000 * 60 * 60 * 10);
    testCLMServer.getHdsServer().respondWith("{\"productNotifications\":[{" +
        "\"id\" : \"1\"," +
        "\"type\" : \"DEFAULT\"," +
        "\"summaryText\" : \"summary1\"," +
        "\"detailHtml\" : \"detail1\"," +
        "\"dateCreated\" : " + tenMinutesAgo +
        "},{" +
        "\"id\" : \"2\"," +
        "\"type\" : \"DEFAULT\"," +
        "\"summaryText\" : \"summary2\"," +
        "\"detailHtml\" : \"<a href='about:blank?foo' target='_blank'>detail2</a>\"," +
        "\"dateCreated\" : " + tenHoursAgo +
        "}]}").atUri("rest/productNotifications");

    refreshOrOpen(ReportListPage.URL);
    loginAsAdmin();
  }

  @After
  public void after() {
    // logout if not already logged out
    hardreset();
  }

  @Test
  public void testNotificationMenu() {
    NotificationMenu notificationMenu = new NotificationMenu();
    notificationMenu.notificationCount().shouldHave(text("2"));

    notificationMenu.dropdown().click();

    notificationMenu.notificationListItem(0).age().shouldHave(text("10"));
    notificationMenu.notificationListItem(0).ageLabel().shouldHave(text("minutes ago"));
    notificationMenu.notificationListItem(0).summary().shouldHave(text("summary1"));
    notificationMenu.notificationListItem(1).age().shouldHave(text("10"));
    notificationMenu.notificationListItem(1).ageLabel().shouldHave(text("hours ago"));
    notificationMenu.notificationListItem(1).summary().shouldHave(text("summary2"));

    notificationMenu.notificationListItem(0).click();
    notificationMenu.detailHeader().shouldHave(text("summary1"));
    notificationMenu.detailBody().shouldHave(text("detail1"));

    // notification count should go down
    notificationMenu.notificationCount().shouldHave(text("1"));

    notificationMenu.notificationListItem(1).click();
    notificationMenu.detailHeader().shouldHave(text("summary2"));
    notificationMenu.detailBody().shouldHave(text("detail2"));

    // notification count should disappear
    notificationMenu.notificationCount().shouldNotBe(visible);

    // click the same notification again to remove the details
    notificationMenu.notificationListItem(1).click();
    notificationMenu.detailHeader().shouldNotBe(visible);

    // open the first one again
    notificationMenu.notificationListItem(0).click();
    notificationMenu.detailHeader().shouldHave(text("summary1"));
    notificationMenu.detailBody().shouldHave(text("detail1"));

    // clicking on the details should not close them
    notificationMenu.detailHeader().click();
    notificationMenu.detailHeader().shouldBe(visible);
    notificationMenu.detailBody().shouldBe(visible);

    notificationMenu.detailBody().click();
    notificationMenu.detailHeader().shouldBe(visible);
    notificationMenu.detailBody().shouldBe(visible);

    // open the second notification again and click its link
    notificationMenu.notificationListItem(1).click();

    // click the link; ensure it opens in a new tab
    notificationMenu.detailLink().shouldBe(visible).click();
    Selenide.switchTo().window(1);
    waitUntilUrl("about:blank?foo");
    WebDriverRunner.getWebDriver().close();
    Selenide.switchTo().window(0);
    waitUntilUrl(ReportListPage.URL);
  }
}
