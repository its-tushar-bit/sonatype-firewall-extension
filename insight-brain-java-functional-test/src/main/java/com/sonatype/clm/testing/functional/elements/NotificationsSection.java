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
    return new AddNotificationItem(".iq-policy-editor__add-notification");
  }

  public static ElementsCollection notifications() {
    return $$(createSelector(ROOT_SELECTOR, "tbody", "tr"));
  }

  public ElementsCollection headers() {
    return $$(createSelector(ROOT_SELECTOR, "th:nth-child(n+2):nth-child(-n+8)"));
  }

  public static class NotificationItem
  {
    private final String rootSelector;

    public NotificationItem(String recipient) {
      this.rootSelector =
          createSelector(NotificationsSection.ROOT_SELECTOR, "tr[data-recipient=\"" + recipient + "\"]");
    }

    public IqCheckbox proxy() {
      return new IqCheckbox($(createSelector(rootSelector, "td", nthChild(2), "iq-checkbox")));
    }

    public IqCheckbox develop() {
      return new IqCheckbox($(createSelector(rootSelector, "td", nthChild(3), "iq-checkbox")));
    }

    public IqCheckbox build() {
      return new IqCheckbox($(createSelector(rootSelector, "td", nthChild(4), "iq-checkbox")));
    }

    public IqCheckbox stageRelease() {
      return new IqCheckbox($(createSelector(rootSelector, "td", nthChild(5), "iq-checkbox")));
    }

    public IqCheckbox release() {
      return new IqCheckbox($(createSelector(rootSelector, "td", nthChild(6), "iq-checkbox")));
    }

    public IqCheckbox operate() {
      return new IqCheckbox($(createSelector(rootSelector, "td", nthChild(7), "iq-checkbox")));
    }

    public IqCheckbox continuousMonitoring() {
      return new IqCheckbox($(createSelector(rootSelector, "td:nth-last-child(2) iq-checkbox")));
    }

    public SelenideElement deleteButton() {
      return $(createSelector(rootSelector, " button"));
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
      return new Dropdown(rootSelector, ".iq-policy-editor__editor-notification-type");
    }

    public SelenideElement email() {
      return $(createSelector(rootSelector, ".iq-policy-editor__editor-notification-email"));
    }

    public Dropdown role() {
      return new Dropdown(rootSelector, "#recipient-role");
    }

    public Dropdown webhook() {
      return new Dropdown(rootSelector, "#recipient-webhook");
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

    public ErrorBox errorBox() {
      return new ErrorBox(createSelector(rootSelector, ".iq-alert.iq-alert--error"));
    }
  }
}
