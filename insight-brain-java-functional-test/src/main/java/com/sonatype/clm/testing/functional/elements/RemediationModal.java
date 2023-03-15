/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class RemediationModal extends BasicElement<RemediationModal>
{
  private static final String ROOT = "#custom-remediation-modal";

  private static final String SCOPE_SELECT = "#remediation-scope";

  public RemediationModal() {
    super(ROOT);
  }

  public static ElementsCollection scopes() {
    return $$(ROOT + " " + SCOPE_SELECT + " .nx-form-select__select option");
  }

  public static ElementsCollection categories() {
    return $$(ROOT + " fieldset label.nx-radio-checkbox");
  }

  public static SelenideElement modalTitle() {
    return $("dialog header h2");
  }

  public static SelenideElement remediationMessage() {
    return $(ROOT + " #remediation-message textarea");
  }

  public static SelenideElement comment() {
    return $(ROOT + " #remediation-audit-comment textarea");
  }

  public static Button cancelButton() {
    return new Button("dialog .nx-form__cancel-btn");
  }

  public static Button saveButton() {
    return new Button("dialog .nx-form__submit-btn");
  }
}
