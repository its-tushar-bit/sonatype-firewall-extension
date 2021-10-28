/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class AddProprietaryComponentMatchersPopover
    extends BasicElement<AddProprietaryComponentMatchersPopover>
{
  private static final String ROOT = "#component-details-add-proprietary-component-matchers-popover";

  public AddProprietaryComponentMatchersPopover() {
    super(ROOT);
  }

  public Button closeBtn() {
    return new Button("#add-proprietary-component-matchers-btn");
  }

  public ElementsCollection alerts() {
    return children(".nx-alert");
  }

  public ElementsCollection matchers() {
    return children(".nx-checkbox");
  }

  public SelenideElement regexInput() {
    return child("#add-proprietary-component-matchers-regex");
  }

  public Button cancelBtn() {
    return new Button(".nx-form__cancel-btn");
  }

  public Button addBtn() {
    return new Button(".nx-form__submit-btn");
  }
}
