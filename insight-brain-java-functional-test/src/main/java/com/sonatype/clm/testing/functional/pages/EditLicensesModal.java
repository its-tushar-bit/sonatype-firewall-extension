/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.pages.ComponentLegalOverviewPage.License;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class EditLicensesModal
    extends BasicElement<EditLicensesModal>
{
  public EditLicensesModal() {
    super("#edit-licenses-attribution-modal");
  }

  public License licenseAt(int index) {
    return new License("#license-row-" + index);
  }

  public ElementsCollection allLicenses() {
    return children("tbody tr");
  }

  public Button addLicenseButton() {
    return new Button("#add-license");
  }

  public SelenideElement scopeDropdown() {
    return $("#edit-license-scope-selection");
  }

  public Button save() {
    return new Button(childSelector(".nx-btn--primary"));
  }

  public Button cancel() {
    return new Button(childSelector(".nx-btn--undefined"));
  }

  public static class License
      extends BasicElement<License>
  {
    License(String selector) {
      super(selector);
    }

    public SelenideElement textInput() {
      return child(".nx-text-input__input");
    }

    public SelenideElement statusCheckbox() {
      return child(".nx-toggle__input");
    }

    public SelenideElement statusToggle() {
      return child(".nx-toggle__content");
    }
  }
}
