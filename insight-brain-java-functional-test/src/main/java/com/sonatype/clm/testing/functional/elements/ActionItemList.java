/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;

public class ActionItemList
{
  private String rootSelector;

  public ActionItemList(String rootSelector) {
    this.rootSelector = rootSelector;
  }

  public ElementsCollection elements() {
    return $$(rootSelector);
  }


  public static class NotificationItem
      extends BasicElement<NotificationItem>
  {
    public NotificationItem(String... selectors) {
      super(selectors);
    }

    public SelenideElement deleteButton() {
      return child("button");
    }
  }

  public static class AddNotificationItem
  {

    private final String rootSelector;

    public AddNotificationItem(final String rootSelector) {
      this.rootSelector = rootSelector;
    }

    public Dropdown notificationType() {
      return new Dropdown(rootSelector, ".editor-notification-type");
    }

    public SelenideElement email() {
      return $(createSelector(rootSelector, ".editor-notification-email"));
    }

    public Dropdown role() {
      return new Dropdown(rootSelector, ".editor-notification-role");
    }

    public SelenideElement addButton() {
      return $(createSelector(rootSelector, "button"));
    }
  }
}
