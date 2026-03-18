/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.sbom;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.ElementUtils;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class ImportSbomModal
    extends BasicElement<ImportSbomModal>
{
  public ImportSbomModal() {
    super("#import-sbom-modal");
  }

  public SelenideElement title() {
    return child(".nx-h2");
  }

  // Initial content
  public SelenideElement fileUpload() {
    return child(".nx-file-upload__input");
  }

  public SelenideElement errorAlert() {
    return child(".nx-alert--error");
  }

  public SelenideElement warnAlert() {
    return child(".nx-alert--warning");
  }

  public SelenideElement validationErrors() {
    return child("textarea");
  }

  public SelenideElement copyToClipboardButton() {
    return child(".nx-copy-to-clipboard .nx-btn");
  }

  // Uploading and commiting content
  public SelenideElement progressBar() {
    return child(".nx-progress-bar");
  }

  // Summary content
  public SelenideElement summaryApplicationName() {
    return child("#import-sbom-modal-application-name");
  }

  public SelenideElement summaryVersionId() {
    return child("#import-sbom-modal-application-version");
  }

  public SelenideElement summaryTotalComponents() {
    return child("#import-sbom-modal-summary-total-components");
  }

  public SelenideElement summaryTotalVulnerabilities() {
    return child("#import-sbom-modal-summary-total-vulnerabilities");
  }

  public SelenideElement viewSbomButton() {
    return $("#import-sbom-modal-summary-view-sbom");
  }

  public SelenideElement cancelCloseButton() {
    return $(".nx-modal .nx-btn--secondary");
  }

  public SelenideElement importSbomButton() {
    return child(".nx-btn--primary");
  }

  public SelenideElement skipValidationCheckbox() {
    return child(".nx-checkbox");
  }

  public SelenideElement fileSelected() {
    return child(".nx-selected-file__name");
  }

  // Binary summary
  public SelenideElement binaryFilename() {
    return child("#import-sbom-modal-filename");
  }

  public SelenideElement versionInput() {
    return child("#import-sbom-modal-version-input");
  }

  public void seeEvaluationInProgressPageAndWaitForDismissal() {
    SelenideElement page = $("#import-sbom-modal-evaluation-in-progress-content");
    ElementUtils.seeElementAndWaitForDismissal(page);
  }

  public void seeEvaluationCompletePageAndWaitForDismissal() {
    SelenideElement page = $("#import-sbom-modal-evaluation-complete-content");
    ElementUtils.seeElementAndWaitForDismissal(page);
  }
}
