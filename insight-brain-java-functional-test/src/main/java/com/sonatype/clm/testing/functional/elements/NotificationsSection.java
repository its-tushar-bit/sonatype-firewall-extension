/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class NotificationsSection
{
  public static final String ROOT_SELECTOR = "#policy-edit-notifications";

  public static NotificationItem notificationFor(String recipient) {
    return new NotificationItem(recipient);
  }

  public static AddNotificationItem addNotification() {
    return new AddNotificationItem(".add-notification");
  }

  public static ElementsCollection notifications() {
    return $$(createSelector(ROOT_SELECTOR, "tbody", "tr"));
  }

  public ElementsCollection headers() {
    return $$(createSelector(ROOT_SELECTOR, "th:nth-child(n+2):nth-child(-n+8)"));
  }

  public static class NotificationItem
  {

    private final String ROOT_SELECTOR;

    public NotificationItem(String recipient) {
      this.ROOT_SELECTOR = createSelector(NotificationsSection.ROOT_SELECTOR, "tr[data-recipient=\"" + recipient + "\"]");
    }

    public Checkbox proxy() {
      return new Checkbox($(createSelector(ROOT_SELECTOR, "td", nthChild(2))));
    }

    public Checkbox develop() {
      return new Checkbox($(createSelector(ROOT_SELECTOR, "td", nthChild(3))));
    }

    public Checkbox build() {
      return new Checkbox($(createSelector(ROOT_SELECTOR, "td", nthChild(4))));
    }

    public Checkbox stageRelease() {
      return new Checkbox($(createSelector(ROOT_SELECTOR, "td", nthChild(5))));
    }

    public Checkbox release() {
      return new Checkbox($(createSelector(ROOT_SELECTOR, "td", nthChild(6))));
    }

    public Checkbox operate() {
      return new Checkbox($(createSelector(ROOT_SELECTOR, "td", nthChild(7))));
    }

    public Checkbox continuousMonitoring() {
      return new Checkbox($(createSelector(ROOT_SELECTOR, "td:nth-last-child(2) label.checkbox")));
    }

    public SelenideElement deleteButton() {
      return $(createSelector(ROOT_SELECTOR, " button"));
    }
  }

  public static class AddNotificationItem
  {
    public static Condition ISSUE_TYPE_NEEDS_PROJECT = text("-- Select JIRA Project --");

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
      return new Dropdown(rootSelector, "#recipient-role");
    }

    public Dropdown project() {
      return new Dropdown(rootSelector, "#recipient-jira-project");
    }

    public Dropdown issueType() {
      return new Dropdown(rootSelector, "#recipient-jira-issue-type");
    }

    public SelenideElement addButton() {
      return $(createSelector(rootSelector, "button"));
    }
  }

}
