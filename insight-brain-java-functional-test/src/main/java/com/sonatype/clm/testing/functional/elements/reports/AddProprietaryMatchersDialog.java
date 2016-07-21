/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.reports;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class AddProprietaryMatchersDialog
    extends BasicElement<AddProprietaryMatchersDialog>
{
  private static final String FOOTER_SELECTOR = ".clm-modal-footer";

  public AddProprietaryMatchersDialog() {
    super("#add-proprietary-matchers-modal");
  }

  public ElementsCollection pathMatcherCheckboxes() {
    return children("#path-matchers", "input[type=checkbox]");
  }

  public SelenideElement regexInput() {
    return $("#regex-matcher");
  }

  public SelenideElement footer() {
    return child(FOOTER_SELECTOR);
  }

  public SelenideElement addButton() {
    return child(FOOTER_SELECTOR, ".clm-btn-primary");
  }

  public SelenideElement cancelButton() {
    return $("#dismissBtn");
  }

  public SelenideElement retryButton() {
    return child(FOOTER_SELECTOR, ".clm-btn-error");
  }

  public SelenideElement linkToAppConfig() {
    return child(".alert-info", "a[target=_blank]");
  }
}
