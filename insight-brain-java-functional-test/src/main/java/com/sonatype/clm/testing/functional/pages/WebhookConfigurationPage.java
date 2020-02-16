/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.ActionList;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

public class WebhookConfigurationPage
    extends BasicElement<WebhookConfigurationPage>
{
  public static String url() {
    return BaseUrl.resolvePageUrl("/webhooks/list");
  }

  private static String ROOT_SELECTOR = "#webhooks-list";

  public WebhookConfigurationPage() {
    super(ROOT_SELECTOR);
  }

  public SelenideElement newWebhook() {
    return child("#create-webhook");
  }

  public ActionList webhooksList() {
    return new ActionList(".iq-list");
  }
}
