/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;

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
    return new AddNotificationItem("#iq-policy-editor__add-notification");
  }

  public SelenideElement header() {
    return $(ROOT_SELECTOR + " thead");
  }

  public static ElementsCollection notifications() {
    return $$(createSelector(ROOT_SELECTOR, "tbody", "tr"));
  }

  public ElementsCollection headers() {
    return $$(createSelector(ROOT_SELECTOR, "th:nth-child(n+2):nth-child(-n+9)"));
  }

  public NxRadio inheritParentNotifications() {
    return new NxRadio($("#edit-policy-notifications-override-inherit"));
  }

  public NxRadio overrideParentNotifications() {
    return new NxRadio($("#edit-policy-notifications-override-override"));
  }

  public SelenideElement notificationsOverrideSection() {
    return $(createSelector("#edit-policy-notifications-override"));
  }

  public static class NotificationItem
  {
    private final String rootSelector;

    public NotificationItem(String recipient) {
      this.rootSelector =
          createSelector(NotificationsSection.ROOT_SELECTOR, "tr[data-recipient=\"" + recipient + "\"]");
    }

    public NxCheckbox proxy() {
      return new NxCheckbox($(createSelector(rootSelector, "td", nthChild(2), ".nx-checkbox")));
    }

    public NxCheckbox develop() {
      return new NxCheckbox($(createSelector(rootSelector, "td", nthChild(3), ".nx-checkbox")));
    }

    public NxCheckbox source() {
      return new NxCheckbox($(createSelector(rootSelector, "td", nthChild(4), ".nx-checkbox")));
    }

    public NxCheckbox build() {
      return new NxCheckbox($(createSelector(rootSelector, "td", nthChild(5), ".nx-checkbox")));
    }

    public NxCheckbox stageRelease() {
      return new NxCheckbox($(createSelector(rootSelector, "td", nthChild(6), ".nx-checkbox")));
    }

    public NxCheckbox release() {
      return new NxCheckbox($(createSelector(rootSelector, "td", nthChild(7), ".nx-checkbox")));
    }

    public NxCheckbox operate() {
      return new NxCheckbox($(createSelector(rootSelector, "td", nthChild(8), ".nx-checkbox")));
    }

    public NxCheckbox continuousMonitoring() {
      return new NxCheckbox($(createSelector(rootSelector, "td:nth-last-child(2) .nx-checkbox")));
    }

    public SelenideElement deleteButton() {
      return $(createSelector(rootSelector, " button"));
    }
  }

  public static class AddNotificationItem
  {
    public static WebElementCondition ISSUE_TYPE_NEEDS_PROJECT = text("-- Select JIRA Project --");

    private final String rootSelector;

    public AddNotificationItem(final String rootSelector) {
      this.rootSelector = rootSelector;
    }

    public NxFormSelect notificationType() {
      return new NxFormSelect(rootSelector, "#recipient-type");
    }

    public SelenideElement email() {
      return $(createSelector(rootSelector, "#recipient-email"));
    }

    public NxFormSelect role() {
      return new NxFormSelect(rootSelector, "#recipient-role");
    }

    public NxFormSelect webhook() {
      return new NxFormSelect(rootSelector, "#recipient-webhook");
    }

    public NxFormSelect project() {
      return new NxFormSelect(rootSelector, "#recipient-jira-project");
    }

    public NxFormSelect issueType() {
      return new NxFormSelect(rootSelector, "#recipient-jira-issue-type");
    }

    public SelenideElement addButton() {
      return $(createSelector(rootSelector, "button"));
    }

    public ErrorBox errorBox() {
      return new ErrorBox(createSelector(rootSelector, ".iq-alert.iq-alert--error"));
    }
  }
}
