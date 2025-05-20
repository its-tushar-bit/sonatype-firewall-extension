/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

public class CreatePRModal
    extends BasicElement<CreatePRModal>
{
  public CreatePRModal() {
    super("#iq-create-pr-modal");
  }

  public SelenideElement createPullRequestModalHeader() {
    return child("#iq-create-pr-modal-header");
  }

  public SelenideElement createPrModalPrTitle() {
    return child("#iq-create-pr-modal-pr-title");
  }

  public SelenideElement createPrModalComponentName() {
    return child("#iq-create-pr-modal-component-name");
  }

  public SelenideElement createPrModalCurrentVersion() {
    return child("#iq-create-pr-modal-current-version");
  }

  public SelenideElement createPrModalTargetVersion() {
    return child("#iq-create-pr-modal-target-version");
  }

  public SelenideElement createPrModalBreakingChanges() {
    return child("#iq-create-pr-modal-breaking-changes");
  }

  public SelenideElement createPrModalDefaultBranch() {
    return child("#iq-create-pr-modal-default-branch");
  }

  public SelenideElement createPullRequestModalCreateButton() {
    return child(".nx-btn--primary");
  }

  public SelenideElement createPullRequestModalCancelButton() {
    return child(".nx-btn--secondary");
  }
}
