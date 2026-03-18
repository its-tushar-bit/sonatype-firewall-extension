/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class EvaluateApplicationModal
    extends BasicElement<EvaluateApplicationModal>
{
  private static final String ROOT = "#evaluate-application-modal";

  public static final String SELECT_STAGE_TEXT = "Select Stage";

  public EvaluateApplicationModal() {
    super(ROOT);
  }

  public SelenideElement fileInput() {
    return $(" .nx-file-upload__input");
  }

  public SelenideElement dismissSelectedFileButton() {
    return $(".nx-selected-file__dismiss-btn");
  }

  public SelenideElement fileUploadError() {
    return $(".nx-file-upload__no-file-message");
  }

  public NxFormSelect stageSelect() {
    return new NxFormSelect(ROOT, ".nx-form-select__select");
  }

  public NotifyRadioButtons notifyRadioButtons() {
    return new NotifyRadioButtons();
  }

  public ProgressBar progressBar() {
    return new ProgressBar("#evaluate-application-modal .nx-progress-bar");
  }

  public SelenideElement uploadButton() {
    return $(" .nx-form__submit-btn");
  }

  public SelenideElement cancelButton() {
    return $(" .nx-form__cancel-btn");
  }

  public SelenideElement notificationsContainer() {
    return $$(".nx-fieldset").get(1);
  }

  public static SelenideElement disabledNotificationsMessage() {
    return $(".nx-alert__content");
  }

  public static class NotifyRadioButtons
  {
    public NxRadio yes() {
      return new NxRadio($$(".nx-radio-checkbox").get(0));
    }

    public NxRadio no() {
      return new NxRadio($$(".nx-radio-checkbox").get(1));
    }
  }

  public static class ProgressBar
      extends BasicElement<ProgressBar>
  {
    public ProgressBar(String selector) {
      super(selector);
    }

    public SelenideElement counter() {
      return child(".nx-counter");
    }

    public SelenideElement label() {
      return child(".nx-progress-bar__counter-and-label");
    }
  }
}
