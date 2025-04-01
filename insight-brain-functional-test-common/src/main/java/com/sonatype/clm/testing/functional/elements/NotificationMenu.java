/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class NotificationMenu
    extends BasicElement<NotificationMenu>
{
  public NotificationMenu() {
    super(".iq-notifications-menu-button");
  }

  public SelenideElement notificationDot() {
    return child(".iq-unread-dot");
  }

  public NotificationListItem notificationListItem(int index) {
    return new NotificationListItem(childSelector(".iq-notification", nthChild(index + 1)));
  }

  public SelenideElement detailModal() {
    return child(".iq-notification-detail-modal");
  }

  public SelenideElement detailBody() {
    return detailModal().$(".iq-notification-detail-modal-content");
  }

  public SelenideElement detailHeader() {
    return detailModal().$(".iq-notification-detail-modal-header");
  }

  public SelenideElement detailLink() {
    return detailBody().$("a");
  }

  public SelenideElement detailModalCloseButton() {
    return detailModal().$(".nx-btn");
  }

  public static class NotificationListItem
      extends BasicElement<NotificationListItem>
  {
    public NotificationListItem(String selector) {
      super(selector);
    }

    public SelenideElement age() {
      return child(".iq-notification__age");
    }

    public SelenideElement summary() {
      return child(".iq-notification__text");
    }
  }
}
