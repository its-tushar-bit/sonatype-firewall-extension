/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.NxBackButton;
import com.sonatype.clm.testing.functional.elements.NxCheckbox;
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

  public NxBackButton backButton() {
    return new NxBackButton();
  }

  public SelenideElement title() {
    return child(".nx-page-title");
  }

  public SelenideElement url() {
    return child("#editor-webhook-url");
  }

  public SelenideElement secretKey() {
    return child("#editor-webhook-secret-key");
  }

  public ElementsCollection eventTypes() {
    return children(".nx-checkbox");
  }

  public NxCheckbox management() {
    return new NxCheckbox(eventTypes().get(3));
  }

  public NxCheckbox applicationEvaluation() {
    return new NxCheckbox(eventTypes().get(0));
  }

  public NxCheckbox violationAlert() {
    return new NxCheckbox(eventTypes().get(5));
  }

  public SelenideElement save() {
    return child(".nx-form__submit-btn");
  }

  public SelenideElement remove() {
    return child("#delete-webhook-button");
  }

  public AlertError errorAlert() {
    return new AlertError(childSelector(".nx-alert--error"));
  }

  public HttpUrlWarningModal httpUrlWarningModal() {
    return new HttpUrlWarningModal("#http-url-warning-modal");
  }

  public SelenideElement httpUrlWarningAlertMessage() {
    return child("#editor-webhook-url-http-alert");
  }

  public SelenideElement form() {
    return child(".nx-form");
  }

  public static SelenideElement disabledApplicationEvaluationMessage() {
    return $("#application-evaluation-disabled-message");
  }

  public class AlertError
      extends BasicElement<AlertError>
  {
    public AlertError(String... selector) {
      super(selector);
    }

    public SelenideElement retryButton() {
      return child("button");
    }
  }

  public class HttpUrlWarningModal
      extends BasicElement<HttpUrlWarningModal>
  {
    public HttpUrlWarningModal(String... selector) {
      super(selector);
    }

    public SelenideElement retryButton() {
      return child("button");
    }

    public SelenideElement content() {
      return child(".nx-modal-content");
    }

    public SelenideElement continueButton() {
      return child(".nx-form__submit-btn");
    }

    public SelenideElement cancelButton() {
      return child(".nx-form__cancel-btn");
    }
  }
}
