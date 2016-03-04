/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.selector;

public class ActionItemList
{
  private String rootSelector;

  public ActionItemList(String rootSelector) {
    this.rootSelector = rootSelector;
  }

  public ElementsCollection elements() {
    return $$(rootSelector);
  }

  public ActionItem proxy() {
    return new ActionItem(selector(rootSelector, nthChild(1)));
  }

  public ActionItem develop() {
    return new ActionItem(selector(rootSelector, nthChild(2)));
  }

  public ActionItem build() {
    return new ActionItem(selector(rootSelector, nthChild(3)));
  }

  public ActionItem stageRelease() {
    return new ActionItem(selector(rootSelector, nthChild(4)));
  }

  public ActionItem release() {
    return new ActionItem(selector(rootSelector, nthChild(5)));
  }

  public ActionItem operate() {
    return new ActionItem(selector(rootSelector, nthChild(6)));
  }

  public ActionItem continuousMonitoring() {
    return new ActionItem(selector(rootSelector, nthChild(7)));
  }

  public static class ActionItem
  {
    private String rootSelector;

    public ActionItem(String rootSelector) {
      this.rootSelector = rootSelector;
    }

    private String actionItemElementSelector(int childNum) {
      return selector(rootSelector, "tr", nthChild(1), "td", nthChild(childNum + 1));
    }

    public SelenideElement name() {
      return $(actionItemElementSelector(0));
    }

    public SelenideElement twisty() {
      return $(selector(actionItemElementSelector(0), ".twisty"));
    }

    public Radio noActionRadio() {
      return new Radio($(selector(actionItemElementSelector(1), ".radio")));
    }

    public Radio warnRadio() {
      return new Radio($(selector(actionItemElementSelector(2), ".radio")));
    }

    public Radio failRadio() {
      return new Radio($(selector(actionItemElementSelector(3), ".radio")));
    }

    public SelenideElement notificationCount() {
      return $(selector(actionItemElementSelector(4), ".notification-count"));
    }

    public static final Condition EXPANDED = cssClass("expand");

    public static final Condition COLLAPSED = cssClass("collapse");

    public AddNotificationItem addNotification() {
      return new AddNotificationItem(selector(rootSelector, "tr", nthChild(2), ".add-notification"));
    }

    public String notificationsSelector() {
      return selector(rootSelector, "tr", nthChild(2), ".notification-list li");
    }

    public ElementsCollection notifications() {
      return $$(notificationsSelector());
    }

    public NotificationItem getNotification(int num) {
      return new NotificationItem(notificationsSelector(), nthChild(num));
    }
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
      return $(selector(rootSelector, ".editor-notification-email"));
    }

    public Dropdown role() {
      return new Dropdown(rootSelector, ".editor-notification-role");
    }

    public SelenideElement addButton() {
      return $(selector(rootSelector, "button"));
    }
  }
}
