/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Checkbox;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class WebhookEditPage
    extends BasicElement<WebhookEditPage>
{
  private static String ROOT_SELECTOR = "#webhook-editor";

  public WebhookEditPage() {
    super(ROOT_SELECTOR);
  }

  public SelenideElement title() {
    return child(".title");
  }

  public SelenideElement url() {
    return child("#editor-webhook-url");
  }

  public SelenideElement secretKey() {
    return child("#editor-webhook-secret-key");
  }

  public ElementsCollection eventTypes() {
    return children(".checkbox");
  }

  public Checkbox management() {
    return new Checkbox(eventTypes().get(0));
  }

  public Checkbox applicationEvaluation() {
    return new Checkbox(eventTypes().get(1));
  }

  public Checkbox component() {
    return new Checkbox(eventTypes().get(2));
  }

  public SelenideElement save() {
    return child(".btn-primary");
  }
}
