/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * Playwright page object for the notification menu in the header.
 */
public class NotificationMenuComponent
    extends BasePage
{
  private static final String ROOT = ".iq-notifications-menu-button";

  public NotificationMenuComponent() {
    super();
  }

  public Locator menuButton() {
    return byRole(AriaRole.BUTTON, "Notifications");
  }

  public Locator notificationDot() {
    return locator(ROOT + " .iq-unread-dot");
  }

  public void click() {
    menuButton().click();
  }

  public Locator notificationListItem(int index) {
    return locator(ROOT + " .iq-notification:nth-child(" + (index + 1) + ")");
  }

  public Locator notificationItemAge(int index) {
    return notificationListItem(index).locator(".iq-notification__age");
  }

  public Locator notificationItemSummary(int index) {
    return notificationListItem(index).locator(".iq-notification__text");
  }

  public Locator detailModal() {
    return locator(".iq-notification-detail-modal");
  }

  public Locator detailHeader() {
    return locator(".iq-notification-detail-modal-header");
  }

  public Locator detailBody() {
    return locator(".iq-notification-detail-modal-content");
  }

  public Locator detailModalCloseButton() {
    return detailModal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Close"));
  }

  public Locator detailLink() {
    return detailBody().locator("a");
  }

  // --------------- Actions ---------------

  public void clickDetailLinkOpeningPopup() {
    Page popup = page.waitForPopup(() -> detailLink().click());
    popup.waitForLoadState();
    popup.close();
  }

}
