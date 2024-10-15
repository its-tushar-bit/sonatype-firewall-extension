/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.sbom;

import com.sonatype.clm.testing.functional.BasicElement;

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

  //Initial content
  public SelenideElement fileUpload() {
    return child(".nx-file-upload__input");
  }

  public SelenideElement errorAlert() {
    return child(".nx-alert--error");
  }

  //Uploading and commiting content
  public SelenideElement progressBar() {
    return child(".nx-progress-bar");
  }

  //Summary content
  public SelenideElement summaryApplicationName() {
    return child(".import-sbom-modal__application-name > dd");
  }

  public SelenideElement summaryInputVersionId() {
    return child(".nx-text-input__input");
  }

  public SelenideElement summaryTotalComponents() {
    return child("#import-sbom-modal-summary-total-components");
  }

  public SelenideElement summaryTotalVulnerabilities() {
    return child("#import-sbom-modal-summary-total-vulnerabilities");
  }

  public SelenideElement cancelCloseButton() {
    return $(".nx-btn--secondary");
  }

  public SelenideElement importSbomButton() {
    return child(".nx-btn--primary");
  }

  public SelenideElement fileSelected() {
    return child(".nx-selected-file__name");
  }

  // Binary summary
  public SelenideElement binaryFilename() {
    return child(".sbom-manager-import-sbom-modal__binary-summary > dd.filename");
  }

  public SelenideElement binaryAppName() {
    return child(".sbom-manager-import-sbom-modal__binary-summary > dd.application-name");
  }
}
