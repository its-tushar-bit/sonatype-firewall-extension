/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class WebhookConfigurationPage
    extends BasicElement<WebhookConfigurationPage>
{
  public static String URL = BaseUrl.uriBuilder().fragment("/webhooks/list").build().toString();

  private static String ROOT_SELECTOR = "#webhooks-list";

  public WebhookConfigurationPage() {
    super(ROOT_SELECTOR);
  }

  public SelenideElement newWebhook() {
    return child("#create-webhook");
  }

  public ElementsCollection webhooksList() {
    return children(".webhook-item");
  }

  public WebhookSummary webhook(int row) {
    return new WebhookSummary(webhooksList().get(row));
  }

  public static class WebhookSummary {
    private SelenideElement element;

    WebhookSummary(SelenideElement element) {
      this.element = element;
    }

    public SelenideElement title() {
      return element.find(".title");
    }

    public ElementsCollection webhookEvents() {
      return element.findAll("li");
    }

    public void click() {
      element.click();
    }
  }
}
