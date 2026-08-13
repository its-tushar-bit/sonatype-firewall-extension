/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

public class WebhookListPage
    extends BasePage
{
  private static final String ROOT = "#webhooks-list";

  private static final Locator.GetByRoleOptions H1_OPTS =
      new Locator.GetByRoleOptions().setLevel(1);

  private static final Locator.GetByRoleOptions H2_OPTS =
      new Locator.GetByRoleOptions().setLevel(2);

  private static final Locator.GetByRoleOptions ADD_WEBHOOK_OPTS =
      new Locator.GetByRoleOptions().setName("Add a Webhook");

  public WebhookListPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/webhooks/list";
  }

  /** Webhook list accessed from Firewall context. Matches ui-router state {@code firewall.listWebhooks}. */
  public static String firewallUrl() {
    return "/assets/index.html#/firewall/webhooks/list";
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator heading() {
    return locator(ROOT).getByRole(AriaRole.HEADING, H1_OPTS);
  }

  public Locator tileHeading() {
    return locator(ROOT).getByRole(AriaRole.HEADING, H2_OPTS);
  }

  public Locator addWebhookButton() {
    return locator(ROOT).getByRole(AriaRole.BUTTON, ADD_WEBHOOK_OPTS);
  }

  public Locator emptyMessage() {
    return locator(ROOT + " .nx-list__text");
  }

  public Locator webhookListItems() {
    return locator(ROOT + " .nx-list__item");
  }

  public Locator webhookItemByUrl(String url) {
    return locator(ROOT + " .nx-list__link").filter(
        new Locator.FilterOptions().setHasText(url));
  }
}
