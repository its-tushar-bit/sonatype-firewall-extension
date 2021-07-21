/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.ActionList;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

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

  public ElementsCollection webhooks() {
    return children(".nx-list__item.nx-list__item--link");
  }

  public WebhookListElement webhook(int num) {
    return new WebhookListElement(num);
  }

  public SelenideElement emptyListMessage() {
    return child(".nx-list__item--empty");
  }

  public class WebhookListElement
      extends BasicElement<ActionList.ActionListElement>
  {
    public WebhookListElement(int num) {
      super(ROOT_SELECTOR, "li:not(.nx-list__item--empty)", nthChild(num + 1));
    }

    public SelenideElement text() {
      return child(".nx-list__text");
    }

    public SelenideElement subtext() {
      return child(".nx-list__subtext");
    }

    public SelenideElement link() {
      return child(".nx-list__link");
    }
  }
}
