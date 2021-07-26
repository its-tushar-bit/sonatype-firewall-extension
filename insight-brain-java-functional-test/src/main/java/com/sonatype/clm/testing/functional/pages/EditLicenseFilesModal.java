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

import static com.codeborne.selenide.Selenide.$;

public class EditLicenseFilesModal
    extends BasicElement<EditLicenseFilesModal>
{
  public EditLicenseFilesModal() {
    super("#edit-licenses-attribution-modal");
  }

  public LicenseFile licenseAt(int index) {
    return new LicenseFile("#license-row-" + index);
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
    return new Button(childSelector(".nx-form__cancel-btn"));
  }

  public static class LicenseFile
      extends BasicElement<LicenseFile>
  {
    LicenseFile(String selector) {
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
