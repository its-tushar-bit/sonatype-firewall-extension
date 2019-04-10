/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.ErrorBox;
import com.sonatype.clm.testing.functional.elements.IqBackButton;
import com.sonatype.clm.testing.functional.elements.IqCheckbox;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class WebhookEditPage
    extends BasicElement<WebhookEditPage>
{
  public static String url(String id) {
    return BaseUrl.resolvePageUrl("/webhooks/{id}", id);
  }

  private static String ROOT_SELECTOR = "#webhook-editor";

  public WebhookEditPage() {
    super(ROOT_SELECTOR);
  }

  public IqBackButton backButton() {
    return new IqBackButton(ROOT_SELECTOR);
  }

  public SelenideElement title() {
    return child(".iq-tile-header__title");
  }

  public SelenideElement url() {
    return child("#editor-webhook-url");
  }

  public SelenideElement secretKey() {
    return child("#editor-webhook-secret-key");
  }

  public ElementsCollection eventTypes() {
    return children("iq-checkbox");
  }

  public IqCheckbox management() {
    return new IqCheckbox(eventTypes().get(0));
  }

  public IqCheckbox applicationEvaluation() {
    return new IqCheckbox(eventTypes().get(1));
  }

  public IqCheckbox component() {
    return new IqCheckbox(eventTypes().get(3));
  }

  public SelenideElement save() {
    return child(".iq-btn--primary");
  }

  public SelenideElement remove() {
    return child(".iq-btn--tertiary");
  }

  public ErrorBox errorAlert() {
    return new ErrorBox(childSelector(".iq-alert--error"));
  }

  public SelenideElement form() {
    return child(".iq-form");
  }

  public static SelenideElement disabledApplicationEvaluationMessage() {
    return $("#application-evaluation-disabled-message");
  }
}
