/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class NxCollapsible
    extends BasicElement<NxCollapsible>
{
  public NxCollapsible(String selector) {
    super(selector);
  }

  public SelenideElement header() {
    return child(".nx-collapsible-items__header");
  }

  public SelenideElement trigger() {
    return header().find(".nx-collapsible-items__trigger");
  }

  public ElementsCollection children() {
    return children(".nx-collapsible-items__child");
  }

  public SelenideElement applicationsPlusIcon() {
    return child(".nx-icon-dropdown__toggle");
  }

  public SelenideElement organizationPlusIcon() {
    return child(".nx-btn--icon-only");
  }

  public ElementsCollection applicationsDropdownMenuItems() {
    return children(".nx-dropdown-menu .nx-dropdown-button");
  }

  public SelenideElement applicationsDropdownMenuItems(int idx) {
    return applicationsDropdownMenuItems().get(idx);
  }
}
