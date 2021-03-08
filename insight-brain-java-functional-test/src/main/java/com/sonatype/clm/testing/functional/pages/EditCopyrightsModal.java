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

public class EditCopyrightsModal
    extends BasicElement<EditCopyrightsModal>
{
  public EditCopyrightsModal() {
    super("#edit-copyright-attribution-modal");
  }

  public Button addCopyrightButton() {
    return new Button("#add-copyright");
  }

  public SelenideElement copyrightInputAt(int index) {
    return $("#copyright-" + index);
  }

  public ElementsCollection copyrightInputs() {
    return children(".copyright-override-input-content input");
  }

  public SelenideElement copyrightStatusToggleAt(int index) {
    return child("#copyright-status-toggle-" + index).parent();
  }

  public SelenideElement copyrightStatusCheckboxAt(int index) {
    return child("#copyright-status-toggle-" + index);
  }

  public SelenideElement scopeDropdown() {
    return $("#edit-copyright-scope-selection");
  }

  public Button save() {
    return new Button(childSelector(".nx-btn--primary"));
  }

  public Button cancel() {
    //Note: currently no way to pass a class/id to cancel button
    return new Button(childSelector(".nx-btn--undefined"));
  }
}
