/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.text;
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
      return selector(rootSelector, "tr", nthChild(1), "td", nthChild(childNum));
    }

    public SelenideElement name() {
      return $(actionItemElementSelector(1));
    }

    public SelenideElement twisty() {
      return $(selector(actionItemElementSelector(1), ".twisty"));
    }

    public Radio noActionRadio() {
      return new Radio($(selector(actionItemElementSelector(2), ".radio")));
    }

    public Radio warnRadio() {
      return new Radio($(selector(actionItemElementSelector(3), ".radio")));
    }

    public Radio failRadio() {
      return new Radio($(selector(actionItemElementSelector(4), ".radio")));
    }

    public SelenideElement notificationCount() {
      return $(selector(actionItemElementSelector(5), ".notification-count"));
    }

    public static final Condition EXPANDED = cssClass("expand");

    public static final Condition COLLAPSED = cssClass("collapse");

    public AddNotificationItem addNotification() {
      return new AddNotificationItem(selector(rootSelector, "tr", nthChild(2), ".add-notification"));
    }

    public ElementsCollection notifications() {
      return $$(selector(rootSelector, "tr", nthChild(2), ".notification-list li"));
    }

    public NotificationItem getNotificationByName(String name) {
      return new NotificationItem(notifications().find(text(name)));
    }

  }

  public static class NotificationItem
  {

    private final SelenideElement root;

    public NotificationItem(final SelenideElement root) {
      this.root = root;
    }

    public SelenideElement deleteButton() {
      return root.$("button");
    }
  }

  public static class AddNotificationItem
  {

    private final String rootSelector;

    public AddNotificationItem(final String rootSelector) {
      this.rootSelector = rootSelector;
    }

    public DropdownSelector notificationType() {
      return new DropdownSelector($(selector(rootSelector, ".editor-notification-type")));
    }

    public SelenideElement email() {
      return $(selector(rootSelector, ".editor-notification-email"));
    }

    public DropdownSelector role() {
      return new DropdownSelector($(selector(rootSelector, ".editor-notification-role")));
    }

    public SelenideElement addButton() {
      return $(selector(rootSelector, "button"));
    }
  }
}
