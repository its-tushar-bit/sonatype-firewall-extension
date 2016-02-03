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
import static com.codeborne.selenide.Selenide.$$;

public class ActionItemList
{
  private String rootSelector;

  public ActionItemList(String rootSelector) {
    this.rootSelector = rootSelector;
  }

  public ElementsCollection elements() {
    return $$(rootSelector + " tr");
  }

  public ActionItem proxy() {
    return new ActionItem($$(rootSelector + " tr.action-stage").get(0));
  }

  public ActionItem develop() {
    return new ActionItem($$(rootSelector + " tr.action-stage").get(1));
  }

  public ActionItem build() {
    return new ActionItem($$(rootSelector + " tr.action-stage").get(2));
  }

  public ActionItem stageRelease() {
    return new ActionItem($$(rootSelector + " tr.action-stage").get(3));
  }

  public ActionItem release() {
    return new ActionItem($$(rootSelector + " tr.action-stage").get(4));
  }

  public ActionItem operate() {
    return new ActionItem($$(rootSelector + " tr.action-stage").get(5));
  }

  public ActionItem continuousMonitoring() {
    return new ActionItem($$(rootSelector + " tr.action-stage").get(6));
  }

  public static class ActionItem
  {
    private SelenideElement root;

    public ActionItem(SelenideElement root) {
      this.root = root;
    }

    public SelenideElement name() {
      return root.$("td:nth-child(1)");
    }

    public SelenideElement twisty() {
      return root.$("td:nth-child(1) .twisty");
    }

    public Radio warnRadio() {
      return new Radio(root.$("td:nth-child(3) .radio"));
    }

    public Radio failRadio() {
      return new Radio(root.$("td:nth-child(4) .radio"));
    }

    public Radio noActionRadio() {
      return new Radio(root.$("td:nth-child(2) .radio"));
    }

    public SelenideElement notificationCount() {
      return root.$("td:nth-child(5) .notification-count");
    }

    public static final Condition EXPANDED = cssClass("expand");

    public static final Condition COLLAPSED = cssClass("collapse");

    public ElementsCollection notifications() {
      return root.parent().$$("tr.expanded + tr div.notification-list li");
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
}
