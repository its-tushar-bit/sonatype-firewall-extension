/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.NotificationMenuComponent;
import com.sonatype.clm.testing.playwright.pages.NotificationMenuComponentAssertions;
import com.sonatype.clm.testing.playwright.pages.ReportListPage;
import org.junit.Before;
import org.junit.Test;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import org.junit.experimental.categories.Category;

/**
 * Playwright test for the Notification Menu.
 */
public class NotificationPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String NOTIFICATION_1_ID = "1";

  private static final String NOTIFICATION_1_SUMMARY = "summary1";

  private static final String NOTIFICATION_1_DETAIL = "detail1";

  private static final String NOTIFICATION_1_AGE_TEXT = "10 minutes ago";

  private static final long NOTIFICATION_1_AGE_OFFSET_MS = 600_000L;

  private static final String NOTIFICATION_2_ID = "2";

  private static final String NOTIFICATION_2_SUMMARY = "summary2";

  private static final String NOTIFICATION_2_DETAIL = "detail2";

  private static final String NOTIFICATION_2_AGE_TEXT = "10 hours ago";

  private static final long NOTIFICATION_2_AGE_OFFSET_MS = 36_000_000L;

  private static final String REACT2_SHELL_SUMMARY = "NEW: React2Shell Impact Report";

  private static final String REACT2_SHELL_AGE_TEXT = "Just now";

  private static final String REACT2_SHELL_URL_FRAGMENT = "/reports/react2shell";

  private static final String POST_NOTIFICATION_URL_FRAGMENT = "/reports/violations";

  /**
   * Builds the HDS-side {@code productNotifications} response body with dates pinned to "now",
   * shifted backwards by the per-row offset, so the UI renders the expected age text.
   */
  private static String hdsResponseJson() {
    long now = System.currentTimeMillis();
    return "{\"productNotifications\":[{" +
        "\"id\":\"" + NOTIFICATION_1_ID + "\"," +
        "\"type\":\"DEFAULT\"," +
        "\"summaryText\":\"" + NOTIFICATION_1_SUMMARY + "\"," +
        "\"detailHtml\":\"" + NOTIFICATION_1_DETAIL + "\"," +
        "\"dateCreated\":" + (now - NOTIFICATION_1_AGE_OFFSET_MS) +
        "},{" +
        "\"id\":\"" + NOTIFICATION_2_ID + "\"," +
        "\"type\":\"DEFAULT\"," +
        "\"summaryText\":\"" + NOTIFICATION_2_SUMMARY + "\"," +
        "\"detailHtml\":\"<a href='about:blank?foo' target='_blank'>" + NOTIFICATION_2_DETAIL + "</a>\"," +
        "\"dateCreated\":" + (now - NOTIFICATION_2_AGE_OFFSET_MS) +
        "}]}";
  }

  @Before
  public void stubNotificationsAndLogin() {
    testCLMServer.getHdsServer()
        .respondWith(hdsResponseJson())
        .atUri("rest/productNotifications");
    playwrightRefreshOrOpen(ReportListPage.url());
    playwrightLogin();
  }

  @Test
  @Category(SanityTest.class)
  public void testNotificationMenu() {
    NotificationMenuComponent notifications = new NotificationMenuComponent();
    NotificationMenuComponentAssertions notificationAssertions =
        new NotificationMenuComponentAssertions(notifications);
    notifications.click();

    notificationAssertions.shouldShowItemAge(0, REACT2_SHELL_AGE_TEXT);
    notificationAssertions.shouldShowItemSummary(0, REACT2_SHELL_SUMMARY);
    notificationAssertions.shouldShowItemAge(1, NOTIFICATION_1_AGE_TEXT);
    notificationAssertions.shouldShowItemSummary(1, NOTIFICATION_1_SUMMARY);
    notificationAssertions.shouldShowItemAge(2, NOTIFICATION_2_AGE_TEXT);
    notificationAssertions.shouldShowItemSummary(2, NOTIFICATION_2_SUMMARY);

    notifications.notificationListItem(0).click();
    playwrightWaitUntilUrlContains(REACT2_SHELL_URL_FRAGMENT);

    playwrightRefreshOrOpen(ReportListPage.url());

    notifications.click();
    notifications.notificationListItem(1).click();
    notificationAssertions.shouldShowDetailModal();
    notificationAssertions.shouldShowDetailContent(NOTIFICATION_1_SUMMARY, NOTIFICATION_1_DETAIL);
    notifications.detailModalCloseButton().click();
    notificationAssertions.shouldHideDetailModal();

    notifications.click();
    notifications.notificationListItem(2).click();
    notificationAssertions.shouldShowDetailModal();
    notificationAssertions.shouldShowDetailContent(NOTIFICATION_2_SUMMARY, NOTIFICATION_2_DETAIL);
    notifications.detailModalCloseButton().click();
    notificationAssertions.shouldHideDetailModal();

    notificationAssertions.shouldHideDot();

    notifications.click();
    notifications.notificationListItem(2).click();
    notificationAssertions.shouldShowDetailModal();

    notifications.clickDetailLinkOpeningPopup();

    playwrightWaitUntilUrlContains(POST_NOTIFICATION_URL_FRAGMENT);
  }
}
