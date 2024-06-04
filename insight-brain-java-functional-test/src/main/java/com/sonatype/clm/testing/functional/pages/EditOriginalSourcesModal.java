/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.utils.SelectorUtils;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class EditOriginalSourcesModal
    extends BasicElement<EditOriginalSourcesModal>
{
  public EditOriginalSourcesModal() {
    super("#edit-original-sources-attribution-modal");
  }

  public Button addSourceButton() {
    return new Button("#add-source-link");
  }

  public SelenideElement originalSourceInputAt(int index) {
    return $("#source-" + index);
  }

  public ElementsCollection originalSourceInputs() {
    return children(".legal-modal-override-input-content input");
  }

  public SelenideElement originalSourceStatusToggleAt(int index) {
    return child("#source-status-toggle-" + index).parent();
  }

  public SelenideElement originalSourceStatusCheckboxAt(int index) {
    return child("#source-status-toggle-" + index);
  }

  public SelenideElement scopeDropdown() {
    return $("#edit-original-sources-scope-selection");
  }

  public StatusDropdown statusDropdown() {
    return new StatusDropdown();
  }

  public ElementsCollection statusDropdownItems() {
    return children("#edit-copyright-obligation-status-selection button");
  }

  public Button save() {
    return new Button(childSelector(".nx-btn--primary"));
  }

  public Button cancel() {
    return new Button(childSelector(".nx-form__cancel-btn"));
  }

  public static class StatusDropdown
      extends BasicElement<StatusDropdown>
  {
    public StatusDropdown() {
      super("#edit-copyright-obligation-status-selection");
    }

    public SelenideElement selectedStatus() {
      return child(".nx-dropdown__toggle");
    }

    public SelenideElement openMenuButton() {
      return child(".nx-dropdown__toggle");
    }

    public StatusDropdownMenu dropdownMenu() {
      return new StatusDropdownMenu(selector);
    }
  }

  public static class StatusDropdownMenu
      extends BasicElement<StatusDropdownMenu>
  {
    public StatusDropdownMenu(String selector) {
      super(selector, ".nx-dropdown-menu");
    }

    public ElementsCollection options() {
      return children(".edit-copyright-obligation-status-selection__option");
    }

    public StatusDropdownOption option(int i) {
      return new StatusDropdownOption("edit-copyright-obligation-status-selection__option",
          SelectorUtils.nthChild(i + 1));
    }
  }

  public static class StatusDropdownOption
      extends BasicElement<StatusDropdownOption>
  {
    public StatusDropdownOption(String... selectors) {
      super(selectors);
    }
  }
}
