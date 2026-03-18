/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class ImportSbomModal
    extends BasicElement<ImportSbomModal>
{
  private static final String ROOT = "#import-sbom-modal";

  public static final String SELECT_STAGE_TEXT = "Select Stage";

  public ImportSbomModal() {
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

  public ProgressBar progressBar() {
    return new ProgressBar("#import-sbom-modal .nx-progress-bar");
  }

  public SelenideElement uploadButton() {
    return $(" .nx-form__submit-btn");
  }

  public SelenideElement cancelButton() {
    return $(" .nx-form__cancel-btn");
  }

  public SelenideElement submitButton() {
    return $(".import-sbom-modal__submit-button");
  }

  public SelenideElement applicationNameTextInput() {
    return $("#import-sbom-modal-application-name-input");
  }

  public SelenideElement versionIdInput() {
    return $("#import-sbom-modal-version-id-input");
  }

  public SelenideElement infoAlert() {
    return $("#import-sbom-modal-info-alert");
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
